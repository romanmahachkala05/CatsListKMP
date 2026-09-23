package com.example.catslist.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.example.catslist.core.designsystem.resources.Res
import com.example.catslist.core.designsystem.resources.common_cd_pull_to_refresh
import com.example.catslist.core.designsystem.resources.common_cd_refresh_failed
import com.example.catslist.core.designsystem.resources.common_cd_refresh_succeeded
import com.example.catslist.core.designsystem.resources.common_cd_refreshing
import com.example.catslist.core.designsystem.resources.ic_arrow_down
import com.example.catslist.core.designsystem.resources.ic_check
import com.example.catslist.core.designsystem.resources.ic_close
import com.example.catslist.presentation.theme.successColor
import kotlin.time.TimeSource
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private enum class RefreshPhase { Idle, Refreshing, Succeeded, Failed }

/**
 * Pull-to-refresh that reports the outcome: spinner while the request runs, then a tick or a
 * cross, and only then retracts. Each step is held [PHASE_MINIMUM_MILLIS] so a fast response
 * still reads as a sequence rather than a flicker.
 *
 * @param topInset where the top of the screen effectively is. Content is edge-to-edge, so
 *   without this the indicator rests behind the status bar. Lists pass their `contentPadding`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatPullToRefresh(
    signal: RefreshSignal,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    topInset: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    // Paging refreshes on its own at launch; only a real pull should show the indicator.
    var pullRequested by remember { mutableStateOf(false) }
    val phase = rememberRefreshPhase(if (pullRequested) signal else RefreshSignal.Idle)
    val state = rememberPullToRefreshState()
    val isBusy = phase != RefreshPhase.Idle

    // One value for both drag and settle: with two, release would jump before animating.
    val pulledOffset = CONTENT_OFFSET * state.distanceFraction.coerceIn(0f, 1f)
    val animatedOffset = remember { Animatable(0.dp, Dp.VectorConverter) }
    LaunchedEffect(isBusy, pulledOffset) {
        when {
            isBusy -> animatedOffset.animateTo(CONTENT_OFFSET)
            // Following the finger has to be exact; an animation here reads as lag.
            pulledOffset > 0.dp -> animatedOffset.snapTo(pulledOffset)
            else -> animatedOffset.animateTo(0.dp)
        }
    }
    val offset = animatedOffset.value

    // The result has to outlive its phase: Idle draws the pull arrow, so without this the
    // tick would flip back to an arrow while the indicator is still sliding away. Position,
    // not `distanceFraction`, decides when it is gone — that stays near 1 during the refresh.
    var lastOutcome by remember { mutableStateOf(RefreshPhase.Idle) }
    val hasSettled = offset < SETTLED_THRESHOLD
    LaunchedEffect(phase, hasSettled) {
        if (phase != RefreshPhase.Idle) {
            lastOutcome = phase
        } else if (hasSettled && lastOutcome != RefreshPhase.Idle) {
            // Only once a cycle has actually run. Letting go of the pull as soon as it
            // settles would drop the whole sequence for a signal that arrives a frame late.
            lastOutcome = RefreshPhase.Idle
            pullRequested = false
        }
    }
    val shownPhase = if (phase == RefreshPhase.Idle) lastOutcome else phase

    // Hidden has to mean fully above the screen: under edge-to-edge the status bar is
    // transparent, so parking the indicator at `topInset - size` leaves it visible.
    val progress = if (CONTENT_OFFSET > 0.dp) (offset / CONTENT_OFFSET).coerceIn(0f, 1f) else 0f
    val indicatorY = lerp(-INDICATOR_SIZE, topInset + INDICATOR_MARGIN, progress)

    Box(
        modifier = modifier.pullToRefresh(
            isRefreshing = isBusy,
            state = state,
            onRefresh = {
                pullRequested = true
                onRefresh()
            },
        ),
    ) {
        Box(modifier = Modifier.fillMaxSize().graphicsLayer { translationY = offset.toPx() }) {
            content()
        }
        RefreshIndicator(
            phase = shownPhase,
            pullFraction = state.distanceFraction,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .graphicsLayer { translationY = indicatorY.toPx() },
        )
    }
}

/** Drives [RefreshPhase] from [signal], holding each step long enough to be read. */
@Composable
private fun rememberRefreshPhase(signal: RefreshSignal): RefreshPhase {
    var phase by remember { mutableStateOf(RefreshPhase.Idle) }
    var spinnerShownAt by remember { mutableStateOf(TimeSource.Monotonic.markNow()) }
    LaunchedEffect(signal) {
        if (signal == RefreshSignal.Running) {
            phase = RefreshPhase.Refreshing
            spinnerShownAt = TimeSource.Monotonic.markNow()
        } else if (phase == RefreshPhase.Refreshing) {
            // Held here, not in the branch above: a delay there is canceled by the very
            // `signal` change it would be holding back.
            val shownFor = spinnerShownAt.elapsedNow().inWholeMilliseconds
            if (shownFor < PHASE_MINIMUM_MILLIS) delay(PHASE_MINIMUM_MILLIS - shownFor)
            phase = if (signal == RefreshSignal.Failed) RefreshPhase.Failed else RefreshPhase.Succeeded
            delay(PHASE_MINIMUM_MILLIS)
            phase = RefreshPhase.Idle
        }
    }
    return phase
}

@Composable
private fun RefreshIndicator(
    phase: RefreshPhase,
    pullFraction: Float,
    modifier: Modifier = Modifier,
) {
    val refreshing = stringResource(Res.string.common_cd_refreshing)
    Surface(
        modifier = modifier.size(INDICATOR_SIZE),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = INDICATOR_ELEVATION,
    ) {
        Box(contentAlignment = Alignment.Center) {
            when (phase) {
                RefreshPhase.Idle -> Icon(
                    painter = painterResource(Res.drawable.ic_arrow_down),
                    contentDescription = stringResource(Res.string.common_cd_pull_to_refresh),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(ICON_SIZE)
                        .rotate(pullFraction.coerceIn(0f, 1f) * HALF_TURN),
                )
                RefreshPhase.Refreshing -> CircularProgressIndicator(
                    modifier = Modifier
                        .size(ICON_SIZE)
                        // The one phase that can last seconds; the others announce themselves
                        // through their icon.
                        .semantics { contentDescription = refreshing },
                    strokeWidth = SPINNER_STROKE,
                    color = MaterialTheme.colorScheme.primary,
                )
                RefreshPhase.Succeeded -> Icon(
                    painter = painterResource(Res.drawable.ic_check),
                    contentDescription = stringResource(Res.string.common_cd_refresh_succeeded),
                    tint = successColor,
                    modifier = Modifier.size(ICON_SIZE),
                )
                RefreshPhase.Failed -> Icon(
                    painter = painterResource(Res.drawable.ic_close),
                    contentDescription = stringResource(Res.string.common_cd_refresh_failed),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(ICON_SIZE),
                )
            }
        }
    }
}

private val INDICATOR_SIZE = 40.dp
private val INDICATOR_MARGIN = 8.dp

/** Near enough to home to count as arrived; an animation's tail need not reach zero. */
private val SETTLED_THRESHOLD = 1.dp
private val ICON_SIZE = 22.dp
private val INDICATOR_ELEVATION = 4.dp
private val CONTENT_OFFSET = 72.dp
private val SPINNER_STROKE = 2.5.dp
private const val PHASE_MINIMUM_MILLIS = 300L
private const val HALF_TURN = 180f
