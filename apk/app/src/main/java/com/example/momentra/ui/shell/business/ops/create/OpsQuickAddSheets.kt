package com.example.momentra.ui.shell.business.ops.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R
import com.example.momentra.data.api.CreateBusinessApprovalRequestBody
import com.example.momentra.data.api.CreateBusinessExpenseBody
import com.example.momentra.data.api.CreateBusinessImprovementBody
import com.example.momentra.data.api.CreateBusinessIssueBody
import com.example.momentra.data.api.CreateBusinessMemoryBody
import com.example.momentra.data.api.CreateBusinessReviewBody
import com.example.momentra.data.api.CreateBusinessUpdateBody
import com.example.momentra.data.api.CreateBusinessVendorBody
import com.example.momentra.data.api.CreateSlaCheckBody
import com.example.momentra.data.api.CreateSlaDefinitionBody
import com.example.momentra.data.api.UpdateBusinessVendorBody
import com.example.momentra.data.api.VendorItemDto
import com.example.momentra.data.repository.BusinessSliceRepository
import com.example.momentra.ui.setup.SetupDateTimeUtils
import com.example.momentra.ui.shell.business.shared.BusinessQuickAddKind
import com.example.momentra.ui.shell.business.ops.components.OpsAmountField
import com.example.momentra.ui.shell.business.ops.components.OpsChipRow
import com.example.momentra.ui.shell.business.ops.components.OpsDateField
import com.example.momentra.ui.shell.business.ops.components.OpsDropdownField
import com.example.momentra.ui.shell.business.ops.components.OpsErrorText
import com.example.momentra.ui.shell.business.ops.components.OpsFieldLabel
import com.example.momentra.ui.shell.business.ops.components.OpsPrimaryCta
import com.example.momentra.ui.shell.business.ops.components.OpsSheetHandle
import com.example.momentra.ui.shell.business.ops.components.OpsSheetHeader
import com.example.momentra.ui.shell.business.ops.components.OpsSheetTokens
import com.example.momentra.ui.shell.business.ops.components.OpsTextField
import com.example.momentra.ui.shell.business.ops.components.opsStripAmount
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.launch
import java.time.LocalDate

private val SpendCategories = listOf(
    "Operations & Logistics",
    "SaaS & Software",
    "Professional Services",
    "Marketing",
    "Office",
    "Travel",
    "Other",
)
private val VendorCategories = listOf("SaaS", "Services", "Logistics", "Hardware", "Other")
private val ApprovalCategories = listOf("Spend", "Hiring", "Vendor", "Scope Change", "Other")
private val AffectedAreas = listOf("Engineering", "Ops", "Finance", "Vendors", "Customer", "Other")
private val FocusAreas = listOf("Overall Budget", "Vendor Spend", "Payroll", "Marketing", "COGS")
private val ImpactAreas = listOf("Cost", "Speed", "Quality", "Reliability", "Process")
private val MemoryCategories = listOf(
    "Engineering Playbook",
    "Budget Playbook",
    "Vendor Playbook",
    "Ops Playbook",
    "General",
)
private val Frequencies = listOf("Recurring", "One-time", "Urgent")
private val VendorStatuses = listOf("Active", "On Hold", "Churn Risk")
private val Priorities = listOf("Normal", "High", "Urgent")
private val Severities = listOf("Low", "Medium", "High")
private val Periods = listOf("This Week", "This Month", "This Quarter")
private val ImpactLevels = listOf("Low", "Medium", "High")
private val SlaResults = listOf("Pass", "Warn", "Fail")
private val Visibilities = listOf("Team", "Leadership", "All")
private val UpdateStatuses = listOf("Info", "Success", "Warning")
private val MemoryTypes = listOf("Learning", "Pattern", "Playbook")

