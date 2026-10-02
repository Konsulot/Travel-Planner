package com.travelplanner.util

import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Форматирование денег, дат и времени.
 *
 * Сделано вручную (без NumberFormat/DateTimeFormatter с локалью), чтобы результат
 * был одинаковым на любом устройстве и легко проверялся unit-тестами.
 */
object Formatters {

    private val monthsGenitive = listOf(
        "января", "февраля", "марта", "апреля", "мая", "июня",
        "июля", "августа", "сентября", "октября", "ноября", "декабря",
    )
    private val weekdaysShort = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

    /** 12345.5, "RUB" → "12 345,50 ₽"; 1500.0, "USD" → "1 500 $". */
    fun money(amount: Double, currency: String): String =
        "${number(amount)} ${Currencies.symbol(currency)}"

    /** Число с разделителем тысяч и двумя знаками после запятой (если есть дробная часть). */
    fun number(amount: Double): String {
        val cents = (abs(amount) * 100).roundToLong()
        val integerPart = cents / 100
        val fraction = cents % 100
        val grouped = integerPart.toString()
            .reversed()
            .chunked(3)
            .joinToString(" ")
            .reversed()
        val sign = if (amount < 0 && cents != 0L) "-" else ""
        return if (fraction == 0L) {
            "$sign$grouped"
        } else {
            "$sign$grouped," + fraction.toString().padStart(2, '0')
        }
    }

    /** Курс с адаптивной точностью: 0.010837 → "0,01084", 92.4567 → "92,46". */
    fun rate(value: Double): String {
        if (abs(value) >= 1.0 || value == 0.0) return number(value)
        return java.math.BigDecimal(value)
            .round(java.math.MathContext(4))
            .stripTrailingZeros()
            .toPlainString()
            .replace('.', ',')
    }

    /** 20 000 → «20 тыс.» — короткая подпись для графиков. */
    fun compact(amount: Double): String = when {
        abs(amount) >= 1_000_000 -> trimZero(amount / 1_000_000) + " млн"
        abs(amount) >= 1_000 -> trimZero(amount / 1_000) + " тыс."
        else -> number(amount)
    }

    private fun trimZero(value: Double): String {
        val rounded = (value * 10).roundToLong() / 10.0
        return if (rounded % 1.0 == 0.0) rounded.toLong().toString()
        else rounded.toString().replace('.', ',')
    }

    /** "5 октября 2026". */
    fun date(epochDay: Long): String {
        val d = LocalDate.ofEpochDay(epochDay)
        return "${d.dayOfMonth} ${monthsGenitive[d.monthValue - 1]} ${d.year}"
    }

    /** "5 октября" — без года. */
    fun dateShort(epochDay: Long): String {
        val d = LocalDate.ofEpochDay(epochDay)
        return "${d.dayOfMonth} ${monthsGenitive[d.monthValue - 1]}"
    }

    /** "05.10" — для подписей на графике. */
    fun dayMonth(epochDay: Long): String {
        val d = LocalDate.ofEpochDay(epochDay)
        return "%02d.%02d".format(d.dayOfMonth, d.monthValue)
    }

    /** "Пн, 5 октября". */
    fun dayHeader(epochDay: Long): String {
        val d = LocalDate.ofEpochDay(epochDay)
        return "${weekdaysShort[d.dayOfWeek.value - 1]}, ${dateShort(epochDay)}"
    }

    /**
     * Диапазон дат без лишних повторов:
     * «5 – 12 октября 2026», «28 сентября – 3 октября 2026», «30 декабря 2026 – 2 января 2027».
     */
    fun dateRange(start: Long, end: Long): String {
        val s = LocalDate.ofEpochDay(start)
        val e = LocalDate.ofEpochDay(end)
        return when {
            start == end -> date(start)
            s.year != e.year -> "${date(start)} – ${date(end)}"
            s.monthValue != e.monthValue -> "${dateShort(start)} – ${date(end)}"
            else -> "${s.dayOfMonth} – ${date(end)}"
        }
    }

    /** Минуты от полуночи → "09:05". */
    fun time(minutes: Int): String = "%02d:%02d".format(minutes / 60, minutes % 60)

    /** 1 день, 2 дня, 5 дней, 21 день. */
    fun days(count: Long): String = "$count ${plural(count, "день", "дня", "дней")}"

    fun plural(count: Long, one: String, few: String, many: String): String {
        val n = abs(count) % 100
        val n1 = n % 10
        return when {
            n in 11..14 -> many
            n1 == 1L -> one
            n1 in 2..4 -> few
            else -> many
        }
    }

    /** Разбор суммы, введённой пользователем: допускаются пробелы и запятая. */
    fun parseAmount(text: String): Double? =
        text.filterNot { it == ' ' || it == '\u00A0' || it == '\u202F' }
            .replace(',', '.')
            .toDoubleOrNull()
            ?.takeIf { it.isFinite() }
}
