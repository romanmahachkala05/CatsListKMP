package com.example.catslist.presentation.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Clips to [shape] with antialiasing, which `Modifier.clip` does not do for an arbitrary path:
 * hardware clipping is a per-pixel in-or-out test, so a curve steps. This masks instead —
 * content is composited offscreen and `bounds − shape` is erased with [BlendMode.Clear],
 * through the antialiased path rasterizer.
 *
 * Erasing the outside rather than keeping the inside with [BlendMode.DstIn] matters: a blend
 * only touches the pixels its own draw covers, so `DstIn` would leave a concave bite like this
 * card's notch behind.
 *
 * Costs one offscreen layer per use, so it is not a drop-in for every `clip` in the app.
 */
fun Modifier.smoothClip(shape: Shape): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithCache {
        val kept = Path().apply {
            addOutline(shape.createOutline(size, layoutDirection, this@drawWithCache))
        }
        val bounds = Path().apply { addRect(Rect(Offset.Zero, size)) }
        val erased = Path().apply { op(bounds, kept, PathOperation.Difference) }
        onDrawWithContent {
            drawContent()
            // The color is irrelevant; Clear only reads this shape's coverage.
            drawPath(path = erased, color = Color.Black, blendMode = BlendMode.Clear)
        }
    }
