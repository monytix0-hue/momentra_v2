package com.example.momentra.ui.shell.business.runway

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.momentra.R
import com.example.momentra.data.api.BusinessInvoiceLineDto
import com.example.momentra.data.api.CreateBusinessExpenseBody
import com.example.momentra.data.api.CreateBusinessInvoiceBody
import com.example.momentra.data.api.CreateBusinessMemoryBody
import com.example.momentra.data.api.CreateBusinessRevenueBody
import com.example.momentra.data.api.CreateBusinessUpdateBody
import com.example.momentra.data.api.CreateTaxObligationBody
import com.example.momentra.data.api.CreateInvestorUpdateBody
import com.example.momentra.data.api.CreateBudgetAlertBody
import com.example.momentra.data.api.CreateForecastScenarioBody
import com.example.momentra.data.api.ForecastScenarioLineBody
import com.example.momentra.data.repository.BusinessSliceRepository
import com.example.momentra.ui.shell.business.BusinessQuickAddKind
import com.example.momentra.ui.shell.business.emoji
import com.example.momentra.ui.shell.business.label
import com.example.momentra.ui.shell.business.runway.components.RunwayAmberAccent
import com.example.momentra.ui.shell.business.runway.components.RunwayAmountField
import com.example.momentra.ui.shell.business.runway.components.RunwayDateField
import com.example.momentra.ui.shell.business.runway.components.RunwayDropdownField
import com.example.momentra.ui.shell.business.runway.components.RunwayEmeraldAccent
import com.example.momentra.ui.shell.business.runway.components.RunwayErrorText
import com.example.momentra.ui.shell.business.runway.components.RunwayFieldLabel
import com.example.momentra.ui.shell.business.runway.components.RunwayLavenderAccent
import com.example.momentra.ui.shell.business.runway.components.RunwayPrimaryCta
import com.example.momentra.ui.shell.business.runway.components.RunwayRedAccent
import com.example.momentra.ui.shell.business.runway.components.RunwaySegmentedControl
import com.example.momentra.ui.shell.business.runway.components.RunwaySheetAccent
import com.example.momentra.ui.shell.business.runway.components.RunwaySheetHandle
import com.example.momentra.ui.shell.business.runway.components.RunwaySheetHeader
import com.example.momentra.ui.shell.business.runway.components.RunwaySheetTokens
import com.example.momentra.ui.shell.business.runway.components.RunwayTextField
import com.example.momentra.ui.shell.business.runway.components.runwayStripAmount
import com.example.momentra.ui.shell.business.subtitle
import kotlinx.coroutines.launch
import java.time.LocalDate

