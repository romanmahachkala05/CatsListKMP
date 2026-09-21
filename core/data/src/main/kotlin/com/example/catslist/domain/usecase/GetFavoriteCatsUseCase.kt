package com.example.catslist.domain.usecase

import com.example.catslist.domain.model.Cat
import com.example.catslist.domain.repository.CatRepository
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow

class GetFavoriteCatsUseCase(
    private val repository: CatRepository,
) {
    operator fun invoke(): Flow<ImmutableList<Cat>> = repository.favorites
}
