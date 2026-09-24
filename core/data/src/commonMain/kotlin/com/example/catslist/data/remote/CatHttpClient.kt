package com.example.catslist.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** Trailing slash matters: Ktor resolves a request path relative to it. */
internal const val CAT_API_BASE_URL = "https://api.thecatapi.com/"

/**
 * Past this, a request is reported as a timeout rather than left hanging behind a spinner. Set
 * on each platform's engine, since that is where OkHttp and URLSession each keep theirs.
 */
internal const val REQUEST_TIMEOUT_SECONDS = 15L

/**
 * kotlinx.serialization rejects unknown keys by default, so a field added upstream would
 * start failing every response. Tolerating them is the safe default for a wire model we
 * do not own.
 */
private val json = Json { ignoreUnknownKeys = true }

/**
 * The client every request goes through. Takes its engine rather than choosing one, so a test
 * can run this exact configuration against `MockEngine`.
 */
internal fun catHttpClient(engine: HttpClientEngine): HttpClient = HttpClient(engine) {
    defaultRequest { url(CAT_API_BASE_URL) }
    install(ContentNegotiation) { json(json) }
    // Retrofit's suspend-fun adapter threw for any non-2xx by default; this is that same
    // behavior stated explicitly, and it is what makes ClientRequestException/
    // ServerResponseException reach ErrorMapper at all (ADR-0030).
    expectSuccess = true
}
