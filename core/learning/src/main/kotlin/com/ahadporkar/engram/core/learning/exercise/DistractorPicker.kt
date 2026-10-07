package com.ahadporkar.engram.core.learning.exercise

import com.ahadporkar.engram.core.learning.answer.TextNormalizer
import kotlin.math.abs
import kotlin.random.Random

/**
 * Picks plausible wrong answers. Distractors that look like the right answer (similar length,
 * same first letter) force real retrieval instead of elimination by appearance.
 */
object DistractorPicker {

    fun pick(correct: String, pool: List<String>, count: Int, random: Random): List<String> {
        if (count <= 0) return emptyList()
        val target = TextNormalizer.normalize(correct)
        val unique = pool.asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { TextNormalizer.normalize(it) }
            .filter { TextNormalizer.normalize(it) != target }
            .toList()
        if (unique.size <= count) return unique.shuffled(random)

        val scored = unique.map { it to similarityPenalty(it, correct) + random.nextDouble() * JITTER }
        return scored.sortedBy { it.second }
            .take(count * 2)
            .map { it.first }
            .shuffled(random)
            .take(count)
    }

    private fun similarityPenalty(candidate: String, correct: String): Double {
        val lengthGap = abs(candidate.length - correct.length).toDouble()
        val firstLetterBonus = if (candidate.firstOrNull()?.lowercaseChar() == correct.firstOrNull()?.lowercaseChar()) -1.5 else 0.0
        return lengthGap + firstLetterBonus
    }

    private const val JITTER = 3.0
}
