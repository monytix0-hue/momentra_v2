package com.example.momentra.ui.shell.business.shared

import androidx.compose.runtime.Composable
import com.example.momentra.data.api.OperatingProjectionDto

/**
 * Shared chrome host for legacy industry Active kits (masters / story / capture overlays).
 * V4 operating hub posts through [OperatingMomentPage]; this stays as a no-op mount point
 * so restored Active kits compile without inventing new APIs.
 */
@Composable
fun OperatingChromeSheets(
    model: OperatingProjectionDto,
    companyId: String? = null,
) {
    // Intentionally empty — Active kits open their own sheets; OperatingMomentPage owns V4.
}
