package feature.auth.ui
import androidx.compose.ui.res.stringResource
import com.evmap.mobileapp.R

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
    isLoading: Boolean = false,
    errorMessage: String? = null,
    fieldErrors: Map<String,String> = emptyMap(),
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
            text = stringResource(R.string.form_log_in_d527bf),
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
            label = stringResource(R.string.form_email_username_bc2d6a),
            required = true,
            error = fieldErrors["usernameOrEmail"],
            placeholder = stringResource(R.string.form_email_username_bc2d6a),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )
        Spacer(modifier = Modifier.height(Spacing.m))


        // Password field with forgot password link
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            PasswordTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = stringResource(R.string.form_password_8be3c9),
            required = true,
            error = fieldErrors["password"],
                placeholder = stringResource(R.string.form_password_8be3c9),
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

        if (!errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(Spacing.s))
        }
        
        // Login button
        PrimaryButton(
            text = if (isLoading) stringResource(R.string.form_logging_in_2b01ab) else "Log in",
            onClick = onLogin,
            enabled = !isLoading && emailOrUsername.isNotBlank() && password.isNotBlank()
        )
        Spacer(modifier = Modifier.height(Spacing.s))

        // Sign up link
        Row(
            modifier = Modifier.fillMaxWidth(),
            //modifier = Modifier.align(Alignment.CenterHorizontally),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.form_don_t_have_an_account_yet_18d066),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            TextLink(
                text = stringResource(R.string.form_sign_up_0b8149),
                onClick = onNavigateToSignup
            )
        }

        Spacer(modifier = Modifier.height(Spacing.s))
    }
}
