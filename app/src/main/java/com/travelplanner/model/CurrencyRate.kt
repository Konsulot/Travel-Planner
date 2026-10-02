package com.travelplanner.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Курс валюты относительно доллара США: сколько единиц [code] стоит 1 USD. */
@Entity(tableName = "currency_rates")
data class CurrencyRate(
    @PrimaryKey val code: String,
    val ratePerUsd: Double,
    /** Время обновления курса на стороне API, мс. */
    val updatedAt: Long,
)

/** Курсы, полученные из сети. */
data class FetchedRates(
    val ratesPerUsd: Map<String, Double>,
    val updatedAt: Long,
)

/**
 * Набор курсов, которым пользуется приложение.
 *
 * @property isFallback true, если актуальные курсы ещё ни разу не загружались
 * и используются встроенные примерные значения.
 */
data class RatesSnapshot(
    val ratesPerUsd: Map<String, Double>,
    val updatedAt: Long?,
    val isFallback: Boolean,
)
