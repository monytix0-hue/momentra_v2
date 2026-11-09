package com.example.momentra.ui.shell.business.trading

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.OperatingActivityDto
import com.example.momentra.data.api.OperatingBoardSectionDto
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.shell.business.shared.BusinessActivityList
import com.example.momentra.ui.shell.business.shared.BusinessEmptyState
import com.example.momentra.ui.shell.business.shared.OperatingBadgeChip
import com.example.momentra.ui.shell.business.shared.OperatingMetricTiles
import com.example.momentra.ui.shell.business.shared.OperatingMomentCatalog
import com.example.momentra.ui.shell.business.shared.OperatingTab
import com.example.momentra.ui.shell.business.shared.OperatingWhyFootnote
import com.example.momentra.ui.theme.PlusJakartaSans
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// Domain palette "trading" in design/tokens/business.tokens.json (TRD-PULSE-v12 2566:29337).
// The old values were the amber BusinessRunway palette; Figma binds emerald here.
internal val TradeBg = Color(0xFF090D16) // domain.trading.canvas
internal val TradeCard = Color(0xFF0F1623) // domain.trading.surface
internal val TradeNotice = Color(0xFF0F1623) // domain.trading.canvasTop
internal val TradeField = Color(0xFF090D16) // domain.trading.inset
internal val TradeBorder = Color(0xFF1E293B) // color.border.default
internal val TradeBrand = Color(0xFF10B981) // domain.trading.accent
internal val TradeAccent = Color(0xFF10B981) // domain.trading.accent
internal val TradeSecondary = Color(0xFF94A3B8) // color.text.muted
internal val TradeInk = Color(0xFFFFFFFF) // color.text.primary
internal val TradeSuccess = Color(0xFF6EE7B7) // domain.trading.accentSoft

private val dayFormat = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)

@Composable
fun TradingActive(
    model: OperatingProjectionDto,
    tab: OperatingTab,
    onOpenQuickAdd: () -> Unit,
    onRecord: suspend (TradingEventDraft) -> Unit,
    onDecide: suspend (String, String) -> Unit,
    companyId: String? = null,
    modifier: Modifier = Modifier,
) {
    // One sheet slot for all eighteen view-all surfaces; `null` means no sheet is open.
    var sheet by remember { mutableStateOf<TradingSheetState?>(null) }
    val openSheet: (TradingSheetState) -> Unit = { sheet = it }
    Box(modifier.fillMaxSize().background(TradeBg)) {
    if (tab == OperatingTab.QUICK_ADD) {
        TradingQuickAddHub(model, onRecord, onDecide)
    } else {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TradeBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 17.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        when (tab) {
            OperatingTab.PULSE -> TradingPulse(model, onViewAll = openSheet, onOpenQuickAdd = onOpenQuickAdd)
            OperatingTab.MOMENTS -> TradingMoments(model, onOpenQuickAdd, openSheet)
            OperatingTab.LIFE -> TradingLife(model, openSheet)
            OperatingTab.MEMORY -> TradingMemory(model, openSheet)
            OperatingTab.QUICK_ADD -> Unit
        }
    }
    }
    sheet?.let { state ->
        TradingBoardSheet(state, model, TradeSheetTheme, onDismiss = { sheet = null })
    }
    com.example.momentra.ui.shell.business.shared.OperatingChromeSheets(model, companyId)
    }
}

/** Figma `TRD-SHEET` tokens: emerald accent on the trading canvas. */
internal val TradeSheetTheme = com.example.momentra.ui.shell.business.shared.OperatingSheetTheme(
    accent = TradeBrand,
    accentSoft = TradeSuccess,
    canvas = TradeBg,
    surface = TradeCard,
    inset = TradeField,
    border = TradeBorder,
    ink = TradeInk,
    body = TradeSecondary,
    warning = com.example.momentra.ui.shell.business.shared.OperatingSheetWarning,
    negative = com.example.momentra.ui.shell.business.shared.OperatingSheetNegative,
)

/** Board section code → the view-all surface it opens, or `null` when there is none. */
internal fun tradingSheetForCode(code: String): TradingSheetState? = when (code) {
    "orders" -> TradingSheetState.OrdersCommitments
    "stock" -> TradingSheetState.StockProducts
    "network" -> TradingSheetState.PeopleNetwork
    "M4" -> TradingSheetState.OpenCommitments
    "L1" -> TradingSheetState.LifeCurrentBusiness
    "L2" -> TradingSheetState.LifeDealersChannels
    "L3" -> TradingSheetState.LifeMoneyMoving
    "L4" -> TradingSheetState.LifePeopleActivity
    "ME1" -> TradingSheetState.MonthlyMemory
    "ME2" -> TradingSheetState.MoneyMemory
    "fulfilment" -> TradingSheetState.FulfilmentMemory
    "ME4" -> TradingSheetState.PeoplePatterns
    else -> null
}

