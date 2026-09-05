import Foundation

enum APIErrorKind: Error, Equatable {
    case unauthenticated(String)
    case forbidden(String)
    case notFound(String)
    case conflict(String)
    case validation(String)
    case rateLimited(String)
    case server(String)
    case network(String)
    case unknown(String)

    static func from(status: Int, code: String?, message: String?) -> APIErrorKind {
        let detail = message?.trimmingCharacters(in: .whitespacesAndNewlines)
        let resolved = (detail?.isEmpty == false) ? detail! : (code ?? "Error")
        switch status {
        case 401: return .unauthenticated(resolved)
        case 403: return .forbidden(resolved)
        case 404: return .notFound(resolved)
        case 409: return .conflict(resolved)
        case 400, 422: return .validation(resolved)
        case 429: return .rateLimited(resolved)
        case 500 ... 599: return .server(resolved)
        default: return .unknown(message ?? "Unexpected status \(status)")
        }
    }
}

extension APIErrorKind: LocalizedError {
    var errorDescription: String? {
        switch self {
        case .unauthenticated(let message): return message
        case .forbidden(let message): return message
        case .notFound(let message): return message
        case .conflict(let message): return message
        case .validation(let message): return message
        case .rateLimited(let message): return message
        case .server(let message): return message
        case .network(let message): return message
        case .unknown(let message): return message
        }
    }
}

protocol ShellMeGatewaying {
    func getMe() async throws -> ShellIdentity
    func getBootstrap() async throws -> ShellBootstrap
    func cachedBootstrap(userId: String) -> ShellBootstrap?
    func isBootstrapCacheFresh(userId: String, maxAgeMs: TimeInterval) -> Bool
    func clearBootstrapCache(userId: String?)
    func listCompanies() async throws -> [CompanySummary]
    func groupMomentCount() async throws -> Int
    func listPersonalMoments(limit: Int) async throws -> [MomentSummary]
    func listGroupMoments(limit: Int) async throws -> [MomentSummary]
    func listBusinessMoments(limit: Int) async throws -> [MomentSummary]
    func hasLife360() async throws -> Bool
}

struct ShellMeGateway: ShellMeGatewaying {
    private let client: APIClient
    private let decoder = JSONDecoder()

    init(client: APIClient = .shared) {
        self.client = client
    }

    func getMe() async throws -> ShellIdentity {
        try await getBootstrap().identity
    }

    func getBootstrap() async throws -> ShellBootstrap {
        let me = try await client.bootstrapMe()
        if let raw = try? JSONEncoder().encode(BootstrapCacheEnvelope(from: me)) {
            BootstrapCacheStore.save(userId: me.userId, data: raw)
        }
        return me.toShellBootstrap()
    }

    func cachedBootstrap(userId: String) -> ShellBootstrap? {
        guard let data = BootstrapCacheStore.loadData(userId: userId),
              let envelope = try? decoder.decode(BootstrapCacheEnvelope.self, from: data)
        else { return nil }
        return envelope.toShellBootstrap()
    }

    func isBootstrapCacheFresh(userId: String, maxAgeMs: TimeInterval = 30_000) -> Bool {
        BootstrapCacheStore.isFresh(userId: userId, maxAgeMs: maxAgeMs)
    }

    func clearBootstrapCache(userId: String?) {
        BootstrapCacheStore.clear(userId: userId)
    }

    func listCompanies() async throws -> [CompanySummary] {
        try await client.listCompanies()
    }

    func groupMomentCount() async throws -> Int {
        try await client.groupMomentPreviewCount()
    }

    func listPersonalMoments(limit: Int = 20) async throws -> [MomentSummary] {
        try await client.listPersonalMoments(limit: limit)
    }

    func listGroupMoments(limit: Int = 20) async throws -> [MomentSummary] {
        try await client.listGroupMoments(limit: limit)
    }

