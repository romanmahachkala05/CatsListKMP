package com.example.catslist.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

/** Desktop has no wallpaper-derived palette; the theme falls back to its fixed one. */
@Composable
internal actual fun dynamicColorScheme(darkTheme: Boolean): ColorScheme? = null

/** A desktop window's title bar belongs to the OS, not to the app. */
@Composable
internal actual fun SystemBarIcons(darkTheme: Boolean) = Unit
