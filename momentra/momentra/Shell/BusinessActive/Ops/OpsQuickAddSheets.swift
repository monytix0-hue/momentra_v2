import SwiftUI

enum OpsQuickAddSheets {
    static func isOpsKind(_ kind: BusinessQuickAddKind) -> Bool {
        switch kind {
        case .spendEntry, .updateVendor, .requestApproval, .reportIssue, .logImprovement,
             .budgetReview, .slaCheck, .generalUpdate, .memory, .expense:
            return true
        default:
            return false
        }
    }
}

struct OpsQuickAddSheet: View {
    let kind: BusinessQuickAddKind
    var momentId: String? = nil
    var companyId: String? = nil
    var momentTitle: String? = nil
    var onClose: () -> Void
    var onSaved: () -> Void = {}

    var body: some View {
        NativeSheetScaffold(
            title: kind.label,
            onClose: onClose,
            background: OpsSheetTokens.sheetBg
        ) {
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    switch kind {
                    case .spendEntry, .expense:
                        OpsSpendForm(momentId: momentId, onClose: onClose, onSaved: onSaved)
                    case .updateVendor:
                        OpsVendorForm(companyId: companyId, onClose: onClose, onSaved: onSaved)
                    case .requestApproval:
                        OpsApprovalForm(momentId: momentId, onClose: onClose, onSaved: onSaved)
                    case .reportIssue:
                        OpsIssueForm(momentId: momentId, onClose: onClose, onSaved: onSaved)
                    case .logImprovement:
                        OpsImprovementForm(momentId: momentId, onClose: onClose, onSaved: onSaved)
                    case .budgetReview:
                        OpsBudgetReviewForm(momentId: momentId, onClose: onClose, onSaved: onSaved)
                    case .slaCheck:
                        OpsSlaForm(companyId: companyId, onClose: onClose, onSaved: onSaved)
                    case .generalUpdate:
                        OpsGeneralUpdateForm(momentId: momentId, onClose: onClose, onSaved: onSaved)
                    case .memory:
                        OpsMemoryForm(momentId: momentId, momentTitle: momentTitle, onClose: onClose, onSaved: onSaved)
                    default:
                        Text("Unsupported Ops command")
                            .font(.plusJakarta(size: 14))
                            .foregroundStyle(OpsSheetTokens.muted)
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 16)
            }
        }
        .presentationDetents([.large])
        .presentationDragIndicator(.visible)
    }
}

// MARK: - Helpers

private struct OpsFieldBlock<Content: View>: View {
    let label: String
    @ViewBuilder var content: Content
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            OpsFieldLabel(text: label)
            content
        }
    }
}

// MARK: - Spend

private struct OpsSpendForm: View {
    var momentId: String?
    var onClose: () -> Void
    var onSaved: () -> Void

    @State private var category = "Operations & Logistics"
    @State private var amountDisplay = ""
    @State private var vendor = ""
    @State private var isoDate = SetupDateTimeUtils.localDateString(from: Date())
    @State private var frequency = "One-time"
    @State private var notes = ""
    @State private var submitting = false
    @State private var error: String?

