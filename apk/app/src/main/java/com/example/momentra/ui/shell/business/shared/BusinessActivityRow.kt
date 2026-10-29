package com.example.momentra.ui.shell.business.shared

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.OperatingActivityDto
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.theme.PlusJakartaSans
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Shared Business activity row, mirroring `GroupActivityRow` (Figma 584:15872):
 * a 2dp accent bar, a 36dp accent-tinted glyph tile, the event title, a muted meta line,
 * the relative time, and the right-aligned amount.
 *
 * Replaces the 14 per-module copies ("private fun ActivityRow" on Android,
 * "private func activityRow" on iOS) that each wrapped a whole bordered card per event.
 * Rows are separated by a hairline, not by a card.
 */
@Composable
fun BusinessActivityRow(
    row: OperatingActivityDto,
    model: OperatingProjectionDto,
    accent: Color,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
    onEdit: (() -> Unit)? = null,
) {
    val amount = if (model.financeVisible) {
        formatOperatingAmount(model.currency, row.amount, true).takeIf { it != "—" }
    } else {
        null
    }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onEdit != null) Modifier.clickable(onClick = onEdit) else Modifier)
                .padding(vertical = 12.dp),
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
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    businessActivityGlyph(row.eventType),
                    fontSize = 16.sp,
                    fontFamily = PlusJakartaSans,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    businessActivityTitle(row),
                    color = BusinessSheetTokens.text,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val meta = businessActivityMeta(row)
                if (meta.isNotBlank()) {
                    Text(
                        meta,
                        color = BusinessSheetTokens.muted,
                        fontFamily = PlusJakartaSans,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (amount != null) {
                    Text(
                        amount,
                        color = accent,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                    )
                }
                Text(
                    businessRelativeTime(row.occurredAt),
                    color = BusinessSheetTokens.muted,
                    fontFamily = PlusJakartaSans,
                    fontSize = 11.sp,
                )
            }
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 50.dp)
                    .height(1.dp)
                    .background(BusinessSheetTokens.border),
            )
        }
    }
}

/** Event label with underscores expanded, e.g. "WALK_IN_SALE" -> "Walk-in sale". */
fun businessActivityLabel(eventType: String): String {
    if (eventType.isBlank()) return "Activity"
    val words = eventType.replace('_', ' ').lowercase(Locale.US)
    return words.replaceFirstChar { it.titlecase(Locale.US) }
}

fun businessActivityTitle(row: OperatingActivityDto): String =
    row.title.ifBlank { businessActivityLabel(row.eventType) }

/** Counterparty, quantity, payment state and the calendar day — one muted line. */
fun businessActivityMeta(row: OperatingActivityDto): String {
    val qty = listOfNotNull(row.quantity, row.unit)
        .filter { it.isNotBlank() }
        .joinToString(" ")
    val payment = row.paymentStatus?.takeIf { it.isNotBlank() && it != "NONE" }
    val status = row.status?.takeIf {
        it.isNotBlank() && it != "COMPLETED" && it != "NONE"
    }
    val day = row.occurredAt.take(10).takeIf { it.isNotBlank() }
    return listOfNotNull(businessActivityLabel(row.eventType), row.counterpartyName, qty.ifBlank { null }, payment, status, day)
        .filter { it.isNotBlank() }
        .joinToString(" · ")
}

fun businessActivityGlyph(eventType: String): String {
    val upper = eventType.uppercase(Locale.US)
    return when {
        upper.contains("SALE") || upper.contains("REVENUE") -> "🧾"
        upper.contains("PURCHASE") || upper.contains("SUPPLIER") -> "📦"
        upper.contains("COLLECT") || upper.contains("PAYMENT") || upper.contains("SETTLE") -> "💰"
        upper.contains("SPEND") || upper.contains("EXPENSE") || upper.contains("COST") -> "💸"
        upper.contains("STOCK") || upper.contains("DELIVERY") || upper.contains("SHIPMENT") -> "🚚"
        upper.contains("ORDER") -> "📝"
        upper.contains("SERVICE") || upper.contains("APPOINTMENT") || upper.contains("BOOKING") -> "🎧"
        upper.contains("PRODUCTION") || upper.contains("JOB") || upper.contains("BATCH") -> "🏭"
        upper.contains("CAMPAIGN") || upper.contains("MARKETING") || upper.contains("LEAD") -> "📣"
        upper.contains("RETURN") || upper.contains("VOID") -> "↩️"
        else -> "📌"
    }
}

/** "Today, 4:12 PM" / "Yesterday, 9:30 AM" / "12 Oct, 6:05 PM" — matches Group. */
fun businessRelativeTime(raw: String): String {
    if (raw.isBlank()) return ""
    return try {
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
        raw.take(10)
    }
}

/** Convenience: sum a line's quantity × unit price, or null when either is unparseable. */
fun businessLineTotal(quantity: String?, unitPrice: String?): BigDecimal? {
    val qty = quantity?.toBigDecimalOrNull() ?: return null
    val price = unitPrice?.toBigDecimalOrNull() ?: return null
    return qty.multiply(price)
}

/**
 * Group a run of activity rows into one continuous list, so the parent column's
 * `spacedBy` gap does not land between every row.
 */
@Composable
fun BusinessActivityList(
    rows: List<OperatingActivityDto>,
    model: OperatingProjectionDto,
    accent: Color,
    modifier: Modifier = Modifier,
    onEdit: ((OperatingActivityDto) -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        rows.forEachIndexed { index, row ->
            BusinessActivityRow(
                row = row,
                model = model,
                accent = accent,
                showDivider = index < rows.lastIndex,
                onEdit = onEdit?.let { edit -> { edit(row) } },
            )
        }
    }
}
