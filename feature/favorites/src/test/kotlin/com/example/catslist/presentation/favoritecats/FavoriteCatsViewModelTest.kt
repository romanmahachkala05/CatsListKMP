package com.example.catslist.presentation.favoritecats

import com.example.catslist.domain.model.AppError
import com.example.catslist.domain.usecase.DownloadCatImageUseCase
import com.example.catslist.domain.usecase.GetFavoriteCatsUseCase
import com.example.catslist.domain.usecase.RemoveFavoriteUseCase
import com.example.catslist.feature.favorites.R
import com.example.catslist.presentation.DOWNLOAD_FAILED
import com.example.catslist.presentation.DOWNLOAD_STARTED
import com.example.catslist.presentation.FAVORITE_FAILED
import com.example.catslist.presentation.UiText
import com.example.catslist.testing.FakeCatRepository
import com.example.catslist.testing.FakeImageDownloader
import com.example.catslist.testing.FakeSnackbarNotifier
import com.example.catslist.testing.MainDispatcherRule
import com.example.catslist.testing.cat
import com.google.common.truth.Truth.assertThat
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class FavoriteCatsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeCatRepository()
    private val downloader = FakeImageDownloader()
    private val notifier = FakeSnackbarNotifier()

    @Test
    fun `shows the stored favorites as soon as it is created`() = runTest {
        repository.setFavorites(cat("1"), cat("2"))

        val viewModel = viewModel()

        assertThat(viewModel.state.value.status).isEqualTo(FavoriteCatsUiStatus.Content)
        assertThat(viewModel.state.value.cats.map { it.id }).containsExactly("1", "2").inOrder()
    }

    @Test
    fun `shows the empty state when nothing is favorited`() = runTest {
        val viewModel = viewModel()

        assertThat(viewModel.state.value.status).isEqualTo(FavoriteCatsUiStatus.Empty)
    }

    @Test
    fun `favorites that end in an error are shown, not thrown`() = runTest {
        repository.favoritesError = AppError.Storage

        val viewModel = viewModel()

        assertThat(viewModel.state.value.status).isEqualTo(
            FavoriteCatsUiStatus.Error(UiText.Resource(R.string.favoritecats_error_loading_favorites)),
        )
    }

    @Test
    fun `Retry resubscribes to favorites that had ended in an error`() = runTest {
        repository.favoritesError = AppError.Storage
        val viewModel = viewModel()
        repository.favoritesError = null
        repository.setFavorites(cat("1"))

        viewModel.onEvent(FavoriteCatsEvent.Retry)

        assertThat(viewModel.state.value.status).isEqualTo(FavoriteCatsUiStatus.Content)
        assertThat(viewModel.state.value.cats.map { it.id }).containsExactly("1")
    }

    @Test
    fun `RemoveFavorite drops the cat from the screen`() = runTest {
        repository.setFavorites(cat("1"), cat("2"))
        val viewModel = viewModel()

        viewModel.onEvent(FavoriteCatsEvent.RemoveFavorite(cat("1")))

        assertThat(viewModel.state.value.cats.map { it.id }).containsExactly("2")
    }

    @Test
    fun `removing the last favorite goes back to the empty state`() = runTest {
        repository.setFavorites(cat("1"))
        val viewModel = viewModel()

        viewModel.onEvent(FavoriteCatsEvent.RemoveFavorite(cat("1")))

        assertThat(viewModel.state.value.status).isEqualTo(FavoriteCatsUiStatus.Empty)
    }

    @Test
    fun `a failed removal is reported instead of crashing the screen`() = runTest {
        repository.setFavorites(cat("1"))
        val viewModel = viewModel()
        repository.favoriteError = AppError.Storage

        viewModel.onEvent(FavoriteCatsEvent.RemoveFavorite(cat("1")))

        assertThat(notifier.shown).containsExactly(FAVORITE_FAILED)
        assertThat(viewModel.state.value.cats.map { it.id }).containsExactly("1")
    }

    @Test
    fun `a canceled removal is not reported as a failure`() = runTest {
        repository.setFavorites(cat("1"))
        val viewModel = viewModel()
        repository.favoriteCancelled = true

        viewModel.onEvent(FavoriteCatsEvent.RemoveFavorite(cat("1")))

        assertThat(notifier.shown).isEmpty()
    }

    @Test
    fun `Download hands the cat to the downloader and says so`() = runTest {
        val cat = cat("1", isFavorite = true)
        repository.setFavorites(cat)
        val viewModel = viewModel()

        viewModel.onEvent(FavoriteCatsEvent.Download(cat))

        assertThat(downloader.downloaded).containsExactly(cat.url to cat.id)
        assertThat(notifier.shown).containsExactly(DOWNLOAD_STARTED)
    }

    @Test
    fun `a failed download is reported instead of crashing the screen`() = runTest {
        downloader.error = IOException("no storage")
        repository.setFavorites(cat("1"))
        val viewModel = viewModel()

        viewModel.onEvent(FavoriteCatsEvent.Download(cat("1")))

        assertThat(notifier.shown).containsExactly(DOWNLOAD_FAILED)
        assertThat(viewModel.state.value.cats.map { it.id }).containsExactly("1")
    }

    @Test
    fun `a canceled download is not reported as a failure`() = runTest {
        downloader.error = CancellationException("screen left")
        val viewModel = viewModel()

        viewModel.onEvent(FavoriteCatsEvent.Download(cat("1")))

        assertThat(notifier.shown).isEmpty()
    }

    private fun viewModel(): FavoriteCatsViewModel {
        val stateHolder = FavoriteCatsStateHolder()
        return FavoriteCatsViewModel(
            stateHolder = stateHolder,
            errorHandler = FavoriteCatsErrorHandler(stateHolder),
            getFavoriteCats = GetFavoriteCatsUseCase(repository),
            removeFavorite = RemoveFavoriteUseCase(repository),
            downloadCatImage = DownloadCatImageUseCase(downloader),
            notifier = notifier,
        )
    }
}
