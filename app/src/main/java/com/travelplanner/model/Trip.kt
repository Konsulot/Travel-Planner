package com.travelplanner.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Поездка.
 *
 * Даты хранятся как «эпохальные дни» ([java.time.LocalDate.toEpochDay]) — так их удобно
 * сравнивать и сортировать в SQL, и не возникает проблем с часовыми поясами.
 */
@Entity(tableName = "trips")
data class Trip(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val destination: String,
    val startDate: Long,
    val endDate: Long,
    /** Запланированный бюджет в валюте [currency]. */
    val budget: Double,
    /** ISO-код валюты бюджета, например "RUB". */
    val currency: String,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

/** Поездка вместе с суммой расходов (для списка поездок). */
data class TripWithSpent(
    val trip: Trip,
    val spent: Double,
)

/** Результат агрегирующего SQL-запроса: сколько потрачено по каждой поездке. */
data class TripSpent(
    val tripId: Long,
    val spent: Double,
)
