package com.example.momentra.ui.shell.business.shared

import com.example.momentra.data.api.ActivityItemDto
import com.example.momentra.data.api.ActivityPayloadDto
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Filters that correspond to activity codes the feed actually emits. */
enum class BusinessMomentFilter {
    ALL,
    REVENUE,
    EXPENSE,
    INVOICE,
    SPEND,
    ISSUE,
    UPDATE,
    DECISION,
    APPROVAL,
}

data class BusinessMomentChip(
    val filter: BusinessMomentFilter,
    val label: String,
)

enum class BusinessMomentsContent {
    LOADING,
    ERROR,
    FEED_EMPTY,
    FILTER_EMPTY,
    TIMELINE,
}

data class BusinessMomentsEntry(
    val card: BusinessMomentCardModel,
    val eyebrow: String?,
    val timeLabel: String,
    /** "by {name}" when a profile name exists. */
    val actorLine: String?,
)

data class BusinessMomentsGroup(
    val label: String,
    val entries: List<BusinessMomentsEntry>,
)

data class BusinessMomentsPresentation(
    val familyTitle: String,
    val contextLine: String,
    val chips: List<BusinessMomentChip>,
    val selected: BusinessMomentFilter,
    val groups: List<BusinessMomentsGroup>,
    val content: BusinessMomentsContent,
    val emptyTitle: String?,
    val emptyBody: String?,
    val errorMessage: String?,
) {
    companion object {
        const val CONTEXT = "Company activity"
        const val FEED_EMPTY_TITLE = "Nothing recorded yet"
        const val ADD_LABEL = "Add something"
    }
}

/**
 * In-memory Moments feed for one selected moment.
 * A response applies only when both the moment id and the generation still match.
 */
data class BusinessMomentsFeed(
    val momentId: String? = null,
    val generation: Long = 0,
    val items: List<ActivityItemDto> = emptyList(),
    val nextCursor: String? = null,
    val error: String? = null,
    val loading: Boolean = false,
) {
    fun begin(momentId: String?): BusinessMomentsFeed {
        val switched = this.momentId != momentId
        val kept = if (switched) emptyList() else items
        return copy(
            momentId = momentId,
            generation = generation + 1,
            items = kept,
            nextCursor = if (switched) null else nextCursor,
            error = if (momentId.isNullOrBlank()) "Select a Business Moment." else null,
            loading = !momentId.isNullOrBlank() && kept.isEmpty(),
        )
    }

    fun applyPage(
        momentId: String,
        generation: Long,
        pageItems: List<ActivityItemDto>,
        cursor: String?,
        append: Boolean,
    ): BusinessMomentsFeed {
        if (momentId != this.momentId || generation != this.generation) return this
        return copy(
            items = if (append) items + pageItems else pageItems,
            nextCursor = cursor,
            error = null,
            loading = false,
        )
    }

    fun applyFailure(momentId: String, generation: Long, message: String?): BusinessMomentsFeed {
        if (momentId != this.momentId || generation != this.generation) return this
        return copy(
            error = message?.takeIf { it.isNotBlank() } ?: "Could not load activity.",
            loading = false,
        )
    }
}

fun businessMomentChips(family: BusinessMomentFamilyConfig.Family): List<BusinessMomentChip> =
    when (family) {
        BusinessMomentFamilyConfig.Family.MONEY -> listOf(
            BusinessMomentChip(BusinessMomentFilter.ALL, "All"),
            BusinessMomentChip(BusinessMomentFilter.REVENUE, "Revenue"),
            BusinessMomentChip(BusinessMomentFilter.EXPENSE, "Expenses"),
            BusinessMomentChip(BusinessMomentFilter.INVOICE, "Invoices"),
        )
        BusinessMomentFamilyConfig.Family.DAILY -> listOf(
            BusinessMomentChip(BusinessMomentFilter.ALL, "All"),
            BusinessMomentChip(BusinessMomentFilter.SPEND, "Spend"),
            BusinessMomentChip(BusinessMomentFilter.ISSUE, "Issues"),
            BusinessMomentChip(BusinessMomentFilter.UPDATE, "Updates"),
            BusinessMomentChip(BusinessMomentFilter.APPROVAL, "Approvals"),
        )
        BusinessMomentFamilyConfig.Family.TEAM -> listOf(
            BusinessMomentChip(BusinessMomentFilter.ALL, "All"),
            BusinessMomentChip(BusinessMomentFilter.UPDATE, "Updates"),
            BusinessMomentChip(BusinessMomentFilter.DECISION, "Decisions"),
            BusinessMomentChip(BusinessMomentFilter.APPROVAL, "Approvals"),
        )
    }

