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
 */
class DesktopImageDownloader(
    private val client: HttpClient,
    private val ioDispatcher: CoroutineDispatcher,
) : ImageDownloader {

    override suspend fun download(url: String, id: String) = withContext(ioDispatcher) {
        val downloads = File(System.getProperty("user.home"), "Downloads").apply { mkdirs() }
        val target = File(downloads, "cat_$id.jpg")
        client.get(url).bodyAsChannel().copyTo(target.outputStream())
        Unit
    }
}
