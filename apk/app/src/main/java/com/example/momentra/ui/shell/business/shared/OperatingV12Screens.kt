package com.example.momentra.ui.shell.business.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R
import com.example.momentra.data.api.OperatingActivityDto
import com.example.momentra.data.api.OperatingAttentionDto
import com.example.momentra.data.api.OperatingBoardMetricDto
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlin.math.roundToInt

/**
 * TRD/DST v12 tab bodies — Pulse, Moments, Life, Memory.
 *
 * Shared across Trading and Distribution; only [OperatingV12Theme] changes.
 */

@Composable
fun OperatingV12PulseScreen(
    model: OperatingProjectionDto,
    theme: OperatingV12Theme,
    onCapture: (String) -> Unit,
    onViewAll: (String) -> Unit,
    onOpenRow: ((OperatingActivityDto) -> Unit)? = null,
) {
    val title = model.title.orEmpty().ifBlank { theme.label }
    val currency = model.currency
    val period = OperatingV12Date.monthYear(model.startDate) ?: "This month"
    val gstOn = model.gstTracking.equals("YES", ignoreCase = true)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OperatingV12PulseHero(
            theme = theme,
            chips = pulseHeroChips(model, theme),
            title = title,
            subtitle = "${OperatingV12Date.fiscalYear(model.startDate)} · ${currency ?: "INR"}",
            badge = if (model.retired) "Books closed" else "Books open · live",
        )
        OperatingV12QuickActions(theme = theme, tinted = true, radius = 16.dp, onOpen = onCapture)

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            attentionCard(model, theme, onViewAll)
            targetCard(model, theme, period, currency, onViewAll)
            paymentCard(model, theme, currency, onViewAll)
            if (model.financeVisible) financeCard(model, theme, currency, onViewAll)
            activityCard(model, theme, currency, onViewAll, onOpenRow)
            if (gstOn) gstCard(model, theme, currency, onViewAll)
        }
    }
}

private fun pulseHeroChips(model: OperatingProjectionDto, theme: OperatingV12Theme): List<String> {
    val chips = mutableListOf(model.title.orEmpty().ifBlank { theme.label })
    val state = model.gstState.orEmpty()
    val gstin = model.gstRegistration.orEmpty()
    if (state.isNotEmpty() || gstin.isNotEmpty()) {
        chips += "Channel / Route · ${state.ifBlank { "State" }} · GSTIN ${gstin.ifBlank { "Not recorded" }}"
    } else {
        chips += "${theme.label} · ${model.currency ?: "INR"}"
    }
    return chips
}

@Composable
private fun attentionCard(
    model: OperatingProjectionDto,
    theme: OperatingV12Theme,
    onViewAll: (String) -> Unit,
) {
    val group = model.pulseGroup("ATTENTION")
    val items = model.attention.take(5)
    OperatingV12Card(theme = theme, fill = OperatingV12.pulseSurface) {
        OperatingV12WidgetHeader(
            title = "Needs Attention",
            subtitle = "Severity then due · goods and money are separate axes",
            onViewAll = { onViewAll("ATTENTION") },
        )
        if (items.isEmpty()) {
            OperatingV12QuietRow("Nothing is waiting.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items.forEach { item ->
                    val chip = attentionChip(item)
                    OperatingV12Row(
                        title = item.title,
                        subtitle = attentionSubtitle(item, model),
                        chip = chip,
                        bar = OperatingV12.bar(chip),
                        accent = theme.accent,
                        onClick = {
                            openOperatingAttention(item, model)
                        },
                    )
                }
            }
        }
        OperatingV12Why(
            text = group?.why
                ?: "open orders, deliveries/receipts due, collections/payments due, issues — never conflate delivery with settlement.",
            tone = Color.White,
        )
    }
}

