import SwiftUI

/// Figma Sheet / Add Expense — payer + split strategy; EQUAL default; server-authoritative submit.
/// Pass `expenseId` to load/edit an existing group expense (splits + paid-by).
struct GroupExpenseSheet: View {
    let momentId: String
    @Binding var isPresented: Bool
    var expenseId: String? = nil
    var isWedding: Bool = false
    var momentTypeCode: String? = nil
    var onSaved: () -> Void
    var onDeleted: () -> Void = {}

    @State private var amount = ""
    @State private var currencyCode = "INR"
    @State private var descriptionText = ""
    @State private var expenseDate = ""
    @State private var category: String = GroupExpenseCategoryCatalog.defaultCategory(for: nil)
    @State private var participants: [APIClient.GroupParticipantPayload] = []
    @State private var paidByParticipantId: String?
    @State private var selectedParticipantIds: Set<String> = []
    @State private var splitStrategy = "EQUAL"
    /// PERCENTAGE / EXACT / SHARES input values keyed by participantId
    @State private var splitValues: [String: String] = [:]
    @State private var loading = true
    @State private var submitting = false
    @State private var showDeleteConfirm = false
    @State private var error: String?

    private let currencyOptions = ["INR", "USD", "EUR", "GBP"]
    private var isEditing: Bool { expenseId != nil }
    private var accent: Color { isWedding ? WeddingActiveTheme.accentSolid : TripSheetTokens.accent }
    private var peach: Color { isWedding ? WeddingActiveTheme.accentLight : TripSheetTokens.accentEnd }
    private var sheetAccent: SheetAccent {
        SheetAccent(accent: accent, accentEnd: peach, soft: accent.opacity(0.18))
    }
    private static let livingTypeCodes: Set<String> = [
        "FAMILY_HOUSEHOLD", "FLATMATES", "CO_LIVING", "SHARED_LIVING", "COMMUNITY_LIVING",
    ]
    private var supportsPooled: Bool {
        let code = (momentTypeCode ?? "").trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        return Self.livingTypeCodes.contains(code)
    }
    private var figmaSplitLabels: [(label: String, strategy: String)] {
        var items: [(String, String)] = [
            ("Equal", "EQUAL"),
            ("Custom", "EXACT"),
            ("% Percent", "PERCENTAGE"),
        ]
        if supportsPooled { items.append(("Pooled", "POOLED")) }
        return items
    }
    private var categoryOptions: [String] { GroupExpenseCategoryCatalog.categories(for: momentTypeCode) }
    private var currencySymbol: String {
        switch currencyCode {
        case "USD": return "$"
        case "EUR": return "€"
        case "GBP": return "£"
        default: return "₹"
        }
    }
    private var sheetTitle: String {
        isEditing ? "Edit Expense" : "Add Expense"
    }
    private var people: [(id: String, name: String)] {
        participants.map {
            let base = $0.displayName ?? shortId($0.participantId)
            return (id: $0.participantId, name: $0.guest ? "\(base) · Guest" : base)
        }
    }

