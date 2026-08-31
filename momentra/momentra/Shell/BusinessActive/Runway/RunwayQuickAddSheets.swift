import SwiftUI

private let RevenueSources = ["Product", "Services", "Subscription", "Other"]
private let Clients = ["Internal", "Client A", "Client B", "New project"]
private let ExpenseCategories = ["OPS", "PURCHASE", "SOFTWARE", "TRAVEL", "OTHER"]
private let TaxTypes = ["GST", "TDS", "Income Tax", "Other"]
private let TaxPeriods = ["Q1", "Q2", "Q3", "Q4"]
private let TaxStatuses = ["Filed", "Pending", "Overdue"]
private let InvestorTypes = ["Monthly", "Quarterly", "Ad-hoc"]
private let RunwayStatuses = [
    "6 Months (Tight)",
    "12 Months (Watch)",
    "18 Months (Stable)",
    "36 Months (Secure)",
]
private let Departments = ["Engineering", "Marketing", "Operations", "Sales", "Finance"]
private let BudgetCategories = ["Salaries", "Software", "Marketing", "Infrastructure", "Other"]
private let Severities = ["Warning", "Critical", "Overrun"]
private let ForecastPeriods = ["Next Month", "Next Quarter"]
private let RunwayImpacts = ["Extends", "Neutral", "Shortens"]
private let UpdateVisibilities = ["Team", "Company", "Private"]

struct RunwayQuickAddSheet: View {
    let kind: BusinessQuickAddKind
    var momentId: String? = nil
    var onClose: () -> Void
    var onSaved: () -> Void = {}

    var body: some View {
        ZStack(alignment: .top) {
            Color(hex: "#161B26").ignoresSafeArea()
            VStack(spacing: 0) {
                Capsule()
                    .fill(Color(hex: "#625E70"))
                    .frame(width: 48, height: 4)
                    .padding(.top, 12)
                ScrollView {
                    VStack(alignment: .leading, spacing: 16) {
                        switch kind {
                        case .revenue:
                            RunwayRevenueForm(momentId: momentId, onDismiss: onClose, onSaved: onSaved)
                        case .expense, .spendEntry:
                            RunwayExpenseForm(momentId: momentId, onDismiss: onClose, onSaved: onSaved)
                        case .taxEntry:
                            RunwayTaxForm(momentId: momentId, onDismiss: onClose, onSaved: onSaved)
                        case .investorUpdate:
                            RunwayInvestorForm(momentId: momentId, onDismiss: onClose, onSaved: onSaved)
                        case .budgetAlert:
                            RunwayBudgetForm(momentId: momentId, onDismiss: onClose, onSaved: onSaved)
                        case .forecastUpdate:
                            RunwayForecastForm(momentId: momentId, onDismiss: onClose, onSaved: onSaved)
                        case .invoice:
                            RunwayInvoiceForm(momentId: momentId, onDismiss: onClose, onSaved: onSaved)
                        case .generalUpdate, .teamUpdate:
                            RunwayUpdateForm(momentId: momentId, onDismiss: onClose, onSaved: onSaved)
                        case .memory:
                            RunwayMemoryForm(momentId: momentId, onDismiss: onClose, onSaved: onSaved)
                        default:
                            EmptyView()
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 16)
                    .padding(.bottom, 28)
                }
            }
        }
    }
}

// MARK: - Revenue Form

private struct RunwayRevenueForm: View {
    var momentId: String?
    var onDismiss: () -> Void
    var onSaved: () -> Void

    @State private var source = ""
    @State private var amountDisplay = ""
    @State private var revenueType = "Recurring"
    @State private var client = ""
    @State private var date = SetupDateTimeUtils.localDateString(from: Date())
    @State private var notes = ""
    @State private var submitting = false
    @State private var error: String?

    private let accent = RunwaySheetAccent.amber

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            RunwaySheetHeader(
                emoji: "📈",
                title: "Log Revenue",
                explanation: "Directly added to corporate runway",
                accent: accent,
                onClose: onDismiss
            )
            
