package com.example.momentra.ui.shell.business.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.OperatingBoardSectionDto
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.data.repository.BusinessSliceRepository
import com.example.momentra.domain.BottomDestination
import com.example.momentra.ui.shell.business.distribution.DistributionActive
import com.example.momentra.ui.shell.business.marketing.MarketingActive
import com.example.momentra.ui.shell.business.production.ProductionActive
import com.example.momentra.ui.shell.business.restaurant.RestaurantActive
import com.example.momentra.ui.shell.business.trading.TradingActive
import com.example.momentra.ui.shell.business.service.ConsumerServiceActive
import com.example.momentra.ui.shell.business.retail.RetailStoreActive

private val Bg = Color(0xFF0C0F15)
private val Card = Color(0xFF161B26)
private val Border = Color(0xFF1E293B)
private val Muted = Color(0xFF94A3B8)
private val TextPrimary = Color(0xFFE5E0EE)

enum class OperatingTab { PULSE, MOMENTS, LIFE, MEMORY, QUICK_ADD }

fun operatingTabFor(destination: BottomDestination): OperatingTab = when (destination) {
    BottomDestination.MOMENTS -> OperatingTab.MOMENTS
    BottomDestination.LIFE -> OperatingTab.LIFE
    BottomDestination.MEMORY -> OperatingTab.MEMORY
    BottomDestination.CREATE -> OperatingTab.QUICK_ADD
    else -> OperatingTab.PULSE
}

