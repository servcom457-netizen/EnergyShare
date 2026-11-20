package com.batteryshare.ai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val NightBlueBg = Color(0xFF070D1F)
val NightBlueSurface = Color(0xFF0E1730)
val NightBlueElevated = Color(0xFF152242)
val NightBlueBorder = Color(0xFF23355E)
val ElectricBlue = Color(0xFF2563EB)
val ElectricBlueBright = Color(0xFF3B82F6)
val ActiveGreen = Color(0xFF10B981)
val ActiveGreenDark = Color(0xFF059669)
val WarningAmber = Color(0xFFF59E0B)
val CriticalRed = Color(0xFFEF4444)
val PureWhite = Color(0xFFFFFFFF)
val MutedSlate = Color(0xFF94A3B8)

private val BatteryShareColorScheme = darkColorScheme(
    primary = ElectricBlue,
    onPrimary = PureWhite,
    primaryContainer = NightBlueElevated,
    onPrimaryContainer = PureWhite,
    secondary = ActiveGreen,
    onSecondary = NightBlueBg,
    secondaryContainer = Color(0xFF064E3B),
    onSecondaryContainer = PureWhite,
    background = NightBlueBg,
    onBackground = PureWhite,
    surface = NightBlueSurface,
    onSurface = PureWhite,
    surfaceVariant = NightBlueElevated,
    onSurfaceVariant = MutedSlate,
    outline = NightBlueBorder,
    error = CriticalRed,
    onError = PureWhite
)

private val BatteryShareTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.5).sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp
    )
)

@Composable
fun BatteryShareTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BatteryShareColorScheme,
        typography = BatteryShareTypography,
        content = content
    )
}
