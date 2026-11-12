package com.example.momentra.ui.shell.business.service

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.example.momentra.data.api.OperatingPaymentDto
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.shell.business.shared.OperatingChromeState
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

private fun ServiceSheetState.chromeCode(): String = when (this) {
    ServiceSheetState.Attention -> "ATTENTION"
    ServiceSheetState.Target -> "TARGET_BUDGET"
    ServiceSheetState.Payment -> "PAYMENT_READINESS"
    ServiceSheetState.Finance -> "FINANCE"
    ServiceSheetState.Activity -> "ACTIVITY"
    ServiceSheetState.Gst -> "GST"
    ServiceSheetState.Commitments -> "M4"
    ServiceSheetState.MomentDetail -> "M1"
    ServiceSheetState.People -> "ME3"
    ServiceSheetState.Network -> "network"
    ServiceSheetState.LifeCurrent -> "L1"
    ServiceSheetState.LifeSales -> "services"
    ServiceSheetState.LifeMoney -> "L3"
    ServiceSheetState.LifeOps -> "L4"
    ServiceSheetState.Monthly -> "ME1"
    ServiceSheetState.MoneyMemory -> "ME2"
    ServiceSheetState.PeopleMemory -> "ME3"
    ServiceSheetState.Patterns -> "ME4"
}

