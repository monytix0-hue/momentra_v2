package com.example.momentra.ui.shell.business.shared

/**
 * Callers: AppShellScreen.kt ShellDestinationContent (Business Ready tabs).
 * Prior industry Active* composables were removed; only an unusable sync-conflict copy remained.
 * Consumes GET /operating + /widgets + /widgets/:id/sources; posts Quick Adds via
 * recordOperatingEvent — no client-side finance totaling.
 * Reads: OperatingProjectionDto, WidgetsProjectionDto, WidgetSourcesDto. Writes: none locally.
 * User: "Wire Pulse, Moments, Life and Memory to real backend projections." +
 * "Ensure every widget can navigate to its eligible source records."
 */

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.OperatingBoardSectionDto
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.data.api.OperatingPulseMetricGroupDto
import com.example.momentra.data.api.OperatingWidgetDto
import com.example.momentra.data.api.WidgetSourcesDto
import com.example.momentra.data.api.WidgetsProjectionDto
import com.example.momentra.data.repository.BusinessSliceRepository
import com.example.momentra.domain.BottomDestination
import com.example.momentra.ui.shell.business.appstate.AppStateErrorCodePill
import com.example.momentra.ui.shell.business.appstate.AppStateEyebrow
import com.example.momentra.ui.shell.business.appstate.AppStateGlassCard
import com.example.momentra.ui.shell.business.appstate.AppStateSecondaryButton
import com.example.momentra.ui.shell.business.appstate.BusinessAppStateAccent
import com.example.momentra.ui.shell.business.appstate.BusinessAppStateTokens
import com.example.momentra.ui.theme.PlusJakartaSans
import com.example.momentra.ui.theme.generated.BusinessTokens
import kotlinx.coroutines.launch

private data class HubPalette(
    val bg: Color,
    val card: Color,
    val border: Color,
    val muted: Color,
    val text: Color,
    val accent: Color,
    val onAccent: Color,
)

private fun hubPaletteFor(typeCode: String?, familyCode: String?): HubPalette {
    val trading = typeCode?.uppercase() in setOf("TRADING_WHOLESALE", "DISTRIBUTION_DEALERSHIP") ||
        familyCode == "TRADING_DISTRIBUTION"
    return if (trading) {
        HubPalette(
            bg = BusinessTokens.Colors.TRADINGCanvas,
            card = Color(0xFF111827),
            border = BusinessTokens.Colors.TRADINGAccent.copy(alpha = 0.28f),
            muted = Color(0xFF94A3B8),
            text = Color(0xFFF1F5F9),
            accent = BusinessTokens.Colors.TRADINGAccent,
            onAccent = BusinessTokens.Colors.TRADINGOnAccent,
        )
    } else {
        val accent = BusinessAppStateAccent.forTypeCode(typeCode).color
        HubPalette(
            bg = Color(0xFF0C0F15),
            card = Color(0xFF161B26),
            border = Color(0xFF1E293B),
            muted = Color(0xFF94A3B8),
            text = Color(0xFFE5E0EE),
            accent = accent,
            onAccent = Color(0xFF0C0F15),
        )
    }
}

private fun pulseGroupTitle(code: String): String = when (code.uppercase()) {
    "TARGET_BUDGET" -> "Target & budget"
    "FINANCE" -> "Finance"
    "PAYMENT_READINESS" -> "Payment readiness"
    "ATTENTION" -> "Needs Attention"
    "ACTIVITY" -> "Activity"
    "GST" -> "GST"
    else -> code.replace('_', ' ')
}

enum class OperatingTab { PULSE, MOMENTS, LIFE, MEMORY, QUICK_ADD }

object OperatingProjectionCache {
    private val stored = java.util.concurrent.ConcurrentHashMap<String, OperatingProjectionDto>()

    fun peek(momentId: String): OperatingProjectionDto? = stored[momentId]

    fun put(momentId: String, payload: OperatingProjectionDto) {
        stored[momentId] = payload
    }

    fun invalidate(momentId: String) {
        stored.remove(momentId)
    }
}

fun operatingTabFor(destination: BottomDestination): OperatingTab = when (destination) {
    BottomDestination.MOMENTS -> OperatingTab.MOMENTS
    BottomDestination.LIFE -> OperatingTab.LIFE
    BottomDestination.MEMORY -> OperatingTab.MEMORY
    BottomDestination.CREATE -> OperatingTab.QUICK_ADD
    else -> OperatingTab.PULSE
}

