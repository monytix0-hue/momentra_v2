package com.example.momentra.ui.shell.business.shared

/**
 * Callers: OperatingMomentScreens.kt QUICK_ADD tab (replaces generic title/amount form).
 * Search confirmed no TypedOperatingQuickAdd in apk/. Worktree capture sheets are 6k+ LOC
 * and not on the main compile path.
 * Reads: OperatingProjectionDto.quickAdds, activity, paymentReadiness, life.channels.
 * Writes: none locally — submits via recordOperatingEvent with Idempotency-Key.
 * User: "Build typed forms for all nine Business Moment types."
 * User: "No generic fallback form for Quick Adds requiring typed data."
 * User: "Validate required fields before submission." / "Use idempotency keys."
 * User: "Preserve drafts after validation failures or stale-source conflicts."
 * User: "Show Saved only after confirmed server success."
 */

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.OperatingContractBody
import com.example.momentra.data.api.OperatingLineBody
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.data.api.OperatingQuickAddDto
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.launch
import java.util.UUID

private val Card = Color(0xFF161B26)
private val Border = Color(0xFF1E293B)
private val Muted = Color(0xFF94A3B8)
private val TextPrimary = Color(0xFFE5E0EE)
private val Accent = Color(0xFF34D399)
private val Error = Color(0xFFF87171)

enum class QuickAddField {
    TITLE, AMOUNT, NOTE, COUNTERPARTY, CHANNEL, PAYMENT_STATUS, DUE_AT, PARENT,
    QUANTITY, UNIT, ITEM_NAME, UNIT_PRICE, EXPENSE_CATEGORY, STOCK_ACTION, STOCK_REASON,
    RETURN_REASON, REVISION_KIND, METRIC_CODE, PERIOD_START, PERIOD_END, ACTIVITY_STATUS,
    COST_BASIS, REJECTED_QTY, LOCATION, TABLE_NUMBER, COVERS, ATTRIBUTED_SALES,
    TAX_MODE, GST_RATE,
}

data class QuickAddFieldSpec(
    val fields: Set<QuickAddField>,
    val required: Set<QuickAddField>,
)

