package com.example.catslist.presentation.favoritecats

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.catslist.domain.model.Cat
import com.example.catslist.feature.favorites.resources.Res
import com.example.catslist.feature.favorites.resources.favoritecats_empty_message
import com.example.catslist.presentation.UiText
import com.example.catslist.presentation.components.CatGridCells
import com.example.catslist.presentation.components.CatItem
import com.example.catslist.presentation.components.CatListPlaceholder
import com.example.catslist.presentation.components.EmptyMessage
import com.example.catslist.presentation.components.ErrorMessage
import com.example.catslist.presentation.theme.CatsListTheme
import kotlinx.collections.immutable.persistentListOf
import org.koin.compose.viewmodel.koinViewModel

/** Snackbars are collected by the app's shared host (see MainActivity), not here. */
@Composable
fun FavoriteCatsScreen(modifier: Modifier = Modifier, contentPadding: PaddingValues = PaddingValues()) {
    // A public function can't take an internal type, so koinViewModel()'s default lives on the
    // private overload and FavoriteCatsViewModel stays internal.
    FavoriteCatsScreen(modifier = modifier, contentPadding = contentPadding, viewModel = koinViewModel())
}

@Composable
private fun FavoriteCatsScreen(
    modifier: Modifier,
    contentPadding: PaddingValues,
    viewModel: FavoriteCatsViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    FavoriteCatsContent(
        state = state,
        onEvent = viewModel::onEvent,
        contentPadding = contentPadding,
        modifier = modifier,
    )
}

@Composable
internal fun FavoriteCatsContent(
    state: FavoriteCatsState,
    onEvent: (FavoriteCatsEvent) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    Surface(modifier = modifier.fillMaxSize()) {
        when (val status = state.status) {
            FavoriteCatsUiStatus.Content -> LazyVerticalGrid(
                columns = CatGridCells,
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
            ) {
                items(items = state.cats, key = { it.id }) { cat ->
                    CatItem(
                        cat = cat,
                        onFavoriteClick = { onEvent(FavoriteCatsEvent.RemoveFavorite(cat)) },
                        onDownloadClick = { onEvent(FavoriteCatsEvent.Download(cat)) },
                    )
                }
            }
            FavoriteCatsUiStatus.Empty -> EmptyMessage(UiText.Resource(Res.string.favoritecats_empty_message))
            FavoriteCatsUiStatus.Loading -> CatListPlaceholder(contentPadding = contentPadding)
            is FavoriteCatsUiStatus.Error -> ErrorMessage(message = status.message, onRetry = {
                onEvent(FavoriteCatsEvent.Retry)
            })
        }
    }
}

@Preview(name = "Content", showBackground = true)
@Composable
private fun FavoriteCatsContentPreview() {
    CatsListTheme {
        FavoriteCatsContent(
            state = FavoriteCatsState(
                status = FavoriteCatsUiStatus.Content,
                cats = persistentListOf(
                    Cat(id = "1", url = "", width = 300, height = 300, isFavorite = true),
                ),
            ),
            onEvent = {},
        )
    }
}

@Preview(name = "Empty", showBackground = true)
@Composable
private fun FavoriteCatsEmptyPreview() {
    CatsListTheme {
        FavoriteCatsContent(
            state = FavoriteCatsState(status = FavoriteCatsUiStatus.Empty),
            onEvent = {},
        )
    }
}
