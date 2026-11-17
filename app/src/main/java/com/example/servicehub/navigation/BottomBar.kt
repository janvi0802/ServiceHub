package com.example.servicehub.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/** One tab in a bottom bar. */
data class BottomTab(val route: String, val label: String, val icon: ImageVector)

/** The tabs each role sees at the bottom of the screen. */
object BottomTabs {
    val customer = listOf(
        BottomTab(Routes.CUSTOMER_HOME, "Home", Icons.Filled.Home),
        BottomTab(Routes.MY_BOOKINGS, "Bookings", Icons.AutoMirrored.Filled.EventNote),
        BottomTab(Routes.NOTIFICATIONS, "Alerts", Icons.Filled.Notifications),
        BottomTab(Routes.PROFILE, "Profile", Icons.Filled.Person),
    )

    val provider = listOf(
        BottomTab(Routes.PROVIDER_HOME, "Home", Icons.Filled.Home),
        BottomTab(Routes.PROVIDER_REQUESTS, "Requests", Icons.Filled.Inbox),
        BottomTab(Routes.JOB_HISTORY, "History", Icons.Filled.History),
        BottomTab(Routes.NOTIFICATIONS, "Alerts", Icons.Filled.Notifications),
        BottomTab(Routes.PROFILE, "Profile", Icons.Filled.Person),
    )

    val admin = listOf(
        BottomTab(Routes.ADMIN_DASHBOARD, "Dashboard", Icons.Filled.Dashboard),
        BottomTab(Routes.ADMIN_PENDING_PROVIDERS, "Approvals", Icons.Filled.VerifiedUser),
        BottomTab(Routes.ADMIN_USERS, "Users", Icons.Filled.People),
        BottomTab(Routes.ADMIN_BOOKINGS, "Bookings", Icons.AutoMirrored.Filled.EventNote),
        BottomTab(Routes.ADMIN_SERVICES, "Services", Icons.Filled.Build),
    )
}

/**
 * Draws a bottom bar.
 * @param tabs which tabs to show (customer, provider or admin)
 * @param currentRoute the screen open right now, so its tab is highlighted
 * @param unreadCount number shown as a red badge on the Alerts tab (hidden when 0)
 * @param onTabClick called with the route of the tapped tab
 */
@Composable
fun AppBottomBar(
    tabs: List<BottomTab>,
    currentRoute: String?,
    unreadCount: Int,
    onTabClick: (String) -> Unit,
) {
    NavigationBar {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = { onTabClick(tab.route) },
                icon = {
                    if (tab.route == Routes.NOTIFICATIONS && unreadCount > 0) {
                        BadgedBox(badge = { Badge { Text(if (unreadCount > 99) "99+" else "$unreadCount") } }) {
                            Icon(tab.icon, contentDescription = "${tab.label}, $unreadCount unread")
                        }
                    } else {
                        Icon(tab.icon, contentDescription = tab.label)
                    }
                },
                label = { Text(tab.label) },
            )
        }
    }
}
