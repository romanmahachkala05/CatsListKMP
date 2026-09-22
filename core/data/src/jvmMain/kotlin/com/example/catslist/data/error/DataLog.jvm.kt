package com.example.catslist.data.error

/** No platform logger on the JVM target; stderr is the desktop equivalent of Logcat here. */
internal actual fun dataLogWarning(
    tag: String,
    message: String,
    error: Throwable,
) {
    System.err.println("[$tag] $message: $error")
}
