package com.example.momentra.ui.shell.personal.shared

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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.repository.PersonalSliceRepository
import com.example.momentra.ui.shell.perf.ShellPerf
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val T = PersonalMasterExpenseTheme

/** Figma 453:9376 Master Expense — personal expense create premium layout. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PersonalMasterExpenseSheet(
    momentId: String,
    visible: Boolean,
    pulseFamily: PersonalPulseFamily = PersonalPulseFamily.LIFE_OPERATIONS,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    repository: PersonalSliceRepository = remember { PersonalSliceRepository() },
) {
    if (!visible) return
    @Suppress("UNUSED_PARAMETER")
    val unusedFamily = pulseFamily
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var purpose by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var categoryCode by remember { mutableStateOf(PersonalExpenseCategoryCatalog.masterCategories.first().code) }
    var paidFrom by remember { mutableStateOf("Primary") }
    var whenCode by remember { mutableStateOf("Today") }
    var showDetails by remember { mutableStateOf(true) }
    var selectedFeelings by remember { mutableStateOf(setOf<String>()) }
    var meaningfulness by remember { mutableStateOf("Medium") }
    var memorability by remember { mutableStateOf("High") }
    var sharedExperience by remember { mutableStateOf(true) }
    var sharedWith by remember { mutableStateOf(setOf<String>()) }
    var relationshipImpact by remember { mutableStateOf(setOf<String>()) }
    var reasoning by remember { mutableStateOf(setOf<String>()) }
    var notes by remember { mutableStateOf("") }
    var accounts by remember { mutableStateOf<List<com.example.momentra.data.api.FinancialAccountDto>>(emptyList()) }
    var selectedAccountId by remember { mutableStateOf<String?>(null) }
    var showAccountPicker by remember { mutableStateOf(false) }
    var showWhenMenu by remember { mutableStateOf(false) }
    var paymentMethod by remember { mutableStateOf("CASH") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(momentId) {
        repository.listFinancialAccounts().fold(
            onSuccess = {
                accounts = it
                selectedAccountId = it.firstOrNull()?.financialAccountId
                paidFrom = it.firstOrNull()?.accountName ?: "Primary"
            },
            onFailure = { },
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = T.Bg,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            MeHeader(
                onDismiss = onDismiss,
                onClearAll = {
                    purpose = ""
                    amount = ""
                    categoryCode = PersonalExpenseCategoryCatalog.masterCategories.first().code
                    notes = ""
                    selectedFeelings = emptySet()
                    relationshipImpact = emptySet()
                    sharedWith = emptySet()
                    reasoning = emptySet()
                    whenCode = "Today"
                    meaningfulness = "Medium"
                    memorability = "High"
                    sharedExperience = true
                },
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "One expense. Impact across your life.",
                    color = T.Muted,
                    fontSize = 14.sp,
                    fontFamily = PlusJakartaSans,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(Brush.horizontalGradient(listOf(T.Accent, T.AccentLight))),
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(T.Accent.copy(alpha = 0.08f))
                    .border(1.dp, T.Accent, RoundedCornerShape(16.dp))
                    .padding(16.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    MeIcon(
                        icon = PersonalMasterExpenseIcons.Chrome.Info.vector,
                        contentDescription = null,
                        tint = T.Accent,
                        size = 18.dp,
                    )
                    Text(
                        "This entry can update Life operations, Lifestyle and Relationships.",
                        color = T.Text,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontFamily = PlusJakartaSans,
                    )
                }
            }

            MeSectionLabel("What did you spend on?")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(T.SurfaceSolid.copy(alpha = 0.7f))
                    .border(1.dp, T.Border, RoundedCornerShape(16.dp))
                    .padding(18.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MeIcon(
                        icon = PersonalMasterExpenseIcons.Chrome.Edit.vector,
                        contentDescription = null,
                        tint = T.Accent,
                        size = 18.dp,
                    )
                    BasicTextField(
                        value = purpose,
                        onValueChange = { purpose = it },
                        textStyle = TextStyle(
                            color = T.TextMain,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = PlusJakartaSans,
                        ),
                        cursorBrush = SolidColor(T.Accent),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            if (purpose.isEmpty()) {
                                Text(
                                    "Dinner with friends",
                                    color = T.Muted,
                                    fontSize = 15.sp,
                                    fontFamily = PlusJakartaSans,
                                )
                            }
                            inner()
                        },
                    )
                }
            }

            MeSectionLabel("Amount")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(T.SurfaceSolid.copy(alpha = 0.7f))
                    .border(1.dp, T.Border, RoundedCornerShape(16.dp))
                    .padding(20.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(T.Accent),
                        contentAlignment = Alignment.Center,
                    ) {
                        MeIcon(
                            icon = PersonalMasterExpenseIcons.Chrome.Amount.vector,
                            contentDescription = null,
                            tint = T.Text,
                            size = 18.dp,
                        )
                    }
                    BasicTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        textStyle = TextStyle(
                            color = T.Text,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = PlusJakartaSans,
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        cursorBrush = SolidColor(T.Accent),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            if (amount.isEmpty()) {
                                Text(
                                    "0.00",
                                    color = T.Text,
                                    fontSize = 40.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = PlusJakartaSans,
                                )
                            }
                            inner()
                        },
                    )
                }
            }

            MeSectionLabel("Category")
            CategoryGrid(
                selectedCode = categoryCode,
                onSelect = { categoryCode = it },
            )

            MeSectionLabel("Paid From")
            MeRowCard(
                icon = accounts.firstOrNull { it.financialAccountId == selectedAccountId }
                    ?.let { PersonalFinancialAccountUi.nativeIconForType(it.accountType) }
                    ?: PersonalFinancialAccountUi.nativeIconForType("BANK"),
                label = accounts.firstOrNull { it.financialAccountId == selectedAccountId }?.accountName
                    ?: paidFrom,
                onChange = { showAccountPicker = true },
            )

            MeSectionLabel("When")
            Box {
                MeRowCard(
                    icon = PersonalMasterExpenseIcons.Chrome.Calendar.vector,
                    label = formatWhenLabel(whenCode),
                    onChange = { showWhenMenu = true },
                )
                DropdownMenu(
                    expanded = showWhenMenu,
                    onDismissRequest = { showWhenMenu = false },
                ) {
                    T.whenOptions.forEach { opt ->
                        DropdownMenuItem(
                            text = { Text(opt, fontFamily = PlusJakartaSans) },
                            onClick = {
                                whenCode = opt
                                showWhenMenu = false
                            },
                        )
                    }
                }
            }

            MoreDetailsSection(
                expanded = showDetails,
                onToggle = { showDetails = !showDetails },
                selectedFeelings = selectedFeelings,
                onFeelingToggle = { label ->
                    selectedFeelings = if (label in selectedFeelings) selectedFeelings - label else selectedFeelings + label
                },
                meaningfulness = meaningfulness,
                onMeaningfulness = { meaningfulness = it },
                memorability = memorability,
                onMemorability = { memorability = it },
                sharedExperience = sharedExperience,
                onSharedExperience = { sharedExperience = it },
                sharedWith = sharedWith,
                onSharedWithToggle = { label ->
                    sharedWith = if (label in sharedWith) sharedWith - label else sharedWith + label
                },
                relationshipImpact = relationshipImpact,
                onRelationshipToggle = { label ->
                    relationshipImpact = if (label in relationshipImpact) relationshipImpact - label else relationshipImpact + label
                },
                reasoning = reasoning,
                onReasoningToggle = { label ->
                    reasoning = if (label in reasoning) reasoning - label else reasoning + label
                },
            )

            MeSectionLabel("Notes")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(T.SurfaceSolid.copy(alpha = 0.7f))
                    .border(1.dp, T.Border, RoundedCornerShape(16.dp))
                    .padding(18.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                            MeIcon(
                        icon = PersonalMasterExpenseIcons.Chrome.Edit.vector,
                        contentDescription = null,
                        tint = T.Accent,
                        size = 18.dp,
                    )
                            BasicTextField(
                                value = notes,
                                onValueChange = { if (it.length <= 200) notes = it },
                                textStyle = TextStyle(
                                    color = T.TextMain,
                                    fontSize = 15.sp,
                                    fontFamily = PlusJakartaSans,
                                ),
                                cursorBrush = SolidColor(T.Accent),
                                modifier = Modifier.weight(1f),
                                decorationBox = { inner ->
                                    if (notes.isEmpty()) {
                                        Text(
                                            "Add any additional notes...",
                                            color = T.Muted,
                                            fontSize = 15.sp,
                                            fontFamily = PlusJakartaSans,
                                        )
                                    }
                                    inner()
                                },
                            )
                        }
                    }
                    Text(
                        "${notes.length}/200",
                        color = T.Muted,
                        fontSize = 12.sp,
                        fontFamily = PlusJakartaSans,
                        modifier = Modifier.align(Alignment.End),
                    )
                }
            }

            error?.let {
                Text(it, color = T.Error, fontSize = 12.sp, fontFamily = PlusJakartaSans)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Cancel",
                        color = T.TextMain,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = PlusJakartaSans,
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .alpha(if (amount.isNotBlank() && !submitting) 1f else 0.5f)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, T.Border, RoundedCornerShape(14.dp))
                        .clickable(enabled = amount.isNotBlank() && !submitting) {
                            submitting = true
                            error = null
                            scope.launch {
                                repository.createExpense(
                                    momentId = momentId,
                                    amount = amount.trim(),
                                    currencyCode = "INR",
                                    merchantName = purpose.trim().ifBlank { null },
                                    description = notes.trim().ifBlank { null },
                                    categoryCode = categoryCode,
                                    financialAccountId = selectedAccountId,
                                    paymentMethodCode = paymentMethod,
                                    effectiveAt = effectiveAtFromWhen(whenCode),
                                    asDraft = true,
                                ).fold(
                                    onSuccess = {
                                        submitting = false
                                        onSaved()
                                        onDismiss()
                                    },
                                    onFailure = { e ->
                                        submitting = false
                                        error = e.message
                                    },
                                )
                            }
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Save draft",
                        color = T.TextMain,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = PlusJakartaSans,
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .alpha(if (amount.isNotBlank() && !submitting) 1f else 0.5f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(T.Accent)
                        .clickable(enabled = amount.isNotBlank() && !submitting) {
                            submitting = true
                            error = null
                            val description = buildMasterDescription(
                                notes = notes.trim(),
                                feelings = selectedFeelings,
                                meaningfulness = meaningfulness,
                                memorability = memorability,
                                shared = sharedExperience,
                                sharedWith = sharedWith,
                                relationship = relationshipImpact,
                                reasoning = reasoning,
                                whenCode = whenCode,
                                paidFrom = paidFrom,
                            )
                            scope.launch {
                                val submitMark = ShellPerf.start("expense_submit")
                                val sub = PersonalExpenseCategoryCatalog.masterCategories
                                    .firstOrNull { it.code == categoryCode }
                                    ?.subcategories?.firstOrNull()?.code
                                repository.createExpense(
                                    momentId = momentId,
                                    amount = amount.trim(),
                                    currencyCode = "INR",
                                    merchantName = purpose.trim().ifBlank { null },
                                    description = description,
                                    categoryCode = categoryCode,
                                    subcategoryCode = sub,
                                    financialAccountId = selectedAccountId,
                                    paymentMethodCode = paymentMethod,
                                    effectiveAt = effectiveAtFromWhen(whenCode),
                                    asDraft = false,
                                ).fold(
                                    onSuccess = {
                                        submitting = false
                                        ShellPerf.end(submitMark, mapOf("context" to "PERSONAL"))
                                        onSaved()
                                        onDismiss()
                                    },
                                    onFailure = { e ->
                                        submitting = false
                                        error = e.message
                                    },
                                )
                            }
                        }
                        .padding(vertical = 14.dp)
                        .testTag("master_expense_confirm"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (submitting) "Saving…" else "Confirm Expense ✓",
                        color = T.Text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = PlusJakartaSans,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("🔒", fontSize = 12.sp)
                Spacer(Modifier.width(6.dp))
                Text(
                    "Your details private and secure.",
                    color = T.Muted,
                    fontSize = 12.sp,
                    fontFamily = PlusJakartaSans,
                )
            }

            Spacer(Modifier.height(12.dp))
        }
    }

    PersonalAccountPickerSheet(
        visible = showAccountPicker,
        selectedAccountId = selectedAccountId,
        onDismiss = { showAccountPicker = false },
        onSelect = { account ->
            accounts = (accounts.filter { it.financialAccountId != account.financialAccountId } + account)
                .distinctBy { it.financialAccountId }
            selectedAccountId = account.financialAccountId
            paidFrom = account.accountName
            paymentMethod = PersonalFinancialAccountUi.paymentMethodForAccountType(account.accountType)
            showAccountPicker = false
        },
        repository = repository,
    )
}

@Composable
private fun MeHeader(onDismiss: () -> Unit, onClearAll: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(T.SurfaceSolid.copy(alpha = 0.7f))
                        .border(1.dp, T.Border, RoundedCornerShape(10.dp))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    MeIcon(
                        icon = PersonalMasterExpenseIcons.Chrome.Back.vector,
                        contentDescription = "Back",
                        tint = T.Accent,
                        size = 22.dp,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(T.Accent.copy(alpha = 0.08f))
                            .border(1.dp, T.Accent, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                    ) {
                        MeIcon(
                            icon = PersonalMasterExpenseIcons.Chrome.Header.vector,
                            contentDescription = null,
                            tint = T.Accent,
                            size = 16.dp,
                        )
                    }
                    Text(
                        "Master Expense",
                        color = T.Text,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(T.Accent.copy(alpha = 0.08f))
                    .border(1.dp, T.Accent, RoundedCornerShape(999.dp))
                    .clickable(onClick = onClearAll)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("master_expense_clear_all"),
            ) {
                Text(
                    "Clear All",
                    color = T.Accent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
    }
}

@Composable
private fun MeSectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = T.Accent,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = PlusJakartaSans,
        letterSpacing = 1.2.sp,
    )
}

@Composable
private fun MeRowCard(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onChange: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(T.SurfaceSolid.copy(alpha = 0.7f))
            .border(1.dp, T.Border, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            MeIcon(icon = icon, contentDescription = null, tint = T.Accent, size = 20.dp)
            Text(
                label,
                color = T.TextMain,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = PlusJakartaSans,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(T.Accent.copy(alpha = 0.08f))
                .border(1.dp, T.Accent, RoundedCornerShape(999.dp))
                .clickable(onClick = onChange)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(
                "Change",
                color = T.Accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
        }
    }
}

@Composable
private fun CategoryGrid(selectedCode: String, onSelect: (String) -> Unit) {
    val categories = PersonalExpenseCategoryCatalog.masterCategories
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        categories.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                row.forEach { cat ->
                    val selected = selectedCode == cat.code
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(72.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (selected) T.Accent else T.CategoryUnselected)
                            .border(
                                1.dp,
                                if (selected) T.AccentLight else T.CategoryBorder,
                                RoundedCornerShape(16.dp),
                            )
                            .clickable { onSelect(cat.code) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            MeIcon(
                                icon = PersonalMasterExpenseIcons.categoryIcon(cat.code),
                                contentDescription = cat.label,
                                tint = T.Text,
                                size = 24.dp,
                            )
                            Text(
                                cat.label,
                                color = T.Text,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = PlusJakartaSans,
                            )
                        }
                    }
                }
                if (row.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MoreDetailsSection(
    expanded: Boolean,
    onToggle: () -> Unit,
    selectedFeelings: Set<String>,
    onFeelingToggle: (String) -> Unit,
    meaningfulness: String,
    onMeaningfulness: (String) -> Unit,
    memorability: String,
    onMemorability: (String) -> Unit,
    sharedExperience: Boolean,
    onSharedExperience: (Boolean) -> Unit,
    sharedWith: Set<String>,
    onSharedWithToggle: (String) -> Unit,
    relationshipImpact: Set<String>,
    onRelationshipToggle: (String) -> Unit,
    reasoning: Set<String>,
    onReasoningToggle: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(T.SurfaceSolid.copy(alpha = 0.5f))
            .border(1.dp, T.Border, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                MeIcon(
                    icon = PersonalMasterExpenseIcons.Chrome.Folder.vector,
                    contentDescription = null,
                    tint = T.Accent,
                    size = 16.dp,
                )
                Text(
                    "More details",
                    color = T.TextMain,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                )
            }
            MeIcon(
                icon = if (expanded) {
                    PersonalMasterExpenseIcons.Chrome.ExpandUp.vector
                } else {
                    PersonalMasterExpenseIcons.Chrome.ExpandDown.vector
                },
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = T.Muted,
                size = 12.dp,
            )
        }

        if (expanded) {
            Text(
                "How did this make you feel?",
                color = T.Muted,
                fontSize = 12.sp,
                fontFamily = PlusJakartaSans,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                T.emotionalOptions.forEach { opt ->
                    val on = opt.label in selectedFeelings
                    EmotionalChip(opt.emoji, opt.label, on) { onFeelingToggle(opt.label) }
                }
            }

            MeSegmentControl(
                label = "How meaningful was this experience?",
                options = T.segmentOptions,
                selected = meaningfulness,
                onSelect = onMeaningfulness,
            )
            MeSegmentControl(
                label = "How memorable was this?",
                options = T.segmentOptions,
                selected = memorability,
                onSelect = onMemorability,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("✨", fontSize = 16.sp)
                    Text(
                        "Shared experience",
                        color = T.TextMain,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = PlusJakartaSans,
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (sharedExperience) T.Accent else T.CategoryUnselected)
                        .clickable { onSharedExperience(!sharedExperience) }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Text(
                        if (sharedExperience) "ON" else "OFF",
                        color = T.Text,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                    )
                }
            }

            if (sharedExperience) {
                Text("Shared with", color = T.Muted, fontSize = 12.sp, fontFamily = PlusJakartaSans)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    T.sharedWithOptions.forEach { label ->
                        val on = label in sharedWith
                        MeChip(label, on) { onSharedWithToggle(label) }
                    }
                }

                Text(
                    "What was the impact on this relationship?",
                    color = T.Muted,
                    fontSize = 12.sp,
                    fontFamily = PlusJakartaSans,
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    T.relationshipImpactOptions.forEach { opt ->
                        val on = opt.label in relationshipImpact
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (on) T.Accent.copy(alpha = 0.12f) else T.CategoryUnselected)
                                .border(
                                    1.dp,
                                    if (on) T.Accent else T.CategoryBorder,
                                    RoundedCornerShape(14.dp),
                                )
                                .clickable { onRelationshipToggle(opt.label) }
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(opt.emoji, fontSize = 20.sp)
                            Text(
                                opt.label,
                                color = if (on) T.Accent else T.TextMain,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = PlusJakartaSans,
                            )
                        }
                    }
                }

                Text("Why did this happen?", color = T.Muted, fontSize = 12.sp, fontFamily = PlusJakartaSans)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    T.reasoningOptions.forEach { label ->
                        val on = label in reasoning
                        MeChip(label, on) { onReasoningToggle(label) }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmotionalChip(emoji: String, label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) T.Accent.copy(alpha = 0.15f) else T.CategoryUnselected)
            .border(1.dp, if (selected) T.Accent else T.CategoryBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(emoji, fontSize = 22.sp)
        Text(
            label,
            color = if (selected) T.Accent else T.Muted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = PlusJakartaSans,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun MeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) T.Accent else T.CategoryUnselected)
            .border(1.dp, if (selected) T.AccentLight else T.CategoryBorder, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            label,
            color = T.Text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
        )
    }
}

@Composable
private fun MeSegmentControl(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = T.Muted, fontSize = 12.sp, fontFamily = PlusJakartaSans)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(999.dp))
                .background(T.CategoryUnselected)
                .border(1.dp, T.CategoryBorder, RoundedCornerShape(999.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            options.forEach { opt ->
                val on = selected == opt
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (on) T.Accent else Color.Transparent)
                        .clickable { onSelect(opt) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        opt,
                        color = if (on) T.Text else T.Muted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = PlusJakartaSans,
                    )
                }
            }
        }
    }
}

private fun formatWhenLabel(whenCode: String): String {
    val time = LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a"))
    return when (whenCode) {
        "Yesterday" -> "Yesterday $time"
        "Today" -> "Today $time"
        else -> "Now"
    }
}

private fun effectiveAtFromWhen(whenCode: String): String? = when (whenCode) {
    "Yesterday" -> java.time.Instant.now().minus(java.time.Duration.ofDays(1)).toString()
    "Today", "Now" -> java.time.Instant.now().toString()
    else -> null
}

private fun buildMasterDescription(
    notes: String,
    feelings: Set<String>,
    meaningfulness: String,
    memorability: String,
    shared: Boolean,
    sharedWith: Set<String>,
    relationship: Set<String>,
    reasoning: Set<String>,
    whenCode: String,
    paidFrom: String,
): String? {
    val parts = buildList {
        if (notes.isNotBlank()) add(notes)
        if (feelings.isNotEmpty()) add("Feelings: ${feelings.joinToString(", ")}")
        add("Meaning: $meaningfulness · Memory: $memorability")
        if (shared) {
            add("Shared experience")
            if (sharedWith.isNotEmpty()) add("Shared with: ${sharedWith.joinToString(", ")}")
        }
        if (relationship.isNotEmpty()) add("Relationship impact: ${relationship.joinToString(", ")}")
        if (reasoning.isNotEmpty()) add("Reason: ${reasoning.joinToString(", ")}")
        add("When: $whenCode · Paid from: $paidFrom")
    }
    return parts.joinToString(" · ").ifBlank { null }
}
