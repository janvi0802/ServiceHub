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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color
import com.example.servicehub.data.Category
import com.example.servicehub.data.CategoryIcons
import com.example.servicehub.data.CategoryStore
import com.example.servicehub.data.categoryColorChoices
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material3.HorizontalDivider
import com.example.servicehub.ui.components.AppCard
import com.example.servicehub.ui.components.Caption
import com.example.servicehub.ui.components.ConfirmDialog
import com.example.servicehub.ui.components.InfoRow
import com.example.servicehub.ui.components.SectionHeader
import com.example.servicehub.ui.components.StatTile
import com.example.servicehub.ui.components.StatusChip
import com.example.servicehub.ui.theme.BrandBlue
import com.example.servicehub.ui.theme.BrandOrange
import com.example.servicehub.ui.theme.StatusAccepted
import com.example.servicehub.data.DateUtils
import com.example.servicehub.data.model.Complaint
import com.example.servicehub.data.model.ComplaintStatus
import com.example.servicehub.data.repository.AdminRepository
import com.example.servicehub.data.repository.AuthRepository
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Password
import com.example.servicehub.data.repository.ComplaintRepository
import com.example.servicehub.ui.components.openDialer
import com.example.servicehub.data.repository.ServiceRepository
import com.example.servicehub.ui.components.Avatar
import com.example.servicehub.ui.components.BookingCard
import com.example.servicehub.ui.components.EmptyBox
import com.example.servicehub.ui.components.LoadingBox
import com.example.servicehub.ui.components.ScreenHeader
import com.example.servicehub.ui.components.DropdownField
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
    val complaints = ComplaintRepository.all().live()

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

    /**
     * Firebase emails the user a secure link to choose a new password.
     * (On the free plan nobody, not even the admin, can see or type someone else's password.)
     */
    fun sendPasswordReset(user: User) =
        act("Password reset link sent to ${user.email}.") { AuthRepository.sendPasswordReset(user.email) }

    /** Sets a new password through the admin server. The message shows the server's answer. */
    fun changePassword(user: User, newPassword: String) = viewModelScope.launch {
        message.value = try {
            AdminRepository.changeUserPassword(user.id, newPassword)
            "Password changed for ${user.name}. They have been signed out and notified."
        } catch (e: Exception) {
            "Not changed: ${e.message ?: "please try again."}"
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

    fun updateService(service: Service, price: Int, description: String, included: String, notIncluded: String) =
        act("${service.name} updated.") { AdminRepository.updateService(service.id, price, description, included, notIncluded) }

    fun addService(categoryId: String, name: String, description: String, price: Int, durationMins: Int) =
        act("$name added.") { AdminRepository.addService(categoryId, name, description, price, durationMins) }

    fun addCategory(name: String, iconKey: String, colorHex: Long) =
        act("$name added. Now add its services and let providers register.") {
            AdminRepository.addCategory(name, iconKey, colorHex)
        }

    fun setCategoryActive(category: Category, active: Boolean) =
        act(if (active) "${category.name} is visible." else "${category.name} is hidden (with its services).") {
            AdminRepository.setCategoryActive(category, active)
        }

    fun resolve(complaint: Complaint) =
        act("Marked as resolved. The customer has been told.") { ComplaintRepository.resolve(complaint) }
}

// ===================================================================== Categories

/** Built-in and admin-added categories: hide/show, and add new ones (e.g. Yoga Teacher). */
@Composable
fun AdminCategoriesScreen(onBack: () -> Unit, viewModel: AdminControlViewModel = viewModel()) {
    val categories by CategoryStore.all.collectAsState()
    val services by viewModel.services.collectAsState()
    var adding by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Categories", onBack) {
            TextButton(onClick = { adding = true }) { Text("+ Add") }
        }
        MessageLine(viewModel)
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Text(
                    "New category? Add it here, then add its services in Services. " +
                        "Providers can then register in it.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            items(categories, key = { it.id }) { c ->
                val serviceCount = services.orEmpty().count { it.categoryId == c.id }
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(44.dp).background(c.color.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) { Icon(c.icon, contentDescription = null, tint = c.color) }
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(c.name, fontWeight = FontWeight.Bold)
                            Text(
                                "$serviceCount service(s)" + if (FixedData.defaultCategories.any { it.id == c.id }) " • built-in" else " • added by admin",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            if (serviceCount == 0) {
                                Text("Add services so customers can book it", style = MaterialTheme.typography.labelSmall, color = StatusRequested)
                            }
                        }
                        Switch(checked = c.isActive, onCheckedChange = { viewModel.setCategoryActive(c, it) })
                    }
                }
            }
        }
    }

    if (adding) {
        AddCategoryDialog(
            existingNames = categories.map { it.name.lowercase() },
            onSave = { name, iconKey, color ->
                viewModel.addCategory(name, iconKey, color)
                adding = false
            },
            onDismiss = { adding = false },
        )
    }
}

