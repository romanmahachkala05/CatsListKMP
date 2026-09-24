package com.example.catslist.domain.model

/**
 * What went wrong, in terms the UI can act on. Every failure crossing out of `data` is one of
 * these, so no `presentation` code ever branches on `UnknownHostException` or an HTTP code
 * (`ARCHITECTURE.md` §3c, ADR-0028).
 *
 * The originating [Throwable] is deliberately **not** carried. It is logged where the mapping
 * happens, which is the only place with anything useful to say about it; downstream, nothing
 * can do anything with a `SocketTimeoutException` that it cannot do with [Timeout]. Leaving it
 * out is also what lets these compare by value, so a test asserts `Server(503)` rather than
 * picking a value out of an exception it had to construct.
 */
sealed interface AppError {

    /** The device has no usable connection at all — asked and answered, not inferred. */
    data object NoConnection : AppError

    /** The connection exists but the request ran out of time. */
    data object Timeout : AppError

    /** Online, but the host did not answer: DNS failure, refused connection, dropped socket. */
    data object Unreachable : AppError

    /** HTTP 429. TheCatAPI rate-limits anonymous callers, so this one is routine. */
    data object RateLimited : AppError

    /** HTTP 5xx: the server knows it failed. */
    data class Server(
        val code: Int,
    ) : AppError

    /** HTTP 4xx other than 429: this app asked for something wrong. */
    data class Client(
        val code: Int,
    ) : AppError

    /** The response arrived and could not be parsed — a wire model that no longer matches. */
    data object Malformed : AppError

    /** The local database failed. In this app that is always the favorites table. */
    data object Storage : AppError

    /** Classified as nothing else. Worth watching: a category that fills up wants splitting. */
    data object Unknown : AppError
}

/**
 * Carries an [AppError] through an API that insists on a [Throwable] — `PagingSource`'s
 * `LoadResult.Error` and `Flow`'s failure channel both do. `data` throws this and nothing else;
 * `presentation` unwraps it with `Throwable.asAppError()`.
 */
class AppErrorException(
    val error: AppError,
) : Exception(error.toString())
