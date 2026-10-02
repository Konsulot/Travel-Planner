package com.travelplanner.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.travelplanner.model.CurrencyRate
import com.travelplanner.model.Expense
import com.travelplanner.model.PackingItem
import com.travelplanner.model.Place
import com.travelplanner.model.Trip
import com.travelplanner.model.TripEvent
import com.travelplanner.model.TripSpent
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Query("SELECT * FROM trips ORDER BY startDate ASC, id ASC")
    fun observeAll(): Flow<List<Trip>>

    @Query("SELECT * FROM trips WHERE id = :id")
    fun observeById(id: Long): Flow<Trip?>

    @Query("SELECT * FROM trips WHERE id = :id")
    suspend fun getById(id: Long): Trip?

    /** Сумма расходов по каждой поездке (в валюте поездки). */
    @Query(
        """
        SELECT t.id AS tripId, COALESCE(SUM(e.amountInTripCurrency), 0) AS spent
        FROM trips t LEFT JOIN expenses e ON e.tripId = t.id
        GROUP BY t.id
        """
    )
    fun observeSpentPerTrip(): Flow<List<TripSpent>>

    @Insert
    suspend fun insert(trip: Trip): Long

    @Update
    suspend fun update(trip: Trip)

    @Delete
    suspend fun delete(trip: Trip)
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE tripId = :tripId ORDER BY date DESC, id DESC")
    fun observeForTrip(tripId: Long): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE tripId = :tripId")
    suspend fun getForTrip(tripId: Long): List<Expense>

    @Insert
    suspend fun insert(expense: Expense): Long

    @Update
    suspend fun update(expense: Expense)

    @Update
    suspend fun updateAll(expenses: List<Expense>)

    @Delete
    suspend fun delete(expense: Expense)
}

@Dao
interface EventDao {
    @Query(
        """
        SELECT * FROM events WHERE tripId = :tripId
        ORDER BY date ASC, CASE WHEN time IS NULL THEN 1 ELSE 0 END, time ASC, id ASC
        """
    )
    fun observeForTrip(tripId: Long): Flow<List<TripEvent>>

    @Insert
    suspend fun insert(event: TripEvent): Long

    @Update
    suspend fun update(event: TripEvent)

    @Delete
    suspend fun delete(event: TripEvent)
}

@Dao
interface PlaceDao {
    @Query("SELECT * FROM places WHERE tripId = :tripId ORDER BY isVisited ASC, isWishlist DESC, name COLLATE NOCASE ASC")
    fun observeForTrip(tripId: Long): Flow<List<Place>>

    @Insert
    suspend fun insert(place: Place): Long

    @Update
    suspend fun update(place: Place)

    @Delete
    suspend fun delete(place: Place)
}

@Dao
interface PackingDao {
    @Query("SELECT * FROM packing_items WHERE tripId = :tripId ORDER BY category ASC, id ASC")
    fun observeForTrip(tripId: Long): Flow<List<PackingItem>>

    @Query("SELECT * FROM packing_items WHERE tripId = :tripId")
    suspend fun getForTrip(tripId: Long): List<PackingItem>

    @Insert
    suspend fun insert(item: PackingItem): Long

    @Insert
    suspend fun insertAll(items: List<PackingItem>)

    @Update
    suspend fun update(item: PackingItem)

    @Delete
    suspend fun delete(item: PackingItem)
}

@Dao
interface CurrencyRateDao {
    @Query("SELECT * FROM currency_rates")
    fun observeAll(): Flow<List<CurrencyRate>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rates: List<CurrencyRate>)
}
