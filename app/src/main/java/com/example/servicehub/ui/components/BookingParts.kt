package com.example.servicehub.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.servicehub.data.DateUtils
import com.example.servicehub.data.FixedData
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.HorizontalDivider
import com.example.servicehub.data.model.Booking
import com.example.servicehub.data.model.BookingStatus

/**
 * A booking in a list. [otherPersonName] is the provider (on the customer side)
 * or the customer (on the provider side).
 */
@Composable
fun BookingCard(booking: Booking, otherPersonName: String, onClick: () -> Unit) {
    val category = FixedData.categoryById(booking.categoryId)
    AppCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.Top) {
            IconTile(category?.icon ?: Icons.Filled.Build, category?.color ?: MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(booking.serviceName, style = MaterialTheme.typography.titleMedium)
                Text(otherPersonName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            BookingStatusChip(booking)
        }
        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            Text(
                " ${DateUtils.pretty(booking.date)} · ${booking.timeSlot.substringBefore(" (")}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
            Text(rupees(booking.totalAmount), style = MaterialTheme.typography.titleSmall)
        }
    }
}

/**
 * The step indicator: Requested -> Accepted -> In progress -> Completed.
 * Finished steps get a filled circle with a tick. Rejected/Cancelled show a single colored message.
 */
@Composable
fun StatusSteps(status: BookingStatus) {
    if (status == BookingStatus.REJECTED || status == BookingStatus.CANCELLED) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(status.color.copy(alpha = 0.12f), MaterialTheme.shapes.medium)
                .padding(16.dp)
        ) {
            Text("This booking was ${status.label.lowercase()}.", color = status.color, fontWeight = FontWeight.Bold)
        }
        return
    }

    val steps = listOf(
        BookingStatus.REQUESTED, BookingStatus.ACCEPTED,
        BookingStatus.IN_PROGRESS, BookingStatus.COMPLETED,
    )
    val currentIndex = steps.indexOf(status)

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        steps.forEachIndexed { index, step ->
            val done = index <= currentIndex
            val color = if (done) step.color else Color.LightGray
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // line before the circle (not for the first step)
                    LineSegment(visible = index > 0, color = if (done) color else Color.LightGray)
                    Box(
                        Modifier.size(28.dp).background(color, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (done) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    // line after the circle (not for the last step)
                    LineSegment(
                        visible = index < steps.lastIndex,
                        color = if (index < currentIndex) color else Color.LightGray,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    step.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (index == currentIndex) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

/**
 * The moments that really happened, in order, e.g.
 * ("Requested", 10:02), ("Accepted", 10:15), ("Started", 11:30), ("Completed", 12:40).
 * Times that are 0 have not happened yet and are left out.
 */
fun timelineSteps(booking: Booking): List<Pair<String, Long>> {
    val steps = mutableListOf("Requested" to booking.createdAt)
    if (booking.acceptedAt > 0) steps += "Accepted" to booking.acceptedAt
    if (booking.startedAt > 0) steps += "Started" to booking.startedAt
    if (booking.completedAt > 0) steps += "Completed" to booking.completedAt
    when (booking.bookingStatus) {
        BookingStatus.REJECTED -> steps += "Rejected" to booking.updatedAt
        BookingStatus.CANCELLED -> steps += "Cancelled" to booking.updatedAt
        else -> Unit
    }
    return steps.filter { it.second > 0 }
}

/** A small "what happened when" list under the status steps. */
@Composable
fun TimelineCard(booking: Booking) {
    val steps = timelineSteps(booking)
    if (steps.isEmpty()) return
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Timeline", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            steps.forEachIndexed { index, (label, time) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // the newest step gets a filled dot
                    Box(
                        Modifier.size(10.dp).background(
                            if (index == steps.lastIndex) MaterialTheme.colorScheme.primary else Color.LightGray,
                            CircleShape,
                        )
                    )
                    Text(label, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 10.dp).weight(1f))
                    Text(DateUtils.dateTime(time), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun LineSegment(visible: Boolean, color: Color) {
    Box(
        Modifier
            .width(20.dp)
            .height(3.dp)
            .background(if (visible) color else Color.Transparent)
    )
}
