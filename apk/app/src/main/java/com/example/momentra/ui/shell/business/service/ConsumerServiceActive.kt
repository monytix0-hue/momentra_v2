package com.example.momentra.ui.shell.business.service

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R
import com.example.momentra.data.api.OperatingActivityDto
import com.example.momentra.data.api.OperatingAttentionDto
import com.example.momentra.data.api.OperatingBoardSectionDto
import com.example.momentra.data.api.OperatingPaymentDto
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.shell.business.shared.OperatingChromeState
import com.example.momentra.ui.shell.business.shared.OperatingMomentCatalog
import com.example.momentra.ui.shell.business.shared.OperatingTab
import com.example.momentra.ui.shell.business.shared.operatingAttentionCtaLabel
import com.example.momentra.ui.shell.business.shared.openOperatingAttention
import com.example.momentra.ui.theme.ServicesTeal
import com.example.momentra.ui.shell.business.service.serviceSeverityColor
import com.example.momentra.ui.shell.business.service.serviceSeverityLabel

/**
 * Retail store active experience.
 *
 * Layout + palette come from Figma `2487:37758`:
 * RTL-PULSE-v12 `2399:29655`, RTL-HUB-v5 `2399:29887`,
 * RTL-MOMENTS-v12 `2399:30656`, RTL-LIFE-v12 `2399:30816`, RTL-MEMORY-v12 `2399:30979`.
 *
 * Every number here is read from the operating projection. A metric the backend cannot
 * supply renders `Unavailable` — never a fabricated `0` or `0%` (honesty rules in the plan).
 */
