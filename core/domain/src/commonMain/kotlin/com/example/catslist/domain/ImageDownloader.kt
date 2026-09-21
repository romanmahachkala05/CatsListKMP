package com.example.catslist.domain

/** A domain-owned port: something that can download an image by URL. `data` implements it. */
interface ImageDownloader {
    suspend fun download(url: String, id: String)
}
