package com.example.momentra.ui.shell.personal

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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.momentra.data.repository.PersonalTransactionRepository
import com.example.momentra.data.repository.TransactionDomain
import com.example.momentra.data.repository.TransactionRef
import com.example.momentra.data.repository.TransactionResourceType
import com.example.momentra.ui.shell.maestro.MaestroIds
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.launch
import java.util.UUID

private val Bg = Color(0xFF14121B)
private val Surface = Color(0xFF201E28)
private val TextMain = Color(0xFFE5E0EE)
private val Muted = Color(0xFFC9C4D8)
private val Purple = Color(0xFF7C5CFC)
private val BorderSoft = Color.White.copy(alpha = 0.08f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalCategoryPickerSheet(
    visible: Boolean,
    mode: CategoryPickerMode,
    selectedCategoryCode: String?,
    selectedSubcategoryCode: String?,
    onDismiss: () -> Unit,
    onSelect: (categoryCode: String, subcategoryCode: String?) -> Unit,
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var step by remember(mode, selectedCategoryCode) {
        mutableStateOf(
            if (mode == CategoryPickerMode.SUBCATEGORY && selectedCategoryCode != null) "sub" else "cat",
        )
    }
    var activeCategory by remember(selectedCategoryCode) {
        mutableStateOf(selectedCategoryCode ?: PersonalExpenseCategoryCatalog.masterCategories.first().code)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Bg,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 48.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Muted.copy(alpha = 0.3f)),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (step == "sub") "Sub-category" else "Category",
                    color = TextMain,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                )
                if (step == "sub") {
                    Text(
                        "Back",
                        color = Purple,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = PlusJakartaSans,
                        modifier = Modifier.clickable { step = "cat" },
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (step == "cat") {
                    PersonalExpenseCategoryCatalog.masterCategories.forEach { cat ->
                        val selected = cat.code == selectedCategoryCode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (selected) Purple.copy(alpha = 0.15f) else Surface)
                                .border(1.dp, if (selected) Purple else BorderSoft, RoundedCornerShape(14.dp))
                                .clickable {
                                    activeCategory = cat.code
                                    if (mode == CategoryPickerMode.SUBCATEGORY) {
                                        step = "sub"
                                    } else {
                                        onSelect(cat.code, cat.subcategories.firstOrNull()?.code)
                                        onDismiss()
                                    }
                                }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(cat.emoji, fontSize = 20.sp)
                            Text(cat.label, color = TextMain, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
                        }
                    }
                } else {
                    val cat = PersonalExpenseCategoryCatalog.masterCategories.first { it.code == activeCategory }
                    cat.subcategories.forEach { sub ->
                        val selected = sub.code == selectedSubcategoryCode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (selected) Purple.copy(alpha = 0.15f) else Surface)
                                .border(1.dp, if (selected) Purple else BorderSoft, RoundedCornerShape(14.dp))
                                .clickable {
                                    onSelect(cat.code, sub.code)
                                    onDismiss()
                                }
                                .padding(16.dp),
                        ) {
                            Text(sub.label, color = TextMain, fontSize = 15.sp, fontFamily = PlusJakartaSans)
                        }
                    }
                }
            }
        }
    }
}

