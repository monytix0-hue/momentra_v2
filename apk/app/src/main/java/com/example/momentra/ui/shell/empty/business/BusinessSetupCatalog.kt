package com.example.momentra.ui.shell.empty.business

import com.example.momentra.ui.shell.business.shared.OperatingMomentCatalog

enum class BusinessFieldKind { CHIPS, DROPDOWN, TEXT, TOGGLE, DATE, DATETIME, TIME, MULTI_CHIPS, NUMBER, SELECT }

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
    val familyCode: String,
    val title: String,
    val subtitle: String,
    val momentTypeCode: String,
    val activateLabel: String,
    val defaultTitle: String,
    val defaultPreferences: Map<String, Any>,
)

object BusinessSetupCatalog {
    fun forKind(kind: BusinessSetupKind): BusinessSetupCatalogEntry {
        val subtype = OperatingMomentCatalog.subtypes(kind).first()
        return BusinessSetupCatalogEntry(
            familyCode = kind.familyCode,
            title = kind.title,
            subtitle = kind.body,
            momentTypeCode = subtype.code,
            activateLabel = "Activate",
            defaultTitle = kind.title,
            defaultPreferences = mapOf(
                "subtype" to subtype.code,
                "identityName" to "",
                "partyName" to "",
                "description" to "",
                "currency" to "INR",
                "startDate" to "",
                "participants" to emptyList<Map<String, String>>(),
                "counterparties" to emptyList<Map<String, String>>(),
                "channels" to emptyList<String>(),
                "monthlyBudget" to "",
                "monthlyTarget" to "",
                "targetMetric" to subtype.targetMetrics.first(),
            ),
        )
    }

    fun allowedKeys(kind: BusinessSetupKind): Set<String> = OperatingMomentCatalog.preferenceKeys

    fun footerTagline(kind: BusinessSetupKind, audience: String): String = kind.body

    fun defaultPreferences(kind: BusinessSetupKind, audience: String): Map<String, Any> =
        forKind(kind).defaultPreferences
}
