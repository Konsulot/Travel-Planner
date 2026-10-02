package com.travelplanner.data

import com.travelplanner.model.Expense
import com.travelplanner.model.PackingItem
import com.travelplanner.model.Place
import com.travelplanner.model.Trip
import com.travelplanner.model.TripEvent
import com.travelplanner.model.TripWithSpent
import com.travelplanner.util.CurrencyConverter
import com.travelplanner.util.PackingTemplates
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Единая точка доступа к данным поездок для ViewModel. */
interface TripRepository {
    fun observeTrips(): Flow<List<TripWithSpent>>
    fun observeTrip(id: Long): Flow<Trip?>
    suspend fun getTrip(id: Long): Trip?

    /** Создаёт поездку (id == 0) или обновляет существующую. Возвращает id поездки. */
    suspend fun saveTrip(trip: Trip): Long
    suspend fun deleteTrip(trip: Trip)

    /**
     * Пересчитывает суммы расходов поездки в новую валюту бюджета.
     * Нужен, когда пользователь меняет валюту поездки после добавления расходов.
     */
    suspend fun recalculateExpenses(tripId: Long, newCurrency: String, ratesPerUsd: Map<String, Double>)

    fun observeExpenses(tripId: Long): Flow<List<Expense>>
    suspend fun saveExpense(expense: Expense)
    suspend fun deleteExpense(expense: Expense)

    fun observeEvents(tripId: Long): Flow<List<TripEvent>>
    suspend fun saveEvent(event: TripEvent)
    suspend fun deleteEvent(event: TripEvent)

    fun observePlaces(tripId: Long): Flow<List<Place>>
    suspend fun savePlace(place: Place)
    suspend fun deletePlace(place: Place)

    fun observePackingItems(tripId: Long): Flow<List<PackingItem>>
    suspend fun savePackingItem(item: PackingItem)
    suspend fun deletePackingItem(item: PackingItem)

    /** Добавляет базовый список вещей (без дубликатов). Возвращает количество добавленных. */
    suspend fun addPackingTemplate(tripId: Long): Int
}

class OfflineTripRepository(
    private val tripDao: TripDao,
    private val expenseDao: ExpenseDao,
    private val eventDao: EventDao,
    private val placeDao: PlaceDao,
    private val packingDao: PackingDao,
) : TripRepository {

    constructor(db: AppDatabase) : this(
        db.tripDao(), db.expenseDao(), db.eventDao(), db.placeDao(), db.packingDao(),
    )

    override fun observeTrips(): Flow<List<TripWithSpent>> =
        combine(tripDao.observeAll(), tripDao.observeSpentPerTrip()) { trips, spent ->
            val spentById = spent.associate { it.tripId to it.spent }
            trips.map { TripWithSpent(it, spentById[it.id] ?: 0.0) }
        }

    override fun observeTrip(id: Long): Flow<Trip?> = tripDao.observeById(id)

    override suspend fun getTrip(id: Long): Trip? = tripDao.getById(id)

    override suspend fun saveTrip(trip: Trip): Long =
        if (trip.id == 0L) {
            tripDao.insert(trip)
        } else {
            tripDao.update(trip)
            trip.id
        }

    override suspend fun deleteTrip(trip: Trip) = tripDao.delete(trip)

    override suspend fun recalculateExpenses(tripId: Long, newCurrency: String, ratesPerUsd: Map<String, Double>) {
        val updated = expenseDao.getForTrip(tripId).map { expense ->
            val converted = CurrencyConverter.convert(expense.amount, expense.currency, newCurrency, ratesPerUsd)
            expense.copy(amountInTripCurrency = converted ?: expense.amountInTripCurrency)
        }
        if (updated.isNotEmpty()) expenseDao.updateAll(updated)
    }

    override fun observeExpenses(tripId: Long): Flow<List<Expense>> = expenseDao.observeForTrip(tripId)

    override suspend fun saveExpense(expense: Expense) {
        if (expense.id == 0L) expenseDao.insert(expense) else expenseDao.update(expense)
    }

    override suspend fun deleteExpense(expense: Expense) = expenseDao.delete(expense)

    override fun observeEvents(tripId: Long): Flow<List<TripEvent>> = eventDao.observeForTrip(tripId)

    override suspend fun saveEvent(event: TripEvent) {
        if (event.id == 0L) eventDao.insert(event) else eventDao.update(event)
    }

    override suspend fun deleteEvent(event: TripEvent) = eventDao.delete(event)

    override fun observePlaces(tripId: Long): Flow<List<Place>> = placeDao.observeForTrip(tripId)

    override suspend fun savePlace(place: Place) {
        if (place.id == 0L) placeDao.insert(place) else placeDao.update(place)
    }

    override suspend fun deletePlace(place: Place) = placeDao.delete(place)

    override fun observePackingItems(tripId: Long): Flow<List<PackingItem>> = packingDao.observeForTrip(tripId)

    override suspend fun savePackingItem(item: PackingItem) {
        if (item.id == 0L) packingDao.insert(item) else packingDao.update(item)
    }

    override suspend fun deletePackingItem(item: PackingItem) = packingDao.delete(item)

    override suspend fun addPackingTemplate(tripId: Long): Int {
        val items = PackingTemplates.itemsFor(tripId, packingDao.getForTrip(tripId))
        if (items.isNotEmpty()) packingDao.insertAll(items)
        return items.size
    }
}
