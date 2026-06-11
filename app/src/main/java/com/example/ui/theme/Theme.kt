package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = CyanGlow,
    secondary = CobaltBlue,
    tertiary = CyberGreen,
    background = DarkBg,
    surface = SurfaceDark,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = AlertRed,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = TextSecondary
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    secondary = SecondaryBlue,
    tertiary = CyberGreen,
    background = DarkBg, // Keep deep dark for premium look even in light trigger
    surface = SurfaceDark,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = AlertRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    amoledMode: Boolean = false,
    // Disable dynamic coloring to force our tailored secure dark cyber scheme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val finalBg = if (amoledMode) androidx.compose.ui.graphics.Color(0xFF000000) else DarkBg
    val finalSurface = if (amoledMode) androidx.compose.ui.graphics.Color(0xFF060D0E) else SurfaceDark
    
    val colorScheme = darkColorScheme(
        primary = CyanGlow,
        secondary = CobaltBlue,
        tertiary = CyberGreen,
        background = finalBg,
        surface = finalSurface,
        onBackground = TextPrimary,
        onSurface = TextPrimary,
        error = AlertRed,
        surfaceVariant = SurfaceCard,
        onSurfaceVariant = TextSecondary
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
