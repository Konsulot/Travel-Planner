package com.travelplanner.ui.screens.trips

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.travelplanner.model.TripWithSpent
import com.travelplanner.ui.AppViewModelProvider
import com.travelplanner.ui.components.BudgetProgressBar
import com.travelplanner.ui.components.EmptyState
import com.travelplanner.ui.components.LoadingBox
import com.travelplanner.ui.components.SectionHeader
import com.travelplanner.ui.theme.StatusColors
import com.travelplanner.util.BudgetSummary
import com.travelplanner.util.DateUtils
import com.travelplanner.util.Formatters
import com.travelplanner.util.TripStatus

@Composable
fun TripListScreen(
    onTripClick: (Long) -> Unit,
    onAddTrip: () -> Unit,
    onOpenConverter: () -> Unit,
    viewModel: TripListViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TripListContent(
        uiState = uiState,
        today = DateUtils.today(),
        onTripClick = onTripClick,
        onAddTrip = onAddTrip,
        onAddDemo = viewModel::addDemoTrip,
        onOpenConverter = onOpenConverter,
    )
}

/** Экран без ViewModel — удобно для превью и UI-тестов. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripListContent(
    uiState: TripListUiState,
    today: Long,
    onTripClick: (Long) -> Unit,
    onAddTrip: () -> Unit,
    onAddDemo: () -> Unit,
    onOpenConverter: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            androidx.compose.material3.LargeTopAppBar(
                title = { Text("Мои поездки") },
                actions = {
                    IconButton(onClick = onOpenConverter) {
                        Icon(Icons.Filled.CurrencyExchange, contentDescription = "Конвертер валют")
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            if (!uiState.isEmpty && !uiState.isLoading) {
                ExtendedFloatingActionButton(
                    onClick = onAddTrip,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Новая поездка") },
                    modifier = Modifier.testTag("add_trip_fab"),
                )
            }
        },
    ) { padding ->
        when {
            uiState.isLoading -> LoadingBox(Modifier.padding(padding))

            uiState.isEmpty -> Column(Modifier.padding(padding).fillMaxSize(), verticalArrangement = Arrangement.Center) {
                EmptyState(
                    icon = Icons.Filled.FlightTakeoff,
                    title = "Пока нет поездок",
                    message = "Создайте первую поездку: укажите место, даты и бюджет — а дальше маршрут, расходы и сборы.",
                ) {
                    Button(onClick = onAddTrip, modifier = Modifier.testTag("create_first_trip")) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Создать поездку")
                    }
                    OutlinedButton(onClick = onAddDemo) { Text("Добавить пример поездки") }
                }
            }

            else -> LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .testTag("trip_list"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (uiState.activeTrips.isNotEmpty()) {
                    item(key = "header_active") { SectionHeader("Текущие и предстоящие") }
                    items(uiState.activeTrips, key = { it.trip.id }) { item ->
                        TripCard(item, today, onClick = { onTripClick(item.trip.id) })
                    }
                }
                if (uiState.finishedTrips.isNotEmpty()) {
                    item(key = "header_finished") { SectionHeader("Завершённые") }
                    items(uiState.finishedTrips, key = { it.trip.id }) { item ->
                        TripCard(item, today, onClick = { onTripClick(item.trip.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun TripCard(item: TripWithSpent, today: Long, onClick: () -> Unit) {
    val trip = item.trip
    val summary = BudgetSummary(trip.budget, item.spent)
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    trip.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                StatusChip(trip.startDate, trip.endDate, today)
            }
            IconText(Icons.Filled.LocationOn, trip.destination)
            IconText(
                Icons.Filled.CalendarMonth,
                "${Formatters.dateRange(trip.startDate, trip.endDate)} · " +
                    Formatters.days(DateUtils.durationDays(trip.startDate, trip.endDate).toLong()),
            )
            Spacer(Modifier.height(4.dp))
            BudgetProgressBar(summary)
            Text(
                "Потрачено ${Formatters.money(summary.spent, trip.currency)} из ${Formatters.money(summary.budget, trip.currency)}",
                style = MaterialTheme.typography.bodySmall,
                color = if (summary.isOverBudget) StatusColors.danger else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun IconText(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(6.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Человекочитаемый статус поездки: «Через 5 дней», «В пути · день 2 из 7», «Завершена». */
fun tripStatusLabel(start: Long, end: Long, today: Long): String =
    when (DateUtils.status(start, end, today)) {
        TripStatus.UPCOMING -> {
            val days = DateUtils.daysUntil(start, today)
            if (days == 1L) "Завтра" else "Через ${Formatters.days(days)}"
        }
        TripStatus.ONGOING ->
            "В пути · день ${DateUtils.dayNumber(today, start)} из ${DateUtils.durationDays(start, end)}"
        TripStatus.FINISHED -> "Завершена"
    }

@Composable
fun StatusChip(start: Long, end: Long, today: Long) {
    val status = DateUtils.status(start, end, today)
    val container = when (status) {
        TripStatus.ONGOING -> MaterialTheme.colorScheme.primaryContainer
        TripStatus.UPCOMING -> MaterialTheme.colorScheme.tertiaryContainer
        TripStatus.FINISHED -> MaterialTheme.colorScheme.surfaceVariant
    }
    AssistChip(
        onClick = {},
        label = { Text(tripStatusLabel(start, end, today), maxLines = 1) },
        colors = AssistChipDefaults.assistChipColors(containerColor = container),
        border = null,
    )
}
