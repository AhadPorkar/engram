package com.ahadporkar.engram.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.ahadporkar.engram.core.data.audio.Speaker
import com.ahadporkar.engram.core.data.audio.TextToSpeechSpeaker
import com.ahadporkar.engram.core.data.backup.BackupRepository
import com.ahadporkar.engram.core.data.backup.FileBackupRepository
import com.ahadporkar.engram.core.data.repository.ApplicationScope
import com.ahadporkar.engram.core.data.repository.DataStoreSettingsRepository
import com.ahadporkar.engram.core.data.repository.DeckRepository
import com.ahadporkar.engram.core.data.repository.DefaultDispatcher
import com.ahadporkar.engram.core.data.repository.IoDispatcher
import com.ahadporkar.engram.core.data.repository.NoteRepository
import com.ahadporkar.engram.core.data.repository.OfflineDeckRepository
import com.ahadporkar.engram.core.data.repository.OfflineNoteRepository
import com.ahadporkar.engram.core.data.repository.OfflineStatsRepository
import com.ahadporkar.engram.core.data.repository.OfflineStudyRepository
import com.ahadporkar.engram.core.data.repository.SettingsRepository
import com.ahadporkar.engram.core.data.repository.StatsRepository
import com.ahadporkar.engram.core.data.repository.StudyRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindDeckRepository(impl: OfflineDeckRepository): DeckRepository

    @Binds
    abstract fun bindNoteRepository(impl: OfflineNoteRepository): NoteRepository

    @Binds
    abstract fun bindStudyRepository(impl: OfflineStudyRepository): StudyRepository

    @Binds
    abstract fun bindStatsRepository(impl: OfflineStatsRepository): StatsRepository

    @Binds
    abstract fun bindSettingsRepository(impl: DataStoreSettingsRepository): SettingsRepository

    @Binds
    abstract fun bindBackupRepository(impl: FileBackupRepository): BackupRepository

    @Binds
    abstract fun bindSpeaker(impl: TextToSpeechSpeaker): Speaker
}

@Module
@InstallIn(SingletonComponent::class)
object DataProvidersModule {

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()

    @Provides
    @Singleton
    fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            // A corrupted settings file falls back to defaults instead of crashing at startup.
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            produceFile = { context.preferencesDataStoreFile("engram_settings") },
        )

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