    private let categories = ["Operations & Logistics", "SaaS & Software", "Professional Services", "Marketing", "Office", "Travel", "Other"]
    private let frequencies = ["Recurring", "One-time", "Urgent"]

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            OpsSheetHeader(emoji: "💳", title: "Log Spend Entry", explanation: "Record an ops expense against budget", onClose: onClose)
            OpsFieldBlock(label: "Category") {
                OpsDropdownField(value: category, options: categories, onSelect: { category = $0 }, placeholder: "Select category")
            }
            OpsFieldBlock(label: "Amount") { OpsAmountField(displayValue: $amountDisplay) }
            OpsFieldBlock(label: "Vendor") { OpsTextField(value: $vendor, placeholder: "Vendor name") }
            OpsFieldBlock(label: "Date") { OpsDateField(isoDate: $isoDate) }
            OpsFieldBlock(label: "Frequency") { OpsChipRow(options: frequencies, selected: $frequency) }
            OpsFieldBlock(label: "Notes") {
                OpsTextField(value: $notes, placeholder: "Describe the business expense...", minHeight: 80, singleLine: false)
            }
            if let error { Text(error).font(.plusJakarta(size: 12)).foregroundStyle(OpsSheetTokens.error) }
            OpsPrimaryCta(
                label: submitting ? "Saving…" : "Log Spend",
                enabled: canSubmit && !submitting,
                loading: submitting,
                footerHint: "Transaction will be posted"
            ) { Task { await submit() } }
        }
    }

    private var canSubmit: Bool {
        guard momentId != nil else { return false }
        let amount = Double(OpsAmountFormat.strip(amountDisplay)) ?? 0
        return amount > 0
    }

    private func submit() async {
        guard let momentId else { return }
        let amount = OpsAmountFormat.strip(amountDisplay)
        submitting = true
        error = nil
        do {
            var parts = [String]()
            if !notes.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty { parts.append(notes.trimmingCharacters(in: .whitespacesAndNewlines)) }
            parts.append("Frequency: \(frequency)")
            parts.append("Date: \(isoDate)")
            _ = try await APIClient.shared.createBusinessExpense(
                momentId: momentId,
                amount: amount,
                currencyCode: "INR",
                description: parts.joined(separator: " · "),
                merchantName: vendor.isEmpty ? nil : vendor,
                categoryCode: String(category.uppercased().replacingOccurrences(of: " ", with: "_").prefix(32))
            )
            onSaved(); onClose()
        } catch {
            self.error = error.localizedDescription
        }
        submitting = false
    }
}

// MARK: - Vendor

private struct OpsVendorForm: View {
    var companyId: String?
    var onClose: () -> Void
    var onSaved: () -> Void

    @State private var name = ""
    @State private var category = "SaaS"
    @State private var contact = ""
    @State private var status = "Active"
    @State private var notes = ""
    @State private var submitting = false
    @State private var error: String?
    @State private var existingVendors: [APIClient.VendorItem] = []
    @State private var selectedVendor: APIClient.VendorItem?
    @State private var useExisting = false

    private let categories = ["SaaS", "Services", "Logistics", "Hardware", "Other"]
    private let statuses = ["Active", "On Hold", "Churn Risk"]

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            OpsSheetHeader(emoji: "🏷", title: "Update Vendor", explanation: "Add or update an ops supplier profile", onClose: onClose)
            if !existingVendors.isEmpty {
                OpsFieldBlock(label: "Select Existing") {
                    let vendorNames = ["Create new"] + existingVendors.map { $0.name }
                    OpsDropdownField(
                        value: useExisting && selectedVendor != nil ? selectedVendor!.name : "Create new",
                        options: vendorNames,
                        onSelect: { picked in
                            if let found = existingVendors.first(where: { $0.name == picked }) {
                                useExisting = true
                                selectedVendor = found
                                name = found.name
                            } else {
                                useExisting = false
                                selectedVendor = nil
                                name = ""
                            }
                        },
                        placeholder: "Select existing or create new"
                    )
                }
            }
            OpsFieldBlock(label: "Vendor Name") { OpsTextField(value: $name, placeholder: "Vendor name") }
            OpsFieldBlock(label: "Category") {
                OpsDropdownField(value: category, options: categories, onSelect: { category = $0 }, placeholder: "Select category")
            }
            OpsFieldBlock(label: "Contact Person") { OpsTextField(value: $contact, placeholder: "Optional contact") }
            OpsFieldBlock(label: "Status") { OpsChipRow(options: statuses, selected: $status) }
            OpsFieldBlock(label: "Notes") {
                OpsTextField(value: $notes, placeholder: "Optional notes", minHeight: 80, singleLine: false)
            }
            if companyId == nil {
                Text("Select a company to manage vendors.")
                    .font(.plusJakarta(size: 12)).foregroundStyle(OpsSheetTokens.muted)
            }
            if let error { Text(error).font(.plusJakarta(size: 12)).foregroundStyle(OpsSheetTokens.error) }
            OpsPrimaryCta(
                label: submitting ? "Saving…" : (useExisting ? "Update Vendor" : "Create Vendor"),
                enabled: companyId != nil && !name.trimmingCharacters(in: .whitespaces).isEmpty && !submitting,
                loading: submitting,
                footerHint: "Vendor profile will sync"
            ) { Task { await submit() } }
        }
        .task {
            guard let companyId else { return }
            do {
                let result = try await APIClient.shared.listCompanyVendors(companyId: companyId)
                existingVendors = result.items
            } catch { /* non-blocking */ }
        }
    }

    private func submit() async {
        guard let companyId else { return }
        submitting = true
        error = nil
        do {
            var noteParts = [String]()
            if !contact.isEmpty { noteParts.append("Contact: \(contact)") }
            noteParts.append("Status: \(status)")
            if !notes.isEmpty { noteParts.append(notes) }
            if useExisting, let vendor = selectedVendor {
                _ = try await APIClient.shared.patchCompanyVendor(
                    companyId: companyId,
                    vendorId: vendor.vendorId,
                    name: name.trimmingCharacters(in: .whitespaces),
                    vendorType: category,
                    note: noteParts.isEmpty ? nil : noteParts.joined(separator: " · ")
                )
            } else {
                let created = try await APIClient.shared.createCompanyVendor(
                    companyId: companyId,
                    name: name.trimmingCharacters(in: .whitespaces),
                    vendorType: category
                )
                if !noteParts.isEmpty {
                    _ = try await APIClient.shared.patchCompanyVendor(
                        companyId: companyId,
                        vendorId: created.vendorId,
                        status: status.uppercased().replacingOccurrences(of: " ", with: "_"),
                        note: noteParts.joined(separator: " · ")
                    )
                }
            }
            onSaved(); onClose()
        } catch {
            self.error = error.localizedDescription
        }
        submitting = false
    }
}

