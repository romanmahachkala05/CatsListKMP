package com.example.catslist.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Material 3 has an `error` role but no success one. Two values, because the scheme flips. */
val SuccessLight = Color(0xFF2E7D32)
val SuccessDark = Color(0xFF81C784)

val successColor: Color
    @Composable get() = if (isSystemInDarkTheme()) SuccessDark else SuccessLight

val Purple80 = Color(0xFFD0BCFF)
val PurpleGray80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGray40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
