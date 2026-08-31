package com.example.momentra.ui.shell.group

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
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
                GroupCollabKind.PLANNING -> PlanningBody(momentId, repository, onDismiss, onSaved)
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
private fun SheetTitle(title: String, subtitle: String, accent: Color, glyph: String = "✦") {
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
            Text(glyph, fontSize = 16.sp)
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = TripSheet.Text, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PlusJakartaSans)
            Text(subtitle, color = TripSheet.Muted, fontSize = 12.sp, fontFamily = PlusJakartaSans)
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text.uppercase(Locale.US),
        color = TripSheet.Muted,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = PlusJakartaSans,
    )
}

@Composable
private fun SheetField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean = true,
    minHeight: Int = 44,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(TripSheet.Field)
            .border(1.dp, TripSheet.Border, RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        if (value.isEmpty()) {
            Text(placeholder, color = TripSheet.Muted.copy(alpha = 0.7f), fontSize = 14.sp, fontFamily = PlusJakartaSans)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = TextStyle(color = TripSheet.Text, fontSize = 14.sp, fontFamily = PlusJakartaSans),
            cursorBrush = SolidColor(TripSheet.Purple),
            modifier = Modifier.fillMaxWidth(),
        )
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
) {
    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    SheetTitle("Add Plan", "Schedule an activity for your trip", TripSheet.Teal, "📍")
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldLabel("Plan Title")
        SheetField(title, { title = it }, "Dolphin Watching & Sunset Cruise")
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FieldLabel("Date")
            DatePickField(date, { date = it }, "Pick date")
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FieldLabel("Time")
            TimePickField(time, { time = it }, "Pick time")
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldLabel("Location")
        SheetField(location, { location = it }, "Coco Beach, Nerul")
    }
    error?.let { Text(it, color = Color(0xFFF87171), fontSize = 12.sp, fontFamily = PlusJakartaSans) }
    PrimaryCta(
        label = "Add Plan",
        enabled = title.isNotBlank(),
        loading = submitting,
        gradient = listOf(TripSheet.Teal, Color(0xFF0F766E)),
        onClick = {
            scope.launch {
                submitting = true
                error = null
                // Location stays local-only — not submitted into title or API fields.
                repository.createPlanningItem(
                    momentId = momentId,
                    title = title.trim(),
                    dueAt = tripDateTimeToIso(date, time),
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
    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    SheetTitle("Add Booking", "Reserve stays, rides, or tickets", TripSheet.Orange, "🏨")
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldLabel("Booking Title")
        SheetField(title, { title = it }, "Hotel / Flight / Activity")
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FieldLabel("Date")
            DatePickField(date, { date = it }, "Pick date")
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FieldLabel("Time")
            TimePickField(time, { time = it }, "Pick time")
        }
    }
    error?.let { Text(it, color = Color(0xFFF87171), fontSize = 12.sp, fontFamily = PlusJakartaSans) }
    PrimaryCta(
        label = "Add Booking",
        enabled = title.isNotBlank(),
        loading = submitting,
        gradient = listOf(TripSheet.Orange, Color(0xFFE85940)),
        onClick = {
            scope.launch {
                submitting = true
                error = null
                repository.createBooking(
                    momentId = momentId,
                    title = title.trim(),
                    bookedAt = tripDateTimeToIso(date, time),
                ).fold(
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

    SheetTitle("Create Poll", "Vote on activities with your travel group", TripSheet.Purple, "📊")
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldLabel("Poll Question")
        SheetField(question, { question = it }, "Where should we eat on Day 2?")
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Options")
        options.forEachIndexed { index, value ->
            SheetField(value, { options[index] = it }, "Option ${index + 1}")
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
        FieldLabel("Poll Deadline")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.weight(1f)) { DatePickField(deadlineDate, { deadlineDate = it }, "Date") }
            Box(modifier = Modifier.weight(1f)) { TimePickField(deadlineTime, { deadlineTime = it }, "Set Time") }
        }
    }
    error?.let { Text(it, color = Color(0xFFF87171), fontSize = 12.sp, fontFamily = PlusJakartaSans) }
    PrimaryCta(
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
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var photoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showSourcePicker by remember { mutableStateOf(false) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

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

    SheetTitle("Capture Memory", "Save a snippet of your trip for the shared journal", TripSheet.Coral, "📷")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Photo", "Milestone", "Lesson", "Reflection").forEach { chip ->
            val selected = type == chip
            Text(
                chip,
                color = if (selected) Color.White else TripSheet.Muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (selected) TripSheet.Coral else TripSheet.Field)
                    .border(1.dp, if (selected) TripSheet.Coral else TripSheet.Border, RoundedCornerShape(999.dp))
                    .clickable { type = chip }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
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
        FieldLabel("Caption")
        SheetField(caption, { caption = it }, "What made this special?", singleLine = false, minHeight = 88)
    }
    error?.let { Text(it, color = Color(0xFFF87171), fontSize = 12.sp, fontFamily = PlusJakartaSans) }
    PrimaryCta(
        label = "Save Memory",
        enabled = caption.isNotBlank() || photoUri != null,
        loading = submitting,
        gradient = listOf(TripSheet.Coral, Color(0xFFE8744F)),
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
    var message by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    SheetTitle("Post Update", "Share a status with your travel group", TripSheet.Blue, "✏️")
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldLabel("Update")
        SheetField(message, { message = it }, "What's happening?", singleLine = false, minHeight = 100)
    }
    error?.let { Text(it, color = Color(0xFFF87171), fontSize = 12.sp, fontFamily = PlusJakartaSans) }
    PrimaryCta(
        label = "Post Update",
        enabled = message.isNotBlank(),
        loading = submitting,
        gradient = listOf(TripSheet.Blue, Color(0xFF1D4ED8)),
        onClick = {
            scope.launch {
                submitting = true
                error = null
                repository.postUpdate(momentId, message.trim()).fold(
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

    SheetTitle("Add purchase item", "Track something the group is buying", TripSheet.Orange, "🛒")
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldLabel("Label")
        SheetField(label, { label = it }, "Item name")
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldLabel("Amount (optional)")
        SheetField(amount, { amount = it }, "0.00")
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

    SheetTitle("Add resident", "Add someone to the household roster", TripSheet.Blue, "🏠")
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldLabel("Name")
        SheetField(name, { name = it }, "Display name")
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldLabel("Role (optional)")
        SheetField(role, { role = it }, "Roommate / Owner")
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
