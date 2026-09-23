package com.example.catslist.testing

import com.example.catslist.domain.NetworkMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory connectivity a test drives by hand. Starts online, which is the uninteresting case. */
class FakeNetworkMonitor(
    initiallyOnline: Boolean = true,
) : NetworkMonitor {

    private val _isOnline = MutableStateFlow(initiallyOnline)
    override val isOnline: Flow<Boolean> = _isOnline.asStateFlow()

    /** Flips connectivity; collectors of [isOnline] see it on the next emission. */
    fun setOnline(value: Boolean) {
        _isOnline.value = value
    }
}
