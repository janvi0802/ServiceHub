package com.example.servicehub.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AssignmentLate
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.servicehub.data.DateUtils
import com.example.servicehub.data.FixedData
import com.example.servicehub.data.model.BookingStatus
import com.example.servicehub.ui.components.AppCard
import com.example.servicehub.ui.components.Avatar
import com.example.servicehub.ui.components.BookingStatusChip
import com.example.servicehub.ui.components.Caption
import com.example.servicehub.ui.components.CardTitle
import com.example.servicehub.ui.components.HorizontalBars
import com.example.servicehub.ui.components.ListRow
import com.example.servicehub.ui.components.LoadingBox
import com.example.servicehub.ui.components.MiniBarChart
import com.example.servicehub.ui.components.RatingLabel
import com.example.servicehub.ui.components.ScreenHeader
import com.example.servicehub.ui.components.SectionHeader
import com.example.servicehub.ui.components.StatTile
import com.example.servicehub.ui.components.StatusChip
import com.example.servicehub.ui.components.rupees
import com.example.servicehub.ui.theme.BrandBlue
import com.example.servicehub.ui.theme.BrandOrange
import com.example.servicehub.ui.theme.StatusAccepted
import com.example.servicehub.ui.theme.StatusCompleted
import com.example.servicehub.ui.theme.StatusRejected
import com.example.servicehub.ui.theme.StatusRequested

/**
 * Admin home, built like modern dashboards (Stripe, Linear):
 *  1. "Needs attention": things the admin should act on now
 *  2. Key numbers
 *  3. Trends: bookings per day, earnings per category, bookings by status
 *  4. Top providers and recent activity
 */
@Composable
fun AdminDashboardScreen(
    onOpenPendingProviders: () -> Unit,
    onOpenAllProviders: () -> Unit,
    onOpenUsers: () -> Unit,
    onOpenBookings: () -> Unit,
    onOpenComplaints: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: AdminDashboardViewModel = viewModel(),
) {
    val stats by viewModel.stats.collectAsState()
    val openComplaints by viewModel.openComplaints.collectAsState()
    val sample by viewModel.sampleState.collectAsState()

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Dashboard", subtitle = "ServiceHub admin · ${DateUtils.pretty(DateUtils.today())}") {
            IconButton(onClick = {
                viewModel.logout()
                onLoggedOut()
            }) { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Log out") }
        }

        val s = stats
        if (s == null) {
            LoadingBox()
            return@Column
        }

        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 1. Needs attention
            val attention = s.pendingProviders + openComplaints + s.expiredRequests
            AppCard {
                CardTitle(if (attention == 0) "All caught up" else "Needs attention")
                Caption(if (attention == 0) "Nothing is waiting for you right now." else "Items waiting for an admin decision.")
                if (s.pendingProviders > 0) {
                    ListRow(Icons.Filled.HourglassTop, "${s.pendingProviders} provider(s) waiting for approval",
                        "Review their details and approve or reject", StatusRequested, onClick = onOpenPendingProviders)
                }
                if (openComplaints > 0) {
                    ListRow(Icons.Filled.ReportProblem, "$openComplaints open complaint(s)",
                        "Call both sides and mark as resolved", StatusRejected, onClick = onOpenComplaints)
                }
                if (s.expiredRequests > 0) {
                    ListRow(Icons.Filled.AssignmentLate, "${s.expiredRequests} expired request(s)",
                        "Providers did not answer in time", StatusRequested, onClick = onOpenBookings)
                }
            }

            // 2. Key numbers
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Customers", "${s.customers}", Icons.Filled.Groups, BrandBlue, Modifier.weight(1f), onOpenUsers)
                StatTile("Providers", "${s.approvedProviders}", Icons.Filled.Engineering, BrandOrange, Modifier.weight(1f), onOpenAllProviders)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Bookings", "${s.totalBookings}", Icons.Filled.EventNote, StatusAccepted, Modifier.weight(1f), onOpenBookings)
                StatTile("Completed value", rupees(s.completedValue), Icons.Filled.CurrencyRupee, StatusCompleted, Modifier.weight(1f), onOpenBookings)
            }

            // 3. Trends
            AppCard {
                CardTitle("Bookings this week")
                Caption("${s.bookingsPerDay.sumOf { it.second }} new bookings in the last 7 days")
                MiniBarChart(s.bookingsPerDay, BrandBlue)
            }
            AppCard(onClick = onOpenBookings) {
                CardTitle("Bookings by status")
                Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BookingStatus.entries.forEach { status ->
                        StatusRow({ StatusChip(status) }, s.bookingsByStatus[status] ?: 0)
                    }
                    StatusRow({ StatusChip("Expired", androidx.compose.ui.graphics.Color(0xFF6B7280)) }, s.expiredRequests)
                }
            }
            if (s.revenueByCategory.isNotEmpty()) {
                AppCard {
                    CardTitle("Earnings by category")
                    Caption("Value of completed bookings")
                    HorizontalBars(s.revenueByCategory, StatusCompleted) { rupees(it) }
                }
            }

            // 4. People and activity
            AppCard(onClick = onOpenAllProviders) {
                CardTitle("Top providers")
                if (s.topProviders.isEmpty()) Caption("No ratings or completed jobs yet.")
                s.topProviders.forEachIndexed { index, p ->
                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Avatar(p.name, 36.dp)
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(p.name, style = MaterialTheme.typography.titleSmall)
                            Caption("${FixedData.categoryById(p.categoryId)?.name.orEmpty()} · ${p.jobsCompleted} jobs")
                        }
                        RatingLabel(p.ratingAvg, p.ratingCount)
                    }
                }
            }
            if (s.recent.isNotEmpty()) {
                AppCard(onClick = onOpenBookings) {
                    CardTitle("Recent activity")
                    s.recent.forEachIndexed { index, b ->
                        if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(b.serviceName, style = MaterialTheme.typography.titleSmall)
                                Caption("${b.customerName} → ${b.providerName} · ${DateUtils.dateTime(b.createdAt)}")
                            }
                            BookingStatusChip(b)
                        }
                    }
                }
            }

            // Sample data (setup tool)
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text("Sample data", style = MaterialTheme.typography.titleSmall)
                        Caption(
                            if (sample.alreadyLoaded) "Loaded. Loading again resets sample services and providers."
                            else "Adds services with prices and 2–3 approved providers per category."
                        )
                    }
                }
                if (sample.alreadyLoaded) {
                    OutlinedButton(onClick = viewModel::loadSampleData, enabled = !sample.isLoading, modifier = Modifier.padding(top = 8.dp)) {
                        Text(if (sample.isLoading) "Loading..." else "Reload sample data")
                    }
                } else {
                    Button(onClick = viewModel::loadSampleData, enabled = !sample.isLoading, modifier = Modifier.padding(top = 8.dp)) {
                        Text(if (sample.isLoading) "Loading..." else "Load sample data")
                    }
                }
                sample.message?.let { Caption(it, color = MaterialTheme.colorScheme.primary) }
            }
        }
    }
}

@Composable
private fun StatusRow(chip: @Composable () -> Unit, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { chip() }
        Text("$count", style = MaterialTheme.typography.titleSmall)
    }
}
