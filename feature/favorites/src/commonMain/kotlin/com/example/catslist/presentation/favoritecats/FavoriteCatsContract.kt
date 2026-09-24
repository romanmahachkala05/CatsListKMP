package com.example.catslist.presentation.favoritecats

import androidx.compose.runtime.Immutable
import com.example.catslist.domain.model.Cat
import com.example.catslist.presentation.UiText
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal sealed interface FavoriteCatsUiStatus {
    // Declared most-likely first, and every `when` over this mirrors the order.
    data object Content : FavoriteCatsUiStatus
    data object Empty : FavoriteCatsUiStatus
    data object Loading : FavoriteCatsUiStatus

    /** No `retryable` flag: [FavoriteCatsEvent.Retry] recovers from the one failure there is. */
    data class Error(
        val message: UiText,
    ) : FavoriteCatsUiStatus
}

@Immutable
internal data class FavoriteCatsState(
    val status: FavoriteCatsUiStatus = FavoriteCatsUiStatus.Loading,
    val cats: ImmutableList<Cat> = persistentListOf(),
)

internal sealed interface FavoriteCatsEvent {
    /** The user asking to recover from an error: resubscribes to the favorites stream. */
    data object Retry : FavoriteCatsEvent

    data class RemoveFavorite(
        val cat: Cat,
    ) : FavoriteCatsEvent
    data class Download(
        val cat: Cat,
    ) : FavoriteCatsEvent
}
