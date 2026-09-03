package com.example.momentra.ui.shell.group.purchase.create

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
import com.example.momentra.ui.shell.group.wedding.create.ChipRow
import com.example.momentra.ui.shell.group.wedding.create.FieldLabel
import com.example.momentra.ui.shell.group.wedding.create.PrimaryCta
import com.example.momentra.ui.shell.group.wedding.create.SheetAccent
import com.example.momentra.ui.shell.group.wedding.create.SheetField
import com.example.momentra.ui.shell.group.wedding.create.SheetHeader
import com.example.momentra.ui.shell.group.wedding.create.WeddingBudgetSheetBody
import com.example.momentra.ui.shell.group.wedding.create.WeddingContributionSheetBody
import com.example.momentra.ui.shell.group.wedding.create.WeddingExpenseSheetBody
import com.example.momentra.ui.shell.group.wedding.create.WeddingMemorySheetBody
import com.example.momentra.ui.shell.group.wedding.create.WeddingPollSheetBody
import com.example.momentra.ui.shell.group.wedding.create.WeddingUpdateSheetBody
import com.example.momentra.ui.shell.group.wedding.create.WeddingVendorSheetBody
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.launch

private val EqSheet = Color(0xFF1C1A24)
private val EqHandle = Color(0xFF625E70)
private val EqText = Color(0xFFFFFFFF)
private val EqMuted = Color(0xFF9E9AA8)
private val EqBorder = Color(0xFF322E40)

private fun PurchaseActiveTheme.toSheetAccent(): SheetAccent =
    SheetAccent(accent = accent, accentEnd = accentSolid, soft = accentSoft)

