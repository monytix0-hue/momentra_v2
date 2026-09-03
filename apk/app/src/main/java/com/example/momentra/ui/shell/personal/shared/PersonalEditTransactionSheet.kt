package com.example.momentra.ui.shell.personal.shared

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.ActivityItemDto
import com.example.momentra.data.api.FinancialAccountDto
import com.example.momentra.data.repository.PersonalTransactionRepository
import com.example.momentra.data.repository.PersonalSliceRepository
import com.example.momentra.data.repository.TransactionDomain
import com.example.momentra.data.repository.TransactionRef
import com.example.momentra.data.repository.TransactionResourceType
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TxnBg = Color(0xFF191622)
private val TxnField = Color(0xFF201E28)
private val TxnText = Color(0xFFE5E0EE)
private val TxnMuted = Color(0xFFC9C4D8)
private val TxnDim = Color(0xFF64748B)
private val TxnPurple = Color(0xFF7C5CFC)
private val TxnGreen = Color(0xFF10B981)
private val TxnRed = Color(0xFFF87171)
private val TxnBorder = Color.White.copy(alpha = 0.08f)
private val SaveGradient = Brush.horizontalGradient(listOf(Color(0xFFEC4899), Color(0xFF7C5CFC)))

private val TagOptions = listOf("Essential", "Planned", "Impulse", "Budget", "Weekly")
private val PaymentMethods = listOf("CASH", "CARD", "UPI", "BANK_TRANSFER", "WALLET", "OTHER")

