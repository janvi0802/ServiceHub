package com.example.servicehub.ui.provider

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.servicehub.data.DateUtils
import com.example.servicehub.data.model.Booking
import com.example.servicehub.data.model.BookingStatus
import com.example.servicehub.data.model.Provider
import com.example.servicehub.data.model.Review
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

        /** Requests still waiting for an answer (expired ones go to history). */
        fun newRequests(all: List<Booking>) =
            all.filter { it.bookingStatus == BookingStatus.REQUESTED && !it.isExpired }.sortedBy { it.date + it.timeSlot }

        /** Accepted or in progress, earliest date first. */
        fun upcoming(all: List<Booking>) =
            all.filter { it.bookingStatus == BookingStatus.ACCEPTED || it.bookingStatus == BookingStatus.IN_PROGRESS }
                .sortedBy { it.date + it.timeSlot }

        fun todaysJobs(all: List<Booking>) = upcoming(all).filter { it.date == DateUtils.today() }

        fun history(all: List<Booking>) = all.filter { !it.isOpen }

        /** The soonest accepted / in-progress job (what the provider should do next). */
        fun nextJob(all: List<Booking>): Booking? = upcoming(all).firstOrNull()

        /**
         * Earnings for each of the last 7 days (oldest first), e.g. [("Mon", 0), ... ("Sun", 198)].
         * A job counts on the day it was completed.
         */
        fun earningsLast7Days(all: List<Booking>, now: Long = System.currentTimeMillis()): List<Pair<String, Int>> {
            val completed = all.filter { it.bookingStatus == BookingStatus.COMPLETED && it.completedAt > 0 }
            return DateUtils.lastDays(7, now).map { day ->
                val label = DateUtils.weekdayShortNames[(DateUtils.weekdayOf(day) - 1).coerceIn(0, 6)]
                label to completed.filter { DateUtils.dateOf(it.completedAt) == day }.sumOf { it.totalAmount }
            }
        }

        fun todaysEarnings(all: List<Booking>) = all
            .filter { it.bookingStatus == BookingStatus.COMPLETED && DateUtils.isToday(it.completedAt) }
            .sumOf { it.totalAmount }
    }
}
