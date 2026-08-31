import Foundation

struct CreateObservationOutcome {
    let observationId: String
    let momentId: String
    let observationType: String
}

protocol ObservationCreateGateway {
    func recordObservation(
        draftKey: String,
        momentId: String,
        observationType: String,
        numericValue: Double?,
        textValue: String?,
        note: String?,
        activityTypeCode: String?,
        durationMinutes: Int?,
        energyAfterPct: Double?,
        feelingStateCode: String?,
        moodDrivers: [String]?
    ) async throws -> CreateObservationOutcome

    func recordAttentionCapture(
        draftKey: String,
        momentId: String,
        categoryCode: String,
        intensityCode: String,
        timeBlockCode: String,
        energyRemaining: Int?,
        note: String?
    ) async throws

    func recordLifeOpsAdjust(
        draftKey: String,
        momentId: String,
        rhythmActionCode: String?,
        signalDirectionCode: String?,
        reason: String?,
        priorityWeights: [APIClient.PriorityWeightBody]?
    ) async throws
}

final class ObservationCreateRepository: ObservationCreateGateway {
    private let client: APIClient
    private let idempotency: IdempotencyKeyStore

    init(client: APIClient = .shared, idempotency: IdempotencyKeyStore = IdempotencyKeyStore()) {
        self.client = client
        self.idempotency = idempotency
    }

    func recordObservation(
        draftKey: String,
        momentId: String,
        observationType: String,
        numericValue: Double?,
        textValue: String?,
        note: String?,
        activityTypeCode: String? = nil,
        durationMinutes: Int? = nil,
        energyAfterPct: Double? = nil,
        feelingStateCode: String? = nil,
        moodDrivers: [String]? = nil
    ) async throws -> CreateObservationOutcome {
        let key = idempotency.keyFor(draftKey: draftKey)
        let response = try await client.recordObservation(
            momentId: momentId,
            observationType: observationType,
            numericValue: numericValue,
            textValue: textValue.flatMap { $0.isEmpty ? nil : $0 },
            note: note.flatMap { $0.isEmpty ? nil : $0 },
            activityTypeCode: activityTypeCode,
            durationMinutes: durationMinutes,
            energyAfterPct: energyAfterPct,
            feelingStateCode: feelingStateCode,
            moodDrivers: moodDrivers,
            idempotencyKey: key
        )
        idempotency.clear(draftKey: draftKey)
        return CreateObservationOutcome(
            observationId: response.observationId,
            momentId: response.momentId,
            observationType: response.observationType
        )
    }

    func recordAttentionCapture(
        draftKey: String,
        momentId: String,
        categoryCode: String,
        intensityCode: String,
        timeBlockCode: String,
        energyRemaining: Int?,
        note: String?
    ) async throws {
        let key = idempotency.keyFor(draftKey: draftKey)
        _ = try await client.recordAttentionCapture(
            momentId: momentId,
            categoryCode: categoryCode,
            intensityCode: intensityCode,
            timeBlockCode: timeBlockCode,
            energyRemaining: energyRemaining,
            note: note.flatMap { $0.isEmpty ? nil : $0 },
            idempotencyKey: key
        )
        idempotency.clear(draftKey: draftKey)
    }

    func recordLifeOpsAdjust(
        draftKey: String,
        momentId: String,
        rhythmActionCode: String?,
        signalDirectionCode: String?,
        reason: String?,
        priorityWeights: [APIClient.PriorityWeightBody]?
    ) async throws {
        let key = idempotency.keyFor(draftKey: draftKey)
        _ = try await client.recordLifeOpsAdjust(
            momentId: momentId,
            rhythmActionCode: rhythmActionCode,
            signalDirectionCode: signalDirectionCode,
            reason: reason.flatMap { $0.isEmpty ? nil : $0 },
            priorityWeights: priorityWeights,
            idempotencyKey: key
        )
        idempotency.clear(draftKey: draftKey)
    }
}
