package com.speedradio.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.speedradio.app.ui.feed.FeedScreen
import com.speedradio.app.ui.player.PlayerScreen
import com.speedradio.app.ui.record.RecordScreen

sealed class Screen(val route: String) {
    object Feed : Screen("feed")
    object Record : Screen("record")
    object Player : Screen("player/{postId}") {
        fun createRoute(postId: String) = "player/$postId"
    }
}

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screen.Feed.route) {
        composable(Screen.Feed.route) {
            FeedScreen(
                onNavigateToRecord = { navController.navigate(Screen.Record.route) },
                onNavigateToPlayer = { postId ->
                    navController.navigate(Screen.Player.createRoute(postId))
                }
            )
        }
        composable(Screen.Record.route) {
            RecordScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(
            route = Screen.Player.route,
            arguments = listOf(navArgument("postId") { type = NavType.StringType })
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: return@composable
            PlayerScreen(
                initialPostId = postId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
