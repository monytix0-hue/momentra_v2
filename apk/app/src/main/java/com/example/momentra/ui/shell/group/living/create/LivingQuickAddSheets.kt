package com.example.momentra.ui.shell.group.living.create

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R
import com.example.momentra.data.repository.GroupSliceRepository
import com.example.momentra.ui.shell.group.shared.GroupExpenseSheet
import com.example.momentra.ui.shell.group.shared.TripDatePickField
import com.example.momentra.ui.shell.group.wedding.create.ChipRow
import com.example.momentra.ui.shell.group.wedding.create.FieldLabel
import com.example.momentra.ui.shell.group.wedding.create.PrimaryCta
import com.example.momentra.ui.shell.group.wedding.create.SheetAccent
import com.example.momentra.ui.shell.group.wedding.create.SheetField
import com.example.momentra.ui.shell.group.wedding.create.SheetHeader
import com.example.momentra.ui.shell.group.wedding.create.WeddingContributionSheetBody
import com.example.momentra.ui.shell.group.wedding.create.WeddingMemorySheetBody
import com.example.momentra.ui.shell.group.wedding.create.WeddingPlanningSheetBody
import com.example.momentra.ui.shell.group.wedding.create.WeddingPollSheetBody
import com.example.momentra.ui.shell.group.wedding.create.WeddingUpdateSheetBody
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.launch

private val EqSheet = Color(0xFF1C1A24)
private val EqHandle = Color(0xFF625E70)
private val EqMuted = Color(0xFF9E9AA8)

private fun LivingActiveTheme.toSheetAccent(): SheetAccent =
    SheetAccent(accent = accent, accentEnd = accentSolid, soft = accentSoft)

