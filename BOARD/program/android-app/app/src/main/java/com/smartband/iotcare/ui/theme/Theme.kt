package com.smartband.iotcare.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = ElectricBlue,
    secondary = Cyan400,
    tertiary = Emerald500,
    background = Slate900,
    surface = Slate800,
    onPrimary = Slate50,
    onSecondary = Slate900,
    onBackground = Slate50,
    onSurface = Slate50,
    error = CrimsonRed
)

private val LightColorScheme = lightColorScheme(
    primary = ElectricBlue,
    secondary = Indigo600,
    tertiary = Emerald700,
    background = Slate50,
    surface = Slate50,
    onPrimary = Slate50,
    onSecondary = Slate50,
    onBackground = Slate900,
    onSurface = Slate900,
    error = CrimsonRed
)

@Composable
fun SmartbandCareTheme(
    darkTheme: Boolean = true, // Default to sleek IoT Dark Mode
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
