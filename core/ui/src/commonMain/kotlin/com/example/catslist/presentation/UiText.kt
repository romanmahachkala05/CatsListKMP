package com.example.catslist.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

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
        val id: StringResource,
        /**
         * [ImmutableList], not `List`: a raw `List` is an interface that could be a
         * `MutableList`, which made this class unstable by inference and the `@Immutable`
         * above a claim rather than a fact.
         */
        val args: ImmutableList<Any> = persistentListOf(),
    ) : UiText

    /**
     * An Android `R.string` id, for the modules that are still Android-only. Compose resources
     * need the multiplatform plugin, so until `:feature:favorites` and `:feature:feed` move to
     * `commonMain` their own strings can only be `R` ids. Transitional: it goes when they do,
     * and nothing multiplatform may create one — on desktop it has nothing to resolve against.
     */
    data class AndroidResource(
        val id: Int,
        val args: ImmutableList<Any> = persistentListOf(),
    ) : UiText
}

@Suppress("SpreadOperator") // How `stringResource` takes format arguments; 0-2 of them here.
@Composable
fun UiText.resolve(): String = when (this) {
    is UiText.Raw -> value
    is UiText.Resource -> stringResource(id, *args.toTypedArray())
    is UiText.AndroidResource -> androidStringResource(id, args)
}

@Composable
internal expect fun androidStringResource(id: Int, args: ImmutableList<Any>): String

/**
 * For resolving outside composition, such as inside a `LaunchedEffect`. Suspends because
 * Compose resources are read from files rather than from an Android `Context`. An
 * [UiText.AndroidResource] needs that `Context`, so on Android use the `load(context)` overload.
 */
@Suppress("SpreadOperator")
suspend fun UiText.load(): String = when (this) {
    is UiText.Raw -> value
    is UiText.Resource -> getString(id, *args.toTypedArray())
    is UiText.AndroidResource -> error("An Android string id needs a Context: call load(context)")
}
