package com.example.momentra.ui.shell.tour

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

class TourTargetRegistry {
    private val bounds = mutableStateMapOf<TourTargetId, Rect>()

    fun update(id: TourTargetId, rect: Rect) {
        if (rect.width <= 0f || rect.height <= 0f) {
            bounds.remove(id)
        } else {
            bounds[id] = rect
        }
    }

    fun clear(id: TourTargetId) {
        bounds.remove(id)
    }

    fun get(id: TourTargetId): Rect? = bounds[id]
}

val LocalTourTargetRegistry = staticCompositionLocalOf<TourTargetRegistry?> { null }

val LocalTourController = staticCompositionLocalOf<TourController?> { null }

@Composable
fun rememberTourTargetRegistry(): TourTargetRegistry = remember { TourTargetRegistry() }

/** Registers this node's root bounds for soft-tip spotlights. */
fun Modifier.tourTarget(id: TourTargetId): Modifier = composed {
    val registry = LocalTourTargetRegistry.current
    DisposableEffect(id, registry) {
        onDispose { registry?.clear(id) }
    }
    if (registry == null) {
        this
    } else {
        this.onGloballyPositioned { coords ->
            registry.update(id, coords.boundsInRoot())
        }
    }
}
