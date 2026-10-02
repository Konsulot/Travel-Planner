package com.travelplanner.data

import com.travelplanner.model.CurrencyRate
import com.travelplanner.model.FetchedRates
import com.travelplanner.util.DefaultRates
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class CurrencyRepositoryTest {

    @Test
    fun noStoredRates_usesFallback() = runTest {
        val repo = CurrencyRepository(FakeCurrencyRateDao(), api = { error("not called") })
        val snapshot = repo.currentRates()
        assertTrue(snapshot.isFallback)
        assertEquals(DefaultRates.ratesPerUsd, snapshot.ratesPerUsd)
    }

    @Test
    fun refresh_storesRatesFromApi() = runTest {
        val dao = FakeCurrencyRateDao()
        val repo = CurrencyRepository(dao, api = { FetchedRates(mapOf("USD" to 1.0, "RUB" to 90.0), updatedAt = 1000L) })

        assertTrue(repo.refresh().isSuccess)

        val snapshot = repo.currentRates()
        assertFalse(snapshot.isFallback)
        assertEquals(90.0, snapshot.ratesPerUsd.getValue("RUB"), 1e-9)
        assertEquals(1000L, snapshot.updatedAt)
        // Валюты, которых нет в ответе, берутся из офлайн-курсов.
        assertEquals(DefaultRates.ratesPerUsd.getValue("EUR"), snapshot.ratesPerUsd.getValue("EUR"), 1e-9)
    }

    @Test
    fun refresh_networkError_returnsFailureAndKeepsOldRates() = runTest {
        val dao = FakeCurrencyRateDao(listOf(CurrencyRate("RUB", 95.0, 5L)))
        val repo = CurrencyRepository(dao, api = { throw IOException("offline") })

        assertTrue(repo.refresh().isFailure)
        assertEquals(95.0, repo.currentRates().ratesPerUsd.getValue("RUB"), 1e-9)
    }

    @Test
    fun refreshIfStale_skipsNetworkWhenRatesAreFresh() = runTest {
        var calls = 0
        val now = 10_000_000L
        val dao = FakeCurrencyRateDao(listOf(CurrencyRate("RUB", 95.0, now - 60_000)))
        val repo = CurrencyRepository(
            dao,
            api = { calls++; FetchedRates(mapOf("RUB" to 1.0), now) },
            clock = { now },
        )

        repo.refreshIfStale()
        assertEquals(0, calls)

        repo.refreshIfStale(maxAgeMillis = 1_000)
        assertEquals(1, calls)
    }
}

class ExchangeRateParserTest {

    @Test
    fun parsesSuccessfulResponse() {
        val json = """
            {"result":"success","base_code":"USD","time_last_update_unix":1759363201,
             "rates":{"USD":1,"RUB":81.5,"EUR":0.851}}
        """.trimIndent()
        val rates = ExchangeRateParser.parse(json)
        assertEquals(3, rates.ratesPerUsd.size)
        assertEquals(81.5, rates.ratesPerUsd.getValue("RUB"), 1e-9)
        assertEquals(1759363201000L, rates.updatedAt)
    }

    @Test(expected = IOException::class)
    fun errorResponse_throws() {
        ExchangeRateParser.parse("""{"result":"error","error-type":"unsupported-code"}""")
    }
}
