package com.ahadporkar.engram.core.model

import java.time.Instant

/**
 * A collection of notes on one topic, e.g. "German A1 – Basics".
 *
 * [frontLanguage] / [backLanguage] are BCP-47 tags (e.g. `de-DE`, `en-US`) used for text-to-speech
 * and speech recognition.
 */
data class Deck(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val frontLanguage: String = "en-US",
    val backLanguage: String = "en-US",
    val newCardsPerDay: Int = DEFAULT_NEW_PER_DAY,
    val maxReviewsPerDay: Int = DEFAULT_REVIEWS_PER_DAY,
    val createdAt: Instant,
) {
    companion object {
        const val DEFAULT_NEW_PER_DAY = 15
        const val DEFAULT_REVIEWS_PER_DAY = 200
    }
}

/** A deck plus the counts shown on the home screen. */
data class DeckSummary(
    val deck: Deck,
    val newCount: Int,
    val learningCount: Int,
    val reviewCount: Int,
    val noteCount: Int,
) {
    val dueCount: Int get() = learningCount + reviewCount
}

/** Today's progress towards the daily goal, shown on the home screen. */
data class DailyProgress(
    val reviewsToday: Int = 0,
    val dailyGoal: Int = 20,
    val streak: Int = 0,
    val dueToday: Int = 0,
) {
    val goalFraction: Float
        get() = if (dailyGoal <= 0) 1f else (reviewsToday.toFloat() / dailyGoal).coerceIn(0f, 1f)

    val goalReached: Boolean get() = reviewsToday >= dailyGoal
}
