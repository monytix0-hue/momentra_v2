import Foundation

struct CreateMovementOutcome: Equatable {
    let movementId: String
    let momentId: String
    let amount: String
    let movementType: String
}

protocol MovementCreateGateway {
    func createMovement(
        draftKey: String,
        momentId: String,
        movementType: String,
        amount: String,
        currencyCode: String,
        accountId: String?,
        goalId: String?,
        description: String?,
        effectiveAt: String?
    ) async throws -> CreateMovementOutcome
}

final class MovementCreateRepository: MovementCreateGateway {
    private let client: APIClient
    private let idempotency: IdempotencyKeyStore

    init(client: APIClient = .shared, idempotency: IdempotencyKeyStore = IdempotencyKeyStore()) {
        self.client = client
        self.idempotency = idempotency
    }

    func createMovement(
        draftKey: String,
        momentId: String,
        movementType: String,
        amount: String,
        currencyCode: String,
        accountId: String?,
        goalId: String?,
        description: String?,
        effectiveAt: String?
    ) async throws -> CreateMovementOutcome {
        let key = idempotency.keyFor(draftKey: draftKey)
        let response = try await client.createMovement(
            momentId: momentId,
            movementType: movementType,
            amount: amount,
            currencyCode: currencyCode.uppercased(),
            accountId: accountId.flatMap { $0.isEmpty ? nil : $0 },
            goalId: goalId.flatMap { $0.isEmpty ? nil : $0 },
            description: description.flatMap { $0.isEmpty ? nil : $0 },
            effectiveAt: effectiveAt.flatMap { $0.isEmpty ? nil : $0 },
            idempotencyKey: key
        )
        idempotency.clear(draftKey: draftKey)
        return CreateMovementOutcome(
            movementId: response.movementId,
            momentId: response.momentId,
            amount: response.amount,
            movementType: response.movementType
        )
    }
}
