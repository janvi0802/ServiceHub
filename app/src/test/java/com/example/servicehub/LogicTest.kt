package com.example.servicehub

import com.example.servicehub.data.DateUtils
import com.example.servicehub.data.FixedData
import com.example.servicehub.data.SampleData
import com.example.servicehub.data.model.Booking
import com.example.servicehub.data.model.BookingStatus
import com.example.servicehub.ui.components.initialsOf
import com.example.servicehub.ui.components.ratingText
import com.example.servicehub.ui.components.rupees
import com.example.servicehub.ui.provider.ProviderJobsViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Automatic checks of the app's pure logic (no phone or Firebase needed).
 * Run with: gradlew.bat testDebugUnitTest
 */
class LogicTest {

    // ------------------------------------------------ small helpers

    @Test
    fun initials_areFirstLettersOfTwoWords() {
        assertEquals("JP", initialsOf("Janvi Panchal"))
        assertEquals("R", initialsOf("ramesh"))
        assertEquals("AB", initialsOf("  anil   bhai  kumar "))
        assertEquals("?", initialsOf("   "))
    }

    @Test
    fun rupees_useIndianSymbolAndCommas() {
        assertEquals("₹149", rupees(149))
        assertEquals("₹1,999", rupees(1999))
    }

    @Test
    fun rating_showsNewWhenNoReviews() {
        assertEquals("★ New", ratingText(0.0, 0))
        assertEquals("★ 4.5 (2)", ratingText(4.5, 2))
    }

    // ------------------------------------------------ booking status

    @Test
    fun status_activeOnlyWhileJobIsOpen() {
        assertTrue(BookingStatus.REQUESTED.isActive)
        assertTrue(BookingStatus.ACCEPTED.isActive)
        assertTrue(BookingStatus.IN_PROGRESS.isActive)
        assertFalse(BookingStatus.COMPLETED.isActive)
        assertFalse(BookingStatus.REJECTED.isActive)
        assertFalse(BookingStatus.CANCELLED.isActive)
    }

    @Test
    fun status_readFromFirestoreText() {
        assertEquals(BookingStatus.IN_PROGRESS, BookingStatus.fromName("IN_PROGRESS"))
        assertEquals(BookingStatus.REQUESTED, BookingStatus.fromName("nonsense"))
        assertEquals(BookingStatus.COMPLETED, Booking(status = "COMPLETED").bookingStatus)
    }

    // ------------------------------------------------ dates and slots

    @Test
    fun dates_todayPlusSevenDays() {
        val days = DateUtils.nextDays(7)
        assertEquals(8, days.size)
        assertEquals(DateUtils.today(), days.first())
        assertEquals(days.sorted(), days)              // in order, as plain text
        assertEquals(days.size, days.toSet().size)     // no duplicates
        assertEquals("Today", DateUtils.chipLabel(days[0]))
        assertEquals("Tomorrow", DateUtils.chipLabel(days[1]))
    }

    @Test
    fun dates_prettyFormat() {
        assertEquals("Thu, 5 Nov", DateUtils.pretty("2026-11-05"))
        assertEquals("", DateUtils.dateTime(0L))
    }

    @Test
    fun slots_closeWhenTheirTimeIsOver() {
        val (morning, afternoon, evening) = FixedData.timeSlots
        // at 11 AM everything is still open
        assertFalse(FixedData.isSlotOverToday(morning, 11))
        // at 12 PM morning is over
        assertTrue(FixedData.isSlotOverToday(morning, 12))
        assertFalse(FixedData.isSlotOverToday(afternoon, 12))
        // at 8 PM everything is over
        assertTrue(FixedData.isSlotOverToday(evening, 20))
        // unknown slot text is never "over"
        assertFalse(FixedData.isSlotOverToday("Midnight", 23))
    }

    // ------------------------------------------------ sample data

    @Test
    fun sampleData_isComplete() {
        // every category has 3-5 services, and ids are unique
        FixedData.categories.forEach { category ->
            val count = SampleData.services.count { it.categoryId == category.id }
            assertTrue("${category.name} has $count services", count in 3..5)
        }
        assertEquals(SampleData.services.size, SampleData.services.map { it.id }.toSet().size)
        // every category has 2-3 approved, available providers
        FixedData.categories.forEach { category ->
            val count = SampleData.providers.count { it.categoryId == category.id }
            assertTrue("${category.name} has $count providers", count in 2..3)
        }
        assertTrue(SampleData.providers.all { it.isAvailable && it.area in FixedData.areas })
        // example prices from the project brief
        assertEquals(149, SampleData.services.first { it.name == "Tap repair" }.price)
        assertEquals(1999, SampleData.services.first { it.name == "Gas refill" }.price)
    }

    // ------------------------------------------------ provider job lists

    private val today = DateUtils.today()
    private val jobs = listOf(
        Booking(id = "r1", status = "REQUESTED", date = today),
        Booking(id = "a1", status = "ACCEPTED", date = today, totalAmount = 198),
        Booking(id = "a2", status = "ACCEPTED", date = "2099-01-01"),
        Booking(id = "p1", status = "IN_PROGRESS", date = today),
        Booking(id = "c1", status = "COMPLETED", completedAt = System.currentTimeMillis(), totalAmount = 198),
        Booking(id = "c2", status = "COMPLETED", completedAt = 1L, totalAmount = 500),   // long ago
        Booking(id = "x1", status = "CANCELLED"),
        Booking(id = "j1", status = "REJECTED"),
    )

    @Test
    fun providerLists_groupJobsCorrectly() {
        assertEquals(listOf("r1"), ProviderJobsViewModel.newRequests(jobs).map { it.id })
        assertEquals(setOf("a1", "a2", "p1"), ProviderJobsViewModel.upcoming(jobs).map { it.id }.toSet())
        assertEquals(setOf("a1", "p1"), ProviderJobsViewModel.todaysJobs(jobs).map { it.id }.toSet())
        assertEquals(setOf("c1", "c2", "x1", "j1"), ProviderJobsViewModel.history(jobs).map { it.id }.toSet())
    }

    @Test
    fun providerEarnings_todayCountsOnlyJobsCompletedToday() {
        assertEquals(198, ProviderJobsViewModel.todaysEarnings(jobs))
    }

    @Test
    fun pricing_totalIsServicePlusVisitFee() {
        val tapRepair = SampleData.services.first { it.name == "Tap repair" }
        assertEquals(198, tapRepair.price + FixedData.VISIT_FEE)
    }
}
