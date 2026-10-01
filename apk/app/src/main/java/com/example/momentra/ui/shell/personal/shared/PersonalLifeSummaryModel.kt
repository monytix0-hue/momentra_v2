package com.example.momentra.ui.shell.personal.shared

import com.example.momentra.data.api.LifeDriftDto
import com.example.momentra.data.api.LifeLeverageDto
import com.example.momentra.data.api.PersonalLifeByFamilyDto
import com.example.momentra.data.api.PersonalLifeDto
import com.example.momentra.data.api.PersonalLifeThisWeekDto
import kotlin.math.floor
import kotlin.math.roundToInt

/** Locked M3 family-state vocabulary — same labels on iOS and Android. */
enum class PersonalLifeFamilyStatus(val label: String) {
    STRONG("Strong"),
    GROWING("Growing"),
    STEADY("Steady"),
    QUIET("Quiet"),
    NEEDS_ATTENTION("Needs attention"),
}

enum class PersonalLifeWeekTier {
    RICH, PARTIAL, THIN, EMPTY,
}

enum class PersonalLifeAllocationMode {
    ACTIVITY, MONEY,
}

data class PersonalLifeFamilyState(
    val familyCode: String,
    val label: String,
    val status: PersonalLifeFamilyStatus,
)

data class PersonalLifeWeekFamilyCount(
    val familyCode: String,
    val label: String,
    val periodLogs: Int,
)

data class PersonalLifeWeekSummary(
    val tier: PersonalLifeWeekTier,
    /** Present for rich / partial tiers only. */
    val sentence: String?,
    val familyCounts: List<PersonalLifeWeekFamilyCount>,
    val filterLabel: String?,
)

data class PersonalLifeAllocationSlice(
    val familyCode: String,
    val label: String,
    val value: Double,
    /** Whole-number percent; only meaningful when allocation hasData. */
    val percent: Int,
)

data class PersonalLifeAllocation(
    val hasData: Boolean,
    val slices: List<PersonalLifeAllocationSlice>,
)

data class PersonalLifeInsight(
    val headline: String,
    val body: String,
    val ctaLabel: String?,
    /** LOG_RECOVERY | LOG_SPEND | OPEN_ADD | null */
    val ctaAction: String?,
)

data class PersonalLifeMoneySnapshot(
    val incomeTotal: Double,
    val expenseTotal: Double,
    val available: Double?,
    val currencyCode: String?,
    val byFamilySpend: List<PersonalLifeAllocationSlice>,
)

/**
 * Dumb-view contract for Life — all interpretation happens in [from].
 */
