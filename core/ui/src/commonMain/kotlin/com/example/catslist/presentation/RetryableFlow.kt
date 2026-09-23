package com.example.catslist.presentation

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update

/**
 * A [source] flow that [retry] resubscribes to. `catch` sits on the inner flow: downstream of
 * `flatMapLatest` it would end the trigger too, and `retry` would be a dead button.
 */
class RetryableFlow<T>(
    source: () -> Flow<T>,
    onFailure: (Throwable) -> Unit,
) {
    /** Bumped by [retry]. Its value carries no meaning beyond "different". */
    private val subscriptions = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val flow: Flow<T> = subscriptions.flatMapLatest {
        source().catch { onFailure(it) }
    }

    fun retry() = subscriptions.update { it + 1 }
}
