package com.example.servicehub.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.servicehub.data.repository.AuthRepository
import com.example.servicehub.data.repository.HomeDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** What the splash screen has decided. */
sealed class SplashState {
    data object Checking : SplashState()
    data object GoToLogin : SplashState()
    data class GoToHome(val destination: HomeDestination) : SplashState()
}

/**
 * Runs once when the app opens:
 * is someone already logged in? If yes, find their home screen.
 */
class SplashViewModel : ViewModel() {

    private val _state = MutableStateFlow<SplashState>(SplashState.Checking)
    val state: StateFlow<SplashState> = _state

    init {
        viewModelScope.launch {
            if (AuthRepository.currentUserId == null) {
                _state.value = SplashState.GoToLogin
                return@launch
            }
            _state.value = try {
                SplashState.GoToHome(AuthRepository.findHomeDestination())
            } catch (e: Exception) {
                // e.g. blocked or no internet: send to login, where they can try again
                SplashState.GoToLogin
            }
        }
    }
}
