package com.travelplanner.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.travelplanner.ui.screens.converter.ConverterScreen
import com.travelplanner.ui.screens.details.TripDetailScreen
import com.travelplanner.ui.screens.edit.TripEditScreen
import com.travelplanner.ui.screens.trips.TripListScreen

/** Маршруты экранов приложения. */
object Routes {
    const val TRIPS = "trips"
    const val CONVERTER = "converter"

    const val ARG_TRIP_ID = "tripId"
    const val TRIP_DETAIL = "trip/{$ARG_TRIP_ID}"
    const val TRIP_EDIT = "trip_edit?$ARG_TRIP_ID={$ARG_TRIP_ID}"

    /** Значение аргумента, означающее «новая поездка». */
    const val NEW_TRIP_ID = -1L

    fun tripDetail(tripId: Long) = "trip/$tripId"
    fun tripEdit(tripId: Long? = null) = if (tripId == null) "trip_edit" else "trip_edit?$ARG_TRIP_ID=$tripId"
}

@Composable
fun TravelPlannerNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = Routes.TRIPS,
        modifier = modifier,
    ) {
        composable(Routes.TRIPS) {
            TripListScreen(
                onTripClick = { navController.navigate(Routes.tripDetail(it)) },
                onAddTrip = { navController.navigate(Routes.tripEdit()) },
                onOpenConverter = { navController.navigate(Routes.CONVERTER) },
            )
        }

        composable(
            route = Routes.TRIP_EDIT,
            arguments = listOf(
                navArgument(Routes.ARG_TRIP_ID) {
                    type = NavType.LongType
                    defaultValue = Routes.NEW_TRIP_ID
                },
            ),
        ) {
            TripEditScreen(
                onBack = { navController.popBackStack() },
                onSaved = { tripId, isNew ->
                    if (isNew) {
                        // После создания сразу открываем поездку, а форму убираем из стека.
                        navController.navigate(Routes.tripDetail(tripId)) {
                            popUpTo(Routes.TRIPS)
                        }
                    } else {
                        navController.popBackStack()
                    }
                },
            )
        }

        composable(
            route = Routes.TRIP_DETAIL,
            arguments = listOf(navArgument(Routes.ARG_TRIP_ID) { type = NavType.LongType }),
        ) {
            TripDetailScreen(
                onBack = { navController.popBackStack() },
                onEditTrip = { navController.navigate(Routes.tripEdit(it)) },
                onOpenConverter = { navController.navigate(Routes.CONVERTER) },
            )
        }

        composable(Routes.CONVERTER) {
            ConverterScreen(onBack = { navController.popBackStack() })
        }
    }
}
