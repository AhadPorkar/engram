package com.ahadporkar.engram.core.srs

import java.time.Instant

/**
 * Everything the scheduler needs to know about one card.
 *
 * The memory state follows the DSR model of FSRS:
 * - [stability]: days until recall probability drops to 90 %.
 * - [difficulty]: inherent difficulty of the item, 1 (easy) .. 10 (hard).
 * Retrievability is not stored — it is derived from stability and elapsed time.
 */
data class SchedulingState(
    val phase: CardPhase = CardPhase.NEW,
    /** Index into the (re)learning steps while in [CardPhase.LEARNING] / [CardPhase.RELEARNING]. */
    val step: Int? = null,
    val stability: Double? = null,
    val difficulty: Double? = null,
    val due: Instant,
    val lastReview: Instant? = null,
    val reps: Int = 0,
    val lapses: Int = 0,
) {
    val hasMemoryState: Boolean get() = stability != null && difficulty != null

    val isInLearningSteps: Boolean
        get() = phase == CardPhase.LEARNING || phase == CardPhase.RELEARNING

    companion object {
        /** A brand-new card that is available immediately. */
        fun newCard(now: Instant): SchedulingState = SchedulingState(due = now)
    }
}
