package com.example.servicehub.ui.provider

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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.servicehub.data.CategoryStore
import com.example.servicehub.data.model.ApprovalStatus
import com.example.servicehub.data.repository.AuthRepository
import com.example.servicehub.data.repository.HomeDestination
import com.example.servicehub.data.repository.ProviderDetails
import com.example.servicehub.data.repository.UserRepository
import com.example.servicehub.navigation.Routes
import com.example.servicehub.ui.components.DropdownField
import com.example.servicehub.ui.components.TextInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class PendingUiState(
    val approvalStatus: String = ApprovalStatus.PENDING,
    /** True if this provider account has NO provider details at all (they must fill them in). */
    val detailsMissing: Boolean = false,
    val isChecking: Boolean = false,
    val error: String? = null,
    val approvedDestination: HomeDestination? = null,   // set once the admin approves
)

/** Checks the provider's approvalStatus when the screen opens and when "Check again" is tapped. */
class PendingApprovalViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PendingUiState())
    val uiState: StateFlow<PendingUiState> = _uiState

    init { checkAgain() }

    fun checkAgain() {
        val uid = AuthRepository.currentUserId ?: return
        _uiState.value = _uiState.value.copy(isChecking = true, error = null)
        viewModelScope.launch {
            val provider = try {
                UserRepository.getProvider(uid)
            } catch (e: Exception) {
                // No internet: keep showing the old status
                _uiState.value = _uiState.value.copy(isChecking = false, error = "No internet. Try again.")
                return@launch
            }
            if (provider == null) {
                _uiState.value = PendingUiState(detailsMissing = true)
                return@launch
            }
            val destination = if (provider.approvalStatus == ApprovalStatus.APPROVED) {
                try { AuthRepository.findHomeDestination() } catch (e: Exception) { null }
            } else null
            _uiState.value = PendingUiState(provider.approvalStatus, approvedDestination = destination)
        }
    }

    /** Saves the missing provider details, then shows the normal "Waiting for approval". */
    fun saveMissingDetails(categoryName: String, experience: String, bio: String) {
        val category = CategoryStore.active().firstOrNull { it.name == categoryName }
        val years = experience.toIntOrNull()
        if (category == null || years == null) {
            _uiState.value = _uiState.value.copy(error = "Please choose a category and enter years of experience.")
            return
        }
        _uiState.value = _uiState.value.copy(isChecking = true, error = null)
        viewModelScope.launch {
            try {
                AuthRepository.createMissingProviderDetails(ProviderDetails(category.id, years, bio.trim()))
                checkAgain()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isChecking = false, error = "Could not save. Check internet.")
            }
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
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (state.detailsMissing) {
            MissingDetailsForm(state, viewModel)
        } else {
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
        }
        state.error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onLogout) { Text("Log out") }
    }
}

/** For provider accounts with no provider details: fill them in once, then wait for approval. */
@Composable
private fun MissingDetailsForm(state: PendingUiState, viewModel: PendingApprovalViewModel) {
    val categories = CategoryStore.all.collectAsState().value.filter { it.isActive }
    var categoryName by rememberSaveable { mutableStateOf("") }
    var experience by rememberSaveable { mutableStateOf("") }
    var bio by rememberSaveable { mutableStateOf("") }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
        Text("Finish your provider profile", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Your account is a provider account, but your work details are missing. " +
                "Fill them in so the admin can review and approve you.",
            textAlign = TextAlign.Center,
        )
        DropdownField("Service category", categories.map { it.name }, categoryName, { categoryName = it })
        TextInput(experience, { experience = it.filter(Char::isDigit).take(2) }, "Years of experience", keyboardType = KeyboardType.Number)
        TextInput(bio, { bio = it.take(300) }, "Short bio (about your work)", singleLine = false)
        Button(
            onClick = { viewModel.saveMissingDetails(categoryName, experience, bio) },
            enabled = !state.isChecking,
            modifier = Modifier.fillMaxWidth().height(50.dp),
        ) { Text(if (state.isChecking) "Saving..." else "Send for approval") }
    }
}