/** Typed field inventory for all nine subtypes / Quick Add codes. */
fun quickAddFieldSpec(eventType: String, subtype: String?): QuickAddFieldSpec {
    val retailish = subtype in setOf("RETAIL_STORE", "RESTAURANT_FOOD", "CONSUMER_SERVICE")
    val own = subtype == "OWN_BUSINESS_CAMPAIGN"
    val marketing = subtype in setOf("CLIENT_CAMPAIGN", "OWN_BUSINESS_CAMPAIGN")

    if (own && eventType in setOf("INVOICE", "COLLECTION")) {
        return QuickAddFieldSpec(emptySet(), emptySet())
    }

    return when (eventType) {
        "SALE", "SERVICE" -> QuickAddFieldSpec(
            fields = setOf(
                QuickAddField.TITLE, QuickAddField.AMOUNT, QuickAddField.COUNTERPARTY,
                QuickAddField.CHANNEL, QuickAddField.PAYMENT_STATUS, QuickAddField.DUE_AT,
                QuickAddField.ITEM_NAME, QuickAddField.QUANTITY, QuickAddField.UNIT,
                QuickAddField.UNIT_PRICE, QuickAddField.TAX_MODE, QuickAddField.GST_RATE,
                QuickAddField.NOTE,
            ) + if (subtype == "RESTAURANT_FOOD") {
                setOf(QuickAddField.TABLE_NUMBER, QuickAddField.COVERS)
            } else {
                emptySet()
            } + if (subtype == "MADE_TO_ORDER") setOf(QuickAddField.PARENT) else emptySet(),
            required = buildSet {
                add(QuickAddField.TITLE)
                add(QuickAddField.AMOUNT)
                if (retailish) add(QuickAddField.CHANNEL)
            },
        )
        "INVOICE" -> QuickAddFieldSpec(
            fields = setOf(
                QuickAddField.TITLE, QuickAddField.AMOUNT, QuickAddField.COUNTERPARTY,
                QuickAddField.DUE_AT, QuickAddField.TAX_MODE, QuickAddField.GST_RATE, QuickAddField.NOTE,
            ),
            required = setOf(QuickAddField.TITLE, QuickAddField.AMOUNT, QuickAddField.COUNTERPARTY),
        )
        "PURCHASE" -> QuickAddFieldSpec(
            fields = setOf(
                QuickAddField.TITLE, QuickAddField.AMOUNT, QuickAddField.COUNTERPARTY,
                QuickAddField.PAYMENT_STATUS, QuickAddField.DUE_AT, QuickAddField.ITEM_NAME,
                QuickAddField.QUANTITY, QuickAddField.UNIT, QuickAddField.UNIT_PRICE,
                QuickAddField.TAX_MODE, QuickAddField.GST_RATE, QuickAddField.NOTE,
            ),
            required = setOf(QuickAddField.TITLE, QuickAddField.AMOUNT, QuickAddField.COUNTERPARTY),
        )
        "SPEND" -> QuickAddFieldSpec(
            fields = setOf(
                QuickAddField.TITLE, QuickAddField.AMOUNT, QuickAddField.COUNTERPARTY,
                QuickAddField.EXPENSE_CATEGORY, QuickAddField.PAYMENT_STATUS, QuickAddField.NOTE,
            ) + if (marketing && !own) setOf(QuickAddField.COST_BASIS) else emptySet(),
            required = setOf(QuickAddField.TITLE, QuickAddField.AMOUNT, QuickAddField.EXPENSE_CATEGORY),
        )
        "PAYMENT", "COLLECTION" -> QuickAddFieldSpec(
            fields = setOf(
                QuickAddField.TITLE, QuickAddField.AMOUNT, QuickAddField.PARENT,
                QuickAddField.COUNTERPARTY, QuickAddField.NOTE,
            ),
            required = setOf(QuickAddField.TITLE, QuickAddField.AMOUNT, QuickAddField.PARENT),
        )
        "STOCK" -> QuickAddFieldSpec(
            fields = setOf(
                QuickAddField.TITLE, QuickAddField.ITEM_NAME, QuickAddField.QUANTITY, QuickAddField.UNIT,
                QuickAddField.STOCK_ACTION, QuickAddField.STOCK_REASON, QuickAddField.LOCATION, QuickAddField.NOTE,
            ),
            required = setOf(
                QuickAddField.TITLE, QuickAddField.ITEM_NAME, QuickAddField.QUANTITY, QuickAddField.STOCK_ACTION,
            ),
        )
        "ORDER", "CUSTOMER_ORDER" -> QuickAddFieldSpec(
            fields = setOf(
                QuickAddField.TITLE, QuickAddField.AMOUNT, QuickAddField.COUNTERPARTY,
                QuickAddField.CHANNEL, QuickAddField.DUE_AT, QuickAddField.ITEM_NAME,
                QuickAddField.QUANTITY, QuickAddField.UNIT, QuickAddField.NOTE,
            ),
            required = buildSet {
                add(QuickAddField.TITLE)
                add(QuickAddField.COUNTERPARTY)
                if (retailish) add(QuickAddField.CHANNEL)
            },
        )
        "SHIPMENT", "DELIVERY" -> QuickAddFieldSpec(
            fields = setOf(
                QuickAddField.TITLE, QuickAddField.PARENT, QuickAddField.QUANTITY, QuickAddField.UNIT,
                QuickAddField.COUNTERPARTY, QuickAddField.NOTE,
            ),
            required = setOf(QuickAddField.TITLE, QuickAddField.QUANTITY, QuickAddField.PARENT),
        )
        "RETURN" -> QuickAddFieldSpec(
            fields = setOf(
                QuickAddField.TITLE, QuickAddField.AMOUNT, QuickAddField.PARENT,
                QuickAddField.RETURN_REASON, QuickAddField.QUANTITY, QuickAddField.ITEM_NAME,
                QuickAddField.TAX_MODE, QuickAddField.GST_RATE, QuickAddField.NOTE,
            ),
            required = setOf(QuickAddField.TITLE, QuickAddField.AMOUNT, QuickAddField.RETURN_REASON),
        )
        "PRODUCTION" -> QuickAddFieldSpec(
            fields = setOf(
                QuickAddField.TITLE, QuickAddField.ITEM_NAME, QuickAddField.QUANTITY, QuickAddField.UNIT,
                QuickAddField.REJECTED_QTY, QuickAddField.PARENT, QuickAddField.NOTE,
            ),
            required = setOf(QuickAddField.TITLE, QuickAddField.QUANTITY),
        )
        "APPOINTMENT" -> QuickAddFieldSpec(
            fields = setOf(QuickAddField.TITLE, QuickAddField.COUNTERPARTY, QuickAddField.NOTE),
            required = setOf(QuickAddField.TITLE),
        )
        "BUDGET", "TARGET" -> QuickAddFieldSpec(
            fields = setOf(QuickAddField.TITLE, QuickAddField.AMOUNT, QuickAddField.METRIC_CODE, QuickAddField.NOTE),
            required = setOf(QuickAddField.TITLE, QuickAddField.AMOUNT),
        )
        "REVISION" -> QuickAddFieldSpec(
            fields = setOf(
                QuickAddField.TITLE, QuickAddField.AMOUNT, QuickAddField.REVISION_KIND,
                QuickAddField.METRIC_CODE, QuickAddField.NOTE,
            ),
            required = setOf(QuickAddField.TITLE, QuickAddField.REVISION_KIND),
        )
        "PROGRESS" -> QuickAddFieldSpec(
            fields = setOf(
                QuickAddField.TITLE, QuickAddField.PARENT, QuickAddField.ACTIVITY_STATUS,
                QuickAddField.AMOUNT, QuickAddField.QUANTITY, QuickAddField.NOTE,
            ),
            required = setOf(QuickAddField.TITLE, QuickAddField.PARENT),
        )
        "RESULT" -> QuickAddFieldSpec(
            fields = setOf(
                QuickAddField.TITLE, QuickAddField.AMOUNT, QuickAddField.ATTRIBUTED_SALES,
                QuickAddField.PERIOD_START, QuickAddField.PERIOD_END, QuickAddField.METRIC_CODE, QuickAddField.NOTE,
            ),
            required = setOf(QuickAddField.TITLE),
        )
        "ACTIVITY", "UPDATE", "ISSUE", "APPROVAL" -> QuickAddFieldSpec(
            fields = setOf(QuickAddField.TITLE, QuickAddField.NOTE, QuickAddField.AMOUNT),
            required = setOf(QuickAddField.TITLE),
        )
        else -> QuickAddFieldSpec(
            fields = setOf(QuickAddField.TITLE, QuickAddField.AMOUNT, QuickAddField.NOTE),
            required = setOf(QuickAddField.TITLE),
        )
    }
}

