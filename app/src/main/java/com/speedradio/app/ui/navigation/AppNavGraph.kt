package com.speedradio.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.speedradio.app.ui.feed.FeedScreen
import com.speedradio.app.ui.record.RecordScreen

sealed class Screen(val route: String) {
    object Feed : Screen("feed")
    object Record : Screen("record")
}

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screen.Feed.route) {
        composable(Screen.Feed.route) {
            FeedScreen(onNavigateToRecord = { navController.navigate(Screen.Record.route) })
        }
        composable(Screen.Record.route) {
            RecordScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
