package com.example.catslist.domain.usecase

import com.example.catslist.domain.ImageDownloader
import com.example.catslist.domain.model.Cat

class DownloadCatImageUseCase(
    private val imageDownloader: ImageDownloader,
) {
    suspend operator fun invoke(cat: Cat) = imageDownloader.download(cat.url, cat.id)
}
