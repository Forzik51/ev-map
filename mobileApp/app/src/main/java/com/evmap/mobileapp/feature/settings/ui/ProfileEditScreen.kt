package feature.settings.ui

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
            title = { Text("Edit profile") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )
    }) { padding ->
        if (state.profile == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                if (state.loading) CircularProgressIndicator() else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        TextButton(onClick = onRetry) { Text("Retry") }
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
                    AsyncImage(model = avatar, contentDescription = "Avatar", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
                Row {
                    TextButton(
                        onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        enabled = !state.saving,
                    ) { Text(if (avatar == null) "Choose photo" else "Change photo") }
                    if (avatar != null) {
                        TextButton(onClick = onRemoveAvatar, enabled = !state.saving) { Text("Remove photo") }
                    }
                }
                if (state.avatarChange != AvatarChange.Keep) {
                    TextButton(onClick = onUndoAvatarChange, enabled = !state.saving) { Text("Undo photo change") }
                }
            }
            ProfileInput(state, ProfileField.NAME, state.draft.name, "First name", onEdit)
            ProfileInput(state, ProfileField.SURNAME, state.draft.surname.orEmpty(), "Last name", onEdit)
            ProfileInput(state, ProfileField.USERNAME, state.draft.username, "Username", onEdit)
            ProfileInput(state, ProfileField.PHONE, state.draft.phone, "Phone", onEdit)
            ProfileInput(state, ProfileField.BIO, state.draft.pageDescription, "Bio", onEdit)
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            PrimaryButton(
                text = if (state.saving) "Saving…" else "Apply",
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
        keyboardOptions = if (field == ProfileField.PHONE) KeyboardOptions(keyboardType = KeyboardType.Phone) else KeyboardOptions.Default,
        singleLine = field != ProfileField.BIO,
        enabled = !state.saving,
        error = state.fieldErrors[field.key],
    )
}
