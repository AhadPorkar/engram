package com.ahadporkar.engram.core.learning.answer

import com.ahadporkar.engram.core.model.TypingTolerance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerEvaluatorTest {

    private val evaluator = AnswerEvaluator(TypingTolerance.NORMAL, ignoreAccents = true)

    @Test
    fun `case punctuation and spacing do not matter`() {
        val result = evaluator.evaluate("  Der   HUND! ", listOf("der Hund"))
        assertEquals(Verdict.EXACT, result.verdict)
    }

    @Test
    fun `accents and sharp s are forgiven when enabled`() {
        assertEquals(Verdict.EXACT, evaluator.evaluate("strasse", listOf("Straße")).verdict)
        assertEquals(Verdict.EXACT, evaluator.evaluate("cafe", listOf("café")).verdict)
        assertEquals(Verdict.EXACT, evaluator.evaluate("schon", listOf("schön")).verdict)
    }

    @Test
    fun `accents count when the option is off`() {
        val strict = AnswerEvaluator(TypingTolerance.STRICT, ignoreAccents = false)
        assertEquals(Verdict.WRONG, strict.evaluate("schon", listOf("schön")).verdict)
    }

    @Test
    fun `a missing or wrong gendered article is close, not exact`() {
        val missing = evaluator.evaluate("Hund", listOf("der Hund"))
        assertEquals(Verdict.CLOSE, missing.verdict)
        assertEquals(CloseReason.ARTICLE, missing.closeReason)

        val wrong = evaluator.evaluate("die Hund", listOf("der Hund"))
        assertEquals(CloseReason.ARTICLE, wrong.closeReason)
    }

    @Test
    fun `english fillers like to and the are optional`() {
        assertEquals(Verdict.EXACT, evaluator.evaluate("go", listOf("to go")).verdict)
        assertEquals(Verdict.EXACT, evaluator.evaluate("to go", listOf("go")).verdict)
        assertEquals(Verdict.EXACT, evaluator.evaluate("house", listOf("the house")).verdict)
    }

    @Test
    fun `small typos are close within the tolerance`() {
        val typo = evaluator.evaluate("Schmetterlnig", listOf("Schmetterling"))
        assertEquals(Verdict.CLOSE, typo.verdict)
        assertEquals(CloseReason.TYPO, typo.closeReason)
        assertEquals(1, typo.distance)

        assertEquals(Verdict.WRONG, evaluator.evaluate("Katze", listOf("Hund")).verdict)
    }

    @Test
    fun `short words get no typo allowance in normal mode`() {
        assertEquals(Verdict.WRONG, evaluator.evaluate("ja", listOf("da")).verdict)
    }

    @Test
    fun `tolerance levels grow from strict to lenient`() {
        val strict = AnswerEvaluator(TypingTolerance.STRICT)
        val lenient = AnswerEvaluator(TypingTolerance.LENIENT)
        assertEquals(0, strict.allowedTypos(12))
        assertEquals(2, evaluator.allowedTypos(12))
        assertEquals(3, lenient.allowedTypos(12))
        assertEquals(Verdict.WRONG, strict.evaluate("Hause", listOf("Haus")).verdict)
        assertEquals(Verdict.CLOSE, lenient.evaluate("Hause", listOf("Haus")).verdict)
    }

    @Test
    fun `any listed alternative is accepted`() {
        val accepted = AnswerAlternatives.split("to go; to walk / to travel")
        assertEquals(listOf("to go", "to walk", "to travel"), accepted)
        assertEquals(Verdict.EXACT, evaluator.evaluate("walk", accepted).verdict)
        assertEquals("to walk", evaluator.evaluate("walk", accepted).expected)
    }

    @Test
    fun `synonyms are accepted as close`() {
        val result = evaluator.evaluate("large", listOf("big"), synonyms = listOf("large", "huge"))
        assertEquals(Verdict.CLOSE, result.verdict)
        assertEquals(CloseReason.SYNONYM, result.closeReason)
        assertEquals("big", result.expected)
    }

    @Test
    fun `bracketed notes are ignored`() {
        assertEquals(Verdict.EXACT, evaluator.evaluate("Haus", listOf("Haus (n.)")).verdict)
    }

    @Test
    fun `empty answers are wrong`() {
        val result = evaluator.evaluate("   ", listOf("Haus"))
        assertFalse(result.isAccepted)
    }

    @Test
    fun `persian text keeps its letters`() {
        assertEquals("سلام دنیا", TextNormalizer.normalize("سلام،  دنیا!"))
        assertTrue(evaluator.evaluate("سلام", listOf("سلام")).isAccepted)
    }

    @Test
    fun `transpositions count as a single edit`() {
        assertEquals(1, EditDistance.damerauLevenshtein("teh", "the"))
        assertEquals(3, EditDistance.damerauLevenshtein("kitten", "sitting"))
        assertEquals(2, EditDistance.damerauLevenshtein("abcdef", "uvwxyz", limit = 1))
        assertEquals(0, EditDistance.damerauLevenshtein("same", "same"))
    }

    @Test
    fun `leading words split only before real content`() {
        assertEquals(LeadingWords.Split("der", "hund"), LeadingWords.split("der hund"))
        assertEquals(LeadingWords.Split(null, "die"), LeadingWords.split("die"))
        assertEquals("Hund", LeadingWords.stripRaw("der Hund"))
        assertEquals("eau", LeadingWords.stripRaw("l' eau"))
    }
}
