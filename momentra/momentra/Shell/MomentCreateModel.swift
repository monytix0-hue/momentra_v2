import Combine
import Foundation

@MainActor
final class MomentCreateModel: ObservableObject {
    struct UiState: Equatable {
        var submitting = false
        var error: String?
    }

    @Published private(set) var state = UiState()

    private let repository: MomentCreateGateway

    init(repository: MomentCreateGateway = MomentCreateRepository()) {
        self.repository = repository
    }

    func clearError() {
        state.error = nil
    }

    func submitPersonalSetup(
        kind: PersonalSetupKind,
        preferences: [String: Any],
        title: String? = nil,
        editingMomentId: String? = nil,
        editingMomentStatus: String? = nil,
        status: String? = nil,
        onSuccess: @escaping (CreateMomentOutcome) -> Void
    ) {
        let catalog = PersonalSetupCatalog.forKind(kind)
        let allowedKeys = Set(catalog.defaultPreferences.keys)
        let filtered = SetupPreferenceFilter.filterToCatalogKeys(preferences, allowedKeys: allowedKeys)
        let resolvedTitle = title ?? catalog.defaultTitle
        if let editingMomentId {
            submitPersonalEdit(
                momentId: editingMomentId,
                title: resolvedTitle,
                momentTypeCode: catalog.momentTypeCode,
                preferences: filtered,
                activateAfter: status == "ACTIVE" && editingMomentStatus?.caseInsensitiveCompare("DRAFT") == .orderedSame,
                onSuccess: onSuccess
            )
            return
        }
        submit(
            draftKey: "personal:\(kind.rawValue)",
            block: { draftKey in
                try await self.repository.createPersonalSetup(
                    draftKey: draftKey,
                    systemCode: kind.rawValue,
                    title: resolvedTitle,
                    description: catalog.subtitle,
                    momentTypeCode: catalog.momentTypeCode,
                    preferences: filtered,
                    status: status
                )
            },
            onSuccess: onSuccess
        )
    }

    func submitBusinessSetup(
        kind: BusinessSetupKind,
        companyId: String,
        preferences: [String: Any],
        title: String? = nil,
        editingMomentId: String? = nil,
        editingMomentStatus: String? = nil,
        status: String? = nil,
        onSuccess: @escaping (CreateMomentOutcome) -> Void
    ) {
        let catalog = BusinessSetupCatalog.forKind(kind)
        let allowedKeys = Set(catalog.defaultPreferences.keys)
        let filtered = SetupPreferenceFilter.filterToCatalogKeys(preferences, allowedKeys: allowedKeys)
        let resolvedTitle = title ?? catalog.defaultTitle
        if let editingMomentId {
            submitEdit(
                momentId: editingMomentId,
                title: resolvedTitle,
                momentTypeCode: catalog.momentTypeCode,
                activateAfter: status == "ACTIVE" && editingMomentStatus?.caseInsensitiveCompare("DRAFT") == .orderedSame,
                onSuccess: onSuccess
            )
            return
        }
        submit(
            draftKey: "business:\(kind.rawValue):\(companyId)",
            block: { draftKey in
                try await self.repository.createBusinessSetup(
                    draftKey: draftKey,
                    companyId: companyId,
                    familyCode: kind.rawValue,
                    title: resolvedTitle,
                    description: catalog.subtitle,
                    momentTypeCode: catalog.momentTypeCode,
                    preferences: filtered,
                    status: status
                )
            },
            onSuccess: onSuccess
        )
    }

    func submitGroupMoment(
        section: String,
        momentTypeCode: String,
        title: String,
        description: String?,
        startAt: String?,
        endAt: String?,
        participants: [CreateMomentParticipantInput],
        inviteCode: String? = nil,
        groupSetup: CreateMomentRequest.GroupSetupBlock? = nil,
        editingMomentId: String? = nil,
        editingMomentStatus: String? = nil,
        status: String? = nil,
        onSuccess: @escaping (CreateMomentOutcome) -> Void
    ) {
        let resolved = Self.resolveGroupTypeForApi(section: section, momentTypeCode: momentTypeCode, title: title)
        if let editingMomentId {
            submitGroupEdit(
                momentId: editingMomentId,
                title: title,
                momentTypeCode: resolved.momentTypeCode,
                groupSetup: groupSetup,
                activateAfter: status == "ACTIVE" && editingMomentStatus?.caseInsensitiveCompare("DRAFT") == .orderedSame,
                onSuccess: onSuccess
            )
            return
        }
        submit(
            draftKey: "group:\(section):\(resolved.momentTypeCode):\(title)",
            block: { draftKey in
                try await self.repository.createGroupMoment(
                    draftKey: draftKey,
                    momentTypeCode: resolved.momentTypeCode,
                    title: title,
                    description: description,
                    startAt: startAt,
                    endAt: endAt,
                    participants: participants,
                    inviteCode: inviteCode,
                    customTypeLabel: resolved.customTypeLabel,
                    groupSetup: groupSetup,
                    status: status
                )
            },
            onSuccess: onSuccess
        )
    }

