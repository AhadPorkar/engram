package com.ahadporkar.engram.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ahadporkar.engram.core.database.entity.CardEntity
import com.ahadporkar.engram.core.database.entity.DeckEntity
import com.ahadporkar.engram.core.database.entity.NoteEntity
import com.ahadporkar.engram.core.database.entity.ReviewLogEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class EngramDatabaseTest {

    private lateinit var db: EngramDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, EngramDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() = db.close()

    private suspend fun seed(): Long {
        val deckId = db.deckDao().insert(
            DeckEntity(
                name = "German",
                description = "",
                frontLanguage = "de-DE",
                backLanguage = "en-US",
                newCardsPerDay = 10,
                maxReviewsPerDay = 100,
                createdAt = 0,
            ),
        )
        listOf("der Hund" to "the dog", "die Katze" to "the cat", "das Haus" to "the house")
            .forEachIndexed { index, (front, back) ->
                val noteId = db.noteDao().insert(
                    NoteEntity(
                        deckId = deckId, front = front, back = back, synonyms = "", example = "",
                        mnemonic = "", tags = "", starred = index == 0, createdAt = index.toLong(), updatedAt = 0,
                    ),
                )
                db.cardDao().insert(card(noteId, deckId, phase = if (index == 2) "REVIEW" else "NEW", due = 500))
            }
        return deckId
    }

    private fun card(noteId: Long, deckId: Long, phase: String, due: Long) = CardEntity(
        noteId = noteId, deckId = deckId, direction = "RECOGNITION", phase = phase, step = null,
        stability = if (phase == "NEW") null else 3.0, difficulty = if (phase == "NEW") null else 5.0,
        due = due, lastReview = null, reps = 0, lapses = 0, suspended = false, leech = false,
    )

    @Test
    fun deckCountsSplitNewAndDue() = runTest {
        seed()
        val decks = db.deckDao().observeDecksWithCounts(cutoff = 1_000).first()
        assertEquals(1, decks.size)
        assertEquals(2, decks[0].newCount)
        assertEquals(1, decks[0].reviewCount)
        assertEquals(3, decks[0].noteCount)

        val beforeDue = db.deckDao().observeDecksWithCounts(cutoff = 100).first()
        assertEquals(0, beforeDue[0].reviewCount)
    }

    @Test
    fun searchAndStarFilterWork() = runTest {
        val deckId = seed()
        assertEquals(1, db.noteDao().observeNotesWithCards(deckId, "Katze", false).first().size)
        assertEquals(1, db.noteDao().observeNotesWithCards(deckId, "", true).first().size)
        assertEquals(3, db.noteDao().observeNotesWithCards(deckId, "", false).first().size)
    }

    @Test
    fun deletingADeckCascadesToCardsAndLogs() = runTest {
        val deckId = seed()
        val cardId = db.cardDao().getAll().first().id
        db.reviewLogDao().insert(
            ReviewLogEntity(
                cardId = cardId, rating = 3, phaseBefore = "NEW", reviewedAt = 10, elapsedDays = null,
                scheduledSeconds = 600, durationMillis = 2_000, exercise = "FLASHCARD",
                stabilityAfter = 2.3, difficultyAfter = 2.1, retrievabilityBefore = null,
            ),
        )
        assertEquals(1, db.reviewLogDao().getDailyCounts(since = 0).single().newCount)

        db.deckDao().delete(deckId)
        assertTrue(db.cardDao().getAll().isEmpty())
        assertTrue(db.reviewLogDao().getAll().isEmpty())
    }

    @Test
    fun studyCandidatesIncludeNewAndDueCards() = runTest {
        val deckId = seed()
        assertEquals(3, db.cardDao().getStudyCandidates(deckId, cutoff = 1_000).size)
        assertEquals(2, db.cardDao().getStudyCandidates(null, cutoff = 100).size)
    }
}
