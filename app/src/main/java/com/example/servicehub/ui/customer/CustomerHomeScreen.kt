package com.example.servicehub.ui.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.Button
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.servicehub.data.Category
import com.example.servicehub.data.CategoryStore
import com.example.servicehub.data.DateUtils
import com.example.servicehub.data.FixedData
import com.example.servicehub.data.model.Booking
import com.example.servicehub.data.model.BookingStatus
import com.example.servicehub.data.model.Service
import com.example.servicehub.data.model.User
import com.example.servicehub.data.repository.AuthRepository
import com.example.servicehub.data.repository.BookingRepository
import com.example.servicehub.data.repository.ServiceRepository
import com.example.servicehub.data.repository.UserRepository
import com.example.servicehub.ui.components.Avatar
import com.example.servicehub.ui.components.BookingStatusChip
import com.example.servicehub.ui.components.rupees
import com.example.servicehub.ui.theme.BrandBlue
import com.example.servicehub.ui.theme.BrandBlueDark
import com.example.servicehub.ui.theme.BrandOrange
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import com.example.servicehub.ui.components.ActionItem
import com.example.servicehub.ui.components.ActionList
import com.example.servicehub.ui.components.AppCard
import com.example.servicehub.ui.components.SectionLabel
import com.example.servicehub.ui.components.SoftIcon
import com.example.servicehub.ui.theme.StatusAccepted
import com.example.servicehub.ui.theme.StatusCompleted
import com.example.servicehub.ui.theme.StatusRejected
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Schedule
import kotlinx.coroutines.launch

/** One "Book again" shortcut: a provider + service the customer used before. */
data class BookAgainItem(val providerId: String, val providerName: String, val serviceId: String, val serviceName: String)

class CustomerHomeViewModel : ViewModel() {
    private val uid = AuthRepository.currentUserId.orEmpty()

    /** The customer (for the greeting and the area in the header). */
    val user = MutableStateFlow<User?>(null)

    /** All services (for "from ₹" prices and quick picks). */
    val services: StateFlow<List<Service>> = ServiceRepository.allServices()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** The customer's bookings, live (next booking, rate reminder, book again). */
    val bookings: StateFlow<List<Booking>> = BookingRepository.customerBookings(uid)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Bookings already told "no answer" in this session (so we never send it twice). */
    private val toldNoAnswer = mutableSetOf<String>()

    init {
        viewModelScope.launch { user.value = runCatching { UserRepository.getUser(uid) }.getOrNull() }
        // When the app opens, check for requests the provider has not answered for 1 hour and
        // notify the customer once (without a paid server, this check runs when the app is open).
        viewModelScope.launch {
            bookings.collect { list ->
                list.filter { it.isNoAnswer && !it.noAnswerNotified && toldNoAnswer.add(it.id) }
                    .forEach { runCatching { BookingRepository.notifyNoAnswer(it) } }
            }
        }
    }

    companion object {
        /** The soonest open booking (earliest date, then slot order). */
        fun nextUpcoming(bookings: List<Booking>): Booking? =
            bookings.filter { it.isOpen }
                .minWithOrNull(compareBy<Booking> { it.date }.thenBy { FixedData.timeSlots.indexOf(it.timeSlot) })

        /** Job done but the customer has not paid yet. */
        fun needsPayment(b: Booking) =
            b.bookingStatus == BookingStatus.COMPLETED && b.paymentStatus != com.example.servicehub.data.model.PaymentStatus.PAID

        /** The oldest unpaid finished booking (shown as "Payment due" at the top of Home). */
        fun paymentDue(bookings: List<Booking>): Booking? =
            bookings.filter(::needsPayment).minByOrNull { it.completedAt }

        /** How long the "Rate your service" card keeps reminding: 7 days after completion. */
        private const val RATE_REMINDER_MILLIS = 7L * 24 * 60 * 60 * 1000

        /**
         * The most recent completed booking that is not rated yet, if it finished in the last
         * 7 days (so the reminder doesn't stay forever if the customer prefers not to rate).
         */
        fun rateReminder(bookings: List<Booking>, now: Long = System.currentTimeMillis()): Booking? =
            bookings.filter {
                it.bookingStatus == BookingStatus.COMPLETED && !it.isRated &&
                    now - it.completedAt <= RATE_REMINDER_MILLIS
            }.maxByOrNull { it.completedAt }

        /** Cheapest visible price per category id, for "from ₹149". */
        fun minPriceByCategory(services: List<Service>): Map<String, Int> =
            services.filter { it.isActive }.groupBy { it.categoryId }.mapValues { (_, list) -> list.minOf { it.price } }

        /** "Quick picks": the cheapest visible service of each visible category, in category order. */
        fun quickPicks(services: List<Service>, categories: List<Category>): List<Service> =
            categories.filter { it.isActive }.mapNotNull { c ->
                services.filter { it.isActive && it.categoryId == c.id }.minByOrNull { it.price }
            }

        /** Up to 5 different provider + service pairs from completed bookings, newest first. */
        fun bookAgain(bookings: List<Booking>): List<BookAgainItem> =
            bookings.filter { it.bookingStatus == BookingStatus.COMPLETED }
                .sortedByDescending { it.completedAt }
                .map { BookAgainItem(it.providerId, it.providerName, it.serviceId, it.serviceName) }
                .distinctBy { it.providerId to it.serviceId }
                .take(5)
    }
}

