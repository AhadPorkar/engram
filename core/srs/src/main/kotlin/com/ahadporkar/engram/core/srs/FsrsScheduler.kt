package com.ahadporkar.engram.core.srs

import java.time.Duration
import java.time.Instant
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.random.Random

/** Result of grading one card. */
data class ReviewResult(
    val state: SchedulingState,
    /** Time until the card is due again. */
    val interval: Duration,
    /** Whole days since the previous review, `null` for the first review. */
    val elapsedDays: Long?,
    /** Probability of recall at the moment of this review, `null` for the first review. */
    val retrievabilityBefore: Double?,
)

/**
 * FSRS-6 scheduler with Anki-style (re)learning steps and interval fuzz.
 *
 * Semantics match the reference implementation `py-fsrs` 6.x, which is verified by the
 * golden-vector test in `FsrsSchedulerTest`.
 */
class FsrsScheduler(
    val config: SchedulerConfig = SchedulerConfig(),
    private val random: Random = Random.Default,
) {
    val algorithm = FsrsAlgorithm(config.parameters)

    /** Grades [state] with [rating] at [now] and returns the new state. */
    fun review(state: SchedulingState, rating: Rating, now: Instant): ReviewResult =
        review(state, rating, now, fuzz = config.enableFuzz)

    /**
     * What each button would do — used to label the rating buttons ("10m", "3d", ...).
     * Never fuzzed so labels are stable.
     */
    fun preview(state: SchedulingState, now: Instant): Map<Rating, ReviewResult> =
        Rating.entries.associateWith { review(state, it, now, fuzz = false) }

    /**
     * Current probability of recall using fractional days (smooth, good for sorting and display).
     * `null` when the card has no memory state yet.
     */
    fun retrievability(state: SchedulingState, now: Instant): Double? {
        val stability = state.stability ?: return null
        val last = state.lastReview ?: return null
        val elapsed = Duration.between(last, now).toMillis().coerceAtLeast(0) / MILLIS_PER_DAY
        return algorithm.retrievability(elapsed, stability)
    }

    private fun review(state: SchedulingState, rating: Rating, now: Instant, fuzz: Boolean): ReviewResult {
        val elapsedDays = state.lastReview?.let { Duration.between(it, now).toDays().coerceAtLeast(0) }
        val retrievabilityBefore = if (state.stability != null && elapsedDays != null) {
            algorithm.retrievability(elapsedDays.toDouble(), state.stability)
        } else {
            null
        }

        val reviewed = when (state.phase) {
            CardPhase.NEW, CardPhase.LEARNING -> reviewStepped(
                state = if (state.phase == CardPhase.NEW) state.copy(step = 0) else state,
                rating = rating,
                elapsedDays = elapsedDays,
                retrievability = retrievabilityBefore,
                steps = config.learningSteps,
                phaseWhileStepping = CardPhase.LEARNING,
            )
            CardPhase.RELEARNING -> reviewStepped(
                state = state,
                rating = rating,
                elapsedDays = elapsedDays,
                retrievability = retrievabilityBefore,
                steps = config.relearningSteps,
                phaseWhileStepping = CardPhase.RELEARNING,
            )
            CardPhase.REVIEW -> reviewGraduated(state, rating, elapsedDays, retrievabilityBefore)
        }

        val interval = if (fuzz && reviewed.state.phase == CardPhase.REVIEW) {
            fuzzInterval(reviewed.interval)
        } else {
            reviewed.interval
        }

        val lapsed = state.phase == CardPhase.REVIEW && rating == Rating.AGAIN
        val finalState = reviewed.state.copy(
            due = now.plus(interval),
            lastReview = now,
            reps = state.reps + 1,
            lapses = state.lapses + if (lapsed) 1 else 0,
        )
        return ReviewResult(finalState, interval, elapsedDays, retrievabilityBefore)
    }

    private data class Step(val state: SchedulingState, val interval: Duration)

    private fun updatedMemory(
        state: SchedulingState,
        rating: Rating,
        elapsedDays: Long?,
        retrievability: Double?,
        updateDifficultyOnSameDay: Boolean,
    ): Pair<Double, Double> {
        val s = state.stability
        val d = state.difficulty
        if (s == null || d == null) {
            return algorithm.initialStability(rating) to algorithm.initialDifficulty(rating)
        }
        return if (elapsedDays != null && elapsedDays < 1) {
            val nextD = if (updateDifficultyOnSameDay) algorithm.nextDifficulty(d, rating) else d
            algorithm.shortTermStability(s, rating) to nextD
        } else {
            val r = retrievability ?: algorithm.retrievability((elapsedDays ?: 0).toDouble(), s)
            algorithm.nextStability(d, s, r, rating) to algorithm.nextDifficulty(d, rating)
        }
    }

    private fun reviewStepped(
        state: SchedulingState,
        rating: Rating,
        elapsedDays: Long?,
        retrievability: Double?,
        steps: List<Duration>,
        phaseWhileStepping: CardPhase,
    ): Step {
        val (stability, difficulty) = updatedMemory(state, rating, elapsedDays, retrievability, true)
        val step = state.step ?: 0
        val withMemory = state.copy(stability = stability, difficulty = difficulty)

        fun graduate() = Step(
            withMemory.copy(phase = CardPhase.REVIEW, step = null),
            Duration.ofDays(nextIntervalDays(stability)),
        )

        if (steps.isEmpty() || (step >= steps.size && rating != Rating.AGAIN)) return graduate()

        return when (rating) {
            Rating.AGAIN -> Step(withMemory.copy(phase = phaseWhileStepping, step = 0), steps[0])
            Rating.HARD -> {
                val interval = when {
                    step == 0 && steps.size == 1 -> steps[0].multipliedBy(3).dividedBy(2)
                    step == 0 -> steps[0].plus(steps[1]).dividedBy(2)
                    else -> steps[step]
                }
                Step(withMemory.copy(phase = phaseWhileStepping, step = step), interval)
            }
            Rating.GOOD ->
                if (step + 1 >= steps.size) {
                    graduate()
                } else {
                    Step(withMemory.copy(phase = phaseWhileStepping, step = step + 1), steps[step + 1])
                }
            Rating.EASY -> graduate()
        }
    }

    private fun reviewGraduated(
        state: SchedulingState,
        rating: Rating,
        elapsedDays: Long?,
        retrievability: Double?,
    ): Step {
        val (stability, difficulty) = updatedMemory(state, rating, elapsedDays, retrievability, true)
        val withMemory = state.copy(stability = stability, difficulty = difficulty)
        return if (rating == Rating.AGAIN && config.relearningSteps.isNotEmpty()) {
            Step(withMemory.copy(phase = CardPhase.RELEARNING, step = 0), config.relearningSteps[0])
        } else {
            Step(withMemory.copy(phase = CardPhase.REVIEW, step = null), Duration.ofDays(nextIntervalDays(stability)))
        }
    }

    /** Rounded interval in days for the configured desired retention, clamped to 1..maximum. */
    fun nextIntervalDays(stability: Double): Long {
        val raw = algorithm.interval(stability, config.desiredRetention)
        return round(raw).toLong().coerceIn(1L, config.maximumIntervalDays.toLong())
    }

    /**
     * Spreads reviews so cards learned together do not stay clustered forever.
     * Same fuzz ranges as Anki / py-fsrs: ±15 % for 2.5–7 d, ±10 % for 7–20 d, ±5 % beyond.
     */
    internal fun fuzzInterval(interval: Duration): Duration {
        val days = interval.toDays()
        if (days < 2.5) return interval
        val (minIvl, maxIvl) = fuzzRange(days.toDouble())
        // Uniform integer in [minIvl, maxIvl] (floor avoids the off-by-one of rounding).
        val fuzzed = floor(random.nextDouble() * (maxIvl - minIvl + 1) + minIvl).toLong()
        return Duration.ofDays(min(fuzzed, config.maximumIntervalDays.toLong()))
    }

    internal fun fuzzRange(intervalDays: Double): Pair<Long, Long> {
        var delta = 1.0
        for ((start, end, factor) in FUZZ_RANGES) {
            delta += factor * max(min(intervalDays, end) - start, 0.0)
        }
        var minIvl = round(intervalDays - delta).toLong()
        var maxIvl = round(intervalDays + delta).toLong()
        minIvl = max(2, minIvl)
        maxIvl = min(maxIvl, config.maximumIntervalDays.toLong())
        minIvl = min(minIvl, maxIvl)
        return minIvl to maxIvl
    }

    private companion object {
        const val MILLIS_PER_DAY = 86_400_000.0

        val FUZZ_RANGES = listOf(
            Triple(2.5, 7.0, 0.15),
            Triple(7.0, 20.0, 0.10),
            Triple(20.0, Double.POSITIVE_INFINITY, 0.05),
        )
    }
}
