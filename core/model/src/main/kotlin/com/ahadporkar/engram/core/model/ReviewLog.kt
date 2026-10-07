package com.ahadporkar.engram.core.model

import com.ahadporkar.engram.core.srs.CardPhase
import com.ahadporkar.engram.core.srs.Rating
import java.time.Instant

/**
 * Immutable record of one answer. Review logs drive statistics, streaks and — later —
 * personal FSRS parameter optimisation.
 */
data class ReviewLog(
    val id: Long = 0,
    val cardId: Long,
    val rating: Rating,
    val phaseBefore: CardPhase,
    val reviewedAt: Instant,
    val elapsedDays: Long?,
    val scheduledSeconds: Long,
    val durationMillis: Long,
    val exercise: ExerciseType,
    val stabilityAfter: Double,
    val difficultyAfter: Double,
    val retrievabilityBefore: Double?,
)

/** How deep the retrieval is. Deeper retrieval = stronger testing effect, but harder. */
enum class RetrievalLevel {
    /** First exposure; nothing is retrieved yet. */
    EXPOSURE,

    /** Pick the right answer among options. */
    RECOGNITION,

    /** Produce the answer with strong cues (letters, sentence context). */
    CUED_RECALL,

    /** Produce the answer from memory alone. */
    FREE_RECALL,
}

/** Exercise formats, ordered roughly from easiest to hardest. */
enum class ExerciseType(val level: RetrievalLevel) {
    PRESENTATION(RetrievalLevel.EXPOSURE),
    MULTIPLE_CHOICE(RetrievalLevel.RECOGNITION),
    REVERSE_MULTIPLE_CHOICE(RetrievalLevel.RECOGNITION),
    LISTENING(RetrievalLevel.RECOGNITION),
    LETTER_TILES(RetrievalLevel.CUED_RECALL),
    CLOZE(RetrievalLevel.CUED_RECALL),
    TYPING(RetrievalLevel.FREE_RECALL),
    SPEAKING(RetrievalLevel.FREE_RECALL),

    /** Classic self-graded flashcard (Anki style). */
    FLASHCARD(RetrievalLevel.FREE_RECALL),
}