/** Name + icon + colour for a new category. Names must be unique. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddCategoryDialog(
    existingNames: List<String>,
    onSave: (String, String, Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var iconKey by remember { mutableStateOf("other") }
    var color by remember { mutableStateOf(categoryColorChoices.first()) }
    val trimmed = name.trim()
    val duplicate = trimmed.lowercase() in existingNames
    val valid = trimmed.length >= 3 && !duplicate

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add category") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TextInput(name, { name = it.take(25) }, "Name, e.g. Yoga Teacher")
                if (duplicate) Text("A category with this name already exists.", color = MaterialTheme.colorScheme.error)
                Text("Icon", fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    CategoryIcons.all.forEach { (key, icon) ->
                        val selected = key == iconKey
                        Box(
                            Modifier
                                .size(40.dp)
                                .background(if (selected) Color(color).copy(alpha = 0.25f) else Color(0x11000000), CircleShape)
                                .clickable { iconKey = key },
                            contentAlignment = Alignment.Center,
                        ) { Icon(icon, contentDescription = key, tint = if (selected) Color(color) else Color.Gray) }
                    }
                }
                Text("Colour", fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    categoryColorChoices.forEach { c ->
                        Box(
                            Modifier
                                .size(if (c == color) 34.dp else 28.dp)
                                .background(Color(c), CircleShape)
                                .clickable { color = c }
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(trimmed, iconKey, color) }, enabled = valid) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Back") } },
    )
}

// ===================================================================== Complaints

@Composable
fun AdminComplaintsScreen(onBack: () -> Unit, viewModel: AdminControlViewModel = viewModel()) {
    val complaints by viewModel.complaints.collectAsState()
    var filter by rememberSaveable { mutableStateOf("Open") }
    val context = LocalContext.current

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Complaints", onBack)
        FilterRow(listOf("Open", "Resolved", "All"), filter) { filter = it }
        MessageLine(viewModel)
        val shown = complaints?.filter {
            when (filter) {
                "Open" -> it.status == ComplaintStatus.OPEN
                "Resolved" -> it.status == ComplaintStatus.RESOLVED
                else -> true
            }
        }
        ListOrState(shown, if (filter == "Open") "No open complaints. Everything is fine." else "Nothing here.") { list ->
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(list, key = { it.id }) { c ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(c.serviceName, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Text(
                                    if (c.status == ComplaintStatus.OPEN) "OPEN" else "RESOLVED",
                                    color = if (c.status == ComplaintStatus.OPEN) StatusRejected else StatusCompleted,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                            Text("${c.customerName} → ${c.providerName}", style = MaterialTheme.typography.bodySmall)
                            Text("\"${c.message}\"", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Reported ${DateUtils.dateTime(c.createdAt)} • Booking ${c.bookingId.take(8).uppercase()}",
                                style = MaterialTheme.typography.labelSmall,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { openDialer(context, c.customerPhone) }) { Text("Call customer") }
                                OutlinedButton(onClick = { openDialer(context, c.providerPhone) }) { Text("Call provider") }
                            }
                            if (c.status == ComplaintStatus.OPEN) {
                                Button(onClick = { viewModel.resolve(c) }) { Text("Mark resolved") }
                            }
                        }
                    }
                }
            }
        }
    }
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
fun AdminUsersScreen(onOpenUser: (String) -> Unit, viewModel: AdminControlViewModel = viewModel()) {
    val users by viewModel.users.collectAsState()
    var filter by rememberSaveable { mutableStateOf("All") }
    var search by rememberSaveable { mutableStateOf("") }
    var confirmUser by remember { mutableStateOf<User?>(null) }
    var resetUser by remember { mutableStateOf<User?>(null) }
    val providers by viewModel.providers.collectAsState()
    val categories by CategoryStore.all.collectAsState()
    var categoryName by rememberSaveable { mutableStateOf(ALL_CATEGORIES) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Users", subtitle = "${users.orEmpty().count { it.role != Roles.ADMIN }} people · tap one for details")
        TextInput(
            search, { search = it }, "Search name, email or phone",
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        // Category filter: picking one shows only PROVIDERS who work in that category
        DropdownField(
            "Category", listOf(ALL_CATEGORIES) + categories.map { it.name }, categoryName, { categoryName = it },
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
        )
        FilterRow(listOf("All", "Customers", "Providers", "Blocked"), filter) { filter = it }
        MessageLine(viewModel)
        val categoryId = categories.firstOrNull { it.name == categoryName }?.id
        val idsInCategory = providerIdsInCategory(providers.orEmpty(), categoryId)
        val shown = users?.filter {
            it.role != Roles.ADMIN && userMatches(it, search.trim()) &&
                (idsInCategory == null || it.id in idsInCategory) &&
                when (filter) {
                    "Customers" -> it.role == Roles.CUSTOMER
                    "Providers" -> it.role == Roles.PROVIDER
                    "Blocked" -> it.isBlocked
                    else -> true
                }
        }?.sortedBy { it.name.lowercase() }

        ListOrState(shown, "No users here.") { list ->
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(list, key = { it.id }) { user ->
                    UserCard(user, onClick = { onOpenUser(user.id) }, onResetPassword = { resetUser = user })
                }
            }
        }
    }

    resetUser?.let { user ->
        PasswordResetDialog(user, onConfirm = {
            viewModel.sendPasswordReset(user)
            resetUser = null
        }, onDismiss = { resetUser = null })
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

/** Text of the "no category filter" option in the category dropdowns. */
const val ALL_CATEGORIES = "All categories"

