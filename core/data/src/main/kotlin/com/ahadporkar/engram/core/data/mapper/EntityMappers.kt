package com.ahadporkar.engram.core.data.mapper

import com.ahadporkar.engram.core.database.entity.CardEntity
import com.ahadporkar.engram.core.database.entity.CardWithNote
import com.ahadporkar.engram.core.database.entity.DeckEntity
import com.ahadporkar.engram.core.database.entity.NoteEntity
import com.ahadporkar.engram.core.database.entity.ReviewLogEntity
import com.ahadporkar.engram.core.model.Card
import com.ahadporkar.engram.core.model.CardDirection
import com.ahadporkar.engram.core.model.Deck
import com.ahadporkar.engram.core.model.ExerciseType
import com.ahadporkar.engram.core.model.Note
import com.ahadporkar.engram.core.model.ReviewLog
import com.ahadporkar.engram.core.model.StudyCard
import com.ahadporkar.engram.core.srs.CardPhase
import com.ahadporkar.engram.core.srs.Rating
import com.ahadporkar.engram.core.srs.SchedulingState
import java.time.Instant

fun DeckEntity.toModel() = Deck(
    id = id,
    name = name,
    description = description,
    frontLanguage = frontLanguage,
    backLanguage = backLanguage,
    newCardsPerDay = newCardsPerDay,
    maxReviewsPerDay = maxReviewsPerDay,
    createdAt = Instant.ofEpochMilli(createdAt),
)

fun Deck.toEntity() = DeckEntity(
    id = id,
    name = name.trim(),
    description = description.trim(),
    frontLanguage = frontLanguage,
    backLanguage = backLanguage,
    newCardsPerDay = newCardsPerDay,
    maxReviewsPerDay = maxReviewsPerDay,
    createdAt = createdAt.toEpochMilli(),
)

fun NoteEntity.toModel() = Note(
    id = id,
    deckId = deckId,
    front = front,
    back = back,
    synonyms = synonyms,
    example = example,
    mnemonic = mnemonic,
    tags = tags.split(' ').filter { it.isNotBlank() }.toSet(),
    starred = starred,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
)

fun Note.toEntity() = NoteEntity(
    id = id,
    deckId = deckId,
    front = front.trim(),
    back = back.trim(),
    synonyms = synonyms.trim(),
    example = example.trim(),
    mnemonic = mnemonic.trim(),
    tags = tags.map { it.trim().replace(' ', '_') }.filter { it.isNotEmpty() }.sorted().joinToString(" "),
    starred = starred,
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli(),
)

fun CardEntity.toModel() = Card(
    id = id,
    noteId = noteId,
    deckId = deckId,
    direction = enumValueOrDefault(direction, CardDirection.RECOGNITION),
    scheduling = SchedulingState(
        phase = enumValueOrDefault(phase, CardPhase.NEW),
        step = step,
        stability = stability,
        difficulty = difficulty,
        due = Instant.ofEpochMilli(due),
        lastReview = lastReview?.let(Instant::ofEpochMilli),
        reps = reps,
        lapses = lapses,
    ),
    suspended = suspended,
    leech = leech,
)

fun Card.toEntity() = CardEntity(
    id = id,
    noteId = noteId,
    deckId = deckId,
    direction = direction.name,
    phase = scheduling.phase.name,
    step = scheduling.step,
    stability = scheduling.stability,
    difficulty = scheduling.difficulty,
    due = scheduling.due.toEpochMilli(),
    lastReview = scheduling.lastReview?.toEpochMilli(),
    reps = scheduling.reps,
    lapses = scheduling.lapses,
    suspended = suspended,
    leech = leech,
)

fun ReviewLogEntity.toModel() = ReviewLog(
    id = id,
    cardId = cardId,
    rating = runCatching { Rating.fromValue(rating) }.getOrDefault(Rating.GOOD),
    phaseBefore = enumValueOrDefault(phaseBefore, CardPhase.REVIEW),
    reviewedAt = Instant.ofEpochMilli(reviewedAt),
    elapsedDays = elapsedDays,
    scheduledSeconds = scheduledSeconds,
    durationMillis = durationMillis,
    exercise = enumValueOrDefault(exercise, ExerciseType.FLASHCARD),
    stabilityAfter = stabilityAfter,
    difficultyAfter = difficultyAfter,
    retrievabilityBefore = retrievabilityBefore,
)

fun ReviewLog.toEntity() = ReviewLogEntity(
    id = id,
    cardId = cardId,
    rating = rating.value,
    phaseBefore = phaseBefore.name,
    reviewedAt = reviewedAt.toEpochMilli(),
    elapsedDays = elapsedDays,
    scheduledSeconds = scheduledSeconds,
    durationMillis = durationMillis,
    exercise = exercise.name,
    stabilityAfter = stabilityAfter,
    difficultyAfter = difficultyAfter,
    retrievabilityBefore = retrievabilityBefore,
)

fun CardWithNote.toStudyCard(deck: Deck?) = StudyCard(
    card = card.toModel(),
    note = note.toModel(),
    frontLanguage = deck?.frontLanguage ?: "en-US",
    backLanguage = deck?.backLanguage ?: "en-US",
)

inline fun <reified T : Enum<T>> enumValueOrDefault(name: String, default: T): T =
    enumValues<T>().firstOrNull { it.name == name } ?: default
