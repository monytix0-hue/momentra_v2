package com.example.momentra.ui.shell.business.production

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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.OperatingActivityDto
import com.example.momentra.data.api.OperatingBoardSectionDto
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.shell.business.shared.OperatingMomentCatalog
import com.example.momentra.ui.shell.business.shared.OperatingTab
import com.example.momentra.ui.theme.PlusJakartaSans
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val LocalJob = staticCompositionLocalOf { false }

private val standardBg = Color(0xFF09121F)
private val standardCard = Color(0xFF122235)
private val standardNotice = Color(0xFF16405C)
private val standardBorder = Color(0xFF2B4B63)
private val standardAccent = Color(0xFF38BDF8)
private val standardSecondary = Color(0xFFA5BBCE)
private val standardInk = Color(0xFFE5E0EE)

private val jobBg = Color(0xFF071014)
private val jobCard = Color(0xFF15262C)
private val jobBorder = Color(0xFF345058)
private val jobAccent = Color(0xFF7DD3FC)
private val jobSecondary = Color(0xFFAFC1C3)
private val jobInk = Color(0xFFEFF5F2)

internal val PlantBg @Composable get() = if (LocalJob.current) jobBg else standardBg
internal val PlantCard @Composable get() = if (LocalJob.current) jobCard else standardCard
internal val PlantNotice @Composable get() = if (LocalJob.current) jobCard else standardNotice
internal val PlantField @Composable get() = if (LocalJob.current) jobBg else standardBg
internal val PlantBorder @Composable get() = if (LocalJob.current) jobBorder else standardBorder
internal val PlantBrand @Composable get() = if (LocalJob.current) jobAccent else standardAccent
internal val PlantAccent @Composable get() = if (LocalJob.current) jobAccent else standardAccent
internal val PlantSecondary @Composable get() = if (LocalJob.current) jobSecondary else standardSecondary
internal val PlantInk @Composable get() = if (LocalJob.current) jobInk else standardInk
internal val PlantSuccess @Composable get() = if (LocalJob.current) jobAccent else Color(0xFF8FD6FF)

private val dayFormat = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)

@Composable
fun ProductionActive(
    model: OperatingProjectionDto,
    tab: OperatingTab,
    onOpenQuickAdd: () -> Unit,
    onRecord: suspend (ProductionEventDraft) -> Unit,
    onDecide: suspend (String, String) -> Unit,
    modifier: Modifier = Modifier,
    momentId: String? = null,
) {
    var activityOpen by remember { mutableStateOf(false) }
    CompositionLocalProvider(LocalJob provides (model.subtype == "MADE_TO_ORDER")) {
    Box(modifier.fillMaxSize().background(PlantBg)) {
    if (tab == OperatingTab.QUICK_ADD) {
        ProductionQuickAddHub(model, onRecord, onDecide)
    } else {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PlantBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 17.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        when (tab) {
            OperatingTab.PULSE -> ProductionPulse(model, onViewAll = { activityOpen = true }, onDecide = onDecide)
            OperatingTab.MOMENTS -> ProductionMoments(model, onOpenQuickAdd)
            OperatingTab.LIFE -> ProductionLife(model)
            OperatingTab.MEMORY -> ProductionMemory(model, momentId)
            OperatingTab.QUICK_ADD -> Unit
        }
    }
    }
    if (activityOpen) {
        ActivitySheet(model, onClose = { activityOpen = false })
    }
    com.example.momentra.ui.shell.business.shared.OperatingChromeSheets(model)
    }
    }
}

private fun pulseSubtitle(model: OperatingProjectionDto): String =
    if (model.subtype == "MADE_TO_ORDER") "Revenue qualifying delivered/billed facts, approved spend and settlement readiness."
    else "Sales, spend, collections and approved manufacturing sources."

