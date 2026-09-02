package com.example.momentra.ui.shell.group

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.GroupExpenseSplitInputDto
import com.example.momentra.data.api.GroupParticipantDto
import com.example.momentra.data.repository.GroupExpenseSplitBuilder
import com.example.momentra.data.repository.GroupSliceRepository
import com.example.momentra.ui.shell.empty.group.GeBorder
import com.example.momentra.ui.shell.empty.group.GeCard
import com.example.momentra.ui.shell.empty.group.GeSecondary
import com.example.momentra.ui.shell.empty.group.GeText
import com.example.momentra.ui.shell.group.wedding.WeddingActiveTheme
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
    var description by remember { mutableStateOf("") }
    var splitStrategy by remember { mutableStateOf("EQUAL") }
    var participants by remember { mutableStateOf<List<GroupParticipantDto>>(emptyList()) }
    var selectedSplitIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    /** PERCENTAGE: participantId → percent string; EXACT: amount; SHARES: weight */
    var splitValues by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var paidById by remember { mutableStateOf<String?>(null) }
    var loadingParticipants by remember { mutableStateOf(true) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(momentId, visible) {
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
                selectedSplitIds = active.map { it.participantId }.toSet()
                paidById = active.firstOrNull()?.participantId
                splitValues = emptyMap()
            },
            onFailure = { error = it.message },
        )
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
                        if (isWedding) "Add Expense" else "Group expense",
                        color = sheetText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = PlusJakartaSans,
                    )
                    Text(
                        if (isWedding) "Track and split wedding costs" else "Split is computed on the server. Preview is local only.",
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

            FieldLabel("Amount", color = sheetSecondary)
            SheetField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                placeholder = "0.00",
                keyboardType = KeyboardType.Decimal,
                testTag = MaestroIds.GROUP_EXPENSE_AMOUNT,
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
            FieldLabel("Description (optional)", color = sheetSecondary)
            SheetField(
                value = description,
                onValueChange = { description = it },
                placeholder = "Dinner, groceries…",
                testTag = MaestroIds.GROUP_EXPENSE_NOTE,
                card = sheetCard,
                border = sheetBorder,
                text = sheetText,
                secondary = sheetSecondary,
                accent = sheetAccent,
                cornerRadius = fieldRadius,
            )

            FieldLabel("Paid by", color = sheetSecondary)
            FlowRow(
                modifier = Modifier.testTag(MaestroIds.GROUP_EXPENSE_PAYER),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                participants.forEach { p ->
                    val selected = paidById == p.participantId
                    ParticipantChip(
                        label = p.displayName ?: p.participantId.take(8),
                        selected = selected,
                        onClick = { paidById = p.participantId },
                        accent = sheetAccent,
                        card = sheetCard,
                        border = sheetBorder,
                        text = sheetText,
                    )
                }
            }

            FieldLabel("Split strategy", color = sheetSecondary)
            FlowRow(
                modifier = Modifier.testTag(MaestroIds.GROUP_EXPENSE_SPLIT),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf("EQUAL", "PERCENTAGE", "EXACT", "SHARES").forEach { strategy ->
                    ParticipantChip(
                        label = strategy,
                        selected = splitStrategy == strategy,
                        onClick = {
                            splitStrategy = strategy
                            if (strategy == "PERCENTAGE" && selectedSplitIds.isNotEmpty()) {
                                val even = (100.0 / selectedSplitIds.size)
                                splitValues = selectedSplitIds.associateWith {
                                    String.format("%.2f", even)
                                }
                            } else if (strategy == "SHARES") {
                                splitValues = selectedSplitIds.associateWith { "1" }
                            } else if (strategy == "EXACT") {
                                splitValues = selectedSplitIds.associateWith { "" }
                            } else {
                                splitValues = emptyMap()
                            }
                        },
                        accent = sheetAccent,
                        card = sheetCard,
                        border = sheetBorder,
                        text = sheetText,
                        testTag = MaestroIds.groupExpenseSplitStrategy(strategy),
                    )
                }
            }

            FieldLabel("Split with", color = sheetSecondary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                participants.forEach { p ->
                    val selected = p.participantId in selectedSplitIds
                    ParticipantChip(
                        label = p.displayName ?: p.participantId.take(8),
                        selected = selected,
                        onClick = {
                            selectedSplitIds = if (selected) {
                                selectedSplitIds - p.participantId
                            } else {
                                selectedSplitIds + p.participantId
                            }
                        },
                        accent = sheetAccent,
                        card = sheetCard,
                        border = sheetBorder,
                        text = sheetText,
                    )
                }
            }

            if (splitStrategy != "EQUAL" && selectedSplitIds.isNotEmpty()) {
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

            if (previewShares.isNotEmpty()) {
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
                        if (payer == null || selectedSplitIds.isEmpty()) {
                            error = "Select payer and at least one participant"
                            return@clickable
                        }
                        if (amount.toBigDecimalOrNull() == null || amount.toBigDecimal() <= BigDecimal.ZERO) {
                            error = "Enter a valid amount"
                            return@clickable
                        }
                        val ids = selectedSplitIds.sorted()
                        val inputs = when (splitStrategy) {
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
                                description = description,
                            )
                            repository.createGroupExpense(momentId, body).fold(
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
                        "Save expense",
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
