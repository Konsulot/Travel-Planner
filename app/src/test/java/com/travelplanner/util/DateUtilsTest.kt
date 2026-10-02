package com.travelplanner.util

import com.travelplanner.model.PackingCategory
import com.travelplanner.model.PackingItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DateUtilsTest {

    @Test
    fun durationIncludesBothEnds() {
        assertEquals(1, DateUtils.durationDays(10, 10))
        assertEquals(7, DateUtils.durationDays(10, 16))
    }

    @Test
    fun tripDays_listsEveryDay() {
        assertEquals(listOf(10L, 11L, 12L), DateUtils.tripDays(10, 12))
        assertEquals(listOf(10L), DateUtils.tripDays(10, 5))
    }

    @Test
    fun status() {
        assertEquals(TripStatus.UPCOMING, DateUtils.status(10, 12, today = 9))
        assertEquals(TripStatus.ONGOING, DateUtils.status(10, 12, today = 10))
        assertEquals(TripStatus.ONGOING, DateUtils.status(10, 12, today = 12))
        assertEquals(TripStatus.FINISHED, DateUtils.status(10, 12, today = 13))
    }

    @Test
    fun dayNumberStartsFromOne() {
        assertEquals(1, DateUtils.dayNumber(10, 10))
        assertEquals(3, DateUtils.dayNumber(12, 10))
    }

    @Test
    fun utcMillisRoundTrip() {
        val day = 20_000L
        val millis = DateUtils.epochDayToUtcMillis(day)
        assertEquals(day, DateUtils.utcMillisToEpochDay(millis))
        // Любой момент внутри суток относится к тому же дню.
        assertEquals(day, DateUtils.utcMillisToEpochDay(millis + 23 * 3_600_000L))
    }
}

class ValidatorsTest {

    @Test
    fun validTrip_hasNoErrors() {
        val errors = Validators.validateTrip("Отпуск", "Сочи", 10, 15, "50 000")
        assertFalse(errors.hasErrors)
    }

    @Test
    fun emptyFields_produceErrors() {
        val errors = Validators.validateTrip(" ", "", null, null, "")
        assertNotNull(errors.title)
        assertNotNull(errors.destination)
        assertNotNull(errors.startDate)
        assertNotNull(errors.endDate)
        assertNotNull(errors.budget)
        assertTrue(errors.hasErrors)
    }

    @Test
    fun endBeforeStart_isError() {
        val errors = Validators.validateTrip("A", "B", 15, 10, "100")
        assertEquals("Окончание раньше начала", errors.endDate)
        assertNull(errors.startDate)
    }

    @Test
    fun budget_mustBeNonNegativeNumber() {
        assertNotNull(Validators.validateTrip("A", "B", 1, 2, "-5").budget)
        assertNotNull(Validators.validateTrip("A", "B", 1, 2, "abc").budget)
        assertNull(Validators.validateTrip("A", "B", 1, 2, "0").budget)
    }

    @Test
    fun amount_mustBePositive() {
        assertNull(Validators.validateAmount("12,5"))
        assertNotNull(Validators.validateAmount("0"))
        assertNotNull(Validators.validateAmount(""))
        assertNotNull(Validators.validateAmount("1e20"))
    }
}

class PackingTemplatesTest {

    @Test
    fun template_skipsExistingItemsIgnoringCase() {
        val existing = listOf(PackingItem(id = 1, tripId = 7, name = "паспорт", category = PackingCategory.DOCUMENTS))
        val items = PackingTemplates.itemsFor(7, existing)
        assertTrue(items.isNotEmpty())
        assertTrue(items.none { it.name.equals("Паспорт", ignoreCase = true) })
        assertTrue(items.all { it.tripId == 7L && !it.isPacked && it.id == 0L })
    }

    @Test
    fun template_secondCallAddsNothing() {
        val first = PackingTemplates.itemsFor(1, emptyList())
        assertTrue(PackingTemplates.itemsFor(1, first).isEmpty())
    }
}
