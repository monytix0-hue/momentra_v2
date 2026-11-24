package com.example.momentra.ui.shell.business.shared

/**
 * Figma Own Business Campaign hub (MKT-OWN-PULSE-v12 · Moments · Life · Memory · QA hub).
 * Presentation only — binds to GET /operating + /widgets; no client finance math.
 * Keep in lockstep with `OwnMarketingHub.swift` on iOS.
 */

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.OperatingBoardSectionDto
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.data.api.OperatingPulseMetricGroupDto
import com.example.momentra.data.api.OperatingQuickAddDto
import com.example.momentra.data.api.OperatingQuickAddHubTileDto
import com.example.momentra.data.api.OperatingWidgetDto
import com.example.momentra.ui.theme.PlusJakartaSans
import com.example.momentra.ui.theme.generated.BusinessTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object OwnMarketingHub {
    const val TYPE_CODE: String = "OWN_BUSINESS_CAMPAIGN"
    const val TAGLINE: String = "Plan. Launch. Measure."

    /** Pulse Figma order: Needs Attention → Target → Payment → Finance → Activity. */
    private val pulseOrder = listOf("P04", "P01", "P03", "P02", "P05")
    private val momentsOrder = listOf("M01", "M02", "M03", "M04", "MKT-RESULTS", "MKT-ATTRIBUTION")
    private val lifeOrder = listOf("L01", "L02", "L03", "L04")
    private val memoryOrder = listOf("ME01", "ME02", "ME03", "ME04")

    private val titles: Map<String, String> = mapOf(
        "P04" to "Needs Attention",
        "P01" to "Target & Budget",
        "P03" to "Payment Readiness",
        "P02" to "Finance",
        "P05" to "Activity",
        "M01" to "Campaign / Business Moment",
        "M02" to "People",
        "M03" to "Channels & Network",
        "M04" to "Open Commitments",
        "MKT-RESULTS" to "Campaign Results",
        "MKT-ATTRIBUTION" to "Attribution",
        "L01" to "Current Campaign",
        "L02" to "Channel Activity",
        "L03" to "Money Moving",
        "L04" to "People Today",
        "ME01" to "Monthly Memory",
        "ME02" to "Money Memory",
        "ME03" to "People & Activity",
        "ME04" to "Patterns & Decisions",
    )

    private val anCodes: Map<String, String> = mapOf(
        "P04" to "AN-P01",
        "P01" to "AN-P02",
        "P03" to "AN-P03",
        "P02" to "AN-P04",
        "P05" to "AN-P05",
        "M01" to "AN-M01",
        "M02" to "AN-M02",
        "M03" to "AN-M03",
        "M04" to "AN-M04",
        "L01" to "AN-L01",
        "L02" to "AN-L02",
        "L03" to "AN-L03",
        "L04" to "AN-L04",
        "ME01" to "AN-ME01",
        "ME02" to "AN-ME02",
        "ME03" to "AN-ME03",
        "ME04" to "AN-ME04",
    )

    private val pulseGroupAn: Map<String, String> = mapOf(
        "ATTENTION" to "AN-P01",
        "TARGET_BUDGET" to "AN-P02",
        "PAYMENT_READINESS" to "AN-P03",
        "FINANCE" to "AN-P04",
        "ACTIVITY" to "AN-P05",
    )

    fun isOwn(typeCode: String?): Boolean =
        typeCode?.trim()?.uppercase() == TYPE_CODE

    fun orderForScreen(screen: String): List<String>? = when (screen.uppercase()) {
        "PULSE" -> pulseOrder
        "MOMENTS" -> momentsOrder
        "LIFE" -> lifeOrder
        "MEMORY" -> memoryOrder
        else -> null
    }

    fun displayTitle(widgetId: String, serverTitle: String): String =
        titles[widgetId] ?: serverTitle.ifBlank { widgetId }

    fun displayCode(widgetId: String, serverDisplayCode: String?): String? {
        val server = serverDisplayCode?.trim()?.takeIf { it.isNotEmpty() }
        if (server != null) return server
        return anCodes[widgetId]
    }

    fun pulseGroupDisplayCode(code: String, server: String?): String? {
        val trimmed = server?.trim()?.takeIf { it.isNotEmpty() }
        if (trimmed != null) return trimmed
        return pulseGroupAn[code.uppercase()]
    }

    fun sortWidgets(screen: String, widgets: List<OperatingWidgetDto>): List<OperatingWidgetDto> {
        val order = orderForScreen(screen) ?: return widgets
        val rank = order.withIndex().associate { (index, id) -> id to index }
        return widgets.sortedWith(
            compareBy<OperatingWidgetDto> { rank[it.widgetId] ?: Int.MAX_VALUE }
                .thenBy { it.widgetId },
        )
    }

    fun presentWidgets(screen: String, widgets: List<OperatingWidgetDto>): List<OperatingWidgetDto> =
        sortWidgets(screen, widgets).map { w ->
            w.copy(title = displayTitle(w.widgetId, w.title))
        }

    fun tabHeading(tab: OperatingTab): String = when (tab) {
        OperatingTab.MOMENTS -> "Campaigns & commitments"
        OperatingTab.LIFE -> "Today · live campaign"
        OperatingTab.MEMORY -> "Memory & patterns"
        else -> "Pulse"
    }

    fun buildHeroStats(model: OperatingProjectionDto?): String {
        if (model == null) return ""
        val parts = mutableListOf<String>()
        val target = model.target
        if (target != null && target.configured) {
            val metric = target.metric.replace('_', ' ').lowercase()
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            parts += "$metric ${target.progress} / ${target.value}"
        }
        val budget = model.budget
        if (budget != null && budget.configured && budget.amount.isNotBlank()) {
            val amount = budget.amount.toDoubleOrNull()
            val spent = budget.spent.toDoubleOrNull()
            if (amount != null && amount > 0 && spent != null) {
                val pct = ((spent / amount) * 1000).toInt() / 10.0
                parts += "Budget use $pct%"
            } else {
                parts += "Budget ${budget.amount}"
            }
        }
        model.marketing?.pacing?.takeIf { it.isNotBlank() }?.let { parts += it }
        return parts.joinToString(" · ")
    }

    fun ownCanvasBrush(): Brush = Brush.verticalGradient(
        listOf(
            BusinessTokens.Colors.MARKETING_OWNCanvasTop,
            BusinessTokens.Colors.MARKETING_OWNCanvas,
            BusinessTokens.Colors.MARKETING_OWNCanvasDeep,
        ),
    )
}

