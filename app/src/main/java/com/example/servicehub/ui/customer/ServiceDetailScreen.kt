package com.example.servicehub.ui.customer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.servicehub.data.FixedData
import com.example.servicehub.data.model.GUARANTEE_DAYS
import com.example.servicehub.data.model.Service
import com.example.servicehub.data.model.includedPoints
import com.example.servicehub.data.model.notIncludedPoints
import com.example.servicehub.data.repository.ServiceRepository
import com.example.servicehub.ui.components.AppCard
import com.example.servicehub.ui.components.BottomActionBar
import com.example.servicehub.ui.components.Caption
import com.example.servicehub.ui.components.CardTitle
import com.example.servicehub.ui.components.EmptyBox
import com.example.servicehub.ui.components.IconTile
import com.example.servicehub.ui.components.LoadingBox
import com.example.servicehub.ui.components.ScreenHeader
import com.example.servicehub.ui.components.rupees
import com.example.servicehub.ui.theme.StatusCompleted
import com.example.servicehub.ui.theme.StatusRejected
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/** What the screen shows: still loading, not found, or the service. */
sealed class ServiceLoad {
    data object Loading : ServiceLoad()
    data object NotFound : ServiceLoad()
    data class Loaded(val service: Service) : ServiceLoad()
}

class ServiceDetailViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    private val serviceId: String = savedStateHandle["serviceId"] ?: ""
    val service = MutableStateFlow<ServiceLoad>(ServiceLoad.Loading)

    init {
        viewModelScope.launch {
            val s = runCatching { ServiceRepository.getService(serviceId) }.getOrNull()
            service.value = if (s == null || !s.isActive) ServiceLoad.NotFound else ServiceLoad.Loaded(s)
        }
    }
}

/**
 * One service explained before booking (like Urban Company's service page):
 * price and time, what's included, what's not, how it works, our promise, and a fixed "Book now".
 */
@Composable
fun ServiceDetailScreen(
    onBack: () -> Unit,
    onBook: (categoryId: String, serviceId: String) -> Unit,
    viewModel: ServiceDetailViewModel = viewModel(),
) {
    val load by viewModel.service.collectAsState()

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Service details", onBack)
        val s = when (val l = load) {
            ServiceLoad.Loading -> { LoadingBox(); return@Column }
            ServiceLoad.NotFound -> { EmptyBox("This service is not available right now."); return@Column }
            is ServiceLoad.Loaded -> l.service
        }
        val category = FixedData.categoryById(s.categoryId)

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Title card: icon, name, short description, price and time
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(category?.icon ?: Icons.Filled.Build, category?.color ?: MaterialTheme.colorScheme.primary, size = 52.dp, iconSize = 26.dp)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(s.name, style = MaterialTheme.typography.titleLarge)
                        Caption(category?.name.orEmpty())
                    }
                }
                Text(s.description, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 10.dp))
                Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Fact(Icons.Filled.CurrencyRupee, rupees(s.price))
                    Fact(Icons.Filled.Schedule, "~${s.durationMins} min")
                }
            }

            AppCard {
                CardTitle("What's included")
                includedPoints(s).forEach { Point(Icons.Filled.CheckCircle, StatusCompleted, it) }
            }
            AppCard {
                CardTitle("Not included")
                notIncludedPoints(s).forEach { Point(Icons.Filled.Cancel, StatusRejected, it) }
            }
            AppCard {
                CardTitle("How it works")
                listOf(
                    "Choose a verified professional and a time slot",
                    "The professional accepts and tells you when they're on the way",
                    "Share your 4-digit PIN when they arrive, so the job can start",
                    "Pay by Cash or UPI only after the work is done",
                ).forEachIndexed { i, step -> Point(null, MaterialTheme.colorScheme.primary, step, number = i + 1) }
            }
            AppCard {
                CardTitle("Our promise")
                Point(Icons.Filled.Verified, StatusCompleted, "Professionals approved by the ServiceHub team")
                Point(Icons.Filled.CurrencyRupee, StatusCompleted, "Fixed price shown before you book")
                Point(Icons.Filled.Replay, StatusCompleted, "Not happy? The job is redone free within $GUARANTEE_DAYS days")
            }
        }

        // Fixed bottom bar with the price and the main action
        BottomActionBar {
            Column(Modifier.weight(1f)) {
                Text(rupees(s.price), style = MaterialTheme.typography.titleLarge)
                Caption("+ ₹${FixedData.VISIT_FEE} visit fee · pay after service")
            }
            Button(onClick = { onBook(s.categoryId, s.id) }, modifier = Modifier.height(50.dp)) { Text("Book now") }
        }
    }
}

@Composable
private fun Fact(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Text(" $text", style = MaterialTheme.typography.titleSmall)
    }
}

/** One bullet line: an icon (or a step number) and the text. */
@Composable
private fun Point(icon: ImageVector?, color: Color, text: String, number: Int? = null) {
    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.Top) {
        if (number != null) {
            Text("$number.", style = MaterialTheme.typography.titleSmall, color = color, modifier = Modifier.padding(end = 8.dp))
        } else if (icon != null) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp).padding(top = 1.dp))
        }
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = if (icon != null && number == null) 8.dp else 0.dp),
        )
    }
}
