package com.travelplanner.ui.screens.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.travelplanner.data.CurrencyRepository
import com.travelplanner.data.TripRepository
import com.travelplanner.model.Expense
import com.travelplanner.model.ExpenseCategory
import com.travelplanner.model.PackingItem
import com.travelplanner.model.Place
import com.travelplanner.model.RatesSnapshot
import com.travelplanner.model.Trip
import com.travelplanner.model.TripEvent
import com.travelplanner.ui.navigation.Routes
import com.travelplanner.util.BudgetCalculator
import com.travelplanner.util.BudgetSummary
import com.travelplanner.util.CurrencyConverter
import com.travelplanner.util.DateUtils
import com.travelplanner.util.DefaultRates
import com.travelplanner.util.StatisticsCalculator
import com.travelplanner.util.TripStatistics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Загружена ли поездка: пока идёт загрузка, поездка не найдена (удалена) или готова. */
sealed interface TripLoadState {
    data object Loading : TripLoadState
    data object NotFound : TripLoadState
    data class Loaded(val trip: Trip) : TripLoadState
}

/** Всё, что нужно вкладкам «Обзор», «Расходы» и «Статистика» про деньги. */
data class BudgetUiState(
    val summary: BudgetSummary,
    val dailyAllowance: Double?,
    val averagePerDay: Double,
    val statistics: TripStatistics,
)

class TripDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val tripRepository: TripRepository,
    private val currencyRepository: CurrencyRepository,
    private val today: () -> Long = DateUtils::today,
) : ViewModel() {

    val tripId: Long = checkNotNull(savedStateHandle.get<Long>(Routes.ARG_TRIP_ID)) { "tripId is required" }

    private val started = SharingStarted.WhileSubscribed(5_000)

    val tripState: StateFlow<TripLoadState> = tripRepository.observeTrip(tripId)
        .map { trip -> if (trip == null) TripLoadState.NotFound else TripLoadState.Loaded(trip) }
        .stateIn(viewModelScope, started, TripLoadState.Loading)

    val expenses: StateFlow<List<Expense>> = tripRepository.observeExpenses(tripId)
        .stateIn(viewModelScope, started, emptyList())

    val events: StateFlow<List<TripEvent>> = tripRepository.observeEvents(tripId)
        .stateIn(viewModelScope, started, emptyList())

    val places: StateFlow<List<Place>> = tripRepository.observePlaces(tripId)
        .stateIn(viewModelScope, started, emptyList())

    val packingItems: StateFlow<List<PackingItem>> = tripRepository.observePackingItems(tripId)
        .stateIn(viewModelScope, started, emptyList())

    val rates: StateFlow<RatesSnapshot> = currencyRepository.rates
        // Eagerly: курсы нужны при сохранении расхода, даже если UI их сейчас не отображает.
        .stateIn(viewModelScope, SharingStarted.Eagerly, RatesSnapshot(DefaultRates.ratesPerUsd, null, isFallback = true))

    val budget: StateFlow<BudgetUiState?> = combine(tripState, expenses) { state, list ->
        val trip = (state as? TripLoadState.Loaded)?.trip ?: return@combine null
        val summary = BudgetCalculator.summarize(trip.budget, list)
        val now = today()
        BudgetUiState(
            summary = summary,
            dailyAllowance = BudgetCalculator.dailyAllowance(summary.remaining, trip.startDate, trip.endDate, now),
            averagePerDay = BudgetCalculator.averagePerDay(summary.spent, trip.startDate, trip.endDate, now),
            statistics = StatisticsCalculator.calculate(list, trip.startDate, trip.endDate),
        )
    }.stateIn(viewModelScope, started, null)

    /** Одноразовые сообщения для Snackbar. */
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        // Тихо обновляем курсы, чтобы расходы в другой валюте пересчитывались по актуальному курсу.
        viewModelScope.launch { currencyRepository.refreshIfStale() }
    }

    fun messageShown() {
        _message.value = null
    }

    private val currentTrip: Trip? get() = (tripState.value as? TripLoadState.Loaded)?.trip

    fun deleteTrip() {
        val trip = currentTrip ?: return
        viewModelScope.launch { tripRepository.deleteTrip(trip) }
    }

    // ---------- Расходы ----------

    /** Пересчёт суммы в валюту поездки (для предпросмотра в диалоге). */
    fun convertToTripCurrency(amount: Double, currency: String): Double? {
        val trip = currentTrip ?: return null
        return CurrencyConverter.convert(amount, currency, trip.currency, rates.value.ratesPerUsd)
    }

    fun saveExpense(
        existing: Expense?,
        title: String,
        amount: Double,
        currency: String,
        category: ExpenseCategory,
        date: Long,
        note: String,
    ) {
        val trip = currentTrip ?: return
        val converted = CurrencyConverter.convert(amount, currency, trip.currency, rates.value.ratesPerUsd)
        if (converted == null) {
            _message.value = "Нет курса для $currency — расход не сохранён"
            return
        }
        val expense = (existing ?: Expense(
            tripId = trip.id, title = "", amount = 0.0, currency = currency,
            amountInTripCurrency = 0.0, category = category, date = date,
        )).copy(
            title = title.trim(),
            amount = amount,
            currency = currency,
            amountInTripCurrency = converted,
            category = category,
            date = date,
            note = note.trim(),
        )
        viewModelScope.launch { tripRepository.saveExpense(expense) }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch { tripRepository.deleteExpense(expense) }
    }

    // ---------- Маршрут ----------

    fun saveEvent(event: TripEvent) {
        viewModelScope.launch { tripRepository.saveEvent(event.copy(tripId = tripId, title = event.title.trim())) }
    }

    fun toggleEventDone(event: TripEvent) {
        viewModelScope.launch { tripRepository.saveEvent(event.copy(isDone = !event.isDone)) }
    }

    fun deleteEvent(event: TripEvent) {
        viewModelScope.launch { tripRepository.deleteEvent(event) }
    }

    // ---------- Места ----------

    fun savePlace(place: Place) {
        viewModelScope.launch { tripRepository.savePlace(place.copy(tripId = tripId, name = place.name.trim())) }
    }

    fun togglePlaceVisited(place: Place) {
        viewModelScope.launch { tripRepository.savePlace(place.copy(isVisited = !place.isVisited)) }
    }

    fun togglePlaceWishlist(place: Place) {
        viewModelScope.launch { tripRepository.savePlace(place.copy(isWishlist = !place.isWishlist)) }
    }

    fun deletePlace(place: Place) {
        viewModelScope.launch { tripRepository.deletePlace(place) }
    }

    // ---------- Вещи ----------

    fun savePackingItem(item: PackingItem) {
        viewModelScope.launch {
            tripRepository.savePackingItem(item.copy(tripId = tripId, name = item.name.trim()))
        }
    }

    fun togglePacked(item: PackingItem) {
        viewModelScope.launch { tripRepository.savePackingItem(item.copy(isPacked = !item.isPacked)) }
    }

    fun deletePackingItem(item: PackingItem) {
        viewModelScope.launch { tripRepository.deletePackingItem(item) }
    }

    fun addPackingTemplate() {
        viewModelScope.launch {
            val added = tripRepository.addPackingTemplate(tripId)
            _message.value = if (added > 0) "Добавлено вещей: $added" else "Все вещи из базового списка уже есть"
        }
    }
}
