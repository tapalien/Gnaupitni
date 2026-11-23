package com.gnaupitni

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Green1 = Color(0xFF0B7A5A)
val Green2 = Color(0xFF12A37A)
val Gold = Color(0xFFC9A24A)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0B7A5A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD3F4E6),
    onPrimaryContainer = Color(0xFF00382A),
    secondary = Color(0xFF3F6B5D),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFF9A7418),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF6E7BE),
    onTertiaryContainer = Color(0xFF3B2C00),
    background = Color(0xFFF4F8F6),
    onBackground = Color(0xFF14211C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF14211C),
    surfaceVariant = Color(0xFFE3EBE7),
    onSurfaceVariant = Color(0xFF45554E),
    outline = Color(0xFF7A8A83),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4DD4A5),
    onPrimary = Color(0xFF00382A),
    primaryContainer = Color(0xFF0B4F3B),
    onPrimaryContainer = Color(0xFFBFF2DE),
    secondary = Color(0xFF9BC9B9),
    onSecondary = Color(0xFF123428),
    tertiary = Color(0xFFE6C877),
    onTertiary = Color(0xFF3B2C00),
    tertiaryContainer = Color(0xFF574100),
    onTertiaryContainer = Color(0xFFF6E7BE),
    background = Color(0xFF0E1512),
    onBackground = Color(0xFFE1EAE5),
    surface = Color(0xFF16201C),
    onSurface = Color(0xFFE1EAE5),
    surfaceVariant = Color(0xFF26322D),
    onSurfaceVariant = Color(0xFFB4C4BC),
    outline = Color(0xFF7F8F88),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
