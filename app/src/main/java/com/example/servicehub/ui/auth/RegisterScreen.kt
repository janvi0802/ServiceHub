package com.example.servicehub.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.servicehub.data.CategoryStore
import com.example.servicehub.data.FixedData
import com.example.servicehub.data.model.Roles
import com.example.servicehub.data.repository.HomeDestination
import com.example.servicehub.ui.components.DropdownField
import com.example.servicehub.ui.components.TextInput
import com.example.servicehub.ui.theme.BrandOrange

/**
 * Registration in 2 steps:
 *  1. "How will you use ServiceHub?" — two big cards, nothing pre-selected.
 *  2. The form, with a clear title ("Create provider account") and a "Change" link.
 */
@Composable
fun RegisterScreen(
    onRegistered: (HomeDestination) -> Unit,
    onGoToLogin: () -> Unit,
    viewModel: RegisterViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()

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
        if (state.role == null) {
            ChooseRoleStep(onChoose = viewModel::onRoleChange)
        } else {
            RegisterFormStep(state, viewModel)
        }
        TextButton(onClick = onGoToLogin, modifier = Modifier.fillMaxWidth()) {
            Text("Already have an account? Log in")
        }
    }
}

// ----------------------------------------------------------------- step 1

@Composable
private fun ChooseRoleStep(onChoose: (String) -> Unit) {
    Text("Create account", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Text("How will you use ServiceHub?", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(4.dp))
    RoleCard(
        icon = Icons.Filled.Home,
        color = MaterialTheme.colorScheme.primary,
        title = "I need a service",
        subtitle = "Book trusted plumbers, electricians, cleaners and more at fixed prices.",
        onClick = { onChoose(Roles.CUSTOMER) },
    )
    RoleCard(
        icon = Icons.Filled.Handyman,
        color = BrandOrange,
        title = "I am a service provider",
        subtitle = "Get jobs in your area, build your rating and track your earnings.",
        onClick = { onChoose(Roles.PROVIDER) },
    )
}

@Composable
private fun RoleCard(icon: ImageVector, color: Color, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        border = BorderStroke(2.dp, color.copy(alpha = 0.4f)),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.06f)),
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(56.dp).background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(32.dp)) }
            Column(Modifier.padding(start = 16.dp).weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

// ----------------------------------------------------------------- step 2

@Composable
private fun RegisterFormStep(state: RegisterUiState, viewModel: RegisterViewModel) {
    val isProvider = state.role == Roles.PROVIDER
    val categories = CategoryStore.all.collectAsState().value.filter { it.isActive }   // live, incl. admin-added

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (isProvider) "Create provider account" else "Create customer account",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = { viewModel.onRoleChange(null) }) { Text("Change") }
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
        DropdownField("Service category", categories.map { it.name }, state.categoryName, viewModel::onCategoryChange)
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
        if (state.isLoading) {
            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
        } else {
            // Says exactly what will be created, e.g. "Register as provider (Painter)"
            Text(
                when {
                    !isProvider -> "Register as customer"
                    state.categoryName.isNotBlank() -> "Register as provider (${state.categoryName})"
                    else -> "Register as provider"
                }
            )
        }
    }
}