    var body: some View {
        NativeSheetScaffold(
            title: sheetTitle,
            onClose: { isPresented = false },
            background: isWedding ? Color(hex: "#14121B") : TripSheetTokens.bg
        ) {
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    if loading {
                        ProgressView().tint(accent).frame(maxWidth: .infinity).padding(.vertical, 24)
                    } else {
                        formCard
                        if isEditing {
                            Button(role: .destructive) {
                                showDeleteConfirm = true
                            } label: {
                                Text("Delete expense")
                                    .font(.system(size: 14, weight: .semibold))
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 12)
                            }
                            .accessibilityIdentifier("group.expense.delete")
                        }
                        if let error {
                            Text(error)
                                .font(.caption)
                                .foregroundStyle(Color(hex: "#F87171"))
                        }
                        Text("All residents will be notified")
                            .font(.system(size: 11))
                            .foregroundStyle(isWedding ? Color(hex: "#C9C4D8") : TripSheetTokens.muted)
                    }
                }
                .padding(16)
            }
        } footer: {
            saveButton
                .padding(.horizontal, 16)
                .padding(.bottom, 8)
                .background(isWedding ? Color(hex: "#14121B") : TripSheetTokens.bg)
        }
        .presentationDetents([.large])
        .presentationDragIndicator(.visible)
        .confirmationDialog("Delete this expense?", isPresented: $showDeleteConfirm, titleVisibility: .visible) {
            Button("Delete", role: .destructive) {
                Task { await deleteExpense() }
            }
            Button("Cancel", role: .cancel) {}
        }
        .task {
            category = GroupExpenseCategoryCatalog.defaultCategory(for: momentTypeCode)
            await loadParticipants()
            if let expenseId {
                await loadExpense(expenseId)
            }
        }
    }

    private var formCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .bottom, spacing: 6) {
                Menu {
                    ForEach(currencyOptions, id: \.self) { code in
                        Button(code) { currencyCode = code }
                    }
                } label: {
                    Text(currencySymbol)
                        .font(.system(size: 28, weight: .bold))
                        .foregroundStyle(accent)
                }
                ZStack(alignment: .leading) {
                    if amount.isEmpty {
                        Text("0.00")
                            .font(.system(size: 40, weight: .heavy))
                            .foregroundStyle(Color(hex: "#E5E0EE").opacity(0.35))
                    }
                    TextField("", text: $amount)
                        .font(.system(size: 40, weight: .heavy))
                        .foregroundStyle(Color(hex: "#E5E0EE"))
                        .keyboardType(.decimalPad)
                        .accessibilityIdentifier("group.expense.amount")
                        .onChange(of: amount) { _, new in
                            amount = new.filter { $0.isNumber || $0 == "." }
                        }
                }
            }
            .frame(maxWidth: .infinity)

            Menu {
                ForEach(currencyOptions, id: \.self) { code in
                    Button(code) { currencyCode = code }
                }
            } label: {
                HStack {
                    Text("Currency")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(Color(hex: "#C9C4D8"))
                    Spacer()
                    Text("\(currencySymbol)  \(currencyCode)")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(Color(hex: "#E5E0EE"))
                    Image(systemName: "chevron.down")
                        .font(.system(size: 12))
                        .foregroundStyle(Color(hex: "#C9C4D8"))
                }
                .padding(12)
                .background(Color(hex: "#201E28"))
                .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color(hex: "#938EA1"), lineWidth: 1))
                .clipShape(RoundedRectangle(cornerRadius: 12))
            }

            fieldLabel("DESCRIPTION")
            TextField("What was this for?", text: $descriptionText)
                .foregroundStyle(Color(hex: "#E5E0EE"))
                .padding(12)
                .background(Color(hex: "#201E28"))
                .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color(hex: "#938EA1"), lineWidth: 1))
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .accessibilityIdentifier("group.expense.note")

            HStack(alignment: .top, spacing: 14) {
                VStack(alignment: .leading, spacing: 8) {
                    fieldLabel("PAID BY")
                    Menu {
                        ForEach(participants) { p in
                            let base = p.displayName ?? shortId(p.participantId)
                            Button(p.guest ? "\(base) · Guest" : base) {
                                paidByParticipantId = p.participantId
                            }
                        }
                    } label: {
                        HStack {
                            let selected = participants.first(where: { $0.participantId == paidByParticipantId })
                            let base = selected?.displayName ?? "Select"
                            Text(selected?.guest == true ? "\(base) · Guest" : base)
                                .font(.system(size: 14, weight: .medium))
                                .foregroundStyle(Color(hex: "#E5E0EE"))
                            Spacer()
                            Image(systemName: "chevron.down")
                                .font(.system(size: 12))
                                .foregroundStyle(Color(hex: "#C9C4D8"))
                        }
                        .padding(12)
                        .background(Color(hex: "#201E28"))
                        .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color(hex: "#938EA1"), lineWidth: 1))
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                    }
                    .accessibilityIdentifier("group.expense.payer")
                }
                VStack(alignment: .leading, spacing: 8) {
                    fieldLabel("DATE")
                    WeddingDatePickField(value: $expenseDate, placeholder: "Today")
                }
            }

            fieldLabel("SPLIT BETWEEN")
            if participants.isEmpty {
                Text("No participants yet")
                    .font(.caption)
                    .foregroundStyle(Color(hex: "#C9C4D8"))
            } else {
                AvatarPick(people: people, selected: $selectedParticipantIds, accent: sheetAccent) { id in
                    if selectedParticipantIds.contains(id) {
                        selectedParticipantIds.remove(id)
                    } else {
                        selectedParticipantIds.insert(id)
                    }
                }
            }

            fieldLabel("SPLIT TYPE")
            HStack(spacing: 4) {
                ForEach(figmaSplitLabels, id: \.strategy) { item in
                    let on = splitStrategy == item.strategy
                    Button {
                        splitStrategy = item.strategy
                        seedSplitValues(for: item.strategy)
                    } label: {
                        Text(item.label)
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundStyle(on ? .white : Color(hex: "#C9C4D8"))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 10)
                            .background(on ? accent : Color.clear)
                            .clipShape(RoundedRectangle(cornerRadius: 10))
                    }
                    .buttonStyle(.plain)
                    .accessibilityIdentifier("group.expense.split.\(item.strategy.lowercased())")
                }
            }
            .padding(4)
            .background(Color(hex: "#201E28"))
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .accessibilityIdentifier("group.expense.split")

            if splitStrategy == "POOLED" {
                Text("Household spend — sums for the month. No per-member split.")
                    .font(.system(size: 11))
                    .foregroundStyle(isWedding ? Color(hex: "#C9C4D8") : TripSheetTokens.muted)
            }

            fieldLabel("CATEGORY")
            FlowLayout(spacing: 8) {
                ForEach(categoryOptions, id: \.self) { option in
                    let on = category == option
                    Button {
                        category = option
                    } label: {
                        Text(option)
                            .font(.system(size: 12, weight: .bold))
                            .foregroundStyle(on ? .white : Color(hex: "#C9C4D8"))
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .background(on ? accent : Color(hex: "#201E28"))
                            .clipShape(Capsule())
                    }
                    .buttonStyle(.plain)
                }
            }
            .accessibilityIdentifier("group.expense.category")

            if splitStrategy != "EQUAL" && splitStrategy != "POOLED" && !selectedParticipantIds.isEmpty {
                fieldLabel(splitValueLabel)
                ForEach(Array(selectedParticipantIds).sorted(), id: \.self) { id in
                    HStack {
                        Text(participants.first(where: { $0.participantId == id })?.displayName ?? shortId(id))
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundStyle(Color(hex: "#E5E0EE"))
                        Spacer()
                        TextField(splitValuePlaceholder, text: Binding(
                            get: { splitValues[id] ?? "" },
                            set: { splitValues[id] = $0 }
                        ))
                        .keyboardType(.decimalPad)
                        .multilineTextAlignment(.trailing)
                        .frame(width: 96)
                        .foregroundStyle(Color(hex: "#E5E0EE"))
                        .padding(8)
                        .background(Color(hex: "#201E28"))
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                        .accessibilityIdentifier("group.expense.split.value.\(id)")
                    }
                }
            }
        }
        .padding(12)
        .background(Color.white.opacity(0.05))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.08), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var saveButton: some View {
        Button {
            Task { await save() }
        } label: {
            HStack(spacing: 8) {
                if submitting {
                    ProgressView().tint(.white)
                } else {
                    Text(isEditing ? "Save Expense" : "Add Expense")
                        .font(.system(size: 15, weight: .heavy))
                    Image(systemName: "checkmark")
                        .font(.system(size: 13, weight: .bold))
                }
            }
            .foregroundStyle(.white)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(
                LinearGradient(colors: [accent, peach], startPoint: .leading, endPoint: .trailing)
            )
            .clipShape(RoundedRectangle(cornerRadius: 14))
        }
        .buttonStyle(.plain)
        .disabled(!canSubmit || submitting)
        .opacity(!canSubmit ? 0.55 : 1)
        .accessibilityIdentifier("group.expense.submit")
    }

    private var canSubmit: Bool {
        let amountOk = !amount.isEmpty && paidByParticipantId != nil && currencyCode.count == 3
        if splitStrategy == "POOLED" { return amountOk }
        return amountOk && !selectedParticipantIds.isEmpty
    }

    private var splitValueLabel: String {
        switch splitStrategy {
        case "PERCENTAGE": return "PERCENT (MUST SUM TO 100)"
        case "EXACT": return "EXACT AMOUNT PER PERSON"
        default: return "SHARE WEIGHT"
        }
    }

    private var splitValuePlaceholder: String {
        switch splitStrategy {
        case "PERCENTAGE": return "%"
        case "EXACT": return "0.00"
        default: return "1"
        }
    }

    private func seedSplitValues(for strategy: String) {
        let ids = Array(selectedParticipantIds)
        switch strategy {
        case "PERCENTAGE":
            let even = ids.isEmpty ? 0.0 : 100.0 / Double(ids.count)
            splitValues = Dictionary(uniqueKeysWithValues: ids.map { ($0, String(format: "%.2f", even)) })
        case "SHARES":
            splitValues = Dictionary(uniqueKeysWithValues: ids.map { ($0, "1") })
        case "EXACT":
            splitValues = Dictionary(uniqueKeysWithValues: ids.map { ($0, "") })
        default:
            splitValues = [:]
        }
    }

    private func fieldLabel(_ text: String) -> some View {
        Text(text)
            .font(.system(size: 11, weight: .bold))
            .tracking(0.8)
            .foregroundStyle(peach)
    }

    private func shortId(_ id: String) -> String {
        guard id.count > 8 else { return id }
        return String(id.prefix(8))
    }

    private func loadParticipants() async {
        loading = true
        error = nil
        do {
            let list = try await APIClient.shared.listGroupParticipants(momentId: momentId)
            participants = list.filter { ($0.status ?? "ACTIVE").uppercased() == "ACTIVE" || ($0.status ?? "").uppercased() == "INVITED" }
            if paidByParticipantId == nil {
                paidByParticipantId = participants.first?.participantId
            }
            if selectedParticipantIds.isEmpty {
                selectedParticipantIds = Set(participants.map(\.participantId))
            }
        } catch {
            self.error = error.localizedDescription
        }
        loading = false
    }

    private func save() async {
        guard let paidBy = paidByParticipantId else { return }
        submitting = true
        error = nil
        let ids = Array(selectedParticipantIds).sorted()
        let inputs: [APIClient.GroupSplitInput]
        switch splitStrategy {
        case "POOLED":
            inputs = []
        case "EQUAL":
            inputs = GroupActionRegistry.equalSplitInputs(participantIds: ids)
        case "PERCENTAGE":
            let sum = ids.reduce(0.0) { $0 + (Double(splitValues[$1] ?? "0") ?? 0) }
            if abs(sum - 100) > 0.01 {
                error = "Percents must sum to 100 (now \(sum))"
                submitting = false
                return
            }
            inputs = ids.map { APIClient.GroupSplitInput(participantId: $0, percent: splitValues[$0]) }
        case "EXACT":
            let sum = ids.reduce(Decimal.zero) { acc, id in
                acc + (Decimal(string: splitValues[id] ?? "0") ?? 0)
            }
            let total = Decimal(string: amount.trimmingCharacters(in: .whitespacesAndNewlines)) ?? -1
            if sum != total {
                error = "Exact amounts must equal expense amount"
                submitting = false
                return
            }
            inputs = ids.map { APIClient.GroupSplitInput(participantId: $0, amount: splitValues[$0]) }
        case "SHARES":
            for id in ids {
                let w = Double(splitValues[id] ?? "0") ?? 0
                if w <= 0 {
                    error = "Share weights must be positive"
                    submitting = false
                    return
                }
            }
            inputs = ids.map {
                APIClient.GroupSplitInput(participantId: $0, shares: Double(splitValues[$0] ?? "1") ?? 1)
            }
        default:
            error = "Unknown split strategy"
            submitting = false
            return
        }
        do {
            let finalDescription = GroupExpenseCategoryCatalog.descriptionWithCategory(
                category: category,
                userDescription: descriptionText
            )
            if let expenseId {
                _ = try await APIClient.shared.updateGroupExpense(
                    momentId: momentId,
                    expenseId: expenseId,
                    amount: amount.trimmingCharacters(in: .whitespacesAndNewlines),
                    currencyCode: currencyCode.uppercased(),
                    description: finalDescription.isEmpty ? nil : finalDescription,
                    paidByParticipantId: paidBy,
                    splitStrategy: splitStrategy,
                    splitInputs: inputs
                )
            } else {
                _ = try await APIClient.shared.createGroupExpense(
                    momentId: momentId,
                    amount: amount.trimmingCharacters(in: .whitespacesAndNewlines),
                    currencyCode: currencyCode.uppercased(),
                    description: finalDescription.isEmpty ? nil : finalDescription,
                    paidByParticipantId: paidBy,
                    splitStrategy: splitStrategy,
                    splitInputs: inputs
                )
            }
            isPresented = false
            onSaved()
        } catch {
            self.error = error.localizedDescription
        }
        submitting = false
    }

    private func loadExpense(_ expenseId: String) async {
        do {
            let detail = try await APIClient.shared.getGroupExpense(momentId: momentId, expenseId: expenseId)
            amount = detail.amount
            currencyCode = detail.currencyCode
            let parsed = GroupExpenseCategoryCatalog.parseCategoryAndNote(
                from: detail.description,
                momentTypeCode: momentTypeCode
            )
            category = parsed.category
            descriptionText = parsed.note
            paidByParticipantId = detail.paidByParticipantId
            splitStrategy = detail.splitStrategy
            let shares = detail.shares ?? []
            if detail.splitStrategy == "POOLED" {
                selectedParticipantIds = Set(participants.map(\.participantId))
                splitValues = [:]
            } else {
                selectedParticipantIds = Set(shares.map(\.participantId))
                switch detail.splitStrategy {
                case "PERCENTAGE":
                    splitValues = Dictionary(uniqueKeysWithValues: shares.map {
                        ($0.participantId, $0.sharePercent ?? "")
                    })
                case "EXACT":
                    splitValues = Dictionary(uniqueKeysWithValues: shares.map {
                        ($0.participantId, $0.shareAmount)
                    })
                case "SHARES":
                    splitValues = Dictionary(uniqueKeysWithValues: shares.map {
                        ($0.participantId, $0.sharePercent ?? "1")
                    })
                default:
                    splitValues = [:]
                }
            }
        } catch {
            self.error = error.localizedDescription
        }
    }

    private func deleteExpense() async {
        guard let expenseId else { return }
        submitting = true
        error = nil
        do {
            _ = try await APIClient.shared.voidGroupExpense(momentId: momentId, expenseId: expenseId)
            isPresented = false
            onDeleted()
        } catch {
            self.error = error.localizedDescription
        }
        submitting = false
    }
}

