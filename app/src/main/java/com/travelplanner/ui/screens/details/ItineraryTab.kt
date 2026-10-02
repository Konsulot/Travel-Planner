package com.travelplanner.ui.screens.details

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.travelplanner.model.Trip
import com.travelplanner.model.TripEvent
import com.travelplanner.util.DateUtils
import com.travelplanner.util.Formatters

/** Маршрут по дням: для каждого дня поездки — список событий по времени. */
@Composable
fun ItineraryTab(
    trip: Trip,
    events: List<TripEvent>,
    today: Long,
    onEventClick: (TripEvent) -> Unit,
    onToggleDone: (TripEvent) -> Unit,
    onAddEvent: (day: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val eventsByDay = events.groupBy { it.date }
    val tripDays = DateUtils.tripDays(trip.startDate, trip.endDate)
    // События, которые оказались вне дат поездки (например, после изменения дат), тоже показываем.
    val tripDaySet = tripDays.toSet()
    val outsideDays = eventsByDay.keys.filter { it !in tripDaySet }.sorted()

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(tripDays, key = { "day_$it" }) { day ->
            DayCard(
                title = "День ${DateUtils.dayNumber(day, trip.startDate)}",
                subtitle = Formatters.dayHeader(day),
                isToday = day == today,
                events = eventsByDay[day].orEmpty(),
                onEventClick = onEventClick,
                onToggleDone = onToggleDone,
                onAdd = { onAddEvent(day) },
            )
        }
        items(outsideDays, key = { "outside_$it" }) { day ->
            DayCard(
                title = "Вне дат поездки",
                subtitle = Formatters.dayHeader(day),
                isToday = day == today,
                events = eventsByDay[day].orEmpty(),
                onEventClick = onEventClick,
                onToggleDone = onToggleDone,
                onAdd = null,
            )
        }
    }
}

@Composable
private fun DayCard(
    title: String,
    subtitle: String,
    isToday: Boolean,
    events: List<TripEvent>,
    onEventClick: (TripEvent) -> Unit,
    onToggleDone: (TripEvent) -> Unit,
    onAdd: (() -> Unit)?,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (isToday) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Column(Modifier.padding(vertical = 12.dp)) {
            Row(
                modifier = Modifier.padding(start = 16.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (isToday) "$title · сегодня" else title,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (onAdd != null) {
                    TextButton(onClick = onAdd) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Добавить")
                    }
                }
            }
            if (events.isEmpty()) {
                Text(
                    "Свободный день",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            } else {
                events.forEach { event ->
                    EventRow(event, onClick = { onEventClick(event) }, onToggleDone = { onToggleDone(event) })
                }
            }
        }
    }
}

@Composable
private fun EventRow(event: TripEvent, onClick: () -> Unit, onToggleDone: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            event.time?.let { Formatters.time(it) } ?: "—",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(52.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                event.title,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (event.isDone) TextDecoration.LineThrough else null,
                color = if (event.isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (event.location.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        event.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Checkbox(checked = event.isDone, onCheckedChange = { onToggleDone() })
    }
}
