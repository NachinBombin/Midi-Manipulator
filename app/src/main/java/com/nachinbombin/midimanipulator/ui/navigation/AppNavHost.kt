package com.nachinbombin.midimanipulator.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nachinbombin.midimanipulator.midi.MidiManager
import com.nachinbombin.midimanipulator.theme.ThemeManager
import com.nachinbombin.midimanipulator.ui.screen.*
import com.nachinbombin.midimanipulator.viewmodel.AppViewModel

sealed class Screen(val route: String, val label: String) {
    object Performance : Screen("performance", "PLAY")
    object Routing     : Screen("routing",     "MIDI")
    object Gamepad     : Screen("gamepad",     "PAD")
    object Settings    : Screen("settings",    "THEME")
}

@Composable
fun AppNavHost(
    viewModel: AppViewModel,
    midiManager: MidiManager,
    themeManager: ThemeManager
) {
    val theme   = themeManager.currentTheme.collectAsState().value
    var current by remember { mutableStateOf<Screen>(Screen.Performance) }
    val screens = listOf(Screen.Performance, Screen.Routing, Screen.Gamepad, Screen.Settings)

    Column(Modifier.fillMaxSize().background(Color(theme.bg.value))) {
        Row(
            Modifier.fillMaxWidth().background(Color(theme.bgElevated.value)).padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            screens.forEach { screen ->
                val active = screen == current
                Box(
                    Modifier.weight(1f)
                        .background(
                            if (active) Color(theme.accent.value).copy(alpha = 0.18f) else Color.Transparent,
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { current = screen }.padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        screen.label,
                        color = if (active) Color(theme.accent.value) else Color(theme.textMuted.value),
                        fontSize = 11.sp
                    )
                }
            }
        }
        Box(Modifier.weight(1f)) {
            when (current) {
                // FIX: was passing midiManager.engine (MidiEngine) but PerformanceScreen now
                // correctly accepts MidiEngine directly, so pass engine here.
                Screen.Performance -> PerformanceScreen(
                    viewModel    = viewModel,
                    midiEngine   = midiManager.engine,
                    themeManager = themeManager
                )
                Screen.Routing  -> RoutingScreen(midiManager, themeManager)
                Screen.Gamepad  -> GamepadScreen(viewModel, themeManager)
                Screen.Settings -> SettingsScreen(themeManager)
            }
        }
    }
}
