package com.example.momentra.ui.shell.business.retail

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RetailQuickAddToastContractTest {
    @Test
    fun hubUsesSharedCaptureToastOnRecordSuccess() {
        val hub = moduleMainSource("java/com/example/momentra/ui/shell/business/retail/RetailQuickAdds.kt")
        assertTrue(hub.contains("showRetailCaptureSavedToast()"))
        assertFalse(hub.contains("draft.eventType.equals(\"COLLECTION\""))
    }

    @Test
    fun purchaseSheetDoesNotToastBeforeSubmit() {
        val sheets = moduleMainSource("java/com/example/momentra/ui/shell/business/retail/RetailCaptureSheets.kt")
        assertFalse(sheets.contains("retailMoneySavedToast"))
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