package com.example.servicehub.ui.booking

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.servicehub.data.model.Booking
import com.example.servicehub.data.repository.BookingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What the detail screen shows: still loading, not found, or the booking. */
sealed class BookingLoad {
    data object Loading : BookingLoad()
    data object NotFound : BookingLoad()
    data class Loaded(val booking: Booking) : BookingLoad()
}

/**
 * Used by BOTH the customer's Booking detail and the provider's Job detail screens.
 * The booking is LIVE: when the other phone changes the status, this screen updates by itself.
 */
class BookingDetailViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    private val bookingId: String = savedStateHandle["bookingId"] ?: ""

    val booking: StateFlow<BookingLoad> = BookingRepository.liveBooking(bookingId)
        .map { if (it == null) BookingLoad.NotFound else BookingLoad.Loaded(it) }
        .catch { emit(BookingLoad.NotFound) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BookingLoad.Loading)

    /** True while an action is being saved (buttons are disabled). */
    val isBusy = MutableStateFlow(false)

    /** Error or success message shown under the buttons. */
    val message = MutableStateFlow<String?>(null)

    /**
     * Runs one action (accept, cancel, ...) with busy/error handling in one place.
     * [onSuccess] runs only if the action worked (e.g. close a dialog).
     */
    private fun run(successText: String? = null, onSuccess: () -> Unit = {}, action: suspend () -> Unit) {
        if (isBusy.value) return
        isBusy.value = true
        message.value = null
        viewModelScope.launch {
            try {
                action()
                message.value = successText
                onSuccess()
            } catch (e: Exception) {
                message.value = BookingRepository.friendlyError(e)
            } finally {
                isBusy.value = false
            }
        }
    }

    fun clearMessage() { message.value = null }

    // ---- customer actions
    fun cancel(reason: String, onDone: () -> Unit) =
        run("Booking cancelled.", onDone) { BookingRepository.cancel(bookingId, reason) }

    fun reschedule(current: Booking, date: String, slot: String, onDone: () -> Unit) =
        run("Rescheduled. Waiting for the provider to confirm.", onDone) {
            BookingRepository.reschedule(current, date, slot)
        }

    // ---- provider actions
    fun accept() = run("Job accepted.") { BookingRepository.accept(bookingId) }

    fun reject(reason: String, onDone: () -> Unit) =
        run("Job rejected.", onDone) { BookingRepository.reject(bookingId, reason) }

    fun start(pin: String, onDone: () -> Unit) =
        run("PIN correct. Job started!", onDone) { BookingRepository.startJob(bookingId, pin) }

    fun cancelByProvider(reason: String) =
        run("Job cancelled. The customer has been told.") { BookingRepository.cancelByProvider(bookingId, reason) }

    fun complete(paymentMode: String, onDone: () -> Unit) =
        run("Job completed. Payment recorded.", onDone) { BookingRepository.complete(bookingId, paymentMode) }
}
