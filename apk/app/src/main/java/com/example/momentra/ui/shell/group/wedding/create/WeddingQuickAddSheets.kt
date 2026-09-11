package com.example.momentra.ui.shell.group.wedding.create

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.momentra.R
import com.example.momentra.data.api.CreateGroupExpenseBody
import com.example.momentra.data.api.GroupContributionItemDto
import com.example.momentra.data.api.GroupExpenseSplitInputDto
import com.example.momentra.data.api.GroupLifePlanningItemDto
import com.example.momentra.data.api.GroupParticipantDto
import com.example.momentra.data.repository.GroupExpenseSplitBuilder
import com.example.momentra.data.repository.GroupSliceRepository
import com.example.momentra.ui.shell.components.MomentraModalBottomSheet
import com.example.momentra.ui.shell.empty.group.GroupBudgetUtils
import com.example.momentra.ui.shell.group.shared.contributionPaymentMethodLabel
import com.example.momentra.ui.shell.group.shared.ExperienceChecklistSheetBody
import com.example.momentra.ui.shell.group.shared.GroupExpenseCategoryCatalog
import com.example.momentra.ui.shell.group.shared.GroupPlanningCategoryCatalog
import com.example.momentra.ui.shell.group.shared.GroupReceiptPickControl
import com.example.momentra.ui.shell.group.shared.GroupSettlementSheet
import com.example.momentra.ui.shell.group.shared.GroupTabDataCache
import com.example.momentra.ui.shell.group.shared.encodeMemoryPhotoBytes
import com.example.momentra.ui.shell.group.shared.tryTakePersistableReadPermission
import com.example.momentra.ui.shell.group.shared.tripDateTimeToIso
import com.example.momentra.ui.shell.group.shared.tripIsoToLocalDateTime
import com.example.momentra.ui.shell.group.shared.QuickAddDraftActions
import com.example.momentra.ui.shell.group.shared.TravelCurrencyCatalog
import com.example.momentra.ui.shell.shared.loadGroupCurrencyContext
import com.example.momentra.ui.shell.shared.TravelCurrencyPickerRow
import com.example.momentra.ui.shell.maestro.MaestroIds
import com.example.momentra.ui.setup.SetupDateTimeUtils
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/** Figma 589:8755 sheet surface tokens. */
private object Wq {
    val Sheet = Color(0xFF1C1A24)
    val Field = Color(0xFF252230)
    val Border = Color(0xFF322E40)
    val Muted = Color(0xFF9E9AA8)
    val Text = Color(0xFFFFFFFF)
    val Ink = Color(0xFF14121B)
    val Purple = Color(0xFFBF26D4)
    val PurpleEnd = Color(0xFF871A8F)
    val Coral = Color(0xFFFC7085)
    val CoralEnd = Color(0xFFE83359)
    val SoftPink = Color(0xFFED8CB8)
    val SoftPinkEnd = Color(0xFFD14D85)
    val Contrib = Color(0xFFD945F0)
    val ContribEnd = Color(0xFFA31CB0)
    val Handle = Color(0xFF625E70)
    val AvatarColors = listOf(
        Color(0xFFFDBA74), Color(0xFF86EFAC), Color(0xFFF9A8D4), Color(0xFF93C5FD),
    )
}

data class SheetAccent(
    val accent: Color,
    val accentEnd: Color,
    val soft: Color,
) {
    val cta = Brush.horizontalGradient(listOf(accent, accentEnd))
}

internal val PurpleAccent = SheetAccent(Wq.Purple, Wq.PurpleEnd, Color(0x21BF26D4))
internal val CoralAccent = SheetAccent(Wq.Coral, Wq.CoralEnd, Color(0x21FC7085))
internal val SoftPinkAccent = SheetAccent(Wq.SoftPink, Wq.SoftPinkEnd, Color(0x21ED8CB8))
internal val ContribAccent = SheetAccent(Wq.Contrib, Wq.ContribEnd, Color(0x26D945F0))

