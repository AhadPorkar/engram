package com.ahadporkar.engram.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ahadporkar.engram.core.data.mapper.enumValueOrDefault
import com.ahadporkar.engram.core.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

interface SettingsRepository {
    val settings: Flow<UserSettings>

    suspend fun current(): UserSettings = settings.first()

    suspend fun update(transform: (UserSettings) -> UserSettings)

    /** `true` once the sample decks were offered on first launch. */
    suspend fun isSampleContentSeeded(): Boolean

    suspend fun markSampleContentSeeded()
}

@Singleton
class DataStoreSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val settings: Flow<UserSettings> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { it.toSettings() }

    override suspend fun update(transform: (UserSettings) -> UserSettings) {
        dataStore.edit { prefs -> prefs.write(transform(prefs.toSettings())) }
    }

    override suspend fun isSampleContentSeeded(): Boolean = dataStore.data.first()[Keys.seeded] == true

    override suspend fun markSampleContentSeeded() {
        dataStore.edit { it[Keys.seeded] = true }
    }

    private object Keys {
        val desiredRetention = doublePreferencesKey("desired_retention")
        val learningSteps = stringPreferencesKey("learning_steps")
        val relearningSteps = stringPreferencesKey("relearning_steps")
        val maximumInterval = intPreferencesKey("maximum_interval_days")
        val fuzz = booleanPreferencesKey("enable_fuzz")
        val fsrsWeights = stringPreferencesKey("fsrs_weights")
        val leechThreshold = intPreferencesKey("leech_threshold")
        val leechAction = stringPreferencesKey("leech_action")
        val dayStartHour = intPreferencesKey("day_start_hour")
        val typingTolerance = stringPreferencesKey("typing_tolerance")
        val ignoreAccents = booleanPreferencesKey("ignore_accents")
        val speaking = booleanPreferencesKey("speaking_exercises")
        val autoPlayAudio = booleanPreferencesKey("auto_play_audio")
        val createReverse = booleanPreferencesKey("create_reverse_cards")
        val dailyGoal = intPreferencesKey("daily_goal")
        val reminderEnabled = booleanPreferencesKey("reminder_enabled")
        val reminderMinute = intPreferencesKey("reminder_minute_of_day")
        val themeMode = stringPreferencesKey("theme_mode")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
        val seeded = booleanPreferencesKey("sample_content_seeded")
    }

    private fun Preferences.toSettings(): UserSettings {
        val defaults = UserSettings()
        return UserSettings(
            desiredRetention = this[Keys.desiredRetention] ?: defaults.desiredRetention,
            learningStepsMinutes = this[Keys.learningSteps]?.let(::parseMinutes) ?: defaults.learningStepsMinutes,
            relearningStepsMinutes = this[Keys.relearningSteps]?.let(::parseMinutes) ?: defaults.relearningStepsMinutes,
            maximumIntervalDays = this[Keys.maximumInterval] ?: defaults.maximumIntervalDays,
            enableFuzz = this[Keys.fuzz] ?: defaults.enableFuzz,
            fsrsWeights = this[Keys.fsrsWeights]
                ?.split(',')
                ?.mapNotNull { it.trim().toDoubleOrNull() }
                ?.takeIf { it.isNotEmpty() },
            leechThreshold = this[Keys.leechThreshold] ?: defaults.leechThreshold,
            leechAction = this[Keys.leechAction]?.let { enumValueOrDefault(it, defaults.leechAction) }
                ?: defaults.leechAction,
            dayStartHour = this[Keys.dayStartHour] ?: defaults.dayStartHour,
            typingTolerance = this[Keys.typingTolerance]?.let { enumValueOrDefault(it, defaults.typingTolerance) }
                ?: defaults.typingTolerance,
            ignoreAccents = this[Keys.ignoreAccents] ?: defaults.ignoreAccents,
            speakingExercises = this[Keys.speaking] ?: defaults.speakingExercises,
            autoPlayAudio = this[Keys.autoPlayAudio] ?: defaults.autoPlayAudio,
            createReverseCards = this[Keys.createReverse] ?: defaults.createReverseCards,
            dailyGoal = this[Keys.dailyGoal] ?: defaults.dailyGoal,
            reminderEnabled = this[Keys.reminderEnabled] ?: defaults.reminderEnabled,
            reminderMinuteOfDay = this[Keys.reminderMinute] ?: defaults.reminderMinuteOfDay,
            themeMode = this[Keys.themeMode]?.let { enumValueOrDefault(it, defaults.themeMode) } ?: defaults.themeMode,
            dynamicColor = this[Keys.dynamicColor] ?: defaults.dynamicColor,
        )
    }

    private fun MutablePreferences.write(settings: UserSettings) {
        this[Keys.desiredRetention] = settings.desiredRetention
        this[Keys.learningSteps] = settings.learningStepsMinutes.joinToString(" ")
        this[Keys.relearningSteps] = settings.relearningStepsMinutes.joinToString(" ")
        this[Keys.maximumInterval] = settings.maximumIntervalDays
        this[Keys.fuzz] = settings.enableFuzz
        val weights = settings.fsrsWeights
        if (weights == null) remove(Keys.fsrsWeights) else this[Keys.fsrsWeights] = weights.joinToString(",")
        this[Keys.leechThreshold] = settings.leechThreshold
        this[Keys.leechAction] = settings.leechAction.name
        this[Keys.dayStartHour] = settings.dayStartHour
        this[Keys.typingTolerance] = settings.typingTolerance.name
        this[Keys.ignoreAccents] = settings.ignoreAccents
        this[Keys.speaking] = settings.speakingExercises
        this[Keys.autoPlayAudio] = settings.autoPlayAudio
        this[Keys.createReverse] = settings.createReverseCards
        this[Keys.dailyGoal] = settings.dailyGoal
        this[Keys.reminderEnabled] = settings.reminderEnabled
        this[Keys.reminderMinute] = settings.reminderMinuteOfDay
        this[Keys.themeMode] = settings.themeMode.name
        this[Keys.dynamicColor] = settings.dynamicColor
    }

    companion object {
        /** "1 10" or "1, 10" → [1, 10]; invalid entries are dropped. */
        fun parseMinutes(text: String): List<Int> =
            text.split(' ', ',', ';').mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..(60 * 24 * 7) }
    }
}
