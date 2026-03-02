package com.dokushotracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    data object Dashboard : Screen(
        route = "dashboard",
        label = "Stats",
        selectedIcon = Icons.Filled.BarChart,
        unselectedIcon = Icons.Filled.BarChart,
    )

    data object Log : Screen(
        route = "log",
        label = "Log",
        selectedIcon = Icons.Filled.Edit,
        unselectedIcon = Icons.Filled.Edit,
    )

    data object History : Screen(
        route = "history",
        label = "History",
        selectedIcon = Icons.Filled.List,
        unselectedIcon = Icons.Filled.List,
    )

    data object Settings : Screen(
        route = "settings",
        label = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Filled.Settings,
    )
}

val bottomNavScreens = listOf(Screen.Dashboard, Screen.Log, Screen.History, Screen.Settings)
