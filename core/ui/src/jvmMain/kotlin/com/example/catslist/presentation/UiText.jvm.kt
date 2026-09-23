package com.example.catslist.presentation

import androidx.compose.runtime.Composable
import kotlinx.collections.immutable.ImmutableList

@Composable
internal actual fun androidStringResource(id: Int, args: ImmutableList<Any>): String =
    error("UiText.AndroidResource reached desktop, where there are no Android string ids")
