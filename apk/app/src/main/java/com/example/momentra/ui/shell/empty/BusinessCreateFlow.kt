package com.example.momentra.ui.shell.empty

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.momentra.domain.CreateMomentOutcome
import com.example.momentra.ui.create.MomentCreateViewModel
import com.example.momentra.ui.shell.empty.business.BusinessSetupBottomSheet

/** Business Create chooser + interactive setup wizard bottom sheets. */
@Composable
fun BusinessCreateFlow(
    companyId: String?,
    onCreateBack: () -> Unit,
    onMomentCreated: (String, String, String?) -> Unit,
    modifier: Modifier = Modifier,
    createViewModel: MomentCreateViewModel = viewModel(),
) {
    var openSetup by remember { mutableStateOf<BusinessSetupKind?>(null) }
    val createState by createViewModel.state.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        BusinessCreateMomentContent(
            onBack = onCreateBack,
            onSelectSetup = { kind -> openSetup = kind },
            modifier = Modifier.fillMaxSize(),
        )

        openSetup?.let { kind ->
            BusinessSetupBottomSheet(
                onDismiss = {
                    if (!createState.submitting) {
                        createViewModel.clearError()
                        openSetup = null
                    }
                },
            ) {
                BusinessSetupWizardContent(
                    kind = kind,
                    companyId = companyId.orEmpty(),
                    createViewModel = createViewModel,
                    submitting = createState.submitting,
                    error = createState.error,
                    onClose = {
                        if (!createState.submitting) {
                            createViewModel.clearError()
                            openSetup = null
                        }
                    },
                    onCreated = { outcome: CreateMomentOutcome ->
                        openSetup = null
                        onMomentCreated(outcome.momentId, outcome.title, outcome.momentTypeCode)
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
