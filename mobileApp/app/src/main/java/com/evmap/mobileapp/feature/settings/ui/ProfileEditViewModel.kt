package com.evmap.mobileapp.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evmap.mobileapp.core.data.remote.toUserMessage
import com.evmap.mobileapp.feature.profile.data.UsersRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileEditUiState(
    val userId: Long? = null,
    val username: String = "",
    val phone: String = "",
    val title: String = "",
    val bio: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
)

@HiltViewModel
class ProfileEditViewModel @Inject constructor(
    private val repository: UsersRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ProfileEditUiState(loading = true))
    val state: StateFlow<ProfileEditUiState> = _state

    init {
        load()
    }

    fun updateBio(value: String) {
        _state.update { it.copy(bio = value, error = null) }
    }

    fun updateTitle(value: String) {
        _state.update { it.copy(title = value) }
    }

    fun save() = viewModelScope.launch {
        val userId = _state.value.userId ?: return@launch
        _state.update { it.copy(loading = true, error = null) }
        repository.updateDescription(userId, _state.value.bio)
            .onSuccess {
                _state.update { it.copy(loading = false, saved = true) }
            }
            .onFailure { error ->
                _state.update { it.copy(loading = false, error = error.toUserMessage()) }
            }
    }

    fun consumeSaved() {
        _state.update { it.copy(saved = false) }
    }

    private fun load() = viewModelScope.launch {
        repository.getMe()
            .onSuccess { user ->
                _state.value = ProfileEditUiState(
                    userId = user.id,
                    username = user.username,
                    phone = user.phone,
                    title = listOfNotNull(user.name, user.surname).joinToString(" "),
                    bio = user.pageDescription,
                    loading = false,
                )
            }
            .onFailure { error ->
                _state.value = ProfileEditUiState(loading = false, error = error.toUserMessage())
            }
    }
}
