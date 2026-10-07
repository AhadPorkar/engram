package com.ahadporkar.engram

import android.app.Application
import android.util.Log
import com.ahadporkar.engram.core.data.reminder.ReminderNotifications
import com.ahadporkar.engram.core.data.reminder.ReminderScheduler
import com.ahadporkar.engram.core.data.repository.ApplicationScope
import com.ahadporkar.engram.core.data.seed.SampleContentSeeder
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class EngramApplication : Application() {

    @Inject lateinit var seeder: SampleContentSeeder

    @Inject lateinit var reminderScheduler: ReminderScheduler

    @Inject @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        ReminderNotifications.createChannel(this)
        applicationScope.launch {
            runCatching { seeder.seedIfNeeded() }
                .onFailure { Log.w(TAG, "Sample content could not be added", it) }
            runCatching { reminderScheduler.sync() }
                .onFailure { Log.w(TAG, "Reminder could not be scheduled", it) }
        }
    }

    private companion object {
        const val TAG = "Engram"
    }
}
