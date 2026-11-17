package com.example.servicehub.ui.provider

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
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.example.servicehub.ui.components.TextInput
import com.example.servicehub.ui.components.openDialer
import com.example.servicehub.ui.components.rupees

/**
 * Provider's view of one job. The buttons change with the status:
 * REQUESTED -> Accept / Reject, ACCEPTED -> enter PIN to start, IN_PROGRESS -> Complete + payment.
 */
@Composable
fun JobDetailScreen(
    onBack: () -> Unit,
    onReceipt: (String) -> Unit,
    viewModel: BookingDetailViewModel = viewModel(),
) {
    val load by viewModel.booking.collectAsState()
    val isBusy by viewModel.isBusy.collectAsState()
    val message by viewModel.message.collectAsState()
    val context = LocalContext.current

    var showReject by rememberSaveable { mutableStateOf(false) }
    var showComplete by rememberSaveable { mutableStateOf(false) }
    var pin by rememberSaveable { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Job details", onBack)
        val booking = when (val l = load) {
            BookingLoad.Loading -> { LoadingBox(); return@Column }
            BookingLoad.NotFound -> { EmptyBox("Job not found."); return@Column }
            is BookingLoad.Loaded -> l.booking
        }
        val status = booking.bookingStatus

        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StatusSteps(status)
            BookingInfoCard(booking, "Customer", booking.customerName)

            if (status.isActive) {
                OutlinedButton(onClick = { openDialer(context, booking.customerPhone) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Call, contentDescription = null)
                    Text("  Call customer")
                }
            }

            when (status) {
                BookingStatus.REQUESTED -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = viewModel::accept, enabled = !isBusy, modifier = Modifier.weight(1f).height(50.dp)) {
                        Text("Accept")
                    }
                    OutlinedButton(
                        onClick = { viewModel.clearMessage(); showReject = true },
                        enabled = !isBusy,
                        modifier = Modifier.weight(1f).height(50.dp),
                    ) { Text("Reject", color = MaterialTheme.colorScheme.error) }
                }

                BookingStatus.ACCEPTED -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Start the job", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Ask the customer for their 4-digit PIN when you arrive.", style = MaterialTheme.typography.bodySmall)
                    TextInput(
                        value = pin,
                        onValueChange = { pin = it.filter(Char::isDigit).take(4) },
                        label = "Customer's PIN",
                        keyboardType = KeyboardType.NumberPassword,
                    )
                    Button(
                        onClick = { viewModel.start(pin) { pin = "" } },
                        enabled = !isBusy && pin.length == 4,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                    ) { Text("Start job") }
                }

                BookingStatus.IN_PROGRESS -> Button(
                    onClick = { viewModel.clearMessage(); showComplete = true },
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                ) { Text("Complete job • collect ${rupees(booking.totalAmount)}") }

                BookingStatus.COMPLETED -> OutlinedButton(
                    onClick = { onReceipt(booking.id) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("View receipt") }

                else -> Unit   // rejected / cancelled jobs have no actions
            }

            message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        }

        if (showReject) {
            ReasonDialog(
                title = "Reject this job?",
                confirmText = "Reject",
                onConfirm = { reason ->
                    showReject = false
                    viewModel.reject(reason) {}
                },
                onDismiss = { showReject = false },
            )
        }
        if (showComplete) {
            CompleteJobDialog(
                booking = booking,
                onConfirm = { mode ->
                    showComplete = false
                    viewModel.complete(mode) {}
                },
                onDismiss = { showComplete = false },
            )
        }
    }
}

/** Confirms the payment mode and that the money was received, before completing. */
@Composable
private fun CompleteJobDialog(booking: Booking, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var mode by remember { mutableStateOf(booking.paymentMode.ifBlank { FixedData.paymentModes.first() }) }
    var received by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Complete job") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Amount to collect: ${rupees(booking.totalAmount)}", fontWeight = FontWeight.Bold)
                Text("Paid by")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FixedData.paymentModes.forEach { m ->
                        FilterChip(selected = m == mode, onClick = { mode = m }, label = { Text(m) })
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = received, onCheckedChange = { received = it })
                    Text("Payment received")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(mode) }, enabled = received) { Text("Complete") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Back") } },
    )
}
