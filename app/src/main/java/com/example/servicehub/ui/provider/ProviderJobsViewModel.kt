package com.example.servicehub.ui.provider

import com.example.servicehub.data.model.PaymentStatus
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.servicehub.data.DateUtils
import com.example.servicehub.data.model.Booking
import com.example.servicehub.data.model.BookingStatus
import com.example.servicehub.data.model.Provider
import com.example.servicehub.data.model.Review
import com.example.servicehub.data.model.RevisitStatus
import com.example.servicehub.data.repository.AuthRepository
import com.example.servicehub.data.repository.BookingRepository
import com.example.servicehub.data.repository.ProviderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Live data for the provider's Home, Requests and History screens:
 * the provider's own profile (availability, totals) and all their bookings.
 */
class ProviderJobsViewModel : ViewModel() {
    private val providerId = AuthRepository.currentUserId.orEmpty()

    val provider: StateFlow<Provider?> = ProviderRepository.liveProvider(providerId)
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** null = still loading */
    val bookings: StateFlow<List<Booking>?> = BookingRepository.providerBookings(providerId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Reviews customers wrote about this provider, newest first. */
    val reviews: StateFlow<List<Review>> = ProviderRepository.reviewsFor(providerId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val message = MutableStateFlow<String?>(null)

    fun setAvailable(available: Boolean) = viewModelScope.launch {
        try {
            ProviderRepository.setAvailability(providerId, available)
            message.value = null
        } catch (e: Exception) {
            message.value = "Could not change availability. Check internet."
        }
    }

    /** Tick or untick one weekly day off (1 = Sunday ... 7 = Saturday). Saves at once. */
    fun toggleOffDay(day: Int) = viewModelScope.launch {
        val current = provider.value?.offDays.orEmpty()
        val updated = if (day in current) current - day else current + day
        try {
            ProviderRepository.setOffDays(providerId, updated)
            message.value = null
        } catch (e: Exception) {
            message.value = "Could not save days off. Check internet."
        }
    }

    /** "Work today" on a day off (or undo it). Only today changes; the weekly days off stay. */
    fun setWorkToday(work: Boolean) = viewModelScope.launch {
        try {
            ProviderRepository.setExtraWorkDate(providerId, DateUtils.today(), work)
            message.value = null
        } catch (e: Exception) {
            message.value = "Could not save. Check internet."
        }
    }

    companion object {
        // Small pure functions that sort bookings into groups. "companion object" = no instance needed.

        /**
         * Sort key "2026-11-20 0": the date, then the slot's position in the day
         * (0 = Morning, 1 = Afternoon, 2 = Evening). Sorting by the slot's NAME would put
         * "Evening" before "Morning" (alphabetical order), which was a bug.
         */
        fun timeOrder(b: Booking) = "${b.date} ${com.example.servicehub.data.FixedData.timeSlots.indexOf(b.timeSlot)}"

        /** Requests still waiting for an answer (expired ones go to history). */
        fun newRequests(all: List<Booking>) =
            all.filter { it.bookingStatus == BookingStatus.REQUESTED && !it.isExpired }.sortedBy(::timeOrder)

        /** Job done, money not received yet: the provider must collect cash or wait for UPI. Oldest first. */
        fun paymentsToCollect(all: List<Booking>) =
            all.filter { it.bookingStatus == BookingStatus.COMPLETED && it.paymentStatus != PaymentStatus.PAID }
                .sortedBy { it.completedAt }

        /** Completed jobs where the customer asked for a free revisit (7-day guarantee). */
        fun revisits(all: List<Booking>) =
            all.filter { it.revisitStatus == RevisitStatus.REQUESTED }.sortedBy { it.revisitRequestedAt }

        /** Accepted or in progress, earliest date first. */
        fun upcoming(all: List<Booking>) =
            all.filter { it.bookingStatus == BookingStatus.ACCEPTED || it.bookingStatus == BookingStatus.IN_PROGRESS }
                .sortedBy(::timeOrder)

        fun todaysJobs(all: List<Booking>) = upcoming(all).filter { it.date == DateUtils.today() }

        fun history(all: List<Booking>) = all.filter { !it.isOpen }

        /** The soonest accepted / in-progress job (what the provider should do next). */
        fun nextJob(all: List<Booking>): Booking? = upcoming(all).firstOrNull()

        /**
         * Earnings for each of the last 7 days (oldest first), e.g. [("Mon", 0), ... ("Sun", 198)].
         * A PAID job counts on the day it was completed (same rule as Today).
         */
        fun earningsLast7Days(all: List<Booking>, now: Long = System.currentTimeMillis()): List<Pair<String, Int>> {
            val completed = paidJobs(all).filter { it.completedAt > 0 }   // only money received
            return DateUtils.lastDays(7, now).map { day ->
                val label = DateUtils.weekdayShortNames[(DateUtils.weekdayOf(day) - 1).coerceIn(0, 6)]
                label to completed.filter { DateUtils.dateOf(it.completedAt) == day }.sumOf { it.providerEarning }
            }
        }

        /** Completed jobs whose money has really been received (cash collected or UPI paid). */
        private fun paidJobs(all: List<Booking>) =
            all.filter { it.bookingStatus == BookingStatus.COMPLETED && it.paymentStatus == PaymentStatus.PAID }

        /** What the provider received today (their share: total minus the platform's visit fee). */
        fun todaysEarnings(all: List<Booking>) = paidJobs(all)
            .filter { DateUtils.isToday(it.completedAt) }
            .sumOf { it.providerEarning }

        /** Everything received so far (only paid jobs). */
        fun paidEarnings(all: List<Booking>) = paidJobs(all).sumOf { it.providerEarning }

        /** Job done but money not received yet (cash not confirmed / UPI not paid). Full amount to collect. */
        fun pendingPayments(all: List<Booking>) = paymentsToCollect(all).sumOf { it.totalAmount }
    }
}
