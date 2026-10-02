package com.travelplanner.util

/** Ошибки формы поездки; null — поле заполнено правильно. */
data class TripFormErrors(
    val title: String? = null,
    val destination: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val budget: String? = null,
) {
    val hasErrors: Boolean
        get() = listOf(title, destination, startDate, endDate, budget).any { it != null }
}

object Validators {

    const val MAX_AMOUNT = 1_000_000_000_000.0

    fun validateTrip(
        title: String,
        destination: String,
        startDate: Long?,
        endDate: Long?,
        budgetText: String,
    ): TripFormErrors {
        val budget = Formatters.parseAmount(budgetText)
        return TripFormErrors(
            title = if (title.isBlank()) "Введите название поездки" else null,
            destination = if (destination.isBlank()) "Укажите место назначения" else null,
            startDate = if (startDate == null) "Выберите дату начала" else null,
            endDate = when {
                endDate == null -> "Выберите дату окончания"
                startDate != null && endDate < startDate -> "Окончание раньше начала"
                else -> null
            },
            budget = when {
                budgetText.isBlank() -> "Укажите бюджет"
                budget == null -> "Некорректная сумма"
                budget < 0 -> "Бюджет не может быть отрицательным"
                budget > MAX_AMOUNT -> "Слишком большая сумма"
                else -> null
            },
        )
    }

    /** Проверка суммы расхода: должна быть положительным числом. */
    fun validateAmount(text: String): String? {
        val value = Formatters.parseAmount(text)
        return when {
            text.isBlank() -> "Введите сумму"
            value == null -> "Некорректная сумма"
            value <= 0 -> "Сумма должна быть больше нуля"
            value > MAX_AMOUNT -> "Слишком большая сумма"
            else -> null
        }
    }

    fun validateRequired(text: String, message: String): String? =
        if (text.isBlank()) message else null
}
