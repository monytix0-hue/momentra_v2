import Foundation

struct CreateLifestyleActivityOutcome {
    let activityId: String
    let lifestyleContext: String
    let title: String
}

protocol LifestyleActivityCreateGateway {
    func createLifestyleActivity(
        draftKey: String,
        momentId: String,
        lifestyleContext: String,
        title: String,
        description: String?,
        wellbeingRating: Double?
    ) async throws -> CreateLifestyleActivityOutcome
}

final class LifestyleActivityCreateRepository: LifestyleActivityCreateGateway {
    private let client: APIClient
    private let idempotency: IdempotencyKeyStore

    init(client: APIClient = .shared, idempotency: IdempotencyKeyStore = IdempotencyKeyStore()) {
        self.client = client
        self.idempotency = idempotency
    }

    func createLifestyleActivity(
        draftKey: String,
        momentId: String,
        lifestyleContext: String,
        title: String,
        description: String?,
        wellbeingRating: Double?
    ) async throws -> CreateLifestyleActivityOutcome {
        let key = idempotency.keyFor(draftKey: draftKey)
        let response = try await client.createLifestyleActivity(
            momentId: momentId,
            lifestyleContext: lifestyleContext,
            title: title,
            description: description.flatMap { $0.isEmpty ? nil : $0 },
            wellbeingRating: wellbeingRating,
            idempotencyKey: key
        )
        idempotency.clear(draftKey: draftKey)
        return CreateLifestyleActivityOutcome(
            activityId: response.activityId,
            lifestyleContext: response.lifestyleContext,
            title: response.title
        )
    }
}
