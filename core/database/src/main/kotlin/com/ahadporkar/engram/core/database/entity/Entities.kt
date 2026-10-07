package com.ahadporkar.engram.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/*
 * Storage schema (v1). Times are epoch milliseconds, enums are stored by name.
 *
 * decks 1─* notes 1─* cards 1─* review_logs
 *
 * Replaces the single `word_table(id, word, meaning, synonyms)` of ProjectShaco.
 */

@Entity(tableName = "decks")
data class DeckEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    @ColumnInfo(name = "front_language") val frontLanguage: String,
    @ColumnInfo(name = "back_language") val backLanguage: String,
    @ColumnInfo(name = "new_cards_per_day") val newCardsPerDay: Int,
    @ColumnInfo(name = "max_reviews_per_day") val maxReviewsPerDay: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = DeckEntity::class,
            parentColumns = ["id"],
            childColumns = ["deck_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("deck_id"), Index("front")],
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "deck_id") val deckId: Long,
    val front: String,
    val back: String,
    val synonyms: String,
    val example: String,
    val mnemonic: String,
    /** Space-separated tags. */
    val tags: String,
    val starred: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(
    tableName = "cards",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["note_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = DeckEntity::class,
            parentColumns = ["id"],
            childColumns = ["deck_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["note_id", "direction"], unique = true),
        Index("deck_id"),
        Index(value = ["phase", "due"]),
    ],
)
data class CardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "note_id") val noteId: Long,
    @ColumnInfo(name = "deck_id") val deckId: Long,
    val direction: String,
    val phase: String,
    val step: Int?,
    val stability: Double?,
    val difficulty: Double?,
    val due: Long,
    @ColumnInfo(name = "last_review") val lastReview: Long?,
    val reps: Int,
    val lapses: Int,
    val suspended: Boolean,
    val leech: Boolean,
)

@Entity(
    tableName = "review_logs",
    foreignKeys = [
        ForeignKey(
            entity = CardEntity::class,
            parentColumns = ["id"],
            childColumns = ["card_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("card_id"), Index("reviewed_at")],
)
data class ReviewLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "card_id") val cardId: Long,
    val rating: Int,
    @ColumnInfo(name = "phase_before") val phaseBefore: String,
    @ColumnInfo(name = "reviewed_at") val reviewedAt: Long,
    @ColumnInfo(name = "elapsed_days") val elapsedDays: Long?,
    @ColumnInfo(name = "scheduled_seconds") val scheduledSeconds: Long,
    @ColumnInfo(name = "duration_millis") val durationMillis: Long,
    val exercise: String,
    @ColumnInfo(name = "stability_after") val stabilityAfter: Double,
    @ColumnInfo(name = "difficulty_after") val difficultyAfter: Double,
    @ColumnInfo(name = "retrievability_before") val retrievabilityBefore: Double?,
)

/** A card together with its note. */
data class CardWithNote(
    @Embedded val card: CardEntity,
    @Relation(parentColumn = "note_id", entityColumn = "id")
    val note: NoteEntity,
)

/** A note together with its (one or two) cards. */
data class NoteWithCards(
    @Embedded val note: NoteEntity,
    @Relation(parentColumn = "id", entityColumn = "note_id")
    val cards: List<CardEntity>,
)

/** Deck row plus live counters for the home screen. */
data class DeckWithCounts(
    @Embedded val deck: DeckEntity,
    @ColumnInfo(name = "new_count") val newCount: Int,
    @ColumnInfo(name = "learning_count") val learningCount: Int,
    @ColumnInfo(name = "review_count") val reviewCount: Int,
    @ColumnInfo(name = "note_count") val noteCount: Int,
)

/** How many new cards were introduced / reviews done per deck since a point in time. */
data class DeckDailyCount(
    @ColumnInfo(name = "deck_id") val deckId: Long,
    @ColumnInfo(name = "new_count") val newCount: Int,
    @ColumnInfo(name = "review_count") val reviewCount: Int,
)
