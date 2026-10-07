package com.ahadporkar.engram.core.data.repository

import androidx.room.withTransaction
import com.ahadporkar.engram.core.data.mapper.toEntity
import com.ahadporkar.engram.core.data.mapper.toModel
import com.ahadporkar.engram.core.data.mapper.toStudyCard
import com.ahadporkar.engram.core.data.time.StudyTime
import com.ahadporkar.engram.core.database.EngramDatabase
import com.ahadporkar.engram.core.database.dao.CardDao
import com.ahadporkar.engram.core.database.dao.DeckDao
import com.ahadporkar.engram.core.database.dao.NoteDao
import com.ahadporkar.engram.core.database.dao.ReviewLogDao
import com.ahadporkar.engram.core.learning.exercise.DistractorPool
import com.ahadporkar.engram.core.learning.session.SessionLimits
import com.ahadporkar.engram.core.learning.session.SessionPlanner
import com.ahadporkar.engram.core.model.Card
import com.ahadporkar.engram.core.model.CramOrder
import com.ahadporkar.engram.core.model.ReviewLog
import com.ahadporkar.engram.core.model.StudyCard
import com.ahadporkar.engram.core.model.StudyMode
import com.ahadporkar.engram.core.model.UserSettings
import com.ahadporkar.engram.core.srs.FsrsScheduler
import javax.inject.Inject
import javax.inject.Singleton

data class SessionRequest(
    /** `null` = all decks. */
    val deckId: Long?,
    val mode: StudyMode,
    val cramOrder: CramOrder = CramOrder.SHUFFLE,
    val cramLimit: Int = 50,
)

data class SessionData(
    val cards: List<StudyCard>,
    val pool: DistractorPool,
    val settings: UserSettings,
)

interface StudyRepository {
    suspend fun loadSession(request: SessionRequest): SessionData

    /** Writes the new card state and its review log atomically. Returns the log id. */
    suspend fun persistAnswer(card: Card, log: ReviewLog?): Long?

    /** Undo: restores [previous] and deletes the log written for the undone answer. */
    suspend fun revertAnswer(previous: Card, logId: Long?)
}

@Singleton
class OfflineStudyRepository @Inject constructor(
    private val database: EngramDatabase,
    private val deckDao: DeckDao,
    private val noteDao: NoteDao,
    private val cardDao: CardDao,
    private val reviewLogDao: ReviewLogDao,
    private val settingsRepository: SettingsRepository,
    private val studyTime: StudyTime,
) : StudyRepository {

    override suspend fun loadSession(request: SessionRequest): SessionData {
        val settings = settingsRepository.current()
        val planner = SessionPlanner(FsrsScheduler(settings.toSchedulerConfig()))
        val window = studyTime.window()
        val now = studyTime.now()
        val decks = deckDao.getAll().map { it.toModel() }.associateBy { it.id }

        val cards = if (request.mode == StudyMode.CRAM) {
            val all = cardDao.getActiveWithNotes(request.deckId).map { it.toStudyCard(decks[it.card.deckId]) }
            planner.planCram(all, request.cramOrder, now, request.cramLimit)
        } else {
            val candidates = cardDao.getStudyCandidates(request.deckId, window.end.toEpochMilli())
                .map { it.toStudyCard(decks[it.card.deckId]) }
            val doneToday = reviewLogDao.getDailyCounts(window.start.toEpochMilli()).associateBy { it.deckId }
            // Plan each deck with its own daily limits, then mix decks (interleaving across topics).
            val perDeck = candidates.groupBy { it.card.deckId }.map { (deckId, deckCards) ->
                val deck = decks[deckId]
                val limits = SessionLimits(
                    newCards = ((deck?.newCardsPerDay ?: 0) - (doneToday[deckId]?.newCount ?: 0)).coerceAtLeast(0),
                    reviews = ((deck?.maxReviewsPerDay ?: 0) - (doneToday[deckId]?.reviewCount ?: 0)).coerceAtLeast(0),
                )
                planner.plan(deckCards, now, window.end, limits)
            }
            roundRobin(perDeck)
        }

        return SessionData(cards = cards, pool = distractorPool(request.deckId), settings = settings)
    }

    override suspend fun persistAnswer(card: Card, log: ReviewLog?): Long? = database.withTransaction {
        cardDao.update(card.toEntity())
        log?.let { reviewLogDao.insert(it.toEntity()) }
    }

    override suspend fun revertAnswer(previous: Card, logId: Long?) {
        database.withTransaction {
            cardDao.update(previous.toEntity())
            if (logId != null) reviewLogDao.delete(logId)
        }
    }

    private suspend fun distractorPool(deckId: Long?): DistractorPool {
        val fronts = if (deckId != null) noteDao.frontsInDeck(deckId) else noteDao.randomFronts(POOL_SIZE)
        val backs = if (deckId != null) noteDao.backsInDeck(deckId) else noteDao.randomBacks(POOL_SIZE)
        // Small decks borrow distractors from the whole collection.
        val extraFronts = if (fronts.size < MIN_POOL) noteDao.randomFronts(POOL_SIZE) else emptyList()
        val extraBacks = if (backs.size < MIN_POOL) noteDao.randomBacks(POOL_SIZE) else emptyList()
        return DistractorPool(fronts = (fronts + extraFronts).distinct(), backs = (backs + extraBacks).distinct())
    }

    private fun <T> roundRobin(lists: List<List<T>>): List<T> {
        val result = ArrayList<T>(lists.sumOf { it.size })
        val iterators = lists.map { it.iterator() }
        var added: Boolean
        do {
            added = false
            for (iterator in iterators) {
                if (iterator.hasNext()) {
                    result += iterator.next()
                    added = true
                }
            }
        } while (added)
        return result
    }

    private companion object {
        const val POOL_SIZE = 60
        const val MIN_POOL = 8
    }
}
