package com.speedradio.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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

    NavHost(
        navController = navController,
        startDestination = Screen.Feed.route,
        modifier = Modifier.background(MaterialTheme.colorScheme.background),
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left, 
                animationSpec = tween(500)
            ) + fadeIn(animationSpec = tween(500))
        },
        exitTransition = {
            fadeOut(animationSpec = tween(500)) // Fade out to background color
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(500))
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right, 
                animationSpec = tween(500)
            ) + fadeOut(animationSpec = tween(500))
        }
    ) {
        composable(Screen.Feed.route) {
            FeedScreen(
                onNavigateToPlayer = { postId ->
                    navController.navigate(Screen.Player.createRoute(postId))
                },
                onNavigateToRecord = {
                    navController.navigate(Screen.Record.route)
                }
            )
        }

        composable(Screen.Record.route) {
            RecordScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Player.route) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: ""
            PlayerScreen(
                initialPostId = postId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
