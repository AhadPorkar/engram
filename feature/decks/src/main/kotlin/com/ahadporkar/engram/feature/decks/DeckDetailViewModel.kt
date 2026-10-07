package com.ahadporkar.engram.feature.decks

import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahadporkar.engram.core.data.backup.BackupRepository
import com.ahadporkar.engram.core.data.backup.importErrorMessage
import com.ahadporkar.engram.core.data.repository.DeckRepository
import com.ahadporkar.engram.core.data.repository.NoteRepository
import com.ahadporkar.engram.core.data.repository.NoteWithMemory
import com.ahadporkar.engram.core.model.Deck
import com.ahadporkar.engram.core.model.DeckSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One-off messages shown in a snackbar. */
sealed interface DeckMessage {
    data class Imported(val count: Int, val skipped: Int) : DeckMessage
    data class Failed(@StringRes val message: Int) : DeckMessage
}

data class DeckDetailUiState(
    val loading: Boolean = true,
    val deck: Deck? = null,
    val summary: DeckSummary? = null,
    val notes: List<NoteWithMemory> = emptyList(),
    val query: String = "",
    val starredOnly: Boolean = false,
    val deleted: Boolean = false,
    val message: DeckMessage? = null,
) {
    val availableToday: Int get() = summary?.let { it.newCount + it.dueCount } ?: 0
}

@HiltViewModel
class DeckDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val deckRepository: DeckRepository,
    private val noteRepository: NoteRepository,
    private val backupRepository: BackupRepository,
) : ViewModel() {

    private val deckId: Long = checkNotNull(savedStateHandle.get<Long>(DECK_ID_ARG)) { "deckId missing" }

    private val filter = MutableStateFlow(Filter())
    private val message = MutableStateFlow<DeckMessage?>(null)

    private data class Filter(val query: String = "", val starredOnly: Boolean = false)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val notes = filter.flatMapLatest { noteRepository.observeNotes(deckId, it.query, it.starredOnly) }

    val uiState: StateFlow<DeckDetailUiState> = combine(
        deckRepository.observeDeck(deckId),
        deckRepository.observeDeckSummaries().map { list -> list.firstOrNull { it.deck.id == deckId } },
        notes,
        filter,
        message,
    ) { deck, summary, notes, filter, message ->
        DeckDetailUiState(
            loading = false,
            deck = deck,
            summary = summary,
            notes = notes,
            query = filter.query,
            starredOnly = filter.starredOnly,
            deleted = deck == null,
            message = message,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DeckDetailUiState())

    fun setQuery(query: String) = filter.update { it.copy(query = query) }

    fun toggleStarredOnly() = filter.update { it.copy(starredOnly = !it.starredOnly) }

    fun setStarred(noteId: Long, starred: Boolean) {
        viewModelScope.launch { noteRepository.setStarred(noteId, starred) }
    }

    fun deleteNote(noteId: Long) {
        viewModelScope.launch { noteRepository.deleteNote(noteId) }
    }

    fun updateDeck(draft: DeckDraft) {
        val current = uiState.value.deck ?: return
        viewModelScope.launch {
            deckRepository.saveDeck(
                current.copy(
                    name = draft.name,
                    description = draft.description,
                    frontLanguage = draft.frontLanguage,
                    backLanguage = draft.backLanguage,
                    newCardsPerDay = draft.newCardsPerDay,
                    maxReviewsPerDay = draft.maxReviewsPerDay,
                ),
            )
        }
    }

    fun deleteDeck() {
        viewModelScope.launch { deckRepository.deleteDeck(deckId) }
    }

    fun importWordList(uri: Uri) {
        viewModelScope.launch {
            message.value = runCatching { backupRepository.importDelimited(uri, deckId, newDeckName = "") }
                .fold(
                    onSuccess = { DeckMessage.Imported(it.notes, it.skipped) },
                    onFailure = { DeckMessage.Failed(it.importErrorMessage()) },
                )
        }
    }

    fun messageShown() {
        message.value = null
    }

    companion object {
        /** Must match the property name in [com.ahadporkar.engram.feature.decks.navigation.DeckDetailDestination]. */
        const val DECK_ID_ARG = "deckId"
    }
}
