package com.example.momentra.ui.shell.group.shared

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.momentra.R
import com.example.momentra.data.repository.GroupSliceRepository
import com.example.momentra.ui.setup.SetupDateTimeUtils
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.example.momentra.ui.shell.group.wedding.create.PrimaryCta

/** Figma 575:15497 Trip Quick Add linked sheets. */
enum class GroupCollabKind {
    PLANNING,
    BOOKING,
    POLL,
    UPDATE,
    MEMORY,
    PURCHASE_ITEM,
    RESIDENT,
}

/** Figma 575:15497 Trip Quick Add linked sheets — colors from TripSheetTokens. */
internal object TripSheet {
    val Bg = TripSheetTokens.Bg
    val Field = TripSheetTokens.Field
    val Border = TripSheetTokens.Border
    val Muted = TripSheetTokens.Muted
    val Text = TripSheetTokens.Text
    val Purple = Color(0xFFA855F7)
    val PurpleEnd = Color(0xFFC084FC)
    val Teal = Color(0xFF14B8A6)
    val Orange = TripSheetTokens.Accent
    val Blue = Color(0xFF3B82F6)
    val Coral = Color(0xFFFF8E63)
    val Peach = TripSheetTokens.AccentEnd
}

/** Combine local date + time into ISO-8601 with offset (Zod .datetime()). */
internal fun tripDateTimeToIso(dateIso: String?, timeIso: String?): String? {
    if (dateIso.isNullOrBlank() && timeIso.isNullOrBlank()) return null
    val date = dateIso?.takeIf { it.isNotBlank() }?.let {
        runCatching { LocalDate.parse(it.take(10)) }.getOrNull()
    } ?: LocalDate.now()
    val time = timeIso?.takeIf { it.isNotBlank() }?.let {
        runCatching { LocalTime.parse(it) }.getOrNull()
    } ?: LocalTime.MIDNIGHT
    return date.atTime(time).atZone(ZoneId.systemDefault()).toOffsetDateTime()
        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
}

internal fun tripNowIso(): String =
    OffsetDateTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupCollabSheet(
    kind: GroupCollabKind,
    momentId: String,
    visible: Boolean,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    momentTypeCode: String? = null,
    repository: GroupSliceRepository = remember { GroupSliceRepository() },
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = TripSheet.Bg,
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
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (kind) {
                GroupCollabKind.PLANNING -> PlanningBody(momentId, repository, onDismiss, onSaved, momentTypeCode)
                GroupCollabKind.BOOKING -> BookingBody(momentId, repository, onDismiss, onSaved)
                GroupCollabKind.POLL -> PollBody(momentId, repository, onDismiss, onSaved)
                GroupCollabKind.UPDATE -> UpdateBody(momentId, repository, onDismiss, onSaved)
                GroupCollabKind.MEMORY -> MemoryBody(momentId, repository, onDismiss, onSaved)
                GroupCollabKind.PURCHASE_ITEM -> PurchaseBody(momentId, repository, onDismiss, onSaved)
                GroupCollabKind.RESIDENT -> ResidentBody(momentId, repository, onDismiss, onSaved)
            }
        }
    }
}

