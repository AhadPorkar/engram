package com.ahadporkar.engram.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahadporkar.engram.core.data.repository.StatsOverview
import com.ahadporkar.engram.core.data.repository.StatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface StatsUiState {
    data object Loading : StatsUiState
    data class Ready(val overview: StatsOverview) : StatsUiState
}

@HiltViewModel
class StatsViewModel @Inject constructor(statsRepository: StatsRepository) : ViewModel() {
    val uiState: StateFlow<StatsUiState> = statsRepository.observeStats()
        .map<StatsOverview, StatsUiState> { StatsUiState.Ready(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState.Loading)
}