/** Shared Living Quick Add sheets — theme accent; House Rule is GAP. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LivingGapQuickAddSheet(
    theme: LivingActiveTheme,
    kind: LivingQuickAddKind,
    visible: Boolean,
    momentId: String?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit = {},
    momentTypeCode: String? = null,
    repository: GroupSliceRepository = remember { GroupSliceRepository() },
) {
    if (!visible) return

    if (kind == LivingQuickAddKind.EXPENSE && !momentId.isNullOrBlank()) {
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
                LivingQuickAddKind.EXPENSE -> Unit
                LivingQuickAddKind.CONTRIBUTION ->
                    WeddingContributionSheetBody(momentId, repository, onDismiss, onSaved, accent)
                LivingQuickAddKind.POLL ->
                    WeddingPollSheetBody(momentId, repository, onDismiss, onSaved, accent)
                LivingQuickAddKind.MEMORY ->
                    WeddingMemorySheetBody(momentId, repository, onDismiss, onSaved, accent)
                LivingQuickAddKind.UPDATE ->
                    WeddingUpdateSheetBody(momentId, repository, onDismiss, onSaved, accent)
                LivingQuickAddKind.RESIDENT ->
                    LivingResidentSheetBody(theme, momentId, repository, onDismiss, onSaved, accent)
                LivingQuickAddKind.TASK ->
                    WeddingPlanningSheetBody(
                        momentId,
                        repository,
                        onDismiss,
                        onSaved,
                        accent,
                        momentTypeCode = momentTypeCode,
                    )
                LivingQuickAddKind.ASSET ->
                    LivingAssetSheetBody(momentId, repository, onDismiss, onSaved, accent)
                LivingQuickAddKind.MAINTENANCE ->
                    LivingMaintenanceSheetBody(momentId, repository, onDismiss, onSaved, accent)
                LivingQuickAddKind.RULE ->
                    LivingHouseRuleSheetBody(momentId, repository, onDismiss, onSaved, accent)
            }
        }
    }
}

@Composable
private fun LivingResidentSheetBody(
    theme: LivingActiveTheme,
    momentId: String?,
    repository: GroupSliceRepository,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    accent: SheetAccent,
) {
    var name by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var room by remember { mutableStateOf("") }
    var moveInDate by remember { mutableStateOf("") }
    var rentShare by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(theme.participantRoles.first()) }
    var sendInvitation by remember { mutableStateOf(true) }
    var addToExpenseSplit by remember { mutableStateOf(true) }
    var roomMenuOpen by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val roomOptions = remember { listOf("Unassigned", "Room 1", "Room 2", "Room 3", "Common area") }

    SheetHeader(
        R.drawable.ic_qa_users,
        "Add Resident",
        theme.participantSubtitle,
        accent = accent,
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Full Name")
        SheetField(name, { name = it }, "Full name", minHeight = 42)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Email or Phone")
        SheetField(contact, { contact = it }, "email@example.com or +91…", minHeight = 42)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Room Assignment")
        Box {
            SheetField(
                value = room,
                onValueChange = {},
                placeholder = "Select room",
                minHeight = 42,
                trailing = {
                    Icon(
                        painter = painterResource(R.drawable.ic_biz_create_chevron),
                        contentDescription = null,
                        tint = Color(0xFF9E9AA8),
                        modifier = Modifier.size(16.dp),
                    )
                },
            )
            // Make whole field open the menu
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { roomMenuOpen = true },
            )
            DropdownMenu(
                expanded = roomMenuOpen,
                onDismissRequest = { roomMenuOpen = false },
            ) {
                roomOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option, fontFamily = PlusJakartaSans) },
                        onClick = {
                            room = option
                            roomMenuOpen = false
                        },
                    )
                }
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Move-in Date")
        TripDatePickField(
            value = moveInDate,
            onValueChange = { moveInDate = it },
            placeholder = "Select date",
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Monthly Rent Share")
        SheetField(
            rentShare,
            { rentShare = it.filter { c -> c.isDigit() || c == '.' } },
            "0.00",
            minHeight = 42,
            leading = {
                Text("₹", color = accent.accent, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
            },
            keyboardType = KeyboardType.Decimal,
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Role")
        ChipRow(theme.participantRoles, role, accent) { role = it }
    }
    LivingToggleRow(
        title = "Send Invitation",
        subtitle = "Email/SMS joining link to this person",
        checked = sendInvitation,
        accent = accent.accent,
        onChange = { sendInvitation = it },
    )
    LivingToggleRow(
        title = "Add to Expense Split",
        subtitle = "Auto-add to active shared bills",
        checked = addToExpenseSplit,
        accent = accent.accent,
        onChange = { addToExpenseSplit = it },
    )
    error?.let { Text(it, color = Color(0xFFF87171), fontSize = 12.sp, fontFamily = PlusJakartaSans) }
    PrimaryCta(
        label = if (sendInvitation) "Invite Resident" else "Add Resident",
        enabled = !momentId.isNullOrBlank() && name.isNotBlank() && !submitting,
        loading = submitting,
        accent = accent,
        lightLabel = true,
        onClick = {
            val id = momentId ?: return@PrimaryCta
            // contact / room / moveInDate / rentShare / toggles: SCHEMA_GAP — UI parity only until API frozen
            scope.launch {
                submitting = true
                error = null
                repository.addResident(id, name.trim(), role).fold(
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

@Composable
private fun LivingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    accent: Color,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = Color(0xFFE5E0EE), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
            Text(subtitle, color = EqMuted, fontSize = 11.sp, fontFamily = PlusJakartaSans)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = accent, checkedThumbColor = Color.White),
        )
    }
}

@Composable
private fun LivingAssetSheetBody(
    momentId: String?,
    repository: GroupSliceRepository,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    accent: SheetAccent,
) {
    var title by remember { mutableStateOf("") }
    var assetType by remember { mutableStateOf("") }
    var condition by remember { mutableStateOf("GOOD") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    SheetHeader(
        R.drawable.ic_pulse_cart,
        "Add Shared Asset",
        "Track something the household shares",
        accent = accent,
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Title")
        SheetField(title, { title = it }, "Asset name", minHeight = 42)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Type (optional)")
        SheetField(assetType, { assetType = it }, "Appliance, furniture…", minHeight = 42)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Condition")
        ChipRow(listOf("NEW", "GOOD", "FAIR", "POOR", "OUT_OF_SERVICE"), condition, accent) { condition = it }
    }
    error?.let { Text(it, color = Color(0xFFF87171), fontSize = 12.sp, fontFamily = PlusJakartaSans) }
    PrimaryCta(
        label = "Save Asset",
        enabled = !momentId.isNullOrBlank() && title.isNotBlank() && !submitting,
        loading = submitting,
        accent = accent,
        lightLabel = true,
        onClick = {
            val id = momentId ?: return@PrimaryCta
            scope.launch {
                submitting = true
                error = null
                repository.createSharedAsset(
                    id,
                    title.trim(),
                    assetType.trim().ifBlank { null },
                    condition,
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

@Composable
private fun LivingMaintenanceSheetBody(
    momentId: String?,
    repository: GroupSliceRepository,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    accent: SheetAccent,
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    SheetHeader(
        R.drawable.ic_qa_sliders,
        "Add Maintenance",
        "Log a repair or upkeep item",
        accent = accent,
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Title")
        SheetField(title, { title = it }, "What needs attention", minHeight = 42)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Description (optional)")
        SheetField(description, { description = it }, "Details", minHeight = 42)
    }
    error?.let { Text(it, color = Color(0xFFF87171), fontSize = 12.sp, fontFamily = PlusJakartaSans) }
    PrimaryCta(
        label = "Save Maintenance",
        enabled = !momentId.isNullOrBlank() && title.isNotBlank() && !submitting,
        loading = submitting,
        accent = accent,
        lightLabel = true,
        onClick = {
            val id = momentId ?: return@PrimaryCta
            scope.launch {
                submitting = true
                error = null
                repository.createMaintenanceRecord(
                    id,
                    title.trim(),
                    description.trim().ifBlank { null },
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

@Composable
private fun LivingHouseRuleSheetBody(
    momentId: String?,
    repository: GroupSliceRepository,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    accent: SheetAccent,
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    SheetHeader(
        R.drawable.ic_qa_book,
        "Add House Rule",
        "Set an agreement for the household",
        accent = accent,
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Rule title")
        SheetField(title, { title = it }, "Rule title", minHeight = 42)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Description")
        SheetField(description, { description = it }, "Rule details", minHeight = 72)
    }
    error?.let { Text(it, color = Color(0xFFF87171), fontSize = 12.sp, fontFamily = PlusJakartaSans) }
    PrimaryCta(
        label = if (submitting) "Saving…" else "Add Rule",
        enabled = !momentId.isNullOrBlank() && title.isNotBlank() && description.isNotBlank() && !submitting,
        loading = submitting,
        accent = accent,
        lightLabel = true,
        onClick = {
            val id = momentId ?: return@PrimaryCta
            scope.launch {
                submitting = true
                error = null
                repository.createLivingRule(id, title.trim(), description.trim()).fold(
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
