package com.ahadporkar.engram.feature.study

import androidx.lifecycle.SavedStateHandle
import com.ahadporkar.engram.core.data.audio.Speaker
import com.ahadporkar.engram.core.data.repository.SessionData
import com.ahadporkar.engram.core.data.repository.SessionRequest
import com.ahadporkar.engram.core.data.repository.StudyRepository
import com.ahadporkar.engram.core.learning.answer.Verdict
import com.ahadporkar.engram.core.learning.exercise.DistractorPool
import com.ahadporkar.engram.core.learning.exercise.Exercise
import com.ahadporkar.engram.core.model.Card
import com.ahadporkar.engram.core.model.CardDirection
import com.ahadporkar.engram.core.model.Note
import com.ahadporkar.engram.core.model.ReviewLog
import com.ahadporkar.engram.core.model.StudyCard
import com.ahadporkar.engram.core.model.UserSettings
import com.ahadporkar.engram.core.srs.CardPhase
import com.ahadporkar.engram.core.srs.Rating
import com.ahadporkar.engram.core.srs.SchedulingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class StudyViewModelTest {

    private val now = Instant.parse("2026-03-10T10:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)

    private class FakeStudyRepository(private val cards: List<StudyCard>) : StudyRepository {
        val persisted = mutableListOf<Pair<Card, ReviewLog?>>()
        val reverted = mutableListOf<Long?>()

        override suspend fun loadSession(request: SessionRequest) = SessionData(
            cards = cards,
            pool = DistractorPool(
                fronts = listOf("die Katze", "das Haus", "der Baum", "die Blume"),
                backs = listOf("the cat", "the house", "the tree", "the flower"),
            ),
            settings = UserSettings(enableFuzz = false, autoPlayAudio = false),
        )

        override suspend fun persistAnswer(card: Card, log: ReviewLog?): Long? {
            persisted += card to log
            return persisted.size.toLong()
        }

        override suspend fun revertAnswer(previous: Card, logId: Long?) {
            reverted += logId
        }
    }

    /** Engine ready, but no voice for the language → exercises without audio. */
    private class SilentSpeaker : Speaker {
        override val isReady: StateFlow<Boolean> = MutableStateFlow(true)
        override fun canSpeak(languageTag: String) = false
        override fun speak(text: String, languageTag: String, slow: Boolean) = Unit
        override fun stop() = Unit
    }

    private fun newCard(id: Long, front: String, back: String) = StudyCard(
        card = Card(
            id = id,
            noteId = id,
            deckId = 1,
            direction = CardDirection.RECOGNITION,
            scheduling = SchedulingState.newCard(now),
        ),
        note = Note(id = id, deckId = 1, front = front, back = back, createdAt = now, updatedAt = now),
    )

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel(repository: FakeStudyRepository): StudyViewModel {
        val vm = StudyViewModel(
            savedStateHandle = SavedStateHandle(mapOf("deckId" to 1L, "mode" to "SMART", "cramOrder" to "SHUFFLE")),
            studyRepository = repository,
            speaker = SilentSpeaker(),
            speechRecognition = { false },
            clock = clock,
        )
        advanceUntilIdle()
        return vm
    }

    @Test
    fun `new word is presented first, then tested with multiple choice and persisted`() = runTest {
        val repository = FakeStudyRepository(listOf(newCard(1, "der Hund", "the dog")))
        val vm = viewModel(repository)

        val presentation = vm.uiState.value as StudyUiState.Active
        assertTrue(presentation.exercise is Exercise.Presentation)

        vm.onAction(StudyAction.PresentationDone)
        val quiz = vm.uiState.value as StudyUiState.Active
        val choice = quiz.exercise as Exercise.Choice
        assertEquals("the dog", choice.correctOption)

        vm.onAction(StudyAction.Choose(choice.correctIndex))
        val answered = vm.uiState.value as StudyUiState.Active
        assertEquals(Verdict.EXACT, answered.feedback?.verdict)
        assertEquals(Rating.GOOD, answered.feedback?.rating)
        assertEquals(1, repository.persisted.size)
        assertEquals(CardPhase.LEARNING, repository.persisted.single().first.phase)
        assertNotNull(repository.persisted.single().second)
    }

    @Test
    fun `wrong choice is rated again and can be undone`() = runTest {
        val repository = FakeStudyRepository(listOf(newCard(1, "der Hund", "the dog")))
        val vm = viewModel(repository)
        vm.onAction(StudyAction.PresentationDone)
        val choice = (vm.uiState.value as StudyUiState.Active).exercise as Exercise.Choice

        vm.onAction(StudyAction.Choose((choice.correctIndex + 1) % choice.options.size))
        assertEquals(Rating.AGAIN, (vm.uiState.value as StudyUiState.Active).feedback?.rating)

        vm.onAction(StudyAction.Undo)
        assertEquals(listOf<Long?>(1L), repository.reverted)
        assertEquals(null, (vm.uiState.value as StudyUiState.Active).feedback)
    }

    @Test
    fun `empty sessions show the empty state`() = runTest {
        val vm = viewModel(FakeStudyRepository(emptyList()))
        assertEquals(StudyUiState.Empty, vm.uiState.value)
    }
}
