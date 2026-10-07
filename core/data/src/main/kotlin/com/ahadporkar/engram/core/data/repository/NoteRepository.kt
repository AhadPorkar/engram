package com.ahadporkar.engram.core.data.repository

import androidx.room.withTransaction
import com.ahadporkar.engram.core.data.mapper.toEntity
import com.ahadporkar.engram.core.data.mapper.toModel
import com.ahadporkar.engram.core.database.EngramDatabase
import com.ahadporkar.engram.core.database.dao.CardDao
import com.ahadporkar.engram.core.database.dao.NoteDao
import com.ahadporkar.engram.core.model.Card
import com.ahadporkar.engram.core.model.CardDirection
import com.ahadporkar.engram.core.model.Note
import com.ahadporkar.engram.core.srs.FsrsScheduler
import com.ahadporkar.engram.core.srs.SchedulingState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** A note with its cards and how well it is remembered right now. */
data class NoteWithMemory(
    val note: Note,
    val cards: List<Card>,
    /** Lowest retrievability of its cards, `null` while all cards are new. */
    val retrievability: Double?,
) {
    val isNew: Boolean get() = cards.all { it.isNew }
    val isLeech: Boolean get() = cards.any { it.leech }
}

interface NoteRepository {
    fun observeNotes(deckId: Long, query: String, starredOnly: Boolean): Flow<List<NoteWithMemory>>

    suspend fun getNote(id: Long): Note?

    suspend fun hasProductionCard(noteId: Long): Boolean

    /**
     * Inserts or updates a note and makes sure its cards exist. A production card is added
     * when [withProductionCard] is true; an unstudied one is removed when it is false.
     */
    suspend fun saveNote(note: Note, withProductionCard: Boolean): Long

    suspend fun deleteNote(id: Long)

    suspend fun setStarred(id: Long, starred: Boolean)

    suspend fun isDuplicate(deckId: Long, front: String, excludeNoteId: Long): Boolean

    /** Bulk insert in one transaction. Returns the number of notes created. */
    suspend fun importNotes(deckId: Long, drafts: List<NoteDraft>, withProductionCards: Boolean): Int
}

@Singleton
class OfflineNoteRepository @Inject constructor(
    private val database: EngramDatabase,
    private val noteDao: NoteDao,
    private val cardDao: CardDao,
    private val clock: Clock,
) : NoteRepository {

    private val scheduler = FsrsScheduler()

    override fun observeNotes(deckId: Long, query: String, starredOnly: Boolean): Flow<List<NoteWithMemory>> =
        noteDao.observeNotesWithCards(deckId, query.trim(), starredOnly).map { rows ->
            val now = clock.instant()
            rows.map { row ->
                val cards = row.cards.map { it.toModel() }
                NoteWithMemory(
                    note = row.note.toModel(),
                    cards = cards,
                    retrievability = cards.mapNotNull { scheduler.retrievability(it.scheduling, now) }.minOrNull(),
                )
            }
        }

    override suspend fun getNote(id: Long): Note? = noteDao.getNote(id)?.toModel()

    override suspend fun hasProductionCard(noteId: Long): Boolean =
        cardDao.getForNote(noteId).any { it.direction == CardDirection.PRODUCTION.name }

    override suspend fun saveNote(note: Note, withProductionCard: Boolean): Long = database.withTransaction {
        val now = clock.instant()
        require(note.front.isNotBlank() && note.back.isNotBlank()) { "Front and back are required" }
        if (note.id == 0L) {
            val noteId = noteDao.insert(note.copy(createdAt = now, updatedAt = now).toEntity())
            insertCards(noteId, note.deckId, withProductionCard, now)
            noteId
        } else {
            noteDao.update(note.copy(updatedAt = now).toEntity())
            cardDao.moveNoteCards(note.id, note.deckId)
            val existing = cardDao.getForNote(note.id)
            val production = existing.firstOrNull { it.direction == CardDirection.PRODUCTION.name }
            when {
                withProductionCard && production == null ->
                    cardDao.insert(newCard(note.id, note.deckId, CardDirection.PRODUCTION, now).toEntity())
                !withProductionCard && production != null && production.phase == "NEW" ->
                    cardDao.delete(production.id)
            }
            note.id
        }
    }

    override suspend fun deleteNote(id: Long) = noteDao.delete(id)

    override suspend fun setStarred(id: Long, starred: Boolean) =
        noteDao.setStarred(id, starred, clock.instant().toEpochMilli())

    override suspend fun isDuplicate(deckId: Long, front: String, excludeNoteId: Long): Boolean =
        front.isNotBlank() && noteDao.countWithFront(deckId, front.trim(), excludeNoteId) > 0

    override suspend fun importNotes(deckId: Long, drafts: List<NoteDraft>, withProductionCards: Boolean): Int =
        database.withTransaction {
            val base = clock.instant()
            var created = 0
            drafts.filter { it.front.isNotBlank() && it.back.isNotBlank() }.forEachIndexed { index, draft ->
                // Keep the file order: each note is one millisecond "newer" than the previous one.
                val createdAt = base.plusMillis(index.toLong())
                val noteId = noteDao.insert(
                    Note(
                        deckId = deckId,
                        front = draft.front,
                        back = draft.back,
                        synonyms = draft.synonyms,
                        example = draft.example,
                        mnemonic = draft.mnemonic,
                        tags = draft.tags,
                        createdAt = createdAt,
                        updatedAt = createdAt,
                    ).toEntity(),
                )
                insertCards(noteId, deckId, withProductionCards, createdAt)
                created++
            }
            created
        }

    private suspend fun insertCards(noteId: Long, deckId: Long, withProduction: Boolean, now: Instant) {
        cardDao.insert(newCard(noteId, deckId, CardDirection.RECOGNITION, now).toEntity())
        if (withProduction) cardDao.insert(newCard(noteId, deckId, CardDirection.PRODUCTION, now).toEntity())
    }

    private fun newCard(noteId: Long, deckId: Long, direction: CardDirection, now: Instant) = Card(
        noteId = noteId,
        deckId = deckId,
        direction = direction,
        scheduling = SchedulingState.newCard(now),
    )
}
