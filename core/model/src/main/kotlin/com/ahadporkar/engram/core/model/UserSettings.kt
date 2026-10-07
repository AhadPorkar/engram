package com.ahadporkar.engram.core.model

import com.ahadporkar.engram.core.srs.FsrsParameters
import com.ahadporkar.engram.core.srs.SchedulerConfig
import java.time.Duration

/** All user preferences, persisted with DataStore. */
data class UserSettings(
    // Memory model
    val desiredRetention: Double = SchedulerConfig.DEFAULT_RETENTION,
    val learningStepsMinutes: List<Int> = listOf(1, 10),
    val relearningStepsMinutes: List<Int> = listOf(10),
    val maximumIntervalDays: Int = 36_500,
    val enableFuzz: Boolean = true,
    /** Personal FSRS weights (e.g. optimised in Anki). `null` = FSRS-6 defaults. */
    val fsrsWeights: List<Double>? = null,
    val leechThreshold: Int = 8,
    val leechAction: LeechAction = LeechAction.TAG_ONLY,
    /** Hour at which a new study day begins (Anki uses 4 am so late-night sessions count for "today"). */
    val dayStartHour: Int = 4,

    // Exercises
    val typingTolerance: TypingTolerance = TypingTolerance.NORMAL,
    val ignoreAccents: Boolean = true,
    val speakingExercises: Boolean = false,
    val autoPlayAudio: Boolean = true,
    val createReverseCards: Boolean = true,

    // Motivation
    val dailyGoal: Int = 20,
    val reminderEnabled: Boolean = false,
    val reminderMinuteOfDay: Int = 19 * 60,

    // Appearance
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
) {
    fun toSchedulerConfig(): SchedulerConfig = SchedulerConfig(
        desiredRetention = desiredRetention.coerceIn(SchedulerConfig.MIN_RETENTION, SchedulerConfig.MAX_RETENTION),
        learningSteps = learningStepsMinutes.filter { it > 0 }.map { Duration.ofMinutes(it.toLong()) },
        relearningSteps = relearningStepsMinutes.filter { it > 0 }.map { Duration.ofMinutes(it.toLong()) },
        maximumIntervalDays = maximumIntervalDays.coerceAtLeast(1),
        enableFuzz = enableFuzz,
        parameters = fsrsWeights?.let { runCatching { FsrsParameters(it) }.getOrNull() } ?: FsrsParameters.DEFAULT,
    )
}

enum class LeechAction { TAG_ONLY, SUSPEND }

enum class TypingTolerance { STRICT, NORMAL, LENIENT }

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** How a study session is run. */
enum class StudyMode {
    /** Auto-graded exercises whose difficulty grows with memory stability (Memrise / Babbel style). */
    SMART,

    /** Self-graded flashcards with Again / Hard / Good / Easy (Anki style). */
    FLASHCARDS,

    /** Practice without touching the schedule (the old "Start to end", "Jumble" ... modes). */
    CRAM,
}

/** Card order for [StudyMode.CRAM] — the five modes of the original app. */
enum class CramOrder {
    /** Old "JumbleWord" mode. */
    SHUFFLE,

    /** Old "StartToEnd" mode. */
    OLDEST_FIRST,

    /** Old "EndToStart" mode. */
    NEWEST_FIRST,

    /** Old "Saved word" mode. */
    STARRED,

    /** Cards with the lowest predicted recall first. */
    WEAKEST_FIRST,
}
