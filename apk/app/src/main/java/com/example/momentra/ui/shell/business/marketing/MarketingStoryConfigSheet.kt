package com.example.momentra.ui.shell.business.marketing

import androidx.compose.runtime.Composable
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.shell.business.shared.MARKETING_STORY_PAGES
import com.example.momentra.ui.shell.business.shared.OperatingSheetTheme
import com.example.momentra.ui.shell.business.shared.OperatingStoryConfigSheet

/**
 * Marketing Memory story config — thin wrapper around [OperatingStoryConfigSheet]
 * with campaign chapters (`Moment` … `So far`). Client indigo / Own magenta come from [theme].
 */
@Composable
fun MarketingStoryConfigSheet(
    model: OperatingProjectionDto,
    momentId: String,
    theme: OperatingSheetTheme,
    onDismiss: () -> Unit,
) {
    OperatingStoryConfigSheet(
        model = model,
        momentId = momentId,
        theme = theme,
        noun = if (isOwnCampaign(model.subtype)) "own campaign" else "campaign",
        chapters = MARKETING_STORY_PAGES,
        onDismiss = onDismiss,
    )
}