/** Shared Purchase Quick Add sheets — theme accent, wedding bodies reused for live kinds. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PurchaseGapQuickAddSheet(
    theme: PurchaseActiveTheme,
    kind: PurchaseQuickAddKind,
    visible: Boolean,
    momentId: String?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit = {},
    repository: GroupSliceRepository = remember { GroupSliceRepository() },
) {
    if (!visible) return

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
                PurchaseQuickAddKind.EXPENSE ->
                    WeddingExpenseSheetBody(momentId, repository, onDismiss, onSaved, accent)
                PurchaseQuickAddKind.CONTRIBUTION ->
                    WeddingContributionSheetBody(momentId, repository, onDismiss, onSaved, accent)
                PurchaseQuickAddKind.BUDGET ->
                    WeddingBudgetSheetBody(momentId, repository, onDismiss, onSaved, accent)
                PurchaseQuickAddKind.CONTRIBUTOR ->
                    PurchaseContributorSheetBody(momentId, repository, onDismiss, onSaved, theme, accent)
                PurchaseQuickAddKind.VENDOR ->
                    WeddingVendorSheetBody(momentId, repository, onDismiss, onSaved, accent)
                PurchaseQuickAddKind.POLL ->
                    WeddingPollSheetBody(momentId, repository, onDismiss, onSaved, accent)
                PurchaseQuickAddKind.MEMORY ->
                    WeddingMemorySheetBody(momentId, repository, onDismiss, onSaved, accent)
                PurchaseQuickAddKind.UPDATE ->
                    WeddingUpdateSheetBody(momentId, repository, onDismiss, onSaved, accent)
                PurchaseQuickAddKind.PURCHASE_ITEM ->
                    PurchaseItemSheetBody(momentId, repository, onDismiss, onSaved, accent)
                PurchaseQuickAddKind.DELIVERY ->
                    PurchaseDeliverySheetBody(momentId, repository, onDismiss, onSaved, accent)
                PurchaseQuickAddKind.OWNERSHIP ->
                    PurchaseOwnershipSheetBody(momentId, repository, onDismiss, onSaved, accent)
            }
        }
    }
}

@Composable
private fun PurchaseContributorSheetBody(
    momentId: String?,
    repository: GroupSliceRepository,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    theme: PurchaseActiveTheme,
    accent: SheetAccent,
) {
    var name by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(theme.participantRoles.first()) }
    var notes by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    SheetHeader(
        R.drawable.ic_qa_users,
        "Add Contributor",
        theme.participantSubtitle,
        accent = accent,
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Name")
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
        FieldLabel("Notes")
        SheetField(notes, { notes = it }, "Optional notes", minHeight = 42)
    }
    error?.let { Text(it, color = Color(0xFFF87171), fontSize = 12.sp, fontFamily = PlusJakartaSans) }
    PrimaryCta(
        label = if (submitting) "Saving…" else "Add Contributor",
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
                    roleCode = "CONTRIBUTOR",
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

@Composable
private fun PurchaseItemSheetBody(
    momentId: String?,
    repository: GroupSliceRepository,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    accent: SheetAccent,
) {
    var label by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    SheetHeader(
        R.drawable.ic_pulse_cart,
        "Add Purchase Item",
        "Track something the group is buying",
        accent = accent,
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Label")
        SheetField(label, { label = it }, "Item name", minHeight = 42)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Amount (optional)")
        SheetField(amount, { amount = it }, "0.00", minHeight = 42)
    }
    error?.let { Text(it, color = Color(0xFFF87171), fontSize = 12.sp, fontFamily = PlusJakartaSans) }
    PrimaryCta(
        label = "Save Item",
        enabled = !momentId.isNullOrBlank() && label.isNotBlank() && !submitting,
        loading = submitting,
        accent = accent,
        lightLabel = true,
        onClick = {
            val id = momentId ?: return@PrimaryCta
            scope.launch {
                submitting = true
                error = null
                repository.createPurchaseItem(id, label.trim(), amount.trim().ifBlank { null }).fold(
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
private fun PurchaseDeliverySheetBody(
    momentId: String?,
    repository: GroupSliceRepository,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    accent: SheetAccent,
) {
    var recipient by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var window by remember { mutableStateOf("Flexible") }
    var carrier by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    SheetHeader(
        R.drawable.ic_qa_book,
        "Delivery / Handover",
        "Plan how the purchase reaches the group",
        accent = accent,
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Recipient")
        SheetField(recipient, { recipient = it }, "Who receives it?", minHeight = 42)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Delivery address")
        SheetField(address, { address = it }, "Street, city", minHeight = 42)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Preferred window")
        ChipRow(listOf("Flexible", "This week", "Weekend", "Custom"), window, accent) { window = it }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Carrier / method")
        SheetField(carrier, { carrier = it }, "Courier, pickup, handoff", minHeight = 42)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Notes")
        SheetField(notes, { notes = it }, "Optional notes", minHeight = 42)
    }
    error?.let { Text(it, color = Color(0xFFF87171), fontSize = 12.sp, fontFamily = PlusJakartaSans) }
    PrimaryCta(
        label = if (submitting) "Saving…" else "Save Delivery",
        enabled = !momentId.isNullOrBlank() && address.isNotBlank() && !submitting,
        loading = submitting,
        accent = accent,
        lightLabel = true,
        onClick = {
            val id = momentId ?: return@PrimaryCta
            scope.launch {
                submitting = true
                error = null
                val handoverType = when {
                    carrier.contains("pickup", ignoreCase = true) -> "PICKUP"
                    carrier.contains("hand", ignoreCase = true) -> "HANDOVER"
                    else -> "DELIVERY"
                }
                val note = listOfNotNull(
                    if (window.isNotBlank()) "Window: $window" else null,
                    notes.trim().takeIf { it.isNotBlank() },
                ).joinToString("\n").ifBlank { null }
                repository.createDeliveryHandover(
                    id,
                    recipientName = recipient.trim().ifBlank { null },
                    handoverType = handoverType,
                    address = address.trim(),
                    note = note,
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
private fun PurchaseOwnershipSheetBody(
    momentId: String?,
    repository: GroupSliceRepository,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    accent: SheetAccent,
) {
    var assetLabel by remember { mutableStateOf("") }
    var fromOwner by remember { mutableStateOf("") }
    var toOwner by remember { mutableStateOf("") }
    var share by remember { mutableStateOf("") }
    var effective by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    SheetHeader(
        R.drawable.ic_qa_sliders,
        "Transfer Ownership",
        "Record a share or ownership change",
        accent = accent,
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Asset / item")
        SheetField(assetLabel, { assetLabel = it }, "What is being transferred?", minHeight = 42)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("From")
        SheetField(fromOwner, { fromOwner = it }, "Current owner", minHeight = 42)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("To")
        SheetField(toOwner, { toOwner = it }, "New owner", minHeight = 42)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Share % (optional)")
        SheetField(share, { share = it }, "e.g. 50", minHeight = 42)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Effective date")
        SheetField(effective, { effective = it }, "YYYY-MM-DD", minHeight = 42)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Notes")
        SheetField(notes, { notes = it }, "Optional notes", minHeight = 42)
    }
    error?.let { Text(it, color = Color(0xFFF87171), fontSize = 12.sp, fontFamily = PlusJakartaSans) }
    PrimaryCta(
        label = if (submitting) "Saving…" else "Transfer Ownership",
        enabled = !momentId.isNullOrBlank() && toOwner.isNotBlank() && !submitting,
        loading = submitting,
        accent = accent,
        lightLabel = true,
        onClick = {
            val id = momentId ?: return@PrimaryCta
            scope.launch {
                submitting = true
                error = null
                val shareVal = share.trim().toDoubleOrNull()?.let { if (it > 1) it / 100.0 else it }
                repository.createOwnershipRecord(
                    id,
                    assetLabel = assetLabel.trim().ifBlank { null },
                    fromOwnerName = fromOwner.trim().ifBlank { null },
                    toParticipantName = toOwner.trim(),
                    ownershipShare = shareVal,
                    ownershipNote = notes.trim().ifBlank { null },
                    effectiveAt = effective.trim().ifBlank { null },
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
