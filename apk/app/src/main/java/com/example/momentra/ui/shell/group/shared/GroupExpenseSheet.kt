package com.example.momentra.ui.shell.group.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.GroupExpenseSplitInputDto
import com.example.momentra.R
import com.example.momentra.data.api.GroupParticipantDto
import com.example.momentra.data.repository.GroupExpenseSplitBuilder
import com.example.momentra.data.repository.GroupSliceRepository
import com.example.momentra.ui.shell.empty.group.GeBorder
import com.example.momentra.ui.shell.empty.group.GeCard
import com.example.momentra.ui.shell.empty.group.GeSecondary
import com.example.momentra.ui.shell.empty.group.GeText
import com.example.momentra.ui.shell.group.wedding.create.WeddingActiveTheme
import com.example.momentra.ui.shell.maestro.MaestroIds
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

private val Teal = Color(0xFF14B8A6)
private val Red = Color(0xFFF87171)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GroupExpenseSheet(
    momentId: String,
    visible: Boolean,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    onDeleted: () -> Unit = onSaved,
    expenseId: String? = null,
    isWedding: Boolean = false,
    momentTypeCode: String? = null,
    repository: GroupSliceRepository = remember { GroupSliceRepository() },
) {
    if (!visible) return
    val isEditing = expenseId != null
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sheetBg = if (isWedding) WeddingActiveTheme.Bg else TripSheetTokens.Bg
    val sheetText = if (isWedding) WeddingActiveTheme.Text else TripSheetTokens.Text
    val sheetSecondary = if (isWedding) WeddingActiveTheme.Secondary else TripSheetTokens.Muted
    val sheetAccent = if (isWedding) WeddingActiveTheme.Accent else TripSheetTokens.Accent
    val sheetCard = if (isWedding) WeddingActiveTheme.Card else TripSheetTokens.Field
    val sheetBorder = if (isWedding) WeddingActiveTheme.Border else TripSheetTokens.Border
    val fieldRadius = if (isWedding) 12.dp else 8.dp
    val ctaEnd = if (isWedding) sheetAccent else TripSheetTokens.AccentEnd
    val categoryOptions = remember(momentTypeCode) {
        GroupExpenseCategoryCatalog.categories(momentTypeCode)
    }
    val livingTypes = remember {
        setOf("FAMILY_HOUSEHOLD", "FLATMATES", "CO_LIVING", "SHARED_LIVING", "COMMUNITY_LIVING")
    }
    val supportsPooled = remember(momentTypeCode) {
        livingTypes.contains((momentTypeCode ?: "").trim().uppercase())
    }
    var amount by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("INR") }
    var description by remember { mutableStateOf("") }
    var category by remember(momentTypeCode) {
        mutableStateOf(GroupExpenseCategoryCatalog.defaultCategory(momentTypeCode))
    }
    var splitStrategy by remember { mutableStateOf("EQUAL") }
    var participants by remember { mutableStateOf<List<GroupParticipantDto>>(emptyList()) }
    var selectedSplitIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    /** PERCENTAGE: participantId → percent string; EXACT: amount; SHARES: weight */
    var splitValues by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var paidById by remember { mutableStateOf<String?>(null) }
    var currencyMenuOpen by remember { mutableStateOf(false) }
    var paidByMenuOpen by remember { mutableStateOf(false) }
    var expenseDate by remember { mutableStateOf("") }
    var loadingParticipants by remember { mutableStateOf(true) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val currencyOptions = remember { listOf("INR", "USD", "EUR", "GBP") }
    val currencySymbol = remember(currency) {
        when (currency) {
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            else -> "₹"
        }
    }
    val figmaSplitLabels = remember(supportsPooled) {
        buildList {
            add("Equal" to "EQUAL")
            add("Custom" to "EXACT")
            add("% Percent" to "PERCENTAGE")
            if (supportsPooled) add("Pooled" to "POOLED")
        }
    }

    LaunchedEffect(momentId, visible, expenseId) {
        if (!visible) return@LaunchedEffect
        loadingParticipants = true
        error = null
        repository.getParticipants(momentId).fold(
            onSuccess = { dto ->
                val active = dto.participants.filter {
                    it.status.equals("ACTIVE", ignoreCase = true) ||
                        it.status.equals("INVITED", ignoreCase = true)
                }.ifEmpty { dto.participants }
                participants = active
                if (expenseId == null) {
                    selectedSplitIds = active.map { it.participantId }.toSet()
                    paidById = active.firstOrNull()?.participantId
                    splitValues = emptyMap()
                }
            },
            onFailure = { error = it.message },
        )
        if (expenseId != null) {
            repository.getGroupExpense(momentId, expenseId).fold(
                onSuccess = { detail ->
                    amount = detail.amount
                    currency = detail.currencyCode
                    val parsed = GroupExpenseCategoryCatalog.parseCategoryAndNote(
                        detail.description,
                        momentTypeCode,
                    )
                    category = parsed.first
                    description = parsed.second
                    paidById = detail.paidByParticipantId
                    splitStrategy = detail.splitStrategy
                    if (detail.splitStrategy == "POOLED") {
                        selectedSplitIds = participants.map { it.participantId }.toSet()
                        splitValues = emptyMap()
                    } else {
                        selectedSplitIds = detail.shares.map { it.participantId }.toSet()
                        splitValues = when (detail.splitStrategy) {
                            "PERCENTAGE" -> detail.shares.associate {
                                it.participantId to (it.sharePercent ?: "")
                            }
                            "EXACT" -> detail.shares.associate {
                                it.participantId to it.shareAmount
                            }
                            "SHARES" -> detail.shares.associate {
                                it.participantId to (it.sharePercent ?: "1")
                            }
                            else -> emptyMap()
                        }
                    }
                },
                onFailure = { error = it.message },
            )
        }
        loadingParticipants = false
    }

    val previewShares = remember(amount, selectedSplitIds, splitStrategy, splitValues) {
        val total = amount.toBigDecimalOrNull() ?: return@remember emptyList<Pair<String, String>>()
        if (selectedSplitIds.isEmpty() || total <= BigDecimal.ZERO) return@remember emptyList()
        val ids = selectedSplitIds.sorted()
        when (splitStrategy) {
            "EQUAL" -> {
                val n = ids.size
                val base = total.divide(BigDecimal(n), 4, RoundingMode.DOWN)
                val allocated = base.multiply(BigDecimal(n))
                val remainder = total.subtract(allocated)
                ids.mapIndexed { index, id ->
                    val share = if (index == 0) base.add(remainder) else base
                    id to share.toPlainString()
                }
            }
            "PERCENTAGE" -> ids.map { id ->
                val pct = splitValues[id]?.toBigDecimalOrNull() ?: BigDecimal.ZERO
                id to total.multiply(pct).divide(BigDecimal(100), 4, RoundingMode.HALF_UP).toPlainString()
            }
            "EXACT" -> ids.map { id -> id to (splitValues[id] ?: "0") }
            "SHARES" -> {
                val weights = ids.associateWith { (splitValues[it]?.toBigDecimalOrNull() ?: BigDecimal.ONE).max(BigDecimal.ZERO) }
                val weightSum = weights.values.fold(BigDecimal.ZERO) { a, b -> a.add(b) }
                if (weightSum <= BigDecimal.ZERO) emptyList()
                else ids.map { id ->
                    id to total.multiply(weights.getValue(id)).divide(weightSum, 4, RoundingMode.HALF_UP).toPlainString()
                }
            }
            else -> emptyList()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetBg,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 40.dp, height = 5.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 14.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(sheetAccent.copy(alpha = 0.18f))
                        .border(1.dp, sheetAccent.copy(alpha = 0.35f), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("💳", fontSize = 16.sp)
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        if (isEditing) "Edit Expense" else "Add Expense",
                        color = sheetText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = PlusJakartaSans,
                    )
                    Text(
                        if (isWedding) "Track and split wedding costs" else "Split costs with the group",
                        color = sheetSecondary,
                        fontSize = 12.sp,
                        fontFamily = PlusJakartaSans,
                    )
                }
            }

            if (loadingParticipants) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(color = sheetAccent)
                }
            }

            // Figma GRP-SL-FAM-Q01: large currency amount + currency dropdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom,
            ) {
                Box {
                    Text(
                        currencySymbol,
                        color = sheetAccent,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { currencyMenuOpen = true }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                    DropdownMenu(
                        expanded = currencyMenuOpen,
                        onDismissRequest = { currencyMenuOpen = false },
                    ) {
                        currencyOptions.forEach { code ->
                            DropdownMenuItem(
                                text = { Text(code, fontFamily = PlusJakartaSans) },
                                onClick = {
                                    currency = code
                                    currencyMenuOpen = false
                                },
                            )
                        }
                    }
                }
                BasicTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = sheetText,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = PlusJakartaSans,
                    ),
                    cursorBrush = SolidColor(sheetAccent),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    decorationBox = { inner ->
                        if (amount.isEmpty()) {
                            Text(
                                "0.00",
                                color = sheetText.copy(alpha = 0.35f),
                                fontSize = 40.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = PlusJakartaSans,
                            )
                        }
                        inner()
                    },
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .testTag(MaestroIds.GROUP_EXPENSE_AMOUNT),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(fieldRadius))
                    .background(sheetCard)
                    .border(1.dp, sheetBorder, RoundedCornerShape(fieldRadius))
                    .clickable { currencyMenuOpen = true }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Currency", color = sheetSecondary, fontSize = 12.sp, fontFamily = PlusJakartaSans)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        "$currencySymbol  $currency",
                        color = sheetText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = PlusJakartaSans,
                    )
                    Icon(
                        painter = painterResource(R.drawable.ic_biz_create_chevron),
                        contentDescription = null,
                        tint = sheetSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            FieldLabel("Description", color = sheetSecondary)
            SheetField(
                value = description,
                onValueChange = { description = it },
                placeholder = "What was this for?",
                testTag = MaestroIds.GROUP_EXPENSE_NOTE,
                card = sheetCard,
                border = sheetBorder,
                text = sheetText,
                secondary = sheetSecondary,
                accent = sheetAccent,
                cornerRadius = fieldRadius,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FieldLabel("Paid By", color = sheetSecondary)
                    Box(modifier = Modifier.testTag(MaestroIds.GROUP_EXPENSE_PAYER)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(fieldRadius))
                                .background(sheetCard)
                                .border(1.dp, sheetBorder, RoundedCornerShape(fieldRadius))
                                .clickable { paidByMenuOpen = true }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                participants.firstOrNull { it.participantId == paidById }?.displayName
                                    ?: "Select",
                                color = sheetText,
                                fontSize = 14.sp,
                                fontFamily = PlusJakartaSans,
                            )
                            Icon(
                                painter = painterResource(R.drawable.ic_biz_create_chevron),
                                contentDescription = null,
                                tint = sheetSecondary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        DropdownMenu(
                            expanded = paidByMenuOpen,
                            onDismissRequest = { paidByMenuOpen = false },
                        ) {
                            participants.forEach { p ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            p.displayName ?: p.participantId.take(8),
                                            fontFamily = PlusJakartaSans,
                                        )
                                    },
                                    onClick = {
                                        paidById = p.participantId
                                        paidByMenuOpen = false
                                    },
                                )
                            }
                        }
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FieldLabel("Date", color = sheetSecondary)
                    TripDatePickField(
                        value = expenseDate,
                        onValueChange = { expenseDate = it },
                        placeholder = "Today",
                    )
                }
            }

            FieldLabel("Split Between", color = sheetSecondary)
            if (!loadingParticipants && participants.isEmpty()) {
                Text(
                    "No participants yet",
                    color = sheetSecondary,
                    fontSize = 12.sp,
                    fontFamily = PlusJakartaSans,
                )
            } else {
                val avatarColors = listOf(
                    Color(0xFFFBBF24),
                    Color(0xFF34D399),
                    Color(0xFF60A5FA),
                    Color(0xFFF472B6),
                    Color(0xFFA78BFA),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    participants.forEachIndexed { index, p ->
                        val selected = p.participantId in selectedSplitIds
                        val color = avatarColors[index % avatarColors.size]
                        val name = p.displayName ?: p.participantId.take(8)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.clickable {
                                selectedSplitIds = if (selected) {
                                    selectedSplitIds - p.participantId
                                } else {
                                    selectedSplitIds + p.participantId
                                }
                            },
                        ) {
                            Box {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .then(
                                            if (selected) Modifier.border(2.dp, sheetAccent, CircleShape)
                                            else Modifier,
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        name.split(" ").filter { it.isNotEmpty() }.take(2)
                                            .joinToString("") { it.first().uppercase() }
                                            .ifBlank { "??" },
                                        color = Color(0xFF14121B),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = PlusJakartaSans,
                                    )
                                }
                                if (selected) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(16.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(sheetAccent),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            "✓",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }
                            Text(
                                name.split(" ").first(),
                                color = if (selected) sheetText else sheetSecondary,
                                fontSize = 11.sp,
                                fontFamily = PlusJakartaSans,
                            )
                        }
                    }
                }
            }

            FieldLabel("Split Type", color = sheetSecondary)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(MaestroIds.GROUP_EXPENSE_SPLIT)
                    .clip(RoundedCornerShape(12.dp))
                    .background(sheetCard)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                figmaSplitLabels.forEach { (label, strategy) ->
                    val on = splitStrategy == strategy
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (on) sheetAccent else Color.Transparent)
                            .clickable {
                                splitStrategy = strategy
                                when (strategy) {
                                    "PERCENTAGE" -> if (selectedSplitIds.isNotEmpty()) {
                                        val even = 100.0 / selectedSplitIds.size
                                        splitValues = selectedSplitIds.associateWith {
                                            String.format("%.2f", even)
                                        }
                                    }
                                    "EXACT" -> splitValues = selectedSplitIds.associateWith { "" }
                                    else -> splitValues = emptyMap()
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 10.dp)
                            .testTag(MaestroIds.groupExpenseSplitStrategy(strategy)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label,
                            color = if (on) Color.White else sheetSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = PlusJakartaSans,
                        )
                    }
                }
            }

            if (splitStrategy == "POOLED") {
                Text(
                    "Household spend — sums for the month. No per-member split.",
                    color = sheetSecondary,
                    fontSize = 12.sp,
                    fontFamily = PlusJakartaSans,
                )
            }

            FieldLabel("Category", color = sheetSecondary)
            FlowRow(
                modifier = Modifier.testTag(MaestroIds.GROUP_EXPENSE_CATEGORY),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                categoryOptions.forEach { option ->
                    ParticipantChip(
                        label = option,
                        selected = category == option,
                        onClick = { category = option },
                        accent = sheetAccent,
                        card = sheetCard,
                        border = sheetBorder,
                        text = sheetText,
                    )
                }
            }

            if (splitStrategy != "EQUAL" && splitStrategy != "POOLED" && selectedSplitIds.isNotEmpty()) {
                val valueLabel = when (splitStrategy) {
                    "PERCENTAGE" -> "Percent (must sum to 100)"
                    "EXACT" -> "Exact amount per person"
                    else -> "Share weight"
                }
                FieldLabel(valueLabel, color = sheetSecondary)
                selectedSplitIds.sorted().forEach { id ->
                    val name = participants.firstOrNull { it.participantId == id }?.displayName
                        ?: id.take(8)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            name,
                            color = sheetText,
                            fontSize = 12.sp,
                            fontFamily = PlusJakartaSans,
                            modifier = Modifier.weight(1f),
                        )
                        SheetField(
                            value = splitValues[id] ?: "",
                            onValueChange = { v ->
                                splitValues = splitValues + (id to v.filter { c -> c.isDigit() || c == '.' })
                            },
                            placeholder = when (splitStrategy) {
                                "PERCENTAGE" -> "%"
                                "EXACT" -> "0.00"
                                else -> "1"
                            },
                            keyboardType = KeyboardType.Decimal,
                            card = sheetCard,
                            border = sheetBorder,
                            text = sheetText,
                            secondary = sheetSecondary,
                            testTag = "${MaestroIds.GROUP_EXPENSE_SPLIT_VALUE}.$id",
                            accent = sheetAccent,
                            cornerRadius = fieldRadius,
                        )
                    }
                }
            }

            Text(
                "All residents will be notified",
                color = sheetSecondary,
                fontSize = 12.sp,
                fontFamily = PlusJakartaSans,
            )

            if (previewShares.isNotEmpty() && splitStrategy != "POOLED") {
                FieldLabel(
                    when (splitStrategy) {
                        "EQUAL" -> "Equal split preview"
                        else -> "Split preview (server is authoritative)"
                    },
                    color = sheetSecondary,
                )
                previewShares.forEach { (id, share) ->
                    val name = participants.firstOrNull { it.participantId == id }?.displayName
                        ?: id.take(8)
                    Text(
                        "$name · $share",
                        color = sheetSecondary,
                        fontSize = 12.sp,
                        fontFamily = PlusJakartaSans,
                    )
                }
            }

            error?.let {
                Text(it, color = Red, fontSize = 12.sp, fontFamily = PlusJakartaSans)
            }

            if (isEditing && expenseId != null) {
                Text(
                    "Delete expense",
                    color = Red,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !submitting) {
                            scope.launch {
                                submitting = true
                                error = null
                                repository.voidGroupExpense(momentId, expenseId).fold(
                                    onSuccess = {
                                        submitting = false
                                        onDeleted()
                                        onDismiss()
                                    },
                                    onFailure = {
                                        submitting = false
                                        error = it.message
                                    },
                                )
                            }
                        }
                        .padding(vertical = 10.dp),
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (submitting) {
                            Brush.horizontalGradient(listOf(sheetCard, sheetCard))
                        } else {
                            Brush.horizontalGradient(listOf(sheetAccent, ctaEnd))
                        },
                    )
                    .testTag(MaestroIds.GROUP_EXPENSE_SUBMIT)
                    .clickable(enabled = !submitting) {
                        val payer = paidById
                        if (payer == null) {
                            error = "Select payer"
                            return@clickable
                        }
                        if (splitStrategy != "POOLED" && selectedSplitIds.isEmpty()) {
                            error = "Select payer and at least one participant"
                            return@clickable
                        }
                        if (amount.toBigDecimalOrNull() == null || amount.toBigDecimal() <= BigDecimal.ZERO) {
                            error = "Enter a valid amount"
                            return@clickable
                        }
                        val ids = selectedSplitIds.sorted()
                        val inputs = when (splitStrategy) {
                            "POOLED" -> emptyList()
                            "EQUAL" -> ids.map { GroupExpenseSplitInputDto(participantId = it) }
                            "PERCENTAGE" -> {
                                val pctSum = ids.sumOf {
                                    splitValues[it]?.toBigDecimalOrNull()?.toDouble() ?: 0.0
                                }
                                if (kotlin.math.abs(pctSum - 100.0) > 0.01) {
                                    error = "Percents must sum to 100 (now $pctSum)"
                                    return@clickable
                                }
                                ids.map {
                                    GroupExpenseSplitInputDto(
                                        participantId = it,
                                        percent = splitValues[it],
                                    )
                                }
                            }
                            "EXACT" -> {
                                val sum = ids.fold(BigDecimal.ZERO) { acc, id ->
                                    acc.add(splitValues[id]?.toBigDecimalOrNull() ?: BigDecimal.ZERO)
                                }
                                if (sum.compareTo(amount.toBigDecimal()) != 0) {
                                    error = "Exact amounts must equal expense amount"
                                    return@clickable
                                }
                                ids.map {
                                    GroupExpenseSplitInputDto(
                                        participantId = it,
                                        amount = splitValues[it],
                                    )
                                }
                            }
                            "SHARES" -> ids.map {
                                val w = splitValues[it]?.toDoubleOrNull() ?: 1.0
                                if (w <= 0) {
                                    error = "Share weights must be positive"
                                    return@clickable
                                }
                                GroupExpenseSplitInputDto(participantId = it, shares = w)
                            }
                            else -> {
                                error = "Unknown split strategy"
                                return@clickable
                            }
                        }
                        scope.launch {
                            submitting = true
                            error = null
                            val body = GroupExpenseSplitBuilder.build(
                                amount = amount,
                                currencyCode = currency,
                                paidByParticipantId = payer,
                                splitStrategy = splitStrategy,
                                splitInputs = inputs,
                                description = GroupExpenseCategoryCatalog.descriptionWithCategory(
                                    category = category,
                                    userDescription = description,
                                ),
                            )
                            val result = if (expenseId != null) {
                                repository.updateGroupExpense(momentId, expenseId, body)
                            } else {
                                repository.createGroupExpense(momentId, body)
                            }
                            result.fold(
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
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (submitting) {
                    CircularProgressIndicator(color = sheetAccent, modifier = Modifier.padding(4.dp))
                } else {
                    Text(
                        if (isEditing) "Save Expense" else "Add Expense",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        fontFamily = PlusJakartaSans,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupContributionSheet(
    momentId: String,
    visible: Boolean,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    isWedding: Boolean = false,
    repository: GroupSliceRepository = remember { GroupSliceRepository() },
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sheetBg = if (isWedding) WeddingActiveTheme.Bg else TripSheetTokens.Bg
    val sheetText = if (isWedding) WeddingActiveTheme.Text else TripSheetTokens.Text
    val sheetSecondary = if (isWedding) WeddingActiveTheme.Secondary else TripSheetTokens.Muted
    val sheetAccent = if (isWedding) WeddingActiveTheme.Accent else TripSheetTokens.Accent
    val sheetCard = if (isWedding) WeddingActiveTheme.Card else TripSheetTokens.Field
    val sheetBorder = if (isWedding) WeddingActiveTheme.Border else TripSheetTokens.Border
    val fieldRadius = if (isWedding) 12.dp else 8.dp
    val ctaEnd = if (isWedding) sheetAccent else TripSheetTokens.AccentEnd
    var amount by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("INR") }
    var label by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetBg,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 40.dp, height = 5.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 14.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(sheetAccent.copy(alpha = 0.18f))
                        .border(1.dp, sheetAccent.copy(alpha = 0.35f), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("💵", fontSize = 16.sp)
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        if (isWedding) "Add Contribution" else "Contribution",
                        color = sheetText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = PlusJakartaSans,
                    )
                    Text(
                        "Add funds toward this shared moment",
                        color = sheetSecondary,
                        fontSize = 12.sp,
                        fontFamily = PlusJakartaSans,
                    )
                }
            }
            FieldLabel("Amount", color = sheetSecondary)
            SheetField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                placeholder = "0.00",
                keyboardType = KeyboardType.Decimal,
                card = sheetCard,
                border = sheetBorder,
                text = sheetText,
                secondary = sheetSecondary,
                accent = sheetAccent,
                cornerRadius = fieldRadius,
            )
            FieldLabel("Currency", color = sheetSecondary)
            SheetField(
                value = currency,
                onValueChange = { currency = it.uppercase().take(3) },
                placeholder = "INR",
                card = sheetCard,
                border = sheetBorder,
                text = sheetText,
                secondary = sheetSecondary,
                accent = sheetAccent,
                cornerRadius = fieldRadius,
            )
            FieldLabel("Label (optional)", color = sheetSecondary)
            SheetField(
                value = label,
                onValueChange = { label = it },
                placeholder = "Initial float…",
                card = sheetCard,
                border = sheetBorder,
                text = sheetText,
                secondary = sheetSecondary,
                accent = sheetAccent,
                cornerRadius = fieldRadius,
            )
            error?.let {
                Text(it, color = Red, fontSize = 12.sp, fontFamily = PlusJakartaSans)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (submitting) {
                            Brush.horizontalGradient(listOf(sheetCard, sheetCard))
                        } else {
                            Brush.horizontalGradient(listOf(sheetAccent, ctaEnd))
                        },
                    )
                    .clickable(enabled = !submitting) {
                        if (amount.toBigDecimalOrNull() == null || amount.toBigDecimal() <= BigDecimal.ZERO) {
                            error = "Enter a valid amount"
                            return@clickable
                        }
                        scope.launch {
                            submitting = true
                            error = null
                            repository.recordContribution(
                                momentId = momentId,
                                amount = amount,
                                currencyCode = currency,
                                label = label,
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
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (submitting) "Saving…" else "Record contribution",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String, color: Color = GeSecondary) {
    Text(
        text,
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = PlusJakartaSans,
    )
}

@Composable
private fun SheetField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    testTag: String? = null,
    card: Color = GeCard,
    border: Color = GeBorder,
    text: Color = GeText,
    secondary: Color = GeSecondary,
    accent: Color = Teal,
    cornerRadius: Dp = 8.dp,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .background(card)
            .border(1.dp, border, RoundedCornerShape(cornerRadius))
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        if (value.isEmpty()) {
            Text(placeholder, color = secondary.copy(alpha = 0.6f), fontSize = 14.sp, fontFamily = PlusJakartaSans)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                color = text,
                fontSize = 14.sp,
                fontFamily = PlusJakartaSans,
            ),
            cursorBrush = SolidColor(accent),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ParticipantChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    accent: Color = Teal,
    card: Color = GeCard,
    border: Color = GeBorder,
    text: Color = GeText,
    testTag: String? = null,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) accent.copy(alpha = 0.2f) else card)
            .border(1.dp, if (selected) accent else border, RoundedCornerShape(20.dp))
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            label,
            color = if (selected) accent else text,
            fontSize = 12.sp,
            fontFamily = PlusJakartaSans,
        )
    }
}