private fun screenCode(tab: OperatingTab): String? = when (tab) {
    OperatingTab.PULSE -> "PULSE"
    OperatingTab.MOMENTS -> "MOMENTS"
    OperatingTab.LIFE -> "LIFE"
    OperatingTab.MEMORY -> "MEMORY"
    OperatingTab.QUICK_ADD -> null
}

@Composable
fun OperatingMomentPage(
    momentId: String?,
    momentTypeCode: String?,
    momentTitle: String?,
    tab: OperatingTab,
    refreshToken: Long,
    onCreateMoment: () -> Unit,
    onOpenQuickAdd: () -> Unit = {},
    onViewAllAttention: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (momentId.isNullOrBlank()) {
        RetiredOrMissingMoment(momentTitle, onCreateMoment, modifier)
        return
    }

    var projection by remember(momentId) { mutableStateOf(OperatingProjectionCache.peek(momentId)) }
    var widgets by remember(momentId) { mutableStateOf<WidgetsProjectionDto?>(null) }
    var sources by remember { mutableStateOf<WidgetSourcesDto?>(null) }
    var drillWidgetId by remember { mutableStateOf<String?>(null) }
    var error by remember(momentId) { mutableStateOf<String?>(null) }
    var confirm by remember { mutableStateOf<String?>(null) }
    var tick by remember(momentId) { mutableIntStateOf(0) }
    var loading by remember(momentId) { mutableStateOf(projection == null) }
    val repo = remember { BusinessSliceRepository() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(momentId, refreshToken, tick) {
        loading = projection == null
        repo.getOperating(momentId).fold(
            onSuccess = {
                OperatingProjectionCache.put(momentId, it)
                projection = it
                error = null
            },
            onFailure = {
                if (projection == null) error = it.message ?: "Could not load this moment"
            },
        )
        repo.getWidgets(momentId).fold(
            onSuccess = { widgets = it },
            onFailure = { },
        )
        loading = false
        val openId = drillWidgetId
        if (openId != null) {
            repo.getWidgetSources(momentId, openId).fold(
                onSuccess = { sources = it },
                onFailure = { sources = null },
            )
        }
    }

    val model = projection
    if (model?.retired == true) {
        RetiredOrMissingMoment(model.title ?: momentTitle, onCreateMoment, modifier, momentTypeCode)
        return
    }

    val palette = hubPaletteFor(momentTypeCode ?: model?.subtype, model?.familyCode)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.bg)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            familyKicker(model?.familyCode),
            color = palette.accent,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = PlusJakartaSans,
        )
        Text(
            model?.subtypeLabel ?: momentTitle ?: "Business moment",
            color = palette.text,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            fontFamily = PlusJakartaSans,
        )
        val typeCode = (momentTypeCode ?: model?.subtype)?.uppercase()
        if (typeCode == "CLIENT_CAMPAIGN") {
            Text(
                "Plan. Launch. Measure.",
                color = palette.accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
        }
        Text(model?.partyName.orEmpty(), color = palette.muted, fontSize = 13.sp, fontFamily = PlusJakartaSans)
        if (!model?.role.isNullOrBlank()) {
            Text(
                model!!.role!!.replace('_', ' '),
                color = palette.accent,
                fontSize = 12.sp,
                fontFamily = PlusJakartaSans,
            )
        }

        if (loading && model == null) {
            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = palette.accent)
            }
        }

        if (error != null && model == null) {
            AppStateGlassCard(strokeAlpha = 0.40f) {
                AppStateEyebrow("COULDN'T LOAD THIS MOMENT", BusinessAppStateTokens.ErrorRing)
                Text(error ?: "", color = Color.White.copy(alpha = 0.70f), fontSize = 13.sp, fontFamily = PlusJakartaSans)
                AppStateErrorCodePill(code = "MOMENT_DETAIL_FAILED", accent = BusinessAppStateTokens.ErrorRing)
                AppStateSecondaryButton(label = "Retry", onClick = { tick += 1 })
            }
        }

        confirm?.let { InfoCard("Saved", it, palette) }

        when (tab) {
            OperatingTab.PULSE,
            OperatingTab.MOMENTS,
            OperatingTab.LIFE,
            OperatingTab.MEMORY,
            -> {
                val screen = screenCode(tab)!!
                val screenWidgets = widgets?.widgets.orEmpty().filter { it.screen.equals(screen, ignoreCase = true) }
                val tabWidgets = if (typeCode == ClientCampaignHubWidgets.TYPE_CODE) {
                    ClientCampaignHubWidgets.presentForClient(screen, screenWidgets)
                } else {
                    screenWidgets
                }
                WidgetBlocks(
                    widgets = tabWidgets,
                    financeVisible = model?.financeVisible == true,
                    palette = palette,
                    onOpenSources = { id ->
                        drillWidgetId = id
                        scope.launch {
                            repo.getWidgetSources(momentId, id).fold(
                                onSuccess = { sources = it },
                                onFailure = { sources = null },
                            )
                        }
                    },
                )
                if (tab == OperatingTab.PULSE) {
                    val skipPulseFallback = typeCode == ClientCampaignHubWidgets.TYPE_CODE && tabWidgets.isNotEmpty()
                    if (!skipPulseFallback) {
                        PulseFallback(model, palette, onViewAllAttention)
                    }
                } else {
                    val sections = when (tab) {
                        OperatingTab.MOMENTS -> model?.boards?.moments
                        OperatingTab.LIFE -> model?.boards?.life
                        else -> model?.boards?.memory
                    }
                    BoardSections(sections, momentsHeading(model?.familyCode, tab), palette)
                }
            }
            OperatingTab.QUICK_ADD -> TypedOperatingQuickAddForm(
                model = model,
                palette = palette,
                onSubmit = { eventType, title, amount ->
                    repo.recordOperatingEvent(
                        momentId = momentId,
                        eventType = eventType,
                        title = title,
                        amount = amount,
                        note = null,
                    ).fold(
                        onSuccess = {
                            confirm = "Saved $title"
                            OperatingProjectionCache.invalidate(momentId)
                            tick += 1
                        },
                        onFailure = { error = it.message },
                    )
                },
            )
        }

        sources?.let { src ->
            val cov = WidgetCoveragePresentation.fromServer(src.coverage)
            InfoCard(
                "Sources · ${src.widgetId}",
                buildString {
                    append(cov.label)
                    if (cov.showValue && src.reconciledTotal != null) {
                        append(" · server total ")
                        append(src.reconciledTotal)
                    }
                    append(" · ")
                    append(src.formula)
                },
                palette,
            )
            src.sources.forEach { row ->
                InfoCard(
                    row.eventType.replace('_', ' '),
                    listOfNotNull(row.title, row.amount, row.occurredAt).joinToString(" · "),
                    palette,
                )
            }
            if (src.sources.isEmpty()) {
                InfoCard("Sources", "No eligible source records for this widget.", palette)
            }
            AppStateSecondaryButton(label = "Close sources", onClick = {
                drillWidgetId = null
                sources = null
            })
        }

        if (tab != OperatingTab.QUICK_ADD) {
            AppStateSecondaryButton(label = "Quick Adds", onClick = onOpenQuickAdd)
        }
    }
}

