package com.example.momentra.ui.shell.business.shared

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.CreateBusinessRevenueBody
import com.example.momentra.data.api.CreateKhataEntryBody
import com.example.momentra.data.api.CreateKhataPartyBody
import com.example.momentra.data.api.KhataEntryItemDto
import com.example.momentra.data.api.KhataPartyItemDto
import com.example.momentra.data.repository.BusinessSliceRepository
import com.example.momentra.ui.shell.business.teamops.components.TeamOpsChipRow
import com.example.momentra.ui.shell.business.teamops.components.TeamOpsErrorText
import com.example.momentra.ui.shell.business.teamops.components.TeamOpsFieldLabel
import com.example.momentra.ui.shell.business.teamops.components.TeamOpsIndigoAccent
import com.example.momentra.ui.shell.business.teamops.components.TeamOpsPrimaryCta
import com.example.momentra.ui.shell.business.teamops.components.TeamOpsSheetHandle
import com.example.momentra.ui.shell.business.teamops.components.TeamOpsSheetTokens
import com.example.momentra.ui.shell.business.teamops.components.TeamOpsTextField
import com.example.momentra.ui.shell.business.teamops.components.teamOpsFormatAmountDisplay
import com.example.momentra.ui.shell.business.teamops.components.teamOpsStripAmount
import com.example.momentra.ui.shell.empty.group.sendInviteWhatsApp
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Accent = TeamOpsIndigoAccent
private val PaymentLabels = listOf("Cash", "UPI", "Card")

private fun paymentCode(label: String): String = when (label.trim().lowercase()) {
    "cash" -> "CASH"
    "upi" -> "UPI"
    "card" -> "CARD"
    else -> "OTHER"
}

private fun formatDue(raw: String?): String {
    val n = raw?.toBigDecimalOrNull() ?: BigDecimal.ZERO
    return "₹${n.stripTrailingZeros().toPlainString()}"
}

private fun formatDay(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return runCatching {
        OffsetDateTime.parse(iso).toLocalDate().format(DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH))
    }.getOrElse { "" }
}

