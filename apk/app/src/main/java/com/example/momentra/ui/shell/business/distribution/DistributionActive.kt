package com.example.momentra.ui.shell.business.distribution

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

// Domain palette "distribution" in design/tokens/business.tokens.json (DST-PULSE-v12 2365:29452).
// The old values were the amber BusinessRunway palette; Figma binds orange here.
internal val DistBg = Color(0xFF0F172A) // domain.distribution.canvas
internal val DistCard = Color(0xFF0B1433) // domain.distribution.surface
internal val DistNotice = Color(0xFF0B1226) // domain.distribution.canvasTop
internal val DistField = Color(0xFF071022) // domain.distribution.inset
internal val DistBorder = Color(0xFF1E293B) // color.border.default
internal val DistBrand = Color(0xFFEA580C) // domain.distribution.accent
internal val DistAccent = Color(0xFFEA580C) // domain.distribution.accent
internal val DistSecondary = Color(0xFF94A3B8) // color.text.muted
internal val DistInk = Color(0xFFFFFFFF) // color.text.primary
internal val DistSuccess = Color(0xFFFFB598) // domain.distribution.accentSoft

private val dayFormat = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)

@Composable
fun DistributionActive(
    model: OperatingProjectionDto,
    tab: OperatingTab,
    onOpenQuickAdd: () -> Unit,
    onRecord: suspend (DistributionEventDraft) -> Unit,
    onDecide: suspend (String, String) -> Unit,
    companyId: String? = null,
    modifier: Modifier = Modifier,
) {
    // One sheet slot for all eighteen view-all surfaces; `null` means no sheet is open.
    var sheet by remember { mutableStateOf<DistributionSheetState?>(null) }
    val openSheet: (DistributionSheetState) -> Unit = { sheet = it }
    Box(modifier.fillMaxSize().background(DistBg)) {
    if (tab == OperatingTab.QUICK_ADD) {
        DistributionQuickAddHub(model, onRecord, onDecide)
    } else {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DistBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 17.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        when (tab) {
            OperatingTab.PULSE -> DistributionPulse(model, onViewAll = openSheet, onOpenQuickAdd = onOpenQuickAdd)
            OperatingTab.MOMENTS -> DistributionMoments(model, onOpenQuickAdd, openSheet)
            OperatingTab.LIFE -> DistributionLife(model, openSheet)
            OperatingTab.MEMORY -> DistributionMemory(model, openSheet)
            OperatingTab.QUICK_ADD -> Unit
        }
    }
    }
    sheet?.let { state ->
        DistributionBoardSheet(state, model, DistSheetTheme, onDismiss = { sheet = null })
    }
    com.example.momentra.ui.shell.business.shared.OperatingChromeSheets(model, companyId)
    }
}

/** Figma `DST-SHEET` tokens: orange accent on the distribution canvas. */
internal val DistSheetTheme = com.example.momentra.ui.shell.business.shared.OperatingSheetTheme(
    accent = DistBrand,
    accentSoft = DistSuccess,
    canvas = DistBg,
    surface = DistCard,
    inset = DistField,
    border = DistBorder,
    ink = DistInk,
    body = DistSecondary,
    warning = com.example.momentra.ui.shell.business.shared.OperatingSheetWarning,
    negative = com.example.momentra.ui.shell.business.shared.OperatingSheetNegative,
)

/** Board section code → the view-all surface it opens, or `null` when there is none. */
internal fun distributionSheetForCode(code: String): DistributionSheetState? = when (code) {
    "orders" -> DistributionSheetState.OrdersCommitments
    "stock" -> DistributionSheetState.StockProducts
    "network" -> DistributionSheetState.PeopleNetwork
    "M4" -> DistributionSheetState.OpenCommitments
    "L1" -> DistributionSheetState.LifeCurrentBusiness
    "L2" -> DistributionSheetState.LifeDealersChannels
    "L3" -> DistributionSheetState.LifeMoneyMoving
    "L4" -> DistributionSheetState.LifePeopleActivity
    "ME1" -> DistributionSheetState.MonthlyMemory
    "ME2" -> DistributionSheetState.MoneyMemory
    "fulfilment" -> DistributionSheetState.FulfilmentMemory
    "ME4" -> DistributionSheetState.PeoplePatterns
    else -> null
}

