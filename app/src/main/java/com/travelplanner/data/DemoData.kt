package com.travelplanner.data

import com.travelplanner.model.Expense
import com.travelplanner.model.ExpenseCategory
import com.travelplanner.model.Place
import com.travelplanner.model.Trip
import com.travelplanner.model.TripEvent

/** Пример поездки, чтобы быстро показать возможности приложения (например, на защите проекта). */
object DemoData {

    suspend fun createDemoTrip(repository: TripRepository, today: Long): Long {
        val start = today + 14
        val end = start + 2
        val tripId = repository.saveTrip(
            Trip(
                title = "Выходные в Санкт-Петербурге",
                destination = "Санкт-Петербург, Россия",
                startDate = start,
                endDate = end,
                budget = 45_000.0,
                currency = "RUB",
                notes = "Взять зонт и пауэрбанк. Забронировать столик на вечер второго дня.",
            )
        )

        listOf(
            Expense(tripId = tripId, title = "Билеты на «Сапсан»", amount = 9_800.0, currency = "RUB",
                amountInTripCurrency = 9_800.0, category = ExpenseCategory.TRANSPORT, date = today),
            Expense(tripId = tripId, title = "Отель на 2 ночи", amount = 12_400.0, currency = "RUB",
                amountInTripCurrency = 12_400.0, category = ExpenseCategory.ACCOMMODATION, date = today),
            Expense(tripId = tripId, title = "Билеты в Эрмитаж", amount = 1_000.0, currency = "RUB",
                amountInTripCurrency = 1_000.0, category = ExpenseCategory.ENTERTAINMENT, date = today + 1),
        ).forEach { repository.saveExpense(it) }

        listOf(
            TripEvent(tripId = tripId, date = start, time = 7 * 60 + 40, title = "Поезд Москва → Петербург",
                location = "Ленинградский вокзал"),
            TripEvent(tripId = tripId, date = start, time = 12 * 60, title = "Заселение в отель",
                location = "Невский проспект"),
            TripEvent(tripId = tripId, date = start, time = 14 * 60, title = "Прогулка по Невскому"),
            TripEvent(tripId = tripId, date = start + 1, time = 10 * 60 + 30, title = "Эрмитаж",
                location = "Дворцовая площадь, 2"),
            TripEvent(tripId = tripId, date = start + 1, time = 21 * 60, title = "Развод мостов",
                location = "Дворцовый мост"),
            TripEvent(tripId = tripId, date = end, time = 11 * 60, title = "Петропавловская крепость"),
            TripEvent(tripId = tripId, date = end, time = 19 * 60 + 10, title = "Поезд домой",
                location = "Московский вокзал"),
        ).forEach { repository.saveEvent(it) }

        listOf(
            Place(tripId = tripId, name = "Эрмитаж", address = "Дворцовая площадь, 2", isWishlist = true),
            Place(tripId = tripId, name = "Исаакиевский собор", address = "Исаакиевская площадь, 4",
                note = "Подняться на колоннаду", isWishlist = true),
            Place(tripId = tripId, name = "Новая Голландия", address = "наб. Адмиралтейского канала, 2"),
            Place(tripId = tripId, name = "Петергоф", address = "Петергоф, Разводная ул., 2",
                note = "Если останется время"),
        ).forEach { repository.savePlace(it) }

        repository.addPackingTemplate(tripId)
        return tripId
    }
}
