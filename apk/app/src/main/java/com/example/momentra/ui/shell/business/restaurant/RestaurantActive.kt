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
import com.example.momentra.data.api.OperatingActivityDto
import com.example.momentra.data.api.OperatingBoardSectionDto
import com.example.momentra.data.api.OperatingMasterDto
import com.example.momentra.data.api.OperatingPaymentDto
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.shell.business.shared.OperatingChromeSheets
import com.example.momentra.ui.shell.business.shared.OperatingChromeState
import com.example.momentra.ui.shell.business.shared.OperatingMomentCatalog
import com.example.momentra.ui.shell.business.shared.OperatingSheetScrim
import com.example.momentra.ui.shell.business.shared.OperatingTab
import com.example.momentra.ui.shell.business.shared.OperatingCaptureSheet
import com.example.momentra.ui.shell.business.shared.openOperatingAttention
import com.example.momentra.ui.theme.RestaurantEmber
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Restaurant active experience — Figma `2487:37611` (`resuarant`).
 *
 * Frames: `RST-PULSE-v12` `2400:29655`, `RST-MOMENTS-v12` `2400:30664`,
 * `RST-LIFE-v12` `2400:30824`, `RST-MEMORY-v12` `2400:30984`.
 *
 * Every number is read from the operating projection. A metric the backend cannot
 * supply renders `Unavailable` — never a fabricated `0`, `0%` or `—`-as-zero.
 * Covers, tables and KOT ticket ids have no field on the strict operating contract,
 * so those frame values are replaced by the closest recorded fact instead of invented.
 */
