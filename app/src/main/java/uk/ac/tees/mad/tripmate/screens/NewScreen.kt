package uk.ac.tees.mad.tripmate.screens

import com.google.android.gms.auth.api.Auth

@Composable
fun AppNavGraph(navController: NavHostController) {

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {


        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Login.route) {
            // Login Screen
        }
    }
}