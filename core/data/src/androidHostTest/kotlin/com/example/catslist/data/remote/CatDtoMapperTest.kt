package com.example.catslist.data.remote

import com.example.catslist.domain.model.Cat
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CatDtoMapperTest {

    @Test
    fun `carries every wire field into the domain model`() {
        val dto = CatDto(id = "abc", url = "https://cdn.example/abc.jpg", width = 640, height = 480)

        assertThat(dto.toDomain()).isEqualTo(
            Cat(id = "abc", url = "https://cdn.example/abc.jpg", width = 640, height = 480, isFavorite = false),
        )
    }

    @Test
    fun `defaults to not favorite, since the API has no such concept`() {
        assertThat(CatDto(id = "abc", url = "", width = 0, height = 0).toDomain().isFavorite).isFalse()
    }
}
