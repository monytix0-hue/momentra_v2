package com.example.momentra.ui.shell.business.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.CreateBusinessExpenseBody
import com.example.momentra.data.repository.BusinessSliceRepository
import com.example.momentra.ui.shell.maestro.MaestroIds
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.launch
import com.example.momentra.ui.shell.group.wedding.create.FieldLabel
import com.example.momentra.ui.shell.group.wedding.create.SheetField

private val Indigo = Color(0xFF818CF8)
private val Bg = Color(0xFF131313)
private val Card = Color(0xFF1C1C1E)
private val Border = Color(0xFF2C2C2E)
private val TextPrimary = Color(0xFFE5E2E1)
private val TextSecondary = Color(0xFFA1A1AA)
private val Red = Color(0xFFF87171)

private val Categories = listOf("PURCHASE", "EXPENSE")

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
    var amount by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("INR") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("PURCHASE") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

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
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Business expense",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = PlusJakartaSans,
            )
            Text(
                "Posted to company finance. Approval may apply when thresholds are set.",
                color = TextSecondary,
                fontSize = 12.sp,
                fontFamily = PlusJakartaSans,
            )

            FieldLabel("Amount")
            SheetField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                placeholder = "0.00",
                keyboardType = KeyboardType.Decimal,
                testTag = MaestroIds.BUSINESS_EXPENSE_AMOUNT,
            )
            FieldLabel("Currency")
            SheetField(
                value = currency,
                onValueChange = { currency = it.uppercase().take(3) },
                placeholder = "INR",
            )
            FieldLabel("Category")
            FlowRow(
                modifier = Modifier.testTag(MaestroIds.BUSINESS_EXPENSE_CATEGORY),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Categories.forEach { code ->
                    CategoryChip(
                        label = code,
                        selected = category == code,
                        onClick = { category = code },
                    )
                }
            }
            FieldLabel("Description (optional)")
            SheetField(
                value = description,
                onValueChange = { description = it },
                placeholder = "Software, travel…",
                testTag = MaestroIds.BUSINESS_EXPENSE_NOTE,
            )

            error?.let {
                Text(it, color = Red, fontSize = 12.sp, fontFamily = PlusJakartaSans)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (submitting) Indigo.copy(alpha = 0.45f) else Indigo)
                    .testTag(MaestroIds.BUSINESS_EXPENSE_SUBMIT)
                    .clickable(enabled = !submitting) {
                        val amt = amount.trim()
                        if (amt.isEmpty()) {
                            error = "Enter an amount"
                            return@clickable
                        }
                        submitting = true
                        error = null
                        scope.launch {
                            repository.createExpense(
                                momentId = momentId,
                                body = CreateBusinessExpenseBody(
                                    amount = amt,
                                    currencyCode = currency.ifBlank { "INR" }.uppercase(),
                                    description = description.takeIf { it.isNotBlank() },
                                    categoryCode = category,
                                ),
                            ).fold(
                                onSuccess = {
                                    submitting = false
                                    onSaved()
                                    onDismiss()
                                },
                                onFailure = {
                                    submitting = false
                                    error = it.message ?: "Could not save expense"
                                },
                            )
                        }
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (submitting) "Saving…" else "Save expense",
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
private fun FieldLabel(text: String) {
    Text(
        text,
        color = TextSecondary,
        fontSize = 12.sp,
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
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Card)
            .border(1.dp, Border, RoundedCornerShape(12.dp))
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        if (value.isEmpty()) {
            Text(placeholder, color = TextSecondary.copy(alpha = 0.6f), fontSize = 14.sp, fontFamily = PlusJakartaSans)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                color = TextPrimary,
                fontSize = 14.sp,
                fontFamily = PlusJakartaSans,
            ),
            cursorBrush = SolidColor(Indigo),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) Indigo.copy(alpha = 0.25f) else Card)
            .border(1.dp, if (selected) Indigo else Border, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            label,
            color = if (selected) Indigo else TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
        )
    }
}