            FieldBlock(label: "Source") {
                RunwayDropdownField(value: source, options: RevenueSources, onSelect: { source = $0 }, placeholder: "Select source")
            }
            
            FieldBlock(label: "Amount") {
                RunwayAmountField(displayValue: $amountDisplay, placeholder: "₹ Enter revenue amount", accent: accent)
            }
            
            FieldBlock(label: "Type") {
                RunwaySegmentedControl(options: ["Recurring", "One-time"], selected: $revenueType, accent: accent)
            }
            
            FieldBlock(label: "Client/Project") {
                RunwayDropdownField(value: client, options: Clients, onSelect: { client = $0 }, placeholder: "Select client or project")
            }
            
            FieldBlock(label: "Date") {
                RunwayDateField(isoDate: $date)
            }
            
            FieldBlock(label: "Notes") {
                RunwayTextField(value: $notes, placeholder: "Add internal payment notes...", minHeight: 70, singleLine: false, accent: accent)
            }
            
            RunwayErrorText(message: error)
            
            RunwayPrimaryCta(
                label: submitting ? "Saving…" : "Log Revenue",
                enabled: !momentId.isNullOrBlank && !amountDisplay.isEmpty && !submitting,
                loading: submitting,
                footerHint: "Directly added to corporate runway",
                accent: accent
            ) {
                Task { await submit() }
            }
        }
    }

    private func submit() async {
        guard let momentId, !amountDisplay.isEmpty else { return }
        submitting = true
        error = nil
        let strippedAmount = RunwayAmountFormat.strip(amountDisplay)
        let descParts = [
            "Source: \(source.isEmpty ? "—" : source)",
            "Type: \(revenueType)",
            client.isEmpty ? nil : "Client: \(client)",
            "Date: \(date)",
            notes.isEmpty ? nil : notes
        ].compactMap { $0 }
        
        do {
            _ = try await APIClient.shared.createBusinessRevenue(
                momentId: momentId,
                amount: strippedAmount,
                currencyCode: "INR",
                description: descParts.joined(separator: " · "),
                categoryCode: source.isEmpty ? nil : source
            )
            onSaved()
            onDismiss()
        } catch {
            self.error = error.localizedDescription
        }
        submitting = false
    }
}

// MARK: - Expense Form

private struct RunwayExpenseForm: View {
    var momentId: String?
    var onDismiss: () -> Void
    var onSaved: () -> Void

    @State private var category = ""
    @State private var amountDisplay = ""
    @State private var merchant = ""
    @State private var date = SetupDateTimeUtils.localDateString(from: Date())
    @State private var notes = ""
    @State private var submitting = false
    @State private var error: String?

    private let accent = RunwaySheetAccent.amber

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            RunwaySheetHeader(
                emoji: "💳",
                title: "Log Expense",
                explanation: "Record business spend",
                accent: accent,
                onClose: onDismiss
            )
            
            FieldBlock(label: "Category") {
                RunwayDropdownField(value: category, options: ExpenseCategories, onSelect: { category = $0 }, placeholder: "Select category")
            }
            
            FieldBlock(label: "Amount") {
                RunwayAmountField(displayValue: $amountDisplay, placeholder: "₹ Enter expense amount", accent: accent)
            }
            
            FieldBlock(label: "Merchant") {
                RunwayTextField(value: $merchant, placeholder: "Vendor or merchant", accent: accent)
            }
            
            FieldBlock(label: "Date") {
                RunwayDateField(isoDate: $date)
            }
            
            FieldBlock(label: "Notes") {
                RunwayTextField(value: $notes, placeholder: "Optional notes...", minHeight: 70, singleLine: false, accent: accent)
            }
            
            RunwayErrorText(message: error)
            
