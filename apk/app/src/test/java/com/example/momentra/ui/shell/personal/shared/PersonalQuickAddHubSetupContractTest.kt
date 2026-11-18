package com.example.momentra.ui.shell.personal.shared

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalQuickAddHubSetupContractTest {
    @Test
    fun tilesRemainTappableWithoutActiveMomentForSetupRouting() {
        val hub = moduleMainSource("java/com/example/momentra/ui/shell/personal/shared/PersonalQuickAddHub.kt")
        assertTrue(hub.contains("enabled = tile.tappable"))
        assertTrue(hub.contains("dimmed = !hasActiveMoment"))
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