package com.example.catslist.presentation

/**
 * Standard output, which Xcode's console shows. Not `NSLog`: a Kotlin `String` passed through
 * its C varargs is not bridged to an `NSString`, and `NSLog` crashes formatting it.
 */
internal actual fun uiLogError(
    tag: String?,
    message: String,
    error: Throwable,
) {
    println("[$tag] $message: $error")
}
