package com.example.servicehub.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.servicehub.data.FixedData
import com.example.servicehub.data.model.Roles
import com.example.servicehub.data.repository.HomeDestination
import com.example.servicehub.ui.components.DropdownField
import com.example.servicehub.ui.components.TextInput

@Composable
fun RegisterScreen(
    onRegistered: (HomeDestination) -> Unit,
    onGoToLogin: () -> Unit,
    viewModel: RegisterViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val isProvider = state.role == Roles.PROVIDER

    LaunchedEffect(state.destination) {
        state.destination?.let(onRegistered)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Create account", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        // Customer or Provider (admin is never offered here)
        Text("I want to", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !isProvider,
                onClick = { viewModel.onRoleChange(Roles.CUSTOMER) },
                label = { Text("Book services") },
            )
            FilterChip(
                selected = isProvider,
                onClick = { viewModel.onRoleChange(Roles.PROVIDER) },
                label = { Text("Offer services") },
            )
        }

        TextInput(state.name, viewModel::onNameChange, "Full name")
        TextInput(state.email, viewModel::onEmailChange, "Email", keyboardType = KeyboardType.Email)
        TextInput(state.phone, viewModel::onPhoneChange, "Phone (10 digits)", keyboardType = KeyboardType.Phone)
        TextInput(state.password, viewModel::onPasswordChange, "Password (min 6 characters)", isPassword = true)
        DropdownField("Area", FixedData.areas, state.area, viewModel::onAreaChange)
        TextInput(
            state.address, viewModel::onAddressChange,
            if (isProvider) "Address (optional)" else "Address",
            singleLine = false,
        )

        // Extra fields only for providers
        if (isProvider) {
            Text("Your work", style = MaterialTheme.typography.titleMedium)
            DropdownField(
                "Service category",
                FixedData.categories.map { it.name },
                state.categoryName,
                viewModel::onCategoryChange,
            )
            TextInput(
                state.experienceYears, viewModel::onExperienceChange,
                "Years of experience", keyboardType = KeyboardType.Number,
            )
            TextInput(state.bio, viewModel::onBioChange, "Short bio (about your work)", singleLine = false)
            Text(
                "Provider accounts must be approved by the admin before customers can see you.",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Spacer(Modifier.height(4.dp))
        Button(
            onClick = viewModel::register,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth().height(50.dp),
        ) {
            if (state.isLoading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            else Text("Register")
        }
        TextButton(onClick = onGoToLogin, modifier = Modifier.fillMaxWidth()) {
            Text("Already have an account? Log in")
        }
    }
}
