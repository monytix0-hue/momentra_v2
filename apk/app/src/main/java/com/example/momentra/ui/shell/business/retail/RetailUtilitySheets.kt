package com.example.momentra.ui.shell.business.retail

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.OperatingActivityDto
import com.example.momentra.data.api.OperatingContractBody
import com.example.momentra.data.api.OperatingDocumentDto
import com.example.momentra.data.api.OperatingLineBody
import com.example.momentra.data.api.OperatingMasterDto
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.data.repository.BusinessSliceRepository
import com.example.momentra.ui.shell.business.mto.MtoToastHost
import com.example.momentra.ui.shell.business.mto.MtoToastKind
import com.example.momentra.ui.shell.business.mto.MtoToastSpec
import com.example.momentra.ui.shell.business.shared.OperatingChromeState
import com.example.momentra.ui.theme.RetailRose
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.math.BigDecimal
import java.util.UUID

/** Cross-nav targets for Hold | Transfer | Reprint chips (Figma utility strip). */
internal enum class RetailUtilityNav { HOLD, TRANSFER, REPRINT }

/**
 * Device-local parked carts — SharedPreferences scoped by company/moment.
 * Survives process death. Not a register/holds API (`RETAIL_MISSING_APIS`).
 * Codes are sequential `HOLD-n` slips — never fiscal bill numbers.
 */
class RetailHoldSession(
    context: Context? = null,
    private val momentId: String? = null,
    private val companyId: String? = null,
) {
    val holds = mutableStateListOf<RetailParkedCart>()
    private var nextHoldNumber = 1
    private var nextTransferNumber = 1
    private val appContext = context?.applicationContext
    private val prefs = appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val gson = Gson()

    init {
        load()
    }

    /** Preview next code without consuming — only [nextHoldCode] allocates. */
    fun peekHoldCode(): String = "HOLD-$nextHoldNumber"

    fun nextHoldCode(): String {
        val code = "HOLD-$nextHoldNumber"
        nextHoldNumber += 1
        persistMeta()
        return code
    }

    fun peekTransferCode(): String = "TRF-${nextTransferNumber.toString().padStart(3, '0')}"

    fun nextTransferCode(): String {
        val code = peekTransferCode()
        nextTransferNumber += 1
        return code
    }

    fun remove(id: String) {
        holds.removeAll { it.id == id }
        persistHolds()
    }

    private fun scopeKey(): String {
        val moment = momentId?.takeIf { it.isNotBlank() } ?: "_session"
        val company = companyId?.takeIf { it.isNotBlank() } ?: "_"
        return "$company::$moment"
    }

    private fun load() {
        val p = prefs ?: return
        val key = scopeKey()
        nextHoldNumber = p.getInt(SEQ_PREFIX + key, 1).coerceAtLeast(1)
        val raw = p.getString(HOLDS_PREFIX + key, null) ?: return
        val type = object : TypeToken<List<RetailParkedCart>>() {}.type
        val loaded = runCatching { gson.fromJson<List<RetailParkedCart>>(raw, type) }.getOrNull().orEmpty()
        holds.clear()
        holds.addAll(loaded)
        val maxCode = loaded.mapNotNull { it.holdCode.removePrefix("HOLD-").toIntOrNull() }.maxOrNull() ?: 0
        if (maxCode >= nextHoldNumber) nextHoldNumber = maxCode + 1
    }

    private fun persistHolds() {
        val p = prefs ?: return
        val key = scopeKey()
        p.edit()
            .putString(HOLDS_PREFIX + key, gson.toJson(holds.toList()))
            .putInt(SEQ_PREFIX + key, nextHoldNumber)
            .apply()
    }

    private fun persistMeta() {
        val p = prefs ?: return
        p.edit().putInt(SEQ_PREFIX + scopeKey(), nextHoldNumber).apply()
    }

    internal fun persistAfterMutation() = persistHolds()

    companion object {
        private const val PREFS = "retail_hold_session"
        private const val HOLDS_PREFIX = "holds:"
        private const val SEQ_PREFIX = "seq:"
    }
}

data class RetailParkedCart(
    val id: String = UUID.randomUUID().toString(),
    val holdCode: String,
    val label: String,
    val lines: List<RetailPosLine>,
    val note: String?,
    val channelName: String?,
    val summary: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
) {
    fun total(): BigDecimal = lines.fold(BigDecimal.ZERO) { acc, line -> acc + line.amount() }
}

internal fun RetailHoldSession.park(
    label: String,
    lines: List<RetailPosLine>,
    note: String?,
    channelName: String?,
    id: String = UUID.randomUUID().toString(),
    holdCode: String = nextHoldCode(),
    summary: String? = null,
) {
    holds += RetailParkedCart(
        id = id,
        holdCode = holdCode,
        label = label,
        lines = lines.toList(),
        note = note,
        channelName = channelName,
        summary = summary,
    )
    persistAfterMutation()
}

