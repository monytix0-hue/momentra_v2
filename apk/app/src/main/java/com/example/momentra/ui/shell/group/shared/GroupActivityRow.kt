package com.example.momentra.ui.shell.group.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.ActivityItemDto
import com.example.momentra.ui.theme.PlusJakartaSans
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Shared Pulse / All-activity row matching Figma 584:15872:
 * left accent bar + 36dp rounded icon + actor-prefixed title + relative time.
 */
@Composable
fun GroupActivityRow(
    item: ActivityItemDto,
    accent: Color,
    textColor: Color = Color(0xFFE5E0EE),
    secondaryColor: Color = Color(0xFFC9C4D8),
    showChevron: Boolean = false,
    compactPadding: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val canTap = onClick != null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (canTap) Modifier.clickable(onClick = onClick!!) else Modifier)
            .padding(
                horizontal = if (compactPadding) 0.dp else 16.dp,
                vertical = if (compactPadding) 0.dp else 12.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(36.dp)
                .background(accent.copy(alpha = 0.4f), RoundedCornerShape(1.dp)),
        )
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(groupActivityGlyph(item.activityCode), fontSize = 16.sp)
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                groupActivityDisplayTitle(item),
                color = textColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
            )
            Text(
                formatGroupActivityOccurredAt(item.occurredAt),
                color = secondaryColor,
                fontSize = 11.sp,
                fontFamily = PlusJakartaSans,
            )
        }
        if (showChevron) {
            Text("›", color = secondaryColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

fun groupActivityDisplayTitle(item: ActivityItemDto): String =
    groupActivityDisplayTitle(item.title, item.activityCode, item.actorDisplayName)

fun groupActivityDisplayTitle(
    title: String,
    activityCode: String,
    actorDisplayName: String?,
): String {
    val trimmedTitle = title.trim()
    val actor = actorDisplayName?.trim()?.takeIf { it.isNotEmpty() } ?: return trimmedTitle
    val firstName = actor.split(Regex("\\s+")).firstOrNull()?.takeIf { it.isNotEmpty() } ?: actor
    if (trimmedTitle.contains(firstName, ignoreCase = true) ||
        trimmedTitle.startsWith(actor, ignoreCase = true)
    ) {
        return trimmedTitle
    }
    return "$firstName ${groupActivityVerb(activityCode)} $trimmedTitle"
}

fun groupActivityVerb(activityCode: String): String {
    val upper = activityCode.uppercase(Locale.US)
    return when {
        upper.contains("UPDATE") -> "shared"
        upper.contains("BOOKING") || upper.contains("ATTENDANCE") -> "confirmed"
        upper.contains("SETTLE") -> "recorded"
        upper.contains("POLL") || upper.contains("RULE") -> "created"
        upper.contains("OWNERSHIP") || upper.contains("TRANSFER") -> "updated"
        upper.contains("PLANNING") ||
            upper.contains("EXPENSE") ||
            upper.contains("PURCHASE") ||
            upper.contains("MEMORY") ||
            upper.contains("RESIDENT") ||
            upper.contains("VENDOR") ||
            upper.contains("ASSET") ||
            upper.contains("MAINTENANCE") ||
            upper.contains("MEMBER") ||
            upper.contains("CONTRIB") -> "added"
        else -> "updated"
    }
}

fun groupActivityGlyph(code: String): String {
    val upper = code.uppercase(Locale.US)
    return when {
        upper.contains("EXPENSE") || upper.contains("CONTRIB") -> "💸"
        upper.contains("SETTLE") || upper.contains("ATTENDANCE") -> "✅"
        upper.contains("BOOKING") -> "🎧"
        upper.contains("PURCHASE") || upper.contains("DELIVERY") -> "🍔"
        upper.contains("UPDATE") || upper.contains("MEMORY") || upper.contains("POLL") -> "🎵"
        upper.contains("MEMBER") || upper.contains("RESIDENT") -> "👋"
        else -> "📌"
    }
}

fun formatGroupActivityOccurredAt(raw: String): String = try {
    val instant = Instant.parse(raw)
    val zone = ZoneId.systemDefault()
    val date = instant.atZone(zone).toLocalDate()
    val today = LocalDate.now(zone)
    val time = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
        .withZone(zone)
        .format(instant)
    when (date) {
        today -> "Today, $time"
        today.minusDays(1) -> "Yesterday, $time"
        else -> {
            val day = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
                .withZone(zone)
                .format(instant)
            "$day, $time"
        }
    }
} catch (_: Exception) {
    raw
}

/** Accent cycle for Experience family rows (Figma 584:15872 blues). */
fun groupActivityAccentCycle(index: Int, base: Color = Color(0xFF3B82F6)): Color {
    val palette = listOf(
        Color(0xFF3B82F6),
        Color(0xFF60A5FA),
        Color(0xFF93C5FD),
    )
    return palette[index % palette.size]
}
