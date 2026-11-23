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

    @Test
    fun momentsSortOrderAndTitlesMatchFigma() {
        val screen = "MOMENTS"
        val incoming = listOf("M04", "MKT-RESULTS", "M01", "M03", "M02").map { widget(it, screen) }
        val sorted = ClientCampaignHubWidgets.sortWidgets(screen, incoming)
        assertEquals(listOf("M01", "M02", "M03", "M04", "MKT-RESULTS"), sorted.map { it.widgetId })
        assertEquals("Campaign Overview", ClientCampaignHubWidgets.displayTitle("M01", "Moment Identity"))
        assertEquals("People", ClientCampaignHubWidgets.displayTitle("M02", "People"))
        assertEquals("Channels & Network", ClientCampaignHubWidgets.displayTitle("M03", "Network & Channels"))
        assertEquals("Open Commitments", ClientCampaignHubWidgets.displayTitle("M04", "Open Commitments"))
        assertEquals("Campaign Results", ClientCampaignHubWidgets.displayTitle("MKT-RESULTS", "Campaign Results"))
    }

    @Test
    fun lifeSortOrderAndTitlesMatchFigma() {
        val screen = "LIFE"
        val incoming = listOf("L04", "L01", "L03", "L02").map { widget(it, screen) }
        val sorted = ClientCampaignHubWidgets.sortWidgets(screen, incoming)
        assertEquals(listOf("L01", "L02", "L03", "L04"), sorted.map { it.widgetId })
        assertEquals("Current Campaign", ClientCampaignHubWidgets.displayTitle("L01", "Current Business"))
        assertEquals("Channel Activity", ClientCampaignHubWidgets.displayTitle("L02", "Channels / Orders"))
        assertEquals("Money Moving", ClientCampaignHubWidgets.displayTitle("L03", "Money Moving"))
        assertEquals("People Today", ClientCampaignHubWidgets.displayTitle("L04", "Operations / Execution"))
    }
}