// MARK: - Approval

private struct OpsApprovalForm: View {
    var momentId: String?
    var onClose: () -> Void
    var onSaved: () -> Void

    @State private var title = ""
    @State private var category = "Spend"
    @State private var amountDisplay = ""
    @State private var priority = "Normal"
    @State private var justification = ""
    @State private var submitting = false
    @State private var error: String?

    private let categories = ["Spend", "Hiring", "Vendor", "Scope Change", "Other"]
    private let priorities = ["Normal", "High", "Urgent"]

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            OpsSheetHeader(emoji: "✅", title: "Request Approval", explanation: "Route a spend or scope for sign-off", onClose: onClose)
            OpsFieldBlock(label: "Request Title") { OpsTextField(value: $title, placeholder: "What needs sign-off") }
            OpsFieldBlock(label: "Category") {
                OpsDropdownField(value: category, options: categories, onSelect: { category = $0 }, placeholder: "Select category")
            }
            OpsFieldBlock(label: "Amount") { OpsAmountField(displayValue: $amountDisplay) }
            OpsFieldBlock(label: "Priority") { OpsChipRow(options: priorities, selected: $priority) }
            OpsFieldBlock(label: "Justification") {
                OpsTextField(value: $justification, placeholder: "Why this needs approval...", minHeight: 80, singleLine: false)
            }
            if let error { Text(error).font(.plusJakarta(size: 12)).foregroundStyle(OpsSheetTokens.error) }
            OpsPrimaryCta(
                label: submitting ? "Saving…" : "Submit for Approval",
                enabled: momentId != nil && !title.trimmingCharacters(in: .whitespaces).isEmpty && !submitting,
                loading: submitting,
                footerHint: "Stakeholders will be notified"
            ) { Task { await submit() } }
        }
    }

    private func submit() async {
        guard let momentId else { return }
        submitting = true
        error = nil
        do {
            let amount = OpsAmountFormat.strip(amountDisplay)
            var noteParts = ["Category: \(category)", "Priority: \(priority)"]
            if !justification.isEmpty { noteParts.append(justification) }
            _ = try await APIClient.shared.createBusinessApprovalRequest(
                momentId: momentId,
                title: title.trimmingCharacters(in: .whitespaces),
                amount: amount.isEmpty ? nil : amount,
                currencyCode: amount.isEmpty ? nil : "INR",
                note: noteParts.joined(separator: " · ")
            )
            onSaved(); onClose()
        } catch {
            self.error = error.localizedDescription
        }
        submitting = false
    }
}

