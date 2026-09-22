package com.example.catslist.data.error

import android.util.Log

internal actual fun dataLogWarning(
    tag: String,
    message: String,
    error: Throwable,
) {
    Log.w(tag, message, error)
}
