package com.example.catslist.presentation

/**
 * `android.util.Log` has no multiplatform counterpart. The same tiny expect/actual as
 * `:core:data`'s `dataLogWarning`, for the same reason: one call site, [launchCatching].
 */
internal expect fun uiLogError(
    tag: String?,
    message: String,
    error: Throwable,
)
