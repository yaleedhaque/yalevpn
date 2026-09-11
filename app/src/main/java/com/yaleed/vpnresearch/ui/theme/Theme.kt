package com.yaleed.vpnresearch.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Gold,
    onPrimary = Color(0xFF1A1200),
    secondary = Success,
    onSecondary = Color(0xFF00210F),
    background = Obsidian,
    surface = NightSurface,
    onBackground = Slate,
    onSurface = Slate,
    surfaceVariant = NightSurfaceHigh,
    onSurfaceVariant = SlateDim,
    error = Ember,
    onError = Color.White,
)

@Composable
fun YaleVPNTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}