package com.example.servicehub.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.servicehub.ui.theme.BrandBlue
import com.example.servicehub.ui.theme.BrandBlueDark
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HomeRepairService
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.servicehub.data.repository.HomeDestination
import com.example.servicehub.ui.components.TextInput

@Composable
fun LoginScreen(
    onLoggedIn: (HomeDestination) -> Unit,
    onGoToRegister: () -> Unit,
    viewModel: LoginViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    // Login succeeded -> leave this screen
    LaunchedEffect(state.destination) {
        state.destination?.let(onLoggedIn)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Brand logo: white icon in a blue gradient circle (same colours as the app icon)
        Box(
            Modifier
                .size(96.dp)
                .background(Brush.linearGradient(listOf(BrandBlue, BrandBlueDark)), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.HomeRepairService, contentDescription = null, tint = Color.White, modifier = Modifier.size(52.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text("Welcome to ServiceHub", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Log in to continue", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(24.dp))

        TextInput(state.email, viewModel::onEmailChange, "Email", keyboardType = KeyboardType.Email)
        Spacer(Modifier.height(12.dp))
        TextInput(state.password, viewModel::onPasswordChange, "Password", isPassword = true)
        TextButton(onClick = viewModel::forgotPassword, enabled = !state.isLoading, modifier = Modifier.align(Alignment.End)) {
            Text("Forgot password?")
        }

        state.error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        state.info?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = viewModel::login,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth().height(50.dp),
        ) {
            if (state.isLoading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            else Text("Log in")
        }
        TextButton(onClick = onGoToRegister) {
            Text("New here? Create an account")
        }
    }
}