/// Simple contribution recorder for Group Quick Add.
struct GroupContributionSheet: View {
    let momentId: String
    @Binding var isPresented: Bool
    var isWedding: Bool = false
    var onSaved: () -> Void

    @State private var amount = ""
    @State private var currencyCode = "INR"
    @State private var label = ""
    @State private var submitting = false
    @State private var error: String?

    private var accent: Color { isWedding ? WeddingActiveTheme.accentSolid : Color(hex: "#14B8A6") }
    private var accentLight: Color { isWedding ? WeddingActiveTheme.accentLight : Color(hex: "#2DD4BF") }

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 14) {
                Text("Record contribution")
                    .font(.system(size: 18, weight: .heavy))
                    .foregroundStyle(Color(hex: "#E5E0EE"))
                HStack {
                    TextField("INR", text: $currencyCode)
                        .textInputAutocapitalization(.characters)
                        .frame(width: 56)
                        .foregroundStyle(Color(hex: "#C9C4D8"))
                    TextField("0.00", text: $amount)
                        .keyboardType(.decimalPad)
                        .font(.system(size: 26, weight: .heavy))
                        .foregroundStyle(Color(hex: "#E5E0EE"))
                }
                .padding(12)
                .background(Color(hex: "#201E28"))
                .clipShape(RoundedRectangle(cornerRadius: 12))
                TextField("Label (optional)", text: $label)
                    .foregroundStyle(Color(hex: "#E5E0EE"))
                    .padding(12)
                    .background(Color(hex: "#201E28"))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                if let error {
                    Text(error).font(.caption).foregroundStyle(Color(hex: "#F87171"))
                }
                Button {
                    Task { await save() }
                } label: {
                    if submitting {
                        ProgressView().tint(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                    } else {
                        Text("Save Contribution")
                            .font(.system(size: 15, weight: .heavy))
                            .foregroundStyle(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                    }
                }
                .background(accent)
                .clipShape(RoundedRectangle(cornerRadius: 14))
                .disabled(amount.isEmpty || submitting || currencyCode.count != 3)
                .opacity(amount.isEmpty ? 0.55 : 1)
                Spacer()
            }
            .padding(16)
            .background(Color(hex: "#14121B"))
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { isPresented = false }
                        .foregroundStyle(accentLight)
                }
            }
        }
        .presentationDetents([.medium, .large])
    }

    private func save() async {
        submitting = true
        error = nil
        do {
            _ = try await APIClient.shared.recordContribution(
                momentId: momentId,
                amount: amount.trimmingCharacters(in: .whitespacesAndNewlines),
                currencyCode: currencyCode.uppercased(),
                label: label.isEmpty ? nil : label
            )
            isPresented = false
            onSaved()
        } catch {
            self.error = error.localizedDescription
        }
        submitting = false
    }
}

