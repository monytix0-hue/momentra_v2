package com.example.momentra.ui.shell.empty

import com.example.momentra.ui.shell.empty.personal.PersonalSetupFieldSpec

enum class BusinessFieldKind { CHIPS, DROPDOWN, TEXT, TOGGLE, DATE, DATETIME, TIME }

data class BusinessSetupSectionSpec(
    val title: String,
    val subtitle: String? = null,
    val fields: List<BusinessSetupFieldSpec>,
)

data class BusinessSetupFieldSpec(
    val key: String,
    val label: String,
    val kind: BusinessFieldKind,
    val options: List<String> = emptyList(),
    val localOnly: Boolean = false,
)

data class BusinessSetupCatalogEntry(
    val defaultTitle: String,
    val subtitle: String,
    val momentTypeCode: String,
    val activateLabel: String,
    val footerTagline: String,
    val sections: List<BusinessSetupSectionSpec>,
    val defaultPreferences: Map<String, Any>,
)

object BusinessSetupCatalog {
    private val teamOps = BusinessSetupCatalogEntry(
        defaultTitle = "Team Operations",
        subtitle = "Configure your team's operating system in one go.",
        momentTypeCode = "TEAM_OPERATIONS",
        activateLabel = "Activate Team Operations",
        footerTagline = "YOUR TEAM RUNS ON CLARITY",
        defaultPreferences = mapOf(
            "teamName" to "Growth & Product",
            "size" to "11-25 people",
            "workMode" to "Hybrid",
            "country" to "India",
            "currency" to "INR",
            "timezone" to "IST (UTC+5:30)",
            "language" to "English",
            "financialYear" to "Apr - Mar",
            "taxSystem" to "GST",
            "coordination" to "Structured",
            "reviewCycle" to "Weekly",
            "monitoring" to "Balanced",
            "spendingApproval" to "Required",
            "approvalThreshold" to "₹50,000",
        ),
        sections = listOf(
            BusinessSetupSectionSpec(
                title = "Team Identity",
                subtitle = "Name your team and set baseline context.",
                fields = listOf(
                    BusinessSetupFieldSpec("teamName", "Team name", BusinessFieldKind.TEXT),
                    BusinessSetupFieldSpec("size", "Team size", BusinessFieldKind.CHIPS, listOf("1-10 people", "11-25 people", "26-50 people", "50+ people")),
                    BusinessSetupFieldSpec("workMode", "Work mode", BusinessFieldKind.CHIPS, listOf("Remote", "Hybrid", "In-office")),
                ),
            ),
            BusinessSetupSectionSpec(
                title = "Locale & Finance",
                fields = listOf(
                    BusinessSetupFieldSpec("country", "Country", BusinessFieldKind.TEXT),
                    BusinessSetupFieldSpec("currency", "Currency", BusinessFieldKind.CHIPS, listOf("INR", "USD", "EUR", "GBP")),
                    BusinessSetupFieldSpec("timezone", "Timezone", BusinessFieldKind.TEXT),
                    BusinessSetupFieldSpec("language", "Language", BusinessFieldKind.CHIPS, listOf("English", "Hindi", "Spanish", "French")),
                    BusinessSetupFieldSpec("financialYear", "Financial year", BusinessFieldKind.CHIPS, listOf("Jan - Dec", "Apr - Mar")),
                    BusinessSetupFieldSpec("taxSystem", "Tax system", BusinessFieldKind.CHIPS, listOf("GST", "VAT", "Sales Tax")),
                ),
            ),
            BusinessSetupSectionSpec(
                title = "Coordination",
                fields = listOf(
                    BusinessSetupFieldSpec("coordination", "Coordination style", BusinessFieldKind.CHIPS, listOf("Structured", "Flexible", "Async-first")),
                    BusinessSetupFieldSpec("reviewCycle", "Review cycle", BusinessFieldKind.CHIPS, listOf("Weekly", "Bi-weekly", "Monthly")),
                    BusinessSetupFieldSpec("monitoring", "Monitoring", BusinessFieldKind.CHIPS, listOf("Light", "Balanced", "Detailed")),
                    BusinessSetupFieldSpec("spendingApproval", "Spending approval", BusinessFieldKind.CHIPS, listOf("Required", "Optional", "Not needed")),
                    BusinessSetupFieldSpec("approvalThreshold", "Approval threshold", BusinessFieldKind.TEXT),
                ),
            ),
        ),
    )