/**
 * Ids of providers working in [categoryId], or null when no category is chosen (= show everyone).
 * Users don't store a category; providers do, so we look it up in the providers list.
 */
fun providerIdsInCategory(providers: List<Provider>, categoryId: String?): Set<String>? =
    categoryId?.let { id -> providers.filter { it.categoryId == id }.map { it.id }.toSet() }

/** Sample providers have made-up emails (no real inbox), so a reset email would go nowhere. */
fun canSendPasswordReset(user: User): Boolean =
    user.email.contains("@") && !user.email.endsWith("@sample.servicehub")

/** Empty search matches everyone; otherwise name, email or phone must contain the text. */
fun userMatches(user: User, text: String): Boolean =
    text.isEmpty() ||
        user.name.contains(text, ignoreCase = true) ||
        user.email.contains(text, ignoreCase = true) ||
        user.phone.contains(text)

@Composable
private fun UserCard(user: User, onClick: () -> Unit, onResetPassword: () -> Unit) {
    AppCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Avatar(user.name)
            Column(Modifier.weight(1f)) {
                Text(user.name, style = MaterialTheme.typography.titleSmall)
                Caption(user.email)
                Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RolePill(user.role)
                    if (user.isBlocked) StatusChip("Blocked", StatusRejected)
                }
            }
            // Quick action: email this user a password reset link
            if (canSendPasswordReset(user)) {
                IconButton(onClick = onResetPassword) {
                    Icon(Icons.Filled.LockReset, contentDescription = "Send password reset to ${user.name}",
                        tint = MaterialTheme.colorScheme.primary)
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * Admin types the new password twice. Rules (also checked again on the server):
 * 6–64 characters, both boxes must match.
 */
@Composable
private fun ChangePasswordDialog(user: User, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var first by remember { mutableStateOf("") }
    var second by remember { mutableStateOf("") }
    val problem = when {
        first.isEmpty() -> null
        first.length < 6 -> "At least 6 characters."
        first.length > 64 -> "At most 64 characters."
        second.isNotEmpty() && second != first -> "The two passwords do not match."
        else -> null
    }
    val valid = first.length in 6..64 && first == second

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change ${user.name}'s password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "${user.name} will be signed out on all phones and must log in with this new password. " +
                        "Tell them the new password privately (for example in person or by phone).",
                    style = MaterialTheme.typography.bodySmall,
                )
                TextInput(first, { first = it }, "New password", isPassword = true)
                TextInput(second, { second = it }, "Type it again", isPassword = true)
                problem?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(first) }, enabled = valid) { Text("Change") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Back") } },
    )
}

