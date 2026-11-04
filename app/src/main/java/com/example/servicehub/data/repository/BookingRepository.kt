package com.example.servicehub.data.repository

import com.example.servicehub.data.FixedData
import com.example.servicehub.data.await
import com.example.servicehub.data.liveList
import com.example.servicehub.data.liveObject
import com.example.servicehub.data.model.Booking
import com.example.servicehub.data.model.BookingStatus
import com.example.servicehub.data.model.PaymentStatus
import com.example.servicehub.data.model.Provider
import com.example.servicehub.data.model.Service
import com.example.servicehub.data.model.User
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.random.Random

/** An error we create ourselves, with a message that is safe to show the user. */
class BookingException(message: String) : Exception(message)

/**
 * Everything about bookings.
 *
 * Status changes use Firestore TRANSACTIONS: we read the booking's current status and only
 * update it if the move is allowed (e.g. only a REQUESTED booking can be accepted).
 * If two phones act at the same moment, the transaction makes sure only one change wins.
 */
object BookingRepository {

    private val db = FirebaseFirestore.getInstance()
    private val bookings = db.collection("bookings")

    // ---------------------------------------------------------------- reading (live)

    /** A customer's bookings, newest first. */
    fun customerBookings(customerId: String): Flow<List<Booking>> =
        bookings.whereEqualTo("customerId", customerId)
            .liveList(Booking::class.java)
            .map { list -> list.sortedByDescending { it.createdAt } }

    /** A provider's bookings, newest first. */
    fun providerBookings(providerId: String): Flow<List<Booking>> =
        bookings.whereEqualTo("providerId", providerId)
            .liveList(Booking::class.java)
            .map { list -> list.sortedByDescending { it.createdAt } }

    /** One booking, live. Both phones see every status change instantly. */
    fun liveBooking(bookingId: String): Flow<Booking?> =
        bookings.document(bookingId).liveObject(Booking::class.java)

    // ---------------------------------------------------------------- creating

    /**
     * True if the provider already has an ACTIVE booking (requested/accepted/in progress)
     * on this date and slot. [ignoreBookingId] skips the booking being rescheduled.
     */
    suspend fun isSlotTaken(providerId: String, date: String, timeSlot: String, ignoreBookingId: String = ""): Boolean {
        val snapshot = bookings
            .whereEqualTo("providerId", providerId)
            .whereEqualTo("date", date)
            .whereEqualTo("timeSlot", timeSlot)
            .get().await()
        return snapshot.toObjects(Booking::class.java)
            .any { it.id != ignoreBookingId && it.bookingStatus.isActive }
    }

    /** Saves a new REQUESTED booking and returns its id. Fails if the slot is taken. */
    suspend fun createBooking(
        customer: User,
        provider: Provider,
        service: Service,
        date: String,
        timeSlot: String,
        address: String,
        note: String,
        paymentMode: String,
    ): String {
        if (isSlotTaken(provider.id, date, timeSlot)) {
            throw BookingException("${provider.name} is already booked for this slot. Please choose another date or slot.")
        }
        val ref = bookings.document()          // creates a new random id
        val now = System.currentTimeMillis()
        val booking = Booking(
            id = ref.id,
            customerId = customer.id, customerName = customer.name, customerPhone = customer.phone,
            providerId = provider.id, providerName = provider.name, providerPhone = provider.phone,
            categoryId = service.categoryId, serviceId = service.id, serviceName = service.name,
            servicePrice = service.price,
            visitFee = FixedData.VISIT_FEE,
            totalAmount = service.price + FixedData.VISIT_FEE,
            date = date, timeSlot = timeSlot, address = address, note = note,
            status = BookingStatus.REQUESTED.name,
            startPin = Random.nextInt(1000, 10000).toString(),   // 4 digits: 1000..9999
            paymentMode = paymentMode,
            paymentStatus = PaymentStatus.PENDING,
            createdAt = now, updatedAt = now,
        )
        ref.set(booking).await()
        return ref.id
    }

    // ---------------------------------------------------------------- status changes

    /**
     * The one place where a booking's status changes.
     * Reads the booking inside a transaction, checks the current status is one of [allowedFrom],
     * optionally runs an extra [check], then saves [changes] plus updatedAt.
     */
    private suspend fun changeBooking(
        bookingId: String,
        allowedFrom: Set<BookingStatus>,
        changes: Map<String, Any>,
        check: (Booking) -> Unit = {},
    ) {
        val ref = bookings.document(bookingId)
        db.runTransaction<Unit> { transaction ->
            val booking = transaction.get(ref).toObject(Booking::class.java)
                ?: throw BookingException("Booking not found.")
            if (booking.bookingStatus !in allowedFrom) {
                throw BookingException("Not possible: this booking is already ${booking.bookingStatus.label}.")
            }
            check(booking)
            transaction.update(ref, changes + ("updatedAt" to System.currentTimeMillis()))
            Unit   // the transaction returns nothing
        }.await()
    }

