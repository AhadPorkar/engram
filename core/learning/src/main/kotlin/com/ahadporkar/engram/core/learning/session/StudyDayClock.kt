package com.ahadporkar.engram.core.learning.session

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Maps instants to "study days". A study day starts at [dayStartHour] local time (default 4 am),
 * so a session at 1 am still counts towards the previous day — exactly like Anki.
 */
class StudyDayClock(private val zone: ZoneId, private val dayStartHour: Int = 4) {

    init {
        require(dayStartHour in 0..23) { "dayStartHour must be 0..23" }
    }

    fun studyDate(instant: Instant): LocalDate =
        instant.atZone(zone).minusHours(dayStartHour.toLong()).toLocalDate()

    fun startOf(date: LocalDate): Instant = date.atTime(dayStartHour, 0).atZone(zone).toInstant()

    fun startOfStudyDay(instant: Instant): Instant = startOf(studyDate(instant))

    fun endOfStudyDay(instant: Instant): Instant = startOf(studyDate(instant).plusDays(1))
}
