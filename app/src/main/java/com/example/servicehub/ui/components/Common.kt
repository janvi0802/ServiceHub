package com.example.servicehub.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.servicehub.data.DateUtils
import com.example.servicehub.data.model.ApprovalStatus
import com.example.servicehub.data.model.Booking
import com.example.servicehub.data.model.BookingStatus
import com.example.servicehub.data.model.Provider
import com.example.servicehub.data.model.isTopRated
import com.example.servicehub.ui.theme.BrandOrange
import com.example.servicehub.ui.theme.StatusCompleted

/** Title row at the top of a screen, with a back arrow when [onBack] is given. */
@Composable
fun ScreenHeader(title: String, onBack: (() -> Unit)? = null, actions: @Composable () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        } else {
            Box(Modifier.padding(start = 8.dp))
        }
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        actions()
    }
}

/** Spinner in the middle of the screen. */
@Composable
fun LoadingBox() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        androidx.compose.material3.CircularProgressIndicator()
    }
}

/** Icon + message in the middle of the screen, e.g. "No bookings yet". */
@Composable
fun EmptyBox(message: String, icon: ImageVector = Icons.Filled.Inbox) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                icon, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp),
            )
            Text(
                message,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

/** Small "✔ Verified" and "★ Top rated" labels shown next to a provider. */
@Composable
fun ProviderBadges(provider: Provider) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (provider.approvalStatus == ApprovalStatus.APPROVED) {
            SmallBadge("✔ Verified", StatusCompleted)
        }
        if (isTopRated(provider)) {
            SmallBadge("★ Top rated", BrandOrange)
        }
    }
}

@Composable
private fun SmallBadge(text: String, color: Color) {
    Text(
        text,
        color = color,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

/** A small colored label showing a booking status. */
@Composable
fun StatusChip(status: BookingStatus) = StatusChip(status.label, status.color)

/** Status label for a whole booking: shows "Expired" (grey) for old unanswered requests. */
@Composable
fun BookingStatusChip(booking: Booking) {
    when {
        booking.isExpired -> StatusChip("Expired", Color.Gray)
        booking.isOverdue -> StatusChip("Overdue", Color(0xFFE8710A))   // orange: provider is late
        else -> StatusChip(booking.bookingStatus)
    }
}

@Composable
fun StatusChip(label: String, color: Color) {
    Text(
        text = label,
        color = Color.White,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(color, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** One "label: value" line, used on detail and receipt screens. */
@Composable
fun InfoRow(label: String, value: String, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

/** "★ 4.5 (12 reviews)", or "New" if nobody has rated yet. */
fun ratingText(ratingAvg: Double, ratingCount: Int): String =
    if (ratingCount == 0) "★ New" else "★ %.1f (%d)".format(ratingAvg, ratingCount)

/** 1999 -> "₹1,999" */
fun rupees(amount: Int): String = "₹" + "%,d".format(amount)

/**
 * Opens the phone's dialer with the number filled in.
 * ACTION_DIAL does not call by itself, so no permission is needed.
 */
fun openDialer(context: Context, phone: String) {
    val intent = Intent(Intent.ACTION_DIAL, "tel:$phone".toUri())
    context.startActivity(intent)
}

/** A simple "Are you sure?" popup, so one accidental tap cannot change a booking. */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmText) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Back") } },
    )
}

/**
 * Opens the phone's maps app (e.g. Google Maps) searching for this address.
 * "geo:" links are built into Android: no API key, no GPS permission, no library.
 * If no maps app is installed, nothing happens.
 */
fun openMaps(context: Context, address: String) {
    val intent = Intent(Intent.ACTION_VIEW, "geo:0,0?q=${Uri.encode(address)}".toUri())
    runCatching { context.startActivity(intent) }
}

/**
 * Opens Android's share menu (WhatsApp, SMS, email...) with [text].
 * ACTION_SEND is built into Android, so no library or permission is needed.
 */
fun shareText(context: Context, subject: String, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, "Share via"))
}

/** Plain-text version of a booking, for sharing (e.g. with family: who is coming and when). */
fun bookingShareText(b: Booking): String = buildString {
    appendLine("ServiceHub booking ${b.id.take(8).uppercase()}")
    appendLine("${b.serviceName} with ${b.providerName}")
    appendLine("${DateUtils.pretty(b.date)}, ${b.timeSlot}")
    appendLine("Address: ${b.address}")
    appendLine("Total: ${rupees(b.totalAmount)} (${b.paymentMode})")
    append("Status: ${b.bookingStatus.label}")
}

/** Plain-text receipt for a completed booking. */
fun receiptShareText(b: Booking): String = buildString {
    appendLine("ServiceHub – Receipt")
    appendLine("Booking ID: ${b.id.take(8).uppercase()}")
    appendLine("Service: ${b.serviceName}")
    appendLine("Provider: ${b.providerName}")
    appendLine("Date: ${DateUtils.pretty(b.date)}, ${b.timeSlot}")
    appendLine("Service price: ${rupees(b.servicePrice)}")
    appendLine("Visit fee: ${rupees(b.visitFee)}")
    appendLine("Total paid: ${rupees(b.totalAmount)} by ${b.paymentMode}")
    append("Thank you for choosing ServiceHub!")
}

/**
 * A popup that asks for a reason (used for cancel and reject).
 * [onConfirm] gets the typed reason; the button stays disabled until something is typed.
 */
@Composable
fun ReasonDialog(
    title: String,
    confirmText: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                TextInput(reason, { reason = it }, "Reason", singleLine = false)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(reason.trim()) }, enabled = reason.isNotBlank()) {
                Text(confirmText)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Back") } },
    )
}