private fun attentionSubtitle(item: OperatingAttentionDto, model: OperatingProjectionDto): String? {
    val parts = buildList {
        item.counterpartyName?.takeIf { it.isNotBlank() }?.let(::add)
        OperatingV12Date.short(item.dueAt)?.let { add("due $it") }
        if (model.financeVisible && !item.amount.isNullOrBlank()) {
            add(OperatingV12Money.amount(item.amount, model.currency))
        }
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

private fun attentionChip(item: OperatingAttentionDto): String? = when (item.severity?.uppercase()) {
    "CRITICAL" -> "Overdue"
    "HIGH" -> "Alert"
    "MEDIUM" -> "Unavailable"
    else -> null
}

@Composable
private fun targetCard(
    model: OperatingProjectionDto,
    theme: OperatingV12Theme,
    period: String,
    currency: String?,
    onViewAll: (String) -> Unit,
) {
    val target = model.target
    val budget = model.budget
    val configured = target?.configured == true
    val targetValue = OperatingV12Money.amount(target?.value, currency)
    val achieved = OperatingV12Money.amount(target?.progress, currency)
    val targetNumber = OperatingV12Money.number(target?.value) ?: 0.0
    val progressNumber = OperatingV12Money.number(target?.progress) ?: 0.0
    val fraction = if (targetNumber > 0) progressNumber / targetNumber else 0.0
    val onTrack = targetNumber > 0 && progressNumber >= targetNumber * 0.7
    val spentNumber = OperatingV12Money.number(budget?.spent) ?: 0.0
    val amountNumber = OperatingV12Money.number(budget?.amount) ?: 0.0
    val usedPct = if (amountNumber > 0) ((spentNumber / amountNumber) * 100).roundToInt() else null

    OperatingV12Card(theme = theme, fill = OperatingV12.pulseSurface) {
        OperatingV12WidgetHeader(
            title = "Target & Budget",
            subtitle = "Period · $period · as-of effective plan",
            onViewAll = { onViewAll("TARGET_BUDGET") },
        )
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(theme.canvas)
                .border(1.dp, theme.accent.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OperatingV12Ring(
                fraction = fraction,
                caption = if (targetNumber > 0) "${(fraction * 100).roundToInt()}%" else "—",
                note = "of target",
                accent = theme.accent,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "ACHIEVEMENT VS TARGET",
                    color = theme.accent,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 0.6.sp,
                )
                Text(
                    if (configured && targetNumber > 0) "Sales $achieved of $targetValue" else "No target set",
                    color = Color.White,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 2,
                )
                if (configured && targetNumber > 0) {
                    val tone = if (onTrack) OperatingV12.green else OperatingV12.amber
                    Text(
                        if (onTrack) "● On track" else "● Behind plan",
                        color = tone,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(tone.copy(alpha = 0.16f))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OperatingV12Tile("Sales", achieved, modifier = Modifier.weight(1f))
            OperatingV12Tile("Target", targetValue, modifier = Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OperatingV12Tile("Budget used", usedPct?.let { "$it%" } ?: "—", tone = OperatingV12.cyan, modifier = Modifier.weight(1f))
            OperatingV12Tile("Spend", OperatingV12Money.amount(budget?.spent, currency), modifier = Modifier.weight(1f))
            OperatingV12Tile("Budget", OperatingV12Money.amount(budget?.amount, currency), modifier = Modifier.weight(1f))
        }
        OperatingV12Why(
            text = model.pulseGroup("TARGET_BUDGET")?.why
                ?: "sales ÷ effective target; spend ÷ effective budget. Empty ≠ zero — coverage noted when attribution is partial.",
            tone = Color.White,
        )
    }
}

@Composable
private fun paymentCard(
    model: OperatingProjectionDto,
    theme: OperatingV12Theme,
    currency: String?,
    onViewAll: (String) -> Unit,
) {
    val due = model.paymentReadiness
    val receivable = due.filter { it.relevance.equals("RECEIVABLE", ignoreCase = true) }
    val payable = due.filter { it.relevance.equals("PAYABLE", ignoreCase = true) }
    val receivableTotal = receivable.mapNotNull { OperatingV12Money.number(it.amount) }.sum()
    val payableTotal = payable.mapNotNull { OperatingV12Money.number(it.amount) }.sum()
    val overdueAr = receivable.filter { it.dueBucket.equals("OVERDUE", ignoreCase = true) }
        .mapNotNull { OperatingV12Money.number(it.amount) }.sum()
    val overdueAp = payable.filter { it.dueBucket.equals("OVERDUE", ignoreCase = true) }
        .mapNotNull { OperatingV12Money.number(it.amount) }.sum()

    OperatingV12Card(theme = theme, fill = OperatingV12.pulseSurface) {
        OperatingV12WidgetHeader(
            title = "Payment Readiness",
            subtitle = "Invoice totals include GST · dues with due dates",
            onViewAll = { onViewAll("PAYMENT_READINESS") },
        )
        if (due.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OperatingV12Tile("Collections due", OperatingV12Money.compact(receivableTotal, currency), tone = OperatingV12.red, modifier = Modifier.weight(1f))
                OperatingV12Tile("Overdue AR", OperatingV12Money.compact(overdueAr, currency), tone = OperatingV12.red, modifier = Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OperatingV12Tile("Supplier dues", OperatingV12Money.compact(payableTotal, currency), modifier = Modifier.weight(1f))
                OperatingV12Tile("Overdue AP", OperatingV12Money.compact(overdueAp, currency), tone = OperatingV12.red, modifier = Modifier.weight(1f))
            }
        } else {
            OperatingV12TileGrid(
                theme = theme,
                metrics = model.pulseGroup("PAYMENT_READINESS")?.metrics.orEmpty(),
                columns = 2,
                currency = currency,
            )
        }
        OperatingV12Why(
            text = model.pulseGroup("PAYMENT_READINESS")?.why
                ?: "open payable/receivable = charge − allocations/credits. No overdue alert without a due date.",
            tone = Color.White,
        )
    }
}

@Composable
private fun financeCard(
    model: OperatingProjectionDto,
    theme: OperatingV12Theme,
    currency: String?,
    onViewAll: (String) -> Unit,
) {
    val metrics = model.pulseGroup("FINANCE")?.metrics.orEmpty()
    OperatingV12Card(theme = theme, fill = OperatingV12.pulseSurface) {
        OperatingV12WidgetHeader(
            title = "Finance",
            subtitle = "Flows by event date · balances as-of end",
            onViewAll = { onViewAll("FINANCE") },
        )
        OperatingV12TileGrid(
            theme = theme,
            metrics = metrics,
            columns = if (metrics.size >= 5) 3 else 2,
            currency = currency,
        )
        OperatingV12Why(
            text = model.pulseGroup("FINANCE")?.why
                ?: "revenue/purchase tax-exclusive; AR/AP GST-inclusive. Sale ≠ receipt — cash only from receipts/payments.",
            tone = Color.White,
        )
    }
}

@Composable
private fun activityCard(
    model: OperatingProjectionDto,
    theme: OperatingV12Theme,
    currency: String?,
    onViewAll: (String) -> Unit,
    onOpenRow: ((OperatingActivityDto) -> Unit)?,
) {
    val group = model.pulseGroup("ACTIVITY")
    val rows = model.activity.take(4)
    OperatingV12Card(theme = theme, fill = OperatingV12.pulseSurface) {
        OperatingV12WidgetHeader(
            title = "Activity",
            subtitle = "Today · posted events only",
            onViewAll = { onViewAll("ACTIVITY") },
        )
        OperatingV12TileGrid(
            theme = theme,
            metrics = group?.metrics.orEmpty(),
            columns = 3,
            currency = currency,
        )
        if (rows.isEmpty()) {
            Text("Nothing recorded yet.", color = OperatingV12.subtitle, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                rows.forEach { row ->
                    Text(
                        activityLine(row, model),
                        color = OperatingV12.title,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (onOpenRow != null) Modifier.clickable { onOpenRow(row) } else Modifier,
                            ),
                    )
                }
            }
        }
        OperatingV12Why(
            text = group?.why
                ?: "chronological posted timeline; drafts/reversed distinguished; money vs operational events labeled.",
            tone = Color.White,
        )
    }
}

private fun activityLine(row: OperatingActivityDto, model: OperatingProjectionDto): String {
    val parts = mutableListOf("· ${row.title.ifBlank { row.eventType.replaceFirstChar { it.uppercase() } }}")
    if (model.financeVisible && !row.amount.isNullOrBlank()) {
        parts += OperatingV12Money.amount(row.amount, model.currency)
    }
    row.counterpartyName?.takeIf { it.isNotBlank() }?.let { parts += it }
    return parts.joinToString(" · ")
}

@Composable
private fun gstCard(
    model: OperatingProjectionDto,
    theme: OperatingV12Theme,
    currency: String?,
    onViewAll: (String) -> Unit,
) {
    val gst = model.gstPosition
    val difference = OperatingV12Money.number(gst?.difference) ?: 0.0
    val differenceText = OperatingV12Money.amount(gst?.difference, currency)
    OperatingV12Card(theme = theme, fill = OperatingV12.pulseSurface) {
        OperatingV12WidgetHeader(
            title = "GST Position",
            subtitle = "Current month · As of ${OperatingV12Date.monthDayYear(model.endDate) ?: OperatingV12Date.today()} · GST enabled",
            onViewAll = { onViewAll("GST") },
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OperatingV12Tile("GST on Sales", OperatingV12Money.amount(gst?.outputGst, currency), tone = theme.accent, modifier = Modifier.weight(1f))
            OperatingV12Tile("GST on Purchases", OperatingV12Money.amount(gst?.inputGst, currency), tone = OperatingV12.cyan, modifier = Modifier.weight(1f))
            OperatingV12Tile(
                "Est. difference",
                differenceText,
                tone = if (difference == 0.0) OperatingV12.title else OperatingV12.red,
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            "Estimated difference: $differenceText payable before adjustments",
            color = OperatingV12.subtitle,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
        )
        OperatingV12Why(
            text = model.pulseGroup("GST")?.why
                ?: "Output GST − Input GST (indicative). Never label as government refund due. Hidden when GST disabled.",
            tone = Color.White,
        )
    }
}

// ─── Moments ───────────────────────────────────────────────────────────────

@Composable
fun OperatingV12MomentsScreen(
    model: OperatingProjectionDto,
    theme: OperatingV12Theme,
    onCapture: (String) -> Unit,
    onViewAll: (String) -> Unit,
    onOpenRow: ((OperatingActivityDto) -> Unit)? = null,
) {
    val network = model.sectionByCode("network")
    val commitments = model.sectionByCode("M4")
    val title = model.title.orEmpty().ifBlank { theme.label }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OperatingV12Hero(
            theme = theme,
            eyebrow = "ACTIVE MOMENT",
            title = title,
            chips = listOf(
                OperatingV12ChipSpec(theme.label, theme.accent),
                OperatingV12ChipSpec(
                    if (model.retired) "Books closed" else "Books open",
                    OperatingV12.green,
                    dot = true,
                ),
            ),
            kpis = listOf(
                OperatingV12KpiSpec(
                    "People · ${theme.partyNoun}",
                    OperatingV12Metric.value(network, "buyers", model.currency) ?: "—",
                ),
                OperatingV12KpiSpec(
                    "AR open",
                    OperatingV12Metric.value(commitments, "toCollect", model.currency) ?: "—",
                    theme.accent,
                ),
            ),
        )
        OperatingV12QuickActions(theme = theme, onOpen = onCapture)
        sectionCard(model, theme, "M4", "Open Commitments", "What is still open · not alarms",
            listOf("toCollect", "toPay", "openOrders"),
            mapOf("AR" to "AR", "toCollect" to "AR", "toPay" to "AP", "openOrders" to "Open orders"),
            onViewAll, onOpenRow)
        sectionCard(model, theme, "orders", "Orders & Commitments", "Open buyer & supplier orders · commitment only",
            listOf("openOrders", "inTransit", "netSales"), emptyMap(), onViewAll, onOpenRow)
        sectionCard(model, theme, "stock", "Stock & Products", "Physical movements · warehouse + routes",
            listOf("onHand", "stockMoves", "low"), emptyMap(), onViewAll, onOpenRow)
        sectionCard(model, theme, "network", "People & Network", "Bound suppliers, buyers & routes",
            listOf("buyers", "suppliers", "channels"), emptyMap(), onViewAll, onOpenRow)
    }
}

// ─── Life ──────────────────────────────────────────────────────────────────

@Composable
fun OperatingV12LifeScreen(
    model: OperatingProjectionDto,
    theme: OperatingV12Theme,
    onCapture: (String) -> Unit,
    onViewAll: (String) -> Unit,
    onOpenRow: ((OperatingActivityDto) -> Unit)? = null,
) {
    val money = model.sectionByCode("L3")
    val title = model.title.orEmpty().ifBlank { theme.label }
    val openCount = money?.rows.orEmpty().count { row ->
        when (row.badge?.uppercase()) {
            "OPEN", "PART", "OVERDUE", "DUE", "TRANSIT" -> true
            else -> false
        }
    }
    val doneCount = money?.rows.orEmpty().count { row ->
        when (row.badge?.uppercase()) {
            "DONE", "SETTLED", "DELIVERED" -> true
            else -> false
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OperatingV12QuickActions(theme = theme, onOpen = onCapture)
        OperatingV12Hero(
            theme = theme,
            eyebrow = "TODAY",
            title = "$title · today",
            titleSize = 22,
            chips = listOf(
                OperatingV12ChipSpec("Today · ${OperatingV12Date.today()}", theme.accent),
                OperatingV12ChipSpec(
                    if (model.retired) "Books closed" else "Books open",
                    OperatingV12.green,
                    dot = true,
                ),
            ),
            kpis = listOf(
                OperatingV12KpiSpec("Open", "$openCount", theme.accent),
                OperatingV12KpiSpec("Done", "$doneCount", OperatingV12.green),
                OperatingV12KpiSpec(
                    "Money in motion",
                    OperatingV12Metric.value(money, "net", model.currency) ?: "—",
                ),
            ),
        )
        sectionCard(model, theme, "L1", "Current Business", "As-of today · ${OperatingV12Date.today()}",
            listOf("sales", "stockMoves", "issues"), emptyMap(), onViewAll, onOpenRow)
        sectionCard(
            model, theme, "L2",
            if (theme.isTrading) "Buyers & Channels" else "Dealers & Channels",
            "Channel / territory mix · this month",
            listOf("channels", "buyers", "sales"), emptyMap(), onViewAll, onOpenRow,
        )
        sectionCard(model, theme, "L3", "Money Moving", "Cash in / out · cash basis",
            listOf("collected", "paid", "net"), emptyMap(), onViewAll, onOpenRow)
        sectionCard(model, theme, "L4", "People & Activity", "Assigned follow-ups",
            listOf("stockMoves", "issues", "updates"), emptyMap(), onViewAll, onOpenRow)
    }
}

// ─── Memory ────────────────────────────────────────────────────────────────

@Composable
fun OperatingV12MemoryScreen(
    model: OperatingProjectionDto,
    theme: OperatingV12Theme,
    onCapture: (String) -> Unit,
    onStory: () -> Unit,
    onViewAll: (String) -> Unit,
    onOpenRow: ((OperatingActivityDto) -> Unit)? = null,
) {
    val monthly = model.sectionByCode("ME1")
    val money = model.sectionByCode("ME2")
    val period = OperatingV12Date.monthYear(model.startDate) ?: "This month"
    val title = model.title.orEmpty().ifBlank { theme.label }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OperatingV12QuickActions(theme = theme, onOpen = onCapture)
        OperatingV12Hero(
            theme = theme,
            eyebrow = "MEMORY · CLOSED PERIOD",
            title = "$title · memory",
            titleSize = 22,
            chips = listOf(
                OperatingV12ChipSpec("$period · target v2", theme.accent),
                OperatingV12ChipSpec("vs last period"),
            ),
            kpis = listOf(
                OperatingV12KpiSpec("Sales", OperatingV12Metric.value(monthly, "netSales", model.currency) ?: "—", Color.White),
                OperatingV12KpiSpec("Records", OperatingV12Metric.value(monthly, "records", model.currency) ?: "—", OperatingV12.cyan),
                OperatingV12KpiSpec("Collected", OperatingV12Metric.value(money, "collected", model.currency) ?: "—", theme.accent),
            ),
        )
        storyCta(theme, period, onStory)
        sectionCard(model, theme, "ME1", "Monthly Memory", "Closed periods · target v2 ($period)",
            listOf("netSales", "months", "records"), emptyMap(), onViewAll, onOpenRow)
        sectionCard(model, theme, "ME2", "Money Memory", "Settled chains · FY 2026–27",
            listOf("collected", "paid", "credits"), emptyMap(), onViewAll, onOpenRow)
        sectionCard(model, theme, "fulfilment", "Fulfilment Memory", "Order → dispatch → route delivery",
            listOf("delivered", "received", "returns"), emptyMap(), onViewAll, onOpenRow)
        sectionCard(model, theme, "ME4", "People & Patterns", "Repeat behaviour · 6 months",
            listOf("issues", "approvals"), emptyMap(), onViewAll, onOpenRow, columns = 2)
    }
}

@Composable
private fun storyCta(theme: OperatingV12Theme, period: String, onStory: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(18.dp), ambientColor = theme.accent.copy(alpha = 0.35f), spotColor = theme.accent.copy(alpha = 0.35f))
            .clip(RoundedCornerShape(18.dp))
            .background(theme.accent)
            .clickable(onClick = onStory)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_business_empty_sparkline),
            contentDescription = null,
            tint = OperatingV12.deepInk,
            modifier = Modifier.padding(bottom = 2.dp),
        )
        Text("Generate Business Story", color = OperatingV12.deepInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Black, fontSize = 15.sp)
        Text(
            "Eight pages from recorded activity · $period preselected",
            color = OperatingV12.deepInk.copy(alpha = 0.8f),
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun sectionCard(
    model: OperatingProjectionDto,
    theme: OperatingV12Theme,
    code: String,
    title: String,
    subtitle: String,
    keys: List<String>,
    labels: Map<String, String>,
    onViewAll: (String) -> Unit,
    onOpenRow: ((OperatingActivityDto) -> Unit)?,
    columns: Int = 3,
) {
    val section = model.sectionByCode(code)
    val metrics = OperatingV12Tiles.ordered(section, keys, columns, model.currency).map { metric ->
        val label = labels[metric.key]
        if (label == null) metric else metric.copy(label = label)
    }
    val rows = section?.rows.orEmpty().take(3)
    OperatingV12Card(theme = theme) {
        OperatingV12WidgetHeader(title = title, subtitle = subtitle, onViewAll = { onViewAll(code) })
        OperatingV12TileGrid(theme = theme, metrics = metrics, columns = if (metrics.size == 2) 2 else columns, currency = model.currency)
        if (rows.isEmpty()) {
            Text("Not recorded yet.", color = OperatingV12.subtitle, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                rows.forEach { row ->
                    OperatingV12Row(
                        title = row.title,
                        subtitle = row.subtitle,
                        trailing = rowTrailing(row, model),
                        chip = row.badge,
                        bar = OperatingV12.bar(row.badge),
                        accent = theme.accent,
                        onClick = onOpenRow?.let { { it(row) } },
                    )
                }
            }
        }
        OperatingV12Why(section?.why)
    }
}

private fun rowTrailing(row: OperatingActivityDto, model: OperatingProjectionDto): String? {
    if (model.financeVisible && !row.amount.isNullOrBlank()) {
        return OperatingV12Money.amount(row.amount, model.currency)
    }
    if (!row.quantity.isNullOrBlank()) {
        return listOfNotNull(row.quantity, row.unit).joinToString(" ")
    }
    return null
}
