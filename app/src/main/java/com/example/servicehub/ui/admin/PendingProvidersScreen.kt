package com.example.servicehub.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.servicehub.data.FixedData
import com.example.servicehub.data.model.ApprovalStatus
import com.example.servicehub.data.model.Provider
import com.example.servicehub.data.repository.AdminRepository
import com.example.servicehub.ui.components.Avatar
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PendingProvidersViewModel : ViewModel() {

    /** null = still loading; otherwise the live list. */
    val providers: StateFlow<List<Provider>?> = AdminRepository.pendingProviders()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun approve(id: String) = viewModelScope.launch {
        runCatching { AdminRepository.setApproval(id, ApprovalStatus.APPROVED) }
    }

    fun reject(id: String) = viewModelScope.launch {
        runCatching { AdminRepository.setApproval(id, ApprovalStatus.REJECTED) }
    }
}

/** Live list of new providers. Approve/Reject removes them from the list instantly. */
@Composable
fun PendingProvidersScreen(viewModel: PendingProvidersViewModel = viewModel()) {
    val providers by viewModel.providers.collectAsState()

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text(
            "Pending providers",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 16.dp),
        )
        val list = providers
        when {
            list == null -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            list.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("No providers waiting. All caught up! ✓")
            }
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(list, key = { it.id }) { provider ->
                    PendingProviderCard(
                        provider,
                        onApprove = { viewModel.approve(provider.id) },
                        onReject = { viewModel.reject(provider.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PendingProviderCard(provider: Provider, onApprove: () -> Unit, onReject: () -> Unit) {
    val categoryName = FixedData.categoryById(provider.categoryId)?.name ?: provider.categoryId
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Avatar(provider.name)
                Column {
                    Text(provider.name, style = MaterialTheme.typography.titleMedium)
                    Text("$categoryName • ${provider.area} • ${provider.experienceYears} yrs")
                    Text("📞 ${provider.phone}", style = MaterialTheme.typography.bodySmall)
                }
            }
            if (provider.bio.isNotBlank()) Text(provider.bio, style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onApprove) { Text("Approve") }
                OutlinedButton(onClick = onReject) { Text("Reject") }
            }
        }
    }
}
