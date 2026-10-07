package com.ahadporkar.engram.core.data.repository

import com.ahadporkar.engram.core.data.mapper.toEntity
import com.ahadporkar.engram.core.data.mapper.toModel
import com.ahadporkar.engram.core.data.time.StudyTime
import com.ahadporkar.engram.core.database.dao.DeckDao
import com.ahadporkar.engram.core.database.dao.ReviewLogDao
import com.ahadporkar.engram.core.model.Deck
import com.ahadporkar.engram.core.model.DeckSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface DeckRepository {
    /** Decks with what is left to study today (daily limits already applied). */
    fun observeDeckSummaries(): Flow<List<DeckSummary>>

    fun observeDeck(id: Long): Flow<Deck?>

    suspend fun getDeck(id: Long): Deck?

    suspend fun getDecks(): List<Deck>

    /** Inserts when `deck.id == 0`, otherwise updates. Returns the id. */
    suspend fun saveDeck(deck: Deck): Long

    suspend fun deleteDeck(id: Long)
}

@Singleton
class OfflineDeckRepository @Inject constructor(
    private val deckDao: DeckDao,
    private val reviewLogDao: ReviewLogDao,
    private val studyTime: StudyTime,
) : DeckRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeDeckSummaries(): Flow<List<DeckSummary>> = studyTime.windows().flatMapLatest { window ->
        combine(
            deckDao.observeDecksWithCounts(cutoff = window.end.toEpochMilli()),
            reviewLogDao.observeDailyCounts(since = window.start.toEpochMilli()),
        ) { decks, doneToday ->
            val done = doneToday.associateBy { it.deckId }
            decks.map { row ->
                val deck = row.deck.toModel()
                val newLeft = (deck.newCardsPerDay - (done[deck.id]?.newCount ?: 0)).coerceAtLeast(0)
                val reviewsLeft = (deck.maxReviewsPerDay - (done[deck.id]?.reviewCount ?: 0)).coerceAtLeast(0)
                DeckSummary(
                    deck = deck,
                    newCount = minOf(row.newCount, newLeft),
                    learningCount = row.learningCount,
                    reviewCount = minOf(row.reviewCount, reviewsLeft),
                    noteCount = row.noteCount,
                )
            }
        }
    }

    override fun observeDeck(id: Long): Flow<Deck?> = deckDao.observeDeck(id).map { it?.toModel() }

    override suspend fun getDeck(id: Long): Deck? = deckDao.getDeck(id)?.toModel()

    override suspend fun getDecks(): List<Deck> = deckDao.getAll().map { it.toModel() }

    override suspend fun saveDeck(deck: Deck): Long {
        require(deck.name.isNotBlank()) { "Deck name must not be blank" }
        return if (deck.id == 0L) {
            deckDao.insert(deck.toEntity())
        } else {
            deckDao.update(deck.toEntity())
            deck.id
        }
    }

    override suspend fun deleteDeck(id: Long) = deckDao.delete(id)
}
