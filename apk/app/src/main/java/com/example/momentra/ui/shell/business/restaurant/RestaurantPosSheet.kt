package com.example.momentra.ui.shell.business.restaurant

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.OperatingActivityDto
import com.example.momentra.data.api.OperatingContractBody
import com.example.momentra.data.api.OperatingLineBody
import com.example.momentra.data.api.OperatingMasterDto
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.shell.business.shared.PosSettlement
import com.example.momentra.ui.theme.RestaurantEmber
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * POS Bill — Figma `RST-QA-01-ALT · POS · In-dining` `2446:29655`,
 * `· Takeaway` `2448:29655`, `· Cloud` sheet `2446:29811`.
 *
 * Three modes of one sheet family. Posts itemized `SALE`. Cloud Platform AR /
 * Takeaway Pay on pickup open AR as `PAY_LATER`. Discount + split tender ride
 * `contract.discount` / `contract.payments`. Explicit cess ₹ is optional.
 */
private enum class PosMode(val label: String) {
    InDining("In-dining"),
    Cloud("Cloud"),
    Takeaway("Takeaway"),
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RestaurantPosSheet(
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onSubmit: (RestaurantEventDraft) -> Unit,
    /** When opened from KOT “Bill from KOT · POS”, pre-select that ticket. */
    initialParentKotId: String? = null,
    /** In-memory ticket from KOT handoff — seed cart immediately without waiting for refetch. */
    initialKotTicket: OperatingActivityDto? = null,
) {
    val currency = model.currency?.takeIf { it.isNotBlank() } ?: "INR"
    val venue = model.title?.takeIf { it.isNotBlank() } ?: "Spice Route BKC"
    val menuDishes = remember(model.masters) {
        model.masters.orEmpty()
            .filter { it.kind.uppercase() == "MENU_DISH" }
            .sortedBy { it.name.lowercase() }
    }
    val openKots = remember(model.activity, initialKotTicket?.id) {
        val base = openOrders(model)
            .filter { it.status?.uppercase() !in setOf("COMPLETED", "CANCELLED") }
            .sortedByDescending { it.occurredAt }
        val handoff = initialKotTicket ?: return@remember base
        if (base.any { it.id == handoff.id }) base else listOf(handoff) + base
    }

    var mode by remember { mutableStateOf(PosMode.InDining) }
    var tender by remember { mutableStateOf("UPI") }
    var splitTender by remember { mutableStateOf(false) }
    var splitUpi by remember { mutableStateOf("") }
    var splitCash by remember { mutableStateOf("") }
    var splitCard by remember { mutableStateOf("") }
    var discount by remember { mutableStateOf("") }
    var cessAmount by remember { mutableStateOf("") }
    var dishQuery by remember { mutableStateOf("") }
    var guest by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var tableNumber by remember { mutableStateOf("") }
    var covers by remember { mutableStateOf("") }
    var platform by remember { mutableStateOf("Zomato") }
    var orderId by remember { mutableStateOf("") }
    var readyBy by remember { mutableStateOf("") }
    var prepBy by remember { mutableStateOf("") }
    var packaging by remember { mutableStateOf("Standard") }
    var printChoice by remember { mutableStateOf("Thermal + KOT") }
    var moreOpen by remember { mutableStateOf(false) }
    var showKotPicker by remember { mutableStateOf(initialParentKotId != null || initialKotTicket != null) }
    var parentKotId by remember { mutableStateOf(initialParentKotId ?: initialKotTicket?.id) }
    val cart = remember { mutableStateListOf<PosLine>() }

    fun seedFromTicket(ticket: OperatingActivityDto) {
        parentKotId = ticket.id
        showKotPicker = true
        applyKotContext(
            ticket = ticket,
            menuDishes = menuDishes,
            cart = cart,
            onMode = { label ->
                PosMode.entries.firstOrNull { it.label.equals(label, true) }
                    ?.let { mode = it }
            },
            onTable = { tableNumber = it },
            onCovers = { covers = it },
            onGuest = { if (guest.isBlank()) guest = it },
        )
    }

    // Prefer in-memory handoff ticket so Bill-from-KOT lines appear before operating refetch.
    LaunchedEffect(initialKotTicket?.id) {
        val handoff = initialKotTicket ?: return@LaunchedEffect
        seedFromTicket(handoff)
    }

    LaunchedEffect(initialParentKotId, openKots) {
        val handoffId = initialParentKotId ?: return@LaunchedEffect
        if (initialKotTicket?.id == handoffId && cart.isNotEmpty()) return@LaunchedEffect
        val handoff = openKots.firstOrNull { it.id == handoffId } ?: return@LaunchedEffect
        seedFromTicket(handoff)
    }

    val linkedKot = openKots.firstOrNull { it.id == parentKotId }
        ?: initialKotTicket?.takeIf { it.id == parentKotId }
    val subtotal = cart.fold(BigDecimal.ZERO) { total, line -> total + line.amount() }
    val discountBd = PosSettlement.parseAmount(discount)
    val cessBd = PosSettlement.parseAmount(cessAmount)
    val discountOk = discountBd <= subtotal || subtotal.signum() <= 0
    val isPlatformAr = mode == PosMode.Cloud && tender == "Platform AR"
    val isPayOnPickup = mode == PosMode.Takeaway && tender == "Pay on pickup"
    val openAr = isPlatformAr || isPayOnPickup
    val payable = PosSettlement.payable(subtotal, discountBd, cessBd)
    val splitAmounts = mapOf("UPI" to splitUpi, "Cash" to splitCash, "Card" to splitCard)
    val paymentsPreview = PosSettlement.buildPayments(
        split = splitTender && !openAr,
        primary = tender,
        payable = payable,
        splitAmounts = splitAmounts,
        openAr = openAr,
    )
    val splitOk = openAr || !splitTender || PosSettlement.splitSumsToPayable(splitAmounts, payable)
    val subtotalText = RestaurantFormat.exact(currency, subtotal.stripTrailingZeros().toPlainString())
    val payableText = if (payable.signum() <= 0) {
        "—"
    } else {
        RestaurantFormat.exact(currency, payable.stripTrailingZeros().toPlainString())
    }
    val discountText = if (discountBd.signum() <= 0) {
        "—"
    } else {
        "−${RestaurantFormat.exact(currency, discountBd.stripTrailingZeros().toPlainString())}"
    }
    val tenderLabel = when {
        openAr && isPlatformAr -> "Later AR"
        openAr -> tender
        else -> PosSettlement.tenderLabel(splitTender, tender, paymentsPreview)
    }
    val pricedLines = cart.filter { it.unitPrice.toBigDecimalOrNull()?.signum() == 1 }
    val postable =
        cart.isNotEmpty() && pricedLines.size == cart.size && discountOk && splitOk && payable.signum() == 1
    val gstOn = model.gstTracking.equals("YES", true) &&
        model.gstRegistration.equals("REGULAR", true)
    val gstInclusive = !model.salePriceIncludesGst.equals("NO", true)
    val gstEstimate = remember(cart.toList(), gstOn, gstInclusive) {
        if (!gstOn || cart.isEmpty()) null
        else RestaurantFormat.gstEstimate(
            lineAmounts = cart.map { it.amount() },
            lineRates = cart.map { it.gstRate },
            inclusive = gstInclusive,
        )
    }
    val gstRowLabel = when {
        !gstOn -> "GST"
        gstInclusive -> "GST (incl. est.)"
        else -> "GST (excl. est.)"
    }
    val gstRowValue = when {
        cart.isEmpty() -> "—"
        !gstOn -> "Unavailable"
        gstEstimate != null -> RestaurantFormat.exact(
            currency,
            gstEstimate.stripTrailingZeros().toPlainString(),
        )
        else -> "Unavailable"
    }
    val gstRowCaption = when {
        !gstOn -> "GST tracking off or not REGULAR — Charge stays NOT_CONFIGURED"
        gstEstimate != null -> "Estimate from dish rates — stamped on Charge"
        else -> "Unavailable — rate missing on dish / master"
    }

    // Reset tender defaults when mode changes so Cloud lands on Platform AR.
    fun selectMode(next: PosMode) {
        mode = next
        tender = when (next) {
            PosMode.Cloud -> "Platform AR"
            PosMode.Takeaway -> "UPI"
            PosMode.InDining -> "UPI"
        }
        printChoice = when (next) {
            PosMode.Takeaway -> "Thermal · SMS link"
            PosMode.Cloud -> "Platform ticket"
            PosMode.InDining -> "Thermal + KOT"
        }
    }

    val contextLine = when (mode) {
        PosMode.InDining -> "Floor · $venue · Station open"
        PosMode.Cloud -> "Cloud / aggregator · $venue"
        PosMode.Takeaway -> "Counter pickup · $venue · Ready desk"
    }
    val addLabel = when (mode) {
        PosMode.InDining -> "Add from menu / KOT"
        PosMode.Cloud -> "Confirm platform items"
        PosMode.Takeaway -> "Add from menu"
    }
    val addPlaceholder = when (mode) {
        PosMode.InDining -> "Butter chicken · search menu"
        PosMode.Cloud -> if (orderId.isNotBlank()) "$orderId · synced ticket" else "ZOM-9182 · synced ticket"
        PosMode.Takeaway -> "Search takeaway menu"
    }
    val addAction = if (mode == PosMode.Cloud) "Sync" else "Add"
    val ticketTitle = when (mode) {
        PosMode.InDining -> "Table ticket"
        PosMode.Cloud -> "Cloud ticket"
        PosMode.Takeaway -> "Pickup ticket"
    }
    val ticketMeta = buildString {
        append(if (cart.isEmpty()) "No items yet" else "${cart.size} item${if (cart.size == 1) "" else "s"}")
        val key = linkedKot?.clientKey?.takeIf { it.isNotBlank() }
            ?: orderId.takeIf { it.isNotBlank() && mode == PosMode.Cloud }
            ?: linkedKot?.id?.take(8)?.uppercase()?.let { "KOT-$it" }
        if (key != null) append(" · $key")
    }
    val addItemHint = when (mode) {
        PosMode.InDining -> "Courses · Favourites"
        PosMode.Cloud -> "Platform menu map"
        PosMode.Takeaway -> "Combos · Favourites"
    }
    val moreHint = when (mode) {
        PosMode.InDining -> "Notes · Split seat · Hold table"
        PosMode.Cloud -> "Notes · Rider code · Tags"
        PosMode.Takeaway -> "Notes · Allergy · Hold pickup"
    }
    val whyCopy = when (mode) {
        PosMode.InDining ->
            "In-dining POS posts one bill + tender. KOT/prep status never posts revenue. Recipe stock deduction excluded in v5."
        PosMode.Cloud ->
            "Cloud bill posts revenue now; platform settlement is Collection later. Fees must not double-count cost. Prep/ready status ≠ revenue."
        PosMode.Takeaway ->
            "Takeaway posts bill + tender (or Pay on pickup → open AR until collect). Ready status is ops only — never posts revenue by itself."
    }
    val ctaLabel = when {
        !postable -> when (mode) {
            PosMode.Cloud -> "Post cloud bill"
            PosMode.Takeaway -> "Charge · Post & notify"
            PosMode.InDining -> "Charge · Post & print"
        }
        mode == PosMode.Cloud -> "Post cloud bill · $payableText"
        mode == PosMode.Takeaway -> "Charge $payableText · Post & notify"
        else -> "Charge $payableText · Post & print"
    }
    val payableLabel = if (mode == PosMode.Cloud) "Bill value" else "Payable now"
    val collectLabel = if (mode == PosMode.Cloud) "Collect" else "Tender"
    val collectValue = tenderLabel
    val collectColor = when {
        mode == PosMode.Cloud && tender == "Platform AR" -> RestaurantEmber.amber
        else -> RestaurantEmber.positive
    }
    val thirdTotalLabel = when (mode) {
        PosMode.InDining -> "Service"
        PosMode.Cloud -> "Platform fee (info)"
        PosMode.Takeaway -> "Packaging"
    }
    val thirdTotalValue = when (mode) {
        PosMode.Cloud -> "₹0 on bill"
        else -> "Unavailable"
    }
    val firstTotalLabel = if (mode == PosMode.Cloud) "Menu total" else "Subtotal"

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = RestaurantEmber.gutter, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        EmberSheetGrabber()
        EmberSheetHeader(title = "POS Bill", subtitle = contextLine, onClose = onDismiss)

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            PosMode.entries.forEach { option ->
                EmberModeChip(
                    text = option.label,
                    selected = option == mode,
                    onClick = { selectMode(option) },
                )
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            when (mode) {
                PosMode.InDining -> {
                    EmberModeChip(
                        text = if (tableNumber.isNotBlank()) "Table $tableNumber" else "Table",
                        selected = tableNumber.isNotBlank(),
                        onClick = { /* focus via field below */ },
                    )
                    EmberModeChip(
                        text = if (covers.isNotBlank()) "$covers covers" else "Covers",
                        selected = false,
                        onClick = null,
                    )
                    EmberModeChip(
                        text = "KOT live",
                        selected = parentKotId != null,
                        onClick = { showKotPicker = !showKotPicker },
                    )
                }
                PosMode.Cloud -> {
                    EmberModeChip(text = platform, selected = true, onClick = null)
                    EmberModeChip(text = "SLA 28m", selected = false, onClick = null)
                    EmberModeChip(text = "Prep", selected = false, onClick = null)
                    EmberModeChip(
                        text = "KOT live",
                        selected = parentKotId != null,
                        onClick = { showKotPicker = !showKotPicker },
                    )
                }
                PosMode.Takeaway -> {
                    EmberModeChip(text = "Pickup", selected = true, onClick = null)
                    EmberModeChip(
                        text = if (readyBy.isNotBlank()) "Ready $readyBy" else "Ready 15m",
                        selected = false,
                        onClick = null,
                    )
                    EmberModeChip(
                        text = if (tender == "Pay on pickup") "Pay later" else "Prepaid",
                        selected = tender != "Pay on pickup",
                        onClick = null,
                    )
                    EmberModeChip(
                        text = "KOT live",
                        selected = parentKotId != null,
                        onClick = { showKotPicker = !showKotPicker },
                    )
                }
            }
        }

        if (showKotPicker && openKots.isNotEmpty()) {
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
                                        menuDishes = menuDishes,
                                        cart = cart,
                                        onMode = { label ->
                                            PosMode.entries.firstOrNull { it.label.equals(label, true) }
                                                ?.let { selectMode(it) }
                                        },
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

        EmberFieldLabel(addLabel)
        PosScanField(
            query = dishQuery,
            placeholder = addPlaceholder,
            action = addAction,
            enabled = menuDishes.isNotEmpty() || mode == PosMode.Cloud,
            onQueryChange = { dishQuery = it },
            onAction = {
                if (mode == PosMode.Cloud && dishQuery.isNotBlank() && orderId.isBlank()) {
                    orderId = dishQuery.trim()
                    dishQuery = ""
                    return@PosScanField
                }
                if (menuDishes.isEmpty()) return@PosScanField
                val typed = dishQuery.trim()
                val match = menuDishes.firstOrNull { it.name.equals(typed, true) }
                    ?: menuDishes.firstOrNull { typed.isNotEmpty() && it.name.contains(typed, true) }
                    ?: menuDishes.firstOrNull()
                    ?: return@PosScanField
                addOrBumpDish(cart, match)
                dishQuery = ""
            },
        )

        if (menuDishes.isEmpty()) {
            EmberEmpty(
                "Menu not set up",
                "Add dishes under Menu first. A bill line needs a real dish name to post.",
            )
        } else if (dishQuery.isNotBlank() && mode != PosMode.Cloud) {
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
                                    addOrBumpDish(cart, dish)
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
                        }
                    }
                }
            }
        }

        // Cart / ticket card
        Column(
            Modifier
                .fillMaxWidth()
                .shadow(
                    10.dp,
                    RestaurantEmber.rSheetCta,
                    ambientColor = RestaurantEmber.gold.copy(alpha = 0.15f),
                    spotColor = RestaurantEmber.ember.copy(alpha = 0.25f),
                )
                .clip(RestaurantEmber.rSheetCta)
                .background(RestaurantEmber.chip)
                .border(1.dp, RestaurantEmber.ember.copy(alpha = 0.3f), RestaurantEmber.rSheetCta)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(ticketTitle, style = RestaurantEmber.rowValue(RestaurantEmber.gold).copy(fontSize = 12.sp))
                Spacer(Modifier.weight(1f))
                Text(ticketMeta, style = RestaurantEmber.rowCaption().copy(fontSize = 11.sp), maxLines = 1)
            }
            if (cart.isEmpty()) {
                Text(
                    "Nothing on the bill yet — add a saved dish above.",
                    style = RestaurantEmber.rowCaption(RestaurantEmber.textFaint).copy(fontSize = 11.sp),
                    modifier = Modifier.padding(vertical = 10.dp),
                )
            } else {
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
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .clickable(enabled = menuDishes.isNotEmpty()) {
                        menuDishes.firstOrNull()?.let { dish -> addOrBumpDish(cart, dish) }
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("+ Add item", style = RestaurantEmber.rowValue(RestaurantEmber.gold).copy(fontSize = 13.sp))
                Spacer(Modifier.weight(1f))
                Text(addItemHint, style = RestaurantEmber.rowCaption().copy(fontSize = 11.sp))
            }
        }

        if (cart.isNotEmpty() && !postable) {
            Text(
                "Every line needs a price above ₹0 before the bill can post.",
                style = RestaurantEmber.rowCaption(RestaurantEmber.warning).copy(fontSize = 10.sp),
            )
        }

        // Totals card
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RestaurantEmber.rSheetCta)
                .background(RestaurantEmber.chip)
                .border(1.5.dp, RestaurantEmber.ember, RestaurantEmber.rSheetCta)
                .shadow(10.dp, RestaurantEmber.rSheetCta, ambientColor = RestaurantEmber.ember.copy(alpha = 0.4f))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            EmberTotalRow(
                firstTotalLabel,
                if (cart.isEmpty()) "—" else subtotalText,
                RestaurantEmber.textBody,
            )
            if (discountBd.signum() > 0) {
                EmberTotalRow("Discount", discountText, RestaurantEmber.positive)
            }
            if (cessBd.signum() > 0) {
                EmberTotalRow(
                    "Cess",
                    RestaurantFormat.exact(currency, cessBd.stripTrailingZeros().toPlainString()),
                    RestaurantEmber.textBody,
                )
            }
            EmberTotalRow(
                gstRowLabel,
                gstRowValue,
                if (gstEstimate != null) RestaurantEmber.amber else RestaurantEmber.warning,
                caption = gstRowCaption,
            )
            EmberTotalRow(
                thirdTotalLabel,
                thirdTotalValue,
                RestaurantEmber.warning,
                caption = when (mode) {
                    PosMode.InDining -> "No service-charge field on the contract"
                    PosMode.Cloud -> "Fees settle via Collection — never on this bill"
                    PosMode.Takeaway -> "No packaging field on the contract"
                },
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(RestaurantEmber.hairline),
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(payableLabel, style = RestaurantEmber.rowCaption().copy(fontSize = 11.sp))
                    Text(
                        if (cart.isEmpty()) "—" else payableText,
                        style = RestaurantEmber.metricValue().copy(fontSize = 28.sp),
                        maxLines = 1,
                    )
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        collectLabel,
                        style = RestaurantEmber.rowValue(RestaurantEmber.gold).copy(fontSize = 10.sp),
                    )
                    Text(
                        collectValue,
                        style = RestaurantEmber.rowValue(collectColor).copy(fontSize = 13.sp),
                        maxLines = 1,
                    )
                }
            }
        }

        EmberFieldLabel("Discount")
        EmberTextField(
            value = discount,
            placeholder = "0",
            onValueChange = { discount = it.filter { ch -> ch.isDigit() || ch == '.' }.take(12) },
        )

        EmberFieldLabel("Tender")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            when (mode) {
                PosMode.InDining -> {
                    listOf("UPI", "Cash", "Card").forEach { option ->
                        EmberModeChip(
                            text = option,
                            selected = !splitTender && option == tender,
                            selectedFill = RestaurantEmber.positive,
                            onClick = {
                                splitTender = false
                                tender = option
                            },
                        )
                    }
                    EmberModeChip(
                        text = "Split",
                        selected = splitTender,
                        selectedFill = RestaurantEmber.positive,
                        onClick = { splitTender = true },
                    )
                }
                PosMode.Takeaway -> {
                    listOf("UPI", "Cash", "Card", "Pay on pickup").forEach { option ->
                        EmberModeChip(
                            text = option,
                            selected = !splitTender && option == tender,
                            selectedFill = RestaurantEmber.positive,
                            onClick = {
                                splitTender = false
                                tender = option
                            },
                        )
                    }
                    if (tender != "Pay on pickup") {
                        EmberModeChip(
                            text = "Split",
                            selected = splitTender,
                            selectedFill = RestaurantEmber.positive,
                            onClick = { splitTender = true },
                        )
                    }
                }
                PosMode.Cloud -> {
                    listOf("Platform AR", "Prepaid", "UPI").forEach { option ->
                        EmberModeChip(
                            text = option,
                            selected = option == tender,
                            selectedFill = RestaurantEmber.positive,
                            onClick = {
                                splitTender = false
                                tender = option
                            },
                        )
                    }
                }
            }
        }
        if (splitTender && !openAr) {
            EmberFieldLabel("UPI")
            EmberTextField(
                value = splitUpi,
                placeholder = "0",
                onValueChange = { splitUpi = it.filter { ch -> ch.isDigit() || ch == '.' }.take(12) },
            )
            EmberFieldLabel("Cash")
            EmberTextField(
                value = splitCash,
                placeholder = "0",
                onValueChange = { splitCash = it.filter { ch -> ch.isDigit() || ch == '.' }.take(12) },
            )
            EmberFieldLabel("Card")
            EmberTextField(
                value = splitCard,
                placeholder = "0",
                onValueChange = { splitCard = it.filter { ch -> ch.isDigit() || ch == '.' }.take(12) },
            )
            Text(
                if (splitOk) {
                    "Split instruments post on the contract and must add up to payable."
                } else {
                    "Split amounts must add up to payable before Charge."
                },
                style = RestaurantEmber.rowCaption(
                    if (splitOk) RestaurantEmber.textFaint else RestaurantEmber.warning,
                ).copy(fontSize = 10.sp),
            )
        } else if (mode == PosMode.InDining) {
            Text(
                "One instrument posts as contract.payments · use Split for UPI+Cash+Card.",
                style = RestaurantEmber.rowCaption(RestaurantEmber.textFaint).copy(fontSize = 10.sp),
            )
        }

        when (mode) {
            PosMode.InDining -> {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        EmberFieldLabel("Table")
                        EmberTextField(
                            value = tableNumber,
                            placeholder = "7",
                            onValueChange = { tableNumber = it.take(40) },
                        )
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        EmberFieldLabel("Covers")
                        EmberTextField(
                            value = covers,
                            placeholder = "4",
                            onValueChange = { covers = it.filter { ch -> ch.isDigit() }.take(8) },
                        )
                    }
                }
                EmberChevronField(
                    label = "Bill / print",
                    value = printChoice,
                    placeholder = "Thermal + KOT",
                    onClick = null,
                )
                Text(
                    "Print / PDF endpoint unavailable — charge still posts the bill.",
                    style = RestaurantEmber.rowCaption(RestaurantEmber.textFaint).copy(fontSize = 10.sp),
                )
                EmberChevronField(
                    label = "Guest",
                    value = guest,
                    editable = true,
                    placeholder = "Walk-in",
                    onValueChange = { guest = it },
                )
            }
            PosMode.Takeaway -> {
                EmberChevronField(
                    label = "Guest name",
                    value = guest,
                    editable = true,
                    placeholder = "Priya",
                    onValueChange = { guest = it },
                )
                EmberChevronField(
                    label = "Phone",
                    value = phone,
                    editable = true,
                    placeholder = "+91 …",
                    onValueChange = { phone = it.filter { ch -> ch.isDigit() || ch == '+' || ch == ' ' }.take(18) },
                )
                EmberChevronField(
                    label = "Ready by",
                    value = readyBy,
                    editable = true,
                    placeholder = "12:55 · counter",
                    onValueChange = { readyBy = it.take(40) },
                )
                EmberChevronField(
                    label = "Bill / print",
                    value = printChoice,
                    placeholder = "Thermal · SMS link",
                    onClick = null,
                )
            }
            PosMode.Cloud -> {
                EmberChevronField(
                    label = "Platform",
                    value = platform,
                    editable = true,
                    placeholder = "Zomato",
                    onValueChange = { platform = it.take(40) },
                )
                EmberChevronField(
                    label = "Order id",
                    value = orderId,
                    editable = true,
                    placeholder = "ZOM-9182",
                    onValueChange = { orderId = it.take(40) },
                )
                EmberChevronField(
                    label = "Prep by",
                    value = prepBy,
                    editable = true,
                    placeholder = "12:48",
                    onValueChange = { prepBy = it.take(40) },
                )
                EmberChevronField(
                    label = "Packaging",
                    value = packaging,
                    editable = true,
                    placeholder = "Standard",
                    onValueChange = { packaging = it.take(40) },
                )
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, RestaurantEmber.sheetBorder, RoundedCornerShape(12.dp))
                .clickable { moreOpen = !moreOpen }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("More details", style = RestaurantEmber.rowValue(RestaurantEmber.gold).copy(fontSize = 13.sp))
            Spacer(Modifier.weight(1f))
            Text(moreHint, style = RestaurantEmber.rowCaption().copy(fontSize = 11.sp), maxLines = 1)
            Text(
                if (moreOpen) "⌃" else "⌄",
                style = RestaurantEmber.rowValue(RestaurantEmber.gold).copy(fontSize = 14.sp),
            )
        }
        if (moreOpen) {
            EmberFieldLabel("Notes")
            EmberTextField(
                value = note,
                placeholder = when (mode) {
                    PosMode.InDining -> "Course, kitchen note, reference"
                    PosMode.Cloud -> "Rider code, tags"
                    PosMode.Takeaway -> "Allergy, hold pickup"
                },
                onValueChange = { note = it },
            )
            EmberFieldLabel("Cess ₹ (optional · explicit only)")
            EmberTextField(
                value = cessAmount,
                placeholder = "0",
                onValueChange = { cessAmount = it.filter { ch -> ch.isDigit() || ch == '.' }.take(12) },
            )
            Text(
                "Cess is operator-entered only — no HSN/CBIC schedule lookup.",
                style = RestaurantEmber.rowCaption(RestaurantEmber.textFaint).copy(fontSize = 10.sp),
            )
        }

        EmberWhyNote(whyCopy, tinted = true)

        EmberSheetFooter(
            label = ctaLabel,
            enabled = postable,
            caveat = "Draft preserved if you close or open a picker",
            onClick = {
                val taxMode = when {
                    !gstOn -> "NOT_CONFIGURED"
                    model.salePriceIncludesGst.equals("YES", true) -> "INCLUSIVE"
                    model.salePriceIncludesGst.equals("NO", true) -> "EXCLUSIVE"
                    else -> "INCLUSIVE"
                }
                val lines = cart.map {
                    OperatingLineBody(
                        itemName = it.name,
                        quantity = it.quantity.toString(),
                        unit = it.unit,
                        unitPrice = it.unitPrice,
                        masterId = productionMasterId(it.masterId),
                        taxMode = taxMode.takeIf { mode -> mode != "NOT_CONFIGURED" },
                        gstRate = it.gstRate?.takeIf { rate -> rate.isNotBlank() && rate != "Exempt" },
                    )
                }
                val table = tableNumber.trim()
                val coverCount = covers.trim()
                val amountPlain = subtotal.stripTrailingZeros().toPlainString()
                val payablePlain = PosSettlement.money(payable)
                val discountPlain = discountBd.takeIf { it.signum() > 0 }?.let { PosSettlement.money(it) }
                val cessPlain = cessBd.takeIf { it.signum() > 0 }?.let { PosSettlement.money(it) }
                val payments = PosSettlement.buildPayments(
                    split = splitTender && !openAr,
                    primary = tender,
                    payable = payable,
                    splitAmounts = splitAmounts,
                    openAr = openAr,
                )
                val title = when (mode) {
                    PosMode.Cloud -> {
                        val id = orderId.ifBlank { platform }
                        "Cloud bill · $id"
                    }
                    PosMode.Takeaway -> when {
                        guest.isNotBlank() -> "Takeaway · ${guest.trim()}"
                        else -> "Takeaway · ${cart.size} item${if (cart.size == 1) "" else "s"}"
                    }
                    PosMode.InDining -> when {
                        guest.isNotBlank() -> "Bill · ${guest.trim()}"
                        table.isNotBlank() -> "Bill · Table $table"
                        else -> "Bill · ${cart.size} item${if (cart.size == 1) "" else "s"}"
                    }
                }
                val noteParts = listOfNotNull(
                    if (mode == PosMode.Cloud) "Platform: $platform" else null,
                    orderId.takeIf { it.isNotBlank() }?.let { "Order: $it" },
                    phone.takeIf { it.isNotBlank() }?.let { "Phone: $it" },
                    readyBy.takeIf { it.isNotBlank() }?.let { "Ready by: $it" },
                    prepBy.takeIf { it.isNotBlank() }?.let { "Prep by: $it" },
                    packaging.takeIf { it.isNotBlank() && mode == PosMode.Cloud }?.let { "Packaging: $it" },
                    note.takeIf { it.isNotBlank() },
                )
                onSubmit(
                    RestaurantEventDraft(
                        eventType = "SALE",
                        title = title,
                        amount = amountPlain,
                        note = noteParts.joinToString(" · ").ifBlank { null },
                        channelName = mode.label,
                        counterpartyName = when {
                            mode == PosMode.Cloud -> platform.takeIf { it.isNotBlank() }
                            guest.isNotBlank() -> guest.trim()
                            else -> null
                        },
                        paymentStatus = if (openAr) "PENDING" else "COLLECTED",
                        parentEventId = parentKotId,
                        quantity = coverCount.takeIf { it.isNotBlank() && mode == PosMode.InDining },
                        contract = OperatingContractBody(
                            entryMode = "ITEMIZED",
                            paymentState = if (openAr) "PAY_LATER" else "RECEIVED",
                            receivedAmount = if (openAr) null else payablePlain,
                            discount = discountPlain,
                            cessAmount = cessPlain,
                            payments = payments,
                            taxMode = taxMode,
                            placeOfSupply = model.gstState?.takeIf { gstOn && it.isNotBlank() },
                            tableNumber = table.takeIf { it.isNotBlank() && mode == PosMode.InDining },
                            covers = coverCount.takeIf { it.isNotBlank() && mode == PosMode.InDining },
                            clientKey = orderId.takeIf { it.isNotBlank() && mode == PosMode.Cloud },
                            promisedAt = when (mode) {
                                PosMode.Takeaway -> readyBy.takeIf { it.isNotBlank() }
                                PosMode.Cloud -> prepBy.takeIf { it.isNotBlank() }
                                else -> null
                            },
                            locationName = packaging.takeIf { it.isNotBlank() && mode == PosMode.Cloud },
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
    menuDishes: List<OperatingMasterDto>,
    cart: MutableList<PosLine>,
    onMode: (String) -> Unit,
    onTable: (String) -> Unit,
    onCovers: (String) -> Unit,
    onGuest: (String) -> Unit,
) {
    ticket.channelName?.takeIf { it.isNotBlank() }?.let(onMode)
    ticket.tableNumber?.takeIf { it.isNotBlank() }?.let(onTable)
    (ticket.covers ?: ticket.quantity)?.takeIf { it.isNotBlank() }?.let(onCovers)
    ticket.counterpartyName?.takeIf { it.isNotBlank() }?.let(onGuest)
    val seeded = seedPosCartFromKot(ticket, menuDishes)
    if (seeded.isNotEmpty()) {
        cart.clear()
        cart.addAll(seeded)
    }
}

/** Harbor / demo fixture ids must never ride a production Charge. */
private fun productionMasterId(masterId: String?): String? {
    val id = masterId?.takeIf { it.isNotBlank() } ?: return null
    return if (id.startsWith("fx-", ignoreCase = true)) null else id
}

private fun seedPosCartFromKot(
    ticket: OperatingActivityDto,
    menuDishes: List<OperatingMasterDto>,
): List<PosLine> {
    val rows = ticket.lines.orEmpty().filter { it.itemName.isNotBlank() }
    if (rows.isEmpty()) return emptyList()
    val byId = menuDishes.associateBy { it.id }
    val byName = menuDishes.associateBy { it.name.trim().lowercase() }
    return rows.map { row ->
        val master = row.masterId?.let { byId[it] }
            ?: byName[row.itemName.trim().lowercase()]
        val qty = row.quantity?.toIntOrNull()?.coerceIn(1, 99) ?: 1
        // Never drop a KOT line for a missing price — fall back to menu sellPrice, else "0"
        // so the cart hydrates and Charge stays blocked until the cashier prices the row.
        val price = row.unitPrice?.takeIf { it.isNotBlank() }
            ?: master?.sellPrice?.takeIf { it.isNotBlank() }
            ?: "0"
        PosLine(
            masterId = master?.id ?: row.masterId.orEmpty(),
            name = master?.name ?: row.itemName,
            unit = row.unit?.takeIf { it.isNotBlank() } ?: master?.unit ?: "plate",
            unitPrice = price,
            quantity = qty,
            code = master?.code,
            meta = when {
                price.toBigDecimalOrNull()?.signum() == 1 -> master?.let { PosLine.of(it).meta } ?: "KOT"
                master?.sellPrice.isNullOrBlank() && row.unitPrice.isNullOrBlank() -> "Price needed"
                else -> master?.let { PosLine.of(it).meta } ?: "KOT"
            },
            gstRate = master?.gstRate?.takeIf { it.isNotBlank() },
        )
    }.take(12)
}

/** Re-add increments qty (retail parity); new dishes append until the 12-line cap. */
private fun addOrBumpDish(cart: MutableList<PosLine>, dish: OperatingMasterDto) {
    val idx = cart.indexOfFirst { it.masterId == dish.id }
    if (idx >= 0) {
        val line = cart[idx]
        if (line.quantity < 99) cart[idx] = line.copy(quantity = line.quantity + 1)
        return
    }
    if (cart.size < 12) cart += PosLine.of(dish)
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
private fun PosScanField(
    query: String,
    placeholder: String,
    action: String,
    enabled: Boolean,
    onQueryChange: (String) -> Unit,
    onAction: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(RestaurantEmber.chip)
            .border(1.5.dp, RestaurantEmber.ember, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        androidx.compose.foundation.text.BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = RestaurantEmber.textPrimary,
                fontFamily = com.example.momentra.ui.theme.PlusJakartaSans,
                fontSize = 13.sp,
            ),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(RestaurantEmber.ember),
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text(placeholder, style = RestaurantEmber.rowCaption(RestaurantEmber.textFaint).copy(fontSize = 13.sp))
                }
                inner()
            },
            modifier = Modifier.weight(1f),
        )
        Text(
            action,
            style = RestaurantEmber.chipLabel(true).copy(
                fontSize = 11.sp,
                color = if (enabled) RestaurantEmber.inkOnEmber else RestaurantEmber.textFaint,
            ),
            maxLines = 1,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (enabled) RestaurantEmber.ember else RestaurantEmber.hairline)
                .clickable(enabled = enabled, onClick = onAction)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
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
    Column(Modifier.fillMaxWidth()) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            line.code?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    style = RestaurantEmber.rowValue(RestaurantEmber.gold).copy(fontSize = 10.sp),
                    maxLines = 1,
                )
            }
            Text(
                line.name,
                style = RestaurantEmber.rowTitle().copy(fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                line.meta,
                style = RestaurantEmber.rowCaption().copy(fontSize = 11.sp),
                maxLines = 1,
            )
            // Price stays editable for honesty when master has no sell price
            if (line.unitPrice.isBlank() || line.unitPrice.toBigDecimalOrNull()?.signum() != 1) {
                EmberTextField(
                    value = line.unitPrice,
                    placeholder = "Price",
                    onValueChange = { raw ->
                        onPrice(raw.filter { char -> char.isDigit() || char == '.' }.take(9))
                    },
                )
            }
        }
        StepperButton("−", onDecrement)
        Text(
            line.quantity.toString(),
            style = RestaurantEmber.rowValue().copy(fontSize = 14.sp),
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
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(RestaurantEmber.divider.copy(alpha = 0.6f)),
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
            .border(1.dp, RestaurantEmber.hairline, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, style = RestaurantEmber.rowValue(RestaurantEmber.textPrimary).copy(fontSize = 14.sp))
    }
}

private data class PosLine(
    val masterId: String,
    val name: String,
    val unit: String,
    val unitPrice: String,
    val quantity: Int,
    val code: String? = null,
    val meta: String = "",
    val gstRate: String? = null,
) {
    fun amount(): BigDecimal = (unitPrice.toBigDecimalOrNull() ?: BigDecimal.ZERO)
        .multiply(BigDecimal(quantity))
        .setScale(2, RoundingMode.HALF_UP)

    companion object {
        fun of(master: OperatingMasterDto) = PosLine(
            masterId = master.id,
            name = master.name,
            unit = master.unit?.takeIf { it.isNotBlank() } ?: "plate",
            unitPrice = master.sellPrice?.takeIf { it.isNotBlank() }.orEmpty(),
            quantity = 1,
            code = master.code?.takeIf { it.isNotBlank() },
            meta = listOfNotNull(
                master.unit?.takeIf { it.isNotBlank() },
                master.dimension?.takeIf { it.isNotBlank() },
            ).joinToString(" · ").ifBlank { "Menu" },
            gstRate = master.gstRate?.takeIf { it.isNotBlank() },
        )
    }
}
