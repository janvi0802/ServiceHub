package com.example.servicehub.ui.booking

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.servicehub.data.DateUtils
import com.example.servicehub.data.model.Booking
import com.example.servicehub.data.model.PaymentStatus
import com.example.servicehub.ui.components.InfoRow
import com.example.servicehub.ui.components.rupees

/** The booking facts both sides need: when, where, what, how much. */
@Composable
fun BookingInfoCard(booking: Booking, personLabel: String, personName: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(booking.serviceName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            InfoRow(personLabel, personName)
            InfoRow("Date", DateUtils.pretty(booking.date))
            InfoRow("Slot", booking.timeSlot)
            InfoRow("Address", booking.address)
            // Booked for someone else: the person at the address
            if (booking.contactName.isNotBlank()) {
                InfoRow("Contact person", "${booking.contactName} · ${booking.contactPhone}")
            }
            if (booking.note.isNotBlank()) InfoRow("Problem", booking.note)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            InfoRow("Service price", rupees(booking.servicePrice))
            // Spare parts / extra work added by the provider at completion
            booking.extraItems.forEach { InfoRow(it.name, rupees(it.amount)) }
            InfoRow("Visit fee", rupees(booking.visitFee))
            InfoRow("Total", rupees(booking.totalAmount), bold = true)
            InfoRow(
                "Payment",
                "${booking.paymentMode} • " + when {
                    booking.paymentStatus == PaymentStatus.PAID && booking.paymentId.isNotBlank() -> "Paid online (test)"
                    booking.paymentStatus == PaymentStatus.PAID -> "Paid"
                    booking.bookingStatus == com.example.servicehub.data.model.BookingStatus.COMPLETED -> "Payment pending"
                    else -> "Pay after service"
                },
            )
            if (booking.paymentId.isNotBlank()) InfoRow("Payment ID", booking.paymentId)
            if (booking.rejectReason.isNotBlank()) InfoRow("Reject reason", booking.rejectReason)
            if (booking.cancelReason.isNotBlank()) InfoRow("Cancel reason", booking.cancelReason)
            Text(
                "Booking ID: ${booking.id.take(8).uppercase()}",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
