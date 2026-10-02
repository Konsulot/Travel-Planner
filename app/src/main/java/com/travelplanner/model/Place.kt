package com.travelplanner.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Место, которое хочется посетить в поездке.
 *
 * [isWishlist] — место отмечено как «хочу обязательно» (wishlist),
 * [isVisited] — место уже посещено.
 */
@Entity(
    tableName = "places",
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
data class Place(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val name: String,
    val address: String = "",
    val note: String = "",
    val isWishlist: Boolean = false,
    val isVisited: Boolean = false,
)
