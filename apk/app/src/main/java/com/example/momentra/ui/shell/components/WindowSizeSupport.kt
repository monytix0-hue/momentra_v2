package com.example.momentra.ui.shell.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Adaptive layout tokens for every screen size and shape.
 * Uses [LocalConfiguration] (no Activity required) so sheets and tabs share the same rules.
 */
@Immutable
data class MomentraWindowSize(
    val widthDp: Int,
    val heightDp: Int,
    val isCompactWidth: Boolean,
    val isMediumWidth: Boolean,
    val isExpandedWidth: Boolean,
    val isShortHeight: Boolean,
) {
    val contentHorizontalPadding: Dp
        get() = when {
            isExpandedWidth -> 32.dp
            isMediumWidth -> 24.dp
            else -> 16.dp
        }

    val contentVerticalPadding: Dp
        get() = if (isShortHeight) 8.dp else 12.dp

    val sectionSpacing: Dp
        get() = if (isShortHeight) 10.dp else 14.dp

    /** Max fraction of screen height for modal bottom sheets. */
    val sheetMaxFraction: Float
        get() = when {
            isExpandedWidth -> 0.72f
            isShortHeight -> 0.92f
            else -> 0.88f
        }

    val sheetContentMaxWidth: Dp?
        get() = if (isExpandedWidth) 560.dp else null

    val hubColumnCount: Int
        get() = when {
            isExpandedWidth -> 4
            isMediumWidth -> 3
            else -> 2
        }

    val hubTileMinHeight: Dp
        get() = if (isShortHeight) 80.dp else 88.dp

    val hubTileMaxHeight: Dp
        get() = if (isShortHeight) 96.dp else 104.dp

    val hubHeroHeight: Dp
        get() = when {
            isShortHeight -> 72.dp
            isExpandedWidth -> 140.dp
            else -> 120.dp
        }

    val nestedSheetMaxHeightFraction: Float
        get() = if (isShortHeight) 0.45f else 0.55f

    fun contentPadding(): PaddingValues = PaddingValues(
        horizontal = contentHorizontalPadding,
        vertical = contentVerticalPadding,
    )
}

@Composable
fun rememberMomentraWindowSize(): MomentraWindowSize {
    val configuration = LocalConfiguration.current
    val widthDp = configuration.screenWidthDp
    val heightDp = configuration.screenHeightDp
    return remember(widthDp, heightDp) {
        MomentraWindowSize(
            widthDp = widthDp,
            heightDp = heightDp,
            isCompactWidth = widthDp < 600,
            isMediumWidth = widthDp in 600..839,
            isExpandedWidth = widthDp >= 840,
            isShortHeight = heightDp < 700,
        )
    }
}

@Composable
fun rememberScreenHeightDp(): Dp {
    val configuration = LocalConfiguration.current
    return configuration.screenHeightDp.dp
}

@Composable
fun sheetMaxHeight(window: MomentraWindowSize = rememberMomentraWindowSize()): Dp {
    val screenH = rememberScreenHeightDp()
    return screenH * window.sheetMaxFraction
}

@Composable
fun nestedListMaxHeight(window: MomentraWindowSize = rememberMomentraWindowSize()): Dp {
    val screenH = rememberScreenHeightDp()
    return screenH * window.nestedSheetMaxHeightFraction
}
