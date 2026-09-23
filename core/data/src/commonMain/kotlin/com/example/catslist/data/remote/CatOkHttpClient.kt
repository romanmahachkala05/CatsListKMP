package com.example.catslist.data.remote

import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient

/** Past this, a request is reported as a timeout rather than left hanging behind a spinner. */
private const val TIMEOUT_SECONDS = 15L

/**
 * The app's one client, shared with Coil through `App` (ADR-0030). Sharing is what makes the
 * connection pool and thread pool shared too — the feed's JSON and its images go to hosts this
 * app talks to constantly, so a warm pool is most of the win.
 *
 * OkHttp's own defaults are 10s connect / 10s read / 10s write, but `callTimeout` is 0 — no cap
 * at all on an end-to-end call, including retries and redirects. That is the one worth setting:
 * without it a request that keeps almost-progressing never fails, and the feed shows a spinner
 * with nothing behind it.
 *
 * `okhttp3` in `commonMain`, like `java.net`/`java.io` in [com.example.catslist.data.error.ErrorMapper]:
 * this module targets only `jvm()` and `androidTarget()` today, both of which have it (ADR-0029).
 */
fun catOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
    .callTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
    .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
    .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
    .build()
