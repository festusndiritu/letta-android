package dev.mizzenmast.letta.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.mizzenmast.letta.data.repository.AuthRepository
import dev.mizzenmast.letta.data.repository.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val phoneNumber: String = "",
    // Held in memory between OtpScreen and DisplayNameScreen for new users.
    // Never persisted — if the app is killed the user just re-verifies.
    val setupToken: String? = null,
)

sealed class AuthEvent {
    data object OtpSent : AuthEvent()
    data object Verified : AuthEvent()   // existing user — go to app
    data object NewUser : AuthEvent()    // new user — go to DisplayNameScreen
    data object Complete : AuthEvent()   // profile set — go to app
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _events = MutableStateFlow<AuthEvent?>(null)
    val events: StateFlow<AuthEvent?> = _events.asStateFlow()

    fun isLoggedIn(): Boolean = authRepository.isLoggedIn()

    fun requestOtp(phoneNumber: String) {
        _uiState.update { it.copy(isLoading = true, error = null, phoneNumber = phoneNumber) }
        viewModelScope.launch {
            authRepository.requestOtp(phoneNumber)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.value = AuthEvent.OtpSent
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message ?: "Failed to send OTP.")
                    }
                }
        }
    }

    /** Re-sends an OTP for the phone number already in state. */
    fun resendOtp() {
        val phoneNumber = _uiState.value.phoneNumber
        if (phoneNumber.isBlank()) return
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            authRepository.requestOtp(phoneNumber)
                .onSuccess { _uiState.update { it.copy(isLoading = false) } }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message ?: "Failed to resend OTP.")
                    }
                }
        }
    }

    fun verifyOtp(code: String) {
        val phoneNumber = _uiState.value.phoneNumber
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = authRepository.verifyOtp(phoneNumber, code)) {
                is AuthResult.ExistingUser -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.value = AuthEvent.Verified
                }
                is AuthResult.NewUser -> {
                    // Store the setup token so completeProfile can use it
                    _uiState.update { it.copy(isLoading = false, setupToken = result.setupToken) }
                    _events.value = AuthEvent.NewUser
                }
                is AuthResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    /**
     * Called from DisplayNameScreen. POSTs to /auth/complete-profile with the
     * setup token + display name. Avatar is set later via profile settings.
     */
    fun completeProfile(displayName: String) {
        val setupToken = _uiState.value.setupToken ?: run {
            _uiState.update { it.copy(error = "Session expired. Please verify your number again.") }
            return
        }
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            authRepository.completeProfile(
                setupToken = setupToken,
                displayName = displayName,
                avatarUrl = null,  // set post-login via profile settings
            )
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, setupToken = null) }
                    _events.value = AuthEvent.Complete
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message ?: "Failed to save profile.")
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun consumeEvent() {
        _events.value = null
    }
}