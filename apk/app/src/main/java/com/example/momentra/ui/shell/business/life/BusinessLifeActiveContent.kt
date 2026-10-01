package com.example.momentra.ui.shell.business.life

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.momentra.data.repository.BusinessSliceRepository

/** Business Life tab. One company body. The selected moment only sets the lens. */
@Composable
fun BusinessLifeActiveContent(
    momentId: String?,
    momentTitle: String?,
    refreshToken: Long,
    momentTypeCode: String? = null,
    companyId: String? = null,
    onViewReport: () -> Unit = {},
    onOpenFinance: () -> Unit = {},
    onOpenVendor: () -> Unit = {},
    repository: BusinessSliceRepository = remember { BusinessSliceRepository() },
    modifier: Modifier = Modifier,
) {
    CompanyLifeActiveContent(
        momentId = momentId,
        momentTitle = momentTitle,
        refreshToken = refreshToken,
        onViewReport = onViewReport,
        onOpenFinance = onOpenFinance,
        onOpenVendor = onOpenVendor,
        repository = repository,
        companyId = companyId,
        momentTypeCode = momentTypeCode,
        modifier = modifier,
    )
}
