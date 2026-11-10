package com.example.momentra.ui.shell.business.retail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.momentra.data.api.OperatingMasterBody
import com.example.momentra.data.api.OperatingMasterDto
import com.example.momentra.data.api.OperatingMasterUpdateBody
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.data.repository.BusinessSliceRepository
import com.example.momentra.ui.theme.RetailRose
import kotlinx.coroutines.launch

internal enum class RetailMasterFormKind(
    val title: String,
    val cta: String,
    val masterKind: String,
) {
    Supplier("New supplier", "Save supplier", "SUPPLIER"),
    Location("Add location", "Save location", "LOCATION"),
    Product("Create product / SKU", "Save product", "PRODUCT"),
    StockItem("Create stock item", "Save stock item", "MATERIAL"),
}

/**
 * Master create/edit forms — Figma `2506:29612` Supplier, `29703` Location,
 * `29782` Product, `29907` Stock item.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RetailMasterFormSheet(
    kind: RetailMasterFormKind,
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onSubmit: (RetailEventDraft) -> Unit,
    companyId: String? = null,
) {
    val scope = rememberCoroutineScope()
    val repo = remember { BusinessSliceRepository() }
    var isCreate by remember { mutableStateOf(true) }
    var editing by remember { mutableStateOf<OperatingMasterDto?>(null) }
    var saving by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    var name by remember { mutableStateOf("") }
    var legalName by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var gstin by remember { mutableStateOf("") }
    var gstRegistration by remember { mutableStateOf("Regular") }
    var category by remember { mutableStateOf("") }
    var paymentTerms by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Active") }
    var unit by remember { mutableStateOf("pcs") }
    var barcode by remember { mutableStateOf("") }
    var sellPrice by remember { mutableStateOf("") }
    var mrp by remember { mutableStateOf("") }
    var purchasePrice by remember { mutableStateOf("") }
    var gstRate by remember { mutableStateOf("5") }
    var hsn by remember { mutableStateOf("") }
    var defaultLocation by remember { mutableStateOf("Main store") }
    var variant by remember { mutableStateOf("Single") }
    var trackBatch by remember { mutableStateOf("Off · optional") }
    var locationType by remember { mutableStateOf("Store") }
    var receiptPrinter by remember { mutableStateOf("Screen only") }
    var displayOrder by remember { mutableStateOf("") }
    var stocksCategory by remember { mutableStateOf("Staples") }
    var locationLead by remember { mutableStateOf("") }
    var parQty by remember { mutableStateOf("") }
    var preferredSupplier by remember { mutableStateOf("") }
    var tracked by remember { mutableStateOf("Track stock") }
    var contextChip by remember {
        mutableStateOf(if (kind == RetailMasterFormKind.StockItem) "Stock master" else "Catalogue master")
    }

    val candidates = remember(model.masters, kind) {
        model.masters.orEmpty().filter { masterMatchesKind(it, kind) }
    }
    val locations = remember(model.masters) {
        model.masters.orEmpty().filter {
            it.kind.uppercase() in setOf("LOCATION", "STORE", "WAREHOUSE", "CHANNEL")
        }
    }
    val suppliers = remember(model.masters) {
        model.masters.orEmpty().filter { it.kind.equals("SUPPLIER", true) }
    }

    fun hydrate(m: OperatingMasterDto) {
        editing = m
        name = m.name
        code = m.code.orEmpty()
        gstin = m.gstin.orEmpty()
        unit = m.unit ?: "pcs"
        barcode = m.barcode.orEmpty()
        sellPrice = m.sellPrice.orEmpty()
        mrp = m.mrp.orEmpty()
        purchasePrice = m.purchasePrice.orEmpty()
        gstRate = m.gstRate?.takeIf { it.isNotBlank() } ?: if (kind == RetailMasterFormKind.StockItem) "0" else "5"
        hsn = m.hsn.orEmpty()
        gstRegistration = m.gstRegistration?.takeIf { it.isNotBlank() } ?: "Regular"
        status = when (m.status?.uppercase()) {
            "INACTIVE", "ARCHIVED" -> "Inactive"
            "PAUSED" -> "Paused"
            "PREFERRED" -> "Preferred"
            else -> if (kind == RetailMasterFormKind.Location) "Active" else "Active"
        }
        tracked = if (m.tracked == false) "Non-tracked" else "Track stock"
    }

    fun clearForCreate() {
        editing = null
        name = ""; legalName = ""; code = ""; phone = ""; email = ""; gstin = ""
        gstRegistration = "Regular"
        category = if (kind == RetailMasterFormKind.StockItem) "Packaging" else ""
        paymentTerms = ""; address = ""
        status = "Active"; unit = "pcs"
        barcode = ""; sellPrice = ""; mrp = ""; purchasePrice = ""
        gstRate = if (kind == RetailMasterFormKind.StockItem) "0" else "5"
        hsn = ""; parQty = ""; preferredSupplier = ""; locationLead = ""; displayOrder = ""
        trackBatch = "Off · optional"; tracked = "Track stock"
        variant = "Single"; locationType = "Store"; receiptPrinter = "Screen only"
        contextChip = if (kind == RetailMasterFormKind.StockItem) "Stock master" else "Catalogue master"
    }

    val postable = name.isNotBlank() && !saving && (isCreate || editing != null)
    val canUpdate = !isCreate && editing != null && !companyId.isNullOrBlank()

    RetailSheetBody(Modifier.verticalScroll(rememberScrollState())) {
        RoseGrabber()
        RoseSheetHeader(
            title = if (isCreate) kind.title else "Edit ${kind.masterKind.lowercase()}",
            subtitle = when (kind) {
                RetailMasterFormKind.Supplier -> "Supplier master · GSTIN drives input credit on Purchase / GRN"
                RetailMasterFormKind.Location -> "Stores, back rooms & counters · stock point"
                RetailMasterFormKind.Product -> "Master item · typed fields + GST before it appears in Sales Order"
                RetailMasterFormKind.StockItem -> "Master stock item · unit, rate, HSN & GST before Purchase Order can add it"
            },
            onClose = onDismiss,
        )

        RoseCreateEditMode(
            isCreate = isCreate,
            onCreate = { isCreate = true; clearForCreate() },
            onEdit = { isCreate = false },
            // D2: Edit requires companyId for updateMaster — never silently create.
            editLocked = companyId.isNullOrBlank(),
        )

        if (!isCreate) {
            RoseSectionCard("Edit existing") {
                if (candidates.isEmpty()) {
                    RoseEmpty("No masters yet", "Create one first, then edit from here.")
                } else {
                    candidates.take(8).forEach { row ->
                        RosePickerRow(
                            label = row.kind,
                            value = row.name,
                            onClick = { hydrate(row) },
                        )
                    }
                }
            }
        }

        when (kind) {
            RetailMasterFormKind.Supplier -> {
                RoseFieldLabel("Supplier name")
                RoseTextField(name, "e.g. Coastal Traders", onValueChange = { name = it.take(80) })
                RoseFieldLabel("Legal / trade name")
                RoseTextField(legalName, "As on GST certificate", onValueChange = { legalName = it.take(80) })
                RoseFieldLabel("GSTIN")
                RoseTextField(gstin, "27AAACC1234D1Z7", onValueChange = { gstin = it.take(15).uppercase() })
                Text(
                    "15 chars · state code from GSTIN · local check only",
                    style = RetailRose.rowCaption(RetailRose.textFaint),
                )
                RoseChipFlow("GST registration", listOf("Regular", "Composition", "Unregistered"), gstRegistration) {
                    gstRegistration = it
                }
                RoseFieldLabel("Phone")
                RoseTextField(phone, "+91 98200 12345", onValueChange = { phone = it.take(20) })
                RoseFieldLabel("Email (optional)")
                RoseTextField(email, "orders@example.in", onValueChange = { email = it.take(60) })
                RoseChipFlow(
                    "Category",
                    listOf("FMCG", "Staples", "Personal care", "Home care", "Beverages", "Packaging"),
                    category,
                ) { category = it }
                RoseFieldLabel("Payment terms")
                RoseTextField(paymentTerms, "Net 7 days", onValueChange = { paymentTerms = it.take(40) })
                RoseFieldLabel("Address")
                RoseTextField(address, "MIDC, Andheri East, Mumbai", onValueChange = { address = it.take(120) }, singleLine = false)
                RoseChipFlow("Status", listOf("Active", "Preferred", "Inactive"), status) { status = it }
            }

            RetailMasterFormKind.Location -> {
                RoseFieldLabel("Location name")
                RoseTextField(name, "e.g. Mezzanine store", onValueChange = { name = it.take(80) })
                RoseChipFlow("Type", listOf("Store", "Warehouse", "Counter"), locationType) {
                    locationType = it
                }
                RoseChipFlow(
                    "Receipt printer",
                    listOf("Counter printer 1", "Label printer", "Screen only"),
                    receiptPrinter,
                ) { receiptPrinter = it }
                RoseFieldLabel("Display order")
                RoseTextField(displayOrder, "5", onValueChange = { displayOrder = it.filter { ch -> ch.isDigit() }.take(4) })
                RoseChipFlow(
                    "Stocks categories",
                    listOf("Staples", "Snacks", "Personal care", "Beverages", "Home care"),
                    stocksCategory,
                ) { stocksCategory = it }
                RoseFieldLabel("Location lead (optional)")
                RoseTextField(locationLead, "Imran Shaikh", onValueChange = { locationLead = it.take(40) })
                RoseChipFlow("Status", listOf("Active", "Paused"), status) { status = it }
            }

            RetailMasterFormKind.Product -> {
                RoseChipFlow(
                    null,
                    listOf("Harbor Mart POS", "Catalogue master", "GST-aware"),
                    contextChip,
                ) { contextChip = it }
                RoseFieldLabel("Product name")
                RoseTextField(name, "e.g. Basmati rice 5kg", onValueChange = { name = it.take(80) })
                RoseChipFlow(
                    "Category",
                    listOf("Staples", "Snacks", "Personal care", "Beverages", "Home care"),
                    category,
                ) { category = it }
                RoseChipFlow("Variants", listOf("Size / pack", "Single", "Colour set"), variant) { variant = it }
                RoseAmountHero(
                    label = "Sell price",
                    amount = if (sellPrice.isBlank()) "—" else RetailFormat.exact(model.currency ?: "INR", sellPrice),
                    caption = "Sell price ≤ MRP · per-variant override",
                )
                RoseFieldLabel("Sell price amount")
                RoseTextField(sellPrice, "649", onValueChange = {
                    sellPrice = it.filter { ch -> ch.isDigit() || ch == '.' }.take(12)
                })
                RoseFieldLabel("MRP")
                RoseTextField(mrp, "699", onValueChange = {
                    mrp = it.filter { ch -> ch.isDigit() || ch == '.' }.take(12)
                })
                RoseSectionCard("GST on product") {
                    RoseChipFlow(null, listOf("5%", "18%", "Exempt"), gstChipLabel(gstRate)) { chip ->
                        gstRate = when (chip) {
                            "Exempt" -> ""
                            else -> chip.removeSuffix("%")
                        }
                    }
                    Text(
                        "Rate + HSN live on the master. Output GST posts on bill / POS Sale only — never on Sales Order save.",
                        style = RetailRose.rowCaption(),
                    )
                }
                RoseFieldLabel("HSN / SAC")
                RoseTextField(hsn, "1006 · Rice", onValueChange = { hsn = it.take(24) })
                RoseChipFlow(
                    "Default location",
                    locationChips(locations),
                    defaultLocation,
                ) { defaultLocation = it }
                RoseChipFlow("Unit", listOf("pcs", "kg", "pack"), unit) { unit = it }
                RoseFieldLabel("Barcode / EAN")
                RoseTextField(barcode, "8901234567890 · scan to fill", onValueChange = { barcode = it.take(32) })
                RosePickerRow("Track batch / expiry", trackBatch, onClick = {
                    trackBatch = if (trackBatch.startsWith("Off")) "On · batch required" else "Off · optional"
                })
                RoseChipFlow("Status", listOf("Active", "Inactive"), status) { status = it }
            }

            RetailMasterFormKind.StockItem -> {
                RoseChipFlow(
                    null,
                    listOf("Harbor Mart POS", "Stock master", "GST-aware"),
                    contextChip,
                ) { contextChip = it }
                RoseFieldLabel("Item name")
                RoseTextField(name, "e.g. Carry bags (L)", onValueChange = { name = it.take(80) })
                RoseChipFlow(
                    "Category",
                    listOf("Packaging", "Consumables", "Cleaning", "Stationery", "Display", "Spares"),
                    category.ifBlank { "Packaging" },
                ) { category = it }
                RoseChipFlow("Unit of measure", listOf("kg", "L", "pcs", "bag", "can"), unit) { unit = it }
                RoseAmountHero(
                    label = "Purchase rate",
                    amount = if (purchasePrice.isBlank()) "—" else RetailFormat.exact(model.currency ?: "INR", purchasePrice),
                    caption = "Default rate on PO lines · editable per order",
                )
                RoseFieldLabel("Purchase rate amount")
                RoseTextField(purchasePrice, "480", onValueChange = {
                    purchasePrice = it.filter { ch -> ch.isDigit() || ch == '.' }.take(12)
                })
                RoseSectionCard("GST on purchase") {
                    RoseChipFlow(null, listOf("0%", "5%", "12%", "18%"), gstChipLabel(gstRate.ifBlank { "0" })) { chip ->
                        gstRate = chip.removeSuffix("%")
                    }
                    Text(
                        "Input GST on Purchase / GRN. PO commitment can show estimate; it does not post tax.",
                        style = RetailRose.rowCaption(),
                    )
                }
                RoseFieldLabel("HSN code")
                RoseTextField(hsn, "HSN code", onValueChange = { hsn = it.take(24) })
                RosePickerRow(
                    "Default location",
                    defaultLocation,
                    onClick = {
                        val cycle = locationChips(locations)
                        val idx = cycle.indexOf(defaultLocation)
                        defaultLocation = cycle[(idx + 1).coerceAtLeast(0) % cycle.size]
                    },
                )
                RoseFieldLabel("Par / reorder qty")
                RoseTextField(parQty, "500 pcs", onValueChange = { parQty = it.take(24) })
                RosePickerRow(
                    "Preferred supplier",
                    preferredSupplier.ifBlank { suppliers.firstOrNull()?.name ?: "Select…" },
                    onClick = {
                        val names = suppliers.map { it.name }.ifEmpty { listOf("Select…") }
                        val idx = names.indexOf(preferredSupplier)
                        preferredSupplier = names[(idx + 1).coerceAtLeast(0) % names.size]
                    },
                )
                RoseChipFlow(null, listOf("Track stock", "Non-tracked"), tracked) { tracked = it }
                RoseChipFlow("Status", listOf("Active", "Inactive"), status) { status = it }
            }
        }

        localError?.let {
            Text(it, style = RetailRose.rowCaption(RetailRose.warning))
        }

        RoseWhyBox(masterWhyLine(kind), tinted = true)

        RoseStickyFooter(
            label = if (saving) "Saving…" else kind.cta,
            enabled = postable && (isCreate || canUpdate),
            caveat = when {
                !isCreate && !canUpdate -> "Edit existing needs company context — create still works"
                kind == RetailMasterFormKind.Location -> "Location saved · stock point ready"
                else -> "Master saved · available in browser on next open"
            },
            onClick = {
                val metaNote = buildMasterNote(
                    kind = kind,
                    legalName = legalName,
                    phone = phone,
                    email = email,
                    category = category,
                    paymentTerms = paymentTerms,
                    address = address,
                    status = status,
                    gstRegistration = gstRegistration,
                    locationType = locationType,
                    receiptPrinter = receiptPrinter,
                    displayOrder = displayOrder,
                    stocksCategory = stocksCategory,
                    locationLead = locationLead,
                    defaultLocation = defaultLocation,
                    variant = variant,
                    trackBatch = trackBatch,
                    parQty = parQty,
                    preferredSupplier = preferredSupplier,
                    contextChip = contextChip,
                )
                val hsnDigits = hsn.filter { it.isDigit() }.take(8)
                val gstRegCode = when (gstRegistration) {
                    "Composition" -> "COMPOSITION"
                    "Unregistered" -> "UNREGISTERED"
                    else -> "REGULAR"
                }
                val statusCode = when (status) {
                    "Inactive", "Paused" -> "INACTIVE"
                    else -> "ACTIVE" // Preferred stays ACTIVE; preference lives in note
                }
                val body = OperatingMasterBody(
                    kind = when (kind) {
                        RetailMasterFormKind.Location -> when (locationType) {
                            "Warehouse" -> "WAREHOUSE"
                            else -> "LOCATION" // Store + Counter
                        }
                        else -> kind.masterKind
                    },
                    name = name.trim(),
                    code = when (kind) {
                        RetailMasterFormKind.Supplier ->
                            code.takeIf { it.isNotBlank() }
                                ?: legalName.takeIf { it.isNotBlank() }
                        else -> code.takeIf { it.isNotBlank() } ?: barcode.takeIf { it.isNotBlank() }
                    },
                    unit = unit.takeIf { it.isNotBlank() },
                    barcode = barcode.takeIf { it.isNotBlank() },
                    sellPrice = sellPrice.takeIf { it.isNotBlank() },
                    mrp = mrp.takeIf { it.isNotBlank() },
                    purchasePrice = purchasePrice.takeIf { it.isNotBlank() },
                    gstRate = gstRate.takeIf { it.isNotBlank() },
                    gstApplicable = when {
                        kind == RetailMasterFormKind.Product && gstRate.isBlank() -> "NO"
                        gstRate.isNotBlank() -> "YES"
                        else -> null
                    },
                    gstClassification = when {
                        kind == RetailMasterFormKind.Product && gstRate.isBlank() -> "EXEMPT"
                        gstRate.isNotBlank() -> "TAXABLE"
                        else -> null
                    },
                    gstRegistration = if (kind == RetailMasterFormKind.Supplier) gstRegCode else null,
                    hsn = hsnDigits.takeIf { it.isNotBlank() },
                    gstin = gstin.takeIf { it.isNotBlank() },
                    tracked = when (kind) {
                        RetailMasterFormKind.StockItem -> tracked == "Track stock"
                        else -> null
                    },
                    parentMasterId = suppliers.firstOrNull { it.name == preferredSupplier }?.id
                        ?.takeIf { kind == RetailMasterFormKind.StockItem },
                )
                if (!isCreate && canUpdate) {
                    val target = editing ?: return@RoseStickyFooter
                    saving = true
                    scope.launch {
                        val result = repo.updateMaster(
                            companyId!!,
                            target.id,
                            OperatingMasterUpdateBody(
                                kind = body.kind,
                                name = body.name,
                                code = body.code,
                                unit = body.unit,
                                barcode = body.barcode,
                                sellPrice = body.sellPrice,
                                mrp = body.mrp,
                                purchasePrice = body.purchasePrice,
                                gstApplicable = body.gstApplicable,
                                gstClassification = body.gstClassification,
                                gstRate = body.gstRate,
                                hsn = body.hsn,
                                gstin = body.gstin,
                                tracked = body.tracked,
                                parentMasterId = body.parentMasterId,
                                status = statusCode,
                                expectedVersion = target.version,
                            ),
                        )
                        result.fold(
                            onSuccess = { onDismiss() },
                            onFailure = {
                                localError = it.message ?: "Could not update"
                                saving = false
                            },
                        )
                    }
                } else {
                    onSubmit(
                        RetailEventDraft(
                            eventType = "UPDATE",
                            title = name.trim(),
                            amount = null,
                            note = metaNote,
                            channelName = null,
                            counterpartyName = preferredSupplier.takeIf { it.isNotBlank() },
                            paymentStatus = null,
                            masterKind = body.kind,
                            master = body,
                        ),
                    )
                }
            },
        )
        Spacer(Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RoseChipFlow(
    label: String?,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        label?.let { RoseFieldLabel(it) }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            options.forEach { option ->
                RoseModeChip(option, selected = option == selected, onClick = { onSelect(option) })
            }
        }
    }
}

private fun gstChipLabel(rate: String): String = when {
    rate.isBlank() -> "Exempt"
    else -> "${rate.removeSuffix("%")}%"
}

private fun locationChips(locations: List<OperatingMasterDto>): List<String> {
    val fromMasters = locations.map { it.name }.filter { it.isNotBlank() }.distinct()
    return (fromMasters + listOf("Main store", "Back store", "Warehouse", "Counter")).distinct()
}

private fun masterMatchesKind(master: OperatingMasterDto, kind: RetailMasterFormKind): Boolean {
    val k = master.kind.uppercase()
    return when (kind) {
        RetailMasterFormKind.Supplier -> k == "SUPPLIER"
        RetailMasterFormKind.Location -> k in setOf("LOCATION", "STORE", "WAREHOUSE", "CHANNEL")
        RetailMasterFormKind.Product -> k in setOf("PRODUCT", "SKU")
        RetailMasterFormKind.StockItem -> k in setOf("MATERIAL", "CONSUMABLE", "ITEM")
    }
}

private fun masterWhyLine(kind: RetailMasterFormKind): String = when (kind) {
    RetailMasterFormKind.Supplier ->
        "Supplier is a master write. GSTIN state code decides CGST+SGST vs IGST on Purchase / GRN. PO save is commitment only."
    RetailMasterFormKind.Location ->
        "Locations are stock and sale points. Products pick a default location in Create Product; transfers move stock between them."
    RetailMasterFormKind.Product ->
        "Create Product is a master write. Sales Order and POS only pick saved SKUs; GST posts on the bill, not on save."
    RetailMasterFormKind.StockItem ->
        "Create Stock Item is a master write. Purchase Order only references saved items; input GST posts on Purchase / GRN."
}

private fun buildMasterNote(
    kind: RetailMasterFormKind,
    legalName: String,
    phone: String,
    email: String,
    category: String,
    paymentTerms: String,
    address: String,
    status: String,
    gstRegistration: String,
    locationType: String,
    receiptPrinter: String,
    displayOrder: String,
    stocksCategory: String,
    locationLead: String,
    defaultLocation: String,
    variant: String,
    trackBatch: String,
    parQty: String,
    preferredSupplier: String,
    contextChip: String,
): String? {
    val parts = buildList {
        when (kind) {
            RetailMasterFormKind.Supplier -> {
                legalName.takeIf { it.isNotBlank() }?.let { add("Legal: $it") }
                phone.takeIf { it.isNotBlank() }?.let { add("Phone: $it") }
                email.takeIf { it.isNotBlank() }?.let { add("Email: $it") }
                category.takeIf { it.isNotBlank() }?.let { add("Category: $it") }
                paymentTerms.takeIf { it.isNotBlank() }?.let { add("Terms: $it") }
                address.takeIf { it.isNotBlank() }?.let { add("Address: $it") }
                add("GST registration: $gstRegistration")
                add("Status: $status")
            }
            RetailMasterFormKind.Location -> {
                add("Type: $locationType")
                add("Receipt printer: $receiptPrinter") // D6 UI-only
                displayOrder.takeIf { it.isNotBlank() }?.let { add("Display order: $it") }
                add("Stocks: $stocksCategory")
                locationLead.takeIf { it.isNotBlank() }?.let { add("Lead: $it") }
                add("Status: $status")
            }
            RetailMasterFormKind.Product -> {
                category.takeIf { it.isNotBlank() }?.let { add("Category: $it") }
                add("Variant: $variant")
                add("Default location: $defaultLocation")
                add("Track batch: $trackBatch")
                add("Context: $contextChip")
                add("Status: $status")
            }
            RetailMasterFormKind.StockItem -> {
                category.takeIf { it.isNotBlank() }?.let { add("Category: $it") }
                add("Default location: $defaultLocation")
                parQty.takeIf { it.isNotBlank() }?.let { add("Par: $it") }
                preferredSupplier.takeIf { it.isNotBlank() }?.let { add("Preferred supplier: $it") }
                add("Context: $contextChip")
                add("Status: $status")
            }
        }
    }
    return parts.joinToString(" · ").ifBlank { null }
}
