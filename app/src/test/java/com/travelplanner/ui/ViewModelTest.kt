package com.travelplanner.ui

import com.travelplanner.data.CurrencyRepository
import com.travelplanner.data.FakeCurrencyRateDao
import com.travelplanner.data.FakeTripRepository
import com.travelplanner.model.CurrencyRate
import com.travelplanner.model.Expense
import com.travelplanner.model.ExpenseCategory
import com.travelplanner.model.Trip
import com.travelplanner.model.TripWithSpent
import com.travelplanner.ui.navigation.Routes
import com.travelplanner.ui.screens.converter.ConverterInput
import com.travelplanner.ui.screens.converter.ConverterViewModel
import com.travelplanner.ui.screens.details.TripDetailViewModel
import com.travelplanner.ui.screens.edit.TripEditViewModel
import com.travelplanner.ui.screens.trips.TripListViewModel
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private val testRates = listOf(
    CurrencyRate("USD", 1.0, 0L),
    CurrencyRate("RUB", 80.0, 0L),
    CurrencyRate("EUR", 0.8, 0L),
)

private fun trip(id: Long = 0, start: Long = 100, end: Long = 104, currency: String = "RUB") = Trip(
    id = id, title = "Trip $id", destination = "City", startDate = start, endDate = end,
    budget = 10_000.0, currency = currency,
)

@OptIn(ExperimentalCoroutinesApi::class)
abstract class MainDispatcherTest {
    @Before
    fun setMainDispatcher() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun resetMainDispatcher() = Dispatchers.resetMain()

    fun currencyRepository() = CurrencyRepository(FakeCurrencyRateDao(testRates), api = { error("offline in tests") })
}

class TripListViewModelTest {

    @Test
    fun buildState_splitsActiveAndFinishedTrips() {
        val today = 200L
        val finishedOld = TripWithSpent(trip(1, 10, 12), 0.0)
        val finishedRecent = TripWithSpent(trip(2, 150, 160), 0.0)
        val ongoing = TripWithSpent(trip(3, 199, 205), 0.0)
        val upcoming = TripWithSpent(trip(4, 250, 255), 0.0)

        val state = TripListViewModel.buildState(listOf(upcoming, finishedOld, ongoing, finishedRecent), today)

        assertEquals(listOf(3L, 4L), state.activeTrips.map { it.trip.id })
        assertEquals(listOf(2L, 1L), state.finishedTrips.map { it.trip.id })
        assertTrue(!state.isLoading && !state.isEmpty)
    }

    @Test
    fun buildState_empty() {
        assertTrue(TripListViewModel.buildState(emptyList(), 0).isEmpty)
    }
}

class TripEditViewModelTest : MainDispatcherTest() {

    @Test
    fun save_withEmptyForm_showsErrorsAndDoesNotSave() = runTest {
        val repo = FakeTripRepository()
        val vm = TripEditViewModel(SavedStateHandle(), repo, currencyRepository())

        vm.save()

        val state = vm.state.value
        assertNotNull(state.errors.title)
        assertNotNull(state.errors.budget)
        assertNull(state.savedTripId)
        assertTrue(repo.trips.value.isEmpty())
    }

    @Test
    fun save_validForm_createsTrip() = runTest {
        val repo = FakeTripRepository()
        val vm = TripEditViewModel(SavedStateHandle(), repo, currencyRepository())

        vm.onTitleChange("  Отпуск  ")
        vm.onDestinationChange("Казань")
        vm.onStartDateChange(100)
        vm.onEndDateChange(103)
        vm.onBudgetChange("30 000,50")
        vm.onCurrencyChange("RUB")
        vm.save()

        val saved = repo.trips.value.single()
        assertEquals("Отпуск", saved.title)
        assertEquals(30_000.5, saved.budget, 1e-9)
        assertEquals(saved.id, vm.state.value.savedTripId)
    }

    @Test
    fun startDateAfterEnd_movesEndDate() {
        val vm = TripEditViewModel(SavedStateHandle(), FakeTripRepository(), currencyRepository())
        vm.onEndDateChange(100)
        vm.onStartDateChange(110)
        assertEquals(110L, vm.state.value.endDate)
    }

    @Test
    fun changingCurrency_recalculatesExistingExpenses() = runTest {
        val repo = FakeTripRepository()
        val tripId = repo.saveTrip(trip(currency = "RUB"))
        repo.saveExpense(
            Expense(tripId = tripId, title = "Обед", amount = 8000.0, currency = "RUB",
                amountInTripCurrency = 8000.0, category = ExpenseCategory.FOOD, date = 100),
        )
        val vm = TripEditViewModel(SavedStateHandle(mapOf(Routes.ARG_TRIP_ID to tripId)), repo, currencyRepository())

        assertEquals("RUB", vm.state.value.currency)
        vm.onCurrencyChange("USD")
        vm.save()

        assertEquals("USD", repo.trips.value.single().currency)
        assertEquals(100.0, repo.expenses.value.single().amountInTripCurrency, 1e-9)
    }
}

class TripDetailViewModelTest : MainDispatcherTest() {

    @Test
    fun saveExpense_inForeignCurrency_isConvertedToTripCurrency() = runTest {
        val repo = FakeTripRepository()
        val tripId = repo.saveTrip(trip(currency = "RUB"))
        val vm = TripDetailViewModel(
            SavedStateHandle(mapOf(Routes.ARG_TRIP_ID to tripId)),
            repo,
            currencyRepository(),
            today = { 101 },
        )
        // tripState и budget активны только при подписке — подписываемся, как это делает UI.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.tripState.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.budget.collect {} }

        vm.saveExpense(
            existing = null, title = "Музей", amount = 10.0, currency = "EUR",
            category = ExpenseCategory.ENTERTAINMENT, date = 101, note = "",
        )

        val saved = repo.expenses.value.single()
        assertEquals(1000.0, saved.amountInTripCurrency, 1e-9) // 10 EUR = 12.5 USD = 1000 RUB
        assertEquals(1000.0, vm.budget.value!!.summary.spent, 1e-9)
        assertEquals(9000.0, vm.budget.value!!.summary.remaining, 1e-9)
    }

    @Test
    fun addPackingTemplate_twice_doesNotDuplicate() = runTest {
        val repo = FakeTripRepository()
        val tripId = repo.saveTrip(trip())
        val vm = TripDetailViewModel(SavedStateHandle(mapOf(Routes.ARG_TRIP_ID to tripId)), repo, currencyRepository())

        vm.addPackingTemplate()
        val count = repo.packing.value.size
        vm.addPackingTemplate()

        assertTrue(count > 0)
        assertEquals(count, repo.packing.value.size)
        assertEquals("Все вещи из базового списка уже есть", vm.message.value)
    }
}

class ConverterViewModelTest {

    @Test
    fun buildState_convertsAndProvidesRate() {
        val snapshot = CurrencyRepository.toSnapshot(testRates)
        val state = ConverterViewModel.buildState(ConverterInput("100", "EUR", "RUB"), snapshot, false, null)
        assertEquals(10_000.0, state.result!!, 1e-9)
        assertEquals(100.0, state.rate!!, 1e-9)
        assertTrue(state.quickConversions.none { it.first == "EUR" || it.first == "RUB" })
    }

    @Test
    fun buildState_invalidAmount_hasNoResult() {
        val snapshot = CurrencyRepository.toSnapshot(testRates)
        val state = ConverterViewModel.buildState(ConverterInput("abc", "USD", "RUB"), snapshot, false, null)
        assertNull(state.result)
        assertTrue(state.quickConversions.isEmpty())
    }
}