// MARK: - Issue

private struct OpsIssueForm: View {
    var momentId: String?
    var onClose: () -> Void
    var onSaved: () -> Void

    @State private var title = ""
    @State private var severity = "Medium"
    @State private var area = "Ops"
    @State private var description = ""
    @State private var evidenceNote = ""
    @State private var submitting = false
    @State private var error: String?

    private let severities = ["Low", "Medium", "High"]
    private let areas = ["Engineering", "Ops", "Finance", "Vendors", "Customer", "Other"]

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            OpsSheetHeader(emoji: "🛡", title: "Report Issue", explanation: "Flag an operational problem for the team", onClose: onClose)
            OpsFieldBlock(label: "Issue Title") { OpsTextField(value: $title, placeholder: "Issue title") }
            OpsFieldBlock(label: "Severity") { OpsChipRow(options: severities, selected: $severity) }
            OpsFieldBlock(label: "Affected Area") {
                OpsDropdownField(value: area, options: areas, onSelect: { area = $0 }, placeholder: "Select area")
            }
            OpsFieldBlock(label: "Description") {
                OpsTextField(value: $description, placeholder: "Describe the issue...", minHeight: 80, singleLine: false)
            }
            OpsFieldBlock(label: "Attach Evidence") {
                OpsTextField(value: $evidenceNote, placeholder: "Optional — upload not available yet")
            }
            if let error { Text(error).font(.plusJakarta(size: 12)).foregroundStyle(OpsSheetTokens.error) }
            OpsPrimaryCta(
                label: submitting ? "Saving…" : "Report Issue",
                enabled: momentId != nil && !title.trimmingCharacters(in: .whitespaces).isEmpty && !submitting,
                loading: submitting,
                footerHint: "Team will be notified"
            ) { Task { await submit() } }
        }
    }

    private func submit() async {
        guard let momentId else { return }
        submitting = true
        error = nil
        do {
            var parts = ["Area: \(area)"]
            if !description.isEmpty { parts.append(description) }
            _ = try await APIClient.shared.createBusinessIssue(
                momentId: momentId,
                title: title.trimmingCharacters(in: .whitespaces),
                description: parts.joined(separator: " · "),
                severity: severity.uppercased()
            )
            onSaved(); onClose()
        } catch {
            self.error = error.localizedDescription
        }
        submitting = false
    }
}

// MARK: - Budget review

private struct OpsBudgetReviewForm: View {
    var momentId: String?
    var onClose: () -> Void
    var onSaved: () -> Void

    @State private var period = "This Month"
    @State private var focus = "Overall Budget"
    @State private var variance = ""
    @State private var findings = ""
    @State private var submitting = false
    @State private var error: String?