data class TypedQuickAddDraft(
    var title: String = "",
    var amount: String = "",
    var note: String = "",
    var counterparty: String = "",
    var channel: String = "",
    var paymentStatus: String = "UNPAID",
    var dueAt: String = "",
    var parentEventId: String = "",
    var quantity: String = "",
    var unit: String = "pc",
    var itemName: String = "",
    var unitPrice: String = "",
    var expenseCategory: String = "",
    var stockAction: String = "SET",
    var stockReason: String = "",
    var returnReason: String = "",
    var revisionKind: String = "BUDGET",
    var metricCode: String = "",
    var periodStart: String = "",
    var periodEnd: String = "",
    var activityStatus: String = "",
    var costBasis: String = "AGENCY",
    var rejectedQty: String = "",
    var location: String = "",
    var tableNumber: String = "",
    var covers: String = "",
    var attributedSales: String = "",
    var taxMode: String = "EXCLUSIVE",
    var gstRate: String = "",
)

fun validateTypedQuickAdd(
    eventType: String,
    subtype: String?,
    needsAmount: Boolean,
    draft: TypedQuickAddDraft,
): List<String> {
    val spec = quickAddFieldSpec(eventType, subtype)
    if (spec.fields.isEmpty()) return listOf("This Quick Add is not available for this Moment type.")
    val errors = mutableListOf<String>()
    fun req(field: QuickAddField, value: String, label: String) {
        if (field in spec.required && value.isBlank()) errors += "$label is required"
    }
    req(QuickAddField.TITLE, draft.title, "Title")
    if (needsAmount || QuickAddField.AMOUNT in spec.required) {
        req(QuickAddField.AMOUNT, draft.amount, "Amount")
    }
    req(QuickAddField.COUNTERPARTY, draft.counterparty, "Party")
    req(QuickAddField.CHANNEL, draft.channel, "Channel")
    req(QuickAddField.PARENT, draft.parentEventId, "Linked source")
    req(QuickAddField.QUANTITY, draft.quantity, "Quantity")
    req(QuickAddField.ITEM_NAME, draft.itemName, "Item")
    req(QuickAddField.EXPENSE_CATEGORY, draft.expenseCategory, "Expense category")
    req(QuickAddField.STOCK_ACTION, draft.stockAction, "Stock action")
    req(QuickAddField.RETURN_REASON, draft.returnReason, "Return reason")
    req(QuickAddField.REVISION_KIND, draft.revisionKind, "Revision kind")
    return errors
}

