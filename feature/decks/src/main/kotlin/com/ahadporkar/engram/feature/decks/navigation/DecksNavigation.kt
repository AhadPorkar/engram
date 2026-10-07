package com.ahadporkar.engram.feature.decks.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.ahadporkar.engram.core.model.CramOrder
import com.ahadporkar.engram.core.model.StudyMode
import com.ahadporkar.engram.feature.decks.DeckDetailRoute
import com.ahadporkar.engram.feature.decks.DecksRoute
import kotlinx.serialization.Serializable

/** Home screen: all decks and today's goal. */
@Serializable
data object DecksDestination

/** One deck: its words, search and study modes. */
@Serializable
data class DeckDetailDestination(val deckId: Long)

fun NavController.navigateToDeck(deckId: Long) = navigate(DeckDetailDestination(deckId))

fun NavGraphBuilder.decksScreen(
    onOpenDeck: (Long) -> Unit,
    onStudyAll: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    composable<DecksDestination> {
        DecksRoute(
            onOpenDeck = onOpenDeck,
            onStudyAll = onStudyAll,
            onOpenStats = onOpenStats,
            onOpenSettings = onOpenSettings,
        )
    }
}

fun NavGraphBuilder.deckDetailScreen(
    onBack: () -> Unit,
    onStudy: (deckId: Long, mode: StudyMode, order: CramOrder) -> Unit,
    onAddNote: (deckId: Long) -> Unit,
    onEditNote: (deckId: Long, noteId: Long) -> Unit,
) {
    composable<DeckDetailDestination> {
        DeckDetailRoute(
            onBack = onBack,
            onStudy = onStudy,
            onAddNote = onAddNote,
            onEditNote = onEditNote,
        )
    }
}