@Composable
fun OperatingMomentPage(
    momentId: String?,
    momentTypeCode: String?,
    momentTitle: String?,
    tab: OperatingTab,
    refreshToken: Int,
    onCreateMoment: () -> Unit,
    onOpenQuickAdd: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (momentId.isNullOrBlank()) {
        RetiredBusinessMoment(momentTitle, onCreateMoment, modifier)
        return
    }
    if (OperatingMomentCatalog.isRetired(momentTypeCode)) {
        RetiredBusinessMoment(momentTitle, onCreateMoment, modifier)
        return
    }
    var projection by remember(momentId) { mutableStateOf<OperatingProjectionDto?>(null) }
    var error by remember(momentId) { mutableStateOf<String?>(null) }
    var tick by remember(momentId) { mutableIntStateOf(0) }
    val repo = remember { BusinessSliceRepository() }
    val activeMomentId = momentId
    androidx.compose.runtime.SideEffect {
        OperatingChromeState.record = { draft ->
            repo.recordOperatingEvent(
                activeMomentId,
                draft.eventType,
                draft.title,
                draft.amount,
                draft.note,
                draft.channelName,
                draft.counterpartyName,
                draft.paymentStatus,
                draft.occurredAt,
                draft.parentEventId,
                draft.quantity,
                draft.unit,
                if (draft.itemName.isNullOrBlank()) null else listOf(
                    com.example.momentra.data.api.OperatingLineBody(
                        draft.itemName,
                        draft.quantity,
                        draft.unit,
                        draft.unitPrice,
                        masterId = draft.masterId,
                    ),
                ),
                draft.masterKind?.let {
                    com.example.momentra.data.api.OperatingMasterBody(it, draft.title, draft.unit)
                },
                draft.revisesEventId,
            ).fold(
                onSuccess = { tick += 1 },
                onFailure = { error = it.message },
            )
        }
    }
    LaunchedEffect(momentId, refreshToken, tick) {
        repo.getOperating(momentId).fold(
            onSuccess = {
                projection = it
                OperatingChromeState.participantNames = it.participants
                error = null
            },
            onFailure = { error = it.message ?: "Could not load this moment" },
        )
    }
    val model = projection
    if (model?.retired == true) {
        RetiredBusinessMoment(model.title ?: momentTitle, onCreateMoment, modifier)
        return
    }
    if (model != null && model.subtype == "RETAIL_STORE") {
        RetailStoreActive(
            model = model,
            tab = tab,
            onOpenQuickAdd = onOpenQuickAdd,
            onRecord = { draft ->
                repo.recordOperatingEvent(
                    momentId,
                    draft.eventType,
                    draft.title,
                    draft.amount,
                    draft.note,
                    draft.channelName,
                    draft.counterpartyName,
                    draft.paymentStatus,
                    draft.occurredAt,
                    draft.parentEventId,
                    draft.quantity,
                    draft.unit,
                    if (draft.itemName.isNullOrBlank()) null else listOf(
                        com.example.momentra.data.api.OperatingLineBody(
                            draft.itemName,
                            draft.quantity,
                            draft.unit,
                            draft.unitPrice,
                        ),
                    ),
                    draft.masterKind?.let {
                        com.example.momentra.data.api.OperatingMasterBody(it, draft.title, draft.unit)
                    },
                    draft.revisesEventId,
                    draft.dueAt,
                    draft.contract,
                ).fold(
                    onSuccess = { tick += 1 },
                    onFailure = { error = it.message },
                )
            },
            onDecide = { eventId, decision ->
                repo.decideOperatingEvent(momentId, eventId, decision).fold(
                    onSuccess = { tick += 1 },
                    onFailure = { error = it.message },
                )
            },
            modifier = modifier,
        )
        return
    }
    if (model != null && model.subtype == "RESTAURANT_FOOD") {
        RestaurantActive(
            model = model,
            tab = tab,
            onOpenQuickAdd = onOpenQuickAdd,
            onRecord = { draft ->
                repo.recordOperatingEvent(
                    momentId,
                    draft.eventType,
                    draft.title,
                    draft.amount,
                    draft.note,
                    draft.channelName,
                    draft.counterpartyName,
                    draft.paymentStatus,
                    draft.occurredAt,
                    draft.parentEventId,
                    draft.quantity,
                    draft.unit,
                    if (draft.itemName.isNullOrBlank()) null else listOf(
                        com.example.momentra.data.api.OperatingLineBody(
                            draft.itemName,
                            draft.quantity,
                            draft.unit,
                            draft.unitPrice,
                        ),
                    ),
                    draft.masterKind?.let {
                        com.example.momentra.data.api.OperatingMasterBody(it, draft.title, draft.unit)
                    },
                    draft.revisesEventId,
                    draft.dueAt,
                    draft.contract,
                ).fold(
                    onSuccess = { tick += 1 },
                    onFailure = { error = it.message },
                )
            },
            onDecide = { eventId, decision ->
                repo.decideOperatingEvent(momentId, eventId, decision).fold(
                    onSuccess = { tick += 1 },
                    onFailure = { error = it.message },
                )
            },
            modifier = modifier,
        )
        return
    }
    if (model != null && (model.subtype == "CLIENT_CAMPAIGN" || model.subtype == "OWN_BUSINESS_CAMPAIGN")) {
        MarketingActive(
            model = model,
            tab = tab,
            onOpenQuickAdd = onOpenQuickAdd,
            onRecord = { draft ->
                repo.recordOperatingEvent(
                    momentId,
                    draft.eventType,
                    draft.title,
                    draft.amount,
                    draft.note,
                    draft.channelName,
                    draft.counterpartyName,
                    draft.paymentStatus,
                    draft.occurredAt,
                    draft.parentEventId,
                    draft.quantity,
                    draft.unit,
                    if (draft.itemName.isNullOrBlank()) null else listOf(
                        com.example.momentra.data.api.OperatingLineBody(
                            draft.itemName,
                            draft.quantity,
                            draft.unit,
                            draft.unitPrice,
                        ),
                    ),
                    draft.masterKind?.let {
                        com.example.momentra.data.api.OperatingMasterBody(it, draft.title, draft.unit)
                    },
                    draft.revisesEventId,
                    draft.dueAt,
                    draft.contract,
                ).fold(
                    onSuccess = { tick += 1 },
                    onFailure = { error = it.message },
                )
            },
            onDecide = { eventId, decision ->
                repo.decideOperatingEvent(momentId, eventId, decision).fold(
                    onSuccess = { tick += 1 },
                    onFailure = { error = it.message },
                )
            },
            modifier = modifier,
        )
        return
    }
    if (model != null && (model.subtype == "STANDARD_PRODUCTION" || model.subtype == "MADE_TO_ORDER")) {
        ProductionActive(
            model = model,
            tab = tab,
            onOpenQuickAdd = onOpenQuickAdd,
            onRecord = { draft ->
                repo.recordOperatingEvent(
                    momentId,
                    draft.eventType,
                    draft.title,
                    draft.amount,
                    draft.note,
                    draft.channelName,
                    draft.counterpartyName,
                    draft.paymentStatus,
                    draft.occurredAt,
                    draft.parentEventId,
                    draft.quantity,
                    draft.unit,
                    if (draft.itemName.isNullOrBlank()) null else listOf(
                        com.example.momentra.data.api.OperatingLineBody(
                            draft.itemName,
                            draft.quantity,
                            draft.unit,
                            draft.unitPrice,
                        ),
                    ),
                    draft.masterKind?.let {
                        com.example.momentra.data.api.OperatingMasterBody(it, draft.title, draft.unit)
                    },
                    draft.revisesEventId,
                    draft.dueAt,
                    draft.contract,
                ).fold(
                    onSuccess = { tick += 1 },
                    onFailure = { error = it.message },
                )
            },
            onDecide = { eventId, decision ->
                repo.decideOperatingEvent(momentId, eventId, decision).fold(
                    onSuccess = { tick += 1 },
                    onFailure = { error = it.message },
                )
            },
            modifier = modifier,
        )
        return
    }
    if (model != null && model.subtype == "TRADING_WHOLESALE") {
        TradingActive(
            model = model,
            tab = tab,
            onOpenQuickAdd = onOpenQuickAdd,
            onRecord = { draft ->
                repo.recordOperatingEvent(
                    momentId,
                    draft.eventType,
                    draft.title,
                    draft.amount,
                    draft.note,
                    draft.channelName,
                    draft.counterpartyName,
                    draft.paymentStatus,
                    draft.occurredAt,
                    draft.parentEventId,
                    draft.quantity,
                    draft.unit,
                    if (draft.itemName.isNullOrBlank()) null else listOf(
                        com.example.momentra.data.api.OperatingLineBody(
                            draft.itemName,
                            draft.quantity,
                            draft.unit,
                            draft.unitPrice,
                            masterId = draft.masterId,
                        ),
                    ),
                    draft.masterKind?.let {
                        com.example.momentra.data.api.OperatingMasterBody(it, draft.title, draft.unit)
                    },
                    draft.revisesEventId,
                    draft.dueAt,
                    draft.contract,
                ).fold(
                    onSuccess = { tick += 1 },
                    onFailure = { error = it.message },
                )
            },
            onDecide = { eventId, decision ->
                repo.decideOperatingEvent(momentId, eventId, decision).fold(
                    onSuccess = { tick += 1 },
                    onFailure = { error = it.message },
                )
            },
            modifier = modifier,
        )
        return
    }
    if (model != null && model.subtype == "DISTRIBUTION_DEALERSHIP") {
        DistributionActive(
            model = model,
            tab = tab,
            onOpenQuickAdd = onOpenQuickAdd,
            onRecord = { draft ->
                repo.recordOperatingEvent(
                    momentId,
                    draft.eventType,
                    draft.title,
                    draft.amount,
                    draft.note,
                    draft.channelName,
                    draft.counterpartyName,
                    draft.paymentStatus,
                    draft.occurredAt,
                    draft.parentEventId,
                    draft.quantity,
                    draft.unit,
                    if (draft.itemName.isNullOrBlank()) null else listOf(
                        com.example.momentra.data.api.OperatingLineBody(
                            draft.itemName,
                            draft.quantity,
                            draft.unit,
                            draft.unitPrice,
                            masterId = draft.masterId,
                        ),
                    ),
                    draft.masterKind?.let {
                        com.example.momentra.data.api.OperatingMasterBody(it, draft.title, draft.unit)
                    },
                    draft.revisesEventId,
                    draft.dueAt,
                    draft.contract,
                ).fold(
                    onSuccess = { tick += 1 },
                    onFailure = { error = it.message },
                )
            },
            onDecide = { eventId, decision ->
                repo.decideOperatingEvent(momentId, eventId, decision).fold(
                    onSuccess = { tick += 1 },
                    onFailure = { error = it.message },
                )
            },
            modifier = modifier,
        )
        return
    }
    if (model != null && model.subtype == "CONSUMER_SERVICE") {
        ConsumerServiceActive(
            model = model,
            tab = tab,
            onOpenQuickAdd = onOpenQuickAdd,
            onRecord = { draft ->
                repo.recordOperatingEvent(
                    momentId,
                    draft.eventType,
                    draft.title,
                    draft.amount,
                    draft.note,
                    draft.channelName,
                    draft.counterpartyName,
                    draft.paymentStatus,
                    draft.occurredAt,
                    draft.parentEventId,
                    draft.quantity,
                    draft.unit,
                    if (draft.itemName.isNullOrBlank()) null else listOf(
                        com.example.momentra.data.api.OperatingLineBody(
                            draft.itemName,
                            draft.quantity,
                            draft.unit,
                            draft.unitPrice,
                        ),
                    ),
                    draft.masterKind?.let {
                        com.example.momentra.data.api.OperatingMasterBody(it, draft.title, draft.unit)
                    },
                    draft.revisesEventId,
                    draft.dueAt,
                    draft.contract,
                ).fold(
                    onSuccess = { tick += 1 },
                    onFailure = { error = it.message },
                )
            },
            onDecide = { eventId, decision ->
                repo.decideOperatingEvent(momentId, eventId, decision).fold(
                    onSuccess = { tick += 1 },
                    onFailure = { error = it.message },
                )
            },
            modifier = modifier,
        )
        return
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Bg)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(familyKicker(model?.familyCode), color = familyAccent(model?.familyCode), fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Text(model?.subtypeLabel ?: momentTitle ?: "Business moment", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 22.sp)
        Text(model?.partyName.orEmpty(), color = Muted, fontSize = 13.sp)
        if (model != null) {
            Text(model.role?.replace('_', ' ') ?: "", color = familyAccent(model.familyCode), fontSize = 12.sp)
        }
        if (error != null && model == null) {
            Text(error ?: "", color = Color(0xFFF87171))
        }
        when (tab) {
            OperatingTab.PULSE -> PulseBlocks(model)
            OperatingTab.MOMENTS -> BoardSections(model?.boards?.moments, momentsHeading(model?.familyCode))
            OperatingTab.LIFE -> BoardSections(model?.boards?.life, "How this moment connects")
            OperatingTab.MEMORY -> BoardSections(model?.boards?.memory, "What stays")
            OperatingTab.QUICK_ADD -> OperatingQuickAddForm(
                model = model,
                onSubmit = { code, title, amount ->
                    repo.recordOperatingEvent(momentId, code, title, amount, null).fold(
                        onSuccess = { tick += 1 },
                        onFailure = { error = it.message },
                    )
                },
                onDecide = { eventId, decision ->
                    repo.decideOperatingEvent(momentId, eventId, decision).fold(
                        onSuccess = { tick += 1 },
                        onFailure = { error = it.message },
                    )
                },
            )
        }
    }
}

