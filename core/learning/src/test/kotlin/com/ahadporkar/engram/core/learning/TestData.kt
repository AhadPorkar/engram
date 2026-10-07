package com.ahadporkar.engram.core.learning

import com.ahadporkar.engram.core.model.Card
import com.ahadporkar.engram.core.model.CardDirection
import com.ahadporkar.engram.core.model.Note
import com.ahadporkar.engram.core.model.StudyCard
import com.ahadporkar.engram.core.srs.CardPhase
import com.ahadporkar.engram.core.srs.SchedulingState
import java.time.Duration
import java.time.Instant

object TestData {
    val NOW: Instant = Instant.parse("2026-03-10T09:00:00Z")

    private var nextCardId = 1L

    fun note(
        id: Long,
        front: String = "der Hund",
        back: String = "the dog",
        example: String = "",
        synonyms: String = "",
        starred: Boolean = false,
        createdAt: Instant = NOW.minus(Duration.ofDays(30)).plusSeconds(id),
    ) = Note(
        id = id,
        deckId = 1,
        front = front,
        back = back,
        synonyms = synonyms,
        example = example,
        starred = starred,
        createdAt = createdAt,
        updatedAt = createdAt,
    )

    fun newCard(note: Note, direction: CardDirection = CardDirection.RECOGNITION) = StudyCard(
        card = Card(
            id = nextCardId++,
            noteId = note.id,
            deckId = note.deckId,
            direction = direction,
            scheduling = SchedulingState.newCard(note.createdAt),
        ),
        note = note,
        frontLanguage = "de-DE",
        backLanguage = "en-US",
    )

    fun reviewCard(
        note: Note,
        stability: Double,
        lastReview: Instant,
        due: Instant,
        direction: CardDirection = CardDirection.RECOGNITION,
        lapses: Int = 0,
    ) = StudyCard(
        card = Card(
            id = nextCardId++,
            noteId = note.id,
            deckId = note.deckId,
            direction = direction,
            scheduling = SchedulingState(
                phase = CardPhase.REVIEW,
                stability = stability,
                difficulty = 5.0,
                due = due,
                lastReview = lastReview,
                reps = 3,
                lapses = lapses,
            ),
        ),
        note = note,
        frontLanguage = "de-DE",
        backLanguage = "en-US",
    )

    fun learningCard(note: Note, due: Instant) = StudyCard(
        card = Card(
            id = nextCardId++,
            noteId = note.id,
            deckId = note.deckId,
            direction = CardDirection.RECOGNITION,
            scheduling = SchedulingState(
                phase = CardPhase.LEARNING,
                step = 1,
                stability = 2.3,
                difficulty = 5.0,
                due = due,
                lastReview = due.minus(Duration.ofMinutes(10)),
                reps = 1,
            ),
        ),
        note = note,
    )
}
