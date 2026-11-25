package com.example.servicehub.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.servicehub.data.DateUtils
import com.example.servicehub.data.FixedData
import com.example.servicehub.data.model.ApprovalStatus
import com.example.servicehub.data.model.Booking
import com.example.servicehub.data.model.BookingStatus
import com.example.servicehub.data.model.Provider
import com.example.servicehub.data.model.Roles
import com.example.servicehub.data.model.RevisitStatus
import com.example.servicehub.data.model.User
import com.example.servicehub.data.model.ComplaintStatus
import com.example.servicehub.data.repository.AdminRepository
import com.example.servicehub.data.repository.ComplaintRepository
import kotlinx.coroutines.flow.map
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
    val bookingsByStatus: Map<BookingStatus, Int> = emptyMap(),   // REQUESTED excludes expired ones
    val expiredRequests: Int = 0,
    val totalBookings: Int = 0,
    val completedValue: Int = 0,
    /** ServiceHub's own income: the ₹49 visit fee of every completed booking. */
    val platformEarnings: Int = 0,
    /** What providers earned: completed value minus the visit fees. */
    val providerEarnings: Int = 0,
    /** Completed jobs where the customer asked for a free revisit that is not done yet. */
    val openRevisits: Int = 0,
    val topProviders: List<Provider> = emptyList(),
    /** Bookings made on each of the last 7 days, oldest first, e.g. ("Mon", 3). */
    val bookingsPerDay: List<Pair<String, Int>> = emptyList(),
    /** Completed value per category name, biggest first, e.g. ("Plumber", 1490). */
    val revenueByCategory: List<Pair<String, Int>> = emptyList(),
    /** The 5 newest bookings, for the "Recent activity" list. */
    val recent: List<Booking> = emptyList(),
) {
    companion object {
        /** Counts everything from the three live lists. Pure function = easy to test. */
        fun from(users: List<User>, providers: List<Provider>, bookings: List<Booking>) = DashboardStats(
            customers = users.count { it.role == Roles.CUSTOMER },
            approvedProviders = providers.count { it.approvalStatus == ApprovalStatus.APPROVED },
            pendingProviders = providers.count { it.approvalStatus == ApprovalStatus.PENDING },
            blockedUsers = users.count { it.isBlocked },
            bookingsByStatus = BookingStatus.entries.associateWith { status ->
                bookings.count { it.bookingStatus == status && !it.isExpired }
            },
            expiredRequests = bookings.count { it.isExpired },
            totalBookings = bookings.size,
            completedValue = bookings.filter { it.bookingStatus == BookingStatus.COMPLETED }.sumOf { it.totalAmount },
            platformEarnings = bookings.filter { it.bookingStatus == BookingStatus.COMPLETED }.sumOf { it.visitFee - it.discount },   // coupons are paid by ServiceHub
            providerEarnings = bookings.filter { it.bookingStatus == BookingStatus.COMPLETED }.sumOf { it.providerEarning },
            openRevisits = bookings.count { it.revisitStatus == RevisitStatus.REQUESTED },
            topProviders = topProviders(providers),
            bookingsPerDay = bookingsPerDay(bookings),
            revenueByCategory = revenueByCategory(bookings),
            recent = bookings.sortedByDescending { it.createdAt }.take(5),
        )

        /** How many bookings were CREATED on each of the last 7 days (oldest first). */
        fun bookingsPerDay(bookings: List<Booking>, now: Long = System.currentTimeMillis()): List<Pair<String, Int>> =
            DateUtils.lastDays(7, now).map { day ->
                val label = DateUtils.weekdayShortNames[(DateUtils.weekdayOf(day) - 1).coerceIn(0, 6)]
                label to bookings.count { it.createdAt > 0 && DateUtils.dateOf(it.createdAt) == day }
            }

        /** Total value of COMPLETED bookings per category, biggest first (categories with ₹0 left out). */
        fun revenueByCategory(bookings: List<Booking>): List<Pair<String, Int>> =
            bookings.filter { it.bookingStatus == BookingStatus.COMPLETED }
                .groupBy { it.categoryId }
                .map { (id, list) -> (FixedData.categoryById(id)?.name ?: id) to list.sumOf { it.totalAmount } }
                .filter { it.second > 0 }
                .sortedByDescending { it.second }

        /** Best 3 approved providers who have done some work: highest rating first, then most jobs. */
        fun topProviders(providers: List<Provider>): List<Provider> =
            providers
                .filter { it.approvalStatus == ApprovalStatus.APPROVED && (it.ratingCount > 0 || it.jobsCompleted > 0) }
                .sortedWith(compareByDescending<Provider> { it.ratingAvg }.thenByDescending { it.jobsCompleted })
                .take(3)
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

    /** Live number of complaints the admin has not resolved yet. */
    val openComplaints: StateFlow<Int> = ComplaintRepository.all()
        .map { list -> list.count { it.status == ComplaintStatus.OPEN } }
        .catch { emit(0) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

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
