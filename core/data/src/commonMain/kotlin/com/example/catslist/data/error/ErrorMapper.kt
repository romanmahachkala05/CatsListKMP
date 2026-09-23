package com.example.catslist.data.error

import androidx.sqlite.SQLiteException
import com.example.catslist.domain.NetworkMonitor
import com.example.catslist.domain.model.AppError
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerializationException

/**
 * Turns whatever was thrown into an [AppError]. The only place in the app that knows what a
 * `SocketTimeoutException` or an HTTP 429 means (ADR-0028).
 *
 * `java.net`/`java.io` rather than a multiplatform-neutral equivalent: Ktor does not unify the
 * *connection-level* failures (timed out vs. DNS failure vs. refused) across engines the way it
 * does HTTP-status failures, so there is no common type to catch instead. This module targets
 * only `jvm()` and `androidTarget()` today (ADR-0029), both of which have these types — the
 * deviation from the rest of `commonMain` is scoped to exactly the three catches that need it,
 * and is worth revisiting only once a non-JVM target is real (ADR-0030).
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
            is UnknownHostException -> transportFailure()
            is SerializationException -> AppError.Malformed
            is SQLiteException -> AppError.Storage
            // After the specific ones: SocketTimeoutException and UnknownHostException are
            // both IOException, and a `when` takes the first branch that matches.
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
