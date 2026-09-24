package com.example.catslist.data.network

import com.example.catslist.domain.NetworkMonitor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_cancel
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.darwin.dispatch_queue_create

/**
 * iOS's counterpart of `ConnectivityNetworkMonitor`, over `NWPathMonitor`. A path is
 * "satisfied" when it can reach the internet — the closest iOS comes to Android's
 * `NET_CAPABILITY_VALIDATED`, though it does not probe past a captive portal the way Android
 * does. The monitor reports the current path as soon as it starts, so unlike Android there is
 * no initial state to seed by hand.
 */
class IosNetworkMonitor : NetworkMonitor {

    override val isOnline: Flow<Boolean> = callbackFlow {
        val monitor = nw_path_monitor_create()
        nw_path_monitor_set_update_handler(monitor) { path ->
            trySend(nw_path_get_status(path) == nw_path_status_satisfied)
        }
        // Its own serial queue, off the main thread: the handler only forwards into the flow.
        nw_path_monitor_set_queue(monitor, dispatch_queue_create(QUEUE_LABEL, null))
        nw_path_monitor_start(monitor)

        awaitClose { nw_path_monitor_cancel(monitor) }
    }
        // A slow or resubscribing collector wants the current answer, not the backlog.
        .conflate()
        .distinctUntilChanged()

    private companion object {
        const val QUEUE_LABEL = "com.example.catslist.network-monitor"
    }
}
