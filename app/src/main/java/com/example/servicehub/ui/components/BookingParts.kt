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
import com.example.servicehub.data.model.Booking
import com.example.servicehub.data.model.BookingStatus

/**
 * A booking in a list. [otherPersonName] is the provider (on the customer side)
 * or the customer (on the provider side).
 */
@Composable
fun BookingCard(booking: Booking, otherPersonName: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    booking.serviceName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                StatusChip(booking.bookingStatus)
            }
            Text(otherPersonName, style = MaterialTheme.typography.bodyMedium)
            Text(
                "${DateUtils.pretty(booking.date)} • ${booking.timeSlot}",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(rupees(booking.totalAmount), fontWeight = FontWeight.Bold)
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

@Composable
private fun LineSegment(visible: Boolean, color: Color) {
    Box(
        Modifier
            .width(20.dp)
            .height(3.dp)
            .background(if (visible) color else Color.Transparent)
    )
}
