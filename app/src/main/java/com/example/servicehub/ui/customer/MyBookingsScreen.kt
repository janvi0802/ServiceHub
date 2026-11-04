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
}

/** Customer's bookings in two tabs: Active (still going) and Past (finished). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyBookingsScreen(
    onBookingClick: (String) -> Unit,
    viewModel: MyBookingsViewModel = viewModel(),
) {
    val bookings by viewModel.bookings.collectAsState()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("My Bookings")
        PrimaryTabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Active") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Past") })
        }
        val all = bookings
        if (all == null) {
            LoadingBox()
            return@Column
        }
        val shown = all.filter { if (selectedTab == 0) it.bookingStatus.isActive else !it.bookingStatus.isActive }
        if (shown.isEmpty()) {
            EmptyBox(if (selectedTab == 0) "No active bookings.\nBook a service from Home!" else "No past bookings yet.")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(shown, key = { it.id }) { booking ->
                    BookingCard(booking, "with ${booking.providerName}", onClick = { onBookingClick(booking.id) })
                }
            }
        }
    }
}
