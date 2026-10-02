package com.travelplanner.ui.screens.details

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.travelplanner.model.Expense
import com.travelplanner.model.ExpenseCategory
import com.travelplanner.model.PackingItem
import com.travelplanner.model.Place
import com.travelplanner.model.TripEvent
import com.travelplanner.ui.AppViewModelProvider
import com.travelplanner.ui.components.ConfirmDialog
import com.travelplanner.ui.components.LoadingBox
import com.travelplanner.util.DateUtils
import com.travelplanner.util.TripStatus

enum class DetailTab(val title: String, val icon: ImageVector, val fabLabel: String?) {
    OVERVIEW("Обзор", Icons.Filled.Dashboard, null),
    EXPENSES("Расходы", Icons.AutoMirrored.Filled.ReceiptLong, "Расход"),
    ITINERARY("Маршрут", Icons.Filled.Route, "Событие"),
    PLACES("Места", Icons.Filled.LocationOn, "Место"),
    PACKING("Вещи", Icons.Filled.Luggage, "Вещь"),
    STATS("Статистика", Icons.Filled.Insights, null),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    onBack: () -> Unit,
    onEditTrip: (Long) -> Unit,
    onOpenConverter: () -> Unit,
    viewModel: TripDetailViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val tripState by viewModel.tripState.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val events by viewModel.events.collectAsStateWithLifecycle()
    val places by viewModel.places.collectAsStateWithLifecycle()
    val packingItems by viewModel.packingItems.collectAsStateWithLifecycle()
    val budget by viewModel.budget.collectAsStateWithLifecycle()
    val rates by viewModel.rates.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    val trip = when (val state = tripState) {
        TripLoadState.Loading -> {
            LoadingBox()
            return
        }
        TripLoadState.NotFound -> {
            // Поездку удалили (или открыли несуществующую) — возвращаемся к списку.
            LaunchedEffect(Unit) { onBack() }
            Box(Modifier.fillMaxSize())
            return
        }
        is TripLoadState.Loaded -> state.trip
    }

    var selectedTab by rememberSaveable { mutableStateOf(DetailTab.OVERVIEW) }
    var menuExpanded by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    // Черновики для диалогов: null — диалог закрыт, id == 0 — создание, иначе редактирование.
    var expenseDraft by remember { mutableStateOf<Expense?>(null) }
    var eventDraft by remember { mutableStateOf<TripEvent?>(null) }
    var placeDraft by remember { mutableStateOf<Place?>(null) }
    var packingDraft by remember { mutableStateOf<PackingItem?>(null) }

    val today = DateUtils.today()
    val defaultDay = if (DateUtils.status(trip.startDate, trip.endDate, today) == TripStatus.ONGOING) today else trip.startDate

