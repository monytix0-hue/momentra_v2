package com.example.momentra.ui.shell.business.shared

import android.content.Context
import com.example.momentra.domain.CompanySummary
import com.example.momentra.ui.shell.empty.business.BusinessSetupKind

/**
 * Company module toggles from profileJson.settings.modules (teamOps, dailyBusiness, money, etc.).
 * Cached locally per company; missing keys default to enabled.
 */
object CompanyModules {
    val knownKeys: Set<String> = setOf(
        "teamOps",
        "dailyBusiness",
        "money",
        "vendors",
        "projects",
        "events",
    )

    private const val PREFS = "momentra_company_modules"
    private const val KEY_PREFIX = "module_"

    fun moduleKeyForSetupKind(kind: BusinessSetupKind): String? = when (kind) {
        BusinessSetupKind.TEAM_OPERATIONS -> "teamOps"
        BusinessSetupKind.BUSINESS_RUNWAY -> "money"
        BusinessSetupKind.BUSINESS_OPERATIONS -> "dailyBusiness"
    }

    fun moduleKeyForTheme(theme: BusinessActiveTheme): String = when (theme.typeLabel) {
        BusinessActiveTheme.BusinessRunway.typeLabel -> "money"
        BusinessActiveTheme.BusinessOperations.typeLabel -> "dailyBusiness"
        else -> "teamOps"
    }

    fun saveModules(context: Context, companyId: String, modules: Map<String, Boolean>) {
        val id = companyId.trim()
        if (id.isEmpty() || modules.isEmpty()) return
        val editor = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
        modules.forEach { (key, enabled) ->
            val k = key.trim()
            if (k.isNotEmpty()) {
                editor.putBoolean(prefKey(id, k), enabled)
            }
        }
        editor.apply()
    }

    fun isEnabled(context: Context, companyId: String?, key: String): Boolean {
        val id = companyId?.trim().orEmpty()
        if (id.isEmpty()) return true
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getBoolean(prefKey(id, key), true)
    }

    fun rehydrateFromCompanies(context: Context, companies: List<CompanySummary>) {
        companies.forEach { company ->
            val modules = modulesFromProfile(company.profileJson)
            if (modules.isNotEmpty()) {
                saveModules(context, company.companyId, modules)
            }
        }
    }

    internal fun modulesFromProfile(profileJson: Map<String, Any?>?): Map<String, Boolean> {
        if (profileJson == null) return emptyMap()
        val settings = profileJson["settings"] as? Map<*, *> ?: return emptyMap()
        val modules = settings["modules"] as? Map<*, *> ?: return emptyMap()
        return modules.entries.mapNotNull { (k, v) ->
            val key = k?.toString()?.trim().orEmpty()
            if (key.isEmpty()) return@mapNotNull null
            key to parseBool(v, default = true)
        }.toMap()
    }

    private fun prefKey(companyId: String, moduleKey: String): String = KEY_PREFIX + companyId + "_" + moduleKey

    private fun parseBool(value: Any?, default: Boolean): Boolean = when (value) {
        is Boolean -> value
        is Number -> value.toInt() != 0
        is String -> value.equals("true", ignoreCase = true)
        else -> default
    }
}
