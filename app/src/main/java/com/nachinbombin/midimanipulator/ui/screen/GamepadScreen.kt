package com.nachinbombin.midimanipulator.ui.screen

import android.hardware.input.InputManager
import android.view.InputDevice
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nachinbombin.midimanipulator.theme.ThemeManager
import com.nachinbombin.midimanipulator.viewmodel.AppViewModel
import com.nachinbombin.midimanipulator.viewmodel.GamepadMapping

val MAPPABLE_TARGETS = listOf(
    "Joystick1 X", "Joystick1 Y", "Joystick2 X", "Joystick2 Y",
    "Chord: Triad", "Chord: 7th", "Chord: 9th", "Chord: sus2", "Chord: sus4", "Chord: Power",
    "Strum 1", "Strum 2", "Strum 3", "Strum 4", "Strum 5", "Strum 6",
    "Select Note", "Hold Note", "Portamento", "Pitch Wheel", "Mod Wheel"
)

private fun scanGamepads(): List<InputDevice> =
    InputDevice.getDeviceIds()
        .mapNotNull { InputDevice.getDevice(it) }
        .filter { it.sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD }

@Composable
fun GamepadScreen(viewModel: AppViewModel, themeManager: ThemeManager) {
    val theme          = themeManager.currentTheme.collectAsState().value
    val context        = LocalContext.current
    val testMode       by viewModel.testModeActive.collectAsState()
    val lastTriggered  by viewModel.lastTriggeredControl.collectAsState()
    val gamepadMappings by viewModel.gamepadMappings.collectAsState()

    // FIX: was a static remember{} that never refreshed on hot-plug.
    // Now uses DisposableEffect on InputManager.DeviceListener so the list
    // updates whenever a controller connects or disconnects.
    var gamepads by remember { mutableStateOf(scanGamepads()) }
    DisposableEffect(Unit) {
        val im = context.getSystemService(InputManager::class.java)
        val listener = object : InputManager.InputDeviceListener {
            override fun onInputDeviceAdded(deviceId: Int)   { gamepads = scanGamepads() }
            override fun onInputDeviceRemoved(deviceId: Int) { gamepads = scanGamepads() }
            override fun onInputDeviceChanged(deviceId: Int) { gamepads = scanGamepads() }
        }
        im.registerInputDeviceListener(listener, null)
        onDispose { im.unregisterInputDeviceListener(listener) }
    }

    var selectedDevice by remember { mutableStateOf(gamepads.firstOrNull()) }
    var selectedButton by remember { mutableStateOf<String?>(null) }

    // If selectedDevice was removed, reset it
    if (selectedDevice != null && selectedDevice !in gamepads) selectedDevice = gamepads.firstOrNull()

    val buttonLabels = listOf(
        "A", "B", "X", "Y", "L1", "R1", "L2", "R2",
        "D-Up", "D-Down", "D-Left", "D-Right",
        "LStick X", "LStick Y", "RStick X", "RStick Y"
    )

    Column(
        Modifier.fillMaxSize().background(theme.bg).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("GAMEPAD MAPPING", color = theme.accent, fontSize = 13.sp)
            Box(
                Modifier
                    .background(
                        if (testMode) theme.accent else theme.bgElevated,
                        RoundedCornerShape(6.dp)
                    )
                    .clickable { viewModel.setTestMode(!testMode) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    if (testMode) "TEST ON" else "TEST OFF",
                    color    = if (testMode) theme.bg else theme.textMuted,
                    fontSize = 10.sp
                )
            }
        }

        if (gamepads.isEmpty()) {
            Text(
                "No gamepads detected. Connect via BT or USB.",
                color = theme.textMuted, fontSize = 11.sp
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                gamepads.forEach { device ->
                    val sel = device == selectedDevice
                    Box(
                        Modifier
                            .background(
                                if (sel) theme.accent else theme.bgElevated,
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { selectedDevice = device; selectedButton = null }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            device.name ?: "Gamepad",
                            color    = if (sel) theme.bg else theme.textPrimary,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        selectedDevice?.let { device ->
            val descriptor  = device.descriptor
            val existingMap = gamepadMappings[descriptor]?.mappings ?: emptyMap()

            Text("BUTTONS & AXES", color = theme.textMuted, fontSize = 10.sp)
            LazyColumn(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(theme.bgElevated, RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(buttonLabels) { btn ->
                    val currentTarget = existingMap[btn]
                    val isHighlighted = testMode && lastTriggered == btn
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(
                                if (isHighlighted) theme.accent.copy(alpha = 0.25f)
                                else Color.Transparent,
                                RoundedCornerShape(4.dp)
                            )
                            .clickable { selectedButton = btn },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            btn,
                            color    = if (selectedButton == btn) theme.accentAlt else theme.textPrimary,
                            fontSize = 11.sp,
                            modifier = Modifier.width(80.dp)
                        )
                        Text(currentTarget ?: "\u2014", color = theme.textMuted, fontSize = 10.sp)
                    }
                }
            }

            if (selectedButton != null) {
                Text("ASSIGN \"${selectedButton}\" TO:", color = theme.textMuted, fontSize = 10.sp)
                LazyColumn(
                    Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(theme.bgElevated, RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(MAPPABLE_TARGETS) { target ->
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newMap = existingMap.toMutableMap()
                                    newMap[selectedButton!!] = target
                                    viewModel.setGamepadMapping(
                                        descriptor,
                                        GamepadMapping(descriptor, newMap)
                                    )
                                    selectedButton = null
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(target, color = theme.textPrimary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
