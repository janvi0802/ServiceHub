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

    init {
        viewModelScope.launch { user.value = runCatching { UserRepository.getUser(uid) }.getOrNull() }
    }

    companion object {
        /** The soonest open booking (earliest date, then slot order). */
        fun nextUpcoming(bookings: List<Booking>): Booking? =
            bookings.filter { it.isOpen }
                .minWithOrNull(compareBy<Booking> { it.date }.thenBy { FixedData.timeSlots.indexOf(it.timeSlot) })

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
@Composable
fun CustomerHomeScreen(
    onOpenSearch: () -> Unit,
    onOpenCategory: (String) -> Unit,
    onOpenBooking: (String) -> Unit,
    onRate: (String) -> Unit,
    onOpenService: (categoryId: String, serviceId: String) -> Unit,
    onOpenProvider: (providerId: String, serviceId: String) -> Unit,
    viewModel: CustomerHomeViewModel = viewModel(),
) {
    val user by viewModel.user.collectAsState()
    val services by viewModel.services.collectAsState()
    val bookings by viewModel.bookings.collectAsState()
    val allCategories by CategoryStore.all.collectAsState()   // live: admin-added categories appear here
    val categories = allCategories.filter { it.isActive }

    val upcoming = CustomerHomeViewModel.nextUpcoming(bookings)
    val toRate = CustomerHomeViewModel.rateReminder(bookings)
    val fromPrice = CustomerHomeViewModel.minPriceByCategory(services)
    val quickPicks = CustomerHomeViewModel.quickPicks(services, allCategories)
    val bookAgain = CustomerHomeViewModel.bookAgain(bookings)
    val isNewCustomer = bookings.isEmpty()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        fullWidth { HomeHeader(user, onOpenSearch) }
        fullWidth { TrustStrip() }
        // The clear main action: jump down to all categories
        fullWidth {
            Button(
                onClick = { scope.launch { listState.animateScrollToItem(categoriesIndex(upcoming, toRate, isNewCustomer, bookAgain, quickPicks)) } },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(50.dp),
            ) {
                Icon(Icons.Filled.AddCircle, contentDescription = null)
                Text("  Book a service")
            }
        }

        if (upcoming != null) {
            fullWidth { Padded { UpcomingCard(upcoming, onClick = { onOpenBooking(upcoming.id) }) } }
        }
        if (toRate != null) {
            fullWidth { Padded { RateReminderCard(toRate, onClick = { onRate(toRate.id) }) } }
        }
        if (isNewCustomer) {
            fullWidth { Padded { HowItWorks() } }
        }
        if (bookAgain.isNotEmpty()) {
            fullWidth { SectionTitle("Book again") }
            fullWidth {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(bookAgain, key = { it.providerId + it.serviceId }) { item ->
                        BookAgainCard(item, onClick = { onOpenProvider(item.providerId, item.serviceId) })
                    }
                }
            }
        }
        if (quickPicks.isNotEmpty()) {
            fullWidth { SectionTitle("Quick picks") }
            fullWidth {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(quickPicks, key = { it.id }) { service ->
                        QuickPickCard(service, onClick = { onOpenService(service.categoryId, service.id) })
                    }
                }
            }
        }

        fullWidth { SectionTitle("All categories") }
        // Rows of 3 equal tiles (an empty space fills the last row if needed)
        categories.chunked(3).forEach { row ->
            item(key = "row_" + row.joinToString { it.id }) {
                Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { category ->
                        Box(Modifier.weight(1f)) {
                            CategoryTile(category, fromPrice[category.id], onClick = { onOpenCategory(category.id) })
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/**
 * Position of the "All categories" title in the Home list, so "Book a service" can scroll to it.
 * Counts the items above it: header, trust strip, button (3), then each optional section.
 */
private fun categoriesIndex(
    upcoming: Booking?, toRate: Booking?, isNewCustomer: Boolean,
    bookAgain: List<BookAgainItem>, quickPicks: List<Service>,
): Int {
    var index = 3
    if (upcoming != null) index++
    if (toRate != null) index++
    if (isNewCustomer) index++
    if (bookAgain.isNotEmpty()) index += 2   // title + row
    if (quickPicks.isNotEmpty()) index += 2
    return index
}

// ----------------------------------------------------------------- small layout helpers

private fun LazyListScope.fullWidth(content: @Composable () -> Unit) {
    item { content() }
}

@Composable
private fun Padded(content: @Composable () -> Unit) {
    Box(Modifier.padding(horizontal = 16.dp)) { content() }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

// ----------------------------------------------------------------- header + trust

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
        Text("What service do you need today?", color = Color.White.copy(alpha = 0.9f))
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
            Text("  Search services or categories", color = Color.Gray)
        }
    }
}

/** Three reasons to trust ServiceHub, always visible (like the promise row in Urban Company). */
@Composable
private fun TrustStrip() {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TrustItem(Icons.Filled.Verified, "Verified pros", Modifier.weight(1f))
        TrustItem(Icons.Filled.Lock, "Safe start PIN", Modifier.weight(1f))
        TrustItem(Icons.Filled.CurrencyRupee, "Fixed prices", Modifier.weight(1f))
    }
}

@Composable
private fun TrustItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, modifier: Modifier) {
    Row(
        modifier
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.small)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
        Text(" $label", style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

/** 4 simple steps, shown only until the customer has made a first booking. */
@Composable
private fun HowItWorks() {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("How ServiceHub works", fontWeight = FontWeight.Bold)
            listOf(
                "1" to "Choose a service and a verified provider",
                "2" to "Pick a date and time slot",
                "3" to "Share your 4-digit PIN when they arrive",
                "4" to "Pay by Cash or UPI after the job",
            ).forEach { (n, text) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(24.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Text(n, color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) }
                    Text("  $text", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

// ----------------------------------------------------------------- cards

/** "Your next booking" with status and, once accepted, the PIN. */
@Composable
private fun UpcomingCard(booking: Booking, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Your next booking", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                BookingStatusChip(booking)
            }
            Text(booking.serviceName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("with ${booking.providerName}")
            Text("${DateUtils.pretty(booking.date)} • ${booking.timeSlot}", style = MaterialTheme.typography.bodySmall)
            if (booking.bookingStatus == BookingStatus.ACCEPTED) {
                Text("Start PIN: ${booking.startPin}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

/** Reminds the customer to rate the last completed job. */
@Composable
private fun RateReminderCard(booking: Booking, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = BrandOrange.copy(alpha = 0.12f)),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Star, contentDescription = null, tint = BrandOrange, modifier = Modifier.size(32.dp))
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text("How was your ${booking.serviceName}?", fontWeight = FontWeight.Bold)
                Text("Rate ${booking.providerName}. It helps other customers.", style = MaterialTheme.typography.bodySmall)
            }
            Text("Rate", color = BrandOrange, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BookAgainCard(item: BookAgainItem, onClick: () -> Unit) {
    Card(Modifier.width(150.dp).clickable(onClick = onClick)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Avatar(item.providerName, 40.dp)
            Text(item.providerName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(item.serviceName, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Book again", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun QuickPickCard(service: Service, onClick: () -> Unit) {
    val category = FixedData.categoryById(service.categoryId)
    Card(Modifier.width(150.dp).clickable(onClick = onClick)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (category != null) {
                Box(
                    Modifier.size(40.dp).background(category.color.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Icon(category.icon, contentDescription = null, tint = category.color, modifier = Modifier.size(22.dp)) }
            }
            Text(service.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("~${service.durationMins} min", style = MaterialTheme.typography.bodySmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(rupees(service.price), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("Book", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun CategoryTile(category: Category, fromPrice: Int?, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Icon inside a soft circle of the category's own colour
            Box(
                Modifier.size(52.dp).background(category.color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(category.icon, contentDescription = null, tint = category.color, modifier = Modifier.size(30.dp))
            }
            Text(
                category.name,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                maxLines = 2,
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
