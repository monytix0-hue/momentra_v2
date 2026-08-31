package com.example.momentra.ui.shell.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.momentra.domain.AppContext
import com.example.momentra.ui.create.MomentCreateViewModel
import com.example.momentra.ui.shell.business.BusinessActiveTheme
import com.example.momentra.ui.shell.empty.BusinessSetupKind
import com.example.momentra.ui.shell.empty.BusinessSetupWizardContent
import com.example.momentra.ui.shell.empty.group.GroupExperienceSetupContent
import com.example.momentra.ui.shell.empty.group.GroupLivingSetupContent
import com.example.momentra.ui.shell.empty.group.GroupPurchaseSetupContent
import com.example.momentra.ui.shell.empty.personal.PersonalSetupSystem
import com.example.momentra.ui.shell.empty.personal.PersonalSetupWizardContent
import com.example.momentra.ui.shell.group.groupExperienceFamilyFor
import com.example.momentra.ui.shell.group.isThemedLiving
import com.example.momentra.ui.shell.group.isThemedPurchase
import com.example.momentra.ui.shell.personal.PersonalPulseFamily
import com.example.momentra.ui.shell.personal.personalPulseFamilyFor

@Composable
fun EditMomentSetupHost(
    context: AppContext,
    momentId: String,
    momentTitle: String,
    momentTypeCode: String?,
    companyId: String?,
    onClose: () -> Unit,
    onSaved: () -> Unit,
    createViewModel: MomentCreateViewModel = viewModel(),
) {
    val createState by createViewModel.state.collectAsState()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.94f)
            .background(Color(0xFF0C0F15)),
    ) {
        when (context) {
            AppContext.PERSONAL -> {
                val system = when (personalPulseFamilyFor(momentTypeCode)) {
                    PersonalPulseFamily.LIFE_OPERATIONS -> PersonalSetupSystem.LIFE_OPERATIONS
                    PersonalPulseFamily.FUTURE_BUILDING -> PersonalSetupSystem.FUTURE_BUILDING
                    PersonalPulseFamily.LIFESTYLE -> PersonalSetupSystem.LIFESTYLE
                    PersonalPulseFamily.RELATIONSHIPS -> PersonalSetupSystem.RELATIONSHIPS
                }
                PersonalSetupWizardContent(
                    system = system,
                    onBack = onClose,
                    onCreated = { _, _, _ -> onSaved() },
                    createViewModel = createViewModel,
                    editingMomentId = momentId,
                    initialTitle = momentTitle,
                )
            }
            AppContext.BUSINESS -> {
                val theme = BusinessActiveTheme.forTypeCode(momentTypeCode)
                val kind = when {
                    theme.typeLabel == BusinessActiveTheme.BusinessRunway.typeLabel ->
                        BusinessSetupKind.BUSINESS_RUNWAY
                    theme.typeLabel == BusinessActiveTheme.BusinessOperations.typeLabel ->
                        BusinessSetupKind.BUSINESS_OPERATIONS
                    else -> BusinessSetupKind.TEAM_OPERATIONS
                }
                if (companyId.isNullOrBlank()) {
                    Text(
                        "Company required to edit business setup",
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else {
                    BusinessSetupWizardContent(
                        kind = kind,
                        companyId = companyId,
                        createViewModel = createViewModel,
                        submitting = createState.submitting,
                        error = createState.error,
                        onClose = onClose,
                        onCreated = { onSaved() },
                        editingMomentId = momentId,
                        initialTitle = momentTitle,
                    )
                }
            }
            AppContext.GROUP -> {
                val family = groupExperienceFamilyFor(momentTypeCode)
                when {
                    family.isThemedPurchase() -> GroupPurchaseSetupContent(
                        onBack = onClose,
                        onCreated = { onSaved() },
                        createViewModel = createViewModel,
                        submitting = createState.submitting,
                        error = createState.error,
                        editingMomentId = momentId,
                        initialTitle = momentTitle,
                        initialTypeCode = momentTypeCode,
                    )
                    family.isThemedLiving() -> GroupLivingSetupContent(
                        onBack = onClose,
                        onCreated = { onSaved() },
                        createViewModel = createViewModel,
                        submitting = createState.submitting,
                        error = createState.error,
                        editingMomentId = momentId,
                        initialTitle = momentTitle,
                        initialTypeCode = momentTypeCode,
                    )
                    else -> GroupExperienceSetupContent(
                        onBack = onClose,
                        onCreated = { onSaved() },
                        createViewModel = createViewModel,
                        submitting = createState.submitting,
                        error = createState.error,
                        editingMomentId = momentId,
                        initialTitle = momentTitle,
                        initialTypeCode = momentTypeCode,
                    )
                }
            }
            AppContext.CIRCLE -> onClose()
        }
    }
}
