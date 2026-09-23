package com.example.catslist.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.example.catslist.core.designsystem.resources.Res
import com.example.catslist.core.designsystem.resources.common_cat_image_failed
import com.example.catslist.core.designsystem.resources.common_cd_download_cat
import com.example.catslist.core.designsystem.resources.common_cd_favorite_cat
import com.example.catslist.core.designsystem.resources.common_cd_retry_cat_image
import com.example.catslist.core.designsystem.resources.ic_download
import com.example.catslist.core.designsystem.resources.ic_favorite_filled
import com.example.catslist.core.designsystem.resources.ic_favorite_outline
import com.example.catslist.core.designsystem.resources.ic_image_failed
import com.example.catslist.domain.model.Cat
import com.example.catslist.presentation.theme.CatsListTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** A card is all Boxes and an undescribed image, so a test has nothing else to find it by. */
const val CAT_CARD_TAG = "catCard"

/** One cat card: image in a notched frame, with the action icons sitting in the notch. */
@Composable
fun CatItem(
    cat: Cat,
    onFavoriteClick: () -> Unit,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Bumping `attempt` hands the image a fresh painter, and so a fresh request.
    var attempt by remember(cat.url) { mutableIntStateOf(0) }
    var status by remember(cat.url, attempt) { mutableStateOf(ImageStatus.Loading) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = CARD_MARGIN_HORIZONTAL, vertical = CARD_MARGIN_VERTICAL)
            .testTag(CAT_CARD_TAG),
    ) {
        // One mask for the image and its stand-ins: each smoothClip costs an offscreen layer.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(IMAGE_HEIGHT)
                .smoothClip(CAT_CARD_SHAPE),
        ) {
            key(attempt) {
                AsyncImage(
                    model = cat.url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    onState = { status = it.toImageStatus() },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            when (status) {
                ImageStatus.Loading -> Box(Modifier.fillMaxSize().background(shimmerBrush()))
                ImageStatus.Loaded -> Unit
                ImageStatus.Failed -> CatImageError(
                    onRetry = { attempt++ },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        // Outside the mask: a stroke is centered on the path, so clipping halves its width.
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(BORDER_WIDTH, MaterialTheme.colorScheme.onSurface, CAT_CARD_SHAPE),
        )
        CatActions(
            isFavorite = cat.isFavorite,
            onFavoriteClick = onFavoriteClick,
            onDownloadClick = onDownloadClick,
            modifier = Modifier.align(Alignment.BottomEnd),
        )
    }
}

/** [CatItem]'s footprint with no cat yet: all shimmer, no frame. */
@Composable
fun CatItemPlaceholder(modifier: Modifier = Modifier) {
    val shimmer = shimmerBrush()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = CARD_MARGIN_HORIZONTAL, vertical = CARD_MARGIN_VERTICAL),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(IMAGE_HEIGHT)
                .smoothClip(CAT_CARD_SHAPE)
                .background(shimmer),
        )
    }
}

/** Stands in for a cat whose image request failed. Tapping retries just this one. */
@Composable
private fun CatImageError(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClickLabel = stringResource(Res.string.common_cd_retry_cat_image), onClick = onRetry),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_image_failed),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(ERROR_ICON_SIZE),
        )
        Text(
            text = stringResource(Res.string.common_cat_image_failed),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

private enum class ImageStatus { Loading, Loaded, Failed }

private fun AsyncImagePainter.State.toImageStatus(): ImageStatus = when (this) {
    is AsyncImagePainter.State.Loading -> ImageStatus.Loading
    is AsyncImagePainter.State.Error -> ImageStatus.Failed
    // Empty means no request was made, so nothing is coming and nothing should shimmer.
    is AsyncImagePainter.State.Empty, is AsyncImagePainter.State.Success -> ImageStatus.Loaded
}

/** The skeleton shimmers; it has no text or role for a test to find it by. */
const val CAT_LIST_PLACEHOLDER_TAG = "catListPlaceholder"

/** A screenful of [CatItemPlaceholder]s for a screen waiting on its first cats. */
@Composable
fun CatListPlaceholder(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    count: Int = PLACEHOLDER_COUNT,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().testTag(CAT_LIST_PLACEHOLDER_TAG),
        contentPadding = contentPadding,
        userScrollEnabled = false,
    ) {
        items(count) { CatItemPlaceholder() }
    }
}

/** Sized from the notch constants, so the row cannot outgrow the gap it sits in. */
@Composable
private fun CatActions(
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val favoriteTint by animateColorAsState(
        targetValue = if (isFavorite) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "favoriteTint",
    )
    Row(
        modifier = modifier.width(NOTCH_WIDTH).height(NOTCH_HEIGHT),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onFavoriteClick) {
            Icon(
                painter = painterResource(
                    if (isFavorite) Res.drawable.ic_favorite_filled else Res.drawable.ic_favorite_outline,
                ),
                contentDescription = stringResource(Res.string.common_cd_favorite_cat),
                tint = favoriteTint,
                modifier = Modifier.size(ICON_SIZE),
            )
        }
        IconButton(onClick = onDownloadClick) {
            Icon(
                painter = painterResource(Res.drawable.ic_download),
                contentDescription = stringResource(Res.string.common_cd_download_cat),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(ICON_SIZE),
            )
        }
    }
}

private val CARD_MARGIN_HORIZONTAL = 16.dp
private val CARD_MARGIN_VERTICAL = 8.dp
private val CARD_CORNER = 20.dp
private val IMAGE_HEIGHT = 300.dp
private val BORDER_WIDTH = 4.dp
private val ICON_SIZE = 24.dp
private val ERROR_ICON_SIZE = 40.dp

/** Two 48dp touch targets side by side, plus the breathing room around them. */
private val NOTCH_WIDTH = 104.dp
private val NOTCH_HEIGHT = 42.dp
private val NOTCH_CORNER = 20.dp
private val NOTCH_SWEEP = 16.dp

private const val PLACEHOLDER_COUNT = 2

/** Declared last: top-level initializers run in file order, and this one reads the rest. */
private val CAT_CARD_SHAPE =
    CatCardShape(CARD_CORNER, NOTCH_WIDTH, NOTCH_HEIGHT, NOTCH_CORNER, NOTCH_SWEEP)

@Preview(name = "Not favorite", showBackground = true)
@Composable
private fun CatItemPreview() {
    CatsListTheme {
        CatItem(
            cat = Cat(id = "1", url = "", width = 300, height = 300, isFavorite = false),
            onFavoriteClick = {},
            onDownloadClick = {},
        )
    }
}

@Preview(name = "Favorite", showBackground = true)
@Composable
private fun CatItemFavoritePreview() {
    CatsListTheme {
        CatItem(
            cat = Cat(id = "1", url = "", width = 300, height = 300, isFavorite = true),
            onFavoriteClick = {},
            onDownloadClick = {},
        )
    }
}

@Preview(name = "Placeholder", showBackground = true)
@Composable
private fun CatItemPlaceholderPreview() {
    CatsListTheme { CatItemPlaceholder() }
}

@Preview(name = "Image failed", showBackground = true)
@Composable
private fun CatImageErrorPreview() {
    CatsListTheme {
        Box(modifier = Modifier.fillMaxWidth().padding(CARD_MARGIN_HORIZONTAL)) {
            CatImageError(onRetry = {}, modifier = Modifier.fillMaxWidth().height(IMAGE_HEIGHT))
        }
    }
}