/**
 * Figma 589:8755 — Wedding Quick Add sheets (exact layout).
 * Live kinds (expense/contribution/budget) submit when APIs allow;
 * gap kinds render full Figma UI with no-op CTA (no invented backends).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeddingGapQuickAddSheet(
    kind: WeddingQuickAddKind,
    visible: Boolean,
    onDismiss: () -> Unit,
    momentId: String? = null,
    onSaved: () -> Unit = {},
    repository: GroupSliceRepository = remember { GroupSliceRepository() },
) {
    if (!visible) return

    MomentraModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Wq.Sheet,
        skipPartiallyExpanded = false,
        dragHandle = null,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 48.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Wq.Handle),
            )
            when (kind) {
                WeddingQuickAddKind.EXPENSE -> WeddingExpenseSheetBody(momentId, repository, onDismiss, onSaved)
                WeddingQuickAddKind.CONTRIBUTION -> WeddingContributionSheetBody(momentId, repository, onDismiss, onSaved)
                WeddingQuickAddKind.BUDGET -> WeddingBudgetSheetBody(momentId, repository, onDismiss, onSaved)
                WeddingQuickAddKind.PARTICIPANT -> WeddingParticipantSheetBody(momentId, repository, onDismiss, onSaved)
                WeddingQuickAddKind.VENDOR -> WeddingVendorSheetBody(momentId, repository, onDismiss, onSaved)
                WeddingQuickAddKind.PLANNING -> WeddingPlanningSheetBody(momentId, repository, onDismiss, onSaved, momentTypeCode = "WEDDING")
                WeddingQuickAddKind.CHECKLIST -> ExperienceChecklistSheetBody(
                    momentId = momentId,
                    repository = repository,
                    onDismiss = onDismiss,
                    onSaved = onSaved,
                    accent = SoftPinkAccent,
                )
                WeddingQuickAddKind.ATTENDANCE -> WeddingAttendanceSheetBody(momentId, repository, onDismiss, onSaved)
                WeddingQuickAddKind.POLL -> WeddingPollSheetBody(momentId, repository, onDismiss, onSaved)
                WeddingQuickAddKind.MEMORY -> WeddingMemorySheetBody(momentId, repository, onDismiss, onSaved)
                WeddingQuickAddKind.UPDATE -> WeddingUpdateSheetBody(momentId, repository, onDismiss, onSaved)
                WeddingQuickAddKind.SETTLE -> WeddingSettleSheetBody(momentId, onDismiss, onSaved)
            }
        }
    }
}

@Composable
internal fun SheetHeader(
    @DrawableRes iconRes: Int,
    title: String,
    subtitle: String? = null,
    accent: SheetAccent = PurpleAccent,
    iconSize: Int = 20,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(accent.soft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = accent.accent,
                modifier = Modifier.size(iconSize.dp),
            )
        }
        Column {
            Text(title, color = Wq.Text, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PlusJakartaSans)
            subtitle?.let {
                Text(it, color = Wq.Muted, fontSize = 12.sp, fontFamily = PlusJakartaSans)
            }
        }
    }
}

@Composable
internal fun FieldLabel(text: String) {
    Text(
        text.uppercase(),
        color = Wq.Muted,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = PlusJakartaSans,
    )
}

@Composable
internal fun SheetField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minHeight: Int = 48,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    textColor: Color = Wq.Text,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Wq.Field)
            .border(1.dp, Wq.Border, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        leading?.invoke()
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(placeholder, color = Wq.Muted.copy(alpha = 0.7f), fontSize = 14.sp, fontFamily = PlusJakartaSans)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = singleLine,
                textStyle = TextStyle(color = textColor, fontSize = 14.sp, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Medium),
                cursorBrush = SolidColor(Wq.Purple),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        trailing?.invoke()
    }
}

private val weddingDateDisplay = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)
private val weddingTimeDisplay = DateTimeFormatter.ofPattern("hh:mm a", Locale.US)

private fun formatWeddingDate(iso: String): String {
    if (iso.isBlank()) return ""
    return SetupDateTimeUtils.parseIsoDate(iso)?.format(weddingDateDisplay) ?: iso
}

private fun formatWeddingTime(iso: String): String {
    if (iso.isBlank()) return ""
    return runCatching {
        LocalTime.parse(iso).format(weddingTimeDisplay)
    }.getOrElse { iso }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeddingDatePickField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "Select date",
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }
    val display = formatWeddingDate(value)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Wq.Field)
            .border(1.dp, Wq.Border, RoundedCornerShape(12.dp))
            .clickable { showPicker = true }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            painterResource(R.drawable.ges_icon_calendar),
            contentDescription = null,
            tint = Wq.Muted,
            modifier = Modifier.size(18.dp),
        )
        Text(
            if (display.isBlank()) placeholder else display,
            color = if (display.isBlank()) Wq.Muted.copy(alpha = 0.7f) else Wq.Text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = PlusJakartaSans,
            modifier = Modifier.weight(1f),
        )
    }
    if (showPicker) {
        val initial = SetupDateTimeUtils.parseIsoDate(value) ?: LocalDate.now()
        val state = rememberDatePickerState(
            initialSelectedDateMillis = SetupDateTimeUtils.localDateToMillis(initial),
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onValueChange(SetupDateTimeUtils.localDateToIso(SetupDateTimeUtils.millisToLocalDate(millis)))
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
private fun WeddingTimePickField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "Select time",
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }
    val display = formatWeddingTime(value)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Wq.Field)
            .border(1.dp, Wq.Border, RoundedCornerShape(12.dp))
            .clickable { showPicker = true }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            if (display.isBlank()) placeholder else display,
            color = if (display.isBlank()) Wq.Muted.copy(alpha = 0.7f) else Wq.Text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = PlusJakartaSans,
            modifier = Modifier.weight(1f),
        )
    }
    if (showPicker) {
        val initial = runCatching { if (value.isBlank()) LocalTime.of(9, 0) else LocalTime.parse(value) }
            .getOrElse { LocalTime.of(9, 0) }
        val timeState = rememberTimePickerState(
            initialHour = initial.hour,
            initialMinute = initial.minute,
            is24Hour = false,
        )
        Dialog(onDismissRequest = { showPicker = false }) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Wq.Sheet)
                    .border(1.dp, Wq.Border, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Select time", color = Wq.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                TimePicker(state = timeState)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { showPicker = false }) { Text("Cancel") }
                    TextButton(onClick = {
                        onValueChange(SetupDateTimeUtils.localTimeToIso(LocalTime.of(timeState.hour, timeState.minute)))
                        showPicker = false
                    }) { Text("OK") }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ChipRow(
    options: List<String>,
    selected: String,
    accent: SheetAccent = PurpleAccent,
    testTag: String? = null,
    onSelect: (String) -> Unit,
) {
    FlowRow(
        modifier = if (testTag != null) Modifier.testTag(testTag) else Modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            val on = selected == option
            Text(
                option,
                color = if (on) accent.accent else Wq.Muted,
                fontSize = 12.sp,
                fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                fontFamily = PlusJakartaSans,
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(if (on) accent.soft else Wq.Field)
                    .border(1.dp, if (on) accent.accent else Wq.Border, RoundedCornerShape(100.dp))
                    .clickable { onSelect(option) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
internal fun Segmented(
    options: List<String>,
    selected: String,
    accent: SheetAccent = PurpleAccent,
    selectedTextLight: Boolean = false,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Wq.Field)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { option ->
            val on = selected == option
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (on) accent.accent else Color.Transparent)
                    .clickable { onSelect(option) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    option,
                    color = when {
                        on && selectedTextLight -> Wq.Text
                        on -> Wq.Ink
                        else -> Wq.Muted
                    },
                    fontSize = 13.sp,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
    }
}

@Composable
internal fun PrimaryCta(
    label: String,
    enabled: Boolean,
    accent: SheetAccent = PurpleAccent,
    loading: Boolean = false,
    footer: String? = null,
    lightLabel: Boolean = false,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(accent.cta)
                .then(if (enabled && !loading) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (loading) {
                CircularProgressIndicator(
                    color = if (lightLabel) Wq.Text else Wq.Ink,
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    label,
                    color = if (lightLabel) Wq.Text else Wq.Ink,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
        footer?.let {
            Text(it, color = Wq.Handle, fontSize = 12.sp, fontFamily = PlusJakartaSans)
        }
    }
}

private fun initialsOf(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}".uppercase()
        parts.isNotEmpty() -> parts[0].take(2).uppercase()
        else -> "??"
    }
}

@Composable
internal fun AvatarPick(
    people: List<Pair<String, String>>,
    selected: Set<String>,
    accent: SheetAccent = PurpleAccent,
    onToggle: (String) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        people.forEachIndexed { index, (id, name) ->
            val on = id in selected
            val color = Wq.AvatarColors[index % Wq.AvatarColors.size]
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.clickable { onToggle(id) },
            ) {
                Box {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(color)
                            .then(if (on) Modifier.border(2.dp, accent.accent, CircleShape) else Modifier),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            initialsOf(name),
                            color = Wq.Ink,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PlusJakartaSans,
                        )
                    }
                    if (on) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(accent.accent),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("✓", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Text(
                    name.split(" ").first(),
                    color = if (on) Wq.Text else Wq.Muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WeddingExpenseSheetBody(momentId: String?, repository: GroupSliceRepository, onDismiss: () -> Unit, onSaved: () -> Unit, accent: SheetAccent = PurpleAccent) {
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var expenseDate by remember { mutableStateOf("") }
    var splitStrategy by remember { mutableStateOf("EQUAL") }
    var splitValues by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var category by remember { mutableStateOf(GroupExpenseCategoryCatalog.defaultCategory("WEDDING")) }
    var participants by remember { mutableStateOf<List<GroupParticipantDto>>(emptyList()) }
    var selected by remember { mutableStateOf(emptySet<String>()) }
    var paidBy by remember { mutableStateOf<String?>(null) }
    var currency by remember { mutableStateOf("INR") }
    var preferredCurrencyCodes by remember { mutableStateOf(listOf("INR")) }
    var loading by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var receiptBytes by remember { mutableStateOf<ByteArray?>(null) }
    var receiptContentType by remember { mutableStateOf("application/octet-stream") }
    var receiptName by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val live = !momentId.isNullOrBlank()
    val splitLabels = remember {
        listOf("Equal" to "EQUAL", "Custom" to "EXACT", "% Percent" to "PERCENTAGE")
    }

    val receiptPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: error("Could not read receipt")
                }
                receiptBytes = bytes
                receiptContentType = context.contentResolver.getType(uri) ?: "application/octet-stream"
                receiptName = uri.lastPathSegment?.substringAfterLast('/') ?: "receipt"
            }.onFailure { error = it.message }
        }
    }

    LaunchedEffect(momentId) {
        if (momentId.isNullOrBlank()) return@LaunchedEffect
        loading = true
        val ctx = loadGroupCurrencyContext(momentId)
        currency = ctx.primary
        preferredCurrencyCodes = ctx.preferred
        repository.getParticipants(momentId).fold(
            onSuccess = { dto ->
                val active = dto.participants.filter {
                    it.status.equals("ACTIVE", true) || it.status.equals("INVITED", true)
                }.ifEmpty { dto.participants }
                participants = active
                selected = active.map { it.participantId }.toSet()
                paidBy = active.firstOrNull()?.participantId
            },
            onFailure = { error = it.message },
        )
        loading = false
    }

    val people: List<Pair<String, String>> = participants.map {
        it.participantId to weddingExpenseParticipantLabel(it)
    }

    fun seedSplitValues(strategy: String, ids: Set<String>) {
        splitValues = when (strategy) {
            "PERCENTAGE" -> if (ids.isNotEmpty()) {
                val even = 100.0 / ids.size
                ids.associateWith { String.format(Locale.US, "%.2f", even) }
            } else emptyMap()
            "EXACT" -> {
                val total = amount.toBigDecimalOrNull()
                if (ids.isNotEmpty() && total != null && total > BigDecimal.ZERO) {
                    val n = ids.size
                    val sorted = ids.sorted()
                    val base = total.divide(BigDecimal(n), 2, java.math.RoundingMode.DOWN)
                    val allocated = base.multiply(BigDecimal(n - 1))
                    val last = total.subtract(allocated)
                    sorted.mapIndexed { index, id ->
                        id to if (index == n - 1) last.toPlainString() else base.toPlainString()
                    }.toMap()
                } else {
                    ids.associateWith { "" }
                }
            }
            else -> emptyMap()
        }
    }

    SheetHeader(R.drawable.ic_qa_wallet, "Add Expense", accent = accent)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(TravelCurrencyCatalog.symbol(currency), color = accent.accent, fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
        BasicTextField(
            value = amount,
            onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
            singleLine = true,
            textStyle = TextStyle(color = Wq.Text, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PlusJakartaSans),
            cursorBrush = SolidColor(accent.accent),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            decorationBox = { inner ->
                if (amount.isEmpty()) {
                    Text("0.00", color = Wq.Text.copy(alpha = 0.35f), fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PlusJakartaSans)
                }
                inner()
            },
            modifier = Modifier.padding(start = 6.dp),
        )
    }
    TravelCurrencyPickerRow(
        selectedCode = currency,
        onSelected = { currency = it },
        preferredCodes = preferredCurrencyCodes,
        accentColor = accent.accent,
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Description")
        SheetField(description, { description = it }, "What was this for?")
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FieldLabel("Paid By")
            var paidByOpen by remember { mutableStateOf(false) }
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Wq.Field)
                        .border(1.dp, Wq.Border, RoundedCornerShape(10.dp))
                        .clickable(enabled = participants.isNotEmpty()) { paidByOpen = true }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        participants.firstOrNull { it.participantId == paidBy }?.let { weddingExpenseParticipantLabel(it) }
                            ?: "Select",
                        color = Wq.Text,
                        fontSize = 14.sp,
                        fontFamily = PlusJakartaSans,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(painterResource(R.drawable.ic_biz_create_chevron), null, tint = Wq.Muted, modifier = Modifier.size(16.dp))
                }
                DropdownMenu(expanded = paidByOpen, onDismissRequest = { paidByOpen = false }) {
                    participants.forEach { p ->
                        DropdownMenuItem(
                            text = { Text(weddingExpenseParticipantLabel(p), fontFamily = PlusJakartaSans) },
                            onClick = {
                                paidBy = p.participantId
                                paidByOpen = false
                            },
                        )
                    }
                }
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FieldLabel("Date")
            WeddingDatePickField(
                value = expenseDate,
                onValueChange = { expenseDate = it },
                placeholder = "Today",
            )
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Split Between")
        if (loading) {
            CircularProgressIndicator(color = accent.accent, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        } else if (people.isEmpty()) {
            Text("No participants yet", color = Wq.Muted, fontSize = 12.sp, fontFamily = PlusJakartaSans)
        } else {
            AvatarPick(people, selected, accent) { id ->
                val next = if (id in selected) selected - id else selected + id
                selected = next
                if (paidBy == null) paidBy = id
                if (splitStrategy == "PERCENTAGE" || splitStrategy == "EXACT") {
                    seedSplitValues(splitStrategy, next)
                }
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Split Type")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Wq.Field)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            splitLabels.forEach { (label, strategy) ->
                val on = splitStrategy == strategy
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (on) accent.accent else Color.Transparent)
                        .clickable {
                            splitStrategy = strategy
                            seedSplitValues(strategy, selected)
                        }
                        .padding(horizontal = 8.dp, vertical = 10.dp)
                        .testTag(MaestroIds.groupExpenseSplitStrategy(strategy)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        color = if (on) Color.White else Wq.Muted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = PlusJakartaSans,
                    )
                }
            }
        }
    }
    if (splitStrategy != "EQUAL" && selected.isNotEmpty()) {
        val valueLabel = when (splitStrategy) {
            "PERCENTAGE" -> "Percent (must sum to 100)"
            "EXACT" -> "Exact amount per person"
            else -> "Share"
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FieldLabel(valueLabel)
            selected.sorted().forEach { id ->
                val name = weddingExpenseParticipantLabel(
                    participants.firstOrNull { it.participantId == id },
                    fallbackId = id,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("${MaestroIds.GROUP_EXPENSE_SPLIT_VALUE}.$id"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(name, color = Wq.Text, fontSize = 12.sp, fontFamily = PlusJakartaSans, modifier = Modifier.weight(1f))
                    Box(modifier = Modifier.width(110.dp)) {
                        SheetField(
                            value = splitValues[id] ?: "",
                            onValueChange = { v ->
                                splitValues = splitValues + (id to v.filter { c -> c.isDigit() || c == '.' })
                            },
                            placeholder = if (splitStrategy == "PERCENTAGE") "%" else "0.00",
                            keyboardType = KeyboardType.Decimal,
                            minHeight = 42,
                        )
                    }
                }
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Category")
        ChipRow(
            GroupExpenseCategoryCatalog.categories("WEDDING"),
            category,
            accent,
            testTag = MaestroIds.GROUP_EXPENSE_CATEGORY,
        ) { category = it }
    }
    GroupReceiptPickControl(
        muted = Wq.Muted,
        field = Wq.Field,
        border = Wq.Border,
        enabled = live,
        uploading = false,
        fileName = receiptName,
        onPick = { receiptPicker.launch("*/*") },
        onClear = {
            receiptBytes = null
            receiptName = null
            receiptContentType = "application/octet-stream"
        },
    )
    error?.let { Text(it, color = Color(0xFFF87171), fontSize = 12.sp, fontFamily = PlusJakartaSans) }
    val submitExpenseEnabled = live && amount.toBigDecimalOrNull()?.let { it > BigDecimal.ZERO } == true && selected.isNotEmpty()
    QuickAddDraftActions(
        submitLabel = "Add Expense",
        submitEnabled = submitExpenseEnabled,
        ctaBrush = accent.cta,
        loading = submitting,
        footer = "Everyone will be notified",
        onSubmit = {
            val payer = paidBy ?: selected.firstOrNull() ?: return@QuickAddDraftActions
            val ids = selected.sorted()
            val inputs = when (splitStrategy) {
                "EQUAL" -> ids.map { GroupExpenseSplitInputDto(participantId = it) }
                "PERCENTAGE" -> {
                    val pctSum = ids.sumOf { splitValues[it]?.toBigDecimalOrNull()?.toDouble() ?: 0.0 }
                    if (abs(pctSum - 100.0) > 0.01) {
                        error = "Percents must sum to 100 (now $pctSum)"
                        return@QuickAddDraftActions
                    }
                    ids.map {
                        GroupExpenseSplitInputDto(participantId = it, percent = splitValues[it])
                    }
                }
                "EXACT" -> {
                    val sum = ids.fold(BigDecimal.ZERO) { acc, id ->
                        acc.add(splitValues[id]?.toBigDecimalOrNull() ?: BigDecimal.ZERO)
                    }
                    val total = amount.toBigDecimalOrNull()
                    if (total == null || sum.compareTo(total) != 0) {
                        error = "Exact amounts must equal expense amount"
                        return@QuickAddDraftActions
                    }
                    ids.map {
                        GroupExpenseSplitInputDto(participantId = it, amount = splitValues[it])
                    }
                }
                else -> {
                    error = "Unknown split strategy"
                    return@QuickAddDraftActions
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
                repository.createGroupExpense(momentId!!, body.copy(asDraft = false)).fold(
                    onSuccess = { created ->
                        val bytes = receiptBytes
                        if (bytes != null && bytes.isNotEmpty()) {
                            repository.uploadAndAttachExpenseMedia(
                                momentId = momentId,
                                expenseId = created.expenseId,
                                bytes = bytes,
                                contentType = receiptContentType,
                            )
                        }
                        submitting = false
                        onSaved()
                        onDismiss()
                    },
                    onFailure = { submitting = false; error = it.message },
                )
            }
        },
        onSaveDraft = {
            val payer = paidBy ?: participants.firstOrNull()?.participantId ?: return@QuickAddDraftActions
            scope.launch {
                submitting = true
                error = null
                val draftBody = CreateGroupExpenseBody(
                    amount = amount.ifBlank { "0" },
                    currencyCode = currency,
                    description = GroupExpenseCategoryCatalog.descriptionWithCategory(
                        category = category,
                        userDescription = description,
                    ),
                    paidByParticipantId = payer,
                    splitStrategy = "POOLED",
                    splitInputs = emptyList(),
                    asDraft = true,
                )
                repository.createGroupExpense(momentId!!, draftBody).fold(
                    onSuccess = { created ->
                        val bytes = receiptBytes
                        if (bytes != null && bytes.isNotEmpty()) {
                            repository.uploadAndAttachExpenseMedia(
                                momentId = momentId,
                                expenseId = created.expenseId,
                                bytes = bytes,
                                contentType = receiptContentType,
                            )
                        }
                        submitting = false
                        onSaved()
                        onDismiss()
                    },
                    onFailure = { submitting = false; error = it.message },
                )
            }
        },
        draftEnabled = live && amount.toBigDecimalOrNull()?.let { it > BigDecimal.ZERO } == true && paidBy != null,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WeddingContributionSheetBody(
    momentId: String?,
    repository: GroupSliceRepository,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    accent: SheetAccent = ContribAccent,
    poolPlaceholder: String = "Trip Pool",
    editingContribution: GroupContributionItemDto? = null,
    onDeleted: () -> Unit = onSaved,
) {
    val isEditing = editingContribution != null
    var amount by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("INR") }
    var preferredCurrencyCodes by remember { mutableStateOf(listOf("INR")) }
    var pool by remember { mutableStateOf(poolPlaceholder) }
    var method by remember { mutableStateOf("UPI") }
    var status by remember { mutableStateOf("Paid") }
    var participants by remember { mutableStateOf<List<GroupParticipantDto>>(emptyList()) }
    var selectedParticipantId by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }
    var voiding by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var uploadingDoc by remember { mutableStateOf(false) }
    var attachmentUploadIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var attachmentNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var didPrefill by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val live = !momentId.isNullOrBlank()
    val selected = participants.firstOrNull { it.participantId == selectedParticipantId } ?: participants.firstOrNull()
    val fromLabel = selected?.displayName?.takeIf { it.isNotBlank() } ?: "You"
    val fromInitial = fromLabel.take(1).uppercase()

    val receiptPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null || momentId.isNullOrBlank()) return@rememberLauncherForActivityResult
        scope.launch {
            uploadingDoc = true
            error = null
            runCatching {
                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: error("Could not read receipt")
                }
                val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
                val name = uri.lastPathSegment?.substringAfterLast('/') ?: "receipt"
                val uploadId = repository.uploadBookingMedia(momentId, bytes, mime).getOrThrow()
                attachmentUploadIds = listOf(uploadId)
                attachmentNames = listOf(name)
            }.onFailure { error = it.message }
            uploadingDoc = false
        }
    }

    LaunchedEffect(momentId, editingContribution?.contributionId) {
        if (momentId.isNullOrBlank()) return@LaunchedEffect
        val ctx = loadGroupCurrencyContext(momentId)
        preferredCurrencyCodes = ctx.preferred
        if (!didPrefill) {
            currency = editingContribution?.currencyCode ?: ctx.primary
        }
        if (!didPrefill && editingContribution != null) {
            val existing = editingContribution
            amount = existing.amount.orEmpty()
            currency = existing.currencyCode?.takeIf { it.isNotBlank() } ?: ctx.primary
            pool = existing.label?.takeIf { it.isNotBlank() } ?: poolPlaceholder
            method = contributionPaymentMethodLabel(existing.paymentMethodCode).let {
                if (it == "—") "UPI" else it
            }
            status = if (existing.status.equals("PENDING", true)) "Pending" else "Paid"
            selectedParticipantId = existing.participantId
            didPrefill = true
        }
        repository.getParticipants(momentId).onSuccess { dto ->
            val eligible = dto.participants.filter {
                it.status.equals("ACTIVE", true) || it.status.equals("INVITED", true)
            }
            participants = eligible
            if (selectedParticipantId == null) {
                selectedParticipantId = eligible.firstOrNull { it.status.equals("ACTIVE", true) }?.participantId
                    ?: eligible.firstOrNull()?.participantId
            }
        }
    }

    fun paymentMethodCode(label: String): String = when (label) {
        "Bank Transfer" -> "BANK_TRANSFER"
        "Cash" -> "CASH"
        "Card" -> "CARD"
        else -> "UPI"
    }

    SheetHeader(
        R.drawable.ic_qa_users,
        if (isEditing) "Edit Contribution" else "Add Contribution",
        accent = accent,
        iconSize = 20,
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Amount Contributed")
        SheetField(
            amount,
            { amount = it.filter { c -> c.isDigit() || c == ',' || c == '.' } },
            "0.00",
            leading = { Text(TravelCurrencyCatalog.symbol(currency), color = accent.accent, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans) },
            keyboardType = KeyboardType.Decimal,
        )
        TravelCurrencyPickerRow(
            selectedCode = currency,
            onSelected = { currency = it },
            preferredCodes = preferredCurrencyCodes,
            accentColor = accent.accent,
        )
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FieldLabel("Contribution For")
            SheetField(
                pool,
                { pool = it },
                poolPlaceholder,
                trailing = { Icon(painterResource(R.drawable.ic_biz_create_chevron), null, tint = Wq.Muted, modifier = Modifier.size(14.dp)) },
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FieldLabel("From")
            var fromOpen by remember { mutableStateOf(false) }
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Wq.Field)
                        .border(1.dp, Wq.Border, RoundedCornerShape(10.dp))
                        .clickable(enabled = participants.isNotEmpty()) { fromOpen = true }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accent.soft)
                            .border(1.dp, accent.accent, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(fromInitial, color = accent.accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                    }
                    Text(
                        fromLabel,
                        color = Wq.Text,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = PlusJakartaSans,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(painterResource(R.drawable.ic_biz_create_chevron), null, tint = Wq.Muted, modifier = Modifier.size(14.dp))
                }
                androidx.compose.material3.DropdownMenu(expanded = fromOpen, onDismissRequest = { fromOpen = false }) {
                    participants.forEach { p ->
                        androidx.compose.material3.DropdownMenuItem(
                            text = {
                                Text(p.displayName?.takeIf { it.isNotBlank() } ?: p.participantId.take(8))
                            },
                            onClick = {
                                selectedParticipantId = p.participantId
                                fromOpen = false
                            },
                        )
                    }
                }
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Payment Method")
        ChipRow(listOf("UPI", "Bank Transfer", "Cash", "Card"), method, accent) { method = it }
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FieldLabel("Status")
            Segmented(listOf("Paid", "Pending"), status, accent, selectedTextLight = true) { status = it }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FieldLabel("Receipt")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Wq.Field)
                    .border(1.dp, Wq.Border, RoundedCornerShape(10.dp))
                    .clickable(enabled = live && !uploadingDoc && !isEditing) { receiptPicker.launch("*/*") }
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    when {
                        uploadingDoc -> "Uploading…"
                        attachmentNames.isNotEmpty() -> attachmentNames.joinToString(", ")
                        isEditing -> "📎 Receipt (view in Moments)"
                        else -> "📎 Attach PDF/Img"
                    },
                    color = Wq.Muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                if (attachmentNames.isNotEmpty() && !uploadingDoc) {
                    Text(
                        "✕",
                        color = Wq.Muted,
                        fontSize = 12.sp,
                        modifier = Modifier.clickable {
                            attachmentUploadIds = emptyList()
                            attachmentNames = emptyList()
                        },
                    )
                }
            }
        }
    }
    error?.let { Text(it, color = Color(0xFFF87171), fontSize = 12.sp, fontFamily = PlusJakartaSans) }
    if (isEditing && editingContribution != null) {
        Text(
            if (voiding) "Voiding…" else "Void contribution",
            color = Color(0xFFF87171),
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            fontFamily = PlusJakartaSans,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !submitting && !voiding) {
                    scope.launch {
                        voiding = true
                        error = null
                        repository.voidContribution(momentId!!, editingContribution.contributionId).fold(
                            onSuccess = {
                                voiding = false
                                onDeleted()
                                onDismiss()
                            },
                            onFailure = {
                                voiding = false
                                error = it.message ?: "Could not void contribution"
                            },
                        )
                    }
                }
                .padding(vertical = 8.dp),
        )
    }
    val normalizedAmount = remember(amount) { normalizeContributionAmount(amount) }
    PrimaryCta(
        label = if (isEditing) "Save Contribution" else "Add Contribution",
        enabled = live && normalizedAmount != null && !voiding,
        accent = accent,
        loading = submitting,
        footer = "Balance will be updated for everyone",
        lightLabel = true,
        onClick = {
            val amt = normalizeContributionAmount(amount)
            if (amt == null) {
                error = "Enter a valid amount (up to 4 decimal places)."
                return@PrimaryCta
            }
            scope.launch {
                submitting = true
                error = null
                val statusCode = if (status == "Pending") "PENDING" else "PAID"
                val result = if (editingContribution != null) {
                    repository.updateContribution(
                        momentId = momentId!!,
                        contributionId = editingContribution.contributionId,
                        amount = amt,
                        currencyCode = currency,
                        label = pool.ifBlank { null },
                        paymentMethodCode = paymentMethodCode(method),
                        participantId = selectedParticipantId,
                        status = statusCode,
                    )
                } else {
                    repository.recordContribution(
                        momentId = momentId!!,
                        amount = amt,
                        currencyCode = currency,
                        label = pool.ifBlank { null },
                        paymentMethodCode = paymentMethodCode(method),
                        participantId = selectedParticipantId,
                        status = statusCode,
                        attachmentUploadIds = attachmentUploadIds.ifEmpty { null },
                    )
                }
                result.fold(
                    onSuccess = { submitting = false; onSaved(); onDismiss() },
                    onFailure = { submitting = false; error = it.message ?: "Could not save contribution" },
                )
            }
        },
    )
}

