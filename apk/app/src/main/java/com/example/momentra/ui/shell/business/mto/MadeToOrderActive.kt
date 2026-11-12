package com.example.momentra.ui.shell.business.mto

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.shell.business.manufacturing.ManufacturingActive
import com.example.momentra.ui.shell.business.manufacturing.ManufacturingEventDraft
import com.example.momentra.ui.shell.business.shared.OperatingTab

/**
 * Made-to-Order active shell — Figma MTO + MFG tab family.
 *
 * Routes through [ManufacturingActive] with the MTO subtype theme
 * (`OperatingV12Theme.madeToOrder` / soft-amber zinc Pulse). Dedicated MTO
 * capture/hub chrome stays on Quick Add; Pulse/Moments/Life/Memory + View-all
 * use soft-amber board chrome (not solid MFG hero bleed).
 */
@Composable
fun MadeToOrderActive(
    model: OperatingProjectionDto,
    tab: OperatingTab,
    onOpenQuickAdd: () -> Unit,
    onRecord: suspend (ManufacturingEventDraft) -> Unit,
    onDecide: suspend (String, String) -> Unit,
    companyId: String? = null,
    modifier: Modifier = Modifier,
) {
    ManufacturingActive(
        model = model,
        tab = tab,
        onOpenQuickAdd = onOpenQuickAdd,
        onRecord = onRecord,
        onDecide = onDecide,
        companyId = companyId,
        modifier = modifier,
    )
}
