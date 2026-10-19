package com.example.musictube.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.example.musictube.domain.model.AccentOption

// "Ember Night" palette. Neutrals are warm and get a faint tint of the chosen accent.

/** Accent shades: a bright one for dark backgrounds, a deep one for light backgrounds (both pass 4.5:1). */
data class AccentShades(val onDark: Color, val onLight: Color)

val AccentOption.shades: AccentShades
    get() = when (this) {
        AccentOption.EMBER -> AccentShades(Color(0xFFFF7A1A), Color(0xFFC2410C))
        AccentOption.AMBER -> AccentShades(Color(0xFFFFA51F), Color(0xFFB45309))
        AccentOption.CORAL -> AccentShades(Color(0xFFFF6A4D), Color(0xFFC2361B))
        AccentOption.ROSE -> AccentShades(Color(0xFFFF5C8A), Color(0xFFBE185D))
        AccentOption.VIOLET -> AccentShades(Color(0xFFA78BFA), Color(0xFF6D28D9))
        AccentOption.OCEAN -> AccentShades(Color(0xFF38BDF8), Color(0xFF0369A1))
        AccentOption.MINT -> AccentShades(Color(0xFF34D399), Color(0xFF047857))
    }

// Text placed on a bright accent fill in dark mode
val OnAccentDark = Color(0xFF160B04)

// Dark neutrals
val DarkBackground = Color(0xFF110C0A)
val DarkSurface = Color(0xFF1C1512)
val DarkSurfaceHigh = Color(0xFF271E1A)
val DarkOutline = Color(0xFF3A2D27)
val DarkOnBackground = Color(0xFFF8F1EA)
val DarkOnSurfaceMuted = Color(0xFFB3A396)

// Pure black neutrals (OLED)
val BlackBackground = Color(0xFF000000)
val BlackSurface = Color(0xFF0F0D0C)
val BlackSurfaceHigh = Color(0xFF1B1816)
val BlackOutline = Color(0xFF2C2724)

// Light neutrals
val LightBackground = Color(0xFFFBF7F3)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceHigh = Color(0xFFF2EBE4)
val LightOutline = Color(0xFFE3D8CE)
val LightOnBackground = Color(0xFF1C1512)
val LightOnSurfaceMuted = Color(0xFF66574C)

fun Color.tintedWith(accent: Color, fraction: Float): Color = lerp(this, accent, fraction)
