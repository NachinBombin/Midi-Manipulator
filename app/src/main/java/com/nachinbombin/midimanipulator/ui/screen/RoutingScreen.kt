package com.nachinbombin.midimanipulator.ui.screen

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nachinbombin.midimanipulator.midi.MidiManager
import com.nachinbombin.midimanipulator.midi.MidiPortInfo
import com.nachinbombin.midimanipulator.theme.ThemeManager
import com.nachinbombin.midimanipulator.theme.ThemePreset

@Composable
fun RoutingScreen(midiManager: MidiManager, themeManager: ThemeManager) {
    val theme   = themeManager.currentTheme.collectAsState().value
    val ports   by midiManager.ports.collectAsState()
    val log     by midiManager.activityLog.collectAsState()
    // FIX: was inverted — isInput=false was labelled as INPUTS and vice versa.
    // MidiPortInfo.isInput=true means the device has an input port (app writes to it as output).
    // MidiPortInfo.isInput=false means the device has an output port (app reads from it as input).
    // For user-facing labels: "SOURCES" = device output ports we listen to (isInput=false)
    //                         "DESTINATIONS" = device input ports we send to (isInput=true)
    val sources      = ports.filter { !it.isInput }   // device output ports → app reads
    val destinations = ports.filter { it.isInput }    // device input ports  → app writes
    var selSource by remember { mutableStateOf<MidiPortInfo?>(null) }
    var selDest   by remember { mutableStateOf<MidiPortInfo?>(null) }

    Column(
        Modifier.fillMaxSize().background(Color(theme.bg.value)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("MIDI ROUTING", color = Color(theme.accent.value), fontSize = 13.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                SectionLabel("SOURCES (from device)", theme)
                PortList(sources, selSource, theme) { p ->
                    selSource = p
                    midiManager.connectInput(p)   // open device output port to receive from it
                }
            }
            Column(Modifier.weight(1f)) {
                SectionLabel("DESTINATIONS (to device)", theme)
                PortList(destinations, selDest, theme) { p ->
                    selDest = p
                    midiManager.connectOutput(p)  // open device input port to send to it
                }
            }
        }
        Box(
            Modifier.fillMaxWidth().height(38.dp)
                .background(Color(theme.bgElevated.value), RoundedCornerShape(8.dp))
                .border(1.dp, Color(theme.accent.value), RoundedCornerShape(8.dp))
                .clickable { midiManager.refreshDevices() },
            contentAlignment = Alignment.Center
        ) { Text("REFRESH / SCAN BLE", color = Color(theme.accent.value), fontSize = 11.sp) }
        SectionLabel("ACTIVITY LOG", theme)
        LazyColumn(
            Modifier.fillMaxWidth().weight(1f)
                .background(Color(theme.bgElevated.value), RoundedCornerShape(8.dp)).padding(8.dp)
        ) {
            items(log.take(60)) {
                Text(it, color = Color(theme.textMuted.value), fontSize = 10.sp, lineHeight = 14.sp)
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, theme: ThemePreset) {
    Text(text, color = Color(theme.textMuted.value), fontSize = 10.sp,
        modifier = Modifier.padding(bottom = 4.dp))
}

@Composable
private fun PortList(
    ports: List<MidiPortInfo>,
    selected: MidiPortInfo?,
    theme: ThemePreset,
    onSelect: (MidiPortInfo) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (ports.isEmpty()) {
            Text("No devices", color = Color(theme.textMuted.value), fontSize = 10.sp)
        }
        ports.forEach { port ->
            val isSel = port == selected
            Box(
                Modifier.fillMaxWidth()
                    .background(
                        if (isSel) Color(theme.accent.value) else Color(theme.bgElevated.value),
                        RoundedCornerShape(6.dp)
                    )
                    .border(1.dp, Color(theme.borderSubtle.value), RoundedCornerShape(6.dp))
                    .clickable { onSelect(port) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    port.name,
                    color = if (isSel) Color(theme.bg.value) else Color(theme.textPrimary.value),
                    fontSize = 11.sp
                )
            }
        }
    }
}
