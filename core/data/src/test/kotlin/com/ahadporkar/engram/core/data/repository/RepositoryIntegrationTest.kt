package com.ahadporkar.engram.core.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ahadporkar.engram.core.data.backup.FileBackupRepository
import com.ahadporkar.engram.core.data.backup.RestoreMode
import com.ahadporkar.engram.core.data.time.StudyTime
import com.ahadporkar.engram.core.database.EngramDatabase
import com.ahadporkar.engram.core.learning.session.StudySession
import com.ahadporkar.engram.core.model.Deck
import com.ahadporkar.engram.core.model.ExerciseType
import com.ahadporkar.engram.core.model.Note
import com.ahadporkar.engram.core.model.StudyMode
import com.ahadporkar.engram.core.model.UserSettings
import com.ahadporkar.engram.core.srs.CardPhase
import com.ahadporkar.engram.core.srs.FsrsScheduler
import com.ahadporkar.engram.core.srs.Rating
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class RepositoryIntegrationTest {

    private class FakeSettingsRepository : SettingsRepository {
        val state = MutableStateFlow(UserSettings(enableFuzz = false, createReverseCards = true))
        override val settings: Flow<UserSettings> = state
        override suspend fun update(transform: (UserSettings) -> UserSettings) {
            state.value = transform(state.value)
        }
        override suspend fun isSampleContentSeeded() = true
        override suspend fun markSampleContentSeeded() = Unit
    }

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val clock = Clock.fixed(Instant.parse("2026-03-10T10:00:00Z"), ZoneOffset.UTC)
    private val settings = FakeSettingsRepository()
    private lateinit var db: EngramDatabase
    private lateinit var decks: OfflineDeckRepository
    private lateinit var notes: OfflineNoteRepository
    private lateinit var study: OfflineStudyRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(context, EngramDatabase::class.java).allowMainThreadQueries().build()
        val studyTime = StudyTime(clock, settings)
        decks = OfflineDeckRepository(db.deckDao(), db.reviewLogDao(), studyTime)
        notes = OfflineNoteRepository(db, db.noteDao(), db.cardDao(), clock)
        study = OfflineStudyRepository(db, db.deckDao(), db.noteDao(), db.cardDao(), db.reviewLogDao(), settings, studyTime)
    }

    @After
    fun tearDown() = db.close()

    private suspend fun deckWithWords(count: Int): Long {
        val deckId = decks.saveDeck(Deck(name = "German", frontLanguage = "de-DE", createdAt = clock.instant()))
        notes.importNotes(
            deckId,
            (1..count).map { NoteDraft(front = "Wort $it", back = "word $it") },
            withProductionCards = true,
        )
        return deckId
    }

    @Test
    fun savingANoteCreatesRecognitionAndProductionCards() = runTest {
        val deckId = deckWithWords(0)
        val id = notes.saveNote(
            Note(deckId = deckId, front = "der Hund", back = "the dog", createdAt = clock.instant(), updatedAt = clock.instant()),
            withProductionCard = true,
        )
        assertTrue(notes.hasProductionCard(id))
        assertTrue(notes.isDuplicate(deckId, "DER HUND", excludeNoteId = 0))

        notes.saveNote(notes.getNote(id)!!, withProductionCard = false)
        assertEquals(false, notes.hasProductionCard(id))
    }

    @Test
    fun sessionRespectsDailyNewLimitAndSiblingBurying() = runTest {
        val deckId = deckWithWords(30) // deck default: 15 new per day
        val data = study.loadSession(SessionRequest(deckId, StudyMode.SMART))
        assertEquals(15, data.cards.size)
        assertEquals(15, data.cards.map { it.note.id }.toSet().size)
        assertTrue(data.pool.backs.size >= 3)
    }

    @Test
    fun answersArePersistedAndCanBeUndone() = runTest {
        val deckId = deckWithWords(3)
        val data = study.loadSession(SessionRequest(deckId, StudyMode.FLASHCARDS))
        val session = StudySession(data.cards, StudyMode.FLASHCARDS, FsrsScheduler(data.settings.toSchedulerConfig()))
        val entry = session.next(clock.instant())!!
        val applied = session.answer(Rating.GOOD, clock.instant(), ExerciseType.FLASHCARD, 2_000)
        val logId = study.persistAnswer(applied.card, applied.log)!!
        session.markPersisted(logId)

        assertEquals(CardPhase.LEARNING.name, db.cardDao().getAll().first { it.id == entry.item.card.id }.phase)
        val summary = decks.observeDeckSummaries().first().single()
        assertEquals(1, summary.learningCount)

        val undo = session.undo()!!
        study.revertAnswer(undo.previousCard!!, undo.logId)
        assertEquals(CardPhase.NEW.name, db.cardDao().getAll().first { it.id == entry.item.card.id }.phase)
        assertTrue(db.reviewLogDao().getAll().isEmpty())
    }

    @Test
    fun backupRoundTripRestoresEverything() = runTest {
        val deckId = deckWithWords(5)
        val backups = FileBackupRepository(
            context, db, db.deckDao(), db.noteDao(), db.cardDao(), db.reviewLogDao(),
            decks, notes, settings, clock, StandardTestDispatcher(testScheduler),
        )
        val buffer = ByteArrayOutputStream()
        backups.exportTo(buffer)
        val json = buffer.toString(Charsets.UTF_8.name())
        assertTrue(json.contains("engram-backup"))

        decks.deleteDeck(deckId)
        assertTrue(decks.getDecks().isEmpty())

        val summary = backups.restoreFrom(json, RestoreMode.REPLACE)
        assertEquals(1, summary.decks)
        assertEquals(5, db.noteDao().getAll().size)
        assertEquals(10, db.cardDao().getAll().size)

        backups.restoreFrom(json, RestoreMode.MERGE)
        assertEquals(2, decks.getDecks().size)
        assertEquals(20, db.cardDao().getAll().size)
    }
}
