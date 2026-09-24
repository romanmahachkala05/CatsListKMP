package com.example.catslist.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

/** iOS has no wallpaper-derived palette; the theme falls back to its fixed one. */
@Composable
internal actual fun dynamicColorScheme(darkTheme: Boolean): ColorScheme? = null

/**
 * iOS's default status bar style already follows the system's light or dark mode, which is
 * what the theme follows too — so there is nothing to set.
 */
@Composable
internal actual fun SystemBarIcons(darkTheme: Boolean) = Unit