@Composable
private fun ProductionPulse(model: OperatingProjectionDto, onViewAll: () -> Unit, onDecide: suspend (String, String) -> Unit) {
    val scope = rememberCoroutineScope()
    Header(model, "Pulse", pulseSubtitle(model))
    if (model.financeVisible) {
        SectionTitle("Target & Budget")
        PlantCard {
            val target = model.target
            val budget = model.budget
            Text("Toward the monthly target", color = PlantSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
            Text(money(model, target?.progress), color = PlantInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 28.sp)
            Text("of ${money(model, target?.value)} target", color = PlantSecondary, fontFamily = PlusJakartaSans, fontSize = 13.sp)
            if (targetOpen(target?.coverage)) {
                Meter("Target progress", fraction(target?.progress, target?.value), PlantBrand)
            } else {
                Text(target?.coverage ?: "not recorded", color = PlantSecondary, fontFamily = PlusJakartaSans, fontSize = 13.sp)
            }
            Meter("Budget used", fraction(budget?.spent, budget?.amount), PlantSuccess)
            Line("Budget remaining", money(model, budget?.remaining), PlantSuccess)
            Text("Validated sales count toward a revenue target. Collections do not. Production, orders, and deliveries count only for the measure you chose.", color = PlantSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        }
        SectionTitle("Needs Attention")
        if (model.attention.isEmpty()) {
            PlantCard { Text("Nothing is waiting.", color = PlantSecondary, fontFamily = PlusJakartaSans, fontSize = 14.sp) }
        }
        model.attention.forEach { item ->
            PlantCard {
                Text(item.kind, color = PlantAccent, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                Text(item.title, color = PlantInk, fontFamily = PlusJakartaSans, fontSize = 15.sp)

                if (item.id.startsWith("open-")) {
                    val payment = model.paymentReadiness.firstOrNull { it.id == item.id.removePrefix("open-") }
                    if (payment != null) {
                        Text(
                            "Open",
                            color = Color.White,
                            fontFamily = PlusJakartaSans,
                            fontSize = 13.sp,
                            modifier = Modifier.clickable { com.example.momentra.ui.shell.business.shared.OperatingChromeState.settling = payment },
                        )
                    }
                }
                val canDecide = item.kind == "APPROVAL" && (model.role == "OWNER_ADMIN" || model.role == "APPROVER")
                if (canDecide) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Approve", color = PlantSuccess, fontFamily = PlusJakartaSans, modifier = Modifier.clickable {
                            scope.launch { onDecide(item.id, "APPROVED") }
                        })
                        Text("Reject", color = Color(0xFFF87171), fontFamily = PlusJakartaSans, modifier = Modifier.clickable {
                            scope.launch { onDecide(item.id, "REJECTED") }
                        })
                    }
                }
            }
        }
        SectionTitle("Payment Readiness")
        val toCollect = model.paymentReadiness.filter { it.relevance == "RECEIVABLE" }
        val toPay = model.paymentReadiness.filter { it.relevance != "RECEIVABLE" }
        ReadinessGroup("To collect", toCollect, model)
        ReadinessGroup("To pay", toPay, model)
        if (model.paymentReadiness.isEmpty()) {
            PlantCard { Text("No open payments or collections.", color = PlantSecondary, fontFamily = PlusJakartaSans, fontSize = 14.sp) }
        }
        SectionTitle("Finance")
        PlantCard {
            val finance = model.finance
            Line("Net sales", money(model, finance?.sales), PlantAccent)
            Line("Received", money(model, finance?.received), PlantSuccess)
            Line("Still to collect", money(model, finance?.outstanding), Color(0xFFFBBF24))
            Line("Eligible cost", money(model, finance?.eligibleCost), PlantInk)
            Line("Business cash paid", money(model, finance?.cashPaid), PlantInk)
            Line("Still to pay", money(model, finance?.payable), Color(0xFFFBBF24))
        }
        if (model.gstTracking == "YES" && model.gstPosition != null) {
            SectionTitle("GST Position")
            PlantCard {
                Text("Indicative only. This is not a tax filing or an ITC claim.", color = PlantSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
                Line("GST on sales", money(model, model.gstPosition?.outputGst), PlantAccent)
                Line("GST on purchases", money(model, model.gstPosition?.inputGst), PlantInk)
                Line("Estimated difference", money(model, model.gstPosition?.difference), Color(0xFFFBBF24))
            }
        }
    } else {
        RestrictedPulse(model)
    }
    SectionTitle("Activity")
    val recent = model.activity.take(5)
    if (recent.isEmpty()) {
        PlantCard { Text("Nothing recorded yet.", color = PlantSecondary, fontFamily = PlusJakartaSans, fontSize = 14.sp) }
    }
    recent.forEach { ActivityRow(it, model) }
    Text(
        "View all activity",
        color = PlantAccent,
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, PlantBrand, RoundedCornerShape(14.dp))
            .clickable(onClick = onViewAll)
            .padding(vertical = 14.dp),
    )
}

