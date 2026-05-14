package com.duckblast.game.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Pixel font family. After placing `press_start_2p.ttf` (OFL) in `res/font/`,
 * swap the right-hand side to:
 *
 *     val PixelFont = FontFamily(Font(R.font.press_start_2p, FontWeight.Normal))
 *
 * Keeping it as `FontFamily.Monospace` here so the project builds before the
 * TTF is added.
 */
val PixelFont: FontFamily = FontFamily.Monospace

private val pixelDisplay = TextStyle(
    fontFamily = PixelFont,
    fontWeight = FontWeight.Normal,
    letterSpacing = 0.05.sp
)

val DuckBlastTypography = Typography(
    displayLarge = pixelDisplay.copy(fontSize = 40.sp, lineHeight = 48.sp),
    displayMedium = pixelDisplay.copy(fontSize = 32.sp, lineHeight = 40.sp),
    displaySmall = pixelDisplay.copy(fontSize = 24.sp, lineHeight = 32.sp),
    headlineLarge = pixelDisplay.copy(fontSize = 28.sp, lineHeight = 36.sp),
    headlineMedium = pixelDisplay.copy(fontSize = 22.sp, lineHeight = 30.sp),
    headlineSmall = pixelDisplay.copy(fontSize = 18.sp, lineHeight = 26.sp),
    titleLarge = pixelDisplay.copy(fontSize = 18.sp, lineHeight = 24.sp),
    titleMedium = pixelDisplay.copy(fontSize = 14.sp, lineHeight = 20.sp),
    titleSmall = pixelDisplay.copy(fontSize = 12.sp, lineHeight = 18.sp),
    bodyLarge = pixelDisplay.copy(fontSize = 14.sp, lineHeight = 20.sp),
    bodyMedium = pixelDisplay.copy(fontSize = 12.sp, lineHeight = 18.sp),
    bodySmall = pixelDisplay.copy(fontSize = 10.sp, lineHeight = 14.sp),
    labelLarge = pixelDisplay.copy(fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = pixelDisplay.copy(fontSize = 11.sp, lineHeight = 16.sp),
    labelSmall = pixelDisplay.copy(fontSize = 9.sp, lineHeight = 12.sp)
)
