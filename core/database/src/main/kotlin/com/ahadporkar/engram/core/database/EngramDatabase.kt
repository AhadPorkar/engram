package com.ahadporkar.engram.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ahadporkar.engram.core.database.dao.CardDao
import com.ahadporkar.engram.core.database.dao.DeckDao
import com.ahadporkar.engram.core.database.dao.NoteDao
import com.ahadporkar.engram.core.database.dao.ReviewLogDao
import com.ahadporkar.engram.core.database.entity.CardEntity
import com.ahadporkar.engram.core.database.entity.DeckEntity
import com.ahadporkar.engram.core.database.entity.NoteEntity
import com.ahadporkar.engram.core.database.entity.ReviewLogEntity

/**
 * Successor of `WordRoomDatabase`. Schema JSON is exported to `core/database/schemas`
 * so future versions can ship tested migrations instead of `fallbackToDestructiveMigration()`.
 */
@Database(
    entities = [DeckEntity::class, NoteEntity::class, CardEntity::class, ReviewLogEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class EngramDatabase : RoomDatabase() {
    abstract fun deckDao(): DeckDao
    abstract fun noteDao(): NoteDao
    abstract fun cardDao(): CardDao
    abstract fun reviewLogDao(): ReviewLogDao

    companion object {
        const val NAME = "engram.db"
    }
}
