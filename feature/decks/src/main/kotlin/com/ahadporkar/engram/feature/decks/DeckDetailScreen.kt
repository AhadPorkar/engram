package com.ahadporkar.engram.feature.decks

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ahadporkar.engram.core.data.repository.NoteWithMemory
import com.ahadporkar.engram.core.designsystem.component.CountPill
import com.ahadporkar.engram.core.designsystem.component.EmptyState
import com.ahadporkar.engram.core.designsystem.component.MemoryBar
import com.ahadporkar.engram.core.designsystem.theme.EngramTheme
import com.ahadporkar.engram.core.designsystem.util.percent
import com.ahadporkar.engram.core.model.CramOrder
import com.ahadporkar.engram.core.model.StudyMode
import com.ahadporkar.engram.core.designsystem.R as DesignR

@Composable
fun DeckDetailRoute(
    onBack: () -> Unit,
    onStudy: (deckId: Long, mode: StudyMode, order: CramOrder) -> Unit,
    onAddNote: (deckId: Long) -> Unit,
    onEditNote: (deckId: Long, noteId: Long) -> Unit,
    viewModel: DeckDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.deleted, state.loading) {
        if (!state.loading && state.deleted) onBack()
    }
    DeckDetailScreen(
        state = state,
        onBack = onBack,
        onStudy = { mode, order -> state.deck?.let { onStudy(it.id, mode, order) } },
        onAddNote = { state.deck?.let { onAddNote(it.id) } },
        onEditNote = { noteId -> state.deck?.let { onEditNote(it.id, noteId) } },
        onQueryChange = viewModel::setQuery,
        onToggleStarredOnly = viewModel::toggleStarredOnly,
        onSetStarred = viewModel::setStarred,
        onUpdateDeck = viewModel::updateDeck,
        onDeleteDeck = viewModel::deleteDeck,
        onImport = viewModel::importWordList,
        onMessageShown = viewModel::messageShown,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckDetailScreen(
    state: DeckDetailUiState,
    onBack: () -> Unit,
    onStudy: (StudyMode, CramOrder) -> Unit,
    onAddNote: () -> Unit,
    onEditNote: (Long) -> Unit,
    onQueryChange: (String) -> Unit,
    onToggleStarredOnly: () -> Unit,
    onSetStarred: (Long, Boolean) -> Unit,
    onUpdateDeck: (DeckDraft) -> Unit,
    onDeleteDeck: () -> Unit,
    onImport: (android.net.Uri) -> Unit,
    onMessageShown: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var practiceSheet by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onImport(uri)
    }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        val text = when (message) {
            is DeckMessage.Imported -> context.resources.getQuantityString(R.plurals.import_done, message.count, message.count)
            is DeckMessage.Failed -> context.getString(message.message)
        }
        snackbar.showSnackbar(text)
        onMessageShown()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.deck?.name.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { editing = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit_deck))
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more_options))
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.import_words)) },
                                leadingIcon = { Icon(Icons.Filled.FileUpload, contentDescription = null) },
                                onClick = {
                                    menuOpen = false
                                    importLauncher.launch(arrayOf("text/*", "application/csv", "text/csv", "text/tab-separated-values"))
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.delete_deck)) },
                                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                                onClick = {
                                    menuOpen = false
                                    confirmDelete = true
                                },
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddNote) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_word))
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            item(key = "header") {
                DeckHeader(
                    state = state,
                    onLearn = { onStudy(StudyMode.SMART, CramOrder.SHUFFLE) },
                    onFlashcards = { onStudy(StudyMode.FLASHCARDS, CramOrder.SHUFFLE) },
                    onPractice = { practiceSheet = true },
                )
            }
            item(key = "search") {
                SearchBarRow(
                    query = state.query,
                    starredOnly = state.starredOnly,
                    onQueryChange = onQueryChange,
                    onToggleStarredOnly = onToggleStarredOnly,
                )
            }
            if (state.notes.isEmpty()) {
                item(key = "empty") {
                    if (state.query.isBlank() && !state.starredOnly) {
                        EmptyState(
                            icon = Icons.Filled.Style,
                            title = stringResource(R.string.no_words_title),
                            message = stringResource(R.string.no_words_message),
                        )
                    } else {
                        Text(
                            stringResource(R.string.no_match),
                            modifier = Modifier.padding(24.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            items(state.notes, key = { it.note.id }) { item ->
                NoteRow(
                    item = item,
                    onClick = { onEditNote(item.note.id) },
                    onToggleStar = { onSetStarred(item.note.id, !item.note.starred) },
                )
                HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
            }
        }
    }

    if (editing && state.deck != null) {
        DeckEditorDialog(
            initial = state.deck,
            onDismiss = { editing = false },
            onConfirm = {
                onUpdateDeck(it)
                editing = false
            },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_deck)) },
            text = { Text(stringResource(R.string.delete_deck_confirm, state.deck?.name.orEmpty())) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onDeleteDeck()
                    },
                ) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    if (practiceSheet) {
        ModalBottomSheet(onDismissRequest = { practiceSheet = false }) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(
                    stringResource(R.string.practice_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
                Text(
                    stringResource(R.string.practice_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
                CramOption(Icons.Filled.Shuffle, R.string.cram_shuffle) {
                    practiceSheet = false
                    onStudy(StudyMode.CRAM, CramOrder.SHUFFLE)
                }
                CramOption(Icons.Filled.History, R.string.cram_oldest) {
                    practiceSheet = false
                    onStudy(StudyMode.CRAM, CramOrder.OLDEST_FIRST)
                }
                CramOption(Icons.Filled.Update, R.string.cram_newest) {
                    practiceSheet = false
                    onStudy(StudyMode.CRAM, CramOrder.NEWEST_FIRST)
                }
                CramOption(Icons.Filled.Star, R.string.cram_starred) {
                    practiceSheet = false
                    onStudy(StudyMode.CRAM, CramOrder.STARRED)
                }
                CramOption(Icons.AutoMirrored.Filled.TrendingDown, R.string.cram_weakest) {
                    practiceSheet = false
                    onStudy(StudyMode.CRAM, CramOrder.WEAKEST_FIRST)
                }
            }
        }
    }
}

@Composable
private fun DeckHeader(
    state: DeckDetailUiState,
    onLearn: () -> Unit,
    onFlashcards: () -> Unit,
    onPractice: () -> Unit,
) {
    val colors = EngramTheme.colors
    val summary = state.summary
    Column(modifier = Modifier.padding(16.dp)) {
        state.deck?.description?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
        }
        if (summary != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
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
            Spacer(Modifier.height(16.dp))
        }
        Button(onClick = onLearn, enabled = state.availableToday > 0, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Psychology, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.study_smart))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = onFlashcards, enabled = state.availableToday > 0, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Style, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.study_flashcards))
            }
            OutlinedButton(onClick = onPractice, enabled = state.notes.isNotEmpty() || state.query.isNotEmpty(), modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.FitnessCenter, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.study_practice))
            }
        }
    }
}

