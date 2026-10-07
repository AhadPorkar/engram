package com.ahadporkar.engram.core.srs

import kotlin.math.pow

/**
 * The 21 trainable weights of FSRS-6.
 *
 * Defaults are the published FSRS-6 defaults (trained on ~10k Anki collections).
 * Users who optimised their parameters in Anki (Deck options → FSRS → Optimize)
 * can paste them here; 19-value FSRS-5 sets are upgraded automatically.
 */
data class FsrsParameters(val weights: List<Double> = DEFAULT_WEIGHTS) {

    init {
        require(weights.size == SIZE) { "FSRS-6 needs $SIZE weights, got ${weights.size}" }
        require(weights.all { it.isFinite() }) { "Weights must be finite numbers" }
        require(weights.take(4).all { it > 0.0 }) { "Initial stabilities w0..w3 must be positive" }
        require(weights[20] > 0.0) { "Decay w20 must be positive" }
    }

    /** Exponent of the power forgetting curve (negative). */
    val decay: Double get() = -weights[20]

    /** Chosen so that retrievability equals 90 % when elapsed time equals stability. */
    val factor: Double get() = 0.9.pow(1.0 / decay) - 1.0

    operator fun get(index: Int): Double = weights[index]

    fun format(): String = weights.joinToString(", ") { "%.4f".format(java.util.Locale.ROOT, it) }

    companion object {
        const val SIZE = 21
        private const val FSRS5_SIZE = 19

        /** FSRS-5 used a fixed decay of 0.5 and no short-term stability exponent. */
        private const val FSRS5_DECAY = 0.5

        val DEFAULT_WEIGHTS: List<Double> = listOf(
            0.212, 1.2931, 2.3065, 8.2956, 6.4133, 0.8334, 3.0194, 0.001, 1.8722, 0.1666,
            0.796, 1.4835, 0.0614, 0.2629, 1.6483, 0.6014, 1.8729, 0.5425, 0.0912, 0.0658,
            0.1542,
        )

        val DEFAULT = FsrsParameters()

        /**
         * Parses a comma/space separated list as shown in Anki.
         * Accepts 21 (FSRS-6) or 19 (FSRS-5) values. Returns `null` if the text is invalid.
         */
        fun parse(text: String): FsrsParameters? {
            val values = text.split(',', ' ', '\n', '\t', ';')
                .filter { it.isNotBlank() }
                .map { it.trim().toDoubleOrNull() ?: return null }
            val upgraded = when (values.size) {
                SIZE -> values
                FSRS5_SIZE -> values + listOf(0.0, FSRS5_DECAY)
                else -> return null
            }
            return runCatching { FsrsParameters(upgraded) }.getOrNull()
        }
    }
}
