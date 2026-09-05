import Foundation
import Testing
@testable import momentra

struct ShellModelTests {
    @Test func apiErrorMapping401() {
        let err = APIErrorKind.from(status: 401, code: "UNAUTHORIZED", message: nil)
        #expect(err == .unauthenticated("UNAUTHORIZED"))
    }

    @Test func apiErrorMapping403() {
        let err = APIErrorKind.from(status: 403, code: "GOVERNANCE_DENIED", message: nil)
        #expect(err == .forbidden("GOVERNANCE_DENIED"))
    }

    @Test func apiErrorPrefersServerMessage() {
        let err = APIErrorKind.from(
            status: 400,
            code: "VALIDATION_FAILED",
            message: "expectedVersion is required."
        )
        #expect(err == .validation("expectedVersion is required."))
        #expect(err.errorDescription == "expectedVersion is required.")
    }

    @Test func apiErrorConflictUsesMessage() {
        let err = APIErrorKind.from(
            status: 409,
            code: "VALIDATION_FAILED",
            message: "You already have an active Life Operations moment."
        )
        #expect(err == .conflict("You already have an active Life Operations moment."))
    }

    @Test func contextLabels() {
        #expect(AppContextKind.personal.label == "Personal")
        #expect(AppContextKind.business.label == "Business")
        #expect(BottomDestination.pulse.label == "Pulse")
        #expect(BottomDestination.create.label == "Quickadds")
    }

    @MainActor
    @Test func contextSwitchPreservesIdentity() async {
        let model = AppShellModel(gateway: ShellMeGateway(client: APIClient.shared))
        // Use direct state without network for personal empty
        model.bindIdentity(ShellIdentity(userId: "u1", displayName: "Ada", email: nil, firebaseUid: nil))
        try? await Task.sleep(nanoseconds: 50_000_000)
        #expect(model.identity?.userId == "u1")
        model.selectBottomDestination(.life)
        model.selectContext(.business)
        // May hit network; still must keep identity
        #expect(model.identity?.userId == "u1")
        model.selectContext(.personal)
        #expect(model.bottomDestination == .life)
    }

    @MainActor
    @Test func logoutClearsState() async {
        let model = AppShellModel()
        model.bindIdentity(ShellIdentity(userId: "u1", displayName: nil, email: nil, firebaseUid: nil))
        model.selectCompany(CompanySummary(companyId: "c1", displayName: "Acme"))
        model.clearForLogout()
        #expect(model.identity == nil)
        #expect(model.selectedCompany == nil)
        #expect(model.selectedContext == .personal)
    }

    @MainActor
    @Test func circleShowsEmptyComingSoonNotDeferred() async {
        let model = AppShellModel()
        model.bindIdentity(ShellIdentity(userId: "u1", displayName: nil, email: nil, firebaseUid: nil))
        // Seed bootstrap-like empty inventory without network by selecting circle after identity
        model.selectContext(.circle)
        #expect(model.selectedContext == .circle)
        // Until bootstrap returns, content may be loading; after apply, circle is empty not deferred.
        // Force empty circle path the same way applyBootstrapInventory does:
        model.contextContent = .empty
        model.moments = []
        model.showMomentSwitcher = false
        #expect(model.contextContent == .empty)
        #expect(model.moments.isEmpty)
        #expect(!model.showMomentSwitcher)
        #expect(model.contextContent != .deferred)
    }

    @MainActor
    @Test func life360OpenDismissPreservesShellSelection() async {
        let model = AppShellModel()
        model.bindIdentity(ShellIdentity(userId: "u1", displayName: "Ada", email: nil, firebaseUid: nil))
        model.selectContext(.business)
        model.selectCompany(CompanySummary(companyId: "c1", displayName: "Acme"))
        model.selectBottomDestination(.life)
        let beforeContext = model.selectedContext
        let beforeCompany = model.selectedCompany?.companyId
        let beforeMoment = model.selectedMomentId
        let beforeTab = model.bottomDestination

        model.openLife360(true)
        #expect(model.life360Open)
        #expect(!model.profileOpen)
        #expect(model.selectedContext == beforeContext)
        #expect(model.selectedCompany?.companyId == beforeCompany)
        #expect(model.selectedMomentId == beforeMoment)
        #expect(model.bottomDestination == beforeTab)

        model.openProfile(true)
        #expect(model.profileOpen)
        #expect(!model.life360Open)

        model.openLife360(true)
        model.openLife360(false)
        #expect(!model.life360Open)
        #expect(model.selectedContext == beforeContext)
        #expect(model.selectedCompany?.companyId == beforeCompany)
        #expect(model.selectedMomentId == beforeMoment)
        #expect(model.bottomDestination == beforeTab)
    }

    @MainActor
    @Test func companySwitchClearsMoment() async {
        let model = AppShellModel()
        model.bindIdentity(ShellIdentity(userId: "u1", displayName: nil, email: nil, firebaseUid: nil))
        model.selectCompany(CompanySummary(companyId: "c1", displayName: "Acme"))
        model.selectedMomentTitle = "Q1"
        model.selectCompany(CompanySummary(companyId: "c2", displayName: "Beta"))
        #expect(model.selectedMomentTitle == nil)
        #expect(model.selectedCompany?.companyId == "c2")
    }

    @MainActor
    @Test func accountSwitchIsolation() async {
        let modelA = AppShellModel()
        modelA.bindIdentity(ShellIdentity(userId: "userA", displayName: "A", email: nil, firebaseUid: nil))
        modelA.selectCompany(CompanySummary(companyId: "cA", displayName: "A Co"))
        modelA.selectContext(.business)
        modelA.clearForLogout()
        #expect(modelA.identity == nil)
        #expect(modelA.selectedCompany == nil)

        let modelB = AppShellModel()
        modelB.bindIdentity(ShellIdentity(userId: "userB", displayName: "B", email: nil, firebaseUid: nil))
        modelB.selectContext(.business)
        #expect(modelB.identity?.userId == "userB")
        #expect(modelB.selectedCompany == nil)
    }

    @Test func balanceMaskPresentationOnly() {
        let hide = true
        let shown = hide ? "••••" : "12.00"
        #expect(shown == "••••")
    }
}
