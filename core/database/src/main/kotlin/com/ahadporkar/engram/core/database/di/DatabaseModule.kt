package com.ahadporkar.engram.core.database.di

import android.content.Context
import androidx.room.Room
import com.ahadporkar.engram.core.database.EngramDatabase
import com.ahadporkar.engram.core.database.dao.CardDao
import com.ahadporkar.engram.core.database.dao.DeckDao
import com.ahadporkar.engram.core.database.dao.NoteDao
import com.ahadporkar.engram.core.database.dao.ReviewLogDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): EngramDatabase =
        Room.databaseBuilder(context, EngramDatabase::class.java, EngramDatabase.NAME).build()

    @Provides
    fun provideDeckDao(database: EngramDatabase): DeckDao = database.deckDao()

    @Provides
    fun provideNoteDao(database: EngramDatabase): NoteDao = database.noteDao()

    @Provides
    fun provideCardDao(database: EngramDatabase): CardDao = database.cardDao()

    @Provides
    fun provideReviewLogDao(database: EngramDatabase): ReviewLogDao = database.reviewLogDao()
}
