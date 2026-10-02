package com.travelplanner.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class FormattersTest {

    private fun day(y: Int, m: Int, d: Int) = LocalDate.of(y, m, d).toEpochDay()

    @Test
    fun money_groupsThousandsAndShowsSymbol() {
        assertEquals("12 345,50 ₽", Formatters.money(12345.5, "RUB"))
        assertEquals("1 500 $", Formatters.money(1500.0, "USD"))
        assertEquals("0 €", Formatters.money(0.0, "EUR"))
        assertEquals("1 000 000 ₽", Formatters.money(1_000_000.0, "RUB"))
    }

    @Test
    fun money_unknownCurrency_usesCode() {
        assertEquals("10 XYZ", Formatters.money(10.0, "XYZ"))
    }

    @Test
    fun number_roundsToCentsAndKeepsSign() {
        assertEquals("0,01", Formatters.number(0.005))
        assertEquals("-250,75", Formatters.number(-250.749))
        assertEquals("0", Formatters.number(-0.001))
        assertEquals("999", Formatters.number(999.0))
    }

    @Test
    fun rate_adaptivePrecision() {
        assertEquals("92,46", Formatters.rate(92.4567))
        assertEquals("0,01084", Formatters.rate(0.010837))
        assertEquals("0,5", Formatters.rate(0.5))
    }

    @Test
    fun compact_shortensLargeNumbers() {
        assertEquals("20 тыс.", Formatters.compact(20_000.0))
        assertEquals("1,5 тыс.", Formatters.compact(1_500.0))
        assertEquals("2 млн", Formatters.compact(2_000_000.0))
        assertEquals("950", Formatters.compact(950.0))
    }

    @Test
    fun date_formatsInRussian() {
        assertEquals("5 октября 2026", Formatters.date(day(2026, 10, 5)))
        assertEquals("1 января 2027", Formatters.date(day(2027, 1, 1)))
        assertEquals("05.10", Formatters.dayMonth(day(2026, 10, 5)))
        // 5 октября 2026 — понедельник.
        assertEquals("Пн, 5 октября", Formatters.dayHeader(day(2026, 10, 5)))
    }

    @Test
    fun dateRange_avoidsRepetition() {
        assertEquals("5 – 12 октября 2026", Formatters.dateRange(day(2026, 10, 5), day(2026, 10, 12)))
        assertEquals("28 сентября – 3 октября 2026", Formatters.dateRange(day(2026, 9, 28), day(2026, 10, 3)))
        assertEquals(
            "30 декабря 2026 – 2 января 2027",
            Formatters.dateRange(day(2026, 12, 30), day(2027, 1, 2)),
        )
        assertEquals("5 октября 2026", Formatters.dateRange(day(2026, 10, 5), day(2026, 10, 5)))
    }

    @Test
    fun time_isZeroPadded() {
        assertEquals("09:05", Formatters.time(9 * 60 + 5))
        assertEquals("00:00", Formatters.time(0))
        assertEquals("23:59", Formatters.time(23 * 60 + 59))
    }

    @Test
    fun days_usesCorrectPluralForm() {
        assertEquals("1 день", Formatters.days(1))
        assertEquals("2 дня", Formatters.days(2))
        assertEquals("5 дней", Formatters.days(5))
        assertEquals("11 дней", Formatters.days(11))
        assertEquals("21 день", Formatters.days(21))
        assertEquals("22 дня", Formatters.days(22))
        assertEquals("112 дней", Formatters.days(112))
    }

    @Test
    fun parseAmount_acceptsCommaAndSpaces() {
        assertEquals(1234.5, Formatters.parseAmount("1 234,5")!!, 1e-9)
        assertEquals(10.0, Formatters.parseAmount("10")!!, 1e-9)
        assertEquals(0.75, Formatters.parseAmount("0.75")!!, 1e-9)
        assertEquals(5000.0, Formatters.parseAmount("5 000")!!, 1e-9)
        assertNull(Formatters.parseAmount("abc"))
        assertNull(Formatters.parseAmount(""))
        assertNull(Formatters.parseAmount("NaN"))
        assertNull(Formatters.parseAmount("Infinity"))
    }
}