private val RetailStationDefaults = listOf("All lines", "Staples", "Snacks", "Dairy", "Beverages")

/** `2508:29333` Park bill — parks draft cart locally; no revenue post. */
@Composable
internal fun RetailParkBillSheet(
    model: OperatingProjectionDto,
    holdSession: RetailHoldSession,
    onDismiss: () -> Unit,
    onParked: () -> Unit = onDismiss,
    onNavigate: ((RetailUtilityNav) -> Unit)? = null,
) {
    val currency = model.currency?.takeIf { it.isNotBlank() } ?: "INR"
    val registers = remember(model) {
        model.life?.channels.orEmpty().filter { it.isNotBlank() }
            .ifEmpty { listOf("Register 1", "Register 2", "Online") }
            .take(3)
    }
    val skus = remember(model.masters) { retailCatalogMasters(model) }
    val location = model.location?.takeIf { it.isNotBlank() } ?: "Store"
    val holdId = remember { UUID.randomUUID().toString() }
    val holdCode = holdSession.peekHoldCode()

    var register by remember { mutableStateOf(registers.getOrElse(1) { registers.first() }) }
    var customer by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var station by remember { mutableStateOf("All lines") }
    var query by remember { mutableStateOf("") }
    var slipTo by remember { mutableStateOf("Counter printer 2") }
    val cart = remember { mutableStateListOf<RetailPosLine>() }

    val masterById = remember(skus) { skus.associateBy { it.id } }
    val stations = remember(cart, masterById) {
        val cats = cart.mapNotNull { masterById[it.masterId]?.retailStation() }.distinct()
        listOf("All lines") + (if (cats.isEmpty()) RetailStationDefaults.drop(1) else cats)
    }
    val visibleCart = cart.filter { station == "All lines" || masterById[it.masterId]?.retailStation() == station }
    val postable = cart.isNotEmpty()
    val heroCustomer = customer.trim().ifBlank { "Walk-in · Priya" }
    val gstOn = model.gstTracking.equals("ON", true) || model.gstRegistration != null
    val cashier = model.participants.firstOrNull()?.takeIf { it.isNotBlank() } ?: "Ravi K."

    RetailSheetBody(Modifier.verticalScroll(rememberScrollState())) {
        RoseGrabber()
        RoseSheetHeader(
            title = "Hold / Park bill",
            subtitle = "Parked cart · no revenue, no stock",
            onClose = onDismiss,
        )
        RetailUtilityCrossNav(RetailUtilityNav.HOLD, onNavigate)
        RetailRegisterChips(registers, register) { register = it }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            RetailContextChip("$register · Shift open", selected = true)
            RetailContextChip("Park · recall later", selected = false)
            RetailContextChip(if (gstOn) "GST on" else "GST off", selected = false)
        }
        RetailTicketHero(
            eyebrow = "Hold slip",
            code = holdCode,
            title = heroCustomer,
            caption = "${cart.size} line${if (cart.size == 1) "" else "s"} · $location · $register",
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            stations.forEach { name ->
                RoseModeChip(name, selected = name == station, onClick = { station = name })
            }
        }
        RoseScanField(
            query = query,
            placeholder = if (skus.isEmpty()) "No SKU — add catalogue first" else "Scan or search SKU",
            onQueryChange = { query = it },
            onScan = {},
        )
        retailSkuQuickPick(skus, query, currency) { sku ->
            if (cart.none { it.masterId == sku.id }) cart += RetailPosLine.of(sku)
            query = ""
        }
        if (visibleCart.isEmpty()) {
            RoseEmpty("Nothing on the hold slip", "Scan Harbor Mart SKUs to park a draft cart.")
        } else {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                visibleCart.forEachIndexed { visibleIndex, line ->
                    val index = cart.indexOfFirst { it.masterId == line.masterId }
                    val master = masterById[line.masterId]
                    val cat = master?.retailStation() ?: "Staples"
                    val meta = listOfNotNull(
                        master?.code?.takeIf { it.isNotBlank() }
                            ?: master?.barcode?.takeIf { it.isNotBlank() },
                        line.unitPrice.takeIf { it.isNotBlank() }?.let { RetailFormat.exact(currency, it) },
                        master?.hsn?.takeIf { it.isNotBlank() }?.let { "HSN $it" },
                        master?.gstRate?.takeIf { it.isNotBlank() }?.let { "$it%" }
                            ?: master?.gstApplicable?.takeIf { it.equals("EXEMPT", true) }?.let { "Exempt" },
                    ).joinToString(" · ").ifBlank { line.unit }
                    RetailTicketLineCard(
                        title = line.name,
                        qty = line.quantity,
                        meta = meta,
                        category = cat,
                        status = "Parked",
                        statusTone = if (visibleIndex == 2) RetailTicketStatusTone.Amber else RetailTicketStatusTone.Positive,
                        accent = if (visibleIndex == 2) {
                            RetailRose.amber.copy(alpha = 0.4f)
                        } else {
                            RetailRose.rose.copy(alpha = 0.3f)
                        },
                        onDecrement = if (index >= 0) {
                            {
                                if (line.quantity <= 1) cart.removeAt(index)
                                else cart[index] = line.copy(quantity = line.quantity - 1)
                            }
                        } else null,
                        onIncrement = if (index >= 0) {
                            {
                                if (line.quantity < 99) cart[index] = line.copy(quantity = line.quantity + 1)
                            }
                        } else null,
                        onRemove = if (index >= 0) {
                            { cart.removeAt(index) }
                        } else null,
                    )
                }
            }
        }
        RoseSectionCard("Ticket") {
            RoseFieldLabel("Customer / hold label")
            RoseTextField(customer, "Walk-in · Priya", onValueChange = { customer = it.take(60) })
            RoseFieldLabel("Note")
            RoseTextField(note, "Customer stepped out", onValueChange = { note = it }, singleLine = false)
        }
        RoseMetaRows(
            listOf(
                "Slip to" to slipTo,
                "Cashier" to cashier,
                "Linked bill" to "Not posted · draft",
            ),
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            listOf("Counter printer 1", "Counter printer 2", "Screen only").forEach { printer ->
                RoseModeChip(printer, selected = printer == slipTo, onClick = { slipTo = printer })
            }
            RoseLockedChip("Thermal print")
        }
        Text("Slip to is chrome-only — no print stack.", style = RetailRose.rowCaption(RetailRose.textFaint))
        RetailHtrWhy(
            "Hold parks a draft cart on this device. No revenue, no stock, no tender and no GST until Charge on Scan cart. Device-local until a register/holds API exists — not shared across devices.",
        )
        RoseStickyFooter(
            label = if (postable) "Park bill · $holdCode" else "Park bill",
            enabled = postable,
            caveat = "Device draft only — revenue + GST post on Charge",
            onClick = {
                val parkedLines = cart.toList()
                val allocated = holdSession.nextHoldCode()
                holdSession.park(
                    id = holdId,
                    label = customer.trim().ifBlank { "Walk-in · $register" },
                    lines = parkedLines,
                    note = note.takeIf { it.isNotBlank() },
                    channelName = register,
                    holdCode = allocated,
                    summary = "${parkedLines.size} line${if (parkedLines.size == 1) "" else "s"} · $register",
                )
                onParked()
            },
        )
        Spacer(Modifier.height(16.dp))
    }
}