@Composable
fun RestaurantActive(
    model: OperatingProjectionDto,
    tab: OperatingTab,
    onOpenQuickAdd: () -> Unit,
    onRecord: suspend (RestaurantEventDraft) -> Unit,
    onDecide: suspend (String, String) -> Unit,
    momentId: String? = null,
    modifier: Modifier = Modifier,
) {
    var sheet by remember { mutableStateOf<RestaurantSheetRoute?>(null) }
    var storyConfig by remember { mutableStateOf(false) }
    var picker by remember { mutableStateOf<RestaurantSheetRoute?>(null) }
    var pickerDate by remember { mutableStateOf(java.time.LocalDate.now()) }

    EmberScreenBackground(modifier) {
        if (tab == OperatingTab.QUICK_ADD) {
            RestaurantQuickAddHub(
                model = model,
                momentId = momentId,
                onRecord = onRecord,
                onDecide = onDecide,
            )
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = RestaurantEmber.gutter, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(RestaurantEmber.sectionGap),
            ) {
                EmberScreenHeader(title = tabTitle(model, tab), roleLine = roleLine(model))
                EmberContextChips(
                    subtypeLabel = model.subtypeLabel ?: "Restaurant",
                    periodLine = RestaurantFormat.periodLine(model.startDate, model.currency),
                    gstOn = model.gstTracking == "YES",
                    quickAdds = tab != OperatingTab.QUICK_ADD,
                    onQuickAdds = onOpenQuickAdd,
                )
                when (tab) {
                    OperatingTab.PULSE -> RestaurantPulse(model, onOpenQuickAdd) { sheet = it }
                    OperatingTab.MOMENTS -> RestaurantMoments(model, onOpenQuickAdd) { sheet = it }
                    OperatingTab.LIFE -> RestaurantLife(model) { sheet = it }
                    OperatingTab.MEMORY -> RestaurantMemory(
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

    sheet?.let { route ->
        if (route.pickerMode != null) {
            RestaurantPickerScrim(route.pickerMode!!, model, pickerDate, { pickerDate = it }) { sheet = null }
        } else {
            RestaurantBoardSheet(
                route = route,
                model = model,
                onDismiss = { sheet = null },
                onRange = { picker = it },
            )
        }
    }

    picker?.let { route ->
        route.pickerMode?.let { mode ->
            RestaurantPickerScrim(mode, model, pickerDate, { pickerDate = it }) { picker = null }
        }
    }

    if (storyConfig && momentId != null) {
        OperatingSheetScrim(onDismiss = { storyConfig = false }, accent = RestaurantEmber.ember) {
            RestaurantStoryConfigSheet(
                model = model,
                momentId = momentId,
                onDismiss = { storyConfig = false },
            )
        }
    }

    OperatingChromeSheets(model)
}

/** Bottom-sheet wrapper for the three `2521:*` picker frames. */
@Composable
private fun RestaurantPickerScrim(
    mode: EmberPickerMode,
    model: OperatingProjectionDto,
    date: java.time.LocalDate,
    onDate: (java.time.LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    OperatingSheetScrim(
        onDismiss = onDismiss,
        accent = RestaurantEmber.ember,
        surface = RestaurantEmber.sheet,
        border = RestaurantEmber.divider,
    ) {
        EmberPickerSheet(
            mode = mode,
            venue = model.title.orEmpty(),
            initial = date,
            onDismiss = onDismiss,
            onConfirm = { picked, _ -> onDate(picked) },
        )
    }
}

// ---------------------------------------------------------------------------------
// Header
// ---------------------------------------------------------------------------------

private fun tabTitle(model: OperatingProjectionDto, tab: OperatingTab): String {
    val name = model.title?.takeIf { it.isNotBlank() } ?: "Restaurant"
    return when (tab) {
        OperatingTab.PULSE -> "$name Pulse"
        OperatingTab.MOMENTS -> "Moments"
        OperatingTab.LIFE -> "Life"
        OperatingTab.MEMORY -> "Memory"
        OperatingTab.QUICK_ADD -> "Quick Adds"
    }
}

internal fun roleLine(model: OperatingProjectionDto): String {
    val role = roleLabel(model.role)
    val party = model.partyName?.trim().orEmpty()
    val subtype = model.subtypeLabel ?: "Restaurant"
    return listOf(role, party).filter { it.isNotBlank() }.joinToString(" ") + " · $subtype"
}

internal fun roleLabel(code: String?): String =
    OperatingMomentCatalog.roles.firstOrNull { it.first == code }?.second ?: "Restaurant"

// ---------------------------------------------------------------------------------
// Pulse — RST-PULSE-v12 `2400:29655`
// ---------------------------------------------------------------------------------

@Composable
private fun RestaurantPulse(
    model: OperatingProjectionDto,
    onOpenQuickAdd: () -> Unit,
    onOpenSheet: (RestaurantSheetRoute) -> Unit,
) {
    PulseHero(model, onOpenQuickAdd)
    AttentionCard(model, onOpenQuickAdd, onOpenSheet)
    TargetBudgetCard(model, onOpenSheet)
    PaymentReadinessCard(model, onOpenSheet)
    FinanceCard(model, onOpenSheet)
    ActivityCard(model, onOpenSheet)
    if (model.gstTracking == "YES") GstCard(model, onOpenSheet)
}

/**
 * Ember hero: eyebrow, promise line, one-line position + 40dp glyph well.
 * `Covers 86` from the frame is omitted — the strict contract carries no covers field.
 */
@Composable
private fun PulseHero(model: OperatingProjectionDto, onOpenQuickAdd: () -> Unit) {
    val sales = RestaurantFormat.compact(model.currency, model.finance?.sales ?: model.target?.progress, model.financeVisible)
    val targetValue = if (model.target?.configured == true) {
        RestaurantFormat.compact(model.currency, model.target?.value, model.financeVisible)
    } else {
        null
    }
    val receivable = RestaurantFormat.compact(model.currency, model.finance?.outstanding, model.financeVisible)
    val summary = buildString {
        append(if (targetValue != null) "Sales $sales / $targetValue" else "Sales $sales")
        append(" · Open tickets ${openOrders(model).size}")
        append(" · AR $receivable")
    }
    val eyebrow = listOfNotNull(model.title?.uppercase(), model.location?.uppercase())
        .filter { it.isNotBlank() }
        .joinToString(" · ")
        .ifBlank { "RESTAURANT" }
    val shape = RestaurantEmber.rHero

    Box(
        Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = shape,
                clip = false,
                ambientColor = RestaurantEmber.ember.copy(alpha = 0.5f),
                spotColor = RestaurantEmber.ember.copy(alpha = 0.5f),
            )
            .shadow(
                elevation = 24.dp,
                shape = shape,
                clip = false,
                ambientColor = RestaurantEmber.gold.copy(alpha = 0.3f),
                spotColor = RestaurantEmber.gold.copy(alpha = 0.3f),
            )
            .clip(shape)
            .background(Brush.linearGradient(listOf(RestaurantEmber.heroStart, RestaurantEmber.heroEnd))),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(eyebrow, style = RestaurantEmber.heroEyebrow(), maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 52.dp))
            Text("Order. Serve. Collect.", style = RestaurantEmber.heroHeadline(), maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            Text(summary, style = RestaurantEmber.heroBody(), maxLines = 2,
                modifier = Modifier.padding(end = 56.dp))
            if (model.quickAdds.any { it.code == "SALE" }) {
                Spacer(Modifier.height(4.dp))
                EmberCta("+  Record Sale", onClick = onOpenQuickAdd)
            }
        }
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(40.dp)
                .clip(RestaurantEmber.rHeroIcon)
                .background(RestaurantEmber.surfaceWell),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(RestaurantTileIcons.heroGlyph),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** AN-P01 · Needs Attention `2400:29711`. */
@Composable
private fun AttentionCard(
    model: OperatingProjectionDto,
    onOpenQuickAdd: () -> Unit,
    onOpenSheet: (RestaurantSheetRoute) -> Unit,
) {
    val items = model.attention
    EmberCard {
        EmberSectionHeader(
            title = "Needs Attention",
            chipText = if (items.isEmpty()) null else "Alert",
            onViewAll = { onOpenSheet(RestaurantSheetRoute.Attention) },
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EmberMetricChip("Alerts", items.size.toString(), RestaurantEmber.ember, Modifier.weight(1f))
            EmberMetricChip("Tickets", openOrders(model).size.toString(), modifier = Modifier.weight(1f))
        }
        if (items.isEmpty()) {
            EmberEmpty("Nothing is waiting", "Approvals, overdue bills and blockers will show here.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.take(3).forEach { item -> AttentionRow(item, model, onOpenQuickAdd) }
            }
        }
        EmberWhyCaption("Platform receivable needs explicit settlement link — never invent collection from order complete.")
    }
}

@Composable
private fun AttentionRow(
    item: com.example.momentra.data.api.OperatingAttentionDto,
    model: OperatingProjectionDto,
    onOpenQuickAdd: () -> Unit,
) {
    val hasSource = item.sourceEventId != null || item.id.startsWith("open-") || item.cta != null
    val label = restaurantSeverityLabel(item.kind, item.severity, hasSource)
    val color = if (hasSource) restaurantSeverityColor(item.severity) else RestaurantEmber.warning
    val caption = listOfNotNull(
        item.counterpartyName?.takeIf { it.isNotBlank() },
        RestaurantFormat.shortDay(item.dueAt)?.let { "Due $it" },
        RestaurantFormat.compact(model.currency, item.amount, model.financeVisible).takeIf { it != "—" },
    ).joinToString(" · ").ifBlank { "No detail recorded" }
    EmberAccentRow(
        title = item.title.ifBlank { "Attention item" },
        caption = caption,
        accent = color,
        severityLabel = label,
        severityColor = color,
        onClick = { openOperatingAttention(item, model, onOpenQuickAdd) },
    )
}

/** AN-P02 · Target & Budget `2400:29677`. */
@Composable
private fun TargetBudgetCard(model: OperatingProjectionDto, onOpenSheet: (RestaurantSheetRoute) -> Unit) {
    val target = model.target
    val budget = model.budget
    val configured = target?.configured == true
    val percentText = if (configured) RestaurantFormat.percent(target?.progress, target?.value) ?: "Unavailable" else "Unavailable"
    val fraction = if (configured) RestaurantFormat.fraction(target?.progress, target?.value) else null
    val sales = RestaurantFormat.compact(model.currency, target?.progress ?: model.finance?.sales, model.financeVisible)
    val targetValue = if (configured) RestaurantFormat.compact(model.currency, target?.value, model.financeVisible) else null
    val budgetPercent = if (budget?.configured == true) {
        RestaurantFormat.percent(budget?.spent, budget?.amount) ?: "Unavailable"
    } else {
        "Unavailable"
    }

    EmberCard {
        EmberSectionHeader("Target & Budget", onViewAll = { onOpenSheet(RestaurantSheetRoute.Target) })
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RestaurantEmber.rCardTight)
                .background(RestaurantEmber.surfaceWell)
                .border(1.dp, RestaurantEmber.ember.copy(alpha = 0.22f), RestaurantEmber.rCardTight)
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EmberHealthRing(percentLabel = percentText, caption = "of target", fraction = fraction)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "ACHIEVEMENT VS TARGET",
                    style = RestaurantEmber.why().copy(
                        color = RestaurantEmber.ember,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                    ),
                    maxLines = 1,
                )
                Text(
                    if (configured && targetValue != null) "Sales $sales of $targetValue" else "Sales $sales · no target set",
                    style = RestaurantEmber.metricValue().copy(fontSize = 15.sp),
                    maxLines = 2,
                )
                TargetStatusPill(fraction)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EmberMetricChip(
                "Achievement",
                percentText,
                if (percentText == "Unavailable") RestaurantEmber.warning else RestaurantEmber.textPrimary,
                Modifier.weight(1f),
            )
            EmberMetricChip("Sales", sales, modifier = Modifier.weight(1f))
            EmberMetricChip(
                "Budget use",
                budgetPercent,
                if (budgetPercent == "Unavailable") RestaurantEmber.warning else RestaurantEmber.textPrimary,
                Modifier.weight(1f),
            )
        }
        Column {
            EmberDividedRow(
                title = "Sales vs target",
                caption = "Effective ${RestaurantFormat.month(model.startDate)} · restaurant",
                trailingValue = if (configured && targetValue != null) "$sales / $targetValue" else "Unavailable",
                trailingColor = if (configured) RestaurantEmber.textPrimary else RestaurantEmber.warning,
            )
            EmberDividedRow(
                title = "Eligible spend",
                caption = "Ingredients + running",
                trailingValue = RestaurantFormat.compact(model.currency, model.finance?.eligibleCost, model.financeVisible),
            )
            val coverage = target?.coverage?.takeIf { it.isNotBlank() }
            EmberDividedRow(
                title = "Coverage",
                caption = coverage ?: "Coverage unavailable",
                trailingValue = if (coverage != null) "Recorded" else "Unavailable",
                trailingColor = if (coverage != null) RestaurantEmber.positive else RestaurantEmber.warning,
                captionColor = if (coverage != null) RestaurantEmber.textBody else RestaurantEmber.warning,
            )
        }
        EmberWhyCaption("Order and prep are not revenue — achievement uses posted bills / finalized sales only.")
    }
}

@Composable
private fun TargetStatusPill(fraction: Float?) {
    when {
        fraction == null -> EmberStatusPill("● Unavailable", RestaurantEmber.warning)
        fraction < 0.5f -> EmberStatusPill("● Behind", RestaurantEmber.negative)
        fraction >= 0.75f -> EmberStatusPill("● On track", RestaurantEmber.positive)
        else -> EmberStatusPill("● In progress", RestaurantEmber.amber)
    }
}

/** AN-P03 · Payment Readiness `2400:29742`. */
@Composable
private fun PaymentReadinessCard(model: OperatingProjectionDto, onOpenSheet: (RestaurantSheetRoute) -> Unit) {
    val payables = model.paymentReadiness.filter { it.relevance != "RECEIVABLE" }
    val settled = payables
        .filter { it.status == "PAID" || it.status == "SETTLED" }
        .fold(BigDecimal.ZERO) { total, row -> total + (row.amount.toBigDecimalOrNull() ?: BigDecimal.ZERO) }
    val paidText = if (settled.signum() > 0) {
        RestaurantFormat.compact(model.currency, settled.stripTrailingZeros().toPlainString(), model.financeVisible)
    } else {
        RestaurantFormat.compact(model.currency, model.finance?.cashPaid, model.financeVisible)
    }

    EmberCard {
        EmberSectionHeader("Payment Readiness", onViewAll = { onOpenSheet(RestaurantSheetRoute.Payment) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EmberMetricChip(
                "AP open",
                RestaurantFormat.compact(model.currency, model.finance?.payable, model.financeVisible),
                RestaurantEmber.amber,
                Modifier.weight(1f),
            )
            EmberMetricChip("Paid", paidText, RestaurantEmber.positive, Modifier.weight(1f))
        }
        if (payables.isEmpty()) {
            EmberEmpty("No open payments", "Ingredient bills and kitchen expenses waiting to settle will show here.")
        } else {
            Column {
                payables.take(3).forEach { row ->
                    EmberDividedRow(
                        title = row.title.ifBlank { row.counterpartyName ?: "Open payable" },
                        caption = paymentCaption(row),
                        trailingValue = RestaurantFormat.compact(model.currency, row.amount, model.financeVisible),
                        trailingColor = if (row.dueBucket == "OVERDUE") RestaurantEmber.negative else RestaurantEmber.amber,
                        captionColor = if (row.dueBucket == "OVERDUE") RestaurantEmber.negative else RestaurantEmber.textBody,
                        onClick = { OperatingChromeState.settling = row },
                    )
                }
            }
        }
        EmberWhyCaption("Purchase ≠ payment. Aggregator fees must not duplicate cost facts.")
    }
}

internal fun paymentCaption(row: OperatingPaymentDto): String {
    val parts = listOfNotNull(
        row.counterpartyName?.takeIf { it.isNotBlank() },
        RestaurantFormat.shortDay(row.dueAt)?.let { "Due $it" },
    )
    if (parts.isNotEmpty()) return parts.joinToString(" · ")
    return row.status.lowercase().split("_").joinToString(" ") { word ->
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.ENGLISH) else it.toString() }
    }
}

/** AN-P04 · Finance `2400:29772`. */
@Composable
private fun FinanceCard(model: OperatingProjectionDto, onOpenSheet: (RestaurantSheetRoute) -> Unit) {
    val receivables = model.paymentReadiness.filter { it.relevance == "RECEIVABLE" }
    EmberCard {
        EmberSectionHeader("Finance", onViewAll = { onOpenSheet(RestaurantSheetRoute.Finance) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EmberMetricChip(
                "Billed",
                RestaurantFormat.compact(model.currency, model.finance?.sales, model.financeVisible),
                modifier = Modifier.weight(1f),
            )
            EmberMetricChip(
                "Collected",
                RestaurantFormat.compact(model.currency, model.finance?.received, model.financeVisible),
                RestaurantEmber.positive,
                Modifier.weight(1f),
            )
            EmberMetricChip(
                "Platform AR",
                RestaurantFormat.compact(model.currency, model.finance?.outstanding, model.financeVisible),
                RestaurantEmber.ember,
                Modifier.weight(1f),
            )
        }
        when {
            !model.financeVisible -> EmberEmpty("Totals stay private", "Finance, Approver and Owner see the money position.")
            receivables.isEmpty() -> EmberEmpty("Nothing to collect", "Platform and credit bills waiting on settlement will show here.")
            else -> Column {
                receivables.take(3).forEach { row ->
                    val name = row.counterpartyName?.takeIf { it.isNotBlank() }
                        ?: row.title.ifBlank { "Open receivable" }
                    EmberDividedRow(
                        title = name,
                        caption = RestaurantFormat.shortDay(row.dueAt)?.let { "Due $it" } ?: "Awaiting settle",
                        trailingValue = RestaurantFormat.compact(model.currency, row.amount, true),
                        trailingColor = if (row.dueBucket == "OVERDUE") RestaurantEmber.negative else RestaurantEmber.ember,
                        captionColor = if (row.dueBucket == "OVERDUE") RestaurantEmber.negative else RestaurantEmber.textBody,
                        onClick = { OperatingChromeState.settling = row },
                    )
                }
            }
        }
        EmberWhyCaption("Posted bill creates revenue. Prep/fulfil do not. Platform AR only with explicit allocation.")
    }
}

/** AN-P05 · Activity `2400:29805`. */
@Composable
private fun ActivityCard(model: OperatingProjectionDto, onOpenSheet: (RestaurantSheetRoute) -> Unit) {
    val activity = model.activity
    val stockMoves = activity.count { it.eventType == "STOCK" }
    EmberCard {
        EmberSectionHeader("Activity", onViewAll = { onOpenSheet(RestaurantSheetRoute.Activity) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EmberMetricChip("Orders", orderRows(model).size.toString(), modifier = Modifier.weight(1f))
            EmberMetricChip("Firing", firingOrders(model).size.toString(), RestaurantEmber.amber, Modifier.weight(1f))
            EmberMetricChip("Stock moves", stockMoves.toString(), modifier = Modifier.weight(1f))
        }
        if (activity.isEmpty()) {
            EmberEmpty("Nothing recorded yet", "Bills, kitchen tickets and stock moves will show here.")
        } else {
            Column {
                activity.take(3).forEach { row -> ActivityDividedRow(row, model) }
            }
        }
        EmberWhyCaption("Cancel, amend, spoilage and split tender must not duplicate facts.")
    }
}

@Composable
private fun ActivityDividedRow(row: OperatingActivityDto, model: OperatingProjectionDto) {
    val trailing = activityTrailing(row)
    EmberDividedRow(
        title = row.title.ifBlank { restaurantEventLabel(row.eventType) },
        caption = activityCaption(row),
        trailingValue = trailing,
        trailingColor = activityTrailingColor(trailing),
        onClick = { OperatingChromeState.editing = row },
    )
}

internal fun activityCaption(row: OperatingActivityDto): String {
    val parts = mutableListOf<String>()
    val who = row.counterpartyName?.takeIf { it.isNotBlank() }
    val channel = row.channelName?.takeIf { it.isNotBlank() }
    val note = row.note?.takeIf { it.isNotBlank() }
    when {
        who != null -> parts += who
        channel != null -> parts += channel
        note != null -> parts += note.take(48)
        else -> parts += restaurantEventLabel(row.eventType)
    }
    row.quantity?.takeIf { it.isNotBlank() }?.let { qty ->
        parts += (row.unit?.takeIf { it.isNotBlank() }?.let { "$qty $it" } ?: qty)
    }
    return parts.joinToString(" · ")
}

internal fun activityTrailing(row: OperatingActivityDto): String {
    if (row.eventType == "ORDER" || row.eventType == "CUSTOMER_ORDER") {
        return restaurantOrderStatusLabel(row.status)
    }
    if (row.approvalStatus == "PENDING") return "Pending"
    if (row.paymentStatus in listOf("COLLECTED", "PAID", "SETTLED")) return "Done"
    val status = row.status?.takeIf { it.isNotBlank() }
    if (status != null) {
        return status.lowercase().replaceFirstChar { it.titlecase(java.util.Locale.ENGLISH) }
    }
    return "Open"
}

internal fun activityTrailingColor(trailing: String): Color = when (trailing) {
    "Done", "Served", "Ready", "Approved" -> RestaurantEmber.positive
    "Pending", "Firing", "Sent" -> RestaurantEmber.amber
    "Void" -> RestaurantEmber.negative
    else -> RestaurantEmber.textBody
}

/** AN-P06 · GST Position `2400:29838`. */
@Composable
private fun GstCard(model: OperatingProjectionDto, onOpenSheet: (RestaurantSheetRoute) -> Unit) {
    val gst = model.gstPosition
    val complete = !gst?.outputGst.isNullOrBlank() && !gst?.inputGst.isNullOrBlank()
    EmberCard {
        EmberSectionHeader("GST Position", onViewAll = { onOpenSheet(RestaurantSheetRoute.Gst) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EmberMetricChip("Output", RestaurantFormat.compact(model.currency, gst?.outputGst), modifier = Modifier.weight(1f))
            EmberMetricChip("ITC", RestaurantFormat.compact(model.currency, gst?.inputGst), RestaurantEmber.positive, Modifier.weight(1f))
            EmberMetricChip("Net", RestaurantFormat.compact(model.currency, gst?.difference), RestaurantEmber.amber, Modifier.weight(1f))
        }
        if (!complete) {
            EmberEmpty("Registration incomplete", "GST stays Unavailable until the registration is complete.")
        } else {
            Column {
                EmberDividedRow(
                    title = "Outward taxable",
                    caption = "Posted bills ${RestaurantFormat.month(model.startDate)}",
                    trailingValue = RestaurantFormat.compact(model.currency, model.finance?.sales, true),
                )
                EmberDividedRow(
                    title = "Inward eligible",
                    caption = "Posted purchases",
                    trailingValue = RestaurantFormat.compact(model.currency, model.finance?.eligibleCost, true),
                    trailingColor = RestaurantEmber.positive,
                )
                EmberDividedRow(
                    title = "Net payable",
                    caption = RestaurantFormat.asOf(gst?.asOfMonth?.let { "$it-01" }),
                    trailingValue = RestaurantFormat.compact(model.currency, gst?.difference),
                    trailingColor = RestaurantEmber.amber,
                )
            }
        }
        EmberWhyCaption("GST only when enabled. Incomplete registration → Unavailable. Indicative only — not a filing.")
    }
}

// ---------------------------------------------------------------------------------
// Moments — RST-MOMENTS-v12 `2400:30664`
// ---------------------------------------------------------------------------------

@Composable
private fun RestaurantMoments(
    model: OperatingProjectionDto,
    onOpenQuickAdd: () -> Unit,
    onOpenSheet: (RestaurantSheetRoute) -> Unit,
) {
    val staff = model.participants.size
    val suppliers = networkSuppliers(model)
    EmberBoardHero(
        eyebrow = "ACTIVE MOMENT",
        headline = model.title?.takeIf { it.isNotBlank() } ?: "Restaurant",
        chips = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EmberBoardHeroChip(model.subtypeLabel ?: "Restaurant", RestaurantEmber.ember)
                val location = model.location?.takeIf { it.isNotBlank() }
                if (location != null) EmberBoardHeroChip(location, RestaurantEmber.positive, dot = true)
            }
        },
        kpis = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EmberKpiChip("People · Staff", staff.toString(), modifier = Modifier.weight(1f))
                EmberKpiChip("Network · Suppliers", suppliers.toString(), modifier = Modifier.weight(1f))
                EmberKpiChip(
                    "AP open",
                    RestaurantFormat.compact(model.currency, model.finance?.payable, model.financeVisible),
                    RestaurantEmber.amber,
                    Modifier.weight(1f),
                )
            }
        },
    )

    CommitmentsCard(model, onOpenSheet)
    MyRestaurantCard(model, onOpenSheet)
    PeopleCard(model, onOpenSheet)
    NetworkCard(model, onOpenSheet)

    EmberCard(onClick = onOpenQuickAdd) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EmberIconWell(RestaurantTileIcons.sale)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Record the next restaurant moment", style = RestaurantEmber.sectionTitle(), maxLines = 2)
                Text("Bill, kitchen ticket, purchase or stock count.", style = RestaurantEmber.rowCaption(), maxLines = 2)
            }
            Text("›", color = RestaurantEmber.ember, fontWeight = FontWeight.Bold)
        }
    }
}

