package feature.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import core.navigation.FeatureEntry
import feature.auth.ui.ForgotPasswordScreen
import feature.auth.ui.LoginScreen
import feature.auth.ui.NewPasswordScreen
import feature.auth.ui.SignupScreen

object AuthFeature : FeatureEntry {
    override val route = "auth/login"

    override fun registerGraph(
        navGraphBuilder: NavGraphBuilder,
        onNavigateToRoute: (String) -> Unit,
        onNavigateUp: () -> Unit
    ) {
        // Login Screen
        navGraphBuilder.composable(route = route) {
            var emailOrUsername by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }
            var showPassword by remember { mutableStateOf(false) }

            LoginScreen(
                emailOrUsername = emailOrUsername,
                password = password,
                showPassword = showPassword,
                onEmailChange = { emailOrUsername = it },
                onPasswordChange = { password = it },
                onTogglePassword = { showPassword = !showPassword },
                onLogin = {
                    onNavigateToRoute("feed")
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
            var name by remember { mutableStateOf("") }
            var surname by remember { mutableStateOf("") }
            var email by remember { mutableStateOf("") }
            var birthDate by remember { mutableStateOf("") }
            var phone by remember { mutableStateOf("") }
            var username by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }
            var showPassword by remember { mutableStateOf(false) }

            SignupScreen(
                name = name,
                surname = surname,
                email = email,
                birthDate = birthDate,
                phone = phone,
                username = username,
                password = password,
                showPassword = showPassword,
                onNameChange = { name = it },
                onSurnameChange = { surname = it },
                onEmailChange = { email = it },
                onPickBirthDate = {
                    // TODO: Implement date picker logic
                },
                onPhoneChange = { phone = it },
                onUsernameChange = { username = it },
                onPasswordChange = { password = it },
                onTogglePassword = { showPassword = !showPassword },
                onSignup = {
                    onNavigateToRoute("feed")
                },
                onNavigateToLogin = {
                    onNavigateToRoute(route)
                }
            )
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
                    onNavigateUp()
                }
            )
        }
    }
}
