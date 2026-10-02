package com.travelplanner.util

import com.travelplanner.model.Expense
import kotlin.math.max

/** Сводка по бюджету поездки. */
data class BudgetSummary(
    val budget: Double,
    val spent: Double,
) {
    val remaining: Double get() = budget - spent

    val isOverBudget: Boolean get() = spent > budget + EPS

    /** Доля потраченного бюджета для индикатора прогресса, 0..1. */
    val progress: Float
        get() = when {
            budget <= 0.0 -> if (spent > 0.0) 1f else 0f
            else -> (spent / budget).toFloat().coerceIn(0f, 1f)
        }

    /** Потрачено в процентах (может быть больше 100, если бюджет превышен). */
    val percentUsed: Int
        get() = if (budget <= 0.0) (if (spent > 0.0) 100 else 0) else Math.round(spent / budget * 100).toInt()

    private companion object {
        const val EPS = 0.005
    }
}

object BudgetCalculator {

    fun summarize(budget: Double, expenses: List<Expense>): BudgetSummary =
        BudgetSummary(budget = budget, spent = expenses.sumOf { it.amountInTripCurrency })

    /**
     * Сколько можно тратить в день до конца поездки, чтобы уложиться в бюджет.
     * До начала поездки остаток делится на все дни поездки, во время — на оставшиеся дни
     * (включая сегодняшний). После окончания поездки — null.
     */
    fun dailyAllowance(remaining: Double, start: Long, end: Long, today: Long): Double? {
        if (today > end) return null
        val from = max(today, start)
        val daysLeft = end - from + 1
        if (daysLeft <= 0) return null
        return max(remaining, 0.0) / daysLeft
    }

    /**
     * Средний расход в день. Во время поездки делим на прошедшие дни,
     * до начала и после окончания — на всю длительность поездки.
     */
    fun averagePerDay(spent: Double, start: Long, end: Long, today: Long): Double {
        val duration = DateUtils.durationDays(start, end)
        val days = when (DateUtils.status(start, end, today)) {
            TripStatus.ONGOING -> (today - start + 1).toInt()
            else -> duration
        }
        return spent / days.coerceAtLeast(1)
    }
}
