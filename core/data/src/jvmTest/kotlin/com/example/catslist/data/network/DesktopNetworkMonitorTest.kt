package com.example.catslist.data.network

import com.google.common.truth.Truth.assertThat
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DesktopNetworkMonitorTest {

    private var network = true
    private var internet = true
    private var probes = 0

    @Test
    fun `a network the internet answers on is online, straight away`() = runTest {
        assertThat(monitor().isOnline.first()).isTrue()
    }

    @Test
    fun `no network is offline without probing the internet`() = runTest {
        network = false

        assertThat(monitor().isOnline.first()).isFalse()
        assertThat(probes).isEqualTo(0)
    }

    /** The VPN/Hyper-V case: an adapter is up, and nothing answers through it. */
    @Test
    fun `a network the internet does not answer on is offline`() = runTest {
        internet = false

        assertThat(monitor().isOnline.first()).isFalse()
    }

    @Test
    fun `a change is noticed on the next poll, and only changes are emitted`() = runTest {
        val seen = mutableListOf<Boolean>()
        val collector = launch { monitor().isOnline.take(3).toList(seen) }
        runCurrent()

        internet = false
        advanceTimeBy(POLL + 1.seconds)
        advanceTimeBy(POLL) // still offline: not emitted again
        internet = true
        advanceTimeBy(POLL)
        collector.join()

        assertThat(seen).containsExactly(true, false, true).inOrder()
    }

    private fun TestScope.monitor() = DesktopNetworkMonitor(
        ioDispatcher = StandardTestDispatcher(testScheduler),
        hasNetwork = { network },
        canReachInternet = { probes++; internet },
        pollInterval = POLL,
    )

    private companion object {
        val POLL = 5.seconds
    }
}
