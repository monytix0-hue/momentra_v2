package com.example.momentra.ui.shell.business.setup

import org.junit.Assert.assertEquals
import org.junit.Test

class BusinessSetupHeroTitleTest {

    @Test
    fun emptyNameUsesPlaceholder() {
        assertEquals("Brightline Setup", BusinessSetupHeroTitle.format(null, "Brightline Setup"))
        assertEquals("Brightline Setup", BusinessSetupHeroTitle.format("  ", "Brightline Setup"))
    }

    @Test
    fun typedNameAppendsSetup() {
        assertEquals("Brightline Creative Setup", BusinessSetupHeroTitle.format("Brightline Creative", "Brightline Setup"))
    }

    @Test
    fun nameAlreadyEndingInSetupIsNotDoubled() {
        assertEquals("Brightline Setup", BusinessSetupHeroTitle.format("Brightline Setup", "Harbor & Loom Setup"))
        assertEquals("Brightline Setup", BusinessSetupHeroTitle.format("brightline setup", "Harbor & Loom Setup"))
    }
}