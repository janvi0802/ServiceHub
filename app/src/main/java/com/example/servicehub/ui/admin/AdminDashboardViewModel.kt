package com.example.servicehub.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.servicehub.data.repository.AdminRepository
import com.example.servicehub.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SampleDataState(
    val alreadyLoaded: Boolean = false,
    val isLoading: Boolean = false,
    val message: String? = null,
)

class AdminDashboardViewModel : ViewModel() {

    /** Live number of providers waiting for approval. */
    val pendingCount: StateFlow<Int> = AdminRepository.pendingProviders()
        .map { it.size }
        .catch { emit(0) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _sampleState = MutableStateFlow(SampleDataState())
    val sampleState: StateFlow<SampleDataState> = _sampleState

    init {
        viewModelScope.launch {
            val loaded = try { AdminRepository.hasServices() } catch (e: Exception) { false }
            _sampleState.update { it.copy(alreadyLoaded = loaded) }
        }
    }

    fun loadSampleData() {
        _sampleState.update { it.copy(isLoading = true, message = null) }
        viewModelScope.launch {
            try {
                AdminRepository.loadSampleData()
                _sampleState.update {
                    it.copy(isLoading = false, alreadyLoaded = true, message = "Sample services and providers added ✓")
                }
            } catch (e: Exception) {
                _sampleState.update { it.copy(isLoading = false, message = "Failed: ${e.message}") }
            }
        }
    }

    fun logout() = AuthRepository.logout()
}