/** AN-M04 · Open Commitments `2400:30767`. */
@Composable
private fun CommitmentsCard(model: OperatingProjectionDto, onOpenSheet: (RestaurantSheetRoute) -> Unit) {
    val rows = model.boards?.moments?.firstOrNull { it.code == "M4" }?.rows.orEmpty()
    EmberCard(tone = EmberTone.Board, strokeAlpha = 0.40f) {
        EmberBoardHeader("Open Commitments", onViewAll = { onOpenSheet(RestaurantSheetRoute.Commitments) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EmberKpiChip(
                "AP",
                RestaurantFormat.compact(model.currency, model.finance?.payable, model.financeVisible),
                RestaurantEmber.amber,
                Modifier.weight(1f),
            )
            EmberKpiChip(
                "Platform AR",
                RestaurantFormat.compact(model.currency, model.finance?.outstanding, model.financeVisible),
                RestaurantEmber.ember,
                Modifier.weight(1f),
            )
            EmberKpiChip("Orders", openOrders(model).size.toString(), modifier = Modifier.weight(1f))
        }
        if (rows.isEmpty()) {
            EmberEmpty("Nothing open", "Kitchen tickets, payables and platform receivables will show here.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.take(3).forEach { row -> BoardRowFor(row, model) }
            }
        }
        EmberWhyNote("Catering order ≠ revenue until billed.")
    }
}

/** AN-M01 · My Restaurant `2400:30674`. */
@Composable
private fun MyRestaurantCard(model: OperatingProjectionDto, onOpenSheet: (RestaurantSheetRoute) -> Unit) {
    val retired = model.retired
    val targetConfigured = model.target?.configured == true
    val gstValue = when {
        model.gstTracking != "YES" -> "Not tracked"
        model.gstRegistration.isNullOrBlank() -> "Registration incomplete"
        else -> "Registered" + (model.gstState?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: "")
    }
    EmberCard(tone = EmberTone.Board) {
        EmberBoardHeader("My Restaurant", onViewAll = { onOpenSheet(RestaurantSheetRoute.MomentDetail) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EmberKpiChip(
                "Status",
                if (retired) "Closed" else "Open",
                if (retired) RestaurantEmber.textMuted else RestaurantEmber.positive,
                Modifier.weight(1f),
            )
            EmberKpiChip("Day", dayOfPeriod(model.startDate), modifier = Modifier.weight(1f))
            EmberKpiChip(
                "GST",
                if (model.gstTracking == "YES") "On" else "Off",
                RestaurantEmber.amber,
                Modifier.weight(1f),
            )
        }
        Column {
            DetailRow(
                eyebrow = "Business",
                value = listOfNotNull(model.title, model.location).filter { it.isNotBlank() }.joinToString(" · "),
                trailing = if (retired) "Closed" else "Open",
                trailingColor = if (retired) RestaurantEmber.textMuted else RestaurantEmber.positive,
            )
            DetailRow(
                eyebrow = "Plan",
                value = if (targetConfigured) {
                    "Sales ${RestaurantFormat.compact(model.currency, model.target?.value, model.financeVisible)} target"
                } else {
                    "No target set"
                },
                trailing = if (targetConfigured) "Active" else "Unavailable",
                trailingColor = if (targetConfigured) RestaurantEmber.textPrimary else RestaurantEmber.warning,
            )
            DetailRow(
                eyebrow = "GST",
                value = gstValue,
                trailing = if (model.gstTracking == "YES") "On" else "Off",
                trailingColor = RestaurantEmber.amber,
            )
        }
        EmberWhyNote("Order→prep→bill→collect. Posted bill owns revenue.")
    }
}

/** AN-M02 · People `2400:30707`. */
@Composable
private fun PeopleCard(model: OperatingProjectionDto, onOpenSheet: (RestaurantSheetRoute) -> Unit) {
    val staff = model.participants
    val stations = stationMasters(model).size
    EmberCard(tone = EmberTone.Board) {
        EmberBoardHeader("People", onViewAll = { onOpenSheet(RestaurantSheetRoute.People) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EmberKpiChip("Staff", staff.size.toString(), modifier = Modifier.weight(1f))
            EmberKpiChip(
                "Stations",
                if (stations > 0) stations.toString() else "Unavailable",
                if (stations > 0) RestaurantEmber.textPrimary else RestaurantEmber.warning,
                Modifier.weight(1f),
            )
        }
        if (staff.isEmpty()) {
            EmberEmpty("No people yet", "Invite the kitchen, floor and billing team.")
        } else {
            Column {
                staff.take(3).forEach { name ->
                    val isOwner = name == model.partyName
                    DetailRow(
                        eyebrow = name,
                        value = if (isOwner) roleLabel(model.role) else "Member",
                        trailing = if (isOwner) "Owner" else "Member",
                        trailingColor = if (isOwner) RestaurantEmber.textPrimary else RestaurantEmber.textBody,
                    )
                }
            }
        }
        EmberWhyNote("Roles gate voids, comps and platform settlements.")
    }
}

/** AN-M03 · Business Network `2400:30737`. */
@Composable
private fun NetworkCard(model: OperatingProjectionDto, onOpenSheet: (RestaurantSheetRoute) -> Unit) {
    val masters = model.masters.orEmpty()
    val suppliers = masters.filter { it.kind.uppercase() in setOf("SUPPLIER", "VENDOR", "PRINCIPAL") }
    val platforms = masters.filter { it.kind.uppercase() in setOf("CHANNEL", "PLATFORM", "AGGREGATOR", "CUSTOMER", "BUYER") }
    val payables = model.paymentReadiness.filter { it.relevance != "RECEIVABLE" }
    val receivables = model.paymentReadiness.filter { it.relevance == "RECEIVABLE" }
    val combined = LinkedHashMap<String, Boolean>()
    suppliers.forEach { master -> combined.putIfAbsent(master.name, true) }
    platforms.forEach { master -> combined.putIfAbsent(master.name, false) }

    EmberCard(tone = EmberTone.Board) {
        EmberBoardHeader("Business Network", onViewAll = { onOpenSheet(RestaurantSheetRoute.Network) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EmberKpiChip("Suppliers", suppliers.size.toString(), modifier = Modifier.weight(1f))
            EmberKpiChip("Platforms", platforms.size.toString(), modifier = Modifier.weight(1f))
        }
        if (combined.isEmpty()) {
            EmberEmpty("No network yet", "Suppliers, platforms and credit parties you record will show here.")
        } else {
            Column {
                combined.entries.take(3).forEach { (name, isSupplier) ->
                    val openAp = payables
                        .filter { it.counterpartyName?.equals(name, ignoreCase = true) == true }
                        .fold(BigDecimal.ZERO) { total, row -> total + (row.amount.toBigDecimalOrNull() ?: BigDecimal.ZERO) }
                    val openAr = receivables
                        .filter { it.counterpartyName?.equals(name, ignoreCase = true) == true }
                        .fold(BigDecimal.ZERO) { total, row -> total + (row.amount.toBigDecimalOrNull() ?: BigDecimal.ZERO) }
                    val trailing: String
                    val color: Color
                    when {
                        isSupplier && openAp.signum() > 0 -> {
                            trailing = "AP " + RestaurantFormat.compact(
                                model.currency,
                                openAp.stripTrailingZeros().toPlainString(),
                                model.financeVisible,
                            )
                            color = RestaurantEmber.amber
                        }
                        !isSupplier && openAr.signum() > 0 -> {
                            trailing = "AR " + RestaurantFormat.compact(
                                model.currency,
                                openAr.stripTrailingZeros().toPlainString(),
                                model.financeVisible,
                            )
                            color = RestaurantEmber.ember
                        }
                        else -> {
                            trailing = "No balance"
                            color = RestaurantEmber.textMuted
                        }
                    }
                    DetailRow(
                        eyebrow = name,
                        value = if (isSupplier) "Supplier" else "Platform / buyer",
                        trailing = trailing,
                        trailingColor = color,
                    )
                }
            }
        }
        EmberWhyNote("Platform debt only on explicit allocation.")
    }
}

// ---------------------------------------------------------------------------------
// Life — RST-LIFE-v12 `2400:30824`
// ---------------------------------------------------------------------------------

@Composable
private fun RestaurantLife(model: OperatingProjectionDto, onOpenSheet: (RestaurantSheetRoute) -> Unit) {
    val open = openOrders(model).size
    val done = model.activity.count { it.paymentStatus in setOf("COLLECTED", "PAID", "SETTLED") }
    val sections = model.boards?.life.orEmpty()
    EmberBoardHero(
        eyebrow = "TODAY",
        headline = "${model.title?.takeIf { it.isNotBlank() } ?: "Restaurant"} · today",
        strokeAlpha = 0.45f,
        chips = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EmberBoardHeroChip(RestaurantFormat.todayLine(), RestaurantEmber.ember)
                EmberBoardHeroChip(
                    if (model.retired) "Restaurant closed" else "Service live",
                    if (model.retired) RestaurantEmber.textMuted else RestaurantEmber.positive,
                    dot = true,
                )
            }
        },
        kpis = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EmberKpiChip("Open", open.toString(), RestaurantEmber.ember, Modifier.weight(1f))
                EmberKpiChip("Done", done.toString(), RestaurantEmber.positive, Modifier.weight(1f))
                EmberKpiChip(
                    "Money in motion",
                    RestaurantFormat.compact(model.currency, model.finance?.received, model.financeVisible),
                    modifier = Modifier.weight(1f),
                )
            }
        },
    )
    LifeCurrentCard(model, sections.firstOrNull { it.code == "L1" }, onOpenSheet)
    LifeSalesCard(model, sections, onOpenSheet)
    LifeMoneyCard(model, sections.firstOrNull { it.code == "L3" }, onOpenSheet)
    LifeOpsCard(model, sections.firstOrNull { it.code == "L4" }, onOpenSheet)
}

