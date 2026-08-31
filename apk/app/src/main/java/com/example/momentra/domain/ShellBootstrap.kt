package com.example.momentra.domain

data class ShellBootstrap(
    val identity: ShellIdentity,
    val supportedContexts: List<AppContext>,
    val currentlySelectedContext: AppContext,
    val personalMoments: List<MomentSummary>,
    val groupMoments: List<MomentSummary>,
    val businessMoments: List<MomentSummary>,
    val companies: List<CompanySummary>,
    val selectedCompany: CompanySummary?,
    val capabilities: List<String>,
    val roles: List<String>,
    val preferencesTimezone: String,
    val preferencesLocale: String?,
    val featureFlags: Map<String, String>,
)
