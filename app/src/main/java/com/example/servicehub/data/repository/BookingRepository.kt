package com.example.servicehub.data.repository

import com.example.servicehub.data.DateUtils
import com.example.servicehub.data.FixedData
import com.example.servicehub.data.await
import com.example.servicehub.data.liveList
import com.example.servicehub.data.liveObject
import com.example.servicehub.data.model.ApprovalStatus
import com.example.servicehub.data.model.Booking
import com.example.servicehub.data.model.BookingStatus
import com.example.servicehub.data.model.PaymentStatus
import com.example.servicehub.data.model.Provider
import com.example.servicehub.data.model.Service
import com.example.servicehub.data.model.ExtraItem
import com.example.servicehub.data.model.GUARANTEE_DAYS
import com.example.servicehub.data.model.RevisitStatus
import com.example.servicehub.data.model.canRequestRevisitAt
import com.example.servicehub.data.model.SlotInfo
import com.example.servicehub.data.model.User
import com.example.servicehub.data.model.isOffOn
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.random.Random

/** Start of cancelReason when the PROVIDER cancelled, so both sides can see who cancelled. */
const val PROVIDER_CANCEL_PREFIX = "Provider: "

/** An error we create ourselves, with a message that is safe to show the user. */
class BookingException(message: String) : Exception(message)

/**
 * Id of the "slot" document that marks a provider as busy, e.g. "abc123_2026-11-20_0"
 * (provider id, date, slot number 0/1/2). Only ONE document can have this id,
 * so only one active booking can hold a provider's slot.
 */
fun slotDocId(providerId: String, date: String, timeSlot: String): String =
    "${providerId}_${date}_${FixedData.timeSlots.indexOf(timeSlot)}"

/**
 * Everything about bookings.
 *
 * Status changes use Firestore TRANSACTIONS: we read the booking's current status and only
 * update it if the move is allowed (e.g. only a REQUESTED booking can be accepted).
 * If two phones act at the same moment, the transaction makes sure only one change wins.
 *
 * Busy slots: every active booking owns one document in "slots" (see [slotDocId]).
 * It is created together with the booking and deleted when the booking is
 * rejected, cancelled or completed. It holds no personal details, so bookings
 * themselves can stay private to their customer and provider.
 */
object BookingRepository {

    private val db = FirebaseFirestore.getInstance()
    private val bookings = db.collection("bookings")
    private val slots = db.collection("slots")

    private fun slotRef(providerId: String, date: String, timeSlot: String) =
        slots.document(slotDocId(providerId, date, timeSlot))

    private fun slotRef(booking: Booking) = slotRef(booking.providerId, booking.date, booking.timeSlot)

    /** What a slot document stores: who holds it (no address, phone or names). */
    private fun slotData(booking: Booking) = mapOf(
        "providerId" to booking.providerId,
        "customerId" to booking.customerId,
        "bookingId" to booking.id,
        "date" to booking.date,
        "timeSlot" to booking.timeSlot,
    )

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

    /**
     * Live set of a provider's busy slots on one date, e.g. {"Morning (9 AM – 12 PM)"}.
     * The booking form greys these out, so the customer never picks a taken slot.
     */
    fun busySlots(providerId: String, date: String): Flow<Set<String>> =
        slots.whereEqualTo("providerId", providerId)
            .whereEqualTo("date", date)
            .liveList(SlotInfo::class.java)
            .map { list -> list.map { it.timeSlot }.toSet() }

    /** One booking, live. Both phones see every status change instantly. */
    fun liveBooking(bookingId: String): Flow<Booking?> =
        bookings.document(bookingId).liveObject(Booking::class.java)

    // ---------------------------------------------------------------- creating