@Composable
private fun RetiredOrMissingMoment(
    title: String?,
    onCreateMoment: () -> Unit,
    modifier: Modifier = Modifier,
    momentTypeCode: String? = null,
) {
    val palette = hubPaletteFor(momentTypeCode, null)
    Column(
        modifier = modifier.fillMaxSize().background(palette.bg).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title ?: "Business moment", color = palette.text, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, fontFamily = PlusJakartaSans)
        Text(
            "Select or create a Marketing, Trading, Retail, Distribution, or Manufacturing moment.",
            color = palette.muted,
            fontSize = 14.sp,
            fontFamily = PlusJakartaSans,
        )
        Text(
            "Create a moment",
            color = palette.onAccent,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(palette.accent)
                .clickable(onClick = onCreateMoment)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            fontFamily = PlusJakartaSans,
        )
    }
}

private fun familyKicker(family: String?): String = when (family) {
    "TRADING_DISTRIBUTION" -> "TRADING & DISTRIBUTION"
    "MARKETING_CAMPAIGNS" -> "MARKETING & CAMPAIGNS"
    "MANUFACTURING_PRODUCTION" -> "MANUFACTURING & PRODUCTION"
    else -> "RETAIL & CONSUMER"
}

private fun momentsHeading(family: String?, tab: OperatingTab): String = when (tab) {
    OperatingTab.LIFE -> "How this moment connects"
    OperatingTab.MEMORY -> "What stays"
    else -> when (family) {
        "TRADING_DISTRIBUTION" -> "Sales, purchases, and movement"
        "MARKETING_CAMPAIGNS" -> "Spend, results, and delivery"
        "MANUFACTURING_PRODUCTION" -> "Production, orders, and delivery"
        else -> "Sales, stock, and service"
    }
}

