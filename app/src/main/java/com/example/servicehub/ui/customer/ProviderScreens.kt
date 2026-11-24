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
import com.example.servicehub.data.model.isOffOn
import com.example.servicehub.data.model.nextWorkingDay
import com.example.servicehub.ui.theme.StatusCompleted
import com.example.servicehub.ui.theme.StatusRequested
import com.example.servicehub.data.repository.AuthRepository
import com.example.servicehub.data.repository.ProviderRepository
import com.example.servicehub.data.repository.UserRepository
import com.example.servicehub.data.repository.ServiceRepository
import com.example.servicehub.ui.components.Avatar
import com.example.servicehub.ui.components.EmptyBox
import com.example.servicehub.ui.components.LoadingBox
import com.example.servicehub.ui.components.ProviderBadges
import com.example.servicehub.ui.components.StarRow
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import kotlinx.coroutines.flow.map
import com.example.servicehub.ui.components.AppCard
import com.example.servicehub.ui.components.BottomActionBar
import com.example.servicehub.ui.components.Caption
import com.example.servicehub.ui.components.CardTitle
import com.example.servicehub.ui.components.RatingLabel
import com.example.servicehub.ui.components.SectionHeader
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Icon
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

/** How the customer wants providers ordered. */
enum class ProviderSort(val label: String) { RATING("Rating"), EXPERIENCE("Experience"), JOBS("Jobs done") }

/** Best first for the chosen sort; ties are broken by rating, then by name. */
fun sortProviders(list: List<Provider>, sort: ProviderSort): List<Provider> {
    val primary: Comparator<Provider> = when (sort) {
        ProviderSort.RATING -> compareByDescending<Provider> { it.ratingAvg }.thenByDescending { it.ratingCount }
        ProviderSort.EXPERIENCE -> compareByDescending { it.experienceYears }
        ProviderSort.JOBS -> compareByDescending { it.jobsCompleted }
    }
    return list.sortedWith(primary.thenByDescending { it.ratingAvg }.thenBy { it.name })
}

// ===================================================================== Provider list

/**
 * Reads categoryId and serviceId from the route (SavedStateHandle holds the {values} of the route).
 */
class ProviderListViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    private val categoryId: String = savedStateHandle["categoryId"] ?: ""
    val serviceId: String = savedStateHandle["serviceId"] ?: ""

    private val favoritesHelper = FavoritesHelper(viewModelScope)
    val favorites: StateFlow<Set<String>> = favoritesHelper.ids
    fun toggleFavorite(providerId: String) = favoritesHelper.toggle(providerId)

    val selectedArea = MutableStateFlow(ALL_AREAS)
    val selectedSort = MutableStateFlow(ProviderSort.RATING)
    /** "Available today" chip, for urgent needs: hides providers whose weekly day off is today. */
    val onlyToday = MutableStateFlow(false)
    val service = MutableStateFlow<Service?>(null)

    /** Bookable providers of this category, filtered by area (and today) and sorted. null = loading. */
    val providers: StateFlow<List<Provider>?> =
        combine(ProviderRepository.bookableProviders(categoryId), selectedArea, selectedSort, onlyToday) { list, area, sort, today ->
            val inArea = if (area == ALL_AREAS) list else list.filter { it.area == area }
            val shown = if (today) inArea.filter { !isOffOn(it, DateUtils.today()) } else inArea
            sortProviders(shown, sort)
        }
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        viewModelScope.launch { service.value = runCatching { ServiceRepository.getService(serviceId) }.getOrNull() }
        // Start with the customer's own saved area (they can still tap "All areas").
        viewModelScope.launch {
            val myArea = runCatching {
                UserRepository.getUser(AuthRepository.currentUserId.orEmpty())?.area
            }.getOrNull()
            if (!myArea.isNullOrBlank() && myArea in FixedData.areas) selectedArea.value = myArea
        }
    }
}

