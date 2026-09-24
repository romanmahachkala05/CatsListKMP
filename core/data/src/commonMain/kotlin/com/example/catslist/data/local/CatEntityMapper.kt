package com.example.catslist.data.local

import com.example.catslist.domain.model.Cat

fun CatEntity.toDomain(): Cat = Cat(
    id = id,
    url = url,
    width = width,
    height = height,
    isFavorite = true, // every row in favoriteCatsTable is, by definition, a favorite
)

fun Cat.toEntity(): CatEntity = CatEntity(
    id = id,
    url = url,
    width = width,
    height = height,
)