@Composable
private fun SheetTitle(title: String, subtitle: String, accent: Color, iconRes: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(accent.copy(alpha = 0.18f))
                .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = TripSheet.Text, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PlusJakartaSans)
            Text(subtitle, color = TripSheet.Muted, fontSize = 12.sp, fontFamily = PlusJakartaSans)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickField(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    var show by remember { mutableStateOf(false) }
    val display = value.takeIf { it.isNotBlank() }?.let {
        runCatching {
            LocalDate.parse(it).format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US))
        }.getOrDefault(it)
    }.orEmpty()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(TripSheet.Field)
            .border(1.dp, TripSheet.Border, RoundedCornerShape(8.dp))
            .clickable { show = true }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (display.isBlank()) placeholder else display,
            color = if (display.isBlank()) TripSheet.Muted.copy(alpha = 0.7f) else TripSheet.Text,
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
private fun TimePickField(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    var show by remember { mutableStateOf(false) }
    val display = value.takeIf { it.isNotBlank() }?.let {
        runCatching {
            LocalTime.parse(it).format(DateTimeFormatter.ofPattern("h:mm a", Locale.US))
        }.getOrDefault(it)
    }.orEmpty()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(TripSheet.Field)
            .border(1.dp, TripSheet.Border, RoundedCornerShape(8.dp))
            .clickable { show = true }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (display.isBlank()) placeholder else display,
            color = if (display.isBlank()) TripSheet.Muted.copy(alpha = 0.7f) else TripSheet.Text,
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
                    .background(TripSheet.Bg)
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

@Composable
private fun PrimaryCta(
    label: String,
    enabled: Boolean,
    loading: Boolean,
    gradient: List<Color>,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.horizontalGradient(gradient))
            .then(if (enabled && !loading) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
        } else {
            Text(label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
        }
    }
}

@Composable
private fun PlanningBody(
    momentId: String,
    repository: GroupSliceRepository,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    momentTypeCode: String? = null,
) {
    val categoryLabels = remember(momentTypeCode) { GroupPlanningCategoryCatalog.labels(momentTypeCode) }
    var category by remember(momentTypeCode) { mutableStateOf(GroupPlanningCategoryCatalog.defaultLabel(momentTypeCode)) }
    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("Medium") }
    var participants by remember { mutableStateOf<List<com.example.momentra.data.api.GroupParticipantDto>>(emptyList()) }
    var assignedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(momentId) {
        repository.getParticipants(momentId).fold(
            onSuccess = { dto ->
                participants = dto.participants
                assignedIds = dto.participants.map { it.participantId }.toSet()
            },
            onFailure = { /* best-effort */ },
        )
    }

    TripSheetHeaderRow("Add Plan", "Schedule an activity for your trip", R.drawable.ic_group_qa_calendar, TripFormTokens.Teal)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TripFieldLabel("Category")
        TripChipRow(categoryLabels, category, { category = it }, TripFormTokens.Teal)
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TripFieldLabel("Plan Title")
        TripSheetField(title, { title = it }, "Dolphin Watching & Sunset Cruise")
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            TripFieldLabel("Date")
            TripDatePickField(date, { date = it })
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            TripFieldLabel("Time")
            TripTimePickField(time, { time = it })
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TripFieldLabel("Location")
        TripSheetField(location, { location = it }, "Coco Beach, Nerul")
    }
    if (participants.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            TripFieldLabel("Assign To")
            TripParticipantPicker(
                participants = participants,
                selectedIds = assignedIds,
                onToggle = { id ->
                    assignedIds = if (assignedIds.contains(id)) assignedIds - id else assignedIds + id
                },
                accent = TripFormTokens.Teal,
            )
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TripFieldLabel("Priority")
        TripSegmentedControl(listOf("Low", "Medium", "High"), priority, { priority = it }, TripFormTokens.Teal)
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TripFieldLabel("Add Notes")
        TripSheetField(notes, { notes = it }, "Carry sunglasses and camera…", singleLine = false, minHeight = 80)
    }
    error?.let { Text(it, color = Color(0xFFF87171), fontSize = 12.sp, fontFamily = PlusJakartaSans) }
    TripPrimaryCta(
        label = "Add Plan",
        enabled = title.isNotBlank() && category.isNotBlank(),
        loading = submitting,
        footer = "Added to group itinerary",
        gradient = listOf(TripFormTokens.Teal, Color(0xFF0F766E)),
        onClick = {
            scope.launch {
                submitting = true
                error = null
                repository.createPlanningItem(
                    momentId = momentId,
                    title = title.trim(),
                    dueAt = tripDateTimeToIso(date, time),
                    categoryCode = GroupPlanningCategoryCatalog.codeForLabel(category),
                    location = location.trim().ifBlank { null },
                    priorityCode = GroupPlanningCategoryCatalog.priorityCode(priority),
                    description = notes.trim().ifBlank { null },
                ).fold(
                    onSuccess = { submitting = false; onSaved(); onDismiss() },
                    onFailure = { submitting = false; error = it.message },
                )
            }
        },
    )
}

