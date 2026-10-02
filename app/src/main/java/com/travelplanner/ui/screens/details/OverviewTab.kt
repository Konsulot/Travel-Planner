package com.travelplanner.ui.screens.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.travelplanner.model.PackingItem
import com.travelplanner.model.Place
import com.travelplanner.model.Trip
import com.travelplanner.model.TripEvent
import com.travelplanner.ui.components.BudgetCard
import com.travelplanner.ui.screens.trips.StatusChip
import com.travelplanner.util.DateUtils
import com.travelplanner.util.Formatters

@Composable
fun OverviewTab(
    trip: Trip,
    budget: BudgetUiState?,
    events: List<TripEvent>,
    places: List<Place>,
    packingItems: List<PackingItem>,
    today: Long,
    onOpenTab: (DetailTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Ближайшие события: начиная с сегодняшнего дня (или с начала поездки), не выполненные.
    val upcomingEvents = events
        .filter { it.date >= maxOf(today, trip.startDate) && !it.isDone }
        .take(3)
    val packed = packingItems.count { it.isPacked }
    val visited = places.count { it.isVisited }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(Formatters.dateRange(trip.startDate, trip.endDate), style = MaterialTheme.typography.titleMedium)
                            Text(
                                Formatters.days(DateUtils.durationDays(trip.startDate, trip.endDate).toLong()),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    StatusChip(trip.startDate, trip.endDate, today)
                }
            }
        }

        item {
            if (budget != null) {
                BudgetCard(
                    summary = budget.summary,
                    currency = trip.currency,
                    dailyAllowance = budget.dailyAllowance,
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickStatCard(
                    icon = Icons.Filled.Luggage,
                    title = "Вещи",
                    value = if (packingItems.isEmpty()) "Список пуст" else "$packed из ${packingItems.size}",
                    progress = if (packingItems.isEmpty()) null else packed.toFloat() / packingItems.size,
                    onClick = { onOpenTab(DetailTab.PACKING) },
                    modifier = Modifier.weight(1f),
                )
                QuickStatCard(
                    icon = Icons.Filled.LocationOn,
                    title = "Места",
                    value = if (places.isEmpty()) "Не добавлены" else "$visited из ${places.size}",
                    progress = if (places.isEmpty()) null else visited.toFloat() / places.size,
                    onClick = { onOpenTab(DetailTab.PLACES) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            Card(onClick = { onOpenTab(DetailTab.ITINERARY) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Route, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Ближайшие планы", style = MaterialTheme.typography.titleMedium)
                    }
                    if (upcomingEvents.isEmpty()) {
                        Text(
                            if (events.isEmpty()) "Маршрут пока пуст — добавьте события по дням" else "Все запланированные события выполнены",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        upcomingEvents.forEach { event ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    buildString {
                                        append(Formatters.dateShort(event.date))
                                        event.time?.let { append(", ").append(Formatters.time(it)) }
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(event.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }

        if (trip.notes.isNotBlank()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                ) {
                    Row(Modifier.padding(16.dp)) {
                        Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Text(trip.notes, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickStatCard(
    icon: ImageVector,
    title: String,
    value: String,
    progress: Float?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(onClick = onClick, modifier = modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium)
            if (progress != null) {
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
