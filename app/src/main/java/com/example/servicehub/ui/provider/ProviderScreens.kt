package com.example.servicehub.ui.provider

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Switch
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import com.example.servicehub.data.DateUtils
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.servicehub.data.model.Booking
import com.example.servicehub.ui.components.BookingCard
import com.example.servicehub.ui.components.EmptyBox
import com.example.servicehub.ui.components.LoadingBox
import com.example.servicehub.ui.components.ScreenHeader
import com.example.servicehub.ui.components.ratingText
import com.example.servicehub.ui.components.rupees
import com.example.servicehub.ui.theme.StatusCompleted
import com.example.servicehub.ui.theme.StatusRejected
import com.example.servicehub.ui.theme.StatusRequested

// ===================================================================== Provider home

/** Availability switch, earnings and today's jobs. */
@Composable
fun ProviderHomeScreen(
    onJobClick: (String) -> Unit,
    onOpenRequests: () -> Unit,
    viewModel: ProviderJobsViewModel = viewModel(),
) {
    val provider by viewModel.provider.collectAsState()
    val bookings by viewModel.bookings.collectAsState()
    val message by viewModel.message.collectAsState()
    val reviews by viewModel.reviews.collectAsState()

    val p = provider
    val all = bookings
    if (p == null || all == null) {
        LoadingBox()
        return
    }
    val today = ProviderJobsViewModel.todaysJobs(all)
    val requestCount = ProviderJobsViewModel.newRequests(all).size

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Hello, ${p.name.substringBefore(" ")} 👋", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        // Availability switch
        item {
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (p.isAvailable) "Available" else "Not available", fontWeight = FontWeight.Bold,
                            color = if (p.isAvailable) StatusCompleted else StatusRejected)
                        Text(
                            if (p.isAvailable) "Customers can find and book you." else "You are hidden from customers.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Switch(checked = p.isAvailable, onCheckedChange = viewModel::setAvailable)
                }
            }
            message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
        // Weekly days off
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Weekly days off", fontWeight = FontWeight.Bold)
                    Text(
                        "Customers cannot book you on these days. Jobs already booked stay as they are. " +
                            "Want to work on a day off this week? Just untick it.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    // Monday first, as people usually think of a week (Calendar numbers: 2..7, then 1 = Sunday)
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        listOf(2, 3, 4, 5, 6, 7, 1).forEach { day ->
                            FilterChip(
                                selected = day in p.offDays,
                                onClick = { viewModel.toggleOffDay(day) },
                                label = { Text(DateUtils.weekdayShortNames[day - 1]) },
                            )
                        }
                    }
                }
            }
        }
        // New requests banner
        if (requestCount > 0) {
            item {
                Card(
                    Modifier.fillMaxWidth().clickable(onClick = onOpenRequests),
                    colors = CardDefaults.cardColors(containerColor = StatusRequested.copy(alpha = 0.15f)),
                ) {
                    Text(
                        "🔔 $requestCount new request${if (requestCount > 1) "s" else ""} — tap to review",
                        Modifier.padding(16.dp), fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        // Earnings: 2 x 2 small cards
        item {
            Text("Earnings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Today", rupees(ProviderJobsViewModel.todaysEarnings(all)), Modifier.weight(1f))
                StatCard("Total", rupees(p.totalEarnings), Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Jobs done", p.jobsCompleted.toString(), Modifier.weight(1f))
                StatCard("Rating", ratingText(p.ratingAvg, p.ratingCount), Modifier.weight(1f))
            }
        }
        // Today's jobs
        item {
            Text("Today's jobs (${today.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        if (today.isEmpty()) {
            item { Text("No jobs scheduled for today.", style = MaterialTheme.typography.bodyMedium) }
        }
        items(today, key = { it.id }) { booking ->
            BookingCard(booking, "Customer: ${booking.customerName}", onClick = { onJobClick(booking.id) })
        }
        // What customers said (latest 3)
        item {
            Text("Recent reviews (${reviews.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        if (reviews.isEmpty()) {
            item { Text("No reviews yet. Complete jobs to get rated.", style = MaterialTheme.typography.bodyMedium) }
        }
        items(reviews.take(3), key = { "review_${it.id}" }) { review ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    val stars = review.rating.coerceIn(0, 5)
                    Row {
                        Text(review.customerName, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("★".repeat(stars) + "☆".repeat(5 - stars), color = MaterialTheme.colorScheme.secondary)
                    }
                    if (review.comment.isNotBlank()) Text(review.comment, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
        }
    }
}

// ===================================================================== Requests

/** New requests to accept/reject, and upcoming accepted jobs. */
@Composable
fun ProviderRequestsScreen(
    onJobClick: (String) -> Unit,
    viewModel: ProviderJobsViewModel = viewModel(),
) {
    val bookings by viewModel.bookings.collectAsState()
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Jobs")
        val all = bookings
        if (all == null) { LoadingBox(); return@Column }
        val requests = ProviderJobsViewModel.newRequests(all)
        val upcoming = ProviderJobsViewModel.upcoming(all)
        if (requests.isEmpty() && upcoming.isEmpty()) {
            EmptyBox("No requests or upcoming jobs yet.")
            return@Column
        }
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            jobSection("New requests (${requests.size})", requests, "No new requests.", onJobClick)
            jobSection("Upcoming (${upcoming.size})", upcoming, "No upcoming jobs.", onJobClick)
        }
    }
}

// ===================================================================== History

/** Finished jobs: completed, rejected, cancelled. */
@Composable
fun JobHistoryScreen(
    onJobClick: (String) -> Unit,
    viewModel: ProviderJobsViewModel = viewModel(),
) {
    val bookings by viewModel.bookings.collectAsState()
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Job history")
        val all = bookings
        if (all == null) { LoadingBox(); return@Column }
        val history = ProviderJobsViewModel.history(all)
        if (history.isEmpty()) {
            EmptyBox("No finished jobs yet.")
            return@Column
        }
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(history, key = { it.id }) { booking ->
                BookingCard(booking, "Customer: ${booking.customerName}", onClick = { onJobClick(booking.id) })
            }
        }
    }
}

/** Adds a title and a list of booking cards to a LazyColumn. */
private fun androidx.compose.foundation.lazy.LazyListScope.jobSection(
    title: String,
    list: List<Booking>,
    emptyText: String,
    onJobClick: (String) -> Unit,
) {
    item(key = "title_$title") {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
    if (list.isEmpty()) {
        item(key = "empty_$title") { Text(emptyText, style = MaterialTheme.typography.bodyMedium) }
    }
    items(list, key = { it.id }) { booking ->
        BookingCard(booking, "Customer: ${booking.customerName}", onClick = { onJobClick(booking.id) })
    }
}
