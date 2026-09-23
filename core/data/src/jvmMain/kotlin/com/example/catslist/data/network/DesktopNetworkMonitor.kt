package com.example.catslist.data.network

import com.example.catslist.domain.NetworkMonitor
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * Desktop's counterpart of `ConnectivityNetworkMonitor`. The JVM has no connectivity callback,
 * so this asks on a timer, and asks the same question Android's `NET_CAPABILITY_VALIDATED`
 * answers — not "is an adapter up" but "does the internet answer":
 *
 * 1. [hasNetwork]: some interface is up with a routable address. Instant, and enough on its own
 *    to say "offline" when the cable is out or Wi-Fi is off.
 * 2. [canReachInternet]: a TCP connection to a public address opens. Needed because an adapter
 *    being up proves little on a desktop — VPN, Hyper-V and WSL adapters stay up with the
 *    network gone. An IP rather than a hostname, so an offline check does not first wait out a
 *    DNS lookup; and not TheCatAPI itself, so its being down still reads as
 *    `AppError.Unreachable` rather than as the user being offline.
 */
class DesktopNetworkMonitor(
    private val ioDispatcher: CoroutineDispatcher,
    private val hasNetwork: () -> Boolean = ::hasRoutableInterface,
    private val canReachInternet: () -> Boolean = ::canOpenProbeConnection,
    private val pollInterval: Duration = POLL_INTERVAL,
) : NetworkMonitor {

    override val isOnline: Flow<Boolean> = flow {
        while (true) {
            emit(hasNetwork() && canReachInternet())
            delay(pollInterval)
        }
    }
        .distinctUntilChanged()
        .flowOn(ioDispatcher)

    private companion object {
        val POLL_INTERVAL = 5.seconds
    }
}

private fun hasRoutableInterface(): Boolean = NetworkInterface.getNetworkInterfaces().asSequence().any { network ->
    network.isUp &&
        !network.isLoopback &&
        network.inetAddresses.asSequence().any { !it.isLoopbackAddress && !it.isLinkLocalAddress }
}

private fun canOpenProbeConnection(): Boolean = runCatching {
    Socket().use { it.connect(InetSocketAddress(PROBE_HOST, PROBE_PORT), PROBE_TIMEOUT_MILLIS) }
}.isSuccess

/** Cloudflare's public resolver: anycast, so near everywhere, and open on 443. */
private const val PROBE_HOST = "1.1.1.1"
private const val PROBE_PORT = 443
private const val PROBE_TIMEOUT_MILLIS = 1_500
