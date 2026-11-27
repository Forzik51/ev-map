package com.evmap.mobileapp.feature.settings.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evmap.mobileapp.core.data.dto.UpdateProfileRequest
import com.evmap.mobileapp.core.data.dto.UserDto
import com.evmap.mobileapp.core.data.media.ImageMultipart
import com.evmap.mobileapp.feature.profile.data.UsersRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

enum class ProfileField(val key: String) {
    NAME("name"), SURNAME("surname"), USERNAME("username"), PHONE("phone"), BIO("pageDescription"), BIRTHDATE("dateOfBirth"),
}

sealed interface AvatarChange {
    data object Keep : AvatarChange
    data class Replace(val uri: Uri) : AvatarChange
    data object Remove : AvatarChange
}

data class ProfileEditUiState(
    val profile: UserDto? = null,
    val draft: UpdateProfileRequest = UpdateProfileRequest(),
    val avatarChange: AvatarChange = AvatarChange.Keep,
    val loading: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val fieldErrors: Map<String, String> = emptyMap(),
    val saved: Boolean = false,
) {
    val hasChanges: Boolean
        get() = profile?.let { draft.normalized() != it.toEditRequest().normalized() || avatarChange != AvatarChange.Keep }
            ?: false
}

@HiltViewModel
class ProfileEditViewModel @Inject constructor(
    private val repository: UsersRepository,
    private val imageMultipart: ImageMultipart,
) : ViewModel() {
    private val _state = MutableStateFlow(ProfileEditUiState())
    val state = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        if (state.value.loading || state.value.profile != null) return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            repository.getMe().onSuccess { user ->
                _state.value = ProfileEditUiState(profile = user, draft = user.toEditRequest())
            }.onFailure { error ->
                _state.value = ProfileEditUiState(error = error.toProfileError().message)
            }
        }
    }

    fun edit(field: ProfileField, value: String) {
        _state.update { state ->
            if (state.saving) return@update state
            val draft = when (field) {
                ProfileField.NAME -> state.draft.copy(name = value)
                ProfileField.SURNAME -> state.draft.copy(surname = value)
                ProfileField.USERNAME -> state.draft.copy(username = value)
                ProfileField.PHONE -> state.draft.copy(phone = value)
                ProfileField.BIO -> state.draft.copy(pageDescription = value)
                ProfileField.BIRTHDATE -> state.draft.copy(dateOfBirth = value.takeIf { it.isNotBlank() })
            }
            state.copy(draft = draft, fieldErrors = state.fieldErrors - field.key, error = null)
        }
    }

    fun chooseAvatar(uri: Uri) = updateAvatar(AvatarChange.Replace(uri))
    fun removeAvatar() = updateAvatar(if (state.value.profile?.path.isNullOrBlank()) AvatarChange.Keep else AvatarChange.Remove)
    fun undoAvatarChange() = updateAvatar(AvatarChange.Keep)

    private fun updateAvatar(change: AvatarChange) {
        _state.update { if (it.saving) it else it.copy(avatarChange = change, error = null) }
    }

    fun save() {
        val snapshot = state.value
        val profile = snapshot.profile ?: return
        if (snapshot.saving || snapshot.saved || !snapshot.hasChanges) return

        val draft = snapshot.draft.normalized()
        val saveDetails = draft != profile.toEditRequest().normalized()
        val errors = if (saveDetails) draft.validate() else emptyMap()
        if (errors.isNotEmpty()) {
            _state.update { it.copy(fieldErrors = errors, error = null) }
            return
        }

        _state.update { it.copy(saving = true, fieldErrors = emptyMap(), error = null) }
        viewModelScope.launch {
            var confirmed = profile
            var detailsSaved = false
            try {
                if (saveDetails) {
                    confirmed = repository.updateProfile(draft).getOrThrow()
                    detailsSaved = true
                    // A later avatar failure should leave only that operation pending.
                    _state.update { it.copy(profile = confirmed, draft = confirmed.toEditRequest()) }
                }
                confirmed = when (val change = snapshot.avatarChange) {
                    AvatarChange.Keep -> confirmed
                    is AvatarChange.Replace -> {
                        val photo = repository.replacePhoto(confirmed.id, imageMultipart.filePart(change.uri)).getOrThrow()
                        confirmed.copy(path = photo.path)
                    }
                    AvatarChange.Remove -> {
                        repository.deletePhoto(confirmed.id).getOrThrow()
                        confirmed.copy(path = null)
                    }
                }
                _state.update { it.copy(profile = confirmed, avatarChange = AvatarChange.Keep, saving = false, saved = true) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                val failure = error.toProfileError()
                val avatarFailed = detailsSaved || !saveDetails
                val message = when {
                    detailsSaved -> "Profile details saved. Avatar update failed. ${failure.message}"
                    avatarFailed -> "Avatar update failed. ${failure.message}"
                    failure.fields.isEmpty() -> failure.message
                    else -> null
                }
                _state.update { it.copy(saving = false, fieldErrors = failure.fields, error = message) }
            }
        }
    }

    fun consumeSaved() {
        _state.update { it.copy(saved = false) }
    }
}

private fun UserDto.toEditRequest() = UpdateProfileRequest(name, surname, username, phone.orEmpty(), pageDescription, birthdate)

private fun UpdateProfileRequest.normalized() = copy(
    name = name.trim(),
    surname = surname?.trim()?.takeIf { it.isNotEmpty() },
    username = username.trim(),
    phone = phone.trim(),
)

private val profilePhone = Regex("^[+0-9 ()-]{7,15}$")

private fun UpdateProfileRequest.validate() = buildMap {
    if (name.isBlank() || name.length > 20) put(ProfileField.NAME.key, "Enter a name up to 20 characters")
    if ((surname?.length ?: 0) > 20) put(ProfileField.SURNAME.key, "Use up to 20 characters")
    if (username.isBlank() || username.length > 20) put(ProfileField.USERNAME.key, "Enter a username up to 20 characters")
    if (!com.evmap.mobileapp.core.data.validation.validOptionalPhone(phone)) put(ProfileField.PHONE.key, "Enter a phone number with 7 to 15 characters")
    if (dateOfBirth != null && runCatching { java.time.LocalDate.parse(dateOfBirth).isAfter(java.time.LocalDate.now()) }.getOrDefault(true)) put(ProfileField.BIRTHDATE.key,"Укажіть дату РРРР-ММ-ДД, яка не є майбутньою")
    if (pageDescription.length > 150) put(ProfileField.BIO.key, "Use up to 150 characters")
}

private data class ProfileError(val message: String, val fields: Map<String, String> = emptyMap())

private fun Throwable.toProfileError(): ProfileError = when (this) {
    is HttpException -> {
        val payload = runCatching { response()?.errorBody()?.string()?.let(::JSONObject) }.getOrNull()
        val fields = payload?.optJSONObject("fieldErrors")
        ProfileError(
            message = when (code()) {
                401 -> "Sign in again to continue."
                403 -> "You do not have permission to make this change."
                413 -> "Choose a photo up to 5 MB."
                else -> payload?.optString("message")?.takeIf { it.isNotBlank() } ?: "The server could not complete the request."
            },
            fields = ProfileField.entries.mapNotNull { field ->
                fields?.optString(field.key)?.takeIf { it.isNotBlank() }?.let { field.key to it }
            }.toMap(),
        )
    }
    is IOException -> ProfileError("Check your connection and try again.")
    is IllegalArgumentException -> ProfileError(message ?: "Check the entered data.")
    else -> ProfileError("Could not complete the request. Try again.")
}
