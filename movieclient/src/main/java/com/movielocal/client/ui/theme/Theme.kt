package com.movielocal.client.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val iNoxBlue = Color(0xFF4169E1)
val iNoxDarkBlue = Color(0xFF2854C7)
val iNoxBlack = Color(0xFF0A0A0A)
val iNoxDarkGray = Color(0xFF1A1A1A)
val iNoxGray = Color(0xFF2A2A2A)
val iNoxLightGray = Color(0xFF3A3A3A)

private val DarkColorScheme = darkColorScheme(
    primary = iNoxBlue,
    primaryContainer = iNoxDarkBlue,
    secondary = iNoxDarkBlue,
    secondaryContainer = iNoxGray,
    tertiary = iNoxLightGray,
    background = iNoxBlack,
    surface = iNoxDarkGray,
    surfaceVariant = iNoxGray,
    onPrimary = Color.White,
    onPrimaryContainer = Color.White,
    onSecondary = Color.White,
    onSecondaryContainer = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFB0B0B0)
)

@Composable
fun MovieLocalTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
