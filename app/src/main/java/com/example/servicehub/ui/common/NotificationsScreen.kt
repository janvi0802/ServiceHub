package com.example.servicehub.ui.common

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.servicehub.data.DateUtils
import com.example.servicehub.data.model.AppNotification
import com.example.servicehub.data.repository.AuthRepository
import com.example.servicehub.data.repository.NotificationRepository
import com.example.servicehub.ui.components.EmptyBox
import com.example.servicehub.ui.components.AppCard
import com.example.servicehub.ui.components.IconTile
import com.example.servicehub.ui.theme.StatusAccepted
import com.example.servicehub.ui.theme.StatusCompleted
import com.example.servicehub.ui.theme.StatusInProgress
import com.example.servicehub.ui.theme.StatusRejected
import com.example.servicehub.ui.theme.StatusRequested
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.servicehub.ui.components.LoadingBox
import com.example.servicehub.ui.components.ScreenHeader
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotificationsViewModel : ViewModel() {
    /** Live list for the logged-in person. null = loading. */
    val notifications: StateFlow<List<AppNotification>?> =
        NotificationRepository.forUser(AuthRepository.currentUserId.orEmpty())
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun markRead(notification: AppNotification) = viewModelScope.launch {
        if (!notification.isRead) runCatching { NotificationRepository.markRead(notification.id) }
    }

    fun markAllRead() = viewModelScope.launch {
        runCatching { NotificationRepository.markAllRead(notifications.value.orEmpty()) }
    }
}

/**
 * List of in-app notifications. Unread ones are highlighted with a dot.
 * Tapping one marks it read and opens its booking (if it has one).
 */
@Composable
fun NotificationsScreen(
    onOpenBooking: (bookingId: String) -> Unit,
    viewModel: NotificationsViewModel = viewModel(),
) {
    val list by viewModel.notifications.collectAsState()

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Notifications") {
            if (list.orEmpty().any { !it.isRead }) {
                TextButton(onClick = viewModel::markAllRead) { Text("Mark all read") }
            }
        }
        val items = list
        when {
            items == null -> LoadingBox()
            items.isEmpty() -> EmptyBox(
                title = "You're all caught up",
                message = "Updates about your bookings will appear here.",
                icon = Icons.Filled.NotificationsNone,
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Group: Today, then Earlier
                val today = items.filter { DateUtils.isToday(it.createdAt) }
                val earlier = items.filter { !DateUtils.isToday(it.createdAt) }
                listOf("Today" to today, "Earlier" to earlier).filter { it.second.isNotEmpty() }.forEach { (title, list) ->
                    item(key = "header_$title") {
                        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    items(list, key = { it.id }) { notification ->
                        NotificationCard(notification, onClick = {
                            viewModel.markRead(notification)
                            if (notification.bookingId.isNotBlank()) onOpenBooking(notification.bookingId)
                        })
                    }
                }
            }
        }
    }
}

/** Which icon and colour a notification gets, chosen from its title. */
fun notificationStyle(title: String): Pair<ImageVector, Color> = when {
    title.contains("request", true) -> Icons.Filled.NotificationsActive to StatusRequested
    title.contains("accepted", true) || title.contains("approved", true) && !title.contains("not", true) ->
        Icons.Filled.CheckCircle to StatusAccepted
    title.contains("started", true) -> Icons.Filled.PlayCircle to StatusInProgress
    title.contains("completed", true) || title.contains("resolved", true) -> Icons.Filled.TaskAlt to StatusCompleted
    title.contains("review", true) -> Icons.Filled.Star to Color(0xFFF5A623)
    title.contains("rescheduled", true) -> Icons.Filled.EventRepeat to StatusAccepted
    title.contains("rejected", true) || title.contains("cancelled", true) || title.contains("not approved", true) ->
        Icons.Filled.Cancel to StatusRejected
    else -> Icons.Filled.Notifications to StatusAccepted
}

@Composable
private fun NotificationCard(notification: AppNotification, onClick: () -> Unit) {
    val unread = !notification.isRead
    val (icon, color) = notificationStyle(notification.title)
    AppCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.Top) {
            IconTile(icon, color, size = 40.dp, iconSize = 20.dp)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        notification.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (unread) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.weight(1f),
                    )
                    // Blue dot = not read yet
                    if (unread) Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                }
                Text(notification.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    DateUtils.dateTime(notification.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
