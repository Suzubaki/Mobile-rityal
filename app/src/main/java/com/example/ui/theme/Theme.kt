package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = GoldAccent,
    onPrimary = Color(0xFF1F1A00),
    primaryContainer = Color(0xFF3B3100),
    onPrimaryContainer = Color(0xFFFFE088),
    secondary = Color(0xFFB0C6FF),
    onSecondary = Color(0xFF002D6F),
    secondaryContainer = Color(0xFF1E3A5F),
    onSecondaryContainer = Color(0xFFD9E2FF),
    tertiary = Color(0xFF80D5CB),
    onTertiary = Color(0xFF003732),
    background = SurfaceDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = CardDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = GraniteBorder,
    outlineVariant = Color(0xFF2E3846),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF8B6508),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDE8A),
    onPrimaryContainer = Color(0xFF2A1C00),
    secondary = Color(0xFF334A60),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6E3F8),
    onSecondaryContainer = Color(0xFF0F1D2A),
    tertiary = Color(0xFF006A62),
    onTertiary = Color.White,
    background = SurfaceLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFE9EDF2),
    onSurfaceVariant = TextSecondaryLight,
    outline = Color(0xFFC0CAD5),
    outlineVariant = Color(0xFFDDE3EA),
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun RitualCalculatorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    RitualCalculatorTheme(darkTheme = darkTheme, content = content)
}
