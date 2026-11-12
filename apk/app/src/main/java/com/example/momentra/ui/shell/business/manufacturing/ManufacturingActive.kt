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
import androidx.compose.ui.unit.dp
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.shell.business.appstate.BusinessAppStateAccent
import com.example.momentra.ui.shell.business.appstate.BusinessAppStateBody
import com.example.momentra.ui.shell.business.appstate.BusinessAppStatePhase
import com.example.momentra.ui.shell.business.appstate.BusinessAppStateScreen
import com.example.momentra.ui.shell.business.appstate.BusinessAppStateSpecs
import com.example.momentra.ui.shell.business.shared.OperatingChromeState
import com.example.momentra.ui.shell.business.shared.OperatingTab
import com.example.momentra.ui.shell.business.shared.OperatingV12LifeScreen
import com.example.momentra.ui.shell.business.shared.OperatingV12MemoryScreen
import com.example.momentra.ui.shell.business.shared.OperatingV12MomentsScreen
import com.example.momentra.ui.shell.business.shared.OperatingV12PulseScreen
import com.example.momentra.ui.shell.business.shared.OperatingV12Theme
import com.example.momentra.ui.shell.business.shared.hasUsefulPulseChrome
import com.example.momentra.ui.theme.MadeToOrderAmber

// Domain palette "manufacturing" — shared ManufacturingAmber tokens (MFG-PULSE-v12).
internal val MfgBg get() = com.example.momentra.ui.theme.ManufacturingAmber.canvas
internal val MfgCard get() = com.example.momentra.ui.theme.ManufacturingAmber.surface
internal val MfgNotice get() = com.example.momentra.ui.theme.ManufacturingAmber.canvasTop
internal val MfgField get() = com.example.momentra.ui.theme.ManufacturingAmber.inset
internal val MfgBorder get() = com.example.momentra.ui.theme.ManufacturingAmber.canvasTop
internal val MfgBrand get() = com.example.momentra.ui.theme.ManufacturingAmber.accent
internal val MfgAccent get() = com.example.momentra.ui.theme.ManufacturingAmber.accent
internal val MfgSecondary get() = com.example.momentra.ui.theme.ManufacturingAmber.textBody
internal val MfgInk get() = com.example.momentra.ui.theme.ManufacturingAmber.textPrimary
internal val MfgSoft get() = com.example.momentra.ui.theme.ManufacturingAmber.accentSoft

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
    val isMto = model.subtype.equals("MADE_TO_ORDER", ignoreCase = true)
    // MTO Pulse/Moments/Life/Memory use soft-amber zinc — never solid MFG hero bleed.
    val canvasBg = if (isMto) MadeToOrderAmber.canvas else MfgBg
    val boardTheme = if (isMto) MtoBoardSheetTheme else MfgSheetTheme
    val openCapture: (String) -> Unit = { code ->
        OperatingChromeState.quickAddHint = code.lowercase()
        onOpenQuickAdd()
    }
    val openViewAll: (String) -> Unit = { code ->
        sheet = manufacturingSheetForPulseOrBoard(code)
    }

    Box(modifier.fillMaxSize().background(canvasBg)) {
        if (tab == OperatingTab.QUICK_ADD) {
            // MTO mounts MadeToOrderQuickAddHub; plant journey stays on ManufacturingQuickAddHub.
            if (isMto) {
                com.example.momentra.ui.shell.business.mto.MadeToOrderQuickAddHub(
                    model,
                    onRecord,
                    onDecide,
                )
            } else {
                ManufacturingQuickAddHub(model, onRecord, onDecide)
            }
        } else if (tab == OperatingTab.PULSE && !model.hasUsefulPulseChrome()) {
            // Keep empty Pulse outside verticalScroll — BusinessAppStateBody scrolls itself.
            // Only take over when setup has no budget/target/GST and feeds are quiet.
            BusinessAppStateBody(
                spec = BusinessAppStateSpecs.spec(
                    BusinessAppStateScreen.Pulse,
                    BusinessAppStatePhase.Empty,
                ),
                accent = BusinessAppStateAccent.Emerald,
                onPrimary = { openCapture("PRODUCTION") },
                onSecondary = onOpenQuickAdd,
            )
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .background(canvasBg)
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
                theme = boardTheme,
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

/** Soft-amber lead for MTO View-all / Pulse board sheets (vs solid MFG amber). */
internal val MtoBoardSheetTheme = com.example.momentra.ui.shell.business.shared.OperatingSheetTheme(
    accent = MadeToOrderAmber.accent,
    accentSoft = MadeToOrderAmber.accentSoft,
    canvas = MadeToOrderAmber.canvas,
    surface = MadeToOrderAmber.surface,
    inset = MadeToOrderAmber.inset,
    border = MadeToOrderAmber.canvasTop,
    ink = MadeToOrderAmber.textPrimary,
    body = MadeToOrderAmber.textBody,
    warning = MadeToOrderAmber.warning,
    negative = MadeToOrderAmber.negative,
)

internal fun manufacturingSheetForCode(code: String): ManufacturingSheetState? = when (code) {
    "orders" -> ManufacturingSheetState.OrdersCommitments
    "stock" -> ManufacturingSheetState.StockProducts
    "production" -> ManufacturingSheetState.Production
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