object OpsQuickAddSheets {
    fun isOpsKind(kind: BusinessQuickAddKind): Boolean = when (kind) {
        BusinessQuickAddKind.SPEND_ENTRY,
        BusinessQuickAddKind.UPDATE_VENDOR,
        BusinessQuickAddKind.REQUEST_APPROVAL,
        BusinessQuickAddKind.REPORT_ISSUE,
        BusinessQuickAddKind.LOG_IMPROVEMENT,
        BusinessQuickAddKind.BUDGET_REVIEW,
        BusinessQuickAddKind.SLA_CHECK,
        BusinessQuickAddKind.GENERAL_UPDATE,
        BusinessQuickAddKind.MEMORY,
        -> true
        else -> false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpsGapQuickAddSheet(
    kind: BusinessQuickAddKind,
    visible: Boolean,
    momentId: String?,
    companyId: String?,
    momentTitle: String? = null,
    onDismiss: () -> Unit,
    onSaved: () -> Unit = {},
    onExpense: () -> Unit = {},
    repository: BusinessSliceRepository = remember { BusinessSliceRepository() },
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = OpsSheetTokens.SheetBg,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OpsSheetHandle()
            when (kind) {
                BusinessQuickAddKind.SPEND_ENTRY ->
                    OpsSpendBody(momentId, repository, onDismiss, onSaved)
                BusinessQuickAddKind.UPDATE_VENDOR ->
                    OpsVendorBody(companyId, repository, onDismiss, onSaved)
                BusinessQuickAddKind.REQUEST_APPROVAL ->
                    OpsApprovalBody(momentId, repository, onDismiss, onSaved)
                BusinessQuickAddKind.REPORT_ISSUE ->
                    OpsIssueBody(momentId, repository, onDismiss, onSaved)
                BusinessQuickAddKind.LOG_IMPROVEMENT ->
                    OpsImprovementBody(momentId, repository, onDismiss, onSaved)
                BusinessQuickAddKind.BUDGET_REVIEW ->
                    OpsBudgetReviewBody(momentId, repository, onDismiss, onSaved)
                BusinessQuickAddKind.SLA_CHECK ->
                    OpsSlaBody(companyId, repository, onDismiss, onSaved)
                BusinessQuickAddKind.GENERAL_UPDATE ->
                    OpsGeneralUpdateBody(momentId, repository, onDismiss, onSaved)
                BusinessQuickAddKind.MEMORY ->
                    OpsMemoryBody(momentId, momentTitle, repository, onDismiss, onSaved)
                else -> Unit
            }
        }
    }
}

@Composable
private fun FieldBlock(label: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        OpsFieldLabel(label)
        content()
    }
}

@Composable
private fun OpsSpendBody(
    momentId: String?,
    repository: BusinessSliceRepository,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    var category by remember { mutableStateOf(SpendCategories.first()) }
    var amountDisplay by remember { mutableStateOf("") }
    var vendor by remember { mutableStateOf("") }
    var isoDate by remember { mutableStateOf(SetupDateTimeUtils.localDateToIso(LocalDate.now())) }
    var frequency by remember { mutableStateOf("One-time") }
    var notes by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    OpsSheetHeader(
        iconRes = R.drawable.ic_biz_create_credit_card,
        title = "Log Spend Entry",
        explanation = "Record an ops expense against budget",
        onClose = onDismiss,
    )
    FieldBlock("Category") {
        OpsDropdownField(category, SpendCategories, { category = it }, "Select category")
    }
    FieldBlock("Amount") {
        OpsAmountField(amountDisplay, { amountDisplay = it })
    }
    FieldBlock("Vendor") {
        OpsTextField(vendor, { vendor = it }, "Vendor name")
    }
    FieldBlock("Date") {
        OpsDateField(isoDate, { isoDate = it })
    }
    FieldBlock("Frequency") {
        OpsChipRow(Frequencies, frequency) { frequency = it }
    }
    FieldBlock("Notes") {
        OpsTextField(notes, { notes = it }, "Describe the business expense...", singleLine = false, minHeight = 80)
    }
    OpsErrorText(error)
    OpsPrimaryCta(
        label = if (submitting) "Saving…" else "Log Spend",
        enabled = !momentId.isNullOrBlank() && opsStripAmount(amountDisplay).toDoubleOrNull()?.let { it > 0 } == true && !submitting,
        loading = submitting,
        footerHint = "Transaction will be posted",
        onClick = {
            val id = momentId ?: return@OpsPrimaryCta
            val amount = opsStripAmount(amountDisplay)
            submitting = true
            error = null
            scope.launch {
                val noteParts = buildList {
                    if (notes.isNotBlank()) add(notes.trim())
                    add("Frequency: $frequency")
                    add("Date: $isoDate")
                }
                repository.createExpense(
                    momentId = id,
                    body = CreateBusinessExpenseBody(
                        amount = amount,
                        currencyCode = "INR",
                        description = noteParts.joinToString(" · "),
                        merchantName = vendor.takeIf { it.isNotBlank() },
                        categoryCode = category.uppercase().replace(' ', '_').take(32),
                    ),
                ).fold(
                    onSuccess = { submitting = false; onSaved(); onDismiss() },
                    onFailure = { submitting = false; error = it.message ?: "Could not log spend" },
                )
            }
        },
    )
}

