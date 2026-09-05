package com.example.momentra.ui.shell.group.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R
import com.example.momentra.data.api.GroupLifePlanningItemDto
import com.example.momentra.data.api.GroupLifeUpdateDto
import com.example.momentra.ui.theme.PlusJakartaSans
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Open planning items sorted by dueAt then createdAt; take up to [limit]. */
fun recentOpenPlanningItems(
    items: List<GroupLifePlanningItemDto>,
    limit: Int = 5,
): List<GroupLifePlanningItemDto> {
    fun isOpen(status: String?) =
        status.isNullOrBlank() || !status.equals("DONE", true) && !status.equals("CANCELLED", true)

    return items
        .filter { isOpen(it.status) }
        .sortedWith(
            compareBy<GroupLifePlanningItemDto> { parseInstantMillis(it.dueAt) ?: Long.MAX_VALUE }
                .thenByDescending { parseInstantMillis(it.createdAt) ?: 0L },
        )
        .take(limit)
}

fun parseInstantMillis(iso: String?): Long? {
    if (iso.isNullOrBlank()) return null
    return runCatching { OffsetDateTime.parse(iso).toInstant().toEpochMilli() }.getOrNull()
        ?: runCatching {
            LocalDate.parse(iso.take(10)).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }.getOrNull()
}

fun planningItemDayKey(item: GroupLifePlanningItemDto): LocalDate? {
    val iso = item.dueAt ?: return null
    return runCatching { OffsetDateTime.parse(iso).atZoneSameInstant(ZoneId.systemDefault()).toLocalDate() }
        .getOrNull()
        ?: runCatching { LocalDate.parse(iso.take(10)) }.getOrNull()
}

fun formatPlanningTime(iso: String?): String? {
    if (iso.isNullOrBlank()) return null
    return runCatching {
        val odt = OffsetDateTime.parse(iso).atZoneSameInstant(ZoneId.systemDefault())
        odt.format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
    }.getOrNull()
        ?: runCatching {
            OffsetDateTime.parse(iso).format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
        }.getOrNull()
}

fun formatPlanningDayChip(day: LocalDate, today: LocalDate = LocalDate.now()): String {
    if (day == today) return "Today"
    if (day == today.plusDays(1)) return "Tomorrow"
    return day.format(DateTimeFormatter.ofPattern("EEE d", Locale.getDefault()))
}

fun isUrgentUpdate(item: GroupLifeUpdateDto): Boolean =
    item.urgencyCode.equals("URGENT", ignoreCase = true)

/** DONE / (OPEN+IN_PROGRESS+DONE); 0 when none countable. */
fun planningPlansPercent(items: List<GroupLifePlanningItemDto>): Int {
    var done = 0
    var countable = 0
    for (item in items) {
        val status = item.status.orEmpty().uppercase(Locale.US)
        if (status == "CANCELLED") continue
        countable++
        if (status == "DONE") done++
    }
    if (countable == 0) return 0
    return ((done.toDouble() / countable.toDouble()) * 100).toInt()
}

fun formatRelativeShort(iso: String?): String {
    val millis = parseInstantMillis(iso) ?: return ""
    val seconds = ((System.currentTimeMillis() - millis) / 1000).toInt()
    if (seconds < 60) return "just now"
    if (seconds < 3600) return "${seconds / 60}m ago"
    if (seconds < 86_400) return "${seconds / 3600}h ago"
    if (seconds < 86_400 * 7) return "${seconds / 86_400}d ago"
    return runCatching {
        OffsetDateTime.parse(iso).format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
    }.getOrDefault("")
}

