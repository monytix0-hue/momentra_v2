import Foundation
import Testing
@testable import momentra

struct MomentCreateModelTests {
    @MainActor
    @Test func submitPersonalSetupSuccessClearsError() async {
        let model = MomentCreateModel(repository: FakeCreateGateway())
        var outcome: CreateMomentOutcome?
        model.submitPersonalSetup(
            kind: .lifeOperations,
            preferences: ["currentFeel": "Mostly Stable"],
            onSuccess: { outcome = $0 }
        )
        try? await Task.sleep(nanoseconds: 100_000_000)
        #expect(model.state.submitting == false)
        #expect(model.state.error == nil)
        #expect(outcome?.momentId == "m1")
    }

    @MainActor
    @Test func submitFailurePreservesError() async {
        let model = MomentCreateModel(repository: FakeCreateGateway(fail: true))
        model.submitPersonalSetup(
            kind: .lifeOperations,
            preferences: [:],
            onSuccess: { _ in }
        )
        try? await Task.sleep(nanoseconds: 100_000_000)
        #expect(model.state.submitting == false)
        #expect(model.state.error == "VALIDATION_FAILED")
    }
}

private final class FakeCreateGateway: MomentCreateGateway {
    private let fail: Bool

    init(fail: Bool = false) {
        self.fail = fail
    }

    func createMoment(draftKey: String, body: CreateMomentRequest) async throws -> CreateMomentOutcome {
        if fail { throw APIErrorKind.validation("VALIDATION_FAILED") }
        return success()
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
                timezone: "UTC",
                companyId: nil,
                participants: nil,
                personalSetup: .init(systemCode: systemCode, preferences: nil),
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
                timezone: "UTC",
                companyId: companyId,
                participants: nil,
                personalSetup: nil,
                businessSetup: .init(familyCode: familyCode, preferences: nil)
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
                timezone: "UTC",
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
        GroupInvite(
            inviteId: "inv1",
            inviteCode: "abcdhkmn",
            invitePath: "momentra.app/j/abcdhkmn",
            inviteUrl: "https://momentra.app/j/abcdhkmn",
            status: "PENDING",
            title: title,
            momentTypeCode: momentTypeCode,
            momentId: nil
        )
    }

    func previewGroupInvite(code: String) async throws -> GroupInvite {
        try await mintGroupInvite(draftKey: "preview", title: "Preview", momentTypeCode: "TRIP")
    }

    func redeemGroupInvite(code: String) async throws -> RedeemGroupInviteResult {
        RedeemGroupInviteResult(
            inviteCode: code,
            status: "ACTIVE",
            momentId: "m1",
            participantId: "p1",
            alreadyMember: false
        )
    }

    func mintCompanyInvite(companyId: String, membershipType: String) async throws -> CompanyInvite {
        CompanyInvite(
            inviteId: "cinv1",
            inviteCode: "abcdefgh",
            invitePath: "momentra.app/c/abcdefgh",
            inviteUrl: "https://momentra.app/c/abcdefgh",
            status: "ACTIVE",
            title: "Co",
            companyId: companyId,
            membershipType: membershipType,
            expiresAt: nil
        )
    }

    func previewCompanyInvite(code: String) async throws -> CompanyInvite {
        try await mintCompanyInvite(companyId: "co1", membershipType: "MEMBER")
    }

    func redeemCompanyInvite(code: String) async throws -> RedeemCompanyInviteResult {
        RedeemCompanyInviteResult(
            inviteId: "cinv1",
            inviteCode: code,
            status: "ACTIVE",
            companyId: "co1",
            membershipId: "cm1",
            membershipType: "MEMBER",
            alreadyMember: false
        )
    }

    func listCompanies() async throws -> [CompanySummary] {
        [CompanySummary(companyId: "co1", displayName: "Co")]
    }

    private func success() -> CreateMomentOutcome {
        CreateMomentOutcome(
            momentId: "m1",
            title: "Test",
            domainCode: "PERSONAL",
            status: "ACTIVE",
            version: 1,
            momentTypeCode: "LIFE_RHYTHM",
            setupId: "s1",
            projectionHints: [ProjectionHint(projection: "personal.moments", action: "invalidate")]
        )
    }
}

struct GroupJoinLinkTests {
    @Test func parsesShortDisplayPathAndCustomScheme() {
        #expect(GroupJoinLink.parse("momentra.app/j/abcdhkmn") == "abcdhkmn")
        #expect(GroupJoinLink.parse("https://momentra.app/j/abcdhkmn") == "abcdhkmn")
        #expect(GroupJoinLink.parse("momentra://j/abcdhkmn") == "abcdhkmn")
        #expect(GroupJoinLink.parse("abcdhkmn") == "abcdhkmn")
    }

    @Test func rejectsJwtPayloads() {
        let jwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0In0.abc"
        #expect(GroupJoinLink.parse(jwt) == nil)
        #expect(GroupJoinLink.parse("https://api.example.com/join?token=\(jwt)") == nil)
    }
}
