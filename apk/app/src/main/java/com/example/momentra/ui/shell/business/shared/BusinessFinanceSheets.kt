package com.example.momentra.ui.shell.business.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.example.momentra.data.api.BusinessInvoiceLineDto
import com.example.momentra.data.api.CreateBusinessInvoiceBody
import com.example.momentra.data.api.CreateBusinessRevenueBody
import com.example.momentra.data.repository.BusinessSliceRepository
import com.example.momentra.ui.shell.maestro.MaestroIds
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.launch
import java.time.LocalDate

private val Teal = Color(0xFF14B8A6)
private val Blue = Color(0xFF3B82F6)
private val Bg = Color(0xFF131313)
private val Card = Color(0xFF1C1C1E)
private val Border = Color(0xFF2C2C2E)
private val TextPrimary = Color(0xFFE5E2E1)
private val TextSecondary = Color(0xFFA1A1AA)
private val Red = Color(0xFFF87171)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessRevenueSheet(
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
                "Log revenue",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = PlusJakartaSans,
            )
            FinanceField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                placeholder = "0.00",
                keyboardType = KeyboardType.Decimal,
                testTag = MaestroIds.BUSINESS_REVENUE_AMOUNT,
            )
            FinanceField(
                value = currency,
                onValueChange = { currency = it.uppercase().take(3) },
                placeholder = "INR",
            )
            FinanceField(
                value = description,
                onValueChange = { description = it },
                placeholder = "Description (optional)",
            )
            error?.let {
                Text(it, color = Red, fontSize = 12.sp, fontFamily = PlusJakartaSans)
            }
            SubmitButton(
                label = if (submitting) "Saving…" else "Save revenue",
                accent = Teal,
                enabled = !submitting && amount.isNotBlank(),
                testTag = MaestroIds.BUSINESS_REVENUE_SUBMIT,
                onClick = {
                    submitting = true
                    error = null
                    scope.launch {
                        repository.createRevenue(
                            momentId = momentId,
                            body = CreateBusinessRevenueBody(
                                amount = amount.trim(),
                                currencyCode = currency.ifBlank { "INR" }.uppercase(),
                                description = description.takeIf { it.isNotBlank() },
                            ),
                        ).fold(
                            onSuccess = {
                                submitting = false
                                onSaved()
                                onDismiss()
                            },
                            onFailure = {
                                submitting = false
                                error = it.message ?: "Could not save revenue"
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
fun BusinessInvoiceSheet(
    momentId: String,
    visible: Boolean,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    repository: BusinessSliceRepository = remember { BusinessSliceRepository() },
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var invoiceNumber by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("INR") }
    var lineDescription by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var unitPrice by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val today = LocalDate.now().toString()

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
                "Track invoice",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = PlusJakartaSans,
            )
            FinanceField(
                value = invoiceNumber,
                onValueChange = { invoiceNumber = it },
                placeholder = "Invoice #",
                testTag = MaestroIds.BUSINESS_INVOICE_CUSTOMER,
            )
            FinanceField(
                value = lineDescription,
                onValueChange = { lineDescription = it },
                placeholder = "Line description",
            )
            FinanceField(
                value = quantity,
                onValueChange = { quantity = it.filter { c -> c.isDigit() || c == '.' } },
                placeholder = "Qty",
            )
            FinanceField(
                value = unitPrice,
                onValueChange = { unitPrice = it.filter { c -> c.isDigit() || c == '.' } },
                placeholder = "Unit price",
            )
            FinanceField(
                value = currency,
                onValueChange = { currency = it.uppercase().take(3) },
                placeholder = "INR",
            )
            Text(
                "Tax is server-authoritative (omit or 0).",
                color = TextSecondary,
                fontSize = 11.sp,
                fontFamily = PlusJakartaSans,
            )
            error?.let {
                Text(it, color = Red, fontSize = 12.sp, fontFamily = PlusJakartaSans)
            }
            val canSubmit = invoiceNumber.isNotBlank() && lineDescription.isNotBlank() &&
                quantity.isNotBlank() && unitPrice.isNotBlank() && currency.length == 3
            SubmitButton(
                label = if (submitting) "Saving…" else "Save invoice",
                accent = Blue,
                enabled = !submitting && canSubmit,
                testTag = MaestroIds.BUSINESS_INVOICE_SUBMIT,
                onClick = {
                    submitting = true
                    error = null
                    scope.launch {
                        repository.createInvoice(
                            momentId = momentId,
                            body = CreateBusinessInvoiceBody(
                                invoiceNumber = invoiceNumber.trim(),
                                invoiceDate = today,
                                currencyCode = currency.uppercase(),
                                lines = listOf(
                                    BusinessInvoiceLineDto(
                                        description = lineDescription.trim(),
                                        quantity = quantity.trim(),
                                        unitPrice = unitPrice.trim(),
                                        taxAmount = null,
                                    ),
                                ),
                            ),
                        ).fold(
                            onSuccess = {
                                submitting = false
                                onSaved()
                                onDismiss()
                            },
                            onFailure = {
                                submitting = false
                                error = it.message ?: "Could not save invoice"
                            },
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun FinanceField(
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
            textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp, fontFamily = PlusJakartaSans),
            cursorBrush = SolidColor(Teal),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SubmitButton(
    label: String,
    accent: Color,
    enabled: Boolean,
    testTag: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) accent else accent.copy(alpha = 0.45f))
            .testTag(testTag)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = PlusJakartaSans)
    }
}
