package com.example.momentra.ui.shell.business.shared

import com.example.momentra.ui.shell.empty.business.BusinessSetupKind

/**
 * Industry starter templates for Small shop companies.
 * Config profiles over existing Money / Daily / Team moments — not mini-apps.
 */
data class IndustryTemplate(
    val id: String,
    val label: String,
    val subtitle: String,
    /** Module keys → enabled. Missing keys default to true elsewhere. */
    val modules: Map<String, Boolean>,
    val setupKinds: List<BusinessSetupKind>,
    /** Merged into company profileJson (besides industryTemplate + settings.modules). */
    val profileDefaults: Map<String, Any> = emptyMap(),
    val isCustom: Boolean = false,
)

object IndustryTemplateCatalog {
    const val PROFILE_KEY = "industryTemplate"

    val KIRANA = IndustryTemplate(
        id = "KIRANA",
        label = "Kirana / Grocery",
        subtitle = "Counter sales, suppliers, and udhaar",
        modules = mapOf(
            "money" to true,
            "dailyBusiness" to true,
            "teamOps" to false,
            "vendors" to true,
            "projects" to false,
            "events" to false,
        ),
        setupKinds = listOf(
            BusinessSetupKind.BUSINESS_RUNWAY,
            BusinessSetupKind.BUSINESS_OPERATIONS,
        ),
        profileDefaults = mapOf(
            "industry" to "Retail / Kirana",
            "taxSystem" to "GST",
            "taxModeHint" to "Composition or Regular GST",
            "hubHint" to "Khata and cash sales ready",
        ),
    )

    val PET_RETAIL = IndustryTemplate(
        id = "PET_RETAIL",
        label = "Pet store / Specialty retail",
        subtitle = "Higher ticket sales, stock, and invoices",
        modules = mapOf(
            "money" to true,
            "dailyBusiness" to true,
            "teamOps" to false,
            "vendors" to true,
            "projects" to false,
            "events" to false,
        ),
        setupKinds = listOf(
            BusinessSetupKind.BUSINESS_RUNWAY,
            BusinessSetupKind.BUSINESS_OPERATIONS,
        ),
        profileDefaults = mapOf(
            "industry" to "Pet store / Specialty",
            "taxSystem" to "GST",
            "taxModeHint" to "Regular GST",
            "hubHint" to "Invoices and Khata for regulars",
        ),
    )

    val WORKSHOP = IndustryTemplate(
        id = "WORKSHOP",
        label = "Micro-workshop",
        subtitle = "Make, track spend, and work with a small team",
        modules = mapOf(
            "money" to true,
            "dailyBusiness" to true,
            "teamOps" to true,
            "vendors" to true,
            "projects" to false,
            "events" to false,
        ),
        setupKinds = listOf(
            BusinessSetupKind.BUSINESS_RUNWAY,
            BusinessSetupKind.BUSINESS_OPERATIONS,
            BusinessSetupKind.TEAM_OPERATIONS,
        ),
        profileDefaults = mapOf(
            "industry" to "Manufacturing / Workshop",
            "taxSystem" to "GST",
            "hubHint" to "Money, daily ops, and team together",
        ),
    )

    val SERVICE = IndustryTemplate(
        id = "SERVICE",
        label = "Service / Repair shop",
        subtitle = "Jobs, estimates mindset, and collections",
        modules = mapOf(
            "money" to true,
            "dailyBusiness" to true,
            "teamOps" to false,
            "vendors" to true,
            "projects" to false,
            "events" to false,
        ),
        setupKinds = listOf(
            BusinessSetupKind.BUSINESS_RUNWAY,
            BusinessSetupKind.BUSINESS_OPERATIONS,
        ),
        profileDefaults = mapOf(
            "industry" to "Services",
            "taxSystem" to "GST",
            "taxModeHint" to "Services GST",
            "hubHint" to "Khata for open jobs and invoices",
        ),
    )

