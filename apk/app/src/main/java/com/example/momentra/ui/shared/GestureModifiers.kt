package com.example.momentra.ui.shared

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Native gesture controls for horizontal swiping between tabs or modules.
 */
fun Modifier.horizontalSwipe(
    onSwipeLeft: () -> Unit = {},
    onSwipeRight: () -> Unit = {},
    threshold: Float = 60f,
): Modifier = this.pointerInput(Unit) {
    var totalDrag = 0f
    detectHorizontalDragGestures(
        onDragStart = { totalDrag = 0f },
        onHorizontalDrag = { _, dragAmount ->
            totalDrag += dragAmount
        },
        onDragEnd = {
            if (totalDrag < -threshold) {
                onSwipeLeft()
            } else if (totalDrag > threshold) {
                onSwipeRight()
            }
        },
    )
}