@Composable
private fun OpsVendorBody(
    companyId: String?,
    repository: BusinessSliceRepository,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(VendorCategories.first()) }
    var contact by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Active") }
    var notes by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    var existingVendors by remember { mutableStateOf<List<VendorItemDto>>(emptyList()) }
    var selectedVendor by remember { mutableStateOf<VendorItemDto?>(null) }
    var useExisting by remember { mutableStateOf(false) }

    LaunchedEffect(companyId) {
        if (companyId.isNullOrBlank()) return@LaunchedEffect
        repository.listCompanyVendors(companyId).fold(
            onSuccess = { existingVendors = it.items },
            onFailure = { /* non-blocking */ },
        )
    }

    OpsSheetHeader(
        iconRes = R.drawable.ic_biz_create_briefcase,
        title = "Update Vendor",
        explanation = "Add or update an ops supplier profile",
        onClose = onDismiss,
    )
    if (existingVendors.isNotEmpty()) {
        FieldBlock("Select Existing") {
            val vendorNames = listOf("Create new") + existingVendors.map { it.name }
            OpsDropdownField(
                if (useExisting && selectedVendor != null) selectedVendor!!.name else "Create new",
                vendorNames,
                { picked ->
                    val found = existingVendors.firstOrNull { it.name == picked }
                    if (found != null) {
                        useExisting = true
                        selectedVendor = found
                        name = found.name
                    } else {
                        useExisting = false
                        selectedVendor = null
                        name = ""
                    }
                },
                "Select existing or create new",
            )
        }
    }
    FieldBlock("Vendor Name") {
        OpsTextField(name, { name = it }, "Vendor name")
    }
    FieldBlock("Category") {
        OpsDropdownField(category, VendorCategories, { category = it }, "Select category")
    }
    FieldBlock("Contact Person") {
        OpsTextField(contact, { contact = it }, "Optional contact")
    }
    FieldBlock("Status") {
        OpsChipRow(VendorStatuses, status) { status = it }
    }
    FieldBlock("Notes") {
        OpsTextField(notes, { notes = it }, "Optional notes", singleLine = false, minHeight = 80)
    }
    OpsErrorText(error)
    if (companyId.isNullOrBlank()) {
        Text("Select a company to manage vendors.", color = OpsSheetTokens.Muted, fontSize = 12.sp, fontFamily = PlusJakartaSans)
    }
    OpsPrimaryCta(
        label = if (submitting) "Saving…" else if (useExisting) "Update Vendor" else "Create Vendor",
        enabled = !companyId.isNullOrBlank() && name.isNotBlank() && !submitting,
        loading = submitting,
        footerHint = "Vendor profile will sync",
        onClick = {
            val cid = companyId ?: return@OpsPrimaryCta
            submitting = true
            error = null
            scope.launch {
                val note = buildList {
                    if (contact.isNotBlank()) add("Contact: ${contact.trim()}")
                    add("Status: $status")
                    if (notes.isNotBlank()) add(notes.trim())
                }.joinToString(" · ")
                if (useExisting && selectedVendor != null) {
                    repository.updateVendor(
                        companyId = cid,
                        vendorId = selectedVendor!!.vendorId,
                        body = UpdateBusinessVendorBody(
                            name = name.trim(),
                            vendorType = category,
                            note = note.ifBlank { null },
                        ),
                    ).fold(
                        onSuccess = { submitting = false; onSaved(); onDismiss() },
                        onFailure = { submitting = false; error = it.message ?: "Could not update vendor" },
                    )
                } else {
                    repository.createVendor(
                        companyId = cid,
                        body = CreateBusinessVendorBody(
                            name = name.trim(),
                            vendorType = category,
                        ),
                    ).fold(
                        onSuccess = { created ->
                            if (note.isNotBlank()) {
                                repository.updateVendor(
                                    companyId = cid,
                                    vendorId = created.vendorId,
                                    body = UpdateBusinessVendorBody(note = note),
                                )
                            }
                            submitting = false
                            onSaved()
                            onDismiss()
                        },
                        onFailure = { submitting = false; error = it.message ?: "Could not save vendor" },
                    )
                }
            }
        },
    )
}

