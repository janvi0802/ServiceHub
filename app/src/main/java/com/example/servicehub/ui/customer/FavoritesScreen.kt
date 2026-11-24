package com.example.servicehub.ui.customer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.servicehub.data.FixedData
import com.example.servicehub.data.model.ApprovalStatus
import com.example.servicehub.data.model.Provider
import com.example.servicehub.data.model.Service
import com.example.servicehub.data.repository.ProviderRepository
import com.example.servicehub.data.repository.ServiceRepository
import com.example.servicehub.ui.components.AppCard
import com.example.servicehub.ui.components.Avatar
import com.example.servicehub.ui.components.Caption
import com.example.servicehub.ui.components.EmptyBox
import com.example.servicehub.ui.components.ProviderBadges
import com.example.servicehub.ui.components.RatingLabel
import com.example.servicehub.ui.components.ScreenHeader
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class FavoritesViewModel : ViewModel() {
    private val favoritesHelper = FavoritesHelper(viewModelScope)
    val favoriteIds: StateFlow<Set<String>> = favoritesHelper.ids
    fun toggle(providerId: String) = favoritesHelper.toggle(providerId)

    /** The favourite professionals, live. Whenever the list of ids changes, listen again. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val providers: StateFlow<List<Provider>> = favoritesHelper.ids
        .map { it.sorted() }
        .flatMapLatest { ids -> ProviderRepository.liveProviders(ids) }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Used to pick which service to open when you tap a favourite. */
    val services: StateFlow<List<Service>> = ServiceRepository.allServices()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    companion object {
        /** The cheapest visible service in the provider's category (opened when you tap "Book"). */
        fun defaultServiceFor(provider: Provider, services: List<Service>): Service? =
            services.filter { it.isActive && it.categoryId == provider.categoryId }.minByOrNull { it.price }
    }
}

/** "My favourites": professionals the customer saved with the heart icon. */
@Composable
fun FavoritesScreen(
    onBack: () -> Unit,
    onOpenProvider: (providerId: String, serviceId: String) -> Unit,
    onExplore: () -> Unit,
    viewModel: FavoritesViewModel = viewModel(),
) {
    val providers by viewModel.providers.collectAsState()
    val ids by viewModel.favoriteIds.collectAsState()
    val services by viewModel.services.collectAsState()
    val shown = providers.filter { it.id in ids }.sortedBy { it.name }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("My favourites", onBack, subtitle = "Professionals you saved with the heart icon")
        if (shown.isEmpty()) {
            EmptyBox(
                title = "No favourites yet",
                message = "Tap the heart on a professional's profile to save them here for next time.",
                icon = Icons.Filled.FavoriteBorder,
                actionLabel = "Find professionals",
                onAction = onExplore,
            )
            return@Column
        }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(shown, key = { it.id }) { p ->
                val service = FavoritesViewModel.defaultServiceFor(p, services)
                val bookable = p.approvalStatus == ApprovalStatus.APPROVED && p.isAvailable && service != null
                AppCard(onClick = if (service != null) ({ onOpenProvider(p.id, service.id) }) else null) {
                    Row(verticalAlignment = Alignment.Top) {
                        Avatar(p.name, 52.dp)
                        Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(p.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                FavoriteButton(isFavorite = true, onToggle = { viewModel.toggle(p.id) })
                            }
                            ProviderBadges(p)
                            Caption("${FixedData.categoryById(p.categoryId)?.name.orEmpty()} · ${p.area}")
                            RatingLabel(p.ratingAvg, p.ratingCount)
                            if (!bookable) Caption("Not available for booking right now", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    if (bookable && service != null) {
                        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.End) {
                            Button(onClick = { onOpenProvider(p.id, service.id) }) { Text("Book") }
                        }
                    }
                }
            }
        }
    }
}
