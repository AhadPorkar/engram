package com.ahadporkar.engram.core.data.repository

import com.ahadporkar.engram.core.data.mapper.toModel
import com.ahadporkar.engram.core.data.time.StudyDayWindow
import com.ahadporkar.engram.core.data.time.StudyTime
import com.ahadporkar.engram.core.database.dao.CardDao
import com.ahadporkar.engram.core.database.dao.ReviewLogDao
import com.ahadporkar.engram.core.database.entity.CardWithNote
import com.ahadporkar.engram.core.learning.stats.StatsCalculator
import com.ahadporkar.engram.core.learning.stats.StatsSnapshot
import com.ahadporkar.engram.core.model.Card
import com.ahadporkar.engram.core.model.DailyProgress
import com.ahadporkar.engram.core.model.Note
import com.ahadporkar.engram.core.model.ReviewLog
import com.ahadporkar.engram.core.model.UserSettings
import com.ahadporkar.engram.core.srs.FsrsScheduler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** A word with its current probability of recall. */
data class WordMemory(val note: Note, val retrievability: Double?, val lapses: Int)

data class StatsOverview(
    val snapshot: StatsSnapshot,
    val weakest: List<WordMemory>,
    val leeches: List<WordMemory>,
)

interface StatsRepository {
    fun observeStats(): Flow<StatsOverview>

    fun observeDailyProgress(): Flow<DailyProgress>
}

@Singleton
class OfflineStatsRepository @Inject constructor(
    private val cardDao: CardDao,
    private val reviewLogDao: ReviewLogDao,
    private val settingsRepository: SettingsRepository,
    private val studyTime: StudyTime,
    @DefaultDispatcher private val computeDispatcher: CoroutineDispatcher,
) : StatsRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeStats(): Flow<StatsOverview> = combine(
        cardDao.observeAll(),
        reviewLogDao.observeAll(),
        cardDao.observeLeeches(),
        settingsRepository.settings,
        studyTime.windows(),
    ) { cards, logs, leeches, settings, window ->
        Inputs(cards.map { it.toModel() }, logs.map { it.toModel() }, leeches, settings, window)
    }.mapLatest { input ->
        val scheduler = FsrsScheduler(input.settings.toSchedulerConfig())
        val now = studyTime.now()
        val snapshot = StatsCalculator(scheduler, input.window.clock)
            .compute(input.cards, input.logs, now, input.settings.dailyGoal)
        val weakest = cardDao.getWithNotes(snapshot.weakestCardIds)
            .map { row ->
                val card = row.card.toModel()
                WordMemory(row.note.toModel(), scheduler.retrievability(card.scheduling, now), card.scheduling.lapses)
            }
            .sortedBy { it.retrievability ?: 0.0 }
            .distinctBy { it.note.id }
        val leeches = input.leeches.map { row ->
            val card = row.card.toModel()
            WordMemory(row.note.toModel(), scheduler.retrievability(card.scheduling, now), card.scheduling.lapses)
        }.distinctBy { it.note.id }
        StatsOverview(snapshot, weakest, leeches)
    }.flowOn(computeDispatcher)

    override fun observeDailyProgress(): Flow<DailyProgress> = combine(
        reviewLogDao.observeTimestamps(),
        settingsRepository.settings,
        studyTime.windows(),
        cardDao.observeAll(),
    ) { timestamps, settings, window, cards ->
        val days = timestamps.mapTo(HashSet()) { window.clock.studyDate(Instant.ofEpochMilli(it)) }
        val since = window.start.toEpochMilli()
        val cutoff = window.end.toEpochMilli()
        DailyProgress(
            reviewsToday = timestamps.count { it >= since },
            dailyGoal = settings.dailyGoal,
            streak = StatsCalculator.currentStreak(days, window.date),
            dueToday = cards.count { !it.suspended && it.phase != "NEW" && it.due < cutoff },
        )
    }.flowOn(computeDispatcher)

    private data class Inputs(
        val cards: List<Card>,
        val logs: List<ReviewLog>,
        val leeches: List<CardWithNote>,
        val settings: UserSettings,
        val window: StudyDayWindow,
    )
}
