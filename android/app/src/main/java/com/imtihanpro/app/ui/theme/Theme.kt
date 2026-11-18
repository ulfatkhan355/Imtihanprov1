package com.imtihanpro.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class ImtihanExtendedColors(
    val canvasDeep: Color,
    val canvasMid: Color,
    val cardSurface: Color,
    val cardElevated: Color,
    val cardBorder: Color,
    val glassHighlight: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accentBlue: Color,
    val success: Color,
    val warningGold: Color,
    val danger: Color,
    val isDark: Boolean,
    val cyanPulse: Color = SkySilverAccent,
    val glassTopSheen: Color = GlassSpecularTop,
)

private val DarkExtended = ImtihanExtendedColors(
    canvasDeep = NavyCanvasDeep,
    canvasMid = NavyCanvasMid,
    cardSurface = NavySurfaceCard,
    cardElevated = NavySurfaceElevated,
    cardBorder = SilverBlueBorder,
    glassHighlight = SilverGlassHighlight,
    textPrimary = SilverTextPrimary,
    textSecondary = SilverTextSecondary,
    textMuted = SilverTextMuted,
    accentBlue = ElectricBluePrimary,
    success = EmeraldSuccess,
    warningGold = AmberGold,
    danger = CrimsonAlert,
    isDark = true,
    cyanPulse = SkySilverAccent,
    glassTopSheen = GlassSpecularTop,
)

private val LightExtended = ImtihanExtendedColors(
    canvasDeep = LightCanvas,
    canvasMid = LightSurfaceElevated,
    cardSurface = LightSurfaceCard,
    cardElevated = LightSurfaceElevated,
    cardBorder = LightSilverBorder,
    glassHighlight = Color(0x141D4ED8),
    textPrimary = LightInkPrimary,
    textSecondary = LightInkSecondary,
    textMuted = Color(0xFF64748B),
    accentBlue = LightBluePrimary,
    success = Color(0xFF16A34A),
    warningGold = Color(0xFFD97706),
    danger = Color(0xFFDC2626),
    isDark = false,
    cyanPulse = LightBluePrimary,
    glassTopSheen = Color(0xAAFFFFFF),
)

val LocalImtihanColors = staticCompositionLocalOf { DarkExtended }

private val DarkColorScheme = darkColorScheme(
    primary = ElectricBluePrimary,
    onPrimary = Color.White,
    secondary = SkySilverAccent,
    background = NavyCanvasDeep,
    onBackground = SilverTextPrimary,
    surface = NavySurfaceCard,
    onSurface = SilverTextPrimary,
    surfaceVariant = NavySurfaceElevated,
    onSurfaceVariant = SilverTextSecondary,
    outline = SilverBlueBorder,
    error = CrimsonAlert
)

private val LightColorScheme = lightColorScheme(
    primary = LightBluePrimary,
    onPrimary = Color.White,
    secondary = ElectricBluePrimary,
    background = LightCanvas,
    onBackground = LightInkPrimary,
    surface = LightSurfaceCard,
    onSurface = LightInkPrimary,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = LightInkSecondary,
    outline = LightSilverBorder,
    error = Color(0xFFDC2626)
)

@Composable
fun ImtihanProTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val extended = if (darkTheme) DarkExtended else LightExtended
    val scheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalImtihanColors provides extended) {
        MaterialTheme(
            colorScheme = scheme,
            typography = ImtihanTypography,
            content = content
        )
    }
}