@Composable
private fun DistributionPulse(
    model: OperatingProjectionDto,
    onViewAll: (DistributionSheetState) -> Unit,
    onOpenQuickAdd: () -> Unit = {},
) {
    Header(model, "Pulse", "Your sales plan, distribution commitments and ready money.")
    if (model.financeVisible) {
        ViewAllSectionTitle("Target & Budget") { onViewAll(DistributionSheetState.TargetBudget) }
        DistCard {
            val target = model.target
            val budget = model.budget
            Text("Recorded sales", color = DistSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
            Text(money(model, recordedSales(model)), color = DistInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 28.sp)
            if (target?.configured == true) {
                Text("of ${money(model, target.value)} sales target", color = DistSecondary, fontFamily = PlusJakartaSans, fontSize = 13.sp)
                Meter("Sales progress", fraction(recordedSales(model), target.value), DistBrand)
            } else {
                Text("No sales target set for this period.", color = DistSecondary, fontFamily = PlusJakartaSans, fontSize = 13.sp)
            }
            if (budget?.configured == true) {
                Meter("Budget used", fraction(budget.spent, budget.amount), DistSuccess)
                Line("Budget remaining", money(model, budget.remaining), DistSuccess)
            } else {
                Text("No period budget set.", color = DistSecondary, fontFamily = PlusJakartaSans, fontSize = 13.sp)
            }
            Text("Validated sales count toward the target. Eligible purchase and expense facts count once toward budget.", color = DistSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        }
        ViewAllSectionTitle("Needs Attention") { onViewAll(DistributionSheetState.NeedsAttention) }
        if (model.attention.isEmpty()) {
            BusinessEmptyState(
            title = "Nothing is waiting",
            body = "Approvals, overdue items, and blockers will show here.",
            accent = DistAccent,
            glyph = "✅",
            compact = true,
        )
        }
        model.attention.forEach { item ->
            AttentionCard(item, model, onOpenQuickAdd)
        }
        ViewAllSectionTitle("Payment Readiness") { onViewAll(DistributionSheetState.PaymentReadiness) }
        val toCollect = model.paymentReadiness.filter { it.relevance == "RECEIVABLE" }
        val toPay = model.paymentReadiness.filter { it.relevance != "RECEIVABLE" }
        ReadinessGroup("To collect", toCollect, model)
        ReadinessGroup("To pay", toPay, model)
        if (model.paymentReadiness.isEmpty()) {
            DistCard { Text("No open payments or collections.", color = DistSecondary, fontFamily = PlusJakartaSans, fontSize = 14.sp) }
        }
        ViewAllSectionTitle("Finance") { onViewAll(DistributionSheetState.Finance) }
        DistCard {
            val finance = model.finance
            Line("Recorded sales", money(model, finance?.sales), DistAccent)
            Line("Received", money(model, finance?.received), DistSuccess)
            Line("Still to collect", money(model, finance?.outstanding), DistAccent)
            Line("Eligible cost", money(model, finance?.eligibleCost), DistInk)
            Line("Business cash paid", money(model, finance?.cashPaid), DistInk)
            Line("Still to pay", money(model, finance?.payable), DistAccent)
        }
        val position = model.gstPosition
        if (position != null) {
            var open by remember { mutableStateOf(false) }
            SectionTitle("GST Position")
            DistCard {
                Text("Estimated GST from recorded sales and purchases", color = DistSecondary, fontFamily = PlusJakartaSans, fontSize = 13.sp)
                Text(position.asOfMonth.orEmpty(), color = DistSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
                if (position.sources.isEmpty()) {
                    Text("No GST transactions recorded for this period.", color = DistInk, fontFamily = PlusJakartaSans, fontSize = 14.sp)
                } else {
                    Line("GST on sales", money(model, position.outputGst), DistAccent)
                    Line("GST on purchases", money(model, position.inputGst), DistInk)
                    val negative = position.difference.orEmpty().trim().startsWith("-")
                    Text(
                        if (negative) "Estimated excess purchase GST: ${money(model, position.difference?.removePrefix("-"))}"
                        else "Estimated difference: ${money(model, position.difference)} payable before adjustments",
                        color = DistInk, fontFamily = PlusJakartaSans, fontSize = 14.sp,
                    )
                    Text("View GST details", color = DistAccent, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { open = !open })
                    if (open) {
                        Line("CGST on sales", money(model, position.cgstSales), DistSecondary)
                        Line("SGST on sales", money(model, position.sgstSales), DistSecondary)
                        Line("IGST on sales", money(model, position.igstSales), DistSecondary)
                        Line("CGST on purchases", money(model, position.cgstPurchases), DistSecondary)
                        Line("SGST on purchases", money(model, position.sgstPurchases), DistSecondary)
                        Line("IGST on purchases", money(model, position.igstPurchases), DistSecondary)
                        Text("Recorded purchase GST is not eligible input tax credit.", color = DistSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
                        position.sources.forEach { source ->
                            Text("${source.title.orEmpty()} · ${money(model, source.gstTotal)}", color = DistSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
                        }
                    }
                }
                Text("Indicative GST position — not a GST return or verified amount payable", color = DistSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
            }
        }
    } else {
        StaffPulse(model, onOpenQuickAdd)
    }
    ViewAllSectionTitle("Activity") { onViewAll(DistributionSheetState.Activity) }
    val recent = model.activity.take(5)
    if (recent.isEmpty()) {
        BusinessEmptyState(
            title = "Nothing recorded yet",
            body = "Dispatches, dealer sales, and money collection will show here.",
            accent = DistAccent,
            glyph = "📌",
            actionLabel = "Use quick add",
            onAction = onOpenQuickAdd,
        )
    } else {
        BusinessActivityList(
            rows = recent,
            model = model,
            accent = DistAccent,
            onEdit = { com.example.momentra.ui.shell.business.shared.OperatingChromeState.editing = it },
        )
    }
    Text(
        "View all activity",
        color = DistAccent,
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, DistBrand, RoundedCornerShape(14.dp))
            .clickable { onViewAll(DistributionSheetState.Activity) }
            .padding(vertical = 14.dp),
    )
}

@Composable
private fun StaffPulse(model: OperatingProjectionDto, onOpenQuickAdd: () -> Unit = {}) {
    Pill(roleLabel(model.role))
    DistCard {
        Text("Your view", color = DistInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        Text("Assigned work stays visible. Protected amounts stay hidden.", color = DistSecondary, fontFamily = PlusJakartaSans, fontSize = 13.sp)
    }
    model.attention.forEach { item ->
        AttentionCard(item, model, onOpenQuickAdd)
    }
    DistCard {
        Text("Finance totals stay with Finance, Approver, and Owner.", color = DistInk, fontFamily = PlusJakartaSans, fontSize = 15.sp, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun AttentionCard(
    item: com.example.momentra.data.api.OperatingAttentionDto,
    model: OperatingProjectionDto,
    onOpenQuickAdd: () -> Unit,
) {
    DistCard {
        Text(item.kind, color = DistAccent, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
        Text(item.title, color = DistInk, fontFamily = PlusJakartaSans, fontSize = 15.sp)
        val meta = listOfNotNull(
            item.counterpartyName,
            item.dueAt?.take(10),
            if (model.financeVisible) money(model, item.amount).takeIf { it != "—" } else null,
        ).filter { it.isNotBlank() }
        if (meta.isNotEmpty()) {
            Text(meta.joinToString(" · "), color = DistSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        }
        if (item.cta != null || item.id.startsWith("open-") || item.sourceEventId != null) {
            Text(
                com.example.momentra.ui.shell.business.shared.operatingAttentionCtaLabel(item),
                color = DistAccent,
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
private fun DistributionMoments(
    model: OperatingProjectionDto,
    onOpenQuickAdd: () -> Unit,
    onViewAll: (DistributionSheetState) -> Unit,
) {
    Header(model, "Moments", "Dispatches, stock, and the distribution this month.")
    ViewAllSectionTitle("This month, in motion") { onViewAll(DistributionSheetState.OrdersCommitments) }
    DistCard {
        Text(periodLabel(model.startDate), color = DistAccent, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        Text("This month, in motion", color = DistInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 22.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Metric("Sales", money(model, model.finance?.sales ?: model.target?.progress), Modifier.weight(1f))
            Metric("Target", if (model.target?.configured == true) money(model, model.target?.value) else "—", Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Metric("Spent", if (model.budget?.configured == true) money(model, model.budget?.spent) else "—", Modifier.weight(1f))
            Metric("Remaining", if (model.budget?.configured == true) money(model, model.budget?.remaining) else "—", Modifier.weight(1f))
        }
        if (model.target?.configured == true) {
            Meter("Sales toward target", fraction(recordedSales(model), model.target?.value), DistBrand)
        }
    }
    (model.boards?.moments ?: emptyList()).forEach { section ->
        BoardBlock(section, model, onViewAll)
    }
    DistCard {
        Text("Create the next distribution moment", color = DistInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Text(
            "Open Quick Add",
            color = DistBg,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(DistBrand)
                .clickable(onClick = onOpenQuickAdd)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun DistributionLife(model: OperatingProjectionDto, onViewAll: (DistributionSheetState) -> Unit) {
    Header(model, "Life", "How the distribution is moving today.")
    (model.boards?.life ?: emptyList()).forEach { BoardBlock(it, model, onViewAll) }
    if (model.boards?.life.isNullOrEmpty()) {
        BusinessEmptyState(
            title = "Nothing recorded yet",
            body = "Board updates and the live position will show here.",
            accent = DistAccent,
            glyph = "📋",
            compact = true,
        )
    }
}

@Composable
private fun DistributionMemory(model: OperatingProjectionDto, onViewAll: (DistributionSheetState) -> Unit) {
    Header(model, "Memory", "What this distribution keeps from the month.")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Pill(periodLabel(model.startDate))
        Pill("Current")
    }
    (model.boards?.memory ?: emptyList()).forEach { BoardBlock(it, model, onViewAll) }
    DistributionStoryCta(model)
}

@Composable
private fun Header(model: OperatingProjectionDto, title: String, body: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = DistInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 32.sp)
        Pill(roleLabel(model.role))
    }
    Text(body, color = DistSecondary, fontFamily = PlusJakartaSans, fontSize = 14.sp)
    Text(snapshot(model.startDate, model.endDate), color = DistSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
}

@Composable
private fun BoardBlock(
    section: OperatingBoardSectionDto,
    model: OperatingProjectionDto,
    onViewAll: (DistributionSheetState) -> Unit,
) {
    val sheet = distributionSheetForCode(section.code)
    if (sheet != null) {
        ViewAllSectionTitle(section.title) { onViewAll(sheet) }
    } else {
        SectionTitle(section.title)
    }
    DistCard {
        // Quantified tiles replace the parsed prose sentence. `Not recorded` is a
        // real answer here, never a zero.
        if (section.metrics.isNotEmpty()) {
            OperatingMetricTiles(section.metrics, DistSheetTheme)
        } else {
            Text(
                com.example.momentra.ui.shell.business.shared.formatOperatingDetail(
                    section.detail.ifBlank { "Nothing recorded yet." },
                    model.currency,
                ),
                color = DistSecondary,
                fontFamily = PlusJakartaSans,
                fontSize = 12.sp,
            )
        }
        OperatingWhyFootnote(section.why, DistSheetTheme)
        if (!section.coverage.isNullOrBlank()) {
            Text(section.coverage, color = DistAccent, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
        }
        section.rows.take(5).forEach { row ->
            Column(Modifier.padding(vertical = 6.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        row.title,
                        color = DistInk,
                        fontFamily = PlusJakartaSans,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f),
                    )
                    OperatingBadgeChip(row.badge, DistSheetTheme)
                }
                val extra = row.subtitle?.takeIf { it.isNotBlank() }
                    ?: listOfNotNull(row.channelName, row.counterpartyName, row.paymentStatus?.takeIf { it != "NONE" })
                        .joinToString(" · ")
                if (extra.isNotBlank()) Text(extra, color = DistSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
                if (model.financeVisible && !row.amount.isNullOrBlank()) {
                    Text(com.example.momentra.ui.shell.business.shared.formatOperatingAmount(model.currency, row.amount, true), color = DistAccent, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }
        if (section.rows.size > 5 && sheet != null) {
            Text(
                "View all ${section.rows.size}",
                color = DistAccent,
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
            color = DistInk,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            modifier = Modifier.weight(1f),
        )
        Text(
            "View all ›",
            color = DistAccent,
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

/** The Memory-tab CTA that opens the eight-page business story (DST-MEMORY-v12). */
@Composable
private fun DistributionStoryCta(model: OperatingProjectionDto) {
    DistCard {
        Text("Business story", color = DistInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Text(
            "Eight pages from the month — moment, motion, orders, money, stock, network, attention, and so far.",
            color = DistSecondary,
            fontFamily = PlusJakartaSans,
            fontSize = 12.sp,
        )
        Text(
            "Generate business story",
            color = DistBg,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(DistBrand)
                .clickable { com.example.momentra.ui.shell.business.shared.OperatingChromeState.storyConfigOpen = true }
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}


@Composable
private fun ReadinessGroup(label: String, rows: List<com.example.momentra.data.api.OperatingPaymentDto>, model: OperatingProjectionDto) {
    if (rows.isEmpty()) return
    DistCard {
        Text(label, color = DistAccent, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        rows.forEach { item ->
            Column(Modifier.clickable { com.example.momentra.ui.shell.business.shared.OperatingChromeState.settling = item }) {
                Line(item.title, money(model, item.amount), DistInk)
                val bucket = when (item.dueBucket) {
                    "OVERDUE" -> "Overdue"
                    "DUE_SOON" -> "Due soon"
                    "OPEN" -> if (item.dueAt.isNullOrBlank()) "Open · no due date" else "Open"
                    else -> null
                }
                val meta = listOfNotNull(item.counterpartyName, item.dueAt?.take(10), bucket).filter { it.isNotBlank() }.joinToString(" · ")
                if (meta.isNotBlank()) Text(meta, color = DistSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun DistCard(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(DistCard)
            .border(1.dp, DistBorder, RoundedCornerShape(18.dp))
            .shadow(10.dp, RoundedCornerShape(18.dp), ambientColor = DistBrand.copy(alpha = 0.4f), spotColor = DistBrand.copy(alpha = 0.4f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = { content() },
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = DistInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
}

@Composable
private fun Pill(text: String) {
    Text(
        text,
        color = DistAccent,
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(DistNotice)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DistNotice)
            .padding(12.dp),
    ) {
        Text(label, color = DistSecondary, fontFamily = PlusJakartaSans, fontSize = 11.sp)
        Text(value, color = DistInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

@Composable
private fun Meter(label: String, fraction: Float, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = DistSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
            Text("${(fraction * 100).toInt()}%", color = color, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        }
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp)).background(DistBorder)) {
            Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(6.dp).background(color))
        }
    }
}

@Composable
private fun Line(label: String, value: String, color: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = DistInk, fontFamily = PlusJakartaSans, fontSize = 14.sp)
        Text(value, color = color, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

private fun roleLabel(code: String?): String =
    OperatingMomentCatalog.roles.firstOrNull { it.first == code }?.second ?: "Distribution"

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
