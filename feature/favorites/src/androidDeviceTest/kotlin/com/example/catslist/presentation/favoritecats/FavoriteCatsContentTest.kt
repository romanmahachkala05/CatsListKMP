package com.example.catslist.presentation.favoritecats

import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.catslist.core.designsystem.resources.Res as DesignsystemRes
import com.example.catslist.core.designsystem.resources.common_action_retry
import com.example.catslist.core.designsystem.resources.common_cd_download_cat
import com.example.catslist.core.designsystem.resources.common_cd_favorite_cat
import com.example.catslist.feature.favorites.resources.Res
import com.example.catslist.feature.favorites.resources.favoritecats_empty_message
import com.example.catslist.feature.favorites.resources.favoritecats_error_loading_favorites
import com.example.catslist.presentation.UiText
import com.example.catslist.presentation.components.CAT_CARD_TAG
import com.example.catslist.presentation.components.CAT_LIST_PLACEHOLDER_TAG
import com.example.catslist.presentation.theme.CatsListTheme
import com.example.catslist.testing.cat
import com.google.common.truth.Truth.assertThat
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * One test per `FavoriteCatsUiStatus`: the ViewModel tests prove which status the screen ends
 * up in, and these prove what that status puts on screen.
 */
@RunWith(AndroidJUnit4::class)
class FavoriteCatsContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    /** Set from composition, because only the composable can read the window's insets. */
    private var topInsetPx = 0

    /** The app runs edge-to-edge, so the composable under test has to as well. */
    @Before
    fun goEdgeToEdge() {
        composeRule.runOnUiThread { composeRule.activity.enableEdgeToEdge() }
    }

    /**
     * The skeleton and the cards shimmer forever, and an animation that never ends never lets
     * the test clock go idle. Driving the clock by hand is what keeps assertions from hanging.
     */
    @Before
    fun holdTheClock() {
        composeRule.mainClock.autoAdvance = false
    }

    /** A count, not an identity check: which cat is which is pinned by the event tests below. */
    @Test
    fun contentPutsACardUpForEveryFavorite() {
        showContent(FavoriteCatsState(status = FavoriteCatsUiStatus.Content, cats = twoCats))

        composeRule.onAllNodesWithContentDescription(string(DesignsystemRes.string.common_cd_favorite_cat))
            .assertCountEquals(2)
        composeRule.onNodeWithTag(CAT_LIST_PLACEHOLDER_TAG).assertDoesNotExist()
    }

    @Test
    fun theListRunsUnderTheStatusBarAndKeepsItsCatsClearOfIt() {
        showContent(FavoriteCatsState(status = FavoriteCatsUiStatus.Content, cats = twoCats))

        val firstCard = composeRule.onAllNodesWithTag(CAT_CARD_TAG)[0].fetchSemanticsNode()

        assertFullBleedButClearOfTheStatusBar(firstCard.boundsInWindow.top)
    }

    @Test
    fun anEmptyListSaysSoRatherThanShowingNothing() {
        showContent(FavoriteCatsState(status = FavoriteCatsUiStatus.Empty))

        composeRule.onNodeWithText(string(Res.string.favoritecats_empty_message)).assertIsDisplayed()
    }

    @Test
    fun loadingShowsTheSkeleton() {
        showContent(FavoriteCatsState(status = FavoriteCatsUiStatus.Loading))

        composeRule.onNodeWithTag(CAT_LIST_PLACEHOLDER_TAG).assertIsDisplayed()
        composeRule.onNodeWithText(string(Res.string.favoritecats_empty_message)).assertDoesNotExist()
    }

    /** The empty list and a failure to read it are not the same thing to the user. */
    @Test
    fun anErrorShowsItsOwnMessageAndAskingAgainIsAnEvent() {
        val events = mutableListOf<FavoriteCatsEvent>()
        showContent(
            state = FavoriteCatsState(status = FavoriteCatsUiStatus.Error(errorMessage)),
            onEvent = { events += it },
        )

        composeRule.onNodeWithText(string(Res.string.favoritecats_error_loading_favorites)).assertIsDisplayed()
        composeRule.onNodeWithText(string(DesignsystemRes.string.common_action_retry)).performClick()

        assertThat(events).containsExactly(FavoriteCatsEvent.Retry)
    }

    /** The same star that favorites a cat on the feed takes it off this screen. */
    @Test
    fun theStarOnACardRemovesThatCat() {
        val events = mutableListOf<FavoriteCatsEvent>()
        showContent(
            state = FavoriteCatsState(status = FavoriteCatsUiStatus.Content, cats = twoCats),
            onEvent = { events += it },
        )

        composeRule.onAllNodesWithContentDescription(string(DesignsystemRes.string.common_cd_favorite_cat))[1]
            .performClick()

        assertThat(events).containsExactly(FavoriteCatsEvent.RemoveFavorite(cat("2", isFavorite = true)))
    }

    @Test
    fun theDownloadOnACardAsksForThatCat() {
        val events = mutableListOf<FavoriteCatsEvent>()
        showContent(
            state = FavoriteCatsState(status = FavoriteCatsUiStatus.Content, cats = twoCats),
            onEvent = { events += it },
        )

        composeRule.onAllNodesWithContentDescription(string(DesignsystemRes.string.common_cd_download_cat))[0]
            .performClick()

        assertThat(events).containsExactly(FavoriteCatsEvent.Download(cat("1", isFavorite = true)))
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

    private fun showContent(state: FavoriteCatsState, onEvent: (FavoriteCatsEvent) -> Unit = {}) {
        composeRule.setContent {
            CatsListTheme {
                topInsetPx = WindowInsets.safeDrawing.getTop(LocalDensity.current)
                FavoriteCatsContent(
                    state = state,
                    onEvent = onEvent,
                    // What CatsNavDisplay hands the screen: the bars' insets, not zero.
                    contentPadding = WindowInsets.safeDrawing.asPaddingValues(),
                )
            }
        }
        // Enough for the first frame, and past any hold a status change animates through.
        composeRule.mainClock.advanceTimeBy(SETTLE_MILLIS)
    }

    /** Strings are Compose resources (ADR-0037), read the way the app reads them. */
    private fun string(resource: StringResource) = runBlocking { getString(resource) }

    private companion object {
        val twoCats = persistentListOf(cat("1", isFavorite = true), cat("2", isFavorite = true))
        val errorMessage = UiText.Resource(Res.string.favoritecats_error_loading_favorites)
        const val SETTLE_MILLIS = 1_000L
    }
}
