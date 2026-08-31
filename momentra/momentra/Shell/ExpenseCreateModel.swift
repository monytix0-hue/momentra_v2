import Combine
import Foundation

@MainActor
final class ExpenseCreateModel: ObservableObject {
    struct UiState: Equatable {
        var amount = ""
        var currencyCode = "INR"
        var description = ""
        var merchantName = ""
        var moreDetailsOpen = false
        var submitting = false
        var error: String?
        var draftId = UUID().uuidString
    }

    @Published var state = UiState()

    private let repository: ExpenseCreateGateway

    init(repository: ExpenseCreateGateway = ExpenseCreateRepository()) {
        self.repository = repository
    }

    func updateAmount(_ value: String) {
        state.amount = value
        state.error = nil
    }

    func updateCurrency(_ code: String) {
        state.currencyCode = String(code.uppercased().prefix(3))
        state.error = nil
    }

    func updateDescription(_ value: String) {
        state.description = value
    }

    func updateMerchant(_ value: String) {
        state.merchantName = value
    }

    func setMoreDetailsOpen(_ open: Bool) {
        state.moreDetailsOpen = open
    }

    func resetForAnother() {
        let currency = state.currencyCode
        state = UiState(currencyCode: currency, draftId: UUID().uuidString)
    }

    func submit(momentId: String, onSuccess: @escaping (CreateExpenseOutcome) -> Void) {
        guard !state.submitting else { return }
        guard let amount = ExpenseMoney.validateForSubmit(state.amount) else {
            state.error = "Enter a valid positive amount."
            return
        }
        let currency = state.currencyCode.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        guard currency.count == 3 else {
            state.error = "Currency must be a 3-letter code."
            return
        }

        let draftKey = "expense:\(momentId):\(state.draftId)"
        let description = state.description
        let merchant = state.merchantName
        state.submitting = true
        state.error = nil

        Task {
            do {
                let outcome = try await repository.createExpense(
                    draftKey: draftKey,
                    momentId: momentId,
                    amount: amount,
                    currencyCode: currency,
                    description: description,
                    merchantName: merchant,
                    categoryCode: nil,
                    subcategoryCode: nil,
                    financialAccountId: nil,
                    paymentMethodCode: nil,
                    effectiveAt: nil
                )
                state.submitting = false
                onSuccess(outcome)
            } catch {
                state.submitting = false
                state.error = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
            }
        }
    }
}
