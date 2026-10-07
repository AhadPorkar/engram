package com.ahadporkar.engram.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ahadporkar.engram.core.database.entity.CardEntity
import com.ahadporkar.engram.core.database.entity.CardWithNote
import com.ahadporkar.engram.core.database.entity.DeckDailyCount
import com.ahadporkar.engram.core.database.entity.DeckEntity
import com.ahadporkar.engram.core.database.entity.DeckWithCounts
import com.ahadporkar.engram.core.database.entity.NoteEntity
import com.ahadporkar.engram.core.database.entity.NoteWithCards
import com.ahadporkar.engram.core.database.entity.ReviewLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeckDao {

    /** Decks with new / learning / review counts; [cutoff] is the end of the current study day. */
    @Query(
        """
        SELECT d.*,
            (SELECT COUNT(*) FROM cards c
                WHERE c.deck_id = d.id AND c.suspended = 0 AND c.phase = 'NEW') AS new_count,
            (SELECT COUNT(*) FROM cards c
                WHERE c.deck_id = d.id AND c.suspended = 0
                AND c.phase IN ('LEARNING', 'RELEARNING') AND c.due < :cutoff) AS learning_count,
            (SELECT COUNT(*) FROM cards c
                WHERE c.deck_id = d.id AND c.suspended = 0
                AND c.phase = 'REVIEW' AND c.due < :cutoff) AS review_count,
            (SELECT COUNT(*) FROM notes n WHERE n.deck_id = d.id) AS note_count
        FROM decks d
        ORDER BY d.name COLLATE NOCASE
        """,
    )
    fun observeDecksWithCounts(cutoff: Long): Flow<List<DeckWithCounts>>

    @Query("SELECT * FROM decks WHERE id = :id")
    fun observeDeck(id: Long): Flow<DeckEntity?>

    @Query("SELECT * FROM decks WHERE id = :id")
    suspend fun getDeck(id: Long): DeckEntity?

    @Query("SELECT * FROM decks ORDER BY name COLLATE NOCASE")
    suspend fun getAll(): List<DeckEntity>

    @Query("SELECT COUNT(*) FROM decks")
    suspend fun count(): Int

    @Insert
    suspend fun insert(deck: DeckEntity): Long

    @Insert
    suspend fun insertAll(decks: List<DeckEntity>)

    @Update
    suspend fun update(deck: DeckEntity)

    @Query("DELETE FROM decks WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM decks")
    suspend fun deleteAll()
}

@Dao
interface NoteDao {

    @Transaction
    @Query(
        """
        SELECT * FROM notes
        WHERE deck_id = :deckId
          AND (:starredOnly = 0 OR starred = 1)
          AND (:query = ''
               OR front LIKE '%' || :query || '%'
               OR back LIKE '%' || :query || '%'
               OR synonyms LIKE '%' || :query || '%'
               OR tags LIKE '%' || :query || '%')
        ORDER BY created_at DESC, id DESC
        """,
    )
    fun observeNotesWithCards(deckId: Long, query: String, starredOnly: Boolean): Flow<List<NoteWithCards>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNote(id: Long): NoteEntity?

    @Query("SELECT * FROM notes ORDER BY id")
    suspend fun getAll(): List<NoteEntity>

    @Query("SELECT COUNT(*) FROM notes WHERE deck_id = :deckId AND front = :front COLLATE NOCASE AND id != :excludeId")
    suspend fun countWithFront(deckId: Long, front: String, excludeId: Long): Int

    @Query("SELECT front FROM notes WHERE deck_id = :deckId")
    suspend fun frontsInDeck(deckId: Long): List<String>

    @Query("SELECT back FROM notes WHERE deck_id = :deckId")
    suspend fun backsInDeck(deckId: Long): List<String>

    @Query("SELECT front FROM notes ORDER BY RANDOM() LIMIT :limit")
    suspend fun randomFronts(limit: Int): List<String>

    @Query("SELECT back FROM notes ORDER BY RANDOM() LIMIT :limit")
    suspend fun randomBacks(limit: Int): List<String>

    @Insert
    suspend fun insert(note: NoteEntity): Long

    @Insert
    suspend fun insertAll(notes: List<NoteEntity>)

    @Update
    suspend fun update(note: NoteEntity)

