import SwiftUI

/// Figma 581:13027 — Update Budget sheet for Trip Quick Add.
struct GroupBudgetSheet: View {
    let momentId: String
    @Binding var isPresented: Bool
    var isWedding: Bool = false
    var onSaved: () -> Void

    @State private var amount = ""
    @State private var currency = "INR"
    @State private var spentDisplay = ""
    @State private var budgetDisplay = ""
    @State private var utilization = 0
    @State private var adjustment = "Increase"
    @State private var error: String?
    @State private var saving = false

    private var accent: Color { isWedding ? WeddingActiveTheme.accentSolid : TripSheetTokens.accent }

    var body: some View {
        NativeSheetScaffold(
            title: "Update Budget",
            onClose: { isPresented = false },
            background: isWedding ? WeddingActiveTheme.bg : TripForm.bg
        ) {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    TripSheetHeader(
                        iconAsset: "GroupQaChartBar",
                        title: "Update Budget",
                        subtitle: "Adjust the planned budget for this trip",
                        accent: accent
                    )
                    HStack(spacing: 16) {
                        ZStack {
                            Circle()
                                .stroke(TripForm.border, lineWidth: 4)
                                .frame(width: 60, height: 60)
                            Circle()
                                .trim(from: 0, to: CGFloat(utilization) / 100)
                                .stroke(accent, style: StrokeStyle(lineWidth: 4, lineCap: .round))
                                .rotationEffect(.degrees(-90))
                                .frame(width: 60, height: 60)
                            Text("\(utilization)%")
                                .font(.plusJakarta(size: 12, weight: .bold))
                                .foregroundStyle(TripForm.text)
                        }
                        VStack(alignment: .leading, spacing: 4) {
                            Text("Current Budget Status")
                                .font(.plusJakarta(size: 12))
                                .foregroundStyle(TripForm.muted)
                            Text(budgetDisplay.isEmpty ? "—" : budgetDisplay)
                                .font(.plusJakarta(size: 20, weight: .bold))
                                .foregroundStyle(TripForm.text)
                            if !spentDisplay.isEmpty {
                                Text(spentDisplay)
                                    .font(.plusJakarta(size: 11))
                                    .foregroundStyle(TripForm.muted)
                            }
                        }
                    }
                    .padding(16)
                    .background(TripForm.field)
                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(TripForm.border))
                    .clipShape(RoundedRectangle(cornerRadius: 12))

                    VStack(alignment: .leading, spacing: 6) {
                        TripFieldLabel(text: "New Budget Amount")
                        HStack(spacing: 12) {
                            Image(systemName: "wallet.pass")
                                .foregroundStyle(TripForm.muted)
                            TextField("95,000", text: $amount)
                                .font(.plusJakarta(size: 14))
                                .foregroundStyle(TripForm.text)
                                .keyboardType(.decimalPad)
                        }
                        .padding(12)
                        .background(TripForm.field)
                        .overlay(RoundedRectangle(cornerRadius: 8).stroke(TripForm.border))
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                    }

                    VStack(alignment: .leading, spacing: 6) {
                        TripFieldLabel(text: "Adjustment Strategy")
                        TripSegmentedControl(
                            options: ["Increase", "Decrease", "Redistribute"],
                            selected: $adjustment,
                            accent: accent
                        )
                    }

                    if let error {
                        Text(error)
                            .font(.plusJakarta(size: 12))
                            .foregroundStyle(Color(hex: "#F87171"))
                    }
                }
                .padding(24)
            }
        } footer: {
            TripPrimaryCta(
                label: "Update Budget",
                enabled: !amount.isEmpty && !saving,
                loading: saving,
                footer: "All members will see the update",
                colors: [accent, TripSheetTokens.accentEnd],
                onTap: { Task { await save() } }
            )
            .padding(.horizontal, 24)
            .padding(.bottom, 8)
            .background(isWedding ? WeddingActiveTheme.bg : TripForm.bg)
        }
        .presentationDetents([.large])
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
                budgetDisplay = GroupFinanceFormat.formatMoney(budget, currencyCode: currency)
            }
            if let spent = total?.expenseTotal {
                let spentFmt = GroupFinanceFormat.formatMoney(spent, currencyCode: currency)
                spentDisplay = "\(spentFmt) spent out of \(budgetDisplay)"
                utilization = GroupFinanceFormat.utilizationPercent(
                    expenseTotal: spent,
                    budgetTotal: total?.budgetTotal
                )
            }
        } catch let loadError {
            error = loadError.localizedDescription
        }
    }

    private func save() async {
        guard let parsed = GroupBudgetUtils.resolveBudgetAmount(
            displayBudget: GroupBudgetUtils.customOption,
            customAmount: amount
        ) else {
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
