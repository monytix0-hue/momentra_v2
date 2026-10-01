package com.example.momentra.ui.shell.personal.shared

import com.example.momentra.data.api.ActivityItemDto
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class MomentCardTapDestination {
    /** M2: open existing activity detail/edit flow. */
    ACTIVITY_DETAIL,
    /** Reserved for M5 clustered / cinematic moment detail. */
    MOMENT_DETAIL,
}

enum class MomentSourceIconKind {
    SPEND, INCOME, MOOD, RECOVERY, ATTENTION,
    MILESTONE, PROGRESS, LEARNING, OPPORTUNITY, PIVOT,
    EXPERIENCE, WELLBEING, DISCOVERY,
    CONNECTION, SHARED, SUPPORT,
    GENERIC,
    ;

    companion object {
        fun from(activityCode: String): MomentSourceIconKind {
            val code = activityCode.uppercase()
            return when {
                code.contains("EXPENSE") -> SPEND
                code.contains("INCOME") -> INCOME
                code.contains("MOOD") -> MOOD
                code.contains("RECOVERY") -> RECOVERY
                code.contains("ATTENTION") -> ATTENTION
                code.contains("MILESTONE") -> MILESTONE
                code.contains("PROGRESS") -> PROGRESS
                code.contains("LEARNING") -> LEARNING
                code.contains("OPPORTUNITY") -> OPPORTUNITY
                code.contains("PIVOT") -> PIVOT
                code.contains("EXPERIENCE") -> EXPERIENCE
                code.contains("WELLBEING") -> WELLBEING
                code.contains("DISCOVERY") || code.contains("CREATION") -> DISCOVERY
                code.contains("CONNECTION") -> CONNECTION
                code.contains("SHARED") -> SHARED
                code.contains("SUPPORT") -> SUPPORT
                else -> GENERIC
            }
        }
    }
}

/**
 * Canonical Moment card for Pulse Recent and Moments stream.
 * v1 maps 1:1 from an activity row; M5 clusters can set multiple [sourceActivityIds].
 */
data class MomentCardModel(
    val id: String,
    val title: String,
    val subtitle: String?,
    val contextLabel: String?,
    val family: PersonalPulseFamily,
    val amountLabel: String?,
    val signalLabel: String?,
    val peopleCount: Int?,
    val occurredAt: String,
    val sourceIconKind: MomentSourceIconKind,
    val activityCode: String,
    val sourceActivityIds: List<String>,
    val tapDestination: MomentCardTapDestination = MomentCardTapDestination.ACTIVITY_DETAIL,
    /** M5B — signed media URLs when present; empty for activity-only cards. */
    val mediaUrls: List<String> = emptyList(),
) {
    companion object {
        fun from(activity: ActivityItemDto, family: PersonalPulseFamily): MomentCardModel {
            val code = activity.activityCode.uppercase()
            val context = PersonalActivityTimelineDerived.categoryChip(activity)
                ?: typeContextLabel(code, family)
            val subtitle = PersonalActivityTimelineDerived.feelingsChip(activity)
                ?: activity.activityPayload?.description
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() && it.length <= 80 && !looksLikeEssay(it) }
            val sourceId = activity.activityPayload?.activityId
                ?: activity.activityPayload?.expenseId
                ?: activity.activityPayload?.incomeId
                ?: activity.activityPayload?.contributionId
                ?: "${activity.occurredAt}|${activity.title}|${activity.activityCode}"
            return MomentCardModel(
                id = sourceId,
                title = PersonalActivityTimelineDerived.displayTitle(activity),
                subtitle = subtitle,
                contextLabel = context,
                family = family,
                amountLabel = PersonalActivityTimelineDerived.amountLabel(activity),
                signalLabel = signalLabel(code, family),
                peopleCount = null,
                occurredAt = activity.occurredAt,
                sourceIconKind = MomentSourceIconKind.from(activity.activityCode),
                activityCode = activity.activityCode,
                sourceActivityIds = listOf(sourceId),
                tapDestination = MomentCardTapDestination.ACTIVITY_DETAIL,
                mediaUrls = emptyList(),
            )
        }

        private fun looksLikeEssay(text: String): Boolean {
            val lower = text.lowercase()
            return lower.contains("feelings:") || lower.contains("paid from:") || text.length > 60
        }

        private fun typeContextLabel(code: String, family: PersonalPulseFamily): String? = when {
            code.contains("EXPENSE") -> null
            code.contains("MOOD") -> "Mood"
            code.contains("RECOVERY") -> "Recovery"
            code.contains("ATTENTION") -> "Attention"
            code.contains("MILESTONE") -> "Milestone"
            code.contains("PROGRESS") -> "Progress"
            code.contains("LEARNING") -> "Learning"
            code.contains("EXPERIENCE") -> "Experience"
            code.contains("CONNECTION") -> "Connection"
            code.contains("SHARED") -> "Shared"
            code.contains("SUPPORT") -> "Support"
            else -> when (family) {
                PersonalPulseFamily.LIFE_OPERATIONS -> "Everyday"
                PersonalPulseFamily.FUTURE_BUILDING -> "Future"
                PersonalPulseFamily.LIFESTYLE -> "Lifestyle"
                PersonalPulseFamily.RELATIONSHIPS -> "People"
            }
        }

        private fun signalLabel(code: String, family: PersonalPulseFamily): String? = when {
            code.contains("RECOVERY") -> "Recovery"
            code.contains("ATTENTION") -> "Discipline"
            code.contains("MILESTONE") -> "Milestone"
            code.contains("PROGRESS") -> "Progress"
            code.contains("EXPERIENCE") -> "Joy"
            code.contains("CONNECTION") -> "Presence"
            else -> null
        }
    }
}

object MomentCardTime {
    private val zone: ZoneId = ZoneId.systemDefault()
    private val clockFmt = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    private val dayFmt = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())

    fun label(occurredAt: String): String {
        val date = parse(occurredAt) ?: return ""
        val local = date.atZone(zone).toLocalDate()
        val today = LocalDate.now(zone)
        return when {
            local == today -> clockFmt.format(date.atZone(zone))
            local == today.minusDays(1) -> "Yesterday"
            else -> dayFmt.format(date.atZone(zone))
        }
    }

    fun parse(raw: String): Instant? = runCatching {
        Instant.parse(raw)
    }.getOrNull() ?: runCatching {
        java.time.OffsetDateTime.parse(raw).toInstant()
    }.getOrNull()
}

data class MomentStreamSection(
    val header: String,
    val cards: List<MomentCardModel>,
)

object MomentStreamGrouping {
    fun sections(cards: List<MomentCardModel>): List<MomentStreamSection> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val weekAgo = today.minusDays(7)
        val monthFmt = DateTimeFormatter.ofPattern("MMMM", Locale.getDefault())
        val order = mutableListOf<String>()
        val map = linkedMapOf<String, MutableList<MomentCardModel>>()
        for (card in cards) {
            val instant = MomentCardTime.parse(card.occurredAt)
            val header = if (instant == null) {
                "Earlier"
            } else {
                val local = instant.atZone(zone).toLocalDate()
                when {
                    local == today -> "Today"
                    local == today.minusDays(1) -> "Yesterday"
                    !local.isBefore(weekAgo) -> "Earlier this week"
                    else -> monthFmt.format(local)
                }
            }
            if (header !in map) {
                order += header
                map[header] = mutableListOf()
            }
            map[header]!!.add(card)
        }
        return order.map { MomentStreamSection(it, map[it].orEmpty()) }
    }
}
