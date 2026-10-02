package com.travelplanner.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.travelplanner.model.Expense
import com.travelplanner.model.ExpenseCategory
import com.travelplanner.model.Trip
import com.travelplanner.model.TripEvent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Интеграционные тесты Room: реальная SQLite-база в памяти на устройстве/эмуляторе. */
@RunWith(AndroidJUnit4::class)
class TravelDatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: OfflineTripRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = OfflineTripRepository(db)
    }

    @After
    fun closeDb() = db.close()

    private fun newTrip() = Trip(
        title = "Тест", destination = "Москва", startDate = 100, endDate = 103,
        budget = 5000.0, currency = "RUB",
    )

    private fun expense(tripId: Long, amount: Double) = Expense(
        tripId = tripId, title = "Расход", amount = amount, currency = "RUB",
        amountInTripCurrency = amount, category = ExpenseCategory.FOOD, date = 101,
    )

    @Test
    fun saveTrip_insertsThenUpdates() = runTest {
        val id = repository.saveTrip(newTrip())
        assertTrue(id > 0)

        repository.saveTrip(repository.getTrip(id)!!.copy(title = "Новое название"))

        assertEquals("Новое название", repository.getTrip(id)!!.title)
        assertEquals(1, repository.observeTrips().first().size)
    }

    @Test
    fun observeTrips_sumsExpensesPerTrip() = runTest {
        val first = repository.saveTrip(newTrip())
        val second = repository.saveTrip(newTrip().copy(title = "Без расходов"))
        repository.saveExpense(expense(first, 1200.0))
        repository.saveExpense(expense(first, 300.0))

        val trips = repository.observeTrips().first().associateBy { it.trip.id }

        assertEquals(1500.0, trips.getValue(first).spent, 1e-9)
        assertEquals(0.0, trips.getValue(second).spent, 1e-9)
    }

    @Test
    fun deleteTrip_cascadesToChildTables() = runTest {
        val id = repository.saveTrip(newTrip())
        repository.saveExpense(expense(id, 100.0))
        repository.saveEvent(TripEvent(tripId = id, date = 100, title = "Событие"))
        repository.addPackingTemplate(id)

        repository.deleteTrip(repository.getTrip(id)!!)

        assertNull(repository.getTrip(id))
        assertTrue(repository.observeExpenses(id).first().isEmpty())
        assertTrue(repository.observeEvents(id).first().isEmpty())
        assertTrue(repository.observePackingItems(id).first().isEmpty())
    }

    @Test
    fun events_areOrderedByDateThenTimeWithUntimedLast() = runTest {
        val id = repository.saveTrip(newTrip())
        repository.saveEvent(TripEvent(tripId = id, date = 101, time = null, title = "C"))
        repository.saveEvent(TripEvent(tripId = id, date = 101, time = 9 * 60, title = "B"))
        repository.saveEvent(TripEvent(tripId = id, date = 100, time = 20 * 60, title = "A"))

        val titles = repository.observeEvents(id).first().map { it.title }

        assertEquals(listOf("A", "B", "C"), titles)
    }

    @Test
    fun recalculateExpenses_convertsToNewCurrency() = runTest {
        val id = repository.saveTrip(newTrip())
        repository.saveExpense(expense(id, 8000.0))

        repository.recalculateExpenses(id, "USD", mapOf("USD" to 1.0, "RUB" to 80.0))

        assertEquals(100.0, repository.observeExpenses(id).first().single().amountInTripCurrency, 1e-9)
    }
}
