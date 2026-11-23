package com.example.momentra.ui.shell.business.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.data.api.OperatingPulseMetricGroupDto
import com.example.momentra.data.api.OperatingQuickAddDto
import com.example.momentra.data.api.OperatingWidgetDto
import com.example.momentra.ui.shell.business.appstate.AppStateSecondaryButton
import com.example.momentra.ui.theme.PlusJakartaSans
import com.example.momentra.ui.theme.generated.BusinessTokens
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** DST-PULSE/MOMENTS/LIFE/MEMORY/QA presentation for DISTRIBUTION_DEALERSHIP. */
object DistributionDealershipHub {
    const val TYPE_CODE: String = "DISTRIBUTION_DEALERSHIP"

    val pulseWidgetOrder = listOf("DD-ATTENTION", "DD-GST", "P01", "P02", "P03", "P04", "P05")
    val momentsWidgetOrder = listOf("DD-COMMITMENTS", "DD-ORDERS", "DD-STOCK", "DD-PEOPLE", "M01", "M02", "M03")

    private val widgetTitles = mapOf(
        "DD-ATTENTION" to "Needs Attention",
        "DD-GST" to "GST Position",
        "DD-COMMITMENTS" to "Open Commitments",
        "DD-ORDERS" to "Orders & Fulfilment",
        "DD-STOCK" to "Stock & Consignment",
        "DD-PEOPLE" to "People & Network",
    )

    fun isDistribution(typeCode: String?): Boolean =
        typeCode?.trim()?.uppercase() == TYPE_CODE

    fun sortWidgets(screen: String, widgets: List<OperatingWidgetDto>): List<OperatingWidgetDto> {
        val order = when (screen.uppercase()) {
            "PULSE" -> pulseWidgetOrder
            "MOMENTS" -> momentsWidgetOrder
            else -> emptyList()
        }
        if (order.isEmpty()) return widgets
        val rank = order.withIndex().associate { (i, id) -> id to i }
        return widgets.sortedWith(
            compareBy<OperatingWidgetDto> { rank[it.widgetId] ?: Int.MAX_VALUE }.thenBy { it.widgetId },
        )
    }

    fun displayTitle(widgetId: String, serverTitle: String): String =
        widgetTitles[widgetId] ?: serverTitle.ifBlank { widgetId }

    fun presentWidgets(screen: String, widgets: List<OperatingWidgetDto>): List<OperatingWidgetDto> =
        sortWidgets(screen, widgets).map { w -> w.copy(title = displayTitle(w.widgetId, w.title)) }

    val quickActionStrip = listOf(
        "SALE" to "Sale",
        "PURCHASE" to "Purchase",
        "COLLECTION" to "Collection",
        "PAYMENT" to "Payment",
    )

