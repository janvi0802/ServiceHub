package com.example.servicehub.ui.admin

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.servicehub.data.FixedData
import com.example.servicehub.data.model.ApprovalStatus
import com.example.servicehub.data.model.Booking
import com.example.servicehub.data.model.BookingStatus
import com.example.servicehub.data.model.Provider
import com.example.servicehub.data.model.Roles
import com.example.servicehub.data.model.Service
import com.example.servicehub.data.model.User
import com.example.servicehub.data.repository.AdminRepository
import com.example.servicehub.data.repository.ServiceRepository
import com.example.servicehub.ui.components.Avatar
import com.example.servicehub.ui.components.BookingCard
import com.example.servicehub.ui.components.EmptyBox
import com.example.servicehub.ui.components.LoadingBox
import com.example.servicehub.ui.components.ScreenHeader
import com.example.servicehub.ui.components.TextInput
import com.example.servicehub.ui.components.ratingText
import com.example.servicehub.ui.components.rupees
import com.example.servicehub.ui.theme.StatusCompleted
import com.example.servicehub.ui.theme.StatusRejected
import com.example.servicehub.ui.theme.StatusRequested
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * One ViewModel for the four admin control screens.
 * A Firestore listener only runs while a screen actually shows its list (WhileSubscribed).
 */
class AdminControlViewModel : ViewModel() {

    private fun <T> Flow<List<T>>.live(): StateFlow<List<T>?> =
        catch { emit(emptyList()) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val users = AdminRepository.allUsers().live()
    val providers = AdminRepository.allProviders().live()
    val services = ServiceRepository.allServices().live()
    val bookings = AdminRepository.allBookings().live()

    /** Error or success message shown at the bottom of the screen. */
    val message = MutableStateFlow<String?>(null)

    /** Runs one admin action and shows a message if it fails. */
    private fun act(successText: String, action: suspend () -> Unit) = viewModelScope.launch {
        message.value = try {
            action()
            successText
        } catch (e: Exception) {
            "Failed: check your internet and try again."
        }
    }

    fun setBlocked(user: User, blocked: Boolean) =
        act(if (blocked) "${user.name} blocked." else "${user.name} unblocked.") {
            AdminRepository.setBlocked(user.id, blocked)
        }

    fun setApproval(provider: Provider, status: String) =
        act("${provider.name}: ${status.lowercase()}.") { AdminRepository.setApproval(provider.id, status) }

    fun setServiceActive(service: Service, active: Boolean) =
        act(if (active) "${service.name} is visible." else "${service.name} is hidden.") {
            AdminRepository.setServiceActive(service.id, active)
        }

    fun updateService(service: Service, price: Int, description: String) =
        act("${service.name} updated.") { AdminRepository.updateService(service.id, price, description) }
}

// ===================================================================== shared bits

/** A row of filter chips that scrolls sideways. */
@Composable
private fun FilterRow(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            FilterChip(selected = option == selected, onClick = { onSelect(option) }, label = { Text(option) })
        }
    }
}

@Composable
private fun MessageLine(viewModel: AdminControlViewModel) {
    val message by viewModel.message.collectAsState()
    message?.let { Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 16.dp)) }
}

/** Shows loading / empty / the list, so every screen handles the three states the same way. */
@Composable
private fun <T> ListOrState(list: List<T>?, emptyText: String, content: @Composable (List<T>) -> Unit) {
    when {
        list == null -> LoadingBox()
        list.isEmpty() -> EmptyBox(emptyText)
        else -> content(list)
    }
}

// ===================================================================== Users (block / unblock)

@Composable
fun AdminUsersScreen(viewModel: AdminControlViewModel = viewModel()) {
    val users by viewModel.users.collectAsState()
    var filter by rememberSaveable { mutableStateOf("All") }
    var confirmUser by remember { mutableStateOf<User?>(null) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Users")
        FilterRow(listOf("All", "Customers", "Providers", "Blocked"), filter) { filter = it }
        MessageLine(viewModel)
        val shown = users?.filter {
            it.role != Roles.ADMIN && when (filter) {
                "Customers" -> it.role == Roles.CUSTOMER
                "Providers" -> it.role == Roles.PROVIDER
                "Blocked" -> it.isBlocked
                else -> true
            }
        }?.sortedBy { it.name.lowercase() }

        ListOrState(shown, "No users here.") { list ->
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(list, key = { it.id }) { user ->
                    UserCard(user, onToggleBlock = { confirmUser = user })
                }
            }
        }
    }

    // Ask before blocking/unblocking, so a mistaken tap does nothing
    confirmUser?.let { user ->
        val block = !user.isBlocked
        AlertDialog(
            onDismissRequest = { confirmUser = null },
            title = { Text(if (block) "Block ${user.name}?" else "Unblock ${user.name}?") },
            text = {
                Text(
                    if (block) "They will not be able to log in. Blocked providers are hidden from customers."
                    else "They will be able to log in again."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setBlocked(user, block)
                    confirmUser = null
                }) { Text(if (block) "Block" else "Unblock") }
            },
            dismissButton = { TextButton(onClick = { confirmUser = null }) { Text("Back") } },
        )
    }
}

@Composable
private fun UserCard(user: User, onToggleBlock: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Avatar(user.name)
            Column(Modifier.weight(1f)) {
                Text(user.name, fontWeight = FontWeight.Bold)
                Text("${user.role.replaceFirstChar { it.uppercase() }} • ${user.area}", style = MaterialTheme.typography.bodySmall)
                Text(user.email, style = MaterialTheme.typography.bodySmall)
                if (user.isBlocked) Text("BLOCKED", color = StatusRejected, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            }
            if (user.isBlocked) {
                Button(onClick = onToggleBlock) { Text("Unblock") }
            } else {
                OutlinedButton(onClick = onToggleBlock) { Text("Block", color = StatusRejected) }
            }
        }
    }
}

