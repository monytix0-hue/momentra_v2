import Foundation

struct CreateIncomeOutcome: Equatable {
    let incomeId: String
    let momentId: String
    let amount: String
    let currencyCode: String
    let status: String
    let version: Int
}

protocol IncomeCreateGateway {
    func createIncome(
        draftKey: String,
        momentId: String,
        amount: String,
        currencyCode: String,
        description: String?,
        merchantName: String?,
        categoryCode: String?,
        financialAccountId: String?,
        paymentMethodCode: String?,
        effectiveAt: String?
    ) async throws -> CreateIncomeOutcome
}

final class IncomeCreateRepository: IncomeCreateGateway {
    private let client: APIClient
    private let idempotency: IdempotencyKeyStore

    init(client: APIClient = .shared, idempotency: IdempotencyKeyStore = IdempotencyKeyStore()) {
        self.client = client
        self.idempotency = idempotency
    }

    func createIncome(
        draftKey: String,
        momentId: String,
        amount: String,
        currencyCode: String,
        description: String?,
        merchantName: String?,
        categoryCode: String?,
        financialAccountId: String?,
        paymentMethodCode: String?,
        effectiveAt: String?
    ) async throws -> CreateIncomeOutcome {
        let key = idempotency.keyFor(draftKey: draftKey)
        let response = try await client.createPersonalIncome(
            momentId: momentId,
            amount: amount,
            currencyCode: currencyCode.uppercased(),
            description: description.flatMap { $0.isEmpty ? nil : $0 },
            merchantName: merchantName.flatMap { $0.isEmpty ? nil : $0 },
            categoryCode: categoryCode.flatMap { $0.isEmpty ? nil : $0 },
            financialAccountId: financialAccountId.flatMap { $0.isEmpty ? nil : $0 },
            paymentMethodCode: paymentMethodCode.flatMap { $0.isEmpty ? nil : $0 },
            effectiveAt: effectiveAt.flatMap { $0.isEmpty ? nil : $0 },
            idempotencyKey: key
        )
        idempotency.clear(draftKey: draftKey)
        return CreateIncomeOutcome(
            incomeId: response.incomeId,
            momentId: response.momentId,
            amount: response.amount,
            currencyCode: response.currencyCode,
            status: response.status,
            version: response.version
        )
    }
}
