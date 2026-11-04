package com.example.servicehub.ui.admin

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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Admin home. Phase 3: pending approvals + "Load sample data".
 * Phase 11 adds the full counts (customers, bookings, earnings...).
 */
@Composable
fun AdminDashboardScreen(
    onOpenPendingProviders: () -> Unit,
    onOpenAllProviders: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: AdminDashboardViewModel = viewModel(),
) {
    val pendingCount by viewModel.pendingCount.collectAsState()
    val sample by viewModel.sampleState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
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

        // Pending approvals card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Pending provider approvals", style = MaterialTheme.typography.titleMedium)
                Text(
                    "$pendingCount",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onOpenPendingProviders) { Text("Review") }
                    OutlinedButton(onClick = onOpenAllProviders) { Text("All providers") }
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
