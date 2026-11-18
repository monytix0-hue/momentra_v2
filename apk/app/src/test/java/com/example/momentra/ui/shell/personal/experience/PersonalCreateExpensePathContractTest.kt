package com.example.momentra.ui.shell.personal.experience

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalCreateExpensePathContractTest {
  @Test
  fun createBranch_routesSpendToMasterExpenseNotLocalQuickAddsHub() {
    val host = moduleMainSource(
      "java/com/example/momentra/ui/shell/personal/experience/PersonalExperienceHost.kt",
    )
    assertFalse(host.contains("QuickAddsHub"))
    assertTrue(host.contains("BottomDestination.CREATE -> PersonalQuickAddHub"))
    assertTrue(host.contains("onSpend = { onMoneyQuickAdd(MoneyQuickAddKind.MASTER_EXPENSE) }"))
  }

  @Test
  fun appShell_doesNotMountDeadPersonalExpenseFab() {
    val shell = moduleMainSource("java/com/example/momentra/ui/shell/AppShellScreen.kt")
    assertFalse(shell.contains("PersonalExpenseFab"))
    assertFalse(shell.contains("false &&"))
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