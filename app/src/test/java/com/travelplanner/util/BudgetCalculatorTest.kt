package com.travelplanner.util

import com.travelplanner.model.Expense
import com.travelplanner.model.ExpenseCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BudgetCalculatorTest {

    private fun expense(amount: Double, date: Long = 0) = Expense(
        tripId = 1, title = "x", amount = amount, currency = "RUB",
        amountInTripCurrency = amount, category = ExpenseCategory.FOOD, date = date,
    )

    @Test
    fun summarize_sumsConvertedAmounts() {
        val expenses = listOf(
            expense(1000.0),
            // Расход в евро: в бюджете учитывается сумма в валюте поездки.
            expense(10.0).copy(currency = "EUR", amountInTripCurrency = 950.0),
        )
        val summary = BudgetCalculator.summarize(10_000.0, expenses)
        assertEquals(1950.0, summary.spent, 1e-9)
        assertEquals(8050.0, summary.remaining, 1e-9)
        assertEquals(20, summary.percentUsed)
        assertEquals(0.195f, summary.progress, 1e-6f)
        assertFalse(summary.isOverBudget)
    }

    @Test
    fun summarize_emptyList_spentIsZero() {
        val summary = BudgetCalculator.summarize(5000.0, emptyList())
        assertEquals(0.0, summary.spent, 0.0)
        assertEquals(5000.0, summary.remaining, 0.0)
        assertEquals(0f, summary.progress, 0f)
    }

    @Test
    fun overBudget_progressIsCappedButPercentIsNot() {
        val summary = BudgetSummary(budget = 1000.0, spent = 1500.0)
        assertTrue(summary.isOverBudget)
        assertEquals(-500.0, summary.remaining, 1e-9)
        assertEquals(1f, summary.progress, 0f)
        assertEquals(150, summary.percentUsed)
    }

    @Test
    fun zeroBudget_isHandledWithoutDivisionByZero() {
        assertEquals(0f, BudgetSummary(0.0, 0.0).progress, 0f)
        assertEquals(0, BudgetSummary(0.0, 0.0).percentUsed)
        assertEquals(1f, BudgetSummary(0.0, 10.0).progress, 0f)
        assertTrue(BudgetSummary(0.0, 10.0).isOverBudget)
    }

    @Test
    fun exactBudget_isNotOverBudget() {
        assertFalse(BudgetSummary(1000.0, 1000.0).isOverBudget)
    }

    @Test
    fun dailyAllowance_beforeTrip_dividesByWholeTrip() {
        // Поездка 10..14 (5 дней), сегодня 3-е число.
        assertEquals(1000.0, BudgetCalculator.dailyAllowance(5000.0, 10, 14, today = 3)!!, 1e-9)
    }

    @Test
    fun dailyAllowance_duringTrip_dividesByDaysLeftIncludingToday() {
        // Сегодня 13-е: остались 13 и 14 число.
        assertEquals(1500.0, BudgetCalculator.dailyAllowance(3000.0, 10, 14, today = 13)!!, 1e-9)
        // Последний день.
        assertEquals(700.0, BudgetCalculator.dailyAllowance(700.0, 10, 14, today = 14)!!, 1e-9)
    }

    @Test
    fun dailyAllowance_afterTripOrOverBudget() {
        assertNull(BudgetCalculator.dailyAllowance(3000.0, 10, 14, today = 15))
        assertEquals(0.0, BudgetCalculator.dailyAllowance(-200.0, 10, 14, today = 12)!!, 0.0)
    }

    @Test
    fun averagePerDay_usesElapsedDaysDuringTrip() {
        // В пути 3-й день из 5.
        assertEquals(1000.0, BudgetCalculator.averagePerDay(3000.0, 10, 14, today = 12), 1e-9)
        // После поездки — на всю длительность.
        assertEquals(600.0, BudgetCalculator.averagePerDay(3000.0, 10, 14, today = 20), 1e-9)
        // До поездки — тоже на всю длительность.
        assertEquals(600.0, BudgetCalculator.averagePerDay(3000.0, 10, 14, today = 1), 1e-9)
    }
}
