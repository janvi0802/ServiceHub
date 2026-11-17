package com.example.servicehub.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.servicehub.data.model.ApprovalStatus
import com.example.servicehub.data.model.Booking
import com.example.servicehub.data.model.BookingStatus
import com.example.servicehub.data.model.Provider
import com.example.servicehub.data.model.Roles
import com.example.servicehub.data.model.User
import com.example.servicehub.data.repository.AdminRepository
import com.example.servicehub.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The numbers on the admin dashboard. */
data class DashboardStats(
    val customers: Int = 0,
    val approvedProviders: Int = 0,
    val pendingProviders: Int = 0,
    val blockedUsers: Int = 0,
    val bookingsByStatus: Map<BookingStatus, Int> = emptyMap(),
    val totalBookings: Int = 0,
    val completedValue: Int = 0,
) {
    companion object {
        /** Counts everything from the three live lists. Pure function = easy to test. */
        fun from(users: List<User>, providers: List<Provider>, bookings: List<Booking>) = DashboardStats(
            customers = users.count { it.role == Roles.CUSTOMER },
            approvedProviders = providers.count { it.approvalStatus == ApprovalStatus.APPROVED },
            pendingProviders = providers.count { it.approvalStatus == ApprovalStatus.PENDING },
            blockedUsers = users.count { it.isBlocked },
            bookingsByStatus = BookingStatus.entries.associateWith { status ->
                bookings.count { it.bookingStatus == status }
            },
            totalBookings = bookings.size,
            completedValue = bookings.filter { it.bookingStatus == BookingStatus.COMPLETED }.sumOf { it.totalAmount },
        )
    }
}

data class SampleDataState(
    val alreadyLoaded: Boolean = false,
    val isLoading: Boolean = false,
    val message: String? = null,
)

class AdminDashboardViewModel : ViewModel() {

    /** Live counts: recalculated whenever users, providers or bookings change. null = loading. */
    val stats: StateFlow<DashboardStats?> =
        combine(AdminRepository.allUsers(), AdminRepository.allProviders(), AdminRepository.allBookings()) { u, p, b ->
            DashboardStats.from(u, p, b)
        }
            .catch { emit(DashboardStats()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _sampleState = MutableStateFlow(SampleDataState())
    val sampleState: StateFlow<SampleDataState> = _sampleState

    init {
        viewModelScope.launch {
            val loaded = try { AdminRepository.hasServices() } catch (e: Exception) { false }
            _sampleState.update { it.copy(alreadyLoaded = loaded) }
        }
    }

    fun loadSampleData() {
        _sampleState.update { it.copy(isLoading = true, message = null) }
        viewModelScope.launch {
            try {
                AdminRepository.loadSampleData()
                _sampleState.update {
                    it.copy(isLoading = false, alreadyLoaded = true, message = "Sample services and providers added ✓")
                }
            } catch (e: Exception) {
                _sampleState.update { it.copy(isLoading = false, message = "Failed: ${e.message}") }
            }
        }
    }

    fun logout() = AuthRepository.logout()
}
