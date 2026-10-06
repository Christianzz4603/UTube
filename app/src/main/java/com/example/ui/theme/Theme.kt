package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val YouTubeDarkScheme = darkColorScheme(
    primary = YouTubeRed,
    onPrimary = Color.White,
    primaryContainer = YouTubeElevatedSurface,
    onPrimaryContainer = YouTubeTextPrimary,
    secondary = YouTubeChipSelected,
    onSecondary = YouTubeDarkBg,
    secondaryContainer = YouTubeElevatedSurface,
    onSecondaryContainer = YouTubeTextPrimary,
    tertiary = YouTubeBlueLink,
    background = YouTubeDarkBg,
    onBackground = YouTubeTextPrimary,
    surface = YouTubeDarkBg,
    onSurface = YouTubeTextPrimary,
    surfaceVariant = YouTubeElevatedSurface,
    onSurfaceVariant = YouTubeTextSecondary,
    outline = Color(0xFF3F3F3F)
)

private val YouTubeLightScheme = lightColorScheme(
    primary = YouTubeRed,
    onPrimary = Color.White,
    primaryContainer = YouTubeLightSurface,
    onPrimaryContainer = YouTubeLightTextPrimary,
    secondary = YouTubeLightTextPrimary,
    onSecondary = Color.White,
    secondaryContainer = YouTubeLightSurface,
    onSecondaryContainer = YouTubeLightTextPrimary,
    tertiary = YouTubeBlueLink,
    background = YouTubeLightBg,
    onBackground = YouTubeLightTextPrimary,
    surface = YouTubeLightBg,
    onSurface = YouTubeLightTextPrimary,
    surfaceVariant = YouTubeLightSurface,
    onSurfaceVariant = YouTubeLightTextSecondary,
    outline = Color(0xFFD9D9D9)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) YouTubeDarkScheme else YouTubeLightScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
