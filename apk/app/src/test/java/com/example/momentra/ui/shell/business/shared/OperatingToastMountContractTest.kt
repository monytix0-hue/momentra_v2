package com.example.momentra.ui.shell.business.shared

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OperatingToastMountContractTest {
  @Test
  fun operatingMomentPage_mentionsToastOverlay() {
    val src = operatingMomentScreensSource()
    assertTrue(
      src.contains("OperatingToastOverlay") || src.contains("MtoToastOverlay"),
    )
  }

  @Test
  fun madeToOrderQuickAdds_hubDoesNotDoubleMountToastOverlay() {
    val src = madeToOrderQuickAddsSource()
    assertFalse(src.contains("MtoToastOverlay"))
    assertFalse(src.contains("OperatingToastOverlay"))
  }

  private fun operatingMomentScreensSource(): String =
    moduleMainSource(
      "java/com/example/momentra/ui/shell/business/shared/OperatingMomentScreens.kt",
    )

  private fun madeToOrderQuickAddsSource(): String =
    moduleMainSource(
      "java/com/example/momentra/ui/shell/business/mto/MadeToOrderQuickAdds.kt",
    )

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