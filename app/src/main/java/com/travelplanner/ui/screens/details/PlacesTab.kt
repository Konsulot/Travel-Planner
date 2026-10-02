package com.travelplanner.ui.screens.details

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.travelplanner.model.Place
import com.travelplanner.ui.components.EmptyState
import com.travelplanner.ui.theme.StatusColors

enum class PlaceFilter(val title: String) {
    ALL("Все"),
    WISHLIST("Wishlist"),
    TO_VISIT("Не посещены"),
    VISITED("Посещены"),
}

fun filterPlaces(places: List<Place>, filter: PlaceFilter): List<Place> = when (filter) {
    PlaceFilter.ALL -> places
    PlaceFilter.WISHLIST -> places.filter { it.isWishlist }
    PlaceFilter.TO_VISIT -> places.filter { !it.isVisited }
    PlaceFilter.VISITED -> places.filter { it.isVisited }
}

@Composable
fun PlacesTab(
    places: List<Place>,
    onPlaceClick: (Place) -> Unit,
    onToggleVisited: (Place) -> Unit,
    onToggleWishlist: (Place) -> Unit,
    onAddPlace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var filter by rememberSaveable { mutableStateOf(PlaceFilter.ALL) }
    val context = LocalContext.current
    val filtered = filterPlaces(places, filter)

    fun openOnMap(place: Place) {
        val query = listOf(place.name, place.address).filter { it.isNotBlank() }.joinToString(", ")
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(query)))
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // Нет приложения карт — открываем поиск в браузере.
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(query))),
                )
            }
        }
    }

    if (places.isEmpty()) {
        Column(modifier) {
            EmptyState(
                icon = Icons.Filled.LocationOn,
                title = "Мест пока нет",
                message = "Собирайте здесь достопримечательности, кафе и музеи. Отмечайте звёздочкой самое важное (wishlist) и ставьте галочку, когда побываете.",
            ) {
                Button(onClick = onAddPlace) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Добавить место")
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "filters") {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PlaceFilter.entries.forEach { option ->
                    FilterChip(
                        selected = filter == option,
                        onClick = { filter = option },
                        label = { Text(option.title, maxLines = 1) },
                    )
                }
            }
        }
        if (filtered.isEmpty()) {
            item(key = "nothing") {
                Text(
                    "Нет мест в этом фильтре",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }
        }
        items(filtered, key = { it.id }) { place ->
            PlaceCard(
                place = place,
                onClick = { onPlaceClick(place) },
                onToggleVisited = { onToggleVisited(place) },
                onToggleWishlist = { onToggleWishlist(place) },
                onOpenMap = { openOnMap(place) },
            )
        }
    }
}

@Composable
private fun PlaceCard(
    place: Place,
    onClick: () -> Unit,
    onToggleVisited: () -> Unit,
    onToggleWishlist: () -> Unit,
    onOpenMap: () -> Unit,
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onToggleVisited) {
                Icon(
                    if (place.isVisited) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                    contentDescription = if (place.isVisited) "Отметить как непосещённое" else "Отметить как посещённое",
                    tint = if (place.isVisited) StatusColors.ok else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    place.name,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (place.isVisited) TextDecoration.LineThrough else null,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (place.address.isNotBlank()) {
                    Text(
                        place.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (place.note.isNotBlank()) {
                    Text(
                        place.note,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(onClick = onOpenMap) {
                Icon(Icons.Filled.Map, contentDescription = "Открыть на карте")
            }
            IconButton(onClick = onToggleWishlist) {
                Icon(
                    if (place.isWishlist) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = if (place.isWishlist) "Убрать из wishlist" else "Добавить в wishlist",
                    tint = if (place.isWishlist) StatusColors.warning else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
