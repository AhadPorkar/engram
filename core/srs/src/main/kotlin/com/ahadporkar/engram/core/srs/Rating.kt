package com.ahadporkar.engram.core.srs

/**
 * The four answer grades used by FSRS. They map 1:1 to Anki's buttons.
 *
 * [value] is the numeric grade `G` used in the FSRS formulas (1..4).
 */
enum class Rating(val value: Int) {
    AGAIN(1),
    HARD(2),
    GOOD(3),
    EASY(4),
    ;

    /** `true` for every grade except [AGAIN] — the item was recalled. */
    val isSuccess: Boolean get() = this != AGAIN

    companion object {
        fun fromValue(value: Int): Rating =
            entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Unknown rating value: $value")
    }
}

/** Lifecycle phase of a card inside the scheduler. */
enum class CardPhase {
    /** Never reviewed. Treated like learning step 0 on the first answer. */
    NEW,

    /** In short-term learning steps (minutes), not yet graduated. */
    LEARNING,

    /** Graduated; scheduled in days from its memory stability. */
    REVIEW,

    /** Forgotten during review; going through relearning steps. */
    RELEARNING,
}
