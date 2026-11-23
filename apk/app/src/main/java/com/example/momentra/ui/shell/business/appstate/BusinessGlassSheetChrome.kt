package com.example.momentra.ui.shell.business.appstate

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Business glass sheet chrome — clear container, soft scrim, frost stack + translucent tint.
 * Callers: DateRow, ManageMoment (BUSINESS), attention View-all.
 */
object BusinessGlassSheetChrome {
    /** Soft enough that live UI still reads through the panel. */
    val Scrim: Color = Color.Black.copy(alpha = 0.35f)
    val Container: Color = Color.Transparent
    /** ~0.55–0.65 effective over frost (not flat 0.72 slab). */
    val PanelFill: Color get() = BusinessAppStateTokens.GlassCardFill.copy(alpha = 0.58f)
    val FrostTop: Color = Color.White.copy(alpha = 0.10f)
    val FrostMid: Color = Color.White.copy(alpha = 0.04f)
    val PanelStroke: Color get() = BusinessAppStateTokens.GlassCardStroke
    val PanelStrokeWidth get() = BusinessAppStateTokens.GlassCardStrokeWidth
    val CornerRadius get() = BusinessAppStateTokens.CardRadius
}

fun businessGlassSheetShape(): RoundedCornerShape = RoundedCornerShape(
    topStart = BusinessGlassSheetChrome.CornerRadius,
    topEnd = BusinessGlassSheetChrome.CornerRadius,
)

/** Frost gradient + tint for in-place content (Manage Moment root). */
fun Modifier.businessGlassPanelBackground(): Modifier {
    val shape = businessGlassSheetShape()
    return this
        .clip(shape)
        .background(
            Brush.verticalGradient(
                listOf(
                    BusinessGlassSheetChrome.FrostTop,
                    BusinessGlassSheetChrome.FrostMid,
                    BusinessGlassSheetChrome.PanelFill,
                ),
            ),
        )
        .border(
            BusinessGlassSheetChrome.PanelStrokeWidth,
            BusinessGlassSheetChrome.PanelStroke,
            shape,
        )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessGlassModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = businessGlassSheetShape()
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = BusinessGlassSheetChrome.Container,
        scrimColor = BusinessGlassSheetChrome.Scrim,
        tonalElevation = 0.dp,
        dragHandle = null,
        shape = shape,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape),
        ) {
            // Soft frost wash (fake backdrop frost — Material3 modals do not blur the window).
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                BusinessGlassSheetChrome.FrostTop,
                                BusinessGlassSheetChrome.FrostMid,
                                Color.Transparent,
                            ),
                        ),
                    )
                    .blur(16.dp),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BusinessGlassSheetChrome.PanelFill)
                    .border(
                        BusinessGlassSheetChrome.PanelStrokeWidth,
                        BusinessGlassSheetChrome.PanelStroke,
                        shape,
                    )
                    .padding(bottom = 8.dp),
                content = content,
            )
        }
    }
}
