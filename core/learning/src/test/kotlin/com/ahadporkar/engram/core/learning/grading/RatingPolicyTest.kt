package com.ahadporkar.engram.core.learning.grading

import com.ahadporkar.engram.core.learning.answer.Verdict
import com.ahadporkar.engram.core.model.ExerciseType
import com.ahadporkar.engram.core.srs.Rating
import org.junit.Assert.assertEquals
import org.junit.Test

class RatingPolicyTest {

    private fun rate(type: ExerciseType, verdict: Verdict, millis: Long, hint: Boolean = false) =
        RatingPolicy.rate(ExerciseOutcome(type, verdict, millis, answerLength = 4, hintUsed = hint))

    @Test
    fun `wrong is again`() {
        assertEquals(Rating.AGAIN, rate(ExerciseType.TYPING, Verdict.WRONG, 1_000))
        assertEquals(Rating.AGAIN, rate(ExerciseType.MULTIPLE_CHOICE, Verdict.WRONG, 1_000))
    }

    @Test
    fun `close or hinted answers are hard`() {
        assertEquals(Rating.HARD, rate(ExerciseType.TYPING, Verdict.CLOSE, 1_000))
        assertEquals(Rating.HARD, rate(ExerciseType.TYPING, Verdict.EXACT, 1_000, hint = true))
    }

    @Test
    fun `recognition never earns easy`() {
        assertEquals(Rating.GOOD, rate(ExerciseType.MULTIPLE_CHOICE, Verdict.EXACT, 500))
        assertEquals(Rating.GOOD, rate(ExerciseType.LETTER_TILES, Verdict.EXACT, 500))
    }

    @Test
    fun `fluent free recall is easy, slow recall is good or hard`() {
        assertEquals(Rating.EASY, rate(ExerciseType.TYPING, Verdict.EXACT, 2_000))
        assertEquals(Rating.GOOD, rate(ExerciseType.TYPING, Verdict.EXACT, 10_000))
        assertEquals(Rating.HARD, rate(ExerciseType.TYPING, Verdict.EXACT, 60_000))
        assertEquals(Rating.HARD, rate(ExerciseType.MULTIPLE_CHOICE, Verdict.EXACT, 20_000))
    }
}
