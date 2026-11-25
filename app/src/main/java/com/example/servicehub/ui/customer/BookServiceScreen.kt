package com.example.servicehub.ui.customer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.servicehub.data.CategoryStore
import com.example.servicehub.data.FixedData
import com.example.servicehub.data.model.Service
import com.example.servicehub.data.repository.ServiceRepository
import com.example.servicehub.ui.components.AppCard
import com.example.servicehub.ui.components.BottomActionBar
import com.example.servicehub.ui.components.Caption
import com.example.servicehub.ui.components.DropdownField
import com.example.servicehub.ui.components.InfoRow
import com.example.servicehub.ui.components.ScreenHeader
import com.example.servicehub.ui.components.rupees
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** All services customers can book (hidden ones left out), live. */
class BookServiceViewModel : ViewModel() {
    val services: StateFlow<List<Service>> = ServiceRepository.allServices()
        .map { list -> list.filter { it.isActive }.sortedBy { it.price } }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

/** "Service name · ₹149" — the text shown in the service dropdown. */
fun serviceOptionText(service: Service) = "${service.name} · ${rupees(service.price)}"

/**
 * "Book a service" from Home: a short guided form.
 * Step 1 choose the category, step 2 the service (price shown), then see professionals
 * (the provider list already starts with the customer's own area).
 */
@Composable
fun BookServiceScreen(
    onBack: () -> Unit,
    onChooseProfessional: (categoryId: String, serviceId: String) -> Unit,
    viewModel: BookServiceViewModel = viewModel(),
) {
    val allCategories by CategoryStore.all.collectAsState()
    val categories = allCategories.filter { it.isActive }
    val services by viewModel.services.collectAsState()

    var categoryId by rememberSaveable { mutableStateOf("") }
    var serviceId by rememberSaveable { mutableStateOf("") }

    val category = categories.firstOrNull { it.id == categoryId }
    val inCategory = services.filter { it.categoryId == categoryId }
    val service = inCategory.firstOrNull { it.id == serviceId }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Book a service", onBack, subtitle = "Choose what you need in 2 steps")
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("1. What do you need?", style = MaterialTheme.typography.titleSmall)
            DropdownField(
                label = "Category",
                options = categories.map { it.name },
                selected = category?.name.orEmpty(),
                onSelected = { name ->
                    categoryId = categories.firstOrNull { it.name == name }?.id.orEmpty()
                    serviceId = ""   // a new category means choosing its service again
                },
            )

            Text("2. Which service?", style = MaterialTheme.typography.titleSmall)
            if (category == null) {
                Caption("Choose a category first.")
            } else if (inCategory.isEmpty()) {
                Caption("No services in ${category.name} yet.")
            } else {
                DropdownField(
                    label = "Service",
                    options = inCategory.map(::serviceOptionText),
                    selected = service?.let(::serviceOptionText).orEmpty(),
                    onSelected = { text -> serviceId = inCategory.firstOrNull { serviceOptionText(it) == text }?.id.orEmpty() },
                )
            }

            // Short preview of the chosen service, so the price is clear before going on
            if (service != null) {
                AppCard {
                    Text(service.name, style = MaterialTheme.typography.titleMedium)
                    if (service.description.isNotBlank()) Caption(service.description)
                    InfoRow("Price", rupees(service.price))
                    InfoRow("Visit fee", rupees(FixedData.VISIT_FEE))
                    InfoRow("Total", rupees(service.price + FixedData.VISIT_FEE), bold = true)
                    InfoRow("Takes about", "${service.durationMins} min")
                }
                Caption("Next you choose a professional in your area, then the date and time.")
            }
        }
        BottomActionBar {
            Button(
                onClick = { service?.let { onChooseProfessional(it.categoryId, it.id) } },
                enabled = service != null,
                modifier = Modifier.weight(1f).height(50.dp),
            ) { Text("Choose professional") }
        }
    }
}
