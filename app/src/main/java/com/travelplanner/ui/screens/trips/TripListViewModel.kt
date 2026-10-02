package com.travelplanner.ui.screens.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.travelplanner.data.DemoData
import com.travelplanner.data.TripRepository
import com.travelplanner.model.TripWithSpent
import com.travelplanner.util.DateUtils
import com.travelplanner.util.TripStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TripListUiState(
    val activeTrips: List<TripWithSpent> = emptyList(),
    val finishedTrips: List<TripWithSpent> = emptyList(),
    val isLoading: Boolean = true,
) {
    val isEmpty: Boolean get() = !isLoading && activeTrips.isEmpty() && finishedTrips.isEmpty()
}

class TripListViewModel(
    private val repository: TripRepository,
    private val today: () -> Long = DateUtils::today,
) : ViewModel() {

    val uiState: StateFlow<TripListUiState> = repository.observeTrips()
        .map { trips -> buildState(trips, today()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TripListUiState(isLoading = true),
        )

    fun addDemoTrip() {
        viewModelScope.launch { DemoData.createDemoTrip(repository, today()) }
    }

    companion object {
        /** Текущие и будущие поездки — по дате начала, завершённые — сначала самые свежие. */
        fun buildState(trips: List<TripWithSpent>, today: Long): TripListUiState {
            val (finished, active) = trips.partition {
                DateUtils.status(it.trip.startDate, it.trip.endDate, today) == TripStatus.FINISHED
            }
            return TripListUiState(
                activeTrips = active.sortedBy { it.trip.startDate },
                finishedTrips = finished.sortedByDescending { it.trip.endDate },
                isLoading = false,
            )
        }
    }
}