/**
 * AN-L01 · Current Service `2400:30834`.
 * The frame's `Covers 86` has no contract field, so the second chip reports posted bills.
 */
@Composable
private fun LifeCurrentCard(
    model: OperatingProjectionDto,
    section: OperatingBoardSectionDto?,
    onOpenSheet: (RestaurantSheetRoute) -> Unit,
) {
    val rows = section?.rows.orEmpty()
    val bills = model.activity.count { it.eventType == "SALE" }
    EmberCard(tone = EmberTone.Board) {
        EmberBoardHeader("Current Service", onViewAll = { onOpenSheet(RestaurantSheetRoute.LifeCurrent) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EmberKpiChip("Open tickets", openOrders(model).size.toString(), RestaurantEmber.ember, Modifier.weight(1f))
            EmberKpiChip("Posted bills", bills.toString(), RestaurantEmber.positive, Modifier.weight(1f))
        }
        if (rows.isEmpty()) {
            EmberEmpty(
                "Nothing recorded yet",
                section?.detail?.takeIf { it.isNotBlank() } ?: "Today's kitchen tickets and posted bills will show here.",
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.take(3).forEach { row ->
                    BoardRowFor(
                        row,
                        model,
                        trailingOverride = if (row.eventType == "ORDER") restaurantOrderStatusLabel(row.status) else null,
                    )
                }
            }
        }
        section?.coverage?.takeIf { it.isNotBlank() }?.let { coverage ->
            EmberSeverityChip(coverage, RestaurantEmber.warning)
        }
        EmberWhyNote("Today tracks tickets and posted bills — not forecasts.")
    }
}

/** AN-L02 · Sales & Orders `2400:30864`. */
@Composable
private fun LifeSalesCard(
    model: OperatingProjectionDto,
    sections: List<OperatingBoardSectionDto>,
    onOpenSheet: (RestaurantSheetRoute) -> Unit,
) {
    val bills = model.activity.filter { it.eventType == "SALE" }
    val creditOpen = model.paymentReadiness.count { it.relevance == "RECEIVABLE" }
    val section = sections.firstOrNull { it.code == "L2" }
    EmberCard(tone = EmberTone.Board) {
        EmberBoardHeader("Sales & Orders", onViewAll = { onOpenSheet(RestaurantSheetRoute.LifeSales) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EmberKpiChip("Bills", bills.size.toString(), modifier = Modifier.weight(1f))
            EmberKpiChip("Avg ticket", averageTicket(model, bills), modifier = Modifier.weight(1f))
            EmberKpiChip("Credit open", creditOpen.toString(), RestaurantEmber.ember, Modifier.weight(1f))
        }
        val rows = section?.rows.orEmpty()
        when {
            rows.isNotEmpty() -> Column {
                rows.take(3).forEach { row ->
                    DetailRow(
                        eyebrow = row.channelName?.takeIf { it.isNotBlank() } ?: row.title.ifBlank { "Channel" },
                        value = row.title.ifBlank { "Recorded" },
                        trailing = RestaurantFormat.compact(model.currency, row.amount, model.financeVisible),
                        trailingColor = RestaurantEmber.textPrimary,
                    )
                }
            }
            !section?.detail.isNullOrBlank() -> EmberEmpty("Channel mix", section.detail)
            else -> EmberEmpty("Nothing recorded yet", "Dine-in, takeaway and platform bills will show here.")
        }
        EmberWhyNote("Order value separate from posted sale.")
    }
}

/** AN-L03 · Money Moving `2400:30897`. */
@Composable
private fun LifeMoneyCard(
    model: OperatingProjectionDto,
    section: OperatingBoardSectionDto?,
    onOpenSheet: (RestaurantSheetRoute) -> Unit,
) {
    val rows = section?.rows.orEmpty()
    EmberCard(tone = EmberTone.Board, strokeAlpha = 0.45f) {
        EmberBoardHeader("Money Moving", onViewAll = { onOpenSheet(RestaurantSheetRoute.LifeMoney) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EmberKpiChip(
                "Collected",
                RestaurantFormat.compact(model.currency, model.finance?.received, model.financeVisible),
                RestaurantEmber.positive,
                Modifier.weight(1f),
            )
            EmberKpiChip(
                "Paid",
                RestaurantFormat.compact(model.currency, model.finance?.cashPaid, model.financeVisible),
                modifier = Modifier.weight(1f),
            )
            EmberKpiChip(
                "AR move",
                RestaurantFormat.compact(model.currency, model.finance?.outstanding, model.financeVisible),
                RestaurantEmber.ember,
                Modifier.weight(1f),
            )
        }
        if (rows.isEmpty()) {
            EmberEmpty(
                "Nothing moved yet",
                section?.detail?.takeIf { it.isNotBlank() } ?: "Bill collections and supplier payments will show here.",
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.take(3).forEach { row ->
                    BoardRowFor(
                        row,
                        model,
                        trailingOverride = if (row.paymentStatus in setOf("COLLECTED", "PAID", "SETTLED")) "Done" else "Open",
                    )
                }
            }
        }
        EmberWhyNote("Settlement never reposts the bill.")
    }
}

/** AN-L04 · Operations / Stock `2400:30930`. */
@Composable
private fun LifeOpsCard(
    model: OperatingProjectionDto,
    section: OperatingBoardSectionDto?,
    onOpenSheet: (RestaurantSheetRoute) -> Unit,
) {
    val receipts = model.activity.count { it.eventType == "PURCHASE" }
    val alerts = model.attention.count { it.kind.uppercase().startsWith("LOW_STOCK") }
    val rows = section?.rows.orEmpty()
    EmberCard(tone = EmberTone.Board) {
        EmberBoardHeader("Operations / Stock", onViewAll = { onOpenSheet(RestaurantSheetRoute.LifeOps) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EmberKpiChip(
                "Prep alerts",
                alerts.toString(),
                if (alerts > 0) RestaurantEmber.ember else RestaurantEmber.textPrimary,
                Modifier.weight(1f),
            )
            EmberKpiChip("Receipts", receipts.toString(), modifier = Modifier.weight(1f))
        }
        if (rows.isEmpty()) {
            EmberEmpty(
                "Nothing moving",
                section?.detail?.takeIf { it.isNotBlank() } ?: "Ingredient receipts, counts and waste will show here.",
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.take(3).forEach { row ->
                    BoardRowFor(
                        row,
                        model,
                        trailingOverride = if (row.eventType == "STOCK") "Logged" else "Open",
                    )
                }
            }
        }
        EmberWhyNote("No automatic recipe deduction.")
    }
}

// ---------------------------------------------------------------------------------
// Memory — RST-MEMORY-v12 `2400:30984`
// ---------------------------------------------------------------------------------

@Composable
private fun RestaurantMemory(
    model: OperatingProjectionDto,
    momentId: String?,
    onOpenStoryConfig: () -> Unit,
    onOpenSheet: (RestaurantSheetRoute) -> Unit,
) {
    val target = model.target
    val percent = if (target?.configured == true) RestaurantFormat.percent(target.progress, target.value) else null
    val period = RestaurantFormat.month(model.startDate)
    val sections = model.boards?.memory.orEmpty()
    val revisions = model.memory?.revisions.orEmpty()
    val bills = model.activity.count { it.eventType == "SALE" }

    EmberBoardHero(
        eyebrow = "MEMORY · THIS MONTH",
        headline = "${model.title?.takeIf { it.isNotBlank() } ?: "Restaurant"} · memory",
        strokeAlpha = 0.45f,
        chips = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EmberBoardHeroChip(
                    if (percent != null) {
                        "$period · $percent of ${RestaurantFormat.compact(model.currency, target?.value, model.financeVisible)}"
                    } else {
                        "$period · memory"
                    },
                    RestaurantEmber.ember,
                )
                if (revisions.isNotEmpty()) {
                    EmberBoardHeroChip("${revisions.size} revisions", RestaurantEmber.textMuted)
                }
            }
        },
        kpis = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EmberKpiChip(
                    "Revenue",
                    RestaurantFormat.compact(model.currency, model.finance?.sales, model.financeVisible),
                    modifier = Modifier.weight(1f),
                )
                EmberKpiChip(
                    "vs last period",
                    lastPeriodDelta(sections),
                    RestaurantEmber.warning,
                    Modifier.weight(1f),
                )
                EmberKpiChip("Bills", bills.toString(), modifier = Modifier.weight(1f))
            }
        },
    )

    MonthlyMemoryCard(model, sections.firstOrNull { it.code == "ME1" }, onOpenSheet)
    sections.firstOrNull { it.code == "ME2" }?.let { MoneyMemoryCard(model, it, onOpenSheet) }
    sections.firstOrNull { it.code == "ME3" }?.let { PeopleMemoryCard(model, it, onOpenSheet) }
    sections.firstOrNull { it.code == "ME4" }?.let { PatternsCard(model, it, onOpenSheet) }
    if (momentId != null) StoryCta(period, onOpenStoryConfig)
}

