package com.example.momentra.ui.shell.business.trading

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

// Domain palette "trading" in design/tokens/business.tokens.json (TRD-PULSE-v12 2566:29337).
internal val TradeBg = Color(0xFF090D16)
internal val TradeCard = Color(0xFF0F1623)
internal val TradeNotice = Color(0xFF0F1623)
internal val TradeField = Color(0xFF090D16)
internal val TradeBorder = Color(0xFF1E293B)
internal val TradeBrand = Color(0xFF10B981)
internal val TradeAccent = Color(0xFF10B981)
internal val TradeSecondary = Color(0xFF94A3B8)
internal val TradeInk = Color(0xFFFFFFFF)
internal val TradeSuccess = Color(0xFF6EE7B7)

@Composable
fun TradingActive(
    model: OperatingProjectionDto,
    tab: OperatingTab,
    onOpenQuickAdd: () -> Unit,
    onRecord: suspend (TradingEventDraft) -> Unit,
    onDecide: suspend (String, String) -> Unit,
    companyId: String? = null,
    modifier: Modifier = Modifier,
) {
    var sheet by remember { mutableStateOf<TradingSheetState?>(null) }
    val theme = remember { OperatingV12Theme.trading() }
    val openCapture: (String) -> Unit = { code ->
        OperatingChromeState.quickAddHint = code.lowercase()
        onOpenQuickAdd()
    }
    val openViewAll: (String) -> Unit = { code ->
        sheet = tradingSheetForPulseOrBoard(code)
    }

    Box(modifier.fillMaxSize().background(TradeBg)) {
        if (tab == OperatingTab.QUICK_ADD) {
            TradingQuickAddHub(model, onRecord, onDecide)
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
                    .background(TradeBg)
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
            TradingBoardSheet(
                state,
                model,
                TradeSheetTheme,
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

/** Pulse / board card surface — distinct from TRD-QA capture sheet fill. */
internal val TradeSheetSurface = Color(0xFF151D2E)
internal val TradeCyan = Color(0xFF06B6D4)
internal val TradeLabel = Color(0xFF8B97AD)
internal val TradeHint = Color(0xFF6B7790)
internal val TradeValue = Color(0xFFF1F5F9)
internal val TradeWhyBody = Color(0xFFCFEFF6)

/** Figma `TRD-SHEET` / TRD-QA tokens: slate sheet `#151D2E`, emerald accent, cyan WHY. */
internal val TradeSheetTheme = com.example.momentra.ui.shell.business.shared.OperatingSheetTheme(
    accent = TradeBrand,
    accentSoft = TradeSuccess,
    canvas = TradeBg,
    surface = TradeSheetSurface,
    inset = TradeBorder,
    border = TradeBorder,
    ink = TradeInk,
    body = TradeLabel,
    warning = com.example.momentra.ui.shell.business.shared.OperatingSheetWarning,
    negative = com.example.momentra.ui.shell.business.shared.OperatingSheetNegative,
)

internal fun tradingSheetForCode(code: String): TradingSheetState? = when (code) {
    "orders" -> TradingSheetState.OrdersCommitments
    "stock" -> TradingSheetState.StockProducts
    "network" -> TradingSheetState.PeopleNetwork
    "M4" -> TradingSheetState.OpenCommitments
    "L1" -> TradingSheetState.LifeCurrentBusiness
    "L2" -> TradingSheetState.LifeDealersChannels
    "L3" -> TradingSheetState.LifeMoneyMoving
    "L4" -> TradingSheetState.LifePeopleActivity
    "ME1" -> TradingSheetState.MonthlyMemory
    "ME2" -> TradingSheetState.MoneyMemory
    "fulfilment" -> TradingSheetState.FulfilmentMemory
    "ME4" -> TradingSheetState.PeoplePatterns
    else -> null
}

private fun tradingSheetForPulseOrBoard(code: String): TradingSheetState? = when (code) {
    "ATTENTION" -> TradingSheetState.NeedsAttention
    "TARGET_BUDGET" -> TradingSheetState.TargetBudget
    "PAYMENT_READINESS" -> TradingSheetState.PaymentReadiness
    "FINANCE" -> TradingSheetState.Finance
    "ACTIVITY" -> TradingSheetState.Activity
    "GST" -> TradingSheetState.Gst
    else -> tradingSheetForCode(code)
}
