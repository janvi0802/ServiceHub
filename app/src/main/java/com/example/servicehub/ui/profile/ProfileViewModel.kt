package com.example.servicehub.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.servicehub.data.model.Roles
import com.example.servicehub.data.model.SavedAddress
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
    val bio: String = "",            // providers only
    val experienceYears: String = "",// providers only (text while typing)
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val message: String? = null,     // "Saved" or an error
) {
    val isProvider: Boolean get() = user?.role == Roles.PROVIDER
}

class ProfileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    init { load() }

    private fun load() {
        val uid = AuthRepository.currentUserId ?: return
        viewModelScope.launch {
            try {
                val user = UserRepository.getUser(uid)
                // Providers also have a bio and experience in providers/{uid}
                val provider = if (user?.role == Roles.PROVIDER) UserRepository.getProvider(uid) else null
                _uiState.update {
                    it.copy(
                        user = user, isLoading = false,
                        name = user?.name.orEmpty(), phone = user?.phone.orEmpty(),
                        area = user?.area.orEmpty(), address = user?.address.orEmpty(),
                        bio = provider?.bio.orEmpty(),
                        experienceYears = provider?.experienceYears?.toString().orEmpty(),
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
    fun onBioChange(v: String) = _uiState.update { it.copy(bio = v.take(300), message = null) }
    fun onExperienceChange(v: String) = _uiState.update { it.copy(experienceYears = v.filter(Char::isDigit).take(2), message = null) }

    fun save() {
        val s = _uiState.value
        val user = s.user ?: return
        val problem = when {
            s.name.isBlank() -> "Name cannot be empty."
            s.phone.length != 10 -> "Phone number must be 10 digits."
            s.isProvider && s.experienceYears.isBlank() -> "Please enter years of experience."
            else -> null
        }
        if (problem != null) {
            _uiState.update { it.copy(message = problem) }
            return
        }
        _uiState.update { it.copy(isSaving = true, message = null) }
        viewModelScope.launch {
            try {
                UserRepository.updateProfile(
                    user, s.name.trim(), s.phone, s.area, s.address.trim(),
                    providerBio = s.bio.trim(),
                    providerExperienceYears = s.experienceYears.toIntOrNull() ?: 0,
                )
                _uiState.update { it.copy(isSaving = false, message = "Profile saved ✓") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, message = "Could not save. Check internet.") }
            }
        }
    }

    /** Adds a saved address (e.g. "Work") and saves the list at once. */
    fun addSavedAddress(label: String, address: String) {
        val user = _uiState.value.user ?: return
        if (label.isBlank() || address.isBlank()) return
        saveAddresses(user.savedAddresses + SavedAddress(label.trim(), address.trim()), "Address added ✓")
    }

    fun removeSavedAddress(index: Int) {
        val user = _uiState.value.user ?: return
        saveAddresses(user.savedAddresses.filterIndexed { i, _ -> i != index }, "Address removed")
    }

    private fun saveAddresses(list: List<SavedAddress>, successText: String) = viewModelScope.launch {
        val user = _uiState.value.user ?: return@launch
        try {
            UserRepository.setSavedAddresses(user.id, list)
            _uiState.update { it.copy(user = user.copy(savedAddresses = list), message = successText) }
        } catch (e: Exception) {
            _uiState.update { it.copy(message = "Could not save the address. Check internet.") }
        }
    }

    fun logout() = AuthRepository.logout()
}