private val RevenueSources = listOf("Product", "Services", "Subscription", "Other")
private val Clients = listOf("Internal", "Client A", "Client B", "New project")
private val ExpenseCategories = listOf("OPS", "PURCHASE", "SOFTWARE", "TRAVEL", "OTHER")
private val TaxTypes = listOf("GST", "TDS", "Income Tax", "Other")
private val TaxPeriods = listOf("Q1", "Q2", "Q3", "Q4")
private val TaxStatuses = listOf("Filed", "Pending", "Overdue")
private val InvestorTypes = listOf("Monthly", "Quarterly", "Ad-hoc")
private val RunwayStatuses = listOf(
    "6 Months (Tight)",
    "12 Months (Watch)",
    "18 Months (Stable)",
    "36 Months (Secure)",
)
private val Departments = listOf("Engineering", "Marketing", "Operations", "Sales", "Finance")
private val BudgetCategories = listOf("Salaries", "Software", "Marketing", "Infrastructure", "Other")
private val Severities = listOf("Warning", "Critical", "Overrun")
private val ForecastPeriods = listOf("Next Month", "Next Quarter")
private val RunwayImpacts = listOf("Extends", "Neutral", "Shortens")
private val UpdateVisibilities = listOf("Team", "Company", "Private")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunwayQuickAddSheet(
    kind: BusinessQuickAddKind,
    visible: Boolean,
    momentId: String?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit = {},
    repository: BusinessSliceRepository = remember { BusinessSliceRepository() },
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = RunwaySheetTokens.SheetBg,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            RunwaySheetHandle()
            when (kind) {
                BusinessQuickAddKind.REVENUE ->
                    RunwayRevenueForm(momentId, onDismiss, onSaved, repository)
                BusinessQuickAddKind.EXPENSE, BusinessQuickAddKind.SPEND_ENTRY ->
                    RunwayExpenseForm(momentId, onDismiss, onSaved, repository)
                BusinessQuickAddKind.TAX_ENTRY ->
                    RunwayTaxForm(momentId, onDismiss, onSaved, repository)
                BusinessQuickAddKind.INVESTOR_UPDATE ->
                    RunwayInvestorForm(momentId, onDismiss, onSaved, repository)
                BusinessQuickAddKind.BUDGET_ALERT ->
                    RunwayBudgetForm(momentId, onDismiss, onSaved, repository)
                BusinessQuickAddKind.FORECAST_UPDATE ->
                    RunwayForecastForm(momentId, onDismiss, onSaved, repository)
                BusinessQuickAddKind.INVOICE ->
                    RunwayInvoiceForm(momentId, onDismiss, onSaved, repository)
                BusinessQuickAddKind.GENERAL_UPDATE, BusinessQuickAddKind.TEAM_UPDATE ->
                    RunwayUpdateForm(momentId, onDismiss, onSaved, repository)
                BusinessQuickAddKind.MEMORY ->
                    RunwayMemoryForm(momentId, onDismiss, onSaved, repository)
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
    ) {
        RunwayFieldLabel(label)
        content()
    }
}

@Composable
private fun RunwayRevenueForm(
    momentId: String?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    repository: BusinessSliceRepository,
) {
    val accent = RunwayAmberAccent
    var source by remember { mutableStateOf("") }
    var amountDisplay by remember { mutableStateOf("") }
    var revenueType by remember { mutableStateOf("Recurring") }
    var client by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var notes by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    RunwaySheetHeader(
        iconRes = R.drawable.ic_biz_create_trending,
        emojiFallback = BusinessQuickAddKind.REVENUE.emoji(),
        title = "Log Revenue",
        explanation = "Directly added to corporate runway",
        accent = accent,
        onClose = onDismiss,
    )
    FieldBlock("Source") {
        RunwayDropdownField(source, RevenueSources, { source = it }, "Select source")
    }
    FieldBlock("Amount") {
        RunwayAmountField(amountDisplay, { amountDisplay = it }, accent, "₹ Enter revenue amount")
    }
    FieldBlock("Type") {
        RunwaySegmentedControl(listOf("Recurring", "One-time"), revenueType, accent) { revenueType = it }
    }
    FieldBlock("Client/Project") {
        RunwayDropdownField(client, Clients, { client = it }, "Select client or project")
    }
    FieldBlock("Date") { RunwayDateField(isoDate = date, onIsoDateChange = { date = it }) }
    FieldBlock("Notes") {
        RunwayTextField(notes, { notes = it }, "Add internal payment notes...", accent, singleLine = false, minHeight = 70)
    }
    RunwayErrorText(error)
    RunwayPrimaryCta(
        label = if (submitting) "Saving…" else "Log Revenue",
        enabled = !momentId.isNullOrBlank() && amountDisplay.isNotBlank() && !submitting,
        loading = submitting,
        footerHint = "Directly added to corporate runway",
        accent = accent,
        onClick = {
            val id = momentId ?: return@RunwayPrimaryCta
            submitting = true
            error = null
            scope.launch {
                repository.createRevenue(
                    momentId = id,
                    body = CreateBusinessRevenueBody(
                        amount = runwayStripAmount(amountDisplay),
                        currencyCode = "INR",
                        categoryCode = source.ifBlank { null },
                        description = joinParts(
                            "Source: ${source.ifBlank { "—" }}",
                            "Type: $revenueType",
                            "Client: $client".takeIf { client.isNotBlank() },
                            "Date: $date",
                            notes.takeIf { it.isNotBlank() },
                        ),
                    ),
                ).fold(
                    onSuccess = { submitting = false; onSaved(); onDismiss() },
                    onFailure = { submitting = false; error = it.message ?: "Could not log revenue" },
                )
            }
        },
    )
}

