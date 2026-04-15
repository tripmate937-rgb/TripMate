package uk.ac.tees.mad.tripmate.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import uk.ac.tees.mad.tripmate.screens.AuthScreen
import uk.ac.tees.mad.tripmate.screens.HomeScreen
import uk.ac.tees.mad.tripmate.screens.SettingsScreen
import uk.ac.tees.mad.tripmate.screens.SplashScreen
import uk.ac.tees.mad.tripmate.screens.TripScreen

import uk.ac.tees.mad.tripmate.viewmodel.AuthViewModel
import uk.ac.tees.mad.tripmate.viewmodel.TripViewModel

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Splash.route
) {
    val authViewModel: AuthViewModel = viewModel()
    val tripViewModel: TripViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToAuth = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                checkAuthStatus = { authViewModel.checkAuthStatus() }
            )
        }

        composable(Screen.Auth.route) {
            AuthScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                },
                viewModel = authViewModel
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToTrip = { tripId ->
                    navController.navigate(Screen.Trip.createRoute(tripId))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                viewModel = tripViewModel
            )
        }

        composable(
            route = Screen.Trip.route,
            arguments = listOf(
                navArgument("tripId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId") ?: "new"
            TripScreen(
                tripId = tripId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                viewModel = tripViewModel
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onLogout = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                viewModel = tripViewModel
            )
        }
    }
}