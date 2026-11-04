package com.example.servicehub.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.servicehub.data.model.User
import com.example.servicehub.data.repository.AuthRepository
import com.example.servicehub.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val user: User? = null,          // as loaded from Firestore
    val name: String = "",           // the editable copies below
    val phone: String = "",
    val area: String = "",
    val address: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val message: String? = null,     // "Saved" or an error
)

class ProfileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    init { load() }

    private fun load() {
        val uid = AuthRepository.currentUserId ?: return
        viewModelScope.launch {
            try {
                val user = UserRepository.getUser(uid)
                _uiState.update {
                    it.copy(
                        user = user, isLoading = false,
                        name = user?.name.orEmpty(), phone = user?.phone.orEmpty(),
                        area = user?.area.orEmpty(), address = user?.address.orEmpty(),
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, message = "Could not load profile. Check internet.") }
            }
        }
    }

    fun onNameChange(v: String) = _uiState.update { it.copy(name = v, message = null) }
    fun onPhoneChange(v: String) = _uiState.update { it.copy(phone = v.filter(Char::isDigit).take(10), message = null) }
    fun onAreaChange(v: String) = _uiState.update { it.copy(area = v, message = null) }
    fun onAddressChange(v: String) = _uiState.update { it.copy(address = v, message = null) }

    fun save() {
        val s = _uiState.value
        val user = s.user ?: return
        when {
            s.name.isBlank() -> { _uiState.update { it.copy(message = "Name cannot be empty.") }; return }
            s.phone.length != 10 -> { _uiState.update { it.copy(message = "Phone number must be 10 digits.") }; return }
        }
        _uiState.update { it.copy(isSaving = true, message = null) }
        viewModelScope.launch {
            try {
                UserRepository.updateProfile(user, s.name.trim(), s.phone, s.area, s.address.trim())
                _uiState.update { it.copy(isSaving = false, message = "Profile saved ✓") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, message = "Could not save. Check internet.") }
            }
        }
    }

    fun logout() = AuthRepository.logout()
}
