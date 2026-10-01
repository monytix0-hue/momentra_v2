package com.example.momentra.ui.shell.business.shared

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.momentra.data.repository.BusinessSliceRepository

/** Fallback Pulse for an unrecognized Business type code. Delegates to the shared body. */
@Composable
fun BusinessPulseActiveContent(
    momentId: String?,
    momentTitle: String?,
    refreshToken: Long,
    momentTypeCode: String? = null,
    onAddExpense: () -> Unit = {},
    onOpenQuickAdd: () -> Unit = {},
    onToday: (BusinessQuickAddKind) -> Unit = { onOpenQuickAdd() },
    capabilities: List<String> = emptyList(),
    repository: BusinessSliceRepository = remember { BusinessSliceRepository() },
    modifier: Modifier = Modifier,
) {
    val family = businessPulseLoadFamily(momentTypeCode)
    BusinessPulseScreen(
        family = family,
        momentId = momentId,
        momentTitle = momentTitle,
        momentTypeCode = momentTypeCode,
        refreshToken = refreshToken,
        capabilities = capabilities,
        onToday = { kind ->
            if (kind == BusinessQuickAddKind.EXPENSE || kind == BusinessQuickAddKind.SPEND_ENTRY) {
                onAddExpense()
            }
            onToday(kind)
        },
        repository = repository,
        modifier = modifier,
    )
}
