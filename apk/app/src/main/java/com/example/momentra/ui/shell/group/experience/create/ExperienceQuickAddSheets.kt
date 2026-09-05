package com.example.momentra.ui.shell.group.experience.create

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R
import com.example.momentra.data.repository.GroupSliceRepository
import com.example.momentra.ui.shell.group.shared.GroupExpenseSheet
import com.example.momentra.ui.shell.group.shared.TripSheetTokens
import com.example.momentra.ui.shell.group.wedding.create.ChipRow
import com.example.momentra.ui.shell.group.wedding.create.FieldLabel
import com.example.momentra.ui.shell.group.wedding.create.PrimaryCta
import com.example.momentra.ui.shell.group.wedding.create.SheetAccent
import com.example.momentra.ui.shell.group.wedding.create.SheetField
import com.example.momentra.ui.shell.group.wedding.create.SheetHeader
import com.example.momentra.ui.shell.group.wedding.create.WeddingAttendanceSheetBody
import com.example.momentra.ui.shell.group.wedding.create.WeddingBudgetSheetBody
import com.example.momentra.ui.shell.group.wedding.create.WeddingContributionSheetBody
import com.example.momentra.ui.shell.group.wedding.create.WeddingMemorySheetBody
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.launch
import com.example.momentra.ui.shell.group.wedding.create.WeddingPlanningSheetBody
import com.example.momentra.ui.shell.group.wedding.create.WeddingPollSheetBody
import com.example.momentra.ui.shell.group.wedding.create.WeddingSettleSheetBody
import com.example.momentra.ui.shell.group.wedding.create.WeddingUpdateSheetBody
import com.example.momentra.ui.shell.group.wedding.create.WeddingVendorSheetBody

private fun ExperienceActiveTheme.toSheetAccent(): SheetAccent =
    SheetAccent(accent = TripSheetTokens.Accent, accentEnd = TripSheetTokens.AccentEnd, soft = accentSoft)

/** Figma 575:15497 — moment-colored Experience Quick Add sheets. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExperienceGapQuickAddSheet(
    theme: ExperienceActiveTheme,
    kind: ExperienceQuickAddKind,
    visible: Boolean,
    momentId: String?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit = {},
    onBooking: () -> Unit = {},
    momentTypeCode: String? = null,
    repository: GroupSliceRepository = remember { GroupSliceRepository() },
) {
    if (!visible) return
    if (kind == ExperienceQuickAddKind.BOOKING) {
        LaunchedEffect(kind) {
            onBooking()
            onDismiss()
        }
        return
    }
    if (kind == ExperienceQuickAddKind.EXPENSE && !momentId.isNullOrBlank()) {
        GroupExpenseSheet(
            momentId = momentId,
            visible = true,
            onDismiss = onDismiss,
            onSaved = {
                onSaved()
                onDismiss()
            },
            momentTypeCode = momentTypeCode,
            repository = repository,
        )
        return
    }

    val accent = theme.toSheetAccent()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = TripSheetTokens.Bg,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 40.dp, height = 5.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(TripSheetTokens.Border),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (kind) {
                ExperienceQuickAddKind.EXPENSE -> Unit
                ExperienceQuickAddKind.CONTRIBUTION ->
                    WeddingContributionSheetBody(momentId, repository, onDismiss, onSaved, accent)
                ExperienceQuickAddKind.BUDGET ->
                    WeddingBudgetSheetBody(momentId, repository, onDismiss, onSaved, accent)
                ExperienceQuickAddKind.PARTICIPANT ->
                    ExperienceParticipantSheetBody(momentId, repository, onDismiss, onSaved, theme, accent)
                ExperienceQuickAddKind.VENDOR ->
                    WeddingVendorSheetBody(momentId, repository, onDismiss, onSaved, accent)
                ExperienceQuickAddKind.PLANNING ->
                    WeddingPlanningSheetBody(
                        momentId,
                        repository,
                        onDismiss,
                        onSaved,
                        accent,
                        momentTypeCode = momentTypeCode,
                    )
                ExperienceQuickAddKind.ATTENDANCE ->
                    WeddingAttendanceSheetBody(momentId, repository, onDismiss, onSaved, accent)
                ExperienceQuickAddKind.POLL ->
                    WeddingPollSheetBody(momentId, repository, onDismiss, onSaved, accent)
                ExperienceQuickAddKind.MEMORY ->
                    WeddingMemorySheetBody(momentId, repository, onDismiss, onSaved, accent)
                ExperienceQuickAddKind.UPDATE ->
                    WeddingUpdateSheetBody(momentId, repository, onDismiss, onSaved, accent)
                ExperienceQuickAddKind.SETTLE ->
                    WeddingSettleSheetBody(momentId, onDismiss, onSaved)
                ExperienceQuickAddKind.BOOKING -> Unit
            }
        }
    }
}

@Composable
private fun ExperienceParticipantSheetBody(
    momentId: String?,
    repository: GroupSliceRepository,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    theme: ExperienceActiveTheme,
    accent: SheetAccent,
) {
    var name by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(theme.participantRoles.first()) }
    var rsvp by remember { mutableStateOf("Pending") }
    var plusOne by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    SheetHeader(
        R.drawable.ic_qa_users,
        "Add Participant",
        theme.participantSubtitle,
        accent = accent,
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Participant Name")
        SheetField(name, { name = it }, "Full name", minHeight = 42)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Email or Phone")
        SheetField(contact, { contact = it }, "Email or phone", minHeight = 42)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Role")
        ChipRow(theme.participantRoles, role, accent) { role = it }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("RSVP Status")
        ChipRow(listOf("Confirmed", "Pending", "Declined"), rsvp, accent) { rsvp = it }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Dietary Preferences / Notes")
        SheetField(notes, { notes = it }, "Optional notes", minHeight = 42)
    }
    error?.let { Text(it, color = Color(0xFFF87171), fontSize = 12.sp, fontFamily = PlusJakartaSans) }
    PrimaryCta(
        label = if (submitting) "Saving…" else "Add Participant",
        enabled = !momentId.isNullOrBlank() && name.isNotBlank() && !submitting,
        loading = submitting,
        accent = accent,
        lightLabel = true,
        onClick = {
            val id = momentId ?: return@PrimaryCta
            scope.launch {
                submitting = true
                error = null
                val trimmedContact = contact.trim()
                val email = if (trimmedContact.contains("@")) trimmedContact else null
                val phone = if (email == null && trimmedContact.isNotBlank()) trimmedContact else null
                repository.addParticipant(
                    id,
                    name.trim(),
                    roleCode = "PARTICIPANT",
                    email = email,
                    phone = phone,
                ).fold(
                    onSuccess = {
                        submitting = false
                        onSaved()
                        onDismiss()
                    },
                    onFailure = {
                        submitting = false
                        error = it.message
                    },
                )
            }
        },
    )
}
