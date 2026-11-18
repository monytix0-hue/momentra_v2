package com.example.momentra.ui.shell.personal.shared

import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalActionRegistryTest {
    @Test
    fun lifeOpsCatalog_includesSpendMoodRecoveryIncomeTransferSavings() {
        val labels = PersonalActionRegistry.catalogTiles(PersonalPulseFamily.LIFE_OPERATIONS).map { it.label }
        assertTrue(labels.containsAll(listOf("Spend", "Mood", "Recovery", "Income", "Transfer", "Savings")))
    }
}