/** `2508:29447` Stock transfer between locations — multi-line cart; existing STOCK+TRANSFER path. */
@Composable
internal fun RetailStockTransferSheet(
    model: OperatingProjectionDto,
    holdSession: RetailHoldSession? = null,
    onDismiss: () -> Unit,
    onSubmit: (RetailEventDraft) -> Unit,
    onNavigate: ((RetailUtilityNav) -> Unit)? = null,
) {
    val currency = model.currency?.takeIf { it.isNotBlank() } ?: "INR"
    val locations = remember(model.masters) {
        model.masters.orEmpty().filter { it.kind in setOf("LOCATION", "STORE", "CHANNEL", "WAREHOUSE") }
    }
    val skus = remember(model.masters) { retailCatalogMasters(model) }
    val registers = remember(model) {
        model.life?.channels.orEmpty().filter { it.isNotBlank() }
            .ifEmpty { listOf("Register 1", "Register 2", "Online") }
            .take(3)
    }
    val transferCodes = holdSession ?: remember { RetailHoldSession() }
    val transferCode = transferCodes.peekTransferCode()

    var register by remember { mutableStateOf(registers.firstOrNull() ?: "Register 1") }
    var fromName by remember {
        mutableStateOf(
            locations.firstOrNull()?.name?.takeIf { it.isNotBlank() } ?: "Back store",
        )
    }
    var toName by remember {
        mutableStateOf(
            locations.getOrNull(1)?.name?.takeIf { it.isNotBlank() } ?: "Main store",
        )
    }
    var station by remember { mutableStateOf("All lines") }
    var query by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val cart = remember { mutableStateListOf<RetailPosLine>() }

    val from = locations.firstOrNull { it.name.equals(fromName, true) }
    val to = locations.firstOrNull { it.name.equals(toName, true) }
    val masterById = remember(skus) { skus.associateBy { it.id } }
    val stations = remember(cart, masterById) {
        val cats = cart.mapNotNull { masterById[it.masterId]?.retailStation() }.distinct()
        listOf("All lines") + (if (cats.isEmpty()) listOf("Staples", "Beverages") else cats)
    }
    val visibleCart = cart.filter { station == "All lines" || masterById[it.masterId]?.retailStation() == station }
    val totalUnits = cart.sumOf { it.quantity }
    val locationsReady = locations.size >= 2
    val postable = when {
        locationsReady -> from != null && to != null && from.id != to.id && cart.isNotEmpty()
        else -> fromName.isNotBlank() && toName.isNotBlank() && fromName != toName && cart.isNotEmpty()
    }
    val routeLabel = when {
        fromName.isBlank() || toName.isBlank() -> "Pick locations"
        else -> "$fromName → $toName"
    }
    val gstOn = model.gstTracking.equals("ON", true) || model.gstRegistration != null
    val receiverName = model.participants.getOrNull(1)?.takeIf { it.isNotBlank() }
        ?: model.participants.firstOrNull()?.takeIf { it.isNotBlank() }
        ?: "Imran S."

    RetailSheetBody(Modifier.verticalScroll(rememberScrollState())) {
        RoseGrabber()
        RoseSheetHeader(
            title = "Stock transfer",
            subtitle = "Move stock between locations · no revenue",
            onClose = onDismiss,
        )
        RetailUtilityCrossNav(RetailUtilityNav.TRANSFER, onNavigate)
        RetailRegisterChips(registers, register) { register = it }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            RetailContextChip(routeLabel, selected = true)
            RetailContextChip("Same GSTIN", selected = false)
            RetailContextChip(if (gstOn) "GST on" else "GST off", selected = false)
        }
        RetailTicketHero(
            eyebrow = "Transfer slip",
            code = transferCode,
            title = routeLabel,
            caption = "${cart.size} line${if (cart.size == 1) "" else "s"} · dispatch now",
        )
        RoseSectionCard("Route") {
            if (!locationsReady) {
                RoseEmpty("Need two locations", "Add stores or aisles under Locations first.")
            }
            RosePickerRow("From", fromName.ifBlank { "Pick location" }, onClick = null)
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val fromChoices = if (locations.isEmpty()) {
                    listOf("Back store", "Main store", "Counter")
                } else {
                    locations.take(8).map { it.name }
                }
                fromChoices.forEach { name ->
                    RoseModeChip(name, selected = name == fromName, onClick = { fromName = name })
                }
            }
            RosePickerRow("To", toName.ifBlank { "Pick location" }, onClick = null)
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val toChoices = if (locations.isEmpty()) {
                    listOf("Back store", "Main store", "Counter")
                } else {
                    locations.take(8).map { it.name }
                }
                toChoices.forEach { name ->
                    RoseModeChip(name, selected = name == toName, onClick = { toName = name })
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            stations.forEach { name ->
                RoseModeChip(name, selected = name == station, onClick = { station = name })
            }
        }
        RoseScanField(
            query = query,
            placeholder = if (skus.isEmpty()) "No SKU — add catalogue first" else "Scan or search SKU",
            onQueryChange = { query = it },
            onScan = {},
        )
        retailSkuQuickPick(skus, query, currency) { sku ->
            val existing = cart.indexOfFirst { it.masterId == sku.id }
            if (existing >= 0) {
                val line = cart[existing]
                if (line.quantity < 99) cart[existing] = line.copy(quantity = line.quantity + 1)
            } else {
                cart += RetailPosLine.of(sku)
            }
            query = ""
        }
        if (visibleCart.isEmpty()) {
            RoseEmpty("No lines to move", "Scan SKUs to build a multi-line transfer.")
        } else {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                visibleCart.forEach { line ->
                    val index = cart.indexOfFirst { it.masterId == line.masterId }
                    val master = masterById[line.masterId]
                    val cat = master?.retailStation() ?: "Staples"
                    val code = master?.code?.takeIf { it.isNotBlank() }
                        ?: master?.barcode?.takeIf { it.isNotBlank() }
                        ?: "SKU"
                    RetailTicketLineCard(
                        title = line.name,
                        qty = line.quantity,
                        meta = "$code · ${line.quantity} ${line.unit} · on hand —",
                        category = cat,
                        status = "Move",
                        statusTone = RetailTicketStatusTone.Amber,
                        accent = RetailRose.amber.copy(alpha = 0.4f),
                        onDecrement = if (index >= 0) {
                            {
                                if (line.quantity <= 1) cart.removeAt(index)
                                else cart[index] = line.copy(quantity = line.quantity - 1)
                            }
                        } else null,
                        onIncrement = if (index >= 0) {
                            {
                                if (line.quantity < 99) cart[index] = line.copy(quantity = line.quantity + 1)
                            }
                        } else null,
                        onRemove = if (index >= 0) {
                            { cart.removeAt(index) }
                        } else null,
                    )
                }
            }
        }
        RoseMetaRows(
            listOf(
                "Valuation" to "At cost · no GST within the same GSTIN",
                "Receiver" to "${toName.ifBlank { "—" }} · $receiverName",
                "Sale impact" to "None",
            ),
        )
        RoseFieldLabel("Note")
        RoseTextField(note, "Dispatch note", onValueChange = { note = it }, singleLine = false)
        RetailHtrWhy(
            "Transfer moves stock between locations at cost: − from, + to on confirm. No revenue, no AP, and no GST within the same GSTIN.",
        )
        RoseStickyFooter(
            label = if (postable) "Confirm transfer · $totalUnits units" else "Confirm transfer",
            enabled = postable,
            caveat = "Transfer never posts a sale",
            onClick = {
                transferCodes.nextTransferCode()
                val lines = cart.map {
                    OperatingLineBody(
                        itemName = it.name,
                        quantity = it.quantity.toString(),
                        unit = it.unit,
                        unitPrice = it.unitPrice.takeIf { p -> p.isNotBlank() },
                        masterId = it.masterId,
                    )
                }
                val first = cart.first()
                onSubmit(
                    RetailEventDraft(
                        eventType = "STOCK",
                        title = "Transfer · ${cart.size} line${if (cart.size == 1) "" else "s"}",
                        amount = null,
                        note = buildString {
                            append(note.takeIf { it.isNotBlank() } ?: "Register $register")
                            append(" · $routeLabel")
                            if (from == null || to == null) {
                                append(" · from/to master ids unavailable — note-only transfer")
                            }
                        },
                        channelName = fromName,
                        counterpartyName = toName,
                        paymentStatus = null,
                        quantity = totalUnits.toString(),
                        unit = first.unit,
                        itemName = if (cart.size == 1) first.name else "${cart.size} SKUs",
                        contract = OperatingContractBody(
                            stockAction = "TRANSFER",
                            transferFromName = fromName,
                            transferToName = toName,
                            transferFromMasterId = from?.id,
                            transferToMasterId = to?.id,
                        ),
                        lines = lines,
                    ),
                )
            },
        )
        Spacer(Modifier.height(16.dp))
    }
}

