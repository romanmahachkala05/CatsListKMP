package com.example.catslist.presentation.components

import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.catslist.core.designsystem.resources.Res
import com.example.catslist.core.designsystem.resources.common_cd_refresh_failed
import com.example.catslist.core.designsystem.resources.common_cd_refresh_succeeded
import com.example.catslist.core.designsystem.resources.common_cd_refreshing
import com.example.catslist.presentation.theme.CatsListTheme
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The indicator reports the outcome of a refresh the user asked for, so every phase it shows
 * is a function of both the [RefreshSignal] and whether a pull happened — which only a gesture
 * against the real component can exercise.
 */
@RunWith(AndroidJUnit4::class)
class CatPullToRefreshTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val signal = mutableStateOf(RefreshSignal.Idle)
    private var refreshes = 0

    /** Set from composition, because only the composable can read the window's insets. */
    private var topInsetPx = 0

    /** The app runs edge-to-edge, so the composable under test has to as well. */
    @Before
    fun goEdgeToEdge() {
        composeRule.runOnUiThread { composeRule.activity.enableEdgeToEdge() }
    }

    /** Each phase is held deliberately, so the clock is the thing under test as much as the state. */
    @Before
    fun holdTheClock() {
        composeRule.mainClock.autoAdvance = false
    }

    @Test
    fun aPullAsksForARefresh() {
        showPullToRefresh()

        pull()

        assertThat(refreshes).isEqualTo(1)
    }

    /**
     * Paging refreshes on its own at launch. The indicator has to stay out of the way for one
     * the user never asked for, which is what `pullRequested` is there to decide.
     */
    @Test
    fun aRefreshNobodyPulledForIsNotAnnounced() {
        showPullToRefresh()

        signalNow(RefreshSignal.Running)

        composeRule.onNodeWithContentDescription(string(Res.string.common_cd_refreshing)).assertDoesNotExist()
    }

    /**
     * The signal is set a full settle later than the pull on purpose: the refresh it kicked
     * off reports `Running` when it is good and ready, and the indicator owes the user the
     * sequence either way.
     */
    @Test
    fun aPulledRefreshShowsTheSpinnerWhileItRuns() {
        showPullToRefresh()
        pull()

        signalNow(RefreshSignal.Running)

        composeRule.onNodeWithContentDescription(string(Res.string.common_cd_refreshing)).assertIsDisplayed()
    }

    /** What `topInset` is for: parked behind the status bar, the indicator says nothing. */
    @Test
    fun theSpinnerRestsBelowTheStatusBarInAFullBleedWindow() {
        showPullToRefresh()
        pull()

        signalNow(RefreshSignal.Running)

        val spinner = composeRule.onNodeWithContentDescription(string(Res.string.common_cd_refreshing))
        assertFullBleedButClearOfTheStatusBar(spinner.fetchSemanticsNode().boundsInWindow.top)
    }

    @Test
    fun aRefreshThatWorkedShowsTheTickBeforeRetracting() {
        showPullToRefresh()
        pull()
        signalNow(RefreshSignal.Running)

        signalNow(RefreshSignal.Idle, advanceBy = PHASE_HOLD)

        composeRule.onNodeWithContentDescription(string(Res.string.common_cd_refresh_succeeded)).assertIsDisplayed()
        // And it is gone again once the hold is over, rather than parked on screen.
        composeRule.mainClock.advanceTimeBy(PHASE_HOLD + SETTLE_MILLIS)
        composeRule.onNodeWithContentDescription(string(Res.string.common_cd_refresh_succeeded)).assertDoesNotExist()
    }

    /** A failure the user pulled for is its own phase: the tick would be a lie. */
    @Test
    fun aRefreshThatFailedShowsTheCross() {
        showPullToRefresh()
        pull()
        signalNow(RefreshSignal.Running)

        signalNow(RefreshSignal.Failed, advanceBy = PHASE_HOLD)

        composeRule.onNodeWithContentDescription(string(Res.string.common_cd_refresh_failed)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(string(Res.string.common_cd_refresh_succeeded)).assertDoesNotExist()
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

    private fun showPullToRefresh() {
        composeRule.setContent {
            CatsListTheme {
                topInsetPx = WindowInsets.safeDrawing.getTop(LocalDensity.current)
                CatPullToRefresh(
                    signal = signal.value,
                    onRefresh = { refreshes++ },
                    modifier = Modifier.fillMaxSize(),
                    // The reason this parameter exists: under edge-to-edge the indicator
                    // would otherwise rest behind the status bar.
                    topInset = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding(),
                ) {
                    // Scrollable on purpose: the pull is delivered through nested scroll, and
                    // inset the way the feed insets its own list.
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().testTag(CONTENT_TAG),
                        contentPadding = WindowInsets.safeDrawing.asPaddingValues(),
                    ) {
                        items((1..20).toList()) { Text(text = "cat $it") }
                    }
                }
            }
        }
        composeRule.mainClock.advanceTimeBy(SETTLE_MILLIS)
    }

    private fun pull() {
        composeRule.onNodeWithTag(CONTENT_TAG).performTouchInput { swipeDown() }
        composeRule.mainClock.advanceTimeBy(SETTLE_MILLIS)
    }

    private fun signalNow(next: RefreshSignal, advanceBy: Long = SETTLE_MILLIS) {
        composeRule.runOnUiThread { signal.value = next }
        composeRule.mainClock.advanceTimeBy(advanceBy)
    }

    private fun string(resource: StringResource) = runBlocking { getString(resource) }

    private companion object {
        const val CONTENT_TAG = "content"

        /** Comfortably past the component's own 300ms minimum per phase. */
        const val PHASE_HOLD = 400L
        const val SETTLE_MILLIS = 1_000L
    }
}
