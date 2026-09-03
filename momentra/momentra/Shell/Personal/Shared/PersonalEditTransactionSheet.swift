import SwiftUI

/// Figma 417:8759 Edit Transaction — expense rows from Activity Timeline.
struct PersonalEditTransactionSheet: View {
    let momentId: String
    let item: APIClient.ActivityItemPayload
    var onClose: () -> Void
    var onSaved: () -> Void
    var onDeleted: () -> Void

    @State private var title: String
    @State private var amount: String
    @State private var categoryCode: String
    @State private var subcategoryCode: String?
    @State private var notes: String
    @State private var selectedTags: Set<String> = []
    @State private var attachments: [String] = []
    @State private var accounts: [APIClient.FinancialAccount] = []
    @State private var selectedAccountId: String?
    @State private var paymentMethod = "CASH"
    @State private var effectiveAtIso: String
    @State private var showCategoryPicker = false
    @State private var showAccountPicker = false
    @State private var categoryPickerMode: PersonalCategoryPickerSheet.Mode = .category
    @State private var showUpload = false
    @State private var showDeleteConfirm = false
    @State private var loadingDetail = true
    @State private var submitting = false
    @State private var error: String?

    private let tags = ["Essential", "Planned", "Impulse", "Budget", "Weekly"]
    private let paymentMethods = ["CASH", "CARD", "UPI", "BANK_TRANSFER", "WALLET", "OTHER"]
    private let accent = Color(hex: "#7C5CFC")
    private let repository = PersonalTransactionRepository()

    private var txnRef: TransactionRef? {
        TransactionRef.fromActivity(momentId: momentId, item: item)
    }

