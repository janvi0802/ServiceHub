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
import androidx.compose.material.icons.filled.Map
import com.example.servicehub.ui.components.openMaps
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
import com.example.servicehub.data.model.ExtraItem
import com.example.servicehub.data.model.RevisitStatus
import com.example.servicehub.ui.components.Caption
import com.example.servicehub.ui.components.InfoBanner
import com.example.servicehub.ui.components.InfoRow
import com.example.servicehub.ui.theme.StatusAccepted
import com.example.servicehub.ui.theme.StatusCompleted
import com.example.servicehub.ui.theme.StatusRequested
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.runtime.mutableStateListOf
import com.example.servicehub.data.model.Booking
import com.example.servicehub.data.model.BookingStatus
import com.example.servicehub.ui.booking.BookingDetailViewModel
import com.example.servicehub.ui.booking.BookingInfoCard
import com.example.servicehub.ui.booking.BookingLoad
import com.example.servicehub.data.DateUtils
import com.example.servicehub.ui.components.ConfirmDialog
import com.example.servicehub.ui.components.EmptyBox
import com.example.servicehub.ui.components.LoadingBox
import com.example.servicehub.ui.components.ReasonDialog
import com.example.servicehub.ui.components.ScreenHeader
import com.example.servicehub.ui.components.StatusSteps
import com.example.servicehub.ui.components.TimelineCard
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
    var showAccept by rememberSaveable { mutableStateOf(false) }
    var showStart by rememberSaveable { mutableStateOf(false) }
    var showCancel by rememberSaveable { mutableStateOf(false) }
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
            TimelineCard(booking)

            if (status.isActive) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { openDialer(context, booking.customerPhone) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Call, contentDescription = null)
                        Text("  Call")
                    }
                    OutlinedButton(onClick = { openMaps(context, booking.address) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Map, contentDescription = null)
                        Text("  Open in Maps")
                    }
                }
            }

            when {
                // Nobody accepted it in time: nothing can be done any more.
                booking.isExpired -> Text(
                    "This request has expired: its time has already passed.",
                    color = MaterialTheme.colorScheme.error,
                )

                status == BookingStatus.REQUESTED -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.clearMessage(); showAccept = true },
                        enabled = !isBusy,
                        modifier = Modifier.weight(1f).height(50.dp),
                    ) {
                        Text("Accept")
                    }
                    OutlinedButton(
                        onClick = { viewModel.clearMessage(); showReject = true },
                        enabled = !isBusy,
                        modifier = Modifier.weight(1f).height(50.dp),
                    ) { Text("Reject", color = MaterialTheme.colorScheme.error) }
                }

                status == BookingStatus.ACCEPTED -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (booking.isOverdue) {
                        Text(
                            "Overdue: this job was for ${DateUtils.pretty(booking.date)}. Call the customer, then start or cancel it.",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    // Step 1: tell the customer you are coming
                    if (booking.onTheWayAt == 0L) {
                        OutlinedButton(
                            onClick = viewModel::markOnTheWay,
                            enabled = !isBusy,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                        ) {
                            Icon(Icons.Filled.DirectionsRun, contentDescription = null)
                            Text("  I'm on my way")
                        }
                    } else {
                        InfoBanner(
                            Icons.Filled.DirectionsRun,
                            "Customer was told you are on the way (${DateUtils.dateTime(booking.onTheWayAt).substringAfter(", ")}).",
                            StatusAccepted,
                        )
                    }
                    // Step 2: start with the PIN
                    Text("Start the job", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Ask the customer for their 4-digit PIN when you arrive.", style = MaterialTheme.typography.bodySmall)
                    TextInput(
                        value = pin,
                        onValueChange = { pin = it.filter(Char::isDigit).take(4) },
                        label = "Customer's PIN",
                        keyboardType = KeyboardType.NumberPassword,
                    )
                    Button(
                        onClick = { viewModel.clearMessage(); showStart = true },
                        enabled = !isBusy && pin.length == 4,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                    ) { Text("Start job") }
                    TextButton(
                        onClick = { viewModel.clearMessage(); showCancel = true },
                        enabled = !isBusy,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Can't come? Cancel this job", color = MaterialTheme.colorScheme.error) }
                }

                status == BookingStatus.IN_PROGRESS -> Button(
                    onClick = { viewModel.clearMessage(); showComplete = true },
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                ) { Text("Complete job • collect ${rupees(booking.totalAmount)}") }

                status == BookingStatus.COMPLETED -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 7-day guarantee: the customer asked for a free revisit
                    if (booking.revisitStatus == RevisitStatus.REQUESTED) {
                        InfoBanner(
                            Icons.Filled.Replay,
                            "Free revisit requested: \"${booking.revisitReason}\". Call the customer and fix it.",
                            StatusRequested,
                        )
                        Button(
                            onClick = viewModel::markRevisitDone,
                            enabled = !isBusy,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                        ) { Text("Mark revisit done") }
                    } else if (booking.revisitStatus == RevisitStatus.DONE) {
                        InfoBanner(Icons.Filled.TaskAlt, "Free revisit completed.", StatusCompleted)
                    }
                    OutlinedButton(
                        onClick = { onReceipt(booking.id) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("View receipt") }
                }

                else -> Unit   // rejected / cancelled jobs have no actions
            }

            message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        }

        if (showAccept) {
            ConfirmDialog(
                title = "Accept this job?",
                message = "${booking.serviceName} for ${booking.customerName} on ${DateUtils.pretty(booking.date)}, ${booking.timeSlot}.",
                confirmText = "Accept",
                onConfirm = {
                    showAccept = false
                    viewModel.accept()
                },
                onDismiss = { showAccept = false },
            )
        }
        if (showStart) {
            ConfirmDialog(
                title = "Start the job now?",
                message = "Start only when you are at the customer's address.",
                confirmText = "Start job",
                onConfirm = {
                    showStart = false
                    viewModel.start(pin) { pin = "" }
                },
                onDismiss = { showStart = false },
            )
        }
        if (showCancel) {
            ReasonDialog(
                title = "Cancel this accepted job?",
                confirmText = "Cancel job",
                onConfirm = { reason ->
                    showCancel = false
                    viewModel.cancelByProvider(reason)
                },
                onDismiss = { showCancel = false },
            )
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
                onConfirm = { mode, extras ->
                    showComplete = false
                    viewModel.complete(mode, extras) {}
                },
                onDismiss = { showComplete = false },
            )
        }
    }
}

