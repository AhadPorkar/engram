package com.ahadporkar.engram.feature.study

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ahadporkar.engram.core.designsystem.component.EmptyState
import com.ahadporkar.engram.core.designsystem.theme.EngramTheme
import com.ahadporkar.engram.core.learning.exercise.Exercise
import com.ahadporkar.engram.core.learning.session.RemainingCounts

@Composable
fun StudyRoute(
    onClose: () -> Unit,
    viewModel: StudyViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    StudyScreen(state = state, onAction = viewModel::onAction, onClose = onClose)
}

@Composable
fun StudyScreen(
    state: StudyUiState,
    onAction: (StudyAction) -> Unit,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)
    Scaffold(
        topBar = {
            StudyTopBar(
                active = state as? StudyUiState.Active,
                onClose = onClose,
                onUndo = { onAction(StudyAction.Undo) },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (state) {
                StudyUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                StudyUiState.Empty -> EmptyState(
                    icon = Icons.Filled.CheckCircle,
                    title = stringResource(R.string.empty_session_title),
                    message = stringResource(R.string.empty_session_message),
                    modifier = Modifier.align(Alignment.Center),
                    action = { Button(onClick = onClose) { Text(stringResource(R.string.done)) } },
                )
                is StudyUiState.Finished -> SessionFinished(
                    state = state,
                    onDone = onClose,
                    onCheckAgain = { onAction(StudyAction.CheckAgain) },
                )
                is StudyUiState.Active -> key(state.exerciseKey) {
                    ActiveExercise(state = state, onAction = onAction)
                }
            }
        }
    }
}

@Composable
private fun ActiveExercise(state: StudyUiState.Active, onAction: (StudyAction) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (val exercise = state.exercise) {
                is Exercise.Presentation -> PresentationExercise(exercise, state.audioAvailable, onAction)
                is Exercise.Flashcard -> FlashcardExercise(exercise, state, onAction)
                is Exercise.Choice -> ChoiceExercise(exercise, state, onAction)
                is Exercise.LetterTiles -> LetterTilesExercise(exercise, state, onAction)
                is Exercise.Typing -> TypingExercise(exercise, state, onAction)
                is Exercise.Cloze -> ClozeExercise(exercise, state, onAction)
                is Exercise.Speaking -> SpeakingExercise(exercise, state, onAction)
            }
        }
        state.feedback?.let { feedback ->
            FeedbackPanel(
                feedback = feedback,
                exercise = state.exercise,
                audioAvailable = state.audioAvailable,
                onAction = onAction,
            )
        }
    }
}

@Composable
private fun StudyTopBar(active: StudyUiState.Active?, onClose: () -> Unit, onUndo: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.end_session))
        }
        if (active != null) {
            LinearProgressIndicator(
                progress = { active.completion },
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(12.dp))
            RemainingCountsText(active.remaining)
            IconButton(onClick = onUndo, enabled = active.canUndo) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = stringResource(R.string.undo))
            }
        } else {
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun RemainingCountsText(remaining: RemainingCounts) {
    val colors = EngramTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(remaining.new.toString(), color = colors.newCards, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
        Text(remaining.learning.toString(), color = colors.learningCards, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
        Text(remaining.review.toString(), color = colors.reviewCards, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
    }
}