    func discardMomentDraft(momentId: String, onSuccess: @escaping () -> Void) {
        guard !state.submitting else { return }
        state = UiState(submitting: true, error: nil)
        Task {
            do {
                _ = try await APIClient.shared.discardMomentDraft(momentId: momentId)
                state = UiState(submitting: false, error: nil)
                onSuccess()
            } catch {
                state = UiState(
                    submitting: false,
                    error: (error as? APIErrorKind).map(Self.message(for:)) ?? error.localizedDescription
                )
            }
        }
    }

    func getGroupSetupPrefill(momentId: String) async -> GroupSetupPrefill? {
        try? await APIClient.shared.getGroupSetupPrefill(momentId: momentId)
    }

    func getDomainSetupPrefill(momentId: String) async -> DomainSetupPrefill? {
        try? await APIClient.shared.getDomainSetupPrefill(momentId: momentId)
    }

    private func submitGroupEdit(
        momentId: String,
        title: String,
        momentTypeCode: String,
        groupSetup: CreateMomentRequest.GroupSetupBlock?,
        activateAfter: Bool,
        onSuccess: @escaping (CreateMomentOutcome) -> Void
    ) {
        guard !state.submitting else { return }
        state = UiState(submitting: true, error: nil)
        Task {
            do {
                let detail = try await APIClient.shared.getMomentDetail(momentId: momentId)
                let result = try await APIClient.shared.updateMoment(
                    momentId: momentId,
                    title: title,
                    expectedVersion: detail.version
                )
                if let setup = groupSetup {
                    let primary = setup.budgets?.first(where: { $0.isPrimary == true })
                        ?? setup.budgets?.first
                    let amount = primary?.amount ?? setup.budgetAmount
                    let currency = primary?.currencyCode ?? setup.budgetCurrencyCode
                    if let amount, let currency {
                        _ = try? await APIClient.shared.patchGroupBudget(
                            momentId: momentId,
                            budgetAmount: amount,
                            budgetCurrencyCode: currency
                        )
                    }
                    if let budgets = setup.budgets, budgets.count > 1 {
                        for budget in budgets.dropFirst() {
                            _ = try? await APIClient.shared.patchGroupBudget(
                                momentId: momentId,
                                budgetAmount: budget.amount,
                                budgetCurrencyCode: budget.currencyCode
                            )
                        }
                    }
                    if let reminders = setup.reminderPreferences {
                        _ = try? await APIClient.shared.patchMomentNotificationPreferences(
                            momentId: momentId,
                            reminderPreferences: reminders
                        )
                    }
                }
                if activateAfter {
                    let activated = try await APIClient.shared.activateMoment(momentId: momentId)
                    let outcome = CreateMomentOutcome(
                        momentId: activated.momentId,
                        title: activated.title,
                        domainCode: activated.domainCode,
                        status: activated.status,
                        version: activated.version,
                        momentTypeCode: momentTypeCode,
                        setupId: activated.setupId,
                        projectionHints: []
                    )
                    state = UiState(submitting: false, error: nil)
                    onSuccess(outcome)
                } else {
                    let outcome = CreateMomentOutcome(
                        momentId: result.momentId,
                        title: result.title,
                        domainCode: result.domainCode,
                        status: result.status,
                        version: result.version,
                        momentTypeCode: momentTypeCode,
                        setupId: nil,
                        projectionHints: []
                    )
                    state = UiState(submitting: false, error: nil)
                    onSuccess(outcome)
                }
            } catch {
                state = UiState(
                    submitting: false,
                    error: (error as? APIErrorKind).map(Self.message(for:)) ?? error.localizedDescription
                )
            }
        }
    }

    /// Catalog may use `CUSTOM` for purchase/living — map to seeded taxonomy + customTypeLabel.
    static func resolveGroupTypeForApi(
        section: String,
        momentTypeCode: String,
        title: String
    ) -> (momentTypeCode: String, customTypeLabel: String?) {
        guard momentTypeCode == "CUSTOM" else { return (momentTypeCode, nil) }
        if section == "living" {
            return ("COMMUNITY_LIVING", title)
        }
        return ("COMMUNITY_PURCHASE", title)
    }

    func mintGroupInvite(title: String, momentTypeCode: String, section: String = "") async -> GroupInvite? {
        let resolved = Self.resolveGroupTypeForApi(section: section, momentTypeCode: momentTypeCode, title: title)
        return try? await repository.mintGroupInvite(
            draftKey: "group-invite:\(resolved.momentTypeCode)",
            title: title,
            momentTypeCode: resolved.momentTypeCode
        )
    }

    func redeemGroupInvite(code: String) async -> RedeemGroupInviteResult? {
        try? await repository.redeemGroupInvite(code: code)
    }

    func previewGroupInvite(code: String) async -> GroupInvite? {
        try? await repository.previewGroupInvite(code: code)
    }

    func previewCompanyInvite(code: String) async -> CompanyInvite? {
        try? await repository.previewCompanyInvite(code: code)
    }

