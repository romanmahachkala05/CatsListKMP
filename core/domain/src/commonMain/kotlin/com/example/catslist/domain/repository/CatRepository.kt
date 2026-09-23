package com.example.catslist.domain.repository

import androidx.paging.PagingData
import com.example.catslist.domain.model.Cat
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow

/** Single source of truth for cats: the paged feed and the Room-backed favorites. */
interface CatRepository {

    /**
     * The feed, one page at a time, straight from the network. Never carries
     * [Cat.isFavorite]; a consumer overlays that from [favorites] (ADR-0024).
     */
    val feed: Flow<PagingData<Cat>>

    /** Cats persisted as favorites. */
    val favorites: Flow<ImmutableList<Cat>>

    /** Adds [cat] to favorites if absent, removes it otherwise. */
    suspend fun toggleFavorite(cat: Cat)

    /** Removes [cat] from favorites. */
    suspend fun removeFavorite(cat: Cat)
}
