package com.example.momentra.ui.shell.business.manufacturing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.momentra.ui.theme.ManufacturingAmber
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Amber-themed centered Date picker — Figma `2549:29336`.
 * Opens a calendar-grid modal; [MfgDateQuickPicker] remains an inline chip strip for dense sheets.
 */
@Composable
fun MfgDatePickerModal(
    selectedIso: String,
    visible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    title: String = "Select date",
    subtitle: String = "Forge & Form · tap a day",
) {
    if (!visible) return
    val initial = runCatching { LocalDate.parse(selectedIso.take(10)) }.getOrElse { LocalDate.now() }
    var cursor by remember(visible) { mutableStateOf(YearMonth.from(initial)) }
    var picked by remember(visible) { mutableStateOf(initial) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(ManufacturingAmber.canvas.copy(alpha = 0.9f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .shadow(24.dp, RoundedCornerShape(20.dp), ambientColor = ManufacturingAmber.accent.copy(alpha = 0.25f))
                    .clip(RoundedCornerShape(20.dp))
                    .background(ManufacturingAmber.surface)
                    .border(1.dp, ManufacturingAmber.accent.copy(alpha = 0.28f), RoundedCornerShape(20.dp))
                    .clickable(enabled = false) {}
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(title, style = ManufacturingAmber.rowTitle().copy(fontSize = 18.sp))
                        Text(subtitle, style = ManufacturingAmber.rowCaption())
                    }
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(ManufacturingAmber.chip)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("✕", style = ManufacturingAmber.rowCaption().copy(color = ManufacturingAmber.textPrimary))
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "Today" to LocalDate.now(),
                        "Tomorrow" to LocalDate.now().plusDays(1),
                        "+7 days" to LocalDate.now().plusDays(7),
                    ).forEach { (label, date) ->
                        MfgModeChip(label, selected = picked == date, onClick = {
                            picked = date
                            cursor = YearMonth.from(date)
                        })
                    }
                    MfgModeChip("Clear", selected = false, onClick = {
                        picked = LocalDate.now()
                        cursor = YearMonth.now()
                    })
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        cursor.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + cursor.year,
                        style = ManufacturingAmber.rowTitle().copy(fontSize = 15.sp),
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ManufacturingAmber.chip)
                            .clickable { cursor = cursor.minusMonths(1) },
                        contentAlignment = Alignment.Center,
                    ) { Text("‹", style = ManufacturingAmber.rowTitle()) }
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ManufacturingAmber.chip)
                            .clickable { cursor = cursor.plusMonths(1) },
                        contentAlignment = Alignment.Center,
                    ) { Text("›", style = ManufacturingAmber.rowTitle()) }
                }
                MfgCalendarGrid(
                    yearMonth = cursor,
                    selected = picked,
                    onSelect = { picked = it },
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ManufacturingAmber.accent.copy(alpha = 0.12f))
                        .border(1.dp, ManufacturingAmber.accent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                ) {
                    val fmt = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy", Locale.ENGLISH)
                    Text(
                        "Selected: ${picked.format(fmt)}",
                        style = ManufacturingAmber.rowCaption().copy(color = ManufacturingAmber.accentSoft, fontSize = 13.sp),
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ManufacturingAmber.chip)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Cancel", style = ManufacturingAmber.rowTitle().copy(fontSize = 14.sp))
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ManufacturingAmber.accent)
                            .clickable {
                                onConfirm(picked.toString())
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Confirm",
                            style = ManufacturingAmber.rowTitle().copy(fontSize = 14.sp, color = ManufacturingAmber.textPrimary),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MfgCalendarGrid(
    yearMonth: YearMonth,
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit,
) {
    val monStartBlank = (yearMonth.atDay(1).dayOfWeek.value + 6) % 7
    val daysInMonth = yearMonth.lengthOfMonth()
    val today = LocalDate.now()
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ManufacturingAmber.canvas)
            .border(1.dp, ManufacturingAmber.canvasTop, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { d ->
                Text(
                    d,
                    style = ManufacturingAmber.rowCaption().copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
            }
        }
        var day = 1
        repeat(6) { week ->
            if (day > daysInMonth) return@repeat
            Row(Modifier.fillMaxWidth()) {
                repeat(7) { col ->
                    val cellIndex = week * 7 + col
                    if (cellIndex < monStartBlank || day > daysInMonth) {
                        Box(Modifier.weight(1f).height(36.dp))
                    } else {
                        val date = yearMonth.atDay(day)
                        val isSelected = date == selected
                        val isToday = date == today
                        Box(
                            Modifier
                                .weight(1f)
                                .height(36.dp)
                                .padding(1.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) ManufacturingAmber.accent
                                    else androidx.compose.ui.graphics.Color.Transparent,
                                )
                                .then(
                                    if (isToday && !isSelected) {
                                        Modifier.border(1.dp, ManufacturingAmber.accentSoft, RoundedCornerShape(10.dp))
                                    } else Modifier
                                )
                                .clickable { onSelect(date) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                day.toString(),
                                style = ManufacturingAmber.rowCaption().copy(
                                    fontSize = 13.sp,
                                    color = when {
                                        isSelected -> ManufacturingAmber.textPrimary
                                        isToday -> ManufacturingAmber.accentSoft
                                        else -> ManufacturingAmber.textPrimary
                                    },
                                    fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold
                                    else androidx.compose.ui.text.font.FontWeight.Medium,
                                ),
                            )
                        }
                        day++
                    }
                }
            }
        }
    }
}

