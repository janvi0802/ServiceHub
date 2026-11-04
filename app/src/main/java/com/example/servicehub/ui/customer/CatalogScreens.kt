package com.example.servicehub.ui.customer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.servicehub.data.FixedData
import com.example.servicehub.data.model.Service
import com.example.servicehub.data.repository.ServiceRepository
import com.example.servicehub.ui.components.EmptyBox
import com.example.servicehub.ui.components.LoadingBox
import com.example.servicehub.ui.components.ScreenHeader
import com.example.servicehub.ui.components.rupees
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Gives screens the live list of services customers may see (hidden ones removed).
 * null = still loading.
 */
class CatalogViewModel : ViewModel() {
    val activeServices: StateFlow<List<Service>?> = ServiceRepository.allServices()
        .map { list -> list.filter { it.isActive }.sortedBy { it.price } }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}

/** Services of one category, with price and duration. */
@Composable
fun CategoryServicesScreen(
    categoryId: String,
    onBack: () -> Unit,
    onServiceClick: (Service) -> Unit,
    viewModel: CatalogViewModel = viewModel(),
) {
    val all by viewModel.activeServices.collectAsState()
    val categoryName = FixedData.categoryById(categoryId)?.name ?: "Services"

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(categoryName, onBack)
        val services = all?.filter { it.categoryId == categoryId }
        when {
            services == null -> LoadingBox()
            services.isEmpty() -> EmptyBox("No services yet. (Admin: use \"Load sample data\".)")
            else -> ServiceList(services, showCategory = false, onServiceClick)
        }
    }
}

/** Search services by name across all categories. */
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onServiceClick: (Service) -> Unit,
    viewModel: CatalogViewModel = viewModel(),
) {
    val all by viewModel.activeServices.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }

    // Open the keyboard straight away
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Search", onBack)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("e.g. tap, fan, AC") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .focusRequester(focusRequester),
        )
        val text = query.trim()
        val results = all?.filter { text.isNotEmpty() && it.name.contains(text, ignoreCase = true) }
        when {
            all == null -> LoadingBox()
            text.isEmpty() -> EmptyBox("Type a service name to search.")
            results.isNullOrEmpty() -> EmptyBox("No services found for \"$text\".")
            else -> ServiceList(results, showCategory = true, onServiceClick)
        }
    }
}

@Composable
private fun ServiceList(services: List<Service>, showCategory: Boolean, onServiceClick: (Service) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(services, key = { it.id }) { service ->
            ServiceCard(service, showCategory, onClick = { onServiceClick(service) })
        }
    }
}

@Composable
private fun ServiceCard(service: Service, showCategory: Boolean, onClick: () -> Unit) {
    val category = FixedData.categoryById(service.categoryId)
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (category != null) {
                Icon(category.icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(service.name, style = MaterialTheme.typography.titleMedium)
                if (showCategory && category != null) {
                    Text(category.name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                Text(service.description, style = MaterialTheme.typography.bodySmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Schedule, null, modifier = Modifier.size(14.dp))
                    Text(" ~${service.durationMins} min", style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(rupees(service.price), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}
