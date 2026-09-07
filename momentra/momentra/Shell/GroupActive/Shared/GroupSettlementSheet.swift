import SwiftUI

/// Shared Group settlement sheet for all G01–G12 subtypes (QH-W).
/// Payer = debtor settling; payee = creditor receiving.
struct GroupSettlementSheet: View {
    let momentId: String
    var momentTypeCode: String? = nil
    @Binding var isPresented: Bool
    var onSaved: () -> Void

    @State private var amount = ""
    @State private var currencyCode = "INR"
    @State private var preferredCurrencyCodes: [String] = ["INR"]
    @State private var participants: [APIClient.GroupParticipantPayload] = []
    @State private var positions: [APIClient.GroupFinancePositionPayload] = []
    @State private var payerId: String?
    @State private var payeeId: String?
    /// LOCAL_ONLY — how paid; not sent (no settlement method column).
    @State private var paymentMethod = "EXTERNAL"
    @State private var loading = true
    @State private var submitting = false
    @State private var error: String?
    @State private var amountEditedByUser = false
    @State private var lastSuggestedAmount = ""

    private var accent: Color {
        MomentThemes.resolve(context: .group, momentTypeCode: momentTypeCode).primary
    }

    var body: some View {
        NavigationStack {
            Group {
                if loading {
                    ProgressView().tint(accent)
                } else {
                    ScrollView {
                        VStack(alignment: .leading, spacing: 14) {
                            Text("Record settlement")
                                .font(.system(size: 18, weight: .heavy))
                                .foregroundStyle(Color(hex: "#E5E0EE"))
                            Text("Payer pays payee to reduce open balances. Method chips are local-only (not processed).")
                                .font(.system(size: 12))
                                .foregroundStyle(Color(hex: "#A8A3B5"))

                            fieldLabel("Payer (debtor)")
                            participantChips(selected: payerId) { id in
                                payerId = id
                                applySuggestionForPayer()
                            }

                            fieldLabel("Payee (creditor)")
                            participantChips(selected: payeeId) { payeeId = $0 }

                            fieldLabel("Amount")
                            TravelCurrencyPicker(
                                selectedCode: $currencyCode,
                                preferredCodes: preferredCurrencyCodes,
                                accentColor: accent
                            )
                            TextField("0.00", text: $amount)
                                .keyboardType(.decimalPad)
                                .font(.system(size: 26, weight: .heavy))
                                .foregroundStyle(Color(hex: "#E5E0EE"))
                                .padding(12)
                                .background(Color(hex: "#201E28"))
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                                .onChange(of: amount) { _, newValue in
                                    if newValue != lastSuggestedAmount {
                                        amountEditedByUser = true
                                    }
                                }

                            fieldLabel("How paid (local only)")
                            FlowLayout(spacing: 8) {
                                ForEach([("UPI", "UPI"), ("BANK", "Bank Transfer"), ("EXTERNAL", "Mark as Paid Externally")], id: \.0) { code, label in
                                    let on = paymentMethod == code
                                    Button { paymentMethod = code } label: {
                                        Text(label)
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
                                    Text("Save Settlement")
                                        .font(.system(size: 15, weight: .heavy))
                                        .foregroundStyle(.white)
                                        .frame(maxWidth: .infinity)
                                        .padding(.vertical, 14)
                                }
                            }
                            .background(accent)
                            .clipShape(RoundedRectangle(cornerRadius: 14))
                            .disabled(!canSubmit || submitting)
                            .opacity(canSubmit ? 1 : 0.55)
                        }
                        .padding(16)
                    }
                }
            }
            .background(Color(hex: "#14121B"))
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { isPresented = false }
                        .foregroundStyle(accent)
                }
            }
        }
        .presentationDetents([.medium, .large])
        .accessibilityIdentifier("qa.tile.settle.sheet")
        .task { await loadParticipants() }
        .onChange(of: currencyCode) { _, _ in
            applySuggestionForCurrency()
        }
    }

    private var canSubmit: Bool {
        guard let payerId, let payeeId, payerId != payeeId else { return false }
        return !amount.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && currencyCode.count == 3
    }

    private func fieldLabel(_ text: String) -> some View {
        Text(text)
            .font(.system(size: 11, weight: .semibold))
            .foregroundStyle(Color(hex: "#A8A3B5"))
    }

    private func participantChips(selected: String?, onSelect: @escaping (String) -> Void) -> some View {
        FlowLayout(spacing: 8) {
            ForEach(participants) { p in
                let isSelected = p.participantId == selected
                let label = (p.displayName?.isEmpty == false ? p.displayName! : (p.roleCode ?? "Member"))
                Text(label)
                    .font(.system(size: 12))
                    .foregroundStyle(Color(hex: "#E5E0EE"))
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background(isSelected ? accent.opacity(0.25) : Color(hex: "#201E28"))
                    .overlay(
                        RoundedRectangle(cornerRadius: 20)
                            .stroke(isSelected ? accent : Color(hex: "#3A3648"), lineWidth: 1)
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 20))
                    .onTapGesture { onSelect(p.participantId) }
            }
        }
    }

    private func loadParticipants() async {
        loading = true
        error = nil
        amountEditedByUser = false
        lastSuggestedAmount = ""
        let ctx = await MomentCurrencyContextLoader.loadGroup(momentId: momentId)
        preferredCurrencyCodes = ctx.preferred
        currencyCode = ctx.primary
        do {
            async let participantsTask = APIClient.shared.listGroupParticipants(momentId: momentId)
            async let financeTask = APIClient.shared.getGroupFinance(momentId: momentId)
            let list = try await participantsTask
            let finance = try? await financeTask
            positions = finance?.payload?.positions ?? []

            let active = list.filter {
                ($0.status ?? "").uppercased() == "ACTIVE" || ($0.status ?? "").uppercased() == "INVITED"
            }
            participants = active.isEmpty ? list : active

            let debtor = defaultDebtorId()
            let creditor = defaultCreditorId(excluding: debtor)
            payerId = debtor
            payeeId = creditor
            applySuggestionForPayer()
        } catch {
            self.error = error.localizedDescription
        }
        loading = false
    }

    private func defaultDebtorId() -> String? {
        for p in participants {
            let rows = positions.filter { $0.participantId == p.participantId }
            if rows.contains(where: {
                GroupFinanceFormat.parseAmount($0.payableTotal) > 0
                    || GroupFinanceFormat.parseAmount($0.netPosition) < 0
            }) {
                return p.participantId
            }
        }
        return participants.dropFirst().first?.participantId ?? participants.first?.participantId
    }

    private func defaultCreditorId(excluding payer: String?) -> String? {
        for p in participants where p.participantId != payer {
            let rows = positions.filter { $0.participantId == p.participantId }
            if rows.contains(where: {
                GroupFinanceFormat.parseAmount($0.receivableTotal) > 0
                    || GroupFinanceFormat.parseAmount($0.netPosition) > 0
            }) {
                return p.participantId
            }
        }
        return participants.first(where: { $0.participantId != payer })?.participantId
    }

    /// Best open outstanding for the payer: prefer payableTotal > 0, else abs(negative net).
    private func outstandingForPayer(currency: String? = nil) -> (amount: Decimal, currency: String)? {
        guard let payerId else { return nil }
        let rows = positions.filter { $0.participantId == payerId }
        let candidates: [(Decimal, String)] = rows.compactMap { pos in
            let payable = GroupFinanceFormat.parseAmount(pos.payableTotal)
            if payable > 0 { return (payable, pos.currencyCode) }
            let net = GroupFinanceFormat.parseAmount(pos.netPosition)
            if net < 0 { return (-net, pos.currencyCode) }
            return nil
        }
        if let currency {
            let code = currency.uppercased()
            return candidates.first { $0.1.uppercased() == code }.map { ($0.0, $0.1) }
        }
        return candidates.max(by: { $0.0 < $1.0 })
    }

    private func formatSuggestionAmount(_ value: Decimal) -> String {
        var v = value
        var rounded = Decimal()
        NSDecimalRound(&rounded, &v, 4, .plain)
        let formatter = NumberFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.numberStyle = .decimal
        formatter.usesGroupingSeparator = false
        formatter.minimumFractionDigits = 0
        formatter.maximumFractionDigits = 4
        return formatter.string(from: rounded as NSDecimalNumber) ?? "0"
    }

    private func applySuggestionForPayer() {
        amountEditedByUser = false
        guard let suggestion = outstandingForPayer() else { return }
        currencyCode = suggestion.currency
        if !preferredCurrencyCodes.contains(where: { $0.uppercased() == suggestion.currency.uppercased() }) {
            preferredCurrencyCodes.insert(suggestion.currency, at: 0)
        }
        let formatted = formatSuggestionAmount(suggestion.amount)
        lastSuggestedAmount = formatted
        amount = formatted
    }

    private func applySuggestionForCurrency() {
        guard !amountEditedByUser else { return }
        guard let suggestion = outstandingForPayer(currency: currencyCode) else { return }
        let formatted = formatSuggestionAmount(suggestion.amount)
        lastSuggestedAmount = formatted
        amount = formatted
    }

    private func save() async {
        guard let payerId, let payeeId else { return }
        submitting = true
        error = nil
        do {
            _ = try await APIClient.shared.createSettlement(
                momentId: momentId,
                payerParticipantId: payerId,
                payeeParticipantId: payeeId,
                amount: amount.trimmingCharacters(in: .whitespacesAndNewlines),
                currencyCode: currencyCode.uppercased()
            )
            isPresented = false
            onSaved()
        } catch {
            self.error = error.localizedDescription
        }
        submitting = false
    }
}
