package com.ahadporkar.engram.feature.study

import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ahadporkar.engram.core.designsystem.theme.EngramTheme
import com.ahadporkar.engram.core.designsystem.util.intervalLabel
import com.ahadporkar.engram.core.learning.answer.Verdict
import com.ahadporkar.engram.core.learning.exercise.Exercise
import com.ahadporkar.engram.core.model.CardDirection
import com.ahadporkar.engram.core.model.ExerciseType
import com.ahadporkar.engram.core.model.StudyCard
import com.ahadporkar.engram.core.srs.Rating

// ---------------------------------------------------------------------------------------------
// Shared building blocks
// ---------------------------------------------------------------------------------------------

@Composable
private fun ExerciseColumn(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        content()
    }
}

@Composable
private fun Instruction(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun PromptText(text: String) {
    Text(
        text = text,
        style = if (text.length > 28) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.displaySmall,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SpeakButton(text: String, language: String, onAction: (StudyAction) -> Unit) {
    IconButton(onClick = { onAction(StudyAction.Speak(text, language)) }) {
        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = stringResource(R.string.play_audio))
    }
}

@Composable
private fun MnemonicCard(text: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
            Spacer(Modifier.width(8.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
        }
    }
}

@Composable
private fun WordDetails(item: StudyCard, audioAvailable: Boolean, onAction: (StudyAction) -> Unit) {
    val note = item.note
    if (note.synonyms.isNotBlank()) {
        Text(
            stringResource(R.string.synonyms_label, note.synonyms),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
    }
    if (note.example.isNotBlank()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                note.example,
                style = MaterialTheme.typography.bodyLarge,
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (audioAvailable) SpeakButton(note.example, item.frontLanguage, onAction)
        }
        Spacer(Modifier.height(12.dp))
    }
    if (note.mnemonic.isNotBlank()) {
        MnemonicCard(note.mnemonic)
    }
}

// ---------------------------------------------------------------------------------------------
// Presentation — first exposure to a new word
// ---------------------------------------------------------------------------------------------

@Composable
fun PresentationExercise(exercise: Exercise.Presentation, audioAvailable: Boolean, onAction: (StudyAction) -> Unit) {
    val item = exercise.item
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            ExerciseColumn {
                Surface(
                    color = EngramTheme.colors.newCards.copy(alpha = 0.14f),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        stringResource(R.string.new_word),
                        color = EngramTheme.colors.newCards,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }
                Spacer(Modifier.height(24.dp))
                PromptText(item.note.front)
                if (audioAvailable) {
                    Row {
                        SpeakButton(item.note.front, item.frontLanguage, onAction)
                        IconButton(onClick = { onAction(StudyAction.Speak(item.note.front, item.frontLanguage, slow = true)) }) {
                            Icon(Icons.Filled.SlowMotionVideo, contentDescription = stringResource(R.string.play_slowly))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(item.note.back, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                WordDetails(item, audioAvailable, onAction)
            }
        }
        Button(
            onClick = { onAction(StudyAction.PresentationDone) },
            modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(min = 52.dp),
        ) { Text(stringResource(R.string.got_it)) }
    }
}

// ---------------------------------------------------------------------------------------------
// Flashcard — self-graded (Anki style)
// ---------------------------------------------------------------------------------------------

@Composable
fun FlashcardExercise(exercise: Exercise.Flashcard, state: StudyUiState.Active, onAction: (StudyAction) -> Unit) {
    val item = exercise.item
    val rotation by animateFloatAsState(if (state.revealed) 180f else 0f, label = "flip")
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 12f * density
                }
                .clickable(enabled = !state.revealed) { onAction(StudyAction.Reveal) },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationY = if (rotation > 90f) 180f else 0f },
                contentAlignment = Alignment.Center,
            ) {
                ExerciseColumn {
                    Spacer(Modifier.height(24.dp))
                    PromptText(item.prompt)
                    if (state.audioAvailable && item.card.direction == CardDirection.RECOGNITION) {
                        SpeakButton(item.note.front, item.frontLanguage, onAction)
                    }
                    if (rotation > 90f) {
                        Spacer(Modifier.height(24.dp))
                        Text(
                            item.answer,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                        )
                        if (state.audioAvailable && item.card.direction == CardDirection.PRODUCTION) {
                            SpeakButton(item.note.front, item.frontLanguage, onAction)
                        }
                        Spacer(Modifier.height(16.dp))
                        WordDetails(item, state.audioAvailable, onAction)
                    } else {
                        Spacer(Modifier.height(32.dp))
                        Text(
                            stringResource(R.string.tap_to_reveal),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        if (state.revealed) {
            RatingButtons(previews = state.previews, onRate = { onAction(StudyAction.Rate(it)) })
        } else {
            Button(
                onClick = { onAction(StudyAction.Reveal) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) { Text(stringResource(R.string.show_answer)) }
        }
    }
}

@Composable
private fun RatingButtons(previews: Map<Rating, Long>?, onRate: (Rating) -> Unit) {
    val colors = EngramTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Rating.entries.forEach { rating ->
            val color = when (rating) {
                Rating.AGAIN -> colors.again
                Rating.HARD -> colors.hard
                Rating.GOOD -> colors.good
                Rating.EASY -> colors.easy
            }
            val label = when (rating) {
                Rating.AGAIN -> R.string.rating_again
                Rating.HARD -> R.string.rating_hard
                Rating.GOOD -> R.string.rating_good
                Rating.EASY -> R.string.rating_easy
            }
            OutlinedButton(
                onClick = { onRate(rating) },
                border = BorderStroke(1.5.dp, color),
                modifier = Modifier.weight(1f).heightIn(min = 60.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(label), color = color, style = MaterialTheme.typography.labelLarge)
                    previews?.get(rating)?.let {
                        Text(intervalLabel(it), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Multiple choice / listening
// ---------------------------------------------------------------------------------------------

@Composable
fun ChoiceExercise(exercise: Exercise.Choice, state: StudyUiState.Active, onAction: (StudyAction) -> Unit) {
    val colors = EngramTheme.colors
    val feedback = state.feedback
    ExerciseColumn {
        Instruction(
            stringResource(
                when (exercise.type) {
                    ExerciseType.LISTENING -> R.string.instruction_listening
                    ExerciseType.REVERSE_MULTIPLE_CHOICE -> R.string.instruction_choose_word
                    else -> R.string.instruction_choose_meaning
                },
            ),
        )
        if (exercise.audioOnly) {
            FilledIconButton(
                onClick = { onAction(StudyAction.Speak(exercise.prompt, exercise.promptLanguage)) },
                modifier = Modifier.size(96.dp),
                colors = IconButtonDefaults.filledIconButtonColors(),
            ) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = stringResource(R.string.play_audio), modifier = Modifier.size(48.dp))
            }
            TextButton(onClick = { onAction(StudyAction.Speak(exercise.prompt, exercise.promptLanguage, slow = true)) }) {
                Text(stringResource(R.string.play_slowly))
            }
            if (feedback != null) {
                Text(exercise.prompt, style = MaterialTheme.typography.headlineSmall)
            }
        } else {
            PromptText(exercise.prompt)
            if (state.audioAvailable && exercise.type == ExerciseType.MULTIPLE_CHOICE) {
                SpeakButton(exercise.prompt, exercise.promptLanguage, onAction)
            }
        }
        Spacer(Modifier.height(24.dp))
        exercise.options.forEachIndexed { index, option ->
            val container = when {
                feedback == null -> Color.Transparent
                index == exercise.correctIndex -> colors.correctContainer
                index == feedback.selectedIndex -> colors.incorrectContainer
                else -> Color.Transparent
            }
            OutlinedCard(
                onClick = { onAction(StudyAction.Choose(index)) },
                enabled = feedback == null,
                colors = CardDefaults.outlinedCardColors(containerColor = container, disabledContainerColor = container),
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            ) {
                Text(
                    option,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Letter tiles — spell the word from scrambled letters
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LetterTilesExercise(exercise: Exercise.LetterTiles, state: StudyUiState.Active, onAction: (StudyAction) -> Unit) {
    var picked by rememberSaveable { mutableStateOf(listOf<Int>()) }
    val locked = state.feedback != null
    ExerciseColumn {
        Instruction(stringResource(R.string.instruction_spell))
        PromptText(exercise.prompt)
        Spacer(Modifier.height(24.dp))

        // The word being built, keeping the spaces of the answer visible.
        val built = buildString {
            var next = 0
            for (char in exercise.answer) {
                if (char.isWhitespace()) {
                    append("   ")
                } else {
                    append(picked.getOrNull(next)?.let { exercise.tiles[it] } ?: "_")
                    append(' ')
                    next++
                }
            }
        }
        Text(built.trimEnd(), style = MaterialTheme.typography.headlineMedium, letterSpacing = 2.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            exercise.tiles.forEachIndexed { index, tile ->
                val used = index in picked
                FilledTonalButton(
                    onClick = { picked = picked + index },
                    enabled = !used && !locked,
                    modifier = Modifier.size(52.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                ) { Text(tile, style = MaterialTheme.typography.titleLarge) }
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { picked = picked.dropLast(1) }, enabled = picked.isNotEmpty() && !locked) {
                Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = stringResource(R.string.remove_letter))
            }
            Button(
                onClick = { onAction(StudyAction.SubmitTiles(picked.joinToString("") { exercise.tiles[it] })) },
                enabled = picked.size == exercise.tiles.size && !locked,
            ) { Text(stringResource(R.string.check)) }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Typing and cloze — free recall
// ---------------------------------------------------------------------------------------------

@Composable
fun TypingExercise(exercise: Exercise.Typing, state: StudyUiState.Active, onAction: (StudyAction) -> Unit) {
    ExerciseColumn {
        Instruction(stringResource(R.string.instruction_type_word))
        PromptText(exercise.prompt)
        Spacer(Modifier.height(24.dp))
        AnswerField(hint = exercise.hint, state = state, onAction = onAction)
    }
}

@Composable
fun ClozeExercise(exercise: Exercise.Cloze, state: StudyUiState.Active, onAction: (StudyAction) -> Unit) {
    val gapColor = MaterialTheme.colorScheme.primary
    ExerciseColumn {
        Instruction(stringResource(R.string.instruction_cloze))
        Text(
            text = buildAnnotatedString {
                append(exercise.before)
                withStyle(SpanStyle(color = gapColor, fontWeight = FontWeight.Bold)) {
                    append(state.feedback?.correctAnswer ?: "_____")
                }
                append(exercise.after)
            },
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.cloze_translation, exercise.translation),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        AnswerField(hint = exercise.hint, state = state, onAction = onAction)
    }
}

@Composable
private fun AnswerField(hint: String, state: StudyUiState.Active, onAction: (StudyAction) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val locked = state.feedback != null
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    OutlinedTextField(
        value = text,
        onValueChange = { if (!locked) text = it },
        label = { Text(stringResource(R.string.your_answer)) },
        singleLine = true,
        readOnly = locked,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { if (!locked) onAction(StudyAction.SubmitText(text)) }),
        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
    )
    Spacer(Modifier.height(8.dp))
    if (state.hintUsed) {
        Text(hint, style = MaterialTheme.typography.titleMedium, letterSpacing = 2.sp)
    }
    Spacer(Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = { onAction(StudyAction.ShowHint) }, enabled = !state.hintUsed && !locked) {
            Icon(Icons.Filled.Lightbulb, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.show_hint))
        }
        Button(onClick = { onAction(StudyAction.SubmitText(text)) }, enabled = text.isNotBlank() && !locked) {
            Text(stringResource(R.string.check))
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Speaking — say the word, checked with on-device speech recognition
// ---------------------------------------------------------------------------------------------

@Composable
fun SpeakingExercise(exercise: Exercise.Speaking, state: StudyUiState.Active, onAction: (StudyAction) -> Unit) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val candidates = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).orEmpty()
        if (candidates.isNotEmpty()) onAction(StudyAction.SpeechResult(candidates))
    }
    val prompt = stringResource(R.string.instruction_speak)
    ExerciseColumn {
        Instruction(prompt)
        PromptText(exercise.prompt)
        Spacer(Modifier.height(32.dp))
        FilledIconButton(
            onClick = {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, exercise.language)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, exercise.prompt)
                }
                try {
                    launcher.launch(intent)
                } catch (e: ActivityNotFoundException) {
                    onAction(StudyAction.SkipSpeaking)
                }
            },
            enabled = state.feedback == null,
            modifier = Modifier.size(96.dp),
        ) {
            Icon(Icons.Filled.Mic, contentDescription = stringResource(R.string.start_speaking), modifier = Modifier.size(48.dp))
        }
        Spacer(Modifier.height(16.dp))
        if (state.feedback == null) {
            TextButton(onClick = { onAction(StudyAction.SkipSpeaking) }) {
                Text(stringResource(R.string.cant_speak_now))
            }
        } else if (state.feedback.verdict == Verdict.WRONG && state.feedback.typed != null) {
            Text(stringResource(R.string.we_heard, state.feedback.typed), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
