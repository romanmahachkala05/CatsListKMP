package com.example.catslist.presentation.favoritecats

import com.example.catslist.domain.repository.CatRepository
import com.example.catslist.domain.usecase.DownloadCatImageUseCase
import com.example.catslist.domain.usecase.GetFavoriteCatsUseCase
import com.example.catslist.domain.usecase.RemoveFavoriteUseCase
import com.example.catslist.presentation.SnackbarNotifier
import com.example.catslist.testing.FakeCatRepository
import com.example.catslist.testing.FakeImageDownloader
import com.example.catslist.testing.FakeSnackbarNotifier
import com.example.catslist.testing.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.koin.dsl.koinApplication
import org.koin.dsl.module

/** The favorites half of [feature/feed]'s graph check — see `FeedModuleTest` for why. */
class FavoritesModuleTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fakeDataModule = module {
        single<CatRepository> { FakeCatRepository() }
        factory { GetFavoriteCatsUseCase(get()) }
        factory { RemoveFavoriteUseCase(get()) }
        factory { DownloadCatImageUseCase(FakeImageDownloader()) }
        single<SnackbarNotifier> { FakeSnackbarNotifier() }
    }

    @Test
    fun `resolves the favorites ViewModel`() {
        val koin = koinApplication { modules(fakeDataModule, favoritesModule) }.koin

        assertThat(koin.get<FavoriteCatsViewModel>()).isNotNull()
    }
}
