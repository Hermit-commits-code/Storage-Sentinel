package com.example.storagesentinel.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = SecondaryShieldBlue,       // A lighter blue for better contrast in dark mode
    secondary = EyeDetailBlue,           // The accent blue
    background = Color(0xFF0D1B2A),       // A very dark, deep blue instead of black
    surface = Color(0xFF1B263B),         // A slightly lighter dark blue for cards/surfaces
    surfaceVariant = ContainerGray,      // The user's specified gray for variant surfaces
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = ContainerHighlight,   // The near-white for text on dark background
    onSurface = ContainerHighlight,      // The near-white for text on dark surfaces
    onSurfaceVariant = Color.Black       // Black text on the light gray variant surface
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryShieldBlue,         // The main, deep blue
    secondary = SecondaryShieldBlue,
    background = ContainerHighlight,     // The near-white background
    surface = Color.White,               // Pure white for cards to pop
    surfaceVariant = ContainerGray,      // The user's specified gray for variant surfaces
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black,
    onSurfaceVariant = Color.Black       // Black text on the light gray variant surface
)

@Composable
fun StorageSentinelTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            // Use WindowInsetsControllerCompat instead of deprecated statusBarColor
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