            RunwayPrimaryCta(
                label: submitting ? "Saving…" : "Log Expense",
                enabled: !momentId.isNullOrBlank && !amountDisplay.isEmpty && !submitting,
                loading: submitting,
                footerHint: "Updates burn and runway",
                accent: accent
            ) {
                Task { await submit() }
            }
        }
    }

    private func submit() async {
        guard let momentId, !amountDisplay.isEmpty else { return }
        submitting = true
        error = nil
        let strippedAmount = RunwayAmountFormat.strip(amountDisplay)
        let descParts = ["Date: \(date)", notes.isEmpty ? nil : notes].compactMap { $0 }
        
        do {
            _ = try await APIClient.shared.createBusinessExpense(
                momentId: momentId,
                amount: strippedAmount,
                currencyCode: "INR",
                description: descParts.isEmpty ? nil : descParts.joined(separator: " · "),
                merchantName: merchant.isEmpty ? nil : merchant,
                categoryCode: category.isEmpty ? nil : category
            )
            onSaved()
            onDismiss()
        } catch {
            self.error = error.localizedDescription
        }
        submitting = false
    }
}

// MARK: - Tax Form

private struct RunwayTaxForm: View {
    var momentId: String?
    var onDismiss: () -> Void
    var onSaved: () -> Void

    @State private var taxType = "GST"
    @State private var period = "Q2"
    @State private var amountDisplay = ""
    @State private var dueDate = SetupDateTimeUtils.localDateString(from: Date())
    @State private var status = "Pending"
    @State private var notes = ""
    @State private var submitting = false
    @State private var error: String?

    private let accent = RunwaySheetAccent.emerald

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            RunwaySheetHeader(
                emoji: "📑",
                title: "Tax Entry",
                explanation: "Keeps financial timeline compliant",
                accent: accent,
                onClose: onDismiss
            )
            
            FieldBlock(label: "Tax Type") {
                RunwayDropdownField(value: taxType, options: TaxTypes, onSelect: { taxType = $0 }, placeholder: "Select tax type")
            }
            
            FieldBlock(label: "Period") {
                RunwaySegmentedControl(options: TaxPeriods, selected: $period, accent: accent)
            }
            
            FieldBlock(label: "Amount") {
                RunwayAmountField(displayValue: $amountDisplay, placeholder: "₹ Enter tax liability", accent: accent)
            }
            
            FieldBlock(label: "Due Date") {
                RunwayDateField(isoDate: $dueDate)
            }
            
            FieldBlock(label: "Status") {
                RunwaySegmentedControl(options: TaxStatuses, selected: $status, accent: accent)
            }
            
            FieldBlock(label: "Notes") {
                RunwayTextField(value: $notes, placeholder: "Tax compliance reference notes...", minHeight: 70, singleLine: false, accent: accent)
            }
            
            RunwayErrorText(message: error)
            
            RunwayPrimaryCta(
                label: submitting ? "Saving…" : "Save Tax Entry",
                enabled: !momentId.isNullOrBlank && !amountDisplay.isEmpty && !submitting,
                loading: submitting,
                footerHint: "Keeps financial timeline compliant",
                accent: accent
            ) {
                Task { await submit() }
            }
        }
    }

    private func submit() async {
        guard let momentId, !amountDisplay.isEmpty else { return }
        submitting = true
        error = nil
        let strippedAmount = RunwayAmountFormat.strip(amountDisplay)
        let notesParts = [
            "Status: \(status)",
            notes.isEmpty ? nil : notes
        ].compactMap { $0 }
        
        do {
            _ = try await APIClient.shared.createTaxObligation(
                momentId: momentId,
                title: "\(taxType) · \(period)",
                taxType: taxType,
                amount: strippedAmount.isEmpty ? nil : strippedAmount,
                currencyCode: "INR",
                dueDate: dueDate,
                notes: notesParts.isEmpty ? nil : notesParts.joined(separator: " · ")
            )
            onSaved()
            onDismiss()
        } catch {
            self.error = error.localizedDescription
        }
        submitting = false
    }
}

// MARK: - Investor Update Form

private struct RunwayInvestorForm: View {
    var momentId: String?
    var onDismiss: () -> Void
    var onSaved: () -> Void

    @State private var updateType = "Monthly"
    @State private var subject = ""
    @State private var metrics = ""
    @State private var runwayStatus = ""
    @State private var highlights = ""
    @State private var nextSteps = ""
    @State private var submitting = false
    @State private var error: String?

