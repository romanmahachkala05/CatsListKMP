package com.example.catslist.data.download

import com.example.catslist.domain.ImageDownloader
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes
import platform.Photos.PHAccessLevelAddOnly
import platform.Photos.PHAssetCreationRequest
import platform.Photos.PHAssetResourceCreationOptions
import platform.Photos.PHAssetResourceTypePhoto
import platform.Photos.PHAuthorizationStatus
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHAuthorizationStatusLimited
import platform.Photos.PHAuthorizationStatusNotDetermined
import platform.Photos.PHPhotoLibrary

/**
 * iOS has no Downloads folder an image belongs in; a photo goes to Photos. Add-only access is
 * all this asks for — it can put a cat in the library, never read what is already there. The
 * first download shows the system prompt, whose text is `NSPhotoLibraryAddUsageDescription`
 * in the app's Info.plist.
 *
 * A refusal throws [PhotosAccessDeniedException], which the screen reports like any other
 * failed download.
 */
class IosImageDownloader(
    private val client: HttpClient,
    private val ioDispatcher: CoroutineDispatcher,
) : ImageDownloader {

    override suspend fun download(url: String, id: String) {
        // Asked before the fetch, so a refusal does not first cost a download.
        if (!requestAddAccess()) throw PhotosAccessDeniedException()
        val bytes = withContext(ioDispatcher) { client.get(url).bodyAsBytes() }
        saveToPhotos(bytes, fileName = "cat_$id.jpg")
    }

    private suspend fun requestAddAccess(): Boolean {
        val current = PHPhotoLibrary.authorizationStatusForAccessLevel(PHAccessLevelAddOnly)
        val status = if (current == PHAuthorizationStatusNotDetermined) {
            suspendCancellableCoroutine { continuation ->
                PHPhotoLibrary.requestAuthorizationForAccessLevel(PHAccessLevelAddOnly) { answer ->
                    continuation.resume(answer)
                }
            }
        } else {
            current
        }
        return status.canAdd()
    }

    private suspend fun saveToPhotos(bytes: ByteArray, fileName: String) {
        val data = bytes.toNSData()
        suspendCancellableCoroutine { continuation ->
            PHPhotoLibrary.sharedPhotoLibrary().performChanges(
                changeBlock = {
                    val options = PHAssetResourceCreationOptions().apply { originalFilename = fileName }
                    PHAssetCreationRequest.creationRequestForAsset()
                        .addResourceWithType(PHAssetResourceTypePhoto, data = data, options = options)
                },
                completionHandler = { saved, error ->
                    if (saved) {
                        continuation.resume(Unit)
                    } else {
                        continuation.resumeWithException(PhotosSaveException(error?.localizedDescription))
                    }
                },
            )
        }
    }

    private fun PHAuthorizationStatus.canAdd() =
        this == PHAuthorizationStatusAuthorized || this == PHAuthorizationStatusLimited
}

class PhotosAccessDeniedException : IllegalStateException("No permission to add to Photos")

class PhotosSaveException(
    reason: String?,
) : IllegalStateException("Photos did not save the image: $reason")

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData = usePinned { pinned ->
    NSData.dataWithBytes(pinned.addressOf(0), size.toULong())
}
