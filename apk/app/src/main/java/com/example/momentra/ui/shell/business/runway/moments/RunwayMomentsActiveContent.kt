package com.example.momentra.ui.shell.business.runway.moments

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.momentra.data.repository.BusinessSliceRepository
import com.example.momentra.ui.shell.business.shared.BusinessMomentFamilyConfig
import com.example.momentra.ui.shell.business.shared.BusinessMomentsScreen

/** Money Moments. The shared screen owns the activity timeline. */
@Composable
fun RunwayMomentsActiveContent(
    momentId: String?,
    momentTitle: String?,
    refreshToken: Long = 0L,
    onLogExpense: () -> Unit = {},
    onOpenQuickAdd: () -> Unit = {},
    onOpenMoments: () -> Unit = {},
    onViewAllActivity: () -> Unit = {},
    repository: BusinessSliceRepository = androidx.compose.runtime.remember { BusinessSliceRepository() },
    modifier: Modifier = Modifier,
) {
    BusinessMomentsScreen(
        family = BusinessMomentFamilyConfig.Family.MONEY,
        momentId = momentId,
        momentTitle = momentTitle,
        refreshToken = refreshToken,
        onOpenQuickAdd = onOpenQuickAdd,
        repository = repository,
        modifier = modifier,
    )
}