/** AN-H01 · Monthly Memory `2400:30997`. */
@Composable
private fun MonthlyMemoryCard(
    model: OperatingProjectionDto,
    section: OperatingBoardSectionDto?,
    onOpenSheet: (RestaurantSheetRoute) -> Unit,
) {
    val months = section?.rows.orEmpty().filter {
        it.eventType == "MONTH" || it.eventType.isBlank() || it.eventType == "REVISION"
    }
    val average = averageAchievement(months)
    EmberCard(tone = EmberTone.Board) {
        EmberBoardHeader("Monthly Memory", onViewAll = { onOpenSheet(RestaurantSheetRoute.Monthly) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EmberKpiChip("Months", months.size.toString(), modifier = Modifier.weight(1f))
            EmberKpiChip(
                "Avg achv.",
                average,
                if (average == "Unavailable") RestaurantEmber.warning else RestaurantEmber.amber,
                Modifier.weight(1f),
            )
        }
        if (months.isEmpty()) {
            EmberEmpty("No history yet", section?.detail?.takeIf { it.isNotBlank() } ?: "Closed months will show here.")
        } else {
            Column {
                months.take(3).forEach { row ->
                    val parts = row.title.split("·").map { it.trim() }
                    DetailRow(
                        eyebrow = parts.firstOrNull()?.takeIf { it.isNotBlank() }
                            ?: RestaurantFormat.month(row.occurredAt),
                        value = parts.drop(1).joinToString(" · "),
                        trailing = "Recorded",
                        trailingColor = RestaurantEmber.textBody,
                    )
                }
            }
        }
        section?.coverage?.takeIf { it.isNotBlank() }?.let { coverage ->
            EmberSeverityChip(coverage, RestaurantEmber.warning)
        }
        EmberWhyNote("Missing coverage stays unavailable — never averaged as zero.")
    }
}

/** AN-H02 · Money Memory `2400:31027`. */
@Composable
private fun MoneyMemoryCard(
    model: OperatingProjectionDto,
    section: OperatingBoardSectionDto,
    onOpenSheet: (RestaurantSheetRoute) -> Unit,
) {
    val revisions = model.memory?.revisions.orEmpty()
    val rows = section.rows
    EmberCard(tone = EmberTone.Board, strokeAlpha = 0.50f) {
        EmberBoardHeader("Money Memory", onViewAll = { onOpenSheet(RestaurantSheetRoute.MoneyMemory) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EmberKpiChip(
                "Billed",
                RestaurantFormat.compact(model.currency, model.finance?.sales, model.financeVisible),
                modifier = Modifier.weight(1f),
            )
            EmberKpiChip(
                "Collected",
                RestaurantFormat.compact(model.currency, model.finance?.received, model.financeVisible),
                RestaurantEmber.positive,
                Modifier.weight(1f),
            )
            EmberKpiChip(
                "Paid",
                RestaurantFormat.compact(model.currency, model.finance?.cashPaid, model.financeVisible),
                RestaurantEmber.ember,
                Modifier.weight(1f),
            )
        }
        revisions.firstOrNull()?.let { revision ->
            DetailRow(
                eyebrow = "Plan revision",
                value = revision.title.ifBlank { "Target revision" },
                trailing = RestaurantFormat.shortDay(revision.occurredAt) ?: "Recorded",
                trailingColor = RestaurantEmber.amber,
            )
        }
        if (rows.isEmpty()) {
            EmberEmpty(
                "Nothing settled yet",
                section.detail.ifBlank { "Platform settlements and supplier payments will show here." },
            )
        } else {
            Column {
                rows.take(3).forEach { row ->
                    DetailRow(
                        eyebrow = row.title.ifBlank { restaurantEventLabel(row.eventType) },
                        value = row.counterpartyName?.takeIf { it.isNotBlank() } ?: restaurantEventLabel(row.eventType),
                        trailing = RestaurantFormat.compact(model.currency, row.amount, model.financeVisible),
                        trailingColor = RestaurantEmber.textPrimary,
                    )
                }
            }
        }
        EmberWhyNote("Reversals versioned — never silent overwrite.")
    }
}

/** AN-H03 · People & Activity `2400:31060`. */
@Composable
private fun PeopleMemoryCard(
    model: OperatingProjectionDto,
    section: OperatingBoardSectionDto,
    onOpenSheet: (RestaurantSheetRoute) -> Unit,
) {
    val shifts = model.activity.count { it.eventType == "SHIFT" }
    val rows = section.rows
    EmberCard(tone = EmberTone.Board) {
        EmberBoardHeader("People & Activity", trailingNote = "Shifts", onViewAll = {
            onOpenSheet(RestaurantSheetRoute.PeopleMemory)
        })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EmberKpiChip("People", model.participants.size.toString(), modifier = Modifier.weight(1f))
            EmberKpiChip(
                "Shifts",
                if (shifts > 0) shifts.toString() else "Unavailable",
                if (shifts > 0) RestaurantEmber.textPrimary else RestaurantEmber.warning,
                Modifier.weight(1f),
            )
        }
        if (rows.isEmpty()) {
            EmberEmpty("Nothing recorded yet", section.detail.ifBlank { "Who worked which station will show here." })
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.take(3).forEach { row ->
                    BoardRowFor(
                        row,
                        model,
                        trailingOverride = if (row.approvalStatus == "PENDING") "Open" else "Settled",
                    )
                }
            }
        }
    }
}

