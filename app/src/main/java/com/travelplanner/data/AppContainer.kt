package com.travelplanner.data

import android.content.Context

/** Простой контейнер зависимостей (ручной DI): создаёт базу и репозитории один раз на приложение. */
interface AppContainer {
    val tripRepository: TripRepository
    val currencyRepository: CurrencyRepository
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val database: AppDatabase by lazy { AppDatabase.getInstance(context) }

    override val tripRepository: TripRepository by lazy { OfflineTripRepository(database) }

    override val currencyRepository: CurrencyRepository by lazy {
        CurrencyRepository(database.currencyRateDao(), OpenExchangeRateApi())
    }
}
