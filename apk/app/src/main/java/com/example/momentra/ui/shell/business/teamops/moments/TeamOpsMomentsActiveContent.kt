package com.example.momentra.ui.shell.business.teamops.moments

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.momentra.data.repository.BusinessSliceRepository
import com.example.momentra.ui.shell.business.shared.BusinessMomentFamilyConfig
import com.example.momentra.ui.shell.business.shared.BusinessMomentsScreen

/** Team Moments. The shared screen owns the activity timeline. */
@Composable
fun TeamOpsMomentsActiveContent(
    momentId: String?,
    momentTitle: String?,
    refreshToken: Long = 0L,
    onLogWin: () -> Unit = {},
    onOpenQuickAdd: () -> Unit = {},
    onViewAllActivity: () -> Unit = {},
    repository: BusinessSliceRepository = androidx.compose.runtime.remember { BusinessSliceRepository() },
    modifier: Modifier = Modifier,
) {
    BusinessMomentsScreen(
        family = BusinessMomentFamilyConfig.Family.TEAM,
        momentId = momentId,
        momentTitle = momentTitle,
        refreshToken = refreshToken,
        onOpenQuickAdd = onOpenQuickAdd,
        repository = repository,
        modifier = modifier,
    )
}