    /** Provider: REQUESTED -> ACCEPTED */
    suspend fun accept(bookingId: String) = changeBooking(
        bookingId, setOf(BookingStatus.REQUESTED),
        mapOf("status" to BookingStatus.ACCEPTED.name, "acceptedAt" to System.currentTimeMillis()),
    )

    /** Provider: REQUESTED -> REJECTED (with a reason) */
    suspend fun reject(bookingId: String, reason: String) = changeBooking(
        bookingId, setOf(BookingStatus.REQUESTED),
        mapOf("status" to BookingStatus.REJECTED.name, "rejectReason" to reason),
    )

    /** Provider: ACCEPTED -> IN_PROGRESS, only with the correct 4-digit PIN from the customer. */
    suspend fun startJob(bookingId: String, enteredPin: String) = changeBooking(
        bookingId, setOf(BookingStatus.ACCEPTED),
        mapOf("status" to BookingStatus.IN_PROGRESS.name, "startedAt" to System.currentTimeMillis()),
        check = { booking ->
            if (booking.startPin != enteredPin.trim()) throw BookingException("Wrong PIN. Ask the customer for the 4-digit PIN.")
        },
    )

    /** Customer: REQUESTED or ACCEPTED -> CANCELLED (with a reason) */
    suspend fun cancel(bookingId: String, reason: String) = changeBooking(
        bookingId, setOf(BookingStatus.REQUESTED, BookingStatus.ACCEPTED),
        mapOf("status" to BookingStatus.CANCELLED.name, "cancelReason" to reason),
    )

    /**
     * Customer: change date/slot while REQUESTED or ACCEPTED.
     * The status always becomes REQUESTED, so the provider must confirm the new time again.
     */
    suspend fun reschedule(booking: Booking, newDate: String, newSlot: String) {
        if (newDate == booking.date && newSlot == booking.timeSlot) {
            throw BookingException("Please choose a different date or slot.")
        }
        if (isSlotTaken(booking.providerId, newDate, newSlot, ignoreBookingId = booking.id)) {
            throw BookingException("${booking.providerName} is already booked for that slot. Choose another.")
        }
        changeBooking(
            booking.id, setOf(BookingStatus.REQUESTED, BookingStatus.ACCEPTED),
            mapOf(
                "date" to newDate, "timeSlot" to newSlot,
                "status" to BookingStatus.REQUESTED.name, "acceptedAt" to 0L,
            ),
        )
    }

    /**
     * Provider: IN_PROGRESS -> COMPLETED after confirming "Payment received".
     * In the SAME transaction we add 1 job and the amount to the provider's totals,
     * so the booking and the provider's earnings can never disagree.
     */
    suspend fun complete(bookingId: String, paymentMode: String) {
        val bookingRef = bookings.document(bookingId)
        db.runTransaction<Unit> { transaction ->
            // In a transaction, all reads must happen before any write.
            val booking = transaction.get(bookingRef).toObject(Booking::class.java)
                ?: throw BookingException("Booking not found.")
            if (booking.bookingStatus != BookingStatus.IN_PROGRESS) {
                throw BookingException("Only a job in progress can be completed.")
            }
            val providerRef = db.collection("providers").document(booking.providerId)
            val now = System.currentTimeMillis()

            transaction.update(
                bookingRef,
                mapOf(
                    "status" to BookingStatus.COMPLETED.name,
                    "paymentMode" to paymentMode,
                    "paymentStatus" to PaymentStatus.PAID,
                    "completedAt" to now,
                    "updatedAt" to now,
                ),
            )
            transaction.update(
                providerRef,
                mapOf(
                    "jobsCompleted" to FieldValue.increment(1),
                    "totalEarnings" to FieldValue.increment(booking.totalAmount.toLong()),
                ),
            )
            Unit   // the transaction returns nothing
        }.await()
    }

    /** A message that is safe to show, looking inside wrapped errors too. */
    fun friendlyError(e: Throwable): String {
        var current: Throwable? = e
        while (current != null) {
            if (current is BookingException) return current.message ?: "Something went wrong."
            current = current.cause
        }
        return "Something went wrong. Check your internet and try again."
    }
}
