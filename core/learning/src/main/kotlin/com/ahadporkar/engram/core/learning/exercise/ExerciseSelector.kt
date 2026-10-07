package com.ahadporkar.engram.core.learning.exercise

import com.ahadporkar.engram.core.model.CardDirection
import com.ahadporkar.engram.core.model.ExerciseType
import com.ahadporkar.engram.core.model.StudyCard
import com.ahadporkar.engram.core.model.StudyMode
import kotlin.random.Random

data class SelectionContext(
    val mode: StudyMode,
    /** The new card was already presented earlier in this session. */
    val presentedThisSession: Boolean,
    val audioAvailable: Boolean,
    val speakingEnabled: Boolean,
    val pool: DistractorPool,
)

/**
 * Chooses the exercise format for a card — a "difficulty ladder".
 *
 * Weak memories get recognition tasks (multiple choice), stronger memories get cued recall
 * (letter tiles, cloze) and stable memories get free recall (typing, speaking). This keeps
 * retrieval *effortful but successful* — Bjork's "desirable difficulties" — instead of either
 * frustrating or trivial.
 *
 * Recognition cards (term → meaning) build receptive knowledge; production cards
 * (meaning → term) build productive knowledge and climb further up the ladder.
 */
class ExerciseSelector(private val random: Random = Random.Default) {

    fun select(item: StudyCard, context: SelectionContext): ExerciseType {
        if (context.mode == StudyMode.FLASHCARDS) return ExerciseType.FLASHCARD

        val card = item.card
        if (card.isNew && context.mode == StudyMode.SMART && !context.presentedThisSession) {
            return ExerciseType.PRESENTATION
        }

        val stability = card.scheduling.stability ?: 0.0
        val ladder = when (card.direction) {
            CardDirection.RECOGNITION -> when {
                stability < RECOGNITION_LISTENING_FROM -> listOf(ExerciseType.MULTIPLE_CHOICE)
                stability < RECOGNITION_RECALL_FROM -> listOf(ExerciseType.MULTIPLE_CHOICE, ExerciseType.LISTENING)
                else -> listOf(ExerciseType.LISTENING, ExerciseType.FLASHCARD)
            }
            CardDirection.PRODUCTION -> when {
                stability < PRODUCTION_CUED_FROM -> listOf(ExerciseType.REVERSE_MULTIPLE_CHOICE)
                stability < PRODUCTION_CONTEXT_FROM -> listOf(ExerciseType.LETTER_TILES, ExerciseType.REVERSE_MULTIPLE_CHOICE)
                stability < PRODUCTION_FREE_FROM -> listOf(ExerciseType.LETTER_TILES, ExerciseType.CLOZE, ExerciseType.TYPING)
                else -> listOf(ExerciseType.TYPING, ExerciseType.CLOZE, ExerciseType.SPEAKING)
            }
        }

        val feasible = ladder.filter { isFeasible(it, item, context) }
        return feasible.randomOrNull(random) ?: fallback(item)
    }

    fun isFeasible(type: ExerciseType, item: StudyCard, context: SelectionContext): Boolean = when (type) {
        ExerciseType.MULTIPLE_CHOICE ->
            DistractorPicker.pick(item.note.back, context.pool.backs, MIN_DISTRACTORS, random).size >= MIN_DISTRACTORS
        ExerciseType.REVERSE_MULTIPLE_CHOICE ->
            DistractorPicker.pick(item.note.front, context.pool.fronts, MIN_DISTRACTORS, random).size >= MIN_DISTRACTORS
        ExerciseType.LISTENING -> context.audioAvailable &&
            DistractorPicker.pick(item.note.back, context.pool.backs, MIN_DISTRACTORS, random).size >= MIN_DISTRACTORS
        ExerciseType.LETTER_TILES -> LetterTilesBuilder.canBuild(item.note.front)
        ExerciseType.CLOZE -> ClozeBuilder.find(item.note) != null
        ExerciseType.SPEAKING -> context.speakingEnabled
        ExerciseType.TYPING, ExerciseType.FLASHCARD, ExerciseType.PRESENTATION -> true
    }

    private fun fallback(item: StudyCard): ExerciseType =
        if (item.card.direction == CardDirection.PRODUCTION) ExerciseType.TYPING else ExerciseType.FLASHCARD

    companion object {
        const val MIN_DISTRACTORS = 2

        // Stability thresholds (days) for climbing the ladder.
        const val RECOGNITION_LISTENING_FROM = 4.0
        const val RECOGNITION_RECALL_FROM = 14.0
        const val PRODUCTION_CUED_FROM = 1.0
        const val PRODUCTION_CONTEXT_FROM = 4.0
        const val PRODUCTION_FREE_FROM = 14.0
    }
}
