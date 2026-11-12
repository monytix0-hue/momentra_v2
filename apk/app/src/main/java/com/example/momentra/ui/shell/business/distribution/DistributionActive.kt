package com.example.momentra.ui.shell.business.distribution

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

// Domain palette "distribution" in design/tokens/business.tokens.json (DST-PULSE-v12 2365:29452).
internal val DistBg = Color(0xFF0F172A) // domain.distribution.canvas
internal val DistCard = Color(0xFF0B1433) // domain.distribution.surface
internal val DistNotice = Color(0xFF0B1226) // domain.distribution.canvasTop
internal val DistField = Color(0xFF071022) // domain.distribution.inset
internal val DistBorder = Color(0xFF1E293B) // color.border.default
internal val DistBrand = Color(0xFFEA580C) // domain.distribution.accent
internal val DistAccent = Color(0xFFEA580C) // domain.distribution.accent
internal val DistSecondary = Color(0xFF94A3B8) // color.text.muted
internal val DistInk = Color(0xFFFFFFFF) // color.text.primary
internal val DistSuccess = Color(0xFFFFB598) // domain.distribution.accentSoft

/** DST-QA / DST-MASTER / DST-PICKER sheet surface (Figma `#13203A`). */
internal val DistQaSheet = Color(0xFF13203A)
/** DST field / elevated chip fill (`#24365A`). */
internal val DistQaField = Color(0xFF24365A)
/** DST field hairline (`#2A3D63`). */
internal val DistQaBorder = Color(0xFF2A3D63)

@Composable
fun DistributionActive(
    model: OperatingProjectionDto,
    tab: OperatingTab,
    onOpenQuickAdd: () -> Unit,
    onRecord: suspend (DistributionEventDraft) -> Unit,
    onDecide: suspend (String, String) -> Unit,
    companyId: String? = null,
    modifier: Modifier = Modifier,
) {
    // One sheet slot for all eighteen view-all surfaces; `null` means no sheet is open.
    var sheet by remember { mutableStateOf<DistributionSheetState?>(null) }
    val theme = remember { OperatingV12Theme.distribution() }
    val openCapture: (String) -> Unit = { code ->
        OperatingChromeState.quickAddHint = code.lowercase()
        onOpenQuickAdd()
    }
    val openViewAll: (String) -> Unit = { code ->
        sheet = distributionSheetForPulseOrBoard(code)
    }

    Box(modifier.fillMaxSize().background(DistBg)) {
        if (tab == OperatingTab.QUICK_ADD) {
            DistributionQuickAddHub(model, onRecord, onDecide)
        } else if (tab == OperatingTab.PULSE && !model.hasUsefulPulseChrome()) {
            // Keep empty Pulse outside verticalScroll — BusinessAppStateBody scrolls itself.
            // Only take over when setup has no budget/target/GST and feeds are quiet.
            BusinessAppStateBody(
                spec = BusinessAppStateSpecs.spec(
                    BusinessAppStateScreen.Pulse,
                    BusinessAppStatePhase.Empty,
                ),
                accent = BusinessAppStateAccent.Emerald,
                onPrimary = { openCapture("SALE") },
                onSecondary = onOpenQuickAdd,
            )
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .background(DistBg)
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
            DistributionBoardSheet(
                state,
                model,
                DistSheetTheme,
                onDismiss = { sheet = null },
                onFooterAction = { action ->
                    when (action) {
                        "PAYMENT" -> OperatingChromeState.quickAddHint = "payment"
                        "MEMORY" -> OperatingChromeState.storyConfigOpen = true
                        "ATTENTION" -> OperatingChromeState.quickAddHint = "issue"
                        else -> OperatingChromeState.quickAddHint = "sale"
                    }
                    if (action != "MEMORY") onOpenQuickAdd()
                    sheet = null
                },
            )
        }
        com.example.momentra.ui.shell.business.shared.OperatingChromeSheets(model, companyId)
    }
}

/** Figma `DST-SHEET` / `DST-MASTER` tokens: orange accent on QA sheet surface `#13203A`. */
internal val DistSheetTheme = com.example.momentra.ui.shell.business.shared.OperatingSheetTheme(
    accent = DistBrand,
    accentSoft = DistSuccess,
    canvas = DistQaSheet,
    surface = DistQaSheet,
    inset = DistQaField,
    border = DistQaBorder,
    ink = DistInk,
    body = DistSecondary,
    warning = com.example.momentra.ui.shell.business.shared.OperatingSheetWarning,
    negative = com.example.momentra.ui.shell.business.shared.OperatingSheetNegative,
)

/** Board section code → the view-all surface it opens, or `null` when there is none. */
internal fun distributionSheetForCode(code: String): DistributionSheetState? = when (code) {
    "orders" -> DistributionSheetState.OrdersCommitments
    "stock" -> DistributionSheetState.StockProducts
    "network" -> DistributionSheetState.PeopleNetwork
    "M4" -> DistributionSheetState.OpenCommitments
    "L1" -> DistributionSheetState.LifeCurrentBusiness
    "L2" -> DistributionSheetState.LifeDealersChannels
    "L3" -> DistributionSheetState.LifeMoneyMoving
    "L4" -> DistributionSheetState.LifePeopleActivity
    "ME1" -> DistributionSheetState.MonthlyMemory
    "ME2" -> DistributionSheetState.MoneyMemory
    "fulfilment" -> DistributionSheetState.FulfilmentMemory
    "ME4" -> DistributionSheetState.PeoplePatterns
    else -> null
}

private fun distributionSheetForPulseOrBoard(code: String): DistributionSheetState? = when (code) {
    "ATTENTION" -> DistributionSheetState.NeedsAttention
    "TARGET_BUDGET" -> DistributionSheetState.TargetBudget
    "PAYMENT_READINESS" -> DistributionSheetState.PaymentReadiness
    "FINANCE" -> DistributionSheetState.Finance
    "ACTIVITY" -> DistributionSheetState.Activity
    "GST" -> DistributionSheetState.Gst
    else -> distributionSheetForCode(code)
}