/** AN-H04 · Patterns & Decisions `2400:31087`. */
@Composable
private fun PatternsCard(
    model: OperatingProjectionDto,
    section: OperatingBoardSectionDto,
    onOpenSheet: (RestaurantSheetRoute) -> Unit,
) {
    val decisions = model.activity.count { it.eventType == "APPROVAL" }
    val approved = model.activity.count { it.eventType == "APPROVAL" && it.approvalStatus == "APPROVED" }
    val rows = section.rows
    EmberCard(tone = EmberTone.Board) {
        EmberBoardHeader("Patterns & Decisions", onViewAll = { onOpenSheet(RestaurantSheetRoute.Patterns) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EmberKpiChip("Decisions", decisions.toString(), modifier = Modifier.weight(1f))
            EmberKpiChip("Approved", approved.toString(), RestaurantEmber.positive, Modifier.weight(1f))
        }
        if (rows.isEmpty()) {
            EmberEmpty("No patterns yet", section.detail.ifBlank { "Approvals and repeated kitchen issues will show here." })
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.take(3).forEach { row ->
                    val isApproved = row.approvalStatus == "APPROVED"
                    val denied = row.approvalStatus == "REJECTED"
                    BoardRowFor(
                        row,
                        model,
                        accent = when {
                            isApproved -> RestaurantEmber.positive
                            denied -> RestaurantEmber.negative
                            else -> RestaurantEmber.amber
                        },
                        trailingOverride = when {
                            isApproved -> "Approved"
                            denied -> "Denied"
                            else -> "Open"
                        },
                    )
                }
            }
        }
        EmberWhyNote("Approval ≠ disbursement.")
    }
}

