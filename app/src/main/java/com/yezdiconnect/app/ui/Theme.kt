package com.yezdiconnect.app.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/** Palette from docs/design-notes.md. The app is dark-only. */
object YezdiColors {
    val Background = Color(0xFF0F1115)
    val Surface = Color(0xFF171A21)
    val SurfaceHigh = Color(0xFF1E2330)
    val Outline = Color(0xFF2A2F3A)
    val OutlineVariant = Color(0xFF232836)
    val Text = Color(0xFFE8EAF0)
    val Muted = Color(0xFF8A90A2)
    val Accent = Color(0xFFFF5A1F)
    val Ok = Color(0xFF2FB673)
    val Warn = Color(0xFFE8C547)
    val WarnBg = Color(0xFF3A3420)
    val Error = Color(0xFFFF7A6B)
    val ErrorBg = Color(0xFF3A1F1F)
}

private val colors = darkColorScheme(
    primary = YezdiColors.Accent,
    onPrimary = Color.White,
    primaryContainer = YezdiColors.Accent.copy(alpha = 0.18f),
    onPrimaryContainer = YezdiColors.Accent,
    secondary = YezdiColors.Ok,
    onSecondary = Color.White,
    background = YezdiColors.Background,
    onBackground = YezdiColors.Text,
    surface = YezdiColors.Surface,
    onSurface = YezdiColors.Text,
    surfaceVariant = YezdiColors.SurfaceHigh,
    onSurfaceVariant = YezdiColors.Muted,
    surfaceContainer = YezdiColors.Surface,
    surfaceContainerLow = YezdiColors.Surface,
    surfaceContainerHigh = YezdiColors.SurfaceHigh,
    surfaceContainerHighest = YezdiColors.SurfaceHigh,
    outline = YezdiColors.Outline,
    outlineVariant = YezdiColors.OutlineVariant,
    error = YezdiColors.Error,
    errorContainer = YezdiColors.ErrorBg,
    onErrorContainer = YezdiColors.Error,
)

private val type = Typography(
    // Screen title: "RIDES"
    headlineLarge = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 34.sp),
    // Stat value
    headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontSize = 13.sp, lineHeight = 19.sp),
    // Section label: "HOW TO CONNECT"
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.12.em),
    labelLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold),
)

private val shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(13.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun YezdiTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = type, shapes = shapes, content = content)
}
