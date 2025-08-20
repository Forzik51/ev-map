package feature.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import core.designsystem.Spacing
import core.ui.components.AuthTextField
import core.ui.components.DateField
import core.ui.components.FormScaffold
import core.ui.components.PasswordTextField
import core.ui.components.PhoneTextField
import core.ui.components.PrimaryButton
import core.ui.components.TextLink

@Preview(showBackground = true)
@Composable
fun SignupScreen(
    name: String = "",
    surname: String = "",
    email: String = "",
    birthDate: String = "",
    phone: String = "",
    username: String = "",
    password: String = "",
    showPassword: Boolean = false,
    onNameChange: (String) -> Unit = {},
    onSurnameChange: (String) -> Unit = {},
    onEmailChange: (String) -> Unit = {},
    onPickBirthDate: () -> Unit = {},
    onPhoneChange: (String) -> Unit = {},
    onUsernameChange: (String) -> Unit = {},
    onPasswordChange: (String) -> Unit = {},
    onTogglePassword: () -> Unit = {},
    onSignup: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {}
) {
    val focusManager = LocalFocusManager.current

    FormScaffold {
        Spacer(modifier = Modifier.height(Spacing.m))

        // Title
        Text(
            text = "Sign Up",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(Spacing.l))
        
        // Name field
        AuthTextField(
            value = name,
            onValueChange = onNameChange,
            label = "Name",
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )
        
        Spacer(modifier = Modifier.height(Spacing.m))

        // Surname field
        AuthTextField(
            value = surname,
            onValueChange = onSurnameChange,
            label = "Surname",
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )
        
        Spacer(modifier = Modifier.height(Spacing.m))

        // Email field
        AuthTextField(
            value = email,
            onValueChange = onEmailChange,
            label = "Email",
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )
        
        Spacer(modifier = Modifier.height(Spacing.m))

        // Date of birth field
        DateField(
            value = birthDate,
            label = "Date of birth",
            onClick = onPickBirthDate
        )

        Spacer(modifier = Modifier.height(Spacing.m))
        
        // Phone number field
        PhoneTextField(
            value = phone,
            onValueChange = onPhoneChange,
            label = "Phone number",
            imeAction = ImeAction.Next,
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )
        
        Spacer(modifier = Modifier.height(Spacing.m))

        // Username field
        AuthTextField(
            value = username,
            onValueChange = onUsernameChange,
            label = "Username",
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )
        
        Spacer(modifier = Modifier.height(Spacing.m))

        // Password field
        PasswordTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = "Password",
            show = showPassword,
            onToggleShow = onTogglePassword,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    onSignup()
                }
            )
        )
        
        Spacer(modifier = Modifier.height(Spacing.l))
        
        // Sign up button
        PrimaryButton(
            text = "Sign up",
            onClick = onSignup
        )
        
        Spacer(modifier = Modifier.height(Spacing.m))
        
        // Login link
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Already have an account? ",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            TextLink(
                text = "Log in",
                onClick = onNavigateToLogin
            )
        }
        
        Spacer(modifier = Modifier.height(Spacing.s))
    }
}
