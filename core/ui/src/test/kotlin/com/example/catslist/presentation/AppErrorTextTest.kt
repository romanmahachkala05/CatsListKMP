package com.example.catslist.presentation

import com.example.catslist.domain.model.AppError
import com.example.catslist.domain.model.AppErrorException
import com.google.common.truth.Truth.assertThat
import java.io.IOException
import org.junit.Test

class AppErrorTextTest {

    /**
     * Every case, listed by hand. The `when` in `toUiText` is exhaustive, so the compiler
     * already refuses a missing branch — what it cannot catch is two branches pointing at the
     * same string, which is what a copy-pasted case looks like.
     */
    private val everyError = listOf(
        AppError.NoConnection,
        AppError.Timeout,
        AppError.Unreachable,
        AppError.RateLimited,
        AppError.Server(503),
        AppError.Client(404),
        AppError.Malformed,
        AppError.Storage,
        AppError.Unknown,
    )

    @Test
    fun `no two failures share a message`() {
        val ids = everyError.map { (it.toUiText() as UiText.Resource).id }

        assertThat(ids).containsNoDuplicates()
    }

    @Test
    fun `an HTTP code is passed to the string as a format argument`() {
        // The one part of the wording that is not a constant, and the part a bug report needs.
        val text = AppError.Server(503).toUiText() as UiText.Resource

        assertThat(text.args).containsExactly(503)
    }

    @Test
    fun `a classified failure is unwrapped`() {
        val error: Throwable = AppErrorException(AppError.RateLimited)

        assertThat(error.asAppError()).isEqualTo(AppError.RateLimited)
    }

    @Test
    fun `anything that arrived unclassified is Unknown`() {
        // Nothing below presentation throws a raw exception anymore, so reaching this means
        // something bypassed the data layer's boundary — and Unknown is the honest answer.
        assertThat(IOException("straight from somewhere else").asAppError()).isEqualTo(AppError.Unknown)
    }
}
