package com.example.servicehub.ui.booking

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.servicehub.data.model.Booking
import com.example.servicehub.data.model.Complaint
import com.example.servicehub.data.model.ExtraItem
import com.example.servicehub.data.model.Provider
import com.example.servicehub.data.repository.ProviderRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import com.example.servicehub.data.repository.AuthRepository
import com.example.servicehub.data.repository.BookingRepository
import com.example.servicehub.data.repository.ComplaintRepository
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

    /** Problems the logged-in customer reported about this booking (only listened to on the customer screen). */
    val myComplaints: StateFlow<List<Complaint>> =
        ComplaintRepository.forBooking(AuthRepository.currentUserId.orEmpty(), bookingId)
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** The booking's provider, live (for their weekly days off in the reschedule popup). */
    @OptIn(ExperimentalCoroutinesApi::class)
    val provider: StateFlow<Provider?> = booking
        .map { (it as? BookingLoad.Loaded)?.booking?.providerId.orEmpty() }
        .distinctUntilChanged()
        .flatMapLatest { id -> if (id.isEmpty()) flowOf(null) else ProviderRepository.liveProvider(id) }
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Live busy slots of this booking's provider on [date] (used by the reschedule popup). */
    fun busySlotsFor(providerId: String, date: String): Flow<Set<String>> =
        BookingRepository.busySlots(providerId, date).catch { emit(emptySet()) }

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

    fun reportProblem(booking: Booking, message: String) =
        run("Thanks. The admin will look into it.") { ComplaintRepository.report(booking, message) }

    // ---- provider actions
    fun accept() = run("Job accepted.") { BookingRepository.accept(bookingId) }

    fun reject(reason: String, onDone: () -> Unit) =
        run("Job rejected.", onDone) { BookingRepository.reject(bookingId, reason) }

    fun start(pin: String, onDone: () -> Unit) =
        run("PIN correct. Job started!", onDone) { BookingRepository.startJob(bookingId, pin) }

    fun cancelByProvider(reason: String) =
        run("Job cancelled. The customer has been told.") { BookingRepository.cancelByProvider(bookingId, reason) }

    fun complete(paymentMode: String, extras: List<ExtraItem>, paidNow: Boolean, onDone: () -> Unit) =
        run(if (paidNow) "Job completed. Payment recorded." else "Job completed. The customer will pay with UPI in the app.", onDone) {
            BookingRepository.complete(bookingId, paymentMode, extras, paidNow)
        }

    /** Razorpay said the payment worked: record it on the booking. */
    fun markPaidOnline(paymentId: String) =
        run("Payment successful. Thank you!") { BookingRepository.markPaidOnline(bookingId, paymentId) }

    /** Show a payment problem (cancelled / failed) under the buttons. */
    fun showMessage(text: String) { message.value = text }

    fun markOnTheWay() = run("Customer told you are on the way.") { BookingRepository.markOnTheWay(bookingId) }

    fun markRevisitDone() = run("Marked as redone. The customer has been told.") { BookingRepository.markRevisitDone(bookingId) }

    // ---- customer: 7-day guarantee
    fun requestRevisit(reason: String) =
        run("Complaint sent. The professional will contact you to redo the job free.") { BookingRepository.requestRevisit(bookingId, reason) }
}
