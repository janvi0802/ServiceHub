package com.example.servicehub.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.servicehub.data.DateUtils
import com.example.servicehub.data.model.Booking
import com.example.servicehub.data.model.ChatMessage
import com.example.servicehub.data.repository.AuthRepository
import com.example.servicehub.data.repository.BookingRepository
import com.example.servicehub.data.repository.ChatRepository
import com.example.servicehub.data.repository.canChatAt
import com.example.servicehub.ui.components.BottomActionBar
import com.example.servicehub.ui.components.Caption
import com.example.servicehub.ui.components.EmptyBox
import com.example.servicehub.ui.components.LoadingBox
import com.example.servicehub.ui.components.ScreenHeader
import com.example.servicehub.ui.components.TextInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Live booking (for names and whether chat is open) and its live messages. */
class ChatViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    private val bookingId: String = savedStateHandle["bookingId"] ?: ""
    val myId = AuthRepository.currentUserId.orEmpty()

    val booking: StateFlow<Booking?> = BookingRepository.liveBooking(bookingId)
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** null = still loading */
    val messages: StateFlow<List<ChatMessage>?> = ChatRepository.liveMessages(bookingId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val error = MutableStateFlow<String?>(null)
    val sending = MutableStateFlow(false)

    fun send(text: String, onSent: () -> Unit) {
        val b = booking.value ?: return
        if (sending.value) return
        sending.value = true
        error.value = null
        viewModelScope.launch {
            try {
                ChatRepository.send(b, myId, text)
                onSent()
            } catch (e: Exception) {
                error.value = BookingRepository.friendlyError(e)
            } finally {
                sending.value = false
            }
        }
    }
}

/** Chat between the customer and the provider of one booking. My messages are on the right, in blue. */
@Composable
fun ChatScreen(onBack: () -> Unit, viewModel: ChatViewModel = viewModel()) {
    val booking by viewModel.booking.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val error by viewModel.error.collectAsState()
    val sending by viewModel.sending.collectAsState()
    var text by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    val b = booking
    val otherName = when {
        b == null -> ""
        viewModel.myId == b.customerId -> b.providerName
        else -> b.customerName
    }

    // Jump to the newest message whenever one arrives
    LaunchedEffect(messages?.size) {
        val count = messages?.size ?: 0
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(if (otherName.isBlank()) "Chat" else "Chat with $otherName", onBack, subtitle = b?.serviceName)
        val list = messages
        Box(Modifier.weight(1f)) {
            when {
                list == null || b == null -> LoadingBox()
                list.isEmpty() -> EmptyBox(
                    title = "No messages yet",
                    message = "Ask a question or share details, e.g. \"Please ring the bell at gate 2\".",
                    icon = Icons.Filled.ChatBubbleOutline,
                )
                else -> LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(list, key = { it.id }) { message -> MessageBubble(message, mine = message.senderId == viewModel.myId) }
                }
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp)) }

        if (b != null && canChatAt(b, System.currentTimeMillis())) {
            BottomActionBar {
                TextInput(text, { text = it.take(500) }, "Type a message", singleLine = false, modifier = Modifier.weight(1f))
                IconButton(onClick = { viewModel.send(text) { text = "" } }, enabled = text.isNotBlank() && !sending) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                }
            }
        } else if (b != null) {
            Caption("Chat is closed for this booking.", modifier = Modifier.padding(16.dp))
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage, mine: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
        Column(
            Modifier
                .widthIn(max = 280.dp)
                .background(
                    if (mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(14.dp),
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
        ) {
            Text(message.text, color = if (mine) Color.White else MaterialTheme.colorScheme.onSurface)
            Text(
                DateUtils.dateTime(message.createdAt).substringAfter(", "),
                style = MaterialTheme.typography.labelSmall,
                color = if (mine) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