@Composable
fun TypedOperatingQuickAddForm(
    model: OperatingProjectionDto?,
    onSubmit: suspend (
        eventType: String,
        title: String,
        amount: String?,
        note: String?,
        channelName: String?,
        counterpartyName: String?,
        paymentStatus: String?,
        parentEventId: String?,
        quantity: String?,
        unit: String?,
        lines: List<OperatingLineBody>?,
        dueAt: String?,
        contract: OperatingContractBody?,
        metricCode: String?,
        activityStatus: String?,
        attributedSales: String?,
        revisionKind: String?,
        rejectedQuantity: String?,
        costBasis: String?,
        periodStart: String?,
        periodEnd: String?,
        idempotencyKey: String,
    ) -> Result<Unit>,
    onDecide: suspend (String, String) -> Unit,
) {
    val adds = model?.quickAdds.orEmpty()
        .filterNot { model?.subtype == "OWN_BUSINESS_CAMPAIGN" && it.code in setOf("INVOICE", "COLLECTION") }
    var selected by remember(adds) { mutableStateOf(adds.firstOrNull()?.code.orEmpty()) }
    var draft by remember(selected) { mutableStateOf(TypedQuickAddDraft()) }
    var errors by remember { mutableStateOf<List<String>>(emptyList()) }
    var submitting by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val spec = quickAddFieldSpec(selected, model?.subtype)
    val needsAmount = adds.firstOrNull { it.code == selected }?.needsAmount == true

    Text("Quick Add", color = TextPrimary, fontWeight = FontWeight.Medium, fontFamily = PlusJakartaSans)
    if (adds.isEmpty()) {
        Text("Your role cannot add to this moment.", color = Muted, fontSize = 13.sp, fontFamily = PlusJakartaSans)
        return
    }
    ChipRow(adds, selected) {
        selected = it
        errors = emptyList()
        confirm = null
    }

    if (QuickAddField.TITLE in spec.fields) DraftField("Title *", draft.title) { draft = draft.copy(title = it) }
    if (QuickAddField.AMOUNT in spec.fields) {
        DraftField(
            if (needsAmount || QuickAddField.AMOUNT in spec.required) "Amount *" else "Amount",
            draft.amount,
        ) { draft = draft.copy(amount = it) }
    }
    if (QuickAddField.COUNTERPARTY in spec.fields) {
        DraftField(
            if (QuickAddField.COUNTERPARTY in spec.required) "Party *" else "Party",
            draft.counterparty,
        ) { draft = draft.copy(counterparty = it) }
    }
    if (QuickAddField.CHANNEL in spec.fields) {
        val channels = model?.life?.channels.orEmpty()
        if (channels.isNotEmpty()) {
            Text("Channel", color = Muted, fontSize = 11.sp, fontFamily = PlusJakartaSans)
            channels.forEach { ch ->
                val on = draft.channel == ch
                Text(
                    ch,
                    color = if (on) Color(0xFF0C0F15) else TextPrimary,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (on) Accent else Card)
                        .clickable { draft = draft.copy(channel = ch) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    fontFamily = PlusJakartaSans,
                )
            }
        } else {
            DraftField(
                if (QuickAddField.CHANNEL in spec.required) "Channel *" else "Channel",
                draft.channel,
            ) { draft = draft.copy(channel = it) }
        }
    }
    if (QuickAddField.PAYMENT_STATUS in spec.fields) {
        ChoiceChips("Payment", listOf("PAID", "PARTIAL", "UNPAID"), draft.paymentStatus) {
            draft = draft.copy(paymentStatus = it)
        }
    }
    if (QuickAddField.DUE_AT in spec.fields) {
        DraftField("Due at (ISO date)", draft.dueAt) { draft = draft.copy(dueAt = it) }
    }
    if (QuickAddField.PARENT in spec.fields) {
        ParentPicker(model, selected, draft.parentEventId) { draft = draft.copy(parentEventId = it) }
    }
    if (QuickAddField.ITEM_NAME in spec.fields) {
        DraftField(
            if (QuickAddField.ITEM_NAME in spec.required) "Item *" else "Item",
            draft.itemName,
        ) { draft = draft.copy(itemName = it) }
    }
    if (QuickAddField.QUANTITY in spec.fields) {
        DraftField(
            if (QuickAddField.QUANTITY in spec.required) "Quantity *" else "Quantity",
            draft.quantity,
        ) { draft = draft.copy(quantity = it) }
    }
    if (QuickAddField.UNIT in spec.fields) DraftField("Unit", draft.unit) { draft = draft.copy(unit = it) }
    if (QuickAddField.UNIT_PRICE in spec.fields) {
        DraftField("Unit price", draft.unitPrice) { draft = draft.copy(unitPrice = it) }
    }
    // Callers: SALE/PURCHASE/INVOICE/RETURN specs. Server owns tax math; client only collects.
    // User: "Support item lines, GST... No client-side authoritative financial calculations."
    if (QuickAddField.TAX_MODE in spec.fields) {
        ChoiceChips("Tax mode", listOf("EXCLUSIVE", "INCLUSIVE", "NONE"), draft.taxMode) {
            draft = draft.copy(taxMode = it)
        }
    }
    if (QuickAddField.GST_RATE in spec.fields) {
        DraftField("GST rate % (server applies)", draft.gstRate) { draft = draft.copy(gstRate = it) }
    }
    if (QuickAddField.EXPENSE_CATEGORY in spec.fields) {
        ChoiceChips(
            "Expense category",
            listOf("Rent", "Utilities", "Payroll", "Marketing", "Logistics", "Consumables", "Other"),
            draft.expenseCategory.ifBlank { "Other" },
        ) { draft = draft.copy(expenseCategory = it) }
    }
    if (QuickAddField.STOCK_ACTION in spec.fields) {
        ChoiceChips("Stock action", listOf("SET", "ADD", "REMOVE", "TRANSFER"), draft.stockAction) {
            draft = draft.copy(stockAction = it)
        }
    }
    if (QuickAddField.STOCK_REASON in spec.fields) {
        DraftField("Stock reason", draft.stockReason) { draft = draft.copy(stockReason = it) }
    }
    if (QuickAddField.RETURN_REASON in spec.fields) {
        DraftField("Return reason *", draft.returnReason) { draft = draft.copy(returnReason = it) }
    }
    if (QuickAddField.REVISION_KIND in spec.fields) {
        ChoiceChips("Revision", listOf("BUDGET", "TARGET", "CATEGORY"), draft.revisionKind) {
            draft = draft.copy(revisionKind = it)
        }
    }
    if (QuickAddField.METRIC_CODE in spec.fields) {
        DraftField("Metric code", draft.metricCode) { draft = draft.copy(metricCode = it) }
    }
    if (QuickAddField.PERIOD_START in spec.fields) {
        DraftField("Period start", draft.periodStart) { draft = draft.copy(periodStart = it) }
    }
    if (QuickAddField.PERIOD_END in spec.fields) {
        DraftField("Period end", draft.periodEnd) { draft = draft.copy(periodEnd = it) }
    }
    if (QuickAddField.ACTIVITY_STATUS in spec.fields) {
        ChoiceChips(
            "Status",
            listOf("PLANNED", "IN_PROGRESS", "DONE", "BLOCKED"),
            draft.activityStatus.ifBlank { "IN_PROGRESS" },
        ) { draft = draft.copy(activityStatus = it) }
    }
    if (QuickAddField.COST_BASIS in spec.fields) {
        ChoiceChips("Cost basis", listOf("AGENCY", "REIMBURSABLE"), draft.costBasis) {
            draft = draft.copy(costBasis = it)
        }
    }
    if (QuickAddField.REJECTED_QTY in spec.fields) {
        DraftField("Rejected quantity", draft.rejectedQty) { draft = draft.copy(rejectedQty = it) }
    }
    if (QuickAddField.LOCATION in spec.fields) {
        DraftField("Location", draft.location) { draft = draft.copy(location = it) }
    }
    if (QuickAddField.TABLE_NUMBER in spec.fields) {
        DraftField("Table", draft.tableNumber) { draft = draft.copy(tableNumber = it) }
    }
    if (QuickAddField.COVERS in spec.fields) {
        DraftField("Covers", draft.covers) { draft = draft.copy(covers = it) }
    }
    if (QuickAddField.ATTRIBUTED_SALES in spec.fields) {
        DraftField("Attributed sales", draft.attributedSales) { draft = draft.copy(attributedSales = it) }
    }
    if (QuickAddField.NOTE in spec.fields) DraftField("Note", draft.note) { draft = draft.copy(note = it) }

    errors.forEach { Text(it, color = Error, fontSize = 12.sp, fontFamily = PlusJakartaSans) }
    confirm?.let { Text(it, color = Accent, fontSize = 13.sp, fontFamily = PlusJakartaSans) }

    Text(
        if (submitting) "Saving…" else "Add",
        color = Color(0xFF0C0F15),
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Accent)
            .clickable(enabled = !submitting && selected.isNotBlank()) {
                val validation = validateTypedQuickAdd(selected, model?.subtype, needsAmount, draft)
                if (validation.isNotEmpty()) {
                    errors = validation
                    return@clickable
                }
                errors = emptyList()
                val idem = UUID.randomUUID().toString()
                // Callers: onSubmit → recordOperatingEvent. GST rate is operator-entered; server totals.
                // User: "No client-side authoritative financial calculations."
                val lines = if (draft.itemName.isNotBlank()) {
                    listOf(
                        OperatingLineBody(
                            itemName = draft.itemName.trim(),
                            quantity = draft.quantity.trim().ifBlank { null },
                            unit = draft.unit.trim().ifBlank { null },
                            unitPrice = draft.unitPrice.trim().ifBlank { null },
                            taxMode = draft.taxMode.trim().ifBlank { null },
                            gstRate = draft.gstRate.trim().ifBlank { null },
                        ),
                    )
                } else {
                    null
                }
                val contract = OperatingContractBody(
                    expenseCategory = draft.expenseCategory.trim().ifBlank { null },
                    stockAction = draft.stockAction.trim().ifBlank { null },
                    stockReason = draft.stockReason.trim().ifBlank { null },
                    returnReason = draft.returnReason.trim().ifBlank { null },
                    taxMode = draft.taxMode.trim().ifBlank { null },
                    gstRate = draft.gstRate.trim().ifBlank { null },
                    locationName = draft.location.trim().ifBlank { null },
                    tableNumber = draft.tableNumber.trim().ifBlank { null },
                    covers = draft.covers.trim().ifBlank { null },
                )
                scope.launch {
                    submitting = true
                    onSubmit(
                        selected,
                        draft.title.trim(),
                        draft.amount.trim().ifBlank { null },
                        draft.note.trim().ifBlank { null },
                        draft.channel.trim().ifBlank { null },
                        draft.counterparty.trim().ifBlank { null },
                        draft.paymentStatus.trim().ifBlank { null },
                        draft.parentEventId.trim().ifBlank { null },
                        draft.quantity.trim().ifBlank { null },
                        draft.unit.trim().ifBlank { null },
                        lines,
                        draft.dueAt.trim().ifBlank { null },
                        contract,
                        draft.metricCode.trim().ifBlank { null },
                        draft.activityStatus.trim().ifBlank { null },
                        draft.attributedSales.trim().ifBlank { null },
                        draft.revisionKind.trim().ifBlank { null },
                        draft.rejectedQty.trim().ifBlank { null },
                        draft.costBasis.trim().ifBlank { null },
                        draft.periodStart.trim().ifBlank { null },
                        draft.periodEnd.trim().ifBlank { null },
                        idem,
                    ).fold(
                        onSuccess = {
                            confirm = "Saved ${draft.title.trim()}"
                            draft = draft.copy(title = "", amount = "", note = "", quantity = "", itemName = "")
                        },
                        onFailure = { err ->
                            errors = listOf(err.message ?: "Could not save")
                        },
                    )
                    submitting = false
                }
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        fontFamily = PlusJakartaSans,
    )

    model?.attention?.filter { it.kind == "APPROVAL" }?.forEach { item ->
        Text(
            "Approve ${item.title}",
            color = Accent,
            modifier = Modifier.clickable { scope.launch { onDecide(item.id, "APPROVED") } },
            fontFamily = PlusJakartaSans,
        )
    }
}

