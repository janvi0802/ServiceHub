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
import com.example.servicehub.ui.components.DropdownField
import com.example.servicehub.ui.components.TextInput

/** Shows and edits the logged-in person's details. Same screen for every role. */
@Composable
fun ProfileScreen(
    onLoggedOut: () -> Unit,
    viewModel: ProfileViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Avatar(state.name, size = 80.dp)
        Text(state.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "${state.user?.email.orEmpty()} • ${state.user?.role.orEmpty().replaceFirstChar { it.uppercase() }}",
            style = MaterialTheme.typography.bodyMedium,
        )

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

        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

        Button(
            onClick = viewModel::save,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth().height(50.dp),
        ) {
            Text(if (state.isSaving) "Saving..." else "Save changes")
        }
        OutlinedButton(
            onClick = {
                viewModel.logout()
                onLoggedOut()
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
            Text("  Log out")
        }

        // App version, read from the installed app (versionName in app/build.gradle.kts)
        val context = LocalContext.current
        val version = remember {
            runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull()
        }
        Text("ServiceHub version ${version ?: "1.0"}", style = MaterialTheme.typography.labelSmall)
    }
}
