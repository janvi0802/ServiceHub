package com.example.servicehub.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.servicehub.data.FixedData
import com.example.servicehub.data.model.Roles
import com.example.servicehub.data.repository.AuthRepository
import com.example.servicehub.data.repository.HomeDestination
import com.example.servicehub.data.repository.ProviderDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Everything the register form shows. Provider-only fields are used when role = provider. */
data class RegisterUiState(
    /** null = not chosen yet (step 1). Nothing is pre-selected, so nobody registers as the wrong type. */
    val role: String? = null,
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val password: String = "",
    val area: String = "",
    val address: String = "",
    // provider only
    val categoryName: String = "",
    val experienceYears: String = "",
    val bio: String = "",
    // screen status
    val isLoading: Boolean = false,
    val error: String? = null,
    val destination: HomeDestination? = null,
)

class RegisterViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState

    /** One small function per field: copy the state with the new value and clear old errors. */
    /** Step 1 -> step 2 (or back to step 1 with null, via "Change"). */
    fun onRoleChange(v: String?) = _uiState.update { it.copy(role = v, error = null) }
    fun onNameChange(v: String) = _uiState.update { it.copy(name = v, error = null) }
    fun onEmailChange(v: String) = _uiState.update { it.copy(email = v, error = null) }
    fun onPhoneChange(v: String) = _uiState.update { it.copy(phone = v.filter(Char::isDigit).take(10), error = null) }
    fun onPasswordChange(v: String) = _uiState.update { it.copy(password = v, error = null) }
    fun onAreaChange(v: String) = _uiState.update { it.copy(area = v, error = null) }
    fun onAddressChange(v: String) = _uiState.update { it.copy(address = v, error = null) }
    fun onCategoryChange(v: String) = _uiState.update { it.copy(categoryName = v, error = null) }
    fun onExperienceChange(v: String) = _uiState.update { it.copy(experienceYears = v.filter(Char::isDigit).take(2), error = null) }
    fun onBioChange(v: String) = _uiState.update { it.copy(bio = v, error = null) }

    /** Returns an error message if something is missing or wrong, or null if the form is OK. */
    private fun validate(s: RegisterUiState): String? = when {
        s.role == null -> "Please choose how you will use ServiceHub."
        s.name.isBlank() -> "Please enter your name."
        !s.email.contains("@") || !s.email.contains(".") -> "Please enter a valid email."
        s.phone.length != 10 -> "Phone number must be 10 digits."
        s.password.length < 6 -> "Password must be at least 6 characters."
        s.area.isBlank() -> "Please choose your area."
        s.role == Roles.CUSTOMER && s.address.isBlank() -> "Please enter your address."
        s.role == Roles.PROVIDER && s.categoryName.isBlank() -> "Please choose your service category."
        s.role == Roles.PROVIDER && s.experienceYears.isBlank() -> "Please enter years of experience."
        else -> null
    }

    fun register() {
        val s = _uiState.value
        validate(s)?.let { message ->
            _uiState.update { it.copy(error = message) }
            return
        }

        // Providers send extra details; customers send null.
        val providerDetails = if (s.role == Roles.PROVIDER) {
            // firstOrNull: the admin might have hidden the category a moment ago
            val category = FixedData.categories.firstOrNull { it.name == s.categoryName }
            if (category == null) {
                _uiState.update { it.copy(error = "Please choose your service category again.") }
                return
            }
            ProviderDetails(
                categoryId = category.id,
                experienceYears = s.experienceYears.toInt(),
                bio = s.bio.trim(),
            )
        } else null

        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                AuthRepository.register(
                    name = s.name.trim(), email = s.email.trim(), password = s.password,
                    phone = s.phone, area = s.area, address = s.address.trim(),
                    role = s.role ?: Roles.CUSTOMER, providerDetails = providerDetails,
                )
                // After registering, Firebase has already logged the person in.
                val destination = AuthRepository.findHomeDestination()
                _uiState.update { it.copy(isLoading = false, destination = destination) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = AuthRepository.friendlyError(e)) }
            }
        }
    }
}
