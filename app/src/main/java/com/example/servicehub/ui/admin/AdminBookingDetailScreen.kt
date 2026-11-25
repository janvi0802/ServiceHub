package com.example.servicehub.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.servicehub.ui.booking.BookingDetailViewModel
import com.example.servicehub.ui.booking.BookingInfoCard
import com.example.servicehub.ui.booking.BookingLoad
import com.example.servicehub.ui.components.AppCard
import com.example.servicehub.ui.components.EmptyBox
import com.example.servicehub.ui.components.InfoRow
import com.example.servicehub.ui.components.LoadingBox
import com.example.servicehub.ui.components.ScreenHeader
import com.example.servicehub.ui.components.SectionLabel
import com.example.servicehub.ui.components.StatusSteps
import com.example.servicehub.ui.components.TimelineCard
import com.example.servicehub.ui.components.openDialer

/**
 * Admin: everything about one booking, READ-ONLY (no accept/cancel buttons),
 * plus "Call customer" / "Call provider" for handling complaints.
 * Reuses the same live BookingDetailViewModel as the customer and provider screens.
 */
@Composable
fun AdminBookingDetailScreen(onBack: () -> Unit, viewModel: BookingDetailViewModel = viewModel()) {
    val load by viewModel.booking.collectAsState()
    val context = LocalContext.current

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Booking details", onBack, subtitle = "Admin view · read only")
        val b = when (val l = load) {
            BookingLoad.Loading -> { LoadingBox(); return@Column }
            BookingLoad.NotFound -> { EmptyBox("Booking not found."); return@Column }
            is BookingLoad.Loaded -> l.booking
        }
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatusSteps(b.bookingStatus)

            SectionLabel("People")
            AppCard {
                InfoRow("Customer", "${b.customerName} · ${b.customerPhone}")
                InfoRow("Provider", "${b.providerName} · ${b.providerPhone}")
                if (b.contactPhone.isNotBlank()) InfoRow("Person at address", "${b.contactName} · ${b.contactPhone}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    OutlinedButton(onClick = { openDialer(context, b.customerPhone) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Call, contentDescription = null); Text("  Customer")
                    }
                    OutlinedButton(onClick = { openDialer(context, b.providerPhone) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Call, contentDescription = null); Text("  Provider")
                    }
                }
            }

            SectionLabel("Booking")
            BookingInfoCard(b, "Provider", b.providerName)

            // Reasons and complaints, only when there are any
            val notes = buildList {
                if (b.rejectReason.isNotBlank()) add("Reject reason" to b.rejectReason)
                if (b.cancelReason.isNotBlank()) add("Cancel reason" to b.cancelReason)
                if (b.revisitReason.isNotBlank()) add("Redo complaint (${b.revisitStatus.lowercase()})" to b.revisitReason)
            }
            if (notes.isNotEmpty()) {
                SectionLabel("Reasons")
                AppCard { notes.forEach { (label, text) -> InfoRow(label, text) } }
            }

            SectionLabel("Timeline")
            TimelineCard(b)
        }
    }
}
