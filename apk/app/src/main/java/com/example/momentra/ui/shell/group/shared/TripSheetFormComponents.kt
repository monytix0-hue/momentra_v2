package com.example.momentra.ui.shell.group.shared

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.momentra.data.api.GroupParticipantDto
import com.example.momentra.ui.setup.SetupDateTimeUtils
import com.example.momentra.ui.shell.empty.group.InviteSendChannelRow
import com.example.momentra.ui.theme.PlusJakartaSans
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

internal object TripFormTokens {
    val Teal = Color(0xFF14B8A6)
    val Purple = Color(0xFFA855F7)
    val Blue = Color(0xFF3B82F6)
    val Pink = Color(0xFFEC4899)
    val Green = Color(0xFF10B981)
    val AvatarColors = listOf(
        Color(0xFFFDBA74), Color(0xFF86EFAC), Color(0xFFF9A8D4), Color(0xFF93C5FD),
    )
}

@Composable
internal fun TripFieldLabel(text: String) {
    Text(
        text.uppercase(Locale.US),
        color = TripSheetTokens.Muted,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = PlusJakartaSans,
    )
}

@Composable
internal fun TripAmountField(
    value: String,
    onValueChange: (String) -> Unit,
    accent: Color = TripSheetTokens.Accent,
    currencySymbol: String = "₹",
    testTag: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TripSheetTokens.Field)
            .border(1.dp, TripSheetTokens.Border, RoundedCornerShape(12.dp))
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(currencySymbol, color = accent, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
        BasicTextField(
            value = value,
            onValueChange = { onValueChange(it.filter { c -> c.isDigit() || c == '.' }) },
            textStyle = TextStyle(color = TripSheetTokens.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            cursorBrush = SolidColor(accent),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
internal fun TripSheetField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean = true,
    minHeight: Int = 44,
    keyboardType: KeyboardType = KeyboardType.Text,
    accent: Color = TripFormTokens.Purple,
    testTag: String? = null,
    readOnly: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(TripSheetTokens.Field)
            .border(1.dp, TripSheetTokens.Border, RoundedCornerShape(8.dp))
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        if (value.isEmpty()) {
            Text(placeholder, color = TripSheetTokens.Muted.copy(alpha = 0.7f), fontSize = 14.sp, fontFamily = PlusJakartaSans)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            readOnly = readOnly,
            enabled = onClick == null,
            textStyle = TextStyle(color = TripSheetTokens.Text, fontSize = 14.sp, fontFamily = PlusJakartaSans),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            cursorBrush = SolidColor(accent),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
internal fun TripInviteLinkRow(
    minting: Boolean,
    displayPath: String?,
    mintError: String?,
    copied: Boolean,
    onCopy: () -> Unit,
    onShareLink: (() -> Unit)? = null,
) {
    when {
        minting -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(TripSheetTokens.Field)
                    .border(1.dp, TripSheetTokens.Border, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CircularProgressIndicator(
                    color = TripFormTokens.Purple,
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                )
                Text("Minting invite…", color = TripSheetTokens.Muted, fontSize = 13.sp, fontFamily = PlusJakartaSans)
            }
        }
        displayPath != null -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(TripSheetTokens.Field)
                    .border(1.dp, TripSheetTokens.Border, RoundedCornerShape(8.dp)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    displayPath,
                    color = TripSheetTokens.Text,
                    fontSize = 13.sp,
                    fontFamily = PlusJakartaSans,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp, top = 12.dp, bottom = 12.dp),
                )
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(TripSheetTokens.Accent)
                        .clickable(onClick = onCopy)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(
                        if (copied) "Copied" else "Copy",
                        color = TripSheetTokens.Text,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                    )
                }
                if (onShareLink != null) {
                    Box(
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, TripSheetTokens.Accent, RoundedCornerShape(8.dp))
                            .clickable(onClick = onShareLink)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text(
                            "Share",
                            color = TripSheetTokens.Accent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PlusJakartaSans,
                        )
                    }
                }
            }
        }
        else -> {
            Text(
                mintError ?: "Invite unavailable",
                color = Color(0xFFF87171),
                fontSize = 12.sp,
                fontFamily = PlusJakartaSans,
            )
        }
    }
}

