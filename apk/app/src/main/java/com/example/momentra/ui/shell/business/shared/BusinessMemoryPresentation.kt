package com.example.momentra.ui.shell.business.shared

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class BusinessMemoryItemFact(
    val title: String,
    val body: String? = null,
    val occurredAt: String? = null,
    val memoryType: String? = null,
    val businessFamily: String? = null,
)

data class BusinessMemoryWorthItem(
    val title: String,
    val familyLabel: String,
    val dateLabel: String?,
    val businessFamily: String?,
)

data class BusinessMemoryPattern(
    val title: String,
    val evidence: List<String>,
)

data class BusinessMemoryComparison(
    val thenTitle: String,
    val thenDate: String,
    val nowTitle: String,
    val nowDate: String,
)

data class BusinessMemoryPresentation(
    val heroPeriod: String,
    val heroSentence: String,
    val heroCount: Int,
    val worthRemembering: List<BusinessMemoryWorthItem>,
    val pattern: BusinessMemoryPattern?,
    val worked: List<String>,
    val didnt: List<String>,
    val thenNow: BusinessMemoryComparison?,
)

const val BUSINESS_MEMORY_WORTH_EMPTY = "Nothing saved yet."
const val BUSINESS_MEMORY_WORKED_EMPTY = "No success memories yet."
const val BUSINESS_MEMORY_DIDNT_EMPTY = "No risk memories yet."

private val memoryDateFormat = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

fun businessMemoryIsRisk(title: String?, body: String?): Boolean {
    val hay = "${title.orEmpty()} ${body.orEmpty()}".lowercase()
    return hay.contains("risk") || hay.contains("issue") || hay.contains("incident")
}

fun businessMemoryFamilyLabel(family: String?): String {
    val code = family.orEmpty().uppercase()
    return when {
        code.contains("TEAM") -> "Team"
        code.contains("RUNWAY") -> "Money"
        code.contains("OPERATIONS") -> "Daily"
        else -> ""
    }
}

fun buildBusinessMemoryPresentation(
    items: List<BusinessMemoryItemFact>,
    @Suppress("UNUSED_PARAMETER") now: Instant,
    zone: ZoneId,
): BusinessMemoryPresentation {
    val dated = items.mapNotNull { item -> datedItem(item, zone) }
    val dates = dated.map { it.localDate }.distinct()
    val period = when {
        dates.isEmpty() -> "Company memory"
        dates.size == 1 -> formatMemoryDate(dates.first())
        else -> "${formatMemoryDate(dates.min())} – ${formatMemoryDate(dates.max())}"
    }
    val count = items.size
    val risk = items.filter { businessMemoryIsRisk(it.title, it.body) }
    val worked = items.filter { !businessMemoryIsRisk(it.title, it.body) }
    return BusinessMemoryPresentation(
        heroPeriod = period,
        heroSentence = if (count == 0) BUSINESS_MEMORY_WORTH_EMPTY else "$count saved memories.",
        heroCount = count,
        worthRemembering = items.map { worthItem(it, zone) },
        pattern = memoryPattern(items, risk),
        worked = worked.map { displayTitle(it.title) },
        didnt = risk.map { displayTitle(it.title) },
        thenNow = memoryComparison(dated),
    )
}

fun filterWorthRemembering(
    items: List<BusinessMemoryWorthItem>,
    lens: BusinessLifeLens,
): List<BusinessMemoryWorthItem> {
    return when (lens) {
        BusinessLifeLens.OVERVIEW -> items
        BusinessLifeLens.MONEY -> items.filter { it.businessFamily.orEmpty().uppercase().contains("RUNWAY") }
        BusinessLifeLens.DAILY -> items.filter {
            val code = it.businessFamily.orEmpty().uppercase()
            code.contains("OPERATIONS") && !code.contains("TEAM")
        }
        BusinessLifeLens.TEAM -> items.filter { it.businessFamily.orEmpty().uppercase().contains("TEAM") }
    }
}

private data class DatedMemory(
    val item: BusinessMemoryItemFact,
    val instant: Instant,
    val localDate: LocalDate,
)

private fun datedItem(item: BusinessMemoryItemFact, zone: ZoneId): DatedMemory? {
    val raw = item.occurredAt?.trim().orEmpty()
    if (raw.isEmpty()) return null
    val instant = runCatching { Instant.parse(raw) }.getOrNull() ?: return null
    return DatedMemory(item, instant, instant.atZone(zone).toLocalDate())
}

private fun memoryComparison(dated: List<DatedMemory>): BusinessMemoryComparison? {
    val dates = dated.map { it.localDate }.distinct()
    if (dates.size < 2) return null
    val earliest = dated.minWith(compareBy<DatedMemory> { it.localDate }.thenBy { it.instant })
    val latest = dated.maxWith(compareBy<DatedMemory> { it.localDate }.thenBy { it.instant })
    return BusinessMemoryComparison(
        thenTitle = displayTitle(earliest.item.title),
        thenDate = formatMemoryDate(earliest.localDate),
        nowTitle = displayTitle(latest.item.title),
        nowDate = formatMemoryDate(latest.localDate),
    )
}

private fun memoryPattern(
    items: List<BusinessMemoryItemFact>,
    risk: List<BusinessMemoryItemFact>,
): BusinessMemoryPattern? {
    val repeatedType = items
        .filter { typeKey(it.memoryType) != null }
        .groupBy { typeKey(it.memoryType) }
        .maxByOrNull { it.value.size }
        ?.takeIf { it.value.size >= 2 }
    val riskQualifies = risk.size >= 2
    val typeQualifies = repeatedType != null
    return when {
        riskQualifies && typeQualifies -> {
            val typeItems = repeatedType!!.value
            if (typeItems.size > risk.size) typePattern(repeatedType.key!!, typeItems) else riskPattern(risk)
        }
        riskQualifies -> riskPattern(risk)
        typeQualifies -> typePattern(repeatedType!!.key!!, repeatedType.value)
        else -> null
    }
}

private fun riskPattern(items: List<BusinessMemoryItemFact>): BusinessMemoryPattern {
    val count = items.size
    return BusinessMemoryPattern(
        title = "$count memories mention risk",
        evidence = items.map { displayTitle(it.title) },
    )
}

private fun typePattern(type: String, items: List<BusinessMemoryItemFact>): BusinessMemoryPattern {
    val label = type.lowercase().replaceFirstChar { it.titlecase(Locale.ENGLISH) }
    return BusinessMemoryPattern(
        title = "${items.size} $label memories",
        evidence = items.map { displayTitle(it.title) },
    )
}

private fun typeKey(memoryType: String?): String? {
    val key = memoryType?.trim().orEmpty().uppercase()
    if (key.isEmpty() || key == "GENERAL") return null
    return key
}

private fun worthItem(item: BusinessMemoryItemFact, zone: ZoneId): BusinessMemoryWorthItem {
    val dated = datedItem(item, zone)
    return BusinessMemoryWorthItem(
        title = displayTitle(item.title),
        familyLabel = businessMemoryFamilyLabel(item.businessFamily),
        dateLabel = dated?.let { formatMemoryDate(it.localDate) },
        businessFamily = item.businessFamily,
    )
}

private fun displayTitle(title: String): String = title.trim().ifEmpty { "Memory" }

private fun formatMemoryDate(date: LocalDate): String = date.format(memoryDateFormat)
