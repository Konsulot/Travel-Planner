package com.travelplanner.ui.screens.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.travelplanner.data.CurrencyRepository
import com.travelplanner.data.TripRepository
import com.travelplanner.model.Trip
import com.travelplanner.ui.navigation.Routes
import com.travelplanner.util.Currencies
import com.travelplanner.util.Formatters
import com.travelplanner.util.TripFormErrors
import com.travelplanner.util.Validators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TripFormState(
    val title: String = "",
    val destination: String = "",
    val startDate: Long? = null,
    val endDate: Long? = null,
    val budgetText: String = "",
    val currency: String = Currencies.DEFAULT,
    val notes: String = "",
    val errors: TripFormErrors = TripFormErrors(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    /** Заполняется после успешного сохранения — экран по нему уходит назад. */
    val savedTripId: Long? = null,
)

class TripEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val tripRepository: TripRepository,
    private val currencyRepository: CurrencyRepository,
) : ViewModel() {

    private val tripId: Long = savedStateHandle.get<Long>(Routes.ARG_TRIP_ID) ?: Routes.NEW_TRIP_ID
    val isNewTrip: Boolean = tripId == Routes.NEW_TRIP_ID

    private var originalTrip: Trip? = null

    private val _state = MutableStateFlow(TripFormState(isLoading = !isNewTrip))
    val state: StateFlow<TripFormState> = _state.asStateFlow()

    init {
        if (!isNewTrip) {
            viewModelScope.launch {
                val trip = tripRepository.getTrip(tripId)
                originalTrip = trip
                _state.update {
                    if (trip == null) {
                        it.copy(isLoading = false)
                    } else {
                        it.copy(
                            title = trip.title,
                            destination = trip.destination,
                            startDate = trip.startDate,
                            endDate = trip.endDate,
                            budgetText = Formatters.number(trip.budget).replace(" ", ""),
                            currency = trip.currency,
                            notes = trip.notes,
                            isLoading = false,
                        )
                    }
                }
            }
        }
    }

    // Изменение поля сбрасывает только его собственную ошибку.
    fun onTitleChange(value: String) = _state.update { it.copy(title = value, errors = it.errors.copy(title = null)) }

    fun onDestinationChange(value: String) =
        _state.update { it.copy(destination = value, errors = it.errors.copy(destination = null)) }

    fun onStartDateChange(value: Long) = _state.update { s ->
        // Если начало сдвинули позже окончания — подтягиваем окончание, чтобы не ловить ошибку.
        val end = s.endDate?.takeIf { it >= value } ?: value
        s.copy(startDate = value, endDate = end, errors = s.errors.copy(startDate = null, endDate = null))
    }

    fun onEndDateChange(value: Long) = _state.update { it.copy(endDate = value, errors = it.errors.copy(endDate = null)) }

    fun onBudgetChange(value: String) {
        if (value.length > 18) return
        _state.update { it.copy(budgetText = value, errors = it.errors.copy(budget = null)) }
    }

    fun onCurrencyChange(value: String) = _state.update { it.copy(currency = value) }

    fun onNotesChange(value: String) = _state.update { it.copy(notes = value) }

    fun save() {
        val s = _state.value
        if (s.isSaving) return
        val errors = Validators.validateTrip(s.title, s.destination, s.startDate, s.endDate, s.budgetText)
        if (errors.hasErrors) {
            _state.update { it.copy(errors = errors) }
            return
        }
        _state.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            val base = originalTrip
            val trip = Trip(
                id = base?.id ?: 0,
                title = s.title.trim(),
                destination = s.destination.trim(),
                startDate = s.startDate!!,
                endDate = s.endDate!!,
                budget = Formatters.parseAmount(s.budgetText) ?: 0.0,
                currency = s.currency,
                notes = s.notes.trim(),
                createdAt = base?.createdAt ?: System.currentTimeMillis(),
            )
            val savedId = tripRepository.saveTrip(trip)

            // Валюту поездки поменяли — пересчитываем уже внесённые расходы в новую валюту.
            if (base != null && base.currency != trip.currency) {
                val rates = currencyRepository.currentRates().ratesPerUsd
                tripRepository.recalculateExpenses(savedId, trip.currency, rates)
            }
            _state.update { it.copy(isSaving = false, savedTripId = savedId) }
        }
    }
}
