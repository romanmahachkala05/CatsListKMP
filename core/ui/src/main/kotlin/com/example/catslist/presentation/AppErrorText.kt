package com.example.catslist.presentation

import com.example.catslist.core.ui.R
import com.example.catslist.domain.model.AppError
import com.example.catslist.domain.model.AppErrorException
import kotlinx.collections.immutable.persistentListOf

/**
 * Unwraps what `data` threw. The one `as?` in the app: everything below `presentation` throws
 * [AppErrorException] and nothing else, so anything that is not one arrived from somewhere
 * unclassified and is [AppError.Unknown] by definition (ADR-0028).
 */
fun Throwable.asAppError(): AppError = (this as? AppErrorException)?.error ?: AppError.Unknown

/**
 * The default wording for each failure, shared by every screen. A screen that can say something
 * more useful about a particular case — because it knows what it was doing — branches on that
 * case itself and falls back here for the rest.
 */
fun AppError.toUiText(): UiText = when (this) {
    AppError.NoConnection -> UiText.Resource(R.string.common_error_no_connection)
    AppError.Timeout -> UiText.Resource(R.string.common_error_timeout)
    AppError.Unreachable -> UiText.Resource(R.string.common_error_unreachable)
    AppError.RateLimited -> UiText.Resource(R.string.common_error_rate_limited)
    // The code is shown deliberately: it is the one thing that makes a bug report actionable,
    // and a number in parentheses reads as a detail rather than as an instruction.
    is AppError.Server -> UiText.Resource(R.string.common_error_server, persistentListOf(code))
    is AppError.Client -> UiText.Resource(R.string.common_error_client, persistentListOf(code))
    AppError.Malformed -> UiText.Resource(R.string.common_error_malformed)
    AppError.Storage -> UiText.Resource(R.string.common_error_storage)
    AppError.Unknown -> UiText.Resource(R.string.common_error_unknown)
}
