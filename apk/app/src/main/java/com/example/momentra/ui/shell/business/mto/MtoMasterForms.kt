package com.example.momentra.ui.shell.business.mto
import com.example.momentra.ui.shell.business.manufacturing.ManufacturingEventDraft

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.momentra.data.api.OperatingProjectionDto

@Composable
internal fun MtoMasterFormSheet(
    form: MtoMasterForm,
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onSubmit: (ManufacturingEventDraft) -> Unit,
) {
    when (form) {
        MtoMasterForm.VENDOR -> MtoVendorForm(model, onDismiss, onSubmit)
        MtoMasterForm.LINE_STORE -> MtoLineStoreForm(model, onDismiss, onSubmit)
        MtoMasterForm.PART -> MtoPartForm(model, onDismiss, onSubmit)
        MtoMasterForm.BOM -> MtoBomForm(model, onDismiss, onSubmit)
    }
}

@Composable
private fun MtoVendorForm(model: OperatingProjectionDto, onDismiss: () -> Unit, onSubmit: (ManufacturingEventDraft) -> Unit) {
    var isCreate by remember { mutableStateOf(true) }
    var name by remember { mutableStateOf("") }
    var legal by remember { mutableStateOf("") }
    var gstin by remember { mutableStateOf("") }
    var gstReg by remember { mutableStateOf("Regular") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("Customer") }
    var terms by remember { mutableStateOf("Net 30") }
    var address by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Active") }
    // D9: lock Edit when no hydrate API for party update
    val editLocked = true
    val masterKind = when (role) {
        "Supplier" -> "SUPPLIER"
        "Job work" -> "VENDOR"
        else -> "CUSTOMER"
    }
    MtoSheetBody(Modifier.verticalScroll(rememberScrollState())) {
        MtoGrabber()
        MtoSheetHeader("New customer / supplier", "Party master · role · GSTIN", onClose = onDismiss)
        MtoCreateEditMode(isCreate = isCreate, onCreate = { isCreate = true }, onEdit = { isCreate = false }, editLocked = editLocked)
        MtoTextField(name, "Party name", onValueChange = { name = it })
        MtoTextField(legal, "Legal / trade name", onValueChange = { legal = it })
        MtoTextField(gstin, "GSTIN", onValueChange = { gstin = it.take(15).uppercase() })
        MtoChipRow("GST registration", listOf("Regular", "Composition", "Unregistered"), gstReg) { gstReg = it }
        MtoTextField(phone, "Phone", onValueChange = { phone = it })
        MtoTextField(email, "Email optional", onValueChange = { email = it })
        MtoChipRow("Role", listOf("Customer", "Supplier", "Job work", "Spares", "Consumables"), role) { role = it }
        MtoTextField(terms, "Payment terms", onValueChange = { terms = it })
        MtoTextField(address, "Address", onValueChange = { address = it }, singleLine = false)
        MtoChipRow("Status", listOf("Active", "Preferred", "Inactive"), status) { status = it }
        MtoWhyNote("Party master save does not post GST, AP, or stock. GSTIN is identity for later invoices.")
        MtoStickyFooter(label = "Save party", enabled = name.isNotBlank(), caveat = "Sheet closes and returns back", onClick = {
            onSubmit(
                ManufacturingEventDraft(
                    eventType = "UPDATE",
                    title = name,
                    amount = null,
                    note = "role=$role · terms=$terms · status=$status · $address",
                    channelName = null,
                    counterpartyName = name,
                    paymentStatus = null,
                    masterKind = masterKind,
                    itemName = name,
                    masterGstin = gstin.ifBlank { null },
                    masterGstRegistration = gstReg.uppercase(),
                ),
            )
        })
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun MtoLineStoreForm(model: OperatingProjectionDto, onDismiss: () -> Unit, onSubmit: (ManufacturingEventDraft) -> Unit) {
    var isCreate by remember { mutableStateOf(true) }
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Line") }
    var printer by remember { mutableStateOf("Tablet only") }
    var holds by remember { mutableStateOf("Raw") }
    var status by remember { mutableStateOf("Active") }
    MtoSheetBody(Modifier.verticalScroll(rememberScrollState())) {
        MtoGrabber()
        MtoSheetHeader("Add line / store", "Capacity · Active/Planned", onClose = onDismiss)
        MtoCreateEditMode(isCreate, { isCreate = true }, { isCreate = false }, editLocked = true)
        MtoTextField(name, "Name", onValueChange = { name = it })
        MtoChipRow("Type", listOf("Store", "Line", "Station"), type) { type = it }
        MtoChipRow("Job-card printer", listOf("Shop printer 1", "Label printer", "Tablet only"), printer) { printer = it }
        MtoChipRow("Holds stock", listOf("Raw", "WIP", "Finished", "Consumables", "Spares"), holds) { holds = it }
        MtoChipRow("Status", listOf("Active", "Paused"), status) { status = it }
        MtoLockedChip("Print SDK · chrome only")
        MtoWhyNote("Line/store master is capacity chrome. Job-card printer chips are UI-only until a print stack ships.")
        MtoStickyFooter(label = "Save line / store", enabled = name.isNotBlank(), caveat = "Sheet closes and returns back", onClick = {
            onSubmit(
                ManufacturingEventDraft(
                    eventType = "UPDATE",
                    title = name,
                    amount = null,
                    note = "type=$type · printer=$printer · holds=$holds · status=$status",
                    channelName = null,
                    counterpartyName = null,
                    paymentStatus = null,
                    masterKind = "LOCATION",
                    itemName = name,
                ),
            )
        })
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun MtoPartForm(model: OperatingProjectionDto, onDismiss: () -> Unit, onSubmit: (ManufacturingEventDraft) -> Unit) {
    var isCreate by remember { mutableStateOf(true) }
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Finished") }
    var partType by remember { mutableStateOf("Finished") }
    var sell by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }
    var gst by remember { mutableStateOf("18%") }
    var hsn by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("nos") }
    var defaultStore by remember { mutableStateOf("Job FG bay") }
    var drawing by remember { mutableStateOf("") }
    var trackBatch by remember { mutableStateOf("Track batch/heat") }
    var status by remember { mutableStateOf("Active") }
    val currency = model.currency ?: "INR"
    MtoSheetBody(Modifier.verticalScroll(rememberScrollState())) {
        MtoGrabber()
        MtoSheetHeader("Create part / SKU", "Parts master · GST-aware labels", onClose = onDismiss)
        MtoCreateEditMode(isCreate, { isCreate = true }, { isCreate = false }, editLocked = true)
        RowChipsContext()
        MtoTextField(name, "Part name", onValueChange = { name = it })
        MtoChipRow("Category", listOf("Raw", "WIP", "Finished", "Consumables", "Spares"), category) { category = it }
        MtoChipRow("Part type", listOf("Finished", "Raw", "WIP"), partType) { partType = it }
        MtoAmountHero("Sell price", if (sell.isBlank()) "—" else MtoFormat.exact(currency, sell), currencyBadge = currency)
        MtoNumericKeypad(sell) { sell = it }
        MtoTextField(cost, "Standard cost", onValueChange = { cost = it.filter { ch -> ch.isDigit() || ch == '.' }.take(12) })
        MtoChipRow("GST on part", listOf("18%", "5%", "Exempt"), gst) { gst = it }
        MtoTextField(hsn, "HSN / SAC", onValueChange = { hsn = it })
        MtoChipRow(
            "Default store",
            listOf("Job FG bay", "Job material store", "WIP rack", "QC bay"),
            defaultStore,
        ) { defaultStore = it }
        MtoChipRow("Unit", listOf("nos", "kg", "set"), unit) { unit = it }
        MtoTextField(drawing, "Drawing / revision", onValueChange = { drawing = it })
        MtoChipRow(null, listOf("Track batch/heat", "No batch"), trackBatch) { trackBatch = it }
        MtoChipRow("Status", listOf("Active", "Inactive"), status) { status = it }
        MtoWhyNote("Part master save does not post GST. Rate chips are labels — tax posts on Sale invoice / GRN only.")
        MtoStickyFooter(label = "Save part", enabled = name.isNotBlank(), caveat = "Sheet closes and returns back", onClick = {
            onSubmit(
                ManufacturingEventDraft(
                    eventType = "UPDATE",
                    title = name,
                    amount = sell.ifBlank { null },
                    note = "category=$category · type=$partType · cost=$cost · store=$defaultStore · drawing=$drawing · $trackBatch · $status",
                    channelName = defaultStore,
                    counterpartyName = null,
                    paymentStatus = null,
                    masterKind = "SKU",
                    itemName = name,
                    unit = unit,
                    unitPrice = sell.ifBlank { null },
                    masterHsn = hsn.ifBlank { null },
                    masterGstRate = gst.removeSuffix("%").takeIf { gst != "Exempt" },
                    masterGstApplicable = if (gst == "Exempt") "EXEMPT" else "TAXABLE",
                    masterGstClassification = category.uppercase(),
                ),
            )
        })
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun RowChipsContext() {
    MtoChipRow(null, listOf("live moment ERP", "Parts master", "GST-aware"), "Parts master") {}
}

@Composable
private fun MtoBomForm(model: OperatingProjectionDto, onDismiss: () -> Unit, onSubmit: (ManufacturingEventDraft) -> Unit) {
    var isCreate by remember { mutableStateOf(true) }
    var finished by remember { mutableStateOf("") }
    var routing by remember { mutableStateOf("Cut") }
    var uom by remember { mutableStateOf("nos") }
    var gst by remember { mutableStateOf("18%") }
    var hsn by remember { mutableStateOf("") }
    var batch by remember { mutableStateOf("1") }
    var issueFrom by remember { mutableStateOf("Job material store") }
    var defaultLine by remember { mutableStateOf("Machining cell 1") }
    var scrapPct by remember { mutableStateOf("2") }
    var issue by remember { mutableStateOf("Backflush on accept") }
    var status by remember { mutableStateOf("Active") }
    val components = remember { mutableStateListOf("CRC sheet", "M8 bolt set") }
    val parts = model.masters.orEmpty().filter {
        it.kind.equals("SKU", true) || it.kind.equals("PART", true) || it.kind.equals("PRODUCT", true)
    }
    val stores = model.masters.orEmpty().filter {
        it.kind.equals("LOCATION", true) || it.kind.equals("STORE", true) || it.kind.equals("LINE", true)
    }
    // D6: UI-local component list when BOM explosion API absent
    MtoSheetBody(Modifier.verticalScroll(rememberScrollState())) {
        MtoGrabber()
        MtoSheetHeader("Create BOM / job item", "Job BOM · routing · scrap · backflush", onClose = onDismiss)
        MtoCreateEditMode(isCreate, { isCreate = true }, { isCreate = false }, editLocked = true)
        MtoPickerRow("Finished part", finished.ifBlank { "Select FG" }, onClick = {
            finished = parts.firstOrNull()?.name ?: finished
        }, externalLabel = true)
        parts.take(5).forEach { MtoPickerRow(it.name, it.unit ?: "nos", onClick = { finished = it.name }) }
        MtoChipRow("Routing", listOf("Cut", "Machine", "Plate (out)", "QC", "Pack", "Dispatch"), routing) { routing = it }
        MtoChipRow("Output UOM", listOf("nos", "kg", "set"), uom) { uom = it }
        MtoAmountHero("Rolled-up cost", "Est. · UI local", caption = "Components below are local until BOM write API accepts lines", currencyBadge = "Est.")
        MtoChipRow("GST on FG", listOf("18%", "5%", "Exempt"), gst) { gst = it }
        MtoTextField(hsn, "HSN (FG)", onValueChange = { hsn = it })
        MtoPickerRow("Issue from", issueFrom, onClick = {
            issueFrom = stores.firstOrNull()?.name ?: issueFrom
        }, externalLabel = true)
        stores.take(3).forEach { MtoPickerRow(it.name, it.kind, onClick = { issueFrom = it.name }) }
        MtoTextField(batch, "Batch size", onValueChange = { batch = it.filter { ch -> ch.isDigit() }.take(6) })
        MtoPickerRow("Default line", defaultLine, onClick = {
            defaultLine = stores.firstOrNull { it.kind.equals("LINE", true) }?.name
                ?: stores.firstOrNull()?.name
                ?: defaultLine
        }, externalLabel = true)
        MtoTextField(scrapPct, "Scrap %", onValueChange = { scrapPct = it.filter { ch -> ch.isDigit() || ch == '.' }.take(5) })
        MtoChipRow(null, listOf("Backflush on accept", "Manual issue"), issue) { issue = it }
        MtoChipRow("Status", listOf("Active", "Inactive"), status) { status = it }
        MtoLinesCard("Components", components.size) {
            components.forEach { MtoPickerRow(it, "UI local", onClick = null) }
            androidx.compose.material3.Text(
                "+ Add component",
                style = com.example.momentra.ui.theme.MadeToOrderAmber.viewAll(),
                modifier = Modifier.clickable {
                    parts.firstOrNull { p -> components.none { it == p.name } }?.let { components += it.name }
                },
            )
        }
        MtoWhyNote("Save BOM stores supported master fields only. Stock moves only on issue / accepted production — never on BOM save. GST is informational.")
        MtoStickyFooter(label = "Save BOM", enabled = finished.isNotBlank(), caveat = "Sheet closes and returns back", onClick = {
            onSubmit(
                ManufacturingEventDraft(
                    eventType = "UPDATE",
                    title = "BOM · $finished",
                    amount = null,
                    note = "routing=$routing · uom=$uom · batch=$batch · issueFrom=$issueFrom · line=$defaultLine · scrap=$scrapPct% · $issue · components=${components.joinToString()} · $status",
                    channelName = defaultLine,
                    counterpartyName = issueFrom,
                    paymentStatus = null,
                    masterKind = "BOM",
                    itemName = finished,
                    unit = uom,
                    masterHsn = hsn.ifBlank { null },
                    masterGstRate = gst.removeSuffix("%").takeIf { gst != "Exempt" },
                ),
            )
        })
        Spacer(Modifier.height(16.dp))
    }
}