@Composable
private fun TradingPulse(
    model: OperatingProjectionDto,
    onViewAll: (TradingSheetState) -> Unit,
    onOpenQuickAdd: () -> Unit = {},
) {
    Header(model, "Pulse", "Your sales plan, trade commitments and ready money.")
    if (model.financeVisible) {
        ViewAllSectionTitle("Target & Budget") { onViewAll(TradingSheetState.TargetBudget) }
        TradeCard {
            val target = model.target
            val budget = model.budget
            Text("Recorded sales", color = TradeSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
            Text(money(model, recordedSales(model)), color = TradeInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 28.sp)
            if (target?.configured == true) {
                Text("of ${money(model, target.value)} sales target", color = TradeSecondary, fontFamily = PlusJakartaSans, fontSize = 13.sp)
                Meter("Sales progress", fraction(recordedSales(model), target.value), TradeBrand)
            } else {
                Text("No sales target set for this period.", color = TradeSecondary, fontFamily = PlusJakartaSans, fontSize = 13.sp)
            }
            if (budget?.configured == true) {
                Meter("Budget used", fraction(budget.spent, budget.amount), TradeSuccess)
                Line("Budget remaining", money(model, budget.remaining), TradeSuccess)
            } else {
                Text("No period budget set.", color = TradeSecondary, fontFamily = PlusJakartaSans, fontSize = 13.sp)
            }
            Text("Validated sales count toward the target. Eligible purchase and expense facts count once toward budget.", color = TradeSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        }
        ViewAllSectionTitle("Needs Attention") { onViewAll(TradingSheetState.NeedsAttention) }
        if (model.attention.isEmpty()) {
            BusinessEmptyState(
            title = "Nothing is waiting",
            body = "Approvals, overdue items, and blockers will show here.",
            accent = TradeAccent,
            glyph = "✅",
            compact = true,
        )
        }
        model.attention.forEach { item ->
            AttentionCard(item, model, onOpenQuickAdd)
        }
        ViewAllSectionTitle("Payment Readiness") { onViewAll(TradingSheetState.PaymentReadiness) }
        val toCollect = model.paymentReadiness.filter { it.relevance == "RECEIVABLE" }
        val toPay = model.paymentReadiness.filter { it.relevance != "RECEIVABLE" }
        ReadinessGroup("To collect", toCollect, model)
        ReadinessGroup("To pay", toPay, model)
        if (model.paymentReadiness.isEmpty()) {
            TradeCard { Text("No open payments or collections.", color = TradeSecondary, fontFamily = PlusJakartaSans, fontSize = 14.sp) }
        }
        ViewAllSectionTitle("Finance") { onViewAll(TradingSheetState.Finance) }
        TradeCard {
            val finance = model.finance
            Line("Recorded sales", money(model, finance?.sales), TradeAccent)
            Line("Received", money(model, finance?.received), TradeSuccess)
            Line("Still to collect", money(model, finance?.outstanding), TradeAccent)
            Line("Eligible cost", money(model, finance?.eligibleCost), TradeInk)
            Line("Business cash paid", money(model, finance?.cashPaid), TradeInk)
            Line("Still to pay", money(model, finance?.payable), TradeAccent)
            if (model.gstTracking == "YES") {
                Text("Recorded sales exclude GST. Amounts still to collect or pay include GST.", color = TradeSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
            }
        }
        GstPositionCard(model)
    } else {
        StaffPulse(model, onOpenQuickAdd)
    }
    ViewAllSectionTitle("Activity") { onViewAll(TradingSheetState.Activity) }
    val recent = model.activity.take(5)
    if (recent.isEmpty()) {
        BusinessEmptyState(
            title = "Nothing recorded yet",
            body = "Sales, purchases, and money collection will show here.",
            accent = TradeAccent,
            glyph = "🧾",
            actionLabel = "Record a sale",
            onAction = onOpenQuickAdd,
        )
    } else {
        BusinessActivityList(
            rows = recent,
            model = model,
            accent = TradeAccent,
            onEdit = { com.example.momentra.ui.shell.business.shared.OperatingChromeState.editing = it },
        )
    }
    Text(
        "View all activity",
        color = TradeAccent,
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, TradeBrand, RoundedCornerShape(14.dp))
            .clickable { onViewAll(TradingSheetState.Activity) }
            .padding(vertical = 14.dp),
    )
}

@Composable
private fun StaffPulse(model: OperatingProjectionDto, onOpenQuickAdd: () -> Unit = {}) {
    Pill(roleLabel(model.role))
    TradeCard {
        Text("Your view", color = TradeInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        Text("Assigned work stays visible. Protected amounts stay hidden.", color = TradeSecondary, fontFamily = PlusJakartaSans, fontSize = 13.sp)
    }
    model.attention.forEach { item ->
        AttentionCard(item, model, onOpenQuickAdd)
    }
    TradeCard {
        Text("Finance totals stay with Finance, Approver, and Owner.", color = TradeInk, fontFamily = PlusJakartaSans, fontSize = 15.sp, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun AttentionCard(
    item: com.example.momentra.data.api.OperatingAttentionDto,
    model: OperatingProjectionDto,
    onOpenQuickAdd: () -> Unit,
) {
    TradeCard {
        Text(item.kind, color = TradeAccent, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
        Text(item.title, color = TradeInk, fontFamily = PlusJakartaSans, fontSize = 15.sp)
        val meta = listOfNotNull(
            item.counterpartyName,
            item.dueAt?.take(10),
            if (model.financeVisible) money(model, item.amount).takeIf { it != "—" } else null,
        ).filter { it.isNotBlank() }
        if (meta.isNotEmpty()) {
            Text(meta.joinToString(" · "), color = TradeSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        }
        if (item.cta != null || item.id.startsWith("open-") || item.sourceEventId != null) {
            Text(
                com.example.momentra.ui.shell.business.shared.operatingAttentionCtaLabel(item),
                color = TradeAccent,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier.clickable {
                    com.example.momentra.ui.shell.business.shared.openOperatingAttention(item, model, onOpenQuickAdd)
                },
            )
        }
    }
}

@Composable
private fun TradingMoments(
    model: OperatingProjectionDto,
    onOpenQuickAdd: () -> Unit,
    onViewAll: (TradingSheetState) -> Unit,
) {
    Header(model, "Moments", "Orders, stock, and the trade this month.")
    ViewAllSectionTitle("This month, in motion") { onViewAll(TradingSheetState.OrdersCommitments) }
    TradeCard {
        Text(periodLabel(model.startDate), color = TradeAccent, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        Text("This month, in motion", color = TradeInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 22.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Metric("Sales", money(model, model.finance?.sales ?: model.target?.progress), Modifier.weight(1f))
            Metric("Target", if (model.target?.configured == true) money(model, model.target?.value) else "—", Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Metric("Spent", if (model.budget?.configured == true) money(model, model.budget?.spent) else "—", Modifier.weight(1f))
            Metric("Remaining", if (model.budget?.configured == true) money(model, model.budget?.remaining) else "—", Modifier.weight(1f))
        }
        if (model.target?.configured == true) {
            Meter("Sales toward target", fraction(recordedSales(model), model.target?.value), TradeBrand)
        }
    }
    (model.boards?.moments ?: emptyList()).forEach { section ->
        BoardBlock(section, model, onViewAll)
    }
    TradeCard {
        Text("Create the next trading moment", color = TradeInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Text(
            "Open Quick Add",
            color = TradeBg,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(TradeBrand)
                .clickable(onClick = onOpenQuickAdd)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun TradingLife(model: OperatingProjectionDto, onViewAll: (TradingSheetState) -> Unit) {
    Header(model, "Life", "How the trade is moving today.")
    (model.boards?.life ?: emptyList()).forEach { BoardBlock(it, model, onViewAll) }
    if (model.boards?.life.isNullOrEmpty()) {
        BusinessEmptyState(
            title = "Nothing recorded yet",
            body = "Board updates and the live position will show here.",
            accent = TradeAccent,
            glyph = "📋",
            compact = true,
        )
    }
}

@Composable
private fun TradingMemory(model: OperatingProjectionDto, onViewAll: (TradingSheetState) -> Unit) {
    Header(model, "Memory", "What this trade keeps from the month.")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Pill(periodLabel(model.startDate))
        Pill("Current")
    }
    (model.boards?.memory ?: emptyList()).forEach { BoardBlock(it, model, onViewAll) }
    TradingStoryCta(model)
}

@Composable
private fun Header(model: OperatingProjectionDto, title: String, body: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = TradeInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 32.sp)
        Pill(roleLabel(model.role))
    }
    Text(body, color = TradeSecondary, fontFamily = PlusJakartaSans, fontSize = 14.sp)
    Text(snapshot(model.startDate, model.endDate), color = TradeSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
}

@Composable
private fun BoardBlock(
    section: OperatingBoardSectionDto,
    model: OperatingProjectionDto,
    onViewAll: (TradingSheetState) -> Unit,
) {
    val sheet = tradingSheetForCode(section.code)
    if (sheet != null) {
        ViewAllSectionTitle(section.title) { onViewAll(sheet) }
    } else {
        SectionTitle(section.title)
    }
    TradeCard {
        // Quantified tiles replace the parsed prose sentence. `Not recorded` is a
        // real answer here, never a zero.
        if (section.metrics.isNotEmpty()) {
            OperatingMetricTiles(section.metrics, TradeSheetTheme)
        } else {
            Text(
                com.example.momentra.ui.shell.business.shared.formatOperatingDetail(
                    section.detail.ifBlank { "Nothing recorded yet." },
                    model.currency,
                ),
                color = TradeSecondary,
                fontFamily = PlusJakartaSans,
                fontSize = 12.sp,
            )
        }
        OperatingWhyFootnote(section.why, TradeSheetTheme)
        if (!section.coverage.isNullOrBlank()) {
            Text(section.coverage, color = TradeAccent, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
        }
        section.rows.take(5).forEach { row ->
            Column(Modifier.padding(vertical = 6.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        row.title,
                        color = TradeInk,
                        fontFamily = PlusJakartaSans,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f),
                    )
                    OperatingBadgeChip(row.badge, TradeSheetTheme)
                }
                val extra = row.subtitle?.takeIf { it.isNotBlank() }
                    ?: listOfNotNull(row.channelName, row.counterpartyName, row.paymentStatus?.takeIf { it != "NONE" })
                        .joinToString(" · ")
                if (extra.isNotBlank()) Text(extra, color = TradeSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
                if (model.financeVisible && !row.amount.isNullOrBlank()) {
                    Text(com.example.momentra.ui.shell.business.shared.formatOperatingAmount(model.currency, row.amount, true), color = TradeAccent, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }
        if (section.rows.size > 5 && sheet != null) {
            Text(
                "View all ${section.rows.size}",
                color = TradeAccent,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier.clickable { onViewAll(sheet) },
            )
        }
    }
}

/** A section heading with the Figma `View all ›` affordance on the right. */
@Composable
private fun ViewAllSectionTitle(text: String, onViewAll: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text,
            color = TradeInk,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            modifier = Modifier.weight(1f),
        )
        Text(
            "View all ›",
            color = TradeAccent,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .clickable(onClick = onViewAll)
                .padding(horizontal = 8.dp, vertical = 6.dp),
        )
    }
}

/** The Memory-tab CTA that opens the eight-page business story (TRD-MEMORY-v12). */
@Composable
private fun TradingStoryCta(model: OperatingProjectionDto) {
    TradeCard {
        Text("Business story", color = TradeInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Text(
            "Eight pages from the month — moment, motion, orders, money, stock, network, attention, and so far.",
            color = TradeSecondary,
            fontFamily = PlusJakartaSans,
            fontSize = 12.sp,
        )
        Text(
            "Generate business story",
            color = TradeBg,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(TradeBrand)
                .clickable { com.example.momentra.ui.shell.business.shared.OperatingChromeState.storyConfigOpen = true }
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun ReadinessGroup(label: String, rows: List<com.example.momentra.data.api.OperatingPaymentDto>, model: OperatingProjectionDto) {
    if (rows.isEmpty()) return
    TradeCard {
        Text(label, color = TradeAccent, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        rows.forEach { item ->
            Column(Modifier.clickable { com.example.momentra.ui.shell.business.shared.OperatingChromeState.settling = item }) {
                Line(item.title, money(model, item.amount), TradeInk)
                val bucket = when (item.dueBucket) {
                    "OVERDUE" -> "Overdue"
                    "DUE_SOON" -> "Due soon"
                    "OPEN" -> if (item.dueAt.isNullOrBlank()) "Open · no due date" else "Open"
                    else -> null
                }
                val meta = listOfNotNull(item.counterpartyName, item.dueAt?.take(10), bucket).filter { it.isNotBlank() }.joinToString(" · ")
                if (meta.isNotBlank()) Text(meta, color = TradeSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun TradeCard(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(TradeCard)
            .border(1.dp, TradeBorder, RoundedCornerShape(18.dp))
            .shadow(10.dp, RoundedCornerShape(18.dp), ambientColor = TradeBrand.copy(alpha = 0.4f), spotColor = TradeBrand.copy(alpha = 0.4f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = { content() },
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = TradeInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
}

@Composable
private fun Pill(text: String) {
    Text(
        text,
        color = TradeAccent,
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(TradeNotice)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(TradeNotice)
            .padding(12.dp),
    ) {
        Text(label, color = TradeSecondary, fontFamily = PlusJakartaSans, fontSize = 11.sp)
        Text(value, color = TradeInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

@Composable
private fun Meter(label: String, fraction: Float, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = TradeSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
            Text("${(fraction * 100).toInt()}%", color = color, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        }
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp)).background(TradeBorder)) {
            Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(6.dp).background(color))
        }
    }
}

@Composable
private fun GstPositionCard(model: OperatingProjectionDto) {
    val position = model.gstPosition ?: return
    var open by remember { mutableStateOf(false) }
    SectionTitle("GST Position")
    TradeCard {
        Text("Estimated GST from recorded sales and purchases", color = TradeSecondary, fontFamily = PlusJakartaSans, fontSize = 13.sp)
        Text(position.asOfMonth.orEmpty(), color = TradeSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        if (position.sources.isEmpty()) {
            Text("No GST transactions recorded for this period.", color = TradeInk, fontFamily = PlusJakartaSans, fontSize = 14.sp)
        } else {
            Line("GST on sales", money(model, position.outputGst), TradeAccent)
            Line("GST on purchases", money(model, position.inputGst), TradeInk)
            val negative = position.difference.orEmpty().trim().startsWith("-")
            Text(
                if (negative) "Estimated excess purchase GST: ${money(model, position.difference?.removePrefix("-"))}"
                else "Estimated difference: ${money(model, position.difference)} payable before adjustments",
                color = TradeInk,
                fontFamily = PlusJakartaSans,
                fontSize = 14.sp,
            )
            Text(
                "View GST details",
                color = TradeAccent,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { open = !open },
            )
            if (open) {
                Line("CGST on sales", money(model, position.cgstSales), TradeSecondary)
                Line("SGST on sales", money(model, position.sgstSales), TradeSecondary)
                Line("IGST on sales", money(model, position.igstSales), TradeSecondary)
                Line("CGST on purchases", money(model, position.cgstPurchases), TradeSecondary)
                Line("SGST on purchases", money(model, position.sgstPurchases), TradeSecondary)
                Line("IGST on purchases", money(model, position.igstPurchases), TradeSecondary)
                Text("Recorded purchase GST is not eligible input tax credit.", color = TradeSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
                position.sources.forEach { source ->
                    Text(
                        "${source.title.orEmpty()} · ${source.eventType.orEmpty()} · ${money(model, source.gstTotal)}",
                        color = TradeSecondary,
                        fontFamily = PlusJakartaSans,
                        fontSize = 12.sp,
                    )
                }
            }
        }
        Text("Indicative GST position — not a GST return or verified amount payable", color = TradeSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
    }
}

@Composable
private fun Line(label: String, value: String, color: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TradeInk, fontFamily = PlusJakartaSans, fontSize = 14.sp)
        Text(value, color = color, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

private fun roleLabel(code: String?): String =
    OperatingMomentCatalog.roles.firstOrNull { it.first == code }?.second ?: "Trading"

private fun recordedSales(model: OperatingProjectionDto): String? {
    val metric = model.target?.metric?.trim().orEmpty()
    return if (metric.isEmpty() || metric.equals("REVENUE", ignoreCase = true)) {
        model.finance?.sales ?: model.target?.progress
    } else {
        model.target?.progress
    }
}

private fun money(model: OperatingProjectionDto, raw: String?): String =
    com.example.momentra.ui.shell.business.shared.formatOperatingAmount(model.currency, raw, model.financeVisible)

private fun fraction(part: String?, whole: String?): Float {
    val p = part?.toFloatOrNull() ?: return 0f
    val w = whole?.toFloatOrNull() ?: return 0f
    if (w <= 0f) return 0f
    return p / w
}

private fun periodLabel(start: String?): String {
    val date = start?.let { runCatching { java.time.LocalDate.parse(it.take(10)) }.getOrNull() } ?: java.time.LocalDate.now()
    return date.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale.ENGLISH))
}

private fun snapshot(start: String?, end: String? = null): String {
    val today = java.time.LocalDate.now().format(dayFormat)
    val from = start?.take(10)?.ifBlank { null } ?: today
    val until = end?.take(10)?.ifBlank { null }
    return if (until != null) "Snapshot · $from – $until" else "Snapshot · from $from · $today"
}
