package com.example.musictube.domain.model

enum class ThemeMode {
    DARK,
    LIGHT,
    SYSTEM
}

enum class AccentOption(val label: String) {
    EMBER("Ember"),
    AMBER("Amber"),
    CORAL("Coral"),
    ROSE("Rose"),
    VIOLET("Violet"),
    OCEAN("Ocean"),
    MINT("Mint")
}

data class ThemeSettings(
    val mode: ThemeMode = ThemeMode.DARK,
    val accent: AccentOption = AccentOption.EMBER,
    // Pitch-black backgrounds for OLED screens (dark mode only)
    val pureBlack: Boolean = false
)
