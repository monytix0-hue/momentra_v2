import Foundation

protocol MomentCreateGateway {
    func createMoment(draftKey: String, body: CreateMomentRequest) async throws -> CreateMomentOutcome
    func createPersonalSetup(
        draftKey: String,
        systemCode: String,
        title: String,
        description: String?,
        momentTypeCode: String,
        preferences: [String: Any]
    ) async throws -> CreateMomentOutcome
    func createBusinessSetup(
        draftKey: String,
        companyId: String,
        familyCode: String,
        title: String,
        description: String?,
        momentTypeCode: String,
        preferences: [String: Any]
    ) async throws -> CreateMomentOutcome
    func createGroupMoment(
        draftKey: String,
        momentTypeCode: String,
        title: String,
        description: String?,
        startAt: String?,
        endAt: String?,
        participants: [CreateMomentParticipantInput],
        inviteCode: String?,
        customTypeLabel: String?,
        groupSetup: CreateMomentRequest.GroupSetupBlock?
    ) async throws -> CreateMomentOutcome
    func mintGroupInvite(draftKey: String, title: String, momentTypeCode: String) async throws -> GroupInvite
    func previewGroupInvite(code: String) async throws -> GroupInvite
    func redeemGroupInvite(code: String) async throws -> RedeemGroupInviteResult
    func mintCompanyInvite(companyId: String, membershipType: String) async throws -> CompanyInvite
    func previewCompanyInvite(code: String) async throws -> CompanyInvite
    func redeemCompanyInvite(code: String) async throws -> RedeemCompanyInviteResult
    func listCompanies() async throws -> [CompanySummary]
}

final class MomentCreateRepository: MomentCreateGateway {
    private let client: APIClient
    private let idempotency: IdempotencyKeyStore

    init(client: APIClient = .shared, idempotency: IdempotencyKeyStore = IdempotencyKeyStore()) {
        self.client = client
        self.idempotency = idempotency
    }

    func createMoment(draftKey: String, body: CreateMomentRequest) async throws -> CreateMomentOutcome {
        try await runCreate(draftKey: draftKey) { key in
            try await client.createMoment(body: body, idempotencyKey: key)
        }
    }

    func createPersonalSetup(
        draftKey: String,
        systemCode: String,
        title: String,
        description: String?,
        momentTypeCode: String,
        preferences: [String: Any]
    ) async throws -> CreateMomentOutcome {
        try await createMoment(
            draftKey: draftKey,
            body: CreateMomentRequest(
                domainCode: "PERSONAL",
                momentTypeCode: momentTypeCode,
                title: title,
                description: description,
                startAt: nil,
                endAt: nil,
                timezone: TimeZone.current.identifier,
                companyId: nil,
                participants: nil,
                personalSetup: .init(
                    systemCode: systemCode,
                    preferences: JSONEncodableValue.map(preferences)
                ),
                businessSetup: nil
            )
        )
    }

    func createBusinessSetup(
        draftKey: String,
        companyId: String,
        familyCode: String,
        title: String,
        description: String?,
        momentTypeCode: String,
        preferences: [String: Any]
    ) async throws -> CreateMomentOutcome {
        try await createMoment(
            draftKey: draftKey,
            body: CreateMomentRequest(
                domainCode: "BUSINESS",
                momentTypeCode: momentTypeCode,
                title: title,
                description: description,
                startAt: nil,
                endAt: nil,
                timezone: TimeZone.current.identifier,
                companyId: companyId,
                participants: nil,
                personalSetup: nil,
                businessSetup: .init(
                    familyCode: familyCode,
                    preferences: JSONEncodableValue.map(preferences)
                )
            )
        )
    }

    func createGroupMoment(
        draftKey: String,
        momentTypeCode: String,
        title: String,
        description: String?,
        startAt: String?,
        endAt: String?,
        participants: [CreateMomentParticipantInput],
        inviteCode: String?,
        customTypeLabel: String? = nil,
        groupSetup: CreateMomentRequest.GroupSetupBlock? = nil
    ) async throws -> CreateMomentOutcome {
        try await createMoment(
            draftKey: draftKey,
            body: CreateMomentRequest(
                domainCode: "GROUP",
                momentTypeCode: momentTypeCode,
                title: title,
                description: description,
                startAt: startAt,
                endAt: endAt,
                timezone: TimeZone.current.identifier,
                customTypeLabel: customTypeLabel,
                companyId: nil,
                participants: participants.isEmpty ? nil : participants,
                inviteCode: inviteCode,
                personalSetup: nil,
                businessSetup: nil,
                groupSetup: groupSetup
            )
        )
    }

    func mintGroupInvite(draftKey: String, title: String, momentTypeCode: String) async throws -> GroupInvite {
        let key = idempotency.keyFor(draftKey: draftKey)
        return try await client.mintGroupInvite(
            title: title,
            momentTypeCode: momentTypeCode,
            idempotencyKey: key
        )
    }

    func redeemGroupInvite(code: String) async throws -> RedeemGroupInviteResult {
        try await client.redeemGroupInvite(code: code, idempotencyKey: UUID().uuidString)
    }

    func previewGroupInvite(code: String) async throws -> GroupInvite {
        try await client.getGroupInvite(code: code)
    }

    func previewCompanyInvite(code: String) async throws -> CompanyInvite {
        try await client.getCompanyInvite(code: code)
    }

    func mintCompanyInvite(companyId: String, membershipType: String) async throws -> CompanyInvite {
        try await client.mintCompanyInvite(
            companyId: companyId,
            membershipType: membershipType,
            idempotencyKey: UUID().uuidString
        )
    }

    func redeemCompanyInvite(code: String) async throws -> RedeemCompanyInviteResult {
        try await client.redeemCompanyInvite(code: code, idempotencyKey: UUID().uuidString)
    }

    func listCompanies() async throws -> [CompanySummary] {
        try await client.listCompanies()
    }

    private func runCreate(
        draftKey: String,
        call: (String) async throws -> CreateMomentAPIResponse
    ) async throws -> CreateMomentOutcome {
        let key = idempotency.keyFor(draftKey: draftKey)
        do {
            let response = try await call(key)
            idempotency.clear(draftKey: draftKey)
            return response.toOutcome()
        } catch {
            throw error
        }
    }
}

private extension CreateMomentAPIResponse {
    func toOutcome() -> CreateMomentOutcome {
        CreateMomentOutcome(
            momentId: result.momentId,
            title: result.title,
            domainCode: result.domainCode,
            status: result.status,
            version: result.version,
            momentTypeCode: result.momentTypeCode,
            setupId: result.setupId,
            projectionHints: projectionHints.map {
                ProjectionHint(projection: $0.projection, action: $0.action ?? "invalidate")
            }
        )
    }
}