    func mintCompanyInvite(companyId: String, membershipType: String = "MEMBER") async -> CompanyInvite? {
        try? await repository.mintCompanyInvite(companyId: companyId, membershipType: membershipType)
    }

    func redeemCompanyInvite(code: String) async -> RedeemCompanyInviteResult? {
        try? await repository.redeemCompanyInvite(code: code)
    }

    func listCompanies() async -> [CompanySummary] {
        (try? await repository.listCompanies()) ?? []
    }

    private func submitPersonalEdit(
        momentId: String,
        title: String,
        momentTypeCode: String,
        preferences: [String: Any],
        activateAfter: Bool,
        onSuccess: @escaping (CreateMomentOutcome) -> Void
    ) {
        guard !state.submitting else { return }
        state = UiState(submitting: true, error: nil)
        Task {
            do {
                guard let setup = try await APIClient.shared.personalSetup(forMomentId: momentId),
                      let setupVersion = setup.version
                else {
                    throw APIErrorKind.notFound("Personal setup not found for this moment.")
                }
                _ = try await APIClient.shared.patchPersonalSetup(
                    setupId: setup.setupId,
                    expectedVersion: setupVersion,
                    title: title,
                    preferences: preferences
                )
                let detail = try await APIClient.shared.getMomentDetail(momentId: momentId)
                let result = try await APIClient.shared.updateMoment(
                    momentId: momentId,
                    title: title,
                    expectedVersion: detail.version
                )
                if activateAfter {
                    let activated = try await APIClient.shared.activateMoment(momentId: momentId)
                    let outcome = CreateMomentOutcome(
                        momentId: activated.momentId,
                        title: activated.title,
                        domainCode: activated.domainCode,
                        status: activated.status,
                        version: activated.version,
                        momentTypeCode: momentTypeCode,
                        setupId: setup.setupId,
                        projectionHints: []
                    )
                    state = UiState(submitting: false, error: nil)
                    onSuccess(outcome)
                } else {
                    let outcome = CreateMomentOutcome(
                        momentId: result.momentId,
                        title: result.title,
                        domainCode: result.domainCode,
                        status: result.status,
                        version: result.version,
                        momentTypeCode: momentTypeCode,
                        setupId: setup.setupId,
                        projectionHints: []
                    )
                    state = UiState(submitting: false, error: nil)
                    onSuccess(outcome)
                }
            } catch {
                state = UiState(
                    submitting: false,
                    error: (error as? APIErrorKind).map(Self.message(for:)) ?? error.localizedDescription
                )
            }
        }
    }

    private func submitEdit(
        momentId: String,
        title: String,
        momentTypeCode: String,
        activateAfter: Bool,
        onSuccess: @escaping (CreateMomentOutcome) -> Void
    ) {
        guard !state.submitting else { return }
        state = UiState(submitting: true, error: nil)
        Task {
            do {
                let detail = try await APIClient.shared.getMomentDetail(momentId: momentId)
                let result = try await APIClient.shared.updateMoment(
                    momentId: momentId,
                    title: title,
                    expectedVersion: detail.version
                )
                if activateAfter {
                    let activated = try await APIClient.shared.activateMoment(momentId: momentId)
                    let outcome = CreateMomentOutcome(
                        momentId: activated.momentId,
                        title: activated.title,
                        domainCode: activated.domainCode,
                        status: activated.status,
                        version: activated.version,
                        momentTypeCode: momentTypeCode,
                        setupId: activated.setupId,
                        projectionHints: []
                    )
                    state = UiState(submitting: false, error: nil)
                    onSuccess(outcome)
                } else {
                    let outcome = CreateMomentOutcome(
                        momentId: result.momentId,
                        title: result.title,
                        domainCode: result.domainCode,
                        status: result.status,
                        version: result.version,
                        momentTypeCode: momentTypeCode,
                        setupId: nil,
                        projectionHints: []
                    )
                    state = UiState(submitting: false, error: nil)
                    onSuccess(outcome)
                }
            } catch {
                state = UiState(
                    submitting: false,
                    error: (error as? APIErrorKind).map(Self.message(for:)) ?? error.localizedDescription
                )
            }
        }
    }

    private func submit(
        draftKey: String,
        block: @escaping (String) async throws -> CreateMomentOutcome,
        onSuccess: @escaping (CreateMomentOutcome) -> Void
    ) {
        guard !state.submitting else { return }
        state = UiState(submitting: true, error: nil)
        Task {
            do {
                let outcome = try await block(draftKey)
                state = UiState(submitting: false, error: nil)
                onSuccess(outcome)
            } catch {
                state = UiState(
                    submitting: false,
                    error: (error as? APIErrorKind).map(Self.message(for:)) ?? error.localizedDescription
                )
            }
        }
    }

    private static func message(for error: APIErrorKind) -> String {
        switch error {
        case .validation(let code), .conflict(let code), .notFound(let code),
             .rateLimited(let code), .server(let code), .unknown(let code):
            return code
        case .network(let message):
            return message
        case .unauthenticated(let code), .forbidden(let code):
            return code
        }
    }
}
