package com.example.momentra.ui.shell.business.service

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.momentra.data.api.OperatingActivityDto
import com.example.momentra.data.api.OperatingAttentionDto
import com.example.momentra.data.api.OperatingPaymentDto
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.shell.business.shared.BusinessActivityList
import com.example.momentra.ui.shell.business.shared.OperatingChromeState
import com.example.momentra.ui.shell.business.shared.formatOperatingAmount
import com.example.momentra.ui.shell.business.shared.openOperatingAttention
import com.example.momentra.ui.shell.business.shared.operatingAttentionCtaLabel
import com.example.momentra.ui.theme.ServicesTeal

// View-all sheets
// ---------------------------------------------------------------------------------

/** Which board a `View all ›` opens. */
sealed interface ServiceSheetState {
    data object Attention : ServiceSheetState
    data object Target : ServiceSheetState
    data object Payment : ServiceSheetState
    data object Finance : ServiceSheetState
    data object Activity : ServiceSheetState
    data object Gst : ServiceSheetState
    data object Commitments : ServiceSheetState
    data object MomentDetail : ServiceSheetState
    data object People : ServiceSheetState
    data object Network : ServiceSheetState
    data object LifeCurrent : ServiceSheetState
    data object LifeSales : ServiceSheetState
    data object LifeMoney : ServiceSheetState
    data object LifeOps : ServiceSheetState
    data object Monthly : ServiceSheetState
    data object MoneyMemory : ServiceSheetState
    data object PeopleMemory : ServiceSheetState
    data object Patterns : ServiceSheetState
}

