package com.example.momentra.ui.shell.business.shared

import com.example.momentra.data.api.OperatingWidgetDto
import org.junit.Assert.assertEquals
import org.junit.Test

class ClientCampaignHubWidgetsTest {

    private fun widget(id: String, screen: String, title: String = "Server") =
        OperatingWidgetDto(widgetId = id, screen = screen, title = title)

    @Test
    fun pulseSortOrderAndTitlesMatchFigma() {
        val screen = "PULSE"
        val incoming = listOf("P01", "P02", "P03", "P04", "P05").map { widget(it, screen) }
        val sorted = ClientCampaignHubWidgets.sortWidgets(screen, incoming)
        assertEquals(listOf("P04", "P01", "P03", "P02", "P05"), sorted.map { it.widgetId })
        assertEquals("Needs Attention", ClientCampaignHubWidgets.displayTitle("P04", "Attention"))
        assertEquals("Target & Budget", ClientCampaignHubWidgets.displayTitle("P01", "Target & Budget"))
        assertEquals("Payment Readiness", ClientCampaignHubWidgets.displayTitle("P03", "Payment Readiness"))
        assertEquals("Finance", ClientCampaignHubWidgets.displayTitle("P02", "Finance"))
        assertEquals("Activity", ClientCampaignHubWidgets.displayTitle("P05", "Activity"))
    }
}