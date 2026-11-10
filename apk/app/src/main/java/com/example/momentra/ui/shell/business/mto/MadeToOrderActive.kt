package com.example.momentra.ui.shell.business.mto

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.shell.business.manufacturing.ManufacturingBoardSheet
import com.example.momentra.ui.shell.business.manufacturing.ManufacturingEventDraft
import com.example.momentra.ui.shell.business.manufacturing.ManufacturingSheetState
import com.example.momentra.ui.shell.business.manufacturing.MfgSheetTheme
import com.example.momentra.ui.shell.business.manufacturing.manufacturingSheetForPulseOrBoard
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

/** MTO zinc/amber canvas — same plant tokens as MFG, own hub kit. */
internal val MtoBg = Color(0xFF18181B)
internal val MtoBrand = Color(0xFFF59E0B)

/**
 * Made-to-Order active experience.
 *
 * Figma: `MTO-HUB` `2686:30690`, `MTO-MOMENTS` `2686:30122`, `MTO-LIFE` `2686:30403`,
 * Memory/job sheets under `2692:*` / `2694:*`. Not a Manufacturing relabel — hub tiles and
 * Moments/Life nouns are job/customer language.
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
    var sheet by remember { mutableStateOf<ManufacturingSheetState?>(null) }
    val theme = remember { OperatingV12Theme.madeToOrder() }
    val openCapture: (String) -> Unit = { code ->
        OperatingChromeState.quickAddHint = code.lowercase()
        onOpenQuickAdd()
    }
    val openViewAll: (String) -> Unit = { code ->
        sheet = manufacturingSheetForPulseOrBoard(code)
    }

    Box(modifier.fillMaxSize().background(MtoBg)) {
        if (tab == OperatingTab.QUICK_ADD) {
            MadeToOrderQuickAddHub(model, onRecord, onDecide)
        } else if (tab == OperatingTab.PULSE && !model.hasUsefulPulseChrome()) {
            // Keep empty Pulse outside verticalScroll — BusinessAppStateBody scrolls itself.
            // Only take over when setup has no budget/target/GST and feeds are quiet.
            BusinessAppStateBody(
                spec = BusinessAppStateSpecs.spec(
                    BusinessAppStateScreen.Pulse,
                    BusinessAppStatePhase.Empty,
                ),
                accent = BusinessAppStateAccent.Emerald,
                onPrimary = { openCapture("ORDER") },
                onSecondary = onOpenQuickAdd,
            )
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .background(MtoBg)
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
        androidx.compose.runtime.DisposableEffect(Unit) {
            MtoToastHost.onAction = { action ->
                when (action) {
                    MtoToastAction.OpenPulse -> Unit // already on operating surface
                    MtoToastAction.ConfirmOrder -> openCapture("CONFIRM")
                    MtoToastAction.GoBack -> Unit
                    MtoToastAction.OpenCustomers -> OperatingChromeState.mastersOpen = "Principals"
                    MtoToastAction.OpenProducts ->
                        OperatingChromeState.partsBrowserOpen = "parts"
                    MtoToastAction.OpenLines ->
                        OperatingChromeState.partsBrowserOpen = "materials"
                    MtoToastAction.Dismiss -> Unit
                }
            }
            onDispose { MtoToastHost.onAction = null }
        }
        MtoToastOverlay(Modifier.align(Alignment.TopCenter))
    }
}