    val quickAddCore = setOf(
        "SALE", "PURCHASE", "COLLECTION", "PAYMENT", "ORDER", "SHIPMENT", "DELIVERY", "STOCK",
    )
    val quickAddSupport = setOf(
        "SPEND", "BUDGET", "UPDATE", "ISSUE", "APPROVAL", "RETURN",
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DistributionHeroHeader(
    model: OperatingProjectionDto?,
    momentTitle: String?,
    palette: HubPalette,
    companyId: String?,
) {
    Text(
        "DISTRIBUTION",
        color = palette.accent,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        fontFamily = PlusJakartaSans,
    )
    Text(
        model?.title?.takeIf { it.isNotBlank() } ?: momentTitle ?: model?.subtypeLabel ?: "Distribution",
        color = palette.text,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        fontFamily = PlusJakartaSans,
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        model?.partyName?.takeIf { it.isNotBlank() }?.let { DistributionChip(it, palette) }
        companyId?.takeIf { it.isNotBlank() }?.let { DistributionChip("Company", palette) }
        fyLabel(model)?.let { DistributionChip(it, palette) }
        if (booksOpen(model)) {
            DistributionChip("Books open", palette, emphasized = true)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DistributionQuickActionStrip(
    palette: HubPalette,
    onQuickAddCode: (String) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        DistributionDealershipHub.quickActionStrip.forEach { (code, label) ->
            Text(
                label,
                color = palette.text,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(palette.card)
                    .border(1.dp, palette.border, RoundedCornerShape(20.dp))
                    .clickable { onQuickAddCode(code) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun DistributionChip(label: String, palette: HubPalette, emphasized: Boolean = false) {
    Text(
        label,
        color = if (emphasized) palette.onAccent else palette.text,
        fontSize = 11.sp,
        fontFamily = PlusJakartaSans,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (emphasized) palette.accent else palette.card)
            .border(1.dp, palette.border, RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

private fun fyLabel(model: OperatingProjectionDto?): String? {
    val start = model?.startDate?.take(4)
    val end = model?.endDate?.take(4)
    return when {
        !start.isNullOrBlank() && !end.isNullOrBlank() && start != end -> "FY $start–$end"
        !start.isNullOrBlank() -> "FY $start"
        else -> null
    }
}

private fun booksOpen(model: OperatingProjectionDto?): Boolean {
    if (model == null) return false
  val today = LocalDate.now()
    val start = runCatching { LocalDate.parse(model.startDate?.take(10)) }.getOrNull()
    val end = model.endDate?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    if (start != null && today.isBefore(start)) return false
    if (end != null && today.isAfter(end)) return false
    return model.financeVisible || (model.budget?.configured == true || model.target?.configured == true)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DistributionMomentsHero(model: OperatingProjectionDto?, palette: HubPalette) {
    val routes = model?.life?.channels?.takeIf { it.isNotEmpty() }?.joinToString(" · ")
        ?: model?.boards?.moments?.firstOrNull()?.detail?.takeIf { it.isNotBlank() }
    val people = model?.participants?.size ?: 0
  FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DistributionChip(if (routes != null) "Routes live" else "Routes", palette, emphasized = routes != null)
        DistributionChip(
            if (people > 0) "People · $people" else "People · Dealers",
            palette,
        )
    }
}

@Composable
fun DistributionMemoryHero(model: OperatingProjectionDto?, palette: HubPalette) {
    val period = model?.memory?.let { "Closed period" } ?: "Closed period"
    DistributionChip(period, palette, emphasized = true)
    val revisions = model?.memory?.revisions.orEmpty()
    if (revisions.isNotEmpty()) {
        DistributionChip("${revisions.size} revisions", palette)
    }
}

@Composable
fun DistributionPulseMetricCards(
    groups: List<OperatingPulseMetricGroupDto>,
    palette: HubPalette,
    onViewAllAttention: () -> Unit,
) {
    groups.forEach { group ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(palette.card)
                .border(1.dp, palette.border, RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    pulseGroupTitle(group.code),
                    color = palette.text,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    fontFamily = PlusJakartaSans,
                )
                if (group.code.equals("ATTENTION", ignoreCase = true)) {
                    Text(
                        "View all ›",
                        color = palette.accent,
                        fontSize = 12.sp,
                        fontFamily = PlusJakartaSans,
                        modifier = Modifier.clickable(onClick = onViewAllAttention),
                    )
                }
            }
            DistributionMetricTileRow(
                group.metrics.map { it.label to formatMetricValue(it.value) },
                palette,
            )
            group.why?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    color = palette.muted,
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
    }
}

fun formatMetricValue(raw: String?): String {
    val v = raw?.trim().orEmpty()
    if (v.isBlank() || v == "0" || v == "0.0000" || v == "₹0" || v == "₹0.0000") {
        return "Not recorded"
    }
    return v
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DistributionMetricTileRow(tiles: List<Pair<String, String>>, palette: HubPalette) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tiles.forEach { (label, value) ->
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.bg)
                    .border(1.dp, palette.border.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Text(label, color = palette.muted, fontSize = 11.sp, fontFamily = PlusJakartaSans)
                Text(
                    value.ifBlank { "Not recorded" },
                    color = palette.text,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
    }
}

@Composable
fun DistributionLifeHeader(palette: HubPalette) {
    Text(
        "Today",
        color = palette.text,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        fontFamily = PlusJakartaSans,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DistributionPaymentChips(model: OperatingProjectionDto?, palette: HubPalette) {
    val chips = model?.paymentReadiness.orEmpty().mapNotNull { row ->
        when (row.dueBucket?.uppercase()) {
            "OVERDUE" -> "Overdue · ${row.title}"
            "DUE_SOON" -> "Due soon · ${row.title}"
            else -> null
        }
    }
    if (chips.isEmpty()) return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        chips.forEach { DistributionChip(it, palette) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DistributionQuickAddHub(
    adds: List<OperatingQuickAddDto>,
    palette: HubPalette,
    initialCode: String?,
    onSubmit: suspend (eventType: String, title: String, amount: String?) -> Unit,
) {
    var selected by remember(adds, initialCode) {
        mutableStateOf(initialCode?.takeIf { code -> adds.any { it.code == code } } ?: adds.firstOrNull()?.code.orEmpty())
    }
    var search by remember { mutableStateOf("") }
    var titleText by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    val filtered = adds.filter {
        val q = search.trim()
        q.isBlank() || it.label.contains(q, true) || it.code.contains(q, true)
    }
    val groups = listOf(
        "Core" to filtered.filter { DistributionDealershipHub.quickAddCore.contains(it.code.uppercase()) },
        "Support" to filtered.filter { DistributionDealershipHub.quickAddSupport.contains(it.code.uppercase()) },
        "Utilities" to filtered.filter {
            val c = it.code.uppercase()
            !DistributionDealershipHub.quickAddCore.contains(c) && !DistributionDealershipHub.quickAddSupport.contains(c)
        },
    ).filter { it.second.isNotEmpty() }

    Text(
        "Source. Move. Settle.",
        color = palette.accent,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = PlusJakartaSans,
    )
    BasicTextField(
        value = search,
        onValueChange = { search = it },
        textStyle = TextStyle(color = palette.text, fontSize = 14.sp, fontFamily = PlusJakartaSans),
        cursorBrush = SolidColor(palette.accent),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(palette.card)
            .border(1.dp, palette.border, RoundedCornerShape(12.dp))
            .padding(12.dp),
        decorationBox = { inner ->
            if (search.isEmpty()) Text("Search quick adds", color = palette.muted, fontSize = 14.sp)
            inner()
        },
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("SHIPMENT" to "Dispatch", "SALE" to "Record Sale").forEach { (code, label) ->
            if (adds.any { it.code.equals(code, true) }) {
                Text(
                    label,
                    color = palette.onAccent,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.accent)
                        .clickable { selected = code }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    fontFamily = PlusJakartaSans,
                )
            }
        }
    }
    groups.forEach { (heading, items) ->
        Text(heading, color = palette.text, fontWeight = FontWeight.Medium, fontFamily = PlusJakartaSans)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items.forEach { add ->
                val on = add.code == selected
                Text(
                    add.label,
                    color = if (on) palette.accent else palette.text,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (on) palette.accent.copy(alpha = 0.18f) else palette.card)
                        .border(1.dp, if (on) palette.accent else palette.border, RoundedCornerShape(16.dp))
                        .clickable { selected = add.code }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    fontFamily = PlusJakartaSans,
                )
            }
        }
    }
    Text(
        "Every post becomes evidence on Pulse — nothing is invented until you add it.",
        color = palette.muted,
        fontSize = 11.sp,
        fontStyle = FontStyle.Italic,
        fontFamily = PlusJakartaSans,
    )
    TypedOperatingQuickAddFormFields(
        selectedCode = selected,
        adds = adds,
        titleText = titleText,
        amountText = amountText,
        onTitleChange = { titleText = it },
        onAmountChange = { amountText = it },
        palette = palette,
        onSubmit = onSubmit,
    )
}

@Composable
fun DistributionGenerateStoryButton(
    palette: HubPalette,
    onGenerateStory: () -> Unit,
) {
    Text(
        "Generate Business Story",
        color = palette.onAccent,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(palette.accent)
            .clickable(onClick = onGenerateStory)
            .padding(vertical = 14.dp, horizontal = 16.dp),
        fontFamily = PlusJakartaSans,
    )
}

fun distributionBoardHeading(tab: OperatingTab): String = when (tab) {
    OperatingTab.LIFE -> "How this moment connects"
    OperatingTab.MEMORY -> "What stays"
    else -> "Sales, purchases, and movement"
}