package com.example.servicehub.ui.customer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.servicehub.data.model.Booking
import com.example.servicehub.data.repository.AuthRepository
import com.example.servicehub.data.repository.BookingRepository
import com.example.servicehub.data.repository.ReviewRepository
import com.example.servicehub.ui.components.LoadingBox
import com.example.servicehub.ui.components.ScreenHeader
import com.example.servicehub.ui.components.TextInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RateUiState(
    val stars: Int = 0,
    val comment: String = "",
    val isSaving: Boolean = false,
    val error: String? = null,
    val done: Boolean = false,
)

class RateReviewViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    private val bookingId: String = savedStateHandle["bookingId"] ?: ""

    val booking: StateFlow<Booking?> = BookingRepository.liveBooking(bookingId)
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _uiState = MutableStateFlow(RateUiState())
    val uiState: StateFlow<RateUiState> = _uiState

    fun onStars(n: Int) = _uiState.update { it.copy(stars = n, error = null) }
    fun onComment(v: String) = _uiState.update { it.copy(comment = v.take(300), error = null) }

    fun submit() {
        val s = _uiState.value
        if (s.stars == 0) {
            _uiState.update { it.copy(error = "Please tap the stars to give a rating.") }
            return
        }
        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            try {
                ReviewRepository.submit(bookingId, AuthRepository.currentUserId.orEmpty(), s.stars, s.comment.trim())
                _uiState.update { it.copy(isSaving = false, done = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = BookingRepository.friendlyError(e)) }
            }
        }
    }
}

/** 1–5 stars and an optional comment, once per completed booking. */
@Composable
fun RateReviewScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: RateReviewViewModel = viewModel(),
) {
    val booking by viewModel.booking.collectAsState()
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.done) { if (state.done) onDone() }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Rate service", onBack)
        val b = booking ?: run { LoadingBox(); return@Column }

        Column(
            Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("How was ${b.serviceName}?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("by ${b.providerName}")

            if (b.isRated) {
                Text("You have already rated this booking. Thank you!")
                return@Column
            }

            // Five tappable stars
            Row {
                (1..5).forEach { n ->
                    IconButton(onClick = { viewModel.onStars(n) }, modifier = Modifier.size(56.dp)) {
                        Icon(
                            if (n <= state.stars) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = "$n star",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(44.dp),
                        )
                    }
                }
            }
            Text(
                when (state.stars) {
                    1 -> "Poor"; 2 -> "Fair"; 3 -> "Good"; 4 -> "Very good"; 5 -> "Excellent!"
                    else -> "Tap a star"
                }
            )
            TextInput(state.comment, viewModel::onComment, "Comment (optional)", singleLine = false)
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = viewModel::submit,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth().height(50.dp),
            ) { Text(if (state.isSaving) "Submitting..." else "Submit review") }
        }
    }
}