/** Customer home: header, trust, next booking, reminders, shortcuts and categories. */

/**
 * Customer home, in clear sections (top = most urgent):
 *  1. Header with search
 *  2. Needs your attention (pay, rate, provider did not answer) — only if something is waiting
 *  3. Your next booking (with the start PIN)
 *  4. Book a service (main button)
 *  5. Categories (the main menu)
 *  6. Book again / Popular services as simple lists (nothing cut off)
 */
@Composable
fun CustomerHomeScreen(
    onOpenSearch: () -> Unit,
    onOpenCategory: (String) -> Unit,
    onOpenBooking: (String) -> Unit,
    onRate: (String) -> Unit,
    onOpenService: (categoryId: String, serviceId: String) -> Unit,
    onOpenProvider: (providerId: String, serviceId: String) -> Unit,
    onBookService: () -> Unit,
    viewModel: CustomerHomeViewModel = viewModel(),
) {
    val user by viewModel.user.collectAsState()
    val services by viewModel.services.collectAsState()
    val bookings by viewModel.bookings.collectAsState()
    val allCategories by CategoryStore.all.collectAsState()   // live: admin-added categories appear here
    val categories = allCategories.filter { it.isActive }

    val upcoming = CustomerHomeViewModel.nextUpcoming(bookings)
    val fromPrice = CustomerHomeViewModel.minPriceByCategory(services)
    val popular = CustomerHomeViewModel.quickPicks(services, allCategories).take(4)
    val bookAgain = CustomerHomeViewModel.bookAgain(bookings).take(3)
    val attention = attentionItems(bookings, onOpenBooking, onRate)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        fullWidth { HomeHeader(user, onOpenSearch) }

        if (attention.isNotEmpty()) {
            fullWidth { Padded { SectionLabel("Needs your attention") } }
            fullWidth { Padded { ActionList(attention) } }
        }

        if (upcoming != null) {
            fullWidth { Padded { SectionLabel("Your next booking") } }
            fullWidth { Padded { UpcomingCard(upcoming, onClick = { onOpenBooking(upcoming.id) }) } }
        }

        // The clear main action: a short guided form (category -> service -> professional)
        fullWidth {
            Button(
                onClick = onBookService,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).height(52.dp),
            ) {
                Icon(Icons.Filled.AddCircle, contentDescription = null)
                Text("  Book a service")
            }
        }

        fullWidth { Padded { SectionLabel("Categories") } }
        // Rows of 3 equal tiles (an empty space fills the last row if needed)
        categories.chunked(3).forEach { row ->
            item(key = "row_" + row.joinToString { it.id }) {
                Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { category ->
                        Box(Modifier.weight(1f)) {
                            CategoryTile(category, fromPrice[category.id], onClick = { onOpenCategory(category.id) })
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }

        if (bookAgain.isNotEmpty()) {
            fullWidth { Padded { SectionLabel("Book again") } }
            fullWidth {
                Padded {
                    ActionList(bookAgain.map { item ->
                        ActionItem(
                            icon = Icons.Filled.Replay, color = StatusAccepted,
                            title = item.serviceName, subtitle = "with ${item.providerName}",
                            action = "Book", onClick = { onOpenProvider(item.providerId, item.serviceId) },
                        )
                    })
                }
            }
        }

        if (popular.isNotEmpty()) {
            fullWidth { Padded { SectionLabel("Popular services") } }
            fullWidth {
                Padded {
                    ActionList(popular.map { service ->
                        val category = allCategories.firstOrNull { it.id == service.categoryId }
                        ActionItem(
                            icon = category?.icon ?: Icons.Filled.Search,
                            color = category?.color ?: StatusAccepted,
                            title = service.name,
                            subtitle = "${rupees(service.price)} · about ${service.durationMins} min",
                            action = "View", onClick = { onOpenService(service.categoryId, service.id) },
                        )
                    })
                }
            }
        }

        if (bookings.isEmpty()) {
            fullWidth { Padded { SectionLabel("How it works") } }
            fullWidth { Padded { HowItWorks() } }
        }
    }
}

/**
 * Everything waiting for the customer, most urgent first:
 * money owed, a provider who did not answer in time, then a rating reminder.
 */
private fun attentionItems(
    bookings: List<Booking>,
    onOpenBooking: (String) -> Unit,
    onRate: (String) -> Unit,
): List<ActionItem> = buildList {
    bookings.filter(CustomerHomeViewModel::needsPayment).forEach { b ->
        add(
            ActionItem(
                Icons.Filled.CurrencyRupee, BrandOrange,
                "Pay ${rupees(b.totalAmount)}",
                "${b.serviceName} · " + if (b.paymentMode == "UPI") "UPI in the app" else "cash to ${b.providerName}",
                if (b.paymentMode == "UPI") "Pay" else "View",
            ) { onOpenBooking(b.id) }
        )
    }
    bookings.filter { it.isNoAnswer }.forEach { b ->
        add(
            ActionItem(
                Icons.Filled.Schedule, StatusRejected,
                "No answer from ${b.providerName}",
                "${b.serviceName} · choose another provider or time",
                "Open",
            ) { onOpenBooking(b.id) }
        )
    }
    val toRate = CustomerHomeViewModel.rateReminder(bookings)
    if (toRate != null && !CustomerHomeViewModel.needsPayment(toRate)) {
        add(
            ActionItem(
                Icons.Filled.Star, StatusCompleted,
                "Rate your ${toRate.serviceName}",
                "How was ${toRate.providerName}?",
                "Rate",
            ) { onRate(toRate.id) }
        )
    }
}

// ----------------------------------------------------------------- small layout helpers

private fun LazyListScope.fullWidth(content: @Composable () -> Unit) {
    item { content() }
}

@Composable
private fun Padded(content: @Composable () -> Unit) {
    Box(Modifier.padding(horizontal = 16.dp)) { content() }
}

// ----------------------------------------------------------------- header

/** Blue gradient header: greeting, the customer's area, and the search box. */
@Composable
private fun HomeHeader(user: User?, onOpenSearch: () -> Unit) {
    val firstName = user?.name?.substringBefore(" ").orEmpty()
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            .background(Brush.verticalGradient(listOf(BrandBlue, BrandBlueDark)))
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            if (firstName.isBlank()) "Welcome" else "Hi, $firstName",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        if (!user?.area.isNullOrBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Text(" ${user?.area}", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyMedium)
            }
        }
        // Search box (tapping opens the Search screen)
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .clickable(onClick = onOpenSearch)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = Color.Gray)
            Text("  Search services, e.g. tap repair", color = Color.Gray)
        }
    }
}

