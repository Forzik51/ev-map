package feature.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import core.ui.components.AuthTextField
import core.ui.components.FormScaffold
import core.ui.components.PasswordTextField
import core.ui.components.PrimaryButton
import core.ui.components.TextLink


@Preview(showBackground = true)
@Composable
fun LoginScreen(
    emailOrUsername: String = "",
    password: String = "",
    showPassword: Boolean = false,
    onEmailChange: (String) -> Unit = {},
    onPasswordChange: (String) -> Unit = {},
    onTogglePassword: () -> Unit = {},
    onLogin: () -> Unit = {},
    onNavigateToSignup: () -> Unit = {},
    onNavigateToForgot: () -> Unit = {}
) {
    val focusManager = LocalFocusManager.current

    FormScaffold {
        Spacer(modifier = Modifier.height(Spacing.xxl))
        
        // Title
        Text(
            text = "Log In",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
            //modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        
        Spacer(modifier = Modifier.height(Spacing.l))
        
        // Email/Username field
        AuthTextField(
            value = emailOrUsername,
            onValueChange = onEmailChange,
            label = "Email/Username",
            placeholder = "Email/Username",
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )
        
        // Password field with forgot password link
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            PasswordTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = "Password",
                placeholder = "Password",
                show = showPassword,
                onToggleShow = onTogglePassword,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        onLogin()
                    }
                )
            )
            Spacer(modifier = Modifier.height(Spacing.s))

            Box(
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                TextLink(
                    text = "Forgot your password?",
                    onClick = onNavigateToForgot
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.m))
        
        // Login button
        PrimaryButton(
            text = "Log in",
            onClick = onLogin,
            enabled = emailOrUsername.isNotBlank() && password.isNotBlank()
        )
        Spacer(modifier = Modifier.height(Spacing.s))

        // Sign up link
        Row(
            modifier = Modifier.fillMaxWidth(),
            //modifier = Modifier.align(Alignment.CenterHorizontally),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Don't have an account yet? ",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            TextLink(
                text = "Sign up",
                onClick = onNavigateToSignup
            )
        }

        Spacer(modifier = Modifier.height(Spacing.s))
    }
}
