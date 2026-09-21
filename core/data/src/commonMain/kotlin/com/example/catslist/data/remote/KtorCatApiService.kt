package com.example.catslist.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/** The real [CatApiService]: one GET, with the paging window as query parameters. */
internal class KtorCatApiService(
    private val client: HttpClient,
) : CatApiService {

    override suspend fun requestCatInfo(limit: Int, page: Int): List<CatDto> = client.get(SEARCH_PATH) {
        parameter("limit", limit)
        parameter("page", page)
    }.body()

    private companion object {
        /** Relative: the base URL is configured once, on the client. */
        const val SEARCH_PATH = "v1/images/search"
    }
}
