package com.dokushotracker.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dokushotracker.domain.model.AppSettings
import com.dokushotracker.ui.screens.dashboard.DashboardScreen
import com.dokushotracker.ui.screens.history.HistoryScreen
import com.dokushotracker.ui.screens.log.LogScreen
import com.dokushotracker.ui.screens.settings.SettingsScreen

@Composable
fun DokushoNavGraph(
    modifier: Modifier = Modifier,
    appSettings: AppSettings,
) {
    val navController = rememberNavController()
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry.value?.destination
    val showBottomBar = currentDestination?.route in bottomNavScreens.map { it.route }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    navController = navController,
                    currentDestination = currentDestination,
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(if (showBottomBar) innerPadding else PaddingValues()),
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onOpenSettings = { navController.navigate(Screen.Settings.route) },
                    viewModel = hiltViewModel(),
                )
            }
            composable(Screen.Log.route) {
                LogScreen(
                    onOpenSettings = { navController.navigate(Screen.Settings.route) },
                    viewModel = hiltViewModel(),
                )
            }
            composable(Screen.History.route) {
                HistoryScreen(
                    onOpenSettings = { navController.navigate(Screen.Settings.route) },
                    viewModel = hiltViewModel(),
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    appSettings = appSettings,
                    onNavigateBack = { navController.popBackStack() },
                    viewModel = hiltViewModel(),
                )
            }
        }
    }
}
