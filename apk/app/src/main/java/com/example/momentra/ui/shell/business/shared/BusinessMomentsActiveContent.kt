package com.example.momentra.ui.shell.business.shared

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.momentra.data.repository.BusinessSliceRepository

/** Fallback Business Moments when the type code is not Money, Daily, or Team. */
@Composable
fun BusinessMomentsActiveContent(
    momentId: String?,
    momentTitle: String?,
    momentTypeCode: String? = null,
    refreshToken: Long = 0L,
    onOpenQuickAdd: () -> Unit = {},
    repository: BusinessSliceRepository = androidx.compose.runtime.remember { BusinessSliceRepository() },
    modifier: Modifier = Modifier,
) {
    val family = BusinessMomentFamilyConfig.familyOrNull(momentTypeCode)
        ?: BusinessMomentFamilyConfig.Family.TEAM
    BusinessMomentsScreen(
        family = family,
        momentId = momentId,
        momentTitle = momentTitle,
        refreshToken = refreshToken,
        onOpenQuickAdd = onOpenQuickAdd,
        repository = repository,
        modifier = modifier,
    )
}