private fun ServiceSheetState.sheetTitle(): String = when (this) {
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

private val ServiceSheetTheme = com.example.momentra.ui.shell.business.shared.OperatingSheetTheme(
    accent = ServicesTeal.teal,
    accentSoft = ServicesTeal.tealSoft,
    canvas = ServicesTeal.screenBottom,
    surface = ServicesTeal.card,
    inset = ServicesTeal.surfaceRow,
    border = ServicesTeal.cardStroke,
    ink = ServicesTeal.textPrimary,
    body = ServicesTeal.textBody,
    warning = ServicesTeal.warning,
    negative = ServicesTeal.negative,
)

@Composable
fun ServiceBoardSheet(
    state: ServiceSheetState,
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onOpenQuickAdd: () -> Unit,
) {
    val code = state.chromeCode()
    val total = when (state) {
        ServiceSheetState.Attention -> model.attention.size
        ServiceSheetState.Payment, ServiceSheetState.Finance -> model.paymentReadiness.size
        ServiceSheetState.Activity, ServiceSheetState.LifeSales -> model.activity.size
        ServiceSheetState.People, ServiceSheetState.PeopleMemory -> model.participants.size
        else -> model.boards?.let { boards ->
            listOfNotNull(boards.moments, boards.life, boards.memory)
                .flatten()
                .firstOrNull {
                    it.code == code ||
                        (state == ServiceSheetState.Network && it.code == "M3") ||
                        (state == ServiceSheetState.MomentDetail && it.code == "M1")
                }
                ?.rows?.size
        } ?: 0
    }
    com.example.momentra.ui.shell.business.shared.OperatingSheetFrame(
        title = state.sheetTitle(),
        eyebrow = "",
        model = model,
        theme = ServiceSheetTheme,
        onDismiss = onDismiss,
        sheetCode = code,
        totalCount = total,
        visibleCount = { status ->
            when (state) {
                ServiceSheetState.Attention -> model.attention.count {
                    val chip = when (it.severity?.uppercase()) {
                        "CRITICAL" -> "Overdue"
                        "HIGH" -> "Alert"
                        else -> "Open"
                    }
                    com.example.momentra.ui.shell.business.shared.OperatingSheetChrome.matchesStatus(status, chip)
                }
                ServiceSheetState.Payment, ServiceSheetState.Finance -> model.paymentReadiness.count {
                    com.example.momentra.ui.shell.business.shared.OperatingSheetChrome.matchesStatus(status, it.dueBucket)
                }
                ServiceSheetState.Activity, ServiceSheetState.LifeSales -> model.activity.count {
                    com.example.momentra.ui.shell.business.shared.OperatingSheetChrome.matchesStatus(status, it.badge)
                }
                ServiceSheetState.People, ServiceSheetState.PeopleMemory -> model.participants.count {
                    com.example.momentra.ui.shell.business.shared.OperatingSheetChrome.matchesStatus(status, null)
                }
                else -> model.boards?.let { boards ->
                    listOfNotNull(boards.moments, boards.life, boards.memory)
                        .flatten()
                        .firstOrNull {
                            it.code == code ||
                                (state == ServiceSheetState.Network && it.code == "M3") ||
                                (state == ServiceSheetState.MomentDetail && it.code == "M1")
                        }
                        ?.rows
                        ?.count {
                            com.example.momentra.ui.shell.business.shared.OperatingSheetChrome.matchesStatus(status, it.badge)
                        }
                } ?: total
            }
        },
        onFooterAction = { action ->
            when (action) {
                "PAYMENT" -> OperatingChromeState.quickAddHint = "payment"
                "MEMORY" -> OperatingChromeState.storyConfigOpen = true
                "ATTENTION" -> OperatingChromeState.quickAddHint = "issue"
                else -> onOpenQuickAdd()
            }
        },
    ) { status ->
        when (state) {
            ServiceSheetState.Attention -> SheetAttention(model, status, onOpenQuickAdd)
            ServiceSheetState.Payment -> SheetPayment(model, status, receivables = false)
            ServiceSheetState.Finance -> SheetPayment(model, status, receivables = true)
            ServiceSheetState.Activity, ServiceSheetState.LifeSales -> SheetActivity(model, status)
            ServiceSheetState.Commitments -> SheetSection(model, "M4", status)
            ServiceSheetState.MomentDetail -> SheetSection(model, "M1", status)
            ServiceSheetState.People -> SheetPeople(model, status)
            ServiceSheetState.Network -> SheetSection(model, "M3", status)
            ServiceSheetState.LifeCurrent -> SheetSection(model, "L1", status)
            ServiceSheetState.LifeMoney -> SheetSection(model, "L3", status)
            ServiceSheetState.LifeOps -> SheetSection(model, "L4", status)
            ServiceSheetState.Monthly -> SheetSection(model, "ME1", status)
            ServiceSheetState.MoneyMemory -> SheetSection(model, "ME2", status)
            ServiceSheetState.PeopleMemory -> SheetSection(model, "ME3", status)
            ServiceSheetState.Patterns -> SheetSection(model, "ME4", status)
            ServiceSheetState.Target -> SheetTarget(model)
            ServiceSheetState.Gst -> SheetGst(model)
        }
    }
}

@Composable
private fun SheetAttention(model: OperatingProjectionDto, status: String, onOpenQuickAdd: () -> Unit) {
    val rows = model.attention.filter {
        val chip = when (it.severity?.uppercase()) {
            "CRITICAL" -> "Overdue"
            "HIGH" -> "Alert"
            else -> "Open"
        }
        com.example.momentra.ui.shell.business.shared.OperatingSheetChrome.matchesStatus(status, chip)
    }
    if (rows.isEmpty()) {
        TealEmpty("Nothing is waiting", "Approvals, overdue items and blockers will show here.")
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { item -> AttentionRow(item, model, onOpenQuickAdd) }
    }
}

@Composable
private fun SheetPayment(model: OperatingProjectionDto, status: String, receivables: Boolean) {
    val rows: List<OperatingPaymentDto> = model.paymentReadiness.filter {
        val side = if (receivables) it.relevance == "RECEIVABLE" else it.relevance != "RECEIVABLE"
        side && com.example.momentra.ui.shell.business.shared.OperatingSheetChrome.matchesStatus(status, it.dueBucket)
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
private fun SheetActivity(model: OperatingProjectionDto, status: String) {
    val rows = model.activity.filter {
        com.example.momentra.ui.shell.business.shared.OperatingSheetChrome.matchesStatus(status, it.badge)
    }
    if (rows.isEmpty()) {
        TealEmpty("Nothing recorded yet", "Sales, spend and settlement will show here.")
        return
    }
    Column {
        rows.forEach { row -> ActivityDividedRow(row, model) }
    }
}

@Composable
private fun SheetPeople(model: OperatingProjectionDto, status: String) {
    val names = model.participants.filter {
        com.example.momentra.ui.shell.business.shared.OperatingSheetChrome.matchesStatus(status, null)
    }
    if (names.isEmpty()) {
        TealEmpty("No people yet", "Invite the shop floor, billing and approvers.")
        return
    }
    Column {
        names.forEach { name ->
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
private fun SheetSection(model: OperatingProjectionDto, code: String, status: String) {
    val section = (model.boards?.moments.orEmpty() + model.boards?.life.orEmpty() + model.boards?.memory.orEmpty())
        .firstOrNull { it.code == code }
    if (section == null) {
        TealEmpty("Not available", "This board has not been recorded yet.")
        return
    }
    val rows = section.rows.filter {
        com.example.momentra.ui.shell.business.shared.OperatingSheetChrome.matchesStatus(status, it.badge)
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
        if (rows.isEmpty()) {
            TealEmpty("Nothing recorded yet", "Rows will appear as activity is recorded.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.forEach { row -> BoardRowFor(row = row, model = model) }
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
