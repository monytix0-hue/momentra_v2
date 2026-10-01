package com.example.momentra.ui.shell.business.shared

import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

enum class BusinessLifeLens {
    OVERVIEW,
    MONEY,
    DAILY,
    TEAM,
}

enum class BusinessLifeMovementTab {
    ACTIVITY,
    MONEY,
}

data class BusinessLifeSignalFact(
    val title: String,
    val family: String,
    val statusLabel: String,
)

data class BusinessLifeActivityFact(
    val title: String,
    val occurredAt: String,
    val activityCode: String,
)

data class BusinessLifeFacts(
    val sections: Map<String, String> = emptyMap(),
    val runway: Map<String, Any?> = emptyMap(),
    val daily: Map<String, Any?> = emptyMap(),
    val team: Map<String, Any?> = emptyMap(),
    val runwayStatus: String? = null,
    val dailyStatus: String? = null,
    val teamStatus: String? = null,
    val runwayMonths: String? = null,
    val signals: List<BusinessLifeSignalFact> = emptyList(),
    val activity: List<BusinessLifeActivityFact> = emptyList(),
)

data class BusinessLifeStateLine(
    val title: String,
    val state: String,
)

data class BusinessLifeValueLine(
    val label: String,
    val value: String,
)

data class BusinessLifeCompanyState(
    val overview: List<BusinessLifeStateLine>,
    val thisWeek: String,
    val activity: List<BusinessLifeActivityFact>,
    val moneyMovement: List<BusinessLifeValueLine>,
    val needsAttention: List<String>,
    val working: List<String>,
    val financialPosition: List<BusinessLifeValueLine>,
)

private val genericStates = setOf(
    "money & cash flow",
    "daily business",
    "team & work",
    "vendor operations",
)

fun businessLifeLens(momentTypeCode: String?): BusinessLifeLens {
    val code = momentTypeCode.orEmpty().uppercase()
    return when {
        code.contains("TEAM") -> BusinessLifeLens.TEAM
        code.contains("RUNWAY") -> BusinessLifeLens.MONEY
        code.contains("OPERATIONS") -> BusinessLifeLens.DAILY
        else -> BusinessLifeLens.OVERVIEW
    }
}

fun movementTabFor(lens: BusinessLifeLens): BusinessLifeMovementTab =
    if (lens == BusinessLifeLens.MONEY) BusinessLifeMovementTab.MONEY else BusinessLifeMovementTab.ACTIVITY

fun buildBusinessLifeCompanyState(
    facts: BusinessLifeFacts,
    now: Instant,
    zone: ZoneId,
): BusinessLifeCompanyState {
    val overview = listOf(
        BusinessLifeStateLine(
            "Money",
            familyState(facts.sections["runway"], facts.runway, facts.runwayStatus, facts.signals, setOf("RUNWAY")),
        ),
        BusinessLifeStateLine(
            "Daily Business",
            familyState(
                facts.sections["businessOperations"],
                facts.daily,
                facts.dailyStatus,
                facts.signals,
                setOf("OPERATIONS"),
            ),
        ),
        BusinessLifeStateLine(
            "Team",
            familyState(
                facts.sections["teamOperations"],
                facts.team,
                facts.teamStatus,
                facts.signals,
                setOf("TEAM_OPS", "TEAM_OPERATIONS"),
            ),
        ),
    )
    return BusinessLifeCompanyState(
        overview = overview,
        thisWeek = weekSummary(facts.activity, now, zone),
        activity = facts.activity,
        moneyMovement = moneyMovement(facts),
        needsAttention = facts.signals
            .filter { it.statusLabel.equals("Action", true) || it.statusLabel.equals("Watch", true) }
            .map { it.title }
            .filter { it.isNotBlank() },
        working = facts.signals
            .filter { it.statusLabel.equals("Healthy", true) }
            .map { it.title }
            .filter { it.isNotBlank() },
        financialPosition = financialPosition(facts),
    )
}

private fun familyState(
    section: String?,
    payload: Map<String, Any?>,
    statusLabel: String?,
    signals: List<BusinessLifeSignalFact>,
    families: Set<String>,
): String {
    if (section == "EMPTY_SUPPORTED" || payload.isEmpty()) return "Not set up"
    val own = signals.filter { it.family.uppercase() in families }
    if (own.any { it.statusLabel.equals("Action", true) }) return "Needs attention"
    if (own.any { it.statusLabel.equals("Watch", true) }) return "Watch"
    val label = statusLabel?.trim().orEmpty()
    if (label.isNotEmpty() && label.lowercase() !in genericStates) return label
    if (own.any { it.statusLabel.equals("Healthy", true) }) return "Steady"
    return "Recorded"
}

private fun weekSummary(
    activity: List<BusinessLifeActivityFact>,
    now: Instant,
    zone: ZoneId,
): String {
    if (activity.isEmpty()) return "Nothing recorded yet."
    val start = now.atZone(zone).toLocalDate().minusDays(6).atStartOfDay(zone).toInstant()
    val count = activity.count { fact ->
        val at = runCatching { Instant.parse(fact.occurredAt) }.getOrNull() ?: return@count false
        !at.isBefore(start) && !at.isAfter(now.plus(1, ChronoUnit.MINUTES))
    }
    return if (count == 0) {
        "No recent company events are from this week."
    } else {
        val noun = if (count == 1) "event is" else "events are"
        "$count recent company $noun from this week."
    }
}

private fun moneyMovement(facts: BusinessLifeFacts): List<BusinessLifeValueLine> {
    val lines = mutableListOf<BusinessLifeValueLine>()
    presentValue(facts.runway["availableCash"])?.let { lines += BusinessLifeValueLine("Available cash", it) }
    presentValue(facts.runway["revenueTotal"])?.let { lines += BusinessLifeValueLine("Revenue", it) }
    presentValue(facts.runway["expenseTotal"])?.let { lines += BusinessLifeValueLine("Expenses", it) }
    presentValue(facts.runway["monthlyRevenue"])?.let { lines += BusinessLifeValueLine("Monthly revenue", it) }
    presentValue(facts.runway["monthlySpending"])?.let { lines += BusinessLifeValueLine("Monthly spending", it) }
    return lines
}

private fun financialPosition(facts: BusinessLifeFacts): List<BusinessLifeValueLine> {
    val lines = mutableListOf<BusinessLifeValueLine>()
    presentValue(facts.runway["availableCash"])?.let { lines += BusinessLifeValueLine("Available cash", it) }
    presentValue(facts.runway["revenueTotal"])?.let { lines += BusinessLifeValueLine("Revenue", it) }
    presentValue(facts.runway["expenseTotal"])?.let { lines += BusinessLifeValueLine("Expenses", it) }
    presentValue(facts.runwayMonths)?.let { lines += BusinessLifeValueLine("Runway", "$it months") }
    return lines
}

private fun presentValue(value: Any?): String? {
    if (value == null) return null
    val text = value.toString().trim()
    if (text.isEmpty() || text.equals("null", true)) return null
    return text
}

const val BUSINESS_LIFE_FINANCE_SCOPE = "Company totals"