@Composable
fun RetiredBusinessMoment(
    title: String?,
    onCreateMoment: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Bg)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title ?: "Business moment", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 22.sp)
        Text(
            "Team & Work, Money & Cash Flow, and Daily Business are retired. Create a Marketing, Trading, Retail, or Manufacturing moment.",
            color = Muted,
            fontSize = 14.sp,
        )
        Text(
            "Create a moment",
            color = Color(0xFF0C0F15),
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF818CF8))
                .clickable(onClick = onCreateMoment)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

private fun familyAccent(family: String?): Color = when (family) {
    "TRADING_DISTRIBUTION" -> Color(0xFFFBBF24)
    "MARKETING_CAMPAIGNS" -> Color(0xFF818CF8)
    "MANUFACTURING_PRODUCTION" -> Color(0xFF38BDF8)
    else -> Color(0xFF34D399)
}

private fun familyKicker(family: String?): String = when (family) {
    "TRADING_DISTRIBUTION" -> "TRADING & DISTRIBUTION"
    "MARKETING_CAMPAIGNS" -> "MARKETING & CAMPAIGNS"
    "MANUFACTURING_PRODUCTION" -> "MANUFACTURING & PRODUCTION"
    else -> "RETAIL & CONSUMER"
}

private fun momentsHeading(family: String?): String = when (family) {
    "TRADING_DISTRIBUTION" -> "Sales, purchases, and movement"
    "MARKETING_CAMPAIGNS" -> "Spend, results, and delivery"
    "MANUFACTURING_PRODUCTION" -> "Production, orders, and delivery"
    else -> "Sales, stock, and service"
}

