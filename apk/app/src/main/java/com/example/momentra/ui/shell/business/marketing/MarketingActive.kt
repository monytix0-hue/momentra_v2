package com.example.momentra.ui.shell.business.marketing

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

internal val CampBg = Color(0xFF09121F)
internal val CampCard = Color(0xFF122235)
internal val CampNotice = Color(0xFF16405C)
internal val CampField = Color(0xFF09121F)
internal val CampBorder = Color(0xFF2B4B63)
internal val CampBrand = Color(0xFF6DDCFF)
internal val CampAccent = Color(0xFF6DDCFF)
internal val CampSecondary = Color(0xFFA5BBCE)
internal val CampInk = Color(0xFFE5E0EE)
internal val CampSuccess = Color(0xFF8FD6FF)

private val dayFormat = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)

@Composable
fun MarketingActive(
    model: OperatingProjectionDto,
    tab: OperatingTab,
    onOpenQuickAdd: () -> Unit,
    onRecord: suspend (MarketingEventDraft) -> Unit,
    onDecide: suspend (String, String) -> Unit,
    momentId: String? = null,
    modifier: Modifier = Modifier,
) {
    var activityOpen by remember { mutableStateOf(false) }
    Box(modifier.fillMaxSize().background(CampBg)) {
    if (tab == OperatingTab.QUICK_ADD) {
        MarketingQuickAddHub(model, onRecord, onDecide)
    } else {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CampBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 17.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        when (tab) {
            OperatingTab.PULSE -> MarketingPulse(model, onViewAll = { activityOpen = true }, onDecide = onDecide, onOpenQuickAdd = onOpenQuickAdd)
            OperatingTab.MOMENTS -> MarketingMoments(model, onOpenQuickAdd)
            OperatingTab.LIFE -> MarketingLife(model)
            OperatingTab.MEMORY -> MarketingMemory(model, momentId)
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

private fun pulseSubtitle(model: OperatingProjectionDto): String =
    if (model.subtype == "OWN_BUSINESS_CAMPAIGN") "Your campaign plan, approved obligations and ready money."
    else "Your campaign revenue plan, approved spend and ready money."

@Composable
private fun MarketingPulse(
    model: OperatingProjectionDto,
    onViewAll: () -> Unit,
    onDecide: suspend (String, String) -> Unit,
    onOpenQuickAdd: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    Header(model, "Pulse", pulseSubtitle(model))
    if (model.financeVisible) {
        SectionTitle("Target & Budget")
        CampCard {
            val target = model.target
            val budget = model.budget
            val measured = model.activity.any { it.eventType == "RESULT" || it.eventType == "PROGRESS" }
            Text("Accepted results", color = CampSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
            Text(
                if (target?.configured == false || !measured) "N/A" else money(model, target?.progress),
                color = CampInk,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 28.sp,
            )
            Text(
                if (target?.configured == false) "Target not set" else "of ${money(model, target?.value)} target",
                color = CampSecondary,
                fontFamily = PlusJakartaSans,
                fontSize = 13.sp,
            )
            model.marketing?.pacing?.let { Text(it, color = CampSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp) }
            Text(
                "CPL ${model.marketing?.cpl ?: "N/A"}",
                color = CampSecondary,
                fontFamily = PlusJakartaSans,
                fontSize = 12.sp,
            )
            if (measured) Meter("Results toward target", fraction(target?.progress, target?.value), CampBrand)
            ChannelTotals("Results by channel", model.activity.filter { it.eventType == "RESULT" || it.eventType == "PROGRESS" }, model)
            Meter("Budget used", fraction(budget?.spent, budget?.amount), CampSuccess)
            ChannelTotals("Spend by channel", model.activity.filter { it.eventType == "SPEND" || it.eventType == "VENDOR_COST" }, model)
            Line("Budget remaining", money(model, budget?.remaining), CampSuccess)
            Text("Accepted results count toward the target. Money collected does not. Spend and vendor costs count once toward the budget.", color = CampSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        }
        SectionTitle("Needs Attention")
        if (model.attention.isEmpty()) {
            CampCard { Text("Nothing is waiting.", color = CampSecondary, fontFamily = PlusJakartaSans, fontSize = 14.sp) }
        }
        model.attention.forEach { item ->
            CampCard {
                Text(item.kind, color = CampAccent, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                Text(item.title, color = CampInk, fontFamily = PlusJakartaSans, fontSize = 15.sp)

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
                val canDecide = (item.kind == "APPROVAL" || item.kind == "ATT-03" || item.cta == "approval") && (model.role == "OWNER_ADMIN" || model.role == "APPROVER")
                if (canDecide) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Approve", color = CampSuccess, fontFamily = PlusJakartaSans, modifier = Modifier.clickable {
                            scope.launch { onDecide(item.id, "APPROVED") }
                        })
                        Text("Reject", color = Color(0xFFF87171), fontFamily = PlusJakartaSans, modifier = Modifier.clickable {
                            scope.launch { onDecide(item.id, "REJECTED") }
                        })
                    }
                } else if (!item.cta.isNullOrBlank() && !item.id.startsWith("open-")) {
                    Text(
                        com.example.momentra.ui.shell.business.shared.operatingAttentionCtaLabel(item),
                        color = CampInk,
                        fontFamily = PlusJakartaSans,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable {
                            com.example.momentra.ui.shell.business.shared.openOperatingAttention(item, model, onOpenQuickAdd)
                        },
                    )
                }
            }
        }
        SectionTitle("Payment Readiness")
        val toCollect = if (model.subtype == "CLIENT_CAMPAIGN") model.paymentReadiness.filter { it.relevance == "RECEIVABLE" } else emptyList()
        val toPay = model.paymentReadiness.filter { it.relevance != "RECEIVABLE" }
        if (model.subtype == "CLIENT_CAMPAIGN") ReadinessGroup("To collect", toCollect, model)
        ReadinessGroup("To pay", toPay, model)
        if (model.paymentReadiness.isEmpty()) {
            CampCard { Text("No open payments or collections.", color = CampSecondary, fontFamily = PlusJakartaSans, fontSize = 14.sp) }
        }
        SectionTitle("Finance")
        CampCard {
            val finance = model.finance
            val measured = model.activity.any { it.eventType == "RESULT" || it.eventType == "PROGRESS" }
            Line("Accepted results", if (measured) money(model, model.target?.progress) else "N/A", CampAccent)
            if (model.subtype == "CLIENT_CAMPAIGN") {
                Line("Client invoices", money(model, finance?.sales), CampAccent)
                Line("Received", money(model, finance?.received), CampSuccess)
                Line("Still to collect", money(model, finance?.outstanding), Color(0xFFFBBF24))
            }
            Line("Eligible cost", money(model, finance?.eligibleCost), CampInk)
            Line("Business cash paid", money(model, finance?.cashPaid), CampInk)
            Line("Still to pay", money(model, finance?.payable), Color(0xFFFBBF24))
        }
    } else {
        RestrictedPulse(model)
    }
    SectionTitle("Activity")
    val recent = model.activity.take(5)
    if (recent.isEmpty()) {
        CampCard { Text("Nothing recorded yet.", color = CampSecondary, fontFamily = PlusJakartaSans, fontSize = 14.sp) }
    }
    recent.forEach { ActivityRow(it, model) }
    Text(
        "View all activity",
        color = CampAccent,
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, CampBrand, RoundedCornerShape(14.dp))
            .clickable(onClick = onViewAll)
            .padding(vertical = 14.dp),
    )
}

@Composable
private fun RestrictedPulse(model: OperatingProjectionDto) {
    Text("The campaign today, with the work you're allowed to see.", color = CampSecondary, fontFamily = PlusJakartaSans, fontSize = 14.sp)
    CampCard {
        Text(model.title ?: "Campaign", color = CampInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        val channels = model.life?.channels?.joinToString(" · ").orEmpty()
        if (channels.isNotBlank()) Text("Channels · $channels", color = CampSecondary, fontFamily = PlusJakartaSans, fontSize = 13.sp)
        if (!model.partyName.isNullOrBlank()) Text(model.partyName, color = CampSecondary, fontFamily = PlusJakartaSans, fontSize = 13.sp)
        Text("Financial totals stay hidden in this view.", color = CampSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
    }
    SectionTitle("Needs Attention")
    if (model.attention.isEmpty()) {
        CampCard { Text("Nothing is waiting.", color = CampSecondary, fontFamily = PlusJakartaSans, fontSize = 14.sp) }
    }
    model.attention.forEach { item ->
        CampCard {
            Text(item.title, color = CampInk, fontFamily = PlusJakartaSans, fontSize = 15.sp)
            Text(item.kind, color = CampSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        }
    }
    CampCard {
        Text("Finance access restricted", color = CampInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Text("Financial records, amounts, and budget history are not available in your current scope.", color = CampSecondary, fontFamily = PlusJakartaSans, fontSize = 13.sp)
    }
}

@Composable
private fun ChannelTotals(title: String, rows: List<OperatingActivityDto>, model: OperatingProjectionDto) {
    val grouped = rows.filter { it.approvalStatus != "REJECTED" && it.approvalStatus != "PENDING" }
        .groupBy { it.channelName?.ifBlank { null } ?: "Unassigned" }
    if (grouped.isEmpty()) return
    Text(title, color = CampSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
    grouped.forEach { (channel, items) ->
        val total = items.sumOf { it.amount?.toDoubleOrNull() ?: 0.0 }
        Line(channel, money(model, total.toString()), CampInk)
    }
}

@Composable
private fun MarketingMoments(model: OperatingProjectionDto, onOpenQuickAdd: () -> Unit) {
    Header(model, "Moments", "Deliverables, results, and the campaign this month.")
    CampCard {
        Text(periodLabel(model.startDate), color = CampAccent, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        Text("This month, in motion", color = CampInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 22.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Metric("Results", money(model, model.target?.progress), Modifier.weight(1f))
            Metric("Target", money(model, model.target?.value), Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Metric("Spent", money(model, model.budget?.spent), Modifier.weight(1f))
            Metric("Remaining", money(model, model.budget?.remaining), Modifier.weight(1f))
        }
        Meter("Results toward target", fraction(model.target?.progress, model.target?.value), CampBrand)
    }
    (model.boards?.moments ?: emptyList()).forEach { section ->
        BoardBlock(section, model)
    }
    CampCard {
        Text("Create the next campaign moment", color = CampInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Text(
            "Open Quick Add",
            color = CampBg,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(CampBrand)
                .clickable(onClick = onOpenQuickAdd)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun MarketingLife(model: OperatingProjectionDto) {
    Header(model, "Life", "How the campaign is moving today.")
    (model.boards?.life ?: emptyList()).forEach { BoardBlock(it, model) }
    if (model.boards?.life.isNullOrEmpty()) {
        CampCard { Text("Nothing recorded yet.", color = CampSecondary, fontFamily = PlusJakartaSans, fontSize = 14.sp) }
    }
}

@Composable
private fun MarketingMemory(model: OperatingProjectionDto, momentId: String? = null) {
    Header(model, "Memory", "What this campaign keeps from the month.")
    Text(
        "Preview story",
        color = CampBg,
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CampBrand)
            .clickable { if (momentId != null) com.example.momentra.ui.shell.business.shared.OperatingChromeState.previewStoryId = momentId }
            .padding(horizontal = 16.dp, vertical = 12.dp),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Pill(periodLabel(model.startDate))
        Pill("Current")
    }
    (model.boards?.memory ?: emptyList()).forEach { BoardBlock(it, model) }
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
                .background(CampBg)
                .clickable(enabled = false) {}
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("All activity", color = CampInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 22.sp)
                Text("Close", color = CampSecondary, fontFamily = PlusJakartaSans, modifier = Modifier.clickable(onClick = onClose))
            }
            Text(model.title ?: "Marketing", color = CampSecondary, fontFamily = PlusJakartaSans, fontSize = 13.sp)
            if (model.activity.isEmpty()) {
                Text("Nothing recorded yet.", color = CampSecondary, fontFamily = PlusJakartaSans, fontSize = 14.sp)
            }
            model.activity.forEach { ActivityRow(it, model) }
        }
    }
}

@Composable
private fun Header(model: OperatingProjectionDto, title: String, body: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = CampInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 32.sp)
        Pill(roleLabel(model.role))
    }
    Text(body, color = CampSecondary, fontFamily = PlusJakartaSans, fontSize = 14.sp)
    Text(snapshot(model.startDate, model.endDate), color = CampSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
}

@Composable
private fun BoardBlock(section: OperatingBoardSectionDto, model: OperatingProjectionDto) {
    SectionTitle(section.title)
    CampCard {
        Text(com.example.momentra.ui.shell.business.shared.formatOperatingDetail(section.detail.ifBlank { "Nothing recorded yet." }, model.currency), color = CampSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        if (!section.coverage.isNullOrBlank()) {
            Text(section.coverage, color = CampAccent, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        }
        section.rows.forEach { row ->
            Column(Modifier.padding(vertical = 6.dp)) {
                Text(row.title, color = CampInk, fontFamily = PlusJakartaSans, fontSize = 14.sp)
                val extra = listOfNotNull(row.channelName, row.counterpartyName, row.paymentStatus?.takeIf { it != "NONE" }).joinToString(" · ")
                if (extra.isNotBlank()) Text(extra, color = CampSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
                if (model.financeVisible && !row.amount.isNullOrBlank()) {
                    Text(com.example.momentra.ui.shell.business.shared.formatOperatingAmount(model.currency, row.amount, true), color = CampAccent, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun ActivityRow(row: OperatingActivityDto, model: OperatingProjectionDto) {
    CampCard {
        Text(row.eventType.replace('_', ' '), color = CampAccent, fontFamily = PlusJakartaSans, fontSize = 11.sp)
        Text(row.title, color = CampInk, fontFamily = PlusJakartaSans, fontSize = 14.sp)
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
        if (bits.isNotEmpty()) Text(bits.joinToString(" · "), color = CampSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
    }
}

@Composable
private fun ReadinessGroup(label: String, rows: List<com.example.momentra.data.api.OperatingPaymentDto>, model: OperatingProjectionDto) {
    if (rows.isEmpty()) return
    CampCard {
        Text(label, color = CampAccent, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        rows.forEach { item ->
            Column(Modifier.clickable { com.example.momentra.ui.shell.business.shared.OperatingChromeState.settling = item }) {
                Line(item.title, money(model, item.amount), CampInk)
                val meta = listOfNotNull(item.counterpartyName, item.dueAt?.take(10)).filter { it.isNotBlank() }.joinToString(" · ")
                if (meta.isNotBlank()) Text(meta, color = CampSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun CampCard(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CampCard)
            .border(1.dp, CampBorder, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = { content() },
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = CampInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
}

@Composable
private fun Pill(text: String) {
    Text(
        text,
        color = CampAccent,
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(CampNotice)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CampNotice)
            .padding(12.dp),
    ) {
        Text(label, color = CampSecondary, fontFamily = PlusJakartaSans, fontSize = 11.sp)
        Text(value, color = CampInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

@Composable
private fun Meter(label: String, fraction: Float, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = CampSecondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
            Text("${(fraction * 100).toInt()}%", color = color, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        }
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp)).background(CampBorder)) {
            Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(6.dp).background(color))
        }
    }
}

@Composable
private fun Line(label: String, value: String, color: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = CampInk, fontFamily = PlusJakartaSans, fontSize = 14.sp)
        Text(value, color = color, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

private fun roleLabel(code: String?): String =
    OperatingMomentCatalog.roles.firstOrNull { it.first == code }?.second ?: "Marketing"

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
