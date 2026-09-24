package com.example.catslist.domain

import kotlinx.coroutines.flow.Flow

/**
 * A domain-owned port: whether the device currently has a usable internet connection. `data`
 * implements it over `ConnectivityManager`.
 *
 * A [Flow], not a `suspend fun isOnline(): Boolean`, and deliberately so. A one-shot check is a
 * guess made an instant before the request, and it answers only the question "should I try?" —
 * it cannot say the connection came back, so nothing can retry on its own, and nothing can show
 * or hide an offline notice as the state changes. A stream answers all three (ADR-0027).
 */
interface NetworkMonitor {

    /** Emits the current state immediately on collection, then on every change. */
    val isOnline: Flow<Boolean>
}