@Composable
private fun WidgetBlocks(
    widgets: List<OperatingWidgetDto>,
    financeVisible: Boolean,
    palette: HubPalette,
    onOpenSources: (String) -> Unit,
) {
    if (widgets.isEmpty()) return
    Text("Widgets", color = palette.text, fontWeight = FontWeight.Medium, fontFamily = PlusJakartaSans)
    widgets.forEach { w ->
        val cov = WidgetCoveragePresentation.fromServer(w.coverage)
        val metricLine = w.metrics.joinToString(" · ") { m ->
            val mCov = WidgetCoveragePresentation.fromServer(m.coverage)
            val valueText = when {
                !mCov.showValue || m.value == null -> mCov.label
                else -> "${m.value}${m.unit?.let { " $it" }.orEmpty()}"
            }
            "${m.label}: $valueText"
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(palette.card)
                .border(1.dp, palette.border, RoundedCornerShape(16.dp))
                .clickable(enabled = cov.showSources || w.sourceEventIds.isNotEmpty()) {
                    onOpenSources(w.widgetId)
                }
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(w.title.ifBlank { w.widgetId }, color = palette.muted, fontSize = 11.sp, fontFamily = PlusJakartaSans)
                Text(cov.label, color = palette.accent, fontSize = 11.sp, fontFamily = PlusJakartaSans)
            }
            Text(
                if (!financeVisible && w.financeOnly) "Finance redacted for your role" else metricLine.ifBlank { cov.label },
                color = palette.text,
                fontSize = 14.sp,
                fontFamily = PlusJakartaSans,
            )
            w.why?.takeIf { it.isNotBlank() }?.let {
                Text(it, color = palette.muted, fontSize = 12.sp, fontFamily = PlusJakartaSans)
            }
        }
    }
}

@Composable
private fun BoardSections(
    sections: List<OperatingBoardSectionDto>?,
    heading: String,
    palette: HubPalette,
) {
    Text(heading, color = palette.text, fontWeight = FontWeight.Medium, fontFamily = PlusJakartaSans)
    val blocks = sections.orEmpty()
    if (blocks.isEmpty()) InfoCard("This moment", "Nothing recorded yet.", palette)
    blocks.forEach { section ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(palette.card)
                .border(1.dp, palette.border, RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(section.title, color = palette.text, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = PlusJakartaSans)
            Text(
                section.detail.ifBlank { "Nothing recorded yet." },
                color = palette.muted,
                fontSize = 12.sp,
                fontFamily = PlusJakartaSans,
            )
            if (section.metrics.isNotEmpty()) {
                MetricTileRow(section.metrics.map { it.label to it.value }, palette)
            }
            section.rows.forEach { row ->
                val amount = row.amount?.let { " · $it" }.orEmpty()
                AttentionRow(
                    title = row.title + amount,
                    badge = row.badge ?: row.eventType.replace('_', ' '),
                    palette = palette,
                )
            }
        }
    }
}

@Composable
private fun PulseFallback(
    model: OperatingProjectionDto?,
    palette: HubPalette,
    onViewAllAttention: () -> Unit,
) {
    val groups = model?.pulseMetrics.orEmpty()
    if (groups.isNotEmpty()) {
        groups.forEach { group ->
            PulseMetricGroupCard(group, palette, onViewAllAttention)
        }
        return
    }
    if (model?.financeVisible == true) {
        model.budget?.let { budget ->
            val body = if (budget.configured) {
                "${budget.remaining} left of ${budget.amount} ${model.currency.orEmpty()}"
            } else {
                "Not recorded"
            }
            InfoCard("Budget", body, palette)
        }
        model.target?.let { target ->
            val body = if (target.configured) {
                "${target.progress} of ${target.value} (${target.metric})"
            } else {
                "Not recorded"
            }
            InfoCard("Target", body, palette)
        }
        model.paymentReadiness.forEach { item ->
            InfoCard("Payment Readiness", "${item.title} · ${item.amount} · ${item.status}", palette)
        }
    } else {
        InfoCard("Your view", "Finance totals stay with Finance, Approver, and Owner.", palette)
    }
    model?.attention?.forEach { AttentionRow(it.title, it.kind, palette) }
    if (model?.attention.isNullOrEmpty()) {
        InfoCard("Needs Attention", "Nothing is waiting.", palette)
    } else {
        AppStateSecondaryButton(label = "View all", onClick = onViewAllAttention)
    }
    val recent = model?.activity.orEmpty().take(5)
    if (recent.isNotEmpty()) {
        Text("Activity", color = palette.text, fontWeight = FontWeight.Medium, fontFamily = PlusJakartaSans)
        recent.forEach { row ->
            val amount = row.amount?.let { " · $it" }.orEmpty()
            InfoCard(row.eventType, row.title + amount, palette)
        }
    }
}

