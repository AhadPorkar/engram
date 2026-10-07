package com.ahadporkar.engram.core.learning.exercise

import com.ahadporkar.engram.core.model.ExerciseType
import com.ahadporkar.engram.core.model.StudyCard

/** A concrete, ready-to-render question built from a [StudyCard]. */
sealed interface Exercise {
    val item: StudyCard
    val type: ExerciseType

    /** First exposure of a new word: term, meaning, example and audio — no test yet. */
    data class Presentation(override val item: StudyCard) : Exercise {
        override val type: ExerciseType get() = ExerciseType.PRESENTATION
    }

    /** Classic flip card graded by the learner. */
    data class Flashcard(override val item: StudyCard) : Exercise {
        override val type: ExerciseType get() = ExerciseType.FLASHCARD
    }

    /** Multiple choice; [audioOnly] hides the prompt text (listening comprehension). */
    data class Choice(
        override val item: StudyCard,
        override val type: ExerciseType,
        val prompt: String,
        val promptLanguage: String,
        val audioOnly: Boolean,
        val options: List<String>,
        val correctIndex: Int,
    ) : Exercise {
        init {
            require(type in CHOICE_TYPES) { "$type is not a choice exercise" }
            require(correctIndex in options.indices) { "correctIndex out of range" }
        }

        val correctOption: String get() = options[correctIndex]
    }

    /** Spell the term by tapping scrambled letters (successor of the old "Jumble" idea). */
    data class LetterTiles(
        override val item: StudyCard,
        val prompt: String,
        val answer: String,
        val tiles: List<String>,
    ) : Exercise {
        override val type: ExerciseType get() = ExerciseType.LETTER_TILES
    }

    /** Free recall: type the term for the given meaning. */
    data class Typing(
        override val item: StudyCard,
        val prompt: String,
        val accepted: List<String>,
        val synonyms: List<String>,
        val hint: String,
    ) : Exercise {
        override val type: ExerciseType get() = ExerciseType.TYPING
    }

    /** Fill the gap in the example sentence — vocabulary in context. */
    data class Cloze(
        override val item: StudyCard,
        val before: String,
        val after: String,
        val accepted: List<String>,
        val translation: String,
        val hint: String,
    ) : Exercise {
        override val type: ExerciseType get() = ExerciseType.CLOZE
    }

    /** Say the term out loud; checked with on-device speech recognition. */
    data class Speaking(
        override val item: StudyCard,
        val prompt: String,
        val accepted: List<String>,
        val language: String,
    ) : Exercise {
        override val type: ExerciseType get() = ExerciseType.SPEAKING
    }

    companion object {
        val CHOICE_TYPES = setOf(
            ExerciseType.MULTIPLE_CHOICE,
            ExerciseType.REVERSE_MULTIPLE_CHOICE,
            ExerciseType.LISTENING,
        )
    }
}

/** Candidate wrong answers for multiple-choice questions. */
data class DistractorPool(
    val fronts: List<String> = emptyList(),
    val backs: List<String> = emptyList(),
)
