package com.telebridge.sms.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object TeleBridgeColors {
    val CanvasBg = Color(0xFFF8FAFC)       // Slate 50
    val Surface = Color(0xFFFFFFFF)        // White
    val SurfaceMuted = Color(0xFFF1F5F9)   // Slate 100
    val Border = Color(0xFFE2E8F0)         // Slate 200
    val TextPrimary = Color(0xFF0F172A)    // Slate 900
    val TextSecondary = Color(0xFF475569)  // Slate 600
    val TextMuted = Color(0xFF64748B)      // Slate 500
    val Accent = Color(0xFF0284C7)         // Sky 600
    val AccentDark = Color(0xFF0369A1)     // Sky 700
    val AccentSoft = Color(0xFFF0F9FF)     // Sky 50
    val Success = Color(0xFF16A34A)        // Emerald 600
    val SuccessSoft = Color(0xFFECFDF5)    // Emerald 50
    val Warning = Color(0xFFD97706)        // Amber 600
    val WarningSoft = Color(0xFFFFFBEB)    // Amber 50
    val Error = Color(0xFFDC2626)          // Red 600
    val ErrorSoft = Color(0xFFFEF2F2)      // Red 50
    val CodeBg = Color(0xFF0F172A)         // Slate 900
}

private val LightColors = lightColorScheme(
    primary = TeleBridgeColors.Accent,
    onPrimary = Color.White,
    primaryContainer = TeleBridgeColors.AccentSoft,
    onPrimaryContainer = TeleBridgeColors.AccentDark,
    secondary = TeleBridgeColors.TextSecondary,
    background = TeleBridgeColors.CanvasBg,
    onBackground = TeleBridgeColors.TextPrimary,
    surface = TeleBridgeColors.Surface,
    onSurface = TeleBridgeColors.TextPrimary,
    surfaceVariant = TeleBridgeColors.SurfaceMuted,
    onSurfaceVariant = TeleBridgeColors.TextSecondary,
    outline = TeleBridgeColors.Border,
    error = TeleBridgeColors.Error,
    onError = Color.White,
)

private val AppTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        color = TeleBridgeColors.TextPrimary,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        color = TeleBridgeColors.TextPrimary,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = TeleBridgeColors.TextPrimary,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = TeleBridgeColors.TextSecondary,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
)

@Composable
fun TeleBridgeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        content = content,
    )
}
