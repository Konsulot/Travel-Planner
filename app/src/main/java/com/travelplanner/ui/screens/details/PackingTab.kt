package com.travelplanner.ui.screens.details

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.travelplanner.model.PackingCategory
import com.travelplanner.model.PackingItem
import com.travelplanner.ui.components.SectionHeader
import com.travelplanner.ui.components.EmptyState

@Composable
fun PackingTab(
    items: List<PackingItem>,
    onToggle: (PackingItem) -> Unit,
    onItemClick: (PackingItem) -> Unit,
    onDelete: (PackingItem) -> Unit,
    onAddTemplate: () -> Unit,
    onAddItem: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) {
        Column(modifier) {
            EmptyState(
                icon = Icons.Filled.Luggage,
                title = "Список вещей пуст",
                message = "Начните с базового списка (документы, одежда, аптечка, электроника) и дополните своими вещами.",
            ) {
                Button(onClick = onAddTemplate, modifier = Modifier.testTag("packing_template")) {
                    Icon(Icons.Filled.Checklist, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Заполнить базовым списком")
                }
                OutlinedButton(onClick = onAddItem) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Добавить вещь")
                }
            }
        }
        return
    }

    val packed = items.count { it.isPacked }
    // Группы в порядке объявления категорий, внутри — сначала несобранные.
    val groups = items
        .groupBy { it.category }
        .toSortedMap(compareBy<PackingCategory> { it.ordinal })
        .mapValues { (_, list) -> list.sortedBy { it.isPacked } }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
    ) {
        item(key = "progress") {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (packed == items.size) "Всё собрано! 🎉" else "Собрано $packed из ${items.size}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = onAddTemplate) { Text("+ базовый список") }
                    }
                    LinearProgressIndicator(
                        progress = { packed.toFloat() / items.size },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        groups.forEach { (category, list) ->
            item(key = "cat_${category.name}") {
                SectionHeader("${category.emoji} ${category.title}")
            }
            items(list, key = { it.id }) { item ->
                PackingRow(
                    item = item,
                    onToggle = { onToggle(item) },
                    onClick = { onItemClick(item) },
                    onDelete = { onDelete(item) },
                )
            }
        }
    }
}

@Composable
private fun PackingRow(item: PackingItem, onToggle: () -> Unit, onClick: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = item.isPacked, onCheckedChange = { onToggle() })
        Text(
            if (item.quantity > 1) "${item.name} × ${item.quantity}" else item.name,
            style = MaterialTheme.typography.bodyLarge,
            textDecoration = if (item.isPacked) TextDecoration.LineThrough else null,
            color = if (item.isPacked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Close, contentDescription = "Удалить ${item.name}", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
