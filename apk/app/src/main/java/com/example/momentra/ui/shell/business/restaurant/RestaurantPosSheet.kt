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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.OperatingActivityDto
import com.example.momentra.data.api.OperatingContractBody
import com.example.momentra.data.api.OperatingLineBody
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.theme.RestaurantEmber
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * POS bill — Figma `RST-QA-01-ALT · POS · In-dining` `2446:29655`, `· Cloud` `2446:29802`,
 * `· Takeaway` `2448:29655`.
 *
 * Posts a real `SALE` with `entryMode = ITEMIZED` and `lines[]`. Mode chips set
 * `channelName`. Table / covers persist on `contract.tableNumber` / `contract.covers`.
 * Optional open KOT links via `parentEventId`. GST / Service / Split / PDF reprint stay
 * Unavailable — no client tax engine, split tender model, or document endpoint.
 */
@Composable
internal fun RestaurantPosSheet(
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onSubmit: (RestaurantEventDraft) -> Unit,
) {
    val currency = model.currency?.takeIf { it.isNotBlank() } ?: "INR"
    val menuDishes = remember(model.masters) {
        model.masters.orEmpty()
            .filter { it.kind.uppercase() == "MENU_DISH" }
            .sortedBy { it.name.lowercase() }
    }
    val channels = remember(model) {
        model.life?.channels.orEmpty().filter { it.isNotBlank() }
            .ifEmpty { listOf("In-dining", "Cloud", "Takeaway") }
    }
    val openKots = remember(model.activity) {
        openOrders(model)
            .filter { it.status?.uppercase() !in setOf("COMPLETED", "CANCELLED") }
            .sortedByDescending { it.occurredAt }
    }

    var mode by remember { mutableStateOf(channels.first()) }
    var tender by remember { mutableStateOf("UPI") }
    var dishQuery by remember { mutableStateOf("") }
    var guest by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var tableNumber by remember { mutableStateOf("") }
    var covers by remember { mutableStateOf("") }
    var parentKotId by remember { mutableStateOf<String?>(null) }
    val cart = remember { mutableStateListOf<PosLine>() }

    val linkedKot = openKots.firstOrNull { it.id == parentKotId }
    val subtotal = cart.fold(BigDecimal.ZERO) { total, line -> total + line.amount() }
    val subtotalText = RestaurantFormat.exact(currency, subtotal.stripTrailingZeros().toPlainString())
    val pricedLines = cart.filter { it.unitPrice.toBigDecimalOrNull()?.signum() == 1 }
    val postable = cart.isNotEmpty() && pricedLines.size == cart.size

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = RestaurantEmber.gutter, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        EmberSheetGrabber()
        EmberSheetHeader(
            title = "POS Bill",
            subtitle = listOfNotNull(
                model.title?.takeIf { it.isNotBlank() },
                "${cart.size} line${if (cart.size == 1) "" else "s"}",
                linkedKot?.let { "KOT linked" },
            ).joinToString(" · "),
            onClose = onDismiss,
        )

        EmberFieldLabel("Mode")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            channels.take(3).forEach { channel ->
                EmberModeChip(
                    text = channel,
                    selected = channel == mode,
                    onClick = { mode = channel },
                )
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                EmberFieldLabel("Table")
                EmberTextField(
                    value = tableNumber,
                    placeholder = "12",
                    onValueChange = { tableNumber = it.take(40) },
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                EmberFieldLabel("Covers")
                EmberTextField(
                    value = covers,
                    placeholder = "4",
                    onValueChange = { covers = it.filter { ch -> ch.isDigit() || ch == '.' }.take(8) },
                )
            }
        }

        if (openKots.isNotEmpty()) {
            EmberFieldLabel("Bill from KOT")
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                openKots.take(4).forEach { ticket ->
                    val selected = ticket.id == parentKotId
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (selected) RestaurantEmber.ember.copy(alpha = 0.16f)
                                else RestaurantEmber.surfaceWell,
                            )
                            .border(
                                1.dp,
                                if (selected) RestaurantEmber.ember else Color.White.copy(alpha = 0.06f),
                                RoundedCornerShape(10.dp),
                            )
                            .clickable {
                                if (selected) {
                                    parentKotId = null
                                } else {
                                    parentKotId = ticket.id
                                    applyKotContext(
                                        ticket = ticket,
                                        onMode = { if (channels.contains(it)) mode = it },
                                        onTable = { tableNumber = it },
                                        onCovers = { covers = it },
                                        onGuest = { if (guest.isBlank()) guest = it },
                                    )
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                ticket.title.ifBlank { "Kitchen ticket" },
                                style = RestaurantEmber.rowTitle(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                kotMetaLine(ticket),
                                style = RestaurantEmber.rowCaption().copy(fontSize = 10.sp),
                                maxLines = 1,
                            )
                        }
                        EmberSeverityChip(
                            label = restaurantOrderStatusLabel(ticket.status),
                            color = RestaurantEmber.ember,
                        )
                    }
                }
            }
        }

        EmberFieldLabel("Add from menu")
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f)) {
                EmberTextField(
                    value = dishQuery,
                    placeholder = if (menuDishes.isEmpty()) "No saved dish — add one under Menu" else "Search menu",
                    onValueChange = { dishQuery = it },
                )
            }
            Text(
                "Add",
                style = RestaurantEmber.chipLabel(true).copy(
                    color = if (menuDishes.isEmpty()) RestaurantEmber.textFaint else Color.White,
                ),
                maxLines = 1,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (menuDishes.isEmpty()) RestaurantEmber.chip else RestaurantEmber.ember)
                    .clickable(enabled = menuDishes.isNotEmpty()) {
                        val typed = dishQuery.trim()
                        val match = menuDishes.firstOrNull { it.name.equals(typed, true) }
                            ?: menuDishes.firstOrNull { typed.isNotEmpty() && it.name.contains(typed, true) }
                            ?: menuDishes.first()
                        if (cart.none { it.masterId == match.id }) cart += PosLine.of(match)
                        dishQuery = ""
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            )
        }

        if (menuDishes.isEmpty()) {
            EmberEmpty(
                "Menu not set up",
                "Add dishes under Menu first. A bill line needs a real dish name to post.",
            )
        } else if (dishQuery.isNotBlank()) {
            val matches = menuDishes
                .filter { it.name.contains(dishQuery.trim(), ignoreCase = true) }
                .take(4)
            if (matches.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    matches.forEach { dish ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(RestaurantEmber.surfaceWell)
                                .clickable {
                                    if (cart.none { it.masterId == dish.id }) cart += PosLine.of(dish)
                                    dishQuery = ""
                                }
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                dish.name,
                                style = RestaurantEmber.rowTitle(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            dish.sellPrice?.takeIf { it.isNotBlank() }?.let {
                                Text(
                                    RestaurantFormat.exact(currency, it),
                                    style = RestaurantEmber.rowCaption(RestaurantEmber.gold).copy(fontSize = 10.sp),
                                )
                            }
                            dish.gstRate?.takeIf { it.isNotBlank() }?.let {
                                EmberSeverityChip("GST $it%", RestaurantEmber.gold, bordered = false)
                            }
                        }
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Table ticket", style = RestaurantEmber.rowValue(RestaurantEmber.gold), maxLines = 1)
            Spacer(Modifier.weight(1f))
            Text(
                if (cart.isEmpty()) "No items yet" else "${cart.size} of 12 lines",
                style = RestaurantEmber.rowCaption(),
                maxLines = 1,
            )
        }

        if (cart.isEmpty()) {
            EmberEmpty(
                "Nothing on the bill",
                "Pick a saved dish above, then confirm its price. Every line posts as an itemized sale line.",
            )
        } else {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(RestaurantEmber.chip)
                    .padding(horizontal = 12.dp),
            ) {
                cart.forEachIndexed { index, line ->
                    PosLineRow(
                        line = line,
                        currency = currency,
                        onPrice = { cart[index] = line.copy(unitPrice = it) },
                        onDecrement = {
                            if (line.quantity <= 1) cart.removeAt(index)
                            else cart[index] = line.copy(quantity = line.quantity - 1)
                        },
                        onIncrement = {
                            if (line.quantity < 99) cart[index] = line.copy(quantity = line.quantity + 1)
                        },
                        onRemove = { cart.removeAt(index) },
                    )
                }
            }
            if (!postable) {
                Text(
                    "Every line needs a price above ₹0 before the bill can post.",
                    style = RestaurantEmber.rowCaption(RestaurantEmber.warning).copy(fontSize = 10.sp),
                )
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.24f))
                .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            EmberTotalRow("Subtotal", if (cart.isEmpty()) "—" else subtotalText)
            EmberTotalRow(
                "GST",
                "Unavailable",
                RestaurantEmber.warning,
                caption = "Stamped server-side from each dish's rate",
            )
            EmberTotalRow(
                "Service",
                "Unavailable",
                RestaurantEmber.warning,
                caption = "No service-charge field on the contract",
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(RestaurantEmber.divider),
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text("Payable now", style = RestaurantEmber.rowCaption().copy(fontSize = 10.sp))
                    Text(
                        if (cart.isEmpty()) "—" else subtotalText,
                        style = RestaurantEmber.metricValue().copy(fontSize = 26.sp),
                        maxLines = 1,
                    )
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text("Tender", style = RestaurantEmber.rowCaption().copy(fontSize = 10.sp))
                    Text(tender, style = RestaurantEmber.rowValue(RestaurantEmber.gold), maxLines = 1)
                }
            }
        }

        EmberFieldLabel("Tender")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("UPI", "Cash", "Card").forEach { option ->
                EmberModeChip(text = option, selected = option == tender, onClick = { tender = option })
            }
            LockedChip("Split")
        }
        Text(
            "One bill carries one payment state — a split tender has nowhere to land.",
            style = RestaurantEmber.rowCaption(RestaurantEmber.textFaint).copy(fontSize = 10.sp),
        )

        EmberFieldLabel("Guest")
        EmberTextField(
            value = guest,
            placeholder = "Walk-in or customer name",
            onValueChange = { guest = it },
        )
        EmberFieldLabel("Note")
        EmberTextField(
            value = note,
            placeholder = "Course, kitchen note, reference",
            onValueChange = { note = it },
        )

        EmberWhyNote(
            "In-dining POS posts one bill + one tender. Prep status never posts revenue. " +
                "Recipe stock deduction is excluded.",
            tinted = true,
        )

        EmberSheetFooter(
            label = if (postable) "Post bill · $subtotalText" else "Post bill",
            enabled = postable,
            caveat = "Draft preserved if you close or open a picker",
            onClick = {
                val lines = cart.map {
                    OperatingLineBody(
                        itemName = it.name,
                        quantity = it.quantity.toString(),
                        unit = it.unit,
                        unitPrice = it.unitPrice,
                        masterId = it.masterId,
                    )
                }
                val table = tableNumber.trim()
                val coverCount = covers.trim()
                val title = when {
                    guest.isNotBlank() -> "Bill · ${guest.trim()}"
                    table.isNotBlank() -> "Bill · Table $table"
                    else -> "Bill · ${cart.size} item${if (cart.size == 1) "" else "s"}"
                }
                val noteParts = listOfNotNull(
                    "Tender: $tender",
                    note.takeIf { it.isNotBlank() },
                )
                onSubmit(
                    RestaurantEventDraft(
                        eventType = "SALE",
                        title = title,
                        amount = subtotal.stripTrailingZeros().toPlainString(),
                        note = noteParts.joinToString(" · "),
                        channelName = mode,
                        counterpartyName = guest.takeIf { it.isNotBlank() },
                        paymentStatus = "COLLECTED",
                        parentEventId = parentKotId,
                        quantity = coverCount.takeIf { it.isNotBlank() },
                        contract = OperatingContractBody(
                            entryMode = "ITEMIZED",
                            paymentState = "RECEIVED",
                            receivedAmount = subtotal.stripTrailingZeros().toPlainString(),
                            taxMode = "NOT_CONFIGURED",
                            tableNumber = table.takeIf { it.isNotBlank() },
                            covers = coverCount.takeIf { it.isNotBlank() },
                        ),
                        lines = lines,
                    ),
                )
            },
        )
        Spacer(Modifier.height(16.dp))
    }
}