/**
 * Before completing: optional extra items (spare parts / extra work), payment mode,
 * and "Payment received". The amount to collect updates as items are added.
 */
@Composable
private fun CompleteJobDialog(booking: Booking, onConfirm: (String, List<ExtraItem>) -> Unit, onDismiss: () -> Unit) {
    var mode by remember { mutableStateOf(booking.paymentMode.ifBlank { FixedData.paymentModes.first() }) }
    var received by remember { mutableStateOf(false) }
    // Each row: name + amount typed as text (turned into numbers when saving)
    val names = remember { mutableStateListOf<String>() }
    val amounts = remember { mutableStateListOf<String>() }
    val extraSum = amounts.sumOf { it.toIntOrNull() ?: 0 }
    val rowsValid = names.indices.all { i ->
        (names[i].isBlank() && amounts[i].isBlank()) ||
            (names[i].isNotBlank() && (amounts[i].toIntOrNull() ?: 0) in 1..50_000)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Complete job") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoRow("Service + visit fee", rupees(booking.servicePrice + booking.visitFee))
                Text("Extra parts or work (optional)", style = MaterialTheme.typography.titleSmall)
                names.indices.forEach { i ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextInput(names[i], { names[i] = it.take(30) }, "Item", modifier = Modifier.weight(1.4f))
                        TextInput(
                            amounts[i], { amounts[i] = it.filter(Char::isDigit).take(5) }, "₹",
                            keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { names.removeAt(i); amounts.removeAt(i) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove item")
                        }
                    }
                }
                if (names.size < 10) {
                    TextButton(onClick = { names.add(""); amounts.add("") }) { Text("+ Add item (e.g. tap cartridge)") }
                }
                if (!rowsValid) Caption("Each item needs a name and an amount from ₹1 to ₹50,000.", color = MaterialTheme.colorScheme.error)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    "Amount to collect: ${rupees(booking.servicePrice + booking.visitFee + extraSum)}",
                    fontWeight = FontWeight.Bold,
                )
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
            TextButton(
                onClick = {
                    val extras = names.indices
                        .filter { names[it].isNotBlank() }
                        .map { ExtraItem(names[it].trim(), amounts[it].toIntOrNull() ?: 0) }
                    onConfirm(mode, extras)
                },
                enabled = received && rowsValid,
            ) { Text("Complete") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Back") } },
    )
}
