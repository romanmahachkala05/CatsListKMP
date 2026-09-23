package com.example.catslist.presentation.catslist

import androidx.compose.runtime.Immutable
import com.example.catslist.domain.model.Cat
import com.example.catslist.presentation.UiText
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

/**
 * Whether the favorite overlay is still live. Not a screen-wide `UiStatus` as on other screens:
 * the list's loading, error and retry are Paging's, read from `LazyPagingItems.loadState` in the
 * Composable (ADR-0024).
 */
@Immutable
internal sealed interface CatsListFavoritesStatus {
    // Declared most-likely first, and every `when` over this mirrors the order.
    data object Live : CatsListFavoritesStatus

    /**
     * The stream ended in a failure, so [CatsListState.favoriteIds] will never change again.
     * Carries the message rather than leaving the screen to pick one: what to say depends on
     * which failure it was, and only the error handler knows that (ADR-0028).
     */
    data class Unavailable(
        val message: UiText,
    ) : CatsListFavoritesStatus
}

@Immutable
internal data class CatsListState(
    val favoritesStatus: CatsListFavoritesStatus = CatsListFavoritesStatus.Live,
    /** Ids of the favorited cats, overlaid onto the paged cats at render time (ADR-0024). */
    val favoriteIds: ImmutableSet<String> = persistentSetOf(),
)

internal sealed interface CatsListEvent {
    data class ToggleFavorite(
        val cat: Cat,
    ) : CatsListEvent
    data class Download(
        val cat: Cat,
    ) : CatsListEvent
}
