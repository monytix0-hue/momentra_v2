package com.example.momentra.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupDateField(
    label: String,
    isoValue: String?,
    onIsoChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "setup.date",
) {
    var showPicker by remember { mutableStateOf(false) }
    val display = SetupDateTimeUtils.formatDateDisplay(isoValue)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = SetupTokens.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SetupTokens.BizCard)
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                .clickable { showPicker = true }
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .testTag(testTag),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(display, color = if (isoValue.isNullOrBlank()) SetupTokens.TextSecondary else Color.White, fontSize = 15.sp)
            Text("▼", color = SetupTokens.TextSecondary, fontSize = 12.sp)
        }
    }

    if (showPicker) {
        val initial = SetupDateTimeUtils.parseIsoDate(isoValue) ?: LocalDate.now()
        val state = rememberDatePickerState(
            initialSelectedDateMillis = SetupDateTimeUtils.localDateToMillis(initial),
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onIsoChange(SetupDateTimeUtils.localDateToIso(SetupDateTimeUtils.millisToLocalDate(millis)))
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
fun SetupDateTimeField(
    label: String,
    isoValue: String?,
    onIsoChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "setup.datetime",
) {
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    var pendingDate by remember { mutableStateOf<LocalDate?>(null) }
    val parsed = SetupDateTimeUtils.parseIsoDateTime(isoValue)
    val display = SetupDateTimeUtils.formatDateTimeDisplay(isoValue)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = SetupTokens.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SetupTokens.BizCard)
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                .clickable {
                    pendingDate = parsed?.first ?: LocalDate.now()
                    showDate = true
                }
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .testTag(testTag),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(display, color = if (isoValue.isNullOrBlank()) SetupTokens.TextSecondary else Color.White, fontSize = 15.sp)
            Text("▼", color = SetupTokens.TextSecondary, fontSize = 12.sp)
        }
    }

    if (showDate) {
        val initial = pendingDate ?: LocalDate.now()
        val state = rememberDatePickerState(
            initialSelectedDateMillis = SetupDateTimeUtils.localDateToMillis(initial),
        )
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        pendingDate = SetupDateTimeUtils.millisToLocalDate(millis)
                        showDate = false
                        showTime = true
                    }
                }) { Text("Next") }
            },
            dismissButton = {
                TextButton(onClick = { showDate = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = state)
        }
    }

    if (showTime) {
        val initialTime = parsed?.second ?: LocalTime.of(9, 0)
        val timeState = rememberTimePickerState(
            initialHour = initialTime.hour,
            initialMinute = initialTime.minute,
            is24Hour = false,
        )
        Dialog(onDismissRequest = { showTime = false }) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(SetupTokens.BizCard)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TimePicker(state = timeState)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { showTime = false }) { Text("Cancel") }
                    TextButton(onClick = {
                        val date = pendingDate ?: LocalDate.now()
                        val time = LocalTime.of(timeState.hour, timeState.minute)
                        onIsoChange(SetupDateTimeUtils.localDateTimeToIso(date, time))
                        showTime = false
                    }) { Text("OK") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupDateRangeField(
    label: String,
    startIso: String?,
    endIso: String?,
    onStartChange: (String) -> Unit,
    onEndChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "setup.date.range",
) {
    Column(modifier = modifier.testTag(testTag), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SetupDateField(
            label = "$label (start)",
            isoValue = startIso,
            onIsoChange = onStartChange,
            testTag = "$testTag.start",
        )
        SetupDateField(
            label = "$label (end)",
            isoValue = endIso,
            onIsoChange = onEndChange,
            testTag = "$testTag.end",
        )
    }
}
