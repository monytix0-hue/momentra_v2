package com.example.momentra.ui.shell.personal

import androidx.compose.ui.graphics.Color
import com.example.momentra.data.api.ActivityItemDto
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

/** Client-side helpers for Activity Timeline (Figma 1006:8434). */
object PersonalActivityTimelineDerived {
    data class FilterChip(val id: String, val label: String, val emoji: String)

    data class TimelineStats(
        val totalLogs: Int,
        val thisMonth: Int,
        val totalAmountLabel: String,
    )

    data class RowVisual(
        val emoji: String,
        val accent: Color,
        val metadata: String,
        val timeLabel: String,
    )

    val primaryFilters = listOf(
        FilterChip("Money", "Money", "💰"),
        FilterChip("Attention", "Attention", "🎯"),
        FilterChip("Recovery", "Recovery", "💪"),
    )

    val categoryFilters = listOf(
        FilterChip("Groceries", "Groceries", "🛒"),
        FilterChip("Shopping", "Shopping", "🛍️"),
        FilterChip("Food", "Food", "🍔"),
        FilterChip("Housing", "Housing", "🏠"),
        FilterChip("Transport", "Transport", "🚗"),
    )

    fun matchesFilter(item: ActivityItemDto, filter: String): Boolean {
        val code = item.activityCode.uppercase()
        val title = item.title.uppercase()
        val category = item.activityPayload?.categoryCode?.uppercase().orEmpty()
        return when (filter) {
            "All" -> true
            "Money" -> code.contains("EXPENSE")
            "Attention" -> code.contains("ATTENTION") || title.contains("ATTENTION") || title.contains("DEEP WORK")
            "Recovery" -> code.contains("RECOVERY") || code.contains("WELLBEING") || title.contains("RECOVERY")
            "Groceries" -> category.contains("GROCER") || title.contains("GROCER")
            "Shopping" -> category.contains("SHOP") || title.contains("SHOP")
            "Food" -> category.contains("FOOD") || category.contains("CAFE") || title.contains("FOOD")
            "Housing" -> category.contains("HOUSING") || category.contains("RENT") || title.contains("HOUSING")
            "Transport" -> category.contains("TRANSPORT") || category.contains("FUEL") || title.contains("TRANSPORT")
            else -> true
        }
    }

    fun matchesSearch(item: ActivityItemDto, query: String): Boolean {
        if (query.isBlank()) return true
        val q = query.trim().lowercase(Locale.getDefault())
        val haystack = buildList {
            add(item.title)
            add(item.activityCode)
            item.activityPayload?.categoryCode?.let { add(it) }
            item.activityPayload?.amount?.let { add(it) }
            item.activityPayload?.description?.let { add(it) }
        }.joinToString(" ").lowercase(Locale.getDefault())
        return haystack.contains(q)
    }

    fun computeStats(items: List<ActivityItemDto>): TimelineStats {
        val now = Instant.now()
        val zone = ZoneId.systemDefault()
        val monthStart = now.atZone(zone).withDayOfMonth(1).toLocalDate().atStartOfDay(zone).toInstant()
        val thisMonth = items.count {
            parseInstant(it.occurredAt)?.isAfter(monthStart) == true
        }
        val expenseAmounts = items.mapNotNull { item ->
            if (!item.activityCode.contains("EXPENSE", ignoreCase = true)) return@mapNotNull null
            item.activityPayload?.amount?.toDoubleOrNull()
        }
        val totalAmountLabel = if (expenseAmounts.isEmpty()) {
            "—"
        } else {
            val sum = expenseAmounts.sum()
            val currency = items.firstNotNullOfOrNull { it.activityPayload?.currencyCode } ?: "INR"
            val symbol = if (currency == "INR") "₹" else currency
            "$symbol${formatCompact(sum)}"
        }
        return TimelineStats(
            totalLogs = items.size,
            thisMonth = thisMonth,
            totalAmountLabel = totalAmountLabel,
        )
    }

    fun rowVisual(item: ActivityItemDto): RowVisual {
        val code = item.activityCode.uppercase()
        val payload = item.activityPayload
        val category = payload?.categoryCode?.let { PersonalExpenseCategoryCatalog.labelForCode(it) }
        val amount = payload?.amount?.let { amt ->
            val cur = payload.currencyCode?.let { if (it == "INR") "₹" else "$it " } ?: "₹"
            "$cur$amt"
        }
        val signal = when {
            code.contains("EXPENSE") -> "Low pressure"
            code.contains("RECOVERY") -> "Calm"
            code.contains("MOOD") || code.contains("WELLBEING") -> "Focused"
            code.contains("ATTENTION") -> "High focus"
            else -> "Logged"
        }
        val metadataParts = buildList {
            category?.let { add(it) }
            if (code.contains("EXPENSE") && amount != null) add(amount)
            if (!code.contains("EXPENSE")) add(typeLabel(code))
            add(signal)
        }
        return RowVisual(
            emoji = emojiFor(code),
            accent = accentFor(code),
            metadata = metadataParts.joinToString(" · "),
            timeLabel = formatTimelineTime(item.occurredAt),
        )
    }

    fun isExpense(item: ActivityItemDto): Boolean =
        item.activityCode.contains("EXPENSE", ignoreCase = true) ||
            item.activityPayload?.expenseId != null

    fun isVisible(item: ActivityItemDto): Boolean =
        item.activityPayload?.status != "VOIDED"

    private fun typeLabel(code: String): String = when {
        code.contains("RECOVERY") -> "Recovery"
        code.contains("ATTENTION") -> "Attention"
        code.contains("MOOD") -> "Mood"
        code.contains("TRANSFER") -> "Money"
        else -> code.replace('_', ' ').lowercase().replaceFirstChar { it.titlecase() }
    }

    private fun emojiFor(code: String): String = when {
        code.contains("EXPENSE") -> "🛒"
        code.contains("RECOVERY") -> "💪"
        code.contains("ATTENTION") -> "🎯"
        code.contains("MOOD") -> "🧘"
        code.contains("TRANSFER") -> "💸"
        else -> "•"
    }

    private fun accentFor(code: String): Color = when {
        code.contains("EXPENSE") -> Color(0xFF10B981)
        code.contains("RECOVERY") -> Color(0xFF3B82F6)
        code.contains("ATTENTION") -> Color(0xFFF97316)
        code.contains("MOOD") -> Color(0xFF7C5CFC)
        code.contains("TRANSFER") -> Color(0xFF10B981)
        else -> Color(0xFFEC4899)
    }

    private fun parseInstant(iso: String): Instant? = try {
        Instant.parse(iso)
    } catch (_: Exception) {
        null
    }

    private fun formatTimelineTime(iso: String): String {
        val instant = parseInstant(iso) ?: return iso
        val zoned = instant.atZone(ZoneId.systemDefault())
        val now = Instant.now().atZone(ZoneId.systemDefault())
        val days = ChronoUnit.DAYS.between(zoned.toLocalDate(), now.toLocalDate())
        val time = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()).format(zoned)
        return when (days) {
            0L -> "TODAY $time"
            1L -> "YESTERDAY $time"
            else -> "${days} DAYS AGO $time"
        }
    }

    private fun formatCompact(value: Double): String {
        return if (value >= 1000) {
            "${(value / 1000.0).roundToInt()}k"
        } else {
            value.roundToInt().toString()
        }
    }
}
