package com.travelplanner.ui.screens.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.travelplanner.model.Trip
import com.travelplanner.ui.components.BarChart
import com.travelplanner.ui.components.BarData
import com.travelplanner.ui.components.CategoryColors
import com.travelplanner.ui.components.DonutChart
import com.travelplanner.ui.components.EmptyState
import com.travelplanner.ui.components.LabeledValue
import com.travelplanner.util.DateUtils
import com.travelplanner.util.Formatters
import kotlin.math.roundToInt

@Composable
fun StatsTab(
    trip: Trip,
    budget: BudgetUiState?,
    modifier: Modifier = Modifier,
) {
    if (budget == null || budget.statistics.count == 0) {
        Column(modifier) {
            EmptyState(
                icon = Icons.Filled.Insights,
                title = "Статистики пока нет",
                message = "Добавьте расходы — здесь появятся графики по категориям и по дням.",
            )
        }
        return
    }

    val stats = budget.statistics
    val duration = DateUtils.durationDays(trip.startDate, trip.endDate)
    // Дневной лимит «по плану»: бюджет, равномерно распределённый по дням поездки.
    val plannedPerDay = if (trip.budget > 0) trip.budget / duration else null

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "totals") {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row {
                        LabeledValue("Всего потрачено", Formatters.money(stats.total, trip.currency), Modifier.weight(1f))
                        LabeledValue("Расходов", stats.count.toString(), Modifier.weight(1f), alignEnd = true)
                    }
                    Row {
                        LabeledValue(
                            "В среднем за день",
                            Formatters.money(budget.averagePerDay, trip.currency),
                            Modifier.weight(1f),
                        )
                        LabeledValue(
                            "До поездки",
                            Formatters.money(stats.beforeTripTotal, trip.currency),
                            Modifier.weight(1f),
                            alignEnd = true,
                        )
                    }
                    stats.maxExpense?.let { max ->
                        Text(
                            "Самая крупная трата: ${max.title} — ${Formatters.money(max.amountInTripCurrency, trip.currency)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item(key = "categories") {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("По категориям", style = MaterialTheme.typography.titleMedium)
                    DonutChart(
                        values = stats.byCategory.map { it.total.toFloat() },
                        colors = stats.byCategory.map { CategoryColors.of(it.category) },
                        modifier = Modifier
                            .size(200.dp)
                            .align(Alignment.CenterHorizontally),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Итого", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(Formatters.compact(stats.total), style = MaterialTheme.typography.titleLarge)
                        }
                    }
                    stats.byCategory.forEach { share ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(CategoryColors.of(share.category)),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "${share.category.emoji} ${share.category.title}",
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "${(share.fraction * 100).roundToInt()}%",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(Formatters.money(share.total, trip.currency), style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
            }
        }

        item(key = "days") {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("По дням", style = MaterialTheme.typography.titleMedium)
                    BarChart(
                        bars = stats.byDay.map { day ->
                            BarData(
                                label = Formatters.dayMonth(day.epochDay),
                                value = day.total,
                                valueLabel = Formatters.compact(day.total),
                                highlighted = plannedPerDay != null &&
                                    day.epochDay in trip.startDate..trip.endDate &&
                                    day.total > plannedPerDay,
                            )
                        },
                    )
                    if (plannedPerDay != null) {
                        Text(
                            "Цветом выделены дни, когда траты превысили план ${Formatters.money(plannedPerDay, trip.currency)} в день",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
