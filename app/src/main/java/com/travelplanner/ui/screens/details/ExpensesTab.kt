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
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.travelplanner.model.Expense
import com.travelplanner.model.Trip
import com.travelplanner.ui.components.BudgetCard
import com.travelplanner.ui.components.EmojiBadge
import com.travelplanner.ui.components.EmptyState
import com.travelplanner.ui.components.SectionHeader
import com.travelplanner.util.Formatters

@Composable
fun ExpensesTab(
    trip: Trip,
    expenses: List<Expense>,
    budget: BudgetUiState?,
    onExpenseClick: (Expense) -> Unit,
    onAddExpense: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Список уже отсортирован в DAO по дате (новые сверху) — группируем с сохранением порядка.
    val grouped = expenses.groupBy { it.date }

    LazyColumn(
        modifier = modifier.testTag("expenses_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
    ) {
        if (budget != null) {
            item(key = "budget") {
                BudgetCard(budget.summary, trip.currency, budget.dailyAllowance)
            }
        }

        if (expenses.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    title = "Расходов пока нет",
                    message = "Записывайте траты прямо в поездке — бюджет и остаток пересчитаются автоматически. " +
                        "Можно вводить суммы в местной валюте.",
                ) {
                    Button(onClick = onAddExpense) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Добавить расход")
                    }
                }
            }
        }

        grouped.forEach { (date, dayExpenses) ->
            item(key = "date_$date") {
                SectionHeader(
                    text = Formatters.dayHeader(date),
                    trailing = {
                        Text(
                            Formatters.money(dayExpenses.sumOf { it.amountInTripCurrency }, trip.currency),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
            }
            items(dayExpenses, key = { it.id }) { expense ->
                ExpenseRow(expense, trip.currency, onClick = { onExpenseClick(expense) })
            }
        }
    }
}

@Composable
private fun ExpenseRow(expense: Expense, tripCurrency: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EmojiBadge(expense.category.emoji)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(expense.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (expense.note.isBlank()) expense.category.title else "${expense.category.title} · ${expense.note}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                Formatters.money(expense.amountInTripCurrency, tripCurrency),
                style = MaterialTheme.typography.titleSmall,
            )
            if (expense.currency != tripCurrency) {
                Text(
                    Formatters.money(expense.amount, expense.currency),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
