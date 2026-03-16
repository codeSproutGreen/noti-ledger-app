package com.notiledger.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF8CB4F8),
    onPrimary = Color(0xFF003062),
    primaryContainer = Color(0xFF00468A),
    secondary = Color(0xFFBBC7DB),
    tertiary = Color(0xFFD6BEE4),
    background = Color(0xFF1A1C1E),
    surface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFF43474E),
    error = Color(0xFFFFB4AB)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1B6EF3),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8E2FF),
    secondary = Color(0xFF565E71),
    tertiary = Color(0xFF715573),
    background = Color(0xFFFBFCFF),
    surface = Color(0xFFFBFCFF),
    surfaceVariant = Color(0xFFE0E2EC),
    error = Color(0xFFBA1A1A)
)

@Composable
fun NotiLedgerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

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
        content = content
    )
}
