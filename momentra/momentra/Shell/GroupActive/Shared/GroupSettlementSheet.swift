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
    @State private var participants: [APIClient.GroupParticipantPayload] = []
    @State private var payerId: String?
    @State private var payeeId: String?
    /// LOCAL_ONLY — how paid; not sent (no settlement method column).
    @State private var paymentMethod = "EXTERNAL"
    @State private var loading = true
    @State private var submitting = false
    @State private var error: String?

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
                            participantChips(selected: payerId) { payerId = $0 }

                            fieldLabel("Payee (creditor)")
                            participantChips(selected: payeeId) { payeeId = $0 }

                            fieldLabel("Amount")
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
        do {
            let list = try await APIClient.shared.listGroupParticipants(momentId: momentId)
            let active = list.filter {
                ($0.status ?? "").uppercased() == "ACTIVE" || ($0.status ?? "").uppercased() == "INVITED"
            }
            participants = active.isEmpty ? list : active
            payeeId = participants.first?.participantId
            payerId = participants.dropFirst().first?.participantId ?? participants.first?.participantId
        } catch {
            self.error = error.localizedDescription
        }
        loading = false
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
