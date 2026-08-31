import Foundation

struct CreateFinancialAccountOutcome: Equatable {
    let financialAccountId: String
    let accountType: String
    let accountName: String
    let currencyCode: String
    let institutionName: String?
    let status: String
}

protocol FinancialAccountCreateGateway {
    func createAccount(
        draftKey: String,
        accountType: String,
        accountName: String,
        currencyCode: String,
        institutionName: String?
    ) async throws -> CreateFinancialAccountOutcome
}

final class FinancialAccountCreateRepository: FinancialAccountCreateGateway {
    private let client: APIClient
    private let idempotency: IdempotencyKeyStore

    init(client: APIClient = .shared, idempotency: IdempotencyKeyStore = IdempotencyKeyStore()) {
        self.client = client
        self.idempotency = idempotency
    }

    func createAccount(
        draftKey: String,
        accountType: String,
        accountName: String,
        currencyCode: String,
        institutionName: String?
    ) async throws -> CreateFinancialAccountOutcome {
        let key = idempotency.keyFor(draftKey: draftKey)
        let response = try await client.createFinancialAccount(
            accountType: accountType,
            accountName: accountName,
            currencyCode: currencyCode,
            institutionName: institutionName,
            idempotencyKey: key
        )
        idempotency.clear(draftKey: draftKey)
        return CreateFinancialAccountOutcome(
            financialAccountId: response.financialAccountId,
            accountType: response.accountType,
            accountName: response.accountName,
            currencyCode: response.currencyCode,
            institutionName: response.institutionName,
            status: response.status
        )
    }
}
