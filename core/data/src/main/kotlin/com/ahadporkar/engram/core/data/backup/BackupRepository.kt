package com.ahadporkar.engram.core.data.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.net.Uri
import android.provider.OpenableColumns
import androidx.room.withTransaction
import com.ahadporkar.engram.core.data.importer.DelimitedTextParser
import com.ahadporkar.engram.core.data.repository.DeckRepository
import com.ahadporkar.engram.core.data.repository.IoDispatcher
import com.ahadporkar.engram.core.data.repository.NoteDraft
import com.ahadporkar.engram.core.data.repository.NoteRepository
import com.ahadporkar.engram.core.data.repository.SettingsRepository
import com.ahadporkar.engram.core.database.EngramDatabase
import com.ahadporkar.engram.core.database.dao.CardDao
import com.ahadporkar.engram.core.database.dao.DeckDao
import com.ahadporkar.engram.core.database.dao.NoteDao
import com.ahadporkar.engram.core.database.dao.ReviewLogDao
import com.ahadporkar.engram.core.database.entity.CardEntity
import com.ahadporkar.engram.core.database.entity.DeckEntity
import com.ahadporkar.engram.core.database.entity.NoteEntity
import com.ahadporkar.engram.core.database.entity.ReviewLogEntity
import com.ahadporkar.engram.core.model.Deck
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.io.OutputStream
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

enum class RestoreMode {
    /** Delete everything, then restore the backup exactly (ids included). */
    REPLACE,

    /** Add the backup's decks next to the existing ones. */
    MERGE,
}

data class ImportSummary(val decks: Int, val notes: Int, val skipped: Int = 0, val deckId: Long? = null)

/** Import failures with a reason the UI can translate. */
class ImportException(val reason: Reason, cause: Throwable? = null) : Exception(reason.name, cause) {
    enum class Reason { UNREADABLE, NOT_A_BACKUP, NEWER_VERSION, NOT_A_PROJECT_SHACO_DATABASE, NO_WORDS }
}

interface BackupRepository {
    suspend fun export(uri: Uri)

    suspend fun restore(uri: Uri, mode: RestoreMode): ImportSummary

    /** CSV / TSV / Anki plain-text export into an existing deck or a new deck named [newDeckName]. */
    suspend fun importDelimited(uri: Uri, targetDeckId: Long?, newDeckName: String): ImportSummary

    /** Imports the `word_database` file written by the 2019 ProjectShaco "BackUp Database" menu. */
    suspend fun importLegacyShaco(uri: Uri, deckName: String): ImportSummary

    suspend fun displayName(uri: Uri): String?
}

