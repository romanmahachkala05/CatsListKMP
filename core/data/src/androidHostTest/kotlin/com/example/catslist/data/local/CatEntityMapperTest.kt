package com.example.catslist.data.local

import com.example.catslist.domain.model.Cat
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CatEntityMapperTest {

    @Test
    fun `a stored row maps to a favorite cat`() {
        val entity = CatEntity(id = "abc", url = "https://cdn.example/abc.jpg", width = 640, height = 480)

        assertThat(entity.toDomain()).isEqualTo(
            Cat(id = "abc", url = "https://cdn.example/abc.jpg", width = 640, height = 480, isFavorite = true),
        )
    }

    @Test
    fun `a cat maps to a row, dropping the favorite flag the table implies`() {
        val cat = Cat(id = "abc", url = "https://cdn.example/abc.jpg", width = 640, height = 480, isFavorite = false)

        assertThat(cat.toEntity()).isEqualTo(
            CatEntity(id = "abc", url = "https://cdn.example/abc.jpg", width = 640, height = 480),
        )
    }

    @Test
    fun `round-trips a favorite cat unchanged`() {
        val cat = Cat(id = "abc", url = "https://cdn.example/abc.jpg", width = 640, height = 480, isFavorite = true)

        assertThat(cat.toEntity().toDomain()).isEqualTo(cat)
    }
}
