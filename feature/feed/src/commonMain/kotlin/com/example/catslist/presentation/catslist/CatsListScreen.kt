package com.example.catslist.presentation.catslist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.example.catslist.domain.model.AppError
import com.example.catslist.domain.model.AppErrorException
import com.example.catslist.domain.model.Cat
import com.example.catslist.feature.feed.resources.Res
import com.example.catslist.feature.feed.resources.catslist_action_retry
import com.example.catslist.feature.feed.resources.catslist_empty_message
import com.example.catslist.feature.feed.resources.catslist_error_favorites_unavailable
import com.example.catslist.presentation.UiText
import com.example.catslist.presentation.asAppError
import com.example.catslist.presentation.components.CatItem
import com.example.catslist.presentation.components.CatItemPlaceholder
import com.example.catslist.presentation.components.CatListPlaceholder
import com.example.catslist.presentation.components.CatPullToRefresh
import com.example.catslist.presentation.components.EmptyMessage
import com.example.catslist.presentation.components.ErrorMessage
import com.example.catslist.presentation.components.RefreshSignal
import com.example.catslist.presentation.heldAtLeast
import com.example.catslist.presentation.resolve
import com.example.catslist.presentation.theme.CatsListTheme
import com.example.catslist.presentation.toUiText
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.flow.flowOf
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Snackbars are collected by the app's shared host (see MainActivity), not here. */
@Composable
fun CatsListScreen(modifier: Modifier = Modifier, contentPadding: PaddingValues = PaddingValues()) {
    // A public function can't take an internal type, so koinViewModel()'s default lives on the
    // private overload and CatsListViewModel stays internal.
    CatsListScreen(modifier = modifier, contentPadding = contentPadding, viewModel = koinViewModel())
}

@Composable
private fun CatsListScreen(
    modifier: Modifier,
    contentPadding: PaddingValues,
    viewModel: CatsListViewModel,
) {
    val pagingItems = viewModel.pagedCats.collectAsLazyPagingItems()
    val state by viewModel.state.collectAsStateWithLifecycle()
    CatsListContent(
        pagingItems = pagingItems,
        state = state,
        onEvent = viewModel::onEvent,
        contentPadding = contentPadding,
        modifier = modifier,
    )
}

@Composable
internal fun CatsListContent(
    pagingItems: LazyPagingItems<Cat>,
    onEvent: (CatsListEvent) -> Unit,
    modifier: Modifier = Modifier,
    state: CatsListState = CatsListState(),
    contentPadding: PaddingValues = PaddingValues(),
) {
    Surface(modifier = modifier.fillMaxSize()) {
        // Having cats decides this, not the load state: once cats are up, a failed reload is
        // the append footer's problem rather than a reason to blank the screen.
        val refresh = pagingItems.loadState.refresh
        val hasNoCats = pagingItems.itemCount == 0
        // Only a load in flight counts: Paging never marks a refresh `endOfPaginationReached`,
        // so a finished one is `NotLoading` whether or not it found any cats.
        val isStillWorking = refresh is LoadState.Loading || pagingItems.loadState.append is LoadState.Loading
        // Only the skeleton is held: holding the empty branch would keep claiming "no cats"
        // over a feed that has since arrived.
        val showSkeleton = heldAtLeast(hasNoCats && isStillWorking, SKELETON_MINIMUM_MILLIS)
        when {
            showSkeleton -> CatListPlaceholder(contentPadding = contentPadding)
            hasNoCats -> EmptyFeed(refresh = refresh, onRetry = pagingItems::retry)
            else -> CatsFeed(
                pagingItems = pagingItems,
                state = state,
                onEvent = onEvent,
                contentPadding = contentPadding,
            )
        }
    }
}

/** Why there are no cats. Reached only once the caller has ruled out "still working". */
@Composable
private fun EmptyFeed(refresh: LoadState, onRetry: () -> Unit) {
    if (refresh is LoadState.Error) {
        // Not one message for every failure anymore: the PagingSource classified it, so
        // being offline, being rate-limited and a 503 each read differently (ADR-0028).
        ErrorMessage(message = refresh.error.asAppError().toUiText(), onRetry = onRetry)
    } else {
        EmptyMessage(UiText.Resource(Res.string.catslist_empty_message))
    }
}

