package com.example.momentra.ui.shell.business.shared

import com.example.momentra.data.api.OperatingWidgetDto
import com.example.momentra.ui.shell.business.appstate.BusinessAppStateAccent
import com.example.momentra.ui.shell.business.appstate.BusinessSetupActivateToastCopy
import com.example.momentra.ui.theme.generated.BusinessTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OwnMarketingHubTest {
    @Test
    fun detectsOwnSubtype() {
        assertTrue(OwnMarketingHub.isOwn("OWN_BUSINESS_CAMPAIGN"))
        assertTrue(OwnMarketingHub.isOwn(" own_business_campaign "))
        assertFalse(OwnMarketingHub.isOwn("CLIENT_CAMPAIGN"))
        assertFalse(OwnMarketingHub.isOwn(null))
    }

    @Test
    fun pulseOrderMatchesFigmaNeedsAttentionFirst() {
        val widgets = listOf(
            OperatingWidgetDto(widgetId = "P01", title = "Target"),
            OperatingWidgetDto(widgetId = "P04", title = "Attention"),
            OperatingWidgetDto(widgetId = "P05", title = "Activity"),
        )
        val presented = OwnMarketingHub.presentWidgets("PULSE", widgets)
        assertEquals(listOf("P04", "P01", "P05"), presented.map { it.widgetId })
        assertEquals("Needs Attention", presented.first().title)
        assertEquals("AN-P01", OwnMarketingHub.displayCode("P04", null))
        assertEquals("AN-P02", OwnMarketingHub.displayCode("P01", null))
    }

    @Test
    fun ownAccentAndActivateCopy() {
        assertEquals(BusinessAppStateAccent.Magenta, BusinessAppStateAccent.forTypeCode("OWN_BUSINESS_CAMPAIGN"))
        assertEquals("Moment activated", BusinessSetupActivateToastCopy.DEFAULT_TITLE)
        assertEquals(
            "Harbor & Loom · Campaign is live · opening Pulse",
            BusinessSetupActivateToastCopy.subtitle("Harbor & Loom", "OWN_BUSINESS_CAMPAIGN"),
        )
    }

    @Test
    fun hubPaletteUsesOwnMagentaSurface() {
        val palette = hubPaletteFor("OWN_BUSINESS_CAMPAIGN", "MARKETING_CAMPAIGNS")
        assertEquals(BusinessTokens.Colors.MARKETING_OWNAccent, palette.accent)
        assertEquals(BusinessTokens.Colors.MARKETING_OWNSurface, palette.card)
    }
}
