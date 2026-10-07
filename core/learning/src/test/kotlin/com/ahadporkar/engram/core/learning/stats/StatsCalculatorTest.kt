package com.ahadporkar.engram.core.learning.stats

import com.ahadporkar.engram.core.learning.TestData
import com.ahadporkar.engram.core.learning.TestData.NOW
import com.ahadporkar.engram.core.learning.session.StudyDayClock
import com.ahadporkar.engram.core.model.ExerciseType
import com.ahadporkar.engram.core.model.ReviewLog
import com.ahadporkar.engram.core.srs.CardPhase
import com.ahadporkar.engram.core.srs.FsrsScheduler
import com.ahadporkar.engram.core.srs.Rating
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class StatsCalculatorTest {

    private val clock = StudyDayClock(ZoneOffset.UTC, dayStartHour = 4)
    private val calculator = StatsCalculator(FsrsScheduler(), clock)

    private fun log(at: Instant, rating: Rating, phase: CardPhase = CardPhase.REVIEW) = ReviewLog(
        cardId = 1,
        rating = rating,
        phaseBefore = phase,
        reviewedAt = at,
        elapsedDays = 3,
        scheduledSeconds = 86_400,
        durationMillis = 4_000,
        exercise = ExerciseType.TYPING,
        stabilityAfter = 4.0,
        difficultyAfter = 5.0,
        retrievabilityBefore = 0.9,
    )

    @Test
    fun `streak counts consecutive study days and survives until the day is over`() {
        val today = LocalDate.of(2026, 3, 10)
        val days = setOf(today.minusDays(1), today.minusDays(2), today.minusDays(3), today.minusDays(6))
        assertEquals(3, StatsCalculator.currentStreak(days, today))
        assertEquals(4, StatsCalculator.currentStreak(days + today, today))
        assertEquals(0, StatsCalculator.currentStreak(setOf(today.minusDays(2)), today))
        assertEquals(3, StatsCalculator.longestStreak(days))
    }

    @Test
    fun `true retention only uses review-phase answers`() {
        val logs = listOf(
            log(NOW.minus(Duration.ofDays(1)), Rating.GOOD),
            log(NOW.minus(Duration.ofDays(1)), Rating.GOOD),
            log(NOW.minus(Duration.ofDays(2)), Rating.HARD),
            log(NOW.minus(Duration.ofDays(2)), Rating.AGAIN),
            log(NOW.minus(Duration.ofDays(2)), Rating.AGAIN, CardPhase.LEARNING),
            log(NOW.minus(Duration.ofDays(90)), Rating.AGAIN),
        )
        val stats = calculator.compute(emptyList(), logs, NOW, dailyGoal = 20)
        assertEquals(0.75, stats.trueRetention!!, 1e-9)
        assertEquals(4, stats.retentionSampleSize)
    }

    @Test
    fun `today counts, goal progress and per-day history`() {
        val logs = List(5) { log(NOW.minus(Duration.ofMinutes(it.toLong())), Rating.GOOD) }
        val stats = calculator.compute(emptyList(), logs, NOW, dailyGoal = 10)
        assertEquals(5, stats.reviewsToday)
        assertEquals(20_000L, stats.studyMillisToday)
        assertEquals(0.5f, stats.goalProgress, 1e-6f)
        assertEquals(30, stats.reviewsPerDay.size)
        assertEquals(5, stats.reviewsPerDay.last().count)
        assertNull(calculator.compute(emptyList(), emptyList(), NOW, 10).trueRetention)
    }

    @Test
    fun `forecast puts overdue cards on today and counts maturity`() {
        val overdue = TestData.reviewCard(TestData.note(1), 30.0, NOW.minus(Duration.ofDays(40)), NOW.minus(Duration.ofDays(10))).card
        val inThreeDays = TestData.reviewCard(TestData.note(2), 5.0, NOW.minus(Duration.ofDays(2)), NOW.plus(Duration.ofDays(3))).card
        val newCard = TestData.newCard(TestData.note(3)).card

        val stats = calculator.compute(listOf(overdue, inThreeDays, newCard), emptyList(), NOW, 10)
        assertEquals(1, stats.forecast[0].count)
        assertEquals(1, stats.forecast[3].count)
        assertEquals(1, stats.counts.mature)
        assertEquals(1, stats.counts.young)
        assertEquals(1, stats.counts.new)
        assertEquals(3, stats.counts.total)
        assertTrue(stats.estimatedKnown in 0.5..2.0)
        assertEquals(listOf(overdue.id, inThreeDays.id).toSet(), stats.weakestCardIds.toSet())
    }

    @Test
    fun `study day starts at the configured hour`() {
        val lateNight = Instant.parse("2026-03-11T02:30:00Z")
        assertEquals(LocalDate.of(2026, 3, 10), clock.studyDate(lateNight))
        assertEquals(Instant.parse("2026-03-11T04:00:00Z"), clock.endOfStudyDay(lateNight))
        assertEquals(Instant.parse("2026-03-10T04:00:00Z"), clock.startOfStudyDay(lateNight))
    }
}
