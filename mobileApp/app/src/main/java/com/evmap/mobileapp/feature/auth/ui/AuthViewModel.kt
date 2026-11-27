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
    val fieldErrors: Map<String,String> = emptyMap(),
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
                    val payload=runCatching { (error as? retrofit2.HttpException)?.response()?.errorBody()?.string()?.let { org.json.JSONObject(it) } }.getOrNull()
                    val fields=payload?.optJSONObject("fieldErrors")
                    _state.value = AuthUiState(error = payload?.optString("message")?.takeIf {it.isNotBlank()} ?: error.toUserMessage(),fieldErrors=fields?.keys()?.asSequence()?.associateWith {fields.optString(it)}.orEmpty())
                }
        }
    }

    fun register(user: CreateUserRequest) {
        val validationErrors = user.validationErrors()
        if (validationErrors.isNotEmpty()) {
            _state.update { it.copy(error = "Перевірте позначені поля",fieldErrors=validationErrors) }
            return
        }

        viewModelScope.launch {
            _state.value = AuthUiState(loading = true)
            repository.register(user)
                .onSuccess { response ->
                    _state.value = AuthUiState(authenticatedUserId = response.userId)
                }
                .onFailure { error ->
                    val payload=runCatching { (error as? retrofit2.HttpException)?.response()?.errorBody()?.string()?.let { org.json.JSONObject(it) } }.getOrNull()
                    val fields=payload?.optJSONObject("fieldErrors")
                    _state.value = AuthUiState(error = payload?.optString("message")?.takeIf {it.isNotBlank()} ?: error.toUserMessage(),fieldErrors=fields?.keys()?.asSequence()?.associateWith {fields.optString(it)}.orEmpty())
                }
        }
    }

    fun consumeAuthenticated() {
        _state.update { it.copy(authenticatedUserId = null) }
    }

    private fun CreateUserRequest.validationErrors() = buildMap {
        if(name.trim().length !in 1..20) put("name","Ім’я: від 1 до 20 символів")
        if(surname.orEmpty().length>20) put("surname","До 20 символів")
        if(email.length>254 || !EMAIL_PATTERN.matches(email)) put("email","Укажіть коректну email-адресу")
        if(!com.evmap.mobileapp.core.data.validation.validOptionalPhone(phone)) put("phone","Укажіть коректний телефон або залиште поле порожнім")
        if(dateOfBirth!=null && runCatching {java.time.LocalDate.parse(dateOfBirth).isAfter(java.time.LocalDate.now())}.getOrDefault(true)) put("dateOfBirth","Дата народження не може бути в майбутньому")
        if(username.trim().length !in 1..20) put("username","Ім’я користувача: від 1 до 20 символів")
        if(password.length<6 || password.toByteArray(Charsets.UTF_8).size>72 || password.isBlank()) put("password","Від 6 символів і до 72 байтів UTF-8")
    }

    private companion object {
        val EMAIL_PATTERN = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
        val PHONE_PATTERN = Regex("^[+0-9 ()-]{7,15}$")
    }
}
