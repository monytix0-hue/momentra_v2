package com.example.momentra.ui.shell.business.teamops.pulse

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.momentra.data.repository.BusinessSliceRepository
import com.example.momentra.ui.shell.business.shared.BusinessMomentFamilyConfig
import com.example.momentra.ui.shell.business.shared.BusinessPulseScreen
import com.example.momentra.ui.shell.business.shared.BusinessQuickAddKind

/** Thin route into [BusinessPulseScreen]. Does not read the finance score. */
@Suppress("UNUSED_PARAMETER")
@Composable
fun TeamOpsPulseActiveContent(
    momentId: String?,
    momentTitle: String?,
    refreshToken: Long,
    onLogDelivery: () -> Unit = {},
    onOpenQuickAdd: () -> Unit = {},
    onViewAllActivity: () -> Unit = {},
    onAddExpense: () -> Unit = {},
    onOpenMoments: () -> Unit = onViewAllActivity,
    onToday: (BusinessQuickAddKind) -> Unit = { onOpenQuickAdd() },
    capabilities: List<String> = emptyList(),
    momentTypeCode: String? = "TEAM_OPERATIONS",
    repository: BusinessSliceRepository = remember { BusinessSliceRepository() },
    modifier: Modifier = Modifier,
) {
    BusinessPulseScreen(
        family = BusinessMomentFamilyConfig.Family.TEAM,
        momentId = momentId,
        momentTitle = momentTitle,
        momentTypeCode = momentTypeCode,
        refreshToken = refreshToken,
        capabilities = capabilities,
        onToday = onToday,
        onOpenMoments = onOpenMoments,
        repository = repository,
        modifier = modifier,
    )
}
