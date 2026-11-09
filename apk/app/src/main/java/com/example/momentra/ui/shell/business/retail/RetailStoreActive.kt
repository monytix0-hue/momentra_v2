package com.example.momentra.ui.shell.business.retail

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
import com.example.momentra.ui.theme.RetailRose

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
fun RetailStoreActive(
    model: OperatingProjectionDto,
    tab: OperatingTab,
    onOpenQuickAdd: () -> Unit,
    onRecord: suspend (RetailEventDraft) -> Unit,
    onDecide: suspend (String, String) -> Unit,
    momentId: String? = null,
    modifier: Modifier = Modifier,
) {
    var sheet by remember { mutableStateOf<RetailSheetState?>(null) }
    var storyConfig by remember { mutableStateOf(false) }

    RoseScreenBackground(modifier) {
        when (tab) {
            OperatingTab.QUICK_ADD -> RetailQuickAddHub(
                model = model,
                onRecord = onRecord,
                onDecide = onDecide,
                momentId = momentId,
            )

            else -> Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = RetailRose.gutter, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(RetailRose.sectionGap),
            ) {
                ScreenHeader(
                    title = tabTitle(model, tab),
                    roleLine = "${roleLabel(model.role)} ${model.partyName?.takeIf { it.isNotBlank() } ?: ""}".trim() +
                        " · ${model.subtypeLabel ?: "Retail Store"}",
                )
                ContextChips(
                    subtypeLabel = model.subtypeLabel ?: "Retail Store",
                    periodLine = RetailFormat.periodLine(model.startDate, model.currency),
                    gstOn = model.gstTracking == "YES",
                )
                when (tab) {
                    OperatingTab.PULSE -> RetailPulse(
                        model = model,
                        onOpenQuickAdd = onOpenQuickAdd,
                        onOpenSheet = { sheet = it },
                    )
                    OperatingTab.MOMENTS -> RetailMoments(
                        model = model,
                        onOpenQuickAdd = onOpenQuickAdd,
                        onOpenSheet = { sheet = it },
                    )
                    OperatingTab.LIFE -> RetailLife(model = model, onOpenSheet = { sheet = it })
                    OperatingTab.MEMORY -> RetailMemory(
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
        RetailBoardSheet(
            state = open,
            model = model,
            onDismiss = { sheet = null },
            onOpenQuickAdd = onOpenQuickAdd,
        )
    }
    if (storyConfig && momentId != null) {
        com.example.momentra.ui.shell.business.shared.OperatingSheetScrim(
            onDismiss = { storyConfig = false },
            accent = RetailRose.rose,
        ) {
            RetailStoryConfigSheet(
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
private fun RetailPulse(
    model: OperatingProjectionDto,
    onOpenQuickAdd: () -> Unit,
    onOpenSheet: (RetailSheetState) -> Unit,
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
    val heroShape = RetailRose.rHero
    val identity = (model.title ?: "Retail Store").uppercase()
    val location = model.location?.takeIf { it.isNotBlank() }?.uppercase()
    val alerts = model.attention.size
    val receivable = RetailFormat.compact(model.currency, model.finance?.outstanding, model.financeVisible)
    val sales = RetailFormat.compact(
        model.currency,
        model.finance?.sales ?: model.target?.progress,
        model.financeVisible,
    )
    val target = if (model.target?.configured == true) {
        RetailFormat.compact(model.currency, model.target?.value, model.financeVisible)
    } else {
        null
    }
    val summary = buildString {
        append(if (target != null) "Sales $sales / $target" else "Sales $sales")
        append(" · Stock alerts $alerts")
        append(" · AR $receivable open")
    }

    Box(
        Modifier
            .fillMaxWidth()
            .shadow(12.dp, heroShape, clip = false, ambientColor = RetailRose.indigo.copy(alpha = 0.5f), spotColor = RetailRose.indigo.copy(alpha = 0.5f))
            .shadow(24.dp, heroShape, clip = false, ambientColor = RetailRose.rose.copy(alpha = 0.3f), spotColor = RetailRose.rose.copy(alpha = 0.3f))
            .clip(heroShape)
            .background(Brush.linearGradient(listOf(RetailRose.rose, RetailRose.rose)))
            .padding(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            Text(
                listOfNotNull(identity, location).joinToString(" · "),
                style = RetailRose.heroEyebrow(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text("Stock. Sell. Settle.", style = RetailRose.heroHeadline(), maxLines = 1)
            Text(
                summary,
                style = RetailRose.heroBody(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 56.dp),
            )
            if ("SALE" in model.quickAdds.map { it.code }) {
                Spacer(Modifier.height(4.dp))
                RoseCta("+  Record Sale", onClick = onOpenQuickAdd)
            }
        }
        HeroGlyph()
    }
}

/** 40dp rose-tinted glyph well pinned to the hero's top-right. */
@Composable
private fun HeroGlyph() {
    val shape = RetailRose.rHeroIcon
    Box(
        Modifier
            .size(40.dp)
            .shadow(12.dp, shape, clip = false, ambientColor = RetailRose.rose.copy(alpha = 0.5f), spotColor = RetailRose.rose.copy(alpha = 0.5f))
            .clip(shape)
            .background(RetailRose.surfaceRow.copy(alpha = 0.9f))
            .border(1.dp, RetailRose.rose, shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(R.drawable.ic_retail_hero_glyph),
            contentDescription = null,
            tint = RetailRose.rose,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** AN-P01 · Needs Attention `2399:29710` */
@Composable
private fun AttentionCard(
    model: OperatingProjectionDto,
    onOpen: (RetailSheetState) -> Unit,
    onOpenQuickAdd: () -> Unit,
) {
    val lowStock = model.attention.count { it.kind.startsWith("LOW_STOCK") }
    RoseCard {
        SectionHeader(
            title = "Needs Attention",
            chipText = if (model.attention.isEmpty()) null else "Alert",
            onViewAll = { onOpen(RetailSheetState.Attention) },
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricChip("Alerts", model.attention.size.toString(), RetailRose.rose, Modifier.weight(1f))
            MetricChip("Reorder", lowStock.toString(), RetailRose.amber, Modifier.weight(1f))
        }
        if (model.attention.isEmpty()) {
            RoseEmpty("Nothing is waiting", "Approvals, overdue items and blockers will show here.")
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
private fun AttentionRow(
    item: OperatingAttentionDto,
    model: OperatingProjectionDto,
    onOpenQuickAdd: () -> Unit,
) {
    val hasSource = item.sourceEventId != null || item.id.startsWith("open-") || item.cta != null
    val label = retailSeverityLabel(item.kind, item.severity, hasSource)
    val color = if (!hasSource) {
        RetailRose.warning
    } else {
        retailSeverityColor(item.severity)
    }
    val caption = listOfNotNull(
        item.counterpartyName?.takeIf { it.isNotBlank() },
        RetailFormat.shortDay(item.dueAt)?.let { "Due $it" },
        RetailFormat.compact(model.currency, item.amount, model.financeVisible).takeIf { it != "—" },
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
private fun TargetBudgetCard(model: OperatingProjectionDto, onOpen: (RetailSheetState) -> Unit) {
    val target = model.target
    val budget = model.budget
    val configured = target?.configured == true
    val percentText = if (configured) {
        RetailFormat.percent(target?.progress, target?.value) ?: "Unavailable"
    } else {
        "Unavailable"
    }
    val fraction = if (configured) RetailFormat.fraction(target?.progress, target?.value) else null
    val sales = RetailFormat.compact(model.currency, target?.progress ?: model.finance?.sales, model.financeVisible)
    val targetValue = if (configured) RetailFormat.compact(model.currency, target?.value, model.financeVisible) else null
    val budgetPercent = if (budget?.configured == true) {
        RetailFormat.percent(budget?.spent, budget?.amount) ?: "Unavailable"
    } else {
        "Unavailable"
    }
    val onTrack = fraction != null && fraction >= 0.75f
    val behind = fraction != null && fraction < 0.5f

    RoseCard {
        SectionHeader("Target & Budget", onViewAll = { onOpen(RetailSheetState.Target) })
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(RetailRose.surfaceRow)
                .border(1.dp, RetailRose.rose.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
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
                    style = RetailRose.boardEyebrow().copy(letterSpacing = 0.6.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                )
                Text(
                    if (configured && targetValue != null) "Sales $sales of $targetValue" else "Sales $sales · no target set",
                    style = RetailRose.sectionTitle().copy(fontSize = 15.sp),
                    maxLines = 2,
                )
                when {
                    fraction == null -> StatusPill("● Unavailable", RetailRose.warning)
                    behind -> StatusPill("● Behind", RetailRose.negative)
                    onTrack -> StatusPill("● On track", RetailRose.positive)
                    else -> StatusPill("● In progress", RetailRose.amber)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricChip("Sales", sales, RetailRose.textPrimary, Modifier.weight(1f))
            MetricChip(
                "Budget use",
                budgetPercent,
                if (budgetPercent == "Unavailable") RetailRose.warning else RetailRose.textPrimary,
                Modifier.weight(1f),
            )
        }
        Column {
            DividedRow(
                title = "Sales vs target",
                caption = "Effective ${RetailFormat.month(model.startDate)} · store",
                trailingValue = if (configured && targetValue != null) "$sales / $targetValue" else "Unavailable",
                trailingColor = if (configured) RetailRose.textPrimary else RetailRose.warning,
            )
            DividedRow(
                title = "Eligible spend",
                caption = "COGS + running",
                trailingValue = RetailFormat.compact(model.currency, model.finance?.eligibleCost, model.financeVisible),
            )
            DividedRow(
                title = "Footfall attributed",
                caption = target?.coverage ?: "Coverage unavailable",
                trailingValue = if (target?.coverage.isNullOrBlank()) "Unavailable" else target?.coverage.orEmpty(),
                trailingColor = if (target?.coverage.isNullOrBlank()) RetailRose.warning else RetailRose.textPrimary,
                captionColor = if (target?.coverage.isNullOrBlank()) RetailRose.warning else RetailRose.textBody,
            )
        }
        WhyCaption("Achievement = eligible actual ÷ effective target. Missing coverage shows Unavailable — never invent 0%.")
    }
}

/** AN-P03 · Payment Readiness `2399:29742` */
@Composable
private fun PaymentReadinessCard(model: OperatingProjectionDto, onOpen: (RetailSheetState) -> Unit) {
    val payables = model.paymentReadiness.filter { it.relevance != "RECEIVABLE" }
    val paid = model.paymentReadiness
        .filter { it.relevance != "RECEIVABLE" && it.status in setOf("PAID", "SETTLED") }
        .sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
    RoseCard {
        SectionHeader("Payment Readiness", onViewAll = { onOpen(RetailSheetState.Payment) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricChip("AP open", RetailFormat.compact(model.currency, model.finance?.payable, model.financeVisible), RetailRose.amber, Modifier.weight(1f))
            MetricChip(
                "Paid",
                RetailFormat.compact(model.currency, if (paid > 0) paid.toString() else model.finance?.cashPaid, model.financeVisible),
                RetailRose.positive,
                Modifier.weight(1f),
            )
        }
        if (payables.isEmpty()) {
            RoseEmpty("No open payments", "Purchases and expenses waiting to settle will show here.")
        } else {
            Column {
                payables.take(3).forEach { row ->
                    val caption = listOfNotNull(
                        row.counterpartyName?.takeIf { it.isNotBlank() },
                        RetailFormat.shortDay(row.dueAt)?.let { "Due $it" },
                    ).joinToString(" · ").ifBlank { row.status.lowercase().replaceFirstChar { it.uppercase() } }
                    DividedRow(
                        title = row.title.ifBlank { row.counterpartyName ?: "Open payable" },
                        caption = caption,
                        trailingValue = RetailFormat.compact(model.currency, row.amount, model.financeVisible),
                        trailingColor = if (row.dueBucket == "OVERDUE") RetailRose.negative else RetailRose.amber,
                        captionColor = if (row.dueBucket == "OVERDUE") RetailRose.negative else RetailRose.textBody,
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
private fun FinanceCard(model: OperatingProjectionDto, onOpen: (RetailSheetState) -> Unit) {
    val finance = model.finance
    val receivables = model.paymentReadiness.filter { it.relevance == "RECEIVABLE" }
    RoseCard {
        SectionHeader("Finance", onViewAll = { onOpen(RetailSheetState.Finance) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricChip("Sales", RetailFormat.compact(model.currency, finance?.sales, model.financeVisible), RetailRose.textPrimary, Modifier.weight(1f))
            MetricChip("Collected", RetailFormat.compact(model.currency, finance?.received, model.financeVisible), RetailRose.positive, Modifier.weight(1f))
            MetricChip("AR", RetailFormat.compact(model.currency, finance?.outstanding, model.financeVisible), RetailRose.rose, Modifier.weight(1f))
        }
        if (!model.financeVisible) {
            RoseEmpty("Totals stay private", "Finance, Approver and Owner see the money position.")
        } else if (receivables.isEmpty()) {
            RoseEmpty("Nothing to collect", "Credit sales waiting on collection will show here.")
        } else {
            Column {
                receivables.take(3).forEach { row ->
                    DividedRow(
                        title = (row.counterpartyName ?: row.title).ifBlank { "Open receivable" },
                        caption = RetailFormat.shortDay(row.dueAt)?.let { "Due $it" } ?: "Still open",
                        trailingValue = RetailFormat.compact(model.currency, row.amount, true),
                        trailingColor = if (row.dueBucket == "OVERDUE") RetailRose.negative else RetailRose.rose,
                        captionColor = if (row.dueBucket == "OVERDUE") RetailRose.negative else RetailRose.textBody,
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
private fun ActivityCard(model: OperatingProjectionDto, onOpen: (RetailSheetState) -> Unit) {
    val sales = model.activity.count { it.eventType == "SALE" }
    val purchases = model.activity.count { it.eventType == "PURCHASE" }
    val stockMoves = model.activity.count { it.eventType == "STOCK" }
    RoseCard {
        SectionHeader("Activity", onViewAll = { onOpen(RetailSheetState.Activity) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricChip("Sales", sales.toString(), RetailRose.textPrimary, Modifier.weight(1f))
            MetricChip("Purchases", purchases.toString(), RetailRose.textPrimary, Modifier.weight(1f))
            MetricChip("Stock moves", stockMoves.toString(), RetailRose.textPrimary, Modifier.weight(1f))
        }
        if (model.activity.isEmpty()) {
            RoseEmpty("Nothing recorded yet", "Sales, purchases and stock moves will show here.")
        } else {
            Column {
                model.activity.take(3).forEach { row -> ActivityDividedRow(row, model) }
            }
        }
        WhyCaption("Stock changes only on explicit receipt, sale fulfilment or adjustment — never from billing alone.")
    }
}

@Composable
private fun ActivityDividedRow(row: OperatingActivityDto, model: OperatingProjectionDto) {
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
        "Done" -> RetailRose.positive
        "Pending" -> RetailRose.amber
        else -> RetailRose.textBody
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
private fun GstCard(model: OperatingProjectionDto, onOpen: (RetailSheetState) -> Unit) {
    val gst = model.gstPosition
    val complete = !gst?.outputGst.isNullOrBlank() && !gst?.inputGst.isNullOrBlank()
    RoseCard {
        SectionHeader("GST Position", onViewAll = { onOpen(RetailSheetState.Gst) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricChip("Output", RetailFormat.compact(model.currency, gst?.outputGst, true), RetailRose.textPrimary, Modifier.weight(1f))
            MetricChip("ITC", RetailFormat.compact(model.currency, gst?.inputGst, true), RetailRose.positive, Modifier.weight(1f))
            MetricChip("Net", RetailFormat.compact(model.currency, gst?.difference, true), RetailRose.amber, Modifier.weight(1f))
        }
        if (!complete) {
            RoseEmpty("Registration incomplete", "GST stays Unavailable until the registration is complete.")
        } else {
            Column {
                val sources = gst?.sources.orEmpty()
                if (sources.isEmpty()) {
                    DividedRow("Outward taxable", "Posted sales", RetailFormat.compact(model.currency, model.finance?.sales, true))
                    DividedRow("Inward eligible", "Posted purchases", RetailFormat.compact(model.currency, model.finance?.eligibleCost, true))
                } else {
                    sources.take(3).forEach { source ->
                        DividedRow(
                            title = source.title.ifBlank { eventLabel(source.eventType) },
                            caption = if (source.direction == "OUTPUT") "Outward taxable" else "Inward eligible",
                            trailingValue = RetailFormat.compact(model.currency, source.gstTotal, true),
                            trailingColor = if (source.direction == "OUTPUT") RetailRose.textPrimary else RetailRose.positive,
                        )
                    }
                }
                DividedRow(
                    title = "Net payable",
                    caption = RetailFormat.asOf(gst?.asOfMonth?.let { "$it-01" }),
                    trailingValue = RetailFormat.compact(model.currency, gst?.difference, true),
                    trailingColor = RetailRose.amber,
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
private fun RetailMoments(
    model: OperatingProjectionDto,
    onOpenQuickAdd: () -> Unit,
    onOpenSheet: (RetailSheetState) -> Unit,
) {
    MomentsHero(model)
    OpenCommitmentsCard(model, onOpenSheet)
    MyBusinessCard(model, onOpenSheet)
    PeopleCard(model, onOpenSheet)
    NetworkCard(model, onOpenSheet)
    RoseCard(tone = RoseTone.Board, onClick = onOpenQuickAdd) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SheetIconWell(RetailTileIcons.sale)
            Column(Modifier.weight(1f)) {
                Text("Record the next store moment", style = RetailRose.sectionTitle(), maxLines = 2)
                Text("Sale, purchase, collection or adjustment.", style = RetailRose.rowCaption())
            }
            Text("›", style = RetailRose.sectionTitle().copy(color = RetailRose.rose))
        }
    }
}

@Composable
private fun MomentsHero(model: OperatingProjectionDto) {
    val staff = model.participants.size
    val suppliers = model.masters.orEmpty().count { it.kind == "SUPPLIER" }
    BoardHero(
        eyebrow = "ACTIVE MOMENT",
        headline = model.title?.ifBlank { "Retail Store" } ?: "Retail Store",
        chips = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BoardHeroChip(model.subtypeLabel ?: "Retail", RetailRose.rose)
                if (!model.location.isNullOrBlank()) {
                    BoardHeroChip(model.location!!, RetailRose.positive, dot = true)
                }
            }
        },
        kpis = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiChip("People · Staff", staff.toString(), RetailRose.textPrimary, Modifier.weight(1f))
                KpiChip("Network · Suppliers", suppliers.toString(), RetailRose.textPrimary, Modifier.weight(1f))
                KpiChip(
                    "AP open",
                    RetailFormat.compact(model.currency, model.finance?.payable, model.financeVisible),
                    RetailRose.amber,
                    Modifier.weight(1f),
                )
            }
        },
    )
}

/** AN-M04 · Open Commitments `2399:30767` */
@Composable
private fun OpenCommitmentsCard(model: OperatingProjectionDto, onOpen: (RetailSheetState) -> Unit) {
    val openOrders = model.activity.count {
        (it.eventType == "ORDER" || it.eventType == "CUSTOMER_ORDER") &&
            it.status !in setOf("COMPLETED", "CANCELLED")
    }
    val rows = model.boards?.moments.orEmpty().firstOrNull { it.code == "M4" }?.rows.orEmpty()
    RoseCard(tone = RoseTone.Board, strokeAlpha = 0.40f) {
        SectionHeader("Open Commitments", onViewAll = { onOpen(RetailSheetState.Commitments) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiChip("AP", RetailFormat.compact(model.currency, model.finance?.payable, model.financeVisible), RetailRose.amber, Modifier.weight(1f))
            KpiChip("AR", RetailFormat.compact(model.currency, model.finance?.outstanding, model.financeVisible), RetailRose.rose, Modifier.weight(1f))
            KpiChip("Orders", openOrders.toString(), RetailRose.textPrimary, Modifier.weight(1f))
        }
        if (rows.isEmpty()) {
            RoseEmpty("Nothing open", "Orders, payables and receivables will show here.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.take(3).forEach { row -> BoardRowFor(row, model) }
            }
        }
        WhyNote("Orders are commitments — revenue only on posted sale.")
    }
}

/** AN-M01 · My Retail Business `2399:30674` */
@Composable
private fun MyBusinessCard(model: OperatingProjectionDto, onOpen: (RetailSheetState) -> Unit) {
    val dayOfPeriod = dayOfPeriod(model)
    RoseCard(tone = RoseTone.Board) {
        SectionHeader("My Retail Business", onViewAll = { onOpen(RetailSheetState.MomentDetail) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiChip("Status", if (model.retired) "Closed" else "Live", if (model.retired) RetailRose.textMuted else RetailRose.positive, Modifier.weight(1f))
            KpiChip("Channel", model.life?.channels?.firstOrNull() ?: "Unavailable", RetailRose.textPrimary, Modifier.weight(1f))
            KpiChip("Day", dayOfPeriod, RetailRose.textPrimary, Modifier.weight(1f))
        }
        Column {
            DetailRow(
                eyebrow = "Business",
                value = listOfNotNull(model.title, model.location?.takeIf { it.isNotBlank() }).joinToString(" · ").ifBlank { "Unavailable" },
                trailing = if (model.retired) "Closed" else "Live",
                trailingColor = if (model.retired) RetailRose.textMuted else RetailRose.positive,
            )
            DetailRow(
                eyebrow = "Plan",
                value = if (model.target?.configured == true) {
                    "Sales ${RetailFormat.compact(model.currency, model.target?.value, model.financeVisible)} target"
                } else {
                    "No target set"
                },
                trailing = if (model.target?.configured == true) "Active" else "Unavailable",
                trailingColor = if (model.target?.configured == true) RetailRose.textPrimary else RetailRose.warning,
            )
            DetailRow(
                eyebrow = "GST",
                value = when {
                    model.gstTracking != "YES" -> "Not tracked"
                    model.gstRegistration.isNullOrBlank() -> "Registration incomplete"
                    else -> "Registered${model.gstState?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""}"
                },
                trailing = if (model.gstTracking == "YES") "On" else "Off",
                trailingColor = RetailRose.amber,
            )
        }
        WhyNote("One Moment holds catalogue, stock, sales and money. Quick amount never invents SKU movement.")
    }
}

/** AN-M02 · People `2399:30707` */
@Composable
private fun PeopleCard(model: OperatingProjectionDto, onOpen: (RetailSheetState) -> Unit) {
    val staff = model.participants
    RoseCard(tone = RoseTone.Board) {
        SectionHeader("People", onViewAll = { onOpen(RetailSheetState.People) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiChip("Staff", staff.size.toString(), RetailRose.textPrimary, Modifier.weight(1f))
            KpiChip(
                "Approvers",
                if (model.role == "OWNER_ADMIN" || model.role == "APPROVER") "1" else "Unavailable",
                if (model.role == "OWNER_ADMIN" || model.role == "APPROVER") RetailRose.amber else RetailRose.warning,
                Modifier.weight(1f),
            )
        }
        if (staff.isEmpty()) {
            RoseEmpty("No people yet", "Invite the shop floor, billing and approvers.")
        } else {
            Column {
                staff.take(3).forEach { name ->
                    val role = if (name == model.partyName) roleLabel(model.role) else "Member"
                    DetailRow(
                        eyebrow = name,
                        value = role,
                        trailing = if (name == model.partyName) "Owner" else "Member",
                        trailingColor = if (name == model.partyName) RetailRose.textPrimary else RetailRose.textBody,
                    )
                }
            }
        }
        WhyNote("Roles gate credit sales, stock adjustments and payments.")
    }
}

/** AN-M03 · Business Network `2399:30737` */
@Composable
private fun NetworkCard(model: OperatingProjectionDto, onOpen: (RetailSheetState) -> Unit) {
    val masters = model.masters.orEmpty()
    val suppliers = masters.filter { it.kind == "SUPPLIER" }
    val buyers = masters.filter { it.kind == "CUSTOMER" || it.kind == "BUYER" }
    val payables = model.paymentReadiness.filter { it.relevance != "RECEIVABLE" }
    val receivables = model.paymentReadiness.filter { it.relevance == "RECEIVABLE" }
    val combined = (suppliers.map { it.name to true } + buyers.map { it.name to false }).distinctBy { it.first }
    RoseCard(tone = RoseTone.Board) {
        SectionHeader("Business Network", onViewAll = { onOpen(RetailSheetState.Network) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiChip("Suppliers", suppliers.size.toString(), RetailRose.textPrimary, Modifier.weight(1f))
            KpiChip("Credit buyers", buyers.size.toString(), RetailRose.textPrimary, Modifier.weight(1f))
        }
        if (combined.isEmpty()) {
            RoseEmpty("No network yet", "Suppliers and credit buyers you record will show here.")
        } else {
            Column {
                combined.take(3).forEach { (name, isSupplier) ->
                    val openAp = payables.filter { it.counterpartyName.equals(name, true) }.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
                    val openAr = receivables.filter { it.counterpartyName.equals(name, true) }.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
                    val trailing = when {
                        isSupplier && openAp > 0 -> "AP ${RetailFormat.compact(model.currency, openAp.toString(), model.financeVisible)}"
                        !isSupplier && openAr > 0 -> "AR ${RetailFormat.compact(model.currency, openAr.toString(), model.financeVisible)}"
                        else -> "No balance"
                    }
                    DetailRow(
                        eyebrow = name,
                        value = if (isSupplier) "Supplier" else "Credit buyer",
                        trailing = trailing,
                        trailingColor = when {
                            isSupplier && openAp > 0 -> RetailRose.amber
                            !isSupplier && openAr > 0 -> RetailRose.rose
                            else -> RetailRose.textMuted
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
private fun RetailLife(model: OperatingProjectionDto, onOpenSheet: (RetailSheetState) -> Unit) {
    val open = model.life?.openCommitments ?: 0
    val done = model.activity.count { it.paymentStatus in setOf("COLLECTED", "PAID", "SETTLED") }
    BoardHero(
        eyebrow = "TODAY",
        headline = "${model.title?.ifBlank { "Retail Store" } ?: "Retail Store"} · today",
        strokeAlpha = 0.45f,
        chips = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BoardHeroChip(RetailFormat.todayLine(), RetailRose.rose)
                BoardHeroChip(if (model.retired) "Store closed" else "Store open", if (model.retired) RetailRose.textMuted else RetailRose.positive, dot = true)
            }
        },
        kpis = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiChip("Open", open.toString(), RetailRose.rose, Modifier.weight(1f))
                KpiChip("Done", done.toString(), RetailRose.positive, Modifier.weight(1f))
                KpiChip(
                    "Money in motion",
                    RetailFormat.compact(model.currency, model.finance?.received, model.financeVisible),
                    RetailRose.textPrimary,
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
private fun LifeCurrentCard(model: OperatingProjectionDto, section: OperatingBoardSectionDto?, onOpen: (RetailSheetState) -> Unit) {
    val target = model.target
    val percent = if (target?.configured == true) RetailFormat.percent(target?.progress, target?.value) ?: "Unavailable" else "Unavailable"
    RoseCard(tone = RoseTone.Board) {
        SectionHeader("Current Business", onViewAll = { onOpen(RetailSheetState.LifeCurrent) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiChip(
                "Sales today",
                RetailFormat.compact(model.currency, model.finance?.sales, model.financeVisible),
                RetailRose.positive,
                Modifier.weight(1f),
            )
            KpiChip("Achv.", percent, if (percent == "Unavailable") RetailRose.warning else RetailRose.amber, Modifier.weight(1f))
        }
        val rows = section?.rows.orEmpty()
        if (rows.isEmpty()) {
            RoseEmpty("Nothing recorded yet", section?.detail ?: "Today's posted sales and stock moves will show here.")
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
            SeverityChip(section?.coverage.orEmpty(), RetailRose.warning)
        }
        WhyNote("Today shows posted sales and explicit stock moves only.")
    }
}

/** AN-L02 · Sales & Orders `2399:30864` */
@Composable
private fun LifeSalesCard(model: OperatingProjectionDto, life: List<OperatingBoardSectionDto>, onOpen: (RetailSheetState) -> Unit) {
    val tickets = model.activity.count { it.eventType == "SALE" }
    val creditOpen = model.paymentReadiness.count { it.relevance == "RECEIVABLE" }
    val holds = model.activity.count { (it.eventType == "ORDER" || it.eventType == "CUSTOMER_ORDER") && it.status !in setOf("COMPLETED", "CANCELLED") }
    val channelSection = life.firstOrNull { it.code == "L2" }
    RoseCard(tone = RoseTone.Board) {
        SectionHeader("Sales & Orders", onViewAll = { onOpen(RetailSheetState.LifeSales) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiChip("Tickets", tickets.toString(), RetailRose.textPrimary, Modifier.weight(1f))
            KpiChip("Credit open", creditOpen.toString(), RetailRose.rose, Modifier.weight(1f))
            KpiChip("Holds", holds.toString(), RetailRose.amber, Modifier.weight(1f))
        }
        if (channelSection != null && channelSection.rows.isNotEmpty()) {
            Column {
                channelSection.rows.take(3).forEach { row ->
                    DetailRow(
                        eyebrow = row.channelName ?: row.title.ifBlank { "Channel" },
                        value = row.title.ifBlank { "Recorded" },
                        trailing = RetailFormat.compact(model.currency, row.amount, model.financeVisible),
                        trailingColor = RetailRose.textPrimary,
                    )
                }
            }
        } else if (!channelSection?.detail.isNullOrBlank()) {
            RoseEmpty("Counter mix", channelSection?.detail.orEmpty())
        } else {
            RoseEmpty("Nothing recorded yet", "Sales, credit and holds will show here.")
        }
        WhyNote("Order value stays separate from posted sale.")
    }
}

/** AN-L03 · Money Moving `2399:30897` */
@Composable
private fun LifeMoneyCard(model: OperatingProjectionDto, section: OperatingBoardSectionDto?, onOpen: (RetailSheetState) -> Unit) {
    RoseCard(tone = RoseTone.Board, strokeAlpha = 0.45f) {
        SectionHeader("Money Moving", onViewAll = { onOpen(RetailSheetState.LifeMoney) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiChip("Collected", RetailFormat.compact(model.currency, model.finance?.received, model.financeVisible), RetailRose.positive, Modifier.weight(1f))
            KpiChip("Paid", RetailFormat.compact(model.currency, model.finance?.cashPaid, model.financeVisible), RetailRose.textPrimary, Modifier.weight(1f))
            KpiChip("AR move", RetailFormat.compact(model.currency, model.finance?.outstanding, model.financeVisible), RetailRose.rose, Modifier.weight(1f))
        }
        val rows = section?.rows.orEmpty()
        if (rows.isEmpty()) {
            RoseEmpty("Nothing moved yet", section?.detail ?: "Collections and payments will show here.")
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

/** AN-L04 · Operations / Stock `2399:30930` */
@Composable
private fun LifeOpsCard(model: OperatingProjectionDto, life: List<OperatingBoardSectionDto>, onOpen: (RetailSheetState) -> Unit) {
    val receipts = model.activity.count { it.eventType == "PURCHASE" }
    val adjustments = model.activity.count { it.eventType == "STOCK" }
    val alerts = model.attention.size
    val section = life.firstOrNull { it.code == "L4" }
    RoseCard(tone = RoseTone.Board) {
        SectionHeader("Operations / Stock", onViewAll = { onOpen(RetailSheetState.LifeOps) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiChip("Receipts", receipts.toString(), RetailRose.textPrimary, Modifier.weight(1f))
            KpiChip("Adjustments", adjustments.toString(), RetailRose.textPrimary, Modifier.weight(1f))
            KpiChip("Alerts", alerts.toString(), RetailRose.rose, Modifier.weight(1f))
        }
        val rows = section?.rows.orEmpty()
        if (rows.isEmpty()) {
            RoseEmpty("Nothing moving", section?.detail ?: "Receipts, counts and damage will show here.")
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
private fun RetailMemory(
    model: OperatingProjectionDto,
    momentId: String?,
    onOpenStoryConfig: () -> Unit,
    onOpenSheet: (RetailSheetState) -> Unit,
) {
    val target = model.target
    val percent = if (target?.configured == true) RetailFormat.percent(target?.progress, target?.value) else null
    BoardHero(
        eyebrow = "MEMORY · THIS MONTH",
        headline = "${model.title?.ifBlank { "Retail Store" } ?: "Retail Store"} · memory",
        strokeAlpha = 0.45f,
        chips = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val period = RetailFormat.month(model.startDate)
                BoardHeroChip(
                    if (percent != null) "$period · $percent of ${RetailFormat.compact(model.currency, target?.value, model.financeVisible)}" else "$period · memory",
                    RetailRose.rose,
                )
                if (model.memory?.revisions.orEmpty().isNotEmpty()) {
                    BoardHeroChip("${model.memory?.revisions?.size} revisions", RetailRose.textMuted)
                }
            }
        },
        kpis = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiChip("Sales", RetailFormat.compact(model.currency, model.finance?.sales, model.financeVisible), RetailRose.textPrimary, Modifier.weight(1f))
                KpiChip(
                    "vs last period",
                    lastPeriodDelta(model),
                    if (lastPeriodDelta(model) == "Unavailable") RetailRose.warning else RetailRose.warning,
                    Modifier.weight(1f),
                )
                KpiChip("Supplier paid", RetailFormat.compact(model.currency, model.finance?.cashPaid, model.financeVisible), RetailRose.rose, Modifier.weight(1f))
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
        month = RetailFormat.month(model.startDate),
    )
}

/** AN-H01 · Monthly Memory `2399:30997` */
@Composable
private fun MonthlyMemoryCard(model: OperatingProjectionDto, section: OperatingBoardSectionDto?, onOpen: (RetailSheetState) -> Unit) {
    val months = section?.rows.orEmpty().filter { it.eventType == "MONTH" || it.eventType.isBlank() || it.eventType == "REVISION" }
    RoseCard(tone = RoseTone.Board) {
        SectionHeader("Monthly Memory", onViewAll = { onOpen(RetailSheetState.Monthly) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiChip("Months", months.size.toString(), RetailRose.textPrimary, Modifier.weight(1f))
            KpiChip(
                "Avg achv.",
                averageAchievement(months, model),
                if (averageAchievement(months, model) == "Unavailable") RetailRose.warning else RetailRose.amber,
                Modifier.weight(1f),
            )
        }
        if (months.isEmpty()) {
            RoseEmpty("No history yet", section?.detail ?: "Closed months will show here.")
        } else {
            Column {
                months.take(3).forEach { row ->
                    val parts = row.title.split("·").map { it.trim() }
                    DetailRow(
                        eyebrow = parts.firstOrNull().orEmpty().ifBlank { RetailFormat.month(row.occurredAt) },
                        value = parts.drop(1).joinToString(" · ").ifBlank { RetailFormat.compact(model.currency, row.amount, model.financeVisible) },
                        trailing = "Recorded",
                        trailingColor = RetailRose.textBody,
                    )
                }
            }
        }
        if (!section?.coverage.isNullOrBlank()) SeverityChip(section?.coverage.orEmpty(), RetailRose.warning)
        WhyNote("Missing coverage stays unavailable — never averaged as zero.")
    }
}

/** AN-H02 · Money Memory `2399:31027` */
@Composable
private fun MoneyMemoryCard(model: OperatingProjectionDto, section: OperatingBoardSectionDto, onOpen: (RetailSheetState) -> Unit) {
    RoseCard(tone = RoseTone.Board, strokeAlpha = 0.50f) {
        SectionHeader("Money Memory", onViewAll = { onOpen(RetailSheetState.MoneyMemory) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiChip("Sales", RetailFormat.compact(model.currency, model.finance?.sales, model.financeVisible), RetailRose.textPrimary, Modifier.weight(1f))
            KpiChip("Collected", RetailFormat.compact(model.currency, model.finance?.received, model.financeVisible), RetailRose.positive, Modifier.weight(1f))
            KpiChip("Paid", RetailFormat.compact(model.currency, model.finance?.cashPaid, model.financeVisible), RetailRose.rose, Modifier.weight(1f))
        }
        val revisions = model.memory?.revisions.orEmpty()
        if (revisions.isNotEmpty()) {
            revisions.take(1).forEach {
                DetailRow(
                    eyebrow = "Plan revision",
                    value = it.title.ifBlank { "Target revision" },
                    trailing = RetailFormat.shortDay(it.occurredAt) ?: "Recorded",
                    trailingColor = RetailRose.amber,
                )
            }
        }
        val rows = section.rows
        if (rows.isEmpty()) {
            RoseEmpty("Nothing settled yet", section.detail.ifBlank { "Collections and payments will show here." })
        } else {
            Column {
                rows.take(3).forEach { row ->
                    DetailRow(
                        eyebrow = row.title.ifBlank { eventLabel(row.eventType) },
                        value = row.counterpartyName?.takeIf { it.isNotBlank() } ?: eventLabel(row.eventType),
                        trailing = RetailFormat.compact(model.currency, row.amount, model.financeVisible),
                        trailingColor = RetailRose.textPrimary,
                    )
                }
            }
        }
        WhyNote("Plan revisions versioned; memory never overwrites prior targets.")
    }
}

/** AN-H03 · People & Activity `2399:31060` */
@Composable
private fun PeopleMemoryCard(model: OperatingProjectionDto, section: OperatingBoardSectionDto, onOpen: (RetailSheetState) -> Unit) {
    val shifts = model.activity.count { it.eventType == "SHIFT" }
    RoseCard(tone = RoseTone.Board) {
        SectionHeader("People & Activity", onViewAll = { onOpen(RetailSheetState.PeopleMemory) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiChip("People", model.participants.size.toString(), RetailRose.textPrimary, Modifier.weight(1f))
            KpiChip("Shifts", if (shifts > 0) shifts.toString() else "Unavailable", if (shifts > 0) RetailRose.textPrimary else RetailRose.warning, Modifier.weight(1f))
        }
        val rows = section.rows
        if (rows.isEmpty()) {
            RoseEmpty("Nothing recorded yet", section.detail.ifBlank { "Who did what will show here." })
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
private fun PatternsCard(model: OperatingProjectionDto, section: OperatingBoardSectionDto, onOpen: (RetailSheetState) -> Unit) {
    val decisions = model.activity.count { it.eventType == "APPROVAL" }
    val approved = model.activity.count { it.eventType == "APPROVAL" && it.approvalStatus == "APPROVED" }
    RoseCard(tone = RoseTone.Board) {
        SectionHeader("Patterns & Decisions", onViewAll = { onOpen(RetailSheetState.Patterns) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiChip("Decisions", decisions.toString(), RetailRose.textPrimary, Modifier.weight(1f))
            KpiChip("Approved", approved.toString(), RetailRose.positive, Modifier.weight(1f))
        }
        val rows = section.rows
        if (rows.isEmpty()) {
            RoseEmpty("No patterns yet", section.detail.ifBlank { "Approvals and repeated issues will show here." })
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.take(3).forEach { row ->
                    val decided = row.approvalStatus == "APPROVED"
                    val denied = row.approvalStatus == "REJECTED"
                    BoardRowFor(
                        row = row,
                        model = model,
                        accent = when {
                            decided -> RetailRose.positive
                            denied -> RetailRose.negative
                            else -> RetailRose.amber
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
    val shape = RetailRose.rStoryCta
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(8.dp, shape, clip = false, ambientColor = RetailRose.positive.copy(alpha = 0.35f), spotColor = RetailRose.positive.copy(alpha = 0.35f))
            .clip(shape)
            .background(RetailRose.rose)
            .clickable(onClick = onTap)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painterResource(R.drawable.ic_retail_plus),
                contentDescription = null,
                tint = RetailRose.inkOnRose,
                modifier = Modifier.size(15.dp),
            )
            Text("Generate Business Story", style = RetailRose.storyCtaTitle(), maxLines = 1)
        }
        Text(
            "Eight pages from recorded activity · $month preselected",
            style = RetailRose.storyCtaCaption(),
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
private fun BoardRowFor(
    row: OperatingActivityDto,
    model: OperatingProjectionDto,
    accent: Color = RetailRose.rose,
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
        valueColor = RetailRose.textBody,
        trailing = trailingOverride ?: row.paymentStatus?.let { prettyStatus(it) } ?: "Open",
        trailingColor = when {
            trailingOverride == "Done" || trailingOverride == "Settled" || trailingOverride == "Approved" -> RetailRose.positive
            trailingOverride == "Denied" -> RetailRose.negative
            trailingOverride == "Open" -> RetailRose.amber
            row.paymentStatus in setOf("COLLECTED", "PAID", "SETTLED") -> RetailRose.positive
            else -> RetailRose.textBody
        },
        accent = accent,
        onClick = { OperatingChromeState.editing = row },
    )
}

/** Board row in the `[eyebrow / value / trailing]` form used inside slate cards. */
@Composable
private fun DetailRow(
    eyebrow: String,
    value: String,
    trailing: String,
    trailingColor: Color = RetailRose.textPrimary,
    accent: Color = RetailRose.rose,
) {
    BoardRow(
        eyebrow = eyebrow,
        value = value,
        valueColor = RetailRose.textBody,
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

private fun eventLabel(code: String): String = when (code) {
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
    val name = model.title?.takeIf { it.isNotBlank() } ?: "Retail Store"
    return when (tab) {
        OperatingTab.PULSE -> "$name Pulse"
        OperatingTab.MOMENTS -> "Moments"
        OperatingTab.LIFE -> "Life"
        OperatingTab.MEMORY -> "Memory"
        OperatingTab.QUICK_ADD -> "Quick Adds"
    }
}

private fun roleLabel(code: String?): String =
    OperatingMomentCatalog.roles.firstOrNull { it.first == code }?.second ?: "Retail Store"

private fun dayOfPeriod(model: OperatingProjectionDto): String {
    val start = RetailFormat.parse(model.startDate) ?: return "Unavailable"
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
// View-all sheets
// ---------------------------------------------------------------------------------

/** Which board a `View all ›` opens. */
sealed interface RetailSheetState {
    data object Attention : RetailSheetState
    data object Target : RetailSheetState
    data object Payment : RetailSheetState
    data object Finance : RetailSheetState
    data object Activity : RetailSheetState
    data object Gst : RetailSheetState
    data object Commitments : RetailSheetState
    data object MomentDetail : RetailSheetState
    data object People : RetailSheetState
    data object Network : RetailSheetState
    data object LifeCurrent : RetailSheetState
    data object LifeSales : RetailSheetState
    data object LifeMoney : RetailSheetState
    data object LifeOps : RetailSheetState
    data object Monthly : RetailSheetState
    data object MoneyMemory : RetailSheetState
    data object PeopleMemory : RetailSheetState
    data object Patterns : RetailSheetState
}

@Composable
private fun RetailBoardSheet(
    state: RetailSheetState,
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onOpenQuickAdd: () -> Unit,
) {
    val title = when (state) {
        RetailSheetState.Attention -> "Needs Attention"
        RetailSheetState.Target -> "Target & Budget"
        RetailSheetState.Payment -> "Payment Readiness"
        RetailSheetState.Finance -> "Finance"
        RetailSheetState.Activity -> "Activity"
        RetailSheetState.Gst -> "GST Position"
        RetailSheetState.Commitments -> "Open Commitments"
        RetailSheetState.MomentDetail -> "My Retail Business"
        RetailSheetState.People -> "People"
        RetailSheetState.Network -> "Business Network"
        RetailSheetState.LifeCurrent -> "Current Business"
        RetailSheetState.LifeSales -> "Sales & Orders"
        RetailSheetState.LifeMoney -> "Money Moving"
        RetailSheetState.LifeOps -> "Operations / Stock"
        RetailSheetState.Monthly -> "Monthly Memory"
        RetailSheetState.MoneyMemory -> "Money Memory"
        RetailSheetState.PeopleMemory -> "People & Activity"
        RetailSheetState.Patterns -> "Patterns & Decisions"
    }
    com.example.momentra.ui.shell.business.shared.OperatingSheetScrim(
        onDismiss = onDismiss,
        accent = RetailRose.rose,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = RetailRose.gutter, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth().height(22.dp), horizontalArrangement = Arrangement.Center) {
                Box(
                    Modifier
                        .width(40.dp).height(5.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                )
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(title, style = RetailRose.screenTitle(), modifier = Modifier.weight(1f))
                Text(
                    "Close",
                    style = RetailRose.viewAll().copy(color = RetailRose.textBody),
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
            Text(RetailFormat.periodLine(model.startDate, model.currency), style = RetailRose.roleLine(), maxLines = 1)
            when (state) {
                RetailSheetState.Attention -> SheetAttention(model, onOpenQuickAdd)
                RetailSheetState.Payment -> SheetPayment(model, receivables = false)
                RetailSheetState.Finance -> SheetPayment(model, receivables = true)
                RetailSheetState.Activity, RetailSheetState.LifeSales -> SheetActivity(model)
                RetailSheetState.Commitments -> SheetSection(model, "M4")
                RetailSheetState.MomentDetail -> SheetSection(model, "M1")
                RetailSheetState.People -> SheetPeople(model)
                RetailSheetState.Network -> SheetSection(model, "M3")
                RetailSheetState.LifeCurrent -> SheetSection(model, "L1")
                RetailSheetState.LifeMoney -> SheetSection(model, "L3")
                RetailSheetState.LifeOps -> SheetSection(model, "L4")
                RetailSheetState.Monthly -> SheetSection(model, "ME1")
                RetailSheetState.MoneyMemory -> SheetSection(model, "ME2")
                RetailSheetState.PeopleMemory -> SheetSection(model, "ME3")
                RetailSheetState.Patterns -> SheetSection(model, "ME4")
                RetailSheetState.Target -> SheetTarget(model)
                RetailSheetState.Gst -> SheetGst(model)
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SheetAttention(model: OperatingProjectionDto, onOpenQuickAdd: () -> Unit) {
    if (model.attention.isEmpty()) {
        RoseEmpty("Nothing is waiting", "Approvals, overdue items and blockers will show here.")
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        model.attention.forEach { item -> AttentionRow(item, model, onOpenQuickAdd) }
    }
}

@Composable
private fun SheetPayment(model: OperatingProjectionDto, receivables: Boolean) {
    val rows: List<OperatingPaymentDto> = model.paymentReadiness.filter {
        if (receivables) it.relevance == "RECEIVABLE" else it.relevance != "RECEIVABLE"
    }
    if (rows.isEmpty()) {
        RoseEmpty(
            if (receivables) "Nothing to collect" else "Nothing to pay",
            if (receivables) "Credit sales waiting on collection will show here." else "Purchases waiting to settle will show here.",
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            AccentRow(
                title = row.title.ifBlank { row.counterpartyName ?: "Open balance" },
                caption = listOfNotNull(
                    row.counterpartyName?.takeIf { it.isNotBlank() },
                    RetailFormat.shortDay(row.dueAt)?.let { "Due $it" },
                    row.status.lowercase().replaceFirstChar { c -> c.uppercase() },
                ).joinToString(" · "),
                accent = when (row.dueBucket) {
                    "OVERDUE" -> RetailRose.negative
                    "DUE_SOON" -> RetailRose.warning
                    else -> RetailRose.rose
                },
                trailingValue = RetailFormat.compact(model.currency, row.amount, model.financeVisible),
                trailingColor = if (row.dueBucket == "OVERDUE") RetailRose.negative else RetailRose.textPrimary,
                onClick = { OperatingChromeState.settling = row },
            )
        }
    }
}

@Composable
private fun SheetActivity(model: OperatingProjectionDto) {
    if (model.activity.isEmpty()) {
        RoseEmpty("Nothing recorded yet", "Sales, spend and settlement will show here.")
        return
    }
    Column {
        model.activity.forEach { row -> ActivityDividedRow(row, model) }
    }
}

@Composable
private fun SheetPeople(model: OperatingProjectionDto) {
    if (model.participants.isEmpty()) {
        RoseEmpty("No people yet", "Invite the shop floor, billing and approvers.")
        return
    }
    Column {
        model.participants.forEach { name ->
            DividedRow(
                title = name,
                caption = if (name == model.partyName) roleLabel(model.role) else "Member",
                trailingValue = if (name == model.partyName) "Owner" else "Member",
                trailingColor = RetailRose.textBody,
            )
        }
    }
}

@Composable
private fun SheetSection(model: OperatingProjectionDto, code: String) {
    val section = (model.boards?.moments.orEmpty() + model.boards?.life.orEmpty() + model.boards?.memory.orEmpty())
        .firstOrNull { it.code == code }
    if (section == null) {
        RoseEmpty("Not available", "This board has not been recorded yet.")
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (section.detail.isNotBlank()) {
            RoseCard(tone = RoseTone.Board) {
                Text(
                    RetailFormat.detail(model.currency, section.detail),
                    style = RetailRose.rowCaption(),
                )
                if (!section.coverage.isNullOrBlank()) SeverityChip(section.coverage!!, RetailRose.warning)
            }
        }
        if (section.rows.isEmpty()) {
            RoseEmpty("Nothing recorded yet", "Rows will appear as activity is recorded.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                section.rows.forEach { row -> BoardRowFor(row = row, model = model) }
            }
        }
    }
}

@Composable
private fun SheetTarget(model: OperatingProjectionDto) {
    val target = model.target
    val budget = model.budget
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DetailRow(
            eyebrow = "Sales vs target",
            value = when {
                target?.configured != true -> "No target set"
                else -> "${RetailFormat.compact(model.currency, target?.progress, model.financeVisible)} of ${RetailFormat.compact(model.currency, target?.value, model.financeVisible)}"
            },
            trailing = if (target?.configured == true) {
                RetailFormat.percent(target?.progress, target?.value) ?: "Unavailable"
            } else {
                "Unavailable"
            },
            trailingColor = RetailRose.amber,
        )
        DetailRow(
            eyebrow = "Budget",
            value = when {
                budget?.configured != true -> "No period budget"
                else -> "${RetailFormat.compact(model.currency, budget?.spent, true)} of ${RetailFormat.compact(model.currency, budget?.amount, true)}"
            },
            trailing = if (budget?.configured == true) {
                RetailFormat.percent(budget?.spent, budget?.amount) ?: "Unavailable"
            } else {
                "Unavailable"
            },
            trailingColor = RetailRose.textPrimary,
        )
        DetailRow(
            eyebrow = "Budget remaining",
            value = "After eligible spend",
            trailing = if (budget?.configured == true) RetailFormat.compact(model.currency, budget?.remaining, true) else "Unavailable",
            trailingColor = if (budget?.configured == true) RetailRose.positive else RetailRose.warning,
        )
    }
}

@Composable
private fun SheetGst(model: OperatingProjectionDto) {
    val gst = model.gstPosition
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DetailRow("GST on sales", "Outward taxable", RetailFormat.compact(model.currency, gst?.outputGst, true))
        DetailRow("GST on purchases", "Inward eligible", RetailFormat.compact(model.currency, gst?.inputGst, true), RetailRose.positive)
        DetailRow("Estimated difference", RetailFormat.asOf(gst?.asOfMonth?.let { "$it-01" }), RetailFormat.compact(model.currency, gst?.difference, true), RetailRose.amber)
        WhyCaption("Indicative only. This is not a tax filing or an ITC claim.")
    }
}
