package com.travelplanner.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Вещь из списка сборов. */
@Entity(
    tableName = "packing_items",
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
data class PackingItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val name: String,
    val category: PackingCategory = PackingCategory.OTHER,
    val quantity: Int = 1,
    val isPacked: Boolean = false,
)

enum class PackingCategory(val title: String, val emoji: String) {
    DOCUMENTS("Документы", "📄"),
    CLOTHES("Одежда", "👕"),
    HYGIENE("Гигиена", "🧴"),
    ELECTRONICS("Электроника", "🔌"),
    MEDICINE("Аптечка", "🩹"),
    OTHER("Другое", "🎒"),
}
