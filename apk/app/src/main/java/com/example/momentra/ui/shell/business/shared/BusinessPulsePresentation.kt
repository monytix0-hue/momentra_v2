package com.example.momentra.ui.shell.business.shared

import com.example.momentra.data.api.ActivityItemDto
import com.example.momentra.data.api.BusinessApprovalItemDto
import com.example.momentra.data.api.BusinessFinancePayloadDto
import com.example.momentra.data.api.BusinessIssueItemDto
import com.example.momentra.data.api.OpsPulseExtrasDto
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Shown value, a real zero, or omitted. A DTO default of 0 is not [Zero]. */
sealed class PulseMetric {
    data class Available(val value: String) : PulseMetric()
    data object Zero : PulseMetric()
    data object Unavailable : PulseMetric()
}

data class PulseFact(
    val label: String,
    val metric: PulseMetric,
    /** Finance snapshot or Life cash/runway. Not a location total. */
    val companyWide: Boolean,
)

private val pulseActivityTimeFormat =
    DateTimeFormatter.ofPattern("d MMM, h:mm a", Locale.ENGLISH)

/**
 * Local label for a Pulse recent-activity row. The stored timestamp stays ISO.
 * Unparseable values are omitted so a raw ISO string is never the label.
 */
fun formatBusinessPulseOccurredAt(
    raw: String,
    zone: ZoneId = ZoneId.of("Asia/Kolkata"),
): String {
    val instant = runCatching { Instant.parse(raw.trim()) }.getOrNull() ?: return ""
    return pulseActivityTimeFormat.format(instant.atZone(zone))
}

data class PulseAttentionItem(
    val id: String,
    val title: String,
)

data class BusinessMomentCardModel(
    val id: String,
    val title: String,
    val occurredAt: String,
    val amountLabel: String?,
    val activityCode: String,
    val sourceActivityIds: List<String>,
    /** Profile name of the member who recorded the row. Null when the profile has none. */
    val actorDisplayName: String? = null,
    /** Supporting payload text, such as an expense category. Null when the payload has none. */
    val subtitle: String? = null,
    /** Payload status or issue severity. Null when the payload has none. */
    val statusLabel: String? = null,
)

data class BusinessPulsePresentation(
    val familyTitle: String,
    val facts: List<PulseFact>,
    val today: List<BusinessQuickAddKind>,
    val attention: List<PulseAttentionItem>,
    /** Set only when the attention source loaded and was empty. */
    val attentionEmptyLine: String?,
    val recent: List<BusinessMomentCardModel>,
) {
    /** Hero and snapshot both use this list. Unavailable facts are omitted. */
    fun displayedFacts(): List<PulseFact> =
        facts.filter { it.metric !is PulseMetric.Unavailable }.take(3)

    fun showsCompanyTotals(): Boolean = displayedFacts().any { it.companyWide }

    companion object {
        const val RECENT_TITLE = "Recent activity"
        const val COMPANY_TOTALS = "Company totals"
        const val ATTENTION_EMPTY = "No open issues right now."
    }
}

data class BusinessPulseFactsInput(
    val family: BusinessMomentFamilyConfig.Family,
    val smallShop: Boolean,
    val capabilities: List<String>?,
    val momentTypeCode: String?,
    val finance: BusinessFinancePayloadDto? = null,
    val runwayMonths: String? = null,
    val availableCash: String? = null,
    /** True when Money Pulse's own getLife call failed. Cash and runway are then omitted. */
    val lifeFailed: Boolean = false,
    val operations: OpsPulseExtrasDto? = null,
    /** Null when roster was not loaded. */
    val rosterCount: Int? = null,
    /** Null when the approvals list was not loaded. */
    val approvals: List<BusinessApprovalItemDto>? = null,
    /** Null when the issue list was not loaded. */
    val issues: List<BusinessIssueItemDto>? = null,
    val activities: List<ActivityItemDto> = emptyList(),
)

