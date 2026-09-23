package com.example.catslist.presentation.components

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.core.graphics.createBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.catslist.core.designsystem.resources.Res
import com.example.catslist.core.designsystem.resources.common_cat_image_failed
import com.example.catslist.presentation.theme.CatsListTheme
import com.example.catslist.testing.cat
import com.google.common.truth.Truth.assertThat
import java.io.File
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The card's three image states are Coil's, so they are exercised through a real request — for
 * a local file, which fails and then succeeds exactly when this test says it does.
 */
@RunWith(AndroidJUnit4::class)
class CatItemTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    /** Set from composition, because only the composable can read the window's insets. */
    private var topInsetPx = 0

    /**
     * A path of its own per test: Coil's memory cache lives on the singleton loader and
     * outlives any one test, so a shared name would serve the previous test's image.
     */
    private val imageFile = File(context.cacheDir, "cat-under-test-${SystemClock.elapsedRealtimeNanos()}.png")

    /** The app runs edge-to-edge, so the composable under test has to as well. */
    @Before
    fun holdTheClockAndStartWithNoImage() {
        composeRule.runOnUiThread { composeRule.activity.enableEdgeToEdge() }
        composeRule.mainClock.autoAdvance = false
        imageFile.delete()
    }

    @After
    fun cleanUp() {
        imageFile.delete()
    }

    @Test
    fun theCardSitsClearOfTheStatusBarInAFullBleedList() {
        showCatItem()

        val card = composeRule.onAllNodesWithTag(CAT_CARD_TAG)[0].fetchSemanticsNode()

        assertFullBleedButClearOfTheStatusBar(card.boundsInWindow.top)
    }

    @Test
    fun aCatWhoseImageWillNotLoadSaysSoRatherThanShimmeringOn() {
        showCatItem()

        awaitFailureStandIn(present = true)
    }

    /** The retry is per cat: a fresh painter, and so a fresh request, for this card alone. */
    @Test
    fun tappingTheStandInAsksForTheImageAgain() {
        showCatItem()
        awaitFailureStandIn(present = true)

        writeTheImage()
        composeRule.onNodeWithText(failedText).performClick()

        awaitFailureStandIn(present = false)
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

    private fun showCatItem() {
        composeRule.setContent {
            CatsListTheme {
                topInsetPx = WindowInsets.safeDrawing.getTop(LocalDensity.current)
                // A card is never on its own in the app: it sits in an inset list, edge-to-edge.
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = WindowInsets.safeDrawing.asPaddingValues(),
                ) {
                    item {
                        CatItem(
                            cat = cat("1").copy(url = "file://${imageFile.absolutePath}"),
                            onFavoriteClick = {},
                            onDownloadClick = {},
                        )
                    }
                }
            }
        }
        composeRule.mainClock.advanceTimeByFrame()
    }

    private fun writeTheImage() {
        val bitmap = createBitmap(width = 1, height = 1)
        imageFile.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, QUALITY, it) }
    }

    /**
     * Frame by frame, with a real pause in between: the clock is held for the shimmer's sake,
     * but Coil's own work runs on the wall clock and has to be given room to land.
     */
    private fun awaitFailureStandIn(present: Boolean) {
        val deadline = SystemClock.elapsedRealtime() + TIMEOUT_MILLIS
        while (isShowing(failedText) != present) {
            check(SystemClock.elapsedRealtime() < deadline) {
                "The failure stand-in was ${if (present) "never shown" else "never dismissed"}"
            }
            composeRule.mainClock.advanceTimeByFrame()
            Thread.sleep(FRAME_MILLIS)
        }
    }

    private fun isShowing(text: String) = composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    private val failedText get() = string(Res.string.common_cat_image_failed)

    private fun string(resource: StringResource) = runBlocking { getString(resource) }

    private companion object {
        const val TIMEOUT_MILLIS = 10_000L
        const val FRAME_MILLIS = 16L
        const val QUALITY = 100
    }
}