/** `2508:29537` Reprint — thermal receipt preview / share; never posts a new sale; never invents bill numbers. */
@Composable
internal fun RetailReprintSheet(
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onNavigate: ((RetailUtilityNav) -> Unit)? = null,
    companyId: String? = null,
) {
    val context = LocalContext.current
    val currency = model.currency?.takeIf { it.isNotBlank() } ?: "INR"
    val repo = remember { BusinessSliceRepository() }
    val momentId = OperatingChromeState.momentId
    val registers = remember(model) {
        model.life?.channels.orEmpty().filter { it.isNotBlank() }
            .ifEmpty { listOf("Register 1", "Register 2", "Online") }
            .take(3)
    }
    val sales = remember(model.activity) {
        model.activity
            .filter { it.eventType == "SALE" }
            .sortedByDescending { it.occurredAt }
            .take(8)
    }
    var register by remember { mutableStateOf(registers.getOrElse(1) { registers.first() }) }
    var selected by remember { mutableStateOf<OperatingActivityDto?>(sales.firstOrNull()) }
    var station by remember { mutableStateOf("All lines") }
    var reason by remember { mutableStateOf("Customer copy lost") }
    var document by remember { mutableStateOf<OperatingDocumentDto?>(null) }
    var paper by remember { mutableStateOf(RetailReceiptPaperSize.Default) }
    var shareFormat by remember { mutableStateOf(RetailReceiptShareFormat.JPG) }
    var logoTick by remember { mutableStateOf(0) }
    val logo = remember(companyId, momentId, logoTick) {
        RetailReceiptLogoStore.loadBitmap(context, companyId, momentId)
    }
    val amount = selected?.amount?.let { RetailFormat.exact(currency, it) } ?: "—"
    // Honesty: only show a bill no. when the API allocated one (clientKey). Never invent HM-B-*.
    val billNo = selected?.clientKey?.takeIf { it.isNotBlank() }
    val billChip = billNo?.let { "Bill $it" } ?: "No bill no."
    val postedAt = selected?.occurredAt?.let { raw ->
        raw.takeLast(5).takeIf { it.contains(":") } ?: raw.take(16).takeIf { it.isNotBlank() }
    } ?: "—"
    val tenderHint = selected?.paymentStatus?.takeIf { it.isNotBlank() } ?: "UPI"
    val gstOn = model.gstTracking.equals("ON", true) ||
        model.gstTracking.equals("YES", true) ||
        model.gstRegistration != null
    val heroTitle = selected?.let { sale ->
        listOfNotNull(
            "B2C",
            model.gstState?.takeIf { it.isNotBlank() }?.let { "$it intra-state" }
                ?: sale.counterpartyName?.takeIf { it.isNotBlank() },
        ).joinToString(" · ").ifBlank { sale.title.ifBlank { amount } }
    } ?: "Nothing selected"
    val lineCount = document?.lines?.size
    val heroCaption = listOfNotNull(
        when {
            lineCount != null && lineCount > 0 -> "$lineCount line${if (lineCount == 1) "" else "s"}"
            selected?.quantity != null -> "qty ${selected?.quantity}"
            selected != null -> "Posted sale"
            else -> null
        },
        if (gstOn) "CGST 2.5% + SGST 2.5%" else "GST off",
    ).joinToString(" · ")
    val cashier = model.participants.firstOrNull()?.takeIf { it.isNotBlank() } ?: "Cashier"
    val receipt = selected?.let { sale ->
        buildRetailReceiptFromSale(
            model = model,
            sale = sale,
            document = document,
            duplicate = true,
            reason = reason,
            tenderOverride = tenderHint,
            enteredBy = cashier,
        )
    }

    LaunchedEffect(selected?.id, momentId) {
        document = null
        val eventId = selected?.id?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        val mid = momentId?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        repo.getOperatingDocument(mid, eventId, "INVOICE").onSuccess { document = it }
    }

    RetailSheetBody(Modifier.verticalScroll(rememberScrollState())) {
        RoseGrabber()
        RoseSheetHeader(
            title = "Reprint receipt",
            subtitle = "Re-print posted bill · same bill no.",
            onClose = onDismiss,
        )
        RetailUtilityCrossNav(RetailUtilityNav.REPRINT, onNavigate)
        RetailRegisterChips(registers, register) { register = it }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            RetailContextChip(billChip, selected = true)
            RetailContextChip("Posted $postedAt · $tenderHint", selected = false)
            RetailContextChip(if (gstOn) "GST on" else "GST off", selected = false)
        }
        RetailTicketHero(
            eyebrow = "Tax invoice",
            code = billNo ?: "Unavailable",
            title = heroTitle,
            caption = heroCaption,
        )
        if (billNo == null && selected != null) {
            Text(
                "No allocated bill number on this sale — reprint shows the posted event only.",
                style = RetailRose.rowCaption(RetailRose.amber).copy(fontSize = 11.sp),
            )
        }
        RoseSectionCard("Pick sale") {
            if (sales.isEmpty()) {
                RoseEmpty("Nothing to reprint", "Posted sales will appear here.")
            } else {
                sales.forEach { sale ->
                    val picked = sale.id == selected?.id
                    val saleKey = sale.clientKey?.takeIf { it.isNotBlank() }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (picked) RetailRose.rose.copy(alpha = 0.16f) else RetailRose.field)
                            .border(
                                1.dp,
                                if (picked) RetailRose.rose else Color.White.copy(alpha = 0.06f),
                                RoundedCornerShape(10.dp),
                            )
                            .clickable { selected = sale }
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                sale.title.ifBlank { "Sale" },
                                style = RetailRose.rowTitle(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                listOfNotNull(
                                    saleKey,
                                    sale.occurredAt.take(10).takeIf { it.isNotBlank() },
                                    sale.channelName,
                                ).joinToString(" · "),
                                style = RetailRose.rowCaption().copy(fontSize = 10.sp),
                            )
                        }
                        Text(
                            RetailFormat.exact(currency, sale.amount),
                            style = RetailRose.rowValue(RetailRose.amber).copy(fontSize = 12.sp),
                        )
                    }
                }
            }
        }
        selected?.let { sale ->
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf("All lines", "Staples", "Dairy").forEach { name ->
                    RoseModeChip(name, selected = name == station, onClick = { station = name })
                }
            }
            receipt?.let { r ->
                RetailReceiptPreview(r, paper = paper, logo = logo)
                RetailReceiptShareControls(
                    model = r,
                    companyId = companyId,
                    momentId = momentId,
                    paper = paper,
                    onPaperChange = { paper = it },
                    format = shareFormat,
                    onFormatChange = { shareFormat = it },
                    logo = logo,
                    onLogoChanged = { logoTick += 1 },
                )
            } ?: Text(
                "Duplicate copy · client re-render from posted SALE fields. No receipt PDF allocation API.",
                style = RetailRose.rowCaption(RetailRose.textFaint),
            )
            RoseMetaRows(
                listOf(
                    "Original print" to listOfNotNull(postedAt, cashier, sale.channelName ?: register)
                        .joinToString(" · "),
                    "Reprint reason" to reason,
                    "Payment" to listOfNotNull(
                        "Settled",
                        sale.paymentStatus?.takeIf { it.isNotBlank() } ?: "UPI",
                        amount.takeIf { it != "—" },
                    ).joinToString(" · "),
                ),
            )
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf("Customer copy lost", "Printer jam", "Audit copy").forEach { option ->
                    RoseModeChip(option, selected = option == reason, onClick = { reason = option })
                }
            }
        }
        RetailHtrWhy(
            "Reprint shares a duplicate copy from posted SALE fields (same bill key when present). Client re-render only — no new sale, no stock or tender change, no allocated PDF API.",
        )
        RoseStickyFooter(
            label = "Reprint · duplicate copy",
            enabled = receipt != null,
            caveat = "Share / print duplicate — never duplicates revenue",
            onClick = {
                receipt?.let {
                    shareRetailReceiptFile(
                        context = context,
                        model = it,
                        paper = paper,
                        format = shareFormat,
                        logo = logo,
                        toWhatsApp = false,
                    )
                }
            },
        )
        Spacer(Modifier.height(16.dp))
    }
}

