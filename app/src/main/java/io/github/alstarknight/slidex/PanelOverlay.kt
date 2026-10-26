package io.github.alstarknight.slidex

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest

/**
 * Lets the panel's content make the panel see-through for a moment, e.g. while the brightness slider is dragged
 * so the screen behind shows the new brightness. Pass true to fade the panel out and false to bring it back.
 */
val LocalPanelSeeThrough = staticCompositionLocalOf<(Boolean) -> Unit> { {} }

/**
 * Where the panel is. While a finger holds it, [held] says how far out it is (0 hidden, 1 fully out); otherwise it
 * slides fully [open] or shut. A flick faster than [flingVelocity] px/s decides how a held panel settles.
 */
@Stable
class PanelSlide(private val flingVelocity: Float) {
    var held by mutableStateOf<Float?>(null)
    var open by mutableStateOf(true)
        private set

    /** Lets go of a held panel moving out at [outwardVelocity] px/s (negative when moving in). */
    fun release(outwardVelocity: Float) {
        val fraction = held ?: return
        open = settlesOpen(fraction, outwardVelocity, flingVelocity)
        held = null
    }

    fun close() {
        held = null
        open = false
    }
}

/** A flick at least this fast decides whether a panel let go opens or shuts, like the system's drawers. */
const val PANEL_FLING_DP_PER_SECOND = 400

@Composable
fun PanelOverlay(
    slide: PanelSlide,
    side: Side,
    widthFraction: Float,
    /** How dark the screen behind the panel gets, from 0 to 1. */
    scrimAlpha: Float,
    seeThroughAllowed: Boolean,
    onDismiss: () -> Unit,
    onHidden: () -> Unit,
    content: @Composable () -> Unit,
) {
    // How far out the panel is, from 0 (hidden) to 1.
    val progress = remember { Animatable(0f) }
    var seeThrough by remember { mutableStateOf(false) }
    val setSeeThrough = remember { { on: Boolean -> seeThrough = on } }
    // 0 shows the panel normally, 1 fades it to SEE_THROUGH_OPACITY and removes the dimming behind it.
    val fade by animateFloatAsState(if (seeThrough && seeThroughAllowed) 1f else 0f, label = "seeThrough")

    LaunchedEffect(slide) {
        snapshotFlow { slide.held to slide.open }.collectLatest { (held, open) ->
            if (held != null) {
                progress.snapTo(held)
            } else {
                progress.animateTo(if (open) 1f else 0f, tween(220))
                if (!open) onHidden()
            }
        }
    }
    val pullShut = remember(slide, side) { PullShut(slide, side) }

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = progress.value * (1f - fade) }
                .background(Color.Black.copy(alpha = scrimAlpha))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
        )
        val inner = 24.dp
        Surface(
            modifier = Modifier
                .align(if (side == Side.LEFT) Alignment.CenterStart else Alignment.CenterEnd)
                .fillMaxHeight()
                .fillMaxWidth(widthFraction)
                .onSizeChanged { pullShut.width = it.width.toFloat() }
                .nestedScroll(pullShut)
                .graphicsLayer {
                    val direction = if (side == Side.LEFT) -1f else 1f
                    translationX = (1f - progress.value) * size.width * direction
                    alpha = 1f - fade * (1f - SEE_THROUGH_OPACITY)
                },
            shape = if (side == Side.LEFT) {
                RoundedCornerShape(topEnd = inner, bottomEnd = inner)
            } else {
                RoundedCornerShape(topStart = inner, bottomStart = inner)
            },
            // Every page paints its own background, so pages can look different.
            color = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            CompositionLocalProvider(LocalPanelSeeThrough provides setSeeThrough) {
                content()
            }
        }
    }
}

/** How opaque the panel stays while see-through, so the slider under the finger can still be made out. */
private const val SEE_THROUGH_OPACITY = 0.2f

/**
 * Lets a sideways swipe toward the panel's edge that the pages cannot take (on the first page of a right panel,
 * the last of a left one) push the panel back: it follows the finger and settles like a panel dragged out.
 */
private class PullShut(private val slide: PanelSlide, private val side: Side) : NestedScrollConnection {
    var width = 0f

    /** How far [dx] moves toward the panel's edge. */
    private fun shutward(dx: Float) = if (side == Side.RIGHT) dx else -dx

    private fun pull(dx: Float): Offset {
        slide.held = ((slide.held ?: 1f) - shutward(dx) / width).coerceIn(0f, 1f)
        return Offset(dx, 0f)
    }

    // Once pulling, every sideways move goes to the panel, so it follows the finger both ways.
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
        if (slide.held != null && source == NestedScrollSource.UserInput) pull(available.x) else Offset.Zero

    // A panel already closing is left alone.
    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
        if (source == NestedScrollSource.UserInput && slide.open && width > 0f && shutward(available.x) > 0f) {
            pull(available.x)
        } else {
            Offset.Zero
        }

    override suspend fun onPreFling(available: Velocity): Velocity {
        if (slide.held == null) return Velocity.Zero
        slide.release(-shutward(available.x))
        return available
    }
}
