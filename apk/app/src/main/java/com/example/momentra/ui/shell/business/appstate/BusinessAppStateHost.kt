package com.example.momentra.ui.shell.business.appstate

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.momentra.domain.BottomDestination
import com.example.momentra.domain.ShellContentState
import com.example.momentra.ui.create.MomentCreateViewModel
import com.example.momentra.ui.shell.business.create.BusinessGuidedCreateContent
import com.example.momentra.ui.shell.business.setup.BusinessSetupFamily
import com.example.momentra.ui.shell.business.setup.BusinessSetupShell
import com.example.momentra.ui.shell.components.MomentraFullscreenDialog
import com.example.momentra.ui.shell.empty.business.BusinessSetupBottomSheet

/**
 * Renders the correct Obsidian AppState body for the current Business shell slot.
 *
 * CREATE empty/setup opens guided business selection (`2:1514`) as a bottom sheet,
 * then the setup shell as a full-screen dialog. Keep in lockstep with iOS.
 */
@Composable
fun BusinessAppStateHost(
    destination: BottomDestination,
    content: ShellContentState,
    hasCompany: Boolean,
    momentTypeCode: String?,
    onRetry: () -> Unit,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
    onCreateBusiness: () -> Unit = onPrimary,
    onJoinCompany: () -> Unit = onSecondary,
    onOpenQuickAdds: () -> Unit = onSecondary,
    onBackToPulse: () -> Unit = {},
    onCreateBack: () -> Unit = onBackToPulse,
    companyId: String? = null,
    onMomentCreated: (String, String, String?, String) -> Unit = { _, _, _, _ -> },
    createViewModel: MomentCreateViewModel = viewModel(),
    modifier: Modifier = Modifier,
) {
    val accent = BusinessAppStateAccent.forTypeCode(momentTypeCode)
    val vertical = BusinessAppStateVertical.forTypeCode(momentTypeCode)
    val screen = BusinessAppStateRouting.resolveScreen(destination, content, hasCompany)
    val phase = BusinessAppStateRouting.phaseFor(content, hasCompany)
    val showGuidedCreate = destination == BottomDestination.CREATE &&
        (phase == BusinessAppStatePhase.Empty || phase == BusinessAppStatePhase.Setup)

    var setupFamily by remember { mutableStateOf<BusinessSetupFamily?>(null) }
    var setupTypeCode by remember { mutableStateOf<String?>(null) }
    val createState by createViewModel.state.collectAsState()

    when {
        phase == BusinessAppStatePhase.Loading -> {
            BusinessAppStateLoadingBody(
                label = BusinessAppStateRouting.loadingLabel(destination),
                accent = accent,
                modifier = modifier,
            )
        }
        showGuidedCreate -> {
            // Hub empty stays under the sheet so dismiss returns to CREATE chrome.
            BusinessAppStateHubBody(
                accent = accent,
                vertical = vertical,
                onPrimary = onPrimary,
                onSecondary = onOpenQuickAdds,
                onBackToPulse = onBackToPulse,
                modifier = modifier,
            )
            if (setupFamily == null) {
                BusinessSetupBottomSheet(onDismiss = onCreateBack) {
                    BusinessGuidedCreateContent(
                        onBack = onCreateBack,
                        onContinue = { option ->
                            createViewModel.clearError()
                            setupTypeCode = option.momentTypeCode
                            setupFamily = BusinessSetupFamily.forFamilyCode(option.familyCode)
                        },
                        submitting = createState.submitting,
                        error = createState.error,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            } else {
                val family = setupFamily!!
                MomentraFullscreenDialog(
                    onDismissRequest = {
                        setupFamily = null
                        setupTypeCode = null
                    },
                ) {
                    BusinessSetupShell(
                        family = family,
                        createViewModel = createViewModel,
                        onBack = {
                            setupFamily = null
                            setupTypeCode = null
                        },
                        onCreated = { outcome ->
                            setupFamily = null
                            setupTypeCode = null
                            onMomentCreated(
                                outcome.momentId,
                                outcome.title,
                                outcome.momentTypeCode,
                                outcome.status,
                            )
                        },
                        modifier = Modifier.fillMaxSize(),
                        initialTypeCode = setupTypeCode ?: family.defaultTypeCode,
                        companyId = companyId,
                    )
                }
            }
        }
        screen == BusinessAppStateScreen.Hub && phase == BusinessAppStatePhase.Empty -> {
            BusinessAppStateHubBody(
                accent = accent,
                vertical = vertical,
                onPrimary = onPrimary,
                onSecondary = onOpenQuickAdds,
                onBackToPulse = onBackToPulse,
                modifier = modifier,
            )
        }
        screen == BusinessAppStateScreen.NoCompany -> {
            val spec = BusinessAppStateSpecs.spec(screen, BusinessAppStatePhase.Setup)
            BusinessAppStateBody(
                spec = spec,
                accent = accent,
                onPrimary = onCreateBusiness,
                onSecondary = onJoinCompany,
                modifier = modifier,
            )
        }
        else -> {
            val spec = BusinessAppStateSpecs.spec(screen, phase)
            BusinessAppStateBody(
                spec = spec,
                accent = accent,
                onPrimary = if (phase == BusinessAppStatePhase.Error) onRetry else onPrimary,
                onSecondary = if (phase == BusinessAppStatePhase.Error) onRetry else onOpenQuickAdds,
                modifier = modifier,
            )
        }
    }
}
