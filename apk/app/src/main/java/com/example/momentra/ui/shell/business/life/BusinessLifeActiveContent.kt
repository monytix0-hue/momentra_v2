package com.example.momentra.ui.shell.business.life

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.momentra.data.repository.BusinessSliceRepository
import com.example.momentra.ui.shell.business.life.components.CompanyLifeFilter

/**
 * Business Life tab — delegates to company-unified Figma `695:9782` dashboard.
 * Family-specific Life frames remain payload-only / honesty until separately built.
 */
@Composable
fun BusinessLifeActiveContent(
    momentId: String?,
    momentTitle: String?,
    refreshToken: Long,
    momentTypeCode: String? = null,
    onViewReport: () -> Unit = {},
    onOpenFinance: () -> Unit = {},
    onOpenVendor: () -> Unit = {},
    repository: BusinessSliceRepository = remember { BusinessSliceRepository() },
    modifier: Modifier = Modifier,
) {
    val code = momentTypeCode.orEmpty().uppercase()
    val focus = when {
        code.contains("TEAM_OPERATIONS") -> CompanyLifeFilter.TEAM
        code.contains("RUNWAY") -> CompanyLifeFilter.RUNWAY
        code.contains("OPERATIONS") && !code.contains("TEAM") -> CompanyLifeFilter.OPS
        else -> null
    }
    CompanyLifeActiveContent(
        momentId = momentId,
        momentTitle = momentTitle,
        refreshToken = refreshToken,
        onViewReport = onViewReport,
        onOpenFinance = onOpenFinance,
        onOpenVendor = onOpenVendor,
        repository = repository,
        focusFilter = focus,
        modifier = modifier,
    )
}
