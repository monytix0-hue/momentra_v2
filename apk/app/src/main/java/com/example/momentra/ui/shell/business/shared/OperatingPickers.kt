package com.example.momentra.ui.shell.business.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.ApiClient
import com.example.momentra.ui.theme.PlusJakartaSans
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * The three Figma pickers — `TRD-PICKER` `2566:33326` / `:33450` / `:33507` and
 * `DST-PICKER` `2560:29770` / `:29894` / `:29951`. They replace the stock Material
 * `DatePickerDialog`s so a trading capture matches the frame exactly.
 */
enum class OperatingPickerMode { DAY, MONTH, DATE_TIME }

private val DayLabel = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)

/**
 * A bottom sheet calendar. `mode` decides whether the footer returns a day, a month, or
 * a day plus a quarter-hour time, matching the three frames.
 */
@Composable
internal fun OperatingPickerSheet(
    mode: OperatingPickerMode,
    initial: LocalDate,
    theme: OperatingSheetTheme,
    title: String? = null,
    onDismiss: () -> Unit,
    onDay: (LocalDate) -> Unit,
    onMonth: (YearMonth) -> Unit = {},
    onDateTime: (LocalDateTime) -> Unit = {},
) {
    var cursor by remember { mutableStateOf(YearMonth.from(initial)) }
    var selected by remember { mutableStateOf(initial) }
    var hour by remember { mutableStateOf(9) }
    var minute by remember { mutableStateOf(0) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 17.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth().height(22.dp), horizontalArrangement = Arrangement.Center) {
            Box(
                Modifier
                    .width(40.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
            )
        }
        Text(
            title ?: when (mode) {
                OperatingPickerMode.DAY -> "Pick a day"
                OperatingPickerMode.MONTH -> "Pick a month"
                OperatingPickerMode.DATE_TIME -> "Pick a day and time"
            },
            color = theme.ink,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
        )
        // Month stepper
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            StepButton("‹", theme) { cursor = cursor.minusMonths(1) }
            Text(
                cursor.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + cursor.year,
                color = theme.ink,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            StepButton("›", theme) { cursor = cursor.plusMonths(1) }
        }
        if (mode != OperatingPickerMode.MONTH) {
            Row(Modifier.fillMaxWidth()) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach { label ->
                    Text(
                        label,
                        color = theme.body,
                        fontFamily = PlusJakartaSans,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            val first = cursor.atDay(1)
            val offset = (first.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
            val cells = offset + cursor.lengthOfMonth()
            var day = 1
            repeat((cells + 6) / 7) {
                Row(Modifier.fillMaxWidth()) {
                    repeat(7) { column ->
                        val index = it * 7 + column
                        if (index < offset || day > cursor.lengthOfMonth()) {
                            Spacer(Modifier.weight(1f))
                        } else {
                            val date = cursor.atDay(day)
                            day += 1
                            val on = date == selected
                            val today = date == LocalDate.now()
                            Text(
                                date.dayOfMonth.toString(),
                                color = if (on) theme.canvas else if (today) theme.accent else theme.ink,
                                fontFamily = PlusJakartaSans,
                                fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(if (on) theme.accent else Color.Transparent)
                                    .clickable { selected = date }
                                    .padding(vertical = 7.dp),
                            )
                        }
                    }
                }
            }
        }
        if (mode == OperatingPickerMode.DATE_TIME) {
            Text("Time", color = theme.body, fontFamily = PlusJakartaSans, fontSize = 11.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TimeColumn("Hour", hour, 0..23, theme, Modifier.weight(1f)) { hour = it }
                TimeColumn("Minute", minute, listOf(0, 15, 30, 45), theme, Modifier.weight(1f)) { minute = it }
            }
        }
        Text(
            "Use",
            color = theme.canvas,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(theme.accent)
                .clickable {
                    when (mode) {
                        OperatingPickerMode.DAY -> onDay(selected)
                        OperatingPickerMode.MONTH -> onMonth(YearMonth.from(selected))
                        OperatingPickerMode.DATE_TIME -> onDateTime(
                            LocalDateTime.of(selected, LocalTime.of(hour, minute)),
                        )
                    }
                }
                .padding(vertical = 13.dp),
            textAlign = TextAlign.Center,
        )
        Text(
            "Cancel",
            color = theme.body,
            fontFamily = PlusJakartaSans,
            fontSize = 13.sp,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onDismiss)
                .padding(vertical = 10.dp),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun StepButton(label: String, theme: OperatingSheetTheme, onClick: () -> Unit) {
    Text(
        label,
        color = theme.ink,
        fontFamily = PlusJakartaSans,
        fontSize = 18.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(theme.inset)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    )
}

@Composable
private fun TimeColumn(
    label: String,
    value: Int,
    values: Iterable<Int>,
    theme: OperatingSheetTheme,
    modifier: Modifier = Modifier,
    onChange: (Int) -> Unit,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = theme.body, fontFamily = PlusJakartaSans, fontSize = 10.sp)
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(theme.inset)
                .border(1.dp, theme.border, RoundedCornerShape(10.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "−",
                color = theme.ink,
                modifier = Modifier.clickable { onChange(stepBack(value, values)) }.padding(6.dp),
            )
            Text(
                value.toString().padStart(2, '0'),
                color = theme.ink,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            Text(
                "+",
                color = theme.ink,
                modifier = Modifier.clickable { onChange(stepForward(value, values)) }.padding(6.dp),
            )
        }
    }
}

private fun stepBack(value: Int, values: Iterable<Int>): Int {
    val list = values.toList()
    val index = list.indexOf(value)
    return if (index <= 0) list.last() else list[index - 1]
}

private fun stepForward(value: Int, values: Iterable<Int>): Int {
    val list = values.toList()
    val index = list.indexOf(value)
    return if (index < 0 || index == list.lastIndex) list.first() else list[index + 1]
}

internal fun LocalDate.dayLabel(): String = format(DayLabel)

/**
 * `Switch Moment` — `TRD-SWITCH 2566:33577`. Lists the operator's business moments and
 * moves the shell to the chosen one. Selection is delegated upward; this sheet never
 * navigates by itself.
 */
@Composable
internal fun OperatingSwitchMomentSheet(
    currentMomentId: String?,
    theme: OperatingSheetTheme,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    var moments by remember { mutableStateOf<List<Triple<String, String, String?>>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        runCatching { ApiClient.apiService.listBusinessMoments(limit = 50).data.items }
            .onSuccess { items ->
                moments = items.map { Triple(it.momentId, it.title, it.momentTypeCode) }
            }
            .onFailure { error = it.message }
    }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 17.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth().height(22.dp), horizontalArrangement = Arrangement.Center) {
            Box(
                Modifier
                    .width(40.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
            )
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Switch moment",
                color = theme.ink,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                modifier = Modifier.weight(1f),
            )
            Text(
                "Close",
                color = theme.body,
                fontFamily = PlusJakartaSans,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
        Text("Switching a moment never moves its records. Each moment keeps its own ledger.", color = theme.body, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        if (error != null) Text(error ?: "", color = theme.negative, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        moments.forEach { (id, title, code) ->
            val on = id == currentMomentId
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (on) theme.inset else theme.surface)
                    .border(1.dp, if (on) theme.accent else theme.border, RoundedCornerShape(14.dp))
                    .clickable { onSelect(id) }
                    .padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        title,
                        color = theme.ink,
                        fontFamily = PlusJakartaSans,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        code?.replace('_', ' ')?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Business moment",
                        color = theme.body,
                        fontFamily = PlusJakartaSans,
                        fontSize = 11.sp,
                    )
                }
                if (on) OperatingBadgeChip("Current", theme)
            }
        }
    }
}