@Composable
private fun RunwayExpenseForm(
    momentId: String?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    repository: BusinessSliceRepository,
) {
    val accent = RunwayAmberAccent
    var category by remember { mutableStateOf("") }
    var amountDisplay by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var notes by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    RunwaySheetHeader(
        iconRes = R.drawable.ic_biz_create_credit_card,
        emojiFallback = BusinessQuickAddKind.EXPENSE.emoji(),
        title = "Log Expense",
        explanation = "Record business spend",
        accent = accent,
        onClose = onDismiss,
    )
    FieldBlock("Category") {
        RunwayDropdownField(category, ExpenseCategories, { category = it }, "Select category")
    }
    FieldBlock("Amount") {
        RunwayAmountField(amountDisplay, { amountDisplay = it }, accent, "₹ Enter expense amount")
    }
    FieldBlock("Merchant") {
        RunwayTextField(merchant, { merchant = it }, "Vendor or merchant", accent)
    }
    FieldBlock("Date") { RunwayDateField(isoDate = date, onIsoDateChange = { date = it }) }
    FieldBlock("Notes") {
        RunwayTextField(notes, { notes = it }, "Optional notes...", accent, singleLine = false, minHeight = 70)
    }
    RunwayErrorText(error)
    RunwayPrimaryCta(
        label = if (submitting) "Saving…" else "Log Expense",
        enabled = !momentId.isNullOrBlank() && amountDisplay.isNotBlank() && !submitting,
        loading = submitting,
        footerHint = "Updates burn and runway",
        accent = accent,
        onClick = {
            val id = momentId ?: return@RunwayPrimaryCta
            submitting = true
            error = null
            scope.launch {
                repository.createExpense(
                    momentId = id,
                    body = CreateBusinessExpenseBody(
                        amount = runwayStripAmount(amountDisplay),
                        currencyCode = "INR",
                        categoryCode = category.ifBlank { null },
                        merchantName = merchant.takeIf { it.isNotBlank() },
                        description = joinParts("Date: $date", notes.takeIf { it.isNotBlank() }),
                    ),
                ).fold(
                    onSuccess = { submitting = false; onSaved(); onDismiss() },
                    onFailure = { submitting = false; error = it.message ?: "Could not log expense" },
                )
            }
        },
    )
}

@Composable
private fun RunwayTaxForm(
    momentId: String?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    repository: BusinessSliceRepository,
) {
    val accent = RunwayEmeraldAccent
    var taxType by remember { mutableStateOf("GST") }
    var period by remember { mutableStateOf("Q2") }
    var amountDisplay by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var status by remember { mutableStateOf("Pending") }
    var notes by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    RunwaySheetHeader(
        iconRes = R.drawable.ic_biz_create_trending,
        emojiFallback = BusinessQuickAddKind.TAX_ENTRY.emoji(),
        title = "Tax Entry",
        explanation = "Keeps financial timeline compliant",
        accent = accent,
        onClose = onDismiss,
    )
    FieldBlock("Tax Type") {
        RunwayDropdownField(taxType, TaxTypes, { taxType = it }, "Select tax type")
    }
    FieldBlock("Period") {
        RunwaySegmentedControl(TaxPeriods, period, accent) { period = it }
    }
    FieldBlock("Amount") {
        RunwayAmountField(amountDisplay, { amountDisplay = it }, accent, "₹ Enter tax liability")
    }
    FieldBlock("Due Date") { RunwayDateField(isoDate = dueDate, onIsoDateChange = { dueDate = it }) }
    FieldBlock("Status") {
        RunwaySegmentedControl(TaxStatuses, status, accent) { status = it }
    }
    FieldBlock("Notes") {
        RunwayTextField(notes, { notes = it }, "Tax compliance reference notes...", accent, singleLine = false, minHeight = 70)
    }
    RunwayErrorText(error)
    RunwayPrimaryCta(
        label = if (submitting) "Saving…" else "Save Tax Entry",
        enabled = !momentId.isNullOrBlank() && amountDisplay.isNotBlank() && !submitting,
        loading = submitting,
        footerHint = "Keeps financial timeline compliant",
        accent = accent,
        onClick = {
            val id = momentId ?: return@RunwayPrimaryCta
            submitting = true
            error = null
            scope.launch {
                repository.createTaxObligation(
                    momentId = id,
                    body = CreateTaxObligationBody(
                        title = "$taxType · $period",
                        taxType = taxType,
                        amount = runwayStripAmount(amountDisplay).ifBlank { null },
                        currencyCode = "INR",
                        dueDate = dueDate,
                        notes = joinParts("Status: $status", notes.takeIf { it.isNotBlank() }),
                    ),
                ).fold(
                    onSuccess = { submitting = false; onSaved(); onDismiss() },
                    onFailure = { submitting = false; error = it.message ?: "Could not save tax entry" },
                )
            }
        },
    )
}

