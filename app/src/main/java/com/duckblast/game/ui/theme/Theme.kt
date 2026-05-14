package com.duckblast.game.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DuckBlastColors = lightColorScheme(
    primary = DuckBlastPrimary,
    onPrimary = DuckBlastSurface,
    primaryContainer = DuckBrown,
    onPrimaryContainer = DuckBlastSurface,
    secondary = DuckBlastSecondary,
    onSecondary = DuckBlastSurface,
    secondaryContainer = GrassLight,
    onSecondaryContainer = DuckBlastText,
    tertiary = DuckBlastAccent,
    onTertiary = DuckBlastText,
    background = DuckBlastBackground,
    onBackground = DuckBlastText,
    surface = DuckBlastSurface,
    onSurface = DuckBlastText,
    surfaceVariant = SkyBottom,
    onSurfaceVariant = DuckBlastText,
    outline = DuckBlastOutline,
    error = DuckBlastError,
    onError = DuckBlastSurface
)

@Composable
fun DuckBlastTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    val context = LocalContext.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = SkyTop.toArgb()
            window.navigationBarColor = HudBg.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                @Suppress("DEPRECATION")
                window.isNavigationBarContrastEnforced = false
            }
        }
    }
    MaterialTheme(
        colorScheme = DuckBlastColors,
        typography = DuckBlastTypography,
        content = content
    )
}
