package com.ahadporkar.engram.feature.study

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.ahadporkar.engram.core.designsystem.theme.EngramTheme
import com.ahadporkar.engram.core.designsystem.util.intervalLabel
import com.ahadporkar.engram.core.learning.answer.CloseReason
import com.ahadporkar.engram.core.learning.answer.DiffKind
import com.ahadporkar.engram.core.learning.answer.Verdict
import com.ahadporkar.engram.core.learning.exercise.Exercise

/**
 * Immediate, specific feedback after each answer: what was right, what exactly was wrong,
 * and when the word comes back.
 */
@Composable
fun FeedbackPanel(
    feedback: Feedback,
    exercise: Exercise,
    audioAvailable: Boolean,
    onAction: (StudyAction) -> Unit,
) {
    val colors = EngramTheme.colors
    val correct = feedback.verdict == Verdict.EXACT || feedback.overridden
    val (container, content) = when {
        correct -> colors.correctContainer to colors.onCorrectContainer
        feedback.verdict == Verdict.CLOSE -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        else -> colors.incorrectContainer to colors.onIncorrectContainer
    }
    val item = exercise.item

    Surface(
        color = container,
        contentColor = content,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when {
                        correct -> Icons.Filled.CheckCircle
                        feedback.verdict == Verdict.CLOSE -> Icons.Filled.Info
                        else -> Icons.Filled.Cancel
                    },
                    contentDescription = null,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(
                        when {
                            correct -> R.string.feedback_correct
                            feedback.verdict == Verdict.CLOSE -> R.string.feedback_close
                            else -> R.string.feedback_wrong
                        },
                    ),
                    style = MaterialTheme.typography.titleLarge,
                )
            }

            feedback.closeReason?.takeIf { !feedback.overridden }?.let { reason ->
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(
                        when (reason) {
                            CloseReason.TYPO -> R.string.feedback_reason_typo
                            CloseReason.ARTICLE -> R.string.feedback_reason_article
                            CloseReason.SYNONYM -> R.string.feedback_reason_synonym
                        },
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            if (feedback.diff.isNotEmpty() && !feedback.overridden) {
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.your_answer_was), style = MaterialTheme.typography.labelMedium)
                DiffText(feedback, content)
            }

            if (!correct || feedback.verdict == Verdict.CLOSE) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.correct_answer, feedback.correctAnswer),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (audioAvailable) {
                        IconButton(onClick = { onAction(StudyAction.Speak(item.note.front, item.frontLanguage)) }) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = stringResource(R.string.play_audio))
                        }
                    }
                }
            }

            if (item.note.example.isNotBlank() && exercise !is Exercise.Cloze) {
                Spacer(Modifier.height(4.dp))
                Text(item.note.example, style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic)
            }

            if (!correct && item.note.mnemonic.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.memory_hook, item.note.mnemonic), style = MaterialTheme.typography.bodyMedium)
            }

            if (feedback.becameLeech) {
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.leech_warning), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }

            feedback.intervalSeconds?.let { seconds ->
                Spacer(Modifier.height(8.dp))
                Text(
                    if (seconds < SECONDS_PER_DAY) {
                        stringResource(R.string.next_review_soon)
                    } else {
                        stringResource(R.string.next_review_in, intervalLabel(seconds))
                    },
                    style = MaterialTheme.typography.labelMedium,
                )
            }

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (feedback.canOverride) {
                    TextButton(onClick = { onAction(StudyAction.OverrideCorrect) }) {
                        Text(stringResource(R.string.i_was_right), color = content)
                    }
                    Spacer(Modifier.weight(1f))
                }
                Button(
                    onClick = { onAction(StudyAction.Continue) },
                    modifier = Modifier.heightIn(min = 48.dp).let { if (feedback.canOverride) it else it.fillMaxWidth() },
                ) { Text(stringResource(R.string.continue_label)) }
            }
        }
    }
}

@Composable
private fun DiffText(feedback: Feedback, content: Color) {
    val missing = MaterialTheme.colorScheme.primary
    val extra = EngramTheme.colors.again
    Text(
        text = buildAnnotatedString {
            feedback.diff.forEach { segment ->
                when (segment.kind) {
                    DiffKind.MATCH -> withStyle(SpanStyle(color = content)) { append(segment.text) }
                    DiffKind.MISSING -> withStyle(
                        SpanStyle(color = missing, fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline),
                    ) { append(segment.text) }
                    DiffKind.EXTRA -> withStyle(
                        SpanStyle(color = extra, textDecoration = TextDecoration.LineThrough),
                    ) { append(segment.text) }
                }
            }
        },
        style = MaterialTheme.typography.titleMedium,
    )
}

private const val SECONDS_PER_DAY = 86_400L
