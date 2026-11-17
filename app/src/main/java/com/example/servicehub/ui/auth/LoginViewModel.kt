package com.example.servicehub.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.servicehub.data.repository.AuthRepository
import com.example.servicehub.data.repository.HomeDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Everything the login screen shows. */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val info: String? = null,                    // e.g. "reset link sent"
    val destination: HomeDestination? = null,   // set when login succeeds
)

class LoginViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, error = null, info = null) }
    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, error = null, info = null) }

    /** Sends a password-reset email to the address typed in the Email box. */
    fun forgotPassword() {
        val email = _uiState.value.email.trim()
        if (!email.contains("@") || !email.contains(".")) {
            _uiState.update { it.copy(error = "Type your email above first, then tap \"Forgot password?\".", info = null) }
            return
        }
        _uiState.update { it.copy(isLoading = true, error = null, info = null) }
        viewModelScope.launch {
            try {
                AuthRepository.sendPasswordReset(email)
                // Same message whether or not the account exists (so nobody can check which emails are registered).
                _uiState.update {
                    it.copy(isLoading = false, info = "If an account exists for $email, a reset link has been sent. Check your inbox and spam.")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = AuthRepository.friendlyError(e)) }
            }
        }
    }

    fun login() {
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password

        // Check the form before asking Firebase
        if (email.isEmpty() || password.isEmpty()) {
            _uiState.update { it.copy(error = "Please enter email and password.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                AuthRepository.login(email, password)
                val destination = AuthRepository.findHomeDestination()
                _uiState.update { it.copy(isLoading = false, destination = destination) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = AuthRepository.friendlyError(e)) }
            }
        }
    }
}