fun khataReminderMessage(
    partyName: String,
    amountDue: String,
    shopName: String,
): String {
    val amt = formatDue(amountDue)
    val shop = shopName.ifBlank { "our shop" }
    return "Namaste $partyName,\n\n" +
        "Aapka khata balance $amt hai ($shop).\n" +
        "Your credit balance is $amt.\n\n" +
        "Please settle when convenient. Dhanyavaad!"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessKhataHomeSheet(
    momentId: String,
    companyId: String,
    shopName: String,
    visible: Boolean,
    onDismiss: () -> Unit,
    repository: BusinessSliceRepository = remember { BusinessSliceRepository() },
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var tab by remember { mutableStateOf("CUSTOMER") }
    var items by remember { mutableStateOf<List<KhataPartyItemDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showAddParty by remember { mutableStateOf(false) }
    var historyParty by remember { mutableStateOf<KhataPartyItemDto?>(null) }
    var entryParty by remember { mutableStateOf<KhataPartyItemDto?>(null) }
    var entryMode by remember { mutableStateOf("CREDIT") }
    var cashSaleParty by remember { mutableStateOf<KhataPartyItemDto?>(null) }

    fun reload() {
        scope.launch {
            loading = true
            error = null
            repository.listKhataParties(companyId, tab).fold(
                onSuccess = {
                    items = it.items
                    loading = false
                },
                onFailure = {
                    error = it.message ?: "Could not load khata"
                    loading = false
                },
            )
        }
    }

    LaunchedEffect(companyId, visible, tab) {
        if (!visible || companyId.isBlank()) return@LaunchedEffect
        reload()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = TeamOpsSheetTokens.SheetBg,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            TeamOpsSheetHandle()
            Text(
                "Khata",
                color = TeamOpsSheetTokens.Text,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = PlusJakartaSans,
            )
            Text(
                "Who owes you — and who you owe",
                color = TeamOpsSheetTokens.Muted,
                fontSize = 13.sp,
                fontFamily = PlusJakartaSans,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("CUSTOMER" to "Customers", "SUPPLIER" to "Suppliers").forEach { (key, label) ->
                    val selected = tab == key
                    Text(
                        label,
                        color = if (selected) Color.White else TeamOpsSheetTokens.Muted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = PlusJakartaSans,
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(if (selected) Accent.accent else TeamOpsSheetTokens.Field)
                            .clickable { tab = key }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }

            TeamOpsErrorText(error)

            when {
                loading -> Text("Loading…", color = TeamOpsSheetTokens.Muted, fontSize = 13.sp)
                items.isEmpty() -> Text(
                    if (tab == "CUSTOMER") {
                        "Add a customer, then log udhaar when they take goods on credit"
                    } else {
                        "Add a supplier, then log credit purchases and payouts"
                    },
                    color = TeamOpsSheetTokens.Muted,
                    fontSize = 14.sp,
                    fontFamily = PlusJakartaSans,
                )
                else -> items.forEach { party ->
                    KhataPartyRow(
                        party = party,
                        onOpenHistory = { historyParty = party },
                        onCredit = {
                            entryParty = party
                            entryMode = "CREDIT"
                        },
                        onCollect = {
                            entryParty = party
                            entryMode = "PAYMENT"
                        },
                        onCashSale = { cashSaleParty = party },
                        onRemind = {
                            val msg = khataReminderMessage(party.name, party.balanceDue, shopName)
                            sendInviteWhatsApp(context, party.phone, msg)
                        },
                    )
                }
            }

            TeamOpsPrimaryCta(
                label = if (tab == "CUSTOMER") "Add customer" else "Add supplier",
                enabled = companyId.isNotBlank(),
                loading = false,
                footerHint = "",
                accent = Accent,
                onClick = { showAddParty = true },
            )
        }
    }

    BusinessKhataAddPartySheet(
        companyId = companyId,
        partyKind = tab,
        visible = showAddParty,
        onDismiss = { showAddParty = false },
        onSaved = {
            showAddParty = false
            reload()
        },
        repository = repository,
    )

    entryParty?.let { party ->
        BusinessKhataEntrySheet(
            momentId = momentId,
            party = party,
            entryType = entryMode,
            visible = true,
            onDismiss = { entryParty = null },
            onSaved = {
                entryParty = null
                reload()
            },
            repository = repository,
        )
    }

    cashSaleParty?.let { party ->
        BusinessKhataCashSaleSheet(
            momentId = momentId,
            party = party,
            visible = true,
            onDismiss = { cashSaleParty = null },
            onSaved = {
                cashSaleParty = null
                reload()
            },
            repository = repository,
        )
    }

    historyParty?.let { party ->
        BusinessKhataHistorySheet(
            companyId = companyId,
            party = party,
            visible = true,
            onDismiss = { historyParty = null },
            repository = repository,
        )
    }
}

@Composable
private fun KhataPartyRow(
    party: KhataPartyItemDto,
    onOpenHistory: () -> Unit,
    onCredit: () -> Unit,
    onCollect: () -> Unit,
    onCashSale: () -> Unit,
    onRemind: () -> Unit,
) {
    val due = party.balanceDue.toBigDecimalOrNull() ?: BigDecimal.ZERO
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(TeamOpsSheetTokens.Field)
            .border(1.dp, TeamOpsSheetTokens.Border, RoundedCornerShape(14.dp))
            .clickable(onClick = onOpenHistory)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    party.name,
                    color = TeamOpsSheetTokens.Text,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    fontFamily = PlusJakartaSans,
                )
                val meta = buildList {
                    party.phone?.takeIf { it.isNotBlank() }?.let { add(it) }
                    formatDay(party.lastActivityAt).takeIf { it.isNotBlank() }?.let { add(it) }
                    if (party.overdue && due > BigDecimal.ZERO) add("Overdue")
                }.joinToString(" · ")
                if (meta.isNotBlank()) {
                    Text(meta, color = TeamOpsSheetTokens.Muted, fontSize = 12.sp, fontFamily = PlusJakartaSans)
                }
            }
            Text(
                formatDue(party.balanceDue),
                color = if (due > BigDecimal.ZERO) Color(0xFFF59E0B) else TeamOpsSheetTokens.Text,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                fontFamily = PlusJakartaSans,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KhataMiniBtn(
                if (party.partyKind.equals("SUPPLIER", true)) "Udhaar liya" else "Udhaar diya",
                onCredit,
            )
            KhataMiniBtn(
                if (party.partyKind.equals("SUPPLIER", true)) "Paisa diya" else "Paisa mila",
                onCollect,
            )
            if (!party.partyKind.equals("SUPPLIER", true)) {
                KhataMiniBtn("Cash sale", onCashSale)
            }
            if (!party.phone.isNullOrBlank() && due > BigDecimal.ZERO) {
                KhataMiniBtn("Remind", onRemind)
            }
        }
    }
}

