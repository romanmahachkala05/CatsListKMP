package com.example.catslist.data.error

import com.example.catslist.domain.model.AppError
import com.example.catslist.testing.FakeNetworkMonitor
import io.ktor.client.engine.darwin.DarwinHttpRequestException
import io.ktor.client.network.sockets.SocketTimeoutException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import platform.Foundation.NSError
import platform.Foundation.NSURLErrorCannotFindHost
import platform.Foundation.NSURLErrorDomain

/**
 * What the Darwin engine actually throws, classified. `ErrorMapperTest` covers the JVM's
 * exceptions; these are the ones only URLSession produces.
 */
class ErrorMapperIosTest {

    private val networkMonitor = FakeNetworkMonitor()
    private val mapper = ErrorMapper(networkMonitor)

    @Test
    fun aTimedOutRequestIsATimeout() = runTest {
        // The Darwin engine turns NSURLErrorTimedOut into Ktor's own SocketTimeoutException.
        networkMonitor.setOnline(false)

        assertEquals(AppError.Timeout, mapper.map(SocketTimeoutException("timed out")))
    }

    @Test
    fun anUnresolvableHostWhileOfflineIsNoConnection() = runTest {
        networkMonitor.setOnline(false)

        assertEquals(AppError.NoConnection, mapper.map(urlSessionFailure(NSURLErrorCannotFindHost)))
    }

    @Test
    fun theSameFailureWhileOnlineIsUnreachable() = runTest {
        networkMonitor.setOnline(true)

        assertEquals(AppError.Unreachable, mapper.map(urlSessionFailure(NSURLErrorCannotFindHost)))
    }

    private fun urlSessionFailure(code: Long) =
        DarwinHttpRequestException(NSError(domain = NSURLErrorDomain, code = code, userInfo = null))
}