@Composable
fun ProviderListScreen(
    onBack: () -> Unit,
    onProviderClick: (providerId: String, serviceId: String) -> Unit,
    onBook: (providerId: String, serviceId: String) -> Unit,
    viewModel: ProviderListViewModel = viewModel(),
) {
    val favorites by viewModel.favorites.collectAsState()
    val providers by viewModel.providers.collectAsState()
    val area by viewModel.selectedArea.collectAsState()
    val sort by viewModel.selectedSort.collectAsState()
    val onlyToday by viewModel.onlyToday.collectAsState()
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
        // Sort chips (scroll sideways on small phones)
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = onlyToday,
                onClick = { viewModel.onlyToday.value = !onlyToday },
                label = { Text("Available today") },
                leadingIcon = { Icon(Icons.Filled.Bolt, contentDescription = null, modifier = Modifier.size(16.dp)) },
            )
            Text("Sort:", style = MaterialTheme.typography.labelLarge)
            ProviderSort.entries.forEach { option ->
                FilterChip(
                    selected = option == sort,
                    onClick = { viewModel.selectedSort.value = option },
                    label = { Text(option.label) },
                )
            }
        }
        val list = providers
        when {
            list == null -> LoadingBox()
            list.isEmpty() -> Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    when {
                        area != ALL_AREAS -> "No providers available in $area."
                        onlyToday -> "No providers working today."
                        else -> "No providers available right now."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
                // One tap to widen the search instead of a dead end
                if (area != ALL_AREAS || onlyToday) {
                    Button(
                        onClick = {
                            viewModel.selectedArea.value = ALL_AREAS
                            viewModel.onlyToday.value = false
                        },
                        modifier = Modifier.padding(top = 12.dp),
                    ) { Text("Show all providers") }
                }
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(list, key = { it.id }) { provider ->
                    ProviderCard(
                        provider,
                        isFavorite = provider.id in favorites,
                        onClick = { onProviderClick(provider.id, viewModel.serviceId) },
                        onBook = { onBook(provider.id, viewModel.serviceId) },
                        onToggleFavorite = { viewModel.toggleFavorite(provider.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProviderCard(
    provider: Provider,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onBook: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    AppCard(onClick = onClick) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Avatar(provider.name, 52.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(provider.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    FavoriteButton(isFavorite, onToggleFavorite)
                }
                ProviderBadges(provider)
                if (isOffOn(provider, DateUtils.today())) {
                    val next = nextWorkingDay(provider, DateUtils.nextDays(7))
                    Text(
                        "Off today" + (next?.let { " · next available ${DateUtils.chipLabel(it)}" } ?: ""),
                        style = MaterialTheme.typography.labelSmall, color = StatusRequested,
                    )
                } else {
                    Text("Available today", style = MaterialTheme.typography.labelSmall, color = StatusCompleted)
                }
                Text(
                    "${provider.experienceYears} yrs experience · ${provider.area} · ${provider.jobsCompleted} jobs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                RatingLabel(provider.ratingAvg, provider.ratingCount)
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.End) {
            OutlinedButton(onClick = onClick) { Text("View profile") }
            Button(onClick = onBook, enabled = provider.isAvailable, modifier = Modifier.padding(start = 8.dp)) { Text("Book") }
        }
    }
}

/** Heart icon: filled red when this professional is in the customer's favourites. */
@Composable
fun FavoriteButton(isFavorite: Boolean, onToggle: () -> Unit) {
    IconButton(onClick = onToggle, modifier = Modifier.size(36.dp)) {
        Icon(
            if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            contentDescription = if (isFavorite) "Remove from favourites" else "Add to favourites",
            tint = if (isFavorite) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The logged-in customer's favourite provider ids, live, plus a toggle.
 * Shared by the provider list, provider profile and the Favourites screen.
 */
class FavoritesHelper(private val scope: kotlinx.coroutines.CoroutineScope) {
    private val uid = AuthRepository.currentUserId.orEmpty()

    val ids: StateFlow<Set<String>> = UserRepository.liveUser(uid)
        .map { it?.favoriteProviderIds.orEmpty().toSet() }
        .catch { emit(emptySet()) }
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun toggle(providerId: String) = scope.launch {
        runCatching { UserRepository.setFavorite(uid, providerId, providerId !in ids.value) }
    }
}

// ===================================================================== Provider detail

class ProviderDetailViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    val providerId: String = savedStateHandle["providerId"] ?: ""
    val serviceId: String = savedStateHandle["serviceId"] ?: ""

    private val favoritesHelper = FavoritesHelper(viewModelScope)
    val favorites: StateFlow<Set<String>> = favoritesHelper.ids
    fun toggleFavorite() = favoritesHelper.toggle(providerId)

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
    val favorites by viewModel.favorites.collectAsState()

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Professional", onBack) {
            FavoriteButton(viewModel.providerId in favorites, viewModel::toggleFavorite)
        }
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
                AppCard {
                    CardTitle("About")
                    Text(
                        p.bio.ifBlank { "${p.name} has not written a bio yet." },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    InfoLine(Icons.Filled.LocationOn, "Works in ${p.area}")
                    InfoLine(
                        Icons.Filled.EventBusy,
                        if (p.offDays.isEmpty()) "Works all 7 days" else "Weekly off: ${DateUtils.weekdayNames(p.offDays)}",
                    )
                    InfoLine(Icons.Filled.Verified, "Approved by the ServiceHub team")
                }
            }
            item {
                SectionHeader("Reviews (${reviews.size})")
            }
            if (reviews.isEmpty()) {
                item { Caption("No reviews yet. Be the first to book and rate ${p.name.substringBefore(" ")}.") }
            } else {
                item { RatingSummary(reviews) }
            }
            items(reviews, key = { it.id }) { review -> ReviewItem(review) }
        }

        // Fixed bottom bar: the chosen service and price, and the Book button
        val s = service
        BottomActionBar {
            Column(Modifier.weight(1f)) {
                Text(s?.name ?: "Loading...", style = MaterialTheme.typography.titleSmall, maxLines = 1)
                if (s != null) {
                    Text(rupees(s.price) + " + ₹${FixedData.VISIT_FEE} visit", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Button(
                onClick = { onBook(p.id, viewModel.serviceId) },
                enabled = s != null && p.isAvailable,
                modifier = Modifier.height(50.dp),
            ) { Text(if (p.isAvailable) "Book now" else "Not available") }
        }
    }
}

/** Big centred avatar, name, badges, and three stats (Airbnb-style profile header). */
@Composable
private fun ProviderHeader(p: Provider) {
    AppCard {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Avatar(p.name, 84.dp)
            Text(p.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 10.dp))
            Text(
                FixedData.categoryById(p.categoryId)?.name.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Box(Modifier.padding(top = 8.dp)) { ProviderBadges(p) }
        }
        HorizontalDivider(Modifier.padding(vertical = 14.dp), color = MaterialTheme.colorScheme.outlineVariant)
        Row(Modifier.fillMaxWidth()) {
            HeaderStat(if (p.ratingCount == 0) "New" else "%.1f".format(p.ratingAvg), "Rating", Modifier.weight(1f))
            HeaderStat("${p.experienceYears} yrs", "Experience", Modifier.weight(1f))
            HeaderStat("${p.jobsCompleted}", "Jobs done", Modifier.weight(1f))
        }
    }
}

@Composable
private fun HeaderStat(value: String, label: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Caption(label)
    }
}

@Composable
private fun InfoLine(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Text("  $text", style = MaterialTheme.typography.bodyMedium)
    }
}

/** Average stars and how many reviews gave each star (5 down to 1), as thin bars. */
@Composable
private fun RatingSummary(reviews: List<Review>) {
    val average = reviews.map { it.rating }.average()
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(end = 20.dp)) {
                Text("%.1f".format(average), style = MaterialTheme.typography.headlineMedium)
                StarRow(average.roundToInt())
                Caption("${reviews.size} reviews")
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                (5 downTo 1).forEach { star ->
                    val count = reviews.count { it.rating == star }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Caption("$star", modifier = Modifier.width(12.dp))
                        LinearProgressIndicator(
                            progress = { count.toFloat() / reviews.size },
                            modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFFF5A623),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            drawStopIndicator = {},
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewItem(review: Review) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(review.customerName, 36.dp)
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(review.customerName, style = MaterialTheme.typography.titleSmall)
                Caption(DateUtils.dateTime(review.createdAt))
            }
            StarRow(review.rating)
        }
        if (review.comment.isNotBlank()) {
            Text(review.comment, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
        }
    }
}
