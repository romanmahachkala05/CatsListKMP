package com.example.catslist.presentation

import com.example.catslist.domain.model.AppError
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Reads the real strings, not just their keys. Android's `strings.xml` escapes apostrophes
 * (`\'`) and Compose resources does not, so a file carried across verbatim shows backslashes —
 * a mistake that compiles and that `AppErrorTextTest`'s identity checks cannot see.
 */
class UiTextTest {

    @Test
    fun `a string with an apostrophe reads as written`() = runTest {
        assertThat(AppError.NoConnection.toUiText().load())
            .isEqualTo("You're offline. Check your connection and try again.")
    }

    @Test
    fun `a format argument is substituted`() = runTest {
        assertThat(AppError.Server(503).toUiText().load())
            .isEqualTo("The cat server had a problem (503). Try again shortly.")
    }

    @Test
    fun `raw text is returned as is`() = runTest {
        assertThat(UiText.Raw("as is").load()).isEqualTo("as is")
    }
}
