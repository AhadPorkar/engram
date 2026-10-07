package com.ahadporkar.engram.feature.decks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahadporkar.engram.core.data.repository.DeckRepository
import com.ahadporkar.engram.core.data.repository.StatsRepository
import com.ahadporkar.engram.core.model.DailyProgress
import com.ahadporkar.engram.core.model.Deck
import com.ahadporkar.engram.core.model.DeckSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

data class DecksUiState(
    val loading: Boolean = true,
    val decks: List<DeckSummary> = emptyList(),
    val progress: DailyProgress = DailyProgress(),
) {
    /** Cards that can be studied right now across all decks. */
    val availableToday: Int get() = decks.sumOf { it.newCount + it.dueCount }
}

/** Form values of the deck dialog. */
data class DeckDraft(
    val name: String,
    val description: String,
    val frontLanguage: String,
    val backLanguage: String,
    val newCardsPerDay: Int,
    val maxReviewsPerDay: Int,
)

@HiltViewModel
class DecksViewModel @Inject constructor(
    private val deckRepository: DeckRepository,
    statsRepository: StatsRepository,
    private val clock: Clock,
) : ViewModel() {

    val uiState: StateFlow<DecksUiState> = combine(
        deckRepository.observeDeckSummaries(),
        statsRepository.observeDailyProgress(),
    ) { decks, progress -> DecksUiState(loading = false, decks = decks, progress = progress) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DecksUiState())

    fun createDeck(draft: DeckDraft) {
        if (draft.name.isBlank()) return
        viewModelScope.launch {
            deckRepository.saveDeck(
                Deck(
                    name = draft.name,
                    description = draft.description,
                    frontLanguage = draft.frontLanguage,
                    backLanguage = draft.backLanguage,
                    newCardsPerDay = draft.newCardsPerDay,
                    maxReviewsPerDay = draft.maxReviewsPerDay,
                    createdAt = clock.instant(),
                ),
            )
        }
    }
}
