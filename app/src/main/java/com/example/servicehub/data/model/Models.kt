package com.example.servicehub.data.model

import com.example.servicehub.data.DateUtils
import com.example.servicehub.data.FixedData
import com.google.firebase.firestore.Exclude
import java.util.Calendar
import com.google.firebase.firestore.PropertyName

/*
 * These classes match the documents in Firestore, one class per collection.
 *
 * Rules Firestore needs:
 *  - every field has a default value (so Firestore can create an empty object and fill it)
 *  - fields are `var` when they use @PropertyName (see below)
 *
 * Why @PropertyName on fields starting with "is"?
 *  Kotlin turns `isBlocked` into a getter named isBlocked(), and Firestore would then save it
 *  as "blocked". The annotation forces the Firestore field name to stay exactly "isBlocked".
 *
 * Times are saved as Long = milliseconds since 1970 (System.currentTimeMillis()).
 * Easy to sort and compare.
 */

/** Values for User.role */
object Roles {
    const val CUSTOMER = "customer"
    const val PROVIDER = "provider"
    const val ADMIN = "admin"
}

/** Values for Provider.approvalStatus */
object ApprovalStatus {
    const val PENDING = "PENDING"
    const val APPROVED = "APPROVED"
    const val REJECTED = "REJECTED"
}

/** Values for Booking.paymentStatus */
object PaymentStatus {
    const val PENDING = "PENDING"
    const val PAID = "PAID"
}

/** users/{uid} : every person who has an account */
data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = Roles.CUSTOMER,
    val area: String = "",
    val address: String = "",
    @get:PropertyName("isBlocked") @set:PropertyName("isBlocked")
    var isBlocked: Boolean = false,
    val createdAt: Long = 0L,
)

/** providers/{uid} : extra details for users whose role is "provider" */
data class Provider(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val categoryId: String = "",
    val area: String = "",
    val experienceYears: Int = 0,
    val bio: String = "",
    val approvalStatus: String = ApprovalStatus.PENDING,
    @get:PropertyName("isAvailable") @set:PropertyName("isAvailable")
    var isAvailable: Boolean = true,
    val ratingAvg: Double = 0.0,
    val ratingCount: Int = 0,
    val jobsCompleted: Int = 0,
    val totalEarnings: Int = 0,
    val createdAt: Long = 0L,
    /** Weekly days off, as Calendar day numbers: 1 = Sunday, 2 = Monday ... 7 = Saturday. */
    val offDays: List<Int> = emptyList(),
    /** Single dates ("yyyy-MM-dd") the provider chose to work even though it is their day off. */
    val extraWorkDates: List<String> = emptyList(),
)

/**
 * True if the provider does not work on this date ("yyyy-MM-dd"):
 * it is one of their weekly days off, and they did not choose "Work today" for it.
 */
fun isOffOn(provider: Provider, date: String): Boolean =
    DateUtils.weekdayOf(date) in provider.offDays && date !in provider.extraWorkDates

/** The first date from today (within a week) the provider works, e.g. "2026-11-18". Null if none. */
fun nextWorkingDay(provider: Provider, days: List<String>): String? = days.firstOrNull { !isOffOn(provider, it) }

/** slots/{providerId_date_slotNumber} : marks one provider slot as busy (no personal details). */
data class SlotInfo(
    val providerId: String = "",
    val customerId: String = "",
    val bookingId: String = "",
    val date: String = "",
    val timeSlot: String = "",
)

/** Values for Complaint.status */
object ComplaintStatus {
    const val OPEN = "OPEN"
    const val RESOLVED = "RESOLVED"
}

/** complaints/{id} : a problem a customer reported about a booking, handled by the admin. */
data class Complaint(
    val id: String = "",
    val bookingId: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val providerId: String = "",
    val providerName: String = "",
    val providerPhone: String = "",
    val serviceName: String = "",
    val message: String = "",
    val status: String = ComplaintStatus.OPEN,
    val createdAt: Long = 0L,
    val resolvedAt: Long = 0L,
)

