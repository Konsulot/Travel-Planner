package com.travelplanner.util

import com.travelplanner.model.Expense
import com.travelplanner.model.ExpenseCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StatisticsCalculatorTest {

    private fun expense(id: Long, amount: Double, category: ExpenseCategory, date: Long) = Expense(
        id = id, tripId = 1, title = "e$id", amount = amount, currency = "RUB",
        amountInTripCurrency = amount, category = category, date = date,
    )

    private val expenses = listOf(
        expense(1, 6000.0, ExpenseCategory.TRANSPORT, date = 5), // до поездки
        expense(2, 3000.0, ExpenseCategory.FOOD, date = 10),
        expense(3, 1000.0, ExpenseCategory.FOOD, date = 11),
    )

    @Test
    fun totalsAndCount() {
        val stats = StatisticsCalculator.calculate(expenses, startDate = 10, endDate = 13)
        assertEquals(10_000.0, stats.total, 1e-9)
        assertEquals(3, stats.count)
        assertEquals(6000.0, stats.beforeTripTotal, 1e-9)
        assertEquals(1L, stats.maxExpense?.id)
        assertEquals(2500.0, stats.averagePerTripDay, 1e-9) // 10 000 / 4 дня
    }

    @Test
    fun byCategory_sortedByTotalWithFractions() {
        val stats = StatisticsCalculator.calculate(expenses, 10, 13)
        assertEquals(listOf(ExpenseCategory.TRANSPORT, ExpenseCategory.FOOD), stats.byCategory.map { it.category })
        assertEquals(0.6f, stats.byCategory[0].fraction, 1e-6f)
        assertEquals(0.4f, stats.byCategory[1].fraction, 1e-6f)
        assertEquals(4000.0, stats.byCategory[1].total, 1e-9)
    }

    @Test
    fun byDay_includesEmptyTripDaysAndDaysOutsideTrip() {
        val stats = StatisticsCalculator.calculate(expenses, 10, 13)
        assertEquals(listOf(5L, 10L, 11L, 12L, 13L), stats.byDay.map { it.epochDay })
        assertEquals(listOf(6000.0, 3000.0, 1000.0, 0.0, 0.0), stats.byDay.map { it.total })
    }

    @Test
    fun emptyExpenses() {
        val stats = StatisticsCalculator.calculate(emptyList(), 10, 11)
        assertEquals(0.0, stats.total, 0.0)
        assertEquals(0, stats.byCategory.size)
        assertEquals(2, stats.byDay.size)
        assertNull(stats.maxExpense)
    }
}
