package com.example.momentra.ui.shell.personal

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backspace
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.FinancialAccountDto
import com.example.momentra.data.repository.PersonalSliceRepository
import com.example.momentra.ui.shell.maestro.MaestroIds
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.launch

enum class MoneyQuickAddKind {
    MASTER_EXPENSE,
    INCOME,
    TRANSFER,
    SAVINGS,
}

private enum class AccountPickerTarget { FROM, TO }

private data class SavingsGoal(
    val id: String,
    val emoji: String,
    val name: String,
    val targetLabel: String,
)

private val savingsGoals = listOf(
    SavingsGoal("house", "🏠", "House Fund", "₹5,00,000 target"),
    SavingsGoal("travel", "✈️", "Travel Fund", "₹1,50,000 target"),
    SavingsGoal("education", "🎓", "Education", "₹2,00,000 target"),
)

private val MoneyBg = Color(0xFF14121B)
private val MoneySurface = Color(0xFF201E28)
private val MoneyText = Color(0xFFE5E0EE)
private val MoneySecondary = Color(0xFFC9C4D8)
private val MoneyBrand = Color(0xFFC9BFFF)
private val MoneyGreen = Color(0xFF10B981)
private val MoneyBlue = Color(0xFF3B82F6)
private val MoneyTeal = Color(0xFF14B8A6)
private val MoneyBorder = Color(0xFF938EA1)
private val MoneyCardBorder = Color.White.copy(alpha = 0.08f)

private fun accentFor(kind: MoneyQuickAddKind): Color = when (kind) {
    MoneyQuickAddKind.INCOME -> MoneyGreen
    MoneyQuickAddKind.TRANSFER -> MoneyBlue
    MoneyQuickAddKind.SAVINGS -> MoneyTeal
    MoneyQuickAddKind.MASTER_EXPENSE -> MoneyGreen
}

