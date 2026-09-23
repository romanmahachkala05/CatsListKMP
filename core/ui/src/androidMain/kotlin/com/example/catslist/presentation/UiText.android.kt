package com.example.catslist.presentation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import kotlinx.collections.immutable.ImmutableList

@Suppress("SpreadOperator")
@Composable
internal actual fun androidStringResource(id: Int, args: ImmutableList<Any>): String =
    stringResource(id, *args.toTypedArray())

/** [load], plus the `Context` an [UiText.AndroidResource] resolves against. */
@Suppress("SpreadOperator")
suspend fun UiText.load(context: Context): String = when (this) {
    is UiText.AndroidResource -> context.getString(id, *args.toTypedArray())
    else -> load()
}
