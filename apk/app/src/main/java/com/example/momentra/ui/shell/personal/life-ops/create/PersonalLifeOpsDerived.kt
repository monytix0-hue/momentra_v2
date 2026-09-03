package com.example.momentra.ui.shell.personal.lifeops.create

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.min
import kotlin.math.roundToInt

/** Shared Life Ops derived metrics for Pulse / Moments / Memory populated screens. */
object PersonalLifeOpsDerived {
    fun scoreNumber(raw: String?): Double? {
        val s = raw?.trim().orEmpty()
        if (s.isEmpty()) return null
        return s.toDoubleOrNull()
    }

    fun displayScore(raw: String?): String {
        val n = scoreNumber(raw) ?: return "—"
        return n.roundToInt().toString()
    }

    fun pressure(fromRecovery: String?): String {
        val n = scoreNumber(fromRecovery) ?: return "—"
        return (100.0 - n).roundToInt().toString()
    }

    fun attentionDisplay(count: Int?): String {
        val n = count ?: return "—"
        if (n <= 0) return "—"
        return min(100, 40 + n * 8).toString()
    }

    fun statusBadge(scoreRaw: String?, axis: String): String {
        val n = scoreNumber(scoreRaw) ?: return "Empty"
        return when (axis.lowercase()) {
            "pressure" -> when {
                n >= 70 -> "High"
                n >= 45 -> "Moderate"
                else -> "Low"
            }
            "recovery" -> when {
                n >= 75 -> "Strong"
                n >= 50 -> "Steady"
                else -> "Low"
            }
            "discipline" -> when {
                n >= 80 -> "Excellent"
                n >= 55 -> "Good"
                else -> "Building"
            }
            "attention" -> when {
                n >= 75 -> "Good"
                n >= 45 -> "Fair"
                else -> "Low"
            }
            else -> when {
                n >= 80 -> "Excellent"
                n >= 60 -> "Good"
                else -> "Building"
            }
        }
    }

    fun stageBand(wellbeing: String?): String {
        val n = scoreNumber(wellbeing) ?: return "Stabilizing"
        return when {
            n >= 75 -> "Thriving"
            n >= 50 -> "Structured"
            else -> "Stabilizing"
        }
    }

    fun streakDays(occurredAts: List<String>): Int {
        val zone = ZoneId.systemDefault()
        val days = occurredAts.mapNotNull { parseInstant(it)?.atZone(zone)?.toLocalDate() }.toSet()
        if (days.isEmpty()) return 0
        var streak = 0
        var cursor = LocalDate.now(zone)
        while (days.contains(cursor)) {
            streak += 1
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    fun relativeTime(iso: String): String {
        val instant = parseInstant(iso) ?: return iso
        val seconds = ChronoUnit.SECONDS.between(instant, Instant.now())
        return when {
            seconds < 60 -> "Just now"
            seconds < 3600 -> "${seconds / 60}m ago"
            seconds < 86_400 -> "${seconds / 3600}h ago"
            seconds < 172_800 -> "Yesterday"
            else -> "${seconds / 86_400}d ago"
        }
    }

    data class DriverItem(val label: String, val helping: Boolean)

    fun helpingHurting(activities: List<Pair<String, String>>): Pair<List<DriverItem>, List<DriverItem>> {
        val helping = mutableListOf<DriverItem>()
        val hurting = mutableListOf<DriverItem>()
        activities.take(8).forEach { (code, title) ->
            val upper = code.uppercase()
            when {
                upper.contains("MILESTONE") -> helping += DriverItem("Vision +", true)
                upper.contains("LEARNING") -> helping += DriverItem("Growth +", true)
                upper.contains("OPPORTUNITY") -> helping += DriverItem("Opportunity +", true)
                upper.contains("PROGRESS") -> helping += DriverItem("Momentum +", true)
                upper.contains("PIVOT") -> helping += DriverItem("Pivot +", true)
                upper.contains("EXPERIENCE") -> helping += DriverItem("Joy +", true)
                upper.contains("WELLBEING") -> helping += DriverItem("Vitality +", true)
                upper.contains("DISCOVERY") || upper.contains("CREATION") ->
                    helping += DriverItem("Exploration +", true)
                upper.contains("RECOVERY") -> helping += DriverItem("Recovery +", true)
                upper.contains("MOOD") -> helping += DriverItem("Mood · $title", true)
                upper.contains("RHYTHM") || upper.contains("LIFESTYLE") ->
                    helping += DriverItem("Focus +", true)
                upper.contains("EXPENSE") -> hurting += DriverItem("Spend · $title", false)
            }
        }
        return helping.take(3) to hurting.take(3)
    }

    fun identityLabel(
        wellbeing: String?,
        recovery: String?,
        activityCount: Int,
    ): Triple<String, String, String> {
        if (activityCount <= 0 && scoreNumber(wellbeing) == null && scoreNumber(recovery) == null) {
            return Triple(
                "Building Operator",
                "Low confidence",
                "Log recovery, mood, and spend to reveal your operating identity.",
            )
        }
        return when (stageBand(wellbeing)) {
            "Thriving" -> Triple(
                "Adaptive Operator",
                "${min(92, 55 + activityCount * 3)}% confidence",
                "You respond best when structure and recovery work together.",
            )
            "Structured" -> Triple(
                "Structured Operator",
                "${min(85, 45 + activityCount * 3)}% confidence",
                "Your rhythm is forming — keep pairing pressure with recovery.",
            )
            else -> Triple(
                "Stabilizing Operator",
                "Building…",
                "Early signals show up once recovery and attention logs accumulate.",
            )
        }
    }

    fun parseInstant(iso: String): Instant? = runCatching { Instant.parse(iso) }.getOrNull()

    private val dayFmt: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
}
