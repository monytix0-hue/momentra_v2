package com.example.momentra.ui.shell.business.teamops.components

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
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
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object TeamOpsSheetTokens {
    val SheetBg = Color(0xFF161B26)
    val Handle = Color(0xFF625E70)
    val Field = Color(0xFF252230)
    val Border = Color(0xFF322E40)
    val Text = Color(0xFFFFFFFF)
    val Muted = Color(0xFF9E9AA8)
    val FooterHint = Color(0xFF625E70)
    val Error = Color(0xFFF87171)
    val CloseBg = Color(0xFF252230)
    val Indigo = Color(0xFF818CF8)
    val IndigoEnd = Color(0xFF4F46E5)
    val SoftIndigo = Color(0x21818CF8)
    val Lavender = Color(0xFFA78BFA)
    val LavenderEnd = Color(0xFF7C3AED)
    val SoftLavender = Color(0x21A78BFA)
    val Red = Color(0xFFEF4444)
    val RedEnd = Color(0xFFDC2626)
    val SoftRed = Color(0x21EF4444)
    val CtaDark = Color(0xFF14121B)
}

data class TeamOpsSheetAccent(
    val accent: Color,
    val accentEnd: Color,
    val soft: Color,
    val ctaText: Color = TeamOpsSheetTokens.CtaDark,
)

val TeamOpsIndigoAccent = TeamOpsSheetAccent(
    TeamOpsSheetTokens.Indigo,
    TeamOpsSheetTokens.IndigoEnd,
    TeamOpsSheetTokens.SoftIndigo,
)
val TeamOpsLavenderAccent = TeamOpsSheetAccent(
    TeamOpsSheetTokens.Lavender,
    TeamOpsSheetTokens.LavenderEnd,
    TeamOpsSheetTokens.SoftLavender,
)
val TeamOpsRedAccent = TeamOpsSheetAccent(
    TeamOpsSheetTokens.Red,
    TeamOpsSheetTokens.RedEnd,
    TeamOpsSheetTokens.SoftRed,
    ctaText = Color.White,
)

fun teamOpsFormatAmountDisplay(raw: String): String {
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

fun teamOpsStripAmount(display: String): String =
    display.filter { it.isDigit() || it == '.' }

@Composable
fun TeamOpsSheetHandle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(width = 48.dp, height = 4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(TeamOpsSheetTokens.Handle),
    )
}

@Composable
fun TeamOpsSheetHeader(
    @DrawableRes iconRes: Int?,
    emojiFallback: String,
    title: String,
    explanation: String,
    accent: TeamOpsSheetAccent,
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
                    .background(accent.soft),
                contentAlignment = Alignment.Center,
            ) {
                if (iconRes != null) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint = accent.accent,
                        modifier = Modifier.size(20.dp),
                    )
                } else {
                    Text(emojiFallback, fontSize = 18.sp)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    title,
                    color = TeamOpsSheetTokens.Text,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = PlusJakartaSans,
                )
                Text(
                    explanation,
                    color = TeamOpsSheetTokens.Muted,
                    fontSize = 12.sp,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(TeamOpsSheetTokens.CloseBg)
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_biz_create_x),
                contentDescription = "Close",
                tint = TeamOpsSheetTokens.Muted,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
fun TeamOpsFieldLabel(text: String) {
    Text(
        text.uppercase(),
        color = TeamOpsSheetTokens.Muted,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = PlusJakartaSans,
    )
}