    private let accent = RunwaySheetAccent.lavender

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            RunwaySheetHeader(
                emoji: "📣",
                title: "Investor Update",
                explanation: "Will be dispatched to registered investors",
                accent: accent,
                onClose: onDismiss
            )
            
            FieldBlock(label: "Update Type") {
                RunwaySegmentedControl(options: InvestorTypes, selected: $updateType, accent: accent)
            }
            
            FieldBlock(label: "Subject") {
                RunwayTextField(value: $subject, placeholder: "July 2026 Operations & Financials", accent: accent)
            }
            
            FieldBlock(label: "Key Metrics") {
                RunwayTextField(value: $metrics, placeholder: "Revenue, MRR growth, client acquisition statistics...", minHeight: 70, singleLine: false, accent: accent)
            }
            
            FieldBlock(label: "Runway Status") {
                RunwayDropdownField(value: runwayStatus, options: RunwayStatuses, onSelect: { runwayStatus = $0 }, placeholder: "Select runway status")
            }
            
            FieldBlock(label: "Highlights") {
                RunwayTextField(value: $highlights, placeholder: "Key wins, product milestones achieved...", minHeight: 70, singleLine: false, accent: accent)
            }
            
            FieldBlock(label: "Next Steps") {
                RunwayTextField(value: $nextSteps, placeholder: "Strategic goals for the upcoming period...", minHeight: 70, singleLine: false, accent: accent)
            }
            
            RunwayErrorText(message: error)
            
            RunwayPrimaryCta(
                label: submitting ? "Saving…" : "Send Update",
                enabled: !momentId.isNullOrBlank && !subject.isEmpty && !submitting,
                loading: submitting,
                footerHint: "Will be dispatched to registered investors",
                accent: accent
            ) {
                Task { await submit() }
            }
        }
    }

    private func submit() async {
        guard let momentId, !subject.isEmpty else { return }
        submitting = true
        error = nil
        
        do {
            _ = try await APIClient.shared.createInvestorUpdate(
                momentId: momentId,
                updateType: updateType.uppercased(),
                subject: subject.trimmingCharacters(in: .whitespaces),
                keyMetrics: metrics.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : metrics.trimmingCharacters(in: .whitespacesAndNewlines),
                runwayStatus: runwayStatus.isEmpty ? nil : runwayStatus,
                highlights: highlights.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : highlights.trimmingCharacters(in: .whitespacesAndNewlines),
                nextSteps: nextSteps.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : nextSteps.trimmingCharacters(in: .whitespacesAndNewlines)
            )
            onSaved()
            onDismiss()
        } catch {
            self.error = error.localizedDescription
        }
        submitting = false
    }
}

// MARK: - Budget Alert Form

private struct RunwayBudgetForm: View {
    var momentId: String?
    var onDismiss: () -> Void
    var onSaved: () -> Void

    @State private var department = ""
    @State private var category = ""
    @State private var allocatedDisplay = ""
    @State private var spendDisplay = ""
    @State private var severity = "Overrun"
    @State private var action = ""
    @State private var submitting = false
    @State private var error: String?

    private let accent = RunwaySheetAccent.red

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            RunwaySheetHeader(
                emoji: "🚨",
                title: "Budget Alert",
                explanation: "Triggers urgent leadership notifications",
                accent: accent,
                onClose: onDismiss
            )
            
            FieldBlock(label: "Department") {
                RunwayDropdownField(value: department, options: Departments, onSelect: { department = $0 }, placeholder: "Select department")
            }
            
            FieldBlock(label: "Category") {
                RunwayDropdownField(value: category, options: BudgetCategories, onSelect: { category = $0 }, placeholder: "Select category")
            }
            
            HStack(spacing: 12) {
                VStack(alignment: .leading, spacing: 8) {
                    RunwayFieldLabel(text: "Budget Allocated")
                    RunwayAmountField(displayValue: $allocatedDisplay, placeholder: "₹ 0", accent: accent)
                }
                VStack(alignment: .leading, spacing: 8) {
                    RunwayFieldLabel(text: "Current Spend")
                    RunwayAmountField(displayValue: $spendDisplay, placeholder: "₹ 0", accent: accent)
                }
            }
            
