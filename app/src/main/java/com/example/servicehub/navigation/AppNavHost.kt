package com.example.servicehub.navigation

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.servicehub.data.model.Roles
import com.example.servicehub.data.repository.AuthRepository
import com.example.servicehub.data.repository.NotificationRepository
import com.example.servicehub.data.repository.UserRepository
import com.example.servicehub.data.repository.ProviderRepository
import com.example.servicehub.data.model.ApprovalStatus
import com.example.servicehub.ui.admin.AdminAllProvidersScreen
import com.example.servicehub.ui.admin.AdminBookingsScreen
import com.example.servicehub.ui.admin.AdminCategoriesScreen
import com.example.servicehub.ui.admin.AdminComplaintsScreen
import com.example.servicehub.ui.admin.AdminDashboardScreen
import com.example.servicehub.ui.admin.AdminServicesScreen
import com.example.servicehub.ui.admin.AdminUsersScreen
import com.example.servicehub.ui.common.NotificationsScreen
import com.example.servicehub.ui.common.ReceiptScreen
import com.example.servicehub.ui.customer.RateReviewScreen
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import com.example.servicehub.data.repository.BookingRepository
import com.example.servicehub.ui.provider.ProviderJobsViewModel
import com.example.servicehub.ui.admin.PendingProvidersScreen
import com.example.servicehub.ui.auth.LoginScreen
import com.example.servicehub.ui.auth.RegisterScreen
import com.example.servicehub.ui.customer.BookingDetailScreen
import com.example.servicehub.ui.customer.BookingFormScreen
import com.example.servicehub.ui.customer.BookingSuccessScreen
import com.example.servicehub.ui.customer.CategoryServicesScreen
import com.example.servicehub.ui.customer.CustomerHomeScreen
import com.example.servicehub.ui.customer.MyBookingsScreen
import com.example.servicehub.ui.customer.ProviderDetailScreen
import com.example.servicehub.ui.customer.ProviderListScreen
import com.example.servicehub.ui.customer.SearchScreen
import com.example.servicehub.ui.provider.JobDetailScreen
import com.example.servicehub.ui.provider.JobHistoryScreen
import com.example.servicehub.ui.provider.ProviderHomeScreen
import com.example.servicehub.ui.provider.ProviderRequestsScreen
import com.example.servicehub.ui.profile.ProfileScreen
import com.example.servicehub.ui.provider.PendingApprovalScreen
import com.example.servicehub.ui.splash.SplashScreen

/**
 * Starts a fresh navigation "session" after every logout.
 *
 * Why: bottom-bar tabs remember their screens (and the data inside them).
 * Without this, after logging out and in as ANOTHER person, a tab could show
 * the previous person's data. Changing [session] makes key() throw away the old
 * NavController with everything it remembered, and build a brand new one.
 */
@Composable
fun AppNavHost() {
    var session by rememberSaveable { mutableIntStateOf(0) }
    key(session) {
        SessionNavHost(onLoggedOut = { session++ })
    }
}

/**
 * The map of the whole app: which route opens which screen.
 * Also decides which bottom bar to show.
 * [onLoggedOut] must be called after AuthRepository.logout().
 */