@Composable
fun ServiceBoardSheet(
    state: ServiceSheetState,
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onOpenQuickAdd: () -> Unit,
) {
    val title = when (state) {
        ServiceSheetState.Attention -> "Needs Attention"
        ServiceSheetState.Target -> "Target & Budget"
        ServiceSheetState.Payment -> "Payment Readiness"
        ServiceSheetState.Finance -> "Finance"
        ServiceSheetState.Activity -> "Activity"
        ServiceSheetState.Gst -> "GST Position"
        ServiceSheetState.Commitments -> "Open Commitments"
        ServiceSheetState.MomentDetail -> "My Service Business"
        ServiceSheetState.People -> "People"
        ServiceSheetState.Network -> "Clients & Services"
        ServiceSheetState.LifeCurrent -> "Current Business"
        ServiceSheetState.LifeSales -> "Bookings & Services"
        ServiceSheetState.LifeMoney -> "Money Moving"
        ServiceSheetState.LifeOps -> "Rooms & Staff"
        ServiceSheetState.Monthly -> "Monthly Memory"
        ServiceSheetState.MoneyMemory -> "Money Memory"
        ServiceSheetState.PeopleMemory -> "People & Activity"
        ServiceSheetState.Patterns -> "Patterns & Decisions"
    }
    com.example.momentra.ui.shell.business.shared.OperatingSheetScrim(
        onDismiss = onDismiss,
        accent = ServicesTeal.teal,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ServicesTeal.gutter, vertical = 12.dp),
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
                Text(title, style = ServicesTeal.screenTitle(), modifier = Modifier.weight(1f))
                Text(
                    "Close",
                    style = ServicesTeal.viewAll().copy(color = ServicesTeal.textBody),
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
            Text(ServiceFormat.periodLine(model.startDate, model.currency), style = ServicesTeal.roleLine(), maxLines = 1)
            when (state) {
                ServiceSheetState.Attention -> SheetAttention(model, onOpenQuickAdd)
                ServiceSheetState.Payment -> SheetPayment(model, receivables = false)
                ServiceSheetState.Finance -> SheetPayment(model, receivables = true)
                ServiceSheetState.Activity, ServiceSheetState.LifeSales -> SheetActivity(model)
                ServiceSheetState.Commitments -> SheetSection(model, "M4")
                ServiceSheetState.MomentDetail -> SheetSection(model, "M1")
                ServiceSheetState.People -> SheetPeople(model)
                ServiceSheetState.Network -> SheetSection(model, "M3")
                ServiceSheetState.LifeCurrent -> SheetSection(model, "L1")
                ServiceSheetState.LifeMoney -> SheetSection(model, "L3")
                ServiceSheetState.LifeOps -> SheetSection(model, "L4")
                ServiceSheetState.Monthly -> SheetSection(model, "ME1")
                ServiceSheetState.MoneyMemory -> SheetSection(model, "ME2")
                ServiceSheetState.PeopleMemory -> SheetSection(model, "ME3")
                ServiceSheetState.Patterns -> SheetSection(model, "ME4")
                ServiceSheetState.Target -> SheetTarget(model)
                ServiceSheetState.Gst -> SheetGst(model)
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SheetAttention(model: OperatingProjectionDto, onOpenQuickAdd: () -> Unit) {
    if (model.attention.isEmpty()) {
        TealEmpty("Nothing is waiting", "Approvals, overdue items and blockers will show here.")
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
        TealEmpty(
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
                    ServiceFormat.shortDay(row.dueAt)?.let { "Due $it" },
                    row.status.lowercase().replaceFirstChar { c -> c.uppercase() },
                ).joinToString(" · "),
                accent = when (row.dueBucket) {
                    "OVERDUE" -> ServicesTeal.negative
                    "DUE_SOON" -> ServicesTeal.warning
                    else -> ServicesTeal.teal
                },
                trailingValue = ServiceFormat.compact(model.currency, row.amount, model.financeVisible),
                trailingColor = if (row.dueBucket == "OVERDUE") ServicesTeal.negative else ServicesTeal.textPrimary,
                onClick = { OperatingChromeState.settling = row },
            )
        }
    }
}

@Composable
private fun SheetActivity(model: OperatingProjectionDto) {
    if (model.activity.isEmpty()) {
        TealEmpty("Nothing recorded yet", "Sales, spend and settlement will show here.")
        return
    }
    Column {
        model.activity.forEach { row -> ActivityDividedRow(row, model) }
    }
}

@Composable
private fun SheetPeople(model: OperatingProjectionDto) {
    if (model.participants.isEmpty()) {
        TealEmpty("No people yet", "Invite the shop floor, billing and approvers.")
        return
    }
    Column {
        model.participants.forEach { name ->
            DividedRow(
                title = name,
                caption = if (name == model.partyName) roleLabel(model.role) else "Member",
                trailingValue = if (name == model.partyName) "Owner" else "Member",
                trailingColor = ServicesTeal.textBody,
            )
        }
    }
}

@Composable
private fun SheetSection(model: OperatingProjectionDto, code: String) {
    val section = (model.boards?.moments.orEmpty() + model.boards?.life.orEmpty() + model.boards?.memory.orEmpty())
        .firstOrNull { it.code == code }
    if (section == null) {
        TealEmpty("Not available", "This board has not been recorded yet.")
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (section.detail.isNotBlank()) {
            TealCard(tone = TealTone.Board) {
                Text(
                    ServiceFormat.detail(model.currency, section.detail),
                    style = ServicesTeal.rowCaption(),
                )
                if (!section.coverage.isNullOrBlank()) SeverityChip(section.coverage!!, ServicesTeal.warning)
            }
        }
        if (section.rows.isEmpty()) {
            TealEmpty("Nothing recorded yet", "Rows will appear as activity is recorded.")
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
                else -> "${ServiceFormat.compact(model.currency, target?.progress, model.financeVisible)} of ${ServiceFormat.compact(model.currency, target?.value, model.financeVisible)}"
            },
            trailing = if (target?.configured == true) {
                ServiceFormat.percent(target?.progress, target?.value) ?: "Unavailable"
            } else {
                "Unavailable"
            },
            trailingColor = ServicesTeal.amber,
        )
        DetailRow(
            eyebrow = "Budget",
            value = when {
                budget?.configured != true -> "No period budget"
                else -> "${ServiceFormat.compact(model.currency, budget?.spent, true)} of ${ServiceFormat.compact(model.currency, budget?.amount, true)}"
            },
            trailing = if (budget?.configured == true) {
                ServiceFormat.percent(budget?.spent, budget?.amount) ?: "Unavailable"
            } else {
                "Unavailable"
            },
            trailingColor = ServicesTeal.textPrimary,
        )
        DetailRow(
            eyebrow = "Budget remaining",
            value = "After eligible spend",
            trailing = if (budget?.configured == true) ServiceFormat.compact(model.currency, budget?.remaining, true) else "Unavailable",
            trailingColor = if (budget?.configured == true) ServicesTeal.positive else ServicesTeal.warning,
        )
    }
}

@Composable
private fun SheetGst(model: OperatingProjectionDto) {
    val gst = model.gstPosition
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DetailRow("GST on sales", "Outward taxable", ServiceFormat.compact(model.currency, gst?.outputGst, true))
        DetailRow("GST on purchases", "Inward eligible", ServiceFormat.compact(model.currency, gst?.inputGst, true), ServicesTeal.positive)
        DetailRow("Estimated difference", ServiceFormat.asOf(gst?.asOfMonth?.let { "$it-01" }), ServiceFormat.compact(model.currency, gst?.difference, true), ServicesTeal.amber)
        WhyCaption("Indicative only. This is not a tax filing or an ITC claim.")
    }
}
