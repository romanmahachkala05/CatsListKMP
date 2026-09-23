package com.example.catslist.data.network

import com.example.catslist.domain.NetworkMonitor
import kotlinx.coroutines.flow.flowOf

/**
 * Desktop has no `ConnectivityManager` equivalent wired up yet. Reporting "always online" keeps
 * [com.example.catslist.data.error.ErrorMapper] on the [AppError.Unreachable] branch instead of
 * [AppError.NoConnection] for a genuinely offline desktop — a real monitor (`java.net.NetworkInterface`
 * polling, or platform-specific reachability) is future work, not a regression this port owns.
 */
class DesktopNetworkMonitor : NetworkMonitor {
    override val isOnline = flowOf(true)
}
