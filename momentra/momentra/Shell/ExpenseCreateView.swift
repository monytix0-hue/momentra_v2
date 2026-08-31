import SwiftUI

/// Personal Master Expense — amount-first compact form (contracted fields only).
struct ExpenseCreateView: View {
    let momentId: String
    let momentTitle: String?
    @ObservedObject var model: ExpenseCreateModel
    var onBack: () -> Void
    var onCreated: (CreateExpenseOutcome) -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Button(action: onBack) {
                    HStack(spacing: 8) {
                        Text("‹").font(.system(size: 28))
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Add expense").font(.system(size: 17, weight: .semibold))
                            if let momentTitle, !momentTitle.isEmpty {
                                Text(momentTitle).font(.system(size: 13)).foregroundStyle(Color(hex: "#C9C4D8"))
                            }
                        }
                        Spacer()
                    }
                    .foregroundStyle(MomentraBrandTokens.textOnDark)
                }
                .buttonStyle(.plain)

                Text("Amount")
                    .font(.system(size: 12, weight: .bold))
                    .foregroundStyle(Color(hex: "#C9C4D8"))

                TextField("0.00", text: Binding(
                    get: { model.state.amount },
                    set: { model.updateAmount($0) }
                ))
                .keyboardType(.decimalPad)
                .font(.system(size: 40, weight: .bold))
                .foregroundStyle(MomentraBrandTokens.textOnDark)
                .accessibilityLabel("Expense amount")

                HStack(spacing: 8) {
                    Text("Currency")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundStyle(Color(hex: "#C9C4D8"))
                    TextField("INR", text: Binding(
                        get: { model.state.currencyCode },
                        set: { model.updateCurrency($0) }
                    ))
                    .textInputAutocapitalization(.characters)
                    .font(.system(size: 16, weight: .semibold))
                    .padding(.horizontal, 12)
                    .padding(.vertical, 10)
                    .background(Color(hex: "#1C1926"), in: RoundedRectangle(cornerRadius: 10))
                    .accessibilityLabel("Currency code")
                }

                Button {
                    model.setMoreDetailsOpen(!model.state.moreDetailsOpen)
                } label: {
                    Text("More details")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(Color(hex: "#7C5CFC"))
                }
                .buttonStyle(.plain)

                if model.state.moreDetailsOpen {
                    VStack(alignment: .leading, spacing: 12) {
                        labeledField("Merchant", text: Binding(
                            get: { model.state.merchantName },
                            set: { model.updateMerchant($0) }
                        ))
                        labeledField("Note", text: Binding(
                            get: { model.state.description },
                            set: { model.updateDescription($0) }
                        ))
                    }
                    .padding(16)
                    .background(Color(hex: "#1C1926"), in: RoundedRectangle(cornerRadius: 16))
                }

                if let error = model.state.error {
                    Text(error)
                        .font(.system(size: 13))
                        .foregroundStyle(Color(hex: "#FF8A80"))
                }

                Button {
                    model.submit(momentId: momentId, onSuccess: onCreated)
                } label: {
                    ZStack {
                        if model.state.submitting {
                            ProgressView().tint(.white)
                        } else {
                            Text("Save Expense").font(.system(size: 16, weight: .bold))
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 56)
                    .foregroundStyle(.white)
                    .background(Color(hex: "#7C5CFC"), in: RoundedRectangle(cornerRadius: 14))
                }
                .buttonStyle(.plain)
                .disabled(model.state.submitting)
                .accessibilityLabel("Save expense")
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 16)
        }
        .background(Color(hex: "#14121B").ignoresSafeArea())
    }

    private func labeledField(_ label: String, text: Binding<String>) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(label.uppercased())
                .font(.system(size: 11, weight: .bold))
                .foregroundStyle(Color(hex: "#C9C4D8"))
            TextField("", text: text)
                .font(.system(size: 15))
                .foregroundStyle(MomentraBrandTokens.textOnDark)
        }
    }
}
