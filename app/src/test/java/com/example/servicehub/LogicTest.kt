package com.example.servicehub

import com.example.servicehub.data.CategoryDoc
import com.example.servicehub.data.CategoryStore
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
import com.example.servicehub.data.model.isOffOn
import com.example.servicehub.data.model.nextWorkingDay
import com.example.servicehub.data.model.canRequestRevisitAt
import com.example.servicehub.data.model.Service
import com.example.servicehub.data.model.includedPoints
import com.example.servicehub.data.model.notIncludedPoints
import com.example.servicehub.ui.customer.FavoritesViewModel
import com.example.servicehub.ui.common.notificationStyle
import com.example.servicehub.ui.customer.MyBookingsViewModel
import com.example.servicehub.ui.theme.StatusAccepted
import com.example.servicehub.ui.theme.StatusCompleted
import com.example.servicehub.ui.theme.StatusRejected
import com.example.servicehub.ui.admin.userMatches
import com.example.servicehub.ui.admin.canSendPasswordReset
import com.example.servicehub.ui.admin.providerIdsInCategory
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
        assertEquals("New", ratingText(0.0, 0))
        assertEquals("4.5 (2)", ratingText(4.5, 2))
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
        Booking(id = "c1", status = "COMPLETED", completedAt = System.currentTimeMillis(), totalAmount = 198, paymentStatus = "PAID"),        Booking(id = "c2", status = "COMPLETED", completedAt = 1L, totalAmount = 500),   // long ago
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
    fun coupon_discountRules() {
        val flat = com.example.servicehub.data.model.Coupon(code = "FIRST50", type = "FLAT", value = 50, minOrder = 199)
        assertEquals(50, com.example.servicehub.data.model.couponDiscount(flat, 348).getOrNull())
        assertEquals(null, com.example.servicehub.data.model.couponDiscount(flat, 150).getOrNull())          // below minimum
        val pct = com.example.servicehub.data.model.Coupon(code = "TEN", type = "PERCENT", value = 10, maxDiscount = 100)
        assertEquals(34, com.example.servicehub.data.model.couponDiscount(pct, 348).getOrNull())
        assertEquals(100, com.example.servicehub.data.model.couponDiscount(pct, 2048).getOrNull())          // capped
        assertEquals(null, com.example.servicehub.data.model.couponDiscount(flat.copy(isActive = false), 348).getOrNull())
        // Provider keeps their full share: 348 - 50 coupon = 298 paid, provider still gets 348 - 49
        val b = Booking(totalAmount = 298, visitFee = 49, discount = 50)
        assertEquals(299, b.providerEarning)
    }

    @Test
    fun slotHold_bookedOnHoldOrFreeAfterOneHour() {
        val now = 1_000_000_000L
        val hold = com.example.servicehub.data.model.SlotInfo(heldUntil = now + 60_000)
        assertEquals(com.example.servicehub.data.model.SlotState.ON_HOLD, com.example.servicehub.data.model.slotStateAt(hold, now))
        assertEquals(null, com.example.servicehub.data.model.slotStateAt(hold, now + 120_000))          // hour over: free
        val booked = hold.copy(accepted = true)
        assertEquals(com.example.servicehub.data.model.SlotState.BOOKED, com.example.servicehub.data.model.slotStateAt(booked, now + 999_999))
        val old = com.example.servicehub.data.model.SlotInfo()                                            // old document: no heldUntil
        assertEquals(com.example.servicehub.data.model.SlotState.ON_HOLD, com.example.servicehub.data.model.slotStateAt(old, now))
    }

    @Test
    fun providerLists_sortMorningBeforeEvening() {
        val day = "2026-12-01"
        val list = listOf(
            Booking(id = "e", status = "ACCEPTED", date = day, timeSlot = com.example.servicehub.data.FixedData.timeSlots[2]),
            Booking(id = "m", status = "ACCEPTED", date = day, timeSlot = com.example.servicehub.data.FixedData.timeSlots[0]),
            Booking(id = "a", status = "ACCEPTED", date = day, timeSlot = com.example.servicehub.data.FixedData.timeSlots[1]),
        )
        assertEquals(listOf("m", "a", "e"), ProviderJobsViewModel.upcoming(list).map { it.id })
    }

    @Test
    fun nextStep_tellsEachSideWhatToDo() {
        val unpaidUpi = Booking(status = "COMPLETED", paymentMode = "UPI", totalAmount = 348)
        assertEquals("Pay ₹348 with UPI: tap Pay now below.", com.example.servicehub.ui.booking.customerNextStep(unpaidUpi))
        val unpaidCash = Booking(status = "COMPLETED", paymentMode = "Cash", totalAmount = 198)
        assertEquals("Collect ₹198 in cash and tap Cash collected.", com.example.servicehub.ui.booking.providerNextStep(unpaidCash))
        val paidRated = Booking(status = "COMPLETED", paymentStatus = "PAID", isRated = true)
        assertEquals(null, com.example.servicehub.ui.booking.customerNextStep(paidRated))
    }

    @Test
    fun providerEarnings_todayCountsOnlyPaidJobsCompletedToday() {
        val withUnpaid = jobs + Booking(id = "u1", status = "COMPLETED", completedAt = System.currentTimeMillis(), totalAmount = 300)
        assertEquals(198, ProviderJobsViewModel.todaysEarnings(withUnpaid))      // u1 is not paid yet
        assertEquals(800, ProviderJobsViewModel.pendingPayments(withUnpaid))     // u1 300 + c2 500 (old, unpaid)
        assertEquals(198, ProviderJobsViewModel.paidEarnings(withUnpaid))
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
        // "now" just after the jobs finished -> newest unrated one is shown
        assertEquals("newer", CustomerHomeViewModel.rateReminder(list, now = 100)?.id)
        // 8 days later -> the reminder has gone
        val eightDays = 8L * 24 * 60 * 60 * 1000
        assertEquals(null, CustomerHomeViewModel.rateReminder(list, now = 20 + eightDays))
        assertEquals(null, CustomerHomeViewModel.nextUpcoming(emptyList()))
    }

    @Test
    fun bookableTime_refusesPastDatesAndFinishedSlots() {
        val (morning, _, evening) = FixedData.timeSlots
        val today = "2026-11-20"
        assertFalse(FixedData.isBookableTime("2026-11-19", evening, today, 8))   // yesterday
        assertFalse(FixedData.isBookableTime(today, morning, today, 12))         // morning over at 12
        assertTrue(FixedData.isBookableTime(today, evening, today, 12))          // evening still open
        assertTrue(FixedData.isBookableTime("2026-11-21", morning, today, 23))   // tomorrow
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

    // ------------------------------------------------ weekly days off

    @Test
    fun weekdays_readCorrectlyFromDates() {
        assertEquals(1, DateUtils.weekdayOf("2026-11-22"))   // Sunday
        assertEquals(2, DateUtils.weekdayOf("2026-11-23"))   // Monday
        assertEquals(7, DateUtils.weekdayOf("2026-11-28"))   // Saturday
        assertEquals(0, DateUtils.weekdayOf("not a date"))
        assertEquals("Sun, Sat", DateUtils.weekdayNames(listOf(7, 1)))
    }

    @Test
    fun daysOff_blockOnlyThatWeekday() {
        val sundayOff = Provider(offDays = listOf(1))
        assertTrue(isOffOn(sundayOff, "2026-11-22"))          // Sunday
        assertFalse(isOffOn(sundayOff, "2026-11-23"))         // Monday
        assertFalse(isOffOn(Provider(), "2026-11-22"))        // no days off -> always working
    }

    @Test
    fun adminUserSearch_matchesNameEmailOrPhone() {
        val u = User(name = "Priya Sharma", email = "priya@test.com", phone = "9876543210")
        assertTrue(userMatches(u, ""))
        assertTrue(userMatches(u, "priya"))
        assertTrue(userMatches(u, "TEST.COM"))
        assertTrue(userMatches(u, "98765"))
        assertFalse(userMatches(u, "ramesh"))
    }

    // ------------------------------------------------ admin-managed categories

    @Test
    fun categories_mergeBuiltInWithAdminChanges() {
        val docs = listOf(
            CategoryDoc(id = "painter", name = "Painter", iconKey = "paint", colorHex = 0xFFD81B60, isActive = false, order = 6), // hidden
            CategoryDoc(id = "abc", name = "Yoga Teacher", iconKey = "yoga", colorHex = 0xFF43A047, isActive = true, order = 7),   // new
            CategoryDoc(id = "", name = "Broken"),                                                                              // ignored
        )
        val merged = CategoryStore.merge(FixedData.defaultCategories, docs)
        assertEquals(7, merged.size)                                    // 6 built-in + 1 new
        assertFalse(merged.first { it.id == "painter" }.isActive)      // admin hid it
        assertEquals("Yoga Teacher", merged.last().name)                // new one goes last (order 7)
        // with no Firestore documents, the built-in list is unchanged
        assertEquals(FixedData.defaultCategories, CategoryStore.merge(FixedData.defaultCategories, emptyList()))
    }

    @Test
    fun customerHome_quickPicksAndBookAgain() {
        val picks = CustomerHomeViewModel.quickPicks(SampleData.services, FixedData.defaultCategories)
        assertEquals(6, picks.size)                                     // one per category
        assertEquals("Tap repair", picks.first().name)                  // cheapest plumber service

        val again = CustomerHomeViewModel.bookAgain(
            listOf(
                Booking(providerId = "p1", serviceId = "s1", status = "COMPLETED", completedAt = 1),
                Booking(providerId = "p1", serviceId = "s1", status = "COMPLETED", completedAt = 2),   // same pair -> once
                Booking(providerId = "p2", serviceId = "s1", status = "COMPLETED", completedAt = 3),
                Booking(providerId = "p3", serviceId = "s1", status = "CANCELLED", completedAt = 4),   // not completed
            )
        )
        assertEquals(listOf("p2", "p1"), again.map { it.providerId })
    }

    @Test
    fun providerWeekChart_countsEarningsPerDay() {
        val now = System.currentTimeMillis()
        val oneDay = 24L * 60 * 60 * 1000
        val jobs = listOf(
            Booking(status = "COMPLETED", completedAt = now, totalAmount = 198, paymentStatus = "PAID"),
            Booking(status = "COMPLETED", completedAt = now, totalAmount = 100, paymentStatus = "PAID"),
            Booking(status = "COMPLETED", completedAt = now - oneDay, totalAmount = 50, paymentStatus = "PAID"),
            Booking(status = "COMPLETED", completedAt = now, totalAmount = 700),                 // not paid: not counted
            Booking(status = "COMPLETED", completedAt = now - 10 * oneDay, totalAmount = 999),   // too old
            Booking(status = "CANCELLED", completedAt = now, totalAmount = 500),                // not earned
        )
        val week = ProviderJobsViewModel.earningsLast7Days(jobs, now)
        assertEquals(7, week.size)
        assertEquals(298, week.last().second)        // today
        assertEquals(50, week[5].second)             // yesterday
        assertEquals(348, week.sumOf { it.second })
    }

    // ------------------------------------------------ UI-round logic

    @Test
    fun workToday_opensOneDayOffOnly() {
        // Tuesday off, but chose to work on Tue 2026-11-17 only
        val p = Provider(offDays = listOf(3), extraWorkDates = listOf("2026-11-17"))
        assertFalse(isOffOn(p, "2026-11-17"))                       // working this Tuesday
        assertTrue(isOffOn(p, "2026-11-24"))                        // next Tuesday still off
        assertEquals("2026-11-18", nextWorkingDay(Provider(offDays = listOf(3)), listOf("2026-11-17", "2026-11-18")))
    }

    @Test
    fun myBookings_groupedTodayUpcomingAttention() {
        val today = "2026-11-20"
        val list = listOf(
            Booking(id = "t", status = "ACCEPTED", date = today),
            Booking(id = "u", status = "REQUESTED", date = "2026-11-22"),
            Booking(id = "o", status = "ACCEPTED", date = "2026-11-18"),
        )
        val groups = MyBookingsViewModel.groupActive(list, today)
        assertEquals(listOf("Today", "Upcoming", "Needs attention"), groups.map { it.first })
        assertEquals("t", groups[0].second.single().id)
    }

    @Test
    fun adminCharts_countPerDayAndRevenuePerCategory() {
        val now = System.currentTimeMillis()
        val bookings = listOf(
            Booking(createdAt = now, status = "COMPLETED", categoryId = "plumber", totalAmount = 198),
            Booking(createdAt = now, status = "COMPLETED", categoryId = "plumber", totalAmount = 348),
            Booking(createdAt = now, status = "COMPLETED", categoryId = "painter", totalAmount = 548),
            Booking(createdAt = now, status = "CANCELLED", categoryId = "painter", totalAmount = 999),   // not revenue
        )
        assertEquals(4, DashboardStats.bookingsPerDay(bookings, now).last().second)
        val revenue = DashboardStats.revenueByCategory(bookings)
        assertEquals(listOf("Painter" to 548, "Plumber" to 546), revenue)
    }

    @Test
    fun notifications_getTheRightIconColour() {
        assertEquals(StatusRejected, notificationStyle("Booking rejected").second)
        assertEquals(StatusRejected, notificationStyle("Account not approved").second)
        assertEquals(StatusAccepted, notificationStyle("Account approved").second)
        assertEquals(StatusCompleted, notificationStyle("Job completed").second)
    }

    // ------------------------------------------------ guarantee, extras, earnings split

    @Test
    fun revisit_onlyOnceWithinSevenDaysOfCompletion() {
        val day = 24L * 60 * 60 * 1000
        val done = Booking(status = "COMPLETED", completedAt = 1_000_000L)
        assertTrue(canRequestRevisitAt(done, 1_000_000L + 6 * day))                       // day 6: allowed
        assertFalse(canRequestRevisitAt(done, 1_000_000L + 8 * day))                      // day 8: too late
        assertFalse(canRequestRevisitAt(done.copy(revisitStatus = "REQUESTED"), 1_000_000L))   // only once
        assertFalse(canRequestRevisitAt(Booking(status = "ACCEPTED", completedAt = 1), 2))      // not completed
    }

    @Test
    fun earnings_visitFeeGoesToPlatform() {
        // Tap repair 149 + visit 49 + a part 180 = 378 paid by the customer
        val b = Booking(status = "COMPLETED", servicePrice = 149, visitFee = 49, extraAmount = 180, totalAmount = 378)
        assertEquals(329, b.providerEarning)                     // provider keeps service + part
        val stats = DashboardStats.from(emptyList(), emptyList(), listOf(b))
        assertEquals(49, stats.platformEarnings)
        assertEquals(329, stats.providerEarnings)
        assertEquals(378, stats.completedValue)
    }

    @Test
    fun providerJobs_revisitsListed() {
        val list = listOf(
            Booking(id = "r", status = "COMPLETED", revisitStatus = "REQUESTED"),
            Booking(id = "d", status = "COMPLETED", revisitStatus = "DONE"),
            Booking(id = "n", status = "COMPLETED"),
        )
        assertEquals(listOf("r"), ProviderJobsViewModel.revisits(list).map { it.id })
    }

    // ------------------------------------------------ admin filters and password reset

    @Test
    fun adminCategoryFilter_findsProvidersInThatCategory() {
        val providers = listOf(
            Provider(id = "p1", categoryId = "plumber"),
            Provider(id = "p2", categoryId = "painter"),
            Provider(id = "p3", categoryId = "plumber"),
        )
        assertEquals(setOf("p1", "p3"), providerIdsInCategory(providers, "plumber"))
        assertEquals(null, providerIdsInCategory(providers, null))   // no filter -> everyone
    }

    @Test
    fun passwordReset_onlyForRealEmails() {
        assertTrue(canSendPasswordReset(User(email = "priya@gmail.com")))
        assertFalse(canSendPasswordReset(User(email = "sample_plumber_1@sample.servicehub")))
        assertFalse(canSendPasswordReset(User(email = "")))
    }

    // ------------------------------------------------ service details and favourites

    @Test
    fun serviceDetails_includedTextAndDefaults() {
        val tap = SampleData.services.first { it.id == "plumber_tap_repair" }
        assertEquals(3, includedPoints(tap).size)                              // from the sample text
        assertTrue(notIncludedPoints(tap).first().contains("cartridge"))
        val bare = Service(name = "Geyser repair", description = "Fix geysers")
        assertEquals("Fix geysers", includedPoints(bare).first())                 // sensible default
        assertTrue(notIncludedPoints(bare).first().contains("Spare parts"))
        // every sample service has its own details text
        assertTrue(SampleData.services.all { it.included.isNotBlank() && it.notIncluded.isNotBlank() })
    }

    @Test
    fun favorites_openTheCheapestServiceOfTheirCategory() {
        val painter = Provider(id = "x", categoryId = "painter")
        assertEquals("painter_touch_up", FavoritesViewModel.defaultServiceFor(painter, SampleData.services)?.id)
        assertEquals(null, FavoritesViewModel.defaultServiceFor(Provider(categoryId = "unknown"), SampleData.services))
    }

    @Test
    fun pricing_totalIsServicePlusVisitFee() {
        val tapRepair = SampleData.services.first { it.name == "Tap repair" }
        assertEquals(198, tapRepair.price + FixedData.VISIT_FEE)
    }
}
