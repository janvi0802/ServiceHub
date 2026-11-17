package com.example.servicehub.data.repository

import com.example.servicehub.data.await
import com.example.servicehub.data.model.Booking
import com.example.servicehub.data.model.BookingStatus
import com.example.servicehub.data.model.Provider
import com.example.servicehub.data.model.Review
import com.google.firebase.firestore.FirebaseFirestore

/**
 * New average after adding one rating.
 * Example: average 4.0 from 3 ratings, new rating 5 -> (4.0*3 + 5) / 4 = 4.25
 * (Kept outside the object below so it can be tested without Firebase.)
 */
fun newAverage(oldAverage: Double, oldCount: Int, newRating: Int): Double =
    (oldAverage * oldCount + newRating) / (oldCount + 1)

/** Ratings and reviews written by customers after a completed job. */
object ReviewRepository {

    private val db = FirebaseFirestore.getInstance()

    /**
     * Saves a review in ONE transaction:
     *  1. the review document (its id = the booking id, so a booking can only have one review)
     *  2. the provider's new ratingAvg and ratingCount
     *  3. booking.isRated = true
     * If two things happen at once, the transaction retries, so the average is never wrong.
     */
    suspend fun submit(bookingId: String, customerId: String, rating: Int, comment: String) {
        if (rating !in 1..5) throw BookingException("Please choose 1 to 5 stars.")

        val bookingRef = db.collection("bookings").document(bookingId)
        val reviewRef = db.collection("reviews").document(bookingId)

        val booking = db.runTransaction<Booking> { transaction ->
            // --- all reads first (Firestore rule for transactions)
            val booking = transaction.get(bookingRef).toObject(Booking::class.java)
                ?: throw BookingException("Booking not found.")
            if (booking.customerId != customerId) throw BookingException("You can only rate your own booking.")
            if (booking.bookingStatus != BookingStatus.COMPLETED) throw BookingException("You can rate only after the job is completed.")
            if (booking.isRated || transaction.get(reviewRef).exists()) throw BookingException("You have already rated this booking.")

            val providerRef = db.collection("providers").document(booking.providerId)
            val provider = transaction.get(providerRef).toObject(Provider::class.java)
                ?: throw BookingException("Provider not found.")

            // --- then all writes
            val review = Review(
                id = bookingId, bookingId = bookingId,
                customerId = customerId, customerName = booking.customerName,
                providerId = booking.providerId,
                rating = rating, comment = comment,
                createdAt = System.currentTimeMillis(),
            )
            transaction.set(reviewRef, review)
            transaction.update(
                providerRef,
                mapOf(
                    "ratingAvg" to newAverage(provider.ratingAvg, provider.ratingCount, rating),
                    "ratingCount" to provider.ratingCount + 1,
                ),
            )
            transaction.update(bookingRef, "isRated", true)
            booking
        }.await()

        // Tell the provider about their new review
        NotificationRepository.send(
            booking.providerId, "New review: $rating/5",
            "${booking.customerName} rated your ${booking.serviceName} $rating/5" +
                if (comment.isNotBlank()) ": \"$comment\"" else ".",
            bookingId,
        )
    }
}
