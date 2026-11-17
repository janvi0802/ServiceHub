package com.example.servicehub.data.repository

import com.example.servicehub.data.await
import com.example.servicehub.data.liveList
import com.example.servicehub.data.model.Booking
import com.example.servicehub.data.model.Complaint
import com.example.servicehub.data.model.ComplaintStatus
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * "Report a problem": a customer tells the admin something went wrong with a booking
 * (bad work, asked for extra money, provider did not come...). The admin sees them in a list.
 */
object ComplaintRepository {

    private val complaints = FirebaseFirestore.getInstance().collection("complaints")

    suspend fun report(booking: Booking, message: String) {
        if (message.isBlank()) throw BookingException("Please describe the problem.")
        val ref = complaints.document()
        ref.set(
            Complaint(
                id = ref.id, bookingId = booking.id,
                customerId = booking.customerId, customerName = booking.customerName, customerPhone = booking.customerPhone,
                providerId = booking.providerId, providerName = booking.providerName, providerPhone = booking.providerPhone,
                serviceName = booking.serviceName,
                message = message.trim(),
                status = ComplaintStatus.OPEN,
                createdAt = System.currentTimeMillis(),
            )
        ).await()
    }

    /**
     * The customer's own complaints about one booking (to show "Problem reported").
     * Both filters are needed: the security rules only let customers read their own complaints.
     */
    fun forBooking(customerId: String, bookingId: String): Flow<List<Complaint>> =
        complaints.whereEqualTo("customerId", customerId)
            .whereEqualTo("bookingId", bookingId)
            .liveList(Complaint::class.java)
            .map { list -> list.sortedByDescending { it.createdAt } }

    /** Admin: every complaint, open ones first, newest first. */
    fun all(): Flow<List<Complaint>> =
        complaints.liveList(Complaint::class.java)
            .map { list ->
                list.sortedWith(compareBy<Complaint> { it.status != ComplaintStatus.OPEN }.thenByDescending { it.createdAt })
            }

    /** Admin marks a complaint as handled, and the customer is told. */
    suspend fun resolve(complaint: Complaint) {
        complaints.document(complaint.id)
            .update(mapOf("status" to ComplaintStatus.RESOLVED, "resolvedAt" to System.currentTimeMillis()))
            .await()
        NotificationRepository.send(
            complaint.customerId, "Your problem was resolved",
            "The admin has looked into your report about ${complaint.serviceName}. Thank you for telling us.",
            complaint.bookingId,
        )
    }
}
