package com.example.momentra.ui.shell.business.life

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.momentra.data.repository.BusinessSliceRepository
import com.example.momentra.ui.shell.business.life.CompanyLifeActiveContent

/**
 * Business Life tab — delegates to company-unified Figma `695:9782` dashboard.
 * Family-specific Life frames remain payload-only / honesty until separately built.
 */
@Composable
fun BusinessLifeActiveContent(
    momentId: String?,
    momentTitle: String?,
    refreshToken: Long,
    @Suppress("UNUSED_PARAMETER") momentTypeCode: String? = null,
    onViewReport: () -> Unit = {},
    repository: BusinessSliceRepository = remember { BusinessSliceRepository() },
    modifier: Modifier = Modifier,
) {
    CompanyLifeActiveContent(
        momentId = momentId,
        momentTitle = momentTitle,
        refreshToken = refreshToken,
        onViewReport = onViewReport,
        repository = repository,
        modifier = modifier,
    )
}
