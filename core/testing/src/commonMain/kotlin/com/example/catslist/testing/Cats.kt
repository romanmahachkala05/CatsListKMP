package com.example.catslist.testing

import com.example.catslist.data.remote.CatDto
import com.example.catslist.domain.model.Cat

/** A [Cat] with only the fields a test cares about spelled out. */
fun cat(id: String, isFavorite: Boolean = false) = Cat(
    id = id,
    url = "https://cdn.example/$id.jpg",
    width = 300,
    height = 200,
    isFavorite = isFavorite,
)

/** The wire form of [cat], so a test can state the same cat on either side of the mapper. */
fun catDto(id: String) = CatDto(
    id = id,
    url = "https://cdn.example/$id.jpg",
    width = 300,
    height = 200,
)
