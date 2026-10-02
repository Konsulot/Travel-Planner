package com.travelplanner.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Событие маршрута: что и когда делаем в конкретный день поездки. */
@Entity(
    tableName = "events",
    foreignKeys = [
        ForeignKey(
            entity = Trip::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("tripId")],
)
data class TripEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    /** День события (epoch day). */
    val date: Long,
    /** Время в минутах от полуночи; null — «без времени». */
    val time: Int? = null,
    val title: String,
    val location: String = "",
    val note: String = "",
    val isDone: Boolean = false,
)
