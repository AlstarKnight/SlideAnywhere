package io.github.alstarknight.slidex

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.updateTransition
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first

/**
 * Lets the panel's content make the panel see-through for a moment, e.g. while the brightness slider is dragged
 * so the screen behind shows the new brightness. Pass true to fade the panel out and false to bring it back.
 */
val LocalPanelSeeThrough = staticCompositionLocalOf<(Boolean) -> Unit> { {} }

@Composable
fun PanelOverlay(
    visibleState: MutableTransitionState<Boolean>,
    side: Side,
    widthFraction: Float,
    seeThroughAllowed: Boolean,
    onDismiss: () -> Unit,
    onHidden: () -> Unit,
    content: @Composable () -> Unit,
) {
    val transition = updateTransition(visibleState, label = "panel")
    val progress = transition.animateFloat(label = "progress", transitionSpec = { tween(220) }) { if (it) 1f else 0f }
    var seeThrough by remember { mutableStateOf(false) }
    val setSeeThrough = remember { { on: Boolean -> seeThrough = on } }
    // 0 shows the panel normally, 1 fades it to SEE_THROUGH_OPACITY and removes the dimming behind it.
    val fade by animateFloatAsState(if (seeThrough && seeThroughAllowed) 1f else 0f, label = "seeThrough")

    LaunchedEffect(visibleState) {
        snapshotFlow { visibleState.isIdle && !visibleState.currentState }.first { it }
        onHidden()
    }

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = progress.value * (1f - fade) }
                .background(Color.Black.copy(alpha = 0.35f))
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
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            CompositionLocalProvider(LocalPanelSeeThrough provides setSeeThrough) {
                content()
            }
        }
    }
}

/** How opaque the panel stays while see-through, so the slider under the finger can still be made out. */
private const val SEE_THROUGH_OPACITY = 0.2f
