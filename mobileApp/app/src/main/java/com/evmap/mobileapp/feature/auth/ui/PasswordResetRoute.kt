package com.evmap.mobileapp.feature.auth.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import core.designsystem.Spacing
import core.ui.components.AuthTextField
import core.ui.components.FormScaffold
import core.ui.components.PrimaryButton
import feature.auth.ui.ForgotPasswordScreen
import feature.auth.ui.NewPasswordScreen

@Composable
fun PasswordResetRoute(onBack: () -> Unit, onLogin: () -> Unit, viewModel: PasswordResetViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val back = {
        when (state.step) {
            PasswordResetStep.EMAIL -> onBack()
            PasswordResetStep.SUCCESS -> onLogin()
            else -> viewModel.restart()
        }
    }
    BackHandler(onBack = back)
    when (state.step) {
        PasswordResetStep.EMAIL -> ForgotPasswordScreen(
            email = state.email, onEmailChange = viewModel::updateEmail, onContinue = viewModel::sendCode,
            isLoading = state.loading, errorMessage = state.error, resendSeconds = state.resendSeconds, onBack = back,
        )
        PasswordResetStep.CODE -> VerifyPasswordCodeScreen(state, viewModel::updateCode, viewModel::verifyCode, viewModel::sendCode, back)
        PasswordResetStep.PASSWORD -> {
            var showPassword by remember { mutableStateOf(false) }
            var showConfirmation by remember { mutableStateOf(false) }
            NewPasswordScreen(
                newPass = state.password, confirmPass = state.confirmation,
                showNew = showPassword, showConfirm = showConfirmation,
                onNewChange = viewModel::updatePassword, onConfirmChange = viewModel::updateConfirmation,
                onToggleShowNew = { showPassword = !showPassword }, onToggleShowConfirm = { showConfirmation = !showConfirmation },
                onCreate = viewModel::savePassword, isLoading = state.loading, errorMessage = state.error, onBack = back,
            )
        }
        PasswordResetStep.SUCCESS -> FormScaffold {
            Text("Password changed", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(Spacing.m))
            Text("Your password has been updated. Sign in with your new password.")
            Spacer(Modifier.height(Spacing.l))
            PrimaryButton("Back to login", onLogin)
        }
    }
}

@Composable
private fun VerifyPasswordCodeScreen(
    state: PasswordResetUiState, onCodeChange: (String) -> Unit, onVerify: () -> Unit, onResend: () -> Unit, onBack: () -> Unit,
) {
    val focus = LocalFocusManager.current
    FormScaffold {
        TextButton(onClick = onBack) { Text("Back") }
        Text("Check your email", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(Spacing.m))
        Text("If an account exists for ${state.email}, an 8-digit recovery code will arrive shortly. Check your spam folder too.")
        Spacer(Modifier.height(Spacing.m))
        AuthTextField(
            value = state.code, onValueChange = onCodeChange, label = "Recovery code", placeholder = "8-digit code",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focus.clearFocus(); onVerify() }),
        )
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.fillMaxWidth()) }
        Spacer(Modifier.height(Spacing.m))
        PrimaryButton(if (state.loading) "Please wait…" else "Verify code", onVerify, enabled = !state.loading && state.code.length == 8)
        TextButton(onClick = onResend, enabled = !state.loading && state.resendSeconds == 0L) {
            Text(if (state.resendSeconds > 0) "Resend in ${state.resendSeconds}s" else "Resend code")
        }
    }
}
