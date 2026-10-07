package com.ahadporkar.engram.core.data.backup

import kotlinx.serialization.Serializable

/**
 * Versioned, human-readable backup file (`*.engram.json`).
 * Replaces the raw SQLite copy to /Downloads of the old app, which needed storage permissions.
 */
@Serializable
data class BackupFile(
    val format: String = FORMAT,
    val version: Int = VERSION,
    val exportedAt: Long,
    val decks: List<DeckDto>,
    val notes: List<NoteDto>,
    val cards: List<CardDto>,
    val reviewLogs: List<ReviewLogDto>,
) {
    companion object {
        const val FORMAT = "engram-backup"
        const val VERSION = 1
    }
}

@Serializable
data class DeckDto(
    val id: Long,
    val name: String,
    val description: String = "",
    val frontLanguage: String = "en-US",
    val backLanguage: String = "en-US",
    val newCardsPerDay: Int = 15,
    val maxReviewsPerDay: Int = 200,
    val createdAt: Long,
)

@Serializable
data class NoteDto(
    val id: Long,
    val deckId: Long,
    val front: String,
    val back: String,
    val synonyms: String = "",
    val example: String = "",
    val mnemonic: String = "",
    val tags: String = "",
    val starred: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class CardDto(
    val id: Long,
    val noteId: Long,
    val deckId: Long,
    val direction: String,
    val phase: String,
    val step: Int? = null,
    val stability: Double? = null,
    val difficulty: Double? = null,
    val due: Long,
    val lastReview: Long? = null,
    val reps: Int = 0,
    val lapses: Int = 0,
    val suspended: Boolean = false,
    val leech: Boolean = false,
)

@Serializable
data class ReviewLogDto(
    val id: Long,
    val cardId: Long,
    val rating: Int,
    val phaseBefore: String,
    val reviewedAt: Long,
    val elapsedDays: Long? = null,
    val scheduledSeconds: Long,
    val durationMillis: Long,
    val exercise: String,
    val stabilityAfter: Double,
    val difficultyAfter: Double,
    val retrievabilityBefore: Double? = null,
)
