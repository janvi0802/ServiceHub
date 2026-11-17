package com.example.servicehub.ui.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.servicehub.data.Category
import com.example.servicehub.data.DateUtils
import com.example.servicehub.data.FixedData
import com.example.servicehub.data.model.Booking
import com.example.servicehub.data.model.BookingStatus
import com.example.servicehub.data.model.Service
import com.example.servicehub.data.repository.AuthRepository
import com.example.servicehub.data.repository.BookingRepository
import com.example.servicehub.data.repository.ServiceRepository
import com.example.servicehub.data.repository.UserRepository
import com.example.servicehub.ui.components.BookingStatusChip
import com.example.servicehub.ui.components.rupees
import com.example.servicehub.ui.theme.BrandOrange
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CustomerHomeViewModel : ViewModel() {
    private val uid = AuthRepository.currentUserId.orEmpty()

    val firstName = MutableStateFlow("")

    /** All services (for the "from ₹" prices on the tiles). */
    val services: StateFlow<List<Service>> = ServiceRepository.allServices()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** The customer's bookings, live (for the upcoming card and the rate reminder). */
    val bookings: StateFlow<List<Booking>> = BookingRepository.customerBookings(uid)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            firstName.value = runCatching { UserRepository.getUser(uid)?.name?.substringBefore(" ") }.getOrNull().orEmpty()
        }
    }

    companion object {
        /** The soonest open booking (earliest date, then slot order). */
        fun nextUpcoming(bookings: List<Booking>): Booking? =
            bookings.filter { it.isOpen }
                .minWithOrNull(compareBy<Booking> { it.date }.thenBy { FixedData.timeSlots.indexOf(it.timeSlot) })

        /** The most recent completed booking that has not been rated yet. */
        fun rateReminder(bookings: List<Booking>): Booking? =
            bookings.filter { it.bookingStatus == BookingStatus.COMPLETED && !it.isRated }
                .maxByOrNull { it.completedAt }

        /** Cheapest visible price per category id, for "from ₹149". */
        fun minPriceByCategory(services: List<Service>): Map<String, Int> =
            services.filter { it.isActive }.groupBy { it.categoryId }.mapValues { (_, list) -> list.minOf { it.price } }
    }
}

/** Customer home: greeting, search, next booking, rate reminder and the 6 category tiles. */
@Composable
fun CustomerHomeScreen(
    onOpenSearch: () -> Unit,
    onOpenCategory: (String) -> Unit,
    onOpenBooking: (String) -> Unit,
    onRate: (String) -> Unit,
    viewModel: CustomerHomeViewModel = viewModel(),
) {
    val firstName by viewModel.firstName.collectAsState()
    val services by viewModel.services.collectAsState()
    val bookings by viewModel.bookings.collectAsState()

    val upcoming = CustomerHomeViewModel.nextUpcoming(bookings)
    val toRate = CustomerHomeViewModel.rateReminder(bookings)
    val fromPrice = CustomerHomeViewModel.minPriceByCategory(services)

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Full-width items span all 3 columns
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.padding(top = 20.dp)) {
                Text(
                    if (firstName.isBlank()) "Hello 👋" else "Hello, $firstName 👋",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text("What service do you need today?", style = MaterialTheme.typography.bodyMedium)
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) { SearchBarButton(onOpenSearch) }

        if (upcoming != null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                UpcomingCard(upcoming, onClick = { onOpenBooking(upcoming.id) })
            }
        }
        if (toRate != null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                RateReminderCard(toRate, onClick = { onRate(toRate.id) })
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Text("Categories", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        items(FixedData.categories) { category ->
            CategoryTile(category, fromPrice[category.id], onClick = { onOpenCategory(category.id) })
        }
    }
}

/** Looks like a search box; tapping it opens the Search screen. */
@Composable
private fun SearchBarButton(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Search, contentDescription = null)
            Text("  Search services or categories", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

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
private fun CategoryTile(category: Category, fromPrice: Int?, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
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
                modifier = Modifier.padding(top = 8.dp),
            )
            if (fromPrice != null) {
                Text("from ${rupees(fromPrice)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