@Composable
fun TeamOpsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    accent: TeamOpsSheetAccent,
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
            .background(TeamOpsSheetTokens.Field)
            .border(1.dp, TeamOpsSheetTokens.Border, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        if (value.isEmpty()) {
            Text(
                placeholder,
                color = TeamOpsSheetTokens.Muted.copy(alpha = 0.7f),
                fontSize = 14.sp,
                fontFamily = PlusJakartaSans,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = TextStyle(
                color = TeamOpsSheetTokens.Text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = PlusJakartaSans,
            ),
            cursorBrush = SolidColor(accent.accent),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun TeamOpsAmountField(
    displayValue: String,
    onDisplayChange: (String) -> Unit,
    accent: TeamOpsSheetAccent,
    placeholder: String = "₹ Enter amount",
    modifier: Modifier = Modifier,
) {
    TeamOpsTextField(
        value = if (displayValue.isEmpty()) "" else "₹ $displayValue",
        onValueChange = { raw ->
            val stripped = teamOpsStripAmount(raw.removePrefix("₹").trim())
            onDisplayChange(teamOpsFormatAmountDisplay(stripped))
        },
        placeholder = placeholder,
        accent = accent,
        modifier = modifier,
        keyboardType = KeyboardType.Decimal,
    )
}

@Composable
fun TeamOpsDropdownField(
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
                .background(TeamOpsSheetTokens.Field)
                .border(1.dp, TeamOpsSheetTokens.Border, RoundedCornerShape(12.dp))
                .clickable { expanded = true }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                value.ifBlank { placeholder },
                color = if (value.isBlank()) TeamOpsSheetTokens.Muted.copy(alpha = 0.7f) else TeamOpsSheetTokens.Text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = PlusJakartaSans,
                modifier = Modifier.weight(1f),
            )
            Icon(
                painter = painterResource(R.drawable.ic_biz_create_chevron),
                contentDescription = null,
                tint = TeamOpsSheetTokens.Muted,
                modifier = Modifier.size(14.dp),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(TeamOpsSheetTokens.Field),
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(option, color = TeamOpsSheetTokens.Text, fontFamily = PlusJakartaSans, fontSize = 14.sp)
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
fun TeamOpsDateField(
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
            .background(TeamOpsSheetTokens.Field)
            .border(1.dp, TeamOpsSheetTokens.Border, RoundedCornerShape(12.dp))
            .clickable { showPicker = true }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ges_icon_calendar),
            contentDescription = null,
            tint = TeamOpsSheetTokens.Muted,
            modifier = Modifier.size(16.dp),
        )
        Text(
            display,
            color = TeamOpsSheetTokens.Text,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamOpsTimeField(
    timeHm: String,
    onTimeChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }
    val parsed = runCatching {
        if (timeHm.isBlank()) LocalTime.now() else LocalTime.parse(timeHm)
    }.getOrElse { LocalTime.now() }
    val display = parsed.format(DateTimeFormatter.ofPattern("h:mm a", Locale.US))

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(TeamOpsSheetTokens.Field)
            .border(1.dp, TeamOpsSheetTokens.Border, RoundedCornerShape(12.dp))
            .clickable { showPicker = true }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            display,
            color = TeamOpsSheetTokens.Text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = PlusJakartaSans,
            modifier = Modifier.weight(1f),
        )
    }
    if (showPicker) {
        val state = rememberTimePickerState(
            initialHour = parsed.hour,
            initialMinute = parsed.minute,
            is24Hour = false,
        )
        AlertDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onTimeChange("%02d:%02d".format(state.hour, state.minute))
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            },
            text = { TimePicker(state = state) },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TeamOpsChipRow(
    options: List<String>,
    selected: String,
    accent: TeamOpsSheetAccent,
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
                color = if (isSelected) accent.accent else TeamOpsSheetTokens.Muted,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontFamily = PlusJakartaSans,
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(if (isSelected) accent.soft else TeamOpsSheetTokens.Field)
                    .border(
                        1.dp,
                        if (isSelected) accent.accent else TeamOpsSheetTokens.Border,
                        RoundedCornerShape(100.dp),
                    )
                    .clickable { onSelect(option) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TeamOpsSegmentedControl(
    options: List<String>,
    selected: String,
    accent: TeamOpsSheetAccent,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TeamOpsSheetTokens.Field)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) accent.accent else Color.Transparent)
                    .clickable { onSelect(option) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    option,
                    color = if (isSelected) accent.ctaText else TeamOpsSheetTokens.Muted,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
    }
}

@Composable
fun TeamOpsPrimaryCta(
    label: String,
    enabled: Boolean,
    loading: Boolean,
    footerHint: String,
    accent: TeamOpsSheetAccent,
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
                        Brush.horizontalGradient(listOf(accent.accent, accent.accentEnd))
                    } else {
                        Brush.horizontalGradient(
                            listOf(accent.accent.copy(alpha = 0.35f), accent.accentEnd.copy(alpha = 0.35f)),
                        )
                    },
                )
                .then(if (enabled && !loading) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (loading) {
                CircularProgressIndicator(
                    color = accent.ctaText,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp),
                )
            } else {
                Text(
                    label,
                    color = accent.ctaText.copy(alpha = if (enabled) 1f else 0.55f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
        Text(
            footerHint,
            color = TeamOpsSheetTokens.FooterHint,
            fontSize = 12.sp,
            fontFamily = PlusJakartaSans,
        )
    }
}

@Composable
fun TeamOpsErrorText(message: String?) {
    message?.let {
        Text(it, color = TeamOpsSheetTokens.Error, fontSize = 12.sp, fontFamily = PlusJakartaSans)
    }
}