/** Matches backend Zod `^\d+(\.\d{1,4})?$` with amount > 0. */
private fun normalizeContributionAmount(raw: String): String? {
    var s = raw.replace(",", "").trim()
    if (s.isEmpty()) return null
    if (s.endsWith(".")) s = s.dropLast(1)
    if (s.startsWith(".")) s = "0$s"
    val bd = s.toBigDecimalOrNull() ?: return null
    if (bd <= BigDecimal.ZERO) return null
    val capped = bd.setScale(4, java.math.RoundingMode.DOWN)
    val plain = capped.stripTrailingZeros().toPlainString()
    if (!Regex("""^\d+(\.\d{1,4})?$""").matches(plain)) return null
    return plain
}

private fun weddingExpenseParticipantLabel(
    p: GroupParticipantDto?,
    fallbackId: String? = null,
): String {
    val id = p?.participantId ?: fallbackId.orEmpty()
    val trimmed = p?.displayName?.trim().orEmpty()
    val base = if (trimmed.isNotEmpty()) trimmed else id.take(8).ifEmpty { "Member" }
    return if (p?.isGuest == true) "$base · Guest" else base
}

@Composable
internal fun WeddingBudgetSheetBody(momentId: String?, repository: GroupSliceRepository, onDismiss: () -> Unit, onSaved: () -> Unit, accent: SheetAccent = CoralAccent) {
    var amount by remember { mutableStateOf("") }
    var strategy by remember { mutableStateOf("Increase") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var currentBudget by remember { mutableStateOf<String?>(null) }
    var spent by remember { mutableStateOf<String?>(null) }
    var budgetCurrency by remember { mutableStateOf("INR") }
    val scope = rememberCoroutineScope()
    val live = !momentId.isNullOrBlank()

    LaunchedEffect(momentId) {
        if (momentId.isNullOrBlank()) return@LaunchedEffect
        val ctx = loadGroupCurrencyContext(momentId)
        budgetCurrency = ctx.primary
        repository.getFinance(momentId).onSuccess { facet ->
            val total = facet.payload?.totals?.firstOrNull { it.currencyCode == budgetCurrency }
                ?: facet.payload?.totals?.firstOrNull()
            currentBudget = total?.budgetTotal
            spent = total?.expenseTotal
        }
    }

    SheetHeader(R.drawable.ic_qa_trending, "Update Budget", accent = accent)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Wq.Field)
            .border(1.dp, Wq.Border, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Current Budget Status", color = Wq.Muted, fontSize = 12.sp, fontFamily = PlusJakartaSans)
            Text(
                currentBudget?.let { GroupBudgetUtils.formatApiAmountForDisplay(it, budgetCurrency) } ?: "No budget set",
                color = Wq.Text,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = PlusJakartaSans,
            )
            Text(
                spent?.let {
                    "${GroupBudgetUtils.formatApiAmountForDisplay(it, budgetCurrency)} spent"
                } ?: "No expenses yet",
                color = Wq.Handle,
                fontSize = 11.sp,
                fontFamily = PlusJakartaSans,
            )
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("New Budget Amount")
        SheetField(
            amount,
            { amount = it.filter { c -> c.isDigit() || c == ',' || c == '.' } },
            "0.00",
            leading = {
                Icon(painterResource(R.drawable.ic_qa_wallet), null, tint = Wq.Muted, modifier = Modifier.size(18.dp))
            },
            keyboardType = KeyboardType.Decimal,
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Adjustment Strategy")
        Segmented(listOf("Increase", "Decrease", "Replace"), strategy, accent) { strategy = it }
    }
    error?.let { Text(it, color = Color(0xFFF87171), fontSize = 12.sp, fontFamily = PlusJakartaSans) }
    PrimaryCta(
        label = "Update Budget",
        enabled = live && amount.replace(",", "").toBigDecimalOrNull()?.let { it > BigDecimal.ZERO } == true,
        accent = accent,
        loading = submitting,
        footer = "All members will see the update",
        onClick = {
            scope.launch {
                submitting = true
                error = null
                repository.patchGroupBudget(
                    momentId!!,
                    budgetAmount = amount.replace(",", ""),
                    budgetCurrencyCode = budgetCurrency,
                ).fold(
                    onSuccess = { submitting = false; onSaved(); onDismiss() },
                    onFailure = { submitting = false; error = it.message },
                )
            }
        },
    )
}

@Composable
internal fun WeddingParticipantSheetBody(
    momentId: String?,
    repository: GroupSliceRepository,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    accent: SheetAccent = SoftPinkAccent,
) {
    var name by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var affiliation by remember { mutableStateOf("Bride's Side") }
    var rsvp by remember { mutableStateOf("Pending") }
    var plusOne by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    SheetHeader(
        R.drawable.ic_qa_users,
        "Add Participant",
        "Invite and manage wedding team and guest list",
        accent = accent,
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Participant Name")
        SheetField(name, { name = it }, "Full name", minHeight = 42)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Email or Phone")
        SheetField(contact, { contact = it }, "Email or phone", minHeight = 42)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Affiliation / Role")
        ChipRow(listOf("Bride's Side", "Groom's Side", "Family"), affiliation, accent) { affiliation = it }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("RSVP Status")
        ChipRow(listOf("Confirmed", "Pending", "Declined"), rsvp, accent) { rsvp = it }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { plusOne = !plusOne }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text("Plus One Allowed", color = Wq.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
            Text("Include guest's spouse or partner", color = Wq.Muted, fontSize = 12.sp, fontFamily = PlusJakartaSans)
        }
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(22.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(if (plusOne) accent.accent else Wq.Border),
            contentAlignment = if (plusOne) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .padding(2.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Wq.Text),
            )
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Dietary Preferences / Notes")
        SheetField(notes, { notes = it }, "Optional notes", minHeight = 42)
    }
    error?.let { Text(it, color = Color(0xFFF87171), fontSize = 12.sp, fontFamily = PlusJakartaSans) }
    PrimaryCta(
        label = if (submitting) "Saving…" else "Add Participant",
        enabled = !momentId.isNullOrBlank() && name.isNotBlank() && !submitting,
        loading = submitting,
        accent = accent,
        lightLabel = true,
        onClick = {
            val id = momentId ?: return@PrimaryCta
            scope.launch {
                submitting = true
                error = null
                val trimmedContact = contact.trim()
                val email = if (trimmedContact.contains("@")) trimmedContact else null
                val phone = if (email == null && trimmedContact.isNotBlank()) trimmedContact else null
                repository.addParticipant(
                    id,
                    name.trim(),
                    roleCode = "PARTICIPANT",
                    email = email,
                    phone = phone,
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
        },
    )
}

@Composable
internal fun WeddingVendorSheetBody(momentId: String?, repository: GroupSliceRepository, onDismiss: () -> Unit, onSaved: () -> Unit, accent: SheetAccent = SoftPinkAccent) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Shortlisted") }
    var notes by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val live = !momentId.isNullOrBlank()

    SheetHeader(
        R.drawable.ic_biz_create_briefcase,
        "Add Vendor",
        "Keep track of wedding service providers",
        accent = accent,
        iconSize = 18,
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Vendor Name")
        SheetField(name, { name = it }, "Vendor name", minHeight = 42)
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FieldLabel("Category")
            SheetField(
                category,
                { category = it },
                "Category",
                minHeight = 42,
                trailing = { Icon(painterResource(R.drawable.ic_biz_create_chevron), null, tint = Wq.Muted, modifier = Modifier.size(10.dp)) },
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FieldLabel("Quoted Price")
            SheetField(
                price,
                { price = it },
                "0.00",
                minHeight = 42,
                leading = { Text("₹", color = accent.accent, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = PlusJakartaSans) },
            )
        }
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FieldLabel("Contact Number")
            SheetField(phone, { phone = it }, "Phone", minHeight = 42)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FieldLabel("Email")
            SheetField(email, { email = it }, "Email", minHeight = 42)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Status")
        ChipRow(listOf("Shortlisted", "Confirmed", "Rejected"), status, accent) { status = it }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Notes")
        SheetField(notes, { notes = it }, "Notes", singleLine = false, minHeight = 60)
    }
    error?.let { Text(it, color = Color(0xFFF87171), fontSize = 12.sp, fontFamily = PlusJakartaSans) }
    PrimaryCta(
        label = "Add Vendor",
        enabled = live && name.isNotBlank() && !submitting,
        accent = accent,
        lightLabel = true,
        loading = submitting,
        onClick = {
            scope.launch {
                submitting = true
                error = null
                repository.createGroupVendor(
                    momentId = momentId!!,
                    vendorName = name.trim(),
                    vendorType = category.trim().ifBlank { null },
                    phone = phone.trim().ifBlank { null },
                    email = email.trim().ifBlank { null },
                    notes = notes.trim().ifBlank { null },
                    quotedPrice = price.trim().ifBlank { null },
                    statusLabel = status,
                ).fold(
                    onSuccess = { submitting = false; onSaved(); onDismiss() },
                    onFailure = { submitting = false; error = it.message },
                )
            }
        },
    )
}

@Composable
internal fun WeddingPlanningSheetBody(
    momentId: String?,
    repository: GroupSliceRepository,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    accent: SheetAccent = PurpleAccent,
    momentTypeCode: String? = "WEDDING",
    editingItem: GroupLifePlanningItemDto? = null,
) {
    val categoryLabels = remember(momentTypeCode) { GroupPlanningCategoryCatalog.labels(momentTypeCode) }
    var category by remember(momentTypeCode, editingItem?.planningItemId) {
        mutableStateOf(
            editingItem?.categoryCode?.let { GroupPlanningCategoryCatalog.labelForCode(it, momentTypeCode) }
                ?: GroupPlanningCategoryCatalog.defaultLabel(momentTypeCode),
        )
    }
    var title by remember(editingItem?.planningItemId) { mutableStateOf(editingItem?.title.orEmpty()) }
    val splitDue = remember(editingItem?.dueAt) { tripIsoToLocalDateTime(editingItem?.dueAt) }
    var date by remember(editingItem?.planningItemId) { mutableStateOf(splitDue.first) }
    var time by remember(editingItem?.planningItemId) { mutableStateOf(splitDue.second) }
    var location by remember(editingItem?.planningItemId) { mutableStateOf(editingItem?.location.orEmpty()) }
    var participants by remember { mutableStateOf<List<GroupParticipantDto>>(emptyList()) }
    var selected by remember { mutableStateOf(emptySet<String>()) }
    var priority by remember(editingItem?.planningItemId) {
        mutableStateOf(GroupPlanningCategoryCatalog.priorityLabel(editingItem?.priorityCode))
    }
    var loading by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val live = !momentId.isNullOrBlank()
    val isEditing = !editingItem?.planningItemId.isNullOrBlank()

    LaunchedEffect(momentId) {
        if (momentId.isNullOrBlank()) return@LaunchedEffect
        loading = true
        repository.getParticipants(momentId).fold(
            onSuccess = { dto ->
                val active = dto.participants.filter {
                    it.status.equals("ACTIVE", true) || it.status.equals("INVITED", true)
                }.ifEmpty { dto.participants }
                participants = active
                selected = emptySet()
            },
            onFailure = { error = it.message },
        )
        loading = false
    }

    val people: List<Pair<String, String>> = participants.map {
        it.participantId to (it.displayName ?: it.participantId.take(8))
    }

    SheetHeader(
        R.drawable.ges_icon_calendar,
        if (isEditing) "Edit Planning Item" else "Add Planning Item",
        accent = accent,
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Category")
        ChipRow(categoryLabels, category, accent) { category = it }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Plan Title")
        SheetField(title, { title = it }, "Plan title")
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FieldLabel("Date")
            WeddingDatePickField(value = date, onValueChange = { date = it }, placeholder = "Date")
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FieldLabel("Time")
            WeddingTimePickField(value = time, onValueChange = { time = it }, placeholder = "Time")
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Location")
        SheetField(location, { location = it }, "Location")
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Assign To")
        if (loading) {
            CircularProgressIndicator(color = accent.accent, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        } else if (people.isEmpty()) {
            Text("No participants yet", color = Wq.Muted, fontSize = 12.sp, fontFamily = PlusJakartaSans)
        } else {
            AvatarPick(people, selected, accent) { id ->
                selected = if (id in selected) selected - id else selected + id
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Priority")
        Segmented(listOf("Low", "Medium", "High"), priority, accent) { priority = it }
    }
    error?.let { Text(it, color = Color(0xFFF87171), fontSize = 12.sp, fontFamily = PlusJakartaSans) }
    val submitPlanningEnabled = live && title.isNotBlank() && category.isNotBlank()
    QuickAddDraftActions(
        submitLabel = if (isEditing) "Save Changes" else "Add Planning Item",
        submitEnabled = submitPlanningEnabled,
        ctaBrush = accent.cta,
        loading = submitting,
        footer = if (isEditing) "Members will see the update" else "Everyone will be notified",
        onSubmit = {
            scope.launch {
                submitting = true
                error = null
                val planningItemId = editingItem?.planningItemId
                val result = if (!planningItemId.isNullOrBlank()) {
                    repository.updatePlanningItem(
                        momentId = momentId!!,
                        planningItemId = planningItemId,
                        title = title.trim(),
                        dueAt = tripDateTimeToIso(date, time),
                        categoryCode = GroupPlanningCategoryCatalog.codeForLabel(category),
                        location = location.trim().ifBlank { null },
                        priorityCode = GroupPlanningCategoryCatalog.priorityCode(priority),
                        asDraft = false,
                        status = if (editingItem?.status.equals("DRAFT", true)) "OPEN" else editingItem?.status,
                    )
                } else {
                    repository.createPlanningItem(
                        momentId = momentId!!,
                        title = title.trim(),
                        dueAt = tripDateTimeToIso(date, time),
                        categoryCode = GroupPlanningCategoryCatalog.codeForLabel(category),
                        location = location.trim().ifBlank { null },
                        priorityCode = GroupPlanningCategoryCatalog.priorityCode(priority),
                        asDraft = false,
                    )
                }
                result.fold(
                    onSuccess = { submitting = false; onSaved(); onDismiss() },
                    onFailure = { submitting = false; error = it.message },
                )
            }
        },
        onSaveDraft = {
            scope.launch {
                submitting = true
                error = null
                val planningItemId = editingItem?.planningItemId
                val result = if (!planningItemId.isNullOrBlank()) {
                    repository.updatePlanningItem(
                        momentId = momentId!!,
                        planningItemId = planningItemId,
                        title = title.trim(),
                        dueAt = tripDateTimeToIso(date, time),
                        categoryCode = GroupPlanningCategoryCatalog.codeForLabel(category),
                        location = location.trim().ifBlank { null },
                        priorityCode = GroupPlanningCategoryCatalog.priorityCode(priority),
                        asDraft = true,
                        status = "DRAFT",
                    )
                } else {
                    repository.createPlanningItem(
                        momentId = momentId!!,
                        title = title.trim(),
                        dueAt = tripDateTimeToIso(date, time),
                        categoryCode = GroupPlanningCategoryCatalog.codeForLabel(category),
                        location = location.trim().ifBlank { null },
                        priorityCode = GroupPlanningCategoryCatalog.priorityCode(priority),
                        asDraft = true,
                    )
                }
                result.fold(
                    onSuccess = { submitting = false; onSaved(); onDismiss() },
                    onFailure = { submitting = false; error = it.message },
                )
            }
        },
        draftEnabled = live && title.isNotBlank(),
    )
}

@Composable
internal fun WeddingAttendanceSheetBody(momentId: String?, repository: GroupSliceRepository, onDismiss: () -> Unit, onSaved: () -> Unit, accent: SheetAccent = SoftPinkAccent) {
    var filter by remember { mutableStateOf("All") }
    var search by remember { mutableStateOf("") }
    var participants by remember { mutableStateOf<List<GroupParticipantDto>>(emptyList()) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var statusLabel by remember { mutableStateOf("Confirmed") }
    var loading by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val live = !momentId.isNullOrBlank()

    LaunchedEffect(momentId) {
        if (momentId.isNullOrBlank()) return@LaunchedEffect
        loading = true
        repository.getParticipants(momentId).fold(
            onSuccess = { dto ->
                participants = dto.participants.filter {
                    it.status.equals("ACTIVE", true) || it.status.equals("INVITED", true)
                }.ifEmpty { dto.participants }
            },
            onFailure = { error = it.message },
        )
        loading = false
    }

    val filtered = participants.filter {
        val name = it.displayName ?: it.participantId
        search.isBlank() || name.contains(search, ignoreCase = true)
    }

    fun mapStatus(label: String): String = when (label) {
        "Confirmed" -> "CONFIRMED"
        "Pending" -> "EXPECTED"
        "Declined" -> "ABSENT"
        else -> "EXPECTED"
    }

    SheetHeader(
        R.drawable.ic_money_check,
        "Track Attendance",
        "Manage guest RSVPs and arrival status",
        accent = accent,
        iconSize = 18,
    )
    SheetField(
        search,
        { search = it },
        "Search guests...",
        minHeight = 42,
        leading = { Icon(painterResource(R.drawable.ic_qa_search), null, tint = Wq.Muted, modifier = Modifier.size(14.dp)) },
    )
    ChipRow(
        listOf("All", "Confirmed", "Pending", "Declined"),
        filter,
        accent,
    ) { filter = it }
    if (filter != "All") statusLabel = filter

    when {
        loading -> CircularProgressIndicator(color = accent.accent, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        filtered.isEmpty() -> Text(
            "No guests yet — add participants first.",
            color = Wq.Muted,
            fontSize = 13.sp,
            fontFamily = PlusJakartaSans,
        )
        else -> {
            Text("Select a guest", color = Wq.Muted, fontSize = 12.sp, fontFamily = PlusJakartaSans)
            filtered.take(12).forEach { p ->
                val label = p.displayName ?: p.participantId.take(8)
                val selected = selectedId == p.participantId
                Text(
                    label,
                    color = if (selected) accent.accent else Wq.Text,
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedId = p.participantId }
                        .padding(vertical = 8.dp),
                )
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Attendance status")
        ChipRow(listOf("Confirmed", "Pending", "Declined"), statusLabel, accent) { statusLabel = it }
    }
    error?.let { Text(it, color = Color(0xFFF87171), fontSize = 12.sp, fontFamily = PlusJakartaSans) }
    PrimaryCta(
        label = "Update Attendance",
        enabled = live && selectedId != null && !submitting,
        accent = accent,
        lightLabel = true,
        loading = submitting,
        onClick = {
            val pid = selectedId ?: return@PrimaryCta
            scope.launch {
                submitting = true
                error = null
                repository.recordAttendance(
                    momentId = momentId!!,
                    participantId = pid,
                    attendanceStatus = mapStatus(statusLabel),
                ).fold(
                    onSuccess = { submitting = false; onSaved(); onDismiss() },
                    onFailure = { submitting = false; error = it.message },
                )
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WeddingPollSheetBody(momentId: String?, repository: GroupSliceRepository, onDismiss: () -> Unit, onSaved: () -> Unit, accent: SheetAccent = PurpleAccent) {
    var question by remember { mutableStateOf("") }
    var optA by remember { mutableStateOf("") }
    var optB by remember { mutableStateOf("") }
    var optC by remember { mutableStateOf("") }
    var pollType by remember { mutableStateOf("Single choice") }
    var endDate by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val live = !momentId.isNullOrBlank()

    SheetHeader(R.drawable.ic_qa_activity, "Create Poll", "Decide together with the wedding party", accent = accent)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Question")
        SheetField(question, { question = it }, "Ask a question")
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Options")
        SheetField(optA, { optA = it }, "Option A")
        SheetField(optB, { optB = it }, "Option B")
        SheetField(optC, { optC = it }, "Option C (optional)")
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Poll type")
        Segmented(listOf("Single choice", "Multi choice"), pollType, accent) { pollType = it }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("End date")
        WeddingDatePickField(value = endDate, onValueChange = { endDate = it }, placeholder = "Optional")
    }
    error?.let { Text(it, color = Color(0xFFF87171), fontSize = 12.sp, fontFamily = PlusJakartaSans) }
    val submitPollEnabled = live && question.isNotBlank() && optA.isNotBlank() && optB.isNotBlank()
    QuickAddDraftActions(
        submitLabel = "Create Poll",
        submitEnabled = submitPollEnabled,
        ctaBrush = accent.cta,
        loading = submitting,
        footer = "Everyone will be notified",
        onSubmit = {
            scope.launch {
                submitting = true
                error = null
                val options = listOf(optA, optB, optC).map { it.trim() }.filter { it.isNotEmpty() }
                repository.createPoll(momentId!!, question.trim(), options, asDraft = false).fold(
                    onSuccess = { submitting = false; onSaved(); onDismiss() },
                    onFailure = { submitting = false; error = it.message },
                )
            }
        },
        onSaveDraft = {
            scope.launch {
                submitting = true
                error = null
                val filled = listOf(optA, optB, optC).map { it.trim() }.filter { it.isNotEmpty() }
                val options = when {
                    filled.size >= 2 -> filled
                    filled.size == 1 -> listOf(filled[0], "TBD")
                    else -> listOf("TBD", "TBD")
                }
                repository.createPoll(momentId!!, question.trim(), options, asDraft = true).fold(
                    onSuccess = { submitting = false; onSaved(); onDismiss() },
                    onFailure = { submitting = false; error = it.message },
                )
            }
        },
        draftEnabled = live && question.isNotBlank(),
    )
}

@Composable
internal fun WeddingMemorySheetBody(momentId: String?, repository: GroupSliceRepository, onDismiss: () -> Unit, onSaved: () -> Unit, accent: SheetAccent = PurpleAccent) {
    val context = LocalContext.current
    var type by remember { mutableStateOf("Photo") }
    var title by remember { mutableStateOf("") }
    var caption by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var mood by remember { mutableStateOf("Joyful") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var photoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showSourcePicker by remember { mutableStateOf(false) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val live = !momentId.isNullOrBlank()

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        tryTakePersistableReadPermission(context.contentResolver, uri)
        photoUri = uri
        photoBitmap = runCatching {
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        }.getOrNull()
        if (photoBitmap == null) error = "Could not open that photo"
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { ok ->
        val uri = cameraUri
        if (!ok || uri == null) return@rememberLauncherForActivityResult
        photoUri = uri
        photoBitmap = runCatching {
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        }.getOrNull()
        if (photoBitmap == null) error = "Could not open the captured photo"
    }

    val cameraPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (!granted) {
            error = "Camera permission is required to take a photo"
            return@rememberLauncherForActivityResult
        }
        val file = File(context.cacheDir, "wedding-memory-${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        cameraUri = uri
        cameraLauncher.launch(uri)
    }

    fun openCamera() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            val file = File(context.cacheDir, "wedding-memory-${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            cameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            cameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    SheetHeader(R.drawable.ic_qa_camera, "Capture Memory", "Save a moment for the wedding story", accent = accent)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Type")
        Segmented(listOf("Photo", "Milestone", "Lesson", "Reflection"), type, accent) { type = it }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Wq.Field)
            .border(1.dp, Wq.Border, RoundedCornerShape(14.dp))
            .clickable { showSourcePicker = true },
        contentAlignment = Alignment.Center,
    ) {
        if (photoBitmap != null) {
            Image(
                bitmap = photoBitmap!!.asImageBitmap(),
                contentDescription = "Selected photo",
                modifier = Modifier.fillMaxWidth().height(120.dp),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text("ï¼‹  Upload photo", color = Wq.Muted, fontSize = 14.sp, fontFamily = PlusJakartaSans)
        }
    }
    if (photoUri != null) {
        Text(
            "Photo attached Â· tap to change",
            color = Wq.Handle,
            fontSize = 11.sp,
            fontFamily = PlusJakartaSans,
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Title")
        SheetField(title, { title = it }, "Memory title")
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Caption")
        SheetField(caption, { caption = it }, "What made this special?", singleLine = false, minHeight = 72)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Tags")
        SheetField(tags, { tags = it }, "Optional tags")
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Mood")
        ChipRow(listOf("Joyful", "Emotional", "Fun", "Calm"), mood, accent) { mood = it }
    }
    error?.let { Text(it, color = Color(0xFFF87171), fontSize = 12.sp, fontFamily = PlusJakartaSans) }
    val submitMemoryEnabled = live && title.isNotBlank() && (type != "Photo" || photoUri != null || photoBitmap != null)
    val saveMemory: (Boolean) -> Unit = saveMemory@{ asDraft ->
        scope.launch {
            submitting = true
            error = null
            val wantsPhoto = !asDraft && (photoUri != null || photoBitmap != null || type == "Photo")
            if (!asDraft && type == "Photo" && photoUri == null && photoBitmap == null) {
                submitting = false
                error = "Add a photo before saving"
                return@launch
            }
            val create = repository.createMemory(momentId!!, title.trim(), asDraft = if (asDraft) true else null)
            create.fold(
                onSuccess = { created ->
                    val memoryId = created.memoryId
                    if (memoryId == null) {
                        submitting = false
                        error = "Memory saved but id missing — photo not attached"
                        return@fold
                    }
                    if (!wantsPhoto) {
                        GroupTabDataCache.invalidateMoment(momentId)
                        submitting = false
                        onSaved()
                        onDismiss()
                        return@fold
                    }
                    val bytes = withContext(Dispatchers.IO) {
                        encodeMemoryPhotoBytes(context.contentResolver, photoUri, photoBitmap)
                    }
                    if (bytes == null) {
                        submitting = false
                        error = "Could not read the selected photo. Try picking it again."
                        return@fold
                    }
                    repository.uploadAndAttachMemoryMedia(momentId, memoryId, bytes).fold(
                        onSuccess = {
                            GroupTabDataCache.invalidateMoment(momentId)
                            submitting = false
                            onSaved()
                            onDismiss()
                        },
                        onFailure = { submitting = false; error = it.message ?: "Photo upload failed" },
                    )
                },
                onFailure = { submitting = false; error = it.message },
            )
        }
    }
    QuickAddDraftActions(
        submitLabel = "Capture Memory",
        submitEnabled = submitMemoryEnabled,
        ctaBrush = accent.cta,
        loading = submitting,
        footer = if (type == "Photo") "Photo required for Photo memories" else "Everyone will be notified",
        onSubmit = { saveMemory(false) },
        onSaveDraft = { saveMemory(true) },
        draftEnabled = live && title.isNotBlank(),
    )

    if (showSourcePicker) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showSourcePicker = false },
            title = { Text("Add photo", color = Wq.Text, fontFamily = PlusJakartaSans) },
            text = {
                Text(
                    "Take a new photo or choose one from your library.",
                    color = Wq.Muted,
                    fontFamily = PlusJakartaSans,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showSourcePicker = false
                    openCamera()
                }) { Text("Camera") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSourcePicker = false
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                }) { Text("Photo library") }
            },
            containerColor = Wq.Sheet,
        )
    }
}

@Composable
internal fun WeddingUpdateSheetBody(momentId: String?, repository: GroupSliceRepository, onDismiss: () -> Unit, onSaved: () -> Unit, accent: SheetAccent = PurpleAccent) {
    var update by remember { mutableStateOf("") }
    var audience by remember { mutableStateOf("Everyone") }
    var urgent by remember { mutableStateOf(false) }
    var notify by remember { mutableStateOf(true) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val live = !momentId.isNullOrBlank()

    SheetHeader(R.drawable.ic_qa_activity, "Post Update", "Share a status with the wedding party", accent = accent)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Update")
        SheetField(update, { update = it }, "Share an update…", singleLine = false, minHeight = 88)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Audience")
        ChipRow(listOf("Everyone", "Organizers", "Close family"), audience, accent) { audience = it }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Wq.Field)
            .clickable { urgent = !urgent }
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("Mark as urgent", color = Wq.Text, fontSize = 13.sp, fontFamily = PlusJakartaSans)
        Text(if (urgent) "ON" else "OFF", color = if (urgent) accent.accent else Wq.Muted, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = PlusJakartaSans)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Wq.Field)
            .clickable { notify = !notify }
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("Notify members", color = Wq.Text, fontSize = 13.sp, fontFamily = PlusJakartaSans)
        Text(if (notify) "ON" else "OFF", color = if (notify) accent.accent else Wq.Muted, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = PlusJakartaSans)
    }
    error?.let { Text(it, color = Color(0xFFF87171), fontSize = 12.sp, fontFamily = PlusJakartaSans) }
    QuickAddDraftActions(
        submitLabel = "Post Update",
        submitEnabled = live && update.isNotBlank(),
        ctaBrush = accent.cta,
        loading = submitting,
        footer = "Everyone will be notified",
        onSubmit = {
            scope.launch {
                submitting = true
                error = null
                repository.postUpdate(
                    momentId!!,
                    update.trim(),
                    notifyMembers = notify,
                    urgencyCode = if (urgent) "URGENT" else "NORMAL",
                    asDraft = false,
                ).fold(
                    onSuccess = { submitting = false; onSaved(); onDismiss() },
                    onFailure = { submitting = false; error = it.message },
                )
            }
        },
        onSaveDraft = {
            scope.launch {
                submitting = true
                error = null
                repository.postUpdate(
                    momentId!!,
                    update.trim(),
                    notifyMembers = false,
                    urgencyCode = if (urgent) "URGENT" else "NORMAL",
                    asDraft = true,
                ).fold(
                    onSuccess = { submitting = false; onSaved(); onDismiss() },
                    onFailure = { submitting = false; error = it.message },
                )
            }
        },
        draftEnabled = live && update.isNotBlank(),
    )
}

@Composable
internal fun WeddingSettleSheetBody(
    momentId: String? = null,
    onDismiss: () -> Unit = {},
    onSaved: () -> Unit = {},
) {
    var presentSettlement by remember { mutableStateOf(false) }
    SheetHeader(R.drawable.ic_money_wallet, "Settle Up", "Record settlements between wedding party")
    Text(
        "Ledger settlement records payments against open balances. Payment rails are not processed.",
        color = Wq.Muted,
        fontSize = 13.sp,
        fontFamily = PlusJakartaSans,
    )
    PrimaryCta(
        label = "Settle Up",
        enabled = !momentId.isNullOrBlank(),
        onClick = { presentSettlement = true },
    )
    if (!momentId.isNullOrBlank()) {
        GroupSettlementSheet(
            momentId = momentId,
            visible = presentSettlement,
            onDismiss = { presentSettlement = false },
            onSaved = {
                presentSettlement = false
                onSaved()
                onDismiss()
            },
            momentTypeCode = "WEDDING",
        )
    }
}
