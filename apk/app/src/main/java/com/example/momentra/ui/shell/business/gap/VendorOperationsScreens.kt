package com.example.momentra.ui.shell.business.gap

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.CreateSlaCheckBody
import com.example.momentra.data.api.CreateSlaDefinitionBody
import com.example.momentra.data.api.CreateVendorContractBody
import com.example.momentra.data.api.VendorItemDto
import com.example.momentra.data.repository.BusinessSliceRepository
import com.example.momentra.ui.theme.PlusJakartaSans
import java.util.UUID
import kotlinx.coroutines.launch

data class SessionContract(val vendorId: String, val contractId: String, val name: String)
data class SessionSla(val vendorId: String, val slaDefinitionId: String, val name: String, val contractId: String?)

@Composable
fun VendorOperationsScreen(
    companyId: String,
    onBack: () -> Unit,
    repository: BusinessSliceRepository = remember { BusinessSliceRepository() },
) {
    var loading by remember { mutableStateOf(true) }
    var vendors by remember { mutableStateOf<List<VendorItemDto>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var sheet by remember { mutableStateOf<String?>(null) }
    var contracts by remember { mutableStateOf<List<SessionContract>>(emptyList()) }
    var slas by remember { mutableStateOf<List<SessionSla>>(emptyList()) }
    var refresh by remember { mutableStateOf(0) }
    LaunchedEffect(companyId, refresh) {
        loading = true
        repository.listCompanyVendors(companyId).fold(
            onSuccess = {
                vendors = it.items
                error = null
            },
            onFailure = { error = it.message },
        )
        loading = false
    }
    Box(Modifier.fillMaxSize()) {
    GapScreen(title = "Vendor Operations", subtitle = "Supply chain contracts and SLAs", onBack = onBack) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            error?.let { Text(it, color = GapMuted, fontSize = 12.sp, fontFamily = PlusJakartaSans) }
            if (loading) {
                CircularProgressIndicator(color = GapAccent)
            } else if (vendors.isEmpty()) {
                Text("No vendors yet.", color = GapMuted, fontSize = 13.sp, fontFamily = PlusJakartaSans)
            } else {
                vendors.forEach { vendor ->
                    GapCard {
                        Text(vendor.name, color = GapText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
                        Text(vendor.status?.takeIf { it.isNotBlank() } ?: "—", color = GapMuted, fontSize = 12.sp, fontFamily = PlusJakartaSans)
                    }
                }
            }
            Text("Quick Actions", color = GapText, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
            listOf("Add Contract", "Define SLA", "Record SLA Check").forEach { label ->
                Text(
                    label,
                    color = GapBg,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(GapAccent)
                        .clickable { sheet = label }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                )
            }
        }
    }
    when (sheet) {
        "Add Contract" -> AddContractSheet(
            vendors = vendors,
            onDismiss = { sheet = null },
            onCreated = { created ->
                contracts = contracts + created
                sheet = null
                refresh += 1
            },
            repository = repository,
            companyId = companyId,
        )
        "Define SLA" -> DefineSlaSheet(
            vendors = vendors,
            contracts = contracts,
            onDismiss = { sheet = null },
            onCreated = { created ->
                slas = slas + created
                sheet = null
            },
            repository = repository,
            companyId = companyId,
        )
        "Record SLA Check" -> RecordSlaSheet(
            slas = slas,
            onDismiss = { sheet = null },
            onPosted = { sheet = null },
            repository = repository,
            companyId = companyId,
        )
    }
    }
}

@Composable
private fun AddContractSheet(
    companyId: String,
    vendors: List<VendorItemDto>,
    onDismiss: () -> Unit,
    onCreated: (SessionContract) -> Unit,
    repository: BusinessSliceRepository,
) {
    var vendorId by remember { mutableStateOf(vendors.firstOrNull()?.vendorId.orEmpty()) }
    var name by remember { mutableStateOf("") }
    var reference by remember { mutableStateOf("") }
    var start by remember { mutableStateOf("") }
    var end by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("") }
    var dateTarget by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    VendorSheet(title = "Add Contract", onDismiss = onDismiss) {
        VendorPicker(vendors, vendorId) { vendorId = it }
        GapField("Contract Name", name) { name = it }
        GapField("Contract Reference", reference) { reference = it }
        Text("Start Date", color = GapMuted, fontSize = 12.sp, fontFamily = PlusJakartaSans, modifier = Modifier.clickable { dateTarget = "start" })
        Text(start.ifBlank { "Choose a date" }, color = GapText, fontFamily = PlusJakartaSans)
        Text("End Date", color = GapMuted, fontSize = 12.sp, fontFamily = PlusJakartaSans, modifier = Modifier.clickable { dateTarget = "end" })
        Text(end.ifBlank { "Choose a date" }, color = GapText, fontFamily = PlusJakartaSans)
        GapField("Contract Value", value) { value = it }
        GapField("Currency", currency) { currency = it }
        error?.let { Text(it, color = GapMuted, fontSize = 12.sp) }
        SheetActions(
            label = "Create Contract",
            enabled = !busy && name.isNotBlank() && vendorId.isNotBlank(),
            onCancel = onDismiss,
            onConfirm = {
                busy = true
                scope.launch {
                    repository.createVendorContract(
                        companyId = companyId,
                        vendorId = vendorId,
                        body = CreateVendorContractBody(
                            contractName = name.trim(),
                            contractReference = reference.trim().ifBlank { null },
                            startDate = start.ifBlank { null },
                            endDate = end.ifBlank { null },
                            contractValue = value.trim().ifBlank { null },
                            currencyCode = currency.trim().ifBlank { null },
                        ),
                        idempotencyKey = UUID.randomUUID().toString(),
                    ).fold(
                        onSuccess = { onCreated(SessionContract(vendorId, it.vendorContractId, name.trim())) },
                        onFailure = {
                            error = it.message
                            busy = false
                        },
                    )
                }
            },
        )
    }
    if (dateTarget != null) {
        BusinessDateSheet(
            initial = if (dateTarget == "start") start else end,
            onDismiss = { dateTarget = null },
            onConfirm = { picked ->
                if (dateTarget == "start") start = picked else end = picked
                dateTarget = null
            },
        )
    }
}

