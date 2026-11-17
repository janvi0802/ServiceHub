package com.example.servicehub

import com.example.servicehub.data.DateUtils
import com.example.servicehub.data.FixedData
import com.example.servicehub.data.SampleData
import com.example.servicehub.data.model.ApprovalStatus
import com.example.servicehub.data.model.Booking
import com.example.servicehub.data.model.BookingStatus
import com.example.servicehub.data.model.Provider
import com.example.servicehub.data.model.Roles
import com.example.servicehub.data.model.User
import com.example.servicehub.data.model.isRequestExpired
import com.example.servicehub.data.model.isAcceptedOverdue
import com.example.servicehub.data.model.isTopRated
import com.example.servicehub.ui.components.avatarColorIndex
import com.example.servicehub.ui.components.receiptShareText
import com.example.servicehub.ui.components.timelineSteps
import com.example.servicehub.ui.customer.CustomerHomeViewModel
import com.example.servicehub.ui.customer.ProviderSort
import com.example.servicehub.ui.customer.matchesSearch
import com.example.servicehub.ui.customer.sortProviders
import com.example.servicehub.data.repository.newAverage
import com.example.servicehub.data.repository.slotDocId
import com.example.servicehub.ui.admin.DashboardStats
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

    // ------------------------------------------------ Phase 10: rating average

    @Test
    fun rating_newAverageIsCorrect() {
        assertEquals(5.0, newAverage(0.0, 0, 5), 0.0001)     // first rating
        assertEquals(4.25, newAverage(4.0, 3, 5), 0.0001)    // (4*3+5)/4
        assertEquals(3.0, newAverage(5.0, 1, 1), 0.0001)     // (5+1)/2
    }

    // ------------------------------------------------ Phase 11: admin dashboard counts

    @Test
    fun dashboard_countsEverything() {
        val users = listOf(
            User(id = "c1", role = Roles.CUSTOMER),
            User(id = "c2", role = Roles.CUSTOMER, isBlocked = true),
            User(id = "p1", role = Roles.PROVIDER),
            User(id = "a1", role = Roles.ADMIN),
        )
        val providers = listOf(
            Provider(id = "p1", approvalStatus = ApprovalStatus.APPROVED),
            Provider(id = "p2", approvalStatus = ApprovalStatus.PENDING),
            Provider(id = "p3", approvalStatus = ApprovalStatus.REJECTED),
        )
        val stats = DashboardStats.from(users, providers, jobs)
        assertEquals(2, stats.customers)
        assertEquals(1, stats.approvedProviders)
        assertEquals(1, stats.pendingProviders)
        assertEquals(1, stats.blockedUsers)
        assertEquals(8, stats.totalBookings)
        assertEquals(2, stats.bookingsByStatus[BookingStatus.COMPLETED])
        assertEquals(2, stats.bookingsByStatus[BookingStatus.ACCEPTED])
        assertEquals(698, stats.completedValue)   // 198 + 500
        // every status has a number, even if 0
        assertEquals(BookingStatus.entries.size, stats.bookingsByStatus.size)
    }

    // ------------------------------------------------ busy-slot ids (double-booking protection)

    @Test
    fun slotIds_sameSlotSameId_differentSlotDifferentId() {
        val (morning, afternoon) = FixedData.timeSlots
        val a = slotDocId("prov1", "2026-11-20", morning)
        // the same provider, date and slot always gives the same id -> only one booking can hold it
        assertEquals(a, slotDocId("prov1", "2026-11-20", morning))
        assertEquals("prov1_2026-11-20_0", a)
        // anything different -> a different id
        assertFalse(a == slotDocId("prov1", "2026-11-20", afternoon))
        assertFalse(a == slotDocId("prov1", "2026-11-21", morning))
        assertFalse(a == slotDocId("prov2", "2026-11-20", morning))
    }

    // ------------------------------------------------ expired requests

    @Test
    fun expiry_onlyUnansweredRequestsAfterTheirTime() {
        val (morning, _, evening) = FixedData.timeSlots
        val today = "2026-11-20"
        fun req(date: String, slot: String, status: String = "REQUESTED") =
            Booking(status = status, date = date, timeSlot = slot)

        // yesterday, still requested -> expired
        assertTrue(isRequestExpired(req("2026-11-19", evening), today, 9))
        // today morning at 1 PM -> expired; at 10 AM -> not yet
        assertTrue(isRequestExpired(req(today, morning), today, 13))
        assertFalse(isRequestExpired(req(today, morning), today, 10))
        // future date -> never expired
        assertFalse(isRequestExpired(req("2026-11-21", morning), today, 23))
        // accepted / completed bookings never "expire"
        assertFalse(isRequestExpired(req("2026-11-19", morning, "ACCEPTED"), today, 9))
        assertFalse(isRequestExpired(req("2026-11-19", morning, "COMPLETED"), today, 9))
    }

    // ------------------------------------------------ polish features

    @Test
    fun overdue_onlyAcceptedJobsFromPastDays() {
        val today = "2026-11-20"
        assertTrue(isAcceptedOverdue(Booking(status = "ACCEPTED", date = "2026-11-19"), today))
        assertFalse(isAcceptedOverdue(Booking(status = "ACCEPTED", date = today), today))
        assertFalse(isAcceptedOverdue(Booking(status = "IN_PROGRESS", date = "2026-11-19"), today))
    }

    @Test
    fun topRated_needsHighRatingAndEnoughReviews() {
        assertTrue(isTopRated(Provider(ratingAvg = 4.6, ratingCount = 3)))
        assertFalse(isTopRated(Provider(ratingAvg = 5.0, ratingCount = 1)))   // one review is not enough
        assertFalse(isTopRated(Provider(ratingAvg = 4.2, ratingCount = 10)))
    }

    @Test
    fun avatarColor_sameNameSameColour() {
        assertEquals(avatarColorIndex("Ramesh Patel"), avatarColorIndex("  ramesh patel "))
        assertTrue(avatarColorIndex("Any Name") in 0..7)
    }

    @Test
    fun providerSort_ordersBestFirst() {
        val a = Provider(id = "a", name = "A", ratingAvg = 4.0, experienceYears = 10, jobsCompleted = 1)
        val b = Provider(id = "b", name = "B", ratingAvg = 4.8, experienceYears = 2, jobsCompleted = 9)
        val c = Provider(id = "c", name = "C", ratingAvg = 3.0, experienceYears = 5, jobsCompleted = 20)
        val list = listOf(a, b, c)
        assertEquals(listOf("b", "a", "c"), sortProviders(list, ProviderSort.RATING).map { it.id })
        assertEquals(listOf("a", "c", "b"), sortProviders(list, ProviderSort.EXPERIENCE).map { it.id })
        assertEquals(listOf("c", "b", "a"), sortProviders(list, ProviderSort.JOBS).map { it.id })
    }

    @Test
    fun timeline_showsOnlyWhatHappened() {
        val done = Booking(status = "COMPLETED", createdAt = 1, acceptedAt = 2, startedAt = 3, completedAt = 4)
        assertEquals(listOf("Requested", "Accepted", "Started", "Completed"), timelineSteps(done).map { it.first })
        val waiting = Booking(status = "REQUESTED", createdAt = 1)
        assertEquals(listOf("Requested"), timelineSteps(waiting).map { it.first })
        val cancelled = Booking(status = "CANCELLED", createdAt = 1, updatedAt = 5)
        assertEquals(listOf("Requested", "Cancelled"), timelineSteps(cancelled).map { it.first })
    }

    @Test
    fun customerHome_upcomingAndRateReminder() {
        val (morning, afternoon) = FixedData.timeSlots
        val list = listOf(
            Booking(id = "later", status = "ACCEPTED", date = "2099-01-02", timeSlot = morning),
            Booking(id = "soon", status = "REQUESTED", date = "2099-01-01", timeSlot = afternoon),
            Booking(id = "soonest", status = "ACCEPTED", date = "2099-01-01", timeSlot = morning),
            Booking(id = "old", status = "COMPLETED", completedAt = 10, isRated = false),
            Booking(id = "newer", status = "COMPLETED", completedAt = 20, isRated = false),
            Booking(id = "rated", status = "COMPLETED", completedAt = 30, isRated = true),
        )
        assertEquals("soonest", CustomerHomeViewModel.nextUpcoming(list)?.id)
        assertEquals("newer", CustomerHomeViewModel.rateReminder(list)?.id)
        assertEquals(null, CustomerHomeViewModel.nextUpcoming(emptyList()))
    }

    @Test
    fun customerHome_fromPriceIgnoresHiddenServices() {
        val services = SampleData.services.map { if (it.name == "Tap repair") it.copy(isActive = false) else it }
        val prices = CustomerHomeViewModel.minPriceByCategory(services)
        assertEquals(299, prices["plumber"])   // tap repair (149) hidden -> pipe leakage (299)
        assertEquals(99, prices["electrician"])
    }

    @Test
    fun search_matchesServiceOrCategoryName() {
        val tap = SampleData.services.first { it.name == "Tap repair" }
        assertTrue(matchesSearch(tap, "tap"))
        assertTrue(matchesSearch(tap, "PLUMB"))
        assertFalse(matchesSearch(tap, "fan"))
    }

    @Test
    fun dashboard_topProvidersAndExpiredCount() {
        val providers = listOf(
            Provider(id = "x", approvalStatus = ApprovalStatus.APPROVED, ratingAvg = 4.9, ratingCount = 5),
            Provider(id = "y", approvalStatus = ApprovalStatus.APPROVED, ratingAvg = 4.9, ratingCount = 2, jobsCompleted = 8),
            Provider(id = "z", approvalStatus = ApprovalStatus.APPROVED),                    // no work yet -> not listed
            Provider(id = "p", approvalStatus = ApprovalStatus.PENDING, ratingAvg = 5.0, ratingCount = 9), // not approved
        )
        assertEquals(listOf("y", "x"), DashboardStats.topProviders(providers).map { it.id })

        val expired = Booking(status = "REQUESTED", date = "2000-01-01", timeSlot = FixedData.timeSlots[0])
        val stats = DashboardStats.from(emptyList(), emptyList(), listOf(expired))
        assertEquals(1, stats.expiredRequests)
        assertEquals(0, stats.bookingsByStatus[BookingStatus.REQUESTED])
    }

    @Test
    fun receiptText_hasTheImportantFacts() {
        val b = Booking(id = "abcdefghij", serviceName = "Tap repair", servicePrice = 149, visitFee = 49, totalAmount = 198, paymentMode = "UPI")
        val text = receiptShareText(b)
        assertTrue(text.contains("ABCDEFGH"))
        assertTrue(text.contains("Tap repair"))
        assertTrue(text.contains("₹198 by UPI"))
    }

    @Test
    fun pricing_totalIsServicePlusVisitFee() {
        val tapRepair = SampleData.services.first { it.name == "Tap repair" }
        assertEquals(198, tapRepair.price + FixedData.VISIT_FEE)
    }
}
