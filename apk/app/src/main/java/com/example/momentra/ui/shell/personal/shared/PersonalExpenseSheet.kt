package com.example.momentra.ui.shell.personal.shared

import androidx.compose.runtime.Composable
import com.example.momentra.data.repository.PersonalSliceRepository

/** Thin wrapper — delegates to [PersonalMasterExpenseSheet] (Figma `453:9376`). */
@Composable
fun PersonalExpenseSheet(
    momentId: String,
    visible: Boolean,
    pulseFamily: PersonalPulseFamily = PersonalPulseFamily.LIFE_OPERATIONS,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    repository: PersonalSliceRepository = PersonalSliceRepository(),
) {
    PersonalMasterExpenseSheet(
        momentId = momentId,
        visible = visible,
        pulseFamily = pulseFamily,
        onDismiss = onDismiss,
        onSaved = onSaved,
        repository = repository,
    )
}
