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
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.servicehub.data.FixedData
import com.example.servicehub.ui.components.BookingStatusChip
import com.example.servicehub.ui.components.openDialer
import com.example.servicehub.ui.components.openMaps
import com.example.servicehub.ui.theme.BrandOrange
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
    val nextJob = ProviderJobsViewModel.nextJob(all)
    val week = ProviderJobsViewModel.earningsLast7Days(all)
    val context = LocalContext.current

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Orange gradient header: name, rating, verified
        item {
            Column(
                Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.verticalGradient(listOf(BrandOrange, Color(0xFFE65100))))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("Hello, ${p.name.substringBefore(" ")} 👋", style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    "${FixedData.categoryById(p.categoryId)?.name.orEmpty()} • ${p.area}",
                    color = Color.White.copy(alpha = 0.9f),
                )
                Text(
                    "✔ Verified   ${ratingText(p.ratingAvg, p.ratingCount)}   ${p.jobsCompleted} jobs",
                    color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        // What to do next
        if (nextJob != null) {
            item {
                Card(
                    Modifier.fillMaxWidth().clickable { onJobClick(nextJob.id) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Next job", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                            BookingStatusChip(nextJob)
                        }
                        Text(nextJob.serviceName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${nextJob.customerName} • ${DateUtils.pretty(nextJob.date)}, ${nextJob.timeSlot}",
                            style = MaterialTheme.typography.bodySmall)
                        Text(nextJob.address, style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { openDialer(context, nextJob.customerPhone) }) { Text("Call") }
                            OutlinedButton(onClick = { openMaps(context, nextJob.address) }) { Text("Open in Maps") }
                        }
                    }
                }
            }
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
        // This week's earnings as 7 simple bars (no chart library needed)
        item { WeekChart(week) }
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

/**
 * A tiny bar chart: one bar per day, its height proportional to that day's earnings.
 * Built from plain Boxes, so it needs no library.
 */
@Composable
private fun WeekChart(days: List<Pair<String, Int>>) {
    val max = days.maxOfOrNull { it.second }?.takeIf { it > 0 } ?: 1
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("This week: ${rupees(days.sumOf { it.second })}", fontWeight = FontWeight.Bold)
            Row(
                Modifier.fillMaxWidth().height(120.dp).padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                days.forEachIndexed { index, (label, amount) ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        if (amount > 0) Text("${amount}", style = MaterialTheme.typography.labelSmall)
                        Box(
                            Modifier
                                .width(18.dp)
                                // at least a thin line, so empty days are still visible
                                .height((70f * amount / max).dp.coerceAtLeast(3.dp))
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (index == days.lastIndex) BrandOrange   // today
                                    else MaterialTheme.colorScheme.primary.copy(alpha = if (amount > 0) 0.8f else 0.2f)
                                )
                        )
                        Text(label, style = MaterialTheme.typography.labelSmall)
                    }
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
