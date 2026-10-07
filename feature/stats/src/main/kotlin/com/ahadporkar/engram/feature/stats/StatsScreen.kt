package com.ahadporkar.engram.feature.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ahadporkar.engram.core.data.repository.StatsOverview
import com.ahadporkar.engram.core.data.repository.WordMemory
import com.ahadporkar.engram.core.designsystem.component.BarChart
import com.ahadporkar.engram.core.designsystem.component.MemoryBar
import com.ahadporkar.engram.core.designsystem.component.SectionHeader
import com.ahadporkar.engram.core.designsystem.component.StackedBar
import com.ahadporkar.engram.core.designsystem.component.StatTile
import com.ahadporkar.engram.core.designsystem.theme.EngramTheme
import com.ahadporkar.engram.core.designsystem.util.percent
import kotlin.math.roundToInt

@Composable
fun StatsRoute(onBack: () -> Unit, viewModel: StatsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    StatsScreen(state = state, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(state: StatsUiState, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stats_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        when (state) {
            StatsUiState.Loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is StatsUiState.Ready -> StatsContent(state.overview, Modifier.padding(padding))
        }
    }
}

@Composable
private fun StatsContent(overview: StatsOverview, modifier: Modifier) {
    val stats = overview.snapshot
    val colors = EngramTheme.colors
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    label = stringResource(R.string.stat_streak),
                    value = stats.currentStreak.toString(),
                    icon = Icons.Filled.LocalFireDepartment,
                    accent = colors.streak,
                    supporting = stringResource(R.string.stat_longest_streak, stats.longestStreak),
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    label = stringResource(R.string.stat_today),
                    value = "${stats.reviewsToday}/${stats.dailyGoal}",
                    icon = Icons.Filled.TaskAlt,
                    supporting = stringResource(R.string.stat_minutes_today, (stats.studyMillisToday / 60_000).toInt()),
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    label = stringResource(R.string.stat_retention),
                    value = percent(stats.trueRetention),
                    icon = Icons.Filled.Verified,
                    accent = colors.good,
                    supporting = pluralStringResource(R.plurals.stat_reviews_sample, stats.retentionSampleSize, stats.retentionSampleSize),
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    label = stringResource(R.string.stat_known_now),
                    value = stats.estimatedKnown.roundToInt().toString(),
                    icon = Icons.Filled.Psychology,
                    accent = MaterialTheme.colorScheme.tertiary,
                    supporting = stringResource(R.string.stat_avg_recall, percent(stats.averageRetrievability)),
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                stringResource(R.string.stat_known_explanation),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        item {
            SectionHeader(stringResource(R.string.section_history))
            BarChart(
                values = stats.reviewsPerDay.map { it.count },
                description = stringResource(R.string.section_history),
                highlightIndex = stats.reviewsPerDay.lastIndex,
            )
            AxisLabels(start = stringResource(R.string.axis_30_days_ago), end = stringResource(R.string.axis_today))
        }

        item {
            SectionHeader(stringResource(R.string.section_forecast))
            BarChart(
                values = stats.forecast.map { it.count },
                description = stringResource(R.string.section_forecast),
                barColor = colors.reviewCards,
                highlightIndex = 0,
            )
            AxisLabels(start = stringResource(R.string.axis_today), end = stringResource(R.string.axis_in_30_days))
        }

        item {
            SectionHeader(stringResource(R.string.section_maturity))
            val counts = stats.counts
            StackedBar(
                segments = listOf(
                    counts.new to colors.newCards,
                    counts.learning to colors.learningCards,
                    counts.young to colors.reviewCards.copy(alpha = 0.55f),
                    counts.mature to colors.reviewCards,
                ),
            )
            Spacer(Modifier.height(12.dp))
            Legend(stringResource(R.string.maturity_new), counts.new, colors.newCards)
            Legend(stringResource(R.string.maturity_learning), counts.learning, colors.learningCards)
            Legend(stringResource(R.string.maturity_young), counts.young, colors.reviewCards.copy(alpha = 0.55f))
            Legend(stringResource(R.string.maturity_mature), counts.mature, colors.reviewCards)
            if (counts.suspended > 0) {
                Legend(stringResource(R.string.maturity_suspended), counts.suspended, MaterialTheme.colorScheme.outline)
            }
        }

        if (overview.weakest.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.section_weakest)) }
            items(overview.weakest, key = { "weak-${it.note.id}" }) { WordMemoryRow(it) }
        }

        if (overview.leeches.isNotEmpty()) {
            item {
                SectionHeader(stringResource(R.string.section_leeches))
                Text(
                    stringResource(R.string.leeches_explanation),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(overview.leeches, key = { "leech-${it.note.id}" }) { WordMemoryRow(it, showLapses = true) }
        }

        if (stats.counts.total == 0) {
            item {
                Row(modifier = Modifier.padding(vertical = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.stats_empty))
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun AxisLabels(start: String, end: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Text(start, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        Text(end, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Legend(label: String, count: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(count.toString(), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun WordMemoryRow(word: WordMemory, showLapses: Boolean = false) {
    ListItem(
        leadingContent = { Icon(Icons.Filled.ViewAgenda, contentDescription = null) },
        headlineContent = { Text(word.note.front, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Column {
                Text(word.note.back, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                MemoryBar(word.retrievability, modifier = Modifier.width(96.dp))
            }
        },
        trailingContent = {
            Text(
                if (showLapses) {
                    pluralStringResource(R.plurals.lapses, word.lapses, word.lapses)
                } else {
                    percent(word.retrievability)
                },
                style = MaterialTheme.typography.labelLarge,
            )
        },
    )
}