            FieldBlock(label: "Severity") {
                RunwaySegmentedControl(options: Severities, selected: $severity, accent: accent)
            }
            
            FieldBlock(label: "Action Required") {
                RunwayTextField(value: $action, placeholder: "Immediate containment measures required...", minHeight: 70, singleLine: false, accent: accent)
            }
            
            RunwayErrorText(message: error)
            
            RunwayPrimaryCta(
                label: submitting ? "Saving…" : "Raise Alert",
                enabled: !momentId.isNullOrBlank && !department.isEmpty && !submitting,
                loading: submitting,
                footerHint: "Triggers urgent leadership notifications",
                accent: accent
            ) {
                Task { await submit() }
            }
        }
    }

    private func submit() async {
        guard let momentId, !department.isEmpty else { return }
        submitting = true
        error = nil
        let noteParts = [
            "Spend: \(RunwayAmountFormat.strip(spendDisplay))",
            action.isEmpty ? nil : action
        ].compactMap { $0 }
        
        do {
            _ = try await APIClient.shared.createBudgetAlert(
                momentId: momentId,
                title: "Budget alert: \(department.isEmpty ? "Dept" : department) / \(category.isEmpty ? "Category" : category)",
                metricLabel: category.isEmpty ? nil : category,
                thresholdValue: RunwayAmountFormat.strip(allocatedDisplay).isEmpty ? nil : RunwayAmountFormat.strip(allocatedDisplay),
                currencyCode: "INR",
                severity: severity.uppercased(),
                note: noteParts.isEmpty ? nil : noteParts.joined(separator: " · ")
            )
            onSaved()
            onDismiss()
        } catch {
            self.error = error.localizedDescription
        }
        submitting = false
    }
}

// MARK: - Forecast Update Form

private struct RunwayForecastForm: View {
    var momentId: String?
    var onDismiss: () -> Void
    var onSaved: () -> Void

    @State private var period = "Next Quarter"
    @State private var revenueProjDisplay = ""
    @State private var expenseProjDisplay = ""
    @State private var impact = "Extends"
    @State private var assumptions = ""
    @State private var submitting = false
    @State private var error: String?

    private let accent = RunwaySheetAccent.amber

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            RunwaySheetHeader(
                emoji: "📉",
                title: "Forecast Update",
                explanation: "Updates executive scenario models",
                accent: accent,
                onClose: onDismiss
            )
            
            FieldBlock(label: "Forecast Period") {
                RunwaySegmentedControl(options: ForecastPeriods, selected: $period, accent: accent)
            }
            
            FieldBlock(label: "Revenue Projection") {
                RunwayAmountField(displayValue: $revenueProjDisplay, placeholder: "₹ Enter estimated incoming", accent: accent)
            }
            
            FieldBlock(label: "Expense Projection") {
                RunwayAmountField(displayValue: $expenseProjDisplay, placeholder: "₹ Enter estimated outgoing", accent: accent)
            }
            
            FieldBlock(label: "Runway Impact") {
                RunwaySegmentedControl(options: RunwayImpacts, selected: $impact, accent: accent)
            }
            
            FieldBlock(label: "Assumptions") {
                RunwayTextField(value: $assumptions, placeholder: "Describe models, client conversions and hiring assumptions...", minHeight: 70, singleLine: false, accent: accent)
            }
            
            RunwayErrorText(message: error)
            