@Composable
private fun SessionNavHost(onLoggedOut: () -> Unit) {
    val navController = rememberNavController()

    // Which role is using the app (read from Firestore at login). Picks the bottom bar.
    var role by rememberSaveable { mutableStateOf<String?>(null) }

    // The route of the screen open right now (updates automatically).
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    val tabs = when (role) {
        Roles.CUSTOMER -> BottomTabs.customer
        Roles.PROVIDER -> BottomTabs.provider
        Roles.ADMIN -> BottomTabs.admin
        else -> emptyList()
    }
    // Show the bar only on the main tab screens, not on detail screens.
    val showBottomBar = tabs.any { it.route == currentRoute }

    // Live unread-notification count for the badge. Starts listening once someone is logged in.
    val userId = if (role != null) AuthRepository.currentUserId else null
    val unreadFlow = remember(userId) {
        if (userId == null) flowOf(0) else NotificationRepository.unreadCount(userId).catch { emit(0) }
    }
    val unreadCount by unreadFlow.collectAsState(initial = 0)

    // Providers: live number of new requests, shown as a badge on the Jobs tab.
    val requestsFlow = remember(userId, role) {
        if (userId == null || role != Roles.PROVIDER) flowOf(0)
        else BookingRepository.providerBookings(userId)
            .map { ProviderJobsViewModel.newRequests(it).size }
            .catch { emit(0) }
    }
    val requestCount by requestsFlow.collectAsState(initial = 0)
    val badges = mapOf(Routes.NOTIFICATIONS to unreadCount, Routes.PROVIDER_REQUESTS to requestCount)

    // Watch the logged-in person's own account: if the admin blocks them, log out at once.
    val meFlow = remember(userId) {
        if (userId == null) flowOf(null) else UserRepository.liveUser(userId).catch { emit(null) }
    }
    val me by meFlow.collectAsState(initial = null)
    LaunchedEffect(me?.isBlocked) {
        if (me?.isBlocked == true) {
            AuthRepository.logout()
            onLoggedOut()   // fresh session -> Login screen; logging in again shows "blocked"
        }
    }

    // Providers: if the admin takes back an approval while they use the app,
    // send them to the waiting screen at once (they cannot keep taking jobs).
    val providerFlow = remember(userId, role) {
        if (userId == null || role != Roles.PROVIDER) flowOf(null)
        else ProviderRepository.liveProvider(userId).catch { emit(null) }
    }
    val myProvider by providerFlow.collectAsState(initial = null)
    val approval = myProvider?.approvalStatus
    LaunchedEffect(approval) {
        val onWaitingScreen = navController.currentDestination?.route == Routes.PENDING_APPROVAL
        if (approval != null && approval != ApprovalStatus.APPROVED && !onWaitingScreen) {
            navController.clearAndOpen(Routes.PENDING_APPROVAL)
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                AppBottomBar(tabs, currentRoute, badges) { route ->
                    navController.openTab(route, homeRoute = tabs.first().route)
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH,
            // padding: room for the bottom bar and status bar.
            // imePadding: when the keyboard opens, the screen moves up so fields and
            // buttons are not hidden behind it.
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding(),
        ) {
            // ---------- Common ----------
            composable(Routes.SPLASH) {
                SplashScreen(
                    onGoToLogin = { navController.clearAndOpen(Routes.LOGIN) },
                    onGoToHome = { destination ->
                        role = destination.role
                        navController.clearAndOpen(destination.route)
                    },
                )
            }
            composable(Routes.LOGIN) {
                LoginScreen(
                    onLoggedIn = { destination ->
                        role = destination.role
                        navController.clearAndOpen(destination.route)
                    },
                    onGoToRegister = { navController.navigate(Routes.REGISTER) },
                )
            }
            composable(Routes.REGISTER) {
                RegisterScreen(
                    onRegistered = { destination ->
                        role = destination.role
                        navController.clearAndOpen(destination.route)
                    },
                    onGoToLogin = { navController.popBackStack() },
                )
            }
            composable(Routes.PROFILE) {
                ProfileScreen(onLoggedOut = onLoggedOut)
            }
            composable(Routes.NOTIFICATIONS) {
                NotificationsScreen(onOpenBooking = { bookingId ->
                    // Providers open the job screen, customers the booking screen.
                    val route = if (role == Roles.PROVIDER) Routes.jobDetail(bookingId) else Routes.bookingDetail(bookingId)
                    navController.navigate(route)
                })
            }

            // ---------- Customer ----------
            composable(Routes.CUSTOMER_HOME) {
                CustomerHomeScreen(
                    onOpenSearch = { navController.navigate(Routes.SEARCH) },
                    onOpenCategory = { categoryId -> navController.navigate(Routes.categoryServices(categoryId)) },
                    onOpenBooking = { id -> navController.navigate(Routes.bookingDetail(id)) },
                    onRate = { id -> navController.navigate(Routes.rateReview(id)) },
                    onOpenService = { categoryId, serviceId ->
                        navController.navigate(Routes.providerList(categoryId, serviceId))
                    },
                    onOpenProvider = { providerId, serviceId ->
                        navController.navigate(Routes.providerDetail(providerId, serviceId))
                    },
                )
            }
            composable(Routes.SEARCH) {
                SearchScreen(
                    onBack = { navController.popBackStack() },
                    onServiceClick = { service ->
                        navController.navigate(Routes.providerList(service.categoryId, service.id))
                    },
                )
            }
            composable(Routes.CATEGORY_SERVICES) { entry ->
                CategoryServicesScreen(
                    categoryId = entry.arguments?.getString("categoryId").orEmpty(),
                    onBack = { navController.popBackStack() },
                    onServiceClick = { service ->
                        navController.navigate(Routes.providerList(service.categoryId, service.id))
                    },
                )
            }
            composable(Routes.PROVIDER_LIST) {
                ProviderListScreen(
                    onBack = { navController.popBackStack() },
                    onProviderClick = { providerId, serviceId ->
                        navController.navigate(Routes.providerDetail(providerId, serviceId))
                    },
                )
            }
            composable(Routes.PROVIDER_DETAIL) {
                ProviderDetailScreen(
                    onBack = { navController.popBackStack() },
                    onBook = { providerId, serviceId ->
                        navController.navigate(Routes.bookingForm(providerId, serviceId))
                    },
                )
            }
            composable(Routes.BOOKING_FORM) {
                BookingFormScreen(
                    onBack = { navController.popBackStack() },
                    onBooked = { bookingId ->
                        // Remove the form, provider and service screens: Back now goes to Home.
                        navController.navigate(Routes.bookingSuccess(bookingId)) {
                            popUpTo(Routes.CUSTOMER_HOME)
                        }
                    },
                )
            }
            composable(Routes.BOOKING_SUCCESS) { entry ->
                val bookingId = entry.arguments?.getString("bookingId").orEmpty()
                BookingSuccessScreen(
                    bookingId = bookingId,
                    onViewBooking = {
                        navController.navigate(Routes.bookingDetail(bookingId)) {
                            popUpTo(Routes.CUSTOMER_HOME)
                        }
                    },
                    onGoHome = { navController.popBackStack(Routes.CUSTOMER_HOME, inclusive = false) },
                )
            }
            composable(Routes.MY_BOOKINGS) {
                MyBookingsScreen(onBookingClick = { id -> navController.navigate(Routes.bookingDetail(id)) })
            }
            composable(Routes.BOOKING_DETAIL) {
                BookingDetailScreen(
                    onBack = { navController.popBackStack() },
                    onRate = { id -> navController.navigate(Routes.rateReview(id)) },
                    onReceipt = { id -> navController.navigate(Routes.receipt(id)) },
                    onBookAgain = { providerId, serviceId ->
                        navController.navigate(Routes.bookingForm(providerId, serviceId))
                    },
                    onFindAnotherProvider = { categoryId, serviceId ->
                        navController.navigate(Routes.providerList(categoryId, serviceId))
                    },
                )
            }
            composable(Routes.RATE_REVIEW) {
                RateReviewScreen(
                    onBack = { navController.popBackStack() },
                    onDone = { navController.popBackStack() },   // back to the booking, which now shows "rated"
                )
            }
            composable(Routes.RECEIPT) {
                ReceiptScreen(onBack = { navController.popBackStack() })
            }

            // ---------- Provider ----------
            composable(Routes.PENDING_APPROVAL) {
                PendingApprovalScreen(
                    onApproved = { destination -> navController.clearAndOpen(destination.route) },
                    onLogout = {
                        AuthRepository.logout()
                        onLoggedOut()
                    },
                )
            }
            composable(Routes.PROVIDER_HOME) {
                ProviderHomeScreen(
                    onJobClick = { id -> navController.navigate(Routes.jobDetail(id)) },
                    onOpenRequests = { navController.openTab(Routes.PROVIDER_REQUESTS, Routes.PROVIDER_HOME) },
                )
            }
            composable(Routes.PROVIDER_REQUESTS) {
                ProviderRequestsScreen(onJobClick = { id -> navController.navigate(Routes.jobDetail(id)) })
            }
            composable(Routes.JOB_DETAIL) {
                JobDetailScreen(
                    onBack = { navController.popBackStack() },
                    onReceipt = { id -> navController.navigate(Routes.receipt(id)) },
                )
            }
            composable(Routes.JOB_HISTORY) {
                JobHistoryScreen(onJobClick = { id -> navController.navigate(Routes.jobDetail(id)) })
            }

            // ---------- Admin ----------
            composable(Routes.ADMIN_DASHBOARD) {
                AdminDashboardScreen(
                    onOpenPendingProviders = { navController.openTab(Routes.ADMIN_PENDING_PROVIDERS, Routes.ADMIN_DASHBOARD) },
                    onOpenAllProviders = { navController.navigate(Routes.ADMIN_ALL_PROVIDERS) },
                    onOpenUsers = { navController.openTab(Routes.ADMIN_USERS, Routes.ADMIN_DASHBOARD) },
                    onOpenBookings = { navController.openTab(Routes.ADMIN_BOOKINGS, Routes.ADMIN_DASHBOARD) },
                    onOpenComplaints = { navController.navigate(Routes.ADMIN_COMPLAINTS) },
                    onLoggedOut = onLoggedOut,
                )
            }
            composable(Routes.ADMIN_PENDING_PROVIDERS) { PendingProvidersScreen() }
            composable(Routes.ADMIN_ALL_PROVIDERS) {
                AdminAllProvidersScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.ADMIN_USERS) { AdminUsersScreen() }
            composable(Routes.ADMIN_SERVICES) {
                AdminServicesScreen(onOpenCategories = { navController.navigate(Routes.ADMIN_CATEGORIES) })
            }
            composable(Routes.ADMIN_CATEGORIES) { AdminCategoriesScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.ADMIN_BOOKINGS) { AdminBookingsScreen() }
            composable(Routes.ADMIN_COMPLAINTS) { AdminComplaintsScreen(onBack = { navController.popBackStack() }) }
        }
    }
}

/**
 * Opens a screen and removes every screen before it,
 * so the phone's Back button cannot return to e.g. Login after logging in.
 */
private fun NavHostController.clearAndOpen(route: String) {
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
    }
}

/**
 * Opens a bottom-bar tab the standard way:
 *  - go back to the role's home tab instead of piling up screens
 *    (so the phone's Back button from any tab returns to Home),
 *  - don't open the same tab twice,
 *  - remember each tab's scroll position.
 */
private fun NavHostController.openTab(route: String, homeRoute: String) {
    navigate(route) {
        popUpTo(homeRoute) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
