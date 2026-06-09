package com.nachinbombin.midimanipulator.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nachinbombin.midimanipulator.R
import com.nachinbombin.midimanipulator.midi.MidiManager
import com.nachinbombin.midimanipulator.theme.ThemeManager
import com.nachinbombin.midimanipulator.ui.screen.GamepadScreen
import com.nachinbombin.midimanipulator.ui.screen.PerformanceScreen
import com.nachinbombin.midimanipulator.ui.screen.RoutingScreen
import com.nachinbombin.midimanipulator.ui.screen.SettingsScreen
import com.nachinbombin.midimanipulator.viewmodel.AppViewModel

sealed class Screen(val route: String, val labelRes: Int) {
    object Performance : Screen("performance", R.string.nav_performance)
    object Routing     : Screen("routing",     R.string.nav_routing)
    object Gamepad     : Screen("gamepad",     R.string.nav_gamepad)
    object Settings    : Screen("settings",    R.string.nav_settings)
}

@Composable
fun AppNavHost(
    midiManager: MidiManager,
    themeManager: ThemeManager,
    appViewModel: AppViewModel
) {
    val navController = rememberNavController()
    val items = listOf(Screen.Performance, Screen.Routing, Screen.Gamepad, Screen.Settings)
    val currentTheme by themeManager.currentTheme.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = currentTheme.bgElevated) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                items.forEach { screen ->
                    NavigationBarItem(
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        label = { Text(stringResource(screen.labelRes), color = currentTheme.textPrimary) },
                        icon = {},
                        colors = NavigationBarItemDefaults.colors(
                            selectedIndicatorColor = currentTheme.accent,
                            selectedIconColor = Color.White,
                            unselectedIconColor = currentTheme.textMuted
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Performance.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Performance.route) {
                PerformanceScreen(midiManager = midiManager, appViewModel = appViewModel, theme = currentTheme)
            }
            composable(Screen.Routing.route) {
                RoutingScreen(midiManager = midiManager, theme = currentTheme)
            }
            composable(Screen.Gamepad.route) {
                GamepadScreen(appViewModel = appViewModel, theme = currentTheme)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(themeManager = themeManager, theme = currentTheme)
            }
        }
    }
}
