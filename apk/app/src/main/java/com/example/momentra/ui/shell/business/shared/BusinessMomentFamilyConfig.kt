package com.example.momentra.ui.shell.business.shared

import com.example.momentra.domain.MomentSummary

/**
 * Presentation config for the three Business moment families.
 * Labels, icons, and capability rules stay on [BusinessQuickAddKind] and [BusinessActionRegistry].
 */
object BusinessMomentFamilyConfig {

    enum class Family {
        MONEY,
        DAILY,
        TEAM,
    }

    data class Spec(
        val family: Family,
        val title: String,
        val primary: List<BusinessQuickAddKind>,
        val secondary: List<BusinessQuickAddKind>,
    )

    fun forTypeCode(momentTypeCode: String?, smallShop: Boolean): Spec {
        val family = familyOrNull(momentTypeCode) ?: Family.TEAM
        return forFamily(family, smallShop)
    }

    /**
     * Selected moment only. A blank type on that moment is not replaced by another moment's code.
     * A missing moment row may use the shell's selected type (optimistic create).
     */
    fun selectedQuickAddTypeCode(
        selectedMomentId: String?,
        moments: List<MomentSummary>,
        selectedMomentTypeCode: String?,
    ): String? {
        val moment = moments.firstOrNull { it.momentId == selectedMomentId }
        if (moment != null) return moment.momentTypeCode?.takeIf { it.isNotBlank() }
        return selectedMomentTypeCode?.takeIf { it.isNotBlank() }
    }

    /** Null when the code is blank or not a Business family. Does not default to Team. */
    fun familyOrNull(momentTypeCode: String?): Family? {
        val code = momentTypeCode?.trim()?.uppercase().orEmpty()
        if (code.isEmpty()) return null
        return when {
            code.contains("RUNWAY") -> Family.MONEY
            code.contains("OPERATIONS") && !code.contains("TEAM") -> Family.DAILY
            code.contains("TEAM") -> Family.TEAM
            else -> null
        }
    }

    /** Family catalog for Quick Add. Null when the selected moment has no Business family. */
    fun quickAddSpec(momentTypeCode: String?, smallShop: Boolean): Spec? {
        val family = familyOrNull(momentTypeCode) ?: return null
        return forFamily(family, smallShop)
    }

    fun quickAddPrimary(
        momentTypeCode: String?,
        smallShop: Boolean,
        capabilities: List<String>?,
    ): List<BusinessQuickAddKind> {
        val spec = quickAddSpec(momentTypeCode, smallShop) ?: return emptyList()
        return visibleActions(spec.primary, capabilities, momentTypeCode)
    }

    fun forFamily(family: Family, smallShop: Boolean): Spec = when (family) {
        Family.MONEY -> if (smallShop) shopMoney else growingMoney
        Family.DAILY -> if (smallShop) shopDaily else growingDaily
        Family.TEAM -> if (smallShop) shopTeam else growingTeam
    }

    /**
     * Intersects [kinds] (the selected family's configured ids) with capability availability.
     * Empty capabilities fail open to [BusinessActionRegistry.DEFAULT_CODES], then that set
     * filters this list. The registry does not supply a replacement catalog.
     * Does not pull a later kind forward to fill a slot.
     */
    fun visibleActions(
        kinds: List<BusinessQuickAddKind>,
        capabilities: List<String>?,
        momentTypeCode: String?,
    ): List<BusinessQuickAddKind> = kinds.filter { kind ->
        kind.isLive() && kind.isCapabilityEnabled(capabilities, momentTypeCode)
    }

    private val growingMoney = Spec(
        family = Family.MONEY,
        title = "Money & Cash Flow",
        primary = listOf(
            BusinessQuickAddKind.REVENUE,
            BusinessQuickAddKind.EXPENSE,
            BusinessQuickAddKind.INVOICE,
        ),
        secondary = listOf(
            BusinessQuickAddKind.TAX_ENTRY,
            BusinessQuickAddKind.INVESTOR_UPDATE,
            BusinessQuickAddKind.BUDGET_ALERT,
            BusinessQuickAddKind.FORECAST_UPDATE,
            BusinessQuickAddKind.GENERAL_UPDATE,
            BusinessQuickAddKind.MEMORY,
        ),
    )

    private val shopMoney = Spec(
        family = Family.MONEY,
        title = "Money & Cash Flow",
        primary = listOf(
            BusinessQuickAddKind.KHATA,
            BusinessQuickAddKind.REVENUE,
            BusinessQuickAddKind.EXPENSE,
        ),
        secondary = listOf(
            BusinessQuickAddKind.INVOICE,
            BusinessQuickAddKind.MEMORY,
        ),
    )

    private val growingDaily = Spec(
        family = Family.DAILY,
        title = "Daily Business",
        primary = listOf(
            BusinessQuickAddKind.SPEND_ENTRY,
            BusinessQuickAddKind.UPDATE_VENDOR,
            BusinessQuickAddKind.REPORT_ISSUE,
        ),
        secondary = listOf(
            BusinessQuickAddKind.REQUEST_APPROVAL,
            BusinessQuickAddKind.LOG_IMPROVEMENT,
            BusinessQuickAddKind.BUDGET_REVIEW,
            BusinessQuickAddKind.SLA_CHECK,
            BusinessQuickAddKind.GENERAL_UPDATE,
            BusinessQuickAddKind.MEMORY,
        ),
    )

    private val shopDaily = Spec(
        family = Family.DAILY,
        title = "Daily Business",
        primary = listOf(
            BusinessQuickAddKind.SPEND_ENTRY,
            BusinessQuickAddKind.UPDATE_VENDOR,
            BusinessQuickAddKind.REPORT_ISSUE,
        ),
        secondary = listOf(
            BusinessQuickAddKind.KHATA,
            BusinessQuickAddKind.MEMORY,
        ),
    )

    private val growingTeam = Spec(
        family = Family.TEAM,
        title = "Team & Work",
        primary = listOf(
            BusinessQuickAddKind.TEAM_UPDATE,
            BusinessQuickAddKind.APPROVAL,
            BusinessQuickAddKind.DECISION,
        ),
        secondary = listOf(
            BusinessQuickAddKind.BLOCKER,
            BusinessQuickAddKind.MEETING,
            BusinessQuickAddKind.RECOGNITION,
            BusinessQuickAddKind.MILESTONE,
            BusinessQuickAddKind.RETROSPECTIVE,
            BusinessQuickAddKind.RISK_FLAG,
            BusinessQuickAddKind.ACTIVITY_LOG,
            BusinessQuickAddKind.POLL,
            BusinessQuickAddKind.EXPENSE,
            BusinessQuickAddKind.MEMORY,
        ),
    )

    private val shopTeam = Spec(
        family = Family.TEAM,
        title = "Team & Work",
        primary = listOf(
            BusinessQuickAddKind.KHATA,
            BusinessQuickAddKind.TEAM_UPDATE,
            BusinessQuickAddKind.APPROVAL,
        ),
        secondary = listOf(
            BusinessQuickAddKind.EXPENSE,
            BusinessQuickAddKind.MEMORY,
        ),
    )
}
