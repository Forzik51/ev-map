package com.evmap.mobileapp.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evmap.mobileapp.core.data.session.TokenStore
import com.evmap.mobileapp.feature.auth.data.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    tokenStore: TokenStore,
) : ViewModel() {
    val session = tokenStore.sessions

    fun logout() = viewModelScope.launch {
        authRepository.logout()
    }
}
