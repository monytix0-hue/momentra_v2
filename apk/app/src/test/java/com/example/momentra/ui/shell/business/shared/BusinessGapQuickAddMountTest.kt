package com.example.momentra.ui.shell.business.shared

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class BusinessGapQuickAddMountTest {
    @Test
    fun appShell_mountsBusinessGapQuickAddSheet() {
        val shell = moduleMainSource("java/com/example/momentra/ui/shell/AppShellScreen.kt")
        assertTrue(shell.contains("BusinessGapQuickAddSheet("))
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