private enum class RetailTicketStatusTone { Positive, Amber, Rose }

@Composable
private fun RetailUtilityCrossNav(
    active: RetailUtilityNav,
    onNavigate: ((RetailUtilityNav) -> Unit)?,
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        RetailUtilityNav.entries.forEach { dest ->
            val label = when (dest) {
                RetailUtilityNav.HOLD -> "Hold"
                RetailUtilityNav.TRANSFER -> "Transfer"
                RetailUtilityNav.REPRINT -> "Reprint"
            }
            RoseModeChip(
                text = label,
                selected = dest == active,
                onClick = {
                    if (dest != active) onNavigate?.invoke(dest)
                },
            )
        }
    }
}

@Composable
private fun RetailRegisterChips(
    registers: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        registers.forEach { name ->
            RetailContextChip(name, selected = name == selected, onClick = { onSelect(name) })
        }
    }
}

/** Register / context chips — Figma deep rose `#881337` when selected. */
@Composable
private fun RetailContextChip(
    text: String,
    selected: Boolean,
    onClick: (() -> Unit)? = null,
) {
    val shape = RetailRose.rPill
    val click = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Text(
        text,
        style = RetailRose.chipLabel(selected).copy(
            color = if (selected) RetailRose.field else RetailRose.textMuted,
        ),
        maxLines = 1,
        modifier = Modifier
            .clip(shape)
            .background(if (selected) RetailRose.roseDeep else RetailRose.chip)
            .border(
                1.dp,
                if (selected) RetailRose.amber else Color(0xFF475569),
                shape,
            )
            .then(click)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

/** Ticket hero — Figma Hold / Transfer / Reprint slip card. */
@Composable
private fun RetailTicketHero(
    eyebrow: String,
    code: String,
    title: String,
    caption: String,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RetailRose.rCardTight)
            .background(RetailRose.chip)
            .border(1.5.dp, RetailRose.rose, RetailRose.rCardTight)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(eyebrow, style = RetailRose.rowCaption().copy(fontSize = 11.sp), maxLines = 1)
            Spacer(Modifier.weight(1f))
            Text(
                code,
                style = RetailRose.rowCaption(RetailRose.amber).copy(
                    fontSize = 12.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                ),
                maxLines = 1,
            )
        }
        Text(
            title,
            style = RetailRose.metricValue().copy(fontSize = 28.sp, lineHeight = 34.sp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(caption, style = RetailRose.rowCaption().copy(fontSize = 12.sp), maxLines = 2)
    }
}

@Composable
private fun RetailHtrWhy(body: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(RetailRose.rose.copy(alpha = 0.12f))
            .border(1.dp, RetailRose.rose.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "Why",
            style = RetailRose.why().copy(
                color = RetailRose.amber,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            ),
            maxLines = 1,
        )
        Text(
            body,
            style = RetailRose.why().copy(color = RetailRose.whyCream),
            modifier = Modifier.weight(1f),
        )
    }
}

