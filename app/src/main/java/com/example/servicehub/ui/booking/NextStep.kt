package com.example.servicehub.ui.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.servicehub.data.model.Booking
import com.example.servicehub.data.model.BookingStatus
import com.example.servicehub.data.model.PaymentStatus
import com.example.servicehub.data.model.RevisitStatus

/**
 * "What to do now" for one booking, in one short sentence, so nobody has to read the
 * whole screen to know the next step. null = nothing to do (finished booking).
 * Pure functions (no Android), so they are unit-tested.
 */
fun customerNextStep(b: Booking): String? = when {
    b.isExpired -> "The provider did not answer in time. Reschedule or find another provider."
    b.revisitStatus == RevisitStatus.REQUESTED ->
        "Redo requested. Give your redo PIN ${b.revisitPin} to the professional only after the job is fixed."
    else -> when (b.bookingStatus) {
        BookingStatus.REQUESTED -> "Wait for ${b.providerName} to accept. You will get a notification."
        BookingStatus.ACCEPTED -> "Tell PIN ${b.startPin} to ${b.providerName} when they arrive."
        BookingStatus.IN_PROGRESS -> "${b.providerName} is working. You will pay after the job."
        BookingStatus.COMPLETED -> when {
            b.paymentStatus != PaymentStatus.PAID && b.paymentMode == "UPI" -> "Pay ₹${b.totalAmount} with UPI: tap Pay now below."
            b.paymentStatus != PaymentStatus.PAID -> "Pay ₹${b.totalAmount} in cash to ${b.providerName}."
            !b.isRated -> "All done. Please rate ${b.providerName}."
            else -> null
        }
        else -> null   // rejected / cancelled: the hint under the steps explains it
    }
}

fun providerNextStep(b: Booking): String? = when {
    b.isExpired -> null
    b.revisitStatus == RevisitStatus.REQUESTED ->
        "Redo requested: visit, fix the problem, then enter the customer's redo PIN below."
    else -> when (b.bookingStatus) {
        BookingStatus.REQUESTED -> "Accept or reject this request."
        BookingStatus.ACCEPTED ->
            if (b.onTheWayAt == 0L) "Tap \"I'm on my way\" when you leave, then start with the customer's PIN."
            else "At the address: ask the customer for the PIN and start the job."
        BookingStatus.IN_PROGRESS -> "When the work is finished, tap Complete job (add any extra parts)."
        BookingStatus.COMPLETED -> when {
            b.paymentStatus != PaymentStatus.PAID && b.paymentMode == "UPI" -> "Wait for the customer's UPI payment of ₹${b.totalAmount}."
            b.paymentStatus != PaymentStatus.PAID -> "Collect ₹${b.totalAmount} in cash and tap Cash collected."
            else -> null
        }
        else -> null
    }
}

/** A coloured "Next step" box at the top of a booking screen. Shows nothing if [text] is null. */
@Composable
fun NextStepCard(text: String?) {
    if (text == null) return
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.medium)
            .padding(14.dp),
    ) {
        Text("NEXT STEP", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
    }
}
