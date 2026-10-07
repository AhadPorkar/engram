package com.ahadporkar.engram.core.data.time

import com.ahadporkar.engram.core.data.repository.SettingsRepository
import com.ahadporkar.engram.core.learning.session.StudyDayClock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** The current study day and its bounds. */
data class StudyDayWindow(
    val clock: StudyDayClock,
    val date: LocalDate,
    val start: Instant,
    val end: Instant,
)

/** Single source of "now" and of study-day boundaries — injectable, so tests control time. */
@Singleton
class StudyTime @Inject constructor(
    private val clock: Clock,
    private val settingsRepository: SettingsRepository,
) {
    fun now(): Instant = clock.instant()

    suspend fun dayClock(): StudyDayClock =
        StudyDayClock(clock.zone, settingsRepository.current().dayStartHour)

    suspend fun window(): StudyDayWindow = windowFor(dayClock())

    /** Emits a new window whenever the study day (or the day-start setting) changes. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun windows(): Flow<StudyDayWindow> = settingsRepository.settings
        .map { it.dayStartHour }
        .distinctUntilChanged()
        .flatMapLatest { hour ->
            val dayClock = StudyDayClock(clock.zone, hour)
            ticker().map { windowFor(dayClock) }.distinctUntilChanged()
        }

    private fun windowFor(dayClock: StudyDayClock): StudyDayWindow {
        val now = clock.instant()
        return StudyDayWindow(
            clock = dayClock,
            date = dayClock.studyDate(now),
            start = dayClock.startOfStudyDay(now),
            end = dayClock.endOfStudyDay(now),
        )
    }

    private fun ticker(): Flow<Unit> = flow {
        while (true) {
            emit(Unit)
            delay(TICK_MILLIS)
        }
    }

    private companion object {
        const val TICK_MILLIS = 60_000L
    }
}
