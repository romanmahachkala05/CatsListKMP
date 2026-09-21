package com.example.catslist.domain.usecase

import androidx.paging.PagingData
import com.example.catslist.domain.model.Cat
import com.example.catslist.domain.repository.CatRepository
import kotlinx.coroutines.flow.Flow

class GetCatFeedUseCase(
    private val repository: CatRepository,
) {
    operator fun invoke(): Flow<PagingData<Cat>> = repository.feed
}
