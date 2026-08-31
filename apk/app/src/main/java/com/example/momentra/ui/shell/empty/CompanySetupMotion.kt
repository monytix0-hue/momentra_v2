/**
 * Figma Company Setup motion helpers (692:38403 cohort).
 * Prototype loops in Figma; apps play the stagger once on appear.
 * Ease: cubic-bezier(0.16, 1, 0.3, 1).
 */
package com.example.momentra.ui.shell.empty

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

val CoFigmaEase = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

@Composable
fun CoReveal(
    delayMs: Int,
    durationMs: Int = 400,
    fromY: Float = 24f,
    fromScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val progress by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(
            durationMillis = durationMs,
            delayMillis = delayMs,
            easing = CoFigmaEase,
        ),
        label = "coReveal",
    )
    Box(
        modifier = Modifier.graphicsLayer {
            alpha = progress
            translationY = (1f - progress) * fromY
            val s = fromScale + (1f - fromScale) * progress
            scaleX = s
            scaleY = s
        },
    ) {
        content()
    }
}
