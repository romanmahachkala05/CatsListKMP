package com.example.catslist.data.remote

import com.example.catslist.domain.model.Cat

fun CatDto.toDomain(): Cat = Cat(
    id = id,
    url = url,
    width = width,
    height = height,
)
