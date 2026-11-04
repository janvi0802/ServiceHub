package com.example.servicehub.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.servicehub.data.FixedData
import com.example.servicehub.data.model.Roles
import com.example.servicehub.data.repository.AuthRepository
import com.example.servicehub.ui.auth.LoginScreen
import com.example.servicehub.ui.auth.RegisterScreen
import com.example.servicehub.ui.profile.ProfileScreen
import com.example.servicehub.ui.provider.PendingApprovalScreen
import com.example.servicehub.ui.screens.PlaceholderLink
import com.example.servicehub.ui.screens.PlaceholderScreen
import com.example.servicehub.ui.splash.SplashScreen

/**
 * The map of the whole app: which route opens which screen.
 * Also decides which bottom bar to show.
 */
@Composable
fun AppNavHost() {
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

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                AppBottomBar(tabs, currentRoute) { route ->
                    navController.openTab(route, homeRoute = tabs.first().route)
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH,
            modifier = Modifier.padding(innerPadding),
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
                ProfileScreen(onLoggedOut = {
                    role = null
                    navController.clearAndOpen(Routes.LOGIN)
                })
            }
            composable(Routes.NOTIFICATIONS) { PlaceholderScreen("Notifications") }

            // ---------- Customer ----------
            composable(Routes.CUSTOMER_HOME) {
                // One test link per category, to check routes that carry a value.
                PlaceholderScreen(
                    "Customer Home",
                    note = "Categories + search will appear here",
                    links = FixedData.categories.map { category ->
                        PlaceholderLink(category.name) {
                            navController.navigate("category_services/${category.id}")
                        }
                    },
                )
            }
            composable(Routes.SEARCH) { PlaceholderScreen("Search results") }
            composable(Routes.CATEGORY_SERVICES) { entry ->
                val categoryId = entry.arguments?.getString("categoryId").orEmpty()
                val name = FixedData.categoryById(categoryId)?.name ?: categoryId
                PlaceholderScreen("$name services")
            }
            composable(Routes.PROVIDER_LIST) { PlaceholderScreen("Provider list") }
            composable(Routes.PROVIDER_DETAIL) { PlaceholderScreen("Provider detail") }
            composable(Routes.BOOKING_FORM) { PlaceholderScreen("Booking form") }
            composable(Routes.BOOKING_SUCCESS) { PlaceholderScreen("Booking success") }
            composable(Routes.MY_BOOKINGS) { PlaceholderScreen("My Bookings") }
            composable(Routes.BOOKING_DETAIL) { PlaceholderScreen("Booking detail") }
            composable(Routes.RATE_REVIEW) { PlaceholderScreen("Rate & review") }
            composable(Routes.RECEIPT) { PlaceholderScreen("Receipt") }

            // ---------- Provider ----------
            composable(Routes.PENDING_APPROVAL) {
                PendingApprovalScreen(
                    onApproved = { destination -> navController.clearAndOpen(destination.route) },
                    onLogout = {
                        AuthRepository.logout()
                        role = null
                        navController.clearAndOpen(Routes.LOGIN)
                    },
                )
            }
            composable(Routes.PROVIDER_HOME) { PlaceholderScreen("Provider Home") }
            composable(Routes.PROVIDER_REQUESTS) { PlaceholderScreen("Job requests") }
            composable(Routes.JOB_DETAIL) { PlaceholderScreen("Job detail") }
            composable(Routes.JOB_HISTORY) { PlaceholderScreen("Job history") }

            // ---------- Admin ----------
            composable(Routes.ADMIN_DASHBOARD) { PlaceholderScreen("Admin Dashboard") }
            composable(Routes.ADMIN_PENDING_PROVIDERS) { PlaceholderScreen("Pending providers") }
            composable(Routes.ADMIN_ALL_PROVIDERS) { PlaceholderScreen("All providers") }
            composable(Routes.ADMIN_USERS) { PlaceholderScreen("All users") }
            composable(Routes.ADMIN_SERVICES) { PlaceholderScreen("Manage services") }
            composable(Routes.ADMIN_BOOKINGS) { PlaceholderScreen("All bookings") }
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
