package com.example.catslist.presentation.catslist

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.catslist.domain.model.AppError
import com.example.catslist.domain.model.AppErrorException
import com.example.catslist.domain.model.Cat
import com.example.catslist.presentation.theme.CatsListTheme
import com.example.catslist.testing.cat
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import org.junit.Test

/**
 * The feed on the desktop target, fed Paging state directly. `CatsListContentTest` covers the
 * same screen on a device, edge-to-edge; this one runs in `verify`.
 */
@OptIn(ExperimentalTestApi::class)
class CatsListContentDesktopTest {

    private val events = mutableListOf<CatsListEvent>()

    @Test
    fun `an empty feed says so instead of shimmering forever`() = runComposeUiTest {
        showContent(cats = emptyList(), states = settled())

        onNodeWithText("No cats to show right now.").assertIsDisplayed()
    }

    @Test
    fun `a failed first load names the failure and offers a retry`() = runComposeUiTest {
        val offline = LoadState.Error(AppErrorException(AppError.NoConnection))
        showContent(cats = emptyList(), states = settled(refresh = offline))

        onNodeWithText("You're offline. Check your connection and try again.").assertIsDisplayed()
        onNodeWithText("Retry").assertIsDisplayed()
    }

    @Test
    fun `a card's star asks to toggle that cat`() = runComposeUiTest {
        val cats = listOf(cat("1"), cat("2"))
        showContent(cats = cats, states = settled())

        onAllNodesWithContentDescription("Add or remove this cat from favorites")[1].performClick()

        assertThat(events).containsExactly(CatsListEvent.ToggleFavorite(cats[1]))
    }

    /**
     * The skeleton and the cards shimmer forever, and an animation that never ends never lets
     * the test clock go idle — so the clock is driven by hand, as in the device test.
     */
    private fun ComposeUiTest.showContent(cats: List<Cat>, states: LoadStates) {
        mainClock.autoAdvance = false
        setContent {
            CatsListTheme {
                CatsListContent(
                    pagingItems = flowOf(PagingData.from(cats, states)).collectAsLazyPagingItems(),
                    onEvent = { events += it },
                )
            }
        }
        // Once for the page to reach composition, once for the skeleton's minimum hold.
        mainClock.advanceTimeBy(SETTLE_MILLIS)
        mainClock.advanceTimeBy(SETTLE_MILLIS)
    }

    private fun settled(refresh: LoadState = LoadState.NotLoading(endOfPaginationReached = false)) = LoadStates(
        refresh = refresh,
        prepend = LoadState.NotLoading(endOfPaginationReached = true),
        append = LoadState.NotLoading(endOfPaginationReached = true),
    )

    private companion object {
        const val SETTLE_MILLIS = 1_000L
    }
}