/** Month / budget period centered modal — Figma `2549:29460` year + 3×4 month grid. */
@Composable
fun MfgMonthPickerModal(
    selectedYm: String,
    visible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    if (!visible) return
    val initial = runCatching { YearMonth.parse(selectedYm.take(7)) }.getOrElse { YearMonth.now() }
    var year by remember(visible) { mutableStateOf(initial.year) }
    var picked by remember(visible) { mutableStateOf(initial) }
    val now = YearMonth.now()
    val monthLabels = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val selectedSummary = run {
        val start = picked.atDay(1)
        val end = picked.atEndOfMonth()
        val rangeFmt = DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH)
        val monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
        "Selected: ${picked.format(monthFmt)} · ${start.format(rangeFmt)}–${end.format(rangeFmt)}"
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier.fillMaxSize().background(ManufacturingAmber.canvas.copy(alpha = 0.9f)).clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .shadow(24.dp, RoundedCornerShape(20.dp), ambientColor = ManufacturingAmber.accent.copy(alpha = 0.25f))
                    .clip(RoundedCornerShape(20.dp))
                    .background(ManufacturingAmber.surface)
                    .border(1.dp, ManufacturingAmber.accent.copy(alpha = 0.28f), RoundedCornerShape(20.dp))
                    .clickable(enabled = false) {}
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Select month", style = ManufacturingAmber.rowTitle().copy(fontSize = 18.sp))
                        Text("Budget, target & reporting period", style = ManufacturingAmber.rowCaption())
                    }
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(ManufacturingAmber.chip)
                            .border(1.dp, ManufacturingAmber.accent, CircleShape)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("✕", style = ManufacturingAmber.rowCaption().copy(color = ManufacturingAmber.textPrimary))
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MfgModeChip("This month", selected = picked == now, onClick = {
                        picked = now
                        year = now.year
                    })
                    MfgModeChip("Last month", selected = picked == now.minusMonths(1), onClick = {
                        picked = now.minusMonths(1)
                        year = picked.year
                    })
                    MfgModeChip("Custom range ›", selected = false, onClick = { /* period stays single-month; range Unavailable */ })
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("$year", style = ManufacturingAmber.rowTitle().copy(fontSize = 15.sp), modifier = Modifier.weight(1f))
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ManufacturingAmber.chip)
                            .border(1.dp, ManufacturingAmber.accent, RoundedCornerShape(10.dp))
                            .clickable { year -= 1 },
                        contentAlignment = Alignment.Center,
                    ) { Text("‹", style = ManufacturingAmber.rowTitle()) }
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ManufacturingAmber.chip)
                            .border(1.dp, ManufacturingAmber.accent, RoundedCornerShape(10.dp))
                            .clickable { year += 1 },
                        contentAlignment = Alignment.Center,
                    ) { Text("›", style = ManufacturingAmber.rowTitle()) }
                }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ManufacturingAmber.canvas)
                        .border(1.dp, ManufacturingAmber.accent, RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    monthLabels.chunked(3).forEachIndexed { rowIdx, row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEachIndexed { colIdx, label ->
                                val month = rowIdx * 3 + colIdx + 1
                                val ym = YearMonth.of(year, month)
                                val isSelected = picked == ym
                                val isCurrent = ym == now
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) ManufacturingAmber.accent else ManufacturingAmber.chip)
                                        .then(
                                            if (isCurrent && !isSelected) {
                                                Modifier.border(1.dp, ManufacturingAmber.accentSoft, RoundedCornerShape(12.dp))
                                            } else Modifier
                                        )
                                        .clickable { picked = ym },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        label,
                                        style = ManufacturingAmber.rowTitle().copy(
                                            fontSize = 14.sp,
                                            color = when {
                                                isSelected -> ManufacturingAmber.textPrimary
                                                isCurrent -> ManufacturingAmber.accentSoft
                                                else -> ManufacturingAmber.textPrimary
                                            },
                                            fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold
                                            else androidx.compose.ui.text.font.FontWeight.Medium,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ManufacturingAmber.accent.copy(alpha = 0.12f))
                        .border(1.dp, ManufacturingAmber.accent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                ) {
                    Text(
                        selectedSummary,
                        style = ManufacturingAmber.rowCaption().copy(color = ManufacturingAmber.accentSoft, fontSize = 13.sp),
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ManufacturingAmber.chip)
                            .border(1.dp, ManufacturingAmber.accent, RoundedCornerShape(12.dp))
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center,
                    ) { Text("Cancel", style = ManufacturingAmber.rowTitle().copy(fontSize = 14.sp)) }
                    Box(
                        Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ManufacturingAmber.accent)
                            .clickable {
                                onConfirm(picked.toString())
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center,
                    ) { Text("Confirm", style = ManufacturingAmber.rowTitle().copy(fontSize = 14.sp)) }
                }
            }
        }
    }
}