@Composable
internal fun TripInviteQrBlock(
    qrBitmap: ImageBitmap?,
    minting: Boolean,
    accent: Color = TripSheetTokens.Accent,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TripFieldLabel("Or scan to join")
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(16.dp),
        ) {
            when {
                minting -> {
                    Box(
                        Modifier.size(144.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            color = accent,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
                qrBitmap != null -> {
                    Image(
                        bitmap = qrBitmap,
                        contentDescription = "Invite QR code",
                        modifier = Modifier.size(144.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
                else -> {
                    Box(
                        Modifier.size(144.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "QR unavailable",
                            color = TripSheetTokens.Muted,
                            fontSize = 12.sp,
                            fontFamily = PlusJakartaSans,
                        )
                    }
                }
            }
        }
        Text(
            "Scan with Momentra app to join instantly",
            color = TripSheetTokens.Muted,
            fontSize = 11.sp,
            fontFamily = PlusJakartaSans,
        )
    }
}

@Composable
internal fun TripOutlineActionButton(
    label: String,
    enabled: Boolean,
    accent: Color = TripSheetTokens.Accent,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stroke = if (enabled) accent else accent.copy(alpha = 0.35f)
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, stroke, RoundedCornerShape(14.dp))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Share,
            contentDescription = null,
            tint = stroke,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            color = stroke,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun TripInviteShareSection(
    minting: Boolean,
    displayPath: String?,
    mintError: String?,
    copied: Boolean,
    copyText: String?,
    qrBitmap: ImageBitmap?,
    onCopy: () -> Unit,
    onShareLink: () -> Unit,
    onShareQr: () -> Unit,
    onMessages: (() -> Unit)? = null,
    onWhatsApp: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            TripFieldLabel("Share Invitation Link")
            TripInviteLinkRow(
                minting = minting,
                displayPath = displayPath,
                mintError = mintError,
                copied = copied,
                onCopy = onCopy,
                onShareLink = if (copyText != null && !minting) onShareLink else null,
            )
        }
        if (onMessages != null && onWhatsApp != null) {
            InviteSendChannelRow(
                enabled = copyText != null && !minting,
                accent = TripSheetTokens.Accent,
                onMessages = onMessages,
                onWhatsApp = onWhatsApp,
            )
        }
        TripInviteQrBlock(qrBitmap = qrBitmap, minting = minting)
        TripOutlineActionButton(
            label = "Share QR",
            enabled = qrBitmap != null && copyText != null,
            onClick = onShareQr,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
internal fun TripParticipantListRow(
    displayName: String,
    status: String,
    roleLabel: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(TripSheetTokens.Field)
            .border(1.dp, TripSheetTokens.Border, RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(TripSheetTokens.Accent.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                tripInitials(displayName),
                color = TripSheetTokens.Accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(displayName, color = TripSheetTokens.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
            Text(status, color = TripSheetTokens.Muted, fontSize = 11.sp, fontFamily = PlusJakartaSans)
        }
        Text(
            roleLabel,
            color = TripFormTokens.Green,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(TripFormTokens.Green.copy(alpha = 0.12f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
internal fun TripPaidByField(
    participants: List<GroupParticipantDto>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    accent: Color = TripSheetTokens.Accent,
    testTag: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = participants.firstOrNull { it.participantId == selectedId }?.displayName
        ?: participants.firstOrNull()?.displayName
        ?: "Select"
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(TripSheetTokens.Field)
                .border(1.dp, TripSheetTokens.Border, RoundedCornerShape(8.dp))
                .clickable { expanded = true }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(selectedName, color = TripSheetTokens.Text, fontSize = 14.sp, fontFamily = PlusJakartaSans, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Icon(painter = painterResource(com.example.momentra.R.drawable.ic_biz_create_chevron), contentDescription = null, tint = TripSheetTokens.Muted, modifier = Modifier.size(16.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            participants.forEach { p ->
                DropdownMenuItem(
                    text = { Text(p.displayName ?: p.participantId.take(8), fontFamily = PlusJakartaSans) },
                    onClick = {
                        onSelect(p.participantId)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TripDatePickField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "Pick date",
) {
    var show by remember { mutableStateOf(false) }
    val display = value.takeIf { it.isNotBlank() }?.let {
        runCatching { LocalDate.parse(it.take(10)).format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)) }.getOrDefault(it)
    }.orEmpty()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(TripSheetTokens.Field)
            .border(1.dp, TripSheetTokens.Border, RoundedCornerShape(8.dp))
            .clickable { show = true }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            if (display.isBlank()) placeholder else display,
            color = if (display.isBlank()) TripSheetTokens.Muted.copy(alpha = 0.7f) else TripSheetTokens.Text,
            fontSize = 14.sp,
            fontFamily = PlusJakartaSans,
        )
    }
    if (show) {
        val initial = SetupDateTimeUtils.parseIsoDate(value) ?: LocalDate.now()
        val state = rememberDatePickerState(initialSelectedDateMillis = SetupDateTimeUtils.localDateToMillis(initial))
        DatePickerDialog(
            onDismissRequest = { show = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onValueChange(SetupDateTimeUtils.localDateToIso(SetupDateTimeUtils.millisToLocalDate(millis)))
                    }
                    show = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { show = false }) { Text("Cancel") } },
        ) { DatePicker(state = state) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TripTimePickField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "Pick time",
) {
    var show by remember { mutableStateOf(false) }
    val display = value.takeIf { it.isNotBlank() }?.let {
        runCatching { LocalTime.parse(it).format(DateTimeFormatter.ofPattern("h:mm a", Locale.US)) }.getOrDefault(it)
    }.orEmpty()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(TripSheetTokens.Field)
            .border(1.dp, TripSheetTokens.Border, RoundedCornerShape(8.dp))
            .clickable { show = true }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (display.isBlank()) placeholder else display,
            color = if (display.isBlank()) TripSheetTokens.Muted.copy(alpha = 0.7f) else TripSheetTokens.Text,
            fontSize = 14.sp,
            fontFamily = PlusJakartaSans,
        )
    }
    if (show) {
        val parsed = runCatching { LocalTime.parse(value) }.getOrNull() ?: LocalTime.now()
        val state = rememberTimePickerState(initialHour = parsed.hour, initialMinute = parsed.minute, is24Hour = false)
        Dialog(onDismissRequest = { show = false }) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(TripSheetTokens.Bg)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TimePicker(state = state)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.align(Alignment.End)) {
                    TextButton(onClick = { show = false }) { Text("Cancel") }
                    TextButton(onClick = {
                        onValueChange(LocalTime.of(state.hour, state.minute).toString())
                        show = false
                    }) { Text("OK") }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TripChipRow(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    accent: Color = TripSheetTokens.Accent,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            val on = selected == option
            Text(
                option,
                color = if (on) accent else TripSheetTokens.Muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (on) accent.copy(alpha = 0.15f) else TripSheetTokens.Field)
                    .border(1.dp, if (on) accent else TripSheetTokens.Border, RoundedCornerShape(999.dp))
                    .clickable { onSelect(option) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
internal fun TripSegmentedControl(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    accent: Color = TripSheetTokens.Accent,
    testTag: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(TripSheetTokens.Field)
            .border(1.dp, TripSheetTokens.Border, RoundedCornerShape(10.dp))
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .padding(2.dp),
    ) {
        options.forEach { option ->
            val on = selected == option
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (on) accent else Color.Transparent)
                    .clickable { onSelect(option) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    option,
                    color = if (on) TripSheetTokens.Text else TripSheetTokens.Muted,
                    fontSize = 12.sp,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
    }
}

@Composable
internal fun TripParticipantPicker(
    participants: List<GroupParticipantDto>,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit,
    accent: Color = TripSheetTokens.Accent,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        participants.forEachIndexed { index, p ->
            val on = selectedIds.contains(p.participantId)
            val name = p.displayName ?: p.participantId.take(8)
            val color = TripFormTokens.AvatarColors[index % TripFormTokens.AvatarColors.size]
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.clickable { onToggle(p.participantId) },
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(if (on) 2.dp else 0.dp, accent, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(tripInitials(name), color = Color(0xFF14121B), fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                    }
                    if (on) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(accent),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("✓", color = Color.White, fontSize = 9.sp)
                        }
                    }
                }
                Text(
                    name.split(" ").firstOrNull() ?: name,
                    color = if (on) TripSheetTokens.Text else TripSheetTokens.Muted,
                    fontSize = 11.sp,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
    }
}

@Composable
internal fun TripSheetHeaderRow(
    title: String,
    subtitle: String,
    iconRes: Int,
    accent: Color,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(accent.copy(alpha = 0.18f))
                .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painter = painterResource(iconRes), contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = TripSheetTokens.Text, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PlusJakartaSans)
            Text(subtitle, color = TripSheetTokens.Muted, fontSize = 12.sp, fontFamily = PlusJakartaSans)
        }
    }
}

@Composable
internal fun TripPrimaryCta(
    label: String,
    enabled: Boolean,
    loading: Boolean,
    footer: String? = null,
    gradient: List<Color> = listOf(TripSheetTokens.Accent, TripSheetTokens.AccentEnd),
    testTag: String? = null,
    onClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.horizontalGradient(gradient))
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
                .then(if (enabled && !loading) Modifier.clickable(onClick = onClick) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            if (loading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            } else {
                Text(label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
            }
        }
        footer?.let {
            Text(it, color = TripSheetTokens.Muted, fontSize = 11.sp, fontFamily = PlusJakartaSans, modifier = Modifier.fillMaxWidth())
        }
    }
}

internal fun tripInitials(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}".uppercase(Locale.US)
        parts.isNotEmpty() -> parts[0].take(2).uppercase(Locale.US)
        else -> "??"
    }
}
