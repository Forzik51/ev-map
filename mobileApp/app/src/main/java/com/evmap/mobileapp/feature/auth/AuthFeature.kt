package feature.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.evmap.mobileapp.core.data.dto.CreateUserRequest
import com.evmap.mobileapp.feature.auth.ui.AuthViewModel
import core.navigation.FeatureEntry
import feature.auth.ui.ForgotPasswordScreen
import feature.auth.ui.LoginScreen
import feature.auth.ui.NewPasswordScreen
import feature.auth.ui.SignupScreen
import java.time.Instant
import java.time.ZoneId

object AuthFeature : FeatureEntry {
    override val route = "auth/login"

    @OptIn(ExperimentalMaterial3Api::class)
    override fun registerGraph(
        navGraphBuilder: NavGraphBuilder,
        onNavigateToRoute: (String) -> Unit,
        onNavigateUp: () -> Unit
    ) {
        // Login Screen
        navGraphBuilder.composable(route = route) {
            val viewModel: AuthViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()
            var emailOrUsername by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }
            var showPassword by remember { mutableStateOf(false) }

            LaunchedEffect(state.authenticatedUserId) {
                if (state.authenticatedUserId != null) {
                    viewModel.consumeAuthenticated()
                    onNavigateToRoute("feed")
                }
            }

            LoginScreen(
                emailOrUsername = emailOrUsername,
                password = password,
                showPassword = showPassword,
                isLoading = state.loading,
                errorMessage = state.error,
                onEmailChange = { emailOrUsername = it },
                onPasswordChange = { password = it },
                onTogglePassword = { showPassword = !showPassword },
                onLogin = {
                    viewModel.login(emailOrUsername, password)
                },
                onNavigateToSignup = {
                    onNavigateToRoute("auth/signup")
                },
                onNavigateToForgot = {
                    onNavigateToRoute("auth/forgot")
                }
            )
        }

        // Forgot Password Screen
        navGraphBuilder.composable(route = "auth/forgot") {
            var email by remember { mutableStateOf("") }

            ForgotPasswordScreen(
                email = email,
                onEmailChange = { email = it },
                onContinue = {
                    onNavigateToRoute("auth/new_password")
                }
            )
        }

        // Signup Screen
        navGraphBuilder.composable(route = "auth/signup") {
            val viewModel: AuthViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()
            var name by remember { mutableStateOf("") }
            var surname by remember { mutableStateOf("") }
            var email by remember { mutableStateOf("") }
            var birthDate by remember { mutableStateOf("") }
            var phone by remember { mutableStateOf("") }
            var username by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }
            var showPassword by remember { mutableStateOf(false) }
            var showDatePicker by remember { mutableStateOf(false) }
            val datePickerState = androidx.compose.material3.rememberDatePickerState()

            LaunchedEffect(state.authenticatedUserId) {
                if (state.authenticatedUserId != null) {
                    viewModel.consumeAuthenticated()
                    onNavigateToRoute("feed")
                }
            }

            SignupScreen(
                name = name,
                surname = surname,
                email = email,
                birthDate = birthDate,
                phone = phone,
                username = username,
                password = password,
                showPassword = showPassword,
                isLoading = state.loading,
                errorMessage = state.error,
                onNameChange = { name = it },
                onSurnameChange = { surname = it },
                onEmailChange = { email = it },
                onPickBirthDate = {
                    showDatePicker = true
                },
                onPhoneChange = { phone = it },
                onUsernameChange = { username = it },
                onPasswordChange = { password = it },
                onTogglePassword = { showPassword = !showPassword },
                onSignup = {
                    viewModel.register(
                        CreateUserRequest(
                            name = name.trim(),
                            surname = surname.trim().takeIf { it.isNotBlank() },
                            email = email.trim(),
                            phone = phone.trim(),
                            dateOfBirth = birthDate,
                            username = username.trim(),
                            pageDescription = "New EvMap user",
                            password = password,
                        )
                    )
                },
                onNavigateToLogin = {
                    onNavigateToRoute(route)
                }
            )

            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                datePickerState.selectedDateMillis?.let { millis ->
                                    birthDate = Instant.ofEpochMilli(millis)
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalDate()
                                        .toString()
                                }
                                showDatePicker = false
                            }
                        ) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text("Cancel")
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }
        }

        // New Password Screen
        navGraphBuilder.composable(route = "auth/new_password") {
            var newPass by remember { mutableStateOf("") }
            var confirmPass by remember { mutableStateOf("") }
            var showNew by remember { mutableStateOf(false) }
            var showConfirm by remember { mutableStateOf(false) }

            NewPasswordScreen(
                newPass = newPass,
                confirmPass = confirmPass,
                showNew = showNew,
                showConfirm = showConfirm,
                onNewChange = { newPass = it },
                onConfirmChange = { confirmPass = it },
                onToggleShowNew = { showNew = !showNew },
                onToggleShowConfirm = { showConfirm = !showConfirm },
                onCreate = {
                    onNavigateToRoute(route)
                }
            )
        }
    }
}
