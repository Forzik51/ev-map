package com.evmap.mobileapp.feature.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evmap.mobileapp.feature.auth.data.PasswordResetFailure
import com.evmap.mobileapp.feature.auth.data.PasswordResetGateway
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PasswordResetStep { EMAIL, CODE, PASSWORD, SUCCESS }

data class PasswordResetUiState(
    val step: PasswordResetStep = PasswordResetStep.EMAIL,
    val email: String = "",
    val code: String = "",
    val password: String = "",
    val confirmation: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val resendSeconds: Long = 0,
)

@HiltViewModel
class PasswordResetViewModel @Inject constructor(private val gateway: PasswordResetGateway) : ViewModel() {
    private val _state = MutableStateFlow(PasswordResetUiState())
    val state: StateFlow<PasswordResetUiState> = _state
    // Grants never enter navigation arguments, saved state, preferences, or logs.
    private var requestId: String? = null
    private var resetToken: String? = null
    private var operation: Job? = null
    private var cooldown: Job? = null

    fun updateEmail(value: String) { if (!_state.value.loading) _state.update { it.copy(email = value, error = null) } }
    fun updateCode(value: String) { if (!_state.value.loading) _state.update { it.copy(code = value.filter(Char::isDigit).take(8), error = null) } }
    fun updatePassword(value: String) { if (!_state.value.loading) _state.update { it.copy(password = value, error = null) } }
    fun updateConfirmation(value: String) { if (!_state.value.loading) _state.update { it.copy(confirmation = value, error = null) } }

    fun sendCode() {
        val state = _state.value
        if (state.loading || state.resendSeconds > 0 || state.step !in setOf(PasswordResetStep.EMAIL, PasswordResetStep.CODE)) return
        val email = state.email.trim()
        if (!Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(email) || email.length > 254) {
            _state.update { it.copy(error = "Enter a valid email address.") }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        operation = viewModelScope.launch {
            gateway.requestCode(email).onSuccess { response ->
                requestId = response.requestId
                resetToken = null
                _state.update { it.copy(step = PasswordResetStep.CODE, email = email, code = "", loading = false) }
                startCooldown(response.resendAfterSeconds)
            }.onFailure(::failed)
        }
    }

    fun verifyCode() {
        val state = _state.value
        if (state.loading || state.step != PasswordResetStep.CODE) return
        val id = requestId ?: return restart()
        if (!Regex("[0-9]{8}").matches(state.code)) {
            _state.update { it.copy(error = "Enter the 8-digit code from the email.") }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        operation = viewModelScope.launch {
            gateway.verifyCode(id, state.code).onSuccess { response ->
                resetToken = response.resetToken
                _state.update { it.copy(step = PasswordResetStep.PASSWORD, code = "", loading = false) }
            }.onFailure(::failed)
        }
    }

    fun savePassword() {
        val state = _state.value
        if (state.loading || state.step != PasswordResetStep.PASSWORD) return
        val token = resetToken ?: return restart()
        val error = when {
            state.password.isBlank() || state.password.length < 6 -> "Password must contain at least 6 characters."
            state.password.toByteArray(Charsets.UTF_8).size > 72 -> "Password must be at most 72 UTF-8 bytes."
            state.password != state.confirmation -> "Passwords do not match."
            else -> null
        }
        if (error != null) { _state.update { it.copy(error = error) }; return }
        _state.update { it.copy(loading = true, error = null) }
        operation = viewModelScope.launch {
            gateway.resetPassword(token, state.password).onSuccess {
                requestId = null
                resetToken = null
                cooldown?.cancel()
                _state.value = PasswordResetUiState(step = PasswordResetStep.SUCCESS)
            }.onFailure(::failed)
        }
    }

    fun restart() {
        operation?.cancel()
        requestId = null
        resetToken = null
        _state.value = PasswordResetUiState(email = _state.value.email, resendSeconds = _state.value.resendSeconds)
    }

    private fun failed(error: Throwable) {
        if (error is PasswordResetFailure && error.code == "INVALID_OR_EXPIRED_RESET_TOKEN") {
            requestId = null
            resetToken = null
            _state.value = PasswordResetUiState(email = _state.value.email, error = error.message)
        } else _state.update { it.copy(loading = false, error = error.message ?: "Please try again.") }
        (error as? PasswordResetFailure)?.retryAfterSeconds?.let(::startCooldown)
    }

    private fun startCooldown(seconds: Long) {
        cooldown?.cancel()
        _state.update { it.copy(resendSeconds = seconds.coerceIn(0, 3600)) }
        cooldown = viewModelScope.launch {
            while (_state.value.resendSeconds > 0) {
                delay(1000)
                _state.update { it.copy(resendSeconds = (it.resendSeconds - 1).coerceAtLeast(0)) }
            }
        }
    }
}
