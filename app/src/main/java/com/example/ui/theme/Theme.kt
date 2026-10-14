package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CyberEmerald,
    onPrimary = Color.Black,
    primaryContainer = CyberEmeraldDark,
    onPrimaryContainer = Color.White,
    secondary = CyberCyan,
    onSecondary = Color.Black,
    secondaryContainer = CyberCyanDark,
    onSecondaryContainer = Color.White,
    tertiary = CyberAmber,
    onTertiary = Color.Black,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = SlateSurface,
    onSurface = TextPrimary,
    surfaceVariant = SlateSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = SlateSurfaceBorder,
    error = CyberRed,
    onError = Color.White
)

private val LightColorScheme = darkColorScheme( // High contrast dark theme preferred for OSINT/Terminal app
    primary = CyberEmerald,
    onPrimary = Color.Black,
    secondary = CyberCyan,
    onSecondary = Color.Black,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = SlateSurface,
    onSurface = TextPrimary,
    surfaceVariant = SlateSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = SlateSurfaceBorder
)

@Composable
fun LeadTraceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
