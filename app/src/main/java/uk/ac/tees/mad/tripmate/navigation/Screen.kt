package uk.ac.tees.mad.tripmate.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Auth : Screen("auth")
    object Home : Screen("home")
    object Trip : Screen("trip/{tripId}") {
        fun createRoute(tripId: String = "new") = "trip/$tripId"
    }
    object Settings : Screen("settings")
}