fun buildBusinessMomentsPresentation(
    family: BusinessMomentFamilyConfig.Family,
    items: List<ActivityItemDto>,
    filter: BusinessMomentFilter,
    loading: Boolean,
    error: String?,
    now: Instant,
    zone: ZoneId,
): BusinessMomentsPresentation {
    val chips = businessMomentChips(family)
    val selected = if (chips.any { it.filter == filter }) filter else BusinessMomentFilter.ALL
    val filtered = items.filter { matchesBusinessMomentFilter(family, selected, it.activityCode) }
    val today = now.atZone(zone).toLocalDate()
    val groups = groupBusinessMoments(family, filtered.map { it.toMomentCard() }, today, zone)
    val failed = !error.isNullOrBlank()
    val content = when {
        loading && items.isEmpty() -> BusinessMomentsContent.LOADING
        failed && items.isEmpty() -> BusinessMomentsContent.ERROR
        items.isEmpty() -> BusinessMomentsContent.FEED_EMPTY
        filtered.isEmpty() -> BusinessMomentsContent.FILTER_EMPTY
        else -> BusinessMomentsContent.TIMELINE
    }
    val chipLabel = chips.first { it.filter == selected }.label
    return BusinessMomentsPresentation(
        familyTitle = BusinessMomentFamilyConfig.forFamily(family, smallShop = false).title,
        contextLine = BusinessMomentsPresentation.CONTEXT,
        chips = chips,
        selected = selected,
        groups = if (content == BusinessMomentsContent.TIMELINE) groups else emptyList(),
        content = content,
        emptyTitle = when (content) {
            BusinessMomentsContent.FEED_EMPTY -> BusinessMomentsPresentation.FEED_EMPTY_TITLE
            BusinessMomentsContent.FILTER_EMPTY -> filterEmptyTitle(chipLabel)
            else -> null
        },
        emptyBody = if (content == BusinessMomentsContent.FEED_EMPTY) feedEmptyBody(family) else null,
        errorMessage = if (failed) error else null,
    )
}

private fun feedEmptyBody(family: BusinessMomentFamilyConfig.Family): String = when (family) {
    BusinessMomentFamilyConfig.Family.MONEY ->
        "Revenue, expenses and other money activity will appear here."
    BusinessMomentFamilyConfig.Family.DAILY ->
        "Spend, vendors, issues and updates will appear here."
    BusinessMomentFamilyConfig.Family.TEAM ->
        "Team updates, decisions and approvals will appear here."
}

private fun filterEmptyTitle(chipLabel: String): String {
    val word = when (chipLabel) {
        "Expenses" -> "expenses"
        "Invoices" -> "invoices"
        "Approvals" -> "approvals"
        "Updates" -> "updates"
        "Decisions" -> "decisions"
        "Issues" -> "issues"
        else -> chipLabel.lowercase(Locale.ENGLISH)
    }
    return "No $word yet"
}

internal fun matchesBusinessMomentFilter(
    family: BusinessMomentFamilyConfig.Family,
    filter: BusinessMomentFilter,
    activityCode: String,
): Boolean {
    if (filter == BusinessMomentFilter.ALL) return true
    val code = activityCode.uppercase(Locale.ENGLISH)
    return when (filter) {
        BusinessMomentFilter.REVENUE -> code == "BUSINESS_REVENUE"
        BusinessMomentFilter.EXPENSE -> family == BusinessMomentFamilyConfig.Family.MONEY && code == "BUSINESS_EXPENSE"
        BusinessMomentFilter.SPEND -> family == BusinessMomentFamilyConfig.Family.DAILY && code == "BUSINESS_EXPENSE"
        BusinessMomentFilter.INVOICE -> code == "BUSINESS_INVOICE"
        BusinessMomentFilter.ISSUE -> code == "ISSUE_REPORTED"
        BusinessMomentFilter.UPDATE -> code == "BUSINESS_UPDATE" || code == "IMPROVEMENT_LOGGED" || code == "ACTIVITY_LOGGED"
        BusinessMomentFilter.DECISION -> code == "DECISION_RECORDED"
        BusinessMomentFilter.APPROVAL -> code == "APPROVAL_REQUESTED" || code == "APPROVAL_APPROVED" || code == "APPROVAL_REJECTED"
        BusinessMomentFilter.ALL -> true
    }
}

private fun ActivityItemDto.toMomentCard(): BusinessMomentCardModel {
    val payload = activityPayload
    val sourceIds = listOfNotNull(
        payload?.expenseId,
        payload?.revenueId,
        payload?.invoiceId,
        payload?.issueId,
        payload?.updateId,
        payload?.approvalRequestId,
        payload?.activityId,
    )
    val amountLabel = momentAmountLabel(activityCode, payload)
    val subtitle = if (activityCode.equals("BUSINESS_EXPENSE", ignoreCase = true)) {
        prettyToken(payload?.categoryCode)
    } else {
        null
    }
    val status = prettyToken(payload?.status) ?: prettyToken(payload?.severity)
    val actor = actorDisplayName?.trim()?.takeIf { it.isNotEmpty() }
    return BusinessMomentCardModel(
        id = sourceIds.firstOrNull() ?: "$activityCode-$occurredAt-$title",
        title = title.trim().takeIf { it.isNotEmpty() } ?: fallbackTitle(activityCode),
        occurredAt = occurredAt,
        amountLabel = amountLabel,
        activityCode = activityCode,
        sourceActivityIds = sourceIds,
        actorDisplayName = actor,
        subtitle = subtitle,
        statusLabel = status,
    )
}