// region Headers

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OwnMarketingHeroHeader(
    projection: OperatingProjectionDto?,
    momentTitle: String?,
    palette: HubPalette,
) {
    val title = projection?.title?.takeIf { it.isNotBlank() }
        ?: momentTitle?.takeIf { it.isNotBlank() }
        ?: projection?.subtypeLabel?.takeIf { it.isNotBlank() }
        ?: "Own Business Campaign"

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "$title Pulse",
            color = palette.text,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )

        val owner = projection?.role?.replace('_', ' ')?.trim()?.takeIf { it.isNotEmpty() }
        val party = projection?.partyName?.trim()?.takeIf { it.isNotEmpty() }
        val subtitle = listOfNotNull(owner?.let { "Owner $it" }, party).joinToString(" · ")
        if (subtitle.isNotEmpty()) {
            Text(
                subtitle,
                color = BusinessTokens.Colors.MARKETING_OWNAccentSoft,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = PlusJakartaSans,
            )
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            OwnChip(label = "Own Business Campaign", selected = true)
            val campaign = projection?.title?.takeIf { it.isNotBlank() && it != title }
            if (campaign != null) OwnChip(label = campaign)
            val periodParts = listOfNotNull(
                projection?.startDate?.take(7)?.takeIf { it.isNotBlank() },
                projection?.endDate?.take(7)?.takeIf { it.isNotBlank() },
            ).distinct().sorted()
            val periodText = periodParts.joinToString("–")
            val currency = projection?.currency?.trim()?.takeIf { it.isNotEmpty() }
            val meta = listOfNotNull(periodText.takeIf { it.isNotEmpty() }, currency).joinToString(" · ")
            if (meta.isNotEmpty()) OwnChip(label = meta)
        }

        val tagline = projection?.tagline?.takeIf { it.isNotBlank() } ?: OwnMarketingHub.TAGLINE
        val heroStats = OwnMarketingHub.buildHeroStats(projection)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            BusinessTokens.Colors.MARKETING_OWNHeroStart,
                            BusinessTokens.Colors.MARKETING_OWNHeroEnd,
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(900f, 400f),
                    ),
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                "${title.uppercase()} · OWN BUSINESS",
                color = Color(0xFFF5D0FE),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
            )
            Text(
                tagline,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
            )
            if (heroStats.isNotEmpty()) {
                Text(
                    heroStats,
                    color = BusinessTokens.Colors.MARKETING_OWNLabel,
                    fontSize = 12.sp,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OwnMarketingTabHeader(
    tab: OperatingTab,
    projection: OperatingProjectionDto?,
    momentTitle: String?,
) {
    val heading = when (tab) {
        OperatingTab.MOMENTS -> "Moments"
        OperatingTab.LIFE -> "Life"
        OperatingTab.MEMORY -> "Memory"
        else -> "Pulse"
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            heading,
            color = Color(0xFFF8FAFC),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
        Text(
            OwnMarketingHub.tabHeading(tab),
            color = BusinessTokens.Colors.MARKETING_OWNAccentSoft,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = PlusJakartaSans,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            OwnChip(label = "Own Business Campaign", selected = true)
            val name = (projection?.title ?: momentTitle)?.trim()?.takeIf { it.isNotEmpty() }
            if (name != null) OwnChip(label = name)
        }
    }
}

@Composable
private fun OwnChip(label: String, selected: Boolean = false) {
    Text(
        label,
        color = if (selected) Color.White else Color(0xFF94A3B8),
        fontSize = 12.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        fontFamily = PlusJakartaSans,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) BusinessTokens.Colors.MARKETING_OWNAccent else Color(0xFF334155))
            .border(
                1.dp,
                if (selected) BusinessTokens.Colors.MARKETING_OWNAccentSoft else Color(0xFF475569),
                RoundedCornerShape(50),
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

// endregion

// region Section cards

@Composable
fun OwnMarketingWidgetCards(
    widgets: List<OperatingWidgetDto>,
    financeVisible: Boolean,
    palette: HubPalette,
    onOpenSources: (String) -> Unit,
    onViewAll: (() -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        widgets.forEach { w ->
            val cov = WidgetCoveragePresentation.fromServer(w.coverage)
            val metricLine = w.metrics.joinToString(" · ") { m ->
                val mCov = WidgetCoveragePresentation.fromServer(m.coverage)
                val valueText = when {
                    !mCov.showValue || m.value == null -> mCov.label
                    else -> "${m.value}${m.unit?.let { " $it" }.orEmpty()}"
                }
                "${m.label.ifBlank { m.key.ifBlank { "metric" } }}: $valueText"
            }
            OwnSectionCard(
                displayCode = OwnMarketingHub.displayCode(w.widgetId, w.displayCode),
                title = OwnMarketingHub.displayTitle(w.widgetId, w.title),
                detail = when {
                    !financeVisible && w.financeOnly -> "Finance redacted for your role"
                    metricLine.isBlank() -> cov.label
                    else -> metricLine
                },
                why = w.why,
                palette = palette,
                onViewAll = if (w.widgetId == "P04") onViewAll else null,
                onClick = if (cov.showSources || w.sourceEventIds.isNotEmpty()) {
                    { onOpenSources(w.widgetId) }
                } else {
                    null
                },
            )
        }
    }
}

@Composable
fun OwnMarketingPulseGroups(
    groups: List<OperatingPulseMetricGroupDto>,
    palette: HubPalette,
    onViewAllAttention: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        groups.forEach { group ->
            OwnSectionCard(
                displayCode = OwnMarketingHub.pulseGroupDisplayCode(group.code, group.displayCode),
                title = ownPulseGroupTitle(group.code),
                metrics = group.metrics.map { m ->
                    (m.label.ifBlank { m.key }) to m.value
                },
                why = group.why,
                palette = palette,
                onViewAll = if (group.code.equals("ATTENTION", ignoreCase = true)) {
                    onViewAllAttention
                } else {
                    null
                },
            )
        }
    }
}

@Composable
fun OwnMarketingBoardSections(
    sections: List<OperatingBoardSectionDto>,
    heading: String,
    palette: HubPalette,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            heading,
            color = palette.text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = PlusJakartaSans,
        )
        if (sections.isEmpty()) {
            OwnSectionCard(
                displayCode = null,
                title = "This campaign",
                detail = "Nothing recorded yet.",
                palette = palette,
            )
        } else {
            sections.forEach { section ->
                OwnSectionCard(
                    displayCode = section.displayCode,
                    title = section.title,
                    detail = section.detail.ifBlank { "Nothing recorded yet." },
                    metrics = section.metrics.map { m ->
                        (m.label.ifBlank { m.key }) to m.value
                    },
                    rows = section.rows.map { row ->
                        val amount = row.amount?.let { " · $it" }.orEmpty()
                        val badge = row.badge ?: row.eventType.replace('_', ' ')
                        (row.title + amount) to badge
                    },
                    why = section.why,
                    palette = palette,
                )
            }
        }
    }
}

