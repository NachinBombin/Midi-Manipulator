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
        Modifier.fillMaxSize().background(theme.bg).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("COLOR THEME", color = theme.accent, fontSize = 14.sp)
        // FIX: was using items(ThemePresets.all.size) { i -> ThemePresets.all[i] }
        // which is non-idiomatic and error-prone. Use items(list) directly.
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ThemePresets.all) { preset ->
                val isSelected = preset.name == current
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            if (isSelected) theme.bgElevated else theme.bg,
                            RoundedCornerShape(10.dp)
                        )
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) preset.accent else theme.borderSubtle,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { themeManager.setTheme(preset.name) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Swatch(preset.accent)
                            Swatch(preset.accentSoft)
                            Swatch(preset.bg, bordered = true)
                        }
                        Text(
                            preset.name,
                            color    = if (isSelected) preset.textPrimary else theme.textPrimary,
                            fontSize = 13.sp
                        )
                    }
                    if (isSelected) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .background(preset.accent, RoundedCornerShape(5.dp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Swatch(color: Color, bordered: Boolean = false) {
    Box(
        Modifier
            .size(width = 18.dp, height = 18.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(color)
            .then(
                if (bordered) Modifier.border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(4.dp))
                else Modifier
            )
    )
}
