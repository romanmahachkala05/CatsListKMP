package com.example.catslist.data.download

import com.google.common.truth.Truth.assertThat
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test

class DesktopImageDownloaderTest {

    private val directory: File = Files.createTempDirectory("downloads").toFile()
    private val image = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 1, 2, 3, 0xFF.toByte(), 0xD9.toByte())

    @After
    fun cleanUp() {
        directory.deleteRecursively()
    }

    @Test
    fun `the image lands in the directory under the cat's id`() = runTest {
        download(id = "abc")

        assertThat(File(directory, "cat_abc.jpg").readBytes()).isEqualTo(image)
    }

    /**
     * The regression: the file was written through a stream nobody closed, and Windows keeps
     * an open file locked until the process exits. Only Windows refuses to delete an open file,
     * so this catches it there — on Linux CI it passes either way, and the `use` in the
     * downloader is what the test documents.
     */
    @Test
    fun `the downloaded file is released once the download returns`() = runTest {
        download(id = "abc")

        assertThat(File(directory, "cat_abc.jpg").delete()).isTrue()
    }

    private suspend fun TestScope.download(id: String) {
        val client = HttpClient(MockEngine { respond(image) })
        DesktopImageDownloader(
            client = client,
            ioDispatcher = StandardTestDispatcher(testScheduler),
            directory = directory,
        ).download(url = "https://cdn.example/$id.jpg", id = id)
    }
}
