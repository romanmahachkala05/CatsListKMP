package com.example.catslist.domain.usecase

import com.example.catslist.testing.FakeImageDownloader
import com.example.catslist.testing.cat
import com.google.common.truth.Truth.assertThat
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DownloadCatImageUseCaseTest {

    private val downloader = FakeImageDownloader()
    private val downloadCatImage = DownloadCatImageUseCase(downloader)

    @Test
    fun `downloads the cat's url under the cat's id`() = runTest {
        val cat = cat("1")

        downloadCatImage(cat)

        assertThat(downloader.downloaded).containsExactly(cat.url to cat.id)
    }

    @Test(expected = IOException::class)
    fun `lets a downloader failure propagate to the caller`() = runTest {
        downloader.error = IOException("no storage")

        downloadCatImage(cat("1"))
    }
}
