package com.example.momentra.ui.shell.personal.shared

import com.example.momentra.data.api.ActivityItemDto
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/**
 * M5A — group co-occurring activities into richer Moment cards when evidence supports it.
 * Presentation stays [MomentCardModel]; singles remain 1:1.
 */
object MomentClustering {
    /** Max gap between activities in one cluster (2 hours). */
    private const val WINDOW_SECONDS = 2L * 60L * 60L

    fun cards(
        activities: List<ActivityItemDto>,
        family: PersonalPulseFamily,
    ): List<MomentCardModel> {
        val visible = activities
            .filter { PersonalActivityTimelineDerived.isVisible(it) }
            .sortedByDescending { parseInstant(it.occurredAt)?.toEpochMilli() ?: 0L }
        if (visible.isEmpty()) return emptyList()

        val clusters = mutableListOf<List<ActivityItemDto>>()
        var current = mutableListOf<ActivityItemDto>()

        for (activity in visible) {
            if (current.isEmpty()) {
                current.add(activity)
                continue
            }
            if (canJoin(activity, current)) {
                current.add(activity)
            } else {
                clusters.add(current.toList())
                current = mutableListOf(activity)
            }
        }
        if (current.isNotEmpty()) clusters.add(current.toList())

        return clusters.map { group ->
            if (group.size == 1) MomentCardModel.from(group[0], family)
            else fromCluster(group, family)
        }
    }

    private fun canJoin(candidate: ActivityItemDto, cluster: List<ActivityItemDto>): Boolean {
        val anchor = cluster.firstOrNull() ?: return false
        val t0 = parseInstant(anchor.occurredAt) ?: return false
        val t1 = parseInstant(candidate.occurredAt) ?: return false
        if (abs(ChronoUnit.SECONDS.between(t0, t1)) > WINDOW_SECONDS) return false
        val zone = ZoneId.systemDefault()
        if (t0.atZone(zone).toLocalDate() != t1.atZone(zone).toLocalDate()) return false

        val codes = (cluster + candidate).map { it.activityCode.uppercase() }
        val hasSpend = codes.any { it.contains("EXPENSE") }
        val hasSocial = codes.any { it.contains("CONNECTION") || it.contains("SHARED") || it.contains("SUPPORT") }
        val hasMood = codes.any { it.contains("MOOD") }
        val hasExperience = codes.any { it.contains("EXPERIENCE") || it.contains("WELLBEING") }
        val hasRecovery = codes.any { it.contains("RECOVERY") }

        if (hasSpend && (hasSocial || hasMood || hasExperience)) return true
        if (hasSocial && (hasMood || hasExperience)) return true
        if (hasRecovery && hasMood) return true

        if (hasSpend) {
            val merchants = (cluster + candidate).mapNotNull {
                it.activityPayload?.merchantName?.trim()?.lowercase()?.takeIf { m -> m.isNotEmpty() }
            }
            if (merchants.size >= 2 && merchants.toSet().size == 1) return true
        }
        return false
    }

    private fun fromCluster(
        group: List<ActivityItemDto>,
        family: PersonalPulseFamily,
    ): MomentCardModel {
        val primary = group.firstOrNull { PersonalActivityTimelineDerived.isExpense(it) }
            ?: group.firstOrNull { it.activityCode.uppercase().contains("CONNECTION") }
            ?: group.firstOrNull { it.activityCode.uppercase().contains("EXPERIENCE") }
            ?: group.first()

        val codes = group.map { it.activityCode.uppercase() }
        val hasSocial = codes.any { it.contains("CONNECTION") || it.contains("SHARED") }
        val hasMood = codes.any { it.contains("MOOD") }
        val hasExperience = codes.any { it.contains("EXPERIENCE") }

        val baseTitle = PersonalActivityTimelineDerived.displayTitle(primary)
        val title = if (hasSocial && PersonalActivityTimelineDerived.isExpense(primary)) {
            "$baseTitle with people"
        } else {
            baseTitle
        }

        val subtitleParts = mutableListOf<String>()
        if (hasSocial) subtitleParts.add("Connection")
        if (hasMood) {
            val mood = group.firstOrNull { it.activityCode.uppercase().contains("MOOD") }
            val chip = mood?.let { PersonalActivityTimelineDerived.feelingsChip(it) }
            subtitleParts.add(chip ?: "Mood")
        }
        if (hasExperience) subtitleParts.add("Experience")
        if (subtitleParts.isEmpty()) subtitleParts.add("${group.size} linked logs")

        val amount = group.mapNotNull { PersonalActivityTimelineDerived.amountLabel(it) }.firstOrNull()
        val sourceIds = group.map(::sourceKey)
        val primaryKey = sourceKey(primary)
        val orderedSourceIds = listOf(primaryKey) + sourceIds.filter { it != primaryKey }

        val earliest = group.minByOrNull { parseInstant(it.occurredAt)?.toEpochMilli() ?: Long.MAX_VALUE }
            ?: primary

        return MomentCardModel(
            id = "cluster:${sourceIds.sorted().joinToString("|")}",
            title = title,
            subtitle = subtitleParts.joinToString(" · "),
            contextLabel = PersonalActivityTimelineDerived.categoryChip(primary)
                ?: if (hasSocial) "People" else null,
            family = family,
            amountLabel = amount,
            signalLabel = when {
                hasMood -> "Mood"
                hasSocial -> "Presence"
                else -> null
            },
            peopleCount = if (hasSocial) {
                maxOf(2, group.count { it.activityCode.uppercase().contains("CONNECTION") } + 1)
            } else {
                null
            },
            occurredAt = earliest.occurredAt,
            sourceIconKind = MomentSourceIconKind.from(primary.activityCode),
            activityCode = primary.activityCode,
            sourceActivityIds = orderedSourceIds,
            tapDestination = MomentCardTapDestination.ACTIVITY_DETAIL,
            mediaUrls = emptyList(),
        )
    }

    private fun sourceKey(activity: ActivityItemDto): String =
        activity.activityPayload?.activityId
            ?: activity.activityPayload?.expenseId
            ?: activity.activityPayload?.incomeId
            ?: activity.activityPayload?.contributionId
            ?: "${activity.occurredAt}|${activity.title}|${activity.activityCode}"

    private fun parseInstant(raw: String): Instant? = runCatching { Instant.parse(raw) }.getOrNull()
}
