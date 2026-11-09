package com.example.momentra.ui.shell.business.distribution

import androidx.compose.runtime.Composable
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.shell.business.shared.DISTRIBUTION_STORY_PAGES
import com.example.momentra.ui.shell.business.shared.OperatingSheetTheme
import com.example.momentra.ui.shell.business.shared.OperatingStoryConfigSheet

/**
 * `DST-MEMORY-v12` — compose the eight-page distribution story.
 *
 * Distribution swaps the trading `Orders` page for `Dealers`: the story still reports the
 * same recorded window, but the third page speaks to outlets and settlement rather than
 * wholesale buyers.
 */
@Composable
fun DistributionStoryConfigSheet(
    model: OperatingProjectionDto,
    momentId: String,
    theme: OperatingSheetTheme,
    onDismiss: () -> Unit,
) {
    OperatingStoryConfigSheet(
        model = model,
        momentId = momentId,
        theme = theme,
        noun = "distribution",
        chapters = DISTRIBUTION_STORY_PAGES,
        onDismiss = onDismiss,
    )
}
