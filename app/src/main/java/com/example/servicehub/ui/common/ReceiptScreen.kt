package com.example.servicehub.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HomeRepairService
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.IconButton
import androidx.compose.ui.platform.LocalContext
import com.example.servicehub.ui.components.receiptShareText
import com.example.servicehub.ui.components.shareText
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.servicehub.data.DateUtils
import com.example.servicehub.data.model.BookingStatus
import com.example.servicehub.data.model.PaymentStatus
import com.example.servicehub.ui.booking.BookingDetailViewModel
import com.example.servicehub.ui.booking.BookingLoad
import com.example.servicehub.ui.components.EmptyBox
import com.example.servicehub.ui.components.InfoRow
import com.example.servicehub.ui.components.LoadingBox
import com.example.servicehub.ui.components.ScreenHeader
import com.example.servicehub.ui.components.rupees
import com.example.servicehub.ui.theme.StatusCompleted

/** A clean receipt for a completed booking (customer and provider can both open it). */
@Composable
fun ReceiptScreen(
    onBack: () -> Unit,
    viewModel: BookingDetailViewModel = viewModel(),   // reuses the live booking loader
) {
    val load by viewModel.booking.collectAsState()
    val context = LocalContext.current
    val loaded = (load as? BookingLoad.Loaded)?.booking

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Receipt", onBack) {
            if (loaded != null && loaded.bookingStatus == BookingStatus.COMPLETED) {
                IconButton(onClick = { shareText(context, "ServiceHub receipt", receiptShareText(loaded)) }) {
                    Icon(Icons.Filled.Share, contentDescription = "Share receipt")
                }
            }
        }
        val b = when (val l = load) {
            BookingLoad.Loading -> { LoadingBox(); return@Column }
            BookingLoad.NotFound -> { EmptyBox("Booking not found."); return@Column }
            is BookingLoad.Loaded -> l.booking
        }
        if (b.bookingStatus != BookingStatus.COMPLETED) {
            EmptyBox("A receipt is available after the job is completed.")
            return@Column
        }

        Card(
            Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.HomeRepairService, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                    Text("ServiceHub", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Service receipt", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (b.paymentStatus == PaymentStatus.PAID) "PAID" else "PAYMENT PENDING",
                        color = StatusCompleted, fontWeight = FontWeight.Bold,
                    )
                }
                HorizontalDivider(Modifier.padding(vertical = 10.dp))
                InfoRow("Booking ID", b.id.take(8).uppercase())
                InfoRow("Service", b.serviceName)
                InfoRow("Provider", b.providerName)
                InfoRow("Customer", b.customerName)
                InfoRow("Date", DateUtils.pretty(b.date))
                InfoRow("Slot", b.timeSlot)
                InfoRow("Address", b.address)
                InfoRow("Completed", DateUtils.dateTime(b.completedAt))
                HorizontalDivider(Modifier.padding(vertical = 10.dp))
                InfoRow("Service price", rupees(b.servicePrice))
                b.extraItems.forEach { InfoRow(it.name, rupees(it.amount)) }   // spare parts / extra work
                InfoRow("Visit fee (ServiceHub)", rupees(b.visitFee))
                if (b.discount > 0) InfoRow("Coupon ${b.couponCode}", "− ${rupees(b.discount)}")
                HorizontalDivider(Modifier.padding(vertical = 6.dp))
                InfoRow(if (b.paymentStatus == PaymentStatus.PAID) "Total paid" else "Total due", rupees(b.totalAmount), bold = true)
                InfoRow("Payment mode", if (b.paymentId.isNotBlank()) "UPI online (Razorpay test mode)" else b.paymentMode)
                if (b.paymentId.isNotBlank()) InfoRow("Payment ID", b.paymentId)
                Spacer(Modifier.height(12.dp))
                Text(
                    "Thank you for choosing ServiceHub!",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }
    }
}
