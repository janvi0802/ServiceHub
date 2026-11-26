package com.example.servicehub

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.servicehub.data.CategoryStore
import com.example.servicehub.navigation.AppNavHost
import com.example.servicehub.payment.RazorpayPayments
import com.example.servicehub.ui.theme.ServiceHubTheme
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener

/**
 * The only Activity. Every screen is a Composable inside AppNavHost.
 * It also receives Razorpay's payment result (Razorpay always reports back to the Activity).
 */
class MainActivity : ComponentActivity(), PaymentResultWithDataListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Keep the category list (built-in + admin-added) up to date for the whole app.
        CategoryStore.start()
        // Opened by tapping a push? Remember which booking screen to show.
        com.example.servicehub.push.PushLink.take(intent)
        // Loads Razorpay's screen in advance so "Pay now" opens faster.
        if (RazorpayPayments.isConfigured) Checkout.preload(applicationContext)
        // Our app is always light, so ask for DARK status-bar icons (clock, battery)
        // even when the phone is in dark mode. Otherwise they would be white on white.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            ServiceHubTheme {
                AppNavHost()
            }
        }
    }

    /** Push tapped while the app was already open. */
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        com.example.servicehub.push.PushLink.take(intent)
    }

    // ---- Razorpay results (passed on to RazorpayPayments, which tells the booking screen)

    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        RazorpayPayments.onSuccess(applicationContext, razorpayPaymentId)
    }

    override fun onPaymentError(code: Int, description: String?, paymentData: PaymentData?) {
        RazorpayPayments.onError(applicationContext, description)
    }
}
