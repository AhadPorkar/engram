package com.ahadporkar.engram.feature.decks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.ahadporkar.engram.core.model.Deck
import java.util.Locale
import kotlin.math.roundToInt

/** Languages offered for text-to-speech; names are shown in the user's own language. */
val SupportedLanguages = listOf(
    "en-US", "en-GB", "de-DE", "fr-FR", "es-ES", "it-IT", "pt-BR", "nl-NL",
    "tr-TR", "ru-RU", "pl-PL", "fa-IR", "ar-SA", "zh-CN", "ja-JP", "ko-KR",
)

fun languageName(tag: String): String {
    val locale = Locale.forLanguageTag(tag)
    return locale.getDisplayName(Locale.getDefault()).replaceFirstChar { it.titlecase(Locale.getDefault()) }
}

@Composable
fun DeckEditorDialog(
    initial: Deck?,
    onDismiss: () -> Unit,
    onConfirm: (DeckDraft) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initial?.name.orEmpty()) }
    var description by rememberSaveable { mutableStateOf(initial?.description.orEmpty()) }
    var frontLanguage by rememberSaveable { mutableStateOf(initial?.frontLanguage ?: "de-DE") }
    var backLanguage by rememberSaveable { mutableStateOf(initial?.backLanguage ?: "en-US") }
    var newPerDay by rememberSaveable { mutableFloatStateOf((initial?.newCardsPerDay ?: Deck.DEFAULT_NEW_PER_DAY).toFloat()) }
    var reviewsPerDay by rememberSaveable {
        mutableFloatStateOf((initial?.maxReviewsPerDay ?: Deck.DEFAULT_REVIEWS_PER_DAY).toFloat())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial == null) R.string.new_deck else R.string.edit_deck)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.deck_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.deck_description)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                LanguagePicker(
                    label = stringResource(R.string.deck_front_language),
                    selected = frontLanguage,
                    onSelected = { frontLanguage = it },
                )
                Spacer(Modifier.height(8.dp))
                LanguagePicker(
                    label = stringResource(R.string.deck_back_language),
                    selected = backLanguage,
                    onSelected = { backLanguage = it },
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.deck_new_per_day, newPerDay.roundToInt()),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Slider(value = newPerDay, onValueChange = { newPerDay = it }, valueRange = 0f..50f, steps = 49)
                Text(
                    stringResource(R.string.deck_reviews_per_day, reviewsPerDay.roundToInt()),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Slider(value = reviewsPerDay, onValueChange = { reviewsPerDay = it }, valueRange = 20f..500f, steps = 47)
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onConfirm(
                        DeckDraft(
                            name = name.trim(),
                            description = description.trim(),
                            frontLanguage = frontLanguage,
                            backLanguage = backLanguage,
                            newCardsPerDay = newPerDay.roundToInt(),
                            maxReviewsPerDay = reviewsPerDay.roundToInt(),
                        ),
                    )
                },
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun LanguagePicker(label: String, selected: String, onSelected: (String) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value = languageName(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
        )
        // Transparent layer that opens the menu (a read-only text field swallows clicks).
        Box(modifier = Modifier.matchParentSize().clickable { expanded = true })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SupportedLanguages.forEach { tag ->
                DropdownMenuItem(
                    text = { Text(languageName(tag)) },
                    onClick = {
                        onSelected(tag)
                        expanded = false
                    },
                )
            }
        }
    }
}
