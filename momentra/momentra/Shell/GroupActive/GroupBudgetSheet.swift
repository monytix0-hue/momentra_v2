import SwiftUI

struct GroupBudgetSheet: View {
    let momentId: String
    @Binding var isPresented: Bool
    var isWedding: Bool = false
    var onSaved: () -> Void

    @State private var amount = ""
    @State private var currency = "INR"
    @State private var error: String?
    @State private var saving = false

    private var bg: Color { isWedding ? WeddingActiveTheme.bg : GroupActiveTheme.bg }
    private var text: Color { isWedding ? WeddingActiveTheme.text : GroupActiveTheme.text }
    private var secondary: Color { isWedding ? WeddingActiveTheme.secondary : GroupActiveTheme.secondary }
    private var accent: Color { isWedding ? WeddingActiveTheme.accentSolid : GroupActiveTheme.brand }

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 14) {
                Text("Updates the planned budget for this shared moment.")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(secondary)
                TextField("₹84,000", text: $amount)
                    .font(.plusJakarta(size: 16, weight: .semibold))
                    .foregroundStyle(text)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 10)
                    .background(Color(hex: "#161B26"))
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                    .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color(hex: "#1E293B")))
                if let error {
                    Text(error)
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(Color(hex: "#F87171"))
                }
                Spacer()
            }
            .padding(20)
            .background(bg)
            .navigationTitle("Edit group budget")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { isPresented = false }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(saving ? "Saving…" : "Save") { Task { await save() } }
                        .foregroundStyle(accent)
                        .disabled(saving)
                }
            }
        }
        .task { await loadCurrent() }
    }

    private func loadCurrent() async {
        do {
            let finance = try await APIClient.shared.getGroupFinance(momentId: momentId)
            let total = finance.payload?.totals?.first
            currency = total?.currencyCode ?? "INR"
            if let budget = total?.budgetTotal {
                amount = GroupBudgetUtils.formatApiAmountForDisplay(budget, currencyCode: currency)
                    .replacingOccurrences(of: "₹", with: "")
            }
        } catch let loadError {
            error = loadError.localizedDescription
        }
    }

    private func save() async {
        guard let parsed = GroupBudgetUtils.resolveBudgetAmount(displayBudget: GroupBudgetUtils.customOption, customAmount: amount) else {
            error = "Enter a valid amount"
            return
        }
        saving = true
        error = nil
        do {
            _ = try await APIClient.shared.patchGroupBudget(
                momentId: momentId,
                budgetAmount: parsed,
                budgetCurrencyCode: currency
            )
            saving = false
            onSaved()
            isPresented = false
        } catch {
            saving = false
            self.error = error.localizedDescription
        }
    }
}
