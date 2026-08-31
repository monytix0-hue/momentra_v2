package com.example.momentra.ui.setup

/** Filters wizard state to keys accepted by backend catalog defaults. */
object SetupPreferenceFilter {
    fun filterToCatalogKeys(
        preferences: Map<String, Any>,
        allowedKeys: Set<String>,
    ): Map<String, Any> = preferences.filterKeys { it in allowedKeys }

    fun filterLocalOnly(
        preferences: Map<String, Any>,
        localOnlyKeys: Set<String>,
    ): Map<String, Any> = preferences.filterKeys { it !in localOnlyKeys }
}