@Composable
private fun RunwayInvestorForm(
    momentId: String?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    repository: BusinessSliceRepository,
) {
    val accent = RunwayLavenderAccent
    var updateType by remember { mutableStateOf("Monthly") }
    var subject by remember { mutableStateOf("") }
    var metrics by remember { mutableStateOf("") }
    var runwayStatus by remember { mutableStateOf("") }
    var highlights by remember { mutableStateOf("") }
    var nextSteps by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    RunwaySheetHeader(
        iconRes = R.drawable.ic_biz_create_trending,
        emojiFallback = BusinessQuickAddKind.INVESTOR_UPDATE.emoji(),
        title = "Investor Update",
        explanation = "Will be dispatched to registered investors",
        accent = accent,
        onClose = onDismiss,
    )
    FieldBlock("Update Type") {
        RunwaySegmentedControl(InvestorTypes, updateType, accent) { updateType = it }
    }
    FieldBlock("Subject") {
        RunwayTextField(subject, { subject = it }, "July 2026 Operations & Financials", accent)
    }
    FieldBlock("Key Metrics") {
        RunwayTextField(
            metrics, { metrics = it },
            "Revenue, MRR growth, client acquisition statistics...",
            accent, singleLine = false, minHeight = 70,
        )
    }
    FieldBlock("Runway Status") {
        RunwayDropdownField(runwayStatus, RunwayStatuses, { runwayStatus = it }, "Select runway status")
    }
    FieldBlock("Highlights") {
        RunwayTextField(
            highlights, { highlights = it },
            "Key wins, product milestones achieved...",
            accent, singleLine = false, minHeight = 70,
        )
    }
    FieldBlock("Next Steps") {
        RunwayTextField(
            nextSteps, { nextSteps = it },
            "Strategic goals for the upcoming period...",
            accent, singleLine = false, minHeight = 70,
        )
    }
    RunwayErrorText(error)
    RunwayPrimaryCta(
        label = if (submitting) "Saving…" else "Send Update",
        enabled = !momentId.isNullOrBlank() && subject.isNotBlank() && !submitting,
        loading = submitting,
        footerHint = "Will be dispatched to registered investors",
        accent = accent,
        onClick = {
            val id = momentId ?: return@RunwayPrimaryCta
            submitting = true
            error = null
            scope.launch {
                repository.createInvestorUpdate(
                    momentId = id,
                    body = CreateInvestorUpdateBody(
                        updateType = updateType.uppercase(),
                        subject = subject.trim(),
                        keyMetrics = metrics.trim().ifBlank { null },
                        runwayStatus = runwayStatus.ifBlank { null },
                        highlights = highlights.trim().ifBlank { null },
                        nextSteps = nextSteps.trim().ifBlank { null },
                    ),
                ).fold(
                    onSuccess = { submitting = false; onSaved(); onDismiss() },
                    onFailure = { submitting = false; error = it.message ?: "Could not send update" },
                )
            }
        },
    )
}