@Composable
private fun DefineSlaSheet(
    companyId: String,
    vendors: List<VendorItemDto>,
    contracts: List<SessionContract>,
    onDismiss: () -> Unit,
    onCreated: (SessionSla) -> Unit,
    repository: BusinessSliceRepository,
) {
    var vendorId by remember { mutableStateOf(vendors.firstOrNull()?.vendorId.orEmpty()) }
    val vendorContracts = contracts.filter { it.vendorId == vendorId }
    var contractId by remember(vendorId) { mutableStateOf(vendorContracts.firstOrNull()?.contractId) }
    var name by remember { mutableStateOf("") }
    var metric by remember { mutableStateOf("") }
    var period by remember { mutableStateOf("") }
    var comparator by remember { mutableStateOf(">=") }
    var target by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    VendorSheet(title = "Define SLA", onDismiss = onDismiss) {
        VendorPicker(vendors, vendorId) { vendorId = it }
        Text("Contract", color = GapMuted, fontSize = 12.sp, fontFamily = PlusJakartaSans)
        if (vendorContracts.isEmpty()) {
            Text("No contracts for this vendor yet.", color = GapMuted, fontSize = 13.sp, fontFamily = PlusJakartaSans)
        } else {
            vendorContracts.forEach { contract ->
                Text(
                    contract.name,
                    color = if (contract.contractId == contractId) GapAccent else GapText,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier.clickable { contractId = contract.contractId },
                )
            }
        }
        GapField("SLA Name", name) { name = it }
        GapField("Metric Type", metric) { metric = it }
        GapField("Measurement Period", period) { period = it }
        GapField("Comparator", comparator) { comparator = it }
        GapField("Target Value", target) { target = it }
        GapField("Unit", unit) { unit = it }
        error?.let { Text(it, color = GapMuted, fontSize = 12.sp) }
        SheetActions(
            label = "Create SLA",
            enabled = !busy && name.isNotBlank() && metric.isNotBlank() && comparator.isNotBlank() && vendorId.isNotBlank(),
            onCancel = onDismiss,
            onConfirm = {
                busy = true
                scope.launch {
                    repository.createSlaDefinition(
                        companyId = companyId,
                        vendorId = vendorId,
                        body = CreateSlaDefinitionBody(
                            name = name.trim(),
                            metricCode = metric.trim(),
                            targetValue = target.toDoubleOrNull(),
                            comparator = comparator.trim(),
                            unitCode = unit.trim().ifBlank { null },
                            measurementPeriod = period.trim().ifBlank { null },
                            vendorContractId = contractId,
                        ),
                        idempotencyKey = UUID.randomUUID().toString(),
                    ).fold(
                        onSuccess = {
                            onCreated(SessionSla(vendorId, it.slaDefinitionId, name.trim(), contractId))
                        },
                        onFailure = {
                            error = it.message
                            busy = false
                        },
                    )
                }
            },
        )
    }
}