private fun applyKotContext(
    ticket: OperatingActivityDto,
    onMode: (String) -> Unit,
    onTable: (String) -> Unit,
    onCovers: (String) -> Unit,
    onGuest: (String) -> Unit,
) {
    ticket.channelName?.takeIf { it.isNotBlank() }?.let(onMode)
    ticket.tableNumber?.takeIf { it.isNotBlank() }?.let(onTable)
    (ticket.covers ?: ticket.quantity)?.takeIf { it.isNotBlank() }?.let(onCovers)
    ticket.counterpartyName?.takeIf { it.isNotBlank() }?.let(onGuest)
}

private fun kotMetaLine(ticket: OperatingActivityDto): String =
    listOfNotNull(
        ticket.tableNumber?.takeIf { it.isNotBlank() }?.let { "Table $it" },
        ticket.covers?.takeIf { it.isNotBlank() }?.let { "$it covers" },
        ticket.channelName?.takeIf { it.isNotBlank() },
        ticket.clientKey?.takeIf { it.isNotBlank() },
        ticket.counterpartyName?.takeIf { it.isNotBlank() },
    ).joinToString(" · ").ifBlank { "Open order" }

@Composable
private fun LockedChip(label: String) {
    Text(
        label,
        style = RestaurantEmber.chipLabel(false).copy(color = RestaurantEmber.textFaint),
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(RestaurantEmber.chip.copy(alpha = 0.5f))
            .border(1.dp, RestaurantEmber.hairline.copy(alpha = 0.5f), RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
private fun PosLineRow(
    line: PosLine,
    currency: String,
    onPrice: (String) -> Unit,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                line.name,
                style = RestaurantEmber.rowValue().copy(fontSize = 13.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(line.unit, style = RestaurantEmber.rowCaption().copy(fontSize = 10.sp), maxLines = 1)
                Box(Modifier.width(78.dp)) {
                    EmberTextField(
                        value = line.unitPrice,
                        placeholder = "Price",
                        onValueChange = { raw ->
                            onPrice(raw.filter { char -> char.isDigit() || char == '.' }.take(9))
                        },
                    )
                }
            }
        }
        StepperButton("−", onDecrement)
        Text(
            line.quantity.toString(),
            style = RestaurantEmber.rowValue().copy(fontSize = 13.sp),
            maxLines = 1,
        )
        StepperButton("+", onIncrement)
        Text(
            RestaurantFormat.exact(currency, line.amount().stripTrailingZeros().toPlainString()),
            style = RestaurantEmber.rowValue().copy(fontSize = 13.sp),
            maxLines = 1,
        )
        Text(
            "✕",
            style = RestaurantEmber.rowCaption(RestaurantEmber.textFaint).copy(fontSize = 11.sp),
            maxLines = 1,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onRemove)
                .padding(4.dp),
        )
    }
}

@Composable
private fun StepperButton(glyph: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(RestaurantEmber.chip)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, style = RestaurantEmber.rowValue(RestaurantEmber.textPrimary).copy(fontSize = 15.sp))
    }
}

private data class PosLine(
    val masterId: String,
    val name: String,
    val unit: String,
    val unitPrice: String,
    val quantity: Int,
) {
    fun amount(): BigDecimal = (unitPrice.toBigDecimalOrNull() ?: BigDecimal.ZERO)
        .multiply(BigDecimal(quantity))
        .setScale(2, RoundingMode.HALF_UP)

    companion object {
        fun of(master: com.example.momentra.data.api.OperatingMasterDto) = PosLine(
            masterId = master.id,
            name = master.name,
            unit = master.unit?.takeIf { it.isNotBlank() } ?: "plate",
            unitPrice = master.sellPrice?.takeIf { it.isNotBlank() }.orEmpty(),
            quantity = 1,
        )
    }
}