/** Ember story CTA — opens `RST-STORY-CONFIG` `2671:32352`. */
@Composable
private fun StoryCta(month: String, onTap: () -> Unit) {
    val shape = RestaurantEmber.rStoryCta
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = shape,
                clip = false,
                ambientColor = RestaurantEmber.ember.copy(alpha = 0.4f),
                spotColor = RestaurantEmber.ember.copy(alpha = 0.4f),
            )
            .clip(shape)
            .background(RestaurantEmber.ember)
            .clickable(onClick = onTap)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painterResource(RestaurantTileIcons.plus),
                contentDescription = null,
                tint = RestaurantEmber.inkOnEmber,
                modifier = Modifier.size(15.dp),
            )
            Text("Generate Business Story", style = RestaurantEmber.storyCtaTitle(), maxLines = 1)
        }
        Text(
            "Eight pages from recorded activity · $month preselected",
            style = RestaurantEmber.storyCtaCaption(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ---------------------------------------------------------------------------------
// Shared row shapes
// ---------------------------------------------------------------------------------

@Composable
internal fun BoardRowFor(
    row: OperatingActivityDto,
    model: OperatingProjectionDto,
    accent: Color = RestaurantEmber.ember,
    trailingOverride: String? = null,
) {
    val trailing = trailingOverride
        ?: row.paymentStatus?.let { restaurantPrettyStatus(it) }
        ?: "Open"
    val color = when (trailing) {
        "Done", "Settled", "Approved", "Served", "Ready" -> RestaurantEmber.positive
        "Denied", "Void" -> RestaurantEmber.negative
        "Open", "Pending", "Firing", "Sent" -> RestaurantEmber.amber
        else -> if (row.paymentStatus in setOf("COLLECTED", "PAID", "SETTLED")) {
            RestaurantEmber.positive
        } else {
            RestaurantEmber.textBody
        }
    }
    val captionParts = mutableListOf<String>()
    row.counterpartyName?.takeIf { it.isNotBlank() }?.let { captionParts += it }
    row.channelName?.takeIf { it.isNotBlank() }?.let { captionParts += it }
    row.quantity?.takeIf { it.isNotBlank() }?.let { qty ->
        captionParts += (row.unit?.takeIf { it.isNotBlank() }?.let { "$qty $it" } ?: qty)
    }
    if (captionParts.isEmpty()) captionParts += restaurantEventLabel(row.eventType)
    val value = captionParts.joinToString(" · ").ifBlank { restaurantEventLabel(row.eventType) }
    EmberBoardRow(
        eyebrow = row.title.ifBlank { restaurantEventLabel(row.eventType) },
        value = value,
        trailing = trailing,
        trailingColor = color,
        accent = accent,
        onClick = { OperatingChromeState.editing = row },
    )
}

@Composable
internal fun DetailRow(
    eyebrow: String,
    value: String,
    trailing: String,
    trailingColor: Color = RestaurantEmber.textPrimary,
    accent: Color = RestaurantEmber.ember,
) {
    EmberBoardRow(
        eyebrow = eyebrow,
        value = value,
        trailing = trailing,
        trailingColor = trailingColor,
        accent = accent,
    )
}

// ---------------------------------------------------------------------------------
// Derived facts
// ---------------------------------------------------------------------------------

internal fun orderRows(model: OperatingProjectionDto): List<OperatingActivityDto> =
    model.activity.filter { it.eventType == "ORDER" || it.eventType == "CUSTOMER_ORDER" }

internal fun openOrders(model: OperatingProjectionDto): List<OperatingActivityDto> =
    orderRows(model).filter { it.status?.uppercase() !in setOf("COMPLETED", "CANCELLED") }

internal fun firingOrders(model: OperatingProjectionDto): List<OperatingActivityDto> =
    orderRows(model).filter { it.status?.uppercase() in setOf("PREPARING", "READY") }

internal fun stationMasters(model: OperatingProjectionDto): List<OperatingMasterDto> =
    model.masters.orEmpty().filter { it.kind.uppercase() == "STATION" }

internal fun networkSuppliers(model: OperatingProjectionDto): Int =
    model.masters.orEmpty().count { it.kind.uppercase() in setOf("SUPPLIER", "VENDOR", "PRINCIPAL") }

/** `₹3,265` when at least one bill carries an amount, otherwise `Unavailable`. */
internal fun averageTicket(model: OperatingProjectionDto, bills: List<OperatingActivityDto>): String {
    val amounts = bills.mapNotNull { it.amount?.toBigDecimalOrNull() }
    if (amounts.isEmpty()) return "Unavailable"
    val total = amounts.fold(BigDecimal.ZERO) { acc, value -> acc + value }
    val average = total.divide(BigDecimal(amounts.size), 2, RoundingMode.HALF_UP)
    return RestaurantFormat.exact(model.currency, average.stripTrailingZeros().toPlainString(), model.financeVisible)
}

internal fun dayOfPeriod(start: String?): String {
    val from = RestaurantFormat.parse(start) ?: return "Unavailable"
    val today = java.time.LocalDate.now()
    if (today.isBefore(from)) return "1"
    return (java.time.temporal.ChronoUnit.DAYS.between(from, today) + 1).toString()
}

/** `−6%` when the previous period is known, otherwise `Unavailable`. */
internal fun lastPeriodDelta(sections: List<OperatingBoardSectionDto>): String {
    val monthly = sections.firstOrNull { it.code == "ME1" }?.rows.orEmpty()
    if (monthly.size < 2) return "Unavailable"
    val current = monthly[0].amount?.toBigDecimalOrNull() ?: return "Unavailable"
    val previous = monthly[1].amount?.toBigDecimalOrNull() ?: return "Unavailable"
    if (previous.signum() <= 0) return "Unavailable"
    val delta = current.subtract(previous)
        .divide(previous, 4, RoundingMode.HALF_UP)
        .multiply(BigDecimal(100))
        .setScale(0, RoundingMode.HALF_UP)
    val sign = if (delta.signum() < 0) "−" else "+"
    return "$sign${delta.abs().toPlainString()}%"
}

internal fun averageAchievement(rows: List<OperatingActivityDto>): String {
    val values = rows.mapNotNull { row ->
        Regex("""(\d+)%""").find(row.title)?.groupValues?.getOrNull(1)?.toIntOrNull()
    }
    if (values.isEmpty()) return "Unavailable"
    return "${values.sum() / values.size}%"
}