private fun ownPulseGroupTitle(code: String): String = when (code.uppercase()) {
    "TARGET_BUDGET" -> "Target & budget"
    "FINANCE" -> "Finance"
    "PAYMENT_READINESS" -> "Payment readiness"
    "ATTENTION" -> "Needs Attention"
    "ACTIVITY" -> "Activity"
    else -> code.replace('_', ' ')
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OwnSectionCard(
    displayCode: String?,
    title: String,
    detail: String? = null,
    metrics: List<Pair<String, String>> = emptyList(),
    rows: List<Pair<String, String>> = emptyList(),
    why: String? = null,
    palette: HubPalette,
    onViewAll: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(palette.card)
            .border(1.dp, palette.border, RoundedCornerShape(20.dp))
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false),
            ) {
                displayCode?.trim()?.takeIf { it.isNotEmpty() }?.let { code ->
                    Text(
                        code,
                        color = BusinessTokens.Colors.MARKETING_OWNAccentSoft,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                    )
                }
                Text(
                    title,
                    color = palette.text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                )
            }
            if (onViewAll != null) {
                Text(
                    "View all ›",
                    color = BusinessTokens.Colors.MARKETING_OWNAccentSoftLink,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier
                        .clickable(onClick = onViewAll)
                        .padding(start = 8.dp),
                )
            }
        }
        if (metrics.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                metrics.forEach { (label, value) ->
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(BusinessTokens.Colors.MARKETING_OWNInset)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        Text(label, color = palette.muted, fontSize = 11.sp, fontFamily = PlusJakartaSans)
                        Text(
                            value,
                            color = palette.text,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = PlusJakartaSans,
                        )
                    }
                }
            }
        }
        detail?.takeIf { it.isNotBlank() }?.let {
            Text(it, color = palette.muted, fontSize = 12.sp, fontFamily = PlusJakartaSans)
        }
        rows.forEach { (rowTitle, badge) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(BusinessTokens.Colors.MARKETING_OWNInset)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    rowTitle,
                    color = palette.text,
                    fontSize = 13.sp,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier.weight(1f),
                )
                Text(badge, color = palette.accent, fontSize = 11.sp, fontFamily = PlusJakartaSans)
            }
        }
        why?.takeIf { it.isNotBlank() }?.let {
            Text(it, color = palette.muted, fontSize = 12.sp, fontFamily = PlusJakartaSans)
        }
    }
}

