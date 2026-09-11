package com.example.momentra.ui.shell.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Shape-aware modal bottom sheet:
 * - Caps height by [MomentraWindowSize.sheetMaxFraction]
 * - Uses Material3 window insets only (no extra navigationBarsPadding on content)
 * - Short forms can stay partially expanded; long forms scroll inside the cap
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MomentraModalBottomSheet(
    onDismissRequest: () -> Unit,
    containerColor: Color,
    modifier: Modifier = Modifier,
    skipPartiallyExpanded: Boolean = false,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = skipPartiallyExpanded),
    window: MomentraWindowSize = rememberMomentraWindowSize(),
    dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    content: @Composable ColumnScope.() -> Unit,
) {
    val maxH = sheetMaxHeight(window)
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = containerColor,
        dragHandle = dragHandle,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxH),
            contentAlignment = Alignment.TopCenter,
        ) {
            val maxW = window.sheetContentMaxWidth
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (maxW != null) Modifier.widthIn(max = maxW) else Modifier)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = window.contentHorizontalPadding)
                    .padding(top = 4.dp, bottom = 16.dp),
                content = content,
            )
        }
    }
}