@Composable
private fun BoardSections(sections: List<OperatingBoardSectionDto>?, heading: String) {
    Text(heading, color = TextPrimary, fontWeight = FontWeight.Medium)
    val blocks = sections.orEmpty()
    if (blocks.isEmpty()) InfoCard("This moment", "Nothing recorded yet.")
    blocks.forEach { section ->
        InfoCard(section.title, section.detail.ifBlank { "Nothing recorded yet." })
        section.rows.forEach { row ->
            val amount = row.amount?.let { " · $it" }.orEmpty()
            InfoCard(row.eventType.replace('_', ' '), row.title + amount)
        }
    }
}

@Composable
private fun PulseBlocks(model: OperatingProjectionDto?) {
    val budget = model?.budget
    if (model?.financeVisible == true && budget != null) {
        InfoCard("Budget", "${budget.remaining} left of ${budget.amount} ${model.currency.orEmpty()}")
        val target = model.target
        if (target != null) {
            InfoCard("Target", "${target.progress} of ${target.value} ${target.metric}")
        }
        model.paymentReadiness.forEach { item ->
            InfoCard("Payment Readiness", "${item.title} · ${item.amount} · ${item.status}")
        }
    } else {
        InfoCard("Your view", "Finance totals stay with Finance, Approver, and Owner.")
    }
    model?.attention?.forEach { InfoCard(it.kind, it.title) }
    if (model?.attention.isNullOrEmpty()) {
        InfoCard("Needs Attention", "Nothing is waiting.")
    }
    val recent = model?.activity.orEmpty().take(5)
    if (recent.isNotEmpty()) {
        Text("Activity", color = TextPrimary, fontWeight = FontWeight.Medium)
        recent.forEach { row ->
            val amount = row.amount?.let { " · $it" }.orEmpty()
            InfoCard(row.eventType, row.title + amount)
        }
    }
}

