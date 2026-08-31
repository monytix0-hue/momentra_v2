import Foundation

struct CreateFutureItemOutcome {
    let itemId: String
    let kind: String
    let title: String
}

protocol FutureItemCreateGateway {
    func createFutureItem(
        draftKey: String,
        momentId: String,
        kind: String,
        title: String,
        description: String?,
        progressValue: Double?
    ) async throws -> CreateFutureItemOutcome
}

final class FutureItemCreateRepository: FutureItemCreateGateway {
    private let client: APIClient
    private let idempotency: IdempotencyKeyStore

    init(client: APIClient = .shared, idempotency: IdempotencyKeyStore = IdempotencyKeyStore()) {
        self.client = client
        self.idempotency = idempotency
    }

    func createFutureItem(
        draftKey: String,
        momentId: String,
        kind: String,
        title: String,
        description: String?,
        progressValue: Double?
    ) async throws -> CreateFutureItemOutcome {
        let key = idempotency.keyFor(draftKey: draftKey)
        let response = try await client.createFutureItem(
            momentId: momentId,
            kind: kind,
            title: title,
            description: description.flatMap { $0.isEmpty ? nil : $0 },
            progressValue: progressValue,
            idempotencyKey: key
        )
        idempotency.clear(draftKey: draftKey)
        return CreateFutureItemOutcome(
            itemId: response.itemId,
            kind: response.kind,
            title: response.title
        )
    }
}
