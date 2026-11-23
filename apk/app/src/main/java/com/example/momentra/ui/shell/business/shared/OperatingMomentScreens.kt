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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.example.momentra.data.api.OperatingWidgetDto
import com.example.momentra.data.api.WidgetSourcesDto
import com.example.momentra.data.api.WidgetsProjectionDto
import com.example.momentra.data.repository.BusinessSliceRepository
import com.example.momentra.domain.BottomDestination
import com.example.momentra.ui.shell.business.appstate.AppStateErrorCodePill
import com.example.momentra.ui.shell.business.appstate.AppStateEyebrow
import com.example.momentra.ui.shell.business.appstate.AppStateGlassCard
import com.example.momentra.ui.shell.business.appstate.AppStateSecondaryButton
import com.example.momentra.ui.shell.business.appstate.BusinessAppStateTokens
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.launch

private val Bg = Color(0xFF0C0F15)
private val Card = Color(0xFF161B26)
private val Border = Color(0xFF1E293B)
private val Muted = Color(0xFF94A3B8)
private val TextPrimary = Color(0xFFE5E0EE)
private val Accent = Color(0xFF34D399)

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
        RetiredOrMissingMoment(model.title ?: momentTitle, onCreateMoment, modifier)
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Bg)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            familyKicker(model?.familyCode),
            color = Accent,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = PlusJakartaSans,
        )
        Text(
            model?.subtypeLabel ?: momentTitle ?: "Business moment",
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            fontFamily = PlusJakartaSans,
        )
        Text(model?.partyName.orEmpty(), color = Muted, fontSize = 13.sp, fontFamily = PlusJakartaSans)
        if (!model?.role.isNullOrBlank()) {
            Text(
                model!!.role!!.replace('_', ' '),
                color = Accent,
                fontSize = 12.sp,
                fontFamily = PlusJakartaSans,
            )
        }

        if (loading && model == null) {
            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Accent)
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

        confirm?.let { InfoCard("Saved", it) }

        when (tab) {
            OperatingTab.PULSE,
            OperatingTab.MOMENTS,
            OperatingTab.LIFE,
            OperatingTab.MEMORY,
            -> {
                val screen = screenCode(tab)!!
                WidgetBlocks(
                    widgets = widgets?.widgets.orEmpty().filter { it.screen.equals(screen, ignoreCase = true) },
                    financeVisible = model?.financeVisible == true,
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
                    PulseFallback(model, onViewAllAttention)
                } else {
                    val sections = when (tab) {
                        OperatingTab.MOMENTS -> model?.boards?.moments
                        OperatingTab.LIFE -> model?.boards?.life
                        else -> model?.boards?.memory
                    }
                    BoardSections(sections, momentsHeading(model?.familyCode, tab))
                }
            }
            // Callers: CREATE tab. Typed forms replace generic title/amount fallback.
            // User: "No generic fallback form for Quick Adds requiring typed data."
            OperatingTab.QUICK_ADD -> TypedOperatingQuickAddForm(
                model = model,
                onSubmit = { eventType, title, amount, note, channel, counterparty, paymentStatus,
                    parentEventId, quantity, unit, lines, dueAt, contract, metricCode,
                    activityStatus, attributedSales, revisionKind, rejectedQuantity, costBasis,
                    periodStart, periodEnd, idempotencyKey ->
                    repo.recordOperatingEvent(
                        momentId = momentId,
                        eventType = eventType,
                        title = title,
                        amount = amount,
                        note = note,
                        channelName = channel,
                        counterpartyName = counterparty,
                        paymentStatus = paymentStatus,
                        parentEventId = parentEventId,
                        quantity = quantity,
                        unit = unit,
                        lines = lines,
                        dueAt = dueAt,
                        contract = contract,
                        metricCode = metricCode,
                        activityStatus = activityStatus,
                        attributedSales = attributedSales,
                        revisionKind = revisionKind,
                        rejectedQuantity = rejectedQuantity,
                        costBasis = costBasis,
                        periodStart = periodStart,
                        periodEnd = periodEnd,
                        idempotencyKey = idempotencyKey,
                    ).onSuccess {
                        confirm = "Saved $title"
                        OperatingProjectionCache.invalidate(momentId)
                        tick += 1
                    }
                },
                onDecide = { eventId, decision ->
                    repo.decideOperatingEvent(momentId, eventId, decision).fold(
                        onSuccess = {
                            confirm = "Decision recorded"
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
            )
            src.sources.forEach { row ->
                InfoCard(
                    row.eventType.replace('_', ' '),
                    listOfNotNull(row.title, row.amount, row.occurredAt).joinToString(" · "),
                )
            }
            if (src.sources.isEmpty()) {
                InfoCard("Sources", "No eligible source records for this widget.")
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
) {
    Column(
        modifier = modifier.fillMaxSize().background(Bg).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title ?: "Business moment", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, fontFamily = PlusJakartaSans)
        Text(
            "Select or create a Marketing, Trading, Retail, Distribution, or Manufacturing moment.",
            color = Muted,
            fontSize = 14.sp,
            fontFamily = PlusJakartaSans,
        )
        Text(
            "Create a moment",
            color = Color(0xFF0C0F15),
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Accent)
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
    onOpenSources: (String) -> Unit,
) {
    if (widgets.isEmpty()) return
    Text("Widgets", color = TextPrimary, fontWeight = FontWeight.Medium, fontFamily = PlusJakartaSans)
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
                .background(Card)
                .border(1.dp, Border, RoundedCornerShape(16.dp))
                .clickable(enabled = cov.showSources || w.sourceEventIds.isNotEmpty()) {
                    onOpenSources(w.widgetId)
                }
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(w.title.ifBlank { w.widgetId }, color = Muted, fontSize = 11.sp, fontFamily = PlusJakartaSans)
                Text(cov.label, color = Accent, fontSize = 11.sp, fontFamily = PlusJakartaSans)
            }
            Text(
                if (!financeVisible && w.financeOnly) "Finance redacted for your role" else metricLine.ifBlank { cov.label },
                color = TextPrimary,
                fontSize = 14.sp,
                fontFamily = PlusJakartaSans,
            )
            w.why?.takeIf { it.isNotBlank() }?.let {
                Text(it, color = Muted, fontSize = 12.sp, fontFamily = PlusJakartaSans)
            }
        }
    }
}

@Composable
private fun BoardSections(sections: List<OperatingBoardSectionDto>?, heading: String) {
    Text(heading, color = TextPrimary, fontWeight = FontWeight.Medium, fontFamily = PlusJakartaSans)
    val blocks = sections.orEmpty()
    if (blocks.isEmpty()) InfoCard("This moment", "Nothing recorded yet.")
    blocks.forEach { section ->
        InfoCard(section.title, section.detail.ifBlank { "Nothing recorded yet." })
        section.rows.forEach { row ->
            val amount = row.amount?.let { " · $it" }.orEmpty()
            InfoCard(row.eventType.replace('_', ' '), row.title + amount)
        }
    }
}

@Composable
private fun PulseFallback(model: OperatingProjectionDto?, onViewAllAttention: () -> Unit) {
    if (model?.financeVisible == true) {
        model.budget?.let { budget ->
            InfoCard("Budget", "${budget.remaining} left of ${budget.amount} ${model.currency.orEmpty()}")
        }
        model.paymentReadiness.forEach { item ->
            InfoCard("Payment Readiness", "${item.title} · ${item.amount} · ${item.status}")
        }
    } else {
        InfoCard("Your view", "Finance totals stay with Finance, Approver, and Owner.")
    }
    model?.attention?.forEach { InfoCard(it.kind, it.title) }
    if (model?.attention.isNullOrEmpty()) {
        InfoCard("Needs Attention", "Nothing is waiting.")
    } else {
        AppStateSecondaryButton(label = "View all", onClick = onViewAllAttention)
    }
    val recent = model?.activity.orEmpty().take(5)
    if (recent.isNotEmpty()) {
        Text("Activity", color = TextPrimary, fontWeight = FontWeight.Medium, fontFamily = PlusJakartaSans)
        recent.forEach { row ->
            val amount = row.amount?.let { " · $it" }.orEmpty()
            InfoCard(row.eventType, row.title + amount)
        }
    }
}

@Composable
private fun InfoCard(label: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Card)
            .border(1.dp, Border, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, color = Muted, fontSize = 11.sp, fontFamily = PlusJakartaSans)
        Text(body, color = TextPrimary, fontSize = 14.sp, fontFamily = PlusJakartaSans)
    }
}