    private let periods = ["This Week", "This Month", "This Quarter"]
    private let focuses = ["Overall Budget", "Vendor Spend", "Payroll", "Marketing", "COGS"]

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            OpsSheetHeader(emoji: "📅", title: "Budget Review", explanation: "Log a budget checkpoint for this period", onClose: onClose)
            OpsFieldBlock(label: "Period") { OpsChipRow(options: periods, selected: $period) }
            OpsFieldBlock(label: "Focus Area") {
                OpsDropdownField(value: focus, options: focuses, onSelect: { focus = $0 }, placeholder: "Select focus")
            }
            OpsFieldBlock(label: "Variance Note") { OpsTextField(value: $variance, placeholder: "e.g. 4% below forecast") }
            OpsFieldBlock(label: "Findings") {
                OpsTextField(value: $findings, placeholder: "Key findings...", minHeight: 80, singleLine: false)
            }
            if let error { Text(error).font(.plusJakarta(size: 12)).foregroundStyle(OpsSheetTokens.error) }
            OpsPrimaryCta(
                label: submitting ? "Saving…" : "Save Review",
                enabled: momentId != nil && !findings.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && !submitting,
                loading: submitting,
                footerHint: "Review will be logged"
            ) { Task { await submit() } }
        }
    }

    private func submit() async {
        guard let momentId else { return }
        submitting = true
        error = nil
        do {
            let apiPeriod: String
            switch period {
            case "This Week": apiPeriod = "WEEKLY"
            case "This Month": apiPeriod = "MONTHLY"
            case "This Quarter": apiPeriod = "QUARTERLY"
            default: apiPeriod = "OTHER"
            }
            var summary = "Focus: \(focus)\n"
            if !variance.isEmpty { summary += "Variance: \(variance)\n" }
            summary += findings.trimmingCharacters(in: .whitespacesAndNewlines)
            _ = try await APIClient.shared.createBusinessReview(
                momentId: momentId,
                period: apiPeriod,
                summary: summary
            )
            onSaved(); onClose()
        } catch {
            self.error = error.localizedDescription
        }
        submitting = false
    }
}

// MARK: - Improvement

private struct OpsImprovementForm: View {
    var momentId: String?
    var onClose: () -> Void
    var onSaved: () -> Void

    @State private var title = ""
    @State private var impactArea = "Cost"
    @State private var impact = "Medium"
    @State private var description = ""
    @State private var submitting = false
    @State private var error: String?

    private let areas = ["Cost", "Speed", "Quality", "Reliability", "Process"]
    private let levels = ["Low", "Medium", "High"]

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            OpsSheetHeader(emoji: "✨", title: "Log Improvement", explanation: "Capture an optimization for the ops playbook", onClose: onClose)
            OpsFieldBlock(label: "Title") { OpsTextField(value: $title, placeholder: "Improvement title") }
            OpsFieldBlock(label: "Impact Area") {
                OpsDropdownField(value: impactArea, options: areas, onSelect: { impactArea = $0 }, placeholder: "Select area")
            }
            OpsFieldBlock(label: "Impact Level") { OpsChipRow(options: levels, selected: $impact) }
            OpsFieldBlock(label: "Description") {
                OpsTextField(value: $description, placeholder: "What to improve...", minHeight: 80, singleLine: false)
            }
            if let error { Text(error).font(.plusJakarta(size: 12)).foregroundStyle(OpsSheetTokens.error) }
            OpsPrimaryCta(
                label: submitting ? "Saving…" : "Log Improvement",
                enabled: momentId != nil && !title.trimmingCharacters(in: .whitespaces).isEmpty && !submitting,
                loading: submitting,
                footerHint: "Improvement will be tracked"
            ) { Task { await submit() } }
        }
    }

    private func submit() async {
        guard let momentId else { return }
        submitting = true
        error = nil
        do {
            _ = try await APIClient.shared.createBusinessImprovement(
                momentId: momentId,
                title: title.trimmingCharacters(in: .whitespaces),
                description: description.isEmpty ? nil : description,
                categoryCode: impactArea.uppercased(),
                impactEstimate: impact
            )
            onSaved(); onClose()
        } catch {
            self.error = error.localizedDescription
        }
        submitting = false
    }
}

// MARK: - SLA

private struct OpsSlaForm: View {
    var companyId: String?
    var onClose: () -> Void
    var onSaved: () -> Void

    @State private var vendor = ""
    @State private var metric = "Uptime"
    @State private var result = "Pass"
    @State private var notes = ""
    @State private var submitting = false
    @State private var error: String?
    @State private var existingVendors: [APIClient.VendorItem] = []
    @State private var selectedVendor: APIClient.VendorItem?
    @State private var useExisting = false