/// Read-only participant list for Group Quick Add People tile.
struct GroupParticipantsSheet: View {
    let momentId: String
    @Binding var isPresented: Bool
    var isWedding: Bool = false

    @State private var participants: [APIClient.GroupParticipantPayload] = []
    @State private var loading = true
    @State private var error: String?

    private var accent: Color { isWedding ? WeddingActiveTheme.accentSolid : Color(hex: "#3B82F6") }

    var body: some View {
        NavigationStack {
            Group {
                if loading {
                    ProgressView().tint(accent)
                } else if let error {
                    Text(error).foregroundStyle(Color(hex: "#F87171")).padding()
                } else {
                    List(participants) { p in
                        VStack(alignment: .leading, spacing: 4) {
                            HStack(spacing: 8) {
                                Text(p.displayName ?? String(p.participantId.prefix(8)))
                                    .font(.headline)
                                if p.guest {
                                    Text("Guest")
                                        .font(.caption2.weight(.semibold))
                                        .foregroundStyle(accent)
                                        .padding(.horizontal, 6)
                                        .padding(.vertical, 2)
                                        .background(accent.opacity(0.15))
                                        .clipShape(Capsule())
                                }
                            }
                            Text("\(p.roleLabel ?? p.roleCode ?? "MEMBER") · \(p.status ?? "")")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                    }
                    .scrollContentBackground(.hidden)
                }
            }
            .background(Color(hex: "#14121B"))
            .navigationTitle("People")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { isPresented = false }
                }
            }
        }
        .presentationDetents([.medium, .large])
        .task {
            loading = true
            do {
                participants = try await APIClient.shared.listGroupParticipants(momentId: momentId)
            } catch {
                self.error = error.localizedDescription
            }
            loading = false
        }
    }
}
