package com.ahadporkar.engram.feature.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahadporkar.engram.core.data.audio.Speaker
import com.ahadporkar.engram.core.data.repository.DeckRepository
import com.ahadporkar.engram.core.data.repository.NoteRepository
import com.ahadporkar.engram.core.data.repository.SettingsRepository
import com.ahadporkar.engram.core.model.Note
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

data class NoteEditorUiState(
    val loading: Boolean = true,
    val isNew: Boolean = true,
    val deckName: String = "",
    val frontLanguage: String = "en-US",
    val front: String = "",
    val back: String = "",
    val synonyms: String = "",
    val example: String = "",
    val mnemonic: String = "",
    val tags: String = "",
    val createReverse: Boolean = true,
    val duplicate: Boolean = false,
    val saving: Boolean = false,
    /** Close the screen. */
    val finished: Boolean = false,
    /** Number of words added with "Save & add another" in this visit. */
    val addedInRow: Int = 0,
) {
    val canSave: Boolean get() = front.isNotBlank() && back.isNotBlank() && !saving
}

@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val noteRepository: NoteRepository,
    private val deckRepository: DeckRepository,
    private val settingsRepository: SettingsRepository,
    private val speaker: Speaker,
    private val clock: Clock,
) : ViewModel() {

    private val deckId: Long = checkNotNull(savedStateHandle.get<Long>(DECK_ID_ARG)) { "deckId missing" }
    private val noteId: Long = savedStateHandle.get<Long>(NOTE_ID_ARG) ?: 0L

    private val _uiState = MutableStateFlow(NoteEditorUiState(isNew = noteId == 0L))
    val uiState: StateFlow<NoteEditorUiState> = _uiState.asStateFlow()

    private var original: Note? = null
    private var duplicateCheck: Job? = null

    init {
        viewModelScope.launch {
            val deck = deckRepository.getDeck(deckId)
            val settings = settingsRepository.current()
            val note = if (noteId != 0L) noteRepository.getNote(noteId) else null
            original = note
            _uiState.update {
                it.copy(
                    loading = false,
                    deckName = deck?.name.orEmpty(),
                    frontLanguage = deck?.frontLanguage ?: "en-US",
                    front = note?.front.orEmpty(),
                    back = note?.back.orEmpty(),
                    synonyms = note?.synonyms.orEmpty(),
                    example = note?.example.orEmpty(),
                    mnemonic = note?.mnemonic.orEmpty(),
                    tags = note?.tags?.sorted()?.joinToString(" ").orEmpty(),
                    createReverse = if (note != null) noteRepository.hasProductionCard(note.id) else settings.createReverseCards,
                )
            }
        }
    }

    fun onFrontChange(value: String) {
        _uiState.update { it.copy(front = value) }
        duplicateCheck?.cancel()
        duplicateCheck = viewModelScope.launch {
            delay(DUPLICATE_DEBOUNCE_MILLIS)
            val duplicate = noteRepository.isDuplicate(deckId, value, excludeNoteId = noteId)
            _uiState.update { it.copy(duplicate = duplicate) }
        }
    }

    fun onBackChange(value: String) = _uiState.update { it.copy(back = value) }

    fun onSynonymsChange(value: String) = _uiState.update { it.copy(synonyms = value) }

    fun onExampleChange(value: String) = _uiState.update { it.copy(example = value) }

    fun onMnemonicChange(value: String) = _uiState.update { it.copy(mnemonic = value) }

    fun onTagsChange(value: String) = _uiState.update { it.copy(tags = value) }

    fun onCreateReverseChange(value: Boolean) = _uiState.update { it.copy(createReverse = value) }

    fun speakFront() {
        val state = _uiState.value
        speaker.speak(state.front, state.frontLanguage)
    }

    /** Saves the note; with [addAnother] the form is cleared for the next word (fast entry). */
    fun save(addAnother: Boolean) {
        val state = _uiState.value
        if (!state.canSave) return
        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            val now = clock.instant()
            val base = original ?: Note(deckId = deckId, front = "", back = "", createdAt = now, updatedAt = now)
            noteRepository.saveNote(
                base.copy(
                    deckId = deckId,
                    front = state.front.trim(),
                    back = state.back.trim(),
                    synonyms = state.synonyms.trim(),
                    example = state.example.trim(),
                    mnemonic = state.mnemonic.trim(),
                    tags = state.tags.split(' ', ',').map { it.trim() }.filter { it.isNotEmpty() }.toSet(),
                ),
                withProductionCard = state.createReverse,
            )
            if (addAnother && original == null) {
                _uiState.update {
                    it.copy(
                        front = "",
                        back = "",
                        synonyms = "",
                        example = "",
                        mnemonic = "",
                        duplicate = false,
                        saving = false,
                        addedInRow = it.addedInRow + 1,
                    )
                }
            } else {
                _uiState.update { it.copy(saving = false, finished = true) }
            }
        }
    }

    fun delete() {
        val note = original ?: return
        viewModelScope.launch {
            noteRepository.deleteNote(note.id)
            _uiState.update { it.copy(finished = true) }
        }
    }

    companion object {
        const val DECK_ID_ARG = "deckId"
        const val NOTE_ID_ARG = "noteId"
        private const val DUPLICATE_DEBOUNCE_MILLIS = 300L
    }
}
