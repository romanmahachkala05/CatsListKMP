package com.example.catslist.presentation.components

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import com.example.catslist.presentation.theme.CatsListTheme
import com.example.catslist.testing.cat
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test

/**
 * The card on the desktop target. The Android device test covers what only a device can — the
 * card sitting clear of the status bar — and this one covers the card itself, in `verify`.
 */
@OptIn(ExperimentalTestApi::class)
class CatItemDesktopTest {

    @Test
    fun `the card's two buttons do what they say`() = runComposeUiTest {
        var favorites = 0
        var downloads = 0
        setContent {
            CatsListTheme {
                CatItem(cat = cat("1"), onFavoriteClick = { favorites++ }, onDownloadClick = { downloads++ })
            }
        }

        onNodeWithContentDescription("Add or remove this cat from favorites").performClick()
        onNodeWithContentDescription("Download this cat image").performClick()

        assertThat(favorites).isEqualTo(1)
        assertThat(downloads).isEqualTo(1)
    }

    /** A real Coil request, for a file that is not there: it fails, and the card says so. */
    @Test
    fun `a cat whose image will not load says so rather than shimmering on`() = runComposeUiTest {
        val missing = File.createTempFile("missing-cat", ".png").apply { delete() }
        setContent {
            CatsListTheme {
                CatItem(
                    cat = cat("1").copy(url = missing.toURI().toString()),
                    onFavoriteClick = {},
                    onDownloadClick = {},
                )
            }
        }

        waitUntilAtLeastOneExists(hasText("Couldn't load this cat"), timeoutMillis = TIMEOUT_MILLIS)
    }

    private companion object {
        const val TIMEOUT_MILLIS = 10_000L
    }
}
