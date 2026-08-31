package com.example.momentra.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

import com.example.momentra.ui.theme.MomentraBrandColors

private val DarkColorScheme = darkColorScheme(
    primary = MomentraBrandColors.Indigo500,
    onPrimary = MomentraBrandColors.TextOnDark,
    secondary = MomentraBrandColors.Cta,
    onSecondary = MomentraBrandColors.TextOnEmber,
    background = MomentraBrandColors.Brand,
    onBackground = MomentraBrandColors.TextOnDark,
    surface = MomentraBrandColors.Indigo700,
    onSurface = MomentraBrandColors.TextOnDark,
    surfaceVariant = MomentraBrandColors.Indigo500,
    onSurfaceVariant = MomentraBrandColors.TextOnDark,
    outline = MomentraBrandColors.Indigo300,
)

@Composable
fun MomentraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = DarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = MomentraBrandColors.Brand.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MomentraTypography,
        content = content,
    )
}