@Composable
private fun RunwayBudgetForm(
    momentId: String?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    repository: BusinessSliceRepository,
) {
    val accent = RunwayRedAccent
    var department by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var allocated by remember { mutableStateOf("") }
    var spend by remember { mutableStateOf("") }
    var severity by remember { mutableStateOf("Overrun") }
    var action by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    RunwaySheetHeader(
        iconRes = R.drawable.ic_biz_create_credit_card,
        emojiFallback = BusinessQuickAddKind.BUDGET_ALERT.emoji(),
        title = "Budget Alert",
        explanation = "Triggers urgent leadership notifications",
        accent = accent,
        onClose = onDismiss,
    )
    FieldBlock("Department") {
        RunwayDropdownField(department, Departments, { department = it }, "Select department")
    }
    FieldBlock("Category") {
        RunwayDropdownField(category, BudgetCategories, { category = it }, "Select category")
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            RunwayFieldLabel("Budget Allocated")
            RunwayAmountField(allocated, { allocated = it }, accent, "₹ 0")
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            RunwayFieldLabel("Current Spend")
            RunwayAmountField(spend, { spend = it }, accent, "₹ 0")
        }
    }
    FieldBlock("Severity") {
        RunwaySegmentedControl(Severities, severity, accent) { severity = it }
    }
    FieldBlock("Action Required") {
        RunwayTextField(
            action, { action = it },
            "Immediate containment measures required...",
            accent, singleLine = false, minHeight = 70,
        )
    }
    RunwayErrorText(error)
    RunwayPrimaryCta(
        label = if (submitting) "Saving…" else "Raise Alert",
        enabled = !momentId.isNullOrBlank() && department.isNotBlank() && !submitting,
        loading = submitting,
        footerHint = "Triggers urgent leadership notifications",
        accent = accent,
        onClick = {
            val id = momentId ?: return@RunwayPrimaryCta
            submitting = true
            error = null
            scope.launch {
                repository.createBudgetAlert(
                    momentId = id,
                    body = CreateBudgetAlertBody(
                        title = "Budget alert: ${department.ifBlank { "Dept" }} / ${category.ifBlank { "Category" }}",
                        metricLabel = category.ifBlank { null },
                        thresholdValue = runwayStripAmount(allocated).ifBlank { null },
                        currencyCode = "INR",
                        severity = severity.uppercase(),
                        note = joinParts(
                            "Spend: ${runwayStripAmount(spend)}",
                            action.takeIf { it.isNotBlank() },
                        ),
                    ),
                ).fold(
                    onSuccess = { submitting = false; onSaved(); onDismiss() },
                    onFailure = { submitting = false; error = it.message ?: "Could not raise alert" },
                )
            }
        },
    )
}

