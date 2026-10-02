package com.travelplanner.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.travelplanner.model.Trip
import com.travelplanner.model.TripWithSpent
import com.travelplanner.ui.screens.edit.TripEditContent
import com.travelplanner.ui.screens.edit.TripFormState
import com.travelplanner.ui.screens.trips.TripListContent
import com.travelplanner.ui.screens.trips.TripListUiState
import com.travelplanner.ui.theme.TravelPlannerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** UI-тесты Compose: проверяют отображение и реакцию экранов на действия пользователя. */
@RunWith(AndroidJUnit4::class)
class TripScreensTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val today = 20_000L

    @Test
    fun emptyList_showsEmptyStateAndCreateButton() {
        var addClicked = false
        composeRule.setContent {
            TravelPlannerTheme {
                TripListContent(
                    uiState = TripListUiState(isLoading = false),
                    today = today,
                    onTripClick = {},
                    onAddTrip = { addClicked = true },
                    onAddDemo = {},
                    onOpenConverter = {},
                )
            }
        }

        composeRule.onNodeWithText("Пока нет поездок").assertIsDisplayed()
        composeRule.onNodeWithTag("create_first_trip").performClick()
        assertTrue(addClicked)
    }

    @Test
    fun tripList_showsTripsAndOpensDetails() {
        var openedId: Long? = null
        val trip = Trip(
            id = 7, title = "Стамбул", destination = "Турция", startDate = today + 3, endDate = today + 8,
            budget = 60_000.0, currency = "RUB",
        )
        composeRule.setContent {
            TravelPlannerTheme {
                TripListContent(
                    uiState = TripListUiState(activeTrips = listOf(TripWithSpent(trip, 15_000.0)), isLoading = false),
                    today = today,
                    onTripClick = { openedId = it },
                    onAddTrip = {},
                    onAddDemo = {},
                    onOpenConverter = {},
                )
            }
        }

        composeRule.onNodeWithText("Стамбул").assertIsDisplayed()
        composeRule.onNodeWithText("Через 3 дня").assertIsDisplayed()
        composeRule.onNodeWithText("Потрачено 15 000 ₽ из 60 000 ₽").assertIsDisplayed()
        composeRule.onNodeWithText("Стамбул").performClick()
        assertEquals(7L, openedId)
    }

    @Test
    fun tripForm_showsValidationErrorsAndForwardsInput() {
        var typedTitle = ""
        var saveClicked = false
        composeRule.setContent {
            TravelPlannerTheme {
                TripEditContent(
                    state = TripFormState(
                        errors = com.travelplanner.util.TripFormErrors(title = "Введите название поездки"),
                    ),
                    isNewTrip = true,
                    onBack = {},
                    onTitleChange = { typedTitle = it },
                    onDestinationChange = {},
                    onStartDateChange = {},
                    onEndDateChange = {},
                    onBudgetChange = {},
                    onCurrencyChange = {},
                    onNotesChange = {},
                    onSave = { saveClicked = true },
                )
            }
        }

        composeRule.onNodeWithText("Введите название поездки").assertIsDisplayed()
        composeRule.onNodeWithTag("field_title").performTextInput("Казань")
        assertEquals("Казань", typedTitle)
        composeRule.onNodeWithTag("save_trip").performClick()
        assertTrue(saveClicked)
    }
}
