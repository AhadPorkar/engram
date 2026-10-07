package com.ahadporkar.engram.feature.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ahadporkar.engram.core.data.backup.RestoreMode
import com.ahadporkar.engram.core.designsystem.component.SectionHeader
import com.ahadporkar.engram.core.model.LeechAction
import com.ahadporkar.engram.core.model.ThemeMode
import com.ahadporkar.engram.core.model.TypingTolerance
import com.ahadporkar.engram.core.model.UserSettings
import com.ahadporkar.engram.core.srs.FsrsParameters
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt

@Composable
fun SettingsRoute(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(state = state, viewModel = viewModel, onBack = onBack)
}

private enum class TextDialog { LEARNING_STEPS, RELEARNING_STEPS, MAX_INTERVAL, FSRS_PARAMETERS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(state: SettingsUiState, viewModel: SettingsViewModel, onBack: () -> Unit) {
    val settings = state.settings
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val snackbar = remember { SnackbarHostState() }

    var textDialog by rememberSaveable { mutableStateOf<TextDialog?>(null) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var pendingRestore by rememberSaveable { mutableStateOf<Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) viewModel.export(uri)
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        pendingRestore = uri
    }
    val fallbackDeckName = stringResource(R.string.imported_deck_name)
    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.importWordList(uri, fallbackDeckName)
    }
    val legacyDeckName = stringResource(R.string.legacy_deck_name)
    val legacyLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.importLegacy(uri, legacyDeckName)
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.setReminderEnabled(granted)
    }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbar.showSnackbar(message.text(context))
        viewModel.messageShown()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            if (state.busy) {
                item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            }

            // --- Memory model -------------------------------------------------------------------
            item { Header(R.string.section_memory) }
            item {
                SliderSetting(
                    title = stringResource(R.string.desired_retention),
                    valueLabel = { stringResource(R.string.desired_retention_value, (it * 100).roundToInt()) },
                    description = stringResource(R.string.desired_retention_hint),
                    value = settings.desiredRetention.toFloat(),
                    range = 0.75f..0.97f,
                    steps = 21,
                    onCommit = { viewModel.setDesiredRetention((it * 100).roundToInt() / 100.0) },
                )
            }
            item {
                ClickSetting(
                    title = stringResource(R.string.learning_steps),
                    subtitle = settings.learningStepsMinutes.joinToString(" ") { "${it}m" }.ifEmpty { "–" },
                    onClick = { textDialog = TextDialog.LEARNING_STEPS },
                )
            }
            item {
                ClickSetting(
                    title = stringResource(R.string.relearning_steps),
                    subtitle = settings.relearningStepsMinutes.joinToString(" ") { "${it}m" }.ifEmpty { "–" },
                    onClick = { textDialog = TextDialog.RELEARNING_STEPS },
                )
            }
            item {
                ClickSetting(
                    title = stringResource(R.string.maximum_interval),
                    subtitle = stringResource(R.string.maximum_interval_value, settings.maximumIntervalDays),
                    onClick = { textDialog = TextDialog.MAX_INTERVAL },
                )
            }
            item {
                SwitchSetting(
                    title = stringResource(R.string.fuzz),
                    subtitle = stringResource(R.string.fuzz_hint),
                    checked = settings.enableFuzz,
                    onChange = viewModel::setFuzz,
                )
            }
            item {
                ClickSetting(
                    title = stringResource(R.string.fsrs_parameters),
                    subtitle = stringResource(
                        if (settings.fsrsWeights == null) R.string.fsrs_parameters_default else R.string.fsrs_parameters_custom,
                    ),
                    icon = { Icon(Icons.Filled.Science, contentDescription = null) },
                    onClick = { textDialog = TextDialog.FSRS_PARAMETERS },
                )
            }
            item {
                SliderSetting(
                    title = stringResource(R.string.leech_threshold),
                    valueLabel = { stringResource(R.string.leech_threshold_value, it.roundToInt()) },
                    description = stringResource(R.string.leech_threshold_hint),
                    value = settings.leechThreshold.toFloat(),
                    range = 4f..16f,
                    steps = 11,
                    onCommit = { viewModel.setLeechThreshold(it.roundToInt()) },
                )
            }
            item {
                SegmentedSetting(
                    title = stringResource(R.string.leech_action),
                    options = LeechAction.entries,
                    selected = settings.leechAction,
                    label = {
                        stringResource(if (it == LeechAction.TAG_ONLY) R.string.leech_action_tag else R.string.leech_action_suspend)
                    },
                    onSelect = viewModel::setLeechAction,
                )
            }

            // --- Exercises ----------------------------------------------------------------------
            item { Header(R.string.section_exercises) }
            item {
                SegmentedSetting(
                    title = stringResource(R.string.typing_tolerance),
                    options = TypingTolerance.entries,
                    selected = settings.typingTolerance,
                    label = {
                        stringResource(
                            when (it) {
                                TypingTolerance.STRICT -> R.string.tolerance_strict
                                TypingTolerance.NORMAL -> R.string.tolerance_normal
                                TypingTolerance.LENIENT -> R.string.tolerance_lenient
                            },
                        )
                    },
                    onSelect = viewModel::setTypingTolerance,
                )
            }
            item {
                SwitchSetting(
                    title = stringResource(R.string.ignore_accents),
                    subtitle = stringResource(R.string.ignore_accents_hint),
                    checked = settings.ignoreAccents,
                    onChange = viewModel::setIgnoreAccents,
                )
            }
            item {
                SwitchSetting(
                    title = stringResource(R.string.speaking_exercises),
                    subtitle = stringResource(R.string.speaking_exercises_hint),
                    checked = settings.speakingExercises,
                    onChange = viewModel::setSpeakingExercises,
                )
            }
            item {
                SwitchSetting(
                    title = stringResource(R.string.auto_play_audio),
                    subtitle = stringResource(R.string.auto_play_audio_hint),
                    checked = settings.autoPlayAudio,
                    onChange = viewModel::setAutoPlayAudio,
                )
            }
            item {
                SwitchSetting(
                    title = stringResource(R.string.create_reverse_cards),
                    subtitle = stringResource(R.string.create_reverse_cards_hint),
                    checked = settings.createReverseCards,
                    onChange = viewModel::setCreateReverseCards,
                )
            }

            // --- Goals & reminders --------------------------------------------------------------
            item { Header(R.string.section_goals) }
            item {
                SliderSetting(
                    title = stringResource(R.string.daily_goal),
                    valueLabel = { stringResource(R.string.daily_goal_value, it.roundToInt()) },
                    description = null,
                    value = settings.dailyGoal.toFloat(),
                    range = 5f..100f,
                    steps = 18,
                    onCommit = { viewModel.setDailyGoal(it.roundToInt()) },
                )
            }
            item {
                SwitchSetting(
                    title = stringResource(R.string.reminder),
                    subtitle = stringResource(R.string.reminder_hint),
                    checked = settings.reminderEnabled,
                    onChange = { enabled ->
                        if (enabled && needsNotificationPermission(context)) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.setReminderEnabled(enabled)
                        }
                    },
                )
            }
            item {
                ClickSetting(
                    title = stringResource(R.string.reminder_time),
                    subtitle = formatTime(settings.reminderMinuteOfDay),
                    onClick = { showTimePicker = true },
                )
            }
            item {
                SliderSetting(
                    title = stringResource(R.string.day_start),
                    valueLabel = { formatTime(it.roundToInt() * 60) },
                    description = stringResource(R.string.day_start_hint),
                    value = settings.dayStartHour.toFloat(),
                    range = 0f..12f,
                    steps = 11,
                    onCommit = { viewModel.setDayStartHour(it.roundToInt()) },
                )
            }

            // --- Appearance ---------------------------------------------------------------------
            item { Header(R.string.section_appearance) }
            item {
                SegmentedSetting(
                    title = stringResource(R.string.theme),
                    options = ThemeMode.entries,
                    selected = settings.themeMode,
                    label = {
                        stringResource(
                            when (it) {
                                ThemeMode.SYSTEM -> R.string.theme_system
                                ThemeMode.LIGHT -> R.string.theme_light
                                ThemeMode.DARK -> R.string.theme_dark
                            },
                        )
                    },
                    onSelect = viewModel::setThemeMode,
                )
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                item {
                    SwitchSetting(
                        title = stringResource(R.string.dynamic_color),
                        subtitle = stringResource(R.string.dynamic_color_hint),
                        checked = settings.dynamicColor,
                        onChange = viewModel::setDynamicColor,
                    )
                }
            }

            // --- Data ---------------------------------------------------------------------------
            item { Header(R.string.section_data) }
            item {
                ClickSetting(
                    title = stringResource(R.string.export_backup),
                    subtitle = stringResource(R.string.export_backup_hint),
                    icon = { Icon(Icons.Filled.Backup, contentDescription = null) },
                    onClick = { exportLauncher.launch("engram-backup-${LocalDate.now()}.json") },
                )
            }
            item {
                ClickSetting(
                    title = stringResource(R.string.restore_backup),
                    subtitle = stringResource(R.string.restore_backup_hint),
                    icon = { Icon(Icons.Filled.Restore, contentDescription = null) },
                    onClick = { restoreLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) },
                )
            }
            item {
                ClickSetting(
                    title = stringResource(R.string.import_word_list),
                    subtitle = stringResource(R.string.import_word_list_hint),
                    icon = { Icon(Icons.Filled.FileUpload, contentDescription = null) },
                    onClick = { csvLauncher.launch(arrayOf("text/*", "text/csv", "text/tab-separated-values", "application/csv")) },
                )
            }
            item {
                ClickSetting(
                    title = stringResource(R.string.import_legacy),
                    subtitle = stringResource(R.string.import_legacy_hint),
                    icon = { Icon(Icons.Filled.History, contentDescription = null) },
                    onClick = { legacyLauncher.launch(arrayOf("*/*")) },
                )
            }

            // --- About --------------------------------------------------------------------------
            item { Header(R.string.section_about) }
            item {
                ClickSetting(
                    title = stringResource(R.string.about_version, appVersion(context)),
                    subtitle = stringResource(R.string.about_algorithm),
                    icon = { Icon(Icons.Filled.Info, contentDescription = null) },
                    onClick = {},
                )
            }
            item {
                ClickSetting(
                    title = stringResource(R.string.about_source),
                    subtitle = SOURCE_URL,
                    icon = { Icon(Icons.Filled.Code, contentDescription = null) },
                    onClick = { uriHandler.openUri(SOURCE_URL) },
                )
            }
        }
    }

    textDialog?.let { dialog ->
        val (title, initial, hint) = when (dialog) {
            TextDialog.LEARNING_STEPS -> Triple(
                stringResource(R.string.learning_steps),
                settings.learningStepsMinutes.joinToString(" "),
                stringResource(R.string.steps_hint),
            )
            TextDialog.RELEARNING_STEPS -> Triple(
                stringResource(R.string.relearning_steps),
                settings.relearningStepsMinutes.joinToString(" "),
                stringResource(R.string.steps_hint),
            )
            TextDialog.MAX_INTERVAL -> Triple(
                stringResource(R.string.maximum_interval),
                settings.maximumIntervalDays.toString(),
                stringResource(R.string.maximum_interval_hint),
            )
            TextDialog.FSRS_PARAMETERS -> Triple(
                stringResource(R.string.fsrs_parameters),
                (settings.fsrsWeights ?: FsrsParameters.DEFAULT_WEIGHTS).joinToString(", "),
                stringResource(R.string.fsrs_parameters_hint),
            )
        }
        TextInputDialog(
            title = title,
            initial = initial,
            hint = hint,
            monospace = dialog == TextDialog.FSRS_PARAMETERS,
            onDismiss = { textDialog = null },
            onConfirm = { text ->
                when (dialog) {
                    TextDialog.LEARNING_STEPS -> viewModel.setLearningSteps(text)
                    TextDialog.RELEARNING_STEPS -> viewModel.setRelearningSteps(text)
                    TextDialog.MAX_INTERVAL -> viewModel.setMaximumInterval(text)
                    TextDialog.FSRS_PARAMETERS -> viewModel.setFsrsParameters(text)
                }
                textDialog = null
            },
            extraAction = if (dialog == TextDialog.FSRS_PARAMETERS) {
                {
                    TextButton(
                        onClick = {
                            viewModel.setFsrsParameters("")
                            textDialog = null
                        },
                    ) { Text(stringResource(R.string.reset_defaults)) }
                }
            } else {
                null
            },
        )
    }

    if (showTimePicker) {
        ReminderTimeDialog(
            minuteOfDay = settings.reminderMinuteOfDay,
            onDismiss = { showTimePicker = false },
            onConfirm = {
                showTimePicker = false
                if (needsNotificationPermission(context)) {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                viewModel.setReminderTime(it)
            },
        )
    }

    pendingRestore?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text(stringResource(R.string.restore_backup)) },
            text = { Text(stringResource(R.string.restore_mode_question)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.restore(uri, RestoreMode.REPLACE)
                        pendingRestore = null
                    },
                ) { Text(stringResource(R.string.restore_replace), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.restore(uri, RestoreMode.MERGE)
                        pendingRestore = null
                    },
                ) { Text(stringResource(R.string.restore_merge)) }
            },
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Rows
// ---------------------------------------------------------------------------------------------

