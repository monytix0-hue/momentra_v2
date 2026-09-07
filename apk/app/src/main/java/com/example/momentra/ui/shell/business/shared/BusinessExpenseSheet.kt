package com.example.momentra.ui.shell.business.shared

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R
import com.example.momentra.data.api.CreateBusinessExpenseBody
import com.example.momentra.data.repository.BusinessSliceRepository
import com.example.momentra.ui.shell.business.teamops.components.TeamOpsChipRow
import com.example.momentra.ui.shell.business.teamops.components.TeamOpsDateField
import com.example.momentra.ui.shell.business.teamops.components.TeamOpsDropdownField
import com.example.momentra.ui.shell.business.teamops.components.TeamOpsErrorText
import com.example.momentra.ui.shell.business.teamops.components.TeamOpsFieldLabel
import com.example.momentra.ui.shell.business.teamops.components.TeamOpsIndigoAccent
import com.example.momentra.ui.shell.business.teamops.components.TeamOpsPrimaryCta
import com.example.momentra.ui.shell.business.teamops.components.TeamOpsSheetHandle
import com.example.momentra.ui.shell.business.teamops.components.TeamOpsSheetTokens
import com.example.momentra.ui.shell.business.teamops.components.TeamOpsTextField
import com.example.momentra.ui.shell.business.teamops.components.teamOpsFormatAmountDisplay
import com.example.momentra.ui.shell.business.teamops.components.teamOpsStripAmount
import com.example.momentra.ui.shell.group.shared.TravelCurrencyCatalog
import com.example.momentra.ui.shell.maestro.MaestroIds
import com.example.momentra.ui.shell.shared.MomentCurrencyResolver
import com.example.momentra.ui.shell.shared.loadBusinessCurrencyContext
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Figma `1620:12158` — Add Expense bottom sheet (Team Ops / business). */
private val CategoryLabels = listOf("Software", "Travel", "Office", "Equipment", "Services", "Other")
private val PaidByOptions = listOf("You")

