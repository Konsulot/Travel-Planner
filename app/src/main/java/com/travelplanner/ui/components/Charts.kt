package com.travelplanner.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.travelplanner.model.ExpenseCategory

/** Фиксированные цвета категорий расходов для графиков и легенды. */
object CategoryColors {
    fun of(category: ExpenseCategory): Color = when (category) {
        ExpenseCategory.TRANSPORT -> Color(0xFF1E88E5)
        ExpenseCategory.ACCOMMODATION -> Color(0xFF8E24AA)
        ExpenseCategory.FOOD -> Color(0xFFF4511E)
        ExpenseCategory.ENTERTAINMENT -> Color(0xFF00897B)
        ExpenseCategory.SHOPPING -> Color(0xFFFDD835)
        ExpenseCategory.HEALTH -> Color(0xFFE53935)
        ExpenseCategory.OTHER -> Color(0xFF757575)
    }
}

/**
 * Кольцевая диаграмма. Рисуется на Canvas без сторонних библиотек.
 * В центре можно вывести произвольный контент (например, общую сумму).
 */
@Composable
fun DonutChart(
    values: List<Float>,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 28.dp,
    centerContent: @Composable () -> Unit = {},
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val stroke = strokeWidth.toPx()
            val diameter = size.minDimension - stroke
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            val total = values.sum()

            if (total <= 0f) {
                drawArc(trackColor, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
                return@Canvas
            }

            val gap = if (values.count { it > 0f } > 1) 2f else 0f
            var startAngle = -90f
            values.forEachIndexed { index, value ->
                val sweep = value / total * 360f
                if (sweep > 0f) {
                    drawArc(
                        color = colors.getOrElse(index) { trackColor },
                        startAngle = startAngle + gap / 2,
                        sweepAngle = (sweep - gap).coerceAtLeast(0.5f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke),
                    )
                }
                startAngle += sweep
            }
        }
        centerContent()
    }
}

data class BarData(
    val label: String,
    val value: Double,
    val valueLabel: String,
    val highlighted: Boolean = false,
)

/**
 * Столбчатая диаграмма с горизонтальной прокруткой (удобно для длинных поездок).
 * Подсвеченные столбцы рисуются цветом [highlightColor].
 */
@Composable
fun BarChart(
    bars: List<BarData>,
    modifier: Modifier = Modifier,
    maxBarHeight: Dp = 140.dp,
    barColor: Color = MaterialTheme.colorScheme.primary,
    highlightColor: Color = MaterialTheme.colorScheme.secondary,
) {
    val maxValue = bars.maxOfOrNull { it.value }?.takeIf { it > 0 } ?: 1.0
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        bars.forEach { bar ->
            Column(
                modifier = Modifier.width(44.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = if (bar.value > 0) bar.valueLabel else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
                Spacer(Modifier.height(4.dp))
                val fraction = (bar.value / maxValue).toFloat().coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(maxOf(maxBarHeight * fraction, 2.dp))
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(
                            when {
                                bar.value <= 0 -> MaterialTheme.colorScheme.surfaceVariant
                                bar.highlighted -> highlightColor
                                else -> barColor
                            },
                        ),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = bar.label,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
            }
        }
    }
}
