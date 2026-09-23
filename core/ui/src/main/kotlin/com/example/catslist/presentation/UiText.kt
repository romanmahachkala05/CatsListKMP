package com.example.catslist.presentation

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.stringResource
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * UI text that does not know where it will be rendered. Only a Composable resolves it.
 *
 * `@Immutable` because every parameter typed `UiText` is otherwise compared by identity: the
 * compiler cannot infer stability for a sealed interface, since an implementation it has not
 * seen could be anything. The annotation is what keeps `ErrorMessage` and `EmptyMessage`
 * skippable (ADR-0029).
 */
@Immutable
sealed interface UiText {
    data class Raw(
        val value: String,
    ) : UiText

    data class Resource(
        @param:StringRes val id: Int,
        /**
         * [ImmutableList], not `List`: a raw `List` is an interface that could be a
         * `MutableList`, which made this class unstable by inference and the `@Immutable`
         * above a claim rather than a fact.
         */
        val args: ImmutableList<Any> = persistentListOf(),
    ) : UiText
}

@Suppress("SpreadOperator") // How `stringResource` takes format arguments; 0-2 of them here.
@Composable
fun UiText.resolve(): String = when (this) {
    is UiText.Raw -> value
    is UiText.Resource -> stringResource(id, *args.toTypedArray())
}

/** For resolving outside composition, such as inside a `LaunchedEffect`. */
@Suppress("SpreadOperator")
fun UiText.resolve(context: Context): String = when (this) {
    is UiText.Raw -> value
    is UiText.Resource -> context.getString(id, *args.toTypedArray())
}
