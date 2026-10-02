package com.travelplanner.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Расход в рамках поездки.
 *
 * Расход можно ввести в любой валюте (например, в местной). Чтобы бюджет считался корректно,
 * при сохранении сумма пересчитывается в валюту поездки и хранится в [amountInTripCurrency].
 */
@Entity(
    tableName = "expenses",
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
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val title: String,
    /** Сумма в исходной валюте [currency]. */
    val amount: Double,
    val currency: String,
    /** Сумма, пересчитанная в валюту поездки на момент сохранения. */
    val amountInTripCurrency: Double,
    val category: ExpenseCategory,
    /** Дата расхода (epoch day). */
    val date: Long,
    val note: String = "",
)

enum class ExpenseCategory(val title: String, val emoji: String) {
    TRANSPORT("Транспорт", "🚆"),
    ACCOMMODATION("Жильё", "🏨"),
    FOOD("Еда", "🍽️"),
    ENTERTAINMENT("Развлечения", "🎭"),
    SHOPPING("Покупки", "🛍️"),
    HEALTH("Здоровье", "💊"),
    OTHER("Другое", "📌"),
}