    @Query("UPDATE notes SET starred = :starred, updated_at = :updatedAt WHERE id = :id")
    suspend fun setStarred(id: Long, starred: Boolean, updatedAt: Long)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface CardDao {

    /** Cards that may be studied before [cutoff]: all new cards plus everything due. */
    @Transaction
    @Query(
        """
        SELECT * FROM cards
        WHERE suspended = 0
          AND (:deckId IS NULL OR deck_id = :deckId)
          AND (phase = 'NEW' OR due < :cutoff)
        """,
    )
    suspend fun getStudyCandidates(deckId: Long?, cutoff: Long): List<CardWithNote>

    @Transaction
    @Query("SELECT * FROM cards WHERE suspended = 0 AND (:deckId IS NULL OR deck_id = :deckId)")
    suspend fun getActiveWithNotes(deckId: Long?): List<CardWithNote>

    @Transaction
    @Query("SELECT * FROM cards WHERE id IN (:ids)")
    suspend fun getWithNotes(ids: List<Long>): List<CardWithNote>

    @Transaction
    @Query("SELECT * FROM cards WHERE leech = 1 ORDER BY lapses DESC")
    fun observeLeeches(): Flow<List<CardWithNote>>

    @Query("SELECT * FROM cards")
    fun observeAll(): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards ORDER BY id")
    suspend fun getAll(): List<CardEntity>

    @Query("SELECT * FROM cards WHERE note_id = :noteId")
    suspend fun getForNote(noteId: Long): List<CardEntity>

    @Query(
        """
        SELECT COUNT(*) FROM cards
        WHERE suspended = 0 AND phase != 'NEW' AND due < :cutoff
        """,
    )
    suspend fun countDue(cutoff: Long): Int

    @Insert
    suspend fun insert(card: CardEntity): Long

    @Insert
    suspend fun insertAll(cards: List<CardEntity>)

    @Update
    suspend fun update(card: CardEntity)

    @Query("UPDATE cards SET deck_id = :deckId WHERE note_id = :noteId")
    suspend fun moveNoteCards(noteId: Long, deckId: Long)

    @Query("UPDATE cards SET suspended = :suspended WHERE id = :id")
    suspend fun setSuspended(id: Long, suspended: Boolean)

    @Query("DELETE FROM cards WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface ReviewLogDao {

    @Insert
    suspend fun insert(log: ReviewLogEntity): Long

    @Insert
    suspend fun insertAll(logs: List<ReviewLogEntity>)

    @Query("DELETE FROM review_logs WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM review_logs ORDER BY reviewed_at")
    fun observeAll(): Flow<List<ReviewLogEntity>>

    @Query("SELECT * FROM review_logs ORDER BY id")
    suspend fun getAll(): List<ReviewLogEntity>

    /** Timestamps only — enough for streaks and daily counts. */
    @Query("SELECT reviewed_at FROM review_logs ORDER BY reviewed_at")
    fun observeTimestamps(): Flow<List<Long>>

    @Query("SELECT reviewed_at FROM review_logs ORDER BY reviewed_at")
    suspend fun getTimestamps(): List<Long>

    @Query(
        """
        SELECT c.deck_id AS deck_id,
            SUM(CASE WHEN r.phase_before = 'NEW' THEN 1 ELSE 0 END) AS new_count,
            SUM(CASE WHEN r.phase_before = 'REVIEW' THEN 1 ELSE 0 END) AS review_count
        FROM review_logs r
        INNER JOIN cards c ON c.id = r.card_id
        WHERE r.reviewed_at >= :since
        GROUP BY c.deck_id
        """,
    )
    fun observeDailyCounts(since: Long): Flow<List<DeckDailyCount>>

    @Query(
        """
        SELECT c.deck_id AS deck_id,
            SUM(CASE WHEN r.phase_before = 'NEW' THEN 1 ELSE 0 END) AS new_count,
            SUM(CASE WHEN r.phase_before = 'REVIEW' THEN 1 ELSE 0 END) AS review_count
        FROM review_logs r
        INNER JOIN cards c ON c.id = r.card_id
        WHERE r.reviewed_at >= :since
        GROUP BY c.deck_id
        """,
    )
    suspend fun getDailyCounts(since: Long): List<DeckDailyCount>
}
