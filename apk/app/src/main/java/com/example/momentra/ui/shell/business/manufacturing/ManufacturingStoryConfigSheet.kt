package com.example.momentra.ui.shell.business.manufacturing

import androidx.compose.runtime.Composable
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.shell.business.shared.MANUFACTURING_STORY_PAGES
import com.example.momentra.ui.shell.business.shared.MTO_STORY_PAGES
import com.example.momentra.ui.shell.business.shared.OperatingSheetTheme
import com.example.momentra.ui.shell.business.shared.OperatingStoryConfigSheet

/**
 * MFG-MEMORY / MFG-MTO-STORY — compose the eight-page manufacturing story.
 * Plant and job-order share the manufacturing story profile; chapter nouns differ.
 */
@Composable
fun ManufacturingStoryConfigSheet(
    model: OperatingProjectionDto,
    momentId: String,
    theme: OperatingSheetTheme,
    onDismiss: () -> Unit,
) {
    val mto = model.subtype.equals("MADE_TO_ORDER", ignoreCase = true)
    OperatingStoryConfigSheet(
        model = model,
        momentId = momentId,
        theme = theme,
        noun = if (mto) "make-to-order" else "manufacturing",
        chapters = if (mto) MTO_STORY_PAGES else MANUFACTURING_STORY_PAGES,
        onDismiss = onDismiss,
    )
}
