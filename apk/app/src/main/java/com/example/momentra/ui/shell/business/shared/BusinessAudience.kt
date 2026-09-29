package com.example.momentra.ui.shell.business.shared

import android.content.Context
import com.example.momentra.data.repository.MomentCreateRepository

/**
 * Business audience for India micro-retail vs growing business.
 * Stored on company profileJson and moment setup preferences as [PREF_KEY].
 */
object BusinessAudience {
    const val SMALL_SHOP = "SMALL_SHOP"
    const val GROWING = "GROWING"
    const val PREF_KEY = "audience"

    private const val PREFS = "momentra_business_audience"
    private const val KEY_PREFIX = "audience_"

    fun normalize(raw: String?): String {
        val value = raw?.trim()?.uppercase().orEmpty()
        return if (value == SMALL_SHOP) SMALL_SHOP else GROWING
    }

    fun isSmallShop(raw: String?): Boolean = normalize(raw) == SMALL_SHOP

    fun isSmallShop(prefs: Map<String, Any?>?): Boolean {
        val raw = prefs?.get(PREF_KEY)?.toString()
        return isSmallShop(raw)
    }

    /** Null when the preferences map has no audience key (fall back to company cache). */
    fun audienceFromPrefs(prefs: Map<String, Any?>?): String? {
        if (prefs == null || !prefs.containsKey(PREF_KEY)) return null
        return normalize(prefs[PREF_KEY]?.toString())
    }

    fun saveForCompany(context: Context, companyId: String, audience: String) {
        val id = companyId.trim()
        if (id.isEmpty()) return
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PREFIX + id, normalize(audience))
            .apply()
    }

    fun rehydrateFromCompanies(context: Context, companies: List<com.example.momentra.domain.CompanySummary>) {
        companies.forEach { company ->
            val audience = company.profileJson?.get(PREF_KEY)?.toString()
            if (!audience.isNullOrBlank()) {
                saveForCompany(context, company.companyId, audience)
            }
        }
        IndustryTemplateCatalog.rehydrateHubHints(context, companies)
    }

    /**
     * Prefer cache; if missing and [profileJson] has audience, use that (avoids Growing flash).
     */
    fun forCompany(
        context: Context,
        companyId: String?,
        profileJson: Map<String, Any?>? = null,
    ): String {
        val id = companyId?.trim().orEmpty()
        if (id.isEmpty()) return GROWING
        val cached = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_PREFIX + id, null)
        if (!cached.isNullOrBlank()) return normalize(cached)
        val fromProfile = profileJson?.get(PREF_KEY)?.toString()
        if (!fromProfile.isNullOrBlank()) {
            saveForCompany(context, id, fromProfile)
            return normalize(fromProfile)
        }
        return GROWING
    }

    /**
     * Resolve order: moment prefs.audience if present → company local cache → Growing.
     * Bootstrap / company list now return profileJson; clients rehydrate via [rehydrateFromCompanies].
     */
    suspend fun isSmallShopMoment(
        momentId: String?,
        context: Context? = null,
        fallbackCompanyId: String? = null,
        createRepo: MomentCreateRepository = MomentCreateRepository(),
    ): Boolean {
        if (momentId.isNullOrBlank()) {
            if (context != null && !fallbackCompanyId.isNullOrBlank()) {
                return isSmallShop(forCompany(context, fallbackCompanyId))
            }
            return false
        }
        val prefill = createRepo.getDomainSetupPrefill(momentId).getOrNull()
        audienceFromPrefs(prefill?.preferences)?.let { return it == SMALL_SHOP }
        val companyId = prefill?.companyId?.takeIf { it.isNotBlank() } ?: fallbackCompanyId
        if (context != null && !companyId.isNullOrBlank()) {
            return isSmallShop(forCompany(context, companyId))
        }
        return false
    }
}
