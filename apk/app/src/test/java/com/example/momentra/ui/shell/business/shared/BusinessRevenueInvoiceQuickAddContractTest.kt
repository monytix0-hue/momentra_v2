package com.example.momentra.ui.shell.business.shared

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BusinessRevenueInvoiceQuickAddContractTest {
    @Test
    fun onBusinessQuickAdd_wiresRevenueAndInvoiceToBusinessGapQa() {
        val shell = moduleMainSource("java/com/example/momentra/ui/shell/AppShellScreen.kt")
        assertFalse(shell.contains("BusinessQuickAddKind.INVOICE -> Unit"))
        assertFalse(shell.contains("BusinessQuickAddKind.REVENUE -> Unit"))
        assertTrue(
            shell.contains("BusinessQuickAddKind.REVENUE") &&
                shell.contains("BusinessQuickAddKind.INVOICE") &&
                shell.contains("businessGapQa = kind"),
        )
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