    /**
     * Saves a new REQUESTED booking and returns its id. Fails if the slot is taken.
     * The slot check and the save happen in ONE transaction: if two customers press
     * "Confirm" for the same provider and slot at the same moment, only one succeeds.
     */
    suspend fun createBooking(
        customer: User,
        provider: Provider,
        service: Service,
        date: String,
        timeSlot: String,
        address: String,
        note: String,
        paymentMode: String,
        contactName: String = "",
        contactPhone: String = "",
    ): String {
        // Check again with the LATEST data: the provider or service may have changed
        // while the customer was filling in the form.
        val latestProvider = UserRepository.getProvider(provider.id)
        if (latestProvider == null ||
            latestProvider.approvalStatus != ApprovalStatus.APPROVED ||
            !latestProvider.isAvailable ||
            UserRepository.getUser(provider.id)?.isBlocked == true
        ) {
            throw BookingException("${provider.name} is not available for booking right now. Please choose another provider.")
        }
        if (isOffOn(latestProvider, date)) {
            throw BookingException(
                "${provider.name} is off on that day (weekly off: ${DateUtils.weekdayNames(latestProvider.offDays)}). " +
                    "Please choose another day or provider."
            )
        }
        val latestService = ServiceRepository.getService(service.id)
        if (latestService == null || !latestService.isActive) {
            throw BookingException("${service.name} is not offered right now.")
        }
        if (timeSlot !in FixedData.timeSlots) throw BookingException("Please choose a time slot.")
        if (!FixedData.isBookableTime(date, timeSlot, DateUtils.today(), currentHour())) {
            throw BookingException("That time has already passed. Please choose a later slot.")
        }
        val ref = bookings.document()          // creates a new random id
        val now = System.currentTimeMillis()
        val booking = Booking(
            id = ref.id,
            customerId = customer.id, customerName = customer.name, customerPhone = customer.phone,
            providerId = provider.id, providerName = latestProvider.name, providerPhone = latestProvider.phone,
            categoryId = latestService.categoryId, serviceId = latestService.id, serviceName = latestService.name,
            servicePrice = latestService.price,     // latest price, in case the admin just changed it
            visitFee = FixedData.VISIT_FEE,
            totalAmount = latestService.price + FixedData.VISIT_FEE,
            date = date, timeSlot = timeSlot, address = address, note = note,
            contactName = contactName, contactPhone = contactPhone,
            status = BookingStatus.REQUESTED.name,
            startPin = Random.nextInt(1000, 10000).toString(),   // 4 digits: 1000..9999
            paymentMode = paymentMode,
            paymentStatus = PaymentStatus.PENDING,
            createdAt = now, updatedAt = now,
        )
        val slot = slotRef(booking)
        db.runTransaction<Unit> { transaction ->
            if (transaction.get(slot).exists()) {
                throw BookingException("${provider.name} is already booked for this slot. Please choose another date or slot.")
            }
            transaction.set(slot, slotData(booking))   // claim the slot
            transaction.set(ref, booking)              // save the booking
            Unit
        }.await()
        NotificationRepository.send(
            provider.id, "New booking request",
            "${customer.name} requested ${latestService.name} on ${slotText(booking)}.", ref.id,
        )
        return ref.id
    }