    val CUSTOM = IndustryTemplate(
        id = "CUSTOM",
        label = "Custom / Blank",
        subtitle = "Pick Money, Daily Business, and Team yourself",
        modules = mapOf(
            "money" to true,
            "dailyBusiness" to true,
            "teamOps" to false,
            "vendors" to true,
            "projects" to false,
            "events" to false,
        ),
        setupKinds = emptyList(), // resolved from custom module toggles
        isCustom = true,
    )

    val all: List<IndustryTemplate> = listOf(KIRANA, PET_RETAIL, WORKSHOP, SERVICE, CUSTOM)

    fun byId(id: String?): IndustryTemplate? {
        val key = id?.trim()?.uppercase().orEmpty()
        if (key.isEmpty()) return null
        return all.firstOrNull { it.id == key }
    }

    fun labelFor(id: String?): String = byId(id)?.label ?: "—"

    /** Suggest a template from light onboarding questions. Never let billHow alone force SERVICE. */
    fun suggest(sells: String?, bills: String?): IndustryTemplate {
        val s = sells?.lowercase().orEmpty()
        return when {
            s.contains("service") || s.contains("repair") -> SERVICE
            s.contains("made") || s.contains("workshop") || s.contains("manufactur") -> WORKSHOP
            s.contains("physical") || s.contains("item") || s.contains("grocery") || s.contains("kirana") -> KIRANA
            s.contains("pet") || s.contains("specialty") || s.contains("boutique") -> PET_RETAIL
            else -> KIRANA
        }
    }

    fun hubHintFromProfile(profileJson: Map<String, Any?>?): String? {
        val direct = profileJson?.get("hubHint")?.toString()?.trim().orEmpty()
        if (direct.isNotEmpty()) return direct
        val templateId = profileJson?.get(PROFILE_KEY)?.toString()
        return byId(templateId)?.profileDefaults?.get("hubHint")?.toString()?.trim()?.takeIf { it.isNotEmpty() }
    }

    private const val HUB_HINT_PREFS = "momentra_company_hub_hint"
    private const val HUB_HINT_PREFIX = "hub_hint_"

    fun saveHubHint(context: android.content.Context, companyId: String, hint: String?) {
        val id = companyId.trim()
        if (id.isEmpty()) return
        val value = hint?.trim().orEmpty()
        val prefs = context.applicationContext.getSharedPreferences(HUB_HINT_PREFS, android.content.Context.MODE_PRIVATE)
        if (value.isEmpty()) {
            prefs.edit().remove(HUB_HINT_PREFIX + id).apply()
        } else {
            prefs.edit().putString(HUB_HINT_PREFIX + id, value).apply()
        }
    }

    fun hubHintForCompany(context: android.content.Context, companyId: String?): String? {
        val id = companyId?.trim().orEmpty()
        if (id.isEmpty()) return null
        return context.applicationContext
            .getSharedPreferences(HUB_HINT_PREFS, android.content.Context.MODE_PRIVATE)
            .getString(HUB_HINT_PREFIX + id, null)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }

    fun rehydrateHubHints(context: android.content.Context, companies: List<com.example.momentra.domain.CompanySummary>) {
        companies.forEach { company ->
            hubHintFromProfile(company.profileJson)?.let { saveHubHint(context, company.companyId, it) }
        }
    }

    fun setupKindsFor(template: IndustryTemplate, customModules: Map<String, Boolean>): List<BusinessSetupKind> {
        if (!template.isCustom) return template.setupKinds
        val kinds = mutableListOf<BusinessSetupKind>()
        if (customModules["money"] != false) kinds += BusinessSetupKind.BUSINESS_RUNWAY
        if (customModules["dailyBusiness"] != false) kinds += BusinessSetupKind.BUSINESS_OPERATIONS
        if (customModules["teamOps"] == true) kinds += BusinessSetupKind.TEAM_OPERATIONS
        return kinds
    }

    fun modulesFor(template: IndustryTemplate, customModules: Map<String, Boolean>): Map<String, Boolean> {
        return if (template.isCustom) {
            template.modules + customModules
        } else {
            template.modules
        }
    }
}
