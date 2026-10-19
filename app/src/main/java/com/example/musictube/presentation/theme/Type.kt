package com.example.musictube.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.musictube.R

val SpaceGrotesk = FontFamily(
    Font(R.font.space_grotesk_500, FontWeight.Medium),
    Font(R.font.space_grotesk_600, FontWeight.SemiBold),
    Font(R.font.space_grotesk_700, FontWeight.Bold)
)

val Manrope = FontFamily(
    Font(R.font.manrope_400, FontWeight.Normal),
    Font(R.font.manrope_500, FontWeight.Medium),
    Font(R.font.manrope_600, FontWeight.SemiBold),
    Font(R.font.manrope_700, FontWeight.Bold),
    Font(R.font.manrope_800, FontWeight.ExtraBold)
)

private fun display(size: Int, weight: FontWeight, tracking: Float, lineHeight: Int) = TextStyle(
    fontFamily = SpaceGrotesk,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking.sp
)

private fun body(size: Int, weight: FontWeight, lineHeight: Int, tracking: Float = 0f) = TextStyle(
    fontFamily = Manrope,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking.sp
)

val SoundBuzzTypography = Typography(
    displayLarge = display(48, FontWeight.Bold, -1.5f, 54),
    displayMedium = display(40, FontWeight.Bold, -1.2f, 46),
    displaySmall = display(34, FontWeight.Bold, -1f, 40),
    headlineLarge = display(30, FontWeight.Bold, -0.8f, 34),
    headlineMedium = display(26, FontWeight.Bold, -0.6f, 31),
    headlineSmall = display(22, FontWeight.Bold, -0.4f, 28),
    titleLarge = display(20, FontWeight.SemiBold, -0.3f, 26),
    titleMedium = body(16, FontWeight.Bold, 22),
    titleSmall = body(14, FontWeight.Bold, 20),
    bodyLarge = body(16, FontWeight.Medium, 22),
    bodyMedium = body(14, FontWeight.Medium, 20),
    bodySmall = body(12, FontWeight.Medium, 16),
    labelLarge = body(14, FontWeight.Bold, 20),
    labelMedium = body(12, FontWeight.Bold, 16),
    labelSmall = body(11, FontWeight.ExtraBold, 14, tracking = 1.2f)
)
