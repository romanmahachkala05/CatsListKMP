package com.example.catslist.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGray80,
    tertiary = Pink80,
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGray40,
    tertiary = Pink40,
)

@Composable
fun CatsListTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = (if (dynamicColor) dynamicColorScheme(darkTheme) else null)
        ?: if (darkTheme) DarkColorScheme else LightColorScheme
    SystemBarIcons(darkTheme)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}

/** The wallpaper-derived scheme where the platform has one (Android 12+), else null. */
@Composable
internal expect fun dynamicColorScheme(darkTheme: Boolean): ColorScheme?

/** Tints the status bar's icons to stay legible over the theme. A no-op without one. */
@Composable
internal expect fun SystemBarIcons(darkTheme: Boolean)
