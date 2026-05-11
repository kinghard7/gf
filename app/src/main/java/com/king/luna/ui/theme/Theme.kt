package com.king.luna.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val LightColors = lightColorScheme(
    primary = LunaAccent,
    onPrimary = LunaSurface,
    secondary = LunaAccentSoft,
    onSecondary = LunaInk,
    tertiary = LunaOvulation,
    onTertiary = LunaSurface,
    background = LunaBg,
    onBackground = LunaInk,
    surface = LunaSurface,
    onSurface = LunaInk,
    surfaceVariant = LunaSurfaceMuted,
    onSurfaceVariant = LunaInkMuted,
    outline = LunaDivider
)

private val DarkColors = darkColorScheme(
    primary = LunaAccentDark,
    onPrimary = LunaInkDark,
    secondary = LunaAccentSoftDark,
    onSecondary = LunaInkDark,
    tertiary = LunaOvulationDark,
    onTertiary = LunaInkDark,
    background = LunaBgDark,
    onBackground = LunaInkDark,
    surface = LunaSurfaceDark,
    onSurface = LunaInkDark,
    surfaceVariant = LunaSurfaceMutedDark,
    onSurfaceVariant = LunaInkMutedDark,
    outline = LunaDividerDark
)

@Composable
fun LunaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val palette = if (darkTheme) DarkLunaPalette else LightLunaPalette
    CompositionLocalProvider(LocalLunaPalette provides palette) {
        MaterialTheme(
            colorScheme = colors,
            typography = LunaTypography,
            content = content
        )
    }
}
