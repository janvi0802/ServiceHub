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
import com.example.servicehub.ui.theme.StatusAccepted
import com.example.servicehub.ui.components.InfoBanner
import com.example.servicehub.ui.components.SectionHeader
import com.example.servicehub.ui.components.StatTile
import com.example.servicehub.ui.components.StarRow
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
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
                Text("Hi, ${p.name.substringBefore(" ")}", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                Text(
                    "${FixedData.categoryById(p.categoryId)?.name.orEmpty()} · ${p.area}",
                    color = Color.White.copy(alpha = 0.9f),
                )
                Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    HeaderFact(Icons.Filled.Verified, "Verified")
                    HeaderFact(Icons.Filled.Star, ratingText(p.ratingAvg, p.ratingCount))
                    HeaderFact(Icons.Filled.TaskAlt, "${p.jobsCompleted} jobs")
                }
            }
        }
        // Today is a day off: say so clearly, with a one-tap "work today anyway"
        val todayDate = DateUtils.today()
        val weeklyOffToday = DateUtils.weekdayOf(todayDate) in p.offDays
        if (weeklyOffToday) {
            item {
                val workingAnyway = todayDate in p.extraWorkDates
                InfoBanner(
                    icon = Icons.Filled.EventBusy,
                    color = if (workingAnyway) StatusCompleted else StatusRequested,
                    text = if (workingAnyway) "Today is normally your day off, but you are open for bookings today."
                    else "Today is your weekly day off. Customers cannot book you today.",
                    action = {
                        TextButton(onClick = { viewModel.setWorkToday(!workingAnyway) }) {
                            Text(if (workingAnyway) "Undo" else "Work today")
                        }
                    },
                )
            }
        }
        // New requests waiting for an answer
        if (requestCount > 0) {
            item {
                InfoBanner(
                    icon = Icons.Filled.NotificationsActive,
                    color = StatusAccepted,
                    text = "$requestCount new request${if (requestCount > 1) "s" else ""} waiting for your answer",
                    action = { TextButton(onClick = onOpenRequests) { Text("Review") } },
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
        // Earnings: 2 x 2 tiles with icons
        item { SectionHeader("Earnings") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Today", rupees(ProviderJobsViewModel.todaysEarnings(all)), Icons.Filled.Today, StatusCompleted, Modifier.weight(1f))
                StatTile("Total", rupees(p.totalEarnings), Icons.Filled.AccountBalanceWallet, BrandOrange, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Jobs done", p.jobsCompleted.toString(), Icons.Filled.TaskAlt, StatusAccepted, Modifier.weight(1f))
                StatTile("Rating", ratingText(p.ratingAvg, p.ratingCount), Icons.Filled.Star, Color(0xFFF5A623), Modifier.weight(1f))
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
                    Row {
                        Text(review.customerName, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        StarRow(review.rating)
                    }
                    if (review.comment.isNotBlank()) Text(review.comment, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun HeaderFact(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        Text(" $text", color = Color.White, style = MaterialTheme.typography.labelLarge)
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
        val revisits = ProviderJobsViewModel.revisits(all)
        if (requests.isEmpty() && upcoming.isEmpty() && revisits.isEmpty()) {
            EmptyBox(
                title = "No jobs yet",
                message = "New requests from customers will appear here. Stay available to get more bookings.",
                icon = Icons.Filled.WorkOutline,
            )
            return@Column
        }
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (revisits.isNotEmpty()) {
                jobSection("Free revisits (${revisits.size})", revisits, "", onJobClick)
            }
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
            EmptyBox(title = "No history yet", message = "Completed, rejected and cancelled jobs will appear here.", icon = Icons.Filled.History)
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
