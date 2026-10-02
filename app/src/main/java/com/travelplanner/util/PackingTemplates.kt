package com.travelplanner.util

import com.travelplanner.model.PackingCategory
import com.travelplanner.model.PackingItem

/** Базовый список вещей, которым можно заполнить сборы одним нажатием. */
object PackingTemplates {

    private val base: List<Pair<PackingCategory, String>> = listOf(
        PackingCategory.DOCUMENTS to "Паспорт",
        PackingCategory.DOCUMENTS to "Билеты",
        PackingCategory.DOCUMENTS to "Страховка",
        PackingCategory.DOCUMENTS to "Банковская карта",
        PackingCategory.DOCUMENTS to "Наличные",
        PackingCategory.CLOTHES to "Футболки",
        PackingCategory.CLOTHES to "Брюки / шорты",
        PackingCategory.CLOTHES to "Нижнее бельё",
        PackingCategory.CLOTHES to "Носки",
        PackingCategory.CLOTHES to "Куртка",
        PackingCategory.CLOTHES to "Удобная обувь",
        PackingCategory.HYGIENE to "Зубная щётка и паста",
        PackingCategory.HYGIENE to "Шампунь и гель",
        PackingCategory.HYGIENE to "Солнцезащитный крем",
        PackingCategory.ELECTRONICS to "Телефон",
        PackingCategory.ELECTRONICS to "Зарядка",
        PackingCategory.ELECTRONICS to "Пауэрбанк",
        PackingCategory.ELECTRONICS to "Наушники",
        PackingCategory.ELECTRONICS to "Переходник для розеток",
        PackingCategory.MEDICINE to "Обезболивающее",
        PackingCategory.MEDICINE to "Пластыри",
        PackingCategory.MEDICINE to "Личные лекарства",
    )

    /**
     * Возвращает вещи из шаблона, которых ещё нет в списке (сравнение без учёта регистра),
     * чтобы повторное нажатие не создавало дубликаты.
     */
    fun itemsFor(tripId: Long, existing: List<PackingItem>): List<PackingItem> {
        val existingNames = existing.map { it.name.trim().lowercase() }.toSet()
        return base
            .filter { (_, name) -> name.lowercase() !in existingNames }
            .map { (category, name) -> PackingItem(tripId = tripId, name = name, category = category) }
    }
}
