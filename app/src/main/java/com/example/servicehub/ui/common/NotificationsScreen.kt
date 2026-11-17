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
            items.isEmpty() -> EmptyBox("No notifications yet.\nUpdates about your bookings will appear here.")
            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(items, key = { it.id }) { notification ->
                    NotificationCard(notification, onClick = {
                        viewModel.markRead(notification)
                        if (notification.bookingId.isNotBlank()) onOpenBooking(notification.bookingId)
                    })
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(notification: AppNotification, onClick: () -> Unit) {
    val unread = !notification.isRead
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (unread) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            // Blue dot for unread
            Box(
                Modifier
                    .padding(top = 6.dp, end = 10.dp)
                    .size(10.dp)
                    .background(if (unread) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
            )
            Column(Modifier.weight(1f)) {
                Text(notification.title, fontWeight = if (unread) FontWeight.Bold else FontWeight.Normal)
                Text(notification.message, style = MaterialTheme.typography.bodyMedium)
                Text(DateUtils.dateTime(notification.createdAt), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
