package com.example.catslist.data.remote

/**
 * TheCatAPI's `/v1/images/search`. An interface rather than a concrete client so the paging
 * source can be tested against `FakeCatApiService` without a server or an HTTP engine.
 */
interface CatApiService {

    suspend fun requestCatInfo(limit: Int, page: Int): List<CatDto>
}