enum class CategoryPickerMode { CATEGORY, SUBCATEGORY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalUploadAttachmentSheet(
    visible: Boolean,
    momentId: String,
    expenseId: String,
    onDismiss: () -> Unit,
    onUploaded: (uploadId: String) -> Unit,
    repository: PersonalTransactionRepository = remember { PersonalTransactionRepository() },
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var uploading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Bg,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Purple, Color(0xFFFF9E74), Color(0xFF10B981)),
                        ),
                    ),
            )
            Text(
                "Upload Attachment",
                color = TextMain,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Text(
                "Pick a source — upload persists to this expense when complete.",
                color = Muted,
                fontSize = 12.sp,
                fontFamily = PlusJakartaSans,
            )
            error?.let {
                Text(it, color = Color(0xFFF87171), fontSize = 12.sp, fontFamily = PlusJakartaSans)
            }
            listOf(
                Triple("📸", "Take Photo", "image/jpeg"),
                Triple("🖼️", "Photo Library", "image/jpeg"),
                Triple("📁", "Browse Files", "application/pdf"),
            ).forEach { (emoji, title, contentType) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF3B82F6).copy(alpha = 0.1f))
                        .border(1.dp, Color(0xFF3B82F6).copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                        .clickable(enabled = !uploading) {
                            uploading = true
                            error = null
                            scope.launch {
                                val ref = TransactionRef(
                                    TransactionDomain.PERSONAL,
                                    TransactionResourceType.EXPENSE,
                                    expenseId,
                                    momentId,
                                )
                                repository.attachMedia(
                                    ref,
                                    bytes = ByteArray(64) { 0xFF.toByte() },
                                    contentType = contentType,
                                ).fold(
                                    onSuccess = { dto ->
                                        uploading = false
                                        onUploaded(dto.uploadId)
                                        onDismiss()
                                    },
                                    onFailure = { e ->
                                        uploading = false
                                        error = e.message ?: "Upload failed"
                                    },
                                )
                            }
                        }
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF3B82F6).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) { Text(emoji, fontSize = 20.sp) }
                    Column {
                        Text(title, color = TextMain, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
                        Text(if (uploading) "Uploading…" else contentType, color = Muted, fontSize = 12.sp, fontFamily = PlusJakartaSans)
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Purple.copy(alpha = 0.1f))
                    .border(2.dp, Purple, RoundedCornerShape(20.dp))
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("☁️", fontSize = 28.sp)
                    Text("Drag & drop file here", color = TextMain, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                    Text("Supports PDF, JPG, PNG up to 10MB", color = Muted, fontSize = 12.sp, fontFamily = PlusJakartaSans)
                }
            }
        }
    }
}

private val Green = Color(0xFF10B981)
private val Blue = Color(0xFF3B82F6)
private val Orange = Color(0xFFF59E0B)
private val InputBg = Color(0xFF302E39)
private val Subtle = Color(0xFFABA3BA)

/** Figma account type labels → API codes. */
object PersonalFinancialAccountUi {
    val typeOptions = listOf("Bank" to "BANK", "Cash" to "CASH", "Credit" to "CARD")
    val currencyOptions = listOf("INR", "USD", "EUR")

    fun emojiForType(accountType: String): String = when (accountType.uppercase()) {
        "CASH" -> "💵"
        "CARD", "CREDIT", "CREDIT_CARD" -> "💳"
        "WALLET" -> "👛"
        else -> "🏦"
    }

    fun emojiForLabel(label: String): String = when (label) {
        "Cash" -> "💵"
        "Credit" -> "💳"
        else -> "🏦"
    }

    fun nativeIconForType(accountType: String): androidx.compose.ui.graphics.vector.ImageVector =
        when (accountType.uppercase()) {
            "CASH" -> Icons.Outlined.Payments
            "CARD", "CREDIT", "CREDIT_CARD" -> Icons.Outlined.CreditCard
            "WALLET" -> Icons.Outlined.AccountBalanceWallet
            else -> Icons.Outlined.AccountBalance
        }

    fun nativeIconForLabel(label: String): androidx.compose.ui.graphics.vector.ImageVector = when (label) {
        "Cash" -> Icons.Outlined.Payments
        "Credit" -> Icons.Outlined.CreditCard
        else -> Icons.Outlined.AccountBalance
    }

    fun apiTypeForLabel(label: String): String = typeOptions.firstOrNull { it.first == label }?.second ?: "BANK"

    fun balanceLabel(account: FinancialAccountDto): String {
        val inst = account.institutionName?.takeIf { it.isNotBlank() }
        return inst ?: "${account.accountType.replace('_', ' ')} · ${account.currencyCode}"
    }

