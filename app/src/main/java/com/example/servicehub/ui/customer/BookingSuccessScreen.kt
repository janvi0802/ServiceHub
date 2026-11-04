package com.example.servicehub.ui.customer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.servicehub.ui.theme.StatusCompleted

/** Shown right after a booking is saved. */
@Composable
fun BookingSuccessScreen(
    bookingId: String,
    onViewBooking: () -> Unit,
    onGoHome: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Filled.CheckCircle, null, tint = StatusCompleted, modifier = Modifier.size(96.dp))
        Spacer(Modifier.height(16.dp))
        Text("Booking requested!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "The provider will confirm soon. You can follow the status live in My Bookings.",
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text("Booking ID: ${bookingId.take(8).uppercase()}", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onViewBooking, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("View booking") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onGoHome, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Back to home") }
    }
}