@Composable
private fun KhataMiniBtn(label: String, onClick: () -> Unit) {
    Text(
        label,
        color = Accent.accent,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = PlusJakartaSans,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Accent.soft)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessKhataAddPartySheet(
    companyId: String,
    partyKind: String,
    visible: Boolean,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    repository: BusinessSliceRepository = remember { BusinessSliceRepository() },
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = TeamOpsSheetTokens.SheetBg,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            TeamOpsSheetHandle()
            Text(
                if (partyKind == "SUPPLIER") "Add supplier" else "Add customer",
                color = TeamOpsSheetTokens.Text,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = PlusJakartaSans,
            )
            TeamOpsFieldLabel("Name")
            TeamOpsTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = if (partyKind == "SUPPLIER") "Supplier name" else "Customer name",
                accent = Accent,
            )
            TeamOpsFieldLabel("Phone (for WhatsApp remind)")
            TeamOpsTextField(
                value = phone,
                onValueChange = { phone = it },
                placeholder = "10-digit mobile",
                accent = Accent,
            )
            TeamOpsErrorText(error)
            TeamOpsPrimaryCta(
                label = if (submitting) "Saving…" else "Save",
                enabled = !submitting && name.isNotBlank(),
                loading = submitting,
                footerHint = "",
                accent = Accent,
                onClick = {
                    submitting = true
                    error = null
                    scope.launch {
                        repository.createKhataParty(
                            companyId = companyId,
                            body = CreateKhataPartyBody(
                                name = name.trim(),
                                partyKind = partyKind,
                                phone = phone.trim().takeIf { it.isNotBlank() },
                            ),
                        ).fold(
                            onSuccess = {
                                submitting = false
                                onSaved()
                            },
                            onFailure = {
                                error = it.message ?: "Could not save"
                                submitting = false
                            },
                        )
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessKhataEntrySheet(
    momentId: String,
    party: KhataPartyItemDto,
    entryType: String,
    visible: Boolean,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    repository: BusinessSliceRepository = remember { BusinessSliceRepository() },
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val isPayment = entryType == "PAYMENT"
    val isSupplier = party.partyKind.equals("SUPPLIER", true)
    var amountDisplay by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var paymentLabel by remember { mutableStateOf(PaymentLabels.first()) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val title = when {
        isPayment && isSupplier -> "Paisa diya · Collection"
        isPayment -> "Paisa mila · Collection"
        isSupplier -> "Udhaar liya · Credit purchase"
        else -> "Udhaar diya · Credit sale"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = TeamOpsSheetTokens.SheetBg,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            TeamOpsSheetHandle()
            Text(
                title,
                color = TeamOpsSheetTokens.Text,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = PlusJakartaSans,
            )
            Text(
                party.name,
                color = TeamOpsSheetTokens.Muted,
                fontSize = 13.sp,
                fontFamily = PlusJakartaSans,
            )
            TeamOpsFieldLabel("Amount")
            BasicTextField(
                value = amountDisplay,
                onValueChange = { raw ->
                    amountDisplay = teamOpsFormatAmountDisplay(teamOpsStripAmount(raw))
                },
                textStyle = TextStyle(
                    color = TeamOpsSheetTokens.Text,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = PlusJakartaSans,
                ),
                cursorBrush = SolidColor(Accent.accent),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    Box {
                        if (amountDisplay.isEmpty()) {
                            Text(
                                "0.00",
                                color = TeamOpsSheetTokens.Muted,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = PlusJakartaSans,
                            )
                        }
                        inner()
                    }
                },
            )
            if (isPayment) {
                TeamOpsFieldLabel("Payment")
                TeamOpsChipRow(
                    options = PaymentLabels,
                    selected = paymentLabel,
                    accent = Accent,
                    onSelect = { paymentLabel = it },
                )
            }
            TeamOpsFieldLabel("Note (optional)")
            TeamOpsTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = "Goods / bill ref",
                accent = Accent,
            )
            TeamOpsErrorText(error)
            TeamOpsPrimaryCta(
                label = if (submitting) "Saving…" else "Save",
                enabled = !submitting,
                loading = submitting,
                footerHint = "",
                accent = Accent,
                onClick = {
                    val amt = teamOpsStripAmount(amountDisplay).trim()
                    if (amt.isEmpty() || amt == "0" || amt == "0.00") {
                        error = "Enter an amount"
                        return@TeamOpsPrimaryCta
                    }
                    submitting = true
                    error = null
                    scope.launch {
                        repository.createKhataEntry(
                            momentId = momentId,
                            body = CreateKhataEntryBody(
                                partyId = party.partyId,
                                entryType = entryType,
                                amount = amt,
                                currencyCode = party.currencyCode ?: "INR",
                                note = note.takeIf { it.isNotBlank() },
                                paymentMethodCode = if (isPayment) paymentCode(paymentLabel) else null,
                            ),
                        ).fold(
                            onSuccess = {
                                submitting = false
                                onSaved()
                            },
                            onFailure = {
                                error = it.message ?: "Could not save"
                                submitting = false
                            },
                        )
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessKhataCashSaleSheet(
    momentId: String,
    party: KhataPartyItemDto,
    visible: Boolean,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    repository: BusinessSliceRepository = remember { BusinessSliceRepository() },
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var amountDisplay by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var paymentLabel by remember { mutableStateOf(PaymentLabels.first()) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = TeamOpsSheetTokens.SheetBg,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            TeamOpsSheetHandle()
            Text(
                "Cash sale · Nakal becha",
                color = TeamOpsSheetTokens.Text,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = PlusJakartaSans,
            )
            Text(party.name, color = TeamOpsSheetTokens.Muted, fontSize = 13.sp, fontFamily = PlusJakartaSans)
            TeamOpsFieldLabel("Amount")
            BasicTextField(
                value = amountDisplay,
                onValueChange = { raw ->
                    amountDisplay = teamOpsFormatAmountDisplay(teamOpsStripAmount(raw))
                },
                textStyle = TextStyle(
                    color = TeamOpsSheetTokens.Text,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = PlusJakartaSans,
                ),
                cursorBrush = SolidColor(Accent.accent),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    Box {
                        if (amountDisplay.isEmpty()) {
                            Text(
                                "0.00",
                                color = TeamOpsSheetTokens.Muted,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = PlusJakartaSans,
                            )
                        }
                        inner()
                    }
                },
            )
            TeamOpsFieldLabel("Payment")
            TeamOpsChipRow(
                options = PaymentLabels,
                selected = paymentLabel,
                accent = Accent,
                onSelect = { paymentLabel = it },
            )
            TeamOpsFieldLabel("Note (optional)")
            TeamOpsTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = "What was sold",
                accent = Accent,
            )
            TeamOpsErrorText(error)
            TeamOpsPrimaryCta(
                label = if (submitting) "Saving…" else "Save cash sale",
                enabled = !submitting,
                loading = submitting,
                footerHint = "Does not change udhaar balance",
                accent = Accent,
                onClick = {
                    val amt = teamOpsStripAmount(amountDisplay).trim()
                    if (amt.isEmpty() || amt == "0" || amt == "0.00") {
                        error = "Enter an amount"
                        return@TeamOpsPrimaryCta
                    }
                    submitting = true
                    error = null
                    scope.launch {
                        repository.createRevenue(
                            momentId = momentId,
                            body = CreateBusinessRevenueBody(
                                amount = amt,
                                currencyCode = (party.currencyCode ?: "INR").uppercase(),
                                description = note.takeIf { it.isNotBlank() } ?: "Cash sale · ${party.name}",
                                partyId = party.partyId,
                                paymentMethodCode = paymentCode(paymentLabel),
                            ),
                        ).fold(
                            onSuccess = {
                                submitting = false
                                onSaved()
                            },
                            onFailure = {
                                error = it.message ?: "Could not save"
                                submitting = false
                            },
                        )
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessKhataHistorySheet(
    companyId: String,
    party: KhataPartyItemDto,
    visible: Boolean,
    onDismiss: () -> Unit,
    repository: BusinessSliceRepository = remember { BusinessSliceRepository() },
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var entries by remember { mutableStateOf<List<KhataEntryItemDto>>(emptyList()) }
    var balanceDue by remember { mutableStateOf(party.balanceDue) }

    LaunchedEffect(companyId, party.partyId, visible) {
        if (!visible) return@LaunchedEffect
        loading = true
        error = null
        repository.listKhataPartyEntries(companyId, party.partyId).fold(
            onSuccess = {
                entries = it.items
                balanceDue = it.balanceDue ?: party.balanceDue
                loading = false
            },
            onFailure = {
                error = it.message ?: "Could not load history"
                loading = false
            },
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = TeamOpsSheetTokens.SheetBg,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TeamOpsSheetHandle()
            Text(
                party.name,
                color = TeamOpsSheetTokens.Text,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = PlusJakartaSans,
            )
            Text(
                "Due ${formatDue(balanceDue)}",
                color = TeamOpsSheetTokens.Muted,
                fontSize = 13.sp,
                fontFamily = PlusJakartaSans,
            )
            TeamOpsErrorText(error)
            when {
                loading -> Text("Loading…", color = TeamOpsSheetTokens.Muted, fontSize = 13.sp)
                entries.isEmpty() -> Text(
                    "No entries yet — log udhaar or a collection",
                    color = TeamOpsSheetTokens.Muted,
                    fontSize = 14.sp,
                )
                else -> entries.forEach { entry ->
                    val credit = entry.entryType.equals("CREDIT", true)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(TeamOpsSheetTokens.Field)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (credit) "Udhaar" else "Collection",
                                color = TeamOpsSheetTokens.Text,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                fontFamily = PlusJakartaSans,
                            )
                            val sub = buildList {
                                formatDay(entry.effectiveAt).takeIf { it.isNotBlank() }?.let { add(it) }
                                entry.paymentMethodCode?.let { add(it) }
                                entry.note?.takeIf { it.isNotBlank() }?.let { add(it) }
                            }.joinToString(" · ")
                            if (sub.isNotBlank()) {
                                Text(sub, color = TeamOpsSheetTokens.Muted, fontSize = 12.sp)
                            }
                        }
                        Text(
                            (if (credit) "+" else "−") + formatDue(entry.amount),
                            color = if (credit) Color(0xFFF59E0B) else Color(0xFF10B981),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            fontFamily = PlusJakartaSans,
                        )
                    }
                }
            }
        }
    }
}
