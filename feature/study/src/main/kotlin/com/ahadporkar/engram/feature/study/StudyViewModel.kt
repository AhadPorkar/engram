package com.ahadporkar.engram.feature.study

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahadporkar.engram.core.data.audio.Speaker
import com.ahadporkar.engram.core.data.mapper.enumValueOrDefault
import com.ahadporkar.engram.core.data.repository.SessionRequest
import com.ahadporkar.engram.core.data.repository.StudyRepository
import com.ahadporkar.engram.core.learning.answer.AnswerDiff
import com.ahadporkar.engram.core.learning.answer.AnswerEvaluator
import com.ahadporkar.engram.core.learning.answer.AnswerResult
import com.ahadporkar.engram.core.learning.answer.Verdict
import com.ahadporkar.engram.core.learning.exercise.DistractorPool
import com.ahadporkar.engram.core.learning.exercise.Exercise
import com.ahadporkar.engram.core.learning.exercise.ExerciseFactory
import com.ahadporkar.engram.core.learning.exercise.ExerciseSelector
import com.ahadporkar.engram.core.learning.exercise.SelectionContext
import com.ahadporkar.engram.core.learning.grading.ExerciseOutcome
import com.ahadporkar.engram.core.learning.grading.RatingPolicy
import com.ahadporkar.engram.core.learning.session.SessionEntry
import com.ahadporkar.engram.core.learning.session.StudySession
import com.ahadporkar.engram.core.model.CardDirection
import com.ahadporkar.engram.core.model.CramOrder
import com.ahadporkar.engram.core.model.ExerciseType
import com.ahadporkar.engram.core.model.StudyMode
import com.ahadporkar.engram.core.model.TypingTolerance
import com.ahadporkar.engram.core.model.UserSettings
import com.ahadporkar.engram.core.srs.FsrsScheduler
import com.ahadporkar.engram.core.srs.Rating
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import kotlin.random.Random

/**
 * Drives a study session: picks the exercise format, grades answers, persists every answer
 * immediately (so nothing is lost if the app is killed) and supports undo.
 */
