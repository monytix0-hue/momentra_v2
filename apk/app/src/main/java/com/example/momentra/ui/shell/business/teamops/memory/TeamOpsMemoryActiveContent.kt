package com.example.momentra.ui.shell.business.teamops.memory

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.momentra.data.repository.BusinessSliceRepository
import com.example.momentra.ui.shell.business.shared.BusinessMemoryScreen

/** Team opens the company Memory surface with the Team lens. */
@Composable
fun TeamOpsMemoryActiveContent(
    momentId: String?,
    momentTitle: String?,
    refreshToken: Long,
    companyId: String? = null,
    onRecordLearning: () -> Unit = {},
    onOpenQuickAdd: () -> Unit = {},
    repository: BusinessSliceRepository = remember { BusinessSliceRepository() },
    modifier: Modifier = Modifier,
) {
    BusinessMemoryScreen(
        momentId = momentId,
        refreshToken = refreshToken,
        momentTypeCode = "TEAM_OPERATIONS",
        companyId = companyId,
        onRecordLearning = onRecordLearning,
        repository = repository,
        modifier = modifier,
    )
}
