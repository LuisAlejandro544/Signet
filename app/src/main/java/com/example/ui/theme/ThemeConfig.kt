package com.example.ui.theme

import androidx.compose.ui.graphics.Color

enum class ThemeMode(val displayName: String, val description: String) {
    SYSTEM("Seguir al sistema", "Usa la preferencia de color del dispositivo"),
    LIGHT("Modo Claro", "Limpio y de alto contraste diurno"),
    DARK("Modo Oscuro", "Gris azulado oscuro elegante"),
    PURE_BLACK("Negro 100% (AMOLED)", "Negro absoluto #000000 para pantallas OLED")
}

enum class ColorPalette(
    val displayName: String,
    val previewColor: Color,
    val requiresAndroid12: Boolean = false
) {
    DYNAMIC("Material You", Color(0xFF3865BA), requiresAndroid12 = true),
    NAVY("Azul Marino", SlateNavyPrimary),
    EMERALD("Verde Esmeralda", EmeraldPrimary),
    PURPLE("Púrpura Profundo", PurplePrimary),
    AMBER("Ámbar Cálido", AmberPrimary),
    TEAL("Cian Tecnológico", TealPrimary),
    CRIMSON("Rojo Carmesí", CrimsonPrimary),
    MONOCHROME("Monocromo", MonoPrimary)
}

data class ThemeState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val colorPalette: ColorPalette = ColorPalette.DYNAMIC
)
