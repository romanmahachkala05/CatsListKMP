package com.example.catslist.presentation.components

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.example.catslist.presentation.UiText
import com.example.catslist.presentation.theme.CatsListTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** The shared status screens, rendered on the desktop target with the strings read from `Res`. */
@OptIn(ExperimentalTestApi::class)
class StatusPlaceholdersDesktopTest {

    @Test
    fun `an error with a retry shows both, and the button retries`() = runComposeUiTest {
        var retries = 0
        setContent {
            CatsListTheme { ErrorMessage(UiText.Raw("It broke"), onRetry = { retries++ }) }
        }

        onNodeWithText("It broke").assertIsDisplayed()
        onNodeWithText("Retry").performClick()

        assertThat(retries).isEqualTo(1)
    }

    @Test
    fun `an error nobody can retry has no button`() = runComposeUiTest {
        setContent { CatsListTheme { ErrorMessage(UiText.Raw("It broke")) } }

        onNodeWithText("Retry").assertDoesNotExist()
    }

    @Test
    fun `an empty message is shown as given`() = runComposeUiTest {
        setContent { CatsListTheme { EmptyMessage(UiText.Raw("Nothing here")) } }

        onNodeWithText("Nothing here").assertIsDisplayed()
    }
}