@Composable
private fun ChipRow(adds: List<OperatingQuickAddDto>, selected: String, onSelect: (String) -> Unit) {
    adds.forEach { add ->
        val on = add.code == selected
        Text(
            add.label,
            color = if (on) Color(0xFF0C0F15) else TextPrimary,
            fontSize = 12.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(if (on) Accent else Card)
                .clickable { onSelect(add.code) }
                .padding(horizontal = 10.dp, vertical = 8.dp),
            fontFamily = PlusJakartaSans,
        )
    }
}

@Composable
private fun ChoiceChips(label: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Text(label, color = Muted, fontSize = 11.sp, fontFamily = PlusJakartaSans)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { opt ->
            val on = opt == selected
            Text(
                opt,
                color = if (on) Color(0xFF0C0F15) else TextPrimary,
                fontSize = 12.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (on) Accent else Card)
                    .clickable { onSelect(opt) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                fontFamily = PlusJakartaSans,
            )
        }
    }
}

@Composable
private fun ParentPicker(
    model: OperatingProjectionDto?,
    eventType: String,
    selectedId: String,
    onSelect: (String) -> Unit,
) {
    Text(
        if (eventType == "COLLECTION" || eventType == "PAYMENT") "Linked source *" else "Linked source",
        color = Muted,
        fontSize = 11.sp,
        fontFamily = PlusJakartaSans,
    )
    val parents = buildList {
        model?.paymentReadiness?.forEach { add(it.id to "${it.title} · ${it.amount} · ${it.status}") }
        model?.activity
            ?.filter {
                it.eventType in setOf(
                    "SALE", "PURCHASE", "INVOICE", "ORDER", "CUSTOMER_ORDER", "SHIPMENT", "ACTIVITY",
                )
            }
            ?.forEach { add(it.id to "${it.eventType} · ${it.title}") }
    }.distinctBy { it.first }
    if (parents.isEmpty()) {
        Text("No eligible source records yet.", color = Muted, fontSize = 12.sp, fontFamily = PlusJakartaSans)
        DraftField("Parent event id", selectedId, onSelect)
    } else {
        parents.take(12).forEach { (id, label) ->
            val on = selectedId == id
            Text(
                label,
                color = if (on) Color(0xFF0C0F15) else TextPrimary,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (on) Accent else Card)
                    .clickable { onSelect(id) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                fontFamily = PlusJakartaSans,
            )
        }
    }
}

@Composable
private fun DraftField(label: String, value: String, onChange: (String) -> Unit) {
    Text(label, color = Muted, fontSize = 11.sp, fontFamily = PlusJakartaSans)
    BasicTextField(
        value = value,
        onValueChange = onChange,
        textStyle = TextStyle(color = TextPrimary, fontFamily = PlusJakartaSans),
        cursorBrush = SolidColor(Accent),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Card)
            .border(1.dp, Border, RoundedCornerShape(12.dp))
            .padding(12.dp),
    )
}
