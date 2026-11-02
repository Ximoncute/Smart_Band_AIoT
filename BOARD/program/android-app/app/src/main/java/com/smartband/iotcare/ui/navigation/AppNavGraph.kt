package com.smartband.iotcare.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.smartband.iotcare.ui.auth.LoginScreen
import com.smartband.iotcare.ui.auth.RegisterScreen
import com.smartband.iotcare.ui.main.MainScreen
import com.smartband.iotcare.ui.onboarding.OnboardingScreen
import com.smartband.iotcare.viewmodel.IoTViewModel

@Composable
fun AppNavGraph(
    viewModel: IoTViewModel
) {
    val navController = rememberNavController()
    val hasCompletedOnboarding by viewModel.hasCompletedOnboarding.collectAsState()
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()

    // Start destination: Onboarding if not completed, else Login if not authenticated, else Main
    val startDest = when {
        !hasCompletedOnboarding -> Screen.Onboarding.route
        !isAuthenticated -> Screen.Login.route
        else -> Screen.Main.route
    }

    NavHost(
        navController = navController,
        startDestination = startDest
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onNavigateToLogin = {
                    viewModel.completeOnboarding()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = { userEmail ->
                    viewModel.login(userEmail)
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onBackToOnboarding = {
                    viewModel.reopenOnboarding()
                    navController.navigate(Screen.Onboarding.route)
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = { userEmail ->
                    viewModel.login(userEmail)
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onBackToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Main.route) {
            MainScreen(
                viewModel = viewModel,
                onLogout = {
                    viewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                },
                onReopenOnboarding = {
                    viewModel.reopenOnboarding()
                    navController.navigate(Screen.Onboarding.route)
                }
            )
        }
    }
}
