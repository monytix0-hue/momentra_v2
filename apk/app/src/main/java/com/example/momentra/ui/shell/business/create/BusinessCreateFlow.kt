package com.example.momentra.ui.shell.business.create

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.momentra.ui.create.MomentCreateViewModel
import com.example.momentra.ui.shell.business.setup.BusinessSetupFamily
import com.example.momentra.ui.shell.business.setup.BusinessSetupShell

/**
 * Top-bar "+" → Figma `2:1514` guided selection → in-document setup shell.
 *
 * Keep in lockstep with `BusinessCreateFlowView` on iOS.
 */
@Composable
fun BusinessCreateFlow(
    onBack: () -> Unit,
    onCreated: (momentId: String, title: String, momentTypeCode: String?, status: String) -> Unit,
    companyId: String? = null,
    createViewModel: MomentCreateViewModel = viewModel(),
    modifier: Modifier = Modifier,
) {
    var setupFamily by remember { mutableStateOf<BusinessSetupFamily?>(null) }
    var setupTypeCode by remember { mutableStateOf<String?>(null) }
    val createState by createViewModel.state.collectAsState()

    val family = setupFamily
    if (family == null) {
        BusinessGuidedCreateContent(
            onBack = onBack,
            onContinue = { option ->
                createViewModel.clearError()
                setupTypeCode = option.momentTypeCode
                setupFamily = BusinessSetupFamily.forFamilyCode(option.familyCode)
            },
            submitting = createState.submitting,
            error = createState.error,
            modifier = modifier
                .fillMaxSize()
                .statusBarsPadding(),
        )
    } else {
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
                onCreated(
                    outcome.momentId,
                    outcome.title,
                    outcome.momentTypeCode,
                    outcome.status,
                )
            },
            modifier = modifier.fillMaxSize(),
            initialTypeCode = setupTypeCode ?: family.defaultTypeCode,
            companyId = companyId,
        )
    }
}