    private val runway = BusinessSetupCatalogEntry(
        defaultTitle = "Business Runway",
        subtitle = "Configure your financial operating system on-the-go.",
        momentTypeCode = "BUSINESS_RUNWAY",
        activateLabel = "Activate Business Runway",
        footerTagline = "RUNWAY CLARITY KEEPS YOU MOVING",
        defaultPreferences = mapOf(
            "businessStage" to "Scaling",
            "goalHorizon" to "18-months goal",
            "multiCurrency" to true,
            "availableCash" to "₹ 1,80,00,000",
            "monthlySpending" to "₹ 12,50,000",
            "revenueStage" to "Growing",
            "monthlyRevenue" to "₹ 8,08,000",
            "revenueModel" to "Recurring",
            "warningThreshold" to "6 months",
            "fundingSource" to "Bootstrapped + revenue",
        ),
        sections = listOf(
            BusinessSetupSectionSpec(
                title = "Business Stage",
                fields = listOf(
                    BusinessSetupFieldSpec("businessStage", "Business stage", BusinessFieldKind.CHIPS, listOf("Early", "Scaling", "Mature", "Turnaround")),
                    BusinessSetupFieldSpec("goalHorizon", "Goal horizon", BusinessFieldKind.CHIPS, listOf("6-months goal", "12-months goal", "18-months goal", "24-months goal")),
                    BusinessSetupFieldSpec("multiCurrency", "Multi-currency", BusinessFieldKind.TOGGLE),
                ),
            ),
            BusinessSetupSectionSpec(
                title = "Cash & Spending",
                fields = listOf(
                    BusinessSetupFieldSpec("availableCash", "Available cash", BusinessFieldKind.TEXT),
                    BusinessSetupFieldSpec("monthlySpending", "Monthly spending", BusinessFieldKind.TEXT),
                    BusinessSetupFieldSpec("fundingSource", "Funding source", BusinessFieldKind.TEXT),
                ),
            ),
            BusinessSetupSectionSpec(
                title = "Revenue",
                fields = listOf(
                    BusinessSetupFieldSpec("revenueStage", "Revenue stage", BusinessFieldKind.CHIPS, listOf("Pre-revenue", "Growing", "Stable", "Declining")),
                    BusinessSetupFieldSpec("monthlyRevenue", "Monthly revenue", BusinessFieldKind.TEXT),
                    BusinessSetupFieldSpec("revenueModel", "Revenue model", BusinessFieldKind.CHIPS, listOf("Recurring", "Project-based", "Mixed", "Marketplace")),
                    BusinessSetupFieldSpec("warningThreshold", "Runway warning threshold", BusinessFieldKind.CHIPS, listOf("3 months", "6 months", "9 months", "12 months")),
                ),
            ),
        ),
    )

    private val ops = BusinessSetupCatalogEntry(
        defaultTitle = "Business Operations",
        subtitle = "Configure operational capacity, monitoring and approvals.",
        momentTypeCode = "BUSINESS_OPERATIONS",
        activateLabel = "Activate Business Operations",
        footerTagline = "OPERATIONS WITH INTENTIONAL GUARDRAILS",
        defaultPreferences = mapOf(
            "coreOps" to "Growth & Product",
            "scope" to "Company-wide",
            "model" to "Centralized",
            "cadence" to "Monthly",
            "monthlyBudget" to "₹35,00,000",
            "allocationMethod" to "Category-based",
            "monitoringStyle" to "Proactive",
            "approvalModel" to "Threshold-based",
            "approvalAlarm" to "₹5,00,000",
        ),
        sections = listOf(
            BusinessSetupSectionSpec(
                title = "Operating Scope",
                fields = listOf(
                    BusinessSetupFieldSpec("coreOps", "Core operations focus", BusinessFieldKind.CHIPS, listOf("Growth & Product", "Delivery", "Support", "Finance")),
                    BusinessSetupFieldSpec("scope", "Scope", BusinessFieldKind.CHIPS, listOf("Company-wide", "Department", "Team", "Project")),
                    BusinessSetupFieldSpec("model", "Operating model", BusinessFieldKind.CHIPS, listOf("Centralized", "Distributed", "Hybrid")),
                    BusinessSetupFieldSpec("cadence", "Planning cadence", BusinessFieldKind.CHIPS, listOf("Weekly", "Monthly", "Quarterly")),
                ),
            ),
            BusinessSetupSectionSpec(
                title = "Budget & Approvals",
                subtitle = "Budget fields sync with backend preferences.",
                fields = listOf(
                    BusinessSetupFieldSpec("monthlyBudget", "Monthly budget", BusinessFieldKind.TEXT),
                    BusinessSetupFieldSpec("allocationMethod", "Allocation method", BusinessFieldKind.CHIPS, listOf("Category-based", "Team-based", "Project-based")),
                    BusinessSetupFieldSpec("approvalModel", "Approval model", BusinessFieldKind.CHIPS, listOf("Threshold-based", "Manager-only", "Committee")),
                    BusinessSetupFieldSpec("approvalAlarm", "Approval alarm threshold", BusinessFieldKind.TEXT),
                ),
            ),
            BusinessSetupSectionSpec(
                title = "Monitoring",
                fields = listOf(
                    BusinessSetupFieldSpec("monitoringStyle", "Monitoring style", BusinessFieldKind.CHIPS, listOf("Proactive", "Reactive", "Balanced")),
                ),
            ),
        ),
    )

    fun forKind(kind: BusinessSetupKind): BusinessSetupCatalogEntry = when (kind) {
        BusinessSetupKind.TEAM_OPERATIONS -> teamOps
        BusinessSetupKind.BUSINESS_RUNWAY -> runway
        BusinessSetupKind.BUSINESS_OPERATIONS -> ops
    }

    fun allowedKeys(kind: BusinessSetupKind): Set<String> = forKind(kind).defaultPreferences.keys
}

/** Legacy chip-only field list for tests or callers expecting PersonalSetupFieldSpec. */
fun BusinessSetupCatalog.chipFields(kind: BusinessSetupKind): List<PersonalSetupFieldSpec> =
    forKind(kind).sections.flatMap { section ->
        section.fields.filter { it.kind == BusinessFieldKind.CHIPS }.map {
            PersonalSetupFieldSpec(it.key, it.label, false, it.options)
        }
    }
