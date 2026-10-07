package com.ahadporkar.engram.core.learning.answer

import com.ahadporkar.engram.core.model.TypingTolerance

enum class Verdict { EXACT, CLOSE, WRONG }

/** Why an answer was accepted as "close" rather than exact. */
enum class CloseReason {
    /** Small spelling mistake within the tolerance. */
    TYPO,

    /** Right word, but the gendered article is missing or wrong ("die Hund"). */
    ARTICLE,

    /** A synonym of the target — correct meaning, but not the word being learned. */
    SYNONYM,
}

data class AnswerResult(
    val verdict: Verdict,
    /** The accepted alternative closest to the user's input. */
    val expected: String,
    val closeReason: CloseReason? = null,
    val distance: Int = 0,
) {
    val isAccepted: Boolean get() = verdict != Verdict.WRONG
}

/**
 * Grades free-text answers the way a patient teacher would: forgiving about form,
 * strict about substance.
 */
class AnswerEvaluator(
    private val tolerance: TypingTolerance = TypingTolerance.NORMAL,
    private val ignoreAccents: Boolean = true,
) {

    fun evaluate(input: String, accepted: List<String>, synonyms: List<String> = emptyList()): AnswerResult {
        val fallback = accepted.firstOrNull().orEmpty()
        val user = TextNormalizer.normalize(input, ignoreAccents)
        if (user.isEmpty() || accepted.isEmpty()) return AnswerResult(Verdict.WRONG, fallback)

        var best: AnswerResult? = null
        for (candidate in accepted) {
            val result = compare(user, candidate)
            if (result.verdict == Verdict.EXACT) return result
            best = better(best, result)
        }
        if (best?.verdict != Verdict.CLOSE) {
            for (synonym in synonyms) {
                val result = compare(user, synonym)
                if (result.verdict == Verdict.EXACT) {
                    return AnswerResult(Verdict.CLOSE, fallback, CloseReason.SYNONYM, 0)
                }
            }
        }
        return best ?: AnswerResult(Verdict.WRONG, fallback)
    }

    /** Number of edits still accepted as a typo for an answer of [length] characters. */
    fun allowedTypos(length: Int): Int = when (tolerance) {
        TypingTolerance.STRICT -> 0
        TypingTolerance.NORMAL -> when {
            length <= 3 -> 0
            length <= 7 -> 1
            else -> 2
        }
        TypingTolerance.LENIENT -> when {
            length <= 2 -> 0
            length <= 5 -> 1
            length <= 10 -> 2
            else -> 3
        }
    }

    private fun compare(user: String, rawExpected: String): AnswerResult {
        val target = TextNormalizer.normalize(rawExpected, ignoreAccents)
        if (target.isEmpty()) return AnswerResult(Verdict.WRONG, rawExpected)
        if (user == target || user.withoutSpaces() == target.withoutSpaces()) {
            return AnswerResult(Verdict.EXACT, rawExpected)
        }

        val userSplit = LeadingWords.split(user)
        val targetSplit = LeadingWords.split(target)
        val restsEqual = userSplit.rest == targetSplit.rest ||
            userSplit.rest.withoutSpaces() == targetSplit.rest.withoutSpaces()

        if (restsEqual) {
            val articleMatters = targetSplit.leading in LeadingWords.genderedArticles ||
                userSplit.leading in LeadingWords.genderedArticles
            return if (articleMatters) {
                AnswerResult(Verdict.CLOSE, rawExpected, CloseReason.ARTICLE, 1)
            } else {
                AnswerResult(Verdict.EXACT, rawExpected)
            }
        }

        val allowed = allowedTypos(targetSplit.rest.length)
        val distance = EditDistance.damerauLevenshtein(userSplit.rest, targetSplit.rest, allowed)
        return if (distance <= allowed) {
            AnswerResult(Verdict.CLOSE, rawExpected, CloseReason.TYPO, distance)
        } else {
            AnswerResult(Verdict.WRONG, rawExpected, distance = distance)
        }
    }

    private fun better(current: AnswerResult?, candidate: AnswerResult): AnswerResult {
        if (current == null) return candidate
        if (candidate.verdict.ordinal != current.verdict.ordinal) {
            return if (candidate.verdict.ordinal < current.verdict.ordinal) candidate else current
        }
        return if (candidate.distance < current.distance) candidate else current
    }

    private fun String.withoutSpaces() = replace(" ", "")
}