    init(momentId: String, item: APIClient.ActivityItemPayload, onClose: @escaping () -> Void, onSaved: @escaping () -> Void, onDeleted: @escaping () -> Void = {}) {
        self.momentId = momentId
        self.item = item
        self.onClose = onClose
        self.onSaved = onSaved
        self.onDeleted = onDeleted
        _title = State(initialValue: item.title)
        _amount = State(initialValue: item.activityPayload?.amount ?? "")
        let decoded = PersonalExpenseCategoryCatalog.decodeFromStored(item.activityPayload?.categoryCode)
        _categoryCode = State(initialValue: decoded.0)
        _subcategoryCode = State(initialValue: decoded.1)
        let rawNotes = item.activityPayload?.description ?? ""
        let idx = rawNotes.range(of: " · Tags:")
        _notes = State(initialValue: idx.map { String(rawNotes[..<$0.lowerBound]) } ?? rawNotes)
        _selectedAccountId = State(initialValue: item.activityPayload?.financialAccountId)
        _paymentMethod = State(initialValue: item.activityPayload?.paymentMethodCode ?? "CASH")
        _effectiveAtIso = State(initialValue: item.occurredAt)
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 14) {
                Capsule().fill(Color(hex: "#3A3842")).frame(width: 36, height: 4).padding(.top, 8)
                HStack {
                    Button(action: onClose) {
                        Text("‹").font(.system(size: 22, weight: .bold)).foregroundStyle(accent)
                            .frame(width: 40, height: 40)
                            .background(Color.white.opacity(0.06))
                            .clipShape(Circle())
                    }
                    Text("Edit Transaction")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity)
                    Button(action: save) {
                        Text(submitting ? "…" : "Save")
                            .font(.system(size: 14, weight: .bold))
                            .foregroundStyle(.white)
                            .padding(.horizontal, 16)
                            .padding(.vertical, 8)
                            .background(LinearGradient(colors: [Color(hex: "#EC4899"), accent], startPoint: .leading, endPoint: .trailing))
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                    }
                    .disabled(submitting || title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }

                TextField("", text: $amount)
                    .font(.system(size: 36, weight: .heavy))
                    .foregroundStyle(Color(hex: "#E5E0EE"))
                    .keyboardType(.decimalPad)
                    .multilineTextAlignment(.center)

                HStack(spacing: 4) {
                    segment("Expense", selected: true) {}
                    segment("Income", selected: false) {}
                        .opacity(0.4)
                }
                .padding(4)
                .background(Color(hex: "#201E28"))
                .clipShape(Capsule())

                VStack(spacing: 12) {
                    txnField("Title", text: $title)
                    chevronField("Category", value: PersonalExpenseCategoryCatalog.labelForCode(categoryCode)) {
                        categoryPickerMode = .category
                        showCategoryPicker = true
                    }
                    chevronField("Sub-category", value: subcategoryCode.map { PersonalExpenseCategoryCatalog.labelForCode($0) } ?? "Select sub-category") {
                        categoryPickerMode = .subcategory
                        showCategoryPicker = true
                    }
                    txnField("Date (ISO)", text: $effectiveAtIso)
                    chevronField("Account", value: accountLabel) { showAccountPicker = true }
                    paymentMethodRow()
                }
                .padding(14)
                .background(Color(hex: "#201E28"))
                .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.08)))
                .clipShape(RoundedRectangle(cornerRadius: 16))

                Text("TAGS").font(.system(size: 11, weight: .semibold)).foregroundStyle(Color(hex: "#64748B")).frame(maxWidth: .infinity, alignment: .leading)
                FlowLayout(spacing: 8) {
                    ForEach(tags, id: \.self) { tag in
                        let on = selectedTags.contains(tag)
                        Text(tag)
                            .font(.system(size: 12))
                            .foregroundStyle(on ? accent : Color(hex: "#E5E0EE"))
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .background(on ? accent.opacity(0.2) : Color(hex: "#201E28"))
                            .overlay(Capsule().stroke(on ? accent : Color.white.opacity(0.08)))
                            .clipShape(Capsule())
                            .onTapGesture { toggleTag(tag) }
                    }
                }

                Text("NOTES").font(.system(size: 11, weight: .semibold)).foregroundStyle(Color(hex: "#64748B")).frame(maxWidth: .infinity, alignment: .leading)
                TextEditor(text: $notes)
                    .scrollContentBackground(.hidden)
                    .foregroundStyle(Color(hex: "#E5E0EE"))
                    .frame(minHeight: 72)
                    .padding(12)
                    .background(Color(hex: "#201E28"))
                    .overlay(RoundedRectangle(cornerRadius: 14).stroke(Color.white.opacity(0.08)))
                    .clipShape(RoundedRectangle(cornerRadius: 14))
                    .onChange(of: notes) { _, v in if v.count > 200 { notes = String(v.prefix(200)) } }

                Button { showUpload = true } label: {
                    VStack(spacing: 4) {
                        Text("📎").font(.system(size: 24))
                        Text("Add attachment").foregroundStyle(Color(hex: "#E5E0EE"))
                        Text("Upload persists to backend when complete").font(.system(size: 11)).foregroundStyle(Color(hex: "#64748B"))
                    }
                    .frame(maxWidth: .infinity)
                    .padding(24)
                    .background(accent.opacity(0.08))
                    .overlay(RoundedRectangle(cornerRadius: 16).stroke(accent.opacity(0.3)))
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                }
                .buttonStyle(.plain)
                ForEach(attachments, id: \.self) { uploadId in
                    Text("• \(uploadId.prefix(8))…").font(.system(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
                }

                if loadingDetail {
                    Text("Loading transaction…").font(.system(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
                }

                if let error {
                    Text(error).font(.system(size: 12)).foregroundStyle(Color(hex: "#F87171"))
                }

                if showDeleteConfirm {
                    HStack(spacing: 12) {
                        Button("Cancel") { showDeleteConfirm = false }
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                            .background(Color(hex: "#201E28"))
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                        Button("Void") { voidTransaction() }
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                            .background(Color(hex: "#F87171"))
                            .foregroundStyle(.white)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                    }
                } else {
                    Button { showDeleteConfirm = true } label: {
                        Text("Delete Transaction")
                            .font(.system(size: 15, weight: .heavy))
                            .foregroundStyle(Color(hex: "#F87171"))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .background(Color(hex: "#F87171").opacity(0.12))
                            .overlay(RoundedRectangle(cornerRadius: 14).stroke(Color(hex: "#F87171").opacity(0.35)))
                            .clipShape(RoundedRectangle(cornerRadius: 14))
                    }
                    .buttonStyle(.plain)
                    .disabled(submitting)
                }
            }
            .padding(16)
            .padding(.bottom, 24)
        }
        .background(Color(hex: "#191622"))
        .task { await loadDetail() }
        .sheet(isPresented: $showCategoryPicker) {
            PersonalCategoryPickerSheet(
                mode: categoryPickerMode,
                selectedCategoryCode: categoryCode,
                selectedSubcategoryCode: subcategoryCode,
                onSelect: { cat, sub in
                    categoryCode = cat
                    subcategoryCode = sub
                    showCategoryPicker = false
                },
                onClose: { showCategoryPicker = false }
            )
            .presentationDetents([.medium, .large])
        }
        .sheet(isPresented: $showAccountPicker) {
            PersonalAccountPickerSheet(
                selectedAccountId: selectedAccountId,
                onSelect: { account in
                    if !accounts.contains(where: { $0.financialAccountId == account.financialAccountId }) {
                        accounts.append(account)
                    }
                    selectedAccountId = account.financialAccountId
                    if paymentMethod == "CASH" {
                        paymentMethod = PersonalFinancialAccountUi.paymentMethodForAccountType(account.accountType)
                    }
                },
                onClose: { showAccountPicker = false }
            )
            .presentationDetents([.large])
        }
        .sheet(isPresented: $showUpload) {
            PersonalUploadAttachmentSheet(
                onUpload: { uploadId in attachments.append(uploadId) },
                onClose: { showUpload = false },
                uploadAction: uploadAttachment
            )
            .presentationDetents([.medium])
        }
    }

    private var accountLabel: String {
        accounts.first(where: { $0.financialAccountId == selectedAccountId })?.accountName
            ?? accounts.first?.accountName
            ?? "Select account"
    }

    private func loadDetail() async {
        guard let ref = txnRef else { loadingDetail = false; return }
        loadingDetail = true
        do {
            if ref.resourceType == .expense {
                let detail = try await repository.loadExpenseDetail(ref: ref)
                title = detail.merchantName ?? detail.description ?? item.title
                amount = detail.amount
                if let cat = detail.categoryCode {
                    let decoded = PersonalExpenseCategoryCatalog.decodeFromStored(cat)
                    categoryCode = decoded.0
                    subcategoryCode = detail.subcategoryCode ?? decoded.1
                }
                notes = stripTags(detail.description ?? "")
                selectedAccountId = detail.financialAccountId
                paymentMethod = detail.paymentMethodCode ?? paymentMethod
                effectiveAtIso = detail.effectiveAt ?? item.occurredAt
                attachments = detail.attachmentIds ?? []
            }
            accounts = try await APIClient.shared.listFinancialAccounts()
        } catch {
            self.error = error.localizedDescription
        }
        loadingDetail = false
    }

    @ViewBuilder
    private func paymentMethodRow() -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("Payment Method").font(.system(size: 11, weight: .semibold)).foregroundStyle(Color(hex: "#64748B"))
            FlowLayout(spacing: 8) {
                ForEach(paymentMethods, id: \.self) { method in
                    let selected = paymentMethod == method
                    Text(method)
                        .font(.system(size: 11))
                        .foregroundStyle(selected ? accent : Color(hex: "#E5E0EE"))
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(selected ? accent.opacity(0.2) : Color(hex: "#191622"))
                        .overlay(Capsule().stroke(selected ? accent : Color.white.opacity(0.08)))
                        .clipShape(Capsule())
                        .onTapGesture { paymentMethod = method }
                }
            }
        }
    }

    private func uploadAttachment(contentType: String) async throws -> String {
        guard let ref = txnRef else { throw URLError(.badURL) }
        let attachment = try await repository.attachMedia(ref: ref, bytes: Data(repeating: 0xFF, count: 64), contentType: contentType)
        return attachment.uploadId
    }

    private func voidTransaction() {
        guard let ref = txnRef, !submitting else { return }
        submitting = true
        Task {
            do {
                try await repository.void(ref: ref)
                await MainActor.run {
                    submitting = false
                    onDeleted()
                }
            } catch {
                await MainActor.run {
                    submitting = false
                    self.error = error.localizedDescription
                    showDeleteConfirm = false
                }
            }
        }
    }

    private func segment(_ label: String, selected: Bool, action: @escaping () -> Void) -> some View {
        Text(label)
            .font(.system(size: 13, weight: .semibold))
            .foregroundStyle(selected ? .white : Color(hex: "#C9C4D8"))
            .padding(.horizontal, 20)
            .padding(.vertical, 8)
            .background(selected ? accent : Color.clear)
            .clipShape(Capsule())
    }

    private func txnField(_ label: String, text: Binding<String>, readOnly: Bool = false) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label).font(.system(size: 11, weight: .semibold)).foregroundStyle(Color(hex: "#64748B"))
            TextField("", text: text)
                .disabled(readOnly)
                .foregroundStyle(readOnly ? Color(hex: "#C9C4D8") : Color(hex: "#E5E0EE"))
                .padding(.horizontal, 12)
                .padding(.vertical, 10)
                .background(Color(hex: "#191622"))
                .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.white.opacity(0.08)))
                .clipShape(RoundedRectangle(cornerRadius: 12))
        }
    }

    private func chevronField(_ label: String, value: String, action: @escaping () -> Void) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label).font(.system(size: 11, weight: .semibold)).foregroundStyle(Color(hex: "#64748B"))
            Button(action: action) {
                HStack {
                    Text(value).foregroundStyle(Color(hex: "#E5E0EE"))
                    Spacer()
                    Text("›").foregroundStyle(Color(hex: "#C9C4D8"))
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 10)
                .background(Color(hex: "#191622"))
                .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.white.opacity(0.08)))
                .clipShape(RoundedRectangle(cornerRadius: 12))
            }
            .buttonStyle(.plain)
        }
    }

    private func toggleTag(_ tag: String) {
        if selectedTags.contains(tag) { selectedTags.remove(tag) } else { selectedTags.insert(tag) }
    }

    private func save() {
        guard let ref = txnRef, ref.resourceType == .expense, !submitting else { return }
        submitting = true
        error = nil
        let tagSuffix = selectedTags.isEmpty ? nil : selectedTags.sorted().joined(separator: ", ")
        var parts = [String]()
        let trimmedNotes = notes.trimmingCharacters(in: .whitespacesAndNewlines)
        if !trimmedNotes.isEmpty { parts.append(trimmedNotes) }
        if let tagSuffix { parts.append("Tags: \(tagSuffix)") }
        let description = parts.isEmpty ? nil : parts.joined(separator: " · ")
        let cleanedAmount = amount
            .replacingOccurrences(of: "₹", with: "")
            .replacingOccurrences(of: ",", with: "")
            .trimmingCharacters(in: .whitespacesAndNewlines)
        Task {
            do {
                _ = try await repository.updateExpense(
                    ref: ref,
                    amount: cleanedAmount.isEmpty ? nil : cleanedAmount,
                    description: description,
                    merchantName: title.trimmingCharacters(in: .whitespacesAndNewlines),
                    categoryCode: PersonalExpenseCategoryCatalog.encodeCategoryCode(
                        categoryCode: categoryCode,
                        subcategoryCode: subcategoryCode
                    ),
                    subcategoryCode: subcategoryCode,
                    financialAccountId: selectedAccountId,
                    paymentMethodCode: paymentMethod,
                    effectiveAt: effectiveAtIso
                )
                await MainActor.run {
                    submitting = false
                    onSaved()
                }
            } catch {
                await MainActor.run {
                    submitting = false
                    self.error = error.localizedDescription
                }
            }
        }
    }

    private func stripTags(_ description: String) -> String {
        guard let idx = description.range(of: " · Tags:") else { return description }
        return String(description[..<idx.lowerBound])
    }
}