    fun paymentMethodForAccountType(accountType: String): String = when (accountType.uppercase()) {
        "CARD" -> "CARD"
        "BANK" -> "BANK_TRANSFER"
        "WALLET" -> "WALLET"
        else -> "CASH"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalAddAccountSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onSaved: (FinancialAccountDto) -> Unit,
    repository: PersonalSliceRepository = remember { PersonalSliceRepository() },
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var accountTypeLabel by remember { mutableStateOf("Bank") }
    var accountName by remember { mutableStateOf("") }
    var openingBalance by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("INR") }
    var setDefault by remember { mutableStateOf(true) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val idempotencyKey = remember { UUID.randomUUID().toString() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Bg,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = TextMain,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(onClick = onDismiss),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text("Add Account", color = Color(0xFFF5F2FC), fontSize = 19.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                    Text("Life Operations", color = Subtle, fontSize = 10.sp, fontFamily = PlusJakartaSans)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Surface)
                    .border(1.dp, Color(0xFF575266), RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                AccountFieldLabel("ACCOUNT TYPE")
                AccountTypeChips(accountTypeLabel) { accountTypeLabel = it }
                AccountFieldLabel("ACCOUNT NAME")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(11.dp))
                        .background(InputBg)
                        .border(1.dp, Color(0x33938EA1), RoundedCornerShape(11.dp))
                        .padding(12.dp)
                        .testTag(MaestroIds.PERSONAL_ACCOUNT_ADD_NAME),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(PersonalFinancialAccountUi.emojiForLabel(accountTypeLabel), fontSize = 14.sp)
                    Spacer(Modifier.width(8.dp))
                    BasicTextField(
                        value = accountName,
                        onValueChange = { accountName = it },
                        textStyle = TextStyle(color = Color(0xFFF5F2FC), fontSize = 13.sp, fontFamily = PlusJakartaSans),
                        cursorBrush = SolidColor(Purple),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            if (accountName.isEmpty()) Text("HDFC Savings", color = Subtle, fontSize = 13.sp)
                            inner()
                        },
                    )
                }
                AccountFieldLabel("OPENING BALANCE (OPTIONAL)")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(11.dp))
                        .background(InputBg)
                        .border(1.dp, Color(0x33938EA1), RoundedCornerShape(11.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicTextField(
                        value = openingBalance,
                        onValueChange = { openingBalance = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        textStyle = TextStyle(color = Green, fontSize = 26.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans),
                        cursorBrush = SolidColor(Green),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (openingBalance.isEmpty()) Text("₹", color = Green, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                                inner()
                            }
                        },
                    )
                }
                Text(
                    "Balance tracking coming soon.",
                    color = Subtle,
                    fontSize = 10.sp,
                    fontFamily = PlusJakartaSans,
                )
                AccountFieldLabel("CURRENCY")
                CurrencyChips(currency) { currency = it }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Set as Default Account", color = Color(0xFFF5F2FC), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
                    Switch(
                        checked = setDefault,
                        onCheckedChange = { setDefault = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = Green, checkedThumbColor = Color.White),
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.horizontalGradient(listOf(Purple, Color(0xFFA78BFA))))
                    .testTag(MaestroIds.PERSONAL_ACCOUNT_ADD_SUBMIT)
                    .clickable(enabled = accountName.isNotBlank() && !submitting) {
                        submitting = true
                        error = null
                        scope.launch {
                            repository.createFinancialAccount(
                                accountType = PersonalFinancialAccountUi.apiTypeForLabel(accountTypeLabel),
                                accountName = accountName.trim(),
                                currencyCode = currency,
                                idempotencyKey = idempotencyKey,
                            ).fold(
                                onSuccess = { dto ->
                                    submitting = false
                                    onSaved(dto)
                                    onDismiss()
                                },
                                onFailure = { e ->
                                    submitting = false
                                    error = e.message
                                },
                            )
                        }
                    }
                    .padding(12.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (submitting) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    Text("Add Account", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Top) {
                Text("ℹ️", fontSize = 10.sp)
                Text(
                    "The account will become available immediately in Quick Add and Master Expense.",
                    color = Subtle,
                    fontSize = 10.sp,
                    fontFamily = PlusJakartaSans,
                )
            }
            error?.let { Text(it, color = Color(0xFFF87171), fontSize = 12.sp, fontFamily = PlusJakartaSans) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalAccountPickerSheet(
    visible: Boolean,
    selectedAccountId: String?,
    excludeAccountId: String? = null,
    onDismiss: () -> Unit,
    onSelect: (FinancialAccountDto) -> Unit,
    repository: PersonalSliceRepository = remember { PersonalSliceRepository() },
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var accounts by remember { mutableStateOf<List<FinancialAccountDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var showAddAccount by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun reload() {
        loading = true
    }

    LaunchedEffect(visible) {
        if (visible) {
            repository.listFinancialAccounts().fold(
                onSuccess = { accounts = it; loading = false },
                onFailure = { error = it.message; loading = false },
            )
        }
    }

    PersonalAddAccountSheet(
        visible = showAddAccount,
        onDismiss = { showAddAccount = false },
        onSaved = { created ->
            accounts = accounts + created
            onSelect(created)
            showAddAccount = false
            onDismiss()
        },
        repository = repository,
    )

    if (showAddAccount) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Bg,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 16.dp)
                .testTag(MaestroIds.PERSONAL_ACCOUNT_PICKER),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = TextMain,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable(onClick = onDismiss),
                    )
                    Text("Select Account", color = Color(0xFFF5F2FC), fontSize = 19.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Purple)
                        .border(1.dp, Color(0xFF938EA1), RoundedCornerShape(20.dp))
                        .testTag(MaestroIds.PERSONAL_ACCOUNT_PICKER_CLOSE)
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text("Close", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                }
            }

            val filtered = accounts.filter { it.financialAccountId != excludeAccountId }

            when {
                loading -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Purple)
                    }
                }
                filtered.isEmpty() -> AccountPickerEmptyState(onCreate = { showAddAccount = true })
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        filtered.forEach { account ->
                            AccountPickerRow(
                                account = account,
                                selected = account.financialAccountId == selectedAccountId,
                                onClick = {
                                    onSelect(account)
                                    onDismiss()
                                },
                            )
                        }
                        CreateAccountButton(onClick = { showAddAccount = true })
                    }
                }
            }
            error?.let { Text(it, color = Color(0xFFF87171), fontSize = 12.sp, fontFamily = PlusJakartaSans) }
        }
    }
}

