package com.travelplanner.util

import com.travelplanner.model.Expense
import com.travelplanner.model.ExpenseCategory

data class CategoryShare(
    val category: ExpenseCategory,
    val total: Double,
    /** Доля от всех расходов, 0..1. */
    val fraction: Float,
)

data class DailyTotal(val epochDay: Long, val total: Double)

data class TripStatistics(
    val total: Double,
    val count: Int,
    val byCategory: List<CategoryShare>,
    val byDay: List<DailyTotal>,
    val averagePerTripDay: Double,
    val maxExpense: Expense?,
    /** Расходы, сделанные до начала поездки (билеты, бронь жилья и т.п.). */
    val beforeTripTotal: Double,
)

object StatisticsCalculator {

    fun calculate(expenses: List<Expense>, startDate: Long, endDate: Long): TripStatistics {
        val total = expenses.sumOf { it.amountInTripCurrency }

        val byCategory = expenses
            .groupBy { it.category }
            .map { (category, items) ->
                val sum = items.sumOf { it.amountInTripCurrency }
                CategoryShare(
                    category = category,
                    total = sum,
                    fraction = if (total > 0) (sum / total).toFloat() else 0f,
                )
            }
            .sortedByDescending { it.total }

        // Все дни поездки (даже без трат) + дни вне поездки, в которые были траты.
        val totalsByDate = expenses.groupBy { it.date }.mapValues { (_, v) -> v.sumOf { it.amountInTripCurrency } }
        val days = (DateUtils.tripDays(startDate, endDate) + totalsByDate.keys).distinct().sorted()
        val byDay = days.map { DailyTotal(it, totalsByDate[it] ?: 0.0) }

        return TripStatistics(
            total = total,
            count = expenses.size,
            byCategory = byCategory,
            byDay = byDay,
            averagePerTripDay = total / DateUtils.durationDays(startDate, endDate),
            maxExpense = expenses.maxByOrNull { it.amountInTripCurrency },
            beforeTripTotal = expenses.filter { it.date < startDate }.sumOf { it.amountInTripCurrency },
        )
    }
}
