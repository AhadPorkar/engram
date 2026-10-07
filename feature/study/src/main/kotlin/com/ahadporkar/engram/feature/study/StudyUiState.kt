package com.ahadporkar.engram.feature.study

import com.ahadporkar.engram.core.learning.answer.CloseReason
import com.ahadporkar.engram.core.learning.answer.DiffSegment
import com.ahadporkar.engram.core.learning.answer.Verdict
import com.ahadporkar.engram.core.learning.exercise.Exercise
import com.ahadporkar.engram.core.learning.session.RemainingCounts
import com.ahadporkar.engram.core.model.StudyMode
import com.ahadporkar.engram.core.srs.Rating
import java.time.Instant

sealed interface StudyUiState {
    data object Loading : StudyUiState

    /** Nothing is due and no new cards are left for today. */
    data object Empty : StudyUiState

    data class Active(
        val exercise: Exercise,
        val mode: StudyMode,
        /** Changes for every new exercise so per-exercise UI state is reset. */
        val exerciseKey: Long,
        val remaining: RemainingCounts,
        val completion: Float,
        val canUndo: Boolean,
        val audioAvailable: Boolean,
        val revealed: Boolean = false,
        val hintUsed: Boolean = false,
        /** Interval each rating button would give (seconds); flashcards only. */
        val previews: Map<Rating, Long>? = null,
        val feedback: Feedback? = null,
    ) : StudyUiState

    data class Finished(
        val summary: SessionSummary,
        /** Learning cards that come back later today. */
        val laterLearning: Int,
        val nextLearningDue: Instant?,
    ) : StudyUiState
}

/** Result panel shown after an auto-graded answer. */
data class Feedback(
    val verdict: Verdict,
    val closeReason: CloseReason?,
    val correctAnswer: String,
    val typed: String?,
    val diff: List<DiffSegment>,
    val selectedIndex: Int?,
    val rating: Rating,
    /** Time until the next review, `null` in practice mode. */
    val intervalSeconds: Long?,
    val becameLeech: Boolean,
    /** "I was right" is offered for free-text answers judged wrong. */
    val canOverride: Boolean,
    val overridden: Boolean = false,
)

data class SessionSummary(
    val answered: Int,
    val accuracy: Double?,
    val newIntroduced: Int,
    val studyMillis: Long,
    val leeches: Int,
)

/** Everything the study screen can ask the ViewModel to do. */
sealed interface StudyAction {
    data object PresentationDone : StudyAction
    data class Choose(val index: Int) : StudyAction
    data class SubmitTiles(val answer: String) : StudyAction
    data class SubmitText(val text: String) : StudyAction
    data class SpeechResult(val candidates: List<String>) : StudyAction
    data object SkipSpeaking : StudyAction
    data object Reveal : StudyAction
    data class Rate(val rating: Rating) : StudyAction
    data object ShowHint : StudyAction
    data object Continue : StudyAction
    data object OverrideCorrect : StudyAction
    data object Undo : StudyAction
    data class Speak(val text: String, val language: String, val slow: Boolean = false) : StudyAction
    data object CheckAgain : StudyAction
}
