import Foundation

protocol ExpenseCreateGateway {
    func createExpense(
        draftKey: String,
        momentId: String,
        amount: String,
        currencyCode: String,
        description: String?,
        merchantName: String?,
        categoryCode: String?,
        subcategoryCode: String?,
        financialAccountId: String?,
        paymentMethodCode: String?,
        effectiveAt: String?
    ) async throws -> CreateExpenseOutcome
}

final class ExpenseCreateRepository: ExpenseCreateGateway {
    private let client: APIClient
    private let idempotency: IdempotencyKeyStore

    init(client: APIClient = .shared, idempotency: IdempotencyKeyStore = IdempotencyKeyStore()) {
        self.client = client
        self.idempotency = idempotency
    }

    func createExpense(
        draftKey: String,
        momentId: String,
        amount: String,
        currencyCode: String,
        description: String?,
        merchantName: String?,
        categoryCode: String?,
        subcategoryCode: String? = nil,
        financialAccountId: String? = nil,
        paymentMethodCode: String? = nil,
        effectiveAt: String?
    ) async throws -> CreateExpenseOutcome {
        let key = idempotency.keyFor(draftKey: draftKey)
        let response = try await client.createExpense(
            momentId: momentId,
            amount: amount,
            currencyCode: currencyCode.uppercased(),
            description: description.flatMap { $0.isEmpty ? nil : $0 },
            merchantName: merchantName.flatMap { $0.isEmpty ? nil : $0 },
            categoryCode: categoryCode.flatMap { $0.isEmpty ? nil : $0 },
            subcategoryCode: subcategoryCode,
            financialAccountId: financialAccountId,
            paymentMethodCode: paymentMethodCode,
            effectiveAt: effectiveAt.flatMap { $0.isEmpty ? nil : $0 },
            idempotencyKey: key
        )
        idempotency.clear(draftKey: draftKey)
        return CreateExpenseOutcome(
            expenseId: response.expenseId,
            momentId: response.momentId,
            amount: response.amount,
            currencyCode: response.currencyCode,
            status: response.status,
            version: response.version,
            projectionHints: []
        )
    }
}
