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
    onContinue: () -> Unit = {}
) {
    val focusManager = LocalFocusManager.current

    FormScaffold(

    ) {
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
        Spacer(modifier = Modifier.height(Spacing.s))
        
        // Subtitle
        Text(
            text = "Enter your email address or username",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
            //modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        
        Spacer(modifier = Modifier.height(Spacing.l))
        
        // Email field
        AuthTextField(
            value = email,
            onValueChange = onEmailChange,
            label = "Email/Username",
            placeholder = "Email/Username",
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
        // Continue button
        PrimaryButton(
            text = "Continue",
            onClick = onContinue,
            enabled = email.isNotBlank()
        )
        
        Spacer(modifier = Modifier.height(Spacing.s))
    }
}
