package com.example.catslist.data.download

import com.example.catslist.domain.ImageDownloader
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.jvm.javaio.copyTo
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Desktop has no `DownloadManager`, so the download is the client's own: fetch the bytes and
 * write them to the user's Downloads directory, which is where Android's would have put them.
 *
 * [directory] is a parameter so a test can point the same downloader at a temporary path.
 */
class DesktopImageDownloader(
    private val client: HttpClient,
    private val ioDispatcher: CoroutineDispatcher,
    private val directory: File = File(System.getProperty("user.home"), "Downloads"),
) : ImageDownloader {

    override suspend fun download(url: String, id: String) = withContext(ioDispatcher) {
        directory.mkdirs()
        val target = File(directory, "cat_$id.jpg")
        // `use`, because `copyTo` leaves the stream open: on Windows an open handle keeps the
        // image locked — no other program can replace or delete it — until the app exits.
        target.outputStream().use { client.get(url).bodyAsChannel().copyTo(it) }
        Unit
    }
}
