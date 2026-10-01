package com.example.momentra.ui.shell.business.ops.memory

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.momentra.data.repository.BusinessSliceRepository
import com.example.momentra.ui.shell.business.shared.BusinessMemoryScreen

/** Daily opens the company Memory surface with the Daily lens. */
@Composable
fun OpsMemoryActiveContent(
    momentId: String?,
    momentTitle: String?,
    refreshToken: Long,
    companyId: String? = null,
    onRecordMemory: () -> Unit = {},
    repository: BusinessSliceRepository = remember { BusinessSliceRepository() },
    modifier: Modifier = Modifier,
) {
    BusinessMemoryScreen(
        momentId = momentId,
        refreshToken = refreshToken,
        momentTypeCode = "BUSINESS_OPERATIONS",
        companyId = companyId,
        onRecordLearning = onRecordMemory,
        repository = repository,
        modifier = modifier,
    )
}
