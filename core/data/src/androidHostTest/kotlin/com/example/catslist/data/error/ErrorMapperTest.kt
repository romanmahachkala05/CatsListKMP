package com.example.catslist.data.error

import androidx.sqlite.SQLiteException
import com.example.catslist.data.remote.catHttpClient
import com.example.catslist.domain.model.AppError
import com.example.catslist.testing.FakeNetworkMonitor
import com.google.common.truth.Truth.assertThat
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import org.junit.Test

class ErrorMapperTest {

    private val networkMonitor = FakeNetworkMonitor()
    private val mapper = ErrorMapper(networkMonitor)

    @Test
    fun `a timeout is a timeout whether or not the device is online`() = runTest {
        // The connection was there — it was the request that ran out of time, so connectivity
        // has nothing to add.
        networkMonitor.setOnline(false)

        assertThat(mapper.map(SocketTimeoutException("timeout"))).isEqualTo(AppError.Timeout)
    }

    @Test
    fun `an unresolvable host while offline is NoConnection`() = runTest {
        networkMonitor.setOnline(false)

        assertThat(mapper.map(UnknownHostException("api.thecatapi.com"))).isEqualTo(AppError.NoConnection)
    }

    @Test
    fun `the same unresolvable host while online is Unreachable`() = runTest {
        // The distinction the NetworkMonitor exists for: identical exception, and the only
        // thing that separates "you are offline" from "their DNS is down" is asking.
        networkMonitor.setOnline(true)

        assertThat(mapper.map(UnknownHostException("api.thecatapi.com"))).isEqualTo(AppError.Unreachable)
    }

    @Test
    fun `a refused connection is a transport failure like any other`() = runTest {
        // ConnectException is neither of the two named subclasses, so this is the general
        // IOException branch — it must still consult connectivity rather than fall to Unknown.
        networkMonitor.setOnline(true)

        assertThat(mapper.map(ConnectException("refused"))).isEqualTo(AppError.Unreachable)
    }

    @Test
    fun `a plain IOException while offline is still NoConnection`() = runTest {
        networkMonitor.setOnline(false)

        assertThat(mapper.map(IOException("socket closed"))).isEqualTo(AppError.NoConnection)
    }

    @Test
    fun `429 is its own case, not a generic client error`() = runTest {
        // TheCatAPI rate-limits anonymous callers, so this is the 4xx users actually hit —
        // and the only one where waiting is the right advice.
        assertThat(mapper.map(ktorException(HttpStatusCode.TooManyRequests))).isEqualTo(AppError.RateLimited)
    }

    @Test
    fun `a 5xx keeps its code`() = runTest {
        assertThat(mapper.map(ktorException(HttpStatusCode.ServiceUnavailable))).isEqualTo(AppError.Server(503))
    }

    @Test
    fun `a 4xx other than 429 keeps its code`() = runTest {
        assertThat(mapper.map(ktorException(HttpStatusCode.NotFound))).isEqualTo(AppError.Client(404))
    }

    @Test
    fun `a parse failure is Malformed, not a network problem`() = runTest {
        // A 200 whose body no longer matches the wire model. Telling the user to check their
        // connection would send them after the wrong thing entirely.
        assertThat(mapper.map(SerializationException("Unexpected JSON token"))).isEqualTo(AppError.Malformed)
    }

    @Test
    fun `a database failure is Storage`() = runTest {
        assertThat(mapper.map(SQLiteException("disk I/O error"))).isEqualTo(AppError.Storage)
    }

    @Test
    fun `anything unrecognised is Unknown`() = runTest {
        assertThat(mapper.map(IllegalStateException("something else"))).isEqualTo(AppError.Unknown)
    }

    /**
     * A real [ClientRequestException]/[ServerResponseException], thrown by the actual client
     * config against a mocked response — not hand-built, since Ktor's `expectSuccess` is what
     * decides whether one throws at all (ADR-0030).
     */
    private suspend fun ktorException(status: HttpStatusCode): Throwable {
        val engine = MockEngine { respond(content = "", status = status) }
        return try {
            catHttpClient(engine).get("v1/images/search")
            error("expected the client to throw for $status")
        } catch (error: ClientRequestException) {
            error
        } catch (error: ServerResponseException) {
            error
        }
    }
}
