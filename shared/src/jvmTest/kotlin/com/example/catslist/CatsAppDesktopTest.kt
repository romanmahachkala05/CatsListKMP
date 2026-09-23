package com.example.catslist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.catslist.domain.repository.CatRepository
import com.example.catslist.domain.usecase.DownloadCatImageUseCase
import com.example.catslist.domain.usecase.GetCatFeedUseCase
import com.example.catslist.domain.usecase.GetFavoriteCatsUseCase
import com.example.catslist.domain.usecase.RemoveFavoriteUseCase
import com.example.catslist.domain.usecase.ToggleFavoriteUseCase
import com.example.catslist.presentation.catslist.feedModule
import com.example.catslist.presentation.di.uiModule
import com.example.catslist.presentation.favoritecats.favoritesModule
import com.example.catslist.presentation.navigation.NAV_BAR_TAG
import com.example.catslist.presentation.navigation.NAV_RAIL_TAG
import com.example.catslist.testing.FakeCatRepository
import com.example.catslist.testing.FakeImageDownloader
import com.example.catslist.testing.cat
import org.junit.Test
import org.koin.compose.KoinIsolatedContext
import org.koin.dsl.koinApplication
import org.koin.dsl.module

/**
 * The whole app on the desktop target: real navigation, real screens and ViewModels, over an
 * in-memory repository. Switching tabs saves each back stack, which is exactly where an
 * unregistered `NavKey` fails off Android — so this is the test for `CatsNavDisplay`'s
 * serializer list as much as for the screens.
 */
@OptIn(ExperimentalTestApi::class)
class CatsAppDesktopTest {

    private val repository = FakeCatRepository()

    private val fakeDataModule = module {
        single<CatRepository> { repository }
        factory { GetCatFeedUseCase(get()) }
        factory { GetFavoriteCatsUseCase(get()) }
        factory { ToggleFavoriteUseCase(get()) }
        factory { RemoveFavoriteUseCase(get()) }
        factory { DownloadCatImageUseCase(FakeImageDownloader()) }
    }

    /**
     * A Koin of this test's own, handed to the composition rather than started globally:
     * koin-compose remembers the global instance it first sees, so a later test's
     * `startKoin` would be ignored and the stopped one's closed scope used instead.
     */
    private val koin = koinApplication { modules(fakeDataModule, uiModule, feedModule, favoritesModule) }

    @Test
    fun `the feed shows its cats, and favorites starts empty`() = runComposeUiTest {
        repository.setFeed(cat("1"), cat("2"))
        showApp()

        stars().assertCountEquals(2)

        onNodeWithText("Favorites").performClick()
        settle()

        onNodeWithText("Favorite Cats list is empty").assertIsDisplayed()
    }

    @Test
    fun `a cat favorited in the feed is on the favorites tab`() = runComposeUiTest {
        repository.setFeed(cat("1"), cat("2"))
        showApp()

        stars()[0].performClick()
        settle()
        onNodeWithText("Favorites").performClick()
        settle()

        stars().assertCountEquals(1)
    }

    @Test
    fun `switching back keeps the feed`() = runComposeUiTest {
        repository.setFeed(cat("1"), cat("2"))
        showApp()

        onNodeWithText("Favorites").performClick()
        settle()
        onNodeWithText("Cats list").performClick()
        settle()

        stars().assertCountEquals(2)
    }

    @Test
    fun `a narrow window navigates from a bottom bar`() = runComposeUiTest {
        repository.setFeed(cat("1"))
        showApp(width = 400.dp)

        onNodeWithTag(NAV_BAR_TAG).assertIsDisplayed()
        onNodeWithTag(NAV_RAIL_TAG).assertDoesNotExist()
    }

    @Test
    fun `a wide window navigates from a rail, and the rail still switches tabs`() = runComposeUiTest {
        repository.setFeed(cat("1"))
        showApp(width = 1000.dp)

        onNodeWithTag(NAV_RAIL_TAG).assertIsDisplayed()
        onNodeWithTag(NAV_BAR_TAG).assertDoesNotExist()

        onNodeWithText("Favorites").performClick()
        settle()
        onNodeWithText("Favorite Cats list is empty").assertIsDisplayed()
    }

    private fun ComposeUiTest.stars() = onAllNodesWithContentDescription("Add or remove this cat from favorites")

    /**
     * The cards and the skeleton shimmer forever, so the clock never idles on its own: it is
     * driven by hand, as in the screens' own tests.
     */
    private fun ComposeUiTest.showApp(width: Dp = 400.dp) {
        mainClock.autoAdvance = false
        setContent {
            Box(Modifier.size(width = width, height = 800.dp)) {
                KoinIsolatedContext(context = koin) { CatsApp() }
            }
        }
        settle()
        settle()
    }

    private fun ComposeUiTest.settle() = mainClock.advanceTimeBy(SETTLE_MILLIS)

    private companion object {
        const val SETTLE_MILLIS = 1_000L
    }
}
