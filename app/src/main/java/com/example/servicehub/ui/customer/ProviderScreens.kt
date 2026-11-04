package com.example.servicehub.ui.customer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.servicehub.data.DateUtils
import com.example.servicehub.data.FixedData
import com.example.servicehub.data.model.Provider
import com.example.servicehub.data.model.Review
import com.example.servicehub.data.model.Service
import com.example.servicehub.data.repository.ProviderRepository
import com.example.servicehub.data.repository.ServiceRepository
import com.example.servicehub.ui.components.Avatar
import com.example.servicehub.ui.components.EmptyBox
import com.example.servicehub.ui.components.LoadingBox
import com.example.servicehub.ui.components.ScreenHeader
import com.example.servicehub.ui.components.ratingText
import com.example.servicehub.ui.components.rupees
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val ALL_AREAS = "All areas"

// ===================================================================== Provider list

/**
 * Reads categoryId and serviceId from the route (SavedStateHandle holds the {values} of the route).
 */
class ProviderListViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    private val categoryId: String = savedStateHandle["categoryId"] ?: ""
    val serviceId: String = savedStateHandle["serviceId"] ?: ""

    val selectedArea = MutableStateFlow(ALL_AREAS)
    val service = MutableStateFlow<Service?>(null)

    /** Bookable providers of this category, filtered by the chosen area. null = loading. */
    val providers: StateFlow<List<Provider>?> =
        combine(ProviderRepository.bookableProviders(categoryId), selectedArea) { list, area ->
            if (area == ALL_AREAS) list else list.filter { it.area == area }
        }
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        viewModelScope.launch { service.value = runCatching { ServiceRepository.getService(serviceId) }.getOrNull() }
    }
}

@Composable
fun ProviderListScreen(
    onBack: () -> Unit,
    onProviderClick: (providerId: String, serviceId: String) -> Unit,
    viewModel: ProviderListViewModel = viewModel(),
) {
    val providers by viewModel.providers.collectAsState()
    val area by viewModel.selectedArea.collectAsState()
    val service by viewModel.service.collectAsState()

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Choose a provider", onBack)
        service?.let {
            Text(
                "${it.name} • ${rupees(it.price)}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        // Area filter chips, scrollable sideways
        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            (listOf(ALL_AREAS) + FixedData.areas).forEach { option ->
                FilterChip(
                    selected = option == area,
                    onClick = { viewModel.selectedArea.value = option },
                    label = { Text(option) },
                )
            }
        }
        val list = providers
        when {
            list == null -> LoadingBox()
            list.isEmpty() -> EmptyBox(
                if (area == ALL_AREAS) "No providers available right now."
                else "No providers available in $area. Try \"All areas\"."
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(list, key = { it.id }) { provider ->
                    ProviderCard(provider, onClick = { onProviderClick(provider.id, viewModel.serviceId) })
                }
            }
        }
    }
}

@Composable
private fun ProviderCard(provider: Provider, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Avatar(provider.name, 52.dp)
            Column(Modifier.weight(1f)) {
                Text(provider.name, style = MaterialTheme.typography.titleMedium)
                Text("${provider.experienceYears} yrs experience • ${provider.area}", style = MaterialTheme.typography.bodySmall)
                Text(
                    "${ratingText(provider.ratingAvg, provider.ratingCount)}  •  ${provider.jobsCompleted} jobs done",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

// ===================================================================== Provider detail

class ProviderDetailViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    val providerId: String = savedStateHandle["providerId"] ?: ""
    val serviceId: String = savedStateHandle["serviceId"] ?: ""

    val provider: StateFlow<Provider?> = ProviderRepository.liveProvider(providerId)
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val reviews: StateFlow<List<Review>> = ProviderRepository.reviewsFor(providerId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val service = MutableStateFlow<Service?>(null)

    init {
        viewModelScope.launch { service.value = runCatching { ServiceRepository.getService(serviceId) }.getOrNull() }
    }
}

@Composable
fun ProviderDetailScreen(
    onBack: () -> Unit,
    onBook: (providerId: String, serviceId: String) -> Unit,
    viewModel: ProviderDetailViewModel = viewModel(),
) {
    val provider by viewModel.provider.collectAsState()
    val reviews by viewModel.reviews.collectAsState()
    val service by viewModel.service.collectAsState()

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Provider", onBack)
        val p = provider
        if (p == null) {
            LoadingBox()
            return@Column
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { ProviderHeader(p) }
            item {
                Text("About", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(p.bio.ifBlank { "No bio yet." }, style = MaterialTheme.typography.bodyMedium)
            }
            item {
                HorizontalDivider()
                Text(
                    "Reviews (${reviews.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (reviews.isEmpty()) {
                item { Text("No reviews yet.", style = MaterialTheme.typography.bodyMedium) }
            }
            items(reviews, key = { it.id }) { review -> ReviewItem(review) }
        }

        // Book button fixed at the bottom
        val s = service
        Button(
            onClick = { onBook(p.id, viewModel.serviceId) },
            enabled = s != null && p.isAvailable,
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp),
        ) {
            Text(
                when {
                    !p.isAvailable -> "Provider not available now"
                    s == null -> "Loading..."
                    else -> "Book ${s.name} • ${rupees(s.price)}"
                }
            )
        }
    }
}

@Composable
private fun ProviderHeader(p: Provider) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Avatar(p.name, 72.dp)
        Column {
            Text(p.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(FixedData.categoryById(p.categoryId)?.name.orEmpty() + " • " + p.area)
            Text("${p.experienceYears} years experience", style = MaterialTheme.typography.bodySmall)
            Text(
                "${ratingText(p.ratingAvg, p.ratingCount)}  •  ${p.jobsCompleted} jobs done",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun ReviewItem(review: Review) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row {
                Text(review.customerName, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                val stars = review.rating.coerceIn(0, 5)   // safety: repeat() crashes on negative numbers
                Text("★".repeat(stars) + "☆".repeat(5 - stars), color = MaterialTheme.colorScheme.secondary)
            }
            if (review.comment.isNotBlank()) Text(review.comment, style = MaterialTheme.typography.bodyMedium)
            Text(DateUtils.dateTime(review.createdAt), style = MaterialTheme.typography.labelSmall)
        }
    }
}
