package com.nachinbombin.midimanipulator.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nachinbombin.midimanipulator.theme.ThemeManager
import com.nachinbombin.midimanipulator.theme.ThemePresets

@Composable
fun SettingsScreen(themeManager: ThemeManager) {
    val theme   = themeManager.currentTheme.collectAsState().value
    val current = themeManager.currentThemeName.collectAsState().value
    Column(
        Modifier.fillMaxSize().background(Color(theme.bg.value)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("COLOR THEME", color = Color(theme.accent.value), fontSize = 14.sp)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ThemePresets.all.size) { i ->
                val preset     = ThemePresets.all[i]
                val isSelected = preset.name == current
                Row(
                    Modifier.fillMaxWidth()
                        .background(if (isSelected) Color(theme.bgElevated.value) else Color(theme.bg.value), RoundedCornerShape(10.dp))
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) Color(preset.accent.value) else Color(theme.borderSubtle.value),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { themeManager.setTheme(preset.name) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Swatch(preset.accent.value)
                            Swatch(preset.accentSoft.value)
                            Swatch(preset.bg.value, bordered = true)
                        }
                        Text(preset.name, color = Color(if (isSelected) preset.textPrimary.value else theme.textPrimary.value), fontSize = 13.sp)
                    }
                    if (isSelected) Box(Modifier.size(10.dp).background(Color(preset.accent.value), RoundedCornerShape(5.dp)))
                }
            }
        }
    }
}

@Composable
private fun Swatch(colorLong: Long, bordered: Boolean = false) {
    Box(
        Modifier.size(width = 18.dp, height = 18.dp).clip(RoundedCornerShape(4.dp)).background(Color(colorLong))
            .then(if (bordered) Modifier.border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(4.dp)) else Modifier)
    )
}