@Composable
private fun RecordSlaSheet(
    companyId: String,
    slas: List<SessionSla>,
    onDismiss: () -> Unit,
    onPosted: () -> Unit,
    repository: BusinessSliceRepository,
) {
    var selected by remember { mutableStateOf(slas.firstOrNull()) }
    var observedAt by remember { mutableStateOf("") }
    var observed by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("Met") }
    var note by remember { mutableStateOf("") }
    var showDate by remember { mutableStateOf(false) }
    var showNote by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    VendorSheet(title = "Record SLA Check", onDismiss = onDismiss) {
        if (slas.isEmpty()) {
            Text("Create an SLA in this hub before recording a check.", color = GapMuted, fontSize = 13.sp, fontFamily = PlusJakartaSans)
        } else {
            slas.forEach { sla ->
                Text(
                    sla.name,
                    color = if (sla.slaDefinitionId == selected?.slaDefinitionId) GapAccent else GapText,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier.clickable { selected = sla },
                )
            }
            Text(selected?.name ?: "—", color = GapText, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
            Text("Observation Date", color = GapMuted, fontSize = 12.sp, modifier = Modifier.clickable { showDate = true })
            Text(observedAt.ifBlank { "Choose a date" }, color = GapText, fontFamily = PlusJakartaSans)
            GapField("Observed Value", observed) { observed = it }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Met", "Breached", "Waived").forEach { label ->
                    Text(
                        label,
                        color = if (result == label) GapBg else GapText,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (result == label) GapAccent else GapCard)
                            .clickable { result = label }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
            Text("Evidence / Notes", color = GapMuted, fontSize = 12.sp, modifier = Modifier.clickable { showNote = true })
            Text(note.ifBlank { "Add a note" }, color = GapText, fontFamily = PlusJakartaSans)
        }
        error?.let { Text(it, color = GapMuted, fontSize = 12.sp) }
        SheetActions(
            label = "Record Check",
            enabled = !busy && selected != null,
            onCancel = onDismiss,
            onConfirm = {
                val sla = selected ?: return@SheetActions
                busy = true
                scope.launch {
                    repository.createSlaCheck(
                        companyId = companyId,
                        slaDefinitionId = sla.slaDefinitionId,
                        body = CreateSlaCheckBody(
                            observedAt = observedAt.ifBlank { null },
                            observedValue = observed.toDoubleOrNull(),
                            result = result.uppercase(),
                            note = note.ifBlank { null },
                        ),
                        idempotencyKey = UUID.randomUUID().toString(),
                    ).fold(
                        onSuccess = { onPosted() },
                        onFailure = {
                            error = it.message
                            busy = false
                        },
                    )
                }
            },
        )
    }
    if (showDate) {
        BusinessDateSheet(observedAt, { showDate = false }) {
            observedAt = it
            showDate = false
        }
    }
    if (showNote) {
        BusinessNotesSheet(note, { showNote = false }) {
            note = it
            showNote = false
        }
    }
}

@Composable
private fun VendorPicker(vendors: List<VendorItemDto>, selected: String, onSelect: (String) -> Unit) {
    Text("Vendor", color = GapMuted, fontSize = 12.sp, fontFamily = PlusJakartaSans)
    if (vendors.isEmpty()) {
        Text("No vendors yet.", color = GapMuted, fontSize = 13.sp, fontFamily = PlusJakartaSans)
    }
    vendors.forEach { vendor ->
        Text(
            vendor.name,
            color = if (vendor.vendorId == selected) GapAccent else GapText,
            fontFamily = PlusJakartaSans,
            modifier = Modifier.clickable { onSelect(vendor.vendorId) },
        )
    }
}

@Composable
private fun VendorSheet(title: String, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GapBg.copy(alpha = 0.96f))
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(title, color = GapText, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
        content()
    }
}

@Composable
internal fun GapField(label: String, value: String, onChange: (String) -> Unit) {
    Text(label, color = GapMuted, fontSize = 12.sp, fontFamily = PlusJakartaSans)
    BasicTextField(
        value = value,
        onValueChange = onChange,
        textStyle = TextStyle(color = GapText, fontSize = 14.sp, fontFamily = PlusJakartaSans),
        cursorBrush = SolidColor(GapAccent),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(GapCard)
            .padding(12.dp),
    )
}

@Composable
private fun SheetActions(label: String, enabled: Boolean, onCancel: () -> Unit, onConfirm: () -> Unit) {
    Text(
        label,
        color = if (enabled) GapBg else GapMuted,
        fontWeight = FontWeight.Bold,
        fontFamily = PlusJakartaSans,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) GapAccent else GapCard)
            .clickable(enabled = enabled, onClick = onConfirm)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    )
    Text("Cancel", color = GapMuted, fontFamily = PlusJakartaSans, modifier = Modifier.clickable(onClick = onCancel))
}
