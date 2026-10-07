package com.ahadporkar.engram.core.srs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import kotlin.random.Random

class FsrsSchedulerTest {

    private val start: Instant = Instant.parse("2022-11-29T12:30:00Z")
    private val scheduler = FsrsScheduler(SchedulerConfig(enableFuzz = false))

    /**
     * Golden vector from the official py-fsrs 6 test-suite (`test_review_card`):
     * learning steps 1m/10m, relearning 10m, retention 0.9, default FSRS-6 weights, no fuzz.
     */
    @Test
    fun `reproduces the py-fsrs reference interval history`() {
        val ratings = listOf(
            Rating.GOOD, Rating.GOOD, Rating.GOOD, Rating.GOOD, Rating.GOOD, Rating.GOOD,
            Rating.AGAIN, Rating.AGAIN,
            Rating.GOOD, Rating.GOOD, Rating.GOOD, Rating.GOOD, Rating.GOOD,
        )
        var state = SchedulingState.newCard(start)
        var now = start
        val intervals = ratings.map { rating ->
            val result = scheduler.review(state, rating, now)
            state = result.state
            now = state.due
            Duration.between(state.lastReview, state.due).toDays()
        }
        assertEquals(listOf(0L, 2, 11, 46, 163, 498, 0, 0, 2, 4, 7, 12, 21), intervals)
    }

    @Test
    fun `memory state matches independent reference after the golden sequence`() {
        var state = SchedulingState.newCard(start)
        var now = start
        repeat(3) {
            state = scheduler.review(state, Rating.GOOD, now).state
            now = state.due
        }
        assertEquals(10.971048, state.stability!!, 1e-5)
        assertEquals(2.104331, state.difficulty!!, 1e-5)
    }

    @Test
    fun `new card walks through learning steps`() {
        val new = SchedulingState.newCard(start)

        val again = scheduler.review(new, Rating.AGAIN, start)
        assertEquals(CardPhase.LEARNING, again.state.phase)
        assertEquals(Duration.ofMinutes(1), again.interval)

        val hard = scheduler.review(new, Rating.HARD, start)
        assertEquals(Duration.ofSeconds(330), hard.interval) // (1m + 10m) / 2

        val good = scheduler.review(new, Rating.GOOD, start)
        assertEquals(CardPhase.LEARNING, good.state.phase)
        assertEquals(1, good.state.step)
        assertEquals(Duration.ofMinutes(10), good.interval)

        val easy = scheduler.review(new, Rating.EASY, start)
        assertEquals(CardPhase.REVIEW, easy.state.phase)
        assertNull(easy.state.step)
        assertTrue(easy.interval.toDays() >= 8)
    }

    @Test
    fun `a lapse moves the card to relearning and counts the lapse`() {
        var state = SchedulingState.newCard(start)
        state = scheduler.review(state, Rating.EASY, start).state
        val due = state.due
        val lapse = scheduler.review(state, Rating.AGAIN, due)

        assertEquals(CardPhase.RELEARNING, lapse.state.phase)
        assertEquals(1, lapse.state.lapses)
        assertEquals(Duration.ofMinutes(10), lapse.interval)
        assertTrue(lapse.state.stability!! < state.stability!!)
        assertTrue(lapse.state.difficulty!! > state.difficulty!!)
        assertEquals(0.9, lapse.retrievabilityBefore!!, 0.02)
    }

    @Test
    fun `preview orders intervals Again lt Hard lt Good lt Easy for review cards`() {
        var state = SchedulingState.newCard(start)
        state = scheduler.review(state, Rating.GOOD, start).state
        state = scheduler.review(state, Rating.GOOD, state.due).state
        val preview = scheduler.preview(state, state.due)
        val days = Rating.entries.map { preview.getValue(it).interval.seconds }
        assertEquals(days.sorted(), days)
        assertTrue(days.toSet().size == 4)
    }

    @Test
    fun `reps increase on every review`() {
        var state = SchedulingState.newCard(start)
        var now = start
        repeat(5) {
            state = scheduler.review(state, Rating.GOOD, now).state
            now = state.due
        }
        assertEquals(5, state.reps)
        assertEquals(0, state.lapses)
    }

    @Test
    fun `intervals respect the maximum interval`() {
        val capped = FsrsScheduler(SchedulerConfig(enableFuzz = false, maximumIntervalDays = 30))
        var state = SchedulingState.newCard(start)
        var now = start
        repeat(8) {
            state = capped.review(state, Rating.EASY, now).state
            now = state.due
        }
        assertTrue(Duration.between(state.lastReview, state.due).toDays() <= 30)
    }

    @Test
    fun `higher desired retention gives shorter intervals`() {
        val relaxed = FsrsScheduler(SchedulerConfig(desiredRetention = 0.8, enableFuzz = false))
        val strict = FsrsScheduler(SchedulerConfig(desiredRetention = 0.95, enableFuzz = false))
        assertTrue(relaxed.nextIntervalDays(20.0) > strict.nextIntervalDays(20.0))
        assertEquals(20L, scheduler.nextIntervalDays(20.0))
    }

    @Test
    fun `fuzz stays inside the documented range and is reproducible`() {
        val a = FsrsScheduler(SchedulerConfig(), Random(42))
        val b = FsrsScheduler(SchedulerConfig(), Random(42))
        val (min, max) = a.fuzzRange(30.0)
        assertEquals(27L, min) // delta = 1 + 0.15*4.5 + 0.1*13 + 0.05*10 = 3.475
        assertEquals(33L, max)
        repeat(200) {
            val fuzzed = a.fuzzInterval(Duration.ofDays(30)).toDays()
            assertTrue(fuzzed in min..max)
        }
        assertEquals(
            b.fuzzInterval(Duration.ofDays(100)),
            FsrsScheduler(SchedulerConfig(), Random(42)).fuzzInterval(Duration.ofDays(100)),
        )
    }

    @Test
    fun `short intervals are never fuzzed`() {
        val fuzzy = FsrsScheduler(SchedulerConfig(), Random(1))
        assertEquals(Duration.ofDays(2), fuzzy.fuzzInterval(Duration.ofDays(2)))
    }

    @Test
    fun `retrievability decays over time`() {
        var state = SchedulingState.newCard(start)
        state = scheduler.review(state, Rating.EASY, start).state
        val r0 = scheduler.retrievability(state, start)!!
        val r1 = scheduler.retrievability(state, start.plus(Duration.ofDays(5)))!!
        val r2 = scheduler.retrievability(state, start.plus(Duration.ofDays(50)))!!
        assertEquals(1.0, r0, 1e-9)
        assertTrue(r1 > r2)
        assertNull(scheduler.retrievability(SchedulingState.newCard(start), start))
    }

    @Test
    fun `scheduler works without learning steps`() {
        val noSteps = FsrsScheduler(
            SchedulerConfig(learningSteps = emptyList(), relearningSteps = emptyList(), enableFuzz = false),
        )
        val first = noSteps.review(SchedulingState.newCard(start), Rating.GOOD, start)
        assertEquals(CardPhase.REVIEW, first.state.phase)
        val lapse = noSteps.review(first.state, Rating.AGAIN, first.state.due)
        assertEquals(CardPhase.REVIEW, lapse.state.phase)
        assertTrue(lapse.interval.toDays() >= 1)
    }
}