fun formatPollClosesMeta(closesAt: String?, totalVotes: Int?): String {
    val votes = totalVotes ?: 0
    val votePart = if (votes == 1) "1 vote" else "$votes votes"
    val closeMillis = parseInstantMillis(closesAt) ?: return votePart
    val remaining = closeMillis - System.currentTimeMillis()
    if (remaining <= 0) return "Ended · $votePart"
    if (remaining < 3_600_000) {
        val mins = maxOf(1, (remaining / 60_000).toInt())
        return "Ends in ${mins}m · $votePart"
    }
    if (remaining < 86_400_000) {
        val hours = (remaining / 3_600_000).toInt()
        return "Ends in ${hours}h · $votePart"
    }
    val days = (remaining / 86_400_000).toInt()
    return if (days == 1) "Ends tomorrow · $votePart" else "Ends in ${days}d · $votePart"
}

/** Status pill text for polls list (Figma): "Ends in 2h", "Ends tomorrow", "Closed". */
fun formatPollEndsTag(closesAt: String?, status: String?): String {
    val upper = (status ?: "").uppercase(Locale.US)
    if (upper == "CLOSED" || upper == "CANCELLED") return "Closed"
    val closeMillis = parseInstantMillis(closesAt)
        ?: return if (upper == "OPEN" || upper.isEmpty()) "Open" else upper.replaceFirstChar { it.titlecase(Locale.US) }
    val remaining = closeMillis - System.currentTimeMillis()
    if (remaining <= 0) return "Closed"
    if (remaining < 3_600_000) {
        val mins = maxOf(1, (remaining / 60_000).toInt())
        return "Ends in ${mins}m"
    }
    if (remaining < 86_400_000) {
        val hours = (remaining / 3_600_000).toInt()
        return "Ends in ${hours}h"
    }
    val days = (remaining / 86_400_000).toInt()
    return if (days == 1) "Ends tomorrow" else "Ends in ${days}d"
}

fun initialsFromName(name: String?): String {
    val parts = name.orEmpty().split(" ").filter { it.isNotBlank() }
    if (parts.isEmpty()) return "?"
    if (parts.size == 1) return parts[0].take(2).uppercase(Locale.US)
    return "${parts[0].take(1)}${parts[1].take(1)}".uppercase(Locale.US)
}

fun formatBookingDay(iso: String?): String? {
    val millis = parseInstantMillis(iso) ?: return null
    return runCatching {
        OffsetDateTime.parse(iso).atZoneSameInstant(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
    }.getOrNull() ?: runCatching {
        java.time.Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
    }.getOrNull()
}

fun formatBookingDayTime(iso: String?): String? {
    if (iso.isNullOrBlank()) return null
    return runCatching {
        OffsetDateTime.parse(iso).atZoneSameInstant(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("d MMM · h:mm a", Locale.getDefault()))
    }.getOrNull()
}

fun formatItineraryDayLabel(dayIndex: Int, date: LocalDate): String {
    val day = date.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())).uppercase(Locale.US)
    return "DAY $dayIndex • $day"
}