fun buildBusinessPulsePresentation(input: BusinessPulseFactsInput): BusinessPulsePresentation {
    val spec = BusinessMomentFamilyConfig.forFamily(input.family, input.smallShop)
    val today = BusinessMomentFamilyConfig.visibleActions(
        spec.primary,
        input.capabilities,
        input.momentTypeCode,
    )
    val facts = when (input.family) {
        BusinessMomentFamilyConfig.Family.MONEY -> moneyFacts(input)
        BusinessMomentFamilyConfig.Family.DAILY -> dailyFacts(input)
        BusinessMomentFamilyConfig.Family.TEAM -> teamFacts(input)
    }
    val attention = attentionItems(input)
    val emptyLine = if (attention.isEmpty() && attentionSourceLoaded(input)) {
        BusinessPulsePresentation.ATTENTION_EMPTY
    } else {
        null
    }
    return BusinessPulsePresentation(
        familyTitle = spec.title,
        facts = facts,
        today = today,
        attention = attention.take(3),
        attentionEmptyLine = emptyLine,
        recent = input.activities.take(3).map { it.toCard() },
    )
}

private fun moneyFacts(input: BusinessPulseFactsInput): List<PulseFact> {
    val cashAndRunwayKnown = !input.lifeFailed
    return listOf(
        PulseFact("Revenue", financeAmount(input.finance) { it.revenueTotal }, companyWide = true),
        PulseFact("Outflow", financeAmount(input.finance) { it.expenseTotal }, companyWide = true),
        PulseFact(
            "Cash",
            if (!cashAndRunwayKnown) PulseMetric.Unavailable else textMetric(input.availableCash),
            companyWide = true,
        ),
        PulseFact(
            "Runway",
            if (!cashAndRunwayKnown) PulseMetric.Unavailable else textMetric(input.runwayMonths),
            companyWide = true,
        ),
    )
}

private fun dailyFacts(input: BusinessPulseFactsInput): List<PulseFact> {
    val ops = input.operations
    val quality = ops?.sectionQuality.orEmpty()
    return listOf(
        PulseFact(
            "Open issues",
            countedMetric(quality["needsAttention"], ops?.openIssueCount),
            companyWide = false,
        ),
        PulseFact(
            "Vendors",
            countedMetric(quality["activeVendors"], ops?.activeVendorCount),
            companyWide = false,
        ),
        PulseFact(
            "Spend",
            textMetric(ops?.monthlySpend, real = quality["monthlySpend"] == "REAL_DATA"),
            companyWide = false,
        ),
        PulseFact(
            "SLA",
            countedMetric(quality["slaCompliance"], ops?.slaCompliancePct),
            companyWide = false,
        ),
    )
}

private fun teamFacts(input: BusinessPulseFactsInput): List<PulseFact> {
    return listOf(
        PulseFact("People", countMetric(input.rosterCount), companyWide = false),
        PulseFact("Approvals", countMetric(input.approvals?.size), companyWide = false),
        PulseFact("Your updates", countMetric(input.activities.size), companyWide = false),
    )
}

private fun attentionItems(input: BusinessPulseFactsInput): List<PulseAttentionItem> = when (input.family) {
    BusinessMomentFamilyConfig.Family.MONEY -> {
        val rows = mutableListOf<PulseAttentionItem>()
        input.approvals.orEmpty().forEach { item ->
            rows += PulseAttentionItem(item.approvalRequestId, item.title?.ifBlank { null } ?: "Approval waiting")
        }
        val outstanding = financeAmount(input.finance) { it.invoiceOutstandingTotal }
        if (outstanding is PulseMetric.Available) {
            rows += PulseAttentionItem("invoice-outstanding", "Invoices outstanding ${outstanding.value}")
        }
        rows
    }
    BusinessMomentFamilyConfig.Family.DAILY -> {
        val quality = input.operations?.sectionQuality?.get("needsAttention")
        if (quality == "REAL_DATA") {
            input.operations.needsAttention.map { PulseAttentionItem(it.issueId ?: it.title, it.title) }
        } else {
            openIssues(input.issues)
        }
    }
    BusinessMomentFamilyConfig.Family.TEAM -> {
        val rows = mutableListOf<PulseAttentionItem>()
        input.approvals.orEmpty().forEach { item ->
            rows += PulseAttentionItem(item.approvalRequestId, item.title?.ifBlank { null } ?: "Approval waiting")
        }
        rows += openIssues(input.issues)
        rows
    }
}

