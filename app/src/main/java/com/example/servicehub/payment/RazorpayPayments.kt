package com.example.servicehub.payment

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.example.servicehub.BuildConfig
import com.example.servicehub.data.model.Booking
import com.razorpay.Checkout
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import org.json.JSONObject

/** What came back from the Razorpay payment screen. */
sealed class PaymentResult {
    data class Success(val bookingId: String, val paymentId: String) : PaymentResult()
    data class Failure(val bookingId: String, val message: String) : PaymentResult()
}

/**
 * Online payment with Razorpay in TEST MODE (no real money moves).
 *
 * How it works:
 *  1. [start] opens Razorpay's own payment screen (UPI, cards...) for the booking's amount.
 *  2. Razorpay reports success/failure to MainActivity, which passes it to [onSuccess]/[onError].
 *  3. We send it out through [results]; the booking screen listens and marks the booking PAID.
 *
 * The TEST Key ID comes from local.properties (RAZORPAY_KEY_ID), never from the code on GitHub.
 * Test UPI ids: success@razorpay (payment succeeds), failure@razorpay (payment fails).
 */
object RazorpayPayments {

    /** True when a Razorpay TEST key was put in local.properties. */
    val isConfigured: Boolean
        get() = BuildConfig.RAZORPAY_KEY_ID.startsWith("rzp_")

    /** Which booking the open payment screen is for (Razorpay only gives us the payment id back). */
    private var payingBookingId: String? = null

    private val _results = MutableSharedFlow<PaymentResult>(extraBufferCapacity = 1)
    val results: SharedFlow<PaymentResult> = _results

    /** Opens the Razorpay payment screen for this booking's total. */
    fun start(activity: Activity, booking: Booking, email: String, phone: String) {
        payingBookingId = booking.id
        val checkout = Checkout()
        checkout.setKeyID(BuildConfig.RAZORPAY_KEY_ID)
        val options = JSONObject().apply {
            put("name", "ServiceHub")
            put("description", "${booking.serviceName} · ${booking.id.take(8).uppercase()}")
            put("currency", "INR")
            put("amount", booking.totalAmount * 100)   // Razorpay counts in paise: ₹198 = 19800
            put("theme", JSONObject().put("color", "#3355E0"))
            put("prefill", JSONObject().put("email", email).put("contact", phone))
            put("notes", JSONObject().put("bookingId", booking.id))
        }
        checkout.open(activity, options)
    }

    /** Called by MainActivity when Razorpay says the payment worked. */
    fun onSuccess(paymentId: String?) {
        val bookingId = payingBookingId ?: return
        payingBookingId = null
        _results.tryEmit(PaymentResult.Success(bookingId, paymentId.orEmpty()))
    }

    /** Called by MainActivity when the payment failed or the customer closed the screen. */
    fun onError(description: String?) {
        val bookingId = payingBookingId ?: return
        payingBookingId = null
        val message = if (description.isNullOrBlank() || description.contains("cancel", ignoreCase = true)) {
            "Payment was cancelled. You can try again."
        } else "Payment failed. Please try again."
        _results.tryEmit(PaymentResult.Failure(bookingId, message))
    }
}

/** Compose gives us a Context; Razorpay needs the Activity behind it. */
fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
