package io.github.alstarknight.slideanywhere

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first

@Composable
fun PanelOverlay(
    visibleState: MutableTransitionState<Boolean>,
    side: Side,
    widthFraction: Float,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
    onHidden: () -> Unit,
) {
    val transition = updateTransition(visibleState, label = "panel")
    val progress = transition.animateFloat(label = "progress", transitionSpec = { tween(220) }) { if (it) 1f else 0f }

    LaunchedEffect(visibleState) {
        snapshotFlow { visibleState.isIdle && !visibleState.currentState }.first { it }
        onHidden()
    }

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = progress.value }
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
                },
            shape = if (side == Side.LEFT) {
                RoundedCornerShape(topEnd = inner, bottomEnd = inner)
            } else {
                RoundedCornerShape(topStart = inner, bottomStart = inner)
            },
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            PanelContent(onOpenSettings)
        }
    }
}

@Composable
private fun PanelContent(onOpenSettings: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.panel_empty_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.panel_empty_body), style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onOpenSettings, modifier = Modifier.align(Alignment.End)) {
            Text(stringResource(R.string.panel_open_settings))
        }
    }
}
