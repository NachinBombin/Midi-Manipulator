package com.nachinbombin.midimanipulator.theme

import androidx.compose.ui.graphics.Color

/**
 * Token roles for the app theme system.
 * Mirrors the ThemePreset structure from the MIDI Randomizer (melody-harmony-theory branch).
 */
data class ThemePreset(
    val id: Int,
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

fun Color(hex: String): Color {
    val cleaned = hex.trimStart('#')
    val argb = ("FF" + cleaned).toLong(16).toInt()
    return Color(argb)
}

/** All 10 official theme presets. */
object ThemePresets {

    val Default = ThemePreset(
        id = 0, name = "Default",
        bg          = Color("#171614"),
        bgVoices    = Color("#111318"),
        bgElevated  = Color("#1C1B19"),
        accent      = Color("#4F9AA5"),
        accentSoft  = Color("#01696F"),
        accentAlt   = Color("#6DAA45"),
        borderSubtle = Color("#393836"),
        textPrimary  = Color("#CDCCCA"),
        textMuted    = Color("#7A7974")
    )

    val Vaporwave = ThemePreset(
        id = 1, name = "Vaporwave",
        bg          = Color("#050813"),
        bgVoices    = Color("#0D0A20"),
        bgElevated  = Color("#11152A"),
        accent      = Color("#FF71CE"),
        accentSoft  = Color("#01CDFE"),
        accentAlt   = Color("#05FFA1"),
        borderSubtle = Color("#282B45"),
        textPrimary  = Color("#F5F3FF"),
        textMuted    = Color("#A4A3CF")
    )

    val PayToWin = ThemePreset(
        id = 2, name = "Pay To Win",
        bg          = Color("#070713"),
        bgVoices    = Color("#130A0A"),
        bgElevated  = Color("#121222"),
        accent      = Color("#FA1E4E"),
        accentSoft  = Color("#FF9B4A"),
        accentAlt   = Color("#FFDD6B"),
        borderSubtle = Color("#26263B"),
        textPrimary  = Color("#F8F5FF"),
        textMuted    = Color("#A29FBF")
    )

    val FruttiDiMare = ThemePreset(
        id = 3, name = "Frutti di Mare",
        bg          = Color("#031017"),
        bgVoices    = Color("#060F1A"),
        bgElevated  = Color("#081C25"),
        accent      = Color("#1FB2AA"),
        accentSoft  = Color("#41E3C1"),
        accentAlt   = Color("#3A7FFF"),
        borderSubtle = Color("#16313D"),
        textPrimary  = Color("#F0F7FF"),
        textMuted    = Color("#9EB8C7")
    )

    val Lambda = ThemePreset(
        id = 4, name = "Lambda",
        bg          = Color("#050608"),
        bgVoices    = Color("#0E0C09"),
        bgElevated  = Color("#101215"),
        accent      = Color("#FF9100"),
        accentSoft  = Color("#FFB547"),
        accentAlt   = Color("#00B8FF"),
        borderSubtle = Color("#24262B"),
        textPrimary  = Color("#F5F5F7"),
        textMuted    = Color("#9B9DA4")
    )

    val UltraViolet = ThemePreset(
        id = 5, name = "Ultra Violet",
        bg          = Color("#060513"),
        bgVoices    = Color("#0C0A1E"),
        bgElevated  = Color("#120F26"),
        accent      = Color("#A855FF"),
        accentSoft  = Color("#7C3AED"),
        accentAlt   = Color("#22D3EE"),
        borderSubtle = Color("#26233D"),
        textPrimary  = Color("#F8F5FF"),
        textMuted    = Color("#A5A1D5")
    )

    val AfterEight = ThemePreset(
        id = 6, name = "After Eight",
        bg          = Color("#050909"),
        bgVoices    = Color("#070F0C"),
        bgElevated  = Color("#101717"),
        accent      = Color("#38F1B4"),
        accentSoft  = Color("#24C79B"),
        accentAlt   = Color("#7CF5FF"),
        borderSubtle = Color("#223130"),
        textPrimary  = Color("#F2FFFB"),
        textMuted    = Color("#9FB9B3")
    )

    val RoseQuartz = ThemePreset(
        id = 7, name = "Rose Quartz",
        bg          = Color("#0B0710"),
        bgVoices    = Color("#100810"),
        bgElevated  = Color("#15101F"),
        accent      = Color("#FF7DAB"),
        accentSoft  = Color("#F9A8D4"),
        accentAlt   = Color("#93C5FD"),
        borderSubtle = Color("#2A2235"),
        textPrimary  = Color("#FDF5FF"),
        textMuted    = Color("#B7A9C5")
    )

    val PurpleHaze = ThemePreset(
        id = 8, name = "Purple Haze",
        bg          = Color("#06060F"),
        bgVoices    = Color("#0A0814"),
        bgElevated  = Color("#141428"),
        accent      = Color("#C04DF9"),
        accentSoft  = Color("#8B5CF6"),
        accentAlt   = Color("#F97316"),
        borderSubtle = Color("#292847"),
        textPrimary  = Color("#FAF5FF"),
        textMuted    = Color("#A6A1D2")
    )

    val WhiteWolf = ThemePreset(
        id = 9, name = "White Wolf",
        bg          = Color("#F4F7FB"),
        bgVoices    = Color("#FFFFFF"),
        bgElevated  = Color("#E8EDF5"),
        accent      = Color("#38BDF8"),
        accentSoft  = Color("#22C55E"),
        accentAlt   = Color("#0EA5E9"),
        borderSubtle = Color("#D0D7E3"),
        textPrimary  = Color("#0F172A"),
        textMuted    = Color("#6B7280")
    )

    val all = listOf(
        Default, Vaporwave, PayToWin, FruttiDiMare, Lambda,
        UltraViolet, AfterEight, RoseQuartz, PurpleHaze, WhiteWolf
    )
}
