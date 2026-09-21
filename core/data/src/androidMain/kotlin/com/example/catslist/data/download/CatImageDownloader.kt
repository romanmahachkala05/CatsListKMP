package com.example.catslist.data.download

import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import androidx.core.net.toUri
import com.example.catslist.core.data.R
import com.example.catslist.domain.ImageDownloader
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/** Kicks off a system download of a cat image. The one place that talks to [DownloadManager]. */
class CatImageDownloader(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher,
) : ImageDownloader {

    override suspend fun download(url: String, id: String) = withContext(ioDispatcher) {
        val request = DownloadManager.Request(url.toUri()).apply {
            setTitle(context.getString(R.string.common_download_notification_title, id))
            setDescription(context.getString(R.string.common_download_notification_description))
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "cat_$id.jpg")
            setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        }
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
        Unit
    }
}
