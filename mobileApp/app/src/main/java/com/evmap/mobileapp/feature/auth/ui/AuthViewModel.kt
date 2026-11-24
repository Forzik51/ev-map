package com.evmap.mobileapp.feature.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evmap.mobileapp.core.data.dto.CreateUserRequest
import com.evmap.mobileapp.core.data.remote.toUserMessage
import com.evmap.mobileapp.feature.auth.data.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val authenticatedUserId: Long? = null,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state

    fun login(usernameOrEmail: String, password: String) {
        if (usernameOrEmail.isBlank() || password.isBlank()) {
            _state.update { it.copy(error = "Enter username/email and password") }
            return
        }

        viewModelScope.launch {
            _state.value = AuthUiState(loading = true)
            repository.login(usernameOrEmail.trim(), password)
                .onSuccess { response ->
                    _state.value = AuthUiState(authenticatedUserId = response.userId)
                }
                .onFailure { error ->
                    _state.value = AuthUiState(error = error.toUserMessage())
                }
        }
    }

    fun register(user: CreateUserRequest) {
        val validationError = user.validationError()
        if (validationError != null) {
            _state.update { it.copy(error = validationError) }
            return
        }

        viewModelScope.launch {
            _state.value = AuthUiState(loading = true)
            repository.register(user)
                .onSuccess { response ->
                    _state.value = AuthUiState(authenticatedUserId = response.userId)
                }
                .onFailure { error ->
                    _state.value = AuthUiState(error = error.toUserMessage())
                }
        }
    }

    fun consumeAuthenticated() {
        _state.update { it.copy(authenticatedUserId = null) }
    }

    private fun CreateUserRequest.validationError(): String? =
        when {
            name.isBlank() -> "Enter name"
            email.isBlank() -> "Enter email"
            !EMAIL_PATTERN.matches(email) -> "Enter a valid email address"
            phone.isBlank() -> "Enter phone"
            !PHONE_PATTERN.matches(phone) -> "Enter a valid phone number"
            dateOfBirth.isBlank() -> "Pick date of birth"
            username.isBlank() -> "Enter username"
            password.length < 6 -> "Password must be at least 6 characters"
            password.toByteArray(Charsets.UTF_8).size > 72 -> "Password must be at most 72 UTF-8 bytes"
            else -> null
        }

    private companion object {
        val EMAIL_PATTERN = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
        val PHONE_PATTERN = Regex("^[+0-9 ()-]{7,15}$")
    }
}
