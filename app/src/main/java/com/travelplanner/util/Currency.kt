package com.travelplanner.util

data class CurrencyInfo(val code: String, val name: String, val symbol: String)

/** Валюты, доступные для выбора в приложении. */
object Currencies {

    val all: List<CurrencyInfo> = listOf(
        CurrencyInfo("RUB", "Российский рубль", "₽"),
        CurrencyInfo("USD", "Доллар США", "$"),
        CurrencyInfo("EUR", "Евро", "€"),
        CurrencyInfo("GBP", "Фунт стерлингов", "£"),
        CurrencyInfo("CNY", "Китайский юань", "¥"),
        CurrencyInfo("JPY", "Японская иена", "JP¥"),
        CurrencyInfo("TRY", "Турецкая лира", "₺"),
        CurrencyInfo("AED", "Дирхам ОАЭ", "AED"),
        CurrencyInfo("THB", "Тайский бат", "฿"),
        CurrencyInfo("GEL", "Грузинский лари", "₾"),
        CurrencyInfo("AMD", "Армянский драм", "֏"),
        CurrencyInfo("KZT", "Казахстанский тенге", "₸"),
        CurrencyInfo("BYN", "Белорусский рубль", "Br"),
        CurrencyInfo("UZS", "Узбекский сум", "сўм"),
        CurrencyInfo("KGS", "Киргизский сом", "сом"),
        CurrencyInfo("AZN", "Азербайджанский манат", "₼"),
        CurrencyInfo("CHF", "Швейцарский франк", "CHF"),
        CurrencyInfo("CZK", "Чешская крона", "Kč"),
        CurrencyInfo("PLN", "Польский злотый", "zł"),
        CurrencyInfo("EGP", "Египетский фунт", "E£"),
        CurrencyInfo("INR", "Индийская рупия", "₹"),
        CurrencyInfo("VND", "Вьетнамский донг", "₫"),
        CurrencyInfo("IDR", "Индонезийская рупия", "Rp"),
        CurrencyInfo("KRW", "Южнокорейская вона", "₩"),
    )

    private val byCode = all.associateBy { it.code }

    fun symbol(code: String): String = byCode[code]?.symbol ?: code

    fun name(code: String): String = byCode[code]?.name ?: code

    const val DEFAULT = "RUB"
}

/**
 * Примерные курсы (единиц валюты за 1 USD) для работы без интернета, пока актуальные курсы
 * не загружены. Пользователь видит предупреждение, что курсы примерные.
 */
object DefaultRates {
    val ratesPerUsd: Map<String, Double> = mapOf(
        "USD" to 1.0,
        "RUB" to 82.0,
        "EUR" to 0.86,
        "GBP" to 0.75,
        "CNY" to 7.15,
        "JPY" to 148.0,
        "TRY" to 41.5,
        "AED" to 3.6725,
        "THB" to 32.5,
        "GEL" to 2.7,
        "AMD" to 385.0,
        "KZT" to 540.0,
        "BYN" to 3.0,
        "UZS" to 12_100.0,
        "KGS" to 87.4,
        "AZN" to 1.7,
        "CHF" to 0.80,
        "CZK" to 21.0,
        "PLN" to 3.65,
        "EGP" to 48.5,
        "INR" to 88.0,
        "VND" to 26_300.0,
        "IDR" to 16_500.0,
        "KRW" to 1_390.0,
    )
}

object CurrencyConverter {

    /**
     * Конвертирует сумму через доллар: amount / курс(from) * курс(to).
     * Возвращает null, если для одной из валют нет курса.
     */
    fun convert(amount: Double, from: String, to: String, ratesPerUsd: Map<String, Double>): Double? {
        if (from == to) return amount
        val fromRate = ratesPerUsd[from]?.takeIf { it > 0 } ?: return null
        val toRate = ratesPerUsd[to]?.takeIf { it > 0 } ?: return null
        return amount / fromRate * toRate
    }

    /** Сколько единиц [to] стоит одна единица [from]. */
    fun rate(from: String, to: String, ratesPerUsd: Map<String, Double>): Double? =
        convert(1.0, from, to, ratesPerUsd)
}
