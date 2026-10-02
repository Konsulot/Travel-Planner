package com.travelplanner.data

import com.travelplanner.model.CurrencyRate
import com.travelplanner.model.Expense
import com.travelplanner.model.PackingItem
import com.travelplanner.model.Place
import com.travelplanner.model.Trip
import com.travelplanner.model.TripEvent
import com.travelplanner.model.TripWithSpent
import com.travelplanner.util.CurrencyConverter
import com.travelplanner.util.PackingTemplates
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** Репозиторий в памяти для unit-тестов ViewModel (без Room и Android). */
class FakeTripRepository : TripRepository {
    val trips = MutableStateFlow<List<Trip>>(emptyList())
    val expenses = MutableStateFlow<List<Expense>>(emptyList())
    val events = MutableStateFlow<List<TripEvent>>(emptyList())
    val places = MutableStateFlow<List<Place>>(emptyList())
    val packing = MutableStateFlow<List<PackingItem>>(emptyList())
    private var nextId = 1L

    override fun observeTrips(): Flow<List<TripWithSpent>> = combine(trips, expenses) { t, e ->
        t.map { trip -> TripWithSpent(trip, e.filter { it.tripId == trip.id }.sumOf { it.amountInTripCurrency }) }
    }

    override fun observeTrip(id: Long): Flow<Trip?> = trips.map { list -> list.firstOrNull { it.id == id } }
    override suspend fun getTrip(id: Long): Trip? = trips.value.firstOrNull { it.id == id }

    override suspend fun saveTrip(trip: Trip): Long {
        val saved = if (trip.id == 0L) trip.copy(id = nextId++) else trip
        trips.update { list -> list.filter { it.id != saved.id } + saved }
        return saved.id
    }

    override suspend fun deleteTrip(trip: Trip) {
        trips.update { list -> list.filter { it.id != trip.id } }
        expenses.update { list -> list.filter { it.tripId != trip.id } }
    }

    override suspend fun recalculateExpenses(tripId: Long, newCurrency: String, ratesPerUsd: Map<String, Double>) {
        expenses.update { list ->
            list.map { e ->
                if (e.tripId != tripId) e
                else e.copy(
                    amountInTripCurrency = CurrencyConverter.convert(e.amount, e.currency, newCurrency, ratesPerUsd)
                        ?: e.amountInTripCurrency,
                )
            }
        }
    }

    override fun observeExpenses(tripId: Long) = expenses.map { l -> l.filter { it.tripId == tripId } }
    override suspend fun saveExpense(expense: Expense) {
        val saved = if (expense.id == 0L) expense.copy(id = nextId++) else expense
        expenses.update { list -> list.filter { it.id != saved.id } + saved }
    }
    override suspend fun deleteExpense(expense: Expense) = expenses.update { l -> l.filter { it.id != expense.id } }

    override fun observeEvents(tripId: Long) = events.map { l -> l.filter { it.tripId == tripId } }
    override suspend fun saveEvent(event: TripEvent) {
        val saved = if (event.id == 0L) event.copy(id = nextId++) else event
        events.update { list -> list.filter { it.id != saved.id } + saved }
    }
    override suspend fun deleteEvent(event: TripEvent) = events.update { l -> l.filter { it.id != event.id } }

    override fun observePlaces(tripId: Long) = places.map { l -> l.filter { it.tripId == tripId } }
    override suspend fun savePlace(place: Place) {
        val saved = if (place.id == 0L) place.copy(id = nextId++) else place
        places.update { list -> list.filter { it.id != saved.id } + saved }
    }
    override suspend fun deletePlace(place: Place) = places.update { l -> l.filter { it.id != place.id } }

    override fun observePackingItems(tripId: Long) = packing.map { l -> l.filter { it.tripId == tripId } }
    override suspend fun savePackingItem(item: PackingItem) {
        val saved = if (item.id == 0L) item.copy(id = nextId++) else item
        packing.update { list -> list.filter { it.id != saved.id } + saved }
    }
    override suspend fun deletePackingItem(item: PackingItem) = packing.update { l -> l.filter { it.id != item.id } }

    override suspend fun addPackingTemplate(tripId: Long): Int {
        val items = PackingTemplates.itemsFor(tripId, packing.value.filter { it.tripId == tripId })
        items.forEach { savePackingItem(it) }
        return items.size
    }
}

class FakeCurrencyRateDao(initial: List<CurrencyRate> = emptyList()) : CurrencyRateDao {
    val stored = MutableStateFlow(initial)
    override fun observeAll(): Flow<List<CurrencyRate>> = stored
    override suspend fun upsertAll(rates: List<CurrencyRate>) {
        stored.update { old -> (old.associateBy { it.code } + rates.associateBy { it.code }).values.toList() }
    }
}
