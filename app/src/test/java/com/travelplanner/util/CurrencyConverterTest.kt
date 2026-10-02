package com.travelplanner.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyConverterTest {

    private val rates = mapOf(
        "USD" to 1.0,
        "RUB" to 80.0,
        "EUR" to 0.8,
        "BAD" to 0.0,
    )

    @Test
    fun sameCurrency_returnsAmountUnchanged() {
        assertEquals(42.0, CurrencyConverter.convert(42.0, "XXX", "XXX", emptyMap())!!, 1e-9)
    }

    @Test
    fun convertsThroughUsd() {
        assertEquals(8000.0, CurrencyConverter.convert(100.0, "USD", "RUB", rates)!!, 1e-9)
        assertEquals(1.0, CurrencyConverter.convert(80.0, "RUB", "USD", rates)!!, 1e-9)
        // 100 EUR = 125 USD = 10 000 RUB
        assertEquals(10_000.0, CurrencyConverter.convert(100.0, "EUR", "RUB", rates)!!, 1e-9)
    }

    @Test
    fun roundTrip_returnsOriginalAmount() {
        val rub = CurrencyConverter.convert(123.45, "EUR", "RUB", rates)!!
        assertEquals(123.45, CurrencyConverter.convert(rub, "RUB", "EUR", rates)!!, 1e-9)
    }

    @Test
    fun unknownOrInvalidRate_returnsNull() {
        assertNull(CurrencyConverter.convert(1.0, "USD", "GBP", rates))
        assertNull(CurrencyConverter.convert(1.0, "GBP", "USD", rates))
        assertNull(CurrencyConverter.convert(1.0, "BAD", "USD", rates))
    }

    @Test
    fun rate_isPriceOfOneUnit() {
        assertEquals(100.0, CurrencyConverter.rate("EUR", "RUB", rates)!!, 1e-9)
    }

    @Test
    fun defaultRates_coverAllSelectableCurrencies() {
        Currencies.all.forEach { info ->
            val rate = DefaultRates.ratesPerUsd[info.code]
            assertTrue("Нет офлайн-курса для ${info.code}", rate != null && rate > 0)
        }
    }

    @Test
    fun currencies_symbolAndNameFallbackToCode() {
        assertEquals("₽", Currencies.symbol("RUB"))
        assertEquals("ABC", Currencies.symbol("ABC"))
        assertEquals("ABC", Currencies.name("ABC"))
    }
}
