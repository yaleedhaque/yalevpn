package com.yaleed.vpnresearch.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---- Brand surface (near-black, faintly cool-tinted; never pure black) ----
val Obsidian = Color(0xFF0A0D14)          // app background
val NightSurface = Color(0xFF111622)      // base surface
val NightSurfaceHigh = Color(0xFF1A2130)  // raised surface
val SurfaceTop = Color(0xFF222B3D)        // popover / elevated
val Outline = Color(0xFF262F42)           // hairline borders (1px, recessive)

// ---- Text ----
val Slate = Color(0xFFC7D0DE)             // primary text
val SlateDim = Color(0xFF8B97AA)          // secondary text
val SlateFaint = Color(0xFF5D6878)        // tertiary / placeholder

// ---- Semantic ----
val Gold = Color(0xFFE7B64F)              // primary action / brand accent (dark)
val onGold = Color(0xFF201503)
val Success = Color(0xFF3DDC84)           // connected / healthy
val onSuccess = Color(0xFF06290F)
val Ember = Color(0xFFF0555C)             // error / destructive
val onEmber = Color(0xFF39080B)
val Info = Color(0xFF6FA8DC)              // informational accent (used sparingly)

// ---- Light theme (premium: soft slate canvas + deep gold + emerald) ----
val Dawn = Color(0xFFF4F6F9)              // light app background (soft gray-blue, no glare)
val Cloud = Color(0xFFFFFFFF)             // card surface
val Haze = Color(0xFFEDF1F6)              // raised surface
val Frost = Color(0xFFE3E8EF)             // hairlines / borders (light)
val Ink = Color(0xFF1C2330)               // primary text (slate-navy, not pure black)
val InkDim = Color(0xFF5B6574)            // secondary text
val InkFaint = Color(0xFF8A93A2)          // tertiary / placeholder

val GoldRich = Color(0xFFD4A017)          // primary button container (light + dark) — deep champagne gold
val onGoldRich = Color(0xFF201500)
val GoldDeep = Color(0xFF8A6418)          // brand accent text ON LIGHT (≥4.5:1 on white)
val Jade = Color(0xFF228457)              // success (≥4.5:1 on white)
val onJade = Color(0xFFFFFFFF)
val Rose = Color(0xFFC03B41)              // error (≥4.5:1 on white)
val onRose = Color(0xFFFFFFFF)
val Sky = Color(0xFF2E6FB0)               // info (≥4.5:1 on white)

// Brand accent used as TEXT/highlight on the active theme's background.
val BrandAccentDark = Gold
val BrandAccentLight = GoldDeep

internal val LocalBrandAccent = staticCompositionLocalOf { BrandAccentDark }

/** Theme-aware brand accent (gold on dark, deep gold on light). */
val LocalBrandAccentProvider = staticCompositionLocalOf { BrandAccentDark }

private val DarkColors = darkColorScheme(
    primary = GoldRich,
    onPrimary = onGoldRich,
    primaryContainer = SurfaceTop,
    onPrimaryContainer = Gold,
    secondary = Success,
    onSecondary = onSuccess,
    secondaryContainer = SurfaceTop,
    onSecondaryContainer = Success,
    tertiary = Info,
    onTertiary = Obsidian,
    background = Obsidian,
    onBackground = Slate,
    surface = NightSurface,
    onSurface = Slate,
    surfaceVariant = NightSurfaceHigh,
    onSurfaceVariant = SlateDim,
    surfaceContainerLowest = Obsidian,
    surfaceContainerLow = NightSurface,
    surfaceContainer = NightSurfaceHigh,
    surfaceContainerHigh = SurfaceTop,
    surfaceContainerHighest = SurfaceTop,
    outline = Outline,
    outlineVariant = Outline,
    error = Ember,
    onError = onEmber,
    errorContainer = SurfaceTop,
    onErrorContainer = Ember,
)

private val LightColors = lightColorScheme(
    primary = GoldRich,
    onPrimary = onGoldRich,
    primaryContainer = Color(0xFFF6E8C0),
    onPrimaryContainer = GoldDeep,
    secondary = Jade,
    onSecondary = onJade,
    secondaryContainer = Color(0xFFDDF1E6),
    onSecondaryContainer = Color(0xFF16593A),
    tertiary = Sky,
    onTertiary = Color(0xFFFFFFFF),
    background = Dawn,
    onBackground = Ink,
    surface = Cloud,
    onSurface = Ink,
    surfaceVariant = Haze,
    onSurfaceVariant = InkDim,
    surfaceContainerLowest = Dawn,
    surfaceContainerLow = Cloud,
    surfaceContainer = Haze,
    surfaceContainerHigh = Frost,
    surfaceContainerHighest = Frost,
    outline = Color(0xFFD9DFE8),
    outlineVariant = Frost,
    error = Rose,
    onError = onRose,
    errorContainer = Color(0xFFFBE3E4),
    onErrorContainer = Color(0xFF7A1F24),
)

private val YaleTypography = Typography(
    displaySmall = Typography().displaySmall.copy(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.4).sp),
    headlineMedium = Typography().headlineMedium.copy(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
    titleLarge = Typography().titleLarge.copy(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
    titleMedium = Typography().titleMedium.copy(fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium),
    titleSmall = Typography().titleSmall.copy(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.2.sp),
    bodyLarge = Typography().bodyLarge.copy(fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = Typography().bodyMedium.copy(fontSize = 13.sp, lineHeight = 19.sp),
    bodySmall = Typography().bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = Typography().labelLarge.copy(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
    labelMedium = Typography().labelMedium.copy(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.1.sp),
    labelSmall = Typography().labelSmall.copy(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.4.sp),
)

private val YaleShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
)

/** Compose's MaterialTheme-scoped brand accent; resolves per active theme. */
@Composable
fun brandAccent(): Color = LocalBrandAccent.current

@Composable
fun YaleVPNTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalBrandAccent provides if (darkTheme) BrandAccentDark else BrandAccentLight,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = YaleTypography,
            shapes = YaleShapes,
            content = content,
        )
    }
}