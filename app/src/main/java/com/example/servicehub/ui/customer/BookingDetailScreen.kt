package com.example.servicehub.ui.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.servicehub.data.DateUtils
import com.example.servicehub.data.FixedData
import com.example.servicehub.data.model.Booking
import com.example.servicehub.data.model.BookingStatus
import com.example.servicehub.ui.booking.BookingDetailViewModel
import com.example.servicehub.ui.booking.BookingInfoCard
import com.example.servicehub.ui.booking.BookingLoad
import com.example.servicehub.ui.components.EmptyBox
import com.example.servicehub.ui.components.LoadingBox
import com.example.servicehub.ui.components.ReasonDialog
import com.example.servicehub.ui.components.ScreenHeader
import com.example.servicehub.ui.components.StatusSteps
import com.example.servicehub.ui.components.openDialer
import java.util.Calendar

/** Customer's view of one booking: live status, PIN, call, cancel, reschedule, rate, receipt. */
@Composable
fun BookingDetailScreen(
    onBack: () -> Unit,
    onRate: (String) -> Unit,
    onReceipt: (String) -> Unit,
    onBookAgain: (providerId: String, serviceId: String) -> Unit,
    viewModel: BookingDetailViewModel = viewModel(),
) {
    val load by viewModel.booking.collectAsState()
    val isBusy by viewModel.isBusy.collectAsState()
    val message by viewModel.message.collectAsState()
    val context = LocalContext.current

    var showCancel by rememberSaveable { mutableStateOf(false) }
    var showReschedule by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Booking details", onBack)
        val booking = when (val l = load) {
            BookingLoad.Loading -> { LoadingBox(); return@Column }
            BookingLoad.NotFound -> { EmptyBox("Booking not found."); return@Column }
            is BookingLoad.Loaded -> l.booking
        }
        val status = booking.bookingStatus
        val canChange = status == BookingStatus.REQUESTED || status == BookingStatus.ACCEPTED

        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StatusSteps(status)
            StatusHint(status)

            // The PIN is shown only after the provider accepts; the provider needs it to start.
            if (status == BookingStatus.ACCEPTED) PinCard(booking.startPin)

            BookingInfoCard(booking, "Provider", booking.providerName)

            if (booking.bookingStatus.isActive) {
                OutlinedButton(
                    onClick = { openDialer(context, booking.providerPhone) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Call, contentDescription = null)
                    Text("  Call provider")
                }
            }

            if (canChange) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { viewModel.clearMessage(); showReschedule = true }, enabled = !isBusy, modifier = Modifier.weight(1f)) {
                        Text("Reschedule")
                    }
                    OutlinedButton(onClick = { viewModel.clearMessage(); showCancel = true }, enabled = !isBusy, modifier = Modifier.weight(1f)) {
                        Text("Cancel booking", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            if (status == BookingStatus.COMPLETED) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!booking.isRated) {
                        Button(onClick = { onRate(booking.id) }, modifier = Modifier.weight(1f)) { Text("Rate service") }
                    }
                    OutlinedButton(onClick = { onReceipt(booking.id) }, modifier = Modifier.weight(1f)) { Text("View receipt") }
                }
            }

            // Same provider and service again in one tap (the form checks the provider is still available).
            if (status == BookingStatus.COMPLETED || status == BookingStatus.CANCELLED) {
                OutlinedButton(
                    onClick = { onBookAgain(booking.providerId, booking.serviceId) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Book again") }
            }

            message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        }

        if (showCancel) {
            ReasonDialog(
                title = "Cancel this booking?",
                confirmText = "Cancel booking",
                onConfirm = { reason ->
                    showCancel = false            // close first, so the result message is visible
                    viewModel.cancel(reason) {}
                },
                onDismiss = { showCancel = false },
            )
        }
        if (showReschedule) {
            RescheduleDialog(
                booking = booking,
                isBusy = isBusy,
                error = message,
                onConfirm = { date, slot -> viewModel.reschedule(booking, date, slot) { showReschedule = false } },
                onDismiss = { showReschedule = false },
            )
        }
    }
}

/** One line explaining what happens next. */
@Composable
private fun StatusHint(status: BookingStatus) {
    val text = when (status) {
        BookingStatus.REQUESTED -> "Waiting for the provider to accept."
        BookingStatus.ACCEPTED -> "Confirmed! Share the PIN below when the provider arrives."
        BookingStatus.IN_PROGRESS -> "The provider is working on it."
        BookingStatus.COMPLETED -> "Job done. Thank you for using ServiceHub!"
        BookingStatus.REJECTED -> "The provider could not take this job. Please book another provider."
        BookingStatus.CANCELLED -> "You cancelled this booking."
    }
    Text(text, style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun PinCard(pin: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.medium)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Start-job PIN", style = MaterialTheme.typography.labelLarge)
        Text(pin, fontSize = 36.sp, fontWeight = FontWeight.Bold, letterSpacing = 8.sp)
        Text("Tell this PIN to the provider only when they arrive.", style = MaterialTheme.typography.bodySmall)
    }
}

/** Popup to pick a new date and slot. Today's finished slots are disabled. */
@Composable
private fun RescheduleDialog(
    booking: Booking,
    isBusy: Boolean,
    error: String?,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var date by remember { mutableStateOf(booking.date) }
    var slot by remember { mutableStateOf(booking.timeSlot) }
    val hourNow = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    fun slotOver(d: String, s: String) = d == DateUtils.today() && FixedData.isSlotOverToday(s, hourNow)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reschedule") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Date", fontWeight = FontWeight.Bold)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DateUtils.nextDays(7).forEach { d ->
                        FilterChip(selected = d == date, onClick = { date = d }, label = { Text(DateUtils.chipLabel(d)) })
                    }
                }
                Text("Slot", fontWeight = FontWeight.Bold)
                FixedData.timeSlots.forEach { s ->
                    FilterChip(
                        selected = s == slot,
                        onClick = { slot = s },
                        enabled = !slotOver(date, s),
                        label = { Text(s) },
                        modifier = Modifier.height(36.dp),
                    )
                }
                if (booking.bookingStatus == BookingStatus.ACCEPTED) {
                    Text("The provider will need to accept the new time again.", style = MaterialTheme.typography.bodySmall)
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(date, slot) },
                enabled = !isBusy && !slotOver(date, slot),
            ) { Text(if (isBusy) "Saving..." else "Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Back") } },
    )
}