private fun ambientBrushFor(kind: MoneyQuickAddKind): Brush =
    Brush.verticalGradient(listOf(accentFor(kind).copy(alpha = 0.18f), Color.Transparent))

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PersonalMoneyQuickAddSheet(
    kind: MoneyQuickAddKind,
    momentId: String,
    visible: Boolean,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    repository: PersonalSliceRepository = remember { PersonalSliceRepository() },
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var activeKind by remember(kind) { mutableStateOf(kind) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MoneyBg,
        dragHandle = null,
    ) {
        MoneySheetBody(
            activeKind = activeKind,
            onKindChange = { activeKind = it },
            momentId = momentId,
            onDismiss = onDismiss,
            onSaved = onSaved,
            repository = repository,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MoneySheetBody(
    activeKind: MoneyQuickAddKind,
    onKindChange: (MoneyQuickAddKind) -> Unit,
    momentId: String,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    repository: PersonalSliceRepository,
) {
    val accent = accentFor(activeKind)
    var accounts by remember { mutableStateOf<List<FinancialAccountDto>>(emptyList()) }
    var fromId by remember { mutableStateOf<String?>(null) }
    var toId by remember { mutableStateOf<String?>(null) }
    var showAccountPicker by remember { mutableStateOf(false) }
    var accountPickerTarget by remember { mutableStateOf(AccountPickerTarget.FROM) }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(PersonalExpenseCategoryCatalog.masterCategories.first().code) }
    var subcategory by remember {
        mutableStateOf(PersonalExpenseCategoryCatalog.masterCategories.first().subcategories.first().code)
    }
    var financialImpact by remember { mutableStateOf("Essential") }
    var transferType by remember { mutableStateOf("One-time") }
    var frequency by remember { mutableStateOf("One-time") }
    var selectedGoalId by remember { mutableStateOf(savingsGoals.first().id) }
    var whenCode by remember { mutableStateOf("Now") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(momentId) {
        repository.listFinancialAccounts().fold(
            onSuccess = { list ->
                accounts = list
                fromId = list.firstOrNull()?.financialAccountId
                toId = list.getOrNull(1)?.financialAccountId ?: list.firstOrNull()?.financialAccountId
            },
            onFailure = { },
        )
    }

    val selectedCategory = PersonalExpenseCategoryCatalog.masterCategories.find { it.code == category }
        ?: PersonalExpenseCategoryCatalog.masterCategories.first()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(ambientBrushFor(activeKind)),
        )
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Intelligence OS", color = MoneyText, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PlusJakartaSans)
                    Text("Record what shapes how your day runs.", color = MoneySecondary, fontSize = 12.sp, fontFamily = PlusJakartaSans)
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(MoneySurface)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("2 Entries Today", color = MoneySecondary, fontSize = 11.sp, fontFamily = PlusJakartaSans)
                }
            }
            Spacer(Modifier.height(12.dp))
            MoneyTabBar(activeKind, onKindChange)
            Spacer(Modifier.height(16.dp))

            AnimatedContent(
                targetState = activeKind,
                transitionSpec = { fadeIn(spring()) togetherWith fadeOut(spring()) },
                label = "moneyTab",
            ) { tab ->
                val tabAccent = accentFor(tab)
                when (tab) {
                    MoneyQuickAddKind.INCOME -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Income", color = MoneyText, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                            Text("Track money coming in and financial relief.", color = MoneySecondary, fontSize = 12.sp, fontFamily = PlusJakartaSans)
                            TitleField(title, { title = it })
                            AmountField(amount, { amount = it }, tabAccent, MaestroIds.PERSONAL_INCOME_AMOUNT)
                            PersonalAccountSelectRow(
                                label = "PAID FROM",
                                account = accounts.firstOrNull { it.financialAccountId == fromId },
                                onClick = {
                                    accountPickerTarget = AccountPickerTarget.FROM
                                    showAccountPicker = true
                                },
                                testTag = MaestroIds.PERSONAL_INCOME_ACCOUNT,
                            )
                            CategoryChips(selectedCategory.code, { code ->
                                category = code
                                PersonalExpenseCategoryCatalog.masterCategories.find { it.code == code }?.subcategories?.firstOrNull()?.let {
                                    subcategory = it.code
                                }
                            }, tabAccent, MaestroIds.PERSONAL_INCOME_CATEGORY)
                            SubcategoryChips(selectedCategory, subcategory, { subcategory = it }, tabAccent, MaestroIds.PERSONAL_INCOME_SUBCATEGORY)
                            FieldLabel("FINANCIAL IMPACT")
                            LoSimpleChips(listOf("Essential", "Planned", "Unplanned"), financialImpact, { financialImpact = it }, tabAccent)
                            WhenChips(whenCode, { whenCode = it }, tabAccent)
                            SaveButton(
                                label = "Save Income ✓",
                                accent = tabAccent,
                                enabled = amount.isNotBlank() && !submitting,
                                submitting = submitting,
                                testTag = MaestroIds.PERSONAL_INCOME_SUBMIT,
                            ) {
                                submitting = true
                                scope.launch {
                                    repository.createPersonalIncome(
                                        momentId = momentId,
                                        amount = amount.trim(),
                                        currencyCode = "INR",
                                        description = buildIncomeDescription(title, note, financialImpact, subcategory, whenCode),
                                        merchantName = title.ifBlank { null },
                                        categoryCode = category,
                                        financialAccountId = fromId,
                                    ).fold(
                                        onSuccess = { submitting = false; onSaved(); onDismiss() },
                                        onFailure = { e -> submitting = false; error = e.message },
                                    )
                                }
                            }
                        }
                    }
                    MoneyQuickAddKind.TRANSFER -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Transfer", color = MoneyText, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                            Text("Move money between your accounts.", color = MoneySecondary, fontSize = 12.sp, fontFamily = PlusJakartaSans)
                            FieldLabel("FROM")
                            PersonalAccountSelectRow(
                                label = "FROM",
                                account = accounts.firstOrNull { it.financialAccountId == fromId },
                                onClick = {
                                    accountPickerTarget = AccountPickerTarget.FROM
                                    showAccountPicker = true
                                },
                            )
                            PersonalAccountSelectRow(
                                label = "TO",
                                account = accounts.firstOrNull { it.financialAccountId == toId },
                                onClick = {
                                    accountPickerTarget = AccountPickerTarget.TO
                                    showAccountPicker = true
                                },
                            )
                            AmountField(amount, { amount = it }, tabAccent, MaestroIds.PERSONAL_MONEY_TRANSFER_AMOUNT)
                            NoteField(note, { note = it }, MaestroIds.PERSONAL_MONEY_TRANSFER_NOTE)
                            FieldLabel("TRANSFER TYPE")
                            LoSimpleChips(listOf("One-time", "Recurring"), transferType, { transferType = it }, tabAccent)
                            WhenChips(whenCode, { whenCode = it }, tabAccent)
                            SaveButton(
                                label = "Transfer Now ✓",
                                accent = tabAccent,
                                enabled = amount.isNotBlank() && !submitting,
                                submitting = submitting,
                                testTag = MaestroIds.PERSONAL_MONEY_TRANSFER_SUBMIT,
                            ) {
                                submitting = true
                                scope.launch {
                                    repository.createMovement(
                                        momentId = momentId,
                                        movementType = "TRANSFER",
                                        amount = amount.trim(),
                                        currencyCode = "INR",
                                        accountId = fromId,
                                        description = note.ifBlank { "Transfer · $transferType · $whenCode" },
                                    ).fold(
                                        onSuccess = { submitting = false; onSaved(); onDismiss() },
                                        onFailure = { e -> submitting = false; error = e.message },
                                    )
                                }
                            }
                        }
                    }
                    MoneyQuickAddKind.SAVINGS -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Savings", color = MoneyText, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                            Text("Move money toward your goals.", color = MoneySecondary, fontSize = 12.sp, fontFamily = PlusJakartaSans)
                            FieldLabel("SAVINGS GOAL")
                            savingsGoals.forEach { goal ->
                                SavingsGoalCard(goal, selectedGoalId == goal.id, tabAccent) { selectedGoalId = goal.id }
                            }
                            FieldLabel("DEPOSIT AMOUNT")
                            AmountField(amount, { amount = it }, tabAccent, MaestroIds.PERSONAL_MONEY_SAVINGS_AMOUNT)
                            PersonalAccountSelectRow(
                                label = "DEPOSIT FROM",
                                account = accounts.firstOrNull { it.financialAccountId == fromId },
                                onClick = {
                                    accountPickerTarget = AccountPickerTarget.FROM
                                    showAccountPicker = true
                                },
                                testTag = MaestroIds.PERSONAL_MONEY_SAVINGS_ACCOUNT,
                            )
                            FieldLabel("FREQUENCY")
                            LoSimpleChips(listOf("One-time", "Weekly", "Monthly"), frequency, { frequency = it }, tabAccent)
                            WhenChips(whenCode, { whenCode = it }, tabAccent)
                            SaveButton(
                                label = "Save Now ✓",
                                accent = tabAccent,
                                enabled = amount.isNotBlank() && !submitting,
                                submitting = submitting,
                                testTag = MaestroIds.PERSONAL_MONEY_SAVINGS_SUBMIT,
                            ) {
                                submitting = true
                                val goalName = savingsGoals.find { it.id == selectedGoalId }?.name ?: "Savings"
                                scope.launch {
                                    repository.createMovement(
                                        momentId = momentId,
                                        movementType = "SAVINGS_DEPOSIT",
                                        amount = amount.trim(),
                                        currencyCode = "INR",
                                        accountId = fromId,
                                        description = note.ifBlank { "$goalName · $frequency · $whenCode" },
                                    ).fold(
                                        onSuccess = { submitting = false; onSaved(); onDismiss() },
                                        onFailure = { e -> submitting = false; error = e.message },
                                    )
                                }
                            }
                        }
                    }
                    MoneyQuickAddKind.MASTER_EXPENSE -> Unit
                }
            }
            error?.let { Text(it, color = Color(0xFFF87171), fontSize = 12.sp, fontFamily = PlusJakartaSans) }
        }
    }

    PersonalAccountPickerSheet(
        visible = showAccountPicker,
        selectedAccountId = if (accountPickerTarget == AccountPickerTarget.FROM) fromId else toId,
        excludeAccountId = if (accountPickerTarget == AccountPickerTarget.TO) fromId else null,
        onDismiss = { showAccountPicker = false },
        onSelect = { account ->
            accounts = (accounts.filter { it.financialAccountId != account.financialAccountId } + account)
                .distinctBy { it.financialAccountId }
            when (accountPickerTarget) {
                AccountPickerTarget.FROM -> {
                    fromId = account.financialAccountId
                    if (toId == account.financialAccountId) {
                        toId = accounts.firstOrNull { it.financialAccountId != fromId }?.financialAccountId
                    }
                }
                AccountPickerTarget.TO -> toId = account.financialAccountId
            }
            showAccountPicker = false
        },
        repository = repository,
    )
}