// endregion

// region Quick Adds

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OwnMarketingQuickAddHub(
    projection: OperatingProjectionDto?,
    palette: HubPalette,
    initialCode: String? = null,
    onSubmit: suspend (eventType: String, title: String, amount: String?) -> Unit,
) {
    val writeAdds = projection?.quickAdds.orEmpty()
    val hub = projection?.quickAddHub
    val hubTiles = hub?.groups.orEmpty().flatMap { it.tiles }
    var selectedCode by remember(writeAdds, hubTiles, initialCode) {
        mutableStateOf(
            initialCode?.takeIf { code ->
                writeAdds.any { it.code.equals(code, true) } ||
                    hubTiles.any { it.code.equals(code, true) || it.mapsTo.equals(code, true) }
            } ?: writeAdds.firstOrNull()?.code
                ?: hubTiles.firstOrNull { it.kind.equals("WRITE", true) }?.code.orEmpty(),
        )
    }
    var titleText by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var showSavedToast by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val selectedWrite: OperatingQuickAddDto? = writeAdds.firstOrNull {
        it.code.equals(selectedCode, ignoreCase = true)
    } ?: run {
        val tile = hubTiles.firstOrNull {
            it.code.equals(selectedCode, true) && it.kind.equals("WRITE", true)
        }
        tile?.let { t -> writeAdds.firstOrNull { it.code == t.code } }
    }

    val needsAmount = selectedWrite?.needsAmount == true ||
        hubTiles.firstOrNull { it.code.equals(selectedCode, true) }?.needsAmount == true

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (showSavedToast) {
            OwnQuickAddSavedToast(onDismiss = { showSavedToast = false })
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(palette.card)
                .border(1.dp, palette.border, RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "QUICK ADDS · v5",
                color = BusinessTokens.Colors.MARKETING_OWNAccentSoft,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
            )
            Text(
                "Quick Adds",
                color = palette.text,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
            )
            Text(
                projection?.tagline?.takeIf { it.isNotBlank() } ?: OwnMarketingHub.TAGLINE,
                color = palette.accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
            val meta = listOfNotNull(
                projection?.partyName?.takeIf { it.isNotBlank() },
                projection?.title?.takeIf { it.isNotBlank() },
                "Own",
                projection?.currency?.takeIf { it.isNotBlank() },
            ).joinToString(" · ")
            if (meta.isNotEmpty()) {
                Text(meta, color = palette.muted, fontSize = 12.sp, fontFamily = PlusJakartaSans)
            }
            Text(
                "Add Activity",
                color = palette.onAccent,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.accent)
                    .clickable { selectedCode = "ACTIVITY" }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            )
        }

        val groups = hub?.groups.orEmpty()
        if (groups.isNotEmpty()) {
            groups.forEach { group ->
                Text(
                    group.title,
                    color = palette.muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    group.tiles.forEach { tile ->
                        val selected = selectedCode.equals(tile.code, true) ||
                            tile.mapsTo?.equals(selectedCode, true) == true
                        OwnHubTile(
                            tile = tile,
                            selected = selected,
                            palette = palette,
                            onClick = {
                                // Chrome tiles are NAV only — never invent HOLD/TRANSFER write APIs.
                                when {
                                    tile.chromeOnly && !tile.mapsTo.isNullOrBlank() ->
                                        selectedCode = tile.mapsTo!!
                                    tile.kind.equals("WRITE", true) ->
                                        selectedCode = tile.code
                                    !tile.mapsTo.isNullOrBlank() ->
                                        selectedCode = tile.mapsTo!!
                                }
                            },
                        )
                    }
                }
            }
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                writeAdds.forEach { add ->
                    val on = add.code.equals(selectedCode, true)
                    Text(
                        add.label,
                        color = if (on) palette.accent else palette.text,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (on) palette.accent.copy(alpha = 0.18f) else palette.card)
                            .border(1.dp, if (on) palette.accent else palette.border, RoundedCornerShape(16.dp))
                            .clickable { selectedCode = add.code }
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(BusinessTokens.Colors.MARKETING_OWNInset.copy(alpha = 0.55f))
                .border(
                    1.dp,
                    BusinessTokens.Colors.MARKETING_OWNAccentSoft.copy(alpha = 0.35f),
                    RoundedCornerShape(16.dp),
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                "Why these tiles save differently",
                color = BusinessTokens.Colors.MARKETING_OWNAccentSoft,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
            )
            Text(
                "• Attributed sales ≠ ledger revenue\n• Results are measured, never booked as income\n• Invoice & Collection stay Client-only",
                color = Color(0xFFFCD34D),
                fontSize = 12.sp,
                fontFamily = PlusJakartaSans,
            )
        }

        val showWriteForm = selectedWrite != null ||
            writeAdds.any { it.code.equals(selectedCode, true) }
        if (showWriteForm) {
            val writeCode = selectedWrite?.code ?: selectedCode
            BasicTextField(
                value = titleText,
                onValueChange = { titleText = it },
                textStyle = TextStyle(color = palette.text, fontSize = 14.sp, fontFamily = PlusJakartaSans),
                cursorBrush = SolidColor(palette.accent),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.card)
                    .border(1.dp, palette.border, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                decorationBox = { inner ->
                    if (titleText.isEmpty()) {
                        Text("Title", color = palette.muted, fontSize = 14.sp, fontFamily = PlusJakartaSans)
                    }
                    inner()
                },
            )
            if (needsAmount) {
                BasicTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    textStyle = TextStyle(color = palette.text, fontSize = 14.sp, fontFamily = PlusJakartaSans),
                    cursorBrush = SolidColor(palette.accent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.card)
                        .border(1.dp, palette.border, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    decorationBox = { inner ->
                        if (amountText.isEmpty()) {
                            Text("Amount", color = palette.muted, fontSize = 14.sp, fontFamily = PlusJakartaSans)
                        }
                        inner()
                    },
                )
            }
            val canSubmit = writeCode.isNotBlank() && titleText.trim().isNotEmpty()
            Text(
                "Save",
                color = palette.onAccent,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (canSubmit) palette.accent else palette.muted.copy(alpha = 0.35f))
                    .clickable(enabled = canSubmit) {
                        val title = titleText.trim()
                        val amount = amountText.trim().ifBlank { null }
                        scope.launch {
                            onSubmit(writeCode, title, amount)
                            titleText = ""
                            amountText = ""
                            showSavedToast = true
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun OwnHubTile(
    tile: OperatingQuickAddHubTileDto,
    selected: Boolean,
    palette: HubPalette,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) palette.accent.copy(alpha = 0.18f) else palette.card)
            .border(1.dp, if (selected) palette.accent else palette.border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            tile.label,
            color = if (selected) palette.accent else palette.text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
        tile.subtitle?.takeIf { it.isNotBlank() }?.let {
            Text(it, color = palette.muted, fontSize = 10.sp, fontFamily = PlusJakartaSans)
        }
        if (tile.chromeOnly) {
            Text("Open", color = palette.muted, fontSize = 10.sp, fontFamily = PlusJakartaSans)
        }
    }
}

@Composable
fun OwnGenerateBusinessStoryButton(
    @Suppress("UNUSED_PARAMETER") palette: HubPalette,
    onGenerateStory: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF4F46E5), Color(0xFF7C3AED)),
                ),
            )
            .clickable(onClick = onGenerateStory)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            "Generate Business Story",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
        )
        Text(
            "Eight pages from recorded activity",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 12.sp,
            fontFamily = PlusJakartaSans,
        )
    }
}

@Composable
fun OwnQuickAddSavedToast(onDismiss: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "own-saved-pulse")
    val scale by pulse.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "own-saved-scale",
    )
    LaunchedEffect(Unit) {
        delay(2_750)
        onDismiss()
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF16120E).copy(alpha = 0.95f))
            .border(1.dp, Color(0xFF10B981).copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(Color(0xFF10B981).copy(alpha = 0.22f))
                .border(1.5.dp, Color(0xFF10B981), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("✓", color = Color(0xFF10B981), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "Saved · posted once",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
            )
            Text(
                "Pulse · Moments · Life · Memory updated",
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                fontFamily = PlusJakartaSans,
            )
        }
        Text(
            "View",
            color = Color(0xFF10B981),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
            modifier = Modifier.clickable(onClick = onDismiss),
        )
    }
}

// endregion