@Composable
private fun OpsApprovalBody(
    momentId: String?,
    repository: BusinessSliceRepository,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ApprovalCategories.first()) }
    var amountDisplay by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("Normal") }
    var justification by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    OpsSheetHeader(
        iconRes = R.drawable.ic_biz_create_wallet,
        title = "Request Approval",
        explanation = "Route a spend or scope for sign-off",
        onClose = onDismiss,
    )
    FieldBlock("Request Title") {
        OpsTextField(title, { title = it }, "What needs sign-off")
    }
    FieldBlock("Category") {
        OpsDropdownField(category, ApprovalCategories, { category = it }, "Select category")
    }
    FieldBlock("Amount") {
        OpsAmountField(amountDisplay, { amountDisplay = it })
    }
    FieldBlock("Priority") {
        OpsChipRow(Priorities, priority) { priority = it }
    }
    FieldBlock("Justification") {
        OpsTextField(justification, { justification = it }, "Why this needs approval...", singleLine = false, minHeight = 80)
    }
    OpsErrorText(error)
    OpsPrimaryCta(
        label = if (submitting) "Saving…" else "Submit for Approval",
        enabled = !momentId.isNullOrBlank() && title.isNotBlank() && !submitting,
        loading = submitting,
        footerHint = "Stakeholders will be notified",
        onClick = {
            val id = momentId ?: return@OpsPrimaryCta
            val amount = opsStripAmount(amountDisplay).takeIf { it.isNotBlank() }
            submitting = true
            error = null
            scope.launch {
                val note = buildList {
                    add("Category: $category")
                    add("Priority: $priority")
                    if (justification.isNotBlank()) add(justification.trim())
                }.joinToString(" · ")
                repository.createApprovalRequest(
                    momentId = id,
                    body = CreateBusinessApprovalRequestBody(
                        title = title.trim(),
                        amount = amount,
                        currencyCode = if (amount != null) "INR" else null,
                        note = note,
                    ),
                ).fold(
                    onSuccess = { submitting = false; onSaved(); onDismiss() },
                    onFailure = { submitting = false; error = it.message ?: "Could not create approval" },
                )
            }
        },
    )
}

@Composable
private fun OpsIssueBody(
    momentId: String?,
    repository: BusinessSliceRepository,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var severity by remember { mutableStateOf("Medium") }
    var area by remember { mutableStateOf(AffectedAreas.first()) }
    var description by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    OpsSheetHeader(
        iconRes = R.drawable.ic_biz_create_trending,
        title = "Report Issue",
        explanation = "Flag an operational problem for the team",
        onClose = onDismiss,
    )
    FieldBlock("Issue Title") {
        OpsTextField(title, { title = it }, "Issue title")
    }
    FieldBlock("Severity") {
        OpsChipRow(Severities, severity) { severity = it }
    }
    FieldBlock("Affected Area") {
        OpsDropdownField(area, AffectedAreas, { area = it }, "Select area")
    }
    FieldBlock("Description") {
        OpsTextField(description, { description = it }, "Describe the issue...", singleLine = false, minHeight = 80)
    }
    FieldBlock("Attach Evidence") {
        OpsTextField("", {}, "Optional — upload not available yet", singleLine = true)
    }
    OpsErrorText(error)
    OpsPrimaryCta(
        label = if (submitting) "Saving…" else "Report Issue",
        enabled = !momentId.isNullOrBlank() && title.isNotBlank() && !submitting,
        loading = submitting,
        footerHint = "Team will be notified",
        onClick = {
            val id = momentId ?: return@OpsPrimaryCta
            submitting = true
            error = null
            scope.launch {
                val desc = buildList {
                    add("Area: $area")
                    if (description.isNotBlank()) add(description.trim())
                }.joinToString(" · ")
                repository.createIssue(
                    momentId = id,
                    body = CreateBusinessIssueBody(
                        title = title.trim(),
                        description = desc,
                        severity = severity.uppercase(),
                    ),
                ).fold(
                    onSuccess = { submitting = false; onSaved(); onDismiss() },
                    onFailure = { submitting = false; error = it.message ?: "Could not report issue" },
                )
            }
        },
    )
}