/** Figma ticket line card — SKU · HSN · ₹ · % meta + category + status chips. */
@Composable
private fun RetailTicketLineCard(
    title: String,
    qty: Int,
    meta: String,
    category: String,
    status: String,
    statusTone: RetailTicketStatusTone,
    accent: Color,
    onDecrement: (() -> Unit)? = null,
    onIncrement: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
) {
    val statusFill = when (statusTone) {
        RetailTicketStatusTone.Positive -> RetailRose.positive
        RetailTicketStatusTone.Amber -> RetailRose.amber
        RetailTicketStatusTone.Rose -> RetailRose.rose
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(RetailRose.chip)
            .border(1.dp, accent, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                style = RetailRose.rowTitle().copy(fontSize = 14.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (onDecrement != null && onIncrement != null) {
                RoseStepper(qty, onDecrement = onDecrement, onIncrement = onIncrement)
            } else {
                Text(
                    "×$qty",
                    style = RetailRose.rowValue(RetailRose.amber).copy(fontSize = 14.sp),
                )
            }
            onRemove?.let {
                Text(
                    "✕",
                    modifier = Modifier.clickable(onClick = it).padding(start = 4.dp),
                    style = RetailRose.rowCaption(RetailRose.textFaint),
                )
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                meta,
                style = RetailRose.rowCaption().copy(fontSize = 11.sp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                category,
                style = RetailRose.chipLabel(false).copy(color = RetailRose.textMuted),
                maxLines = 1,
                modifier = Modifier
                    .clip(RetailRose.rPill)
                    .background(RetailRose.chip)
                    .border(1.dp, Color(0xFF475569), RetailRose.rPill)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
            Text(
                status,
                style = RetailRose.chipLabel(true).copy(color = RetailRose.field),
                maxLines = 1,
                modifier = Modifier
                    .clip(RetailRose.rPill)
                    .background(statusFill)
                    .border(1.dp, RetailRose.amber, RetailRose.rPill)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

internal fun OperatingMasterDto.retailStation(): String {
    val raw = dimension?.takeIf { it.isNotBlank() }
        ?: name.substringBefore(' ').takeIf { it.length in 3..12 }
        ?: "Staples"
    return when {
        raw.contains("snack", true) -> "Snacks"
        raw.contains("dairy", true) || raw.contains("milk", true) || name.contains("milk", true) -> "Dairy"
        raw.contains("beverage", true) || raw.contains("drink", true) -> "Beverages"
        raw.contains("personal", true) -> "Personal care"
        raw.contains("home", true) -> "Home care"
        raw.contains("pack", true) || name.contains("bag", true) -> "Packaging"
        raw.contains("consum", true) || name.contains("label", true) -> "Consumables"
        raw.contains("staple", true) || name.contains("basmati", true) ||
            name.contains("dal", true) || name.contains("salt", true) ||
            name.contains("oil", true) -> "Staples"
        else -> raw.replaceFirstChar { it.uppercase() }.take(16)
    }
}

/** On-hand caption for PO browser — live masters omit qty until stock projection ships. */
internal fun OperatingMasterDto.retailOnHandCaption(): String? = null

internal fun retailCatalogMasters(model: OperatingProjectionDto): List<OperatingMasterDto> =
    model.masters.orEmpty()
        .filter { it.kind in setOf("PRODUCT", "ITEM", "SKU", "MATERIAL", "CONSUMABLE") }
        .sortedBy { it.name.lowercase() }

/** Match order: exact code → exact barcode → name contains (min length 2). */
internal fun retailMatchCatalogSku(
    skus: List<OperatingMasterDto>,
    raw: String,
): OperatingMasterDto? {
    val q = raw.trim()
    if (q.isEmpty()) return null
    skus.firstOrNull { it.code?.equals(q, ignoreCase = true) == true }?.let { return it }
    skus.firstOrNull {
        !it.barcode.isNullOrBlank() && it.barcode.equals(q, ignoreCase = true)
    }?.let { return it }
    if (q.length >= 2) {
        skus.firstOrNull { it.name.contains(q, ignoreCase = true) }?.let { return it }
    }
    return null
}

internal fun retailUnmatchedScanToast() {
    MtoToastHost.show(
        MtoToastSpec(
            kind = MtoToastKind.Saved,
            title = "No product for this code",
            body = "Pick from catalogue or add under Catalogue.",
            actionLabel = "OK",
            accent = RetailRose.amber,
        ),
    )
}

@Composable
internal fun retailSkuQuickPick(
    skus: List<OperatingMasterDto>,
    query: String,
    currency: String,
    onPick: (OperatingMasterDto) -> Unit,
) {
    if (skus.isEmpty()) return
    val trimmed = query.trim()
    val matches = when {
        trimmed.isBlank() -> emptyList()
        else -> skus.filter {
            it.name.contains(trimmed, true) || it.barcode?.contains(trimmed, true) == true ||
                it.code?.contains(trimmed, true) == true
        }.take(4)
    }
    if (matches.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            matches.forEach { sku ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(RetailRose.field)
                        .clickable { onPick(sku) }
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(sku.name, style = RetailRose.rowTitle(), modifier = Modifier.weight(1f), maxLines = 1)
                    sku.sellPrice?.takeIf { it.isNotBlank() }?.let {
                        Text(RetailFormat.exact(currency, it), style = RetailRose.rowCaption(RetailRose.amber))
                    }
                }
            }
        }
    }
}

@Composable
internal fun retailCartLines(
    cart: List<RetailPosLine>,
    currency: String,
    onUpdate: (Int, RetailPosLine) -> Unit,
    onRemove: (Int) -> Unit,
) {
    if (cart.isEmpty()) {
        RoseEmpty("Nothing on the ticket", "Scan or search a catalogue SKU.")
        return
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RetailRose.rInner)
            .background(RetailRose.chip)
            .padding(horizontal = 12.dp),
    ) {
        cart.forEachIndexed { index, line ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(line.name, style = RetailRose.rowTitle(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    RoseTextField(
                        line.unitPrice,
                        "Price",
                        onValueChange = { raw ->
                            onUpdate(index, line.copy(unitPrice = raw.filter { it.isDigit() || it == '.' }.take(9)))
                        },
                    )
                }
                RoseStepper(
                    line.quantity,
                    onDecrement = {
                        if (line.quantity <= 1) onRemove(index)
                        else onUpdate(index, line.copy(quantity = line.quantity - 1))
                    },
                    onIncrement = {
                        if (line.quantity < 99) onUpdate(index, line.copy(quantity = line.quantity + 1))
                    },
                )
                Text(
                    RetailFormat.exact(currency, line.amount().stripTrailingZeros().toPlainString()),
                    style = RetailRose.rowValue().copy(fontSize = 12.sp),
                )
                Text(
                    "✕",
                    modifier = Modifier.clickable { onRemove(index) }.padding(4.dp),
                    style = RetailRose.rowCaption(RetailRose.textFaint),
                )
            }
        }
    }
}