@Composable
private fun RunwayForecastForm(
    momentId: String?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    repository: BusinessSliceRepository,
) {
    val accent = RunwayAmberAccent
    var period by remember { mutableStateOf("Next Quarter") }
    var revenueProj by remember { mutableStateOf("") }
    var expenseProj by remember { mutableStateOf("") }
    var impact by remember { mutableStateOf("Extends") }
    var assumptions by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    RunwaySheetHeader(
        iconRes = R.drawable.ic_biz_create_trending,
        emojiFallback = BusinessQuickAddKind.FORECAST_UPDATE.emoji(),
        title = "Forecast Update",
        explanation = "Updates executive scenario models",
        accent = accent,
        onClose = onDismiss,
    )
    FieldBlock("Forecast Period") {
        RunwaySegmentedControl(ForecastPeriods, period, accent) { period = it }
    }
    FieldBlock("Revenue Projection") {
        RunwayAmountField(revenueProj, { revenueProj = it }, accent, "₹ Enter estimated incoming")
    }
    FieldBlock("Expense Projection") {
        RunwayAmountField(expenseProj, { expenseProj = it }, accent, "₹ Enter estimated outgoing")
    }
    FieldBlock("Runway Impact") {
        RunwaySegmentedControl(RunwayImpacts, impact, accent) { impact = it }
    }
    FieldBlock("Assumptions") {
        RunwayTextField(
            assumptions, { assumptions = it },
            "Describe models, client conversions and hiring assumptions...",
            accent, singleLine = false, minHeight = 70,
        )
    }
    RunwayErrorText(error)
    RunwayPrimaryCta(
        label = if (submitting) "Saving…" else "Update Forecast",
        enabled = !momentId.isNullOrBlank() && !submitting,
        loading = submitting,
        footerHint = "Updates executive scenario models",
        accent = accent,
        onClick = {
            val id = momentId ?: return@RunwayPrimaryCta
            submitting = true
            error = null
            scope.launch {
                val horizonMonths = if (period == "Next Month") 1 else 3
                val lines = mutableListOf<ForecastScenarioLineBody>()
                val revAmt = runwayStripAmount(revenueProj)
                val expAmt = runwayStripAmount(expenseProj)
                if (revAmt.isNotBlank()) {
                    lines.add(ForecastScenarioLineBody(lineLabel = "Revenue", amount = revAmt, currencyCode = "INR", periodLabel = period))
                }
                if (expAmt.isNotBlank()) {
                    lines.add(ForecastScenarioLineBody(lineLabel = "Expense", amount = expAmt, currencyCode = "INR", periodLabel = period))
                }
                repository.createForecastScenario(
                    momentId = id,
                    body = CreateForecastScenarioBody(
                        name = "Forecast · $period",
                        horizonMonths = horizonMonths,
                        assumptions = assumptions.trim().ifBlank { null },
                        lines = lines.ifEmpty { null },
                    ),
                ).fold(
                    onSuccess = { submitting = false; onSaved(); onDismiss() },
                    onFailure = { submitting = false; error = it.message ?: "Could not update forecast" },
                )
            }
        },
    )
}