            RunwayPrimaryCta(
                label: submitting ? "Saving…" : "Update Forecast",
                enabled: !momentId.isNullOrBlank && !submitting,
                loading: submitting,
                footerHint: "Updates executive scenario models",
                accent: accent
            ) {
                Task { await submit() }
            }
        }
    }

    private func submit() async {
        guard let momentId else { return }
        submitting = true
        error = nil
        let horizonMonths = period == "Next Month" ? 1 : 3
        var lines = [APIClient.ForecastLineInput]()
        let revAmt = RunwayAmountFormat.strip(revenueProjDisplay)
        let expAmt = RunwayAmountFormat.strip(expenseProjDisplay)
        if !revAmt.isEmpty {
            lines.append(APIClient.ForecastLineInput(lineLabel: "Revenue", amount: revAmt, currencyCode: "INR", periodLabel: period))
        }
        if !expAmt.isEmpty {
            lines.append(APIClient.ForecastLineInput(lineLabel: "Expense", amount: expAmt, currencyCode: "INR", periodLabel: period))
        }
        
        do {
            _ = try await APIClient.shared.createForecastScenario(
                momentId: momentId,
                name: "Forecast · \(period)",
                horizonMonths: horizonMonths,
                assumptions: assumptions.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : assumptions.trimmingCharacters(in: .whitespacesAndNewlines),
                lines: lines.isEmpty ? nil : lines
            )
            onSaved()
            onDismiss()
        } catch {
            self.error = error.localizedDescription
        }
        submitting = false
    }
}

// MARK: - Invoice Form

private struct RunwayInvoiceForm: View {
    var momentId: String?
    var onDismiss: () -> Void
    var onSaved: () -> Void

    @State private var client = ""
    @State private var invoiceNumber = ""
    @State private var amountDisplay = ""
    @State private var issueDate = SetupDateTimeUtils.localDateString(from: Date())
    @State private var dueDate = SetupDateTimeUtils.localDateString(from: Date().addingTimeInterval(30 * 24 * 3600))
    @State private var submitting = false
    @State private var error: String?

    private let accent = RunwaySheetAccent.amber

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            RunwaySheetHeader(
                emoji: "📄",
                title: "Create Invoice",
                explanation: "Track a client invoice",
                accent: accent,
                onClose: onDismiss
            )
            
            FieldBlock(label: "Client") {
                RunwayTextField(value: $client, placeholder: "Client name", accent: accent)
            }
            
            FieldBlock(label: "Invoice Number") {
                RunwayTextField(value: $invoiceNumber, placeholder: "INV-001", accent: accent)
            }
            
            FieldBlock(label: "Amount") {
                RunwayAmountField(displayValue: $amountDisplay, placeholder: "₹ Enter amount", accent: accent)
            }
            
            FieldBlock(label: "Issue Date") {
                RunwayDateField(isoDate: $issueDate)
            }
            
            FieldBlock(label: "Due Date") {
                RunwayDateField(isoDate: $dueDate)
            }
            
            RunwayErrorText(message: error)
            
            RunwayPrimaryCta(
                label: submitting ? "Saving…" : "Create Invoice",
                enabled: !momentId.isNullOrBlank && !invoiceNumber.isEmpty && !amountDisplay.isEmpty && !client.isEmpty && !submitting,
                loading: submitting,
                footerHint: "Adds to receivables",
                accent: accent
            ) {
                Task { await submit() }
            }
        }
    }

    private func submit() async {
        guard let momentId, !invoiceNumber.isEmpty, !amountDisplay.isEmpty, !client.isEmpty else { return }
        submitting = true
        error = nil
        let strippedAmount = RunwayAmountFormat.strip(amountDisplay)
        
        do {
            _ = try await APIClient.shared.createBusinessInvoice(
                momentId: momentId,
                invoiceNumber: invoiceNumber.trimmingCharacters(in: .whitespaces),
                invoiceDate: issueDate,
                dueDate: dueDate,
                currencyCode: "INR",
                lines: [
                    BusinessInvoiceLineInput(
                        description: client.trimmingCharacters(in: .whitespaces),
                        quantity: "1",
                        unitPrice: strippedAmount,
                        taxAmount: nil
                    )
                ]
            )
            onSaved()
            onDismiss()
        } catch {
            self.error = error.localizedDescription
        }
        submitting = false
    }
}

// MARK: - General Update Form

private struct RunwayUpdateForm: View {
    var momentId: String?
    var onDismiss: () -> Void
    var onSaved: () -> Void

    @State private var title = ""
    @State private var visibility = "Team"
    @State private var body = ""
    @State private var submitting = false
    @State private var error: String?

