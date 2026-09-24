package com.example.catslist.presentation.favoritecats

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.example.catslist.presentation.UiText
import com.example.catslist.presentation.theme.CatsListTheme
import com.example.catslist.testing.cat
import com.google.common.truth.Truth.assertThat
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test

/**
 * The favorites screen on the desktop target, fed state directly. `FavoriteCatsContentTest`
 * covers the same screen on a device, edge-to-edge; this one runs in `verify`.
 */
@OptIn(ExperimentalTestApi::class)
class FavoriteCatsContentDesktopTest {

    private val events = mutableListOf<FavoriteCatsEvent>()

    @Test
    fun `no favorites says so`() = runComposeUiTest {
        setContent { show(FavoriteCatsState(status = FavoriteCatsUiStatus.Empty)) }

        onNodeWithText("Favorite Cats list is empty").assertIsDisplayed()
    }

    @Test
    fun `a failure shows its message and retries on request`() = runComposeUiTest {
        setContent { show(FavoriteCatsState(status = FavoriteCatsUiStatus.Error(UiText.Raw("It broke")))) }

        onNodeWithText("It broke").assertIsDisplayed()
        onNodeWithText("Retry").performClick()

        assertThat(events).containsExactly(FavoriteCatsEvent.Retry)
    }

    @Test
    fun `each favorite is a card, and its star removes that cat`() = runComposeUiTest {
        val cats = persistentListOf(cat("1", isFavorite = true), cat("2", isFavorite = true))
        setContent { show(FavoriteCatsState(status = FavoriteCatsUiStatus.Content, cats = cats)) }

        val stars = onAllNodesWithContentDescription("Add or remove this cat from favorites")
        stars.assertCountEquals(2)
        stars[1].performClick()

        assertThat(events).containsExactly(FavoriteCatsEvent.RemoveFavorite(cats[1]))
    }

    @Composable
    private fun show(state: FavoriteCatsState) {
        CatsListTheme { FavoriteCatsContent(state = state, onEvent = { events += it }) }
    }
}
