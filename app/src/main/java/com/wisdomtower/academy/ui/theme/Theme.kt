package com.wisdomtower.academy.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = CyanPrimary,
    onPrimary = OnCyan,
    primaryContainer = NavySurfaceVariant,
    onPrimaryContainer = CyanPrimary,
    secondary = CyanAccent,
    onSecondary = OnNavy,
    secondaryContainer = NavySurfaceElevated,
    onSecondaryContainer = CyanAccent,
    tertiary = GoldAccent,
    onTertiary = OnNavy,
    background = NavyBackground,
    onBackground = TextPrimary,
    surface = NavySurface,
    onSurface = TextPrimary,
    surfaceVariant = NavySurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = NavyCardBorder,
    error = RoseError,
    onError = OnNavy
)

@Composable
fun WisdomTowerTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    WisdomTowerTheme(darkTheme = darkTheme, content = content)
}