    private fun currentHour() = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)

    /** "Thu, 5 Nov, Morning (9 AM – 12 PM)" for notification messages. */
    private fun slotText(booking: Booking) = "${DateUtils.pretty(booking.date)}, ${booking.timeSlot}"

    // ---------------------------------------------------------------- status changes

    /**
     * The one place where a booking's status changes.
     * Reads the booking inside a transaction, checks the current status is one of [allowedFrom],
     * optionally runs an extra [check], then saves [changes] plus updatedAt.
     * [releaseSlot] = true when the booking ends (rejected/cancelled), so the provider's
     * slot becomes free again in the same transaction.
     * Returns the booking as it was BEFORE the change (used for notification texts).
     */
    private suspend fun changeBooking(
        bookingId: String,
        allowedFrom: Set<BookingStatus>,
        changes: Map<String, Any>,
        releaseSlot: Boolean = false,
        check: (Booking) -> Unit = {},
    ): Booking {
        val ref = bookings.document(bookingId)
        return db.runTransaction<Booking> { transaction ->
            val booking = transaction.get(ref).toObject(Booking::class.java)
                ?: throw BookingException("Booking not found.")
            if (booking.bookingStatus !in allowedFrom) {
                throw BookingException("Not possible: this booking is already ${booking.bookingStatus.label}.")
            }
            check(booking)
            transaction.update(ref, changes + ("updatedAt" to System.currentTimeMillis()))
            if (releaseSlot) transaction.delete(slotRef(booking))
            booking
        }.await()
    }

    /** Provider: REQUESTED -> ACCEPTED (not if the request has already expired) */
    suspend fun accept(bookingId: String) {
        val b = changeBooking(
            bookingId, setOf(BookingStatus.REQUESTED),
            mapOf("status" to BookingStatus.ACCEPTED.name, "acceptedAt" to System.currentTimeMillis()),
            check = { booking ->
                if (booking.isExpired) throw BookingException("This request has expired: its time has already passed.")
            },
        )
        NotificationRepository.send(
            b.customerId, "Booking accepted",
            "${b.providerName} accepted your ${b.serviceName} on ${slotText(b)}. Your start PIN is in the booking.", b.id,
        )
    }

    /** Provider: REQUESTED -> REJECTED (with a reason) */
    suspend fun reject(bookingId: String, reason: String) {
        val b = changeBooking(
            bookingId, setOf(BookingStatus.REQUESTED),
            mapOf("status" to BookingStatus.REJECTED.name, "rejectReason" to reason),
            releaseSlot = true,
        )
        NotificationRepository.send(
            b.customerId, "Booking rejected",
            "${b.providerName} could not take your ${b.serviceName}. Reason: $reason", b.id,
        )
    }

    /**
     * Provider taps "On my way" while ACCEPTED. The status does NOT change (the lifecycle stays the
     * same); we only save the time, so the customer sees "Professional is on the way".
     */
    suspend fun markOnTheWay(bookingId: String) {
        val b = changeBooking(
            bookingId, setOf(BookingStatus.ACCEPTED),
            mapOf("onTheWayAt" to System.currentTimeMillis()),
        )
        NotificationRepository.send(
            b.customerId, "Professional on the way",
            "${b.providerName} is on the way for your ${b.serviceName}. Keep your PIN ready.", b.id,
        )
    }

    /** Customer: free revisit within 7 days of completion (service guarantee). */
    suspend fun requestRevisit(bookingId: String, reason: String) {
        val b = changeBooking(
            bookingId, setOf(BookingStatus.COMPLETED),
            mapOf(
                "revisitStatus" to RevisitStatus.REQUESTED,
                "revisitReason" to reason,
                "revisitRequestedAt" to System.currentTimeMillis(),
            ),
            check = { booking ->
                if (!canRequestRevisitAt(booking, System.currentTimeMillis())) {
                    throw BookingException("A free job redo can be requested once, within $GUARANTEE_DAYS days of completion.")
                }
            },
        )
        NotificationRepository.send(
            b.providerId, "Complaint: job redo requested",
            "${b.customerName} is not happy with ${b.serviceName} and asked you to redo it free: $reason", b.id,
        )
    }

    /** Provider: the free revisit has been done. */
    suspend fun markRevisitDone(bookingId: String) {
        val b = changeBooking(
            bookingId, setOf(BookingStatus.COMPLETED),
            mapOf("revisitStatus" to RevisitStatus.DONE, "revisitDoneAt" to System.currentTimeMillis()),
            check = { booking ->
                if (booking.revisitStatus != RevisitStatus.REQUESTED) throw BookingException("No job redo is waiting for this booking.")
            },
        )
        NotificationRepository.send(
            b.customerId, "Job redone",
            "${b.providerName} has redone your ${b.serviceName} free of charge.", b.id,
        )
    }

    /** Provider: ACCEPTED -> IN_PROGRESS, only with the correct 4-digit PIN from the customer. */
    suspend fun startJob(bookingId: String, enteredPin: String) {
        val b = changeBooking(
            bookingId, setOf(BookingStatus.ACCEPTED),
            mapOf("status" to BookingStatus.IN_PROGRESS.name, "startedAt" to System.currentTimeMillis()),
            check = { booking ->
                if (booking.startPin != enteredPin.trim()) throw BookingException("Wrong PIN. Ask the customer for the 4-digit PIN.")
            },
        )
        NotificationRepository.send(b.customerId, "Job started", "${b.providerName} has started your ${b.serviceName}.", b.id)
    }

    /** Customer: REQUESTED or ACCEPTED -> CANCELLED (with a reason) */
    suspend fun cancel(bookingId: String, reason: String) {
        val b = changeBooking(
            bookingId, setOf(BookingStatus.REQUESTED, BookingStatus.ACCEPTED),
            mapOf("status" to BookingStatus.CANCELLED.name, "cancelReason" to reason),
            releaseSlot = true,
        )
        NotificationRepository.send(
            b.providerId, "Booking cancelled",
            "${b.customerName} cancelled ${b.serviceName} on ${slotText(b)}. Reason: $reason", b.id,
        )
    }

    /**
     * Provider: ACCEPTED -> CANCELLED, e.g. they fell sick. Needs a reason, frees the slot
     * and tells the customer. (Before accepting, the provider uses Reject instead.)
     */
    suspend fun cancelByProvider(bookingId: String, reason: String) {
        val b = changeBooking(
            bookingId, setOf(BookingStatus.ACCEPTED),
            mapOf("status" to BookingStatus.CANCELLED.name, "cancelReason" to PROVIDER_CANCEL_PREFIX + reason),
            releaseSlot = true,
        )
        NotificationRepository.send(
            b.customerId, "Job cancelled by provider",
            "${b.providerName} cancelled your ${b.serviceName} on ${slotText(b)}. Reason: $reason. Please book another provider.", b.id,
        )
    }

    /**
     * Customer: change date/slot while REQUESTED or ACCEPTED.
     * The status always becomes REQUESTED, so the provider must confirm the new time again.
     */
    suspend fun reschedule(booking: Booking, newDate: String, newSlot: String) {
        if (newDate == booking.date && newSlot == booking.timeSlot) {
            throw BookingException("Please choose a different date or slot.")
        }
        if (newSlot !in FixedData.timeSlots) throw BookingException("Please choose a time slot.")
        if (!FixedData.isBookableTime(newDate, newSlot, DateUtils.today(), currentHour())) {
            throw BookingException("That time has already passed. Please choose a later slot.")
        }
        val provider = UserRepository.getProvider(booking.providerId)
        if (provider != null && isOffOn(provider, newDate)) {
            throw BookingException("${booking.providerName} does not work on that day. Please choose another date.")
        }

        // One transaction: check the new slot is free, give back the old slot, claim the new one.
        val ref = bookings.document(booking.id)
        val newSlotRef = slotRef(booking.providerId, newDate, newSlot)
        db.runTransaction<Unit> { transaction ->
            // reads first
            val current = transaction.get(ref).toObject(Booking::class.java)
                ?: throw BookingException("Booking not found.")
            if (current.bookingStatus != BookingStatus.REQUESTED && current.bookingStatus != BookingStatus.ACCEPTED) {
                throw BookingException("Not possible: this booking is already ${current.bookingStatus.label}.")
            }
            if (transaction.get(newSlotRef).exists()) {
                throw BookingException("${current.providerName} is already booked for that slot. Choose another.")
            }
            // then writes
            transaction.delete(slotRef(current))
            val moved = current.copy(date = newDate, timeSlot = newSlot)
            transaction.set(newSlotRef, slotData(moved))
            transaction.update(
                ref,
                mapOf(
                    "date" to newDate, "timeSlot" to newSlot,
                    "status" to BookingStatus.REQUESTED.name, "acceptedAt" to 0L,
                    "updatedAt" to System.currentTimeMillis(),
                ),
            )
            Unit
        }.await()
        val moved = booking.copy(date = newDate, timeSlot = newSlot)
        NotificationRepository.send(
            booking.providerId, "Booking rescheduled",
            "${booking.customerName} moved ${booking.serviceName} to ${slotText(moved)}. Please accept again.", booking.id,
        )
    }

    /**
     * Provider: IN_PROGRESS -> COMPLETED after confirming "Payment received".
     * [extras] = spare parts / extra work added now (e.g. "Tap cartridge" ₹180); they are added
     * to the total. In the SAME transaction we add 1 job and the provider's share to their totals,
     * so the booking and the provider's earnings can never disagree.
     */
    suspend fun complete(bookingId: String, paymentMode: String, extras: List<ExtraItem> = emptyList()) {
        val cleanExtras = validateExtras(extras)
        val extraSum = cleanExtras.sumOf { it.amount }
        val bookingRef = bookings.document(bookingId)
        val b = db.runTransaction<Booking> { transaction ->
            // In a transaction, all reads must happen before any write.
            val booking = transaction.get(bookingRef).toObject(Booking::class.java)
                ?: throw BookingException("Booking not found.")
            if (booking.bookingStatus != BookingStatus.IN_PROGRESS) {
                throw BookingException("Only a job in progress can be completed.")
            }
            val providerRef = db.collection("providers").document(booking.providerId)
            val now = System.currentTimeMillis()
            val newTotal = booking.servicePrice + booking.visitFee + extraSum
            val updated = booking.copy(totalAmount = newTotal)

            transaction.update(
                bookingRef,
                mapOf(
                    "status" to BookingStatus.COMPLETED.name,
                    "paymentMode" to paymentMode,
                    "paymentStatus" to PaymentStatus.PAID,
                    // stored as simple name/amount maps (reads back as ExtraItem objects)
                    "extraItems" to cleanExtras.map { mapOf("name" to it.name, "amount" to it.amount) },
                    "extraAmount" to extraSum,
                    "totalAmount" to newTotal,
                    "completedAt" to now,
                    "updatedAt" to now,
                ),
            )
            transaction.update(
                providerRef,
                mapOf(
                    "jobsCompleted" to FieldValue.increment(1),
                    // the provider keeps everything except the platform's visit fee
                    "totalEarnings" to FieldValue.increment(updated.providerEarning.toLong()),
                ),
            )
            transaction.delete(slotRef(booking))   // job finished: slot no longer busy
            updated
        }.await()
        NotificationRepository.send(
            b.customerId, "Job completed",
            "${b.serviceName} is done. Paid ₹${b.totalAmount} by $paymentMode" +
                (if (extraSum > 0) " (incl. ₹$extraSum extra parts/work)" else "") +
                ". Please rate ${b.providerName}!",
            b.id,
        )
    }

    /** Removes empty rows and refuses silly amounts, so a typo cannot create a huge bill. */
    private fun validateExtras(extras: List<ExtraItem>): List<ExtraItem> {
        val clean = extras.map { it.copy(name = it.name.trim()) }.filter { it.name.isNotEmpty() || it.amount > 0 }
        clean.forEach {
            if (it.name.isEmpty()) throw BookingException("Every extra item needs a name.")
            if (it.amount !in 1..50_000) throw BookingException("Extra item amounts must be between ₹1 and ₹50,000.")
        }
        if (clean.size > 10) throw BookingException("Add at most 10 extra items.")
        return clean
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
