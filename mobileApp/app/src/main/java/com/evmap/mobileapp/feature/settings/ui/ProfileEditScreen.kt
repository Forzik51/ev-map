package feature.settings.ui
import androidx.compose.ui.res.stringResource
import com.evmap.mobileapp.R

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.evmap.mobileapp.feature.feed.data.toAbsoluteMediaUrl
import com.evmap.mobileapp.feature.settings.ui.AvatarChange
import com.evmap.mobileapp.feature.settings.ui.ProfileEditUiState
import com.evmap.mobileapp.feature.settings.ui.ProfileField
import core.designsystem.Spacing
import core.ui.components.AuthTextField
import core.ui.components.PrimaryButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    state: ProfileEditUiState,
    onEdit: (ProfileField, String) -> Unit,
    onChooseAvatar: (Uri) -> Unit,
    onRemoveAvatar: () -> Unit,
    onUndoAvatarChange: () -> Unit,
    onApply: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onChooseAvatar(uri)
    }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.form_edit_profile_15141e)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.form_back_b52b36))
                }
            },
        )
    }) { padding ->
        if (state.profile == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                if (state.loading) CircularProgressIndicator() else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        TextButton(onClick = onRetry) { Text(stringResource(R.string.form_retry_9f5cd8)) }
                    }
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(Spacing.m),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            val avatar = when (val change = state.avatarChange) {
                AvatarChange.Keep -> state.profile.path.toAbsoluteMediaUrl()
                is AvatarChange.Replace -> change.uri
                AvatarChange.Remove -> null
            }
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.size(94.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(40.dp))
                    AsyncImage(model = avatar, contentDescription = stringResource(R.string.form_avatar_7631b2), contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
                Row {
                    TextButton(
                        onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        enabled = !state.saving,
                    ) { Text(if (avatar == null) stringResource(R.string.form_choose_photo_4a7ede) else stringResource(R.string.form_change_photo_ed5690)) }
                    if (avatar != null) {
                        TextButton(onClick = onRemoveAvatar, enabled = !state.saving) { Text(stringResource(R.string.form_remove_photo_c8f5ed)) }
                    }
                }
                if (state.avatarChange != AvatarChange.Keep) {
                    TextButton(onClick = onUndoAvatarChange, enabled = !state.saving) { Text(stringResource(R.string.form_undo_photo_change_e5409f)) }
                }
            }
            ProfileInput(state, ProfileField.NAME, state.draft.name, stringResource(R.string.form_first_name_7e568a), onEdit)
            ProfileInput(state, ProfileField.SURNAME, state.draft.surname.orEmpty(), stringResource(R.string.form_last_name_adec36), onEdit)
            ProfileInput(state, ProfileField.USERNAME, state.draft.username, stringResource(R.string.form_username_84c290), onEdit)
            ProfileInput(state, ProfileField.PHONE, state.draft.phone, stringResource(R.string.form_phone_77064d), onEdit)
            ProfileInput(state, ProfileField.BIRTHDATE, state.draft.dateOfBirth.orEmpty(), "Дата народження · РРРР-ММ-ДД · необов’язково", onEdit)
            ProfileInput(state, ProfileField.BIO, state.draft.pageDescription, stringResource(R.string.form_bio_b31fc9), onEdit)
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            PrimaryButton(
                text = if (state.saving) "Saving…" else stringResource(R.string.form_apply_cfea41),
                onClick = onApply,
                enabled = state.hasChanges && !state.saving && !state.saved,
            )
        }
    }
}

@Composable
private fun ProfileInput(
    state: ProfileEditUiState,
    field: ProfileField,
    value: String,
    label: String,
    onEdit: (ProfileField, String) -> Unit,
) {
    AuthTextField(
        value = value,
        onValueChange = { onEdit(field, it) },
        label = label,
        required = field in setOf(ProfileField.NAME,ProfileField.USERNAME),
        maxLength = when(field) {ProfileField.NAME,ProfileField.SURNAME,ProfileField.USERNAME->20;ProfileField.BIO->150;else->null},
        keyboardOptions = if (field == ProfileField.PHONE) KeyboardOptions(keyboardType = KeyboardType.Phone) else KeyboardOptions.Default,
        singleLine = field != ProfileField.BIO,
        enabled = !state.saving,
        error = state.fieldErrors[field.key],
    )
}
