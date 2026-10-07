package com.ahadporkar.engram.feature.editor.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.ahadporkar.engram.feature.editor.NoteEditorRoute
import kotlinx.serialization.Serializable

/** Add (noteId = 0) or edit a word. Successor of the old `word_input_dialog`. */
@Serializable
data class NoteEditorDestination(val deckId: Long, val noteId: Long = 0L)

fun NavController.navigateToNoteEditor(deckId: Long, noteId: Long = 0L) =
    navigate(NoteEditorDestination(deckId, noteId))

fun NavGraphBuilder.noteEditorScreen(onClose: () -> Unit) {
    composable<NoteEditorDestination> {
        NoteEditorRoute(onClose = onClose)
    }
}