@Composable
private fun BookingBody(
    momentId: String,
    repository: GroupSliceRepository,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    var bookingType by remember { mutableStateOf("Hotel") }
    var title by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var confirmed by remember { mutableStateOf(true) }
    var participants by remember { mutableStateOf<List<com.example.momentra.data.api.GroupParticipantDto>>(emptyList()) }
    var bookedById by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(momentId) {
        repository.getParticipants(momentId).fold(
            onSuccess = { dto ->
                participants = dto.participants
                bookedById = dto.participants.firstOrNull()?.participantId
            },
            onFailure = { /* best-effort */ },
        )
    }

    TripSheetHeaderRow("Add Booking", "Attach reservations to your Kyoto timeline", R.drawable.ic_group_qa_ticket, TripSheet.Orange)
    TripChipRow(listOf("Hotel", "Flight", "Transport", "Activity", "Restaurant"), bookingType, { bookingType = it }, TripSheet.Orange)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TripFieldLabel("Booking Name")
        TripSheetField(title, { title = it }, "MIMARU Kyoto Stay")
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            TripFieldLabel("Confirmation #")
            TripSheetField(confirmation, { confirmation = it }, "MMR-98402X")
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            TripFieldLabel("Cost (₹)")
            TripSheetField(cost, { cost = it }, "42,500")
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TripFieldLabel("Date Range (Check-In / Check-Out)")
        TripDatePickField(startDate, { startDate = it }, "Check-in")
        if (startDate.isNotBlank()) {
            TripDatePickField(endDate, { endDate = it }, "Check-out")
        }
    }
  if (participants.isNotEmpty()) {
        val bookedName = participants.firstOrNull { it.participantId == bookedById }?.displayName ?: "You"
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            TripFieldLabel("Booked By")
            Text(bookedName, color = TripSheet.Text, fontSize = 14.sp, fontFamily = PlusJakartaSans,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(TripSheet.Field)
                    .border(1.dp, TripSheet.Border, RoundedCornerShape(10.dp))
                    .clickable {
                        val idx = participants.indexOfFirst { it.participantId == bookedById }
                        val next = participants[(idx + 1) % participants.size]
                        bookedById = next.participantId
                    }
                    .padding(12.dp))
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text("Status: Confirmed", color = TripSheet.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
            Text("Mark booking immediately as secured", color = TripSheet.Muted, fontSize = 11.sp, fontFamily = PlusJakartaSans)
        }
        Switch(checked = confirmed, onCheckedChange = { confirmed = it }, colors = SwitchDefaults.colors(checkedTrackColor = TripSheet.Orange))
    }
    error?.let { Text(it, color = Color(0xFFF87171), fontSize = 12.sp, fontFamily = PlusJakartaSans) }
    TripPrimaryCta(
        label = "Add Booking",
        enabled = title.isNotBlank(),
        loading = submitting,
        gradient = listOf(TripSheet.Orange, Color(0xFFE85940)),
        onClick = {
            scope.launch {
                submitting = true
                error = null
                repository.createBooking(momentId = momentId, title = title.trim(), bookedAt = tripDateTimeToIso(startDate, null)).fold(
                    onSuccess = { submitting = false; onSaved(); onDismiss() },
                    onFailure = { submitting = false; error = it.message },
                )
            }
        },
    )
}

