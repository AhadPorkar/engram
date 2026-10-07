package com.ahadporkar.engram.core.learning.exercise

import com.ahadporkar.engram.core.learning.answer.AnswerAlternatives
import com.ahadporkar.engram.core.learning.answer.LeadingWords
import com.ahadporkar.engram.core.model.ExerciseType
import com.ahadporkar.engram.core.model.Note
import com.ahadporkar.engram.core.model.StudyCard
import kotlin.random.Random

/** Builds concrete [Exercise]s. Falls back to typing when a format is impossible for a note. */
class ExerciseFactory(private val random: Random = Random.Default) {

    fun create(type: ExerciseType, item: StudyCard, pool: DistractorPool): Exercise = when (type) {
        ExerciseType.PRESENTATION -> Exercise.Presentation(item)
        ExerciseType.FLASHCARD -> Exercise.Flashcard(item)
        ExerciseType.MULTIPLE_CHOICE, ExerciseType.LISTENING -> choice(
            item = item,
            type = type,
            prompt = item.note.front,
            promptLanguage = item.frontLanguage,
            correct = item.note.back,
            pool = pool.backs,
        )
        ExerciseType.REVERSE_MULTIPLE_CHOICE -> choice(
            item = item,
            type = type,
            prompt = item.note.back,
            promptLanguage = item.backLanguage,
            correct = item.note.front,
            pool = pool.fronts,
        )
        ExerciseType.LETTER_TILES -> LetterTilesBuilder.build(item.note.front, random)
            ?.let { (answer, tiles) -> Exercise.LetterTiles(item, item.note.back, answer, tiles) }
            ?: typing(item)
        ExerciseType.CLOZE -> cloze(item) ?: typing(item)
        ExerciseType.TYPING -> typing(item)
        ExerciseType.SPEAKING -> Exercise.Speaking(
            item = item,
            prompt = item.note.back,
            accepted = AnswerAlternatives.split(item.note.front),
            language = item.frontLanguage,
        )
    }

    private fun choice(
        item: StudyCard,
        type: ExerciseType,
        prompt: String,
        promptLanguage: String,
        correct: String,
        pool: List<String>,
    ): Exercise {
        val distractors = DistractorPicker.pick(correct, pool, OPTION_COUNT - 1, random)
        if (distractors.size < ExerciseSelector.MIN_DISTRACTORS) return typing(item)
        val options = (distractors + correct).shuffled(random)
        return Exercise.Choice(
            item = item,
            type = type,
            prompt = prompt,
            promptLanguage = promptLanguage,
            audioOnly = type == ExerciseType.LISTENING,
            options = options,
            correctIndex = options.indexOf(correct),
        )
    }

    private fun typing(item: StudyCard): Exercise.Typing {
        val accepted = AnswerAlternatives.split(item.note.front)
        return Exercise.Typing(
            item = item,
            prompt = item.note.back,
            accepted = accepted,
            synonyms = AnswerAlternatives.split(item.note.synonyms),
            hint = hintFor(accepted.firstOrNull().orEmpty()),
        )
    }

    private fun cloze(item: StudyCard): Exercise.Cloze? {
        val gap = ClozeBuilder.find(item.note) ?: return null
        val accepted = (listOf(gap.surface) + AnswerAlternatives.split(item.note.front).map(LeadingWords::stripRaw))
            .distinct()
        return Exercise.Cloze(
            item = item,
            before = gap.before,
            after = gap.after,
            accepted = accepted,
            translation = item.note.back,
            hint = hintFor(gap.surface),
        )
    }

    companion object {
        const val OPTION_COUNT = 4

        /** "der Hund" → "d__ H___": first letter of each word, underscores for the rest. */
        fun hintFor(answer: String): String = buildString {
            var startOfWord = true
            for (char in answer) {
                when {
                    char.isWhitespace() -> {
                        append(' ')
                        startOfWord = true
                    }
                    startOfWord -> {
                        append(char)
                        startOfWord = false
                    }
                    char.isLetterOrDigit() -> append('_')
                    else -> {
                        append(char)
                        startOfWord = true
                    }
                }
            }
        }
    }
}

/** Finds the term inside the example sentence and cuts a gap around it. */
object ClozeBuilder {
    data class Gap(val before: String, val surface: String, val after: String)

    fun find(note: Note): Gap? {
        val sentence = note.example.trim()
        if (sentence.isEmpty()) return null
        val candidates = AnswerAlternatives.split(note.front)
            .flatMap { listOf(it, LeadingWords.stripRaw(it)) }
            .filter { it.length >= 2 }
            .distinct()
            .sortedByDescending { it.length }
        for (candidate in candidates) {
            // Match at a word start, so "go" matches "going" but not "ago".
            val match = Regex("""(?<![\p{L}\p{N}])""" + Regex.escape(candidate), RegexOption.IGNORE_CASE)
                .find(sentence) ?: continue
            return Gap(
                before = sentence.substring(0, match.range.first),
                surface = match.value,
                after = sentence.substring(match.range.last + 1),
            )
        }
        return null
    }
}

/** Scrambled letters for spelling practice. */
object LetterTilesBuilder {
    private const val MAX_LETTERS = 18

    fun canBuild(front: String): Boolean {
        val answer = AnswerAlternatives.split(front).firstOrNull() ?: return false
        val letters = answer.count { !it.isWhitespace() }
        return letters in 2..MAX_LETTERS && answer.toSet().size > 1
    }

    /** Returns the answer and its shuffled tiles (never in the original order). */
    fun build(front: String, random: Random): Pair<String, List<String>>? {
        if (!canBuild(front)) return null
        val answer = AnswerAlternatives.split(front).first()
        val letters = answer.filterNot { it.isWhitespace() }.map { it.toString() }
        var tiles = letters.shuffled(random)
        var attempts = 0
        while (tiles == letters && attempts < MAX_SHUFFLES) {
            tiles = letters.shuffled(random)
            attempts++
        }
        return answer to tiles
    }

    private const val MAX_SHUFFLES = 10
}
