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
        companyId: String?,
        preferences: [String: Any],
        title: String? = nil,
        subtype: String? = nil,
        editingMomentId: String? = nil,
        editingMomentStatus: String? = nil,
        status: String? = nil,
        companyName: String? = nil,
        taxIdentifier: String? = nil,
        onSuccess: @escaping (CreateMomentOutcome) -> Void
    ) {
        let catalog = BusinessSetupCatalog.forKind(kind)
        let allowedKeys = catalog.allowedKeys
        let filtered = SetupPreferenceFilter.filterToCatalogKeys(preferences, allowedKeys: allowedKeys)
        let resolvedTitle = title ?? catalog.defaultTitle
        let momentTypeCode = subtype ?? catalog.momentTypeCode
        if let editingMomentId {
            guard !state.submitting else { return }
            state = UiState(submitting: true, error: nil)
            Task {
                do {
                    _ = try await ensureBusinessCompany(
                        companyId: companyId,
                        companyName: companyName,
                        taxIdentifier: taxIdentifier,
                        editingMomentId: editingMomentId
                    )
                    // The document cancels its debounce when §06 is pressed, so the last edits are
                    // written here before the status flips — activation must not promote a draft
                    // that is one keystroke behind the screen. An already-ACTIVE moment has no
                    // draft row to patch, so this only runs for a DRAFT.
                    if !filtered.isEmpty,
                       editingMomentStatus?.caseInsensitiveCompare("DRAFT") == .orderedSame {
                        _ = try? await APIClient.shared.patchBusinessSetupDraft(
                            momentId: editingMomentId,
                            title: resolvedTitle,
                            preferences: filtered
                        )
                    }
                    let detail = try await APIClient.shared.getMomentDetail(momentId: editingMomentId)
                    let result = try await APIClient.shared.updateMoment(
                        momentId: editingMomentId,
                        title: resolvedTitle,
                        expectedVersion: detail.version
                    )
                    let activateAfter = status == "ACTIVE" && editingMomentStatus?.caseInsensitiveCompare("DRAFT") == .orderedSame
                    if activateAfter {
                        let activated = try await APIClient.shared.activateMoment(momentId: editingMomentId)
                        state = UiState(submitting: false, error: nil)
                        onSuccess(CreateMomentOutcome(
                            momentId: activated.momentId,
                            title: activated.title,
                            domainCode: activated.domainCode,
                            status: activated.status,
                            version: activated.version,
                            momentTypeCode: momentTypeCode,
                            setupId: activated.setupId,
                            projectionHints: []
                        ))
                    } else {
                        state = UiState(submitting: false, error: nil)
                        onSuccess(CreateMomentOutcome(
                            momentId: result.momentId,
                            title: result.title,
                            domainCode: result.domainCode,
                            status: result.status,
                            version: result.version,
                            momentTypeCode: momentTypeCode,
                            setupId: nil,
                            projectionHints: []
                        ))
                    }
                } catch {
                    state = UiState(
                        submitting: false,
                        error: (error as? APIErrorKind).map(Self.message(for:)) ?? error.localizedDescription
                    )
                }
            }
            return
        }
        submit(
            draftKey: "business:\(kind.rawValue):\(companyId ?? "moment")",
            block: { draftKey in
                let resolvedCompanyId = try await self.ensureBusinessCompany(
                    companyId: companyId,
                    companyName: companyName,
                    taxIdentifier: taxIdentifier,
                    editingMomentId: nil
                )
                let outcome = try await self.repository.createBusinessSetup(
                    draftKey: draftKey,
                    companyId: resolvedCompanyId,
                    familyCode: catalog.familyCode,
                    title: resolvedTitle,
                    description: catalog.subtitle,
                    momentTypeCode: momentTypeCode,
                    preferences: filtered,
                    status: status
                )
                return CreateMomentOutcome(
                    momentId: outcome.momentId,
                    title: outcome.title,
                    domainCode: outcome.domainCode,
                    status: outcome.status,
                    version: outcome.version,
                    momentTypeCode: outcome.momentTypeCode ?? momentTypeCode,
                    setupId: outcome.setupId,
                    projectionHints: outcome.projectionHints
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
                startAt: startAt,
                endAt: endAt,
                customTypeLabel: resolved.customTypeLabel,
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

    /// Persist the setup document into its draft. Deliberately silent: a failed intermediate save
    /// must never blank the document or raise an error the operator cannot act on — §06's
    /// activation reports a real failure.
    func saveBusinessSetupDraft(momentId: String, title: String?, preferences: [String: Any]) async {
        _ = try? await APIClient.shared.patchBusinessSetupDraft(
            momentId: momentId,
            title: title,
            preferences: preferences
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

    private func ensureBusinessCompany(
        companyId: String?,
        companyName: String?,
        taxIdentifier: String?,
        editingMomentId: String?
    ) async throws -> String? {
        let name = companyName?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        guard !name.isEmpty else {
            let existing = companyId?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
            return existing.isEmpty ? nil : existing
        }
        let gst = taxIdentifier?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let existing = companyId?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        if existing.isEmpty {
            let created = try await APIClient.shared.createCompany(
                displayName: name,
                legalName: name,
                taxIdentifier: gst.isEmpty ? nil : gst
            )
            if let editingMomentId {
                try await APIClient.shared.linkBusinessMomentCompany(momentId: editingMomentId, companyId: created.companyId)
            }
            return created.companyId
        }
        let detail = try await APIClient.shared.getCompany(companyId: existing)
        _ = try await APIClient.shared.patchCompany(
            companyId: existing,
            body: [
                "displayName": name,
                "legalName": name,
                "taxIdentifier": gst,
                "expectedVersion": detail.version,
            ]
        )
        return existing
    }

    func getDomainSetupPrefill(momentId: String) async -> DomainSetupPrefill? {
        try? await APIClient.shared.getDomainSetupPrefill(momentId: momentId)
    }

    /// The company §04's masters sheets write into.
    ///
    /// A setup draft is created before §01 knows a company, and masters are company-scoped, so the
    /// document creates one the first time a §04 row is opened and links it to the draft. Doing it
    /// here rather than on open means a document nobody takes to §04 leaves no company behind.
    func ensureSetupCompany(
        momentId: String,
        companyName: String,
        taxIdentifier: String? = nil
    ) async -> String? {
        let name = companyName.trimmingCharacters(in: .whitespacesAndNewlines)
        return try? await ensureBusinessCompany(
            companyId: nil,
            companyName: name.isEmpty ? "New business" : name,
            taxIdentifier: taxIdentifier,
            editingMomentId: momentId
        )
    }

    private func submitGroupEdit(
        momentId: String,
        title: String,
        momentTypeCode: String,
        startAt: String?,
        endAt: String?,
        customTypeLabel: String?,
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
                    expectedVersion: detail.version,
                    startAt: startAt,
                    endAt: endAt,
                    customTypeLabel: customTypeLabel,
                    groupSetup: groupSetup
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

    func redeemGroupInvite(code: String) async throws -> RedeemGroupInviteResult {
        try await repository.redeemGroupInvite(code: code)
    }

    func previewGroupInvite(code: String) async -> GroupInvite? {
        try? await repository.previewGroupInvite(code: code)
    }

    func previewCompanyInvite(code: String) async -> CompanyInvite? {
        try? await repository.previewCompanyInvite(code: code)
    }

    func mintCompanyInvite(companyId: String, membershipType: String = "MEMBER", operatingRole: String? = nil) async -> CompanyInvite? {
        try? await repository.mintCompanyInvite(companyId: companyId, membershipType: membershipType, operatingRole: operatingRole)
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