fun itineraryDayGroups(
    items: List<GroupLifePlanningItemDto>,
    limit: Int = 3,
): List<Pair<LocalDate, List<GroupLifePlanningItemDto>>> {
    val open = recentOpenPlanningItems(items, limit = 50)
    val orderedDays = linkedSetOf<LocalDate>()
    val buckets = linkedMapOf<LocalDate, MutableList<GroupLifePlanningItemDto>>()
    for (item in open) {
        val day = planningItemDayKey(item) ?: continue
        if (orderedDays.add(day)) buckets[day] = mutableListOf()
        buckets[day]?.add(item)
    }
    return orderedDays.take(limit).mapNotNull { day ->
        val dayItems = buckets[day].orEmpty()
        if (dayItems.isEmpty()) null else day to dayItems
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanningScheduleSheet(
    items: List<GroupLifePlanningItemDto>,
    visible: Boolean,
    onDismiss: () -> Unit,
    momentTypeCode: String? = null,
    accent: Color = Color(0xFF14B8A6),
    surface: Color = Color(0xFF1C1A24),
    field: Color = Color(0xFF252230),
    border: Color = Color(0xFF322E40),
    text: Color = Color.White,
    muted: Color = Color(0xFF9E9AA8),
) {
    if (!visible) return
    val today = remember { LocalDate.now() }
    val dayKeys = remember(items) {
        val fromItems = items.mapNotNull { planningItemDayKey(it) }.distinct().sorted()
        (listOf(today) + fromItems).distinct().sorted()
    }
    var selectedDay by remember(dayKeys) { mutableStateOf(dayKeys.firstOrNull() ?: today) }
    val dayItems = remember(items, selectedDay) {
        items.filter { planningItemDayKey(it) == selectedDay }
            .sortedBy { parseInstantMillis(it.dueAt) ?: Long.MAX_VALUE }
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = surface,
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
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Schedule", color = text, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
            Text("Plans by day", color = muted, fontSize = 12.sp, fontFamily = PlusJakartaSans)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                dayKeys.forEach { day ->
                    val selected = day == selectedDay
                    Text(
                        formatPlanningDayChip(day, today),
                        color = if (selected) Color.White else muted,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                        fontFamily = PlusJakartaSans,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (selected) accent else field)
                            .border(1.dp, if (selected) accent else border, RoundedCornerShape(20.dp))
                            .clickable { selectedDay = day }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (dayItems.isEmpty()) {
                    Text(
                        "No plans for this day",
                        color = muted,
                        fontSize = 13.sp,
                        fontFamily = PlusJakartaSans,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                } else {
                    dayItems.forEach { item ->
                        PlanningScheduleRow(
                            item = item,
                            momentTypeCode = momentTypeCode,
                            field = field,
                            border = border,
                            text = text,
                            muted = muted,
                            accent = accent,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlanningScheduleRow(
    item: GroupLifePlanningItemDto,
    momentTypeCode: String?,
    field: Color,
    border: Color,
    text: Color,
    muted: Color,
    accent: Color,
) {
    val category = GroupPlanningCategoryCatalog.labelForCode(item.categoryCode, momentTypeCode)
    val time = formatPlanningTime(item.dueAt) ?: "All day"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(field)
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            time,
            color = accent,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
            modifier = Modifier.padding(end = 4.dp),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                category,
                color = accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
            Text(
                item.title ?: item.planningItemId.orEmpty(),
                color = text,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
        }
    }
}

@Composable
fun MomentsPlanningHeader(
    title: String,
    text: Color,
    muted: Color,
    accent: Color,
    onOpenSchedule: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = text, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
        Icon(
            painter = painterResource(R.drawable.ic_group_qa_calendar),
            contentDescription = "Schedule",
            tint = accent,
            modifier = Modifier
                .size(22.dp)
                .clickable(onClick = onOpenSchedule),
        )
    }
}

@Composable
fun MomentsPlanningRecentRow(
    item: GroupLifePlanningItemDto,
    momentTypeCode: String?,
    text: Color,
    muted: Color,
    accent: Color,
    field: Color,
    border: Color,
) {
    val category = GroupPlanningCategoryCatalog.labelForCode(item.categoryCode, momentTypeCode)
    val time = formatPlanningTime(item.dueAt)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(field)
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                category,
                color = accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
            if (!time.isNullOrBlank()) {
                Text(time, color = muted, fontSize = 11.sp, fontFamily = PlusJakartaSans)
            }
        }
        Text(
            item.title ?: item.planningItemId.orEmpty(),
            color = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
        )
    }
}

@Composable
fun MomentsUrgentUpdateRow(
    item: GroupLifeUpdateDto,
    text: Color,
    muted: Color,
    field: Color,
    border: Color,
) {
    val urgent = isUrgentUpdate(item)
    val accentBorder = if (urgent) Color(0xFFF59E0B) else border
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(field)
            .border(1.dp, accentBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (urgent) {
            Text(
                "Urgent",
                color = Color(0xFFF87171),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF87171).copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
        Text(
            item.message ?: item.updateId.orEmpty(),
            color = text,
            fontSize = 13.sp,
            fontFamily = PlusJakartaSans,
        )
        item.createdAt?.let {
            Text(it.take(16).replace('T', ' '), color = muted, fontSize = 11.sp, fontFamily = PlusJakartaSans)
        }
    }
}