@Composable
private fun SearchBarRow(
    query: String,
    starredOnly: Boolean,
    onQueryChange: (String) -> Unit,
    onToggleStarredOnly: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text(stringResource(R.string.search_words)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.clear_search))
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        FilterChip(
            selected = starredOnly,
            onClick = onToggleStarredOnly,
            label = { Text(stringResource(R.string.filter_starred)) },
            leadingIcon = { Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(18.dp)) },
        )
    }
}

@Composable
private fun NoteRow(item: NoteWithMemory, onClick: () -> Unit, onToggleStar: () -> Unit) {
    val note = item.note
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(note.front, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Column {
                Text(note.back, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (note.synonyms.isNotBlank()) {
                    Text(
                        note.synonyms,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.isNew) {
                        Icon(
                            Icons.Filled.NewReleases,
                            contentDescription = null,
                            tint = EngramTheme.colors.newCards,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.word_new), style = MaterialTheme.typography.labelSmall, color = EngramTheme.colors.newCards)
                    } else {
                        MemoryBar(item.retrievability, modifier = Modifier.width(72.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.recall_now, percent(item.retrievability)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (item.isLeech) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.word_leech),
                            style = MaterialTheme.typography.labelSmall,
                            color = EngramTheme.colors.again,
                        )
                    }
                }
            }
        },
        trailingContent = {
            IconButton(onClick = onToggleStar) {
                Icon(
                    if (note.starred) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = stringResource(if (note.starred) R.string.unstar_word else R.string.star_word),
                    tint = if (note.starred) EngramTheme.colors.streak else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

@Composable
private fun CramOption(icon: ImageVector, label: Int, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { Icon(icon, contentDescription = null) },
        headlineContent = { Text(stringResource(label)) },
    )
}