@Composable
private fun PollBody(
    momentId: String,
    repository: GroupSliceRepository,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    var question by remember { mutableStateOf("") }
    val options = remember { mutableStateListOf("", "") }
    var anonymous by remember { mutableStateOf(true) }
    var multi by remember { mutableStateOf(false) }
    var deadlineDate by remember { mutableStateOf("") }
    var deadlineTime by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    TripSheetHeaderRow("Create Poll", "Vote on activities with your travel group", R.drawable.ic_group_qa_vote, TripSheet.Purple)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TripFieldLabel("Poll Question")
        TripSheetField(question, { question = it }, "Where should we eat on Day 2?")
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TripFieldLabel("Options")
        options.forEachIndexed { index, value ->
            TripSheetField(value, { options[index] = it }, "Option ${index + 1}")
        }
        Text(
            "+ Add Option",
            color = TripSheet.Purple,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
            modifier = Modifier
                .clickable { if (options.size < 6) options.add("") }
                .padding(vertical = 4.dp),
        )
    }
    ToggleRow("Anonymous Voting", "Hide voters' names in results", anonymous, TripSheet.Purple) { anonymous = it }
    ToggleRow("Allow Multiple Choice", "Co-travelers can select multiple options", multi, TripSheet.Purple) { multi = it }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TripFieldLabel("Poll Deadline")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.weight(1f)) { TripDatePickField(deadlineDate, { deadlineDate = it }, "Date") }
            Box(modifier = Modifier.weight(1f)) { TripTimePickField(deadlineTime, { deadlineTime = it }, "Set Time") }
        }
    }
    error?.let { Text(it, color = Color(0xFFF87171), fontSize = 12.sp, fontFamily = PlusJakartaSans) }
    TripPrimaryCta(
        label = "Create Poll",
        enabled = question.isNotBlank() && options.count { it.isNotBlank() } >= 2,
        loading = submitting,
        gradient = listOf(TripSheet.Purple, TripSheet.PurpleEnd),
        onClick = {
            scope.launch {
                submitting = true
                error = null
                // Anonymous is UI-only — do not prefix [anon] into the question.
                val opts = options.map { it.trim() }.filter { it.isNotEmpty() }
                repository.createPoll(
                    momentId = momentId,
                    question = question.trim(),
                    options = opts,
                    closesAt = tripDateTimeToIso(deadlineDate, deadlineTime),
                    pollType = if (multi) "MULTI_CHOICE" else "SINGLE_CHOICE",
                ).fold(
                    onSuccess = { submitting = false; onSaved(); onDismiss() },
                    onFailure = { submitting = false; error = it.message },
                )
            }
        },
    )
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, accent: Color, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = TripSheet.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
            Text(subtitle, color = TripSheet.Muted, fontSize = 11.sp, fontFamily = PlusJakartaSans)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = accent, checkedThumbColor = Color.White),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MemoryBody(
    momentId: String,
    repository: GroupSliceRepository,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    val context = LocalContext.current
    var type by remember { mutableStateOf("Photo") }
    var caption by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var mood by remember { mutableStateOf("🍁") }
    var participants by remember { mutableStateOf<List<com.example.momentra.data.api.GroupParticipantDto>>(emptyList()) }
    var taggedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var photoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showSourcePicker by remember { mutableStateOf(false) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(momentId) {
        repository.getParticipants(momentId).fold(
            onSuccess = { dto ->
                participants = dto.participants
                taggedIds = dto.participants.take(3).map { it.participantId }.toSet()
            },
            onFailure = { /* best-effort */ },
        )
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
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
        val file = File(context.cacheDir, "trip-memory-${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        cameraUri = uri
        cameraLauncher.launch(uri)
    }

    fun openCamera() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            val file = File(context.cacheDir, "trip-memory-${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            cameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            cameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    TripSheetHeaderRow("Capture Memory", "Save a snippet of your trip for the shared journal", R.drawable.ic_group_qa_camera, TripFormTokens.Pink)
    TripChipRow(listOf("Photo", "Milestone", "Lesson", "Reflection"), type, { type = it }, TripFormTokens.Pink)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(TripSheet.Field)
            .border(1.dp, TripSheet.Border, RoundedCornerShape(14.dp))
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
            Text("＋  Camera / gallery", color = TripSheet.Muted, fontSize = 14.sp, fontFamily = PlusJakartaSans)
        }
    }
    if (photoUri != null) {
        Text(
            "Photo attached · tap to change",
            color = TripSheet.Purple,
            fontSize = 11.sp,
            fontFamily = PlusJakartaSans,
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TripFieldLabel("Caption")
        TripSheetField(caption, { caption = it }, "Incredible golden autumn leaves at Kiyomizudera!", singleLine = false, minHeight = 88)
    }
    if (participants.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            TripFieldLabel("Tag People")
            TripParticipantPicker(participants, taggedIds, { id ->
                taggedIds = if (taggedIds.contains(id)) taggedIds - id else taggedIds + id
            }, TripFormTokens.Pink)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TripFieldLabel("Location")
        TripSheetField(location, { location = it }, "Kiyomizu-dera Temple, Kyoto")
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TripFieldLabel("Mood")
        TripChipRow(listOf("🍁", "✨", "📸", "🍜", "🏯", "🙌"), mood, { mood = it }, TripFormTokens.Pink)
    }
    error?.let { Text(it, color = Color(0xFFF87171), fontSize = 12.sp, fontFamily = PlusJakartaSans) }
    TripPrimaryCta(
        label = "Save Memory",
        enabled = caption.isNotBlank() || photoUri != null,
        loading = submitting,
        gradient = listOf(TripFormTokens.Pink, Color(0xFFF472B6)),
        onClick = {
            scope.launch {
                submitting = true
                error = null
                val trimmed = caption.trim()
                val title = when {
                    trimmed.isNotBlank() -> "[$type] $trimmed"
                    else -> type
                }
                val create = repository.createMemory(
                    momentId = momentId,
                    title = title,
                    capturedAt = tripNowIso(),
                )
                create.fold(
                    onSuccess = { created ->
                        val memoryId = created.memoryId
                        val uri = photoUri
                        if (memoryId != null && uri != null) {
                            val bytes = withContext(Dispatchers.IO) {
                                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                                    ?: photoBitmap?.let { bmp ->
                                        ByteArrayOutputStream().use { out ->
                                            bmp.compress(Bitmap.CompressFormat.JPEG, 85, out)
                                            out.toByteArray()
                                        }
                                    }
                            }
                            if (bytes != null) {
                                repository.uploadAndAttachMemoryMedia(momentId, memoryId, bytes).fold(
                                    onSuccess = { submitting = false; onSaved(); onDismiss() },
                                    onFailure = { submitting = false; error = it.message },
                                )
                            } else {
                                submitting = false
                                onSaved()
                                onDismiss()
                            }
                        } else {
                            submitting = false
                            onSaved()
                            onDismiss()
                        }
                    },
                    onFailure = { submitting = false; error = it.message },
                )
            }
        },
    )

    if (showSourcePicker) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showSourcePicker = false },
            title = { Text("Add photo", color = TripSheet.Text, fontFamily = PlusJakartaSans) },
            text = {
                Text(
                    "Take a new photo or choose one from your library.",
                    color = TripSheet.Muted,
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
            containerColor = TripSheet.Bg,
        )
    }
}

@Composable
private fun UpdateBody(
    momentId: String,
    repository: GroupSliceRepository,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    var updateType by remember { mutableStateOf("Announcement") }
    var message by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("Normal") }
    var notifyAll by remember { mutableStateOf(true) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    TripSheetHeaderRow("Post Update", "Share a status with your travel group", R.drawable.ic_group_qa_megaphone, TripFormTokens.Blue)
    TripChipRow(listOf("Announcement", "Status", "Question", "Reminder"), updateType, { updateType = it }, TripFormTokens.Blue)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TripFieldLabel("Update Message")
        TripSheetField(message, { message = it }, "Road closure on our route…", singleLine = false, minHeight = 100)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            TripFieldLabel("Attach Media")
            Text("📷  🔗", color = TripSheet.Text, modifier = Modifier.padding(8.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            TripFieldLabel("Priority")
            TripSegmentedControl(listOf("Normal", "Urgent"), priority, { priority = it }, Color(0xFFEF4444))
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text("Notify all members", color = TripSheet.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
            Text("Sends push notifications instantly", color = TripSheet.Muted, fontSize = 11.sp, fontFamily = PlusJakartaSans)
        }
        Switch(checked = notifyAll, onCheckedChange = { notifyAll = it }, colors = SwitchDefaults.colors(checkedTrackColor = TripFormTokens.Blue))
    }
    error?.let { Text(it, color = Color(0xFFF87171), fontSize = 12.sp, fontFamily = PlusJakartaSans) }
    TripPrimaryCta(
        label = "Post Update",
        enabled = message.isNotBlank(),
        loading = submitting,
        footer = "Visible in group feed",
        gradient = listOf(TripFormTokens.Blue, Color(0xFF1D4ED8)),
        onClick = {
            scope.launch {
                submitting = true
                error = null
                repository.postUpdate(
                    momentId,
                    message.trim(),
                    notifyMembers = notifyAll,
                    urgencyCode = GroupPlanningCategoryCatalog.urgencyCode(priority),
                ).fold(
                    onSuccess = { submitting = false; onSaved(); onDismiss() },
                    onFailure = { submitting = false; error = it.message },
                )
            }
        },
    )
}

@Composable
private fun PurchaseBody(
    momentId: String,
    repository: GroupSliceRepository,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    var label by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    SheetTitle("Add purchase item", "Track something the group is buying", TripSheet.Orange, R.drawable.ic_group_qa_chartbar)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TripFieldLabel("Label")
        TripSheetField(label, { label = it }, "Item name")
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TripFieldLabel("Amount (optional)")
        TripSheetField(amount, { amount = it }, "0.00")
    }
    error?.let { Text(it, color = Color(0xFFF87171), fontSize = 12.sp, fontFamily = PlusJakartaSans) }
    PrimaryCta(
        label = "Save",
        enabled = label.isNotBlank(),
        loading = submitting,
        gradient = listOf(TripSheet.Orange, Color(0xFFE85940)),
        onClick = {
            scope.launch {
                submitting = true
                error = null
                repository.createPurchaseItem(momentId, label.trim(), amount.trim().ifBlank { null }).fold(
                    onSuccess = { submitting = false; onSaved(); onDismiss() },
                    onFailure = { submitting = false; error = it.message },
                )
            }
        },
    )
}

@Composable
private fun ResidentBody(
    momentId: String,
    repository: GroupSliceRepository,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    SheetTitle("Add resident", "Add someone to the household roster", TripSheet.Blue, R.drawable.ic_group_qa_userplus)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TripFieldLabel("Name")
        TripSheetField(name, { name = it }, "Display name")
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TripFieldLabel("Role (optional)")
        TripSheetField(role, { role = it }, "Roommate / Owner")
    }
    error?.let { Text(it, color = Color(0xFFF87171), fontSize = 12.sp, fontFamily = PlusJakartaSans) }
    PrimaryCta(
        label = "Save",
        enabled = name.isNotBlank(),
        loading = submitting,
        gradient = listOf(TripSheet.Blue, Color(0xFF1D4ED8)),
        onClick = {
            scope.launch {
                submitting = true
                error = null
                repository.addResident(momentId, name.trim(), role.trim().ifBlank { null }).fold(
                    onSuccess = { submitting = false; onSaved(); onDismiss() },
                    onFailure = { submitting = false; error = it.message },
                )
            }
        },
    )
}
