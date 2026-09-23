package com.example.catslist

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.catslist.domain.repository.CatRepository
import com.example.catslist.domain.usecase.DownloadCatImageUseCase
import com.example.catslist.domain.usecase.GetCatFeedUseCase
import com.example.catslist.domain.usecase.GetFavoriteCatsUseCase
import com.example.catslist.domain.usecase.RemoveFavoriteUseCase
import com.example.catslist.domain.usecase.ToggleFavoriteUseCase
import com.example.catslist.presentation.catslist.feedModule
import com.example.catslist.presentation.di.uiModule
import com.example.catslist.presentation.favoritecats.favoritesModule
import com.example.catslist.testing.FakeCatRepository
import com.example.catslist.testing.FakeImageDownloader
import com.example.catslist.testing.cat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.koinApplication
import org.koin.dsl.module

/**
 * What a rotation does to the app: the Activity is destroyed and created again, and the UI is
 * rebuilt from whatever state was saved. A real recreation rather than Compose's
 * `StateRestorationTester`, which keeps plain `remember`ed state and so passes a screen that
 * a rotation breaks.
 */
@RunWith(AndroidJUnit4::class)
class CatsAppRestorationTest {

    private val repository = FakeCatRepository().apply { setFeed(cat("1"), cat("2")) }

    init {
        CatsAppTestActivity.koin = koinApplication {
            modules(
                module {
                    single<CatRepository> { repository }
                    factory { GetCatFeedUseCase(get()) }
                    factory { GetFavoriteCatsUseCase(get()) }
                    factory { ToggleFavoriteUseCase(get()) }
                    factory { RemoveFavoriteUseCase(get()) }
                    factory { DownloadCatImageUseCase(FakeImageDownloader()) }
                },
                uiModule,
                feedModule,
                favoritesModule,
            )
        }
    }

    @get:Rule
    val composeRule = createAndroidComposeRule<CatsAppTestActivity>()

    @Test
    fun theSelectedTabSurvivesARotation() {
        composeRule.onNodeWithText("Favorites").performClick()
        composeRule.onNodeWithText("Favorite Cats list is empty").assertIsDisplayed()

        composeRule.activityRule.scenario.recreate()

        composeRule.onNodeWithText("Favorite Cats list is empty").assertIsDisplayed()
    }
}
