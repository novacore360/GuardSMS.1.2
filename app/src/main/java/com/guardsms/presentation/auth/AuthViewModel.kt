package com.guardsms.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guardsms.data.repository.GuardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: GuardRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    fun signIn(email: String, password: String) {
        if (!validateInputs(email, password)) return
        viewModelScope.launch {
            _state.value = AuthState(isLoading = true)
            repository.signIn(email, password)
                .onSuccess { _state.value = AuthState(isSuccess = true) }
                .onFailure { _state.value = AuthState(errorMessage = friendlyError(it)) }
        }
    }

    fun signUp(email: String, password: String, confirmPassword: String) {
        if (password != confirmPassword) {
            _state.value = AuthState(errorMessage = "Passwords do not match")
            return
        }
        if (!validateInputs(email, password)) return
        viewModelScope.launch {
            _state.value = AuthState(isLoading = true)
            repository.signUp(email, password)
                .onSuccess { _state.value = AuthState(isSuccess = true) }
                .onFailure { _state.value = AuthState(errorMessage = friendlyError(it)) }
        }
    }

    fun resetPassword(email: String) {
        if (email.isBlank()) {
            _state.value = AuthState(errorMessage = "Enter your email address")
            return
        }
        viewModelScope.launch {
            _state.value = AuthState(isLoading = true)
            repository.resetPassword(email)
                .onSuccess { _state.value = AuthState(errorMessage = "Password reset email sent") }
                .onFailure { _state.value = AuthState(errorMessage = friendlyError(it)) }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    private fun validateInputs(email: String, password: String): Boolean {
        if (email.isBlank()) {
            _state.value = AuthState(errorMessage = "Email is required")
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _state.value = AuthState(errorMessage = "Enter a valid email address")
            return false
        }
        if (password.length < 8) {
            _state.value = AuthState(errorMessage = "Password must be at least 8 characters")
            return false
        }
        return true
    }

    private fun friendlyError(e: Throwable): String = when {
        e.message?.contains("Invalid login", true) == true -> "Incorrect email or password"
        e.message?.contains("already registered", true) == true -> "This email is already registered"
        e.message?.contains("network", true) == true -> "No internet connection"
        e.message?.contains("Email not confirmed", true) == true -> "Please verify your email first"
        else -> e.message ?: "Something went wrong. Try again."
    }
}
