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
import com.example.servicehub.ui.screens.PlaceholderLink
import com.example.servicehub.ui.screens.PlaceholderScreen

/**
 * The map of the whole app: which route opens which screen.
 * Also decides which bottom bar to show.
 */
@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    // Which role is using the app. In Phase 2 this comes from Firestore after login;
    // for now the splash screen has test buttons to pick a role.
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
                // Temporary test buttons. Phase 2 replaces these with a real login check.
                fun enterAs(newRole: String, home: String) {
                    role = newRole
                    navController.navigate(home) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
                PlaceholderScreen(
                    title = "ServiceHub",
                    note = "Phase 1 test: choose a role to preview its screens",
                    links = listOf(
                        PlaceholderLink("Login screen") { navController.navigate(Routes.LOGIN) },
                        PlaceholderLink("Enter as Customer") { enterAs(Roles.CUSTOMER, Routes.CUSTOMER_HOME) },
                        PlaceholderLink("Enter as Provider") { enterAs(Roles.PROVIDER, Routes.PROVIDER_HOME) },
                        PlaceholderLink("Enter as Admin") { enterAs(Roles.ADMIN, Routes.ADMIN_DASHBOARD) },
                    ),
                )
            }
            composable(Routes.LOGIN) {
                PlaceholderScreen(
                    "Login",
                    links = listOf(PlaceholderLink("Register") { navController.navigate(Routes.REGISTER) }),
                )
            }
            composable(Routes.REGISTER) { PlaceholderScreen("Register") }
            composable(Routes.PROFILE) { PlaceholderScreen("Profile") }
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
            composable(Routes.PENDING_APPROVAL) { PlaceholderScreen("Waiting for approval") }
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