private fun buildIncomeDescription(
    title: String,
    note: String,
    financialImpact: String,
    subcategory: String,
    whenCode: String,
): String {
    return buildString {
        if (title.isNotBlank()) append(title.trim())
        if (note.isNotBlank()) {
            if (isNotEmpty()) append(" · ")
            append(note.trim())
        }
        if (isNotEmpty()) append(" · ")
        append(financialImpact)
        append(" · ")
        append(PersonalExpenseCategoryCatalog.subcategoryLabel(subcategory) ?: subcategory)
        append(" · ")
        append(whenCode)
    }
}

@Composable
private fun MoneyTabBar(active: MoneyQuickAddKind, onSelect: (MoneyQuickAddKind) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            Triple(MoneyQuickAddKind.INCOME, "📈", "Income"),
            Triple(MoneyQuickAddKind.TRANSFER, "💸", "Transfer"),
            Triple(MoneyQuickAddKind.SAVINGS, "🌱", "Savings"),
        ).forEach { (kind, emoji, label) ->
            val selected = active == kind
            val tabAccent = accentFor(kind)
            val bg by animateColorAsState(if (selected) tabAccent.copy(alpha = 0.2f) else MoneySurface, label = "tab")
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg)
                    .border(1.dp, if (selected) tabAccent else MoneyCardBorder, RoundedCornerShape(12.dp))
                    .clickable { onSelect(kind) }
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("$emoji ", fontSize = 14.sp)
                Text(
                    label,
                    color = if (selected) MoneyText else MoneySecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, color = MoneyBrand, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans, letterSpacing = 0.8.sp)
}

