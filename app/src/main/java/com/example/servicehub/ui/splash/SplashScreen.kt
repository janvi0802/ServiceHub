package com.example.servicehub.ui.splash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HomeRepairService
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.servicehub.data.repository.HomeDestination

/** First screen: shows the logo while we check the login, then moves on. */
@Composable
fun SplashScreen(
    onGoToLogin: () -> Unit,
    onGoToHome: (HomeDestination) -> Unit,
    viewModel: SplashViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    // When the state changes to a decision, navigate once.
    LaunchedEffect(state) {
        when (val s = state) {
            is SplashState.GoToLogin -> onGoToLogin()
            is SplashState.GoToHome -> onGoToHome(s.destination)
            SplashState.Checking -> Unit
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Filled.HomeRepairService,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(80.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text("ServiceHub", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Trusted home services near you", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(24.dp))
        CircularProgressIndicator()
    }
}
