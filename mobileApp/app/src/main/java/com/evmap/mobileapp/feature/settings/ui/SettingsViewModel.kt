package com.evmap.mobileapp.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evmap.mobileapp.core.data.session.TokenStore
import com.evmap.mobileapp.feature.auth.data.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val loggedOut: Boolean = false,
    val isAdmin: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenStore: TokenStore,
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsUiState(isAdmin = tokenStore.isAdmin()))
    val state: StateFlow<SettingsUiState> = _state

    fun logout() = viewModelScope.launch {
        authRepository.logout()
        _state.value = SettingsUiState(loggedOut = true, isAdmin = false)
    }
}
