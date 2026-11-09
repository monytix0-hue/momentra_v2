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
 * Honest mapping onto the strict operating contract:
 * - the three mode chips set `channelName`, which `validateSale` requires;
 * - each cart line posts as a real `lines[]` entry (`itemName`, `quantity`, `unit`,
 *   `unitPrice`) and the bill posts `SALE` with `entryMode = ITEMIZED`;
 * - the tender chips collapse to the single `paymentState` one bill carries;
 * - `GST` and `Service` have no client-settable field here, so they print `Unavailable`
 *   with the reason instead of a fabricated `₹0` line;
 * - Table / covers / KOT number have no field on `.strict()`, so they render disabled
 *   with the reason printed rather than pretending to save.
 *
 * `MENU_DISH` masters carry no price column, so the price is typed on the ticket line —
 * the same way the backend derives the bill amount from `lines[]`.
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

    var mode by remember { mutableStateOf(channels.first()) }
    var tender by remember { mutableStateOf("UPI") }
    var dishQuery by remember { mutableStateOf("") }
    var guest by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val cart = remember { mutableStateListOf<PosLine>() }

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
            ).joinToString(" · "),
            onClose = onDismiss,
        )

        // Mode chips → `channelName`. `validateSale` rejects a bill without one.
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

        // Table / covers / KOT — no field on the strict contract.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Table", "Covers", "KOT").forEach { LockedChip(it) }
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
            val addLabel = "Add"
            Text(
                addLabel,
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
                "Pick a saved dish above, then type its price. Every line posts as an itemized sale line.",
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

        // Totals — GST and service have no client-settable field in the strict contract.
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

        EmberLockedField(
            label = "Table · covers",
            value = "Not captured",
            reason = "The strict contract has no table, section or covers field — the bill still posts.",
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
                onSubmit(
                    RestaurantEventDraft(
                        eventType = "SALE",
                        title = if (guest.isNotBlank()) {
                            "Bill · $guest"
                        } else {
                            "Bill · ${cart.size} item${if (cart.size == 1) "" else "s"}"
                        },
                        amount = subtotal.stripTrailingZeros().toPlainString(),
                        note = note.takeIf { it.isNotBlank() },
                        channelName = mode,
                        counterpartyName = guest.takeIf { it.isNotBlank() },
                        paymentStatus = "COLLECTED",
                        contract = OperatingContractBody(
                            entryMode = "ITEMIZED",
                            paymentState = "RECEIVED",
                            receivedAmount = subtotal.stripTrailingZeros().toPlainString(),
                            taxMode = "NOT_CONFIGURED",
                        ),
                        lines = lines,
                    ),
                )
            },
        )
        Spacer(Modifier.height(16.dp))
    }
}

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
        /** Dish masters carry no price, so the ticket line starts blank and is filled in. */
        fun of(master: com.example.momentra.data.api.OperatingMasterDto) = PosLine(
            masterId = master.id,
            name = master.name,
            unit = master.unit?.takeIf { it.isNotBlank() } ?: "plate",
            unitPrice = "",
            quantity = 1,
        )
    }
}
