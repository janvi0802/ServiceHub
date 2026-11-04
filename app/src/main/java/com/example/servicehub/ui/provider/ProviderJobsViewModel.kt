package com.example.servicehub.ui.provider

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.servicehub.data.DateUtils
import com.example.servicehub.data.model.Booking
import com.example.servicehub.data.model.BookingStatus
import com.example.servicehub.data.model.Provider
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

    val message = MutableStateFlow<String?>(null)

    fun setAvailable(available: Boolean) = viewModelScope.launch {
        try {
            ProviderRepository.setAvailability(providerId, available)
            message.value = null
        } catch (e: Exception) {
            message.value = "Could not change availability. Check internet."
        }
    }

    companion object {
        // Small pure functions that sort bookings into groups. "companion object" = no instance needed.

        fun newRequests(all: List<Booking>) =
            all.filter { it.bookingStatus == BookingStatus.REQUESTED }.sortedBy { it.date + it.timeSlot }

        /** Accepted or in progress, earliest date first. */
        fun upcoming(all: List<Booking>) =
            all.filter { it.bookingStatus == BookingStatus.ACCEPTED || it.bookingStatus == BookingStatus.IN_PROGRESS }
                .sortedBy { it.date + it.timeSlot }

        fun todaysJobs(all: List<Booking>) = upcoming(all).filter { it.date == DateUtils.today() }

        fun history(all: List<Booking>) = all.filter { !it.bookingStatus.isActive }

        fun todaysEarnings(all: List<Booking>) = all
            .filter { it.bookingStatus == BookingStatus.COMPLETED && DateUtils.isToday(it.completedAt) }
            .sumOf { it.totalAmount }
    }
}
