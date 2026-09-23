package com.example.catslist.testing

import com.example.catslist.domain.ImageDownloader

/** Records what was asked for, and fails on demand so the error path can be exercised. */
class FakeImageDownloader : ImageDownloader {

    /** `url to id`, in call order. */
    val downloaded = mutableListOf<Pair<String, String>>()

    /** When set, every [download] throws it instead of recording the call. */
    var error: Throwable? = null

    override suspend fun download(url: String, id: String) {
        error?.let { throw it }
        downloaded += url to id
    }
}