private fun attentionSourceLoaded(input: BusinessPulseFactsInput): Boolean = when (input.family) {
    BusinessMomentFamilyConfig.Family.MONEY -> input.approvals != null || financeIsReal(input.finance)
    BusinessMomentFamilyConfig.Family.DAILY ->
        input.operations?.sectionQuality?.get("needsAttention") == "REAL_DATA" || input.issues != null
    BusinessMomentFamilyConfig.Family.TEAM -> input.approvals != null || input.issues != null
}

private fun openIssues(issues: List<BusinessIssueItemDto>?): List<PulseAttentionItem> {
    if (issues == null) return emptyList()
    return issues.filter { item ->
        val status = item.status?.uppercase()
        status == null || status == "OPEN" || status == "IN_PROGRESS" || status == "BLOCKED"
    }.map { PulseAttentionItem(it.issueId, it.title) }
}

private fun financeIsReal(finance: BusinessFinancePayloadDto?): Boolean {
    val quality = finance?.dataQuality?.trim().orEmpty()
    return quality.isNotEmpty() && quality != "EMPTY" && quality != "EMPTY_SUPPORTED"
}

private fun financeAmount(
    finance: BusinessFinancePayloadDto?,
    select: (com.example.momentra.data.api.BusinessFinanceTotalDto) -> String,
): PulseMetric {
    if (!financeIsReal(finance)) return PulseMetric.Unavailable
    return textMetric(finance?.totals?.firstOrNull()?.let(select), real = true)
}

/** Null count means the list was not loaded. A loaded 0 is [PulseMetric.Zero]. */
private fun countMetric(count: Int?): PulseMetric = when (count) {
    null -> PulseMetric.Unavailable
    0 -> PulseMetric.Zero
    else -> PulseMetric.Available(count.toString())
}

private fun countedMetric(quality: String?, value: Int?): PulseMetric {
    if (quality != "REAL_DATA" || value == null) return PulseMetric.Unavailable
    return if (value == 0) PulseMetric.Zero else PulseMetric.Available(value.toString())
}

private fun textMetric(raw: String?, real: Boolean = true): PulseMetric {
    if (!real) return PulseMetric.Unavailable
    val text = raw?.trim()?.takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }
        ?: return PulseMetric.Unavailable
    val number = text.replace(",", "").replace("₹", "").trim().toDoubleOrNull()
    return when {
        number == null -> PulseMetric.Available(text)
        number == 0.0 -> PulseMetric.Zero
        else -> PulseMetric.Available(text)
    }
}

private fun ActivityItemDto.toCard(): BusinessMomentCardModel {
    val payloadId = activityPayload?.activityId ?: activityPayload?.expenseId
    val id = payloadId ?: "$activityCode-$occurredAt-$title"
    val amount = activityPayload?.amount?.trim()?.takeIf { it.isNotEmpty() }
    val currency = activityPayload?.currencyCode?.trim()?.takeIf { it.isNotEmpty() }
    return BusinessMomentCardModel(
        id = id,
        title = title,
        occurredAt = occurredAt,
        amountLabel = when {
            amount == null -> null
            currency == null -> amount
            else -> "$currency $amount"
        },
        activityCode = activityCode,
        sourceActivityIds = listOfNotNull(payloadId),
        actorDisplayName = actorDisplayName?.trim()?.takeIf { it.isNotEmpty() },
    )
}
