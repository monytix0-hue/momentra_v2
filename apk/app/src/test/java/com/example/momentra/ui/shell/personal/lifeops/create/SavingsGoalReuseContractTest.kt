package com.example.momentra.ui.shell.personal.lifeops.create

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class SavingsGoalReuseContractTest {
    @Test
    fun savingsDepositReusesCreatedGoalPerStubForSession() {
        val sheet = moduleMainSource(
            "java/com/example/momentra/ui/shell/personal/lifeops/create/PersonalMoneyQuickAddSheets.kt",
        )
        assertTrue(sheet.contains("createdGoalIdByStub"))
        assertTrue(sheet.contains("createdGoalIdByStub[stubGoalId]"))
    }

    private fun moduleMainSource(relativePath: String): String {
        val candidates =
            listOf(
                File("src/main/$relativePath"),
                File("../main/$relativePath"),
            )
        val file = candidates.firstOrNull { it.exists() }
            ?: error("Could not find main source at $relativePath (cwd=${File(".").absolutePath})")
        return file.readText()
    }
}