    private let accent = RunwaySheetAccent.amber

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            RunwaySheetHeader(
                emoji: "📢",
                title: "Update",
                explanation: "Share a runway status update",
                accent: accent,
                onClose: onDismiss
            )
            
            FieldBlock(label: "Title") {
                RunwayTextField(value: $title, placeholder: "What happened?", accent: accent)
            }
            
            FieldBlock(label: "Visibility") {
                RunwaySegmentedControl(options: UpdateVisibilities, selected: $visibility, accent: accent)
            }
            
            FieldBlock(label: "Details") {
                RunwayTextField(value: $body, placeholder: "Write your update...", minHeight: 90, singleLine: false, accent: accent)
            }
            
            RunwayErrorText(message: error)
            
            RunwayPrimaryCta(
                label: submitting ? "Saving…" : "Post Update",
                enabled: !momentId.isNullOrBlank && !title.isEmpty && !submitting,
                loading: submitting,
                footerHint: "Visible on Moments timeline",
                accent: accent
            ) {
                Task { await submit() }
            }
        }
    }

    private func submit() async {
        guard let momentId, !title.isEmpty else { return }
        submitting = true
        error = nil
        let bodyParts = ["Visibility: \(visibility)", body.isEmpty ? nil : body].compactMap { $0 }
        
        do {
            _ = try await APIClient.shared.createBusinessUpdate(
                momentId: momentId,
                body: bodyParts.isEmpty ? title.trimmingCharacters(in: .whitespaces) : bodyParts.joined(separator: " · "),
                title: title.trimmingCharacters(in: .whitespaces)
            )
            onSaved()
            onDismiss()
        } catch {
            self.error = error.localizedDescription
        }
        submitting = false
    }
}

// MARK: - Memory Form

private struct RunwayMemoryForm: View {
    var momentId: String?
    var onDismiss: () -> Void
    var onSaved: () -> Void

    @State private var title = ""
    @State private var memoryBody = ""
    @State private var date = SetupDateTimeUtils.localDateString(from: Date())
    @State private var submitting = false
    @State private var error: String?

    private let accent = RunwaySheetAccent.amber

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            RunwaySheetHeader(
                emoji: "📦",
                title: "Record Learning",
                explanation: "Capture a runway insight",
                accent: accent,
                onClose: onDismiss
            )
            
            FieldBlock(label: "Title") {
                RunwayTextField(value: $title, placeholder: "Learning title", accent: accent)
            }
            
            FieldBlock(label: "Date") {
                RunwayDateField(isoDate: $date)
            }
            
            FieldBlock(label: "Details") {
                RunwayTextField(value: $memoryBody, placeholder: "What did you learn?", minHeight: 90, singleLine: false, accent: accent)
            }
            
            RunwayErrorText(message: error)
            
            RunwayPrimaryCta(
                label: submitting ? "Saving…" : "Record Learning",
                enabled: !momentId.isNullOrBlank && !title.isEmpty && !submitting,
                loading: submitting,
                footerHint: "Adds to Runway Memory",
                accent: accent
            ) {
                Task { await submit() }
            }
        }
    }

    private func submit() async {
        guard let momentId, !title.isEmpty else { return }
        submitting = true
        error = nil
        
        do {
            _ = try await APIClient.shared.createBusinessMemory(
                momentId: momentId,
                title: title.trimmingCharacters(in: .whitespaces),
                body: memoryBody.isEmpty ? nil : memoryBody.trimmingCharacters(in: .whitespaces),
                memoryType: "LEARNING"
            )
            onSaved()
            onDismiss()
        } catch {
            self.error = error.localizedDescription
        }
        submitting = false
    }
}

// MARK: - Helper Views

private struct FieldBlock<Content: View>: View {
    let label: String
    @ViewBuilder let content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            RunwayFieldLabel(text: label)
            content()
        }
    }
}

// MARK: - Extensions

private extension Optional where Wrapped == String {
    var isNullOrBlank: Bool {
        guard let value = self else { return true }
        return value.trimmingCharacters(in: .whitespaces).isEmpty
    }
}
