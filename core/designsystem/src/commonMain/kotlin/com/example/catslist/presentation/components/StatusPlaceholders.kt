package com.example.catslist.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.catslist.core.designsystem.resources.Res
import com.example.catslist.core.designsystem.resources.common_action_retry
import com.example.catslist.presentation.UiText
import com.example.catslist.presentation.resolve
import com.example.catslist.presentation.theme.CatsListTheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun EmptyMessage(message: UiText, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message.resolve(),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun ErrorMessage(
    message: UiText,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message.resolve(),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.error,
        )
        if (onRetry != null) {
            Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
                Text(text = stringResource(Res.string.common_action_retry))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyMessagePreview() {
    // A feature-owned message, not a designsystem one — Raw stands in for it here.
    CatsListTheme { EmptyMessage(UiText.Raw("Favorite Cats list is empty")) }
}

@Preview(showBackground = true)
@Composable
private fun ErrorMessagePreview() {
    CatsListTheme {
        ErrorMessage(UiText.Raw("Couldn't load a cat. Check your connection and try again."), onRetry = {})
    }
}
