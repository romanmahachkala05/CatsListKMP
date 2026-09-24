package com.example.catslist.data.remote

import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient

/**
 * The app's one client, shared with Coil through `App` (ADR-0030). Sharing is what makes the
 * connection pool and thread pool shared too — the feed's JSON and its images go to hosts this
 * app talks to constantly, so a warm pool is most of the win.
 *
 * OkHttp's own defaults are 10s connect / 10s read / 10s write, but `callTimeout` is 0 — no cap
 * at all on an end-to-end call, including retries and redirects. That is the one worth setting:
 * without it a request that keeps almost-progressing never fails, and the feed shows a spinner
 * with nothing behind it. iOS sets the same cap on its own engine (`DataModule.ios.kt`).
 */
fun catOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
    .callTimeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    .connectTimeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    .readTimeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    .build()
