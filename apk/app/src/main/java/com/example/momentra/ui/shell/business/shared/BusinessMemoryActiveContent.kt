package com.example.momentra.ui.shell.business.shared

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.momentra.data.repository.BusinessSliceRepository

/** Business Memory when the opening type is not Money, Daily, or Team. */
@Composable
fun BusinessMemoryActiveContent(
    momentId: String?,
    momentTitle: String?,
    refreshToken: Long,
    momentTypeCode: String? = null,
    companyId: String? = null,
    onOpenQuickAdd: () -> Unit = {},
    repository: BusinessSliceRepository = remember { BusinessSliceRepository() },
    modifier: Modifier = Modifier,
) {
    BusinessMemoryScreen(
        momentId = momentId,
        refreshToken = refreshToken,
        momentTypeCode = momentTypeCode,
        companyId = companyId,
        onRecordLearning = onOpenQuickAdd,
        repository = repository,
        modifier = modifier,
    )
}
