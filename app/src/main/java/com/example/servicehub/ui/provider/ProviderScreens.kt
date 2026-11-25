package com.example.servicehub.ui.provider

import com.example.servicehub.ui.components.ActionItem
import com.example.servicehub.ui.components.ActionList
import com.example.servicehub.ui.components.AppCard
import com.example.servicehub.ui.components.Caption
import com.example.servicehub.ui.components.SectionLabel
import com.example.servicehub.ui.theme.StatusRejected
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

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

/**
 * Provider home, in clear sections (top = most urgent):
 *  1. Small header (name, category, area, rating, jobs)
 *  2. Needs your attention: new requests, payments to collect, redo requests
 *  3. Next job (call / maps)
 *  4. Today's jobs
 *  5. Earnings: three equal tiles + this week's bars (same money rule everywhere)
 *  6. Availability: on/off switch and one line for days off (changed in a popup)
 *  7. Recent reviews
 */
@OptIn(ExperimentalLayoutApi::class)
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
    var showDaysOff by rememberSaveable { mutableStateOf(false) }

    val p = provider
    val all = bookings
    if (p == null || all == null) {
        LoadingBox()
        return
    }
    val today = ProviderJobsViewModel.todaysJobs(all)
    val requests = ProviderJobsViewModel.newRequests(all)
    val toCollect = ProviderJobsViewModel.paymentsToCollect(all)
    val redos = ProviderJobsViewModel.revisits(all)
    val nextJob = ProviderJobsViewModel.nextJob(all)
    val week = ProviderJobsViewModel.earningsLast7Days(all)
    val context = LocalContext.current

    // One row per thing waiting for the provider
    val attention = buildList {
        if (requests.isNotEmpty()) add(
            ActionItem(Icons.Filled.NotificationsActive, StatusAccepted,
                "${requests.size} new request${if (requests.size > 1) "s" else ""}", "Accept or reject", "Review", onOpenRequests)
        )
        if (toCollect.isNotEmpty()) add(
            ActionItem(Icons.Filled.AccountBalanceWallet, BrandOrange,
                "${rupees(toCollect.sumOf { it.totalAmount })} to collect",
                "${toCollect.size} finished job${if (toCollect.size > 1) "s" else ""} not paid yet", "View", onOpenRequests)
        )
        if (redos.isNotEmpty()) add(
            ActionItem(Icons.Filled.Replay, StatusRejected,
                "${redos.size} redo request${if (redos.size > 1) "s" else ""}", "Customer not happy: fix it free", "Open", onOpenRequests)
        )
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // 1. Header
        item {
            Column(
                Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.verticalGradient(listOf(BrandOrange, Color(0xFFE65100))))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("Hi, ${p.name.substringBefore(" ")}", style = MaterialTheme.typography.titleLarge, color = Color.White)
                Text(
                    "${FixedData.categoryById(p.categoryId)?.name.orEmpty()} · ${p.area}",
                    color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyMedium,
                )
                Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    HeaderFact(Icons.Filled.Verified, "Verified")
                    HeaderFact(Icons.Filled.Star, ratingText(p.ratingAvg, p.ratingCount))
                    HeaderFact(Icons.Filled.TaskAlt, "${p.jobsCompleted} job${if (p.jobsCompleted == 1) "" else "s"}")
                }
            }
        }
        // Today is a day off: say so clearly, with a one-tap "work today anyway"
        val todayDate = DateUtils.today()
        if (DateUtils.weekdayOf(todayDate) in p.offDays) {
            item {
                val workingAnyway = todayDate in p.extraWorkDates
                InfoBanner(
                    icon = Icons.Filled.EventBusy,
                    color = if (workingAnyway) StatusCompleted else StatusRequested,
                    text = if (workingAnyway) "Normally your day off, but you are open for bookings today."
                    else "Today is your day off. Customers cannot book you today.",
                    action = {
                        TextButton(onClick = { viewModel.setWorkToday(!workingAnyway) }) {
                            Text(if (workingAnyway) "Undo" else "Work today")
                        }
                    },
                )
            }
        }

        // 2. Needs your attention
        if (attention.isNotEmpty()) {
            item { SectionLabel("Needs your attention") }
            item { ActionList(attention) }
        }

        // 3. Next job
        if (nextJob != null) {
            item { SectionLabel("Next job") }
            item {
                AppCard(onClick = { onJobClick(nextJob.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(nextJob.serviceName, style = MaterialTheme.typography.titleMedium)
                            Text(nextJob.customerName, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        BookingStatusChip(nextJob)
                    }
                    Text(
                        "${DateUtils.pretty(nextJob.date)} · ${nextJob.timeSlot}",
                        style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp),
                    )
                    Text(nextJob.address, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        OutlinedButton(onClick = { openDialer(context, nextJob.customerPhone) }, modifier = Modifier.weight(1f)) { Text("Call") }
                        OutlinedButton(onClick = { openMaps(context, nextJob.address) }, modifier = Modifier.weight(1f)) { Text("Maps") }
                    }
                }
            }
        }

        // 4. Today's jobs
        item { SectionLabel("Today's jobs (${today.size})") }
        if (today.isEmpty()) {
            item { Caption("No jobs scheduled for today.") }
        }
        items(today, key = { "today_${it.id}" }) { booking ->
            BookingCard(booking, "Customer: ${booking.customerName}", onClick = { onJobClick(booking.id) })
        }

        // 5. Earnings: Today and This week = money you keep (received); To collect = what customers still owe
        item { SectionLabel("Earnings") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MoneyTile("Today", rupees(ProviderJobsViewModel.todaysEarnings(all)), StatusCompleted, Modifier.weight(1f))
                MoneyTile("This week", rupees(week.sumOf { it.second }), StatusAccepted, Modifier.weight(1f))
                MoneyTile("To collect", rupees(toCollect.sumOf { it.totalAmount }), BrandOrange, Modifier.weight(1f))
            }
        }
        item { WeekChart(week) }
        item { Caption("Today and This week show what you keep from paid jobs (bill minus the ₹${FixedData.VISIT_FEE} visit fee).") }

        // 6. Availability
        item { SectionLabel("Availability") }
        item {
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (p.isAvailable) "Available" else "Not available", style = MaterialTheme.typography.titleSmall,
                            color = if (p.isAvailable) StatusCompleted else StatusRejected)
                        Caption(if (p.isAvailable) "Customers can find and book you." else "You are hidden from customers.")
                    }
                    Switch(checked = p.isAvailable, onCheckedChange = viewModel::setAvailable)
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Weekly days off", style = MaterialTheme.typography.titleSmall)
                        Caption(if (p.offDays.isEmpty()) "None – you work every day" else DateUtils.weekdayNames(p.offDays))
                    }
                    TextButton(onClick = { showDaysOff = true }) { Text("Change") }
                }
            }
            message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }

        // 7. Reviews
        item { SectionLabel("Recent reviews (${reviews.size})") }
        if (reviews.isEmpty()) {
            item { Caption("No reviews yet. Complete jobs to get rated.") }
        }
        items(reviews.take(3), key = { "review_${it.id}" }) { review ->
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(review.customerName, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    StarRow(review.rating)
                }
                if (review.comment.isNotBlank()) Text(review.comment, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    if (showDaysOff) {
        AlertDialog(
            onDismissRequest = { showDaysOff = false },
            title = { Text("Weekly days off") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Caption("Customers cannot book you on these days. Jobs already booked stay as they are.")
                    // Monday first (Calendar numbers 2..7, then 1 = Sunday). Saved at once.
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(2, 3, 4, 5, 6, 7, 1).forEach { day ->
                            FilterChip(
                                selected = day in p.offDays,
                                onClick = { viewModel.toggleOffDay(day) },
                                label = { Text(DateUtils.weekdayShortNames[day - 1]) },
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showDaysOff = false }) { Text("Done") } },
        )
    }
}

/** One of the three equal earnings tiles: amount on top, small label under it. */
@Composable
private fun MoneyTile(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    AppCard(modifier) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = color, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        val toCollect = ProviderJobsViewModel.paymentsToCollect(all)
        if (requests.isEmpty() && upcoming.isEmpty() && revisits.isEmpty() && toCollect.isEmpty()) {
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
            // Everything waiting for the provider, most urgent first
            jobSection("New requests (${requests.size})", requests, "No new requests.", onJobClick)
            if (revisits.isNotEmpty()) {
                jobSection("Job redo requests (${revisits.size})", revisits, "", onJobClick)
            }
            if (toCollect.isNotEmpty()) {
                jobSection("Payments to collect (${toCollect.size})", toCollect, "", onJobClick)
            }
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
    // The same job can be in two sections (e.g. a redo AND unpaid), so the key includes the section
    items(list, key = { "${title.substringBefore(" (")}_${it.id}" }) { booking ->
        BookingCard(booking, "Customer: ${booking.customerName}", onClick = { onJobClick(booking.id) })
    }
}