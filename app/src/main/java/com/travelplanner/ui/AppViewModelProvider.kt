package com.travelplanner.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.travelplanner.TravelPlannerApp
import com.travelplanner.ui.screens.converter.ConverterViewModel
import com.travelplanner.ui.screens.details.TripDetailViewModel
import com.travelplanner.ui.screens.edit.TripEditViewModel
import com.travelplanner.ui.screens.trips.TripListViewModel

/** Фабрика ViewModel: передаёт в них репозитории из [com.travelplanner.data.AppContainer]. */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            TripListViewModel(app().container.tripRepository)
        }
        initializer {
            TripEditViewModel(
                savedStateHandle = createSavedStateHandle(),
                tripRepository = app().container.tripRepository,
                currencyRepository = app().container.currencyRepository,
            )
        }
        initializer {
            TripDetailViewModel(
                savedStateHandle = createSavedStateHandle(),
                tripRepository = app().container.tripRepository,
                currencyRepository = app().container.currencyRepository,
            )
        }
        initializer {
            ConverterViewModel(app().container.currencyRepository)
        }
    }
}

private fun CreationExtras.app(): TravelPlannerApp =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TravelPlannerApp
