package com.example.momentra.ui.shell.business.restaurant

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.OperatingBoardSectionDto
import com.example.momentra.data.api.OperatingPaymentDto
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.shell.business.shared.OperatingChromeState
import com.example.momentra.ui.shell.business.shared.OperatingSheetScrim
import com.example.momentra.ui.theme.RestaurantEmber

/**
 * Ember bottom sheet for every `View all ›` route — the 16 `RST-SHEET · View all · *`
 * frames under `2487:37611` (e.g. `2482:29487`, `2482:29639`, `2482:30978`, `2482:31573`).
 *
 * Rows come straight from the projection. A board with no rows says so instead of
 * inventing a placeholder metric, and money the role cannot see stays `—`.
 */
@Composable
internal fun RestaurantBoardSheet(
    route: RestaurantSheetRoute,
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onOpenQuickAdd: () -> Unit = {},
    onRange: ((RestaurantSheetRoute) -> Unit)? = null,
) {
    OperatingSheetScrim(onDismiss = onDismiss, accent = RestaurantEmber.ember) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = RestaurantEmber.gutter, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth().height(22.dp), horizontalArrangement = Arrangement.Center) {
                Box(
                    Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(RestaurantEmber.ember.copy(alpha = 0.55f)),
                )
            }
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    route.title,
                    style = RestaurantEmber.screenTitle().copy(fontSize = 20.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "✕",
                    style = RestaurantEmber.viewAll().copy(color = RestaurantEmber.textBody),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
            Text(
                "${model.title?.takeIf { it.isNotBlank() } ?: "Restaurant"} · " +
                    "${RestaurantFormat.month(model.startDate)} · full activity, newest first",
                style = RestaurantEmber.rowCaption(),
                maxLines = 2,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlineEmberChip("This month", onClick = onRange?.let { { it(RestaurantSheetRoute.MonthPicker) } })
                OutlineEmberChip("Last month")
                OutlineEmberChip("Custom range ›", onClick = onRange?.let { { it(RestaurantSheetRoute.DatePicker) } })
            }

            when (route) {
                RestaurantSheetRoute.Attention -> SheetAttention(model, onOpenQuickAdd)
                RestaurantSheetRoute.Payment -> SheetPayment(model, receivables = false)
                RestaurantSheetRoute.Finance -> SheetPayment(model, receivables = true)
                RestaurantSheetRoute.Activity, RestaurantSheetRoute.LifeSales -> SheetActivity(model)
                RestaurantSheetRoute.People -> SheetPeople(model)
                RestaurantSheetRoute.Target -> SheetTarget(model)
                RestaurantSheetRoute.Gst -> SheetGst(model)
                RestaurantSheetRoute.StoryConfig -> EmberEmpty(
                    "Open from Memory",
                    "Generate the story from the Memory tab.",
                )
                RestaurantSheetRoute.DatePicker,
                RestaurantSheetRoute.MonthPicker,
                RestaurantSheetRoute.TimePicker -> EmberEmpty(
                    "Picker",
                    "This range chip opens the picker sheet.",
                )
                else -> {
                    val code = route.boardCode
                    if (code != null) {
                        SheetSection(model, code)
                    } else {
                        EmberEmpty("Not available", "This board has not been recorded yet.")
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SheetAttention(model: OperatingProjectionDto, onOpenQuickAdd: () -> Unit) {
    if (model.attention.isEmpty()) {
        EmberEmpty("Nothing is waiting", "Approvals, overdue bills and blockers will show here.")
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        model.attention.forEach { item ->
            val hasSource = item.sourceEventId != null || item.id.startsWith("open-") || item.cta != null
            val label = restaurantSeverityLabel(item.kind, item.severity, hasSource)
            val color = if (hasSource) restaurantSeverityColor(item.severity) else RestaurantEmber.warning
            val caption = listOfNotNull(
                item.counterpartyName?.takeIf { it.isNotBlank() },
                RestaurantFormat.shortDay(item.dueAt)?.let { "Due $it" },
                RestaurantFormat.compact(model.currency, item.amount, model.financeVisible).takeIf { it != "—" },
            ).joinToString(" · ")
            EmberAccentRow(
                title = item.title.ifBlank { "Attention item" },
                caption = caption.ifBlank { "No detail recorded" },
                accent = color,
                severityLabel = label,
                severityColor = color,
                onClick = {
                    com.example.momentra.ui.shell.business.shared.openOperatingAttention(
                        item,
                        model,
                        onOpenQuickAdd,
                    )
                },
            )
        }
    }
}

@Composable
private fun SheetPayment(model: OperatingProjectionDto, receivables: Boolean) {
    val rows: List<OperatingPaymentDto> = model.paymentReadiness.filter {
        if (receivables) it.relevance == "RECEIVABLE" else it.relevance != "RECEIVABLE"
    }
    if (rows.isEmpty()) {
        EmberEmpty(
            if (receivables) "Nothing to collect" else "Nothing to pay",
            if (receivables) {
                "Platform and credit bills waiting on settlement will show here."
            } else {
                "Ingredient bills waiting to settle will show here."
            },
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            EmberAccentRow(
                title = row.title.ifBlank { row.counterpartyName ?: "Open balance" },
                caption = listOfNotNull(
                    row.counterpartyName?.takeIf { it.isNotBlank() },
                    RestaurantFormat.shortDay(row.dueAt)?.let { "Due $it" },
                    row.status.lowercase().replaceFirstChar { it.uppercase() },
                ).joinToString(" · "),
                accent = when (row.dueBucket) {
                    "OVERDUE" -> RestaurantEmber.negative
                    "DUE_SOON" -> RestaurantEmber.warning
                    else -> RestaurantEmber.ember
                },
                trailingValue = RestaurantFormat.compact(model.currency, row.amount, model.financeVisible),
                trailingColor = if (row.dueBucket == "OVERDUE") RestaurantEmber.negative else RestaurantEmber.textPrimary,
                onClick = { OperatingChromeState.settling = row },
            )
        }
    }
}

@Composable
private fun SheetActivity(model: OperatingProjectionDto) {
    if (model.activity.isEmpty()) {
        EmberEmpty("Nothing recorded yet", "Bills, kitchen tickets and settlement will show here.")
        return
    }
    Column {
        model.activity.forEach { row ->
            EmberDividedRow(
                title = row.title.ifBlank { restaurantEventLabel(row.eventType) },
                caption = listOfNotNull(
                    row.counterpartyName ?: row.channelName,
                    row.quantity?.let { qty -> row.unit?.let { unit -> "$qty $unit" } ?: qty },
                ).filter { it.isNotBlank() }.joinToString(" · "),
                trailingValue = if (row.eventType == "ORDER" || row.eventType == "CUSTOMER_ORDER") {
                    restaurantOrderStatusLabel(row.status)
                } else {
                    row.paymentStatus?.let { restaurantPrettyStatus(it) }
                        ?: if (row.approvalStatus == "PENDING") "Pending" else "Open"
                },
                trailingColor = if (row.paymentStatus in setOf("COLLECTED", "PAID", "SETTLED")) {
                    RestaurantEmber.positive
                } else {
                    RestaurantEmber.textBody
                },
                onClick = { OperatingChromeState.editing = row },
            )
        }
    }
}

@Composable
private fun SheetPeople(model: OperatingProjectionDto) {
    if (model.participants.isEmpty()) {
        EmberEmpty("No people yet", "Invite the kitchen, floor and billing team.")
        return
    }
    Column {
        model.participants.forEach { name ->
            val isOwner = name == model.partyName
            EmberDividedRow(
                title = name,
                caption = if (isOwner) roleLabel(model.role) else "Member",
                trailingValue = if (isOwner) "Owner" else "Member",
                trailingColor = RestaurantEmber.textBody,
            )
        }
    }
}

@Composable
private fun SheetTarget(model: OperatingProjectionDto) {
    val target = model.target
    val budget = model.budget
    val targetConfigured = target?.configured == true
    val budgetConfigured = budget?.configured == true
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        EmberBoardRow(
            eyebrow = "Sales vs target",
            value = if (targetConfigured) {
                "${RestaurantFormat.compact(model.currency, target?.progress, model.financeVisible)} of " +
                    RestaurantFormat.compact(model.currency, target?.value, model.financeVisible)
            } else {
                "No target set"
            },
            trailing = if (targetConfigured) {
                RestaurantFormat.percent(target?.progress, target?.value) ?: "Unavailable"
            } else {
                "Unavailable"
            },
            trailingColor = RestaurantEmber.amber,
        )
        EmberBoardRow(
            eyebrow = "Eligible spend",
            value = "Ingredients + running",
            trailing = RestaurantFormat.compact(model.currency, model.finance?.eligibleCost, model.financeVisible),
        )
        EmberBoardRow(
            eyebrow = "Budget",
            value = if (budgetConfigured) {
                "${RestaurantFormat.compact(model.currency, budget?.spent, true)} of " +
                    RestaurantFormat.compact(model.currency, budget?.amount, true)
            } else {
                "No period budget"
            },
            trailing = if (budgetConfigured) {
                RestaurantFormat.percent(budget?.spent, budget?.amount) ?: "Unavailable"
            } else {
                "Unavailable"
            },
        )
        EmberBoardRow(
            eyebrow = "Budget remaining",
            value = "After eligible spend",
            trailing = if (budgetConfigured) {
                RestaurantFormat.compact(model.currency, budget?.remaining, true)
            } else {
                "Unavailable"
            },
            trailingColor = if (budgetConfigured) RestaurantEmber.positive else RestaurantEmber.warning,
        )
        EmberWhyCaption("Achievement = posted bills ÷ effective target. Missing coverage shows Unavailable — never invent 0%.")
    }
}

@Composable
private fun SheetGst(model: OperatingProjectionDto) {
    val gst = model.gstPosition
    Column {
        EmberDividedRow(
            title = "GST on sales",
            caption = "Outward taxable",
            trailingValue = RestaurantFormat.compact(model.currency, gst?.outputGst),
        )
        EmberDividedRow(
            title = "GST on purchases",
            caption = "Inward eligible",
            trailingValue = RestaurantFormat.compact(model.currency, gst?.inputGst),
            trailingColor = RestaurantEmber.positive,
        )
        EmberDividedRow(
            title = "Estimated difference",
            caption = RestaurantFormat.asOf(gst?.asOfMonth?.let { "$it-01" }),
            trailingValue = RestaurantFormat.compact(model.currency, gst?.difference),
            trailingColor = RestaurantEmber.amber,
        )
        EmberWhyCaption("Indicative only. This is not a tax filing or an ITC claim.")
    }
}

@Composable
private fun SheetSection(model: OperatingProjectionDto, code: String) {
    val section: OperatingBoardSectionDto? =
        (model.boards?.moments.orEmpty() + model.boards?.life.orEmpty() + model.boards?.memory.orEmpty())
            .firstOrNull { it.code == code }
    if (section == null) {
        EmberEmpty("Not available", "This board has not been recorded yet.")
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (section.detail.isNotBlank()) {
            EmberCard(tone = EmberTone.Board) {
                Text(
                    RestaurantFormat.detail(model.currency, section.detail),
                    style = RestaurantEmber.rowCaption(),
                )
                section.coverage?.takeIf { it.isNotBlank() }?.let { coverage ->
                    EmberSeverityChip(coverage, RestaurantEmber.warning)
                }
            }
        }
        if (section.rows.isEmpty()) {
            EmberEmpty("Nothing recorded yet", "Rows will appear as activity is recorded.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                section.rows.forEach { row ->
                    EmberBoardRow(
                        eyebrow = row.title.ifBlank { restaurantEventLabel(row.eventType) },
                        value = listOfNotNull(
                            row.counterpartyName,
                            row.channelName,
                            row.quantity?.let { qty -> row.unit?.let { unit -> "$qty $unit" } ?: qty },
                        ).filter { it.isNotBlank() }.joinToString(" · "),
                        trailing = if (row.eventType == "ORDER" || row.eventType == "CUSTOMER_ORDER") {
                            restaurantOrderStatusLabel(row.status)
                        } else {
                            RestaurantFormat.compact(model.currency, row.amount, model.financeVisible)
                        },
                        trailingColor = RestaurantEmber.textPrimary,
                        onClick = { OperatingChromeState.editing = row },
                    )
                }
            }
        }
    }
}

/** Which board a `View all ›` opens (the 16 `RST-SHEET · View all · *` frames). */
internal enum class RestaurantSheetRoute(val title: String, val boardCode: String?) {
    Attention("Needs Attention", null),
    Target("Target & Budget", null),
    Payment("Payment Readiness", null),
    Finance("Finance", null),
    Activity("Activity", null),
    Gst("GST Position", null),
    StoryConfig("Create Business Story", null),
    Commitments("Open Commitments", "M4"),
    MomentDetail("My Restaurant", "M1"),
    People("People", null),
    Network("Business Network", "M3"),
    LifeCurrent("Current Service", "L1"),
    LifeSales("Sales & Orders", "L2"),
    LifeMoney("Money Moving", "L3"),
    LifeOps("Operations / Stock", "L4"),
    Monthly("Monthly Memory", "ME1"),
    MoneyMemory("Money Memory", "ME2"),
    PeopleMemory("People & Activity", "ME3"),
    Patterns("Patterns & Decisions", "ME4"),
    DatePicker("Select date", null),
    MonthPicker("Select month", null),
    TimePicker("Due date & time", null),
    ;

    /** Picker frame this route opens, when it is one of the three `2521:*` frames. */
    val pickerMode: EmberPickerMode?
        get() = when (this) {
            DatePicker -> EmberPickerMode.Date
            MonthPicker -> EmberPickerMode.Month
            TimePicker -> EmberPickerMode.DateTime
            else -> null
        }
}