@Composable
private fun OpsBudgetReviewBody(
    momentId: String?,
    repository: BusinessSliceRepository,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    var period by remember { mutableStateOf("This Month") }
    var focus by remember { mutableStateOf(FocusAreas.first()) }
    var variance by remember { mutableStateOf("") }
    var findings by remember { mutableStateOf("") }
    var financeHint by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(momentId) {
        if (momentId.isNullOrBlank()) return@LaunchedEffect
        repository.getFinance(momentId).fold(
            onSuccess = { payload ->
                val t = payload.payload?.totals?.firstOrNull()
                financeHint = t?.let {
                    "Live: Exp ${it.expenseTotal} · Rev ${it.revenueTotal} (${it.currencyCode})"
                }
            },
            onFailure = { /* optional context */ },
        )
    }

    OpsSheetHeader(
        iconRes = R.drawable.ic_biz_create_wallet,
        title = "Budget Review",
        explanation = "Log a budget checkpoint for this period",
        onClose = onDismiss,
    )
    FieldBlock("Period") {
        OpsChipRow(Periods, period) { period = it }
    }
    FieldBlock("Focus Area") {
        OpsDropdownField(focus, FocusAreas, { focus = it }, "Select focus")
    }
    FieldBlock("Variance Note") {
        OpsTextField(variance, { variance = it }, "e.g. 4% below forecast")
    }
    FieldBlock("Findings") {
        OpsTextField(findings, { findings = it }, "Key findings...", singleLine = false, minHeight = 80)
    }
    financeHint?.let {
        Text(it, color = OpsSheetTokens.Muted, fontSize = 12.sp, fontFamily = PlusJakartaSans, modifier = Modifier.fillMaxWidth())
    }
    OpsErrorText(error)
    OpsPrimaryCta(
        label = if (submitting) "Saving…" else "Save Review",
        enabled = !momentId.isNullOrBlank() && findings.isNotBlank() && !submitting,
        loading = submitting,
        footerHint = "Review will be logged",
        onClick = {
            val id = momentId ?: return@OpsPrimaryCta
            submitting = true
            error = null
            scope.launch {
                val apiPeriod = when (period) {
                    "This Week" -> "WEEKLY"
                    "This Month" -> "MONTHLY"
                    "This Quarter" -> "QUARTERLY"
                    else -> "OTHER"
                }
                val summary = buildString {
                    appendLine("Focus: $focus")
                    if (variance.isNotBlank()) appendLine("Variance: ${variance.trim()}")
                    append(findings.trim())
                }
                repository.createBusinessReview(
                    momentId = id,
                    body = CreateBusinessReviewBody(
                        period = apiPeriod,
                        summary = summary,
                    ),
                ).fold(
                    onSuccess = { submitting = false; onSaved(); onDismiss() },
                    onFailure = { submitting = false; error = it.message ?: "Could not save review" },
                )
            }
        },
    )
}

@Composable
private fun OpsImprovementBody(
    momentId: String?,
    repository: BusinessSliceRepository,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var impactArea by remember { mutableStateOf(ImpactAreas.first()) }
    var impact by remember { mutableStateOf("Medium") }
    var description by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    OpsSheetHeader(
        iconRes = R.drawable.ic_biz_create_trending,
        title = "Log Improvement",
        explanation = "Capture an optimization for the ops playbook",
        onClose = onDismiss,
    )
    FieldBlock("Title") {
        OpsTextField(title, { title = it }, "Improvement title")
    }
    FieldBlock("Impact Area") {
        OpsDropdownField(impactArea, ImpactAreas, { impactArea = it }, "Select area")
    }
    FieldBlock("Impact Level") {
        OpsChipRow(ImpactLevels, impact) { impact = it }
    }
    FieldBlock("Description") {
        OpsTextField(description, { description = it }, "What to improve...", singleLine = false, minHeight = 80)
    }
    OpsErrorText(error)
    OpsPrimaryCta(
        label = if (submitting) "Saving…" else "Log Improvement",
        enabled = !momentId.isNullOrBlank() && title.isNotBlank() && !submitting,
        loading = submitting,
        footerHint = "Improvement will be tracked",
        onClick = {
            val id = momentId ?: return@OpsPrimaryCta
            submitting = true
            error = null
            scope.launch {
                repository.createImprovement(
                    momentId = id,
                    body = CreateBusinessImprovementBody(
                        title = title.trim(),
                        description = description.takeIf { it.isNotBlank() },
                        categoryCode = impactArea.uppercase(),
                        impactEstimate = impact,
                    ),
                ).fold(
                    onSuccess = { submitting = false; onSaved(); onDismiss() },
                    onFailure = { submitting = false; error = it.message ?: "Could not log improvement" },
                )
            }
        },
    )
}

