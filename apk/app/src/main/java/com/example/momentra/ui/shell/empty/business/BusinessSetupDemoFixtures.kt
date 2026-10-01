package com.example.momentra.ui.shell.empty.business

/**
 * Demo / seed / QA amount examples only.
 * Never merge into [BusinessSetupCatalog.defaultPreferences] or activate/create submission.
 */
object BusinessSetupDemoFixtures {
    val AMOUNTS: Map<String, String> = mapOf(
        "approvalThreshold" to "₹50,000",
        "availableCash" to "₹ 1,80,00,000",
        "monthlySpending" to "₹ 12,50,000",
        "monthlyRevenue" to "₹ 8,08,000",
        "monthlyBudget" to "₹35,00,000",
        "approvalAlarm" to "₹5,00,000",
    )
}