    func listBusinessMoments(limit: Int = 20) async throws -> [MomentSummary] {
        try await client.listBusinessMoments(limit: limit)
    }

    /// S5: Life360 entry is always available as Coming Soon — do not call GET /v1/life360.
    func hasLife360() async throws -> Bool {
        true
    }
}

/// Codable snapshot for SWR — avoids encoding MeBootstrap's AnyDecodable featureFlags.
private struct BootstrapCacheEnvelope: Codable {
    let userId: String
    let displayName: String?
    let email: String?
    let firebaseUid: String?
    let timezone: String?
    let locale: String?
    let roles: [String]
    let capabilities: [String]
    let supportedContexts: [String]
    let currentlySelectedContext: String
    let personal: [CachedMoment]
    let group: [CachedMoment]
    let business: [CachedMoment]
    let companies: [CachedCompany]
    let selectedCompanyId: String?

    struct CachedMoment: Codable {
        let momentId: String
        let title: String
        let status: String
        let momentTypeCode: String?
        let companyId: String?
    }

    struct CachedCompany: Codable {
        let companyId: String
        let displayName: String
    }

    init(from me: MeBootstrap) {
        userId = me.userId
        displayName = me.displayName
        email = me.email
        firebaseUid = me.firebaseUid
        timezone = me.timezone ?? me.preferences?.timezone
        locale = me.locale ?? me.preferences?.locale
        roles = me.roles ?? []
        capabilities = me.capabilities ?? []
        supportedContexts = me.supportedContexts ?? ["PERSONAL", "GROUP", "BUSINESS", "CIRCLE"]
        currentlySelectedContext = me.currentlySelectedContext ?? "PERSONAL"
        personal = (me.activeMoments?.personal ?? []).map {
            CachedMoment(momentId: $0.momentId, title: $0.title, status: $0.status, momentTypeCode: $0.momentTypeCode, companyId: $0.companyId)
        }
        group = (me.activeMoments?.group ?? []).map {
            CachedMoment(momentId: $0.momentId, title: $0.title, status: $0.status, momentTypeCode: $0.momentTypeCode, companyId: nil)
        }
        business = (me.activeMoments?.business ?? []).map {
            CachedMoment(momentId: $0.momentId, title: $0.title, status: $0.status, momentTypeCode: $0.momentTypeCode, companyId: $0.companyId)
        }
        companies = (me.companies ?? []).map {
            CachedCompany(companyId: $0.companyId, displayName: $0.displayName)
        }
        selectedCompanyId = me.selectedCompany?.companyId ?? me.companies?.first?.companyId
    }

    func toShellBootstrap() -> ShellBootstrap {
        let companyModels = companies.map { CompanySummary(companyId: $0.companyId, displayName: $0.displayName) }
        return ShellBootstrap(
            identity: ShellIdentity(userId: userId, displayName: displayName, email: email, firebaseUid: firebaseUid),
            supportedContexts: supportedContexts.compactMap { AppContextKind(rawValue: $0) },
            currentlySelectedContext: AppContextKind(rawValue: currentlySelectedContext) ?? .personal,
            personalMoments: personal.map {
                MomentSummary(momentId: $0.momentId, title: $0.title, status: $0.status, momentTypeCode: $0.momentTypeCode, companyId: $0.companyId)
            },
            groupMoments: group.map {
                MomentSummary(momentId: $0.momentId, title: $0.title, status: $0.status, momentTypeCode: $0.momentTypeCode)
            },
            businessMoments: business.map {
                MomentSummary(momentId: $0.momentId, title: $0.title, status: $0.status, momentTypeCode: $0.momentTypeCode, companyId: $0.companyId)
            },
            companies: companyModels,
            selectedCompany: companyModels.first { $0.companyId == selectedCompanyId } ?? companyModels.first,
            capabilities: capabilities,
            roles: roles,
            preferencesTimezone: timezone ?? "UTC",
            preferencesLocale: locale
        )
    }
}