/** Pulling restarts at page 0: TheCatAPI has no "newer than what I have" signal to prepend. */
@Composable
private fun CatsFeed(
    pagingItems: LazyPagingItems<Cat>,
    state: CatsListState,
    onEvent: (CatsListEvent) -> Unit,
    contentPadding: PaddingValues,
) {
    // `loadState.refresh`, not `.mediator`: there is no RemoteMediator, so mediator is always
    // null and reading it would leave the indicator permanently silent.
    val signal = when (pagingItems.loadState.refresh) {
        is LoadState.Loading -> RefreshSignal.Running
        is LoadState.Error -> RefreshSignal.Failed
        else -> RefreshSignal.Idle
    }
    CatPullToRefresh(
        signal = signal,
        onRefresh = pagingItems::refresh,
        modifier = Modifier.fillMaxSize(),
        topInset = contentPadding.calculateTopPadding(),
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = contentPadding) {
            when (val favorites = state.favoritesStatus) {
                CatsListFavoritesStatus.Live -> Unit
                // Above the cats, not in place of them: only the star icons are stale.
                is CatsListFavoritesStatus.Unavailable -> item {
                    ListNotice {
                        Text(
                            text = favorites.message.resolve(),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            items(count = pagingItems.itemCount, key = pagingItems.itemKey { it.id }) { index ->
                val cat = pagingItems[index] ?: return@items
                // The paged cat carries no favorite status, so it is applied at render time:
                // a recomposition rather than another Paging generation.
                val displayCat = cat.copy(isFavorite = cat.id in state.favoriteIds)
                CatItem(
                    cat = displayCat,
                    onFavoriteClick = { onEvent(CatsListEvent.ToggleFavorite(displayCat)) },
                    onDownloadClick = { onEvent(CatsListEvent.Download(displayCat)) },
                )
            }

            when (val append = pagingItems.loadState.append) {
                // The next card's skeleton, so the page swaps shimmer for photo in place.
                is LoadState.Loading -> item { CatItemPlaceholder() }
                is LoadState.Error -> item {
                    ListNotice {
                        Text(
                            // Classified the same way as a failed refresh, so a rate-limited
                            // next page says so instead of blaming the connection (ADR-0028).
                            text = append.error.asAppError().toUiText().resolve(),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Button(onClick = pagingItems::retry) {
                            Text(text = stringResource(Res.string.catslist_action_retry))
                        }
                    }
                }
                is LoadState.NotLoading -> Unit
            }
        }
    }
}

/** In-list stand-in for [ErrorMessage], which `fillMaxSize()`s and would take the viewport. */
@Composable
private fun ListNotice(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        content()
    }
}

private const val SKELETON_MINIMUM_MILLIS = 300L

@Preview(name = "Content", showBackground = true)
@Composable
private fun CatsListContentPreview() {
    CatsListTheme {
        val cats = listOf(
            Cat(id = "1", url = "", width = 300, height = 300),
            Cat(id = "2", url = "", width = 300, height = 300),
        )
        val pagingItems = flowOf(PagingData.from(cats)).collectAsLazyPagingItems()
        CatsListContent(
            pagingItems = pagingItems,
            state = CatsListState(favoriteIds = persistentSetOf("2")),
            onEvent = {},
        )
    }
}

@Preview(name = "Favorites unavailable", showBackground = true)
@Composable
private fun CatsListFavoritesUnavailablePreview() {
    CatsListTheme {
        val cats = listOf(Cat(id = "1", url = "", width = 300, height = 300))
        val pagingItems = flowOf(PagingData.from(cats)).collectAsLazyPagingItems()
        CatsListContent(
            pagingItems = pagingItems,
            state = CatsListState(
                favoritesStatus = CatsListFavoritesStatus.Unavailable(
                    UiText.Resource(Res.string.catslist_error_favorites_unavailable),
                ),
            ),
            onEvent = {},
        )
    }
}

@Preview(name = "Loading", showBackground = true)
@Composable
private fun CatsListLoadingPreview() {
    CatsListTheme {
        val loadingStates = LoadStates(
            refresh = LoadState.Loading,
            prepend = LoadState.NotLoading(endOfPaginationReached = false),
            append = LoadState.NotLoading(endOfPaginationReached = false),
        )
        val emptyFeed = flowOf(PagingData.from(emptyList<Cat>(), sourceLoadStates = loadingStates))
        CatsListContent(pagingItems = emptyFeed.collectAsLazyPagingItems(), onEvent = {})
    }
}

@Preview(name = "Error", showBackground = true)
@Composable
private fun CatsListErrorPreview() {
    CatsListTheme {
        val errorStates = LoadStates(
            // A real AppError, so the preview renders the message the screen will actually
            // show rather than the Unknown fallback a bare exception maps to.
            refresh = LoadState.Error(AppErrorException(AppError.NoConnection)),
            prepend = LoadState.NotLoading(endOfPaginationReached = false),
            append = LoadState.NotLoading(endOfPaginationReached = false),
        )
        val emptyFeed = flowOf(PagingData.from(emptyList<Cat>(), sourceLoadStates = errorStates))
        val pagingItems = emptyFeed.collectAsLazyPagingItems()
        CatsListContent(pagingItems = pagingItems, onEvent = {})
    }
}
