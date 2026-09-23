package com.example.catslist.data.error

/**
 * `android.util.Log` has no multiplatform counterpart, and this is the one place `data` still
 * wants to log — [ErrorMapper] noting what it classified an exception as. A tiny expect/actual
 * rather than a logging library, since there is exactly one call site.
 */
internal expect fun dataLogWarning(
    tag: String,
    message: String,
    error: Throwable,
)
