package com.example.servicehub.payment

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.example.servicehub.BuildConfig
import com.example.servicehub.data.model.Booking
import com.example.servicehub.data.repository.BookingRepository
import com.razorpay.Checkout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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
 *  3. We mark the booking PAID right away, then send the result out through [results]
 *     so the booking screen can show "Payment successful".
 *
 * The TEST Key ID comes from local.properties (RAZORPAY_KEY_ID), never from the code on GitHub.
 * Test UPI ids: success@razorpay (payment succeeds), failure@razorpay (payment fails).
 */
object RazorpayPayments {

    /** True when a Razorpay TEST key was put in local.properties. */
    val isConfigured: Boolean
        get() = BuildConfig.RAZORPAY_KEY_ID.startsWith("rzp_")

    /**
     * Which booking the open payment screen is for (Razorpay only gives us the payment id back).
     * Also saved on the phone, so it is not forgotten if Android closes the app during payment.
     */
    private var payingBookingId: String? = null
    private const val PREFS = "payments"
    private const val KEY_BOOKING = "payingBookingId"

    /**
     * App-wide scope: the payment is saved even if the booking screen was closed meanwhile.
     * (A screen's own scope would stop when the screen goes away.)
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _results = MutableSharedFlow<PaymentResult>(extraBufferCapacity = 1)
    val results: SharedFlow<PaymentResult> = _results

    /** Opens the Razorpay payment screen for this booking's total. */
    fun start(activity: Activity, booking: Booking, email: String, phone: String) {
        payingBookingId = booking.id
        activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_BOOKING, booking.id).apply()
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

    /** The booking being paid: from memory, or from the phone if the app was restarted. */
    private fun takeBookingId(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val id = payingBookingId ?: prefs.getString(KEY_BOOKING, null)
        payingBookingId = null
        prefs.edit().remove(KEY_BOOKING).apply()
        return id
    }

    /**
     * Called by MainActivity when Razorpay says the payment worked.
     * We save it on the booking HERE (not in the screen), then tell the screen the result.
     */
    fun onSuccess(context: Context, paymentId: String?) {
        val bookingId = takeBookingId(context) ?: return
        scope.launch {
            val result = try {
                BookingRepository.markPaidOnline(bookingId, paymentId.orEmpty())
                PaymentResult.Success(bookingId, paymentId.orEmpty())
            } catch (e: Exception) {
                PaymentResult.Failure(bookingId, "Paid, but could not save it: " + BookingRepository.friendlyError(e))
            }
            _results.tryEmit(result)
        }
    }

    /** Called by MainActivity when the payment failed or the customer closed the screen. */
    fun onError(context: Context, description: String?) {
        val bookingId = takeBookingId(context) ?: return
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
