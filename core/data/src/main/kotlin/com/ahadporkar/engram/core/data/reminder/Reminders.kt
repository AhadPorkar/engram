package com.ahadporkar.engram.core.data.reminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ahadporkar.engram.core.data.R
import com.ahadporkar.engram.core.data.repository.SettingsRepository
import com.ahadporkar.engram.core.data.repository.StatsRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Schedules the daily study reminder with WorkManager (survives reboots, respects Doze). */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clock: Clock,
    private val settingsRepository: SettingsRepository,
) {
    /** Applies the current settings: schedules or cancels the reminder. */
    suspend fun sync() {
        val settings = settingsRepository.current()
        if (settings.reminderEnabled) schedule(settings.reminderMinuteOfDay) else cancel()
    }

    fun schedule(minuteOfDay: Int) {
        val now = ZonedDateTime.now(clock)
        var next = now.toLocalDate().atTime(minuteOfDay / 60, minuteOfDay % 60).atZone(clock.zone)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val delay = Duration.between(now, next)

        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, request)
    }

    fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    private companion object {
        const val WORK_NAME = "daily-study-reminder"
    }
}

/** Posts the reminder only if there is something to do and the daily goal is not reached yet. */
class ReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ReminderEntryPoint {
        fun statsRepository(): StatsRepository
    }

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(applicationContext, ReminderEntryPoint::class.java)
        val progress = entryPoint.statsRepository().observeDailyProgress().first()
        if (progress.goalReached) return Result.success()
        ReminderNotifications.show(applicationContext, progress.dueToday, progress.streak)
        return Result.success()
    }
}

object ReminderNotifications {
    const val CHANNEL_ID = "study_reminders"
    private const val NOTIFICATION_ID = 4_2001

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.reminder_channel_description) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    // The POST_NOTIFICATIONS permission is checked at the top of the function.
    @SuppressLint("MissingPermission")
    fun show(context: Context, dueCount: Int, streak: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        createChannel(context)
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = launch?.let {
            PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val text = if (dueCount > 0) {
            context.resources.getQuantityString(R.plurals.reminder_text_due, dueCount, dueCount)
        } else {
            context.getString(R.string.reminder_text_new)
        }
        val title = if (streak > 0) {
            context.resources.getQuantityString(R.plurals.reminder_title_streak, streak, streak)
        } else {
            context.getString(R.string.reminder_title)
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_engram)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}
