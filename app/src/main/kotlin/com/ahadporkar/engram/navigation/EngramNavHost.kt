package com.ahadporkar.engram.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.ahadporkar.engram.core.model.StudyMode
import com.ahadporkar.engram.feature.decks.navigation.DecksDestination
import com.ahadporkar.engram.feature.decks.navigation.deckDetailScreen
import com.ahadporkar.engram.feature.decks.navigation.decksScreen
import com.ahadporkar.engram.feature.decks.navigation.navigateToDeck
import com.ahadporkar.engram.feature.editor.navigation.navigateToNoteEditor
import com.ahadporkar.engram.feature.editor.navigation.noteEditorScreen
import com.ahadporkar.engram.feature.settings.navigation.navigateToSettings
import com.ahadporkar.engram.feature.settings.navigation.settingsScreen
import com.ahadporkar.engram.feature.stats.navigation.navigateToStats
import com.ahadporkar.engram.feature.stats.navigation.statsScreen
import com.ahadporkar.engram.feature.study.navigation.navigateToStudy
import com.ahadporkar.engram.feature.study.navigation.studyScreen

/** The whole app graph. Feature modules expose destinations; only the app wires them together. */
@Composable
fun EngramNavHost() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = DecksDestination,
        enterTransition = { slideInHorizontally { it / 4 } + fadeIn() },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { slideOutHorizontally { it / 4 } + fadeOut() },
    ) {
        decksScreen(
            onOpenDeck = navController::navigateToDeck,
            onStudyAll = { navController.navigateToStudy(deckId = null, mode = StudyMode.SMART) },
            onOpenStats = navController::navigateToStats,
            onOpenSettings = navController::navigateToSettings,
        )
        deckDetailScreen(
            onBack = navController::popIfResumed,
            onStudy = { deckId, mode, order -> navController.navigateToStudy(deckId, mode, order) },
            onAddNote = { deckId -> navController.navigateToNoteEditor(deckId) },
            onEditNote = { deckId, noteId -> navController.navigateToNoteEditor(deckId, noteId) },
        )
        noteEditorScreen(onClose = navController::popIfResumed)
        studyScreen(onClose = navController::popIfResumed)
        statsScreen(onBack = navController::popIfResumed)
        settingsScreen(onBack = navController::popIfResumed)
    }
}

/**
 * Pops only when the current screen is fully resumed, so a double tap on "back"
 * (or a back tap racing an automatic close) cannot pop the start destination.
 */
private fun NavController.popIfResumed() {
    if (currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
        popBackStack()
    }
}
