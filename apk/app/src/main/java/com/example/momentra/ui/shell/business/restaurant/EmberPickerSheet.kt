package com.example.momentra.ui.shell.business.restaurant

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.ui.theme.RestaurantEmber
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The three picker frames under `2487:37611`:
 * `RST-PICKER · Date` `2521:29334`, `· Month` `2521:29458`, `· Due date & time` `2521:29515`.
 *
 * One surface with three modes so the POS, budget and KOT sheets share the same chrome.
 * Every shortcut and grid cell is real: `Today / Tomorrow / +7 days` resolve against the
 * device clock, the month grid follows the real calendar, and `Now / End of shift` resolve
 * to actual times. Nothing is a placeholder.
 */
internal enum class EmberPickerMode(val title: String, val subtitle: String) {
    Date("Select date", "Tap a day"),
    Month("Select month", "Budget, target & reporting period"),
    DateTime("Due date & time", "Prep, service or delivery slot"),
}

@Composable
internal fun EmberPickerSheet(
    mode: EmberPickerMode,
    venue: String,
    initial: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, LocalTime?) -> Unit,
) {
    var selected by remember { mutableStateOf(initial) }
    var cursor by remember { mutableStateOf(YearMonth.from(initial)) }
    var time by remember { mutableStateOf(LocalTime.of(18, 0)) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = RestaurantEmber.gutter, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        EmberSheetGrabber()
        EmberSheetHeader(
            title = mode.title,
            subtitle = if (venue.isBlank()) mode.subtitle else "$venue · ${mode.subtitle}",
            onClose = onDismiss,
        )

        when (mode) {
            EmberPickerMode.Date -> DayBody(selected, cursor, { selected = it; cursor = YearMonth.from(it) }, { cursor = it })
            EmberPickerMode.Month -> MonthBody(selected, cursor, { selected = it; cursor = YearMonth.from(it) }, { cursor = it })
            EmberPickerMode.DateTime -> DateTimeBody(
                selected = selected,
                cursor = cursor,
                time = time,
                onDay = { selected = it },
                onMonth = { cursor = it },
                onTime = { time = it },
            )
        }

        Text(
            selectionText(mode, selected, time),
            style = RestaurantEmber.rowValue(RestaurantEmber.gold).copy(fontSize = 12.sp),
            maxLines = 1,
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(RestaurantEmber.chip)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Text("Cancel", style = RestaurantEmber.rowValue(RestaurantEmber.textBody).copy(fontSize = 14.sp))
            }
            Box(
                Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(RestaurantEmber.ember)
                    .clickable {
                        onConfirm(selected, if (mode == EmberPickerMode.DateTime) time else null)
                        onDismiss()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text("Confirm", style = RestaurantEmber.rowValue().copy(fontSize = 14.sp))
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun DayBody(
    selected: LocalDate,
    cursor: YearMonth,
    onPick: (LocalDate) -> Unit,
    onCursor: (YearMonth) -> Unit,
) {
    val today = LocalDate.now()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("Today" to 0L, "Tomorrow" to 1L, "+7 days" to 7L).forEach { (label, offset) ->
            val day = today.plusDays(offset)
            EmberModeChip(label, selected = day == selected) { onPick(day) }
        }
    }
    MonthHeader(cursor, onCursor)
    WeekdayHeader()
    DayGrid(selected, cursor, onPick)
}

@Composable
private fun MonthBody(
    selected: LocalDate,
    cursor: YearMonth,
    onPick: (LocalDate) -> Unit,
    onCursor: (YearMonth) -> Unit,
) {
    val now = LocalDate.now()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        EmberModeChip("This month", selected = YearMonth.from(now) == YearMonth.from(selected)) { onPick(now) }
        EmberModeChip("Last month", selected = YearMonth.from(now.minusMonths(1)) == YearMonth.from(selected)) {
            onPick(now.minusMonths(1))
        }
        EmberModeChip("Custom range ›", selected = false, locked = "Not a field", onClick = null)
    }

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Chevron("‹") { onCursor(cursor.minusYears(1)) }
        Text(
            "${cursor.year}",
            style = RestaurantEmber.rowValue().copy(fontSize = 16.sp),
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        Chevron("›") { onCursor(cursor.plusYears(1)) }
    }

    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        months.chunked(4).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                week.forEachIndexed { index, label ->
                    val monthIndex = months.indexOf(label) + 1
                    val on = cursor.monthValue == monthIndex && YearMonth.from(selected) == cursor
                    Box(
                        Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (on) RestaurantEmber.ember else RestaurantEmber.chip)
                            .clickable { onPick(cursor.withMonth(monthIndex).atDay(1)) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label,
                            style = RestaurantEmber.rowValue(
                                if (on) androidx.compose.ui.graphics.Color.White else RestaurantEmber.textBody,
                            ).copy(fontSize = 13.sp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DateTimeBody(
    selected: LocalDate,
    cursor: YearMonth,
    time: LocalTime,
    onDay: (LocalDate) -> Unit,
    onMonth: (YearMonth) -> Unit,
    onTime: (LocalTime) -> Unit,
) {
    MonthHeader(cursor, onMonth)
    WeekdayHeader()
    DayGrid(selected, cursor, onDay)

    EmberFieldLabel("Time")
    val slots = listOf(
        LocalTime.of(9, 0), LocalTime.of(10, 30), LocalTime.of(12, 0), LocalTime.of(13, 30),
        LocalTime.of(15, 0), LocalTime.of(16, 30), LocalTime.of(18, 0), LocalTime.of(19, 30),
        LocalTime.of(21, 0),
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        slots.chunked(5).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { slot ->
                    val on = slot == time
                    Box(
                        Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (on) RestaurantEmber.ember else RestaurantEmber.chip)
                            .clickable { onTime(slot) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            slot.format(DateTimeFormatter.ofPattern("h:mm a", Locale.US)),
                            style = RestaurantEmber.rowValue(
                                if (on) androidx.compose.ui.graphics.Color.White else RestaurantEmber.textBody,
                            ).copy(fontSize = 11.sp),
                            maxLines = 1,
                        )
                    }
                }
                repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        EmberModeChip("Now", selected = false) { onTime(LocalTime.now()) }
        EmberModeChip("End of shift", selected = false) { onTime(LocalTime.of(23, 0)) }
    }
}

@Composable
private fun MonthHeader(cursor: YearMonth, onCursor: (YearMonth) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Chevron("‹") { onCursor(cursor.minusMonths(1)) }
        Text(
            cursor.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)),
            style = RestaurantEmber.rowValue().copy(fontSize = 15.sp),
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        Chevron("›") { onCursor(cursor.plusMonths(1)) }
    }
}

@Composable
private fun WeekdayHeader() {
    Row(Modifier.fillMaxWidth()) {
        listOf("M", "T", "W", "T", "F", "S", "S").forEach { label ->
            Text(
                label,
                style = RestaurantEmber.rowCaption(RestaurantEmber.textFaint).copy(fontSize = 11.sp),
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DayGrid(selected: LocalDate, cursor: YearMonth, onPick: (LocalDate) -> Unit) {
    // `dayOfWeek.value` is 1=Monday, which is exactly the frame's first column.
    val leading = cursor.atDay(1).dayOfWeek.value - 1
    val days = cursor.lengthOfMonth()
    val cells: List<LocalDate?> = List(leading) { null } + (1..days).map { cursor.atDay(it) }
    val padded = cells + List((7 - cells.size % 7) % 7) { null }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        padded.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (day == null) {
                            Spacer(Modifier.height(38.dp))
                        } else {
                            val on = day == selected
                            val today = day == LocalDate.now()
                            Box(
                                Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (on) RestaurantEmber.ember else androidx.compose.ui.graphics.Color.Transparent)
                                    .clickable { onPick(day) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "${day.dayOfMonth}",
                                    style = RestaurantEmber.rowValue(
                                        when {
                                            on -> androidx.compose.ui.graphics.Color.White
                                            today -> RestaurantEmber.gold
                                            else -> RestaurantEmber.textBody
                                        },
                                    ).copy(fontSize = 13.sp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Chevron(glyph: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(RestaurantEmber.chip)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, style = RestaurantEmber.rowValue(RestaurantEmber.textBody).copy(fontSize = 16.sp))
    }
}

private fun selectionText(mode: EmberPickerMode, date: LocalDate, time: LocalTime): String = when (mode) {
    EmberPickerMode.Date -> "Selected: " + date.format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy", Locale.US))
    EmberPickerMode.Month ->
        "Selected: " + date.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)) +
            " · 01–" + date.lengthOfMonth() + date.format(DateTimeFormatter.ofPattern(" MMM", Locale.US))
    EmberPickerMode.DateTime -> {
        val merged = LocalDateTime.of(date, time)
        "Selected: " + merged.format(DateTimeFormatter.ofPattern("EEE, dd MMM · h:mm a", Locale.US))
    }
}
