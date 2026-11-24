package com.example.servicehub.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.servicehub.data.FixedData
import com.example.servicehub.ui.components.Avatar
import com.example.servicehub.data.DateUtils
import com.example.servicehub.ui.components.AppCard
import com.example.servicehub.ui.components.Caption
import com.example.servicehub.ui.components.ConfirmDialog
import com.example.servicehub.ui.components.ListRow
import com.example.servicehub.ui.components.ScreenHeader
import com.example.servicehub.ui.components.SectionHeader
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.servicehub.ui.components.DropdownField
import com.example.servicehub.ui.components.TextInput
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton

/** Label (Home / Work / Other) + address. Saving needs both. */
@Composable
private fun AddAddressDialog(onSave: (String, String) -> Unit, onDismiss: () -> Unit) {
    var label by remember { mutableStateOf("Home") }
    var custom by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    val finalLabel = if (label == "Other") custom.trim() else label
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add address") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Home", "Work", "Other").forEach { option ->
                        FilterChip(selected = option == label, onClick = { label = option }, label = { Text(option) })
                    }
                }
                if (label == "Other") TextInput(custom, { custom = it.take(20) }, "Name, e.g. Parents")
                TextInput(address, { address = it.take(200) }, "Full address", singleLine = false)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(finalLabel, address) },
                enabled = finalLabel.isNotBlank() && address.isNotBlank(),
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Back") } },
    )
}

/** Shows and edits the logged-in person's details. Same screen for every role. */
@Composable
fun ProfileScreen(
    onLoggedOut: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenHelp: () -> Unit,
    viewModel: ProfileViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    var showLogout by remember { mutableStateOf(false) }
    var showAddAddress by remember { mutableStateOf(false) }
    val context = LocalContext.current
    // App version, read from the installed app (versionName in app/build.gradle.kts)
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull()
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Profile")
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Who you are
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(state.name, size = 64.dp)
                    Column(Modifier.weight(1f).padding(start = 14.dp)) {
                        Text(state.name.ifBlank { "Your name" }, style = MaterialTheme.typography.titleLarge)
                        Caption(state.user?.email.orEmpty())
                        Text(
                            state.user?.role.orEmpty().replaceFirstChar { it.uppercase() } +
                                (state.user?.createdAt?.takeIf { it > 0 }?.let { " · member since ${DateUtils.dateTime(it).substringBefore(",")}" } ?: ""),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }

            // Editable details
            SectionHeader("Personal details")
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextInput(state.name, viewModel::onNameChange, "Full name")
                    TextInput(state.phone, viewModel::onPhoneChange, "Phone", keyboardType = KeyboardType.Phone)
                    DropdownField("Area", FixedData.areas, state.area, viewModel::onAreaChange)
                    TextInput(state.address, viewModel::onAddressChange, "Address (used for bookings)", singleLine = false)
                    // Extra fields that customers see on the provider's profile
                    if (state.isProvider) {
                        TextInput(
                            state.experienceYears, viewModel::onExperienceChange,
                            "Years of experience", keyboardType = KeyboardType.Number,
                        )
                        TextInput(state.bio, viewModel::onBioChange, "Short bio (shown to customers)", singleLine = false)
                    }
                    state.message?.let { Caption(it, color = MaterialTheme.colorScheme.primary) }
                    Button(
                        onClick = viewModel::save,
                        enabled = !state.isSaving,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                    ) { Text(if (state.isSaving) "Saving..." else "Save changes") }
                }
            }

            // Saved addresses (customers): pick them in the booking form with one tap
            if (!state.isProvider) {
                SectionHeader("Saved addresses", actionLabel = "+ Add", onAction = { showAddAddress = true })
                AppCard {
                    val saved = state.user?.savedAddresses.orEmpty()
                    if (saved.isEmpty()) {
                        Caption("Add Home, Work or any other place you book services for.")
                    }
                    saved.forEachIndexed { index, a ->
                        ListRow(
                            icon = if (a.label.equals("Work", true)) Icons.Filled.Work else Icons.Filled.Home,
                            title = a.label,
                            subtitle = a.address,
                            trailing = {
                                IconButton(onClick = { viewModel.removeSavedAddress(index) }) {
                                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Remove ${a.label}")
                                }
                            },
                        )
                    }
                }
            }

            // Help, written for this role
            SectionHeader("Help & safety")
            AppCard {
                if (state.isProvider) {
                    ListRow(Icons.Filled.Pin, "Start jobs with the PIN", "Ask the customer for their 4-digit PIN when you arrive.")
                    ListRow(Icons.Filled.EventBusy, "Days off", "Set weekly days off on your Home screen. Booked jobs are never cancelled automatically.")
                    ListRow(Icons.Filled.Payments, "Getting paid", "Collect Cash or UPI after the job, then tap Complete.")
                } else {
                    ListRow(Icons.Filled.Pin, "Share your PIN only on arrival", "The professional needs your 4-digit PIN to start the job.")
                    ListRow(Icons.Filled.Payments, "Pay after the service", "Pay by Cash or UPI only when the job is done.")
                    ListRow(Icons.Filled.ReportProblem, "Something went wrong?", "Open the booking and tap \"Report a problem\".")
                }
            }

            // Account
            AppCard {
                if (!state.isProvider) {
                    ListRow(Icons.Filled.Favorite, "My favourites", "Professionals you saved", onClick = onOpenFavorites)
                }
                ListRow(Icons.AutoMirrored.Filled.HelpOutline, "Help & support", "Answers to common questions", onClick = onOpenHelp)
                ListRow(
                    Icons.AutoMirrored.Filled.Logout, "Log out", null,
                    color = MaterialTheme.colorScheme.error,
                    onClick = { showLogout = true },
                )
            }
            Caption("ServiceHub version ${version ?: "1.0"}", modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }

    if (showAddAddress) {
        AddAddressDialog(
            onSave = { label, address ->
                viewModel.addSavedAddress(label, address)
                showAddAddress = false
            },
            onDismiss = { showAddAddress = false },
        )
    }
    if (showLogout) {
        ConfirmDialog(
            title = "Log out?",
            message = "You can log in again any time with your email and password.",
            confirmText = "Log out",
            onConfirm = {
                showLogout = false
                viewModel.logout()
                onLoggedOut()
            },
            onDismiss = { showLogout = false },
        )
    }
}