@Composable
private fun AccountPickerEmptyState(onCreate: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Purple, Color(0xFFA78BFA))))
                .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("🏦", fontSize = 48.sp)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("No accounts available", color = Color(0xFFF5F2FC), fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
            Text(
                "You'll return here automatically after the account is created.",
                color = Subtle,
                fontSize = 14.sp,
                fontFamily = PlusJakartaSans,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
        CreateAccountButton(onClick = onCreate)
    }
}

@Composable
private fun CreateAccountButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.horizontalGradient(listOf(Purple, Color(0xFFA78BFA))))
            .testTag(MaestroIds.PERSONAL_ACCOUNT_PICKER_CREATE)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("➕", fontSize = 16.sp)
        Text("Create Account", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
    }
}

@Composable
private fun AccountPickerRow(account: FinancialAccountDto, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) Surface else Bg)
            .border(if (selected) 2.dp else 1.dp, if (selected) Purple else Color(0xFF938EA1), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(PersonalFinancialAccountUi.emojiForType(account.accountType), fontSize = 20.sp)
            Column {
                Text(account.accountName, color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
                Text(PersonalFinancialAccountUi.balanceLabel(account), color = Muted, fontSize = 12.sp, fontFamily = PlusJakartaSans)
            }
        }
        if (selected) {
            Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(Purple), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Check, null, tint = Color.White, modifier = Modifier.size(12.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccountTypeChips(selected: String, onSelect: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        PersonalFinancialAccountUi.typeOptions.forEach { (label, _) ->
            val sel = selected == label
            val color = when (label) {
                "Cash" -> Green
                "Credit" -> Orange
                else -> Blue
            }
            val testTag = when (label) {
                "Bank" -> MaestroIds.PERSONAL_ACCOUNT_ADD_TYPE_BANK
                "Cash" -> MaestroIds.PERSONAL_ACCOUNT_ADD_TYPE_CASH
                else -> MaestroIds.PERSONAL_ACCOUNT_ADD_TYPE_CREDIT
            }
            Text(
                label,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (sel) color else color.copy(alpha = 0.35f))
                    .testTag(testTag)
                    .clickable { onSelect(label) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CurrencyChips(selected: String, onSelect: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        PersonalFinancialAccountUi.currencyOptions.forEach { code ->
            val sel = selected == code
            val color = when (code) {
                "USD" -> Blue
                "EUR" -> Green
                else -> Purple
            }
            Text(
                code,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (sel) color else color.copy(alpha = 0.35f))
                    .clickable { onSelect(code) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
        }
    }
}

@Composable
private fun AccountFieldLabel(text: String) {
    Text(text, color = Subtle, fontSize = 9.sp, fontWeight = FontWeight.Normal, fontFamily = PlusJakartaSans, letterSpacing = 0.8.sp)
}

/** Chevron row for account selection in money/expense/edit surfaces. */
@Composable
fun PersonalAccountSelectRow(
    label: String,
    account: FinancialAccountDto?,
    placeholder: String = "Select account",
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = Subtle, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans, letterSpacing = 0.8.sp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Surface)
                .border(1.dp, Color(0xFF938EA1), RoundedCornerShape(14.dp))
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
                .clickable(onClick = onClick)
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (account != null) {
                    Text(PersonalFinancialAccountUi.emojiForType(account.accountType), fontSize = 18.sp)
                }
                Text(
                    account?.accountName ?: placeholder,
                    color = TextMain,
                    fontSize = 14.sp,
                    fontFamily = PlusJakartaSans,
                )
            }
            Text("›", color = Muted, fontSize = 18.sp)
        }
    }
}