    fun newExpense() = Expense(
        tripId = trip.id, title = "", amount = 0.0, currency = trip.currency,
        amountInTripCurrency = 0.0, category = ExpenseCategory.FOOD, date = today,
    )
    fun newEvent(day: Long) = TripEvent(tripId = trip.id, date = day, title = "")

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(trip.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                trip.destination,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                        }
                    },
                    actions = {
                        IconButton(onClick = { onEditTrip(trip.id) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Редактировать поездку")
                        }
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "Ещё")
                            }
                            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                DropdownMenuItem(
                                    text = { Text("Конвертер валют") },
                                    leadingIcon = { Icon(Icons.Filled.CurrencyExchange, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        onOpenConverter()
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Удалить поездку") },
                                    leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        confirmDelete = true
                                    },
                                )
                            }
                        }
                    },
                )
                ScrollableTabRow(selectedTabIndex = selectedTab.ordinal, edgePadding = 8.dp) {
                    DetailTab.entries.forEach { tab ->
                        Tab(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            text = { Text(tab.title) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            modifier = Modifier.testTag("tab_${tab.name.lowercase()}"),
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            val label = selectedTab.fabLabel
            if (label != null) {
                ExtendedFloatingActionButton(
                    onClick = {
                        when (selectedTab) {
                            DetailTab.EXPENSES -> expenseDraft = newExpense()
                            DetailTab.ITINERARY -> eventDraft = newEvent(defaultDay)
                            DetailTab.PLACES -> placeDraft = Place(tripId = trip.id, name = "")
                            DetailTab.PACKING -> packingDraft = PackingItem(tripId = trip.id, name = "")
                            else -> Unit
                        }
                    },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(label) },
                    modifier = Modifier.testTag("detail_fab"),
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        val contentModifier = Modifier
            .padding(padding)
            .fillMaxSize()
        when (selectedTab) {
            DetailTab.OVERVIEW -> OverviewTab(
                trip = trip,
                budget = budget,
                events = events,
                places = places,
                packingItems = packingItems,
                today = today,
                onOpenTab = { selectedTab = it },
                modifier = contentModifier,
            )
            DetailTab.EXPENSES -> ExpensesTab(
                trip = trip,
                expenses = expenses,
                budget = budget,
                onExpenseClick = { expenseDraft = it },
                onAddExpense = { expenseDraft = newExpense() },
                modifier = contentModifier,
            )
            DetailTab.ITINERARY -> ItineraryTab(
                trip = trip,
                events = events,
                today = today,
                onEventClick = { eventDraft = it },
                onToggleDone = viewModel::toggleEventDone,
                onAddEvent = { day -> eventDraft = newEvent(day) },
                modifier = contentModifier,
            )
            DetailTab.PLACES -> PlacesTab(
                places = places,
                onPlaceClick = { placeDraft = it },
                onToggleVisited = viewModel::togglePlaceVisited,
                onToggleWishlist = viewModel::togglePlaceWishlist,
                onAddPlace = { placeDraft = Place(tripId = trip.id, name = "") },
                modifier = contentModifier,
            )
            DetailTab.PACKING -> PackingTab(
                items = packingItems,
                onToggle = viewModel::togglePacked,
                onItemClick = { packingDraft = it },
                onDelete = viewModel::deletePackingItem,
                onAddTemplate = viewModel::addPackingTemplate,
                onAddItem = { packingDraft = PackingItem(tripId = trip.id, name = "") },
                modifier = contentModifier,
            )
            DetailTab.STATS -> StatsTab(
                trip = trip,
                budget = budget,
                modifier = contentModifier,
            )
        }
    }

    // ---------- Диалоги ----------

    expenseDraft?.let { draft ->
        ExpenseDialog(
            draft = draft,
            trip = trip,
            ratesAreFallback = rates.isFallback,
            convert = viewModel::convertToTripCurrency,
            onDismiss = { expenseDraft = null },
            onSave = { title, amount, currency, category, date, note ->
                viewModel.saveExpense(draft, title, amount, currency, category, date, note)
                expenseDraft = null
            },
            onDelete = {
                viewModel.deleteExpense(draft)
                expenseDraft = null
            },
        )
    }

    eventDraft?.let { draft ->
        EventDialog(
            draft = draft,
            trip = trip,
            onDismiss = { eventDraft = null },
            onSave = {
                viewModel.saveEvent(it)
                eventDraft = null
            },
            onDelete = {
                viewModel.deleteEvent(draft)
                eventDraft = null
            },
        )
    }

    placeDraft?.let { draft ->
        PlaceDialog(
            draft = draft,
            onDismiss = { placeDraft = null },
            onSave = {
                viewModel.savePlace(it)
                placeDraft = null
            },
            onDelete = {
                viewModel.deletePlace(draft)
                placeDraft = null
            },
        )
    }

    packingDraft?.let { draft ->
        PackingDialog(
            draft = draft,
            onDismiss = { packingDraft = null },
            onSave = {
                viewModel.savePackingItem(it)
                packingDraft = null
            },
            onDelete = {
                viewModel.deletePackingItem(draft)
                packingDraft = null
            },
        )
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Удалить поездку?",
            message = "«${trip.title}» будет удалена вместе с расходами, маршрутом, местами и списком вещей. Это действие нельзя отменить.",
            confirmText = "Удалить",
            onConfirm = {
                confirmDelete = false
                viewModel.deleteTrip()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}