@Composable
private fun PulseMetricGroupCard(
    group: OperatingPulseMetricGroupDto,
    palette: HubPalette,
    onViewAllAttention: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(palette.card)
            .border(1.dp, palette.border, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            pulseGroupTitle(group.code),
            color = palette.text,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            fontFamily = PlusJakartaSans,
        )
        MetricTileRow(group.metrics.map { it.label to it.value }, palette)
        group.why?.takeIf { it.isNotBlank() }?.let {
            Text(it, color = palette.muted, fontSize = 12.sp, fontFamily = PlusJakartaSans)
        }
        if (group.code.equals("ATTENTION", ignoreCase = true)) {
            AppStateSecondaryButton(label = "View all", onClick = onViewAllAttention)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MetricTileRow(tiles: List<Pair<String, String>>, palette: HubPalette) {
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
                Text(value.ifBlank { "Not recorded" }, color = palette.text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, fontFamily = PlusJakartaSans)
            }
        }
    }
}

@Composable
private fun AttentionRow(title: String, badge: String, palette: HubPalette) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(palette.bg)
            .border(1.dp, palette.border, RoundedCornerShape(12.dp))
            .padding(start = 0.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            modifier = Modifier
                .height(36.dp)
                .padding(end = 12.dp)
                .border(width = 3.dp, color = palette.accent, shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                .padding(start = 14.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(title, color = palette.text, fontSize = 13.sp, fontFamily = PlusJakartaSans)
        }
        Text(badge, color = palette.accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
    }
}

@Composable
private fun InfoCard(label: String, body: String, palette: HubPalette) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(palette.card)
            .border(1.dp, palette.border, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, color = palette.muted, fontSize = 11.sp, fontFamily = PlusJakartaSans)
        Text(body, color = palette.text, fontSize = 14.sp, fontFamily = PlusJakartaSans)
    }
}

/**
 * Figma TRD Quick Adds hub (`5:16417`) — catalog tile grid + compact post form.
 * Posts title/amount only; rich typed capture can layer later without inventing values.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TypedOperatingQuickAddForm(
    model: OperatingProjectionDto?,
    palette: HubPalette,
    onSubmit: suspend (eventType: String, title: String, amount: String?) -> Unit,
) {
    val adds = model?.quickAdds.orEmpty()
    var selected by remember(adds) { mutableStateOf(adds.firstOrNull()?.code.orEmpty()) }
    var titleText by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val selectedAdd = adds.firstOrNull { it.code == selected }

    Text("Quick Adds", color = palette.text, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, fontFamily = PlusJakartaSans)
    Text(
        "Post a real capture — empty tiles stay honest until you add.",
        color = palette.muted,
        fontSize = 12.sp,
        fontFamily = PlusJakartaSans,
    )
    if (adds.isEmpty()) {
        InfoCard("Quick Adds", "Your role cannot add to this moment.", palette)
        return
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        adds.forEach { add ->
            val on = add.code == selected
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (on) palette.accent.copy(alpha = 0.18f) else palette.card)
                    .border(1.dp, if (on) palette.accent else palette.border, RoundedCornerShape(16.dp))
                    .clickable { selected = add.code }
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(add.label, color = if (on) palette.accent else palette.text, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
            }
        }
    }
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
            if (titleText.isEmpty()) Text("Title", color = palette.muted, fontSize = 14.sp, fontFamily = PlusJakartaSans)
            inner()
        },
    )
    if (selectedAdd?.needsAmount == true) {
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
                if (amountText.isEmpty()) Text("Amount", color = palette.muted, fontSize = 14.sp, fontFamily = PlusJakartaSans)
                inner()
            },
        )
    }
    val canSubmit = selected.isNotBlank() && titleText.isNotBlank()
    Text(
        "Add",
        color = palette.onAccent,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (canSubmit) palette.accent else palette.muted.copy(alpha = 0.35f))
            .clickable(enabled = canSubmit) {
                val code = selected
                val title = titleText.trim()
                val amount = amountText.trim().ifBlank { null }
                scope.launch {
                    onSubmit(code, title, amount)
                    titleText = ""
                    amountText = ""
                }
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        fontFamily = PlusJakartaSans,
    )
}
