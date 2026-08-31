import Foundation

struct CreateRelationshipActivityOutcome {
    let activityId: String
    let displayName: String
}

protocol RelationshipActivityCreateGateway {
    func createRelationshipActivity(
        draftKey: String,
        momentId: String,
        activityKind: String,
        displayName: String,
        note: String?
    ) async throws -> CreateRelationshipActivityOutcome
}

final class RelationshipActivityCreateRepository: RelationshipActivityCreateGateway {
    private let client: APIClient
    private let idempotency: IdempotencyKeyStore

    init(client: APIClient = .shared, idempotency: IdempotencyKeyStore = IdempotencyKeyStore()) {
        self.client = client
        self.idempotency = idempotency
    }

    func createRelationshipActivity(
        draftKey: String,
        momentId: String,
        activityKind: String,
        displayName: String,
        note: String?
    ) async throws -> CreateRelationshipActivityOutcome {
        let key = idempotency.keyFor(draftKey: draftKey)
        let response = try await client.createRelationshipActivity(
            momentId: momentId,
            activityKind: activityKind,
            displayName: displayName,
            note: note.flatMap { $0.isEmpty ? nil : $0 },
            idempotencyKey: key
        )
        idempotency.clear(draftKey: draftKey)
        return CreateRelationshipActivityOutcome(
            activityId: response.activityId,
            displayName: response.displayName
        )
    }
}
