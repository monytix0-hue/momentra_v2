package com.example.momentra.ui.shell.business.shared

import androidx.compose.runtime.Composable
import com.example.momentra.data.api.OperatingProjectionDto

/** Chapter labels kept for Dist/legacy callers; Story v3 always renders 8 slides. */
val TRADING_STORY_PAGES: List<String> = listOf("Sales", "Purchases", "Stock", "People")
val DISTRIBUTION_STORY_PAGES: List<String> = listOf("Dealers", "Routes", "Stock", "People")
val MANUFACTURING_STORY_PAGES: List<String> = listOf("Orders", "Production", "Delivery", "People")
val MTO_STORY_PAGES: List<String> = listOf("Orders", "Making", "Delivery", "People")
val MARKETING_STORY_PAGES: List<String> = listOf("Campaign", "Spend", "Results", "People")

/**
 * Create Business Story for Trading and Distribution — Figma TRD-STORY / DST-STORY
 * (config → composing → P1–P8 → saved → share → states) via Story v3 API.
 */
@Composable
internal fun OperatingStoryConfigSheet(
    model: OperatingProjectionDto,
    momentId: String,
    theme: OperatingSheetTheme,
    noun: String,
    chapters: List<String>,
    onDismiss: () -> Unit,
) {
    // `chapters` retained for API compatibility with Dist/Marketing hosts; v3 always uses P1–P8.
    TradingStoryV3Flow(
        model = model,
        momentId = momentId,
        theme = theme,
        noun = noun,
        onDismiss = onDismiss,
    )
}
