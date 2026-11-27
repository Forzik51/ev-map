package feature.auth.ui
import androidx.compose.ui.res.stringResource
import com.evmap.mobileapp.R

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import core.designsystem.Spacing
import core.ui.components.FormScaffold
import core.ui.components.PasswordTextField
import core.ui.components.PrimaryButton



@Preview(showBackground = true)
@Composable
fun NewPasswordScreen(
    newPass: String = "",
    confirmPass: String = "",
    showNew: Boolean = false,
    showConfirm: Boolean = false,
    onNewChange: (String) -> Unit = {},
    onConfirmChange: (String) -> Unit = {},
    onToggleShowNew: () -> Unit = {},
    onToggleShowConfirm: () -> Unit = {},
    onCreate: () -> Unit = {},
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onBack: () -> Unit = {},
) {
    val focusManager = LocalFocusManager.current

    FormScaffold {
        androidx.compose.material3.TextButton(onClick = onBack) { Text(stringResource(R.string.form_start_again_b6fffc)) }
        Spacer(modifier = Modifier.height(Spacing.xxl))
        
        // Title
        Text(
            text = stringResource(R.string.form_new_password_d850ee),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
            //modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        
        // Subtitle
        Text(
            text = stringResource(R.string.form_create_new_password_6c752f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
            //modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        
        Spacer(modifier = Modifier.height(Spacing.l))
        
        // New password field
        PasswordTextField(
            value = newPass,
            onValueChange = onNewChange,
            label = stringResource(R.string.form_create_new_password_6c752f),
            placeholder = stringResource(R.string.form_create_new_password_6c752f),
            show = showNew,
            onToggleShow = onToggleShowNew,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )

        Spacer(modifier = Modifier.height(Spacing.m))
        
        // Confirm password field
        PasswordTextField(
            value = confirmPass,
            onValueChange = onConfirmChange,
            label = stringResource(R.string.form_confirm_new_password_f85039),
            placeholder = stringResource(R.string.form_confirm_new_password_f85039),
            show = showConfirm,
            onToggleShow = onToggleShowConfirm,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    onCreate()
                }
            )
        )

        Spacer(modifier = Modifier.height(Spacing.m))
        
        errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        PrimaryButton(
            text = if (isLoading) stringResource(R.string.form_please_wait_5afeca) else stringResource(R.string.form_change_password_8c6842),
            onClick = onCreate,
            enabled = !isLoading && newPass.isNotBlank() && confirmPass.isNotBlank()
        )
        
        Spacer(modifier = Modifier.height(Spacing.s))
    }
}
