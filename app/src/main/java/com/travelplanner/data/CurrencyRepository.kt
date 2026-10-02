package com.travelplanner.data

import com.travelplanner.model.CurrencyRate
import com.travelplanner.model.FetchedRates
import com.travelplanner.model.RatesSnapshot
import com.travelplanner.util.DefaultRates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Источник актуальных курсов валют. */
fun interface ExchangeRateApi {
    suspend fun fetchLatest(): FetchedRates
}

/**
 * Бесплатный API без ключа: https://open.er-api.com (ExchangeRate-API, open access).
 * Курсы обновляются раз в сутки, база — USD.
 */
class OpenExchangeRateApi(
    private val url: String = "https://open.er-api.com/v6/latest/USD",
) : ExchangeRateApi {

    override suspend fun fetchLatest(): FetchedRates = withContext(Dispatchers.IO) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            requestMethod = "GET"
        }
        try {
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) throw IOException("Сервер курсов ответил кодом $code")
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            ExchangeRateParser.parse(body)
        } finally {
            connection.disconnect()
        }
    }
}

object ExchangeRateParser {
    fun parse(json: String): FetchedRates {
        val root = JSONObject(json)
        val result = root.optString("result")
        if (result != "success") {
            throw IOException("Не удалось получить курсы: ${root.optString("error-type", result)}")
        }
        val ratesJson = root.getJSONObject("rates")
        val rates = buildMap {
            val keys = ratesJson.keys()
            while (keys.hasNext()) {
                val code = keys.next()
                val value = ratesJson.optDouble(code)
                if (!value.isNaN() && value > 0) put(code, value)
            }
        }
        val updatedAt = root.optLong("time_last_update_unix", 0L)
            .takeIf { it > 0 }
            ?.times(1000)
            ?: System.currentTimeMillis()
        return FetchedRates(rates, updatedAt)
    }
}

/**
 * Курсы валют: хранятся в Room, чтобы конвертация работала офлайн.
 * Если курсы ещё ни разу не загружались, используются встроенные примерные значения.
 */
class CurrencyRepository(
    private val dao: CurrencyRateDao,
    private val api: ExchangeRateApi,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    val rates: Flow<RatesSnapshot> = dao.observeAll().map { stored -> toSnapshot(stored) }

    suspend fun currentRates(): RatesSnapshot = rates.first()

    /** Загружает свежие курсы из сети и сохраняет их в базе. */
    suspend fun refresh(): Result<Unit> = runCatching {
        val fetched = api.fetchLatest()
        if (fetched.ratesPerUsd.isEmpty()) throw IOException("Сервер вернул пустой список курсов")
        dao.upsertAll(fetched.ratesPerUsd.map { (code, rate) -> CurrencyRate(code, rate, fetched.updatedAt) })
    }

    /** Обновляет курсы, только если они старше [maxAgeMillis] (по умолчанию 12 часов). */
    suspend fun refreshIfStale(maxAgeMillis: Long = 12 * 60 * 60 * 1000L): Result<Unit> {
        val snapshot = currentRates()
        val updatedAt = snapshot.updatedAt
        val fresh = !snapshot.isFallback && updatedAt != null && clock() - updatedAt < maxAgeMillis
        return if (fresh) Result.success(Unit) else refresh()
    }

    companion object {
        fun toSnapshot(stored: List<CurrencyRate>): RatesSnapshot =
            if (stored.isEmpty()) {
                RatesSnapshot(DefaultRates.ratesPerUsd, updatedAt = null, isFallback = true)
            } else {
                // Встроенные курсы — только «подстраховка» для валют, которых нет в ответе API.
                RatesSnapshot(
                    ratesPerUsd = DefaultRates.ratesPerUsd + stored.associate { it.code to it.ratePerUsd },
                    updatedAt = stored.maxOf { it.updatedAt },
                    isFallback = false,
                )
            }
    }
}
