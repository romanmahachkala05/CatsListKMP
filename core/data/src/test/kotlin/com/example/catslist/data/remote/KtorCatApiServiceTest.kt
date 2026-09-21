package com.example.catslist.data.remote

import com.google.common.truth.Truth.assertThat
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Retrofit generated the request from an annotated interface; this builds it by hand, so the
 * URL and the paging window are now this module's to get right (ADR-0027).
 */
class KtorCatApiServiceTest {

    private var requested: Url? = null

    private fun service(body: String, status: HttpStatusCode = HttpStatusCode.OK): KtorCatApiService {
        val engine = MockEngine { request ->
            requested = request.url
            respond(
                content = body,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        return KtorCatApiService(catHttpClient(engine))
    }

    @Test
    fun `asks the search endpoint for the requested page`() = runTest {
        service(ONE_CAT).requestCatInfo(limit = 10, page = 3)

        assertThat(requested?.encodedPath).isEqualTo("/v1/images/search")
        assertThat(requested?.parameters?.get("limit")).isEqualTo("10")
        assertThat(requested?.parameters?.get("page")).isEqualTo("3")
    }

    @Test
    fun `maps the response body onto CatDto`() = runTest {
        val cats = service(ONE_CAT).requestCatInfo(limit = 1, page = 0)

        assertThat(cats).containsExactly(CatDto(id = "abc", url = "https://cdn/abc.jpg", width = 300, height = 200))
    }

    /** The wire model does not own the API, so an added field must not fail the response. */
    @Test
    fun `ignores fields the wire model does not declare`() = runTest {
        val withExtras = """[{"id":"abc","url":"https://cdn/abc.jpg","width":300,"height":200,"breeds":[]}]"""

        val cats = service(withExtras).requestCatInfo(limit = 1, page = 0)

        assertThat(cats).hasSize(1)
    }

    private companion object {
        const val ONE_CAT = """[{"id":"abc","url":"https://cdn/abc.jpg","width":300,"height":200}]"""
    }
}
