package com.travelplanner.util

import java.time.LocalDate

enum class TripStatus { UPCOMING, ONGOING, FINISHED }

/** Работа с датами поездки. Все даты — epoch day ([LocalDate.toEpochDay]). */
object DateUtils {

    private const val MILLIS_PER_DAY = 86_400_000L

    fun today(): Long = LocalDate.now().toEpochDay()

    /** Длительность поездки в днях, включая день начала и день окончания. */
    fun durationDays(start: Long, end: Long): Int = (end - start + 1).coerceAtLeast(1).toInt()

    /** Список всех дней поездки по порядку. */
    fun tripDays(start: Long, end: Long): List<Long> =
        if (end < start) listOf(start) else (start..end).toList()

    fun status(start: Long, end: Long, today: Long): TripStatus = when {
        today < start -> TripStatus.UPCOMING
        today > end -> TripStatus.FINISHED
        else -> TripStatus.ONGOING
    }

    fun daysUntil(start: Long, today: Long): Long = start - today

    /** Номер дня поездки, начиная с 1 (для заголовков «День 1», «День 2»…). */
    fun dayNumber(day: Long, start: Long): Int = (day - start + 1).toInt()

    /**
     * DatePicker из Material 3 работает с миллисекундами UTC на полночь,
     * поэтому переводим без учёта часового пояса.
     */
    fun epochDayToUtcMillis(epochDay: Long): Long = epochDay * MILLIS_PER_DAY

    fun utcMillisToEpochDay(millis: Long): Long = Math.floorDiv(millis, MILLIS_PER_DAY)
}
