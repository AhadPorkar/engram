package com.ahadporkar.engram.core.learning.stats

import com.ahadporkar.engram.core.learning.session.StudyDayClock
import com.ahadporkar.engram.core.model.Card
import com.ahadporkar.engram.core.model.ReviewLog
import com.ahadporkar.engram.core.srs.CardPhase
import com.ahadporkar.engram.core.srs.FsrsScheduler
import java.time.Instant
import java.time.LocalDate

data class DayCount(val date: LocalDate, val count: Int)

data class CardCounts(
    val new: Int = 0,
    val learning: Int = 0,
    /** Review cards with stability < 21 days. */
    val young: Int = 0,
    /** Review cards with stability ≥ 21 days — long-term memory. */
    val mature: Int = 0,
    val suspended: Int = 0,
    val leeches: Int = 0,
) {
    val total: Int get() = new + learning + young + mature
}

data class StatsSnapshot(
    val counts: CardCounts,
    val reviewsToday: Int,
    val studyMillisToday: Long,
    val dailyGoal: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    /** Share of review-phase answers that were not "Again" in the window; `null` without data. */
    val trueRetention: Double?,
    val retentionSampleSize: Int,
    val reviewsPerDay: List<DayCount>,
    val forecast: List<DayCount>,
    /** Σ R over all cards with a memory state: the number of cards you would recall right now. */
    val estimatedKnown: Double,
    val averageRetrievability: Double?,
    /** Review cards with the lowest predicted recall. */
    val weakestCardIds: List<Long>,
) {
    val goalProgress: Float
        get() = if (dailyGoal <= 0) 1f else (reviewsToday.toFloat() / dailyGoal).coerceAtMost(1f)
}

/** Pure statistics over cards and review logs. */
class StatsCalculator(private val scheduler: FsrsScheduler, private val clock: StudyDayClock) {

    fun compute(
        cards: List<Card>,
        logs: List<ReviewLog>,
        now: Instant,
        dailyGoal: Int,
        windowDays: Int = DEFAULT_WINDOW,
    ): StatsSnapshot {
        val today = clock.studyDate(now)
        val logsByDay = logs.groupBy { clock.studyDate(it.reviewedAt) }
        val todaysLogs = logsByDay[today].orEmpty()

        val windowStart = today.minusDays(windowDays - 1L)
        val retentionLogs = logs.filter {
            it.phaseBefore == CardPhase.REVIEW && !clock.studyDate(it.reviewedAt).isBefore(windowStart)
        }

        val memory = cards
            .filterNot { it.suspended }
            .mapNotNull { card -> scheduler.retrievability(card.scheduling, now)?.let { card to it } }

        return StatsSnapshot(
            counts = countCards(cards),
            reviewsToday = todaysLogs.size,
            studyMillisToday = todaysLogs.sumOf { it.durationMillis },
            dailyGoal = dailyGoal,
            currentStreak = currentStreak(logsByDay.keys, today),
            longestStreak = longestStreak(logsByDay.keys),
            trueRetention = retentionLogs.takeIf { it.isNotEmpty() }
                ?.let { list -> list.count { it.rating.isSuccess }.toDouble() / list.size },
            retentionSampleSize = retentionLogs.size,
            reviewsPerDay = (0 until windowDays).map { offset ->
                val date = windowStart.plusDays(offset.toLong())
                DayCount(date, logsByDay[date]?.size ?: 0)
            },
            forecast = forecast(cards, today, windowDays),
            estimatedKnown = memory.sumOf { it.second },
            averageRetrievability = memory.takeIf { it.isNotEmpty() }?.map { it.second }?.average(),
            weakestCardIds = memory
                .filter { it.first.phase == CardPhase.REVIEW }
                .sortedBy { it.second }
                .take(WEAKEST_COUNT)
                .map { it.first.id },
        )
    }

    private fun countCards(cards: List<Card>): CardCounts {
        var counts = CardCounts()
        for (card in cards) {
            if (card.leech) counts = counts.copy(leeches = counts.leeches + 1)
            if (card.suspended) {
                counts = counts.copy(suspended = counts.suspended + 1)
                continue
            }
            counts = when (card.phase) {
                CardPhase.NEW -> counts.copy(new = counts.new + 1)
                CardPhase.LEARNING, CardPhase.RELEARNING -> counts.copy(learning = counts.learning + 1)
                CardPhase.REVIEW ->
                    if ((card.scheduling.stability ?: 0.0) >= MATURE_STABILITY_DAYS) {
                        counts.copy(mature = counts.mature + 1)
                    } else {
                        counts.copy(young = counts.young + 1)
                    }
            }
        }
        return counts
    }

    private fun forecast(cards: List<Card>, today: LocalDate, days: Int): List<DayCount> {
        val lastDay = today.plusDays(days - 1L)
        val counts = cards
            .filter { !it.suspended && !it.isNew }
            .map { clock.studyDate(it.scheduling.due).let { date -> if (date.isBefore(today)) today else date } }
            .filter { !it.isAfter(lastDay) }
            .groupingBy { it }
            .eachCount()
        return (0 until days).map { offset ->
            val date = today.plusDays(offset.toLong())
            DayCount(date, counts[date] ?: 0)
        }
    }

    companion object {
        const val DEFAULT_WINDOW = 30
        const val MATURE_STABILITY_DAYS = 21.0
        const val WEAKEST_COUNT = 10

        /** Consecutive study days ending today — or yesterday, if today has no reviews yet. */
        fun currentStreak(studyDays: Set<LocalDate>, today: LocalDate): Int {
            var day = when {
                today in studyDays -> today
                today.minusDays(1) in studyDays -> today.minusDays(1)
                else -> return 0
            }
            var streak = 0
            while (day in studyDays) {
                streak++
                day = day.minusDays(1)
            }
            return streak
        }

        fun longestStreak(studyDays: Set<LocalDate>): Int {
            var best = 0
            var run = 0
            var previous: LocalDate? = null
            for (day in studyDays.sorted()) {
                run = if (previous != null && previous.plusDays(1) == day) run + 1 else 1
                best = maxOf(best, run)
                previous = day
            }
            return best
        }
    }
}
