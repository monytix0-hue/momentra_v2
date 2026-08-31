package com.example.momentra.ui.shell.group.experience

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
import com.example.momentra.ui.shell.group.wedding.ChipRow
import com.example.momentra.ui.shell.group.wedding.FieldLabel
import com.example.momentra.ui.shell.group.wedding.PrimaryCta
import com.example.momentra.ui.shell.group.wedding.SheetAccent
import com.example.momentra.ui.shell.group.wedding.SheetField
import com.example.momentra.ui.shell.group.wedding.SheetHeader
import com.example.momentra.ui.shell.group.wedding.WeddingAttendanceSheetBody
import com.example.momentra.ui.shell.group.wedding.WeddingBudgetSheetBody
import com.example.momentra.ui.shell.group.wedding.WeddingContributionSheetBody
import com.example.momentra.ui.shell.group.wedding.WeddingExpenseSheetBody
import com.example.momentra.ui.shell.group.wedding.WeddingMemorySheetBody
import com.example.momentra.ui.shell.group.wedding.WeddingPlanningSheetBody
import com.example.momentra.ui.shell.group.wedding.WeddingPollSheetBody
import com.example.momentra.ui.shell.group.wedding.WeddingSettleSheetBody
import com.example.momentra.ui.shell.group.wedding.WeddingUpdateSheetBody
import com.example.momentra.ui.shell.group.wedding.WeddingVendorSheetBody
import com.example.momentra.ui.theme.PlusJakartaSans

private val EqSheet = Color(0xFF1C1A24)
private val EqHandle = Color(0xFF625E70)
private val EqText = Color(0xFFFFFFFF)
private val EqMuted = Color(0xFF9E9AA8)
private val EqBorder = Color(0xFF322E40)

private fun ExperienceActiveTheme.toSheetAccent(): SheetAccent =
    SheetAccent(accent = accent, accentEnd = accentSolid, soft = accentSoft)

/** Figma party 592:8580 / outing 592:7770 — moment-colored Experience Quick Add sheets. */
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

    val accent = theme.toSheetAccent()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = EqSheet,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 48.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(EqHandle),
            )
            when (kind) {
                ExperienceQuickAddKind.EXPENSE ->
                    WeddingExpenseSheetBody(momentId, repository, onDismiss, onSaved, accent)
                ExperienceQuickAddKind.CONTRIBUTION ->
                    WeddingContributionSheetBody(momentId, repository, onDismiss, onSaved, accent)
                ExperienceQuickAddKind.BUDGET ->
                    WeddingBudgetSheetBody(momentId, repository, onDismiss, onSaved, accent)
                ExperienceQuickAddKind.PARTICIPANT ->
                    ExperienceParticipantSheetBody(theme, accent)
                ExperienceQuickAddKind.VENDOR ->
                    WeddingVendorSheetBody(momentId, repository, onDismiss, onSaved, accent)
                ExperienceQuickAddKind.PLANNING ->
                    WeddingPlanningSheetBody(momentId, repository, onDismiss, onSaved, accent)
                ExperienceQuickAddKind.ATTENDANCE ->
                    WeddingAttendanceSheetBody(momentId, repository, onDismiss, onSaved, accent)
                ExperienceQuickAddKind.POLL ->
                    WeddingPollSheetBody(momentId, repository, onDismiss, onSaved, accent)
                ExperienceQuickAddKind.MEMORY ->
                    WeddingMemorySheetBody(momentId, repository, onDismiss, onSaved, accent)
                ExperienceQuickAddKind.UPDATE ->
                    WeddingUpdateSheetBody(momentId, repository, onDismiss, onSaved, accent)
                ExperienceQuickAddKind.SETTLE ->
                    WeddingSettleSheetBody()
                ExperienceQuickAddKind.BOOKING -> Unit
            }
        }
    }
}

@Composable
private fun ExperienceParticipantSheetBody(theme: ExperienceActiveTheme, accent: SheetAccent) {
    var name by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(theme.participantRoles.first()) }
    var rsvp by remember { mutableStateOf("Pending") }
    var plusOne by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }

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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { plusOne = !plusOne }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text("Plus One Allowed", color = EqText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
            Text("Include guest's spouse or partner", color = EqMuted, fontSize = 12.sp, fontFamily = PlusJakartaSans)
        }
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(22.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(if (plusOne) accent.accent else EqBorder),
            contentAlignment = if (plusOne) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .padding(2.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(EqText),
            )
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Dietary Preferences / Notes")
        SheetField(notes, { notes = it }, "Optional notes", minHeight = 42)
    }
    PrimaryCta("Add Participant", enabled = false, accent = accent, lightLabel = true, onClick = {})
}