@Composable
private fun OperatingQuickAddForm(
    model: OperatingProjectionDto?,
    onSubmit: suspend (String, String, String?) -> Unit,
    onDecide: suspend (String, String) -> Unit,
) {
    val accent = familyAccent(model?.familyCode)
    val adds = model?.quickAdds.orEmpty()
    var selected by remember(adds) { mutableStateOf(adds.firstOrNull()?.code.orEmpty()) }
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    Text("Quick Add", color = TextPrimary, fontWeight = FontWeight.Medium)
    if (adds.isEmpty()) {
        Text("Your role cannot add to this moment.", color = Muted, fontSize = 13.sp)
    }
    adds.forEach { add ->
        val on = add.code == selected
        Text(
            add.label,
            color = if (on) Color(0xFF0C0F15) else TextPrimary,
            fontSize = 12.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(if (on) accent else Card)
                .clickable { selected = add.code }
                .padding(horizontal = 10.dp, vertical = 8.dp),
        )
    }
    BasicTextField(
        value = title,
        onValueChange = { title = it },
        textStyle = TextStyle(color = TextPrimary),
        cursorBrush = SolidColor(accent),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Card)
            .border(1.dp, Border, RoundedCornerShape(12.dp))
            .padding(12.dp),
    )
    val needsAmount = adds.firstOrNull { it.code == selected }?.needsAmount == true
    if (needsAmount) {
        BasicTextField(
            value = amount,
            onValueChange = { amount = it },
            textStyle = TextStyle(color = TextPrimary),
            cursorBrush = SolidColor(accent),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Card)
                .border(1.dp, Border, RoundedCornerShape(12.dp))
                .padding(12.dp),
        )
    }
    Text(
        "Add",
        color = Color(0xFF0C0F15),
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(accent)
            .clickable(enabled = selected.isNotBlank() && title.isNotBlank()) {
                val code = selected
                val label = title.trim()
                val value = amount.trim().ifBlank { null }
                scope.launch {
                    onSubmit(code, label, value)
                    title = ""
                    amount = ""
                }
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
    )
    model?.attention?.filter { it.kind == "APPROVAL" }?.forEach { item ->
        Text(
            "Approve ${item.title}",
            color = accent,
            modifier = Modifier.clickable {
                scope.launch { onDecide(item.id, "APPROVED") }
            },
        )
    }
}

@Composable
private fun InfoCard(label: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Card)
            .border(1.dp, Border, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, color = Muted, fontSize = 11.sp)
        Text(body, color = TextPrimary, fontSize = 14.sp)
    }
}

@Composable
internal fun OperatingChromeSheets(model: OperatingProjectionDto) {
    val editing = OperatingChromeState.editing
    val settling = OperatingChromeState.settling
    val record = OperatingChromeState.record
    if (editing != null && record != null) {
        OperatingCaptureSheet(
            title = "Edit activity",
            icon = com.example.momentra.R.drawable.qa_shield,
            eventType = editing.eventType,
            mode = editing.eventType,
            model = model,
            seed = editing,
            onDismiss = { OperatingChromeState.editing = null },
            onSubmit = { draft ->
                record(draft)
                OperatingChromeState.editing = null
            },
        )
    }
    if (settling != null && record != null) {
        val collect = settling.relevance == "RECEIVABLE"
        OperatingCaptureSheet(
            title = if (collect) "Record Collection" else "Record Payment",
            icon = com.example.momentra.R.drawable.qa_shield,
            eventType = if (collect) "COLLECTION" else "PAYMENT",
            mode = if (collect) "COLLECTION" else "PAYMENT",
            model = model,
            seed = com.example.momentra.data.api.OperatingActivityDto(
                id = "",
                title = settling.title,
                eventType = if (collect) "COLLECTION" else "PAYMENT",
                amount = settling.amount,
                counterpartyName = settling.counterpartyName,
                parentEventId = settling.id,
            ),
            onDismiss = { OperatingChromeState.settling = null },
            onSubmit = { draft ->
                record(draft)
                OperatingChromeState.settling = null
            },
        )
    }
}
