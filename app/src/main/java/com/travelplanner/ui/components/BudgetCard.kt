package com.travelplanner.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.travelplanner.ui.theme.StatusColors
import com.travelplanner.util.BudgetSummary
import com.travelplanner.util.Formatters

/** Цвет индикатора бюджета: зелёный → оранжевый (от 80%) → красный (перерасход). */
@Composable
fun budgetColor(summary: BudgetSummary): Color = when {
    summary.isOverBudget -> StatusColors.danger
    summary.progress >= 0.8f -> StatusColors.warning
    else -> MaterialTheme.colorScheme.primary
}

@Composable
fun BudgetProgressBar(summary: BudgetSummary, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { summary.progress },
        color = budgetColor(summary),
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        strokeCap = StrokeCap.Round,
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(MaterialTheme.shapes.small),
    )
}

/** Карточка бюджета: остаток, прогресс, потрачено и рекомендуемый дневной лимит. */
@Composable
fun BudgetCard(
    summary: BudgetSummary,
    currency: String,
    dailyAllowance: Double?,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.AccountBalanceWallet,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "Бюджет",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .weight(1f),
                )
                Text(
                    "${summary.percentUsed}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = budgetColor(summary),
                )
            }

            Column {
                Text(
                    if (summary.isOverBudget) "Перерасход" else "Осталось",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    Formatters.money(if (summary.isOverBudget) -summary.remaining else summary.remaining, currency),
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (summary.isOverBudget) StatusColors.danger else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("budget_remaining"),
                )
            }

            BudgetProgressBar(summary)

            Row(Modifier.fillMaxWidth()) {
                LabeledValue(
                    label = "Потрачено",
                    value = Formatters.money(summary.spent, currency),
                    modifier = Modifier.weight(1f),
                )
                LabeledValue(
                    label = "Запланировано",
                    value = Formatters.money(summary.budget, currency),
                    modifier = Modifier.weight(1f),
                    alignEnd = true,
                )
            }

            if (dailyAllowance != null) {
                Text(
                    text = if (summary.remaining > 0) {
                        "Чтобы уложиться в бюджет, тратьте до ${Formatters.money(dailyAllowance, currency)} в день"
                    } else {
                        "Бюджет исчерпан — новые траты пойдут сверх плана"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun LabeledValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    alignEnd: Boolean = false,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}
