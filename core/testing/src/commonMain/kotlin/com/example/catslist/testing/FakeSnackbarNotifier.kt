package com.example.catslist.testing

import com.example.catslist.presentation.SnackbarNotifier
import com.example.catslist.presentation.UiText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Keeps every message instead of handing it to a single collector, so a test can
 * assert on what a ViewModel asked to show without racing a collector for it.
 */
class FakeSnackbarNotifier : SnackbarNotifier {

    private val _messages = MutableSharedFlow<UiText>(extraBufferCapacity = MESSAGE_BUFFER)

    /** Everything passed to [showMessage], in order. */
    val shown = mutableListOf<UiText>()

    override val messages: Flow<UiText> = _messages.asSharedFlow()

    override suspend fun showMessage(message: UiText) {
        shown += message
        _messages.emit(message)
    }

    private companion object {
        const val MESSAGE_BUFFER = 16
    }
}
