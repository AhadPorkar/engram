package com.ahadporkar.engram.core.srs

import java.time.Duration

/**
 * User-tunable scheduler settings.
 *
 * [desiredRetention] is the probability of recall the scheduler aims for at the moment a card
 * becomes due. Higher retention → shorter intervals → more reviews per day.
 */
data class SchedulerConfig(
    val desiredRetention: Double = DEFAULT_RETENTION,
    val learningSteps: List<Duration> = listOf(Duration.ofMinutes(1), Duration.ofMinutes(10)),
    val relearningSteps: List<Duration> = listOf(Duration.ofMinutes(10)),
    val maximumIntervalDays: Int = 36_500,
    val enableFuzz: Boolean = true,
    val parameters: FsrsParameters = FsrsParameters.DEFAULT,
) {
    init {
        require(desiredRetention in MIN_RETENTION..MAX_RETENTION) {
            "desiredRetention must be in $MIN_RETENTION..$MAX_RETENTION"
        }
        require(maximumIntervalDays >= 1) { "maximumIntervalDays must be >= 1" }
        require(learningSteps.none { it.isNegative || it.isZero }) { "learning steps must be positive" }
        require(relearningSteps.none { it.isNegative || it.isZero }) { "relearning steps must be positive" }
    }

    companion object {
        const val DEFAULT_RETENTION = 0.9
        const val MIN_RETENTION = 0.7
        const val MAX_RETENTION = 0.99
    }
}
