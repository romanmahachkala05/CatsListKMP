package com.example.catslist.presentation.catslist

import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.catslist.core.designsystem.R as designsystemR
import com.example.catslist.core.ui.R as uiR
import com.example.catslist.domain.model.AppError
import com.example.catslist.domain.model.AppErrorException
import com.example.catslist.domain.model.Cat
import com.example.catslist.feature.feed.R
import com.example.catslist.presentation.UiText
import com.example.catslist.presentation.components.CAT_CARD_TAG
import com.example.catslist.presentation.components.CAT_LIST_PLACEHOLDER_TAG
import com.example.catslist.presentation.theme.CatsListTheme
import com.example.catslist.testing.cat
import com.google.common.truth.Truth.assertThat
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The feed's branches are decided by `LazyPagingItems.loadState` (ADR-0024), which only exists
 * inside composition — so these are the only tests that can reach them at all.
 */
@RunWith(AndroidJUnit4::class)
class CatsListContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    /** Set from composition, because only the composable can read the window's insets. */
    private var topInsetPx = 0

    /** The app runs edge-to-edge, so the composable under test has to as well. */
    @Before
    fun goEdgeToEdge() {
        composeRule.runOnUiThread { composeRule.activity.enableEdgeToEdge() }
    }

    /**
     * The skeleton and the card shimmer forever, and an animation that never ends never lets
     * the test clock go idle. Driving the clock by hand is what keeps assertions from hanging.
     */
    @Before
    fun holdTheClock() {
        composeRule.mainClock.autoAdvance = false
    }

    /**
     * The regression test for the endless skeleton: Paging never reports
     * `endOfPaginationReached` on a refresh, so a feed that came back empty looks exactly like
     * one still loading unless the screen reads the loading flags themselves.
     */
    @Test
    fun anEmptyFeedSaysSoInsteadOfShimmeringForever() {
        showContent(cats = emptyList(), states = settled())

        composeRule.onNodeWithText(string(R.string.catslist_empty_message)).assertIsDisplayed()
        composeRule.onNodeWithTag(CAT_LIST_PLACEHOLDER_TAG).assertDoesNotExist()
    }

    @Test
    fun aRefreshStillRunningShowsTheSkeleton() {
        showContent(cats = emptyList(), states = settled(refresh = LoadState.Loading))

        composeRule.onNodeWithTag(CAT_LIST_PLACEHOLDER_TAG).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.catslist_empty_message)).assertDoesNotExist()
    }

    /**
     * `AppErrorException`, not a bare `IOException`: `CatFeedPagingSource` classifies every
     * failure before Paging ever sees it (ADR-0028), so this is the error the screen really
     * gets — and an unwrapped one would land on the `Unknown` fallback instead.
     */
    @Test
    fun aFailedRefreshWithNoCatsOffersARetry() {
        showContent(
            cats = emptyList(),
            states = settled(refresh = LoadState.Error(AppErrorException(AppError.NoConnection))),
        )

        composeRule.onNodeWithText(string(uiR.string.common_error_no_connection)).assertIsDisplayed()
        composeRule.onNodeWithText(string(designsystemR.string.common_action_retry)).assertIsDisplayed()
    }

    /** The point of the classification: two failures, two different things said about them. */
    @Test
    fun aRateLimitedRefreshSaysSomethingElseEntirely() {
        showContent(
            cats = emptyList(),
            states = settled(refresh = LoadState.Error(AppErrorException(AppError.RateLimited))),
        )

        composeRule.onNodeWithText(string(uiR.string.common_error_rate_limited)).assertIsDisplayed()
        composeRule.onNodeWithText(string(uiR.string.common_error_no_connection)).assertDoesNotExist()
    }

    /** The paged cat carries no favorite status; the screen overlays it (ADR-0024). */
    @Test
    fun theFavoriteOverlayReachesTheEventTheCardSends() {
        val events = mutableListOf<CatsListEvent>()
        showContent(
            cats = listOf(cat("1"), cat("2")),
            states = settled(),
            state = CatsListState(favoriteIds = persistentSetOf("2")),
            onEvent = { events += it },
        )

        composeRule.onAllNodesWithContentDescription(string(designsystemR.string.common_cd_favorite_cat))[1]
            .performClick()

        assertThat(events).containsExactly(CatsListEvent.ToggleFavorite(cat("2").copy(isFavorite = true)))
    }

    @Test
    fun aBrokenFavoritesStreamIsAnnouncedAboveTheCatsRatherThanInsteadOfThem() {
        showContent(
            cats = listOf(cat("1")),
            states = settled(),
            state = CatsListState(
                favoritesStatus = CatsListFavoritesStatus.Unavailable(
                    UiText.Resource(R.string.catslist_error_favorites_unavailable),
                ),
            ),
        )

        composeRule.onNodeWithText(string(R.string.catslist_error_favorites_unavailable)).assertIsDisplayed()
        composeRule.onAllNodesWithContentDescription(string(designsystemR.string.common_cd_favorite_cat))[0]
            .assertIsDisplayed()
    }

    @Test
    fun theFeedRunsUnderTheStatusBarAndKeepsItsCatsClearOfIt() {
        showContent(cats = listOf(cat("1")), states = settled())

        val firstCard = composeRule.onAllNodesWithTag(CAT_CARD_TAG)[0].fetchSemanticsNode()

        assertFullBleedButClearOfTheStatusBar(firstCard.boundsInWindow.top)
    }

    @Test
    fun aFailedNextPageIsAFooterUnderTheCatsThatAreUp() {
        showContent(
            cats = listOf(cat("1")),
            states = settled(append = LoadState.Error(AppErrorException(AppError.Timeout))),
        )

        composeRule.onNodeWithText(string(uiR.string.common_error_timeout)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.catslist_action_retry)).assertIsDisplayed()
    }

    /**
     * Edge-to-edge is two claims, and a test of it owes both: the window runs the full height
     * of the display, *and* the content is padded clear of the bar drawn over it. The inset
     * itself is checked too — where it is zero, neither claim means anything.
     */
    private fun assertFullBleedButClearOfTheStatusBar(contentTop: Float) {
        val root = composeRule.onRoot().fetchSemanticsNode().boundsInWindow
        val decorHeight = composeRule.runOnUiThread { composeRule.activity.window.decorView.height }
        assertThat(topInsetPx).isGreaterThan(0)
        assertThat(root.top).isEqualTo(0f)
        assertThat(root.height).isEqualTo(decorHeight.toFloat())
        assertThat(contentTop).isAtLeast(topInsetPx.toFloat())
    }

    private fun showContent(
        cats: List<Cat>,
        states: LoadStates,
        state: CatsListState = CatsListState(),
        onEvent: (CatsListEvent) -> Unit = {},
    ) {
        composeRule.setContent {
            CatsListTheme {
                topInsetPx = WindowInsets.safeDrawing.getTop(LocalDensity.current)
                CatsListContent(
                    pagingItems = flowOf(PagingData.from(cats, states)).collectAsLazyPagingItems(),
                    onEvent = onEvent,
                    state = state,
                    // What CatsNavDisplay hands the screen: the bars' insets, not zero.
                    contentPadding = WindowInsets.safeDrawing.asPaddingValues(),
                )
            }
        }
        // Twice over: the first advance is for the page to reach composition, the second for
        // the skeleton's minimum hold, whose clock only starts once it has.
        composeRule.mainClock.advanceTimeBy(SETTLE_MILLIS)
        composeRule.mainClock.advanceTimeBy(SETTLE_MILLIS)
    }

    /**
     * What Paging itself produces once a load is done: `Complete` is only ever set on prepend
     * and append, never on refresh — which is the whole reason the empty feed had nowhere to go.
     */
    private fun settled(
        refresh: LoadState = LoadState.NotLoading(endOfPaginationReached = false),
        append: LoadState = LoadState.NotLoading(endOfPaginationReached = true),
    ) = LoadStates(
        refresh = refresh,
        prepend = LoadState.NotLoading(endOfPaginationReached = true),
        append = append,
    )

    private fun string(id: Int) = context.getString(id)

    private companion object {
        const val SETTLE_MILLIS = 1_000L
    }
}
