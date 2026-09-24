package com.example.catslist.presentation.components

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

/**
 * A rounded card whose bottom trailing corner steps inward, leaving a notch for the action
 * icons. The three transitions into the notch alternate curvature — convex, concave, convex —
 * which is why this traces arcs rather than subtracting two rounded rects: a difference rounds
 * only the inner corner and leaves the edge transitions as square steps.
 */
internal data class CatCardShape(
    private val corner: Dp,
    private val notchWidth: Dp,
    private val notchHeight: Dp,
    /** The concave turn where the notch's top meets its wall — the corner facing into the card. */
    private val notchCorner: Dp,
    /** The convex fillets where the notch runs out into the card's right and bottom edges. */
    private val notchSweep: Dp,
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline = with(density) {
        val path = notchedPath(
            size = size,
            corner = corner.toPx(),
            notchWidth = notchWidth.toPx(),
            notchHeight = notchHeight.toPx(),
            notchCorner = notchCorner.toPx(),
            notchSweep = notchSweep.toPx(),
        )
        // The notch belongs on the trailing side, so RTL flips the whole outline.
        if (layoutDirection == LayoutDirection.Rtl) path.mirrorHorizontally(size.width)
        Outline.Generic(path)
    }
}

/**
 * Traced clockwise from the top-left corner. Angles are Skia's: 0° points right and grows
 * clockwise, so a positive sweep rounds a convex corner and a negative one cuts a concave.
 */
@Suppress("LongParameterList") // Each is one independent dimension of the same outline.
private fun notchedPath(
    size: Size,
    corner: Float,
    notchWidth: Float,
    notchHeight: Float,
    notchCorner: Float,
    notchSweep: Float,
): Path {
    val width = size.width
    val height = size.height
    val notchLeft = width - notchWidth
    val notchTop = height - notchHeight
    return Path().apply {
        moveTo(corner, 0f)
        lineTo(width - corner, 0f)
        arcTo(Rect(width - 2 * corner, 0f, width, 2 * corner), TOP, QUARTER, false)

        lineTo(width, notchTop - notchSweep)
        arcTo(Rect(width - 2 * notchSweep, notchTop - 2 * notchSweep, width, notchTop), RIGHT, QUARTER, false)

        lineTo(notchLeft + notchCorner, notchTop)
        arcTo(
            Rect(notchLeft, notchTop, notchLeft + 2 * notchCorner, notchTop + 2 * notchCorner),
            TOP,
            -QUARTER,
            false,
        )

        lineTo(notchLeft, height - notchSweep)
        arcTo(Rect(notchLeft - 2 * notchSweep, height - 2 * notchSweep, notchLeft, height), RIGHT, QUARTER, false)

        lineTo(corner, height)
        arcTo(Rect(0f, height - 2 * corner, 2 * corner, height), BOTTOM, QUARTER, false)

        lineTo(0f, corner)
        arcTo(Rect(0f, 0f, 2 * corner, 2 * corner), LEFT, QUARTER, false)
        close()
    }
}

private fun Path.mirrorHorizontally(width: Float) = transform(
    Matrix().apply {
        translate(x = width)
        scale(x = -1f)
    },
)

private const val RIGHT = 0f
private const val BOTTOM = 90f
private const val LEFT = 180f
private const val TOP = 270f
private const val QUARTER = 90f