@Composable
private fun OpsSlaBody(
    companyId: String?,
    repository: BusinessSliceRepository,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    var vendor by remember { mutableStateOf("") }
    var metric by remember { mutableStateOf("Uptime") }
    var result by remember { mutableStateOf("Pass") }
    var notes by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    var existingVendors by remember { mutableStateOf<List<VendorItemDto>>(emptyList()) }
    var selectedVendor by remember { mutableStateOf<VendorItemDto?>(null) }
    var useExisting by remember { mutableStateOf(false) }

    LaunchedEffect(companyId) {
        if (companyId.isNullOrBlank()) return@LaunchedEffect
        repository.listCompanyVendors(companyId).fold(
            onSuccess = { existingVendors = it.items },
            onFailure = { /* non-blocking */ },
        )
    }

    OpsSheetHeader(
        iconRes = R.drawable.ic_biz_create_layers,
        title = "SLA Check",
        explanation = "Record an SLA observation for a vendor",
        onClose = onDismiss,
    )
    if (existingVendors.isNotEmpty()) {
        FieldBlock("Select Vendor") {
            val vendorNames = listOf("Create new") + existingVendors.map { it.name }
            OpsDropdownField(
                if (useExisting && selectedVendor != null) selectedVendor!!.name else "Create new",
                vendorNames,
                { picked ->
                    val found = existingVendors.firstOrNull { it.name == picked }
                    if (found != null) {
                        useExisting = true
                        selectedVendor = found
                        vendor = found.name
                    } else {
                        useExisting = false
                        selectedVendor = null
                        vendor = ""
                    }
                },
                "Select existing or create new",
            )
        }
    }
    if (!useExisting) {
        FieldBlock("Vendor Name") {
            OpsTextField(vendor, { vendor = it }, "Vendor name")
        }
    }
    FieldBlock("Metric Name") {
        OpsTextField(metric, { metric = it }, "e.g. Uptime")
    }
    FieldBlock("Result") {
        OpsChipRow(SlaResults, result) { result = it }
    }
    FieldBlock("Notes") {
        OpsTextField(notes, { notes = it }, "Optional notes", singleLine = false, minHeight = 80)
    }
    OpsErrorText(error)
    if (companyId.isNullOrBlank()) {
        Text("Select a company to record SLA checks.", color = OpsSheetTokens.Muted, fontSize = 12.sp, fontFamily = PlusJakartaSans)
    }
    OpsPrimaryCta(
        label = if (submitting) "Saving…" else "Log SLA Check",
        enabled = !companyId.isNullOrBlank() && vendor.isNotBlank() && metric.isNotBlank() && !submitting,
        loading = submitting,
        footerHint = "SLA record will update",
        onClick = {
            val cid = companyId ?: return@OpsPrimaryCta
            submitting = true
            error = null
            scope.launch {
                val apiResult = when (result.lowercase()) {
                    "pass" -> "PASS"
                    "fail" -> "FAIL"
                    else -> "UNKNOWN"
                }
                val vendorId = if (useExisting && selectedVendor != null) {
                    selectedVendor!!.vendorId
                } else {
                    val vendorResult = repository.createVendor(
                        companyId = cid,
                        body = CreateBusinessVendorBody(name = vendor.trim(), vendorType = "SaaS"),
                    )
                    vendorResult.getOrElse {
                        submitting = false
                        error = it.message ?: "Could not create vendor"
                        return@launch
                    }.vendorId
                }
                val defResult = repository.createSlaDefinition(
                    companyId = cid,
                    vendorId = vendorId,
                    body = CreateSlaDefinitionBody(
                        name = metric.trim(),
                        metricCode = metric.trim().uppercase().replace(' ', '_').take(32),
                        targetValue = 99.9,
                        comparator = "GTE",
                        unitCode = "PCT",
                    ),
                )
                val slaDefinitionId = defResult.getOrElse {
                    submitting = false
                    error = it.message ?: "Could not create SLA definition"
                    return@launch
                }.slaDefinitionId
                repository.createSlaCheck(
                    companyId = cid,
                    slaDefinitionId = slaDefinitionId,
                    body = CreateSlaCheckBody(
                        result = apiResult,
                        note = notes.takeIf { it.isNotBlank() },
                    ),
                ).fold(
                    onSuccess = { submitting = false; onSaved(); onDismiss() },
                    onFailure = { submitting = false; error = it.message ?: "Could not record SLA check" },
                )
            }
        },
    )
}

