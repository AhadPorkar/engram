package com.ahadporkar.engram.core.learning.grading

import com.ahadporkar.engram.core.learning.answer.Verdict
import com.ahadporkar.engram.core.model.ExerciseType
import com.ahadporkar.engram.core.model.RetrievalLevel
import com.ahadporkar.engram.core.srs.Rating

/** What happened in an auto-graded exercise. */
data class ExerciseOutcome(
    val exercise: ExerciseType,
    val verdict: Verdict,
    val responseMillis: Long,
    val answerLength: Int,
    val hintUsed: Boolean = false,
)

/**
 * Turns exercise performance into an FSRS rating, so learners never have to judge themselves.
 *
 * - wrong → AGAIN
 * - typo, wrong article, synonym, hint used or very slow → HARD (retrieval was effortful)
 * - recognition / cued recall answered correctly → GOOD (never EASY: recognising is easier than recalling)
 * - free recall, exact and fluent → EASY
 * - otherwise → GOOD
 */
object RatingPolicy {

    fun rate(outcome: ExerciseOutcome): Rating {
        require(outcome.exercise != ExerciseType.PRESENTATION) { "Presentations are not graded" }
        val level = outcome.exercise.level
        return when {
            outcome.verdict == Verdict.WRONG -> Rating.AGAIN
            outcome.verdict == Verdict.CLOSE || outcome.hintUsed -> Rating.HARD
            outcome.responseMillis > slowThresholdMillis(level, outcome.answerLength) -> Rating.HARD
            level == RetrievalLevel.FREE_RECALL &&
                outcome.responseMillis <= fluentThresholdMillis(outcome.answerLength) -> Rating.EASY
            else -> Rating.GOOD
        }
    }

    /** Answers faster than this count as fluent recall. */
    fun fluentThresholdMillis(answerLength: Int): Long = 2_500L + 250L * answerLength

    /** Answers slower than this suggest a struggle even if correct. */
    fun slowThresholdMillis(level: RetrievalLevel, answerLength: Int): Long = when (level) {
        RetrievalLevel.EXPOSURE -> Long.MAX_VALUE
        RetrievalLevel.RECOGNITION -> 15_000L
        RetrievalLevel.CUED_RECALL -> 20_000L + 600L * answerLength
        RetrievalLevel.FREE_RECALL -> 25_000L + 800L * answerLength
    }
}