@Singleton
class FileBackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: EngramDatabase,
    private val deckDao: DeckDao,
    private val noteDao: NoteDao,
    private val cardDao: CardDao,
    private val reviewLogDao: ReviewLogDao,
    private val deckRepository: DeckRepository,
    private val noteRepository: NoteRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : BackupRepository {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun export(uri: Uri) = withContext(ioDispatcher) {
        // "wt" truncates an existing document; some providers only support "w".
        val resolver = context.contentResolver
        val output = runCatching { resolver.openOutputStream(uri, "wt") }.getOrNull()
            ?: resolver.openOutputStream(uri, "w")
            ?: throw ImportException(ImportException.Reason.UNREADABLE)
        exportTo(output)
    }

    /** Writes the whole collection as JSON and closes [output]. */
    internal suspend fun exportTo(output: OutputStream) {
        val backup = BackupFile(
            exportedAt = clock.millis(),
            decks = deckDao.getAll().map { it.toDto() },
            notes = noteDao.getAll().map { it.toDto() },
            cards = cardDao.getAll().map { it.toDto() },
            reviewLogs = reviewLogDao.getAll().map { it.toDto() },
        )
        output.bufferedWriter(Charsets.UTF_8).use { it.write(json.encodeToString(BackupFile.serializer(), backup)) }
    }

    override suspend fun restore(uri: Uri, mode: RestoreMode): ImportSummary = withContext(ioDispatcher) {
        restoreFrom(readText(uri), mode)
    }

    internal suspend fun restoreFrom(text: String, mode: RestoreMode): ImportSummary {
        val backup = try {
            json.decodeFromString(BackupFile.serializer(), text)
        } catch (e: SerializationException) {
            throw ImportException(ImportException.Reason.NOT_A_BACKUP, e)
        } catch (e: IllegalArgumentException) {
            throw ImportException(ImportException.Reason.NOT_A_BACKUP, e)
        }
        if (backup.format != BackupFile.FORMAT) throw ImportException(ImportException.Reason.NOT_A_BACKUP)
        if (backup.version > BackupFile.VERSION) throw ImportException(ImportException.Reason.NEWER_VERSION)

        database.withTransaction {
            when (mode) {
                RestoreMode.REPLACE -> {
                    deckDao.deleteAll()
                    deckDao.insertAll(backup.decks.map { it.toEntity(id = it.id) })
                    noteDao.insertAll(backup.notes.map { it.toEntity(id = it.id, deckId = it.deckId) })
                    cardDao.insertAll(backup.cards.map { it.toEntity(id = it.id, noteId = it.noteId, deckId = it.deckId) })
                    reviewLogDao.insertAll(backup.reviewLogs.map { it.toEntity(id = it.id, cardId = it.cardId) })
                }
                RestoreMode.MERGE -> {
                    val deckIds = backup.decks.associate { it.id to deckDao.insert(it.toEntity(id = 0)) }
                    val noteIds = backup.notes.mapNotNull { note ->
                        val deckId = deckIds[note.deckId] ?: return@mapNotNull null
                        note.id to noteDao.insert(note.toEntity(id = 0, deckId = deckId))
                    }.toMap()
                    val cardIds = backup.cards.mapNotNull { card ->
                        val noteId = noteIds[card.noteId] ?: return@mapNotNull null
                        val deckId = deckIds[card.deckId] ?: return@mapNotNull null
                        card.id to cardDao.insert(card.toEntity(id = 0, noteId = noteId, deckId = deckId))
                    }.toMap()
                    reviewLogDao.insertAll(
                        backup.reviewLogs.mapNotNull { log ->
                            cardIds[log.cardId]?.let { log.toEntity(id = 0, cardId = it) }
                        },
                    )
                }
            }
        }
        return ImportSummary(decks = backup.decks.size, notes = backup.notes.size)
    }

    override suspend fun importDelimited(uri: Uri, targetDeckId: Long?, newDeckName: String): ImportSummary =
        withContext(ioDispatcher) {
            val parsed = DelimitedTextParser.parse(readText(uri))
            if (parsed.drafts.isEmpty()) throw ImportException(ImportException.Reason.NO_WORDS)
            val deckId = targetDeckId ?: deckRepository.saveDeck(Deck(name = newDeckName, createdAt = clock.instant()))
            val created = noteRepository.importNotes(deckId, parsed.drafts, settingsRepository.current().createReverseCards)
            ImportSummary(decks = if (targetDeckId == null) 1 else 0, notes = created, skipped = parsed.skippedRows, deckId = deckId)
        }

    override suspend fun importLegacyShaco(uri: Uri, deckName: String): ImportSummary = withContext(ioDispatcher) {
        val copy = File(context.cacheDir, "projectshaco-import.db")
        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: throw ImportException(ImportException.Reason.UNREADABLE)
            input.use { source -> copy.outputStream().use { source.copyTo(it) } }
            val drafts = readLegacyWords(copy)
            if (drafts.isEmpty()) throw ImportException(ImportException.Reason.NO_WORDS)
            val deckId = deckRepository.saveDeck(Deck(name = deckName, createdAt = clock.instant()))
            val created = noteRepository.importNotes(deckId, drafts, settingsRepository.current().createReverseCards)
            ImportSummary(decks = 1, notes = created, deckId = deckId)
        } finally {
            listOf("", "-wal", "-shm", "-journal").forEach { File(copy.path + it).delete() }
        }
    }

    override suspend fun displayName(uri: Uri): String? = withContext(ioDispatcher) {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }

    /** Reads `word_table(id, word, meaning, synonyms)` from the legacy Room 1.1 database. */
    private fun readLegacyWords(file: File): List<NoteDraft> = try {
        SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
            db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'word_table'",
                null,
            ).use { if (!it.moveToFirst()) throw ImportException(ImportException.Reason.NOT_A_PROJECT_SHACO_DATABASE) }

            db.rawQuery("SELECT word, meaning, synonyms FROM word_table ORDER BY id", null).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            NoteDraft(
                                front = cursor.getString(0).orEmpty().trim(),
                                back = cursor.getString(1).orEmpty().trim(),
                                synonyms = cursor.getString(2).orEmpty().trim(),
                            ),
                        )
                    }
                }
            }
        }
    } catch (e: SQLiteException) {
        throw ImportException(ImportException.Reason.NOT_A_PROJECT_SHACO_DATABASE, e)
    }

    private fun readText(uri: Uri): String {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw ImportException(ImportException.Reason.UNREADABLE)
        return input.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }
}

private fun DeckEntity.toDto() = DeckDto(id, name, description, frontLanguage, backLanguage, newCardsPerDay, maxReviewsPerDay, createdAt)

private fun DeckDto.toEntity(id: Long) =
    DeckEntity(id, name, description, frontLanguage, backLanguage, newCardsPerDay, maxReviewsPerDay, createdAt)

private fun NoteEntity.toDto() =
    NoteDto(id, deckId, front, back, synonyms, example, mnemonic, tags, starred, createdAt, updatedAt)

private fun NoteDto.toEntity(id: Long, deckId: Long) =
    NoteEntity(id, deckId, front, back, synonyms, example, mnemonic, tags, starred, createdAt, updatedAt)

private fun CardEntity.toDto() =
    CardDto(id, noteId, deckId, direction, phase, step, stability, difficulty, due, lastReview, reps, lapses, suspended, leech)

private fun CardDto.toEntity(id: Long, noteId: Long, deckId: Long) =
    CardEntity(id, noteId, deckId, direction, phase, step, stability, difficulty, due, lastReview, reps, lapses, suspended, leech)

private fun ReviewLogEntity.toDto() = ReviewLogDto(
    id, cardId, rating, phaseBefore, reviewedAt, elapsedDays, scheduledSeconds, durationMillis, exercise,
    stabilityAfter, difficultyAfter, retrievabilityBefore,
)

private fun ReviewLogDto.toEntity(id: Long, cardId: Long) = ReviewLogEntity(
    id, cardId, rating, phaseBefore, reviewedAt, elapsedDays, scheduledSeconds, durationMillis, exercise,
    stabilityAfter, difficultyAfter, retrievabilityBefore,
)
