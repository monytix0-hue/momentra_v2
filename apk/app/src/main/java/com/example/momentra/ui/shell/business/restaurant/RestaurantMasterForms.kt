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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.OperatingMasterBody
import com.example.momentra.data.api.OperatingMasterDto
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.theme.RestaurantEmber

/**
 * Master create/edit forms — Figma `2461:29655` Dish, `2461:29777` Raw,
 * `2472:29659` Vendor, `2472:29750` Station.
 */
internal enum class RestaurantMasterFormKind(
    val title: String,
    val cta: String,
    val masterKind: String,
) {
    Dish("Create menu dish", "Save menu dish", "MENU_DISH"),
    Raw("Create raw material", "Save raw material", "MATERIAL"),
    Vendor("New vendor", "Save vendor", "VENDOR"),
    Station("Add station", "Save station", "STATION"),
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RestaurantMasterFormSheet(
    kind: RestaurantMasterFormKind,
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onSubmit: (RestaurantEventDraft) -> Unit,
    existing: OperatingMasterDto? = null,
    onSaved: (() -> Unit)? = null,
) {
    var isCreate by remember(existing?.id) { mutableStateOf(existing == null) }
    var editing by remember(existing?.id) { mutableStateOf(existing) }
    var showToast by remember { mutableStateOf(false) }

    var name by remember(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var legalName by remember { mutableStateOf("") }
    var gstin by remember(existing?.id) { mutableStateOf(existing?.gstin.orEmpty()) }
    var gstRegistration by remember { mutableStateOf("Regular") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var category by remember {
        mutableStateOf(
            when (kind) {
                RestaurantMasterFormKind.Dish -> "Mains"
                RestaurantMasterFormKind.Raw -> "Dairy"
                RestaurantMasterFormKind.Vendor -> "Meat & Fish"
                RestaurantMasterFormKind.Station -> "Kitchen"
            },
        )
    }
    var paymentTerms by remember { mutableStateOf("Net 7 days") }
    var address by remember { mutableStateOf("") }
    var status by remember(existing?.id) {
        mutableStateOf(
            when (existing?.status?.uppercase()) {
                "INACTIVE", "ARCHIVED" -> "Inactive"
                "PAUSED" -> "Paused"
                "PREFERRED" -> "Preferred"
                else -> "Active"
            },
        )
    }
    var unit by remember(existing?.id) { mutableStateOf(existing?.unit ?: "kg") }
    var sellPrice by remember(existing?.id) {
        mutableStateOf(existing?.sellPrice?.ifBlank { null } ?: if (kind == RestaurantMasterFormKind.Dish) "480" else "")
    }
    var fullPrice by remember { mutableStateOf("720") }
    var purchasePrice by remember(existing?.id) {
        mutableStateOf(existing?.purchasePrice?.ifBlank { null } ?: if (kind == RestaurantMasterFormKind.Raw) "480" else "")
    }
    var gstChip by remember(existing?.id) {
        mutableStateOf(
            when (kind) {
                RestaurantMasterFormKind.Dish -> when (existing?.gstRate) {
                    "18" -> "18% + ITC"
                    "0", null -> if (existing?.gstRate == "0") "Exempt" else "Rest. 5%"
                    else -> "Rest. 5%"
                }
                RestaurantMasterFormKind.Raw -> "${existing?.gstRate?.ifBlank { "5" } ?: "5"}%"
                else -> "5%"
            },
        )
    }
    var hsn by remember(existing?.id) {
        mutableStateOf(
            existing?.hsn?.ifBlank { null }
                ?: when (kind) {
                    RestaurantMasterFormKind.Dish -> "996331 · Restaurant service"
                    RestaurantMasterFormKind.Raw -> "0406 · Dairy / cheese"
                    else -> ""
                },
        )
    }
    var variant by remember { mutableStateOf("Half / Full") }
    var stationRoute by remember { mutableStateOf("Tandoor") }
    var dishType by remember { mutableStateOf("Non-veg") }
    var modifiers by remember { mutableStateOf("Spice level · less oil · note") }
    var defaultLocation by remember { mutableStateOf("Cold store") }
    var parQty by remember { mutableStateOf("") }
    var preferredSupplier by remember { mutableStateOf("") }
    var stationType by remember { mutableStateOf("Kitchen") }
    var printer by remember { mutableStateOf("Kitchen printer 1") }
    var displayOrder by remember { mutableStateOf("5") }
    var routeCats by remember { mutableStateOf(setOf("Starters")) }
    var stationLead by remember { mutableStateOf("Chef Imran") }
    var tracked by remember { mutableStateOf("Track stock") }

    val candidates = remember(model.masters, kind) {
        model.masters.orEmpty().filter { masterMatchesRestaurantKind(it, kind) }
    }
    val suppliers = remember(model.masters) {
        model.masters.orEmpty().filter {
            it.kind.uppercase() in setOf("VENDOR", "SUPPLIER")
        }
    }
    val locations = remember(model.masters) {
        model.masters.orEmpty().filter {
            it.kind.uppercase() in setOf("STATION", "LOCATION", "STORE", "KITCHEN_STATION")
        }
    }

    fun hydrate(m: OperatingMasterDto) {
        editing = m
        name = m.name
        gstin = m.gstin.orEmpty()
        unit = m.unit ?: unit
        sellPrice = m.sellPrice.orEmpty()
        purchasePrice = m.purchasePrice.orEmpty()
        hsn = m.hsn.orEmpty()
        status = when (m.status?.uppercase()) {
            "INACTIVE", "ARCHIVED" -> "Inactive"
            "PAUSED" -> "Paused"
            "PREFERRED" -> "Preferred"
            else -> "Active"
        }
        gstChip = when (kind) {
            RestaurantMasterFormKind.Dish -> when (m.gstRate) {
                "18" -> "18% + ITC"
                "0" -> "Exempt"
                else -> "Rest. 5%"
            }
            RestaurantMasterFormKind.Raw -> "${m.gstRate?.ifBlank { "5" } ?: "5"}%"
            else -> gstChip
        }
    }

    fun clearForCreate() {
        editing = null
        name = ""; legalName = ""; gstin = ""; phone = ""; email = ""
        gstRegistration = "Regular"
        category = when (kind) {
            RestaurantMasterFormKind.Dish -> "Mains"
            RestaurantMasterFormKind.Raw -> "Dairy"
            RestaurantMasterFormKind.Vendor -> "Meat & Fish"
            RestaurantMasterFormKind.Station -> "Kitchen"
        }
        paymentTerms = "Net 7 days"; address = ""
        status = "Active"
        unit = if (kind == RestaurantMasterFormKind.Raw) "kg" else "pcs"
        sellPrice = if (kind == RestaurantMasterFormKind.Dish) "480" else ""
        fullPrice = "720"
        purchasePrice = if (kind == RestaurantMasterFormKind.Raw) "480" else ""
        gstChip = when (kind) {
            RestaurantMasterFormKind.Dish -> "Rest. 5%"
            RestaurantMasterFormKind.Raw -> "5%"
            else -> "5%"
        }
        hsn = when (kind) {
            RestaurantMasterFormKind.Dish -> "996331 · Restaurant service"
            RestaurantMasterFormKind.Raw -> "0406 · Dairy / cheese"
            else -> ""
        }
        variant = "Half / Full"; stationRoute = "Tandoor"; dishType = "Non-veg"
        modifiers = "Spice level · less oil · note"
        defaultLocation = "Cold store"; parQty = ""; preferredSupplier = ""
        stationType = "Kitchen"; printer = "Kitchen printer 1"
        displayOrder = "5"; routeCats = setOf("Starters"); stationLead = "Chef Imran"
        tracked = "Track stock"
    }

    val postable = name.isNotBlank()
    val brand = RestaurantCaptureCopy.posBrand(model.title)
    val subtitle = when (kind) {
        RestaurantMasterFormKind.Dish ->
            "Master item · typed fields + GST before it appears in Sales Order"
        RestaurantMasterFormKind.Raw ->
            "Master stock item · unit, rate, HSN & GST before Purchase Order can add it"
        RestaurantMasterFormKind.Vendor ->
            "Supplier master · GSTIN drives input credit on Purchase / GRN"
        RestaurantMasterFormKind.Station ->
            "Kitchen & service stations · KOT routing target"
    }
    val why = when (kind) {
        RestaurantMasterFormKind.Dish -> RestaurantCaptureCopy.DISH_WHY
        RestaurantMasterFormKind.Raw -> RestaurantCaptureCopy.RAW_WHY
        RestaurantMasterFormKind.Vendor -> RestaurantCaptureCopy.VENDOR_FORM_WHY
        RestaurantMasterFormKind.Station -> RestaurantCaptureCopy.STATION_FORM_WHY
    }
    val caveat = when (kind) {
        RestaurantMasterFormKind.Vendor -> "Vendor saved · available in Purchase Order"
        RestaurantMasterFormKind.Station -> "Station saved · KOT routing updated"
        else -> "Master saved · available in browser on next open"
    }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        EmberSheetGrabber()
        EmberSheetHeader(
            title = if (isCreate) kind.title else "Edit ${kind.masterKind.lowercase().replace('_', ' ')}",
            subtitle = subtitle,
            onClose = onDismiss,
        )

        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            EmberModeChip("Create", selected = isCreate, onClick = { isCreate = true; clearForCreate() })
            EmberModeChip("Edit existing", selected = !isCreate, onClick = { isCreate = false })
        }

        if (kind == RestaurantMasterFormKind.Dish || kind == RestaurantMasterFormKind.Raw) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                EmberModeChip(brand, selected = true, selectedFill = RestaurantEmber.wine, onClick = null)
                EmberModeChip(if (kind == RestaurantMasterFormKind.Dish) "Menu master" else "Stock master", selected = false, onClick = null)
                EmberModeChip("GST-aware", selected = false, onClick = null)
            }
        }

        if (!isCreate) {
            if (candidates.isEmpty()) {
                EmberEmpty("No masters yet", "Create one first, then edit from here.")
            } else {
                candidates.take(8).forEach { row ->
                    EmberChevronField(row.kind, row.name, onClick = { hydrate(row) })
                }
            }
        }

        when (kind) {
            RestaurantMasterFormKind.Dish -> {
                EmberFieldLabel("Dish name")
                EmberTextField(name, "e.g. Butter chicken") { name = it.take(80) }
                EmberFieldLabel("Category")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    RestaurantCaptureCopy.menuCategories.forEach {
                        EmberModeChip(it, selected = category == it, onClick = { category = it })
                    }
                }
                EmberFieldLabel("Variants")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    RestaurantCaptureCopy.dishVariants.forEach {
                        EmberModeChip(it, selected = variant == it, onClick = { variant = it })
                    }
                }
                priceHeroCard("Sell price", RestaurantFormat.exact(model.currency ?: "INR", sellPrice.ifBlank { "480" }), "Half plate default · Full can differ")
                EmberFieldLabel("Full price (optional)")
                EmberTextField(fullPrice, "₹720") { fullPrice = it.filter { ch -> ch.isDigit() || ch == '.' }.take(12) }
                EmberFieldLabel("GST on dish")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    RestaurantCaptureCopy.dishGstChips.forEach {
                        EmberModeChip(it, selected = gstChip == it, onClick = { gstChip = it })
                    }
                }
                Text(
                    "5% restaurant rate usually without ITC. Bill inherits this; do not invent a second GST line.",
                    style = RestaurantEmber.rowCaption().copy(fontSize = 11.sp),
                )
                EmberFieldLabel("HSN / SAC")
                EmberTextField(hsn, "996331 · Restaurant service") { hsn = it.take(40) }
                com.example.momentra.ui.shell.business.shared.HsnLookupHints(
                    query = hsn,
                    onPick = { code, rate ->
                        hsn = code
                        if (!rate.isNullOrBlank()) {
                            gstChip = when (rate) {
                                "0" -> "Exempt"
                                "18" -> "18% + ITC"
                                "5" -> "Rest. 5%"
                                else -> "$rate%"
                            }
                        }
                    },
                    accent = RestaurantEmber.ember,
                    captionStyle = RestaurantEmber.rowCaption(),
                    titleStyle = RestaurantEmber.rowTitle(),
                )
                EmberFieldLabel("Kitchen station")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    RestaurantCaptureCopy.kitchenStations.forEach {
                        EmberModeChip(it, selected = stationRoute == it, onClick = { stationRoute = it })
                    }
                }
                EmberFieldLabel("Type")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    RestaurantCaptureCopy.dishTypes.forEach {
                        EmberModeChip(it, selected = dishType == it, onClick = { dishType = it })
                    }
                }
                EmberFieldLabel("Modifiers allowed")
                EmberTextField(modifiers, "Spice level · less oil · note") { modifiers = it.take(80) }
                EmberLockedField("Linked recipe (optional)", "Not set · v5 exclusion", "Recipe deduction excluded in v5")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    EmberModeChip("Active", selected = status == "Active", selectedFill = RestaurantEmber.positive, onClick = { status = "Active" })
                    EmberModeChip("Inactive", selected = status == "Inactive", onClick = { status = "Inactive" })
                }
            }

            RestaurantMasterFormKind.Raw -> {
                EmberFieldLabel("Material name")
                EmberTextField(name, "e.g. Paneer") { name = it.take(80) }
                EmberFieldLabel("Category")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    RestaurantCaptureCopy.rawCategories.forEach {
                        EmberModeChip(it, selected = category == it, onClick = { category = it })
                    }
                }
                EmberFieldLabel("Unit of measure")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    RestaurantCaptureCopy.rawUnits.forEach {
                        EmberModeChip(it, selected = unit == it, onClick = { unit = it })
                    }
                }
                priceHeroCard(
                    "Purchase rate",
                    RestaurantFormat.exact(model.currency ?: "INR", purchasePrice.ifBlank { "480" }),
                    "Default rate on PO lines · editable per order",
                    badge = "INR / unit",
                )
                EmberFieldLabel("GST on purchase")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    RestaurantCaptureCopy.rawGstChips.forEach {
                        EmberModeChip(it, selected = gstChip == it, onClick = { gstChip = it })
                    }
                }
                Text(
                    "Input GST on Purchase / GRN. PO commitment can show estimate; it does not post tax.",
                    style = RestaurantEmber.rowCaption().copy(fontSize = 11.sp),
                )
                EmberFieldLabel("HSN code")
                EmberTextField(hsn, "0406 · Dairy / cheese") { hsn = it.take(40) }
                com.example.momentra.ui.shell.business.shared.HsnLookupHints(
                    query = hsn,
                    onPick = { code, rate ->
                        hsn = code
                        if (!rate.isNullOrBlank()) gstChip = "$rate%"
                    },
                    accent = RestaurantEmber.ember,
                    captionStyle = RestaurantEmber.rowCaption(),
                    titleStyle = RestaurantEmber.rowTitle(),
                )
                EmberChevronField(
                    "Default location",
                    defaultLocation,
                    onClick = {
                        val opts = locations.map { it.name }.ifEmpty { listOf("Cold store", "Dry store", "Freezer") }
                        val idx = opts.indexOf(defaultLocation)
                        defaultLocation = opts[(idx + 1).coerceAtLeast(0) % opts.size]
                    },
                )
                EmberFieldLabel("Par / reorder qty")
                EmberTextField(parQty, "5 kg") { parQty = it.take(24) }
                EmberChevronField(
                    "Preferred supplier",
                    preferredSupplier.ifBlank { suppliers.firstOrNull()?.name ?: "Fresh Farm" },
                    onClick = {
                        val names = suppliers.map { it.name }.ifEmpty { listOf("Fresh Farm", "Metro Dairy") }
                        val idx = names.indexOf(preferredSupplier)
                        preferredSupplier = names[(idx + 1).coerceAtLeast(0) % names.size]
                    },
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    EmberModeChip("Track stock", selected = tracked == "Track stock", selectedFill = RestaurantEmber.positive, onClick = { tracked = "Track stock" })
                    EmberModeChip("Non-tracked", selected = tracked != "Track stock", onClick = { tracked = "Non-tracked" })
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    EmberModeChip("Active", selected = status == "Active", selectedFill = RestaurantEmber.positive, onClick = { status = "Active" })
                    EmberModeChip("Inactive", selected = status == "Inactive", onClick = { status = "Inactive" })
                }
            }

            RestaurantMasterFormKind.Vendor -> {
                EmberFieldLabel("Vendor name")
                EmberTextField(name, "e.g. Coastal Fish Co.") { name = it.take(80) }
                EmberFieldLabel("Legal / trade name")
                EmberTextField(legalName, "As on GST certificate") { legalName = it.take(80) }
                EmberFieldLabel("GSTIN")
                EmberTextField(gstin.ifBlank { "27AAACC1234D1Z7" }, "27AAACC1234D1Z7") {
                    gstin = it.take(15).uppercase()
                }
                Text(
                    "15 chars · state code from GSTIN · local check only",
                    style = RestaurantEmber.rowCaption(RestaurantEmber.textFaint).copy(fontSize = 11.sp),
                )
                EmberFieldLabel("GST registration")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Regular", "Composition", "Unregistered").forEach {
                        EmberModeChip(it, selected = gstRegistration == it, onClick = { gstRegistration = it })
                    }
                }
                EmberFieldLabel("Phone")
                EmberTextField(phone.ifBlank { "+91 98200 12345" }, "+91 98200 12345") { phone = it.take(20) }
                EmberFieldLabel("Email (optional)")
                EmberTextField(email, "orders@example.in") { email = it.take(60) }
                EmberFieldLabel("Category")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    RestaurantCaptureCopy.vendorCategories.forEach {
                        EmberModeChip(it, selected = category == it, onClick = { category = it })
                    }
                }
                EmberFieldLabel("Payment terms")
                EmberTextField(paymentTerms, "Net 7 days") { paymentTerms = it.take(40) }
                EmberFieldLabel("Address")
                EmberTextField(address.ifBlank { "Sassoon Dock, Colaba, Mumbai" }, "Address") { address = it.take(120) }
                EmberFieldLabel("Status")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Active", "Preferred", "Inactive").forEach {
                        EmberModeChip(it, selected = status == it, onClick = { status = it })
                    }
                }
            }

            RestaurantMasterFormKind.Station -> {
                EmberFieldLabel("Station name")
                EmberTextField(name, "e.g. Grill") { name = it.take(80) }
                EmberFieldLabel("Type")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RestaurantCaptureCopy.stationTypes.forEach {
                        EmberModeChip(it, selected = stationType == it, onClick = { stationType = it })
                    }
                }
                EmberFieldLabel("KOT printer")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    RestaurantCaptureCopy.stationPrinters.forEach {
                        EmberModeChip(it, selected = printer == it, onClick = { printer = it })
                    }
                }
                Text(
                    "Printer routing is chrome until a kitchen-print endpoint lands — Screen only always works.",
                    style = RestaurantEmber.rowCaption(RestaurantEmber.amber).copy(fontSize = 10.sp),
                )
                EmberFieldLabel("Display order")
                EmberTextField(displayOrder, "5") { displayOrder = it.filter { ch -> ch.isDigit() }.take(3) }
                EmberFieldLabel("Routes dish categories")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    RestaurantCaptureCopy.menuCategories.forEach { cat ->
                        EmberModeChip(
                            cat,
                            selected = cat in routeCats,
                            onClick = {
                                routeCats = if (cat in routeCats) routeCats - cat else routeCats + cat
                            },
                        )
                    }
                }
                EmberFieldLabel("Station lead (optional)")
                EmberTextField(stationLead, "Chef Imran") { stationLead = it.take(40) }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    EmberModeChip("Active", selected = status == "Active", onClick = { status = "Active" })
                    EmberModeChip("Paused", selected = status == "Paused", onClick = { status = "Paused" })
                }
            }
        }

        EmberWhyNote(why, tinted = true)
        val ctaLabel = when {
            showToast -> "Saving…"
            kind == RestaurantMasterFormKind.Dish && name.isNotBlank() -> "${kind.cta} · $name"
            kind == RestaurantMasterFormKind.Raw && name.isNotBlank() -> "${kind.cta} · $name"
            kind == RestaurantMasterFormKind.Vendor && name.isNotBlank() -> "${kind.cta} · $name"
            kind == RestaurantMasterFormKind.Station && name.isNotBlank() -> "${kind.cta} · $name"
            else -> kind.cta
        }
        EmberSheetFooter(ctaLabel, postable, caveat) {
            val gstRate = when (kind) {
                RestaurantMasterFormKind.Dish -> when (gstChip) {
                    "18% + ITC" -> "18"
                    "Exempt" -> "0"
                    else -> "5"
                }
                RestaurantMasterFormKind.Raw -> gstChip.removeSuffix("%")
                else -> null
            }
            val meta = buildList {
                add("Category: $category")
                when (kind) {
                    RestaurantMasterFormKind.Dish -> {
                        add("Variant: $variant"); add("Station: $stationRoute"); add("Type: $dishType")
                        add("Modifiers: $modifiers"); fullPrice.takeIf { it.isNotBlank() }?.let { add("Full: $it") }
                    }
                    RestaurantMasterFormKind.Raw -> {
                        add("Location: $defaultLocation"); parQty.takeIf { it.isNotBlank() }?.let { add("Par: $it") }
                        preferredSupplier.takeIf { it.isNotBlank() }?.let { add("Supplier: $it") }
                    }
                    RestaurantMasterFormKind.Vendor -> {
                        legalName.takeIf { it.isNotBlank() }?.let { add("Legal: $it") }
                        phone.takeIf { it.isNotBlank() }?.let { add("Phone: $it") }
                        email.takeIf { it.isNotBlank() }?.let { add("Email: $it") }
                        add("Terms: $paymentTerms"); address.takeIf { it.isNotBlank() }?.let { add(it) }
                        add("Status: $status")
                    }
                    RestaurantMasterFormKind.Station -> {
                        add("Type: $stationType"); add("Printer: $printer")
                        add("Order: $displayOrder"); add("Routes: ${routeCats.joinToString(",")}")
                        stationLead.takeIf { it.isNotBlank() }?.let { add("Lead: $it") }
                    }
                }
            }.joinToString(" · ")
            val body = OperatingMasterBody(
                kind = kind.masterKind,
                name = name.trim(),
                code = legalName.takeIf { it.isNotBlank() },
                unit = unit.takeIf { it.isNotBlank() },
                sellPrice = sellPrice.takeIf { it.isNotBlank() },
                purchasePrice = purchasePrice.takeIf { it.isNotBlank() },
                gstRate = gstRate,
                gstApplicable = if (gstRate != null && gstRate != "0") "YES" else if (gstRate == "0") "NO" else null,
                gstClassification = when (gstRate) {
                    null -> null
                    "0" -> "EXEMPT"
                    else -> "TAXABLE"
                },
                gstRegistration = if (kind == RestaurantMasterFormKind.Vendor) {
                    when (gstRegistration) {
                        "Composition" -> "COMPOSITION"
                        "Unregistered" -> "UNREGISTERED"
                        else -> "REGULAR"
                    }
                } else null,
                hsn = hsn.filter { it.isDigit() }.take(8).ifBlank { null },
                gstin = gstin.takeIf { it.isNotBlank() },
                tracked = if (kind == RestaurantMasterFormKind.Raw) tracked == "Track stock" else null,
                parentMasterId = suppliers.firstOrNull {
                    it.name.equals(preferredSupplier, true)
                }?.id?.takeIf { kind == RestaurantMasterFormKind.Raw },
            )
            showToast = true
            onSubmit(
                RestaurantEventDraft(
                    eventType = "UPDATE",
                    title = name.trim(),
                    amount = null,
                    note = meta,
                    channelName = stationRoute.takeIf { kind == RestaurantMasterFormKind.Dish },
                    counterpartyName = preferredSupplier.takeIf { it.isNotBlank() },
                    paymentStatus = null,
                    masterKind = body.kind,
                    master = body,
                ),
            )
            onSaved?.invoke()
        }
        if (showToast) {
            EmberSavedToast(RestaurantCaptureCopy.SAVED_TOAST)
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun priceHeroCard(label: String, value: String, caption: String, badge: String = "INR") {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(RestaurantEmber.chip)
            .border(1.5.dp, RestaurantEmber.ember, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = RestaurantEmber.rowCaption().copy(fontSize = 12.sp))
            Text(badge, style = RestaurantEmber.rowCaption(RestaurantEmber.gold).copy(fontSize = 11.sp, fontWeight = FontWeight.Bold))
        }
        Text(value, style = RestaurantEmber.heroMetric().copy(fontSize = 32.sp))
        Text(caption, style = RestaurantEmber.rowCaption().copy(fontSize = 11.sp))
    }
}

@Composable
internal fun EmberSavedToast(message: String) {
    Row(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(RestaurantEmber.card)
            .border(1.dp, RestaurantEmber.gold, RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("✓", color = RestaurantEmber.gold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Text(message.removePrefix("✓ ").trim(), style = RestaurantEmber.rowTitle().copy(fontSize = 12.sp))
    }
}

private fun masterMatchesRestaurantKind(master: OperatingMasterDto, kind: RestaurantMasterFormKind): Boolean {
    val k = master.kind.uppercase()
    return when (kind) {
        RestaurantMasterFormKind.Dish -> k in setOf("MENU_DISH", "DISH", "MENU_ITEM", "PRODUCT")
        RestaurantMasterFormKind.Raw -> k in setOf("MATERIAL", "RAW", "STOCK_ITEM", "INGREDIENT", "CONSUMABLE")
        RestaurantMasterFormKind.Vendor -> k in setOf("VENDOR", "SUPPLIER")
        RestaurantMasterFormKind.Station -> k in setOf("STATION", "KITCHEN_STATION", "LOCATION")
    }
}