/** "Send a reset link to …?" — explains what will happen before sending. */
@Composable
private fun PasswordResetDialog(user: User, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    ConfirmDialog(
        title = "Reset ${user.name}'s password?",
        message = "Firebase will email a secure link to ${user.email}. They open it and choose a new password. " +
            "For safety, nobody (including the admin) can see or type another person's password.",
        confirmText = "Send link",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

/** "Customer" in blue, "Provider" in orange. */
@Composable
private fun RolePill(role: String) {
    if (role == Roles.PROVIDER) StatusChip("Provider", BrandOrange) else StatusChip("Customer", BrandBlue)
}

// ===================================================================== User detail

/**
 * Everything about one user for the admin: contact, role, bookings, money, and block/unblock.
 * Totals: money SPENT for customers, money EARNED for providers (completed bookings only).
 */
@Composable
fun AdminUserDetailScreen(userId: String, onBack: () -> Unit, viewModel: AdminControlViewModel = viewModel()) {
    val users by viewModel.users.collectAsState()
    val bookings by viewModel.bookings.collectAsState()
    val providers by viewModel.providers.collectAsState()
    val context = LocalContext.current
    var confirm by rememberSaveable { mutableStateOf(false) }
    var showReset by rememberSaveable { mutableStateOf(false) }
    var showChangePassword by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("User details", onBack)
        val user = users?.firstOrNull { it.id == userId }
        if (user == null) {
            if (users == null) LoadingBox() else EmptyBox("User not found.")
            return@Column
        }
        val isProvider = user.role == Roles.PROVIDER
        val theirs = bookings.orEmpty().filter { if (isProvider) it.providerId == userId else it.customerId == userId }
        val completed = theirs.filter { it.bookingStatus == BookingStatus.COMPLETED }
        val provider = providers?.firstOrNull { it.id == userId }

        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(user.name, 56.dp)
                        Column(Modifier.weight(1f).padding(start = 14.dp)) {
                            Text(user.name, style = MaterialTheme.typography.titleLarge)
                            Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                RolePill(user.role)
                                if (user.isBlocked) StatusChip("Blocked", StatusRejected)
                            }
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    InfoRow("Email", user.email)
                    InfoRow("Phone", user.phone)
                    InfoRow("Area", user.area.ifBlank { "-" })
                    if (user.address.isNotBlank()) InfoRow("Address", user.address)
                    if (user.createdAt > 0) InfoRow("Joined", DateUtils.dateTime(user.createdAt))
                    if (provider != null) {
                        InfoRow("Category", FixedData.categoryById(provider.categoryId)?.name.orEmpty())
                        InfoRow("Approval", provider.approvalStatus)
                        InfoRow("Rating", ratingText(provider.ratingAvg, provider.ratingCount))
                    }
                    Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { openDialer(context, user.phone) }) { Text("Call") }
                        if (canSendPasswordReset(user)) {
                            OutlinedButton(onClick = { showReset = true }) { Text("Reset link") }
                        }
                        if (user.isBlocked) {
                            Button(onClick = { confirm = true }) { Text("Unblock") }
                        } else {
                            OutlinedButton(onClick = { confirm = true }) { Text("Block", color = StatusRejected) }
                        }
                    }
                    // Set a new password directly (needs the admin server; sample providers have no login)
                    if (user.role != Roles.ADMIN && canSendPasswordReset(user)) {
                        Button(
                            onClick = { showChangePassword = true },
                            enabled = AdminRepository.canChangePasswords,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        ) {
                            Icon(Icons.Filled.Password, contentDescription = null)
                            Text("  Change password")
                        }
                        if (!AdminRepository.canChangePasswords) {
                            Caption("Set up the admin server first (see admin-server/README.md).")
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile("Bookings", "${theirs.size}", Icons.AutoMirrored.Filled.EventNote, StatusAccepted, Modifier.weight(1f))
                    StatTile(
                        if (isProvider) "Earned" else "Spent",
                        rupees(completed.sumOf { it.totalAmount }),
                        Icons.Filled.CurrencyRupee, StatusCompleted, Modifier.weight(1f),
                    )
                }
            }
            item { MessageLine(viewModel) }
            item { SectionHeader("Booking history") }
            if (theirs.isEmpty()) item { Caption("No bookings yet.") }
            items(theirs, key = { it.id }) { b ->
                BookingCard(b, if (isProvider) "Customer: ${b.customerName}" else "with ${b.providerName}", onClick = {})
            }
        }

        if (showChangePassword) {
            ChangePasswordDialog(
                user = user,
                onSave = { newPassword ->
                    viewModel.changePassword(user, newPassword)
                    showChangePassword = false
                },
                onDismiss = { showChangePassword = false },
            )
        }
        if (showReset) {
            PasswordResetDialog(user, onConfirm = {
                viewModel.sendPasswordReset(user)
                showReset = false
            }, onDismiss = { showReset = false })
        }
        if (confirm) {
            val block = !user.isBlocked
            ConfirmDialog(
                title = if (block) "Block ${user.name}?" else "Unblock ${user.name}?",
                message = if (block) "They will be logged out and cannot log in. Blocked providers are hidden from customers."
                else "They will be able to log in again.",
                confirmText = if (block) "Block" else "Unblock",
                onConfirm = {
                    viewModel.setBlocked(user, block)
                    confirm = false
                },
                onDismiss = { confirm = false },
            )
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
fun AdminServicesScreen(onOpenCategories: () -> Unit, onOpenCoupons: () -> Unit, viewModel: AdminControlViewModel = viewModel()) {
    val services by viewModel.services.collectAsState()
    val allCategories by CategoryStore.all.collectAsState()
    var editing by remember { mutableStateOf<Service?>(null) }
    var adding by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Manage services") {
            TextButton(onClick = onOpenCategories) { Text("Categories") }
            TextButton(onClick = onOpenCoupons) { Text("Coupons") }
            TextButton(onClick = { adding = true }) { Text("+ Add") }
        }
        MessageLine(viewModel)
        ListOrState(services, "No services yet. Use \"Load sample data\" on the Dashboard.") { list ->
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Grouped by category (hidden categories too, so the admin can still edit them)
                allCategories.forEach { category ->
                    val inCategory = list.filter { it.categoryId == category.id }.sortedBy { it.price }
                    if (inCategory.isNotEmpty()) {
                        item(key = "cat_${category.id}") {
                            Text(
                                category.name + if (!category.isActive) " (hidden)" else "",
                                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                            )
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

    if (adding) {
        AddServiceDialog(
            onSave = { categoryId, name, description, price, minutes ->
                viewModel.addService(categoryId, name, description, price, minutes)
                adding = false
            },
            onDismiss = { adding = false },
        )
    }

    editing?.let { service ->
        EditServiceDialog(
            service = service,
            onSave = { price, description, included, notIncluded ->
                viewModel.updateService(service, price, description, included, notIncluded)
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
private fun EditServiceDialog(service: Service, onSave: (Int, String, String, String) -> Unit, onDismiss: () -> Unit) {
    var priceText by remember { mutableStateOf(service.price.toString()) }
    var description by remember { mutableStateOf(service.description) }
    var included by remember { mutableStateOf(service.included) }
    var notIncluded by remember { mutableStateOf(service.notIncluded) }
    val price = priceText.toIntOrNull()
    val valid = price != null && price in 1..100000 && description.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit ${service.name}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextInput(priceText, { priceText = it.filter(Char::isDigit).take(6) }, "Price (₹)", keyboardType = KeyboardType.Number)
                TextInput(description, { description = it.take(120) }, "Description", singleLine = false)
                // Shown on the customer's service details page (one point per line)
                TextInput(included, { included = it.take(400) }, "What's included (one per line)", singleLine = false)
                TextInput(notIncluded, { notIncluded = it.take(300) }, "Not included (one per line)", singleLine = false)
                if (!valid) Text("Enter a price from ₹1 to ₹1,00,000 and a description.", color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(price ?: 0, description.trim(), included.trim(), notIncluded.trim()) },
                enabled = valid,
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Back") } },
    )
}

/** New service: category, name, description, price (₹1–1,00,000) and duration (5–600 minutes). */
@Composable
private fun AddServiceDialog(onSave: (String, String, String, Int, Int) -> Unit, onDismiss: () -> Unit) {
    val categories = CategoryStore.all.collectAsState().value   // includes admin-added ones
    var categoryName by remember { mutableStateOf(categories.first().name) }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var minutesText by remember { mutableStateOf("") }
    val price = priceText.toIntOrNull()
    val minutes = minutesText.toIntOrNull()
    val valid = name.isNotBlank() && description.isNotBlank() &&
        price != null && price in 1..100000 && minutes != null && minutes in 5..600

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add service") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DropdownField("Category", categories.map { it.name }, categoryName, { categoryName = it })
                TextInput(name, { name = it.take(40) }, "Name, e.g. Geyser repair")
                TextInput(description, { description = it.take(120) }, "Short description", singleLine = false)
                TextInput(priceText, { priceText = it.filter(Char::isDigit).take(6) }, "Price (₹)", keyboardType = KeyboardType.Number)
                TextInput(minutesText, { minutesText = it.filter(Char::isDigit).take(3) }, "Duration (minutes)", keyboardType = KeyboardType.Number)
                if (!valid) Text("Fill every field. Price ₹1–1,00,000, duration 5–600 min.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val categoryId = categories.first { it.name == categoryName }.id
                    onSave(categoryId, name.trim(), description.trim(), price ?: 0, minutes ?: 0)
                },
                enabled = valid,
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Back") } },
    )
}

// ===================================================================== All bookings

/**
 * Lets the dashboard open "All bookings" with a filter already chosen
 * (e.g. tapping "2 job redo requests" shows only redo requests).
 */
object AdminBookingsFilter {
    const val REDO = "Redo"
    const val UNPAID = "Unpaid"
    const val EXPIRED = "Expired"
    /** Set just before opening the screen; the screen uses it once and clears it. */
    var next: String? = null
}

@Composable
fun AdminBookingsScreen(onOpenBooking: (String) -> Unit, viewModel: AdminControlViewModel = viewModel()) {
    val bookings by viewModel.bookings.collectAsState()
    var filter by rememberSaveable { mutableStateOf("All") }
    LaunchedEffect(Unit) {
        AdminBookingsFilter.next?.let { filter = it }
        AdminBookingsFilter.next = null
    }
    val options = listOf("All") + BookingStatus.entries.map { it.label } +
        listOf(AdminBookingsFilter.EXPIRED, AdminBookingsFilter.REDO, AdminBookingsFilter.UNPAID)
    val categories by CategoryStore.all.collectAsState()
    var categoryName by rememberSaveable { mutableStateOf(ALL_CATEGORIES) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("All bookings")
        DropdownField(
            "Category", listOf(ALL_CATEGORIES) + categories.map { it.name }, categoryName, { categoryName = it },
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
        )
        FilterRow(options, filter) { filter = it }
        val categoryId = categories.firstOrNull { it.name == categoryName }?.id
        val shown: List<Booking>? = bookings?.filter {
            (categoryId == null || it.categoryId == categoryId) &&
                when (filter) {
                    "All" -> true
                    AdminBookingsFilter.EXPIRED -> it.isExpired
                    AdminBookingsFilter.REDO -> it.revisitStatus == com.example.servicehub.data.model.RevisitStatus.REQUESTED
                    AdminBookingsFilter.UNPAID -> it.bookingStatus == BookingStatus.COMPLETED &&
                        it.paymentStatus != com.example.servicehub.data.model.PaymentStatus.PAID
                    else -> it.bookingStatus.label == filter && !it.isExpired   // "Requested" = still waiting
                }
        }

        ListOrState(shown, "No bookings match these filters.") { list ->
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Text(
                        "${list.size} booking(s) · ${rupees(list.filter { it.bookingStatus == BookingStatus.COMPLETED }.sumOf { it.totalAmount })} completed value",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                items(list, key = { it.id }) { booking ->
                    BookingCard(booking, "${booking.customerName} → ${booking.providerName}", onClick = { onOpenBooking(booking.id) })
                }
            }
        }
    }
}