data class PersonalLifeSummaryModel(
    val familyStates: List<PersonalLifeFamilyState>,
    val weekSummary: PersonalLifeWeekSummary,
    val activityAllocation: PersonalLifeAllocation,
    val moneyAllocation: PersonalLifeAllocation,
    val slipping: PersonalLifeInsight?,
    val working: PersonalLifeInsight?,
    val moneySnapshot: PersonalLifeMoneySnapshot?,
    /** Secondary only; never anchors the screen. */
    val globalScore: Int?,
    val scoreMax: Int,
) {
    companion object {
        val CANONICAL_FAMILIES: List<Pair<String, String>> = listOf(
            "LIFE_OPERATIONS" to "Everyday",
            "FUTURE_BUILDING" to "Future",
            "LIFESTYLE" to "Lifestyle",
            "RELATIONSHIPS" to "People",
        )

        fun from(life: PersonalLifeDto, familyFilter: String? = null): PersonalLifeSummaryModel {
            val sq = life.sectionQuality
            val week = life.thisWeek
            val byFamily = week?.byFamily.orEmpty()

            val familyStates = CANONICAL_FAMILIES.map { (code, label) ->
                val row = byFamily.firstOrNull { it.familyCode.equals(code, ignoreCase = true) }
                val score = life.areaScores.firstOrNull { it.code.equals(code, ignoreCase = true) }?.score
                val logs = row?.periodLogs ?: 0
                val expense = parseAmount(row?.expenseTotal)
                PersonalLifeFamilyState(
                    familyCode = code,
                    label = label,
                    status = deriveFamilyStatus(logs, expense, score),
                )
            }

            return PersonalLifeSummaryModel(
                familyStates = familyStates,
                weekSummary = deriveWeekSummary(week, familyFilter),
                activityAllocation = deriveActivityAllocation(byFamily),
                moneyAllocation = deriveMoneyAllocation(byFamily),
                slipping = deriveSlipping(life.drift, sq),
                working = deriveWorking(life.leverage, sq),
                moneySnapshot = deriveMoneySnapshot(week),
                globalScore = life.score,
                scoreMax = life.scoreMax,
            )
        }

        /**
         * Deterministic rules shared with iOS.
         * Weak / insufficient → Quiet or Steady only (never Strong / Growing).
         */
        fun deriveFamilyStatus(logs: Int, expense: Double, areaScore: Int?): PersonalLifeFamilyStatus {
            val hasRichSignal = logs >= 2 || (logs >= 1 && expense > 0) || areaScore != null
            if (!hasRichSignal) {
                return if (logs == 0 && expense <= 0.0) {
                    PersonalLifeFamilyStatus.QUIET
                } else {
                    PersonalLifeFamilyStatus.STEADY
                }
            }
            if (areaScore != null) {
                return when {
                    areaScore < 40 -> PersonalLifeFamilyStatus.NEEDS_ATTENTION
                    areaScore >= 75 && logs >= 2 -> PersonalLifeFamilyStatus.STRONG
                    areaScore >= 55 || logs >= 4 -> PersonalLifeFamilyStatus.GROWING
                    else -> PersonalLifeFamilyStatus.STEADY
                }
            }
            return when {
                logs >= 5 -> PersonalLifeFamilyStatus.STRONG
                logs >= 3 -> PersonalLifeFamilyStatus.GROWING
                else -> PersonalLifeFamilyStatus.STEADY
            }
        }

        private fun deriveWeekSummary(
            week: PersonalLifeThisWeekDto?,
            familyFilter: String?,
        ): PersonalLifeWeekSummary {
            val filterLabel = when (familyFilter?.uppercase()) {
                "LIFE_OPERATIONS" -> "Everyday"
                "FUTURE_BUILDING" -> "Future"
                "LIFESTYLE" -> "Lifestyle"
                "RELATIONSHIPS" -> "People"
                else -> null
            }

            val familyCounts: List<PersonalLifeWeekFamilyCount> = if (familyFilter != null) {
                val row = week?.byFamily.orEmpty()
                    .firstOrNull { it.familyCode.equals(familyFilter, ignoreCase = true) }
                val meta = CANONICAL_FAMILIES.firstOrNull { it.first.equals(familyFilter, ignoreCase = true) }
                listOf(
                    PersonalLifeWeekFamilyCount(
                        familyCode = familyFilter,
                        label = meta?.second ?: row?.label ?: familyFilter,
                        periodLogs = row?.periodLogs ?: 0,
                    ),
                )
            } else {
                CANONICAL_FAMILIES.map { (code, label) ->
                    val row = week?.byFamily.orEmpty()
                        .firstOrNull { it.familyCode.equals(code, ignoreCase = true) }
                    PersonalLifeWeekFamilyCount(
                        familyCode = code,
                        label = label,
                        periodLogs = row?.periodLogs ?: 0,
                    )
                }
            }

            val totalLogs = if (familyFilter != null) {
                familyCounts.firstOrNull()?.periodLogs ?: 0
            } else {
                week?.periodLogs ?: familyCounts.sumOf { it.periodLogs }
            }

            val highlights = week?.highlights.orEmpty().filter { h ->
                familyFilter == null || h.familyCode.equals(familyFilter, ignoreCase = true)
            }
            val highlightTitles = highlights.map { it.title.trim() }.filter { it.isNotEmpty() }
            val familiesWithLogs = familyCounts.count { it.periodLogs > 0 }

            if (totalLogs <= 0) {
                return PersonalLifeWeekSummary(
                    tier = PersonalLifeWeekTier.EMPTY,
                    sentence = null,
                    familyCounts = familyCounts,
                    filterLabel = filterLabel,
                )
            }

            if (totalLogs <= 2 && highlightTitles.isEmpty()) {
                return PersonalLifeWeekSummary(
                    tier = PersonalLifeWeekTier.THIN,
                    sentence = null,
                    familyCounts = familyCounts.filter { it.periodLogs > 0 },
                    filterLabel = filterLabel,
                )
            }

            if (highlightTitles.isEmpty() || familiesWithLogs < 2) {
                val sentence = if (totalLogs == 1) {
                    "1 activity logged this week"
                } else {
                    "$totalLogs activities logged this week"
                }
                return PersonalLifeWeekSummary(
                    tier = PersonalLifeWeekTier.PARTIAL,
                    sentence = sentence,
                    familyCounts = familyCounts.filter { it.periodLogs > 0 },
                    filterLabel = filterLabel,
                )
            }

            val first = highlightTitles.first()
            val sentence = if (familiesWithLogs > 1) {
                "$first — activity across $familiesWithLogs areas"
            } else {
                first
            }
            return PersonalLifeWeekSummary(
                tier = PersonalLifeWeekTier.RICH,
                sentence = sentence,
                familyCounts = familyCounts.filter { it.periodLogs > 0 },
                filterLabel = filterLabel,
            )
        }

        private fun deriveActivityAllocation(
            byFamily: List<PersonalLifeByFamilyDto>,
        ): PersonalLifeAllocation {
            val values = CANONICAL_FAMILIES.map { (code, label) ->
                val row = byFamily.firstOrNull { it.familyCode.equals(code, ignoreCase = true) }
                Triple(code, label, (row?.periodLogs ?: 0).toDouble())
            }
            val total = values.sumOf { it.third }
            if (total <= 0.0) return PersonalLifeAllocation(hasData = false, slices = emptyList())
            val slices = values.mapNotNull { (code, label, value) ->
                if (value <= 0.0) return@mapNotNull null
                PersonalLifeAllocationSlice(
                    familyCode = code,
                    label = label,
                    value = value,
                    percent = (value / total * 100.0).roundToInt(),
                )
            }
            return PersonalLifeAllocation(hasData = true, slices = rebalancePercents(slices))
        }

        private fun deriveMoneyAllocation(
            byFamily: List<PersonalLifeByFamilyDto>,
        ): PersonalLifeAllocation {
            val values = CANONICAL_FAMILIES.map { (code, label) ->
                val row = byFamily.firstOrNull { it.familyCode.equals(code, ignoreCase = true) }
                Triple(code, label, parseAmount(row?.expenseTotal))
            }
            val total = values.sumOf { it.third }
            if (total <= 0.0) return PersonalLifeAllocation(hasData = false, slices = emptyList())
            val slices = values.mapNotNull { (code, label, value) ->
                if (value <= 0.0) return@mapNotNull null
                PersonalLifeAllocationSlice(
                    familyCode = code,
                    label = label,
                    value = value,
                    percent = (value / total * 100.0).roundToInt(),
                )
            }
            return PersonalLifeAllocation(hasData = true, slices = rebalancePercents(slices))
        }

        private fun rebalancePercents(
            slices: List<PersonalLifeAllocationSlice>,
        ): List<PersonalLifeAllocationSlice> {
            if (slices.isEmpty()) return slices
            val sum = slices.sumOf { it.percent }
            val delta = 100 - sum
            if (delta == 0) return slices
            val last = slices.last()
            return slices.dropLast(1) + last.copy(percent = maxOf(last.percent + delta, 0))
        }

        private fun deriveSlipping(
            drift: LifeDriftDto?,
            sectionQuality: Map<String, String>,
        ): PersonalLifeInsight? {
            if (drift == null) return null
            if (sectionQuality["drift"].equals("EMPTY_SUPPORTED", ignoreCase = true)) return null
            val headline = drift.headline.trim()
            val body = drift.body.trim()
            if (headline.isEmpty() || headline.contains("not available", ignoreCase = true)) return null
            if (body.isEmpty()) return null
            val cta = drift.ctaLabel.trim()
            // No stable ctaAction on drift yet — never show a dead CTA button.
            return PersonalLifeInsight(
                headline = headline,
                body = body,
                ctaLabel = null,
                ctaAction = null,
            )
        }

        private fun deriveWorking(
            leverage: LifeLeverageDto?,
            sectionQuality: Map<String, String>,
        ): PersonalLifeInsight? {
            if (leverage == null) return null
            if (sectionQuality["leverage"].equals("EMPTY_SUPPORTED", ignoreCase = true)) return null
            val action = leverage.ctaAction.uppercase()
            if (action == "NONE") return null
            val headline = leverage.actionTitle.trim()
            val body = leverage.actionBody.trim()
            if (headline.isEmpty() || headline.contains("not available", ignoreCase = true)) return null
            if (body.isEmpty()) return null
            val cta = leverage.ctaLabel.trim()
            val ctaLabel = if (cta.isEmpty() || cta.contains("coming soon", ignoreCase = true)) null else cta
            return PersonalLifeInsight(
                headline = headline,
                body = body,
                ctaLabel = ctaLabel,
                ctaAction = action,
            )
        }

        private fun deriveMoneySnapshot(week: PersonalLifeThisWeekDto?): PersonalLifeMoneySnapshot? {
            if (week == null) return null
            val income = parseAmount(week.incomeTotal)
            val expense = parseAmount(week.expenseTotal)
            if (income <= 0.0 && expense <= 0.0) return null
            val currency = week.currencyCode
                ?: week.spendByCurrency.maxByOrNull { parseAmount(it.value) }?.key
            val byFamilySpend = deriveMoneyAllocation(week.byFamily).slices
            val available = if (income > 0.0) income - expense else null
            return PersonalLifeMoneySnapshot(
                incomeTotal = income,
                expenseTotal = expense,
                available = available,
                currencyCode = currency,
                byFamilySpend = byFamilySpend,
            )
        }

        fun parseAmount(raw: String?): Double {
            if (raw.isNullOrBlank()) return 0.0
            return raw.toDoubleOrNull() ?: 0.0
        }

        fun formatMoney(amount: Double, currencyCode: String?): String {
            val symbol = if (currencyCode == null || currencyCode == "INR") "₹" else "$currencyCode "
            return if (amount == floor(amount)) {
                "$symbol${amount.toLong()}"
            } else {
                String.format("%s%.2f", symbol, amount)
            }
        }
    }
}
