package com.example.catslist.presentation.catslist

import com.example.catslist.domain.repository.CatRepository
import com.example.catslist.domain.usecase.DownloadCatImageUseCase
import com.example.catslist.domain.usecase.GetCatFeedUseCase
import com.example.catslist.domain.usecase.GetFavoriteCatsUseCase
import com.example.catslist.domain.usecase.ToggleFavoriteUseCase
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

/**
 * Koin resolves at runtime, so a definition that drifts from its constructor is a crash at
 * screen open rather than a compile error — the one guarantee given up in ADR-0026. This
 * builds the real [feedModule] against fakes and resolves it, which turns that crash into a
 * unit-test failure.
 */
class FeedModuleTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /** Stands in for `:core:data`'s module: the same types, without Room or the network. */
    private val fakeDataModule = module {
        single<CatRepository> { FakeCatRepository() }
        factory { GetCatFeedUseCase(get()) }
        factory { GetFavoriteCatsUseCase(get()) }
        factory { ToggleFavoriteUseCase(get()) }
        factory { DownloadCatImageUseCase(FakeImageDownloader()) }
        single<SnackbarNotifier> { FakeSnackbarNotifier() }
    }

    @Test
    fun `resolves the feed ViewModel`() {
        val koin = koinApplication { modules(fakeDataModule, feedModule) }.koin

        assertThat(koin.get<CatsListViewModel>()).isNotNull()
    }

    @Test
    fun `gives the ViewModel and its ErrorHandler one state holder`() {
        val koin = koinApplication { modules(fakeDataModule, feedModule) }.koin
        val viewModel = koin.get<CatsListViewModel>()

        // What `@ViewModelScoped` used to guarantee: the ErrorHandler writes, and the
        // ViewModel's own state reflects it.
        viewModel.state.value.let { assertThat(it.favoritesStatus).isEqualTo(CatsListFavoritesStatus.Live) }
    }
}
