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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import core.designsystem.Spacing
import core.ui.components.AuthTextField
import core.ui.components.FormScaffold
import core.ui.components.PrimaryButton

@Preview(showBackground = true)
@Composable
fun ForgotPasswordScreen(
    email: String = "",
    onEmailChange: (String) -> Unit = {},
    onContinue: () -> Unit = {},
    isLoading: Boolean = false,
    errorMessage: String? = null,
    resendSeconds: Long = 0,
    onBack: () -> Unit = {},
) {
    val focusManager = LocalFocusManager.current

    FormScaffold(

    ) {
        androidx.compose.material3.TextButton(onClick = onBack) { Text("Back") }
        Spacer(modifier = Modifier.height(Spacing.s))
        
        // Title
        Text(
            text = "Forgot password",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
            //modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(Spacing.m))
        
        // Subtitle
        Text(
            text = "Enter the email address linked to your account",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
            //modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        
        Spacer(modifier = Modifier.height(Spacing.m))
        
        // Email field0
        AuthTextField(
            value = email,
            onValueChange = onEmailChange,
            label = "Email",
            placeholder = "Email address",
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    onContinue()
                }
            )
        )
        Spacer(modifier = Modifier.height(Spacing.m))
        errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (resendSeconds > 0) Text("Try again in ${resendSeconds}s")
        PrimaryButton(
            text = if (isLoading) "Please wait…" else "Send recovery code",
            onClick = onContinue,
            enabled = email.isNotBlank() && !isLoading && resendSeconds == 0L
        )
        
        Spacer(modifier = Modifier.height(Spacing.s))
    }
}