@HiltViewModel
class StudyViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val studyRepository: StudyRepository,
    private val speaker: Speaker,
    private val speechRecognition: SpeechRecognitionAvailability,
    private val clock: Clock,
) : ViewModel() {

    private val request = SessionRequest(
        deckId = savedStateHandle.get<Long>(DECK_ID_ARG)?.takeIf { it > 0 },
        mode = savedStateHandle.get<String>(MODE_ARG)?.let { enumValueOrDefault(it, StudyMode.SMART) } ?: StudyMode.SMART,
        cramOrder = savedStateHandle.get<String>(CRAM_ORDER_ARG)?.let { enumValueOrDefault(it, CramOrder.SHUFFLE) }
            ?: CramOrder.SHUFFLE,
    )

    private val _uiState = MutableStateFlow<StudyUiState>(StudyUiState.Loading)
    val uiState: StateFlow<StudyUiState> = _uiState.asStateFlow()

    private val mutex = Mutex()
    private val random = Random.Default
    private val selector = ExerciseSelector(random)
    private val factory = ExerciseFactory(random)

    private lateinit var settings: UserSettings
    private lateinit var session: StudySession
    private lateinit var pool: DistractorPool
    private lateinit var scheduler: FsrsScheduler
    private lateinit var evaluator: AnswerEvaluator
    private val speechEvaluator by lazy { AnswerEvaluator(TypingTolerance.LENIENT, ignoreAccents = true) }

    private var shownAt: Instant = clock.instant()
    private var exerciseCounter = 0L
    private var speakingDisabled = false

    init {
        viewModelScope.launch { load() }
    }

    fun onAction(action: StudyAction) {
        when (action) {
            StudyAction.PresentationDone -> launchLocked { completePresentation() }
            is StudyAction.Choose -> launchLocked { answerChoice(action.index) }
            is StudyAction.SubmitTiles -> launchLocked { answerTiles(action.answer) }
            is StudyAction.SubmitText -> launchLocked { answerText(action.text) }
            is StudyAction.SpeechResult -> launchLocked { answerSpeech(action.candidates) }
            StudyAction.SkipSpeaking -> launchLocked { skipSpeaking() }
            StudyAction.Reveal -> reveal()
            is StudyAction.Rate -> launchLocked { rateFlashcard(action.rating) }
            StudyAction.ShowHint -> updateActive { it.copy(hintUsed = true) }
            StudyAction.Continue -> launchLocked { if (activeState()?.feedback != null) advance() }
            StudyAction.OverrideCorrect -> launchLocked { overrideCorrect() }
            StudyAction.Undo -> launchLocked { undo() }
            is StudyAction.Speak -> speaker.speak(action.text, action.language, action.slow)
            StudyAction.CheckAgain -> launchLocked { if (_uiState.value is StudyUiState.Finished) advance() }
        }
    }

    private suspend fun load() {
        val data = try {
            studyRepository.loadSession(request)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _uiState.value = StudyUiState.Empty
            return
        }
        // Text-to-speech initialises asynchronously; give it a moment so the first card can use audio.
        withTimeoutOrNull(TTS_WAIT_MILLIS) { speaker.isReady.first { it } }
        settings = data.settings
        pool = data.pool
        scheduler = FsrsScheduler(settings.toSchedulerConfig())
        evaluator = AnswerEvaluator(settings.typingTolerance, settings.ignoreAccents)
        session = StudySession(
            initial = data.cards,
            mode = request.mode,
            scheduler = scheduler,
            leechThreshold = settings.leechThreshold,
            leechAction = settings.leechAction,
        )
        if (data.cards.isEmpty()) {
            _uiState.value = StudyUiState.Empty
        } else {
            mutex.withLock { advance() }
        }
    }

    private fun advance() {
        val entry = session.next(clock.instant())
        if (entry == null) finish() else show(entry)
    }

    private fun show(entry: SessionEntry) {
        val item = entry.item
        val audio = speaker.isReady.value && speaker.canSpeak(item.frontLanguage)
        val type = selector.select(
            item,
            SelectionContext(
                mode = request.mode,
                presentedThisSession = entry.presented,
                audioAvailable = audio,
                speakingEnabled = settings.speakingExercises && !speakingDisabled && speechRecognition.isAvailable(),
                pool = pool,
            ),
        )
        val exercise = factory.create(type, item, pool)
        shownAt = clock.instant()
        exerciseCounter++
        _uiState.value = StudyUiState.Active(
            exercise = exercise,
            mode = request.mode,
            exerciseKey = exerciseCounter,
            remaining = session.remaining(),
            completion = session.completion(),
            canUndo = session.canUndo,
            audioAvailable = audio,
            previews = if (type == ExerciseType.FLASHCARD && request.mode != StudyMode.CRAM) {
                scheduler.preview(item.card.scheduling, shownAt).mapValues { it.value.interval.seconds }
            } else {
                null
            },
        )
        autoPlay(exercise, audio)
    }

    private fun autoPlay(exercise: Exercise, audio: Boolean) {
        if (!audio || !settings.autoPlayAudio) return
        val item = exercise.item
        val speakFront = when (exercise) {
            is Exercise.Presentation -> true
            is Exercise.Choice -> exercise.type != ExerciseType.REVERSE_MULTIPLE_CHOICE
            is Exercise.Flashcard -> item.card.direction == CardDirection.RECOGNITION
            else -> false
        }
        if (speakFront) speaker.speak(item.note.front, item.frontLanguage)
    }

    private fun completePresentation() {
        if (activeState()?.exercise !is Exercise.Presentation) return
        session.completePresentation(elapsedMillis())
        advance()
    }

    private suspend fun answerChoice(index: Int) {
        val state = activeState() ?: return
        val exercise = state.exercise as? Exercise.Choice ?: return
        if (state.feedback != null) return
        val verdict = if (index == exercise.correctIndex) Verdict.EXACT else Verdict.WRONG
        submit(state, AnswerResult(verdict, exercise.correctOption), typed = null, selectedIndex = index)
    }

    private suspend fun answerTiles(answer: String) {
        val state = activeState() ?: return
        val exercise = state.exercise as? Exercise.LetterTiles ?: return
        if (state.feedback != null) return
        val correct = answer.filterNot(Char::isWhitespace).equals(exercise.answer.filterNot(Char::isWhitespace), ignoreCase = true)
        submit(state, AnswerResult(if (correct) Verdict.EXACT else Verdict.WRONG, exercise.answer), typed = answer, selectedIndex = null)
    }

    private suspend fun answerText(text: String) {
        val state = activeState() ?: return
        if (state.feedback != null || text.isBlank()) return
        val result = when (val exercise = state.exercise) {
            is Exercise.Typing -> evaluator.evaluate(text, exercise.accepted, exercise.synonyms)
            is Exercise.Cloze -> evaluator.evaluate(text, exercise.accepted)
            else -> return
        }
        submit(state, result, typed = text, selectedIndex = null)
    }

    private suspend fun answerSpeech(candidates: List<String>) {
        val state = activeState() ?: return
        val exercise = state.exercise as? Exercise.Speaking ?: return
        if (state.feedback != null || candidates.isEmpty()) return
        val best = candidates
            .map { it to speechEvaluator.evaluate(it, exercise.accepted) }
            .minWith(compareBy({ it.second.verdict.ordinal }, { it.second.distance }))
        submit(state, best.second, typed = best.first, selectedIndex = null)
    }

    private fun skipSpeaking() {
        speakingDisabled = true
        session.current?.let(::show)
    }

    private suspend fun submit(state: StudyUiState.Active, result: AnswerResult, typed: String?, selectedIndex: Int?) {
        val exercise = state.exercise
        val duration = elapsedMillis()
        val rating = RatingPolicy.rate(
            ExerciseOutcome(
                exercise = exercise.type,
                verdict = result.verdict,
                responseMillis = duration,
                answerLength = exercise.item.answer.length,
                hintUsed = state.hintUsed,
            ),
        )
        val applied = applyRating(rating, exercise.type, duration)
        _uiState.value = state.copy(
            feedback = Feedback(
                verdict = result.verdict,
                closeReason = result.closeReason,
                correctAnswer = result.expected.ifBlank { exercise.item.answer },
                typed = typed,
                diff = if (typed != null && result.verdict != Verdict.EXACT) AnswerDiff.diff(typed.trim(), result.expected) else emptyList(),
                selectedIndex = selectedIndex,
                rating = rating,
                intervalSeconds = applied.intervalSeconds,
                becameLeech = applied.becameLeech,
                canOverride = result.verdict == Verdict.WRONG && exercise.type in OVERRIDABLE,
            ),
            canUndo = session.canUndo,
            remaining = session.remaining(),
            completion = session.completion(),
        )
        // Hearing the correct word right after producing it reinforces the form.
        if (state.audioAvailable && exercise.item.card.direction == CardDirection.PRODUCTION) {
            speaker.speak(exercise.item.note.front, exercise.item.frontLanguage)
        }
    }

    private suspend fun rateFlashcard(rating: Rating) {
        val state = activeState() ?: return
        if (state.exercise !is Exercise.Flashcard || !state.revealed) return
        applyRating(rating, ExerciseType.FLASHCARD, elapsedMillis())
        advance()
    }

    private fun reveal() {
        val state = activeState() ?: return
        if (state.revealed) return
        _uiState.value = state.copy(revealed = true)
        val item = state.exercise.item
        if (state.audioAvailable && settings.autoPlayAudio && item.card.direction == CardDirection.PRODUCTION) {
            speaker.speak(item.note.front, item.frontLanguage)
        }
    }

    private suspend fun overrideCorrect() {
        val state = activeState() ?: return
        val feedback = state.feedback ?: return
        if (!feedback.canOverride) return
        val undo = session.undo() ?: return
        undo.previousCard?.let { studyRepository.revertAnswer(it, undo.logId) }
        val applied = applyRating(Rating.GOOD, state.exercise.type, elapsedMillis())
        _uiState.value = state.copy(
            feedback = feedback.copy(
                rating = Rating.GOOD,
                overridden = true,
                canOverride = false,
                intervalSeconds = applied.intervalSeconds,
                becameLeech = false,
            ),
            remaining = session.remaining(),
            completion = session.completion(),
            canUndo = session.canUndo,
        )
    }

    private suspend fun undo() {
        val undo = session.undo() ?: return
        undo.previousCard?.let { studyRepository.revertAnswer(it, undo.logId) }
        show(undo.entry)
    }

    private data class Applied(val intervalSeconds: Long?, val becameLeech: Boolean)

    private suspend fun applyRating(rating: Rating, type: ExerciseType, durationMillis: Long): Applied {
        val now = clock.instant()
        val applied = session.answer(rating, now, type, durationMillis.coerceAtMost(MAX_COUNTED_MILLIS))
        val log = applied.log
        if (log != null) {
            studyRepository.persistAnswer(applied.card, log)?.let(session::markPersisted)
        }
        val interval = if (log != null) Duration.between(now, applied.card.scheduling.due).seconds else null
        return Applied(interval, applied.becameLeech)
    }

    private fun finish() {
        val progress = session.progress
        val now = clock.instant()
        _uiState.value = if (progress.answered == 0 && progress.newIntroduced == 0 && session.laterLearningCount(now) == 0) {
            StudyUiState.Empty
        } else {
            StudyUiState.Finished(
                summary = SessionSummary(
                    answered = progress.answered,
                    accuracy = progress.accuracy,
                    newIntroduced = progress.newIntroduced,
                    studyMillis = progress.studyMillis,
                    leeches = progress.leeches,
                ),
                laterLearning = session.laterLearningCount(now),
                nextLearningDue = session.nextLearningDue(),
            )
        }
    }

    private fun activeState(): StudyUiState.Active? = _uiState.value as? StudyUiState.Active

    private fun updateActive(transform: (StudyUiState.Active) -> StudyUiState.Active) {
        activeState()?.let { _uiState.value = transform(it) }
    }

    private fun elapsedMillis(): Long = Duration.between(shownAt, clock.instant()).toMillis().coerceAtLeast(0)

    /** Serialises actions so a double tap cannot grade a card twice. */
    private fun launchLocked(block: suspend () -> Unit) {
        viewModelScope.launch {
            if (!::session.isInitialized) return@launch
            mutex.withLock { block() }
        }
    }

    override fun onCleared() {
        speaker.stop()
        super.onCleared()
    }

    companion object {
        const val DECK_ID_ARG = "deckId"
        const val MODE_ARG = "mode"
        const val CRAM_ORDER_ARG = "cramOrder"

        private const val TTS_WAIT_MILLIS = 1_500L

        /** Like Anki: time beyond one minute per card is not counted as study time. */
        private const val MAX_COUNTED_MILLIS = 60_000L

        private val OVERRIDABLE = setOf(ExerciseType.TYPING, ExerciseType.CLOZE, ExerciseType.SPEAKING)
    }
}