/** Date & time centered modal — Figma `2549:29517`. */
@Composable
fun MfgDateTimePickerModal(
    selectedSlot: String,
    selectedIso: String,
    visible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (iso: String, slot: String) -> Unit,
) {
    if (!visible) return
    var date by remember(visible) {
        mutableStateOf(runCatching { LocalDate.parse(selectedIso.take(10)) }.getOrElse { LocalDate.now() })
    }
    var slot by remember(visible) { mutableStateOf(selectedSlot.ifBlank { "Morning · 09:00" }) }
    val slots = listOf("Morning · 09:00", "Noon · 12:00", "Afternoon · 15:00", "Evening · 18:00")

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier.fillMaxSize().background(ManufacturingAmber.canvas.copy(alpha = 0.9f)).clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(ManufacturingAmber.surface)
                    .border(1.dp, ManufacturingAmber.accent.copy(alpha = 0.28f), RoundedCornerShape(20.dp))
                    .clickable(enabled = false) {}
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("Select date & time", style = ManufacturingAmber.rowTitle().copy(fontSize = 18.sp))
                Text("Dispatch · due · decide-by", style = ManufacturingAmber.rowCaption())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "Today" to LocalDate.now(),
                        "Tomorrow" to LocalDate.now().plusDays(1),
                        "+3 days" to LocalDate.now().plusDays(3),
                    ).forEach { (label, d) ->
                        MfgModeChip(label, selected = date == d, onClick = { date = d })
                    }
                }
                MfgCalendarGrid(YearMonth.from(date), date) { date = it }
                Text("Time slot", style = ManufacturingAmber.rowCaption())
                slots.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { s ->
                            MfgModeChip(s, selected = slot == s, onClick = { slot = s }, modifier = Modifier.weight(1f))
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(12.dp)).background(ManufacturingAmber.chip).clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center,
                    ) { Text("Cancel", style = ManufacturingAmber.rowTitle().copy(fontSize = 14.sp)) }
                    Box(
                        Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(12.dp)).background(ManufacturingAmber.accent).clickable {
                            onConfirm(date.toString(), slot)
                            onDismiss()
                        },
                        contentAlignment = Alignment.Center,
                    ) { Text("Confirm", style = ManufacturingAmber.rowTitle().copy(fontSize = 14.sp)) }
                }
            }
        }
    }
}

/** Amber-themed date quick chips — Figma MFG-PICKER · Date (inline companion). */
@Composable
fun MfgDateQuickPicker(
    selectedIso: String,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit,
) {
    val today = LocalDate.now()
    val options = listOf(
        "Today" to today,
        "Tomorrow" to today.plusDays(1),
        "+3 days" to today.plusDays(3),
        "+1 week" to today.plusWeeks(1),
    )
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MfgFieldLabel("Quick pick")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { (label, date) ->
                val iso = date.toString()
                MfgModeChip(label, selected = selectedIso.take(10) == iso, onClick = { onSelect(iso) })
            }
        }
        Text(
            MfgFormat.day(selectedIso),
            style = ManufacturingAmber.rowTitle().copy(fontSize = 16.sp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(ManufacturingAmber.rInner)
                .background(ManufacturingAmber.chip)
                .border(1.dp, ManufacturingAmber.accent.copy(alpha = 0.35f), ManufacturingAmber.rInner)
                .padding(12.dp),
        )
    }
}

/** Month / budget period picker — Figma MFG-PICKER · Month (inline companion). */
@Composable
fun MfgMonthPicker(
    selectedYm: String,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit,
) {
    val current = runCatching { YearMonth.parse(selectedYm.take(7)) }.getOrElse { YearMonth.now() }
    val months = (-1..2).map { current.plusMonths(it.toLong()) }
    val fmt = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MfgFieldLabel("Period")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            months.forEach { ym ->
                val key = ym.toString()
                MfgModeChip(ym.format(fmt), selected = selectedYm.take(7) == key, onClick = { onSelect(key) })
            }
        }
    }
}

/** Date & time quick slots — Figma MFG-PICKER · Date & time (inline companion). */
@Composable
fun MfgDateTimeSlots(
    selectedSlot: String,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit,
) {
    val slots = listOf("Morning · 09:00", "Noon · 12:00", "Afternoon · 15:00", "Evening · 18:00")
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MfgFieldLabel("Dispatch / due slot")
        slots.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { slot ->
                    MfgModeChip(
                        slot,
                        selected = selectedSlot == slot,
                        onClick = { onSelect(slot) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
