package com.ahadporkar.engram.feature.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ahadporkar.engram.core.designsystem.component.ProgressRing
import com.ahadporkar.engram.core.designsystem.component.StatTile
import com.ahadporkar.engram.core.designsystem.theme.EngramTheme
import com.ahadporkar.engram.core.designsystem.util.percent
import com.ahadporkar.engram.core.designsystem.util.shortDuration

@Composable
fun SessionFinished(
    state: StudyUiState.Finished,
    onDone: () -> Unit,
    onCheckAgain: () -> Unit,
) {
    val summary = state.summary
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ProgressRing(
            progress = (summary.accuracy ?: 1.0).toFloat(),
            size = 120.dp,
            strokeWidth = 10.dp,
            color = EngramTheme.colors.good,
        ) {
            Icon(
                Icons.Filled.EmojiEvents,
                contentDescription = null,
                tint = EngramTheme.colors.streak,
                modifier = Modifier.size(48.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.session_complete), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatTile(
                label = stringResource(R.string.summary_answered),
                value = summary.answered.toString(),
                icon = Icons.Filled.TaskAlt,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = stringResource(R.string.summary_accuracy),
                value = percent(summary.accuracy),
                icon = Icons.Filled.Percent,
                accent = EngramTheme.colors.good,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatTile(
                label = stringResource(R.string.summary_new_words),
                value = summary.newIntroduced.toString(),
                icon = Icons.Filled.AutoAwesome,
                accent = EngramTheme.colors.newCards,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = stringResource(R.string.summary_time),
                value = shortDuration(summary.studyMillis),
                icon = Icons.Filled.Timer,
                accent = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f),
            )
        }

        if (summary.leeches > 0) {
            Spacer(Modifier.height(12.dp))
            Text(
                pluralStringResource(R.plurals.summary_leeches, summary.leeches, summary.leeches),
                style = MaterialTheme.typography.bodyMedium,
                color = EngramTheme.colors.again,
                textAlign = TextAlign.Center,
            )
        }

        if (state.laterLearning > 0) {
            Spacer(Modifier.height(20.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        pluralStringResource(R.plurals.learning_later, state.laterLearning, state.laterLearning),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onCheckAgain) {
                        Icon(Icons.Filled.Replay, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(stringResource(R.string.check_again), modifier = Modifier.padding(start = 6.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Text(stringResource(R.string.done))
        }
    }
}
