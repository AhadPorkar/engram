package com.ahadporkar.engram.feature.settings

import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahadporkar.engram.core.data.backup.BackupRepository
import com.ahadporkar.engram.core.data.backup.RestoreMode
import com.ahadporkar.engram.core.data.backup.importErrorMessage
import com.ahadporkar.engram.core.data.reminder.ReminderScheduler
import com.ahadporkar.engram.core.data.repository.DataStoreSettingsRepository
import com.ahadporkar.engram.core.data.repository.SettingsRepository
import com.ahadporkar.engram.core.model.LeechAction
import com.ahadporkar.engram.core.model.ThemeMode
import com.ahadporkar.engram.core.model.TypingTolerance
import com.ahadporkar.engram.core.model.UserSettings
import com.ahadporkar.engram.core.srs.FsrsParameters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SettingsMessage {
    data object Exported : SettingsMessage
    data class Restored(val decks: Int, val words: Int) : SettingsMessage
    data class Imported(val words: Int) : SettingsMessage
    data object InvalidParameters : SettingsMessage
    data class Failed(@StringRes val message: Int) : SettingsMessage
}

data class SettingsUiState(
    val loading: Boolean = true,
    val settings: UserSettings = UserSettings(),
    val busy: Boolean = false,
    val message: SettingsMessage? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val backupRepository: BackupRepository,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    private val busy = MutableStateFlow(false)
    private val message = MutableStateFlow<SettingsMessage?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(settingsRepository.settings, busy, message) { settings, busy, message ->
        SettingsUiState(loading = false, settings = settings, busy = busy, message = message)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    private fun update(transform: (UserSettings) -> UserSettings) {
        viewModelScope.launch { settingsRepository.update(transform) }
    }

    // Memory model
    fun setDesiredRetention(value: Double) = update { it.copy(desiredRetention = value) }
    fun setLearningSteps(text: String) = update { it.copy(learningStepsMinutes = DataStoreSettingsRepository.parseMinutes(text)) }
    fun setRelearningSteps(text: String) = update { it.copy(relearningStepsMinutes = DataStoreSettingsRepository.parseMinutes(text)) }
    fun setMaximumInterval(text: String) {
        val days = text.trim().toIntOrNull()?.coerceIn(1, 36_500) ?: return
        update { it.copy(maximumIntervalDays = days) }
    }
    fun setFuzz(enabled: Boolean) = update { it.copy(enableFuzz = enabled) }
    fun setLeechThreshold(value: Int) = update { it.copy(leechThreshold = value) }
    fun setLeechAction(action: LeechAction) = update { it.copy(leechAction = action) }
    fun setDayStartHour(hour: Int) = update { it.copy(dayStartHour = hour.coerceIn(0, 23)) }

    /** Accepts 21 FSRS-6 or 19 FSRS-5 weights; blank text restores the defaults. */
    fun setFsrsParameters(text: String) {
        if (text.isBlank()) {
            update { it.copy(fsrsWeights = null) }
            return
        }
        val parsed = FsrsParameters.parse(text)
        if (parsed == null) {
            message.value = SettingsMessage.InvalidParameters
        } else {
            update { it.copy(fsrsWeights = parsed.weights) }
        }
    }

    // Exercises
    fun setTypingTolerance(value: TypingTolerance) = update { it.copy(typingTolerance = value) }
    fun setIgnoreAccents(value: Boolean) = update { it.copy(ignoreAccents = value) }
    fun setSpeakingExercises(value: Boolean) = update { it.copy(speakingExercises = value) }
    fun setAutoPlayAudio(value: Boolean) = update { it.copy(autoPlayAudio = value) }
    fun setCreateReverseCards(value: Boolean) = update { it.copy(createReverseCards = value) }

    // Goals & reminders
    fun setDailyGoal(value: Int) = update { it.copy(dailyGoal = value) }

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(reminderEnabled = enabled) }
            reminderScheduler.sync()
        }
    }

    fun setReminderTime(minuteOfDay: Int) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(reminderMinuteOfDay = minuteOfDay, reminderEnabled = true) }
            reminderScheduler.sync()
        }
    }

    // Appearance
    fun setThemeMode(mode: ThemeMode) = update { it.copy(themeMode = mode) }
    fun setDynamicColor(enabled: Boolean) = update { it.copy(dynamicColor = enabled) }

    // Data
    fun export(uri: Uri) = runOperation {
        backupRepository.export(uri)
        SettingsMessage.Exported
    }

    fun restore(uri: Uri, mode: RestoreMode) = runOperation {
        val summary = backupRepository.restore(uri, mode)
        SettingsMessage.Restored(summary.decks, summary.notes)
    }

    fun importWordList(uri: Uri, fallbackName: String) = runOperation {
        val name = backupRepository.displayName(uri)?.substringBeforeLast('.')?.takeIf { it.isNotBlank() } ?: fallbackName
        val summary = backupRepository.importDelimited(uri, targetDeckId = null, newDeckName = name)
        SettingsMessage.Imported(summary.notes)
    }

    fun importLegacy(uri: Uri, deckName: String) = runOperation {
        val summary = backupRepository.importLegacyShaco(uri, deckName)
        SettingsMessage.Imported(summary.notes)
    }

    fun messageShown() {
        message.value = null
    }

    private fun runOperation(block: suspend () -> SettingsMessage) {
        viewModelScope.launch {
            busy.value = true
            message.value = runCatching { block() }.getOrElse { SettingsMessage.Failed(it.importErrorMessage()) }
            busy.value = false
        }
    }
}
