package com.example.catslist.presentation

/** No platform logger on the JVM target; stderr is the desktop equivalent of Logcat here. */
internal actual fun uiLogError(
    tag: String?,
    message: String,
    error: Throwable,
) {
    System.err.println("[$tag] $message: $error")
}
