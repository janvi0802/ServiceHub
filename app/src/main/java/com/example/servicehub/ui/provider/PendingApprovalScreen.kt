package com.example.servicehub.ui.provider

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.servicehub.data.model.ApprovalStatus
import com.example.servicehub.data.repository.AuthRepository
import com.example.servicehub.data.repository.HomeDestination
import com.example.servicehub.data.repository.UserRepository
import com.example.servicehub.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class PendingUiState(
    val approvalStatus: String = ApprovalStatus.PENDING,
    val isChecking: Boolean = false,
    val approvedDestination: HomeDestination? = null,   // set once the admin approves
)

/** Checks the provider's approvalStatus when the screen opens and when "Check again" is tapped. */
class PendingApprovalViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PendingUiState())
    val uiState: StateFlow<PendingUiState> = _uiState

    init { checkAgain() }

    fun checkAgain() {
        val uid = AuthRepository.currentUserId ?: return
        _uiState.value = _uiState.value.copy(isChecking = true)
        viewModelScope.launch {
            val status = try {
                UserRepository.getProvider(uid)?.approvalStatus ?: ApprovalStatus.PENDING
            } catch (e: Exception) {
                _uiState.value.approvalStatus   // no internet: keep showing the old status
            }
            val destination = if (status == ApprovalStatus.APPROVED) {
                try { AuthRepository.findHomeDestination() } catch (e: Exception) { null }
            } else null
            _uiState.value = PendingUiState(status, isChecking = false, approvedDestination = destination)
        }
    }
}

/** Shown to providers until the admin approves them (or if the admin rejected them). */
@Composable
fun PendingApprovalScreen(
    onApproved: (HomeDestination) -> Unit,
    onLogout: () -> Unit,
    viewModel: PendingApprovalViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val isRejected = state.approvalStatus == ApprovalStatus.REJECTED

    LaunchedEffect(state.approvedDestination) {
        state.approvedDestination?.takeIf { it.route == Routes.PROVIDER_HOME }?.let(onApproved)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            if (isRejected) Icons.Filled.Block else Icons.Filled.HourglassTop,
            contentDescription = null,
            tint = if (isRejected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            if (isRejected) "Application rejected" else "Waiting for approval",
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (isRejected) "Sorry, the admin did not approve your provider account. " +
                "If you think this is a mistake, please contact the ServiceHub admin. " +
                "If they approve you later, tap \"Check again\"."
            else "Thanks for registering! The admin will check your details soon. " +
                "Once approved, customers can find and book you.",
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        // Also shown when rejected: the admin can still approve them later from "All providers".
        Button(onClick = viewModel::checkAgain, enabled = !state.isChecking) {
            Text(if (state.isChecking) "Checking..." else "Check again")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onLogout) { Text("Log out") }
    }
}
