package com.nachinbombin.midimanipulator.theme

import androidx.compose.ui.graphics.Color

data class ThemePreset(
    val name: String,
    val bg: Color,
    val bgVoices: Color,
    val bgElevated: Color,
    val accent: Color,
    val accentSoft: Color,
    val accentAlt: Color,
    val borderSubtle: Color,
    val textPrimary: Color,
    val textMuted: Color
)

object ThemePresets {
    val Default = ThemePreset(
        name        = "Default",
        bg          = Color(0xFF171614),
        bgVoices    = Color(0xFF111318),
        bgElevated  = Color(0xFF1C1B19),
        accent      = Color(0xFF4F9AA5),
        accentSoft  = Color(0xFF01696F),
        accentAlt   = Color(0xFF6DAA45),
        borderSubtle= Color(0xFF393836),
        textPrimary = Color(0xFFCDCCCA),
        textMuted   = Color(0xFF7A7974)
    )
    val Vaporwave = ThemePreset(
        name        = "Vaporwave",
        bg          = Color(0xFF050813),
        bgVoices    = Color(0xFF0D0A20),
        bgElevated  = Color(0xFF11152A),
        accent      = Color(0xFFFF71CE),
        accentSoft  = Color(0xFF01CDFE),
        accentAlt   = Color(0xFF05FFA1),
        borderSubtle= Color(0xFF282B45),
        textPrimary = Color(0xFFF5F3FF),
        textMuted   = Color(0xFFA4A3CF)
    )
    val PayToWin = ThemePreset(
        name        = "Pay To Win",
        bg          = Color(0xFF070713),
        bgVoices    = Color(0xFF130A0A),
        bgElevated  = Color(0xFF121222),
        accent      = Color(0xFFFA1E4E),
        accentSoft  = Color(0xFFFF9B4A),
        accentAlt   = Color(0xFFFFDD6B),
        borderSubtle= Color(0xFF26263B),
        textPrimary = Color(0xFFF8F5FF),
        textMuted   = Color(0xFFA29FBF)
    )
    val FruttiDiMare = ThemePreset(
        name        = "Frutti di Mare",
        bg          = Color(0xFF031017),
        bgVoices    = Color(0xFF060F1A),
        bgElevated  = Color(0xFF081C25),
        accent      = Color(0xFF1FB2AA),
        accentSoft  = Color(0xFF41E3C1),
        accentAlt   = Color(0xFF3A7FFF),
        borderSubtle= Color(0xFF16313D),
        textPrimary = Color(0xFFF0F7FF),
        textMuted   = Color(0xFF9EB8C7)
    )
    val Lambda = ThemePreset(
        name        = "Lambda",
        bg          = Color(0xFF050608),
        bgVoices    = Color(0xFF0E0C09),
        bgElevated  = Color(0xFF101215),
        accent      = Color(0xFFFF9100),
        accentSoft  = Color(0xFFFFB547),
        accentAlt   = Color(0xFF00B8FF),
        borderSubtle= Color(0xFF24262B),
        textPrimary = Color(0xFFF5F5F7),
        textMuted   = Color(0xFF9B9DA4)
    )
    val UltraViolet = ThemePreset(
        name        = "Ultra Violet",
        bg          = Color(0xFF060513),
        bgVoices    = Color(0xFF0C0A1E),
        bgElevated  = Color(0xFF120F26),
        accent      = Color(0xFFA855FF),
        accentSoft  = Color(0xFF7C3AED),
        accentAlt   = Color(0xFF22D3EE),
        borderSubtle= Color(0xFF26233D),
        textPrimary = Color(0xFFF8F5FF),
        textMuted   = Color(0xFFA5A1D5)
    )
    val AfterEight = ThemePreset(
        name        = "After Eight",
        bg          = Color(0xFF050909),
        bgVoices    = Color(0xFF070F0C),
        bgElevated  = Color(0xFF101717),
        accent      = Color(0xFF38F1B4),
        accentSoft  = Color(0xFF24C79B),
        accentAlt   = Color(0xFF7CF5FF),
        borderSubtle= Color(0xFF223130),
        textPrimary = Color(0xFFF2FFFB),
        textMuted   = Color(0xFF9FB9B3)
    )
    val RoseQuartz = ThemePreset(
        name        = "Rose Quartz",
        bg          = Color(0xFF0B0710),
        bgVoices    = Color(0xFF100810),
        bgElevated  = Color(0xFF15101F),
        accent      = Color(0xFFFF7DAB),
        accentSoft  = Color(0xFFF9A8D4),
        accentAlt   = Color(0xFF93C5FD),
        borderSubtle= Color(0xFF2A2235),
        textPrimary = Color(0xFFFDF5FF),
        textMuted   = Color(0xFFB7A9C5)
    )
    val PurpleHaze = ThemePreset(
        name        = "Purple Haze",
        bg          = Color(0xFF06060F),
        bgVoices    = Color(0xFF0A0814),
        bgElevated  = Color(0xFF141428),
        accent      = Color(0xFFC04DF9),
        accentSoft  = Color(0xFF8B5CF6),
        accentAlt   = Color(0xFFF97316),
        borderSubtle= Color(0xFF292847),
        textPrimary = Color(0xFFFAF5FF),
        textMuted   = Color(0xFFA6A1D2)
    )
    val WhiteWolf = ThemePreset(
        name        = "White Wolf",
        bg          = Color(0xFFF4F7FB),
        bgVoices    = Color(0xFFFFFFFF),
        bgElevated  = Color(0xFFE8EDF5),
        accent      = Color(0xFF38BDF8),
        accentSoft  = Color(0xFF22C55E),
        accentAlt   = Color(0xFF0EA5E9),
        borderSubtle= Color(0xFFD0D7E3),
        textPrimary = Color(0xFF0F172A),
        textMuted   = Color(0xFF6B7280)
    )

    val all: List<ThemePreset> = listOf(
        Default, Vaporwave, PayToWin, FruttiDiMare, Lambda,
        UltraViolet, AfterEight, RoseQuartz, PurpleHaze, WhiteWolf
    )
}