// ===================================================================== All providers

@Composable
fun AdminAllProvidersScreen(onBack: () -> Unit, viewModel: AdminControlViewModel = viewModel()) {
    val providers by viewModel.providers.collectAsState()
    var filter by rememberSaveable { mutableStateOf("All") }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("All providers", onBack)
        FilterRow(listOf("All", ApprovalStatus.APPROVED, ApprovalStatus.PENDING, ApprovalStatus.REJECTED), filter) { filter = it }
        MessageLine(viewModel)
        val shown = providers
            ?.filter { filter == "All" || it.approvalStatus == filter }
            ?.sortedBy { it.name.lowercase() }

        ListOrState(shown, "No providers here.") { list ->
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(list, key = { it.id }) { provider ->
                    ProviderAdminCard(
                        provider,
                        onApprove = { viewModel.setApproval(provider, ApprovalStatus.APPROVED) },
                        onReject = { viewModel.setApproval(provider, ApprovalStatus.REJECTED) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProviderAdminCard(provider: Provider, onApprove: () -> Unit, onReject: () -> Unit) {
    val statusColor = when (provider.approvalStatus) {
        ApprovalStatus.APPROVED -> StatusCompleted
        ApprovalStatus.REJECTED -> StatusRejected
        else -> StatusRequested
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Avatar(provider.name)
                Column(Modifier.weight(1f)) {
                    Text(provider.name, fontWeight = FontWeight.Bold)
                    Text(
                        "${FixedData.categoryById(provider.categoryId)?.name.orEmpty()} • ${provider.area} • ${provider.experienceYears} yrs",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text(provider.approvalStatus, color = statusColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            }
            Text(
                "${ratingText(provider.ratingAvg, provider.ratingCount)} • ${provider.jobsCompleted} jobs • ${rupees(provider.totalEarnings)} earned • " +
                    if (provider.isAvailable) "Available" else "Not available",
                style = MaterialTheme.typography.bodySmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (provider.approvalStatus != ApprovalStatus.APPROVED) Button(onClick = onApprove) { Text("Approve") }
                if (provider.approvalStatus != ApprovalStatus.REJECTED) OutlinedButton(onClick = onReject) { Text("Reject") }
            }
        }
    }
}

// ===================================================================== Manage services

@Composable
fun AdminServicesScreen(viewModel: AdminControlViewModel = viewModel()) {
    val services by viewModel.services.collectAsState()
    var editing by remember { mutableStateOf<Service?>(null) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Manage services")
        MessageLine(viewModel)
        ListOrState(services, "No services yet. Use \"Load sample data\" on the Dashboard.") { list ->
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Grouped by category, in the same order as the customer home screen
                FixedData.categories.forEach { category ->
                    val inCategory = list.filter { it.categoryId == category.id }.sortedBy { it.price }
                    if (inCategory.isNotEmpty()) {
                        item(key = "cat_${category.id}") {
                            Text(category.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        items(inCategory, key = { it.id }) { service ->
                            ServiceAdminCard(
                                service,
                                onActiveChange = { viewModel.setServiceActive(service, it) },
                                onEdit = { editing = service },
                            )
                        }
                    }
                }
            }
        }
    }

    editing?.let { service ->
        EditServiceDialog(
            service = service,
            onSave = { price, description ->
                viewModel.updateService(service, price, description)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun ServiceAdminCard(service: Service, onActiveChange: (Boolean) -> Unit, onEdit: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 14.dp, top = 8.dp, bottom = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(service.name, fontWeight = FontWeight.Bold)
                Text("${rupees(service.price)} • ~${service.durationMins} min", color = MaterialTheme.colorScheme.primary)
                Text(service.description, style = MaterialTheme.typography.bodySmall)
                Text(
                    if (service.isActive) "Visible to customers" else "Hidden",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (service.isActive) StatusCompleted else StatusRejected,
                )
            }
            Switch(checked = service.isActive, onCheckedChange = onActiveChange)
            IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Edit ${service.name}") }
        }
    }
}

/** Edit price and description. Price must be a whole number from 1 to 100000. */
@Composable
private fun EditServiceDialog(service: Service, onSave: (Int, String) -> Unit, onDismiss: () -> Unit) {
    var priceText by remember { mutableStateOf(service.price.toString()) }
    var description by remember { mutableStateOf(service.description) }
    val price = priceText.toIntOrNull()
    val valid = price != null && price in 1..100000 && description.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit ${service.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextInput(priceText, { priceText = it.filter(Char::isDigit).take(6) }, "Price (₹)", keyboardType = KeyboardType.Number)
                TextInput(description, { description = it.take(120) }, "Description", singleLine = false)
                if (!valid) Text("Enter a price from ₹1 to ₹1,00,000 and a description.", color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(price ?: 0, description.trim()) }, enabled = valid) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Back") } },
    )
}

// ===================================================================== All bookings

@Composable
fun AdminBookingsScreen(viewModel: AdminControlViewModel = viewModel()) {
    val bookings by viewModel.bookings.collectAsState()
    var filter by rememberSaveable { mutableStateOf("All") }
    val options = listOf("All") + BookingStatus.entries.map { it.label }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("All bookings")
        FilterRow(options, filter) { filter = it }
        val shown: List<Booking>? = bookings?.filter { filter == "All" || it.bookingStatus.label == filter }

        ListOrState(shown, "No bookings here.") { list ->
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Text("${list.size} booking(s)", style = MaterialTheme.typography.labelLarge) }
                items(list, key = { it.id }) { booking ->
                    BookingCard(booking, "${booking.customerName} → ${booking.providerName}", onClick = {})
                }
            }
        }
    }
}
