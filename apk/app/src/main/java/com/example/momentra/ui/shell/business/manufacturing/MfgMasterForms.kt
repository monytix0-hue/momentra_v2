package com.example.momentra.ui.shell.business.manufacturing

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
internal fun MfgMasterFormSheet(
    form: MfgMasterForm,
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onSubmit: (ManufacturingEventDraft) -> Unit,
) {
    when (form) {
        MfgMasterForm.VENDOR -> MfgVendorForm(model, onDismiss, onSubmit)
        MfgMasterForm.LINE_STORE -> MfgLineStoreForm(model, onDismiss, onSubmit)
        MfgMasterForm.PART -> MfgPartForm(model, onDismiss, onSubmit)
        MfgMasterForm.BOM -> MfgBomForm(model, onDismiss, onSubmit)
    }
}

@Composable
private fun MfgVendorForm(model: OperatingProjectionDto, onDismiss: () -> Unit, onSubmit: (ManufacturingEventDraft) -> Unit) {
    var isCreate by remember { mutableStateOf(true) }
    var name by remember { mutableStateOf("") }
    var legal by remember { mutableStateOf("") }
    var gstin by remember { mutableStateOf("") }
    var gstReg by remember { mutableStateOf("Regular") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var supplies by remember { mutableStateOf("Steel") }
    var terms by remember { mutableStateOf("Net 30") }
    var address by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Active") }
    // D10: lock Edit when no hydrate API for MFG vendor update
    val editLocked = true
    MfgSheetBody(Modifier.verticalScroll(rememberScrollState())) {
        MfgGrabber()
        MfgSheetHeader("New vendor", "Vendors & buyers master", onClose = onDismiss)
        MfgCreateEditMode(isCreate = isCreate, onCreate = { isCreate = true }, onEdit = { isCreate = false }, editLocked = editLocked)
        MfgTextField(name, "Vendor name", onValueChange = { name = it })
        MfgTextField(legal, "Legal / trade name", onValueChange = { legal = it })
        MfgTextField(gstin, "GSTIN", onValueChange = { gstin = it.take(15).uppercase() })
        MfgChipRow("GST registration", listOf("Regular", "Composition", "Unregistered"), gstReg) { gstReg = it }
        MfgTextField(phone, "Phone", onValueChange = { phone = it })
        MfgTextField(email, "Email optional", onValueChange = { email = it })
        MfgChipRow("Supplies", listOf("Steel", "Raw", "Finished", "Spares", "Consumables", "Job work"), supplies) { supplies = it }
        MfgTextField(terms, "Payment terms", onValueChange = { terms = it })
        MfgTextField(address, "Address", onValueChange = { address = it }, singleLine = false)
        MfgChipRow("Status", listOf("Active", "Preferred", "Inactive"), status) { status = it }
        MfgWhyBox("Vendor master save does not post GST, AP, or stock. GSTIN is identity for later invoices.")
        MfgStickyFooter(label = "Save vendor", enabled = name.isNotBlank(), caveat = "Sheet closes and returns back", onClick = {
            onSubmit(
                ManufacturingEventDraft(
                    eventType = "UPDATE",
                    title = name,
                    amount = null,
                    note = "supplies=$supplies · terms=$terms · status=$status · $address",
                    channelName = null,
                    counterpartyName = name,
                    paymentStatus = null,
                    masterKind = "SUPPLIER",
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
private fun MfgLineStoreForm(model: OperatingProjectionDto, onDismiss: () -> Unit, onSubmit: (ManufacturingEventDraft) -> Unit) {
    var isCreate by remember { mutableStateOf(true) }
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Line") }
    var printer by remember { mutableStateOf("Tablet only") }
    var holds by remember { mutableStateOf("Raw") }
    var status by remember { mutableStateOf("Active") }
    MfgSheetBody(Modifier.verticalScroll(rememberScrollState())) {
        MfgGrabber()
        MfgSheetHeader("Add line / store", "Capacity · Busy/Open", onClose = onDismiss)
        MfgCreateEditMode(isCreate, { isCreate = true }, { isCreate = false }, editLocked = true)
        MfgTextField(name, "Name", onValueChange = { name = it })
        MfgChipRow("Type", listOf("Store", "Line", "Station"), type) { type = it }
        MfgChipRow("Job-card printer", listOf("Shop printer 1", "Label printer", "Tablet only"), printer) { printer = it }
        MfgChipRow("Holds stock", listOf("Raw", "WIP", "Finished", "Consumables", "Spares"), holds) { holds = it }
        MfgChipRow("Status", listOf("Active", "Paused"), status) { status = it }
        MfgLockedChip("Print SDK · chrome only")
        MfgWhyBox("Line/store master is capacity chrome. Job-card printer chips are UI-only until a print stack ships.")
        MfgStickyFooter(label = "Save line / store", enabled = name.isNotBlank(), caveat = "Sheet closes and returns back", onClick = {
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
private fun MfgPartForm(model: OperatingProjectionDto, onDismiss: () -> Unit, onSubmit: (ManufacturingEventDraft) -> Unit) {
    var isCreate by remember { mutableStateOf(true) }
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Finished") }
    var partType by remember { mutableStateOf("Finished") }
    var sell by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }
    var gst by remember { mutableStateOf("18%") }
    var hsn by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("nos") }
    var drawing by remember { mutableStateOf("") }
    var trackBatch by remember { mutableStateOf("Track batch/heat") }
    var status by remember { mutableStateOf("Active") }
    val currency = model.currency ?: "INR"
    MfgSheetBody(Modifier.verticalScroll(rememberScrollState())) {
        MfgGrabber()
        MfgSheetHeader("Create part / SKU", "Parts master · GST-aware labels", onClose = onDismiss)
        MfgCreateEditMode(isCreate, { isCreate = true }, { isCreate = false }, editLocked = true)
        RowChipsContext()
        MfgTextField(name, "Part name", onValueChange = { name = it })
        MfgChipRow("Category", listOf("Raw", "WIP", "Finished", "Consumables", "Spares"), category) { category = it }
        MfgChipRow("Part type", listOf("Finished", "Raw", "WIP"), partType) { partType = it }
        MfgAmountHero("Sell price", if (sell.isBlank()) "—" else MfgFormat.exact(currency, sell), currencyBadge = currency)
        MfgNumericKeypad(sell) { sell = it }
        MfgTextField(cost, "Standard cost", onValueChange = { cost = it.filter { ch -> ch.isDigit() || ch == '.' }.take(12) })
        MfgChipRow("GST on part", listOf("18%", "5%", "Exempt"), gst) { gst = it }
        MfgTextField(hsn, "HSN / SAC", onValueChange = { hsn = it })
        MfgChipRow("Unit", listOf("nos", "kg", "set"), unit) { unit = it }
        MfgTextField(drawing, "Drawing / revision", onValueChange = { drawing = it })
        MfgChipRow(null, listOf("Track batch/heat", "No batch"), trackBatch) { trackBatch = it }
        MfgChipRow("Status", listOf("Active", "Inactive"), status) { status = it }
        MfgWhyBox("Part master save does not post GST. Rate chips are labels — tax posts on Sale invoice / GRN only.")
        MfgStickyFooter(label = "Save part", enabled = name.isNotBlank(), caveat = "Sheet closes and returns back", onClick = {
            onSubmit(
                ManufacturingEventDraft(
                    eventType = "UPDATE",
                    title = name,
                    amount = sell.ifBlank { null },
                    note = "category=$category · type=$partType · cost=$cost · drawing=$drawing · $trackBatch · $status",
                    channelName = null,
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
    MfgChipRow(null, listOf("Forge & Form ERP", "Parts master", "GST-aware"), "Parts master") {}
}

@Composable
private fun MfgBomForm(model: OperatingProjectionDto, onDismiss: () -> Unit, onSubmit: (ManufacturingEventDraft) -> Unit) {
    var isCreate by remember { mutableStateOf(true) }
    var finished by remember { mutableStateOf("") }
    var routing by remember { mutableStateOf("Press") }
    var uom by remember { mutableStateOf("nos") }
    var gst by remember { mutableStateOf("18%") }
    var hsn by remember { mutableStateOf("") }
    var batch by remember { mutableStateOf("1") }
    var issueFrom by remember { mutableStateOf("Raw store") }
    var defaultLine by remember { mutableStateOf("Line 1") }
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
    MfgSheetBody(Modifier.verticalScroll(rememberScrollState())) {
        MfgGrabber()
        MfgSheetHeader("Create BOM", "Routing · scrap · backflush", onClose = onDismiss)
        MfgCreateEditMode(isCreate, { isCreate = true }, { isCreate = false }, editLocked = true)
        MfgPickerRow("Finished part", finished.ifBlank { "Select FG" }, onClick = {
            finished = parts.firstOrNull()?.name ?: finished
        }, externalLabel = true)
        parts.take(5).forEach { MfgPickerRow(it.name, it.unit ?: "nos", onClick = { finished = it.name }) }
        MfgChipRow("Routing", listOf("Press", "Weld", "Paint", "QC", "Pack", "Dispatch"), routing) { routing = it }
        MfgChipRow("Output UOM", listOf("nos", "kg", "set"), uom) { uom = it }
        MfgAmountHero("Rolled-up cost", "Est. · UI local", caption = "Components below are local until BOM write API accepts lines", currencyBadge = "Est.")
        MfgChipRow("GST on FG", listOf("18%", "5%", "Exempt"), gst) { gst = it }
        MfgTextField(hsn, "HSN (FG)", onValueChange = { hsn = it })
        MfgPickerRow("Issue from", issueFrom, onClick = {
            issueFrom = stores.firstOrNull()?.name ?: issueFrom
        }, externalLabel = true)
        stores.take(3).forEach { MfgPickerRow(it.name, it.kind, onClick = { issueFrom = it.name }) }
        MfgTextField(batch, "Batch size", onValueChange = { batch = it.filter { ch -> ch.isDigit() }.take(6) })
        MfgPickerRow("Default line", defaultLine, onClick = {
            defaultLine = stores.firstOrNull { it.kind.equals("LINE", true) }?.name
                ?: stores.firstOrNull()?.name
                ?: defaultLine
        }, externalLabel = true)
        MfgTextField(scrapPct, "Scrap %", onValueChange = { scrapPct = it.filter { ch -> ch.isDigit() || ch == '.' }.take(5) })
        MfgChipRow(null, listOf("Backflush on accept", "Manual issue"), issue) { issue = it }
        MfgChipRow("Status", listOf("Active", "Inactive"), status) { status = it }
        MfgLinesCard("Components", components.size) {
            components.forEach { MfgPickerRow(it, "UI local", onClick = null) }
            androidx.compose.material3.Text(
                "+ Add component",
                style = com.example.momentra.ui.theme.ManufacturingAmber.viewAll(),
                modifier = Modifier.clickable {
                    parts.firstOrNull { p -> components.none { it == p.name } }?.let { components += it.name }
                },
            )
        }
        MfgWhyBox("Save BOM stores supported master fields only. Stock moves only on issue / accepted production — never on BOM save. GST is informational.")
        MfgStickyFooter(label = "Save BOM", enabled = finished.isNotBlank(), caveat = "Sheet closes and returns back", onClick = {
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
