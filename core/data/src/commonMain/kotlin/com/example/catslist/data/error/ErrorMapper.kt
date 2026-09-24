package com.example.catslist.data.error

import androidx.sqlite.SQLiteException
import com.example.catslist.domain.NetworkMonitor
import com.example.catslist.domain.model.AppError
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import kotlinx.coroutines.flow.first
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException

/**
 * Turns whatever was thrown into an [AppError]. The only place in the app that knows what a
 * `SocketTimeoutException` or an HTTP 429 means (ADR-0028).
 *
 * Both types are the multiplatform ones, and on the JVM they are `java.net`'s and `java.io`'s
 * under another name. OkHttp and URLSession each report a timeout as that
 * `SocketTimeoutException`; every other failure to reach the server is an `IOException` —
 * an unknown host, a refused connection, or on iOS a `DarwinHttpRequestException`.
 */
class ErrorMapper(
    private val networkMonitor: NetworkMonitor,
) {

    /**
     * `suspend`, because separating [AppError.NoConnection] from [AppError.Unreachable] takes
     * asking [NetworkMonitor] — and asking it *now*, when the failure happened, rather than
     * before the request when the answer would only have been a guess.
     */
    suspend fun map(error: Throwable): AppError {
        val mapped = when (error) {
            is ClientRequestException -> error.response.status.value.toAppError()
            is ServerResponseException -> error.response.status.value.toAppError()
            is SocketTimeoutException -> AppError.Timeout
            is SerializationException -> AppError.Malformed
            is SQLiteException -> AppError.Storage
            // After the timeout: SocketTimeoutException is an IOException too, and a `when`
            // takes the first branch that matches.
            is IOException -> transportFailure()
            else -> AppError.Unknown
        }
        // The one place the throwable itself is still available, so the one place worth
        // logging it. Downstream carries the classification only.
        dataLogWarning(TAG, "Mapped ${error::class.simpleName} to $mapped", error)
        return mapped
    }

    /**
     * The request never reached the server. Which of the two that is depends entirely on the
     * device: offline is the more useful thing to say, and it is only true if it is true.
     */
    private suspend fun transportFailure(): AppError =
        if (networkMonitor.isOnline.first()) AppError.Unreachable else AppError.NoConnection

    private fun Int.toAppError(): AppError = when {
        this == HTTP_TOO_MANY_REQUESTS -> AppError.RateLimited
        this in SERVER_ERRORS -> AppError.Server(this)
        this in CLIENT_ERRORS -> AppError.Client(this)
        else -> AppError.Unknown
    }

    private companion object {
        const val TAG = "ErrorMapper"
        const val HTTP_TOO_MANY_REQUESTS = 429
        val CLIENT_ERRORS = 400..499
        val SERVER_ERRORS = 500..599
    }
}
