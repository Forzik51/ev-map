package feature.auth.ui

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
    onCreate: () -> Unit = {}
) {
    val focusManager = LocalFocusManager.current

    FormScaffold {
        Spacer(modifier = Modifier.height(Spacing.xxl))
        
        // Title
        Text(
            text = "New password",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
            //modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        
        // Subtitle
        Text(
            text = "Create new password",
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
            label = "Create new password",
            placeholder = "Create new password",
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

        Spacer(modifier = Modifier.height(Spacing.s))
        
        // Confirm password field
        PasswordTextField(
            value = confirmPass,
            onValueChange = onConfirmChange,
            label = "Confirm new password",
            placeholder = "Confirm new password",
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
        
        // Create button
        PrimaryButton(
            text = "Create",
            onClick = onCreate
        )
        
        Spacer(modifier = Modifier.height(Spacing.s))
    }
}
