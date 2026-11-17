package com.example.servicehub.ui.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.servicehub.data.model.BookingStatus
import com.example.servicehub.ui.components.LoadingBox
import com.example.servicehub.ui.components.StatusChip
import com.example.servicehub.ui.components.rupees

/** Admin home: live counts, shortcuts, sample data and logout. */
@Composable
fun AdminDashboardScreen(
    onOpenPendingProviders: () -> Unit,
    onOpenAllProviders: () -> Unit,
    onOpenUsers: () -> Unit,
    onOpenBookings: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: AdminDashboardViewModel = viewModel(),
) {
    val stats by viewModel.stats.collectAsState()
    val sample by viewModel.sampleState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Title row with a logout button (admin has no Profile tab)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Admin Dashboard",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = {
                viewModel.logout()
                onLoggedOut()
            }) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Log out")
            }
        }

        val s = stats
        if (s == null) {
            LoadingBox()
        } else {
            // Tappable count cards, two per row
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CountCard("Customers", s.customers.toString(), Modifier.weight(1f), onOpenUsers)
                CountCard("Providers", s.approvedProviders.toString(), Modifier.weight(1f), onOpenAllProviders)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CountCard("Pending approvals", s.pendingProviders.toString(), Modifier.weight(1f), onOpenPendingProviders)
                CountCard("Blocked users", s.blockedUsers.toString(), Modifier.weight(1f), onOpenUsers)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CountCard("Total bookings", s.totalBookings.toString(), Modifier.weight(1f), onOpenBookings)
                CountCard("Completed value", rupees(s.completedValue), Modifier.weight(1f), onOpenBookings)
            }

            // Bookings by status
            Card(Modifier.fillMaxWidth().clickable(onClick = onOpenBookings)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Bookings by status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    BookingStatus.entries.forEach { status ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusChip(status)
                            Text(
                                (s.bookingsByStatus[status] ?: 0).toString(),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f).padding(start = 12.dp),
                            )
                        }
                    }
                }
            }
        }

        // Sample data card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Sample data", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (sample.alreadyLoaded) "Services are loaded. Pressing again resets the sample services and providers."
                    else "Adds all services with prices and 2–3 approved providers per category.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(onClick = viewModel::loadSampleData, enabled = !sample.isLoading) {
                    Text(if (sample.isLoading) "Loading..." else "Load sample data")
                }
                sample.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            }
        }
    }
}

@Composable
private fun CountCard(label: String, value: String, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier.clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
