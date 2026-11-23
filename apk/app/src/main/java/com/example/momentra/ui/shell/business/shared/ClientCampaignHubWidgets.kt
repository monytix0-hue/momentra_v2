package com.example.momentra.ui.shell.business.shared

import com.example.momentra.data.api.OperatingWidgetDto

/**
 * Figma CLIENT_CAMPAIGN hub widget order + display titles (nodes 5:29386–5:30195).
 * Callers: OperatingMomentPage — sort/title presentation only; no finance math.
 */
object ClientCampaignHubWidgets {
    const val TYPE_CODE: String = "CLIENT_CAMPAIGN"

    private val pulseOrder = listOf("P04", "P01", "P03", "P02", "P05")
    private val momentsOrder = listOf("M01", "M02", "M03", "M04", "MKT-RESULTS")
    private val lifeOrder = listOf("L01", "L02", "L03", "L04")
    private val memoryOrder = listOf("ME01", "ME02", "ME03", "ME04")

    private val titles: Map<String, String> = mapOf(
        "P04" to "Needs Attention",
        "P01" to "Target & Budget",
        "P03" to "Payment Readiness",
        "P02" to "Finance",
        "P05" to "Activity",
        "M01" to "Campaign Overview",
        "M02" to "People",
        "M03" to "Channels & Network",
        "M04" to "Open Commitments",
        "MKT-RESULTS" to "Campaign Results",
        "L01" to "Current Campaign",
        "L02" to "Channel Activity",
        "L03" to "Money Moving",
        "L04" to "People Today",
        "ME01" to "Monthly History",
        "ME02" to "Money History",
        "ME03" to "People & Activity",
        "ME04" to "Patterns & Decisions",
    )

    fun isClientCampaign(typeCode: String?): Boolean =
        typeCode?.trim()?.uppercase() == TYPE_CODE

    fun orderForScreen(screen: String): List<String>? = when (screen.uppercase()) {
        "PULSE" -> pulseOrder
        "MOMENTS" -> momentsOrder
        "LIFE" -> lifeOrder
        "MEMORY" -> memoryOrder
        else -> null
    }

    fun sortWidgets(screen: String, widgets: List<OperatingWidgetDto>): List<OperatingWidgetDto> {
        val order = orderForScreen(screen) ?: return widgets
        val rank = order.withIndex().associate { (index, id) -> id to index }
        return widgets.sortedWith(
            compareBy<OperatingWidgetDto> { rank[it.widgetId] ?: Int.MAX_VALUE }
                .thenBy { it.widgetId },
        )
    }

    /** CLIENT_CAMPAIGN display title; caller must gate on [isClientCampaign]. */
    fun displayTitle(widgetId: String, serverTitle: String): String =
        titles[widgetId] ?: serverTitle.ifBlank { widgetId }

    fun presentForClient(screen: String, widgets: List<OperatingWidgetDto>): List<OperatingWidgetDto> =
        sortWidgets(screen, widgets).map { w ->
            w.copy(title = displayTitle(w.widgetId, w.title))
        }
}