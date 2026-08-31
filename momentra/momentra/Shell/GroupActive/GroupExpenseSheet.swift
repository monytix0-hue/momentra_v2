import SwiftUI

/// Figma Sheet / Add Expense — payer + split strategy; EQUAL default; server-authoritative submit.
struct GroupExpenseSheet: View {
    let momentId: String
    @Binding var isPresented: Bool
    var isWedding: Bool = false
    var onSaved: () -> Void

    @State private var amount = ""
    @State private var currencyCode = "INR"
    @State private var descriptionText = ""
    @State private var participants: [APIClient.GroupParticipantPayload] = []
    @State private var paidByParticipantId: String?
    @State private var selectedParticipantIds: Set<String> = []
    @State private var splitStrategy = "EQUAL"
    /// PERCENTAGE / EXACT / SHARES input values keyed by participantId
    @State private var splitValues: [String: String] = [:]
    @State private var loading = true
    @State private var submitting = false
    @State private var error: String?

    private var accent: Color { isWedding ? WeddingActiveTheme.accentSolid : TripSheetTokens.accent }
    private var peach: Color { isWedding ? WeddingActiveTheme.accentLight : TripSheetTokens.accentEnd }
    private let strategies = ["EQUAL", "PERCENTAGE", "EXACT", "SHARES"]

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    header
                    formCard
                    if let error {
                        Text(error)
                            .font(.caption)
                            .foregroundStyle(Color(hex: "#F87171"))
                    }
                    saveButton
                    Text("Split math is calculated on the server. Settlements use POST /v1/moments/:id/settlements.")
                        .font(.system(size: 11))
                        .foregroundStyle(isWedding ? Color(hex: "#C9C4D8") : TripSheetTokens.muted)
                }
                .padding(16)
            }
            .background(isWedding ? Color(hex: "#14121B") : TripSheetTokens.bg)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { isPresented = false }
                        .foregroundStyle(peach)
                }
            }
        }
        .presentationDetents([.large])
        .presentationDragIndicator(.visible)
        .task { await loadParticipants() }
    }

    private var header: some View {
        HStack(spacing: 10) {
            Text("💳")
                .font(.system(size: 16))
                .frame(width: 36, height: 36)
                .background(accent.opacity(0.18))
                .overlay(RoundedRectangle(cornerRadius: 18).stroke(accent.opacity(0.35)))
                .clipShape(RoundedRectangle(cornerRadius: 18))
            VStack(alignment: .leading, spacing: 2) {
                Text(isWedding ? "Add Expense" : "Group expense")
                    .font(.system(size: 18, weight: .heavy))
                    .foregroundStyle(isWedding ? Color(hex: "#E5E0EE") : TripSheetTokens.text)
                Text(isWedding ? "Track and split wedding costs" : "Split is computed on the server. Preview is local only.")
                    .font(.system(size: 12))
                    .foregroundStyle(isWedding ? Color(hex: "#C9C4D8") : TripSheetTokens.muted)
            }
        }
    }

    private var formCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            fieldLabel("AMOUNT")
            HStack(spacing: 10) {
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
            .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color(hex: "#938EA1"), lineWidth: 1))
            .clipShape(RoundedRectangle(cornerRadius: 12))

            fieldLabel("DESCRIPTION")
            TextField("Dinner, hotel, supplies…", text: $descriptionText)
                .foregroundStyle(Color(hex: "#E5E0EE"))
                .padding(12)
                .background(Color(hex: "#201E28"))
                .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color(hex: "#938EA1"), lineWidth: 1))
                .clipShape(RoundedRectangle(cornerRadius: 12))

            fieldLabel("PAID BY")
            if loading {
                ProgressView().tint(accent)
            } else if participants.isEmpty {
                Text("No participants on this Moment.")
                    .font(.caption)
                    .foregroundStyle(Color(hex: "#F87171"))
            } else {
                FlowLayout(spacing: 8) {
                    ForEach(participants) { p in
                        let on = paidByParticipantId == p.participantId
                        Button {
                            paidByParticipantId = p.participantId
                        } label: {
                            Text(p.displayName ?? shortId(p.participantId))
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
            }

            fieldLabel("SPLIT TYPE")
            FlowLayout(spacing: 8) {
                ForEach(strategies, id: \.self) { strategy in
                    let on = splitStrategy == strategy
                    Button {
                        splitStrategy = strategy
                        seedSplitValues(for: strategy)
                    } label: {
                        Text(strategy.capitalized)
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

            fieldLabel("SPLIT BETWEEN")
            FlowLayout(spacing: 8) {
                ForEach(participants) { p in
                    let on = selectedParticipantIds.contains(p.participantId)
                    Button {
                        if on {
                            selectedParticipantIds.remove(p.participantId)
                        } else {
                            selectedParticipantIds.insert(p.participantId)
                        }
                    } label: {
                        HStack(spacing: 6) {
                            Image(systemName: on ? "checkmark.circle.fill" : "circle")
                            Text(p.displayName ?? shortId(p.participantId))
                                .font(.system(size: 12, weight: .bold))
                        }
                        .foregroundStyle(on ? .white : Color(hex: "#C9C4D8"))
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(on ? accent.opacity(0.85) : Color(hex: "#201E28"))
                        .clipShape(Capsule())
                    }
                    .buttonStyle(.plain)
                }
            }

            if splitStrategy != "EQUAL" && !selectedParticipantIds.isEmpty {
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
                    Text("Save Expense")
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
    }

    private var canSubmit: Bool {
        !amount.isEmpty
            && paidByParticipantId != nil
            && !selectedParticipantIds.isEmpty
            && currencyCode.count == 3
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
            _ = try await APIClient.shared.createGroupExpense(
                momentId: momentId,
                amount: amount.trimmingCharacters(in: .whitespacesAndNewlines),
                currencyCode: currencyCode.uppercased(),
                description: descriptionText.isEmpty ? nil : descriptionText,
                paidByParticipantId: paidBy,
                splitStrategy: splitStrategy,
                splitInputs: inputs
            )
            isPresented = false
            onSaved()
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
                            Text(p.displayName ?? String(p.participantId.prefix(8)))
                                .font(.headline)
                            Text("\(p.roleCode ?? "MEMBER") · \(p.status ?? "")")
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
