package com.ahadporkar.engram.feature.decks

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ahadporkar.engram.core.designsystem.component.CountPill
import com.ahadporkar.engram.core.designsystem.component.EmptyState
import com.ahadporkar.engram.core.designsystem.component.ProgressRing
import com.ahadporkar.engram.core.designsystem.theme.EngramTheme
import com.ahadporkar.engram.core.model.DailyProgress
import com.ahadporkar.engram.core.model.DeckSummary
import com.ahadporkar.engram.core.designsystem.R as DesignR

@Composable
fun DecksRoute(
    onOpenDeck: (Long) -> Unit,
    onStudyAll: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: DecksViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DecksScreen(
        state = state,
        onOpenDeck = onOpenDeck,
        onStudyAll = onStudyAll,
        onOpenStats = onOpenStats,
        onOpenSettings = onOpenSettings,
        onCreateDeck = viewModel::createDeck,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecksScreen(
    state: DecksUiState,
    onOpenDeck: (Long) -> Unit,
    onStudyAll: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
    onCreateDeck: (DeckDraft) -> Unit,
) {
    var showCreate by rememberSaveable { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.decks_title)) },
                actions = {
                    IconButton(onClick = onOpenStats) {
                        Icon(Icons.Filled.BarChart, contentDescription = stringResource(R.string.statistics))
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreate = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.new_deck)) },
            )
        },
    ) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "today") {
                TodayCard(progress = state.progress, available = state.availableToday, onStudyAll = onStudyAll)
            }
            if (state.decks.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        icon = Icons.AutoMirrored.Filled.LibraryBooks,
                        title = stringResource(R.string.empty_decks_title),
                        message = stringResource(R.string.empty_decks_message),
                    )
                }
            }
            items(state.decks, key = { it.deck.id }) { summary ->
                DeckCard(summary = summary, onClick = { onOpenDeck(summary.deck.id) })
            }
        }
    }

    if (showCreate) {
        DeckEditorDialog(
            initial = null,
            onDismiss = { showCreate = false },
            onConfirm = {
                onCreateDeck(it)
                showCreate = false
            },
        )
    }
}

@Composable
private fun TodayCard(progress: DailyProgress, available: Int, onStudyAll: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(
                progress = progress.goalFraction,
                size = 84.dp,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
            ) {
                Text(
                    text = "${progress.reviewsToday}/${progress.dailyGoal}",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.today_title), style = MaterialTheme.typography.titleLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.LocalFireDepartment,
                        contentDescription = null,
                        tint = EngramTheme.colors.streak,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        pluralStringResource(R.plurals.streak_days, progress.streak, progress.streak),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Spacer(Modifier.height(12.dp))
                if (available > 0) {
                    Button(onClick = onStudyAll) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(pluralStringResource(R.plurals.start_session, available, available))
                    }
                } else {
                    Text(stringResource(R.string.nothing_due), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun DeckCard(summary: DeckSummary, onClick: () -> Unit) {
    val colors = EngramTheme.colors
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(summary.deck.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (summary.deck.description.isNotBlank()) {
                Text(
                    summary.deck.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CountPill(summary.newCount, colors.newCards, stringResource(DesignR.string.new_label))
                CountPill(summary.learningCount, colors.learningCards, stringResource(DesignR.string.learning_label))
                CountPill(summary.reviewCount, colors.reviewCards, stringResource(DesignR.string.review_label))
                Spacer(Modifier.weight(1f))
                Text(
                    pluralStringResource(R.plurals.word_count, summary.noteCount, summary.noteCount),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
