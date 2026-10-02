package com.travelplanner.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.travelplanner.model.CurrencyRate
import com.travelplanner.model.Expense
import com.travelplanner.model.ExpenseCategory
import com.travelplanner.model.PackingCategory
import com.travelplanner.model.PackingItem
import com.travelplanner.model.Place
import com.travelplanner.model.Trip
import com.travelplanner.model.TripEvent

/** Локальная база данных приложения: все данные хранятся на устройстве. */
@Database(
    entities = [
        Trip::class,
        Expense::class,
        TripEvent::class,
        Place::class,
        PackingItem::class,
        CurrencyRate::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun eventDao(): EventDao
    abstract fun placeDao(): PlaceDao
    abstract fun packingDao(): PackingDao
    abstract fun currencyRateDao(): CurrencyRateDao

    companion object {
        private const val DB_NAME = "travel_planner.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DB_NAME)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}

/** Перечисления храним в базе по имени — так порядок констант можно менять безопасно. */
class Converters {
    @TypeConverter
    fun expenseCategoryToString(value: ExpenseCategory): String = value.name

    @TypeConverter
    fun stringToExpenseCategory(value: String): ExpenseCategory =
        ExpenseCategory.entries.firstOrNull { it.name == value } ?: ExpenseCategory.OTHER

    @TypeConverter
    fun packingCategoryToString(value: PackingCategory): String = value.name

    @TypeConverter
    fun stringToPackingCategory(value: String): PackingCategory =
        PackingCategory.entries.firstOrNull { it.name == value } ?: PackingCategory.OTHER
}
