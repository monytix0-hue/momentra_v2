package com.example.momentra.ui.shell.business.manufacturing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.shell.business.shared.OperatingChromeState
import com.example.momentra.ui.shell.business.shared.OperatingTab
import com.example.momentra.ui.shell.business.shared.OperatingV12LifeScreen
import com.example.momentra.ui.shell.business.shared.OperatingV12MemoryScreen
import com.example.momentra.ui.shell.business.shared.OperatingV12MomentsScreen
import com.example.momentra.ui.shell.business.shared.OperatingV12PulseScreen
import com.example.momentra.ui.shell.business.shared.OperatingV12Theme

// Domain palette "manufacturing" in design/tokens/business.tokens.json (MFG-PULSE-v12).
internal val MfgBg = Color(0xFF18181B)
internal val MfgCard = Color(0xFF27272A)
internal val MfgNotice = Color(0xFF3F3F46)
internal val MfgField = Color(0xFF18181B)
internal val MfgBorder = Color(0xFF3F3F46)
internal val MfgBrand = Color(0xFFF59E0B)
internal val MfgAccent = Color(0xFFF59E0B)
internal val MfgSecondary = Color(0xFFA1A1AA)
internal val MfgInk = Color(0xFFFFFFFF)
internal val MfgSoft = Color(0xFFFBBF24)

@Composable
fun ManufacturingActive(
    model: OperatingProjectionDto,
    tab: OperatingTab,
    onOpenQuickAdd: () -> Unit,
    onRecord: suspend (ManufacturingEventDraft) -> Unit,
    onDecide: suspend (String, String) -> Unit,
    companyId: String? = null,
    modifier: Modifier = Modifier,
) {
    var sheet by remember { mutableStateOf<ManufacturingSheetState?>(null) }
    val theme = remember(model.subtype) { OperatingV12Theme.forSubtype(model.subtype) }
    val openCapture: (String) -> Unit = { code ->
        OperatingChromeState.quickAddHint = code.lowercase()
        onOpenQuickAdd()
    }
    val openViewAll: (String) -> Unit = { code ->
        sheet = manufacturingSheetForPulseOrBoard(code)
    }

    Box(modifier.fillMaxSize().background(MfgBg)) {
        if (tab == OperatingTab.QUICK_ADD) {
            ManufacturingQuickAddHub(model, onRecord, onDecide)
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .background(MfgBg)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                when (tab) {
                    OperatingTab.PULSE -> OperatingV12PulseScreen(
                        model = model,
                        theme = theme,
                        onCapture = openCapture,
                        onViewAll = openViewAll,
                        onOpenRow = { OperatingChromeState.editing = it },
                    )
                    OperatingTab.MOMENTS -> OperatingV12MomentsScreen(
                        model = model,
                        theme = theme,
                        onCapture = openCapture,
                        onViewAll = openViewAll,
                        onOpenRow = { OperatingChromeState.editing = it },
                    )
                    OperatingTab.LIFE -> OperatingV12LifeScreen(
                        model = model,
                        theme = theme,
                        onCapture = openCapture,
                        onViewAll = openViewAll,
                        onOpenRow = { OperatingChromeState.editing = it },
                    )
                    OperatingTab.MEMORY -> OperatingV12MemoryScreen(
                        model = model,
                        theme = theme,
                        onCapture = openCapture,
                        onStory = { OperatingChromeState.storyConfigOpen = true },
                        onViewAll = openViewAll,
                        onOpenRow = { OperatingChromeState.editing = it },
                    )
                    OperatingTab.QUICK_ADD -> Unit
                }
            }
        }
        sheet?.let { state ->
            ManufacturingBoardSheet(
                state = state,
                model = model,
                theme = MfgSheetTheme,
                onDismiss = { sheet = null },
            )
        }
        com.example.momentra.ui.shell.business.shared.OperatingChromeSheets(model, companyId)
    }
}

internal val MfgSheetTheme = com.example.momentra.ui.shell.business.shared.OperatingSheetTheme(
    accent = MfgBrand,
    accentSoft = MfgSoft,
    canvas = MfgBg,
    surface = MfgCard,
    inset = MfgField,
    border = MfgBorder,
    ink = MfgInk,
    body = MfgSecondary,
    warning = com.example.momentra.ui.shell.business.shared.OperatingSheetWarning,
    negative = com.example.momentra.ui.shell.business.shared.OperatingSheetNegative,
)

internal fun manufacturingSheetForCode(code: String): ManufacturingSheetState? = when (code) {
    "orders" -> ManufacturingSheetState.OrdersCommitments
    "stock" -> ManufacturingSheetState.StockProducts
    "network" -> ManufacturingSheetState.PeopleNetwork
    "M4" -> ManufacturingSheetState.OpenCommitments
    "L1" -> ManufacturingSheetState.LifeCurrentBusiness
    "L2" -> ManufacturingSheetState.LifeOrdersBuyers
    "L3" -> ManufacturingSheetState.LifeMoneyMoving
    "L4" -> ManufacturingSheetState.LifePeopleActivity
    "ME1" -> ManufacturingSheetState.MonthlyMemory
    "ME2" -> ManufacturingSheetState.MoneyMemory
    "fulfilment" -> ManufacturingSheetState.FulfilmentMemory
    "ME4" -> ManufacturingSheetState.PeoplePatterns
    else -> null
}

internal fun manufacturingSheetForPulseOrBoard(code: String): ManufacturingSheetState? = when (code) {
    "ATTENTION" -> ManufacturingSheetState.NeedsAttention
    "TARGET_BUDGET" -> ManufacturingSheetState.TargetBudget
    "PAYMENT_READINESS" -> ManufacturingSheetState.PaymentReadiness
    "FINANCE" -> ManufacturingSheetState.Finance
    "ACTIVITY" -> ManufacturingSheetState.Activity
    "GST" -> ManufacturingSheetState.Gst
    else -> manufacturingSheetForCode(code)
}