    private let results = ["Pass", "Warn", "Fail"]

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            OpsSheetHeader(emoji: "⏱", title: "SLA Check", explanation: "Record an SLA observation for a vendor", onClose: onClose)
            if !existingVendors.isEmpty {
                OpsFieldBlock(label: "Select Vendor") {
                    let vendorNames = ["Create new"] + existingVendors.map { $0.name }
                    OpsDropdownField(
                        value: useExisting && selectedVendor != nil ? selectedVendor!.name : "Create new",
                        options: vendorNames,
                        onSelect: { picked in
                            if let found = existingVendors.first(where: { $0.name == picked }) {
                                useExisting = true
                                selectedVendor = found
                                vendor = found.name
                            } else {
                                useExisting = false
                                selectedVendor = nil
                                vendor = ""
                            }
                        },
                        placeholder: "Select existing or create new"
                    )
                }
            }
            if !useExisting {
                OpsFieldBlock(label: "Vendor Name") { OpsTextField(value: $vendor, placeholder: "Vendor name") }
            }
            OpsFieldBlock(label: "Metric Name") { OpsTextField(value: $metric, placeholder: "e.g. Uptime") }
            OpsFieldBlock(label: "Result") { OpsChipRow(options: results, selected: $result) }
            OpsFieldBlock(label: "Notes") {
                OpsTextField(value: $notes, placeholder: "Optional notes", minHeight: 80, singleLine: false)
            }
            if companyId == nil {
                Text("Select a company to record SLA checks.")
                    .font(.plusJakarta(size: 12)).foregroundStyle(OpsSheetTokens.muted)
            }
            if let error { Text(error).font(.plusJakarta(size: 12)).foregroundStyle(OpsSheetTokens.error) }
            OpsPrimaryCta(
                label: submitting ? "Saving…" : "Log SLA Check",
                enabled: companyId != nil && !vendor.trimmingCharacters(in: .whitespaces).isEmpty && !metric.isEmpty && !submitting,
                loading: submitting,
                footerHint: "SLA record will update"
            ) { Task { await submit() } }
        }
        .task {
            guard let companyId else { return }
            do {
                let result = try await APIClient.shared.listCompanyVendors(companyId: companyId)
                existingVendors = result.items
            } catch { /* non-blocking */ }
        }
    }

    private func submit() async {
        guard let companyId else { return }
        submitting = true
        error = nil
        do {
            let apiResult: String
            switch result.lowercased() {
            case "pass": apiResult = "PASS"
            case "fail": apiResult = "FAIL"
            default: apiResult = "UNKNOWN"
            }
            let vendorId: String
            if useExisting, let existing = selectedVendor {
                vendorId = existing.vendorId
            } else {
                let created = try await APIClient.shared.createCompanyVendor(
                    companyId: companyId,
                    name: vendor.trimmingCharacters(in: .whitespaces),
                    vendorType: "SaaS"
                )
                vendorId = created.vendorId
            }
            let metricCode = metric.uppercased().replacingOccurrences(of: " ", with: "_")
            let def = try await APIClient.shared.createSlaDefinition(
                companyId: companyId,
                vendorId: vendorId,
                name: metric.trimmingCharacters(in: .whitespaces),
                metricCode: String(metricCode.prefix(32)),
                comparator: "GTE",
                targetValue: 99.9,
                unitCode: "PCT"
            )
            _ = try await APIClient.shared.createSlaCheck(
                companyId: companyId,
                slaDefinitionId: def.slaDefinitionId,
                result: apiResult,
                note: notes.isEmpty ? nil : notes
            )
            onSaved(); onClose()
        } catch {
            self.error = error.localizedDescription
        }
        submitting = false
    }
}

// MARK: - General update

private struct OpsGeneralUpdateForm: View {
    var momentId: String?
    var onClose: () -> Void
    var onSaved: () -> Void

    @State private var title = ""
    @State private var visibility = "Team"
    @State private var status = "Info"
    @State private var message = ""
    @State private var submitting = false
    @State private var error: String?

