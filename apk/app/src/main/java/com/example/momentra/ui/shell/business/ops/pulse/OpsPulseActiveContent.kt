package com.example.momentra.ui.shell.business.ops.pulse

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.momentra.data.repository.BusinessSliceRepository
import com.example.momentra.ui.shell.business.shared.BusinessMomentFamilyConfig
import com.example.momentra.ui.shell.business.shared.BusinessPulseScreen
import com.example.momentra.ui.shell.business.shared.BusinessQuickAddKind

/** Thin route into [BusinessPulseScreen]. */
@Suppress("UNUSED_PARAMETER")
@Composable
fun OpsPulseActiveContent(
    momentId: String?,
    momentTitle: String?,
    refreshToken: Long,
    onLogSpend: () -> Unit = {},
    onOpenQuickAdd: () -> Unit = {},
    onOpenMoments: () -> Unit = {},
    onViewAllActivity: () -> Unit = {},
    onToday: (BusinessQuickAddKind) -> Unit = { onOpenQuickAdd() },
    capabilities: List<String> = emptyList(),
    momentTypeCode: String? = "BUSINESS_OPERATIONS",
    repository: BusinessSliceRepository = remember { BusinessSliceRepository() },
    modifier: Modifier = Modifier,
) {
    BusinessPulseScreen(
        family = BusinessMomentFamilyConfig.Family.DAILY,
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
