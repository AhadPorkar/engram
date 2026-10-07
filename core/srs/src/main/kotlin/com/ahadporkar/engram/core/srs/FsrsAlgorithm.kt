package com.ahadporkar.engram.core.srs

import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Pure FSRS-6 formulas (Free Spaced Repetition Scheduler, open-spaced-repetition project).
 *
 * The model tracks three variables per card:
 * - **R** retrievability: probability of recalling the card right now,
 * - **S** stability: interval (days) at which R falls to 90 %,
 * - **D** difficulty: 1..10.
 *
 * All functions are side-effect free; [FsrsScheduler] combines them with learning steps.
 */
class FsrsAlgorithm(val parameters: FsrsParameters = FsrsParameters.DEFAULT) {

    private val w = parameters.weights
    private val decay = parameters.decay
    private val factor = parameters.factor

    /** Power forgetting curve: R(t, S) = (1 + FACTOR · t / S)^DECAY. */
    fun retrievability(elapsedDays: Double, stability: Double): Double {
        require(stability > 0.0) { "stability must be positive" }
        return (1.0 + factor * max(elapsedDays, 0.0) / stability).pow(decay)
    }

    /** Interval (days, unrounded) after which R drops to [desiredRetention]. Inverse of [retrievability]. */
    fun interval(stability: Double, desiredRetention: Double): Double =
        stability / factor * (desiredRetention.pow(1.0 / decay) - 1.0)

    /** S₀(G) = w[G-1]. */
    fun initialStability(rating: Rating): Double = clampStability(w[rating.value - 1])

    /** D₀(G) = w4 − e^(w5·(G−1)) + 1. */
    fun initialDifficulty(rating: Rating, clamp: Boolean = true): Double {
        val d = w[4] - exp(w[5] * (rating.value - 1)) + 1.0
        return if (clamp) clampDifficulty(d) else d
    }

    /**
     * D' = w7·D₀(Easy) + (1 − w7)·(D + ΔD·(10 − D)/9) with ΔD = −w6·(G − 3).
     * Linear damping makes difficulty converge slowly near 10; mean reversion pulls towards D₀(Easy).
     */
    fun nextDifficulty(difficulty: Double, rating: Rating): Double {
        val delta = -w[6] * (rating.value - 3)
        val damped = difficulty + (10.0 - difficulty) * delta / 9.0
        val reverted = w[7] * initialDifficulty(Rating.EASY, clamp = false) + (1.0 - w[7]) * damped
        return clampDifficulty(reverted)
    }

    /** Stability update for a second review on the same day (FSRS-6 short-term model). */
    fun shortTermStability(stability: Double, rating: Rating): Double {
        var increase = exp(w[17] * (rating.value - 3 + w[18])) * stability.pow(-w[19])
        if (rating == Rating.GOOD || rating == Rating.EASY) increase = max(increase, 1.0)
        return clampStability(stability * increase)
    }

    fun nextStability(difficulty: Double, stability: Double, retrievability: Double, rating: Rating): Double =
        clampStability(
            if (rating == Rating.AGAIN) {
                nextForgetStability(difficulty, stability, retrievability)
            } else {
                nextRecallStability(difficulty, stability, retrievability, rating)
            },
        )

    /**
     * Successful recall: S' = S·(1 + e^w8·(11 − D)·S^−w9·(e^(w10·(1 − R)) − 1)·hardPenalty·easyBonus).
     * The lower R was at review time, the bigger the gain — the "desirable difficulty" effect.
     */
    fun nextRecallStability(difficulty: Double, stability: Double, retrievability: Double, rating: Rating): Double {
        val hardPenalty = if (rating == Rating.HARD) w[15] else 1.0
        val easyBonus = if (rating == Rating.EASY) w[16] else 1.0
        return stability * (
            1.0 + exp(w[8]) *
                (11.0 - difficulty) *
                stability.pow(-w[9]) *
                (exp((1.0 - retrievability) * w[10]) - 1.0) *
                hardPenalty *
                easyBonus
            )
    }

    /** Lapse: post-lapse stability, never higher than the short-term bound S / e^(w17·w18). */
    fun nextForgetStability(difficulty: Double, stability: Double, retrievability: Double): Double {
        val longTerm = w[11] *
            difficulty.pow(-w[12]) *
            ((stability + 1.0).pow(w[13]) - 1.0) *
            exp((1.0 - retrievability) * w[14])
        val shortTerm = stability / exp(w[17] * w[18])
        return min(longTerm, shortTerm)
    }

    companion object {
        const val MIN_STABILITY = 0.001
        const val MIN_DIFFICULTY = 1.0
        const val MAX_DIFFICULTY = 10.0

        fun clampStability(value: Double): Double = max(value, MIN_STABILITY)
        fun clampDifficulty(value: Double): Double = value.coerceIn(MIN_DIFFICULTY, MAX_DIFFICULTY)
    }
}