    private let visibilities = ["Team", "Leadership", "All"]
    private let statuses = ["Info", "Success", "Warning"]

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            OpsSheetHeader(emoji: "📢", title: "General Update", explanation: "Share an operations status with the team", onClose: onClose)
            OpsFieldBlock(label: "Update Title") { OpsTextField(value: $title, placeholder: "Optional title") }
            OpsFieldBlock(label: "Visibility") { OpsChipRow(options: visibilities, selected: $visibility) }
            OpsFieldBlock(label: "Status") { OpsChipRow(options: statuses, selected: $status) }
            OpsFieldBlock(label: "Message") {
                OpsTextField(value: $message, placeholder: "What happened...", minHeight: 96, singleLine: false)
            }
            if let error { Text(error).font(.plusJakarta(size: 12)).foregroundStyle(OpsSheetTokens.error) }
            OpsPrimaryCta(
                label: submitting ? "Saving…" : "Post Update",
                enabled: momentId != nil && !message.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && !submitting,
                loading: submitting,
                footerHint: "Update will be shared"
            ) { Task { await submit() } }
        }
    }

    private func submit() async {
        guard let momentId else { return }
        submitting = true
        error = nil
        do {
            let body = "Visibility: \(visibility)\nStatus: \(status)\n\(message.trimmingCharacters(in: .whitespacesAndNewlines))"
            _ = try await APIClient.shared.createBusinessUpdate(
                momentId: momentId,
                body: body,
                title: title.trimmingCharacters(in: .whitespaces).isEmpty ? "Ops update" : title.trimmingCharacters(in: .whitespaces)
            )
            onSaved(); onClose()
        } catch {
            self.error = error.localizedDescription
        }
        submitting = false
    }
}

// MARK: - Memory

private struct OpsMemoryForm: View {
    var momentId: String?
    var momentTitle: String?
    var onClose: () -> Void
    var onSaved: () -> Void

    @State private var title = ""
    @State private var category = "Engineering Playbook"
    @State private var memoryType = "Learning"
    @State private var insight = ""
    @State private var submitting = false
    @State private var error: String?

    private let categories = ["Engineering Playbook", "Budget Playbook", "Vendor Playbook", "Ops Playbook", "General"]
    private let types = ["Learning", "Pattern", "Playbook"]
    private var sourceMoment: String { momentTitle?.isEmpty == false ? momentTitle! : "Current moment" }

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            OpsSheetHeader(emoji: "📦", title: "Save to Memory", explanation: "Capture an ops learning for the playbook", onClose: onClose)
            OpsFieldBlock(label: "Title") { OpsTextField(value: $title, placeholder: "Memory title") }
            OpsFieldBlock(label: "Category") {
                OpsDropdownField(value: category, options: categories, onSelect: { category = $0 }, placeholder: "Select category")
            }
            OpsFieldBlock(label: "Source Moment") {
                OpsDropdownField(value: sourceMoment, options: [sourceMoment], onSelect: { _ in }, placeholder: sourceMoment)
            }
            OpsFieldBlock(label: "Memory Type") { OpsChipRow(options: types, selected: $memoryType) }
            OpsFieldBlock(label: "Insight") {
                OpsTextField(value: $insight, placeholder: "What should the playbook remember?", minHeight: 80, singleLine: false)
            }
            if let error { Text(error).font(.plusJakarta(size: 12)).foregroundStyle(OpsSheetTokens.error) }
            OpsPrimaryCta(
                label: submitting ? "Saving…" : "Save to Memory",
                enabled: momentId != nil && !title.trimmingCharacters(in: .whitespaces).isEmpty && !insight.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && !submitting,
                loading: submitting,
                footerHint: "Logged under corporate playbook"
            ) { Task { await submit() } }
        }
    }

    private func submit() async {
        guard let momentId else { return }
        submitting = true
        error = nil
        do {
            let body = "Category: \(category)\nSource: \(sourceMoment)\n\(insight.trimmingCharacters(in: .whitespacesAndNewlines))"
            _ = try await APIClient.shared.createBusinessMemory(
                momentId: momentId,
                title: title.trimmingCharacters(in: .whitespaces),
                body: body,
                memoryType: memoryType.uppercased()
            )
            onSaved(); onClose()
        } catch {
            self.error = error.localizedDescription
        }
        submitting = false
    }
}
