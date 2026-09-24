package com.example.catslist.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Runs [block] in `viewModelScope` and routes a failure to [onFailure] rather than letting it
 * reach the default handler, which kills the process.
 *
 * [CancellationException] is rethrown, never reported — which is why `runCatching` is not good
 * enough here. Cancellation means the caller went away, not that the work failed.
 */
@Suppress("TooGenericExceptionCaught") // Catching broadly is the point; see the KDoc and ADR-0013.
fun ViewModel.launchCatching(onFailure: suspend (Throwable) -> Unit, block: suspend () -> Unit): Job =
    viewModelScope.launch {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            uiLogError(this@launchCatching::class.simpleName, "Event handling failed", e)
            onFailure(e)
        }
    }