private fun momentAmountLabel(activityCode: String, payload: ActivityPayloadDto?): String? {
    val code = activityCode.uppercase(Locale.ENGLISH)
    val raw = if (code == "BUSINESS_INVOICE") payload?.totalAmount else payload?.amount
    return formatMomentAmount(raw, payload?.currencyCode)
}

internal fun formatMomentAmount(raw: String?, currency: String?): String? {
    val text = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val number = text.toBigDecimalOrNull()
    val shown = if (number == null) text else formatGrouped(number)
    val code = currency?.trim()?.uppercase(Locale.ENGLISH)?.takeIf { it.isNotEmpty() }
    val prefix = when (code) {
        "INR" -> "₹"
        "USD" -> "$"
        null -> ""
        else -> "$code "
    }
    return prefix + shown
}

private fun formatGrouped(number: BigDecimal): String {
    val format = NumberFormat.getNumberInstance(Locale.ENGLISH)
    format.maximumFractionDigits = 2
    format.minimumFractionDigits = 0
    format.isGroupingUsed = true
    return format.format(number)
}

private fun prettyToken(raw: String?): String? {
    val text = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return text.split('_').joinToString(" ") { word ->
        word.lowercase(Locale.ENGLISH).replaceFirstChar { it.titlecase(Locale.ENGLISH) }
    }
}

private fun fallbackTitle(activityCode: String): String = when (activityCode.uppercase(Locale.ENGLISH)) {
    "BUSINESS_REVENUE" -> "Revenue recorded"
    "BUSINESS_EXPENSE" -> "Expense"
    "BUSINESS_INVOICE" -> "Invoice created"
    "ISSUE_REPORTED" -> "Issue reported"
    "BUSINESS_UPDATE", "ACTIVITY_LOGGED" -> "Update"
    "IMPROVEMENT_LOGGED" -> "Improvement logged"
    "DECISION_RECORDED" -> "Decision recorded"
    "APPROVAL_REQUESTED" -> "Approval requested"
    "APPROVAL_APPROVED" -> "Approval completed"
    "APPROVAL_REJECTED" -> "Approval rejected"
    else -> "Activity"
}

internal fun momentEyebrow(family: BusinessMomentFamilyConfig.Family, activityCode: String): String? =
    when (activityCode.uppercase(Locale.ENGLISH)) {
        "BUSINESS_REVENUE" -> "Revenue"
        "BUSINESS_EXPENSE" -> if (family == BusinessMomentFamilyConfig.Family.DAILY) "Spend" else "Expense"
        "BUSINESS_INVOICE" -> "Invoice"
        "ISSUE_REPORTED" -> "Issue"
        "BUSINESS_UPDATE", "IMPROVEMENT_LOGGED", "ACTIVITY_LOGGED" -> "Update"
        "DECISION_RECORDED" -> "Decision"
        "APPROVAL_REQUESTED", "APPROVAL_APPROVED", "APPROVAL_REJECTED" -> "Approval"
        else -> null
    }

private fun groupBusinessMoments(
    family: BusinessMomentFamilyConfig.Family,
    cards: List<BusinessMomentCardModel>,
    today: LocalDate,
    zone: ZoneId,
): List<BusinessMomentsGroup> {
    val timeFormat = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
    val dayFormat = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
    val groups = mutableListOf<BusinessMomentsGroup>()
    var currentLabel: String? = null
    var current = mutableListOf<BusinessMomentsEntry>()
    fun flush() {
        val label = currentLabel ?: return
        if (current.isNotEmpty()) groups += BusinessMomentsGroup(label, current.toList())
        current = mutableListOf()
    }
    for (card in cards) {
        val zoned = runCatching { Instant.parse(card.occurredAt).atZone(zone) }.getOrNull()
        val label = when {
            zoned == null -> "Earlier"
            zoned.toLocalDate() == today -> "TODAY"
            zoned.toLocalDate() == today.minusDays(1) -> "YESTERDAY"
            else -> dayFormat.format(zoned).uppercase(Locale.ENGLISH)
        }
        if (label != currentLabel) {
            flush()
            currentLabel = label
        }
        current += BusinessMomentsEntry(
            card = card,
            eyebrow = momentEyebrow(family, card.activityCode),
            timeLabel = zoned?.let { timeFormat.format(it) } ?: card.occurredAt,
            actorLine = card.actorDisplayName?.let { "by $it" },
        )
    }
    flush()
    return groups
}
