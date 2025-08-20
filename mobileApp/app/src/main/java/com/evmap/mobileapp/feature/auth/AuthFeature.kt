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
                    // TODO: Implement login logic
                },
                onNavigateToSignup = {
                    // TODO: Navigate to signup
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
                    // TODO: Implement forgot password logic
                    onNavigateToRoute("auth/new_password")
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
                    // TODO: Implement create password logic
                    onNavigateUp()
                }
            )
        }
    }
}