@Composable
private fun AmountField(value: String, onChange: (String) -> Unit, accent: Color, testTag: String) {
    FieldLabel("AMOUNT")
    Spacer(Modifier.height(6.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MoneySurface)
            .border(1.dp, MoneyBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("₹", color = MoneySecondary, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
        Spacer(Modifier.width(8.dp))
        BasicTextField(
            value = value,
            onValueChange = { onChange(it.filter { c -> c.isDigit() || c == '.' }) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = TextStyle(color = MoneyText, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PlusJakartaSans),
            cursorBrush = SolidColor(MoneyBrand),
            modifier = Modifier.weight(1f),
        )
        if (value.isNotEmpty()) {
            Icon(Icons.Outlined.Backspace, null, tint = MoneySecondary, modifier = Modifier.size(22.dp).clickable { onChange(value.dropLast(1)) })
        }
    }
}

@Composable
private fun TitleField(value: String, onChange: (String) -> Unit) {
    FieldLabel("TITLE")
    Spacer(Modifier.height(6.dp))
    BasicTextField(
        value = value,
        onValueChange = onChange,
        textStyle = TextStyle(color = MoneyText, fontSize = 15.sp, fontFamily = PlusJakartaSans),
        cursorBrush = SolidColor(MoneyBrand),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MoneySurface)
            .border(1.dp, MoneyBorder, RoundedCornerShape(14.dp))
            .padding(14.dp),
        decorationBox = { inner ->
            if (value.isEmpty()) Text("Salary deposit", color = MoneySecondary, fontSize = 15.sp)
            inner()
        },
    )
}

@Composable
private fun NoteField(value: String, onChange: (String) -> Unit, testTag: String) {
    FieldLabel("NOTE")
    Spacer(Modifier.height(6.dp))
    BasicTextField(
        value = value,
        onValueChange = onChange,
        textStyle = TextStyle(color = MoneyText, fontSize = 14.sp, fontFamily = PlusJakartaSans),
        cursorBrush = SolidColor(MoneyBrand),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MoneySurface)
            .border(1.dp, MoneyBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
            .testTag(testTag),
        decorationBox = { inner ->
            if (value.isEmpty()) Text("Add a note...", color = MoneySecondary, fontSize = 14.sp)
            inner()
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryChips(selected: String, onSelect: (String) -> Unit, accent: Color, testTag: String) {
    FieldLabel("CATEGORY")
    Spacer(Modifier.height(6.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PersonalExpenseCategoryCatalog.masterCategories.forEach { cat ->
            val sel = selected == cat.code
            val bg by animateColorAsState(if (sel) accent else MoneySurface, label = "cat")
            Text(
                "${cat.emoji} ${cat.label}",
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(bg)
                    .border(1.dp, if (sel) accent else MoneyCardBorder, RoundedCornerShape(999.dp))
                    .clickable { onSelect(cat.code) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag(testTag),
                color = if (sel) Color.White else MoneyText,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubcategoryChips(
    category: PersonalExpenseCategoryCatalog.Category,
    selected: String,
    onSelect: (String) -> Unit,
    accent: Color,
    testTag: String,
) {
    FieldLabel("SUBCATEGORY")
    Spacer(Modifier.height(6.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        category.subcategories.forEach { sub ->
            val sel = selected == sub.code
            val bg by animateColorAsState(if (sel) accent else MoneySurface, label = "sub")
            Text(
                sub.label,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(bg)
                    .border(1.dp, if (sel) accent else MoneyCardBorder, RoundedCornerShape(999.dp))
                    .clickable { onSelect(sub.code) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag(testTag),
                color = if (sel) Color.White else MoneyText,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WhenChips(selected: String, onSelect: (String) -> Unit, accent: Color) {
    FieldLabel("WHEN")
    Spacer(Modifier.height(6.dp))
    LoSimpleChips(listOf("Now", "Today", "Yesterday", "Change"), selected, onSelect, accent)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LoSimpleChips(options: List<String>, selected: String, onSelect: (String) -> Unit, accent: Color) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { opt ->
            val sel = selected == opt
            val bg by animateColorAsState(if (sel) accent else MoneySurface, label = "chip")
            Text(
                opt,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(bg)
                    .border(1.dp, if (sel) accent else MoneyCardBorder, RoundedCornerShape(999.dp))
                    .clickable { onSelect(opt) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                color = if (sel) Color.White else MoneyText,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
        }
    }
}

@Composable
private fun SavingsGoalCard(goal: SavingsGoal, selected: Boolean, accent: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) MoneySurface else MoneyBg)
            .border(if (selected) 2.dp else 1.dp, if (selected) accent else MoneyBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(goal.emoji, fontSize = 20.sp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(goal.name, color = MoneyText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
                Text(goal.targetLabel, color = MoneySecondary, fontSize = 12.sp, fontFamily = PlusJakartaSans)
            }
        }
        if (selected) {
            Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(accent), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Check, null, tint = Color.White, modifier = Modifier.size(12.dp))
            }
        } else {
            Box(modifier = Modifier.size(20.dp).border(2.dp, MoneyBorder, CircleShape))
        }
    }
}

@Composable
private fun SaveButton(
    label: String,
    accent: Color,
    enabled: Boolean,
    submitting: Boolean,
    testTag: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) accent else accent.copy(alpha = 0.5f))
            .testTag(testTag)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(14.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (submitting) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        } else {
            Text(label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PlusJakartaSans)
        }
    }
}
