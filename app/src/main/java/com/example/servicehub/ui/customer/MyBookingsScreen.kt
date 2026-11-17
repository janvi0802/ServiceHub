package com.example.servicehub.ui.customer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.servicehub.data.model.Booking
import com.example.servicehub.data.DateUtils
import com.example.servicehub.data.FixedData
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.MaterialTheme
import com.example.servicehub.data.repository.AuthRepository
import com.example.servicehub.data.repository.BookingRepository
import com.example.servicehub.ui.components.BookingCard
import com.example.servicehub.ui.components.EmptyBox
import com.example.servicehub.ui.components.LoadingBox
import com.example.servicehub.ui.components.ScreenHeader
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn

class MyBookingsViewModel : ViewModel() {
    /** The customer's bookings, live. null = loading. */
    val bookings: StateFlow<List<Booking>?> =
        BookingRepository.customerBookings(AuthRepository.currentUserId.orEmpty())
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    companion object {
        /**
         * Active bookings grouped for easy scanning: "Today", then "Upcoming" (soonest first),
         * then "Earlier" (accepted jobs from past days that never started). Empty groups are left out.
         */
        fun groupActive(open: List<Booking>, today: String): List<Pair<String, List<Booking>>> {
            val sorted = open.sortedWith(compareBy<Booking> { it.date }.thenBy { FixedData.timeSlots.indexOf(it.timeSlot) })
            return listOf(
                "Today" to sorted.filter { it.date == today },
                "Upcoming" to sorted.filter { it.date > today },
                "Needs attention" to sorted.filter { it.date < today },
            ).filter { it.second.isNotEmpty() }
        }
    }
}

/** Customer's bookings in two tabs: Active (still going) and Past (finished). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyBookingsScreen(
    onBookingClick: (String) -> Unit,
    onExplore: () -> Unit,
    viewModel: MyBookingsViewModel = viewModel(),
) {
    val bookings by viewModel.bookings.collectAsState()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("My bookings", subtitle = "Track and manage your services")
        val all = bookings
        // Expired requests move to "Past" even though their status is still REQUESTED.
        val active = all.orEmpty().filter { it.isOpen }
        val past = all.orEmpty().filter { !it.isOpen }
        PrimaryTabRow(selectedTabIndex = selectedTab, containerColor = MaterialTheme.colorScheme.surface) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Active (${active.size})") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Past (${past.size})") })
        }
        if (all == null) {
            LoadingBox()
            return@Column
        }
        val groups = if (selectedTab == 0) MyBookingsViewModel.groupActive(active, DateUtils.today())
        else listOf("" to past)   // already newest first
        if (groups.all { it.second.isEmpty() }) {
            if (selectedTab == 0) {
                EmptyBox(
                    title = "No active bookings",
                    message = "Book a trusted professional in a few taps.",
                    icon = Icons.Filled.EventAvailable,
                    actionLabel = "Explore services",
                    onAction = onExplore,
                )
            } else {
                EmptyBox(title = "No past bookings", message = "Completed and cancelled bookings will appear here.", icon = Icons.Filled.History)
            }
            return@Column
        }
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            groups.forEach { (title, list) ->
                if (title.isNotEmpty()) {
                    item(key = "group_$title") {
                        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                items(list, key = { it.id }) { booking ->
                    BookingCard(booking, "with ${booking.providerName}", onClick = { onBookingClick(booking.id) })
                }
            }
        }
    }
}
