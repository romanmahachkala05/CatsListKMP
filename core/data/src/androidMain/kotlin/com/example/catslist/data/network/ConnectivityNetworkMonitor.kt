package com.example.catslist.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.core.content.getSystemService
import com.example.catslist.domain.NetworkMonitor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn

/** The one place that talks to [ConnectivityManager]. */
class ConnectivityNetworkMonitor(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher,
) : NetworkMonitor {

    override val isOnline: Flow<Boolean> = callbackFlow {
        val manager = context.getSystemService<ConnectivityManager>()
        if (manager == null) {
            // Nothing to ask. Reporting "offline" would block every request on a device that
            // may well be online, so assume the optimistic answer and let the request itself
            // be the judge.
            send(true)
            close()
            return@callbackFlow
        }

        // A set rather than a single boolean: Wi-Fi and cellular can both be validated at
        // once, and losing one of them is not going offline.
        val online = mutableSetOf<Network>()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                // Not `onAvailable`, which fires before the network is validated — the state a
                // captive-portal Wi-Fi never gets past, and where requests fail while the
                // device looks connected.
                if (capabilities.isUsable()) online += network else online -= network
                channel.trySend(online.isNotEmpty())
            }

            override fun onLost(network: Network) {
                online -= network
                channel.trySend(online.isNotEmpty())
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        manager.registerNetworkCallback(request, callback)

        // The callback reports changes from here on, so the state as it already stands is
        // seeded by hand. Without it a collector on a steady connection waits forever.
        send(manager.getNetworkCapabilities(manager.activeNetwork)?.isUsable() == true)

        awaitClose { manager.unregisterNetworkCallback(callback) }
    }
        // A slow or resubscribing collector wants the current answer, not the backlog of
        // every flip it missed.
        .conflate()
        .distinctUntilChanged()
        .flowOn(ioDispatcher)

    private companion object {
        fun NetworkCapabilities.isUsable(): Boolean = hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