@Composable
private fun RunwayInvoiceForm(
    momentId: String?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    repository: BusinessSliceRepository,
) {
    val accent = RunwayAmberAccent
    var client by remember { mutableStateOf("") }
    var invoiceNumber by remember { mutableStateOf("") }
    var amountDisplay by remember { mutableStateOf("") }
    var issueDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var dueDate by remember { mutableStateOf(LocalDate.now().plusDays(30).toString()) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    RunwaySheetHeader(
        iconRes = R.drawable.ic_biz_create_wallet,
        emojiFallback = BusinessQuickAddKind.INVOICE.emoji(),
        title = "Create Invoice",
        explanation = "Track a client invoice",
        accent = accent,
        onClose = onDismiss,
    )
    FieldBlock("Client") {
        RunwayTextField(client, { client = it }, "Client name", accent)
    }
    FieldBlock("Invoice Number") {
        RunwayTextField(invoiceNumber, { invoiceNumber = it }, "INV-001", accent)
    }
    FieldBlock("Amount") {
        RunwayAmountField(amountDisplay, { amountDisplay = it }, accent, "₹ Enter amount")
    }
    FieldBlock("Issue Date") { RunwayDateField(isoDate = issueDate, onIsoDateChange = { issueDate = it }) }
    FieldBlock("Due Date") { RunwayDateField(isoDate = dueDate, onIsoDateChange = { dueDate = it }) }
    RunwayErrorText(error)
    RunwayPrimaryCta(
        label = if (submitting) "Saving…" else "Create Invoice",
        enabled = !momentId.isNullOrBlank() &&
            invoiceNumber.isNotBlank() &&
            amountDisplay.isNotBlank() &&
            client.isNotBlank() &&
            !submitting,
        loading = submitting,
        footerHint = "Adds to receivables",
        accent = accent,
        onClick = {
            val id = momentId ?: return@RunwayPrimaryCta
            submitting = true
            error = null
            scope.launch {
                repository.createInvoice(
                    momentId = id,
                    body = CreateBusinessInvoiceBody(
                        invoiceNumber = invoiceNumber.trim(),
                        invoiceDate = issueDate,
                        dueDate = dueDate,
                        currencyCode = "INR",
                        lines = listOf(
                            BusinessInvoiceLineDto(
                                description = client.trim(),
                                quantity = "1",
                                unitPrice = runwayStripAmount(amountDisplay),
                                taxAmount = null,
                            ),
                        ),
                    ),
                ).fold(
                    onSuccess = { submitting = false; onSaved(); onDismiss() },
                    onFailure = { submitting = false; error = it.message ?: "Could not create invoice" },
                )
            }
        },
    )
}

@Composable
private fun RunwayUpdateForm(
    momentId: String?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    repository: BusinessSliceRepository,
) {
    val accent = RunwayAmberAccent
    var title by remember { mutableStateOf("") }
    var visibility by remember { mutableStateOf("Team") }
    var body by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    RunwaySheetHeader(
        iconRes = R.drawable.ic_biz_create_trending,
        emojiFallback = BusinessQuickAddKind.GENERAL_UPDATE.emoji(),
        title = "Update",
        explanation = "Share a runway status update",
        accent = accent,
        onClose = onDismiss,
    )
    FieldBlock("Title") {
        RunwayTextField(title, { title = it }, "What happened?", accent)
    }
    FieldBlock("Visibility") {
        RunwaySegmentedControl(UpdateVisibilities, visibility, accent) { visibility = it }
    }
    FieldBlock("Details") {
        RunwayTextField(body, { body = it }, "Write your update...", accent, singleLine = false, minHeight = 90)
    }
    RunwayErrorText(error)
    RunwayPrimaryCta(
        label = if (submitting) "Saving…" else "Post Update",
        enabled = !momentId.isNullOrBlank() && title.isNotBlank() && !submitting,
        loading = submitting,
        footerHint = "Visible on Moments timeline",
        accent = accent,
        onClick = {
            val id = momentId ?: return@RunwayPrimaryCta
            submitting = true
            error = null
            scope.launch {
                repository.createBusinessUpdate(
                    momentId = id,
                    body = CreateBusinessUpdateBody(
                        title = title.trim(),
                        body = joinParts("Visibility: $visibility", body.takeIf { it.isNotBlank() })
                            ?: title.trim(),
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
private fun RunwayMemoryForm(
    momentId: String?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    repository: BusinessSliceRepository,
) {
    val accent = RunwayAmberAccent
    var title by remember { mutableStateOf("") }
    var memoryBody by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    RunwaySheetHeader(
        iconRes = R.drawable.ic_biz_create_trending,
        emojiFallback = BusinessQuickAddKind.MEMORY.emoji(),
        title = "Record Learning",
        explanation = "Capture a runway insight",
        accent = accent,
        onClose = onDismiss,
    )
    FieldBlock("Title") {
        RunwayTextField(title, { title = it }, "Learning title", accent)
    }
    FieldBlock("Date") { RunwayDateField(isoDate = date, onIsoDateChange = { date = it }) }
    FieldBlock("Details") {
        RunwayTextField(
            memoryBody, { memoryBody = it },
            "What did you learn?",
            accent, singleLine = false, minHeight = 90,
        )
    }
    RunwayErrorText(error)
    RunwayPrimaryCta(
        label = if (submitting) "Saving…" else "Record Learning",
        enabled = !momentId.isNullOrBlank() && title.isNotBlank() && !submitting,
        loading = submitting,
        footerHint = "Adds to Runway Memory",
        accent = accent,
        onClick = {
            val id = momentId ?: return@RunwayPrimaryCta
            submitting = true
            error = null
            scope.launch {
                repository.createMemory(
                    momentId = id,
                    body = CreateBusinessMemoryBody(
                        title = title.trim(),
                        body = memoryBody.takeIf { it.isNotBlank() },
                        memoryType = "LEARNING",
                        occurredAt = date,
                    ),
                ).fold(
                    onSuccess = { submitting = false; onSaved(); onDismiss() },
                    onFailure = { submitting = false; error = it.message ?: "Could not record learning" },
                )
            }
        },
    )
}

private fun joinParts(vararg parts: String?): String? =
    parts.filterNotNull().filter { it.isNotBlank() }.joinToString(" · ").takeIf { it.isNotBlank() }
