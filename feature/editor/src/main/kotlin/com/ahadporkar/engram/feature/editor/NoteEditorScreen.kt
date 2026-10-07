package com.ahadporkar.engram.feature.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun NoteEditorRoute(
    onClose: () -> Unit,
    viewModel: NoteEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.finished) {
        if (state.finished) onClose()
    }
    NoteEditorScreen(
        state = state,
        onClose = onClose,
        onFrontChange = viewModel::onFrontChange,
        onBackChange = viewModel::onBackChange,
        onSynonymsChange = viewModel::onSynonymsChange,
        onExampleChange = viewModel::onExampleChange,
        onMnemonicChange = viewModel::onMnemonicChange,
        onTagsChange = viewModel::onTagsChange,
        onCreateReverseChange = viewModel::onCreateReverseChange,
        onSpeak = viewModel::speakFront,
        onSave = viewModel::save,
        onDelete = viewModel::delete,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    state: NoteEditorUiState,
    onClose: () -> Unit,
    onFrontChange: (String) -> Unit,
    onBackChange: (String) -> Unit,
    onSynonymsChange: (String) -> Unit,
    onExampleChange: (String) -> Unit,
    onMnemonicChange: (String) -> Unit,
    onTagsChange: (String) -> Unit,
    onCreateReverseChange: (Boolean) -> Unit,
    onSpeak: () -> Unit,
    onSave: (addAnother: Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(state.addedInRow) {
        if (state.addedInRow > 0) {
            snackbar.showSnackbar(
                context.resources.getQuantityString(R.plurals.words_added, state.addedInRow, state.addedInRow),
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(if (state.isNew) R.string.add_word_title else R.string.edit_word_title))
                        if (state.deckName.isNotBlank()) {
                            Text(state.deckName, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close))
                    }
                },
                actions = {
                    if (!state.isNew) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete_word))
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.front,
                onValueChange = onFrontChange,
                label = { Text(stringResource(R.string.field_front)) },
                supportingText = {
                    if (state.duplicate) {
                        Text(stringResource(R.string.duplicate_warning), color = MaterialTheme.colorScheme.error)
                    } else {
                        Text(stringResource(R.string.field_front_hint))
                    }
                },
                trailingIcon = {
                    IconButton(onClick = onSpeak, enabled = state.front.isNotBlank()) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = stringResource(R.string.pronounce))
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.back,
                onValueChange = onBackChange,
                label = { Text(stringResource(R.string.field_back)) },
                supportingText = { Text(stringResource(R.string.field_back_hint)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.synonyms,
                onValueChange = onSynonymsChange,
                label = { Text(stringResource(R.string.field_synonyms)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.example,
                onValueChange = onExampleChange,
                label = { Text(stringResource(R.string.field_example)) },
                supportingText = { Text(stringResource(R.string.field_example_hint)) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.mnemonic,
                onValueChange = onMnemonicChange,
                label = { Text(stringResource(R.string.field_mnemonic)) },
                supportingText = { Text(stringResource(R.string.field_mnemonic_hint)) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.tags,
                onValueChange = onTagsChange,
                label = { Text(stringResource(R.string.field_tags)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.create_reverse)) },
                supportingContent = { Text(stringResource(R.string.create_reverse_hint)) },
                trailingContent = { Switch(checked = state.createReverse, onCheckedChange = onCreateReverseChange) },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                if (state.isNew) {
                    OutlinedButton(onClick = { onSave(true) }, enabled = state.canSave, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.save_and_add))
                    }
                }
                Button(onClick = { onSave(false) }, enabled = state.canSave, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.save))
                }
            }
            if (state.addedInRow > 0) {
                Text(
                    pluralStringResource(R.plurals.words_added, state.addedInRow, state.addedInRow),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_word)) },
            text = { Text(stringResource(R.string.delete_word_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onDelete()
                    },
                ) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