@Composable
fun ConsumerServiceActive(
    model: OperatingProjectionDto,
    tab: OperatingTab,
    onOpenQuickAdd: () -> Unit,
    onRecord: suspend (ServiceEventDraft) -> Unit,
    onDecide: suspend (String, String) -> Unit,
    momentId: String? = null,
    modifier: Modifier = Modifier,
) {
    var sheet by remember { mutableStateOf<ServiceSheetState?>(null) }
    var storyConfig by remember { mutableStateOf(false) }

    TealScreenBackground(modifier) {
        when (tab) {
            OperatingTab.QUICK_ADD -> ServiceQuickAddHub(
                model = model,
                onRecord = onRecord,
                onDecide = onDecide,
                momentId = momentId,
            )

            else -> Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = ServicesTeal.gutter, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(ServicesTeal.sectionGap),
            ) {
                ScreenHeader(
                    title = tabTitle(model, tab),
                    roleLine = "${roleLabel(model.role)} ${model.partyName?.takeIf { it.isNotBlank() } ?: ""}".trim() +
                        " · ${model.subtypeLabel ?: "Consumer Service"}",
                )
                ContextChips(
                    subtypeLabel = model.subtypeLabel ?: "Consumer Service",
                    periodLine = ServiceFormat.periodLine(model.startDate, model.currency),
                    gstOn = model.gstTracking == "YES",
                )
                when (tab) {
                    OperatingTab.PULSE -> ServicePulse(
                        model = model,
                        onOpenQuickAdd = onOpenQuickAdd,
                        onOpenSheet = { sheet = it },
                    )
                    OperatingTab.MOMENTS -> ServiceMoments(
                        model = model,
                        onOpenQuickAdd = onOpenQuickAdd,
                        onOpenSheet = { sheet = it },
                    )
                    OperatingTab.LIFE -> ServiceLife(model = model, onOpenSheet = { sheet = it })
                    OperatingTab.MEMORY -> ServiceMemory(
                        model = model,
                        momentId = momentId,
                        onOpenStoryConfig = { storyConfig = true },
                        onOpenSheet = { sheet = it },
                    )
                    OperatingTab.QUICK_ADD -> Unit
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    sheet?.let { open ->
        ServiceBoardSheet(
            state = open,
            model = model,
            onDismiss = { sheet = null },
            onOpenQuickAdd = onOpenQuickAdd,
        )
    }
    if (storyConfig && momentId != null) {
        com.example.momentra.ui.shell.business.shared.OperatingSheetScrim(
            onDismiss = { storyConfig = false },
            accent = ServicesTeal.teal,
        ) {
            ServiceStoryConfigSheet(
                model = model,
                momentId = momentId,
                onDismiss = { storyConfig = false },
            )
        }
    }
    com.example.momentra.ui.shell.business.shared.OperatingChromeSheets(model)
}

// ---------------------------------------------------------------------------------
// Pulse — RTL-PULSE-v12 `2399:29655`
// ---------------------------------------------------------------------------------

@Composable
private fun ServicePulse(
    model: OperatingProjectionDto,
    onOpenQuickAdd: () -> Unit,
    onOpenSheet: (ServiceSheetState) -> Unit,
) {
    PulseHero(model = model, onOpenQuickAdd = onOpenQuickAdd)
    AttentionCard(model = model, onOpen = onOpenSheet, onOpenQuickAdd = onOpenQuickAdd)
    TargetBudgetCard(model = model, onOpen = onOpenSheet)
    PaymentReadinessCard(model = model, onOpen = onOpenSheet)
    FinanceCard(model = model, onOpen = onOpenSheet)
    ActivityCard(model = model, onOpen = onOpenSheet)
    if (model.gstTracking == "YES") GstCard(model = model, onOpen = onOpenSheet)
}

/** Rose hero: eyebrow, promise line, one-line position + 40dp glyph well. */
@Composable
private fun PulseHero(model: OperatingProjectionDto, onOpenQuickAdd: () -> Unit) {
    val heroShape = ServicesTeal.rHero
    val identity = (model.title ?: "Consumer Service").uppercase()
    val location = model.location?.takeIf { it.isNotBlank() }?.uppercase()
    val alerts = model.attention.size
    val receivable = ServiceFormat.compact(model.currency, model.finance?.outstanding, model.financeVisible)
    val sales = ServiceFormat.compact(
        model.currency,
        model.finance?.sales ?: model.target?.progress,
        model.financeVisible,
    )
    val target = if (model.target?.configured == true) {
        ServiceFormat.compact(model.currency, model.target?.value, model.financeVisible)
    } else {
        null
    }
    val summary = buildString {
        append(if (target != null) "Sales $sales / $target" else "Sales $sales")
        append(" · Booking alerts $alerts")
        append(" · AR $receivable open")
    }

    Box(
        Modifier
            .fillMaxWidth()
            .shadow(12.dp, heroShape, clip = false, ambientColor = ServicesTeal.tealDeep.copy(alpha = 0.5f), spotColor = ServicesTeal.tealDeep.copy(alpha = 0.5f))
            .shadow(24.dp, heroShape, clip = false, ambientColor = ServicesTeal.teal.copy(alpha = 0.3f), spotColor = ServicesTeal.teal.copy(alpha = 0.3f))
            .clip(heroShape)
            .background(Brush.linearGradient(listOf(ServicesTeal.teal, ServicesTeal.teal)))
            .padding(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            Text(
                listOfNotNull(identity, location).joinToString(" · "),
                style = ServicesTeal.heroEyebrow(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text("Book. Charge. Settle.", style = ServicesTeal.heroHeadline(), maxLines = 1)
            Text(
                summary,
                style = ServicesTeal.heroBody(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 56.dp),
            )
            if ("SALE" in model.quickAdds.map { it.code }) {
                Spacer(Modifier.height(4.dp))
                TealCta("+  Record Sale", onClick = onOpenQuickAdd)
            }
        }
        HeroGlyph()
    }
}

/** 40dp rose-tinted glyph well pinned to the hero's top-right. */
@Composable
private fun HeroGlyph() {
    val shape = ServicesTeal.rHeroIcon
    Box(
        Modifier
            .size(40.dp)
            .shadow(12.dp, shape, clip = false, ambientColor = ServicesTeal.teal.copy(alpha = 0.5f), spotColor = ServicesTeal.teal.copy(alpha = 0.5f))
            .clip(shape)
            .background(ServicesTeal.surfaceRow.copy(alpha = 0.9f))
            .border(1.dp, ServicesTeal.teal, shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(R.drawable.rs_setup_service),
            contentDescription = null,
            tint = ServicesTeal.teal,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** AN-P01 · Needs Attention `2399:29710` */
@Composable
private fun AttentionCard(
    model: OperatingProjectionDto,
    onOpen: (ServiceSheetState) -> Unit,
    onOpenQuickAdd: () -> Unit,
) {
    val lowStock = model.attention.count { it.kind.startsWith("CHARGE_PENDING") }
    TealCard {
        SectionHeader(
            title = "Needs Attention",
            chipText = if (model.attention.isEmpty()) null else "Alert",
            onViewAll = { onOpen(ServiceSheetState.Attention) },
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricChip("Alerts", model.attention.size.toString(), ServicesTeal.teal, Modifier.weight(1f))
            MetricChip("Charges", lowStock.toString(), ServicesTeal.amber, Modifier.weight(1f))
        }
        if (model.attention.isEmpty()) {
            TealEmpty("Nothing is waiting", "Approvals, overdue items and blockers will show here.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                model.attention.take(3).forEach { item ->
                    AttentionRow(item, model, onOpenQuickAdd)
                }
            }
        }
        WhyCaption("Each alert links to Stock Update, Collection or Issue. Quick amount sales never invent stock movement.")
    }
}

@Composable
internal fun AttentionRow(
    item: OperatingAttentionDto,
    model: OperatingProjectionDto,
    onOpenQuickAdd: () -> Unit,
) {
    val hasSource = item.sourceEventId != null || item.id.startsWith("open-") || item.cta != null
    val label = serviceSeverityLabel(item.kind, item.severity, hasSource)
    val color = if (!hasSource) {
        ServicesTeal.warning
    } else {
        serviceSeverityColor(item.severity)
    }
    val caption = listOfNotNull(
        item.counterpartyName?.takeIf { it.isNotBlank() },
        ServiceFormat.shortDay(item.dueAt)?.let { "Due $it" },
        ServiceFormat.compact(model.currency, item.amount, model.financeVisible).takeIf { it != "—" },
    ).joinToString(" · ")
    AccentRow(
        title = item.title.ifBlank { "Attention item" },
        caption = caption.ifBlank { "No detail recorded" },
        accent = color,
        severityLabel = label,
        severityColor = color,
        onClick = { openOperatingAttention(item, model, onOpenQuickAdd) },
    )
}

/** AN-P02 · Target & Budget `2399:29677` */
@Composable
private fun TargetBudgetCard(model: OperatingProjectionDto, onOpen: (ServiceSheetState) -> Unit) {
    val target = model.target
    val budget = model.budget
    val configured = target?.configured == true
    val percentText = if (configured) {
        ServiceFormat.percent(target?.progress, target?.value) ?: "Unavailable"
    } else {
        "Unavailable"
    }
    val fraction = if (configured) ServiceFormat.fraction(target?.progress, target?.value) else null
    val sales = ServiceFormat.compact(model.currency, target?.progress ?: model.finance?.sales, model.financeVisible)
    val targetValue = if (configured) ServiceFormat.compact(model.currency, target?.value, model.financeVisible) else null
    val budgetPercent = if (budget?.configured == true) {
        ServiceFormat.percent(budget?.spent, budget?.amount) ?: "Unavailable"
    } else {
        "Unavailable"
    }
    val onTrack = fraction != null && fraction >= 0.75f
    val behind = fraction != null && fraction < 0.5f

    TealCard {
        SectionHeader("Target & Budget", onViewAll = { onOpen(ServiceSheetState.Target) })
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(ServicesTeal.surfaceRow)
                .border(1.dp, ServicesTeal.teal.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
                .padding(14.dp),
        ) {
            HealthRing(percentLabel = percentText, caption = "of target", fraction = fraction)
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    "ACHIEVEMENT VS TARGET",
                    style = ServicesTeal.boardEyebrow().copy(letterSpacing = 0.6.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                )
                Text(
                    if (configured && targetValue != null) "Sales $sales of $targetValue" else "Sales $sales · no target set",
                    style = ServicesTeal.sectionTitle().copy(fontSize = 15.sp),
                    maxLines = 2,
                )
                when {
                    fraction == null -> StatusPill("● Unavailable", ServicesTeal.warning)
                    behind -> StatusPill("● Behind", ServicesTeal.negative)
                    onTrack -> StatusPill("● On track", ServicesTeal.positive)
                    else -> StatusPill("● In progress", ServicesTeal.amber)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricChip("Sales", sales, ServicesTeal.textPrimary, Modifier.weight(1f))
            MetricChip(
                "Budget use",
                budgetPercent,
                if (budgetPercent == "Unavailable") ServicesTeal.warning else ServicesTeal.textPrimary,
                Modifier.weight(1f),
            )
        }
        Column {
            DividedRow(
                title = "Sales vs target",
                caption = "Effective ${ServiceFormat.month(model.startDate)} · store",
                trailingValue = if (configured && targetValue != null) "$sales / $targetValue" else "Unavailable",
                trailingColor = if (configured) ServicesTeal.textPrimary else ServicesTeal.warning,
            )
            DividedRow(
                title = "Eligible spend",
                caption = "COGS + running",
                trailingValue = ServiceFormat.compact(model.currency, model.finance?.eligibleCost, model.financeVisible),
            )
            DividedRow(
                title = "Footfall attributed",
                caption = target?.coverage ?: "Coverage unavailable",
                trailingValue = if (target?.coverage.isNullOrBlank()) "Unavailable" else target?.coverage.orEmpty(),
                trailingColor = if (target?.coverage.isNullOrBlank()) ServicesTeal.warning else ServicesTeal.textPrimary,
                captionColor = if (target?.coverage.isNullOrBlank()) ServicesTeal.warning else ServicesTeal.textBody,
            )
        }
        WhyCaption("Achievement = eligible actual ÷ effective target. Missing coverage shows Unavailable — never invent 0%.")
    }
}

/** AN-P03 · Payment Readiness `2399:29742` */
@Composable
private fun PaymentReadinessCard(model: OperatingProjectionDto, onOpen: (ServiceSheetState) -> Unit) {
    val payables = model.paymentReadiness.filter { it.relevance != "RECEIVABLE" }
    val paid = model.paymentReadiness
        .filter { it.relevance != "RECEIVABLE" && it.status in setOf("PAID", "SETTLED") }
        .sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
    TealCard {
        SectionHeader("Payment Readiness", onViewAll = { onOpen(ServiceSheetState.Payment) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricChip("AP open", ServiceFormat.compact(model.currency, model.finance?.payable, model.financeVisible), ServicesTeal.amber, Modifier.weight(1f))
            MetricChip(
                "Paid",
                ServiceFormat.compact(model.currency, if (paid > 0) paid.toString() else model.finance?.cashPaid, model.financeVisible),
                ServicesTeal.positive,
                Modifier.weight(1f),
            )
        }
        if (payables.isEmpty()) {
            TealEmpty("No open payments", "Purchases and expenses waiting to settle will show here.")
        } else {
            Column {
                payables.take(3).forEach { row ->
                    val caption = listOfNotNull(
                        row.counterpartyName?.takeIf { it.isNotBlank() },
                        ServiceFormat.shortDay(row.dueAt)?.let { "Due $it" },
                    ).joinToString(" · ").ifBlank { row.status.lowercase().replaceFirstChar { it.uppercase() } }
                    DividedRow(
                        title = row.title.ifBlank { row.counterpartyName ?: "Open payable" },
                        caption = caption,
                        trailingValue = ServiceFormat.compact(model.currency, row.amount, model.financeVisible),
                        trailingColor = if (row.dueBucket == "OVERDUE") ServicesTeal.negative else ServicesTeal.amber,
                        captionColor = if (row.dueBucket == "OVERDUE") ServicesTeal.negative else ServicesTeal.textBody,
                        onClick = { OperatingChromeState.settling = row },
                    )
                }
            }
        }
        WhyCaption("Purchase ≠ payment. Payment settles open payable only.")
    }
}

/** AN-P04 · Finance `2399:29772` */
@Composable
private fun FinanceCard(model: OperatingProjectionDto, onOpen: (ServiceSheetState) -> Unit) {
    val finance = model.finance
    val receivables = model.paymentReadiness.filter { it.relevance == "RECEIVABLE" }
    TealCard {
        SectionHeader("Finance", onViewAll = { onOpen(ServiceSheetState.Finance) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricChip("Sales", ServiceFormat.compact(model.currency, finance?.sales, model.financeVisible), ServicesTeal.textPrimary, Modifier.weight(1f))
            MetricChip("Collected", ServiceFormat.compact(model.currency, finance?.received, model.financeVisible), ServicesTeal.positive, Modifier.weight(1f))
            MetricChip("AR", ServiceFormat.compact(model.currency, finance?.outstanding, model.financeVisible), ServicesTeal.teal, Modifier.weight(1f))
        }
        if (!model.financeVisible) {
            TealEmpty("Totals stay private", "Finance, Approver and Owner see the money position.")
        } else if (receivables.isEmpty()) {
            TealEmpty("Nothing to collect", "Credit sales waiting on collection will show here.")
        } else {
            Column {
                receivables.take(3).forEach { row ->
                    DividedRow(
                        title = (row.counterpartyName ?: row.title).ifBlank { "Open receivable" },
                        caption = ServiceFormat.shortDay(row.dueAt)?.let { "Due $it" } ?: "Still open",
                        trailingValue = ServiceFormat.compact(model.currency, row.amount, true),
                        trailingColor = if (row.dueBucket == "OVERDUE") ServicesTeal.negative else ServicesTeal.teal,
                        captionColor = if (row.dueBucket == "OVERDUE") ServicesTeal.negative else ServicesTeal.textBody,
                        onClick = { OperatingChromeState.settling = row },
                    )
                }
            }
        }
        WhyCaption("Posted sale creates revenue. Credit needs a named customer. Collection allocates AR — never new sales.")
    }
}

/** AN-P05 · Activity `2399:29805` */
@Composable
private fun ActivityCard(model: OperatingProjectionDto, onOpen: (ServiceSheetState) -> Unit) {
    val sales = model.activity.count { it.eventType == "SALE" }
    val purchases = model.activity.count { it.eventType == "PURCHASE" }
    val stockMoves = model.activity.count { it.eventType == "STOCK" }
    TealCard {
        SectionHeader("Activity", onViewAll = { onOpen(ServiceSheetState.Activity) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricChip("Sales", sales.toString(), ServicesTeal.textPrimary, Modifier.weight(1f))
            MetricChip("Purchases", purchases.toString(), ServicesTeal.textPrimary, Modifier.weight(1f))
            MetricChip("Stock moves", stockMoves.toString(), ServicesTeal.textPrimary, Modifier.weight(1f))
        }
        if (model.activity.isEmpty()) {
            TealEmpty("Nothing recorded yet", "Sales, purchases and stock moves will show here.")
        } else {
            Column {
                model.activity.take(3).forEach { row -> ActivityDividedRow(row, model) }
            }
        }
        WhyCaption("Stock changes only on explicit receipt, sale fulfilment or adjustment — never from billing alone.")
    }
}

@Composable
internal fun ActivityDividedRow(row: OperatingActivityDto, model: OperatingProjectionDto) {
    val caption = buildString {
        val who = row.counterpartyName?.takeIf { it.isNotBlank() }
        val channel = row.channelName?.takeIf { it.isNotBlank() }
        append(who ?: channel ?: row.note?.take(48) ?: eventLabel(row.eventType))
        row.quantity?.takeIf { it.isNotBlank() }?.let { append(" · $it${row.unit?.let { u -> " $u" } ?: ""}") }
    }
    val trailing = when {
        row.approvalStatus == "PENDING" -> "Pending"
        row.paymentStatus in setOf("COLLECTED", "PAID", "SETTLED") -> "Done"
        row.status != null -> row.status!!.lowercase().replaceFirstChar { c -> c.uppercase() }
        else -> "Open"
    }
    val color = when (trailing) {
        "Done" -> ServicesTeal.positive
        "Pending" -> ServicesTeal.amber
        else -> ServicesTeal.textBody
    }
    DividedRow(
        title = row.title.ifBlank { eventLabel(row.eventType) },
        caption = caption,
        trailingValue = trailing,
        trailingColor = color,
        onClick = { OperatingChromeState.editing = row },
    )
}

/** AN-P06 · GST Position `2399:29838` */
@Composable
private fun GstCard(model: OperatingProjectionDto, onOpen: (ServiceSheetState) -> Unit) {
    val gst = model.gstPosition
    val complete = !gst?.outputGst.isNullOrBlank() && !gst?.inputGst.isNullOrBlank()
    TealCard {
        SectionHeader("GST Position", onViewAll = { onOpen(ServiceSheetState.Gst) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricChip("Output", ServiceFormat.compact(model.currency, gst?.outputGst, true), ServicesTeal.textPrimary, Modifier.weight(1f))
            MetricChip("ITC", ServiceFormat.compact(model.currency, gst?.inputGst, true), ServicesTeal.positive, Modifier.weight(1f))
            MetricChip("Net", ServiceFormat.compact(model.currency, gst?.difference, true), ServicesTeal.amber, Modifier.weight(1f))
        }
        if (!complete) {
            TealEmpty("Registration incomplete", "GST stays Unavailable until the registration is complete.")
        } else {
            Column {
                val sources = gst?.sources.orEmpty()
                if (sources.isEmpty()) {
                    DividedRow("Outward taxable", "Posted sales", ServiceFormat.compact(model.currency, model.finance?.sales, true))
                    DividedRow("Inward eligible", "Posted purchases", ServiceFormat.compact(model.currency, model.finance?.eligibleCost, true))
                } else {
                    sources.take(3).forEach { source ->
                        DividedRow(
                            title = source.title.ifBlank { eventLabel(source.eventType) },
                            caption = if (source.direction == "OUTPUT") "Outward taxable" else "Inward eligible",
                            trailingValue = ServiceFormat.compact(model.currency, source.gstTotal, true),
                            trailingColor = if (source.direction == "OUTPUT") ServicesTeal.textPrimary else ServicesTeal.positive,
                        )
                    }
                }
                DividedRow(
                    title = "Net payable",
                    caption = ServiceFormat.asOf(gst?.asOfMonth?.let { "$it-01" }),
                    trailingValue = ServiceFormat.compact(model.currency, gst?.difference, true),
                    trailingColor = ServicesTeal.amber,
                )
            }
        }
        WhyCaption("GST only when enabled. Incomplete registration → Unavailable. Indicative only — not a filing.")
    }
}

// ---------------------------------------------------------------------------------
// Moments — RTL-MOMENTS-v12 `2399:30656`
// ---------------------------------------------------------------------------------

@Composable
private fun ServiceMoments(
    model: OperatingProjectionDto,
    onOpenQuickAdd: () -> Unit,
    onOpenSheet: (ServiceSheetState) -> Unit,
) {
    MomentsHero(model)
    OpenCommitmentsCard(model, onOpenSheet)
    MyBusinessCard(model, onOpenSheet)
    PeopleCard(model, onOpenSheet)
    NetworkCard(model, onOpenSheet)
    TealCard(tone = TealTone.Board, onClick = onOpenQuickAdd) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SheetIconWell(ServiceTileIcons.sale)
            Column(Modifier.weight(1f)) {
                Text("Record the next store moment", style = ServicesTeal.sectionTitle(), maxLines = 2)
                Text("Sale, purchase, collection or adjustment.", style = ServicesTeal.rowCaption())
            }
            Text("›", style = ServicesTeal.sectionTitle().copy(color = ServicesTeal.teal))
        }
    }
}

@Composable
private fun MomentsHero(model: OperatingProjectionDto) {
    val staff = model.participants.size
    val suppliers = model.masters.orEmpty().count { it.kind == "SUPPLIER" }
    BoardHero(
        eyebrow = "ACTIVE MOMENT",
        headline = model.title?.ifBlank { "Consumer Service" } ?: "Consumer Service",
        chips = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BoardHeroChip(model.subtypeLabel ?: "Service", ServicesTeal.teal)
                if (!model.location.isNullOrBlank()) {
                    BoardHeroChip(model.location!!, ServicesTeal.positive, dot = true)
                }
            }
        },
        kpis = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiChip("People · Staff", staff.toString(), ServicesTeal.textPrimary, Modifier.weight(1f))
                KpiChip("Network · Suppliers", suppliers.toString(), ServicesTeal.textPrimary, Modifier.weight(1f))
                KpiChip(
                    "AP open",
                    ServiceFormat.compact(model.currency, model.finance?.payable, model.financeVisible),
                    ServicesTeal.amber,
                    Modifier.weight(1f),
                )
            }
        },
    )
}

/** AN-M04 · Open Commitments `2399:30767` */
@Composable
private fun OpenCommitmentsCard(model: OperatingProjectionDto, onOpen: (ServiceSheetState) -> Unit) {
    val openOrders = model.activity.count {
        (it.eventType == "ORDER" || it.eventType == "CUSTOMER_ORDER") &&
            it.status !in setOf("COMPLETED", "CANCELLED")
    }
    val rows = model.boards?.moments.orEmpty().firstOrNull { it.code == "M4" }?.rows.orEmpty()
    TealCard(tone = TealTone.Board, strokeAlpha = 0.40f) {
        SectionHeader("Open Commitments", onViewAll = { onOpen(ServiceSheetState.Commitments) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiChip("AP", ServiceFormat.compact(model.currency, model.finance?.payable, model.financeVisible), ServicesTeal.amber, Modifier.weight(1f))
            KpiChip("AR", ServiceFormat.compact(model.currency, model.finance?.outstanding, model.financeVisible), ServicesTeal.teal, Modifier.weight(1f))
            KpiChip("Orders", openOrders.toString(), ServicesTeal.textPrimary, Modifier.weight(1f))
        }
        if (rows.isEmpty()) {
            TealEmpty("Nothing open", "Orders, payables and receivables will show here.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.take(3).forEach { row -> BoardRowFor(row, model) }
            }
        }
        WhyNote("Orders are commitments — revenue only on posted sale.")
    }
}

/** AN-M01 · My Service Business `2399:30674` */
@Composable
private fun MyBusinessCard(model: OperatingProjectionDto, onOpen: (ServiceSheetState) -> Unit) {
    val dayOfPeriod = dayOfPeriod(model)
    TealCard(tone = TealTone.Board) {
        SectionHeader("My Service Business", onViewAll = { onOpen(ServiceSheetState.MomentDetail) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiChip("Status", if (model.retired) "Closed" else "Live", if (model.retired) ServicesTeal.textMuted else ServicesTeal.positive, Modifier.weight(1f))
            KpiChip("Channel", model.life?.channels?.firstOrNull() ?: "Unavailable", ServicesTeal.textPrimary, Modifier.weight(1f))
            KpiChip("Day", dayOfPeriod, ServicesTeal.textPrimary, Modifier.weight(1f))
        }
        Column {
            DetailRow(
                eyebrow = "Business",
                value = listOfNotNull(model.title, model.location?.takeIf { it.isNotBlank() }).joinToString(" · ").ifBlank { "Unavailable" },
                trailing = if (model.retired) "Closed" else "Live",
                trailingColor = if (model.retired) ServicesTeal.textMuted else ServicesTeal.positive,
            )
            DetailRow(
                eyebrow = "Plan",
                value = if (model.target?.configured == true) {
                    "Sales ${ServiceFormat.compact(model.currency, model.target?.value, model.financeVisible)} target"
                } else {
                    "No target set"
                },
                trailing = if (model.target?.configured == true) "Active" else "Unavailable",
                trailingColor = if (model.target?.configured == true) ServicesTeal.textPrimary else ServicesTeal.warning,
            )
            DetailRow(
                eyebrow = "GST",
                value = when {
                    model.gstTracking != "YES" -> "Not tracked"
                    model.gstRegistration.isNullOrBlank() -> "Registration incomplete"
                    else -> "Registered${model.gstState?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""}"
                },
                trailing = if (model.gstTracking == "YES") "On" else "Off",
                trailingColor = ServicesTeal.amber,
            )
        }
        WhyNote("One Moment holds catalogue, stock, sales and money. Quick amount never invents SKU movement.")
    }
}

/** AN-M02 · People `2399:30707` */
@Composable
private fun PeopleCard(model: OperatingProjectionDto, onOpen: (ServiceSheetState) -> Unit) {
    val staff = model.participants
    TealCard(tone = TealTone.Board) {
        SectionHeader("People", onViewAll = { onOpen(ServiceSheetState.People) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiChip("Staff", staff.size.toString(), ServicesTeal.textPrimary, Modifier.weight(1f))
            KpiChip(
                "Approvers",
                if (model.role == "OWNER_ADMIN" || model.role == "APPROVER") "1" else "Unavailable",
                if (model.role == "OWNER_ADMIN" || model.role == "APPROVER") ServicesTeal.amber else ServicesTeal.warning,
                Modifier.weight(1f),
            )
        }
        if (staff.isEmpty()) {
            TealEmpty("No people yet", "Invite the shop floor, billing and approvers.")
        } else {
            Column {
                staff.take(3).forEach { name ->
                    val role = if (name == model.partyName) roleLabel(model.role) else "Member"
                    DetailRow(
                        eyebrow = name,
                        value = role,
                        trailing = if (name == model.partyName) "Owner" else "Member",
                        trailingColor = if (name == model.partyName) ServicesTeal.textPrimary else ServicesTeal.textBody,
                    )
                }
            }
        }
        WhyNote("Roles gate credit sales, stock adjustments and payments.")
    }
}

/** AN-M03 · Clients & Services `2399:30737` */
@Composable
private fun NetworkCard(model: OperatingProjectionDto, onOpen: (ServiceSheetState) -> Unit) {
    val masters = model.masters.orEmpty()
    val suppliers = masters.filter { it.kind == "SUPPLIER" }
    val buyers = masters.filter { it.kind == "CUSTOMER" || it.kind == "BUYER" }
    val payables = model.paymentReadiness.filter { it.relevance != "RECEIVABLE" }
    val receivables = model.paymentReadiness.filter { it.relevance == "RECEIVABLE" }
    val combined = (suppliers.map { it.name to true } + buyers.map { it.name to false }).distinctBy { it.first }
    TealCard(tone = TealTone.Board) {
        SectionHeader("Clients & Services", onViewAll = { onOpen(ServiceSheetState.Network) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiChip("Suppliers", suppliers.size.toString(), ServicesTeal.textPrimary, Modifier.weight(1f))
            KpiChip("Credit buyers", buyers.size.toString(), ServicesTeal.textPrimary, Modifier.weight(1f))
        }
        if (combined.isEmpty()) {
            TealEmpty("No network yet", "Suppliers and credit buyers you record will show here.")
        } else {
            Column {
                combined.take(3).forEach { (name, isSupplier) ->
                    val openAp = payables.filter { it.counterpartyName.equals(name, true) }.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
                    val openAr = receivables.filter { it.counterpartyName.equals(name, true) }.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
                    val trailing = when {
                        isSupplier && openAp > 0 -> "AP ${ServiceFormat.compact(model.currency, openAp.toString(), model.financeVisible)}"
                        !isSupplier && openAr > 0 -> "AR ${ServiceFormat.compact(model.currency, openAr.toString(), model.financeVisible)}"
                        else -> "No balance"
                    }
                    DetailRow(
                        eyebrow = name,
                        value = if (isSupplier) "Supplier" else "Credit buyer",
                        trailing = trailing,
                        trailingColor = when {
                            isSupplier && openAp > 0 -> ServicesTeal.amber
                            !isSupplier && openAr > 0 -> ServicesTeal.teal
                            else -> ServicesTeal.textMuted
                        },
                    )
                }
            }
        }
        WhyNote("Named customer required for unpaid/credit charge.")
    }
}

// ---------------------------------------------------------------------------------
// Life — RTL-LIFE-v12 `2399:30816`
// ---------------------------------------------------------------------------------

@Composable
private fun ServiceLife(model: OperatingProjectionDto, onOpenSheet: (ServiceSheetState) -> Unit) {
    val open = model.life?.openCommitments ?: 0
    val done = model.activity.count { it.paymentStatus in setOf("COLLECTED", "PAID", "SETTLED") }
    BoardHero(
        eyebrow = "TODAY",
        headline = "${model.title?.ifBlank { "Consumer Service" } ?: "Consumer Service"} · today",
        strokeAlpha = 0.45f,
        chips = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BoardHeroChip(ServiceFormat.todayLine(), ServicesTeal.teal)
                BoardHeroChip(if (model.retired) "Store closed" else "Store open", if (model.retired) ServicesTeal.textMuted else ServicesTeal.positive, dot = true)
            }
        },
        kpis = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiChip("Open", open.toString(), ServicesTeal.teal, Modifier.weight(1f))
                KpiChip("Done", done.toString(), ServicesTeal.positive, Modifier.weight(1f))
                KpiChip(
                    "Money in motion",
                    ServiceFormat.compact(model.currency, model.finance?.received, model.financeVisible),
                    ServicesTeal.textPrimary,
                    Modifier.weight(1f),
                )
            }
        },
    )

    val life = model.boards?.life.orEmpty()
    LifeCurrentCard(model, life.firstOrNull { it.code == "L1" || it.code == "L2" }, onOpenSheet)
    LifeSalesCard(model, life, onOpenSheet)
    LifeMoneyCard(model, life.firstOrNull { it.code == "L3" }, onOpenSheet)
    LifeOpsCard(model, life, onOpenSheet)
}

/** AN-L01 · Current Business `2399:30834` */
@Composable
private fun LifeCurrentCard(model: OperatingProjectionDto, section: OperatingBoardSectionDto?, onOpen: (ServiceSheetState) -> Unit) {
    val target = model.target
    val percent = if (target?.configured == true) ServiceFormat.percent(target?.progress, target?.value) ?: "Unavailable" else "Unavailable"
    TealCard(tone = TealTone.Board) {
        SectionHeader("Current Business", onViewAll = { onOpen(ServiceSheetState.LifeCurrent) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiChip(
                "Sales today",
                ServiceFormat.compact(model.currency, model.finance?.sales, model.financeVisible),
                ServicesTeal.positive,
                Modifier.weight(1f),
            )
            KpiChip("Achv.", percent, if (percent == "Unavailable") ServicesTeal.warning else ServicesTeal.amber, Modifier.weight(1f))
        }
        val rows = section?.rows.orEmpty()
        if (rows.isEmpty()) {
            TealEmpty("Nothing recorded yet", section?.detail ?: "Today's posted sales and stock moves will show here.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.take(3).forEach { row ->
                    BoardRowFor(
                        row = row,
                        model = model,
                        trailingOverride = if (row.approvalStatus == "PENDING") "Open" else null,
                    )
                }
            }
        }
        if (!section?.coverage.isNullOrBlank()) {
            SeverityChip(section?.coverage.orEmpty(), ServicesTeal.warning)
        }
        WhyNote("Today shows posted sales and explicit stock moves only.")
    }
}

/** AN-L02 · Bookings & Services `2399:30864` */
@Composable
private fun LifeSalesCard(model: OperatingProjectionDto, life: List<OperatingBoardSectionDto>, onOpen: (ServiceSheetState) -> Unit) {
    val tickets = model.activity.count { it.eventType == "SALE" }
    val creditOpen = model.paymentReadiness.count { it.relevance == "RECEIVABLE" }
    val holds = model.activity.count { (it.eventType == "ORDER" || it.eventType == "CUSTOMER_ORDER") && it.status !in setOf("COMPLETED", "CANCELLED") }
    val channelSection = life.firstOrNull { it.code == "L2" }
    TealCard(tone = TealTone.Board) {
        SectionHeader("Bookings & Services", onViewAll = { onOpen(ServiceSheetState.LifeSales) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiChip("Tickets", tickets.toString(), ServicesTeal.textPrimary, Modifier.weight(1f))
            KpiChip("Credit open", creditOpen.toString(), ServicesTeal.teal, Modifier.weight(1f))
            KpiChip("Holds", holds.toString(), ServicesTeal.amber, Modifier.weight(1f))
        }
        if (channelSection != null && channelSection.rows.isNotEmpty()) {
            Column {
                channelSection.rows.take(3).forEach { row ->
                    DetailRow(
                        eyebrow = row.channelName ?: row.title.ifBlank { "Channel" },
                        value = row.title.ifBlank { "Recorded" },
                        trailing = ServiceFormat.compact(model.currency, row.amount, model.financeVisible),
                        trailingColor = ServicesTeal.textPrimary,
                    )
                }
            }
        } else if (!channelSection?.detail.isNullOrBlank()) {
            TealEmpty("Counter mix", channelSection?.detail.orEmpty())
        } else {
            TealEmpty("Nothing recorded yet", "Sales, credit and holds will show here.")
        }
        WhyNote("Order value stays separate from posted sale.")
    }
}

/** AN-L03 · Money Moving `2399:30897` */
@Composable
private fun LifeMoneyCard(model: OperatingProjectionDto, section: OperatingBoardSectionDto?, onOpen: (ServiceSheetState) -> Unit) {
    TealCard(tone = TealTone.Board, strokeAlpha = 0.45f) {
        SectionHeader("Money Moving", onViewAll = { onOpen(ServiceSheetState.LifeMoney) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiChip("Collected", ServiceFormat.compact(model.currency, model.finance?.received, model.financeVisible), ServicesTeal.positive, Modifier.weight(1f))
            KpiChip("Paid", ServiceFormat.compact(model.currency, model.finance?.cashPaid, model.financeVisible), ServicesTeal.textPrimary, Modifier.weight(1f))
            KpiChip("AR move", ServiceFormat.compact(model.currency, model.finance?.outstanding, model.financeVisible), ServicesTeal.teal, Modifier.weight(1f))
        }
        val rows = section?.rows.orEmpty()
        if (rows.isEmpty()) {
            TealEmpty("Nothing moved yet", section?.detail ?: "Collections and payments will show here.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.take(3).forEach { row ->
                    BoardRowFor(
                        row = row,
                        model = model,
                        trailingOverride = if (row.paymentStatus in setOf("COLLECTED", "PAID", "SETTLED")) "Done" else "Open",
                    )
                }
            }
        }
        WhyNote("Collection/payment allocate — never repost sale or purchase.")
    }
}

/** AN-L04 · Rooms & Staff `2399:30930` */
@Composable
private fun LifeOpsCard(model: OperatingProjectionDto, life: List<OperatingBoardSectionDto>, onOpen: (ServiceSheetState) -> Unit) {
    val receipts = model.activity.count { it.eventType == "PURCHASE" }
    val adjustments = model.activity.count { it.eventType == "STOCK" }
    val alerts = model.attention.size
    val section = life.firstOrNull { it.code == "L4" }
    TealCard(tone = TealTone.Board) {
        SectionHeader("Rooms & Staff", onViewAll = { onOpen(ServiceSheetState.LifeOps) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiChip("Receipts", receipts.toString(), ServicesTeal.textPrimary, Modifier.weight(1f))
            KpiChip("Adjustments", adjustments.toString(), ServicesTeal.textPrimary, Modifier.weight(1f))
            KpiChip("Alerts", alerts.toString(), ServicesTeal.teal, Modifier.weight(1f))
        }
        val rows = section?.rows.orEmpty()
        if (rows.isEmpty()) {
            TealEmpty("Nothing moving", section?.detail ?: "Receipts, counts and damage will show here.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.take(3).forEach { row ->
                    BoardRowFor(row = row, model = model, trailingOverride = "Open")
                }
            }
        }
        WhyNote("Stock += accepted receipt only; never infer from billing.")
    }
}

// ---------------------------------------------------------------------------------
// Memory — RTL-MEMORY-v12 `2399:30979`
// ---------------------------------------------------------------------------------

@Composable
private fun ServiceMemory(
    model: OperatingProjectionDto,
    momentId: String?,
    onOpenStoryConfig: () -> Unit,
    onOpenSheet: (ServiceSheetState) -> Unit,
) {
    val target = model.target
    val percent = if (target?.configured == true) ServiceFormat.percent(target?.progress, target?.value) else null
    BoardHero(
        eyebrow = "MEMORY · THIS MONTH",
        headline = "${model.title?.ifBlank { "Consumer Service" } ?: "Consumer Service"} · memory",
        strokeAlpha = 0.45f,
        chips = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val period = ServiceFormat.month(model.startDate)
                BoardHeroChip(
                    if (percent != null) "$period · $percent of ${ServiceFormat.compact(model.currency, target?.value, model.financeVisible)}" else "$period · memory",
                    ServicesTeal.teal,
                )
                if (model.memory?.revisions.orEmpty().isNotEmpty()) {
                    BoardHeroChip("${model.memory?.revisions?.size} revisions", ServicesTeal.textMuted)
                }
            }
        },
        kpis = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiChip("Sales", ServiceFormat.compact(model.currency, model.finance?.sales, model.financeVisible), ServicesTeal.textPrimary, Modifier.weight(1f))
                KpiChip(
                    "vs last period",
                    lastPeriodDelta(model),
                    if (lastPeriodDelta(model) == "Unavailable") ServicesTeal.warning else ServicesTeal.warning,
                    Modifier.weight(1f),
                )
                KpiChip("Supplier paid", ServiceFormat.compact(model.currency, model.finance?.cashPaid, model.financeVisible), ServicesTeal.teal, Modifier.weight(1f))
            }
        },
    )

    val memory = model.boards?.memory.orEmpty()
    MonthlyMemoryCard(model, memory.firstOrNull { it.code == "ME1" }, onOpenSheet)
    if (memory.any { it.code == "ME2" }) MoneyMemoryCard(model, memory.first { it.code == "ME2" }, onOpenSheet)
    if (memory.any { it.code == "ME3" }) PeopleMemoryCard(model, memory.first { it.code == "ME3" }, onOpenSheet)
    if (memory.any { it.code == "ME4" }) PatternsCard(model, memory.first { it.code == "ME4" }, onOpenSheet)
    if (momentId != null) StoryCta(
        onTap = onOpenStoryConfig,
        month = ServiceFormat.month(model.startDate),
    )
}

/** AN-H01 · Monthly Memory `2399:30997` */
@Composable
private fun MonthlyMemoryCard(model: OperatingProjectionDto, section: OperatingBoardSectionDto?, onOpen: (ServiceSheetState) -> Unit) {
    val months = section?.rows.orEmpty().filter { it.eventType == "MONTH" || it.eventType.isBlank() || it.eventType == "REVISION" }
    TealCard(tone = TealTone.Board) {
        SectionHeader("Monthly Memory", onViewAll = { onOpen(ServiceSheetState.Monthly) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiChip("Months", months.size.toString(), ServicesTeal.textPrimary, Modifier.weight(1f))
            KpiChip(
                "Avg achv.",
                averageAchievement(months, model),
                if (averageAchievement(months, model) == "Unavailable") ServicesTeal.warning else ServicesTeal.amber,
                Modifier.weight(1f),
            )
        }
        if (months.isEmpty()) {
            TealEmpty("No history yet", section?.detail ?: "Closed months will show here.")
        } else {
            Column {
                months.take(3).forEach { row ->
                    val parts = row.title.split("·").map { it.trim() }
                    DetailRow(
                        eyebrow = parts.firstOrNull().orEmpty().ifBlank { ServiceFormat.month(row.occurredAt) },
                        value = parts.drop(1).joinToString(" · ").ifBlank { ServiceFormat.compact(model.currency, row.amount, model.financeVisible) },
                        trailing = "Recorded",
                        trailingColor = ServicesTeal.textBody,
                    )
                }
            }
        }
        if (!section?.coverage.isNullOrBlank()) SeverityChip(section?.coverage.orEmpty(), ServicesTeal.warning)
        WhyNote("Missing coverage stays unavailable — never averaged as zero.")
    }
}

/** AN-H02 · Money Memory `2399:31027` */
@Composable
private fun MoneyMemoryCard(model: OperatingProjectionDto, section: OperatingBoardSectionDto, onOpen: (ServiceSheetState) -> Unit) {
    TealCard(tone = TealTone.Board, strokeAlpha = 0.50f) {
        SectionHeader("Money Memory", onViewAll = { onOpen(ServiceSheetState.MoneyMemory) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiChip("Sales", ServiceFormat.compact(model.currency, model.finance?.sales, model.financeVisible), ServicesTeal.textPrimary, Modifier.weight(1f))
            KpiChip("Collected", ServiceFormat.compact(model.currency, model.finance?.received, model.financeVisible), ServicesTeal.positive, Modifier.weight(1f))
            KpiChip("Paid", ServiceFormat.compact(model.currency, model.finance?.cashPaid, model.financeVisible), ServicesTeal.teal, Modifier.weight(1f))
        }
        val revisions = model.memory?.revisions.orEmpty()
        if (revisions.isNotEmpty()) {
            revisions.take(1).forEach {
                DetailRow(
                    eyebrow = "Plan revision",
                    value = it.title.ifBlank { "Target revision" },
                    trailing = ServiceFormat.shortDay(it.occurredAt) ?: "Recorded",
                    trailingColor = ServicesTeal.amber,
                )
            }
        }
        val rows = section.rows
        if (rows.isEmpty()) {
            TealEmpty("Nothing settled yet", section.detail.ifBlank { "Collections and payments will show here." })
        } else {
            Column {
                rows.take(3).forEach { row ->
                    DetailRow(
                        eyebrow = row.title.ifBlank { eventLabel(row.eventType) },
                        value = row.counterpartyName?.takeIf { it.isNotBlank() } ?: eventLabel(row.eventType),
                        trailing = ServiceFormat.compact(model.currency, row.amount, model.financeVisible),
                        trailingColor = ServicesTeal.textPrimary,
                    )
                }
            }
        }
        WhyNote("Plan revisions versioned; memory never overwrites prior targets.")
    }
}

/** AN-H03 · People & Activity `2399:31060` */
@Composable
private fun PeopleMemoryCard(model: OperatingProjectionDto, section: OperatingBoardSectionDto, onOpen: (ServiceSheetState) -> Unit) {
    val shifts = model.activity.count { it.eventType == "SHIFT" }
    TealCard(tone = TealTone.Board) {
        SectionHeader("People & Activity", onViewAll = { onOpen(ServiceSheetState.PeopleMemory) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiChip("People", model.participants.size.toString(), ServicesTeal.textPrimary, Modifier.weight(1f))
            KpiChip("Shifts", if (shifts > 0) shifts.toString() else "Unavailable", if (shifts > 0) ServicesTeal.textPrimary else ServicesTeal.warning, Modifier.weight(1f))
        }
        val rows = section.rows
        if (rows.isEmpty()) {
            TealEmpty("Nothing recorded yet", section.detail.ifBlank { "Who did what will show here." })
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.take(3).forEach { row ->
                    BoardRowFor(row = row, model = model, trailingOverride = if (row.approvalStatus == "PENDING") "Open" else "Settled")
                }
            }
        }
    }
}

/** AN-H04 · Patterns & Decisions `2399:31087` */
@Composable
private fun PatternsCard(model: OperatingProjectionDto, section: OperatingBoardSectionDto, onOpen: (ServiceSheetState) -> Unit) {
    val decisions = model.activity.count { it.eventType == "APPROVAL" }
    val approved = model.activity.count { it.eventType == "APPROVAL" && it.approvalStatus == "APPROVED" }
    TealCard(tone = TealTone.Board) {
        SectionHeader("Patterns & Decisions", onViewAll = { onOpen(ServiceSheetState.Patterns) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiChip("Decisions", decisions.toString(), ServicesTeal.textPrimary, Modifier.weight(1f))
            KpiChip("Approved", approved.toString(), ServicesTeal.positive, Modifier.weight(1f))
        }
        val rows = section.rows
        if (rows.isEmpty()) {
            TealEmpty("No patterns yet", section.detail.ifBlank { "Approvals and repeated issues will show here." })
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.take(3).forEach { row ->
                    val decided = row.approvalStatus == "APPROVED"
                    val denied = row.approvalStatus == "REJECTED"
                    BoardRowFor(
                        row = row,
                        model = model,
                        accent = when {
                            decided -> ServicesTeal.positive
                            denied -> ServicesTeal.negative
                            else -> ServicesTeal.amber
                        },
                        trailingOverride = when {
                            decided -> "Approved"
                            denied -> "Denied"
                            else -> "Open"
                        },
                    )
                }
            }
        }
        WhyNote("Approval ≠ disbursement.")
    }
}

/** Rose story CTA `2670:34495`. */
@Composable
private fun StoryCta(onTap: () -> Unit, month: String) {
    val shape = ServicesTeal.rStoryCta
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(8.dp, shape, clip = false, ambientColor = ServicesTeal.positive.copy(alpha = 0.35f), spotColor = ServicesTeal.positive.copy(alpha = 0.35f))
            .clip(shape)
            .background(ServicesTeal.teal)
            .clickable(onClick = onTap)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painterResource(R.drawable.qa_plus),
                contentDescription = null,
                tint = ServicesTeal.inkOnTeal,
                modifier = Modifier.size(15.dp),
            )
            Text("Generate Business Story", style = ServicesTeal.storyCtaTitle(), maxLines = 1)
        }
        Text(
            "Eight pages from recorded activity · $month preselected",
            style = ServicesTeal.storyCtaCaption(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ---------------------------------------------------------------------------------
// Shared row shapes
// ---------------------------------------------------------------------------------

/** Board list row with the inner `rgba(0,0,0,.28)` KPI well. */
@Composable
internal fun BoardRowFor(
    row: OperatingActivityDto,
    model: OperatingProjectionDto,
    accent: Color = ServicesTeal.teal,
    trailingOverride: String? = null,
) {
    val caption = buildString {
        row.counterpartyName?.takeIf { it.isNotBlank() }?.let { append(it) }
        if (row.channelName?.takeIf { it.isNotBlank() } != null) {
            if (isNotEmpty()) append(" · ")
            append(row.channelName)
        }
        row.quantity?.takeIf { it.isNotBlank() }?.let {
            if (isNotEmpty()) append(" · ")
            append(it)
            row.unit?.let { unit -> append(" $unit") }
        }
        if (isEmpty()) append(eventLabel(row.eventType))
    }
    BoardRow(
        eyebrow = row.title.ifBlank { eventLabel(row.eventType) },
        value = caption,
        valueColor = ServicesTeal.textBody,
        trailing = trailingOverride ?: row.paymentStatus?.let { prettyStatus(it) } ?: "Open",
        trailingColor = when {
            trailingOverride == "Done" || trailingOverride == "Settled" || trailingOverride == "Approved" -> ServicesTeal.positive
            trailingOverride == "Denied" -> ServicesTeal.negative
            trailingOverride == "Open" -> ServicesTeal.amber
            row.paymentStatus in setOf("COLLECTED", "PAID", "SETTLED") -> ServicesTeal.positive
            else -> ServicesTeal.textBody
        },
        accent = accent,
        onClick = { OperatingChromeState.editing = row },
    )
}

/** Board row in the `[eyebrow / value / trailing]` form used inside slate cards. */
@Composable
internal fun DetailRow(
    eyebrow: String,
    value: String,
    trailing: String,
    trailingColor: Color = ServicesTeal.textPrimary,
    accent: Color = ServicesTeal.teal,
) {
    BoardRow(
        eyebrow = eyebrow,
        value = value,
        valueColor = ServicesTeal.textBody,
        trailing = trailing,
        trailingColor = trailingColor,
        accent = accent,
    )
}

private fun prettyStatus(status: String): String = when (status) {
    "COLLECTED", "PAID", "SETTLED" -> "Done"
    "PENDING" -> "Open"
    "PARTIAL" -> "Part"
    "OVERDUE" -> "Overdue"
    "CANCELLED" -> "Void"
    else -> status.lowercase().replaceFirstChar { it.uppercase() }
}

internal fun eventLabel(code: String): String = when (code) {
    "SALE" -> "Sale"
    "PURCHASE" -> "Purchase"
    "SPEND" -> "Expense"
    "PAYMENT" -> "Payment"
    "COLLECTION" -> "Collection"
    "STOCK" -> "Stock move"
    "ORDER" -> "Order"
    "CUSTOMER_ORDER" -> "Order"
    "ISSUE" -> "Issue"
    "UPDATE" -> "Update"
    "APPROVAL" -> "Approval"
    "BUDGET" -> "Budget"
    "TARGET" -> "Target"
    "SHIFT" -> "Shift"
    "REVISION" -> "Revision"
    "SERVICE" -> "Service"
    "APPOINTMENT" -> "Booking"
    else -> code.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
}

private fun tabTitle(model: OperatingProjectionDto, tab: OperatingTab): String {
    val name = model.title?.takeIf { it.isNotBlank() } ?: "Consumer Service"
    return when (tab) {
        OperatingTab.PULSE -> "$name Pulse"
        OperatingTab.MOMENTS -> "Moments"
        OperatingTab.LIFE -> "Life"
        OperatingTab.MEMORY -> "Memory"
        OperatingTab.QUICK_ADD -> "Quick Adds"
    }
}

internal fun roleLabel(code: String?): String =
    OperatingMomentCatalog.roles.firstOrNull { it.first == code }?.second ?: "Consumer Service"

private fun dayOfPeriod(model: OperatingProjectionDto): String {
    val start = ServiceFormat.parse(model.startDate) ?: return "Unavailable"
    val today = java.time.LocalDate.now()
    if (today < start) return "0"
    val day = java.time.temporal.ChronoUnit.DAYS.between(start, today).toInt() + 1
    return day.toString()
}

/** `−6%` when the previous period is known, otherwise `Unavailable`. */
private fun lastPeriodDelta(model: OperatingProjectionDto): String {
    val monthly = model.boards?.memory.orEmpty().firstOrNull { it.code == "ME1" }?.rows.orEmpty()
    if (monthly.size < 2) return "Unavailable"
    val current = monthly[0].amount?.toBigDecimalOrNull() ?: return "Unavailable"
    val previous = monthly[1].amount?.toBigDecimalOrNull() ?: return "Unavailable"
    if (previous.signum() <= 0) return "Unavailable"
    val delta = (current - previous).divide(previous, 4, java.math.RoundingMode.HALF_UP)
        .multiply(java.math.BigDecimal(100))
        .setScale(0, java.math.RoundingMode.HALF_UP)
    val sign = if (delta.signum() < 0) "−" else "+"
    return "$sign${delta.abs().toPlainString()}%"
}

private fun averageAchievement(rows: List<OperatingActivityDto>, model: OperatingProjectionDto): String {
    val values = rows.mapNotNull { row ->
        Regex("""(\d+)%""").find(row.title)?.groupValues?.get(1)?.toIntOrNull()
    }
    if (values.isEmpty()) return "Unavailable"
    return "${values.average().toInt()}%"
}

// ---------------------------------------------------------------------------------
