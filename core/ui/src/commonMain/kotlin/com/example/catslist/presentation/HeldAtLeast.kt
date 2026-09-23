package com.example.catslist.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.time.TimeSource
import kotlinx.coroutines.delay

/**
 * [value], except that once it turns true it stays true for at least [minimumMillis]. For
 * loading indicators that would otherwise flicker. A floor, not a delay: true still shows
 * immediately.
 */
@Composable
fun heldAtLeast(value: Boolean, minimumMillis: Long): Boolean {
    var held by remember { mutableStateOf(value) }
    // Monotonic on purpose: an NTP sync can step the wall clock backwards.
    var shownAt by remember { mutableStateOf(TimeSource.Monotonic.markNow()) }
    LaunchedEffect(value) {
        if (value) {
            shownAt = TimeSource.Monotonic.markNow()
            held = true
        } else {
            val remaining = minimumMillis - shownAt.elapsedNow().inWholeMilliseconds
            if (remaining > 0) delay(remaining)
            held = false
        }
    }
    return value || held
}