private fun categoryCode(label: String): String = label.uppercase()

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BusinessExpenseSheet(
    momentId: String,
    visible: Boolean,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    repository: BusinessSliceRepository = remember { BusinessSliceRepository() },
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val accent = TeamOpsIndigoAccent
    var amountDisplay by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("INR") }
    var preferredCurrencyCodes by remember { mutableStateOf(listOf("INR")) }
    var description by remember { mutableStateOf("") }
    var categoryLabel by remember { mutableStateOf("Software") }
    var paidBy by remember { mutableStateOf("You") }
    var isoDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var receiptUri by remember { mutableStateOf<Uri?>(null) }
    var receiptName by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    val pickReceipt = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        receiptUri = uri
        receiptName = uri?.lastPathSegment?.substringAfterLast('/') ?: uri?.toString()?.takeLast(40)
    }

    LaunchedEffect(momentId, visible) {
        if (!visible) return@LaunchedEffect
        val ctx = loadBusinessCurrencyContext(momentId)
        currency = ctx.primary
        preferredCurrencyCodes = ctx.preferred
        isoDate = LocalDate.now().toString()
        paidBy = "You"
        receiptUri = null
        receiptName = null
        error = null
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
                .padding(top = 12.dp, bottom = 34.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TeamOpsSheetHandle()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(accent.soft),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_teamops_expense_wallet),
                            contentDescription = null,
                            tint = accent.accent,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Text(
                        "Add Expense",
                        color = TeamOpsSheetTokens.Text,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = PlusJakartaSans,
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TeamOpsFieldLabel("Amount")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(MaestroIds.BUSINESS_EXPENSE_AMOUNT),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ExpenseCurrencyPill(
                            selectedCode = currency,
                            preferredCodes = preferredCurrencyCodes,
                            onSelected = { currency = it },
                        )
                        BasicTextField(
                            value = amountDisplay,
                            onValueChange = { raw ->
                                amountDisplay = teamOpsFormatAmountDisplay(teamOpsStripAmount(raw))
                            },
                            textStyle = TextStyle(
                                color = TeamOpsSheetTokens.Text,
                                fontSize = 40.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = PlusJakartaSans,
                            ),
                            cursorBrush = SolidColor(accent.accent),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            decorationBox = { inner ->
                                Box {
                                    if (amountDisplay.isEmpty()) {
                                        Text(
                                            "0.00",
                                            color = TeamOpsSheetTokens.Text,
                                            fontSize = 40.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontFamily = PlusJakartaSans,
                                        )
                                    }
                                    inner()
                                }
                            },
                        )
                        Box(
                            modifier = Modifier
                                .padding(bottom = 8.dp)
                                .width(3.dp)
                                .height(36.dp)
                                .background(accent.accent),
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TeamOpsFieldLabel("Description")
                    TeamOpsTextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = "Software subscription renewal",
                        accent = accent,
                        modifier = Modifier.testTag(MaestroIds.BUSINESS_EXPENSE_NOTE),
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TeamOpsFieldLabel("Category")
                    TeamOpsChipRow(
                        options = CategoryLabels,
                        selected = categoryLabel,
                        accent = accent,
                        modifier = Modifier.testTag(MaestroIds.BUSINESS_EXPENSE_CATEGORY),
                        onSelect = { categoryLabel = it },
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TeamOpsFieldLabel("Paid By")
                        TeamOpsDropdownField(
                            value = paidBy,
                            options = PaidByOptions,
                            onSelect = { paidBy = it },
                            placeholder = "You",
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TeamOpsFieldLabel("Date")
                        TeamOpsDateField(
                            isoDate = isoDate,
                            onIsoDateChange = { isoDate = it },
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TeamOpsFieldLabel("Receipt")
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(TeamOpsSheetTokens.Field)
                            .border(1.dp, TeamOpsSheetTokens.Border, RoundedCornerShape(12.dp))
                            .clickable { pickReceipt.launch("*/*") }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text(
                            receiptName ?: "Attach PDF/Img",
                            color = if (receiptName != null) {
                                TeamOpsSheetTokens.Text
                            } else {
                                TeamOpsSheetTokens.Muted
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = PlusJakartaSans,
                        )
                    }
                }
            }

            TeamOpsErrorText(error)

            TeamOpsPrimaryCta(
                label = if (submitting) "Saving…" else "Add Expense",
                enabled = !submitting,
                loading = submitting,
                footerHint = "Team will be notified",
                accent = accent,
                modifier = Modifier.testTag(MaestroIds.BUSINESS_EXPENSE_SUBMIT),
                onClick = {
                    val amt = teamOpsStripAmount(amountDisplay).trim()
                    if (amt.isEmpty() || amt == "0" || amt == "0.00") {
                        error = "Enter an amount"
                        return@TeamOpsPrimaryCta
                    }
                    submitting = true
                    error = null
                    val receipt = receiptUri
                    scope.launch {
                        repository.createExpense(
                            momentId = momentId,
                            body = CreateBusinessExpenseBody(
                                amount = amt,
                                currencyCode = currency.ifBlank { "INR" }.uppercase(),
                                description = description.takeIf { it.isNotBlank() },
                                categoryCode = categoryCode(categoryLabel),
                                paidBy = paidBy.takeIf { it.isNotBlank() },
                                effectiveAt = "${isoDate}T12:00:00.000Z",
                            ),
                        ).fold(
                            onSuccess = { created ->
                                if (receipt != null) {
                                    val bytes = runCatching {
                                        context.contentResolver.openInputStream(receipt)?.use { it.readBytes() }
                                    }.getOrNull()
                                    val mime = context.contentResolver.getType(receipt) ?: "application/octet-stream"
                                    if (bytes != null && bytes.isNotEmpty()) {
                                        repository.uploadAndAttachExpenseMedia(
                                            momentId = momentId,
                                            expenseId = created.expenseId,
                                            bytes = bytes,
                                            contentType = mime,
                                        )
                                    }
                                }
                                submitting = false
                                onSaved()
                                if (created.status.equals("DRAFT", ignoreCase = true)) {
                                    error = "Pending approval — burn updates after approve"
                                } else {
                                    onDismiss()
                                }
                            },
                            onFailure = {
                                submitting = false
                                error = it.message ?: "Could not save expense"
                            },
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun ExpenseCurrencyPill(
    selectedCode: String,
    preferredCodes: List<String>,
    onSelected: (String) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val options = remember(preferredCodes) { MomentCurrencyResolver.pickerOptions(preferredCodes) }
    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(TeamOpsSheetTokens.Field)
                .border(1.dp, TeamOpsSheetTokens.Border, RoundedCornerShape(999.dp))
                .clickable { menuOpen = true }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                selectedCode.ifBlank { "INR" },
                color = TeamOpsSheetTokens.Text,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
            )
            Icon(
                painter = painterResource(R.drawable.ic_biz_create_chevron),
                contentDescription = null,
                tint = TeamOpsSheetTokens.Muted,
                modifier = Modifier.size(16.dp),
            )
        }
        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
            modifier = Modifier.background(TeamOpsSheetTokens.Field),
        ) {
            options.forEach { code ->
                DropdownMenuItem(
                    text = {
                        Text(
                            TravelCurrencyCatalog.display(code),
                            color = TeamOpsSheetTokens.Text,
                            fontFamily = PlusJakartaSans,
                            fontSize = 14.sp,
                        )
                    },
                    onClick = {
                        onSelected(code)
                        menuOpen = false
                    },
                )
            }
        }
    }
}