@Composable
private fun RestrictedPulse(model: OperatingProjectionDto) {
    Text("Production status, with the work you're allowed to see.", color = PlantSecondary, fontFamily = PlusJakartaSans, fontSize = 14.sp)
    PlantCard {
        Text(model.title ?: "Factory", color = PlantInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        Text(model.role?.replace('_', ' ')?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Assigned work", color = PlantSecondary, fontFamily = PlusJakartaSans, fontSize = 13.sp)
    }
    SectionTitle("Needs Attention")
    if (model.attention.isEmpty()) {
        PlantCard { Text("Nothing is waiting.", color = PlantSecondary, fontFamily = PlusJakartaSans, fontSize = 14.sp) }
    }
    model.attention.forEach { item ->
        PlantCard {
            Text(item.title, color = PlantInk, fontFamily = PlusJakartaSans, fontSize = 15.sp)
            Text(item.kind, color = PlantSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        }
    }
    PlantCard {
        Text("Finance access restricted", color = PlantInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Text("Financial records, amounts, and budget history are not available in your current scope.", color = PlantSecondary, fontFamily = PlusJakartaSans, fontSize = 13.sp)
    }
}

@Composable
private fun ChannelTotals(title: String, rows: List<OperatingActivityDto>, model: OperatingProjectionDto) {
    val grouped = rows.filter { it.approvalStatus != "REJECTED" && it.approvalStatus != "PENDING" }
        .groupBy { it.channelName?.ifBlank { null } ?: "Unassigned" }
    if (grouped.isEmpty()) return
    Text(title, color = PlantSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
    grouped.forEach { (channel, items) ->
        val total = items.sumOf { it.amount?.toDoubleOrNull() ?: 0.0 }
        Line(channel, money(model, total.toString()), PlantInk)
    }
}

@Composable
private fun ProductionMoments(model: OperatingProjectionDto, onOpenQuickAdd: () -> Unit) {
    Header(model, "Moments", "Production, dispatch, and the factory this month.")
    PlantCard {
        Text(periodLabel(model.startDate), color = PlantAccent, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        Text("This month, in motion", color = PlantInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 22.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Metric("Progress", money(model, model.target?.progress), Modifier.weight(1f))
            Metric("Target", money(model, model.target?.value), Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Metric("Spent", money(model, model.budget?.spent), Modifier.weight(1f))
            Metric("Remaining", money(model, model.budget?.remaining), Modifier.weight(1f))
        }
        if (targetOpen(model.target?.coverage)) {
            Meter("Toward the monthly target", fraction(model.target?.progress, model.target?.value), PlantBrand)
        } else {
            Text(model.target?.coverage ?: "not recorded", color = PlantSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        }
    }
    (model.boards?.moments ?: emptyList()).forEach { section ->
        BoardBlock(section, model)
    }
    PlantCard {
        Text("Capture the next manufacturing event", color = PlantInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Text(
            "Open Quick Add",
            color = PlantBg,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(PlantBrand)
                .clickable(onClick = onOpenQuickAdd)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

private fun targetOpen(coverage: String?): Boolean = coverage.isNullOrBlank()

@Composable
private fun ProductionLife(model: OperatingProjectionDto) {
    Header(model, "Life", "How production is moving today.")
    (model.boards?.life ?: emptyList()).forEach { BoardBlock(it, model) }
    if (model.boards?.life.isNullOrEmpty()) {
        PlantCard { Text("Nothing recorded yet.", color = PlantSecondary, fontFamily = PlusJakartaSans, fontSize = 14.sp) }
    }
}

@Composable
private fun ProductionMemory(model: OperatingProjectionDto, momentId: String?) {
    Header(model, "Memory", "What this factory keeps from the month.")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Pill(periodLabel(model.startDate))
        Pill("Current")
    }
    (model.boards?.memory ?: emptyList()).forEach { BoardBlock(it, model) }
    Text(
        "Preview story",
        color = PlantBg,
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PlantBrand)
            .clickable { if (momentId != null) com.example.momentra.ui.shell.business.shared.OperatingChromeState.previewStoryId = momentId }
            .padding(vertical = 14.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}

@Composable
private fun ActivitySheet(model: OperatingProjectionDto, onClose: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(onClick = onClose)
            .padding(top = 80.dp),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(PlantBg)
                .clickable(enabled = false) {}
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("All activity", color = PlantInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 22.sp)
                Text("Close", color = PlantSecondary, fontFamily = PlusJakartaSans, modifier = Modifier.clickable(onClick = onClose))
            }
            Text(model.title ?: "Production", color = PlantSecondary, fontFamily = PlusJakartaSans, fontSize = 13.sp)
            if (model.activity.isEmpty()) {
                Text("Nothing recorded yet.", color = PlantSecondary, fontFamily = PlusJakartaSans, fontSize = 14.sp)
            }
            model.activity.forEach { ActivityRow(it, model) }
        }
    }
}

@Composable
private fun Header(model: OperatingProjectionDto, title: String, body: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = PlantInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 32.sp)
        Pill(roleLabel(model.role))
    }
    Text(body, color = PlantSecondary, fontFamily = PlusJakartaSans, fontSize = 14.sp)
    Text(snapshot(model.startDate, model.endDate), color = PlantSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
}

@Composable
private fun BoardBlock(section: OperatingBoardSectionDto, model: OperatingProjectionDto) {
    SectionTitle(section.title)
    PlantCard {
        Text(com.example.momentra.ui.shell.business.shared.formatOperatingDetail(section.detail.ifBlank { "Nothing recorded yet." }, model.currency), color = PlantSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        section.rows.forEach { row ->
            Column(Modifier.padding(vertical = 6.dp)) {
                Text(row.title, color = PlantInk, fontFamily = PlusJakartaSans, fontSize = 14.sp)
                val extra = listOfNotNull(row.channelName, row.counterpartyName, row.paymentStatus?.takeIf { it != "NONE" }).joinToString(" · ")
                if (extra.isNotBlank()) Text(extra, color = PlantSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
                if (model.financeVisible && !row.amount.isNullOrBlank()) {
                    Text(com.example.momentra.ui.shell.business.shared.formatOperatingAmount(model.currency, row.amount, true), color = PlantAccent, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun ActivityRow(row: OperatingActivityDto, model: OperatingProjectionDto) {
    PlantCard {
        Text(row.eventType.replace('_', ' '), color = PlantAccent, fontFamily = PlusJakartaSans, fontSize = 11.sp)
        Text(row.title, color = PlantInk, fontFamily = PlusJakartaSans, fontSize = 14.sp)
        val qty = listOfNotNull(row.quantity, row.unit).filter { it.isNotBlank() }.joinToString(" ")
        val bits = listOfNotNull(
            row.counterpartyName,
            qty.ifBlank { null },
            row.paymentStatus?.takeIf { it != "NONE" },
            row.occurredAt.take(10).ifBlank { null },
            if (model.financeVisible) com.example.momentra.ui.shell.business.shared.formatOperatingAmount(model.currency, row.amount, true).takeIf { it != "—" } else null,
        )
        Text(
            "Edit",
            color = Color.White,
            fontFamily = PlusJakartaSans,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            fontSize = 13.sp,
            modifier = Modifier.clickable { com.example.momentra.ui.shell.business.shared.OperatingChromeState.editing = row },
        )
        if (bits.isNotEmpty()) Text(bits.joinToString(" · "), color = PlantSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
    }
}

@Composable
private fun ReadinessGroup(label: String, rows: List<com.example.momentra.data.api.OperatingPaymentDto>, model: OperatingProjectionDto) {
    if (rows.isEmpty()) return
    PlantCard {
        Text(label, color = PlantAccent, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        rows.forEach { item ->
            Column(Modifier.clickable { com.example.momentra.ui.shell.business.shared.OperatingChromeState.settling = item }) {
                Line(item.title, money(model, item.amount), PlantInk)
                val meta = listOfNotNull(item.counterpartyName, item.dueAt?.take(10)).filter { it.isNotBlank() }.joinToString(" · ")
                if (meta.isNotBlank()) Text(meta, color = PlantSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun PlantCard(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(PlantCard)
            .border(1.dp, PlantBorder, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = { content() },
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = PlantInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
}

@Composable
private fun Pill(text: String) {
    Text(
        text,
        color = PlantAccent,
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(PlantNotice)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(PlantNotice)
            .padding(12.dp),
    ) {
        Text(label, color = PlantSecondary, fontFamily = PlusJakartaSans, fontSize = 11.sp)
        Text(value, color = PlantInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

@Composable
private fun Meter(label: String, fraction: Float, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = PlantSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
            Text("${(fraction * 100).toInt()}%", color = color, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        }
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp)).background(PlantBorder)) {
            Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(6.dp).background(color))
        }
    }
}

@Composable
private fun Line(label: String, value: String, color: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = PlantInk, fontFamily = PlusJakartaSans, fontSize = 14.sp)
        Text(value, color = color, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

private fun roleLabel(code: String?): String =
    OperatingMomentCatalog.roles.firstOrNull { it.first == code }?.second ?: "Production"

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