@Composable
private fun OpsGeneralUpdateBody(
    momentId: String?,
    repository: BusinessSliceRepository,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var visibility by remember { mutableStateOf("Team") }
    var status by remember { mutableStateOf("Info") }
    var message by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    OpsSheetHeader(
        iconRes = R.drawable.ic_biz_create_trending,
        title = "General Update",
        explanation = "Share an operations status with the team",
        onClose = onDismiss,
    )
    FieldBlock("Update Title") {
        OpsTextField(title, { title = it }, "Optional title")
    }
    FieldBlock("Visibility") {
        OpsChipRow(Visibilities, visibility) { visibility = it }
    }
    FieldBlock("Status") {
        OpsChipRow(UpdateStatuses, status) { status = it }
    }
    FieldBlock("Message") {
        OpsTextField(message, { message = it }, "What happened...", singleLine = false, minHeight = 96)
    }
    OpsErrorText(error)
    OpsPrimaryCta(
        label = if (submitting) "Saving…" else "Post Update",
        enabled = !momentId.isNullOrBlank() && message.isNotBlank() && !submitting,
        loading = submitting,
        footerHint = "Update will be shared",
        onClick = {
            val id = momentId ?: return@OpsPrimaryCta
            submitting = true
            error = null
            scope.launch {
                val body = buildString {
                    appendLine("Visibility: $visibility")
                    appendLine("Status: $status")
                    append(message.trim())
                }
                repository.createBusinessUpdate(
                    momentId = id,
                    body = CreateBusinessUpdateBody(
                        title = title.takeIf { it.isNotBlank() } ?: "Ops update",
                        body = body,
                    ),
                ).fold(
                    onSuccess = { submitting = false; onSaved(); onDismiss() },
                    onFailure = { submitting = false; error = it.message ?: "Could not post update" },
                )
            }
        },
    )
}

@Composable
private fun OpsMemoryBody(
    momentId: String?,
    momentTitle: String?,
    repository: BusinessSliceRepository,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(MemoryCategories.first()) }
    var memoryType by remember { mutableStateOf("Learning") }
    var insight by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val sourceMoment = momentTitle?.takeIf { it.isNotBlank() } ?: "Current moment"

    OpsSheetHeader(
        iconRes = R.drawable.ic_biz_create_layers,
        title = "Save to Memory",
        explanation = "Capture an ops learning for the playbook",
        onClose = onDismiss,
    )
    FieldBlock("Title") {
        OpsTextField(title, { title = it }, "Memory title")
    }
    FieldBlock("Category") {
        OpsDropdownField(category, MemoryCategories, { category = it }, "Select category")
    }
    FieldBlock("Source Moment") {
        OpsDropdownField(sourceMoment, listOf(sourceMoment), {}, sourceMoment)
    }
    FieldBlock("Memory Type") {
        OpsChipRow(MemoryTypes, memoryType) { memoryType = it }
    }
    FieldBlock("Insight") {
        OpsTextField(insight, { insight = it }, "What should the playbook remember?", singleLine = false, minHeight = 80)
    }
    OpsErrorText(error)
    OpsPrimaryCta(
        label = if (submitting) "Saving…" else "Save to Memory",
        enabled = !momentId.isNullOrBlank() && title.isNotBlank() && insight.isNotBlank() && !submitting,
        loading = submitting,
        footerHint = "Logged under corporate playbook",
        onClick = {
            val id = momentId ?: return@OpsPrimaryCta
            submitting = true
            error = null
            scope.launch {
                val body = buildString {
                    appendLine("Category: $category")
                    appendLine("Source: $sourceMoment")
                    append(insight.trim())
                }
                repository.createMemory(
                    momentId = id,
                    body = CreateBusinessMemoryBody(
                        title = title.trim(),
                        body = body,
                        memoryType = memoryType.uppercase(),
                    ),
                ).fold(
                    onSuccess = { submitting = false; onSaved(); onDismiss() },
                    onFailure = { submitting = false; error = it.message ?: "Could not save memory" },
                )
            }
        },
    )
}
