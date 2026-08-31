package com.example.momentra.ui.shell.business.ops.components

import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R
import com.example.momentra.ui.setup.SetupDateTimeUtils
import com.example.momentra.ui.theme.PlusJakartaSans
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object OpsSheetTokens {
    val SheetBg = Color(0xFF161B26)
    val Handle = Color(0xFF625E70)
    val Field = Color(0xFF252230)
    val Border = Color(0xFF322E40)
    val Text = Color(0xFFFFFFFF)
    val Muted = Color(0xFF9E9AA8)
    val FooterHint = Color(0xFF625E70)
    val Accent = Color(0xFF818CF8)
    val AccentEnd = Color(0xFF4F46E5)
    val Soft = Color(0x21818CF8)
    val CtaText = Color(0xFF14121B)
    val Error = Color(0xFFF87171)
    val CloseBg = Color(0xFF252230)
}

fun opsFormatAmountDisplay(raw: String): String {
    val cleaned = raw.filter { it.isDigit() || it == '.' }
    if (cleaned.isEmpty()) return ""
    val parts = cleaned.split('.', limit = 2)
    val intPart = parts[0].ifEmpty { "0" }
    val symbols = DecimalFormatSymbols(Locale.US).apply { groupingSeparator = ',' }
    val formatted = try {
        DecimalFormat("#,###", symbols).format(intPart.toLongOrNull() ?: 0L)
    } catch (_: Exception) {
        intPart
    }
    return if (parts.size > 1) "$formatted.${parts[1].take(2)}" else formatted
}

fun opsStripAmount(display: String): String =
    display.filter { it.isDigit() || it == '.' }

@Composable
fun OpsSheetHandle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(width = 48.dp, height = 4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(OpsSheetTokens.Handle),
    )
}

@Composable
fun OpsSheetHeader(
    @DrawableRes iconRes: Int,
    title: String,
    explanation: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(OpsSheetTokens.Soft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = OpsSheetTokens.Accent,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    title,
                    color = OpsSheetTokens.Text,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = PlusJakartaSans,
                )
                Text(
                    explanation,
                    color = OpsSheetTokens.Muted,
                    fontSize = 12.sp,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(OpsSheetTokens.CloseBg)
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_biz_create_x),
                contentDescription = "Close",
                tint = OpsSheetTokens.Muted,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
fun OpsFieldLabel(text: String) {
    Text(
        text.uppercase(),
        color = OpsSheetTokens.Muted,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = PlusJakartaSans,
    )
}

@Composable
fun OpsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minHeight: Int = 44,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(OpsSheetTokens.Field)
            .border(1.dp, OpsSheetTokens.Border, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        if (value.isEmpty()) {
            Text(
                placeholder,
                color = OpsSheetTokens.Muted.copy(alpha = 0.7f),
                fontSize = 14.sp,
                fontFamily = PlusJakartaSans,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = TextStyle(
                color = OpsSheetTokens.Text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = PlusJakartaSans,
            ),
            cursorBrush = SolidColor(OpsSheetTokens.Accent),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun OpsAmountField(
    displayValue: String,
    onDisplayChange: (String) -> Unit,
    placeholder: String = "₹ Enter amount",
    modifier: Modifier = Modifier,
) {
    OpsTextField(
        value = if (displayValue.isEmpty()) "" else "₹ $displayValue",
        onValueChange = { raw ->
            val stripped = opsStripAmount(raw.removePrefix("₹").trim())
            onDisplayChange(opsFormatAmountDisplay(stripped))
        },
        placeholder = placeholder,
        modifier = modifier,
        keyboardType = KeyboardType.Decimal,
    )
}

@Composable
fun OpsDropdownField(
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(OpsSheetTokens.Field)
                .border(1.dp, OpsSheetTokens.Border, RoundedCornerShape(12.dp))
                .clickable { expanded = true }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                value.ifBlank { placeholder },
                color = if (value.isBlank()) OpsSheetTokens.Muted.copy(alpha = 0.7f) else OpsSheetTokens.Text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = PlusJakartaSans,
                modifier = Modifier.weight(1f),
            )
            Icon(
                painter = painterResource(R.drawable.ic_biz_create_chevron),
                contentDescription = null,
                tint = OpsSheetTokens.Muted,
                modifier = Modifier.size(14.dp),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(OpsSheetTokens.Field),
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            option,
                            color = OpsSheetTokens.Text,
                            fontFamily = PlusJakartaSans,
                            fontSize = 14.sp,
                        )
                    },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpsDateField(
    isoDate: String,
    onIsoDateChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }
    val date = SetupDateTimeUtils.parseIsoDate(isoDate) ?: LocalDate.now()
    val today = LocalDate.now()
    val display = if (date == today) {
        "Today (${date.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US))})"
    } else {
        date.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US))
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(OpsSheetTokens.Field)
            .border(1.dp, OpsSheetTokens.Border, RoundedCornerShape(12.dp))
            .clickable { showPicker = true }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ges_icon_calendar),
            contentDescription = null,
            tint = OpsSheetTokens.Muted,
            modifier = Modifier.size(16.dp),
        )
        Text(
            display,
            color = OpsSheetTokens.Text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = PlusJakartaSans,
            modifier = Modifier.weight(1f),
        )
    }
    if (showPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = SetupDateTimeUtils.localDateToMillis(date),
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onIsoDateChange(
                            SetupDateTimeUtils.localDateToIso(SetupDateTimeUtils.millisToLocalDate(millis)),
                        )
                    }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = state)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OpsChipRow(
    options: List<String>,
    selected: String,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            val isSelected = option.equals(selected, ignoreCase = true)
            Text(
                option,
                color = if (isSelected) OpsSheetTokens.Accent else OpsSheetTokens.Muted,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontFamily = PlusJakartaSans,
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(if (isSelected) OpsSheetTokens.Soft else OpsSheetTokens.Field)
                    .border(
                        1.dp,
                        if (isSelected) OpsSheetTokens.Accent else OpsSheetTokens.Border,
                        RoundedCornerShape(100.dp),
                    )
                    .clickable { onSelect(option) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
fun OpsPrimaryCta(
    label: String,
    enabled: Boolean,
    loading: Boolean,
    footerHint: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (enabled && !loading) {
                        Brush.horizontalGradient(listOf(OpsSheetTokens.Accent, OpsSheetTokens.AccentEnd))
                    } else {
                        Brush.horizontalGradient(
                            listOf(OpsSheetTokens.Accent.copy(alpha = 0.35f), OpsSheetTokens.AccentEnd.copy(alpha = 0.35f)),
                        )
                    },
                )
                .then(if (enabled && !loading) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (loading) {
                CircularProgressIndicator(
                    color = OpsSheetTokens.CtaText,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp),
                )
            } else {
                Text(
                    label,
                    color = OpsSheetTokens.CtaText.copy(alpha = if (enabled) 1f else 0.55f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
        Text(
            footerHint,
            color = OpsSheetTokens.FooterHint,
            fontSize = 12.sp,
            fontFamily = PlusJakartaSans,
        )
    }
}

@Composable
fun OpsErrorText(message: String?) {
    message?.let {
        Text(it, color = OpsSheetTokens.Error, fontSize = 12.sp, fontFamily = PlusJakartaSans)
    }
}