/** Figma 417:8759 Edit Transaction — expense rows from Activity Timeline. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PersonalEditTransactionSheet(
    item: ActivityItemDto,
    momentId: String,
    onClose: () -> Unit,
    onSaved: () -> Unit,
    onDeleted: () -> Unit = onSaved,
    repository: PersonalTransactionRepository = remember { PersonalTransactionRepository() },
    modifier: Modifier = Modifier,
) {
    val sliceRepo = remember { PersonalSliceRepository() }
    val context = LocalContext.current
    val payload = item.activityPayload
    val expenseId = payload?.expenseId ?: return
    val txnRef = remember(momentId, expenseId) {
        TransactionRef(TransactionDomain.PERSONAL, TransactionResourceType.EXPENSE, expenseId, momentId)
    }
    val (decodedCat, decodedSub) = PersonalExpenseCategoryCatalog.decodeFromStored(payload.categoryCode)

    var title by remember(item) { mutableStateOf(item.title) }
    var amount by remember(item) { mutableStateOf(payload.amount.orEmpty()) }
    var categoryCode by remember(item) { mutableStateOf(decodedCat) }
    var subcategoryCode by remember(item) { mutableStateOf(decodedSub) }
    var notes by remember(item) {
        mutableStateOf(stripStructuredSuffix(payload.description.orEmpty()))
    }
    var selectedTags by remember(item) { mutableStateOf(setOf<String>()) }
    var isExpenseType by remember { mutableStateOf(true) }
    var recurring by remember { mutableStateOf(false) }
    var attachments by remember { mutableStateOf(listOf<String>()) }
    var accounts by remember { mutableStateOf<List<FinancialAccountDto>>(emptyList()) }
    var selectedAccountId by remember(item) { mutableStateOf<String?>(payload?.financialAccountId) }
    var paymentMethod by remember(item) { mutableStateOf(payload?.paymentMethodCode ?: "CASH") }
    var effectiveAtIso by remember(item) { mutableStateOf(item.occurredAt) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var loadingDetail by remember { mutableStateOf(true) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showAccountPicker by remember { mutableStateOf(false) }
    var categoryPickerMode by remember { mutableStateOf(CategoryPickerMode.CATEGORY) }
    var showUpload by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(expenseId, momentId) {
        loadingDetail = true
        repository.loadDetail(txnRef).fold(
            onSuccess = { detail ->
                title = detail.merchantName ?: detail.description ?: item.title
                amount = detail.amount
                categoryCode = detail.categoryCode?.let {
                    PersonalExpenseCategoryCatalog.decodeFromStored(it).first
                } ?: categoryCode
                subcategoryCode = detail.subcategoryCode
                    ?: detail.categoryCode?.let { PersonalExpenseCategoryCatalog.decodeFromStored(it).second }
                notes = stripStructuredSuffix(detail.description.orEmpty())
                selectedAccountId = detail.financialAccountId
                paymentMethod = detail.paymentMethodCode ?: paymentMethod
                effectiveAtIso = detail.effectiveAt ?: item.occurredAt
                attachments = detail.attachmentIds
                loadingDetail = false
            },
            onFailure = {
                loadingDetail = false
            },
        )
        sliceRepo.listFinancialAccounts().fold(
            onSuccess = { accounts = it },
            onFailure = { },
        )
    }

    val whenLabel = formatTxnDate(effectiveAtIso)

    val categoryLabel = PersonalExpenseCategoryCatalog.categoryForCode(categoryCode)?.label
        ?: PersonalExpenseCategoryCatalog.labelForCode(categoryCode)
    val subLabel = subcategoryCode?.let { PersonalExpenseCategoryCatalog.subcategoryLabel(it) }
        ?: "Select sub-category"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(TxnBg)
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = 36.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF3A3842)),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Text("‹", color = TxnPurple, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                "Edit Transaction",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Box(
                modifier = Modifier
                    .alpha(if (submitting) 0.6f else 1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SaveGradient)
                    .clickable(enabled = !submitting && title.isNotBlank()) {
                        submitting = true
                        error = null
                        val tagSuffix = if (selectedTags.isEmpty()) null else selectedTags.joinToString(", ")
                        val description = buildDescription(notes.trim(), tagSuffix)
                        scope.launch {
                            val result = repository.updateExpense(
                                ref = txnRef,
                                amount = amount.trim().ifBlank { null },
                                merchantName = title.trim(),
                                description = description,
                                categoryCode = PersonalExpenseCategoryCatalog.encodeCategoryCode(
                                    categoryCode,
                                    subcategoryCode,
                                ),
                                subcategoryCode = subcategoryCode,
                                financialAccountId = selectedAccountId,
                                paymentMethodCode = paymentMethod,
                                effectiveAt = effectiveAtIso,
                            )
                            submitting = false
                            result.fold(
                                onSuccess = { onSaved() },
                                onFailure = { e -> error = e.message ?: "Could not save" },
                            )
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    if (submitting) "…" else "Save",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BasicTextField(
                value = amount,
                onValueChange = { amount = it },
                textStyle = TextStyle(
                    color = TxnText,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = PlusJakartaSans,
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                cursorBrush = SolidColor(TxnPurple),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(TxnField)
                    .border(1.dp, TxnBorder, RoundedCornerShape(999.dp))
                    .padding(4.dp),
            ) {
                listOf(true to "Expense", false to "Income").forEach { (expense, label) ->
                    val selected = isExpenseType == expense
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (selected) TxnPurple else Color.Transparent)
                            .clickable(enabled = expense) {
                                if (!expense) {
                                    Toast.makeText(context, "Income not supported yet", Toast.LENGTH_SHORT).show()
                                } else {
                                    isExpenseType = true
                                }
                            }
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                    ) {
                        Text(
                            label,
                            color = if (selected) Color.White else if (expense) TxnMuted else TxnDim,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = PlusJakartaSans,
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(TxnField)
                .border(1.dp, TxnBorder, RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TxnFieldRow("Title", title, { title = it })
            TxnChevronField("Category", categoryLabel) {
                categoryPickerMode = CategoryPickerMode.CATEGORY
                showCategoryPicker = true
            }
            TxnChevronField("Sub-category", subLabel) {
                categoryPickerMode = CategoryPickerMode.SUBCATEGORY
                showCategoryPicker = true
            }
            TxnFieldRow("Date (ISO)", effectiveAtIso, { effectiveAtIso = it })
            val accountLabel = accounts.firstOrNull { it.financialAccountId == selectedAccountId }?.accountName
                ?: accounts.firstOrNull()?.accountName
                ?: "Select account"
            TxnChevronField("Account", accountLabel) {
                showAccountPicker = true
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Payment Method", color = TxnDim, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PaymentMethods.forEach { method ->
                        val selected = paymentMethod == method
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (selected) TxnPurple.copy(alpha = 0.2f) else TxnField)
                                .border(1.dp, if (selected) TxnPurple else TxnBorder, RoundedCornerShape(999.dp))
                                .clickable { paymentMethod = method }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Text(method, color = if (selected) TxnPurple else TxnText, fontSize = 11.sp, fontFamily = PlusJakartaSans)
                        }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("TAGS", color = TxnDim, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TagOptions.forEach { tag ->
                    val selected = tag in selectedTags
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (selected) TxnPurple.copy(alpha = 0.2f) else TxnField)
                            .border(1.dp, if (selected) TxnPurple else TxnBorder, RoundedCornerShape(999.dp))
                            .clickable { selectedTags = if (selected) selectedTags - tag else selectedTags + tag }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text(tag, color = if (selected) TxnPurple else TxnText, fontSize = 12.sp, fontFamily = PlusJakartaSans)
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("NOTES", color = TxnDim, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
            BasicTextField(
                value = notes,
                onValueChange = { notes = it.take(200) },
                textStyle = TextStyle(color = TxnText, fontSize = 14.sp, fontFamily = PlusJakartaSans),
                cursorBrush = SolidColor(TxnPurple),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(TxnField)
                    .border(1.dp, TxnBorder, RoundedCornerShape(14.dp))
                    .padding(12.dp),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Recurring", color = TxnText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
                Text("Manage via recurring schedules", color = TxnDim, fontSize = 11.sp, fontFamily = PlusJakartaSans)
            }
            Switch(
                checked = recurring,
                onCheckedChange = {
                    recurring = it
                    if (it) {
                        Toast.makeText(context, "Create a schedule from Master Expense", Toast.LENGTH_SHORT).show()
                        recurring = false
                    }
                },
                colors = SwitchDefaults.colors(checkedTrackColor = TxnPurple),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("ATTACHMENTS", color = TxnDim, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(TxnPurple.copy(alpha = 0.08f))
                    .border(1.dp, TxnPurple.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .clickable { showUpload = true }
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("📎", fontSize = 24.sp)
                    Text("Add attachment", color = TxnText, fontSize = 14.sp, fontFamily = PlusJakartaSans)
                    Text("Upload persists to backend when complete", color = TxnDim, fontSize = 11.sp, fontFamily = PlusJakartaSans)
                }
            }
            attachments.forEach { label ->
                Text("• $label", color = TxnMuted, fontSize = 12.sp, fontFamily = PlusJakartaSans)
            }

        if (loadingDetail) {
            Text("Loading transaction…", color = TxnMuted, fontSize = 12.sp, fontFamily = PlusJakartaSans)
        }
        }

        error?.let { Text(it, color = TxnRed, fontSize = 12.sp, fontFamily = PlusJakartaSans) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(TxnRed.copy(alpha = 0.12f))
                .border(1.dp, TxnRed.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                .clickable(enabled = !submitting) { showDeleteConfirm = true }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Delete Transaction", color = TxnRed, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PlusJakartaSans)
        }

        if (showDeleteConfirm) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(TxnField)
                        .clickable { showDeleteConfirm = false }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("Cancel", color = TxnMuted, fontFamily = PlusJakartaSans) }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(TxnRed)
                        .clickable {
                            submitting = true
                            scope.launch {
                                repository.voidExpense(txnRef).fold(
                                    onSuccess = {
                                        submitting = false
                                        onDeleted()
                                    },
                                    onFailure = { e ->
                                        submitting = false
                                        error = e.message
                                        showDeleteConfirm = false
                                    },
                                )
                            }
                        }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("Void", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans) }
            }
        }

        Spacer(Modifier.height(12.dp))
    }

    PersonalCategoryPickerSheet(
        visible = showCategoryPicker,
        mode = categoryPickerMode,
        selectedCategoryCode = categoryCode,
        selectedSubcategoryCode = subcategoryCode,
        onDismiss = { showCategoryPicker = false },
        onSelect = { cat, sub ->
            categoryCode = cat
            subcategoryCode = sub
            showCategoryPicker = false
        },
    )

    PersonalAccountPickerSheet(
        visible = showAccountPicker,
        selectedAccountId = selectedAccountId,
        onDismiss = { showAccountPicker = false },
        onSelect = { account ->
            accounts = (accounts.filter { it.financialAccountId != account.financialAccountId } + account)
                .distinctBy { it.financialAccountId }
            selectedAccountId = account.financialAccountId
            if (paymentMethod == "CASH") {
                paymentMethod = PersonalFinancialAccountUi.paymentMethodForAccountType(account.accountType)
            }
            showAccountPicker = false
        },
        repository = sliceRepo,
    )

    PersonalUploadAttachmentSheet(
        visible = showUpload,
        momentId = momentId,
        expenseId = expenseId,
        onDismiss = { showUpload = false },
        onUploaded = { uploadId -> attachments = attachments + uploadId },
        repository = repository,
    )
}

@Composable
private fun TxnFieldRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    readOnly: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = TxnDim, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            readOnly = readOnly,
            enabled = !readOnly,
            singleLine = true,
            textStyle = TextStyle(color = if (readOnly) TxnMuted else TxnText, fontSize = 14.sp, fontFamily = PlusJakartaSans),
            cursorBrush = SolidColor(TxnPurple),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(TxnBg)
                .border(1.dp, TxnBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun TxnChevronField(label: String, value: String, onClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = TxnDim, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(TxnBg)
                .border(1.dp, TxnBorder, RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(value, color = TxnText, fontSize = 14.sp, fontFamily = PlusJakartaSans)
            Text("›", color = TxnMuted, fontSize = 18.sp)
        }
    }
}

private fun formatTxnDate(iso: String): String = try {
    val instant = Instant.parse(iso)
    DateTimeFormatter.ofPattern("EEE, MMM d · h:mm a", Locale.getDefault())
        .withZone(ZoneId.systemDefault())
        .format(instant)
} catch (_: Exception) {
    iso
}

private fun stripStructuredSuffix(description: String): String {
    val idx = description.indexOf(" · Tags:")
    return if (idx >= 0) description.substring(0, idx) else description
}

private fun buildDescription(notes: String, tags: String?): String? {
    val parts = listOfNotNull(
        notes.ifBlank { null },
        tags?.let { "Tags: $it" },
    )
    return parts.joinToString(" · ").ifBlank { null }
}