@Composable
private fun Header(title: Int) {
    SectionHeader(stringResource(title), modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun SwitchSetting(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        modifier = Modifier.clickable { onChange(!checked) },
        headlineContent = { Text(title) },
        supportingContent = if (subtitle != null) {
            { Text(subtitle) }
        } else {
            null
        },
        trailingContent = { Switch(checked = checked, onCheckedChange = onChange) },
    )
}

@Composable
private fun ClickSetting(
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    icon: (@Composable () -> Unit)? = null,
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = icon,
        headlineContent = { Text(title) },
        supportingContent = if (subtitle != null) {
            { Text(subtitle) }
        } else {
            null
        },
    )
}

@Composable
private fun SliderSetting(
    title: String,
    valueLabel: @Composable (Float) -> String,
    description: String?,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onCommit: (Float) -> Unit,
) {
    var current by remember(value) { mutableFloatStateOf(value.coerceIn(range.start, range.endInclusive)) }
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(valueLabel(current), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Slider(
            value = current,
            onValueChange = { current = it },
            onValueChangeFinished = { onCommit(current) },
            valueRange = range,
            steps = steps,
        )
        if (description != null) {
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> SegmentedSetting(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 8.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                ) { Text(label(option)) }
            }
        }
    }
}

@Composable
private fun TextInputDialog(
    title: String,
    initial: String,
    hint: String,
    monospace: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    extraAction: (@Composable () -> Unit)?,
) {
    var text by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    textStyle = if (monospace) {
                        MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                    } else {
                        MaterialTheme.typography.bodyLarge
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
                extraAction?.invoke()
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text(stringResource(R.string.ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimeDialog(minuteOfDay: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val context = LocalContext.current
    val pickerState = rememberTimePickerState(
        initialHour = minuteOfDay / 60,
        initialMinute = minuteOfDay % 60,
        is24Hour = android.text.format.DateFormat.is24HourFormat(context),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reminder_time)) },
        text = { TimePicker(state = pickerState) },
        confirmButton = {
            TextButton(onClick = { onConfirm(pickerState.hour * 60 + pickerState.minute) }) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

// ---------------------------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------------------------

private const val SOURCE_URL = "https://github.com/AhadPorkar/engram"

private fun needsNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

private fun formatTime(minuteOfDay: Int): String =
    LocalTime.of((minuteOfDay / 60).coerceIn(0, 23), minuteOfDay % 60)
        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))

private fun appVersion(context: Context): String = runCatching {
    val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(context.packageName, 0)
    }
    info.versionName.orEmpty()
}.getOrDefault("")

private fun SettingsMessage.text(context: Context): String = when (this) {
    SettingsMessage.Exported -> context.getString(R.string.message_exported)
    is SettingsMessage.Restored -> context.getString(R.string.message_restored, decks, words)
    is SettingsMessage.Imported -> context.resources.getQuantityString(R.plurals.message_imported, words, words)
    SettingsMessage.InvalidParameters -> context.getString(R.string.message_invalid_parameters)
    is SettingsMessage.Failed -> context.getString(message)
}
