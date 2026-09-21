package com.example.catslist.domain.model

/**
 * Pure business model. No Android, Room, or serialization types leak in here —
 * data/remote and data/local each map their own representation to this.
 */
data class Cat(
    val id: String,
    val url: String,
    val width: Int,
    val height: Int,
    val isFavorite: Boolean = false,
)