/** "Top rated" badge: at least 4.5 stars from at least 3 reviews (so one lucky 5★ is not enough). */
fun isTopRated(provider: Provider): Boolean = provider.ratingCount >= 3 && provider.ratingAvg >= 4.5

/** services/{id} : one service in the catalog, e.g. "Tap repair ₹149" */
data class Service(
    val id: String = "",
    val categoryId: String = "",
    val name: String = "",
    val description: String = "",
    val price: Int = 0,
    val durationMins: Int = 0,
    @get:PropertyName("isActive") @set:PropertyName("isActive")
    var isActive: Boolean = true,
)

/** bookings/{id} : one job booked by a customer with a provider */
data class Booking(
    val id: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val providerId: String = "",
    val providerName: String = "",
    val providerPhone: String = "",
    val categoryId: String = "",
    val serviceId: String = "",
    val serviceName: String = "",
    val servicePrice: Int = 0,
    val visitFee: Int = 0,
    val totalAmount: Int = 0,
    val date: String = "",          // "2026-11-05"
    val timeSlot: String = "",      // "Morning (9 AM – 12 PM)"
    val address: String = "",
    val note: String = "",
    val status: String = BookingStatus.REQUESTED.name,
    val startPin: String = "",      // 4 digits, shown to customer after ACCEPTED
    val paymentMode: String = "",   // "Cash" or "UPI"
    val paymentStatus: String = PaymentStatus.PENDING,
    @get:PropertyName("isRated") @set:PropertyName("isRated")
    var isRated: Boolean = false,
    val cancelReason: String = "",
    val rejectReason: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val acceptedAt: Long = 0L,
    val startedAt: Long = 0L,
    val completedAt: Long = 0L,
) {
    /**
     * The status as an enum, which is easier to use in code than plain text.
     * @Exclude stops Firestore from saving this as an extra field.
     */
    @get:Exclude
    val bookingStatus: BookingStatus
        get() = BookingStatus.fromName(status)

    /** Still REQUESTED but its date/slot has already passed (the provider never answered). */
    @get:Exclude
    val isExpired: Boolean
        get() = isRequestExpired(this, DateUtils.today(), Calendar.getInstance().get(Calendar.HOUR_OF_DAY))

    /** Accepted for a day that is already over, but never started (the provider is late). */
    @get:Exclude
    val isOverdue: Boolean
        get() = isAcceptedOverdue(this, DateUtils.today())

    /** Active and not expired: shown under "Active" and blocks actions like Accept. */
    @get:Exclude
    val isOpen: Boolean
        get() = bookingStatus.isActive && !isExpired
}

/** ACCEPTED, and its date is before today. It stays open (the job can still happen) but is flagged. */
fun isAcceptedOverdue(booking: Booking, today: String): Boolean =
    booking.bookingStatus == BookingStatus.ACCEPTED && booking.date < today

/**
 * A request expires when nobody accepted it before its time:
 * the date is in the past, or it is today and the slot is already over.
 * (Dates are "yyyy-MM-dd" text, so comparing them as text works.)
 * Pure function with the date and hour passed in, so it is easy to test.
 */
fun isRequestExpired(booking: Booking, today: String, hourNow: Int): Boolean =
    booking.bookingStatus == BookingStatus.REQUESTED && (
        booking.date < today ||
            (booking.date == today && FixedData.isSlotOverToday(booking.timeSlot, hourNow))
        )

/** reviews/{id} : stars + comment a customer left after a completed job */
data class Review(
    val id: String = "",
    val bookingId: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val providerId: String = "",
    val rating: Int = 0,
    val comment: String = "",
    val createdAt: Long = 0L,
)

/** notifications/{id} : an in-app message for one user (no push notifications) */
data class AppNotification(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val bookingId: String = "",
    @get:PropertyName("isRead") @set:PropertyName("isRead")
    var isRead: Boolean = false,
    val createdAt: Long = 0L,
)