/** 4 simple steps, shown only until the customer has made a first booking. */
@Composable
private fun HowItWorks() {
    AppCard {
        listOf(
            "Choose a service and a verified professional",
            "Pick a date and time slot",
            "Share your 4-digit PIN when they arrive",
            "Pay by Cash or UPI after the job",
        ).forEachIndexed { i, text ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                Box(
                    Modifier.size(22.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("${i + 1}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                }
                Text("  $text", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

// ----------------------------------------------------------------- cards

/** "Your next booking": service, provider, date and, once accepted, the PIN in its own box. */
@Composable
private fun UpcomingCard(booking: Booking, onClick: () -> Unit) {
    AppCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SoftIcon(
                allCategoryIcon(booking.categoryId), StatusAccepted,
            )
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(booking.serviceName, style = MaterialTheme.typography.titleMedium)
                Text("with ${booking.providerName}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            BookingStatusChip(booking)
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
            Icon(Icons.Filled.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp))
            Text("  ${DateUtils.pretty(booking.date)} · ${booking.timeSlot}", style = MaterialTheme.typography.bodyMedium)
        }
        if (booking.bookingStatus == BookingStatus.ACCEPTED) {
            Row(
                Modifier
                    .padding(top = 10.dp)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Text("  Start PIN", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text(booking.startPin, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

/** The category's icon (built-in or admin-added); a plain icon if it was removed. */
private fun allCategoryIcon(categoryId: String) =
    CategoryStore.all.value.firstOrNull { it.id == categoryId }?.icon ?: Icons.Filled.Verified

/** One category: a small soft icon, the name, and the lowest price. */
@Composable
private fun CategoryTile(category: Category, fromPrice: Int?, onClick: () -> Unit) {
    AppCard(onClick = onClick) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            SoftIcon(category.icon, category.color, size = 40.dp, iconSize = 22.dp)
            Text(
                category.name,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                if (fromPrice != null) "from ${rupees(fromPrice)}" else "coming soon",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
