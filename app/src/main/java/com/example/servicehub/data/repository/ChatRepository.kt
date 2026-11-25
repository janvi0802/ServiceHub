package com.example.servicehub.data.repository

import com.example.servicehub.data.await
import com.example.servicehub.data.liveList
import com.example.servicehub.data.model.Booking
import com.example.servicehub.data.model.BookingStatus
import com.example.servicehub.data.model.ChatMessage
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** How long after the job the chat stays open (same as the redo guarantee). */
private const val CHAT_DAYS_AFTER_JOB = 7

/**
 * True while the customer and provider may chat: from request until 7 days after completion.
 * Rejected / cancelled bookings have no chat. Pure function, so it is unit-tested.
 */
fun canChatAt(b: Booking, now: Long): Boolean = when (b.bookingStatus) {
    BookingStatus.REQUESTED, BookingStatus.ACCEPTED, BookingStatus.IN_PROGRESS -> true
    BookingStatus.COMPLETED -> b.completedAt > 0 && now - b.completedAt <= CHAT_DAYS_AFTER_JOB * 24L * 60 * 60 * 1000
    else -> false
}

/**
 * Chat inside one booking: bookings/{bookingId}/messages/{messageId}.
 * Only that booking's customer and provider can read or write it (see firestore.rules).
 */
object ChatRepository {

    private val db = FirebaseFirestore.getInstance()

    private fun messages(bookingId: String) = db.collection("bookings").document(bookingId).collection("messages")

    /** All messages of a booking, live, oldest first (like any chat app). */
    fun liveMessages(bookingId: String): Flow<List<ChatMessage>> =
        messages(bookingId).liveList(ChatMessage::class.java).map { list -> list.sortedBy { it.createdAt } }

    /** Sends one message and tells the other person with an in-app notification. */
    suspend fun send(booking: Booking, senderId: String, text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        if (clean.length > 500) throw BookingException("Messages can be at most 500 characters.")
        if (!canChatAt(booking, System.currentTimeMillis())) throw BookingException("Chat is closed for this booking.")

        val fromCustomer = senderId == booking.customerId
        val senderName = if (fromCustomer) booking.customerName else booking.providerName
        val ref = messages(booking.id).document()
        ref.set(
            ChatMessage(id = ref.id, senderId = senderId, senderName = senderName, text = clean, createdAt = System.currentTimeMillis())
        ).await()
        NotificationRepository.send(
            if (fromCustomer) booking.providerId else booking.customerId,
            "New message from $senderName",
            "${booking.serviceName}: ${clean.take(80)}",
            booking.id,
        )
    }
}
