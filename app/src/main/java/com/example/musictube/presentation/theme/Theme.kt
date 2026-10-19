package com.example.musictube.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.musictube.domain.model.AccentOption
import com.example.musictube.domain.model.ThemeMode
import com.example.musictube.domain.model.ThemeSettings

val SoundBuzzShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

fun soundBuzzColorScheme(accentOption: AccentOption, dark: Boolean, pureBlack: Boolean): ColorScheme {
    return if (dark) {
        val accent = accentOption.shades.onDark
        val background = if (pureBlack) BlackBackground else DarkBackground.tintedWith(accent, 0.03f)
        val surface = if (pureBlack) BlackSurface else DarkSurface.tintedWith(accent, 0.04f)
        val surfaceHigh = if (pureBlack) BlackSurfaceHigh else DarkSurfaceHigh.tintedWith(accent, 0.05f)
        val outline = if (pureBlack) BlackOutline else DarkOutline.tintedWith(accent, 0.06f)
        darkColorScheme(
            primary = accent,
            onPrimary = OnAccentDark,
            primaryContainer = surfaceHigh.tintedWith(accent, 0.22f),
            onPrimaryContainer = accent,
            secondary = accent,
            onSecondary = OnAccentDark,
            secondaryContainer = surfaceHigh.tintedWith(accent, 0.22f),
            onSecondaryContainer = accent,
            tertiary = accent,
            onTertiary = OnAccentDark,
            background = background,
            onBackground = DarkOnBackground,
            surface = surface,
            onSurface = DarkOnBackground,
            surfaceVariant = surfaceHigh,
            onSurfaceVariant = DarkOnSurfaceMuted,
            surfaceTint = Color.Transparent,
            surfaceBright = surfaceHigh,
            surfaceDim = background,
            surfaceContainerLowest = background,
            surfaceContainerLow = surface,
            surfaceContainer = surface,
            surfaceContainerHigh = surfaceHigh,
            surfaceContainerHighest = surfaceHigh,
            outline = outline,
            outlineVariant = outline,
            inverseSurface = DarkOnBackground,
            inverseOnSurface = DarkBackground,
            inversePrimary = accentOption.shades.onLight
        )
    } else {
        val accent = accentOption.shades.onLight
        val surfaceHigh = LightSurfaceHigh.tintedWith(accent, 0.04f)
        val outline = LightOutline.tintedWith(accent, 0.05f)
        lightColorScheme(
            primary = accent,
            onPrimary = Color.White,
            primaryContainer = LightSurface.tintedWith(accent, 0.14f),
            onPrimaryContainer = accent,
            secondary = accent,
            onSecondary = Color.White,
            secondaryContainer = LightSurface.tintedWith(accent, 0.14f),
            onSecondaryContainer = accent,
            tertiary = accent,
            onTertiary = Color.White,
            background = LightBackground.tintedWith(accent, 0.02f),
            onBackground = LightOnBackground,
            surface = LightSurface,
            onSurface = LightOnBackground,
            surfaceVariant = surfaceHigh,
            onSurfaceVariant = LightOnSurfaceMuted,
            surfaceTint = Color.Transparent,
            surfaceBright = LightSurface,
            surfaceDim = surfaceHigh,
            surfaceContainerLowest = LightSurface,
            surfaceContainerLow = LightSurface,
            surfaceContainer = LightSurface,
            surfaceContainerHigh = LightSurface,
            surfaceContainerHighest = surfaceHigh,
            outline = outline,
            outlineVariant = outline,
            inverseSurface = LightOnBackground,
            inverseOnSurface = LightBackground,
            inversePrimary = accentOption.shades.onDark
        )
    }
}

@Composable
fun isAppInDarkTheme(settings: ThemeSettings): Boolean = when (settings.mode) {
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
}

@Composable
fun MusicTubeTheme(
    settings: ThemeSettings = ThemeSettings(),
    content: @Composable () -> Unit
) {
    val darkTheme = isAppInDarkTheme(settings)
    val colorScheme = remember(settings.accent, darkTheme, settings.pureBlack) {
        soundBuzzColorScheme(settings.accent, darkTheme, settings.pureBlack)
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SoundBuzzTypography,
        shapes = SoundBuzzShapes,
        content = content
    )
}
