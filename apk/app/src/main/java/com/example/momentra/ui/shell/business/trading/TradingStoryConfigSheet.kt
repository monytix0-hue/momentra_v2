package com.example.momentra.ui.shell.business.trading

import androidx.compose.runtime.Composable
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.shell.business.shared.OperatingSheetTheme
import com.example.momentra.ui.shell.business.shared.OperatingStoryConfigSheet
import com.example.momentra.ui.shell.business.shared.TRADING_STORY_PAGES

/**
 * `TRD-MEMORY-v12` — compose the eight-page trading story.
 *
 * The page list is fixed by the backend (`trd-moment` … `trd-sofar`), so this sheet only
 * supplies the trading accent and the trade's own nouns; the generate call is shared with
 * every other vertical so the `previewStoryId` handoff stays in one place.
 */
@Composable
fun TradingStoryConfigSheet(
    model: OperatingProjectionDto,
    momentId: String,
    theme: OperatingSheetTheme,
    onDismiss: () -> Unit,
) {
    OperatingStoryConfigSheet(
        model = model,
        momentId = momentId,
        theme = theme,
        noun = "trading",
        chapters = TRADING_STORY_PAGES,
        onDismiss = onDismiss,
    )
}
