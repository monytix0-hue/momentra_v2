import Foundation
import FirebaseAuth

struct MeBootstrap: Decodable {
    let userId: String
    let displayName: String?
    let email: String?
    let firebaseUid: String?
    let timezone: String?
    let locale: String?
    let status: String?
    let roles: [String]?
    let permissions: [String]?
    let capabilities: [String]?
    let supportedContexts: [String]?
    let currentlySelectedContext: String?
    let activeMoments: BootstrapMomentsPayload?
    let companies: [CompanyItemPayload]?
    let selectedCompany: CompanyItemPayload?
    let preferences: BootstrapPreferencesPayload?
    let featureFlags: [String: AnyDecodable]?
}

struct BootstrapMomentsPayload: Decodable {
    let personal: [BootstrapMomentPayload]?
    let group: [BootstrapMomentPayload]?
    let business: [BootstrapMomentPayload]?
}

struct BootstrapMomentPayload: Decodable {
    let momentId: String
    let title: String
    let status: String
    let momentTypeCode: String?
    let domainCode: String?
    let companyId: String?
}

struct BootstrapPreferencesPayload: Decodable {
    let timezone: String?
    let locale: String?
}

struct PatchMePayload: Decodable {
    let userId: String
    let displayName: String?
    let timezone: String?
    let locale: String?
}

struct SoftDeletePayload: Decodable {
    let userId: String
    let status: String
}

struct DeviceListPayload: Decodable {
    let items: [DeviceItemPayload]
}

struct DeviceItemPayload: Decodable {
    let deviceId: String
    let userDeviceId: String?
    let platform: String?
    let appVersion: String?
    let lastSeenAt: String?
    let revoked: Bool?
}

struct ConsentListPayload: Decodable {
    let purposes: [ConsentPurposePayload]
}

struct ConsentPurposePayload: Decodable {
    let code: String
    let displayName: String?
    let description: String?
    let status: String?
    let granted: Bool?
    let consentId: String?
    let grantedAt: String?
}

struct ConsentMutationPayload: Decodable {
    let consentId: String?
    let purposeCode: String?
    let status: String?
}

struct RegisterDevicePayload: Decodable {
    let deviceId: String?
    let userId: String?
    let platform: String?
    let status: String?
}

struct RevokeDevicePayload: Decodable {
    let deviceId: String?
    let userId: String?
    let status: String?
}

struct AnalyticsRefreshPayload: Decodable {
    let metricsWritten: Int?
    let narrative: Bool?
    let skippedReason: String?
}

struct AnalyticsInsightItemPayload: Decodable {
    let insightId: String
    let source: String?
    let insightCode: String?
    let title: String?
    let body: String?
    let computedAt: String?
    let dataThrough: String?
    let status: String?
    let version: String?
}

struct AnalyticsInsightsMetaPayload: Decodable {
    let contractVersion: String?
    let status: String?
    let computedAt: String?
    let dataThrough: String?
    let version: String?
}

struct AnalyticsInsightsPayload: Decodable {
    let items: [AnalyticsInsightItemPayload]
    let meta: AnalyticsInsightsMetaPayload?
}

struct AnalyticsMetricItemPayload: Decodable, Identifiable {
    var id: String { metricCode ?? UUID().uuidString }
    let metricCode: String?
    let label: String?
    let value: Double?
    let unitCode: String?
    let computedAt: String?
    let status: String?
}

struct AnalyticsMetricsPayload: Decodable {
    let items: [AnalyticsMetricItemPayload]
}

private struct SuccessEnvelope<T: Decodable>: Decodable {
    let data: T
    let correlationId: String
}

private struct SuccessEnvelopeWithHints<T: Decodable>: Decodable {
    let data: T
    let correlationId: String
    let projectionHints: [ProjectionHintPayload]?
}

private struct CompanyListPayload: Decodable {
    let items: [CompanyItemPayload]
}

struct CompanyItemPayload: Decodable {
    let companyId: String
    let displayName: String
}

private struct CursorPagePayload<T: Decodable>: Decodable {
    let items: [T]
}

private struct GroupMomentItemPayload: Decodable {
    let momentId: String?
    let title: String?
    let status: String?
}

private struct PersonalMomentItemPayload: Decodable {
    let momentId: String
    let title: String
    let status: String
    let momentTypeCode: String?
}

private struct BusinessMomentItemPayload: Decodable {
    let momentId: String
    let title: String
    let status: String
}

private struct ProjectionEnvelopePayload: Decodable {
    // Projection shape varies; presence of a successful envelope is enough for shell.
}

private struct APIErrorBody: Decodable {
    let code: String?
    let message: String?
}

enum APIConfig {
    /// Resolution order:
    /// 1. Scheme env `MOMENTRA_API_BASE_URL`
    /// 2. Info.plist `MomentraAPIBaseURL` (match Android `local.properties` API_BASE_URL)
    /// 3. Simulator loopback fallback
    ///
    /// Physical devices and cross-machine backends must use the host LAN IP
    /// (e.g. `http://192.168.29.112:3000/`), never `127.0.0.1`.
    static var baseURL: URL {
        if let env = ProcessInfo.processInfo.environment["MOMENTRA_API_BASE_URL"]?
            .trimmingCharacters(in: .whitespacesAndNewlines),
           !env.isEmpty,
           let url = Self.normalizedBaseURL(env) {
            return url
        }
        if let plist = Bundle.main.object(forInfoDictionaryKey: "MomentraAPIBaseURL") as? String,
           let url = Self.normalizedBaseURL(plist) {
            return url
        }
#if targetEnvironment(simulator)
        return URL(string: "http://127.0.0.1:3000/")!
#else
        return URL(string: "http://127.0.0.1:3000/")!
#endif
    }

    static var baseURLDescription: String { baseURL.absoluteString }

    private static func normalizedBaseURL(_ raw: String) -> URL? {
        let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return nil }
        let withSlash = trimmed.hasSuffix("/") ? trimmed : trimmed + "/"
        return URL(string: withSlash)
    }
}

/// Caches Firebase ID tokens so every API call does not await getIDToken().
private actor AuthTokenCache {
    static let shared = AuthTokenCache()
    private var token: String?
    private var fetchedAt: Date?
    private let ttl: TimeInterval = 55 * 60

    func get(forceRefresh: Bool = false) async throws -> String {
        if !forceRefresh,
           let token,
           let fetchedAt,
           Date().timeIntervalSince(fetchedAt) < ttl {
            return token
        }
        guard let user = Auth.auth().currentUser else {
            throw APIErrorKind.unauthenticated("UNAUTHORIZED")
        }
        let fresh = try await user.getIDToken(forcingRefresh: forceRefresh)
        token = fresh
        fetchedAt = Date()
        return fresh
    }

    func clear() {
        token = nil
        fetchedAt = nil
    }
}

final class APIClient {
    static let shared = APIClient()
    private let decoder = JSONDecoder()
    private let session: URLSession = {
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 20
        config.timeoutIntervalForResource = 30
        config.waitsForConnectivity = false
        config.httpMaximumConnectionsPerHost = 8
        return URLSession(configuration: config)
    }()

    private init() {}

    func warmAuthToken() {
        Task { _ = try? await AuthTokenCache.shared.get() }
    }

    func clearAuthTokenCache() {
        Task { await AuthTokenCache.shared.clear() }
    }

    func bootstrapMe() async throws -> MeBootstrap {
        try await authorizedGet(path: "v1/me")
    }

    @discardableResult
    func patchMe(displayName: String? = nil, timezone: String? = nil, locale: String? = nil) async throws -> PatchMePayload {
        struct Body: Encodable {
            let displayName: String?
            let timezone: String?
            let locale: String?
        }
        return try await authorizedPatch(
            path: "v1/me",
            body: Body(displayName: displayName, timezone: timezone, locale: locale)
        )
    }

    @discardableResult
    func softDeleteMe() async throws -> SoftDeletePayload {
        try await authorizedDelete(path: "v1/me")
    }

    func listDevices() async throws -> DeviceListPayload {
        try await authorizedGet(path: "v1/me/devices")
    }

    func listConsents() async throws -> ConsentListPayload {
        try await authorizedGet(path: "v1/me/consents")
    }

    @discardableResult
    func grantConsent(purposeCode: String) async throws -> ConsentMutationPayload {
        struct Body: Encodable { let purposeCode: String; let scopeType = "USER" }
        return try await authorizedPost(
            path: "v1/me/consents/grant",
            body: Body(purposeCode: purposeCode),
            idempotencyKey: UUID().uuidString
        )
    }

    @discardableResult
    func withdrawConsent(purposeCode: String) async throws -> ConsentMutationPayload {
        struct Body: Encodable { let purposeCode: String; let scopeType = "USER" }
        return try await authorizedPost(
            path: "v1/me/consents/withdraw",
            body: Body(purposeCode: purposeCode),
            idempotencyKey: UUID().uuidString
        )
    }

    @discardableResult
    func registerDevice(deviceId: String, platform: String = "IOS", pushToken: String? = nil) async throws -> RegisterDevicePayload {
        struct Body: Encodable {
            let deviceId: String
            let platform: String
            let pushToken: String?
        }
        return try await authorizedPost(
            path: "v1/me/devices",
            body: Body(deviceId: deviceId, platform: platform, pushToken: pushToken),
            idempotencyKey: UUID().uuidString
        )
    }

    @discardableResult
    func revokeDevice(deviceId: String) async throws -> RevokeDevicePayload {
        try await authorizedDelete(path: "v1/me/devices/\(deviceId)")
    }

    func listAnalyticsInsights(scopeType: String = "USER", scopeId: String? = nil) async throws -> AnalyticsInsightsPayload {
        var query: [String: String] = ["scopeType": scopeType]
        if let scopeId { query["scopeId"] = scopeId }
        return try await authorizedGet(path: "v1/analytics/insights", query: query)
    }

    func listAnalyticsMetrics(scopeType: String = "USER", scopeId: String? = nil) async throws -> AnalyticsMetricsPayload {
        var query: [String: String] = ["scopeType": scopeType]
        if let scopeId { query["scopeId"] = scopeId }
        return try await authorizedGet(path: "v1/analytics/metrics", query: query)
    }

    @discardableResult
    func refreshAnalytics(context: String, companyId: String? = nil, momentId: String? = nil) async throws -> AnalyticsRefreshPayload {
        struct Body: Encodable {
            let context: String
            let companyId: String?
            let momentId: String?
        }
        return try await authorizedPost(
            path: "v1/analytics/refresh",
            body: Body(context: context, companyId: companyId, momentId: momentId),
            idempotencyKey: UUID().uuidString
        )
    }

    func listCompanies() async throws -> [CompanySummary] {
        let payload: CompanyListPayload = try await authorizedGet(path: "v1/companies")
        return payload.items.map {
            CompanySummary(companyId: $0.companyId, displayName: $0.displayName)
        }
    }

    struct CreateCompanyResult: Decodable {
        let companyId: String
        let displayName: String
        let version: Int
    }

    struct CreateLocationResult: Decodable {
        let locationId: String
        let companyId: String
        let name: String
    }

    @discardableResult
    func createCompany(
        displayName: String,
        legalName: String,
        timezone: String = "UTC",
        companyType: String? = nil,
        taxIdentifier: String? = nil,
        profileJson: [String: String]? = nil
    ) async throws -> CreateCompanyResult {
        struct Body: Encodable {
            let displayName: String
            let legalName: String
            let timezone: String
            let companyType: String?
            let taxIdentifier: String?
            let profileJson: [String: String]?
        }
        return try await authorizedPost(
            path: "v1/companies",
            body: Body(
                displayName: displayName,
                legalName: legalName,
                timezone: timezone,
                companyType: companyType,
                taxIdentifier: taxIdentifier,
                profileJson: profileJson
            ),
            idempotencyKey: UUID().uuidString
        )
    }

    @discardableResult
    func createLocation(
        companyId: String,
        name: String,
        addressText: String?,
        timezone: String?
    ) async throws -> CreateLocationResult {
        struct Body: Encodable {
            let name: String
            let addressText: String?
            let timezone: String?
        }
        return try await authorizedPost(
            path: "v1/companies/\(companyId)/locations",
            body: Body(name: name, addressText: addressText, timezone: timezone),
            idempotencyKey: UUID().uuidString
        )
    }

    struct CompanyLocationItemPayload: Decodable, Identifiable {
        var id: String { locationId }
        let locationId: String
        let name: String
        let addressText: String?
        let timezone: String?
        let status: String?
    }

    struct CompanyLocationListPayload: Decodable {
        let items: [CompanyLocationItemPayload]
    }

    func listCompanyLocations(companyId: String) async throws -> [CompanyLocationItemPayload] {
        let payload: CompanyLocationListPayload = try await authorizedGet(path: "v1/companies/\(companyId)/locations")
        return payload.items
    }

    func groupMomentPreviewCount() async throws -> Int {
        let page: CursorPagePayload<GroupMomentItemPayload> =
            try await authorizedGet(path: "v1/group/moments", query: ["limit": "1"])
        return page.items.count
    }

    func listPersonalMoments(limit: Int = 20) async throws -> [MomentSummary] {
        let page: CursorPagePayload<PersonalMomentItemPayload> =
            try await authorizedGet(path: "v1/personal/moments", query: ["limit": String(limit)])
        return page.items.map {
            MomentSummary(
                momentId: $0.momentId,
                title: $0.title,
                status: $0.status,
                momentTypeCode: $0.momentTypeCode
            )
        }
    }

    func listGroupMoments(limit: Int = 20) async throws -> [MomentSummary] {
        let page: CursorPagePayload<GroupMomentItemPayload> =
            try await authorizedGet(path: "v1/group/moments", query: ["limit": String(limit)])
        return page.items.compactMap { item in
            guard let id = item.momentId else { return nil }
            return MomentSummary(
                momentId: id,
                title: item.title ?? "Moment",
                status: item.status ?? "UNKNOWN"
            )
        }
    }

    func listBusinessMoments(limit: Int = 20) async throws -> [MomentSummary] {
        let page: CursorPagePayload<BusinessMomentItemPayload> =
            try await authorizedGet(path: "v1/business/moments", query: ["limit": String(limit)])
        return page.items.map {
            MomentSummary(momentId: $0.momentId, title: $0.title, status: $0.status)
        }
    }

    struct BusinessSetupCatalogItemPayload: Decodable {
        let familyCode: String
        let title: String
        let subtitle: String?
        let defaultMomentTypeCode: String?
        let activateLabel: String?
        let defaultTitle: String?
    }

    struct BusinessSetupItemPayload: Decodable, Identifiable {
        var id: String { setupId }
        let setupId: String
        let familyCode: String
        let title: String
        let momentId: String
        let companyId: String
        let status: String
        let preferences: [String: AnyDecodable]?
        let createdAt: String
    }

    struct BusinessSetupsPayload: Decodable {
        let catalog: [BusinessSetupCatalogItemPayload]
        let mine: [BusinessSetupItemPayload]
    }

    func getBusinessSetups() async throws -> BusinessSetupsPayload {
        try await authorizedGet(path: "v1/business/setups")
    }

    struct ActivateBusinessSetupResult: Decodable {
        let setupId: String
        let familyCode: String
        let momentId: String
        let companyId: String
        let momentTypeCode: String
        let title: String
        let status: String
        let version: Int
    }

    func activateBusinessSetup(
        familyCode: String,
        companyId: String,
        title: String? = nil,
        momentTypeCode: String? = nil,
        preferences: [String: Any]? = nil,
        timezone: String = "UTC",
        idempotencyKey: String = UUID().uuidString
    ) async throws -> ActivateBusinessSetupResult {
        struct Body: Encodable {
            let companyId: String
            let title: String?
            let momentTypeCode: String?
            let preferences: [String: JSONEncodableValue]?
            let timezone: String
        }
        return try await authorizedPost(
            path: "v1/business/setups/\(familyCode)/activate",
            body: Body(
                companyId: companyId,
                title: title,
                momentTypeCode: momentTypeCode,
                preferences: preferences.map { JSONEncodableValue.map($0) },
                timezone: timezone
            ),
            idempotencyKey: idempotencyKey
        )
    }

    func getLife360() async throws {
        let _: ProjectionEnvelopePayload = try await authorizedGet(path: "v1/life360")
    }

    struct CreateExpenseResult: Decodable {
        let expenseId: String
        let momentId: String
        let amount: String
        let currencyCode: String
        let status: String
        let version: Int
    }

    struct PersonalPulsePayload: Decodable {
        let userId: String
        let attentionCount: Int?
        let activeMomentCount: Int
        let recoveryScore: String?
        let moodState: String?
        let rhythmScore: String?
        let wellbeingScore: String?
        let projectionVersion: Int
        let widgetPayload: [String: AnyDecodable]?
        let updatedAt: String?
    }

    /// Personal Life Health — Figma `1047:7689`.
    struct PersonalLifePayload: Decodable {
        let userId: String
        let activeAreaCount: Int?
        /// FIGMA_SEEDED | REAL — clients must not treat seeded sections as PASS (S2 G3).
        let dataQuality: String?
        let sectionQuality: [String: String]?
        let score: Int?
        let scoreMax: Int?
        let statusLabel: String?
        let trendLabel: String?
        let insight: String?
        let areaScores: [LifeAreaScore]?
        let drift: LifeDrift?
        let leverage: LifeLeverage?
        let balance: [LifeBalanceAxis]?
        let emotionalTrend: LifeEmotionalTrend?
        let dominantEmotion: LifeDominantEmotion?
        let happyDrivers: LifeHappyDrivers?
        let journey: LifeJourney?
        let aiInsights: LifeAiInsights?
        let projectionVersion: Int?
        let updatedAt: String?

        struct LifeAreaScore: Decodable {
            let code: String
            let label: String
            let score: Int
            let color: String
        }
        struct LifeDrift: Decodable {
            let title: String
            let headline: String
            let body: String
            let ctaLabel: String
        }
        struct LifeLeverage: Decodable {
            let title: String
            let actionTitle: String
            let actionBody: String
            let ctaLabel: String
            let impacts: [LifeImpact]?
        }
        struct LifeImpact: Decodable {
            let label: String
            let delta: String
            let tone: String?
        }
        struct LifeBalanceAxis: Decodable {
            let code: String
            let label: String
            let score: Int
            let badge: String
            let badgeTone: String?
        }
        struct LifeEmotionalTrend: Decodable {
            let subtitle: String
            let series: [LifeEmotionSeries]?
        }
        struct LifeEmotionSeries: Decodable {
            let code: String
            let label: String
            let color: String
            let points: [Double]?
        }
        struct LifeDominantEmotion: Decodable {
            let title: String
            let headline: String
            let segments: [LifeEmotionSegment]?
        }
        struct LifeEmotionSegment: Decodable {
            let label: String
            let percent: Int
            let color: String
        }
        struct LifeHappyDrivers: Decodable {
            let title: String
            let subtitle: String
            let items: [String]?
        }
        struct LifeJourney: Decodable {
            let title: String
            let subtitle: String
            let items: [LifeJourneyItem]?
        }
        struct LifeJourneyItem: Decodable {
            let icon: String
            let title: String
            let whenLabel: String?
            let value: String
            let tone: String?

            enum CodingKeys: String, CodingKey {
                case icon, title, value, tone
                case whenLabel = "when"
            }
        }
        struct LifeAiInsights: Decodable {
            let title: String
            let lead: String
            let body: String
        }
    }

    struct ActivityItemPayload: Decodable, Identifiable {
        var id: String { occurredAt + title + (activityPayload?.expenseId ?? activityPayload?.incomeId ?? activityPayload?.activityId ?? "") }
        let activityCode: String
        let title: String
        let occurredAt: String
        let activityPayload: ActivityPayload?

        struct ActivityPayload: Decodable {
            let expenseId: String?
            let incomeId: String?
            let activityId: String?
            let amount: String?
            let currencyCode: String?
            let lifestyleContext: String?
            let description: String?
            let categoryCode: String?
            let subcategoryCode: String?
            let financialAccountId: String?
            let paymentMethodCode: String?
            let status: String?
            let wellbeingRating: Double?
        }
    }

    func createMoment(body: CreateMomentRequest, idempotencyKey: String) async throws -> CreateMomentAPIResponse {
        try await authorizedPostWithHints(path: "v1/moments", body: body, idempotencyKey: idempotencyKey)
    }

    struct MomentDetailPayload: Decodable {
        let momentId: String
        let domainCode: String
        let title: String
        let status: String
        let version: Int
    }

    struct MomentLifecycleResult: Decodable {
        let momentId: String
        let domainCode: String
        let title: String
        let status: String
        let version: Int
    }

    func getMomentDetail(momentId: String) async throws -> MomentDetailPayload {
        try await authorizedGet(path: "v1/moments/\(momentId)")
    }

    func updateMoment(
        momentId: String,
        title: String,
        expectedVersion: Int,
        idempotencyKey: String = UUID().uuidString
    ) async throws -> MomentLifecycleResult {
        struct Body: Encodable {
            let title: String
            let expectedVersion: Int
        }
        return try await authorizedPatch(
            path: "v1/moments/\(momentId)",
            body: Body(title: title, expectedVersion: expectedVersion),
            idempotencyKey: idempotencyKey
        )
    }

    func archiveMoment(
        momentId: String,
        expectedVersion: Int,
        idempotencyKey: String = UUID().uuidString
    ) async throws -> MomentLifecycleResult {
        struct Body: Encodable { let expectedVersion: Int }
        return try await authorizedPost(
            path: "v1/moments/\(momentId)/archive",
            body: Body(expectedVersion: expectedVersion),
            idempotencyKey: idempotencyKey
        )
    }

    func cancelMoment(
        momentId: String,
        expectedVersion: Int,
        idempotencyKey: String = UUID().uuidString
    ) async throws -> MomentLifecycleResult {
        struct Body: Encodable { let expectedVersion: Int }
        return try await authorizedPost(
            path: "v1/moments/\(momentId)/cancel",
            body: Body(expectedVersion: expectedVersion),
            idempotencyKey: idempotencyKey
        )
    }

    func deleteMoment(
        momentId: String,
        expectedVersion: Int,
        idempotencyKey: String = UUID().uuidString
    ) async throws -> MomentLifecycleResult {
        struct Body: Encodable { let expectedVersion: Int }
        return try await authorizedDelete(
            path: "v1/moments/\(momentId)",
            body: Body(expectedVersion: expectedVersion),
            idempotencyKey: idempotencyKey
        )
    }

    func mintGroupInvite(
        title: String,
        momentTypeCode: String,
        momentId: String? = nil,
        idempotencyKey: String
    ) async throws -> GroupInvite {
        struct Body: Encodable {
            let title: String
            let momentTypeCode: String
            let momentId: String?
        }
        return try await authorizedPost(
            path: "v1/group/invites",
            body: Body(title: title, momentTypeCode: momentTypeCode, momentId: momentId),
            idempotencyKey: idempotencyKey
        )
    }

    func getGroupInvite(code: String) async throws -> GroupInvite {
        try await authorizedGet(path: "v1/group/invites/\(code)")
    }

    func redeemGroupInvite(code: String, idempotencyKey: String) async throws -> RedeemGroupInviteResult {
        struct EmptyBody: Codable {}
        return try await authorizedPost(
            path: "v1/group/invites/\(code)/redeem",
            body: EmptyBody(),
            idempotencyKey: idempotencyKey
        )
    }

    func mintCompanyInvite(
        companyId: String,
        membershipType: String = "MEMBER",
        idempotencyKey: String
    ) async throws -> CompanyInvite {
        struct Body: Encodable {
            let companyId: String
            let membershipType: String
        }
        return try await authorizedPost(
            path: "v1/company/invites",
            body: Body(companyId: companyId, membershipType: membershipType),
            idempotencyKey: idempotencyKey
        )
    }

    func getCompanyInvite(code: String) async throws -> CompanyInvite {
        try await authorizedGet(path: "v1/company/invites/\(code)")
    }

    func redeemCompanyInvite(code: String, idempotencyKey: String) async throws -> RedeemCompanyInviteResult {
        struct EmptyBody: Codable {}
        return try await authorizedPost(
            path: "v1/company/invites/\(code)/redeem",
            body: EmptyBody(),
            idempotencyKey: idempotencyKey
        )
    }

    func createPersonalMoment(
        systemCode: String,
        momentTypeCode: String,
        title: String,
        preferences: [String: String]? = nil
    ) async throws -> CreateMomentResult {
        let mapped: [String: JSONEncodableValue]? = preferences.map { dict in
            dict.mapValues { JSONEncodableValue($0) }
        }
        let response = try await createMoment(
            body: CreateMomentRequest(
                domainCode: "PERSONAL",
                momentTypeCode: momentTypeCode,
                title: title,
                description: nil,
                startAt: nil,
                endAt: nil,
                timezone: "UTC",
                companyId: nil,
                participants: nil,
                personalSetup: .init(systemCode: systemCode, preferences: mapped),
                businessSetup: nil
            ),
            idempotencyKey: UUID().uuidString
        )
        return response.result
    }

    func createExpense(
        momentId: String,
        amount: String,
        currencyCode: String,
        description: String? = nil,
        merchantName: String? = nil,
        categoryCode: String? = nil,
        subcategoryCode: String? = nil,
        financialAccountId: String? = nil,
        paymentMethodCode: String? = nil,
        effectiveAt: String? = nil,
        recurringScheduleId: String? = nil,
        idempotencyKey: String = UUID().uuidString
    ) async throws -> CreateExpenseResult {
        struct Body: Encodable {
            let amount: String
            let currencyCode: String
            let description: String?
            let merchantName: String?
            let categoryCode: String?
            let subcategoryCode: String?
            let financialAccountId: String?
            let paymentMethodCode: String?
            let effectiveAt: String?
            let recurringScheduleId: String?
        }
        return try await authorizedPost(
            path: "v1/moments/\(momentId)/expenses",
            body: Body(
                amount: amount,
                currencyCode: currencyCode,
                description: description,
                merchantName: merchantName,
                categoryCode: categoryCode,
                subcategoryCode: subcategoryCode,
                financialAccountId: financialAccountId,
                paymentMethodCode: paymentMethodCode,
                effectiveAt: effectiveAt,
                recurringScheduleId: recurringScheduleId
            ),
            idempotencyKey: idempotencyKey
        )
    }

    struct CreateMovementResult: Decodable {
        let movementId: String
        let momentId: String
        let amount: String
        let movementType: String
    }

    func createMovement(
        momentId: String,
        movementType: String,
        amount: String,
        currencyCode: String,
        accountId: String? = nil,
        goalId: String? = nil,
        description: String? = nil,
        effectiveAt: String? = nil,
        idempotencyKey: String = UUID().uuidString
    ) async throws -> CreateMovementResult {
        struct Body: Encodable {
            let movementType: String
            let amount: String
            let currencyCode: String
            let accountId: String?
            let goalId: String?
            let description: String?
            let effectiveAt: String?
        }
        return try await authorizedPost(
            path: "v1/moments/\(momentId)/movements",
            body: Body(
                movementType: movementType,
                amount: amount,
                currencyCode: currencyCode,
                accountId: accountId,
                goalId: goalId,
                description: description,
                effectiveAt: effectiveAt
            ),
            idempotencyKey: idempotencyKey
        )
    }

    struct RecordObservationResult: Decodable {
        let observationId: String
        let momentId: String
        let observationType: String
    }

    func recordObservation(
        momentId: String,
        observationType: String,
        numericValue: Double? = nil,
        textValue: String? = nil,
        note: String? = nil,
        observedAt: String? = nil,
        activityTypeCode: String? = nil,
        durationMinutes: Int? = nil,
        energyBeforePct: Double? = nil,
        energyAfterPct: Double? = nil,
        feelingStateCode: String? = nil,
        moodDrivers: [String]? = nil,
        idempotencyKey: String = UUID().uuidString
    ) async throws -> RecordObservationResult {
        struct Body: Encodable {
            let observationType: String
            let numericValue: Double?
            let textValue: String?
            let note: String?
            let observedAt: String?
            let activityTypeCode: String?
            let durationMinutes: Int?
            let energyBeforePct: Double?
            let energyAfterPct: Double?
            let feelingStateCode: String?
            let moodDrivers: [String]?
        }
        return try await authorizedPost(
            path: "v1/moments/\(momentId)/observations",
            body: Body(
                observationType: observationType,
                numericValue: numericValue,
                textValue: textValue,
                note: note,
                observedAt: observedAt,
                activityTypeCode: activityTypeCode,
                durationMinutes: durationMinutes,
                energyBeforePct: energyBeforePct,
                energyAfterPct: energyAfterPct,
                feelingStateCode: feelingStateCode,
                moodDrivers: moodDrivers
            ),
            idempotencyKey: idempotencyKey
        )
    }

    struct AttentionCaptureResult: Decodable {
        let attentionCaptureId: String
        let momentId: String
    }

    func recordAttentionCapture(
        momentId: String,
        categoryCode: String,
        intensityCode: String,
        timeBlockCode: String,
        energyRemaining: Int? = nil,
        note: String? = nil,
        idempotencyKey: String = UUID().uuidString
    ) async throws -> AttentionCaptureResult {
        struct Body: Encodable {
            let categoryCode: String
            let intensityCode: String
            let timeBlockCode: String
            let energyRemaining: Int?
            let note: String?
        }
        return try await authorizedPost(
            path: "v1/moments/\(momentId)/attention-captures",
            body: Body(
                categoryCode: categoryCode,
                intensityCode: intensityCode,
                timeBlockCode: timeBlockCode,
                energyRemaining: energyRemaining,
                note: note
            ),
            idempotencyKey: idempotencyKey
        )
    }

    struct LifeOpsAdjustResult: Decodable {
        let adjustmentId: String
        let momentId: String
    }

    struct PriorityWeightBody: Encodable {
        let priorityCode: String
        let weightPct: Double
    }

    func recordLifeOpsAdjust(
        momentId: String,
        rhythmActionCode: String? = nil,
        signalDirectionCode: String? = nil,
        reason: String? = nil,
        priorityWeights: [PriorityWeightBody]? = nil,
        idempotencyKey: String = UUID().uuidString
    ) async throws -> LifeOpsAdjustResult {
        struct Body: Encodable {
            let rhythmActionCode: String?
            let signalDirectionCode: String?
            let reason: String?
            let priorityWeights: [PriorityWeightBody]?
        }
        return try await authorizedPost(
            path: "v1/moments/\(momentId)/life-ops-adjustments",
            body: Body(
                rhythmActionCode: rhythmActionCode,
                signalDirectionCode: signalDirectionCode,
                reason: reason,
                priorityWeights: priorityWeights
            ),
            idempotencyKey: idempotencyKey
        )
    }

    struct RuntimeSummary: Decodable {
        let momentId: String
        let entriesTodayCount: Int
        let lastEntryAt: String?
    }

    func getPersonalRuntimeSummary(momentId: String) async throws -> RuntimeSummary {
        try await authorizedGet(path: "v1/personal/moments/\(momentId)/runtime-summary")
    }

    struct CreateFutureItemResult: Decodable {
        let itemId: String
        let kind: String
        let title: String
    }

    func createFutureItem(
        momentId: String,
        kind: String,
        title: String,
        description: String? = nil,
        targetDate: String? = nil,
        progressValue: Double? = nil,
        unitCode: String? = nil,
        opportunityType: String? = nil,
        pivotReason: String? = nil,
        providerName: String? = nil,
        progressType: String? = nil,
        idempotencyKey: String = UUID().uuidString
    ) async throws -> CreateFutureItemResult {
        struct Body: Encodable {
            let kind: String
            let title: String
            let description: String?
            let targetDate: String?
            let progressValue: Double?
            let unitCode: String?
            let opportunityType: String?
            let pivotReason: String?
            let providerName: String?
            let progressType: String?
        }
        return try await authorizedPost(
            path: "v1/moments/\(momentId)/future-items",
            body: Body(
                kind: kind,
                title: title,
                description: description,
                targetDate: targetDate,
                progressValue: progressValue,
                unitCode: unitCode,
                opportunityType: opportunityType,
                pivotReason: pivotReason,
                providerName: providerName,
                progressType: progressType
            ),
            idempotencyKey: idempotencyKey
        )
    }

    struct FutureAxisSnapshot: Decodable {
        let momentId: String
        let visionScore: Double?
        let growthScore: Double?
        let momentumScore: Double?
        let disciplineScore: Double?
        let source: String?
    }

    func getFutureAxisSnapshot(momentId: String) async throws -> FutureAxisSnapshot {
        try await authorizedGet(path: "v1/personal/moments/\(momentId)/future-axis-snapshot")
    }

    struct LifestyleVitalitySnapshot: Decodable {
        let momentId: String
        let joyScore: Double?
        let fulfillmentScore: Double?
        let vitalityScore: Double?
        let explorationScore: Double?
        let source: String?
    }

    func getLifestyleVitalitySnapshot(momentId: String) async throws -> LifestyleVitalitySnapshot {
        try await authorizedGet(path: "v1/personal/moments/\(momentId)/lifestyle-vitality-snapshot")
    }

    struct RelationshipsBondSnapshot: Decodable {
        let momentId: String
        let trustScore: Double?
        let careScore: Double?
        let supportScore: Double?
        let presenceScore: Double?
        let bondIndex: Double?
        let source: String?
    }

    func getRelationshipsBondSnapshot(momentId: String) async throws -> RelationshipsBondSnapshot {
        try await authorizedGet(path: "v1/personal/moments/\(momentId)/relationships-bond-snapshot")
    }

    struct CreateSettlementResult: Decodable {
        let settlementId: String
        let momentId: String
        let amount: String
        let currencyCode: String
        let status: String
    }

    func createSettlement(
        momentId: String,
        payerParticipantId: String,
        payeeParticipantId: String,
        amount: String,
        currencyCode: String,
        obligationIds: [String]? = nil,
        idempotencyKey: String = UUID().uuidString
    ) async throws -> CreateSettlementResult {
        struct Body: Encodable {
            let payerParticipantId: String
            let payeeParticipantId: String
            let amount: String
            let currencyCode: String
            let obligationIds: [String]?
        }
        return try await authorizedPost(
            path: "v1/moments/\(momentId)/settlements",
            body: Body(
                payerParticipantId: payerParticipantId,
                payeeParticipantId: payeeParticipantId,
                amount: amount,
                currencyCode: currencyCode,
                obligationIds: obligationIds
            ),
            idempotencyKey: idempotencyKey
        )
    }

    struct CreateLifestyleActivityResult: Decodable {
        let activityId: String
        let lifestyleContext: String
        let title: String
    }

    func createLifestyleActivity(
        momentId: String,
        lifestyleContext: String,
        title: String,
        description: String? = nil,
        wellbeingRating: Double? = nil,
        idempotencyKey: String = UUID().uuidString
    ) async throws -> CreateLifestyleActivityResult {
        struct Body: Encodable {
            let lifestyleContext: String
            let title: String
            let description: String?
            let wellbeingRating: Double?
        }
        return try await authorizedPost(
            path: "v1/moments/\(momentId)/lifestyle-activities",
            body: Body(
                lifestyleContext: lifestyleContext,
                title: title,
                description: description,
                wellbeingRating: wellbeingRating
            ),
            idempotencyKey: idempotencyKey
        )
    }

    func updateLifestyleActivity(
        momentId: String,
        activityId: String,
        title: String? = nil,
        description: String? = nil,
        wellbeingRating: Double? = nil
    ) async throws -> CreateLifestyleActivityResult {
        struct Body: Encodable {
            let title: String?
            let description: String?
            let wellbeingRating: Double?
        }
        return try await authorizedPatch(
            path: "v1/moments/\(momentId)/lifestyle-activities/\(activityId)",
            body: Body(title: title, description: description, wellbeingRating: wellbeingRating)
        )
    }

    struct VoidLifestyleActivityResult: Decodable {
        let activityId: String
        let lifestyleContext: String
        let title: String
        let status: String
    }

    func voidLifestyleActivity(momentId: String, activityId: String) async throws -> VoidLifestyleActivityResult {
        try await authorizedDelete(path: "v1/moments/\(momentId)/lifestyle-activities/\(activityId)")
    }

    struct UpdateExpenseResult: Decodable {
        let expenseId: String
        let momentId: String
        let amount: String
        let currencyCode: String
        let status: String
        let version: Int
    }

    struct ExpenseDetail: Decodable {
        let expenseId: String
        let momentId: String
        let amount: String
        let currencyCode: String
        let status: String
        let version: Int
        let description: String?
        let merchantName: String?
        let categoryCode: String?
        let subcategoryCode: String?
        let financialAccountId: String?
        let paymentMethodCode: String?
        let effectiveAt: String?
        let recurringScheduleId: String?
        let attachmentIds: [String]?
    }

    func getExpense(momentId: String, expenseId: String) async throws -> ExpenseDetail {
        try await authorizedGet(path: "v1/moments/\(momentId)/expenses/\(expenseId)")
    }

    struct FinancialAccount: Decodable, Identifiable {
        var id: String { financialAccountId }
        let financialAccountId: String
        let accountType: String
        let accountName: String
        let currencyCode: String
        let institutionName: String?
        let status: String
    }

    func listFinancialAccounts() async throws -> [FinancialAccount] {
        try await authorizedGet(path: "v1/financial-accounts")
    }

    func createFinancialAccount(
        accountType: String,
        accountName: String,
        currencyCode: String,
        institutionName: String? = nil,
        idempotencyKey: String = UUID().uuidString
    ) async throws -> FinancialAccount {
        struct Body: Encodable {
            let accountType: String
            let accountName: String
            let currencyCode: String
            let institutionName: String?
        }
        return try await authorizedPost(
            path: "v1/financial-accounts",
            body: Body(
                accountType: accountType,
                accountName: accountName,
                currencyCode: currencyCode.uppercased(),
                institutionName: institutionName.flatMap { $0.isEmpty ? nil : $0 }
            ),
            idempotencyKey: idempotencyKey
        )
    }

    func voidExpense(momentId: String, expenseId: String) async throws -> UpdateExpenseResult {
        try await authorizedDelete(path: "v1/moments/\(momentId)/expenses/\(expenseId)")
    }

    struct PersonalIncomeResult: Decodable {
        let incomeId: String
        let momentId: String
        let amount: String
        let currencyCode: String
        let status: String
        let version: Int
    }

    func createPersonalIncome(
        momentId: String,
        amount: String,
        currencyCode: String,
        description: String? = nil,
        merchantName: String? = nil,
        categoryCode: String? = nil,
        financialAccountId: String? = nil,
        paymentMethodCode: String? = nil,
        effectiveAt: String? = nil,
        idempotencyKey: String = UUID().uuidString
    ) async throws -> PersonalIncomeResult {
        struct Body: Encodable {
            let amount: String
            let currencyCode: String
            let description: String?
            let merchantName: String?
            let categoryCode: String?
            let financialAccountId: String?
            let paymentMethodCode: String?
            let effectiveAt: String?
        }
        return try await authorizedPost(
            path: "v1/moments/\(momentId)/income",
            body: Body(
                amount: amount,
                currencyCode: currencyCode,
                description: description,
                merchantName: merchantName,
                categoryCode: categoryCode,
                financialAccountId: financialAccountId,
                paymentMethodCode: paymentMethodCode,
                effectiveAt: effectiveAt
            ),
            idempotencyKey: idempotencyKey
        )
    }

    func voidPersonalIncome(momentId: String, incomeId: String) async throws -> PersonalIncomeResult {
        try await authorizedDelete(path: "v1/moments/\(momentId)/income/\(incomeId)")
    }

    struct ExpenseAttachment: Decodable {
        let uploadId: String
        let contentType: String?
        let status: String
        let createdAt: String?
    }

    struct MediaUploadIntentResult: Decodable {
        let uploadId: String
        let signedUrl: String
        let storageKey: String?
        let expiresAt: String
    }

    struct MediaUploadCompleteResult: Decodable {
        let uploadId: String
        let mediaId: String
        let status: String
    }

    struct MemoryAttachment: Decodable {
        let uploadId: String
        let contentType: String?
        let status: String
        let createdAt: String?
    }

    func uploadAndAttachExpenseMedia(
        momentId: String,
        expenseId: String,
        bytes: Data,
        contentType: String = "image/jpeg"
    ) async throws -> ExpenseAttachment {
        struct IntentBody: Encodable {
            let contentType: String
            let byteSize: Int
            let scopeType: String
            let scopeId: String
        }
        struct CompleteBody: Encodable {
            let storageKey: String
        }
        struct AttachBody: Encodable {
            let uploadId: String
        }
        let intent: MediaUploadIntentResult = try await authorizedPost(
            path: "v1/media/uploads",
            body: IntentBody(contentType: contentType, byteSize: bytes.count, scopeType: "MOMENT", scopeId: momentId),
            idempotencyKey: UUID().uuidString
        )
        guard let storageKey = intent.storageKey else {
            throw URLError(.badServerResponse)
        }
        try await putBytesToSignedUrl(signedUrl: intent.signedUrl, bytes: bytes, contentType: contentType)
        let _: MediaUploadCompleteResult = try await authorizedPost(
            path: "v1/media/uploads/\(intent.uploadId)/complete",
            body: CompleteBody(storageKey: storageKey),
            idempotencyKey: UUID().uuidString
        )
        return try await authorizedPost(
            path: "v1/moments/\(momentId)/expenses/\(expenseId)/attachments",
            body: AttachBody(uploadId: intent.uploadId),
            idempotencyKey: UUID().uuidString
        )
    }

    func uploadAndAttachMemoryMedia(
        momentId: String,
        memoryId: String,
        bytes: Data,
        contentType: String = "image/jpeg"
    ) async throws -> MemoryAttachment {
        struct IntentBody: Encodable {
            let contentType: String
            let byteSize: Int
            let scopeType: String
            let scopeId: String
        }
        struct CompleteBody: Encodable {
            let storageKey: String
        }
        struct AttachBody: Encodable {
            let uploadId: String
        }
        let intent: MediaUploadIntentResult = try await authorizedPost(
            path: "v1/media/uploads",
            body: IntentBody(contentType: contentType, byteSize: bytes.count, scopeType: "MOMENT", scopeId: momentId),
            idempotencyKey: UUID().uuidString
        )
        guard let storageKey = intent.storageKey else {
            throw URLError(.badServerResponse)
        }
        try await putBytesToSignedUrl(signedUrl: intent.signedUrl, bytes: bytes, contentType: contentType)
        let _: MediaUploadCompleteResult = try await authorizedPost(
            path: "v1/media/uploads/\(intent.uploadId)/complete",
            body: CompleteBody(storageKey: storageKey),
            idempotencyKey: UUID().uuidString
        )
        return try await authorizedPost(
            path: "v1/moments/\(momentId)/memories/\(memoryId)/media",
            body: AttachBody(uploadId: intent.uploadId),
            idempotencyKey: UUID().uuidString
        )
    }

    private func putBytesToSignedUrl(signedUrl: String, bytes: Data, contentType: String) async throws {
        guard let url = URL(string: signedUrl) else { throw URLError(.badURL) }
        var request = URLRequest(url: url)
        request.httpMethod = "PUT"
        request.setValue(contentType, forHTTPHeaderField: "Content-Type")
        request.httpBody = bytes
        let (_, response) = try await URLSession.shared.data(for: request)
        guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
            throw URLError(.cannotWriteToFile)
        }
    }

    func updateExpense(
        momentId: String,
        expenseId: String,
        amount: String? = nil,
        currencyCode: String? = nil,
        description: String? = nil,
        merchantName: String? = nil,
        categoryCode: String? = nil,
        subcategoryCode: String? = nil,
        financialAccountId: String? = nil,
        paymentMethodCode: String? = nil,
        effectiveAt: String? = nil,
        recurringScheduleId: String? = nil
    ) async throws -> UpdateExpenseResult {
        struct Body: Encodable {
            let amount: String?
            let currencyCode: String?
            let description: String?
            let merchantName: String?
            let categoryCode: String?
            let subcategoryCode: String?
            let financialAccountId: String?
            let paymentMethodCode: String?
            let effectiveAt: String?
            let recurringScheduleId: String?
        }
        return try await authorizedPatch(
            path: "v1/moments/\(momentId)/expenses/\(expenseId)",
            body: Body(
                amount: amount,
                currencyCode: currencyCode,
                description: description,
                merchantName: merchantName,
                categoryCode: categoryCode,
                subcategoryCode: subcategoryCode,
                financialAccountId: financialAccountId,
                paymentMethodCode: paymentMethodCode,
                effectiveAt: effectiveAt,
                recurringScheduleId: recurringScheduleId
            )
        )
    }

    func getPersonalPulse(momentId: String? = nil) async throws -> PersonalPulsePayload {
        var query: [String: String] = [:]
        if let momentId { query["momentId"] = momentId }
        return try await authorizedGet(path: "v1/personal/pulse", query: query)
    }

    func getPersonalLife() async throws -> PersonalLifePayload {
        try await authorizedGet(path: "v1/personal/life")
    }

    struct PersonalAttentionItemPayload: Decodable, Identifiable {
        var id: String { attentionCaptureId }
        let attentionCaptureId: String
        let momentId: String
        let categoryCode: String
        let intensityCode: String
        let timeBlockCode: String
        let energyRemaining: Int?
        let observedAt: String
        let note: String?
    }

    struct PersonalAttentionPayload: Decodable {
        let userId: String
        let items: [PersonalAttentionItemPayload]
    }

    func getPersonalAttention() async throws -> PersonalAttentionPayload {
        try await authorizedGet(path: "v1/personal/attention")
    }

    struct PersonalSetupCatalogItemPayload: Decodable, Identifiable {
        var id: String { systemCode }
        let systemCode: String
        let title: String
        let subtitle: String
        let figmaNodeId: String?
        let defaultMomentTypeCode: String
        let activateLabel: String?
        let defaultTitle: String?
    }

    struct PersonalSetupItemPayload: Decodable, Identifiable {
        var id: String { setupId }
        let setupId: String
        let systemCode: String
        let title: String
        let momentId: String
        let status: String
        let preferences: [String: AnyDecodable]?
        let createdAt: String
    }

    struct PersonalSetupsPayload: Decodable {
        let catalog: [PersonalSetupCatalogItemPayload]
        let items: [PersonalSetupItemPayload]
    }

    func getPersonalSetups() async throws -> PersonalSetupsPayload {
        try await authorizedGet(path: "v1/personal/setups")
    }

    struct ActivatePersonalSetupResult: Decodable {
        let setupId: String
        let systemCode: String
        let momentId: String
        let momentTypeCode: String
        let title: String
        let status: String
        let version: Int
    }

    func activatePersonalSetup(
        systemCode: String,
        title: String? = nil,
        momentTypeCode: String? = nil,
        preferences: [String: Any]? = nil,
        timezone: String = "UTC",
        idempotencyKey: String = UUID().uuidString
    ) async throws -> ActivatePersonalSetupResult {
        struct Body: Encodable {
            let title: String?
            let momentTypeCode: String?
            let preferences: [String: JSONEncodableValue]?
            let timezone: String
        }
        return try await authorizedPost(
            path: "v1/personal/setups/\(systemCode)/activate",
            body: Body(
                title: title,
                momentTypeCode: momentTypeCode,
                preferences: preferences.map { JSONEncodableValue.map($0) },
                timezone: timezone
            ),
            idempotencyKey: idempotencyKey
        )
    }

    struct ExpenseSubcategoryPayload: Decodable, Identifiable {
        var id: String { subcategoryCode }
        let subcategoryCode: String
        let label: String
        let sortOrder: Int
    }

    struct ExpenseCategoryPayload: Decodable, Identifiable {
        var id: String { categoryCode }
        let categoryCode: String
        let label: String
        let sortOrder: Int
        let subcategories: [ExpenseSubcategoryPayload]
    }

    struct ExpenseCategoriesPayload: Decodable {
        let categories: [ExpenseCategoryPayload]
    }

    func listExpenseCategories() async throws -> ExpenseCategoriesPayload {
        try await authorizedGet(path: "v1/finance/expense-categories")
    }

    struct RecurringSchedulePayload: Decodable, Identifiable {
        var id: String { recurringScheduleId }
        let recurringScheduleId: String
        let momentId: String
        let resourceKind: String
        let templatePayload: [String: AnyDecodable]?
        let frequency: String
        let intervalCount: Int
        let startDate: String
        let endDate: String?
        let nextRunAt: String?
        let status: String
        let version: Int
    }

    struct GenerateRecurringInstanceResult: Decodable {
        let expenseId: String?
        let incomeId: String?
        let occurrenceDate: String
    }

    func listRecurringSchedules(momentId: String) async throws -> [RecurringSchedulePayload] {
        try await authorizedGet(path: "v1/moments/\(momentId)/recurring-schedules")
    }

    func createRecurringSchedule(
        momentId: String,
        resourceKind: String,
        templatePayload: [String: Any],
        frequency: String,
        startDate: String,
        intervalCount: Int = 1,
        endDate: String? = nil,
        idempotencyKey: String = UUID().uuidString
    ) async throws -> RecurringSchedulePayload {
        struct Body: Encodable {
            let resourceKind: String
            let templatePayload: [String: JSONEncodableValue]
            let frequency: String
            let intervalCount: Int
            let startDate: String
            let endDate: String?
        }
        return try await authorizedPost(
            path: "v1/moments/\(momentId)/recurring-schedules",
            body: Body(
                resourceKind: resourceKind,
                templatePayload: JSONEncodableValue.map(templatePayload),
                frequency: frequency,
                intervalCount: intervalCount,
                startDate: startDate,
                endDate: endDate
            ),
            idempotencyKey: idempotencyKey
        )
    }

    func updateRecurringSchedule(
        momentId: String,
        scheduleId: String,
        status: String? = nil,
        endDate: String? = nil,
        templatePayload: [String: Any]? = nil
    ) async throws -> RecurringSchedulePayload {
        struct Body: Encodable {
            let status: String?
            let endDate: String?
            let templatePayload: [String: JSONEncodableValue]?
        }
        let mappedPayload = templatePayload.map { JSONEncodableValue.map($0) }
        return try await authorizedPatch(
            path: "v1/moments/\(momentId)/recurring-schedules/\(scheduleId)",
            body: Body(status: status, endDate: endDate, templatePayload: mappedPayload)
        )
    }

    func generateRecurringInstance(
        momentId: String,
        scheduleId: String,
        idempotencyKey: String = UUID().uuidString
    ) async throws -> GenerateRecurringInstanceResult {
        struct EmptyBody: Codable {}
        return try await authorizedPost(
            path: "v1/moments/\(momentId)/recurring-schedules/\(scheduleId)/generate",
            body: EmptyBody(),
            idempotencyKey: idempotencyKey
        )
    }

    /// Personal Memory projection — honest empty when `items` is empty (S2 G4).
    struct PersonalMemoryPayload: Decodable {
        let userId: String
        let items: [PersonalMemoryItem]

        struct PersonalMemoryItem: Decodable, Identifiable {
            var id: String { memoryId ?? title ?? occurredAt ?? "memory-item" }
            let memoryId: String?
            let title: String?
            let body: String?
            let occurredAt: String?
        }
    }

    func getPersonalMemory() async throws -> PersonalMemoryPayload {
        try await authorizedGet(path: "v1/personal/memory")
    }

    struct CreateRelationshipActivityResult: Decodable {
        let activityId: String
        let displayName: String
    }

    /// POST `/v1/moments/:id/relationship-activities` — matches backend `relationshipActivitySchema`.
    func createRelationshipActivity(
        momentId: String,
        activityKind: String,
        displayName: String,
        note: String? = nil,
        occurredAt: String? = nil,
        idempotencyKey: String = UUID().uuidString
    ) async throws -> CreateRelationshipActivityResult {
        struct Body: Encodable {
            let activityKind: String
            let displayName: String
            let note: String?
            let occurredAt: String?
        }
        return try await authorizedPost(
            path: "v1/moments/\(momentId)/relationship-activities",
            body: Body(
                activityKind: activityKind,
                displayName: displayName,
                note: note,
                occurredAt: occurredAt
            ),
            idempotencyKey: idempotencyKey
        )
    }

    func listPersonalActivity(momentId: String? = nil, limit: Int = 20) async throws -> [ActivityItemPayload] {
        var query: [String: String] = ["limit": String(limit)]
        if let momentId { query["momentId"] = momentId }
        let page: CursorPagePayload<ActivityItemPayload> =
            try await authorizedGet(path: "v1/personal/activity", query: query)
        return page.items
    }

    // MARK: - S3 Group facets + finance

    struct GroupFinanceTotalsPayload: Decodable {
        let currencyCode: String
        let expenseTotal: String?
        let budgetTotal: String?
        let contributionTotal: String?
        let settledTotal: String?
        let outstandingTotal: String?
    }

    struct GroupFinancePositionPayload: Decodable, Identifiable {
        var id: String { "\(participantId)-\(currencyCode)" }
        let participantId: String
        let currencyCode: String
        let paidTotal: String?
        let allocatedTotal: String?
        let contributionTotal: String?
        let payableTotal: String?
        let receivableTotal: String?
        let settledTotal: String?
        let netPosition: String?
    }

    struct GroupFinancePayload: Decodable {
        let dataQuality: String?
        let expenseCount: Int?
        let totals: [GroupFinanceTotalsPayload]?
        let positions: [GroupFinancePositionPayload]?
        let viewerPosition: GroupFinancePositionPayload?
        let positionTotalCount: Int?
    }

    struct GroupPulsePayload: Decodable {
        let momentId: String?
        let facet: String?
        let title: String?
        let groupFamily: String?
        let status: String?
        let payload: PulseInner?

        struct PulseInner: Decodable {
            let dataQuality: String?
            let participantCount: Int?
            let attentionCount: Int?
            let openTaskCount: Int?
            let widgetPayload: [String: AnyDecodable]?
            let finance: GroupFinancePayload?
        }
    }

    struct GroupLifePayload: Decodable {
        let momentId: String?
        let facet: String?
        let title: String?
        let groupFamily: String?
        let status: String?
        let payload: LifeInner?

        struct DomainMetric: Decodable {
            let score: Int?
            let label: String?
            let status: String?
        }

        struct Domains: Decodable {
            let experience: DomainMetric?
            let purchase: DomainMetric?
            let living: DomainMetric?
            let goal: DomainMetric?
            let community: DomainMetric?
        }

        struct Health: Decodable {
            let score: Int?
            let label: String?
        }

        struct BalanceBar: Decodable {
            let value: Int?
            let label: String?
        }

        struct Balance: Decodable {
            let participation: BalanceBar?
            let contribution: BalanceBar?
            let coordination: BalanceBar?
            let progress: BalanceBar?
            let community: BalanceBar?
        }

        struct Counts: Decodable {
            let participantCount: Int?
            let openTaskCount: Int?
            let planningCount: Int?
            let bookingCount: Int?
            let updateCount: Int?
            let pollCount: Int?
            let purchaseItemCount: Int?
            let residentCount: Int?
            let expenseTotal: String?
            let budgetTotal: String?
            let contributionTotal: String?
        }

        struct Driver: Decodable, Identifiable {
            var id: String { "\(domain ?? "")-\(title ?? "")" }
            let domain: String?
            let title: String?
            let detail: String?
        }

        struct ActivityItem: Decodable, Identifiable {
            var id: String { "\(kind ?? "")-\(itemId ?? title ?? "")-\(at ?? "")" }
            let kind: String?
            let itemId: String?
            let title: String?
            let at: String?

            enum CodingKeys: String, CodingKey {
                case kind, title, at
                case itemId = "id"
            }
        }

        struct LifeInner: Decodable {
            let dataQuality: String?
            let metricVersion: String?
            let sections: [String: String]?
            let openTaskCount: Int?
            let participantCount: Int?
            let counts: Counts?
            let domains: Domains?
            let health: Health?
            let balance: Balance?
            let drivers: [Driver]?
            let activity: [ActivityItem]?
            let planningItems: [PlanningItem]?
            let bookings: [BookingItem]?
            let updates: [UpdateItem]?

            struct PlanningItem: Decodable {
                let planningItemId: String?
                let title: String?
                let dueAt: String?
                let status: String?
            }

            struct BookingItem: Decodable {
                let bookingId: String?
                let title: String?
                let status: String?
            }

            struct UpdateItem: Decodable {
                let updateId: String?
                let message: String?
                let createdAt: String?
            }
        }
    }

    struct GroupMemoryPayload: Decodable {
        let momentId: String?
        let facet: String?
        let title: String?
        let groupFamily: String?
        let status: String?
        let payload: MemoryInner?

        struct MemoryInner: Decodable {
            let dataQuality: String?
            let items: [GroupMemoryItem]?
            let memoryCount: Int?

            struct GroupMemoryItem: Decodable, Identifiable {
                var id: String { memoryId ?? title ?? occurredAt ?? "memory" }
                let memoryId: String?
                let title: String?
                let occurredAt: String?
            }
        }
    }

    struct GroupFinanceFacetPayload: Decodable {
        let momentId: String?
        let facet: String?
        let title: String?
        let groupFamily: String?
        let status: String?
        let payload: GroupFinancePayload?
    }

    struct GroupParticipantPayload: Decodable, Identifiable {
        var id: String { participantId }
        let participantId: String
        let userId: String?
        let roleCode: String?
        let status: String?
        let displayName: String?
    }

    struct GroupParticipantsPayload: Decodable {
        let momentId: String?
        let participants: [GroupParticipantPayload]
    }

    struct CreateGroupExpenseResult: Decodable {
        let expenseId: String
        let momentId: String
        let amount: String
        let currencyCode: String
        let status: String
        let version: Int
        let paidByParticipantId: String?
        let splitStrategy: String?
    }

    struct RecordContributionResult: Decodable {
        let contributionId: String
        let momentId: String
    }

    struct GroupSplitInput: Encodable {
        let participantId: String
        let amount: String?
        let percent: String?
        let shares: Double?

        init(participantId: String, amount: String? = nil, percent: String? = nil, shares: Double? = nil) {
            self.participantId = participantId
            self.amount = amount
            self.percent = percent
            self.shares = shares
        }
    }

    func getGroupPulse(momentId: String) async throws -> GroupPulsePayload {
        try await authorizedGet(path: "v1/group/moments/\(momentId)/pulse")
    }

    func getGroupLife(momentId: String) async throws -> GroupLifePayload {
        try await authorizedGet(path: "v1/group/moments/\(momentId)/life")
    }

    func getGroupMemory(momentId: String) async throws -> GroupMemoryPayload {
        try await authorizedGet(path: "v1/group/moments/\(momentId)/memory")
    }

    func getGroupFinance(momentId: String) async throws -> GroupFinanceFacetPayload {
        try await authorizedGet(path: "v1/group/moments/\(momentId)/finance")
    }

    struct PatchGroupBudgetResult: Decodable {
        let momentId: String
        let budgetAmount: String
        let budgetCurrencyCode: String
        let budgetId: String
    }

    func patchGroupBudget(
        momentId: String,
        budgetAmount: String,
        budgetCurrencyCode: String,
        idempotencyKey: String = UUID().uuidString
    ) async throws -> PatchGroupBudgetResult {
        struct Body: Encodable {
            let budgetAmount: String
            let budgetCurrencyCode: String
        }
        return try await authorizedPatch(
            path: "v1/group/moments/\(momentId)/budget",
            body: Body(budgetAmount: budgetAmount, budgetCurrencyCode: budgetCurrencyCode),
            idempotencyKey: idempotencyKey
        )
    }

    func listGroupParticipants(momentId: String) async throws -> [GroupParticipantPayload] {
        let page: GroupParticipantsPayload =
            try await authorizedGet(path: "v1/group/moments/\(momentId)/participants")
        return page.participants
    }

    func listGroupActivity(momentId: String, limit: Int = 20) async throws -> [ActivityItemPayload] {
        let page: CursorPagePayload<ActivityItemPayload> =
            try await authorizedGet(
                path: "v1/group/moments/\(momentId)/activity",
                query: ["limit": String(limit)]
            )
        return page.items
    }

    func createGroupExpense(
        momentId: String,
        amount: String,
        currencyCode: String,
        description: String? = nil,
        paidByParticipantId: String,
        splitStrategy: String,
        splitInputs: [GroupSplitInput],
        idempotencyKey: String = UUID().uuidString
    ) async throws -> CreateGroupExpenseResult {
        struct Body: Encodable {
            let amount: String
            let currencyCode: String
            let description: String?
            let paidByParticipantId: String
            let splitStrategy: String
            let splitInputs: [GroupSplitInput]
        }
        return try await authorizedPost(
            path: "v1/moments/\(momentId)/group-expenses",
            body: Body(
                amount: amount,
                currencyCode: currencyCode,
                description: description,
                paidByParticipantId: paidByParticipantId,
                splitStrategy: splitStrategy,
                splitInputs: splitInputs
            ),
            idempotencyKey: idempotencyKey
        )
    }

    func recordContribution(
        momentId: String,
        amount: String,
        currencyCode: String,
        label: String? = nil,
        idempotencyKey: String = UUID().uuidString
    ) async throws -> RecordContributionResult {
        struct Body: Encodable {
            let amount: String
            let currencyCode: String
            let label: String?
        }
        return try await authorizedPost(
            path: "v1/moments/\(momentId)/contributions",
            body: Body(amount: amount, currencyCode: currencyCode, label: label),
            idempotencyKey: idempotencyKey
        )
    }

    struct CollabIdResult: Decodable {
        let planningItemId: String?
        let bookingId: String?
        let pollId: String?
        let updateId: String?
        let memoryId: String?
        let purchaseItemId: String?
        let residentId: String?
        let sharedAssetId: String?
        let maintenanceRecordId: String?
        let momentId: String?
    }

    func createPlanningItem(momentId: String, title: String, dueAt: String? = nil, idempotencyKey: String = UUID().uuidString) async throws -> CollabIdResult {
        struct Body: Encodable { let title: String; let dueAt: String? }
        return try await authorizedPost(path: "v1/moments/\(momentId)/planning-items", body: Body(title: title, dueAt: dueAt), idempotencyKey: idempotencyKey)
    }

    // MARK: - Group collab list GETs (parity with Android ApiService)

    struct GroupPlanningItemsPayload: Decodable {
        let momentId: String?
        let items: [GroupLifePayload.LifeInner.PlanningItem]
        let openCount: Int?
    }

    struct GroupBookingsPayload: Decodable {
        let momentId: String?
        let items: [GroupLifePayload.LifeInner.BookingItem]
    }

    struct GroupUpdatesPayload: Decodable {
        let momentId: String?
        let items: [GroupLifePayload.LifeInner.UpdateItem]
    }

    struct GroupPollOptionPayload: Decodable {
        let pollOptionId: String?
        let text: String?
        let sortOrder: Int?
    }

    struct GroupPollItemPayload: Decodable, Identifiable {
        var id: String { pollId ?? question ?? UUID().uuidString }
        let pollId: String?
        let question: String?
        let status: String?
        let closesAt: String?
        let createdAt: String?
        let options: [GroupPollOptionPayload]?
    }

    struct GroupPollsPayload: Decodable {
        let momentId: String?
        let items: [GroupPollItemPayload]
    }

    struct GroupPollDetailOptionPayload: Decodable, Identifiable {
        var id: String { pollOptionId ?? text ?? UUID().uuidString }
        let pollOptionId: String?
        let text: String?
        let sortOrder: Int?
        let voteCount: Int?
        let votedByMe: Bool?
    }

    struct GroupPollDetailPayload: Decodable {
        let pollId: String?
        let momentId: String?
        let question: String?
        let status: String?
        let pollType: String?
        let closesAt: String?
        let createdAt: String?
        let options: [GroupPollDetailOptionPayload]?
    }

    struct GroupPurchaseItemPayload: Decodable, Identifiable {
        var id: String { purchaseItemId ?? label ?? UUID().uuidString }
        let purchaseItemId: String?
        let label: String?
        let amount: String?
        let status: String?
    }

    struct GroupPurchaseItemsPayload: Decodable {
        let momentId: String?
        let items: [GroupPurchaseItemPayload]
    }

    struct GroupResidentPayload: Decodable, Identifiable {
        var id: String { residentId ?? participantId ?? UUID().uuidString }
        let residentId: String?
        let participantId: String?
        let roleCode: String?
        let status: String?
        let name: String?
    }

    struct GroupResidentsPayload: Decodable {
        let momentId: String?
        let items: [GroupResidentPayload]
    }

    struct GroupMemoriesListPayload: Decodable {
        let momentId: String?
        let items: [GroupMemoryPayload.MemoryInner.GroupMemoryItem]
        let memoryCount: Int?
    }

    func listPlanningItems(momentId: String) async throws -> GroupPlanningItemsPayload {
        try await authorizedGet(path: "v1/group/moments/\(momentId)/planning-items")
    }

    func listBookings(momentId: String) async throws -> GroupBookingsPayload {
        try await authorizedGet(path: "v1/group/moments/\(momentId)/bookings")
    }

    func listPolls(momentId: String) async throws -> GroupPollsPayload {
        try await authorizedGet(path: "v1/group/moments/\(momentId)/polls")
    }

    func getPoll(pollId: String) async throws -> GroupPollDetailPayload {
        try await authorizedGet(path: "v1/polls/\(pollId)")
    }

    func listGroupUpdates(momentId: String) async throws -> GroupUpdatesPayload {
        try await authorizedGet(path: "v1/group/moments/\(momentId)/updates")
    }

    struct GroupLivingRulePayload: Decodable, Identifiable {
        var id: String { livingRuleId }
        let livingRuleId: String
        let title: String
        let ruleText: String
        let status: String
        let createdAt: String
    }

    struct GroupLivingRulesPayload: Decodable {
        let momentId: String
        let items: [GroupLivingRulePayload]
    }

    func listLivingRules(momentId: String) async throws -> GroupLivingRulesPayload {
        try await authorizedGet(path: "v1/group/moments/\(momentId)/living-rules")
    }

    struct GroupCollabListPayload: Decodable {
        let momentId: String?
        let items: [AnyDecodable]?
    }

    func listDeliveryHandovers(momentId: String) async throws -> GroupCollabListPayload {
        try await authorizedGet(path: "v1/group/moments/\(momentId)/delivery-handovers")
    }

    func listOwnershipRecords(momentId: String) async throws -> GroupCollabListPayload {
        try await authorizedGet(path: "v1/group/moments/\(momentId)/ownership-records")
    }

    func listGroupAttendance(momentId: String) async throws -> GroupCollabListPayload {
        try await authorizedGet(path: "v1/group/moments/\(momentId)/attendance")
    }

    func listPurchaseItems(momentId: String) async throws -> GroupPurchaseItemsPayload {
        try await authorizedGet(path: "v1/group/moments/\(momentId)/purchase-items")
    }

    func listResidents(momentId: String) async throws -> GroupResidentsPayload {
        try await authorizedGet(path: "v1/group/moments/\(momentId)/residents")
    }

    struct GroupSharedAssetPayload: Decodable, Identifiable {
        var id: String { sharedAssetId ?? title ?? UUID().uuidString }
        let sharedAssetId: String?
        let title: String?
        let assetType: String?
        let conditionCode: String?
        let status: String?
        let createdAt: String?
    }

    struct GroupSharedAssetsPayload: Decodable {
        let momentId: String?
        let items: [GroupSharedAssetPayload]
    }

    struct GroupMaintenanceRecordPayload: Decodable, Identifiable {
        var id: String { maintenanceRecordId ?? title ?? UUID().uuidString }
        let maintenanceRecordId: String?
        let sharedAssetId: String?
        let title: String?
        let description: String?
        let status: String?
        let createdAt: String?
    }

    struct GroupMaintenanceRecordsPayload: Decodable {
        let momentId: String?
        let items: [GroupMaintenanceRecordPayload]
    }

    func listSharedAssets(momentId: String) async throws -> GroupSharedAssetsPayload {
        try await authorizedGet(path: "v1/group/moments/\(momentId)/shared-assets")
    }

    func listMaintenanceRecords(momentId: String) async throws -> GroupMaintenanceRecordsPayload {
        try await authorizedGet(path: "v1/group/moments/\(momentId)/maintenance-records")
    }

    func listGroupMemories(momentId: String) async throws -> GroupMemoriesListPayload {
        try await authorizedGet(path: "v1/group/moments/\(momentId)/memories")
    }

    func createBooking(momentId: String, title: String, bookedAt: String? = nil, idempotencyKey: String = UUID().uuidString) async throws -> CollabIdResult {
        struct Body: Encodable { let title: String; let bookedAt: String? }
        return try await authorizedPost(path: "v1/moments/\(momentId)/bookings", body: Body(title: title, bookedAt: bookedAt), idempotencyKey: idempotencyKey)
    }

    func createPoll(momentId: String, question: String, options: [String], closesAt: String? = nil, pollType: String? = nil, idempotencyKey: String = UUID().uuidString) async throws -> CollabIdResult {
        struct Body: Encodable { let question: String; let options: [String]; let closesAt: String?; let pollType: String? }
        return try await authorizedPost(path: "v1/moments/\(momentId)/polls", body: Body(question: question, options: options, closesAt: closesAt, pollType: pollType), idempotencyKey: idempotencyKey)
    }

    func postGroupUpdate(momentId: String, message: String, idempotencyKey: String = UUID().uuidString) async throws -> CollabIdResult {
        struct Body: Encodable { let message: String }
        return try await authorizedPost(path: "v1/moments/\(momentId)/updates", body: Body(message: message), idempotencyKey: idempotencyKey)
    }

    func createGroupMemory(momentId: String, title: String, capturedAt: String? = nil, idempotencyKey: String = UUID().uuidString) async throws -> CollabIdResult {
        struct Body: Encodable { let title: String; let capturedAt: String? }
        return try await authorizedPost(path: "v1/moments/\(momentId)/memories", body: Body(title: title, capturedAt: capturedAt), idempotencyKey: idempotencyKey)
    }

    func addGroupParticipant(momentId: String, displayName: String, roleCode: String = "PARTICIPANT", email: String? = nil, phone: String? = nil, idempotencyKey: String = UUID().uuidString) async throws -> CollabIdResult {
        struct Body: Encodable {
            let displayName: String
            let roleCode: String
            let email: String?
            let phone: String?
        }
        struct Result: Decodable {
            let participantId: String
            let momentId: String
        }
        let r: Result = try await authorizedPost(
            path: "v1/moments/\(momentId)/participants",
            body: Body(displayName: displayName, roleCode: roleCode, email: email, phone: phone),
            idempotencyKey: idempotencyKey
        )
        return CollabIdResult(
            planningItemId: nil,
            bookingId: nil,
            pollId: nil,
            updateId: nil,
            memoryId: nil,
            purchaseItemId: nil,
            residentId: nil,
            sharedAssetId: nil,
            maintenanceRecordId: nil,
            momentId: r.momentId
        )
    }

    func createPurchaseItem(momentId: String, label: String, amount: String? = nil, idempotencyKey: String = UUID().uuidString) async throws -> CollabIdResult {
        struct Body: Encodable { let label: String; let amount: String? }
        return try await authorizedPost(path: "v1/moments/\(momentId)/purchase-items", body: Body(label: label, amount: amount), idempotencyKey: idempotencyKey)
    }

    func createDeliveryHandover(
        momentId: String,
        recipientName: String? = nil,
        handoverType: String? = nil,
        scheduledAt: String? = nil,
        address: String? = nil,
        note: String? = nil,
        idempotencyKey: String = UUID().uuidString
    ) async throws -> CollabIdResult {
        struct Body: Encodable {
            let recipientName: String?
            let handoverType: String?
            let scheduledAt: String?
            let address: String?
            let note: String?
        }
        struct Result: Decodable { let deliveryHandoverId: String; let momentId: String }
        let r: Result = try await authorizedPost(
            path: "v1/moments/\(momentId)/delivery-handovers",
            body: Body(recipientName: recipientName, handoverType: handoverType, scheduledAt: scheduledAt, address: address, note: note),
            idempotencyKey: idempotencyKey
        )
        return CollabIdResult(
            planningItemId: nil, bookingId: nil, pollId: nil, updateId: nil, memoryId: nil,
            purchaseItemId: nil, residentId: nil, sharedAssetId: nil, maintenanceRecordId: nil,
            momentId: r.momentId
        )
    }

    func createOwnershipRecord(
        momentId: String,
        assetLabel: String? = nil,
        fromOwnerName: String? = nil,
        toParticipantName: String? = nil,
        ownershipShare: Double? = nil,
        ownershipNote: String? = nil,
        effectiveAt: String? = nil,
        idempotencyKey: String = UUID().uuidString
    ) async throws -> CollabIdResult {
        struct Body: Encodable {
            let assetLabel: String?
            let fromOwnerName: String?
            let toParticipantName: String?
            let ownershipShare: Double?
            let ownershipNote: String?
            let effectiveAt: String?
        }
        struct Result: Decodable { let ownershipRecordId: String; let momentId: String }
        let r: Result = try await authorizedPost(
            path: "v1/moments/\(momentId)/ownership-records",
            body: Body(
                assetLabel: assetLabel,
                fromOwnerName: fromOwnerName,
                toParticipantName: toParticipantName,
                ownershipShare: ownershipShare,
                ownershipNote: ownershipNote,
                effectiveAt: effectiveAt
            ),
            idempotencyKey: idempotencyKey
        )
        return CollabIdResult(
            planningItemId: nil, bookingId: nil, pollId: nil, updateId: nil, memoryId: nil,
            purchaseItemId: nil, residentId: nil, sharedAssetId: nil, maintenanceRecordId: nil,
            momentId: r.momentId
        )
    }

    func addResident(momentId: String, name: String, roleCode: String? = nil, idempotencyKey: String = UUID().uuidString) async throws -> CollabIdResult {
        struct Body: Encodable { let name: String; let roleCode: String? }
        return try await authorizedPost(path: "v1/moments/\(momentId)/residents", body: Body(name: name, roleCode: roleCode), idempotencyKey: idempotencyKey)
    }

    func createLivingRule(momentId: String, title: String, ruleText: String, idempotencyKey: String = UUID().uuidString) async throws -> CollabIdResult {
        struct Body: Encodable { let title: String; let ruleText: String }
        struct Result: Decodable { let livingRuleId: String; let momentId: String }
        let r: Result = try await authorizedPost(
            path: "v1/moments/\(momentId)/living-rules",
            body: Body(title: title, ruleText: ruleText),
            idempotencyKey: idempotencyKey
        )
        return CollabIdResult(
            planningItemId: nil,
            bookingId: nil,
            pollId: nil,
            updateId: nil,
            memoryId: nil,
            purchaseItemId: nil,
            residentId: nil,
            sharedAssetId: nil,
            maintenanceRecordId: nil,
            momentId: r.momentId
        )
    }

    struct CreateGoalResult: Decodable {
        let goalId: String
        let momentId: String
        let title: String
        let version: Int
    }

    func createGoal(
        momentId: String,
        title: String,
        description: String? = nil,
        targetAt: String? = nil,
        idempotencyKey: String = UUID().uuidString
    ) async throws -> CreateGoalResult {
        struct Body: Encodable {
            let title: String
            let description: String?
            let targetAt: String?
        }
        return try await authorizedPost(
            path: "v1/moments/\(momentId)/goals",
            body: Body(title: title, description: description, targetAt: targetAt),
            idempotencyKey: idempotencyKey
        )
    }

    struct CreateTaskResult: Decodable {
        let taskId: String
        let momentId: String
        let title: String
        let version: Int
    }

    func createTask(
        momentId: String,
        title: String,
        description: String? = nil,
        goalId: String? = nil,
        milestoneId: String? = nil,
        dueAt: String? = nil,
        idempotencyKey: String = UUID().uuidString
    ) async throws -> CreateTaskResult {
        struct Body: Encodable {
            let title: String
            let description: String?
            let goalId: String?
            let milestoneId: String?
            let dueAt: String?
        }
        return try await authorizedPost(
            path: "v1/moments/\(momentId)/tasks",
            body: Body(
                title: title,
                description: description,
                goalId: goalId,
                milestoneId: milestoneId,
                dueAt: dueAt
            ),
            idempotencyKey: idempotencyKey
        )
    }

    struct ExecuteActionProposalResult: Decodable {
        let status: String
        let executedResourceId: String?
    }

    func executeActionProposal(
        actionProposalId: String,
        idempotencyKey: String = UUID().uuidString
    ) async throws -> ExecuteActionProposalResult {
        struct EmptyBody: Codable {}
        return try await authorizedPost(
            path: "v1/ai/action-proposals/\(actionProposalId)/execute",
            body: EmptyBody(),
            idempotencyKey: idempotencyKey
        )
    }

    func votePoll(pollId: String, pollOptionId: String, idempotencyKey: String = UUID().uuidString) async throws {
        struct Body: Encodable { let pollOptionId: String }
        struct Result: Decodable { let pollId: String }
        let _: Result = try await authorizedPost(path: "v1/polls/\(pollId)/votes", body: Body(pollOptionId: pollOptionId), idempotencyKey: idempotencyKey)
    }

    func closePoll(pollId: String, idempotencyKey: String = UUID().uuidString) async throws {
        struct EmptyBody: Codable {}
        struct Result: Decodable { let pollId: String; let status: String }
        let _: Result = try await authorizedPost(path: "v1/polls/\(pollId)/close", body: EmptyBody(), idempotencyKey: idempotencyKey)
    }

    func createSharedAsset(
        momentId: String,
        title: String,
        assetType: String? = nil,
        conditionCode: String? = nil,
        idempotencyKey: String = UUID().uuidString
    ) async throws -> CollabIdResult {
        struct Body: Encodable {
            let title: String
            let assetType: String?
            let conditionCode: String?
        }
        return try await authorizedPost(
            path: "v1/moments/\(momentId)/shared-assets",
            body: Body(title: title, assetType: assetType, conditionCode: conditionCode),
            idempotencyKey: idempotencyKey
        )
    }

    func createMaintenanceRecord(
        momentId: String,
        title: String,
        description: String? = nil,
        sharedAssetId: String? = nil,
        idempotencyKey: String = UUID().uuidString
    ) async throws -> CollabIdResult {
        struct Body: Encodable {
            let title: String
            let description: String?
            let sharedAssetId: String?
        }
        return try await authorizedPost(
            path: "v1/moments/\(momentId)/maintenance-records",
            body: Body(title: title, description: description, sharedAssetId: sharedAssetId),
            idempotencyKey: idempotencyKey
        )
    }

    func createGroupVendor(
        momentId: String,
        vendorName: String,
        vendorType: String? = nil,
        phone: String? = nil,
        email: String? = nil,
        notes: String? = nil,
        quotedPrice: String? = nil,
        statusLabel: String? = nil,
        idempotencyKey: String = UUID().uuidString
    ) async throws {
        struct Body: Encodable {
            let vendorName: String
            let vendorType: String?
            let phone: String?
            let email: String?
            let notes: String?
            let quotedPrice: String?
            let statusLabel: String?
        }
        struct Result: Decodable { let vendorId: String?; let momentId: String? }
        let _: Result = try await authorizedPost(
            path: "v1/moments/\(momentId)/vendors",
            body: Body(
                vendorName: vendorName,
                vendorType: vendorType,
                phone: phone,
                email: email,
                notes: notes,
                quotedPrice: quotedPrice,
                statusLabel: statusLabel
            ),
            idempotencyKey: idempotencyKey
        )
    }

    func recordAttendance(
        momentId: String,
        participantId: String,
        attendanceStatus: String,
        note: String? = nil,
        idempotencyKey: String = UUID().uuidString
    ) async throws {
        struct Body: Encodable {
            let participantId: String
            let attendanceStatus: String
            let note: String?
        }
        struct Result: Decodable { let attendanceId: String?; let momentId: String? }
        let _: Result = try await authorizedPost(
            path: "v1/moments/\(momentId)/attendance",
            body: Body(participantId: participantId, attendanceStatus: attendanceStatus, note: note),
            idempotencyKey: idempotencyKey
        )
    }

    // MARK: - S4 Business facets + finance

    struct BusinessFinanceTotalsPayload: Decodable {
        let currencyCode: String
        let expenseTotal: String?
        let revenueTotal: String?
        let invoiceOutstandingTotal: String?
    }

    struct BusinessFinancePayload: Decodable {
        let dataQuality: String?
        let totals: [BusinessFinanceTotalsPayload]?
        let snapshotPayload: [String: AnyDecodable]?
    }

    struct BusinessPulsePayload: Decodable {
        let momentId: String?
        let facet: String?
        let title: String?
        let companyId: String?
        let businessFamily: String?
        let status: String?
        let payload: PulseInner?

        struct PulseInner: Decodable {
            let dataQuality: String?
            let attentionCount: Int?
            let activeMomentCount: Int?
            let runwayMonths: String?
            let financialHealthScore: String?
            let widgetPayload: [String: AnyDecodable]?
            let finance: BusinessFinancePayload?
            /// Bundled pulse preview; when present, skip a second /activity GET.
            let activity: [ActivityItemPayload]?
            /// Ops pulse enrichment (BUSINESS_OPERATIONS only).
            let operations: OperationsExtras?

            struct OperationsExtras: Decodable {
                let monthlySpend: String?
                let activeVendorCount: Int?
                let slaCompliancePct: Int?
                let openIssueCount: Int?
                let spendByCategory: [SpendCategorySlice]?
                let needsAttention: [OpsAttentionItem]?
                let sectionQuality: [String: String]?

                struct SpendCategorySlice: Decodable, Identifiable {
                    var id: String { label }
                    let label: String
                    let pct: Int
                }

                struct OpsAttentionItem: Decodable, Identifiable {
                    var id: String { issueId }
                    let title: String
                    let severity: String
                    let issueId: String
                }
            }
        }
    }

    struct BusinessLifePayload: Decodable {
        let momentId: String?
        let facet: String?
        let title: String?
        let companyId: String?
        let businessFamily: String?
        let status: String?
        let payload: LifeInner?

        struct LifeInner: Decodable {
            let dataQuality: String?
            let sections: [String: String]?
            let teamOperationsPayload: [String: AnyDecodable]?
            let runwayPayload: [String: AnyDecodable]?
            let businessOperationsPayload: [String: AnyDecodable]?
            let vendorOperationsPayload: [String: AnyDecodable]?
            let kpis: LifeKpis?
            let modules: LifeModules?
            let signals: [LifeSignal]?
            let activity: [LifeActivity]?
            let journey: [LifeJourney]?
            let trends: LifeTrends?

            struct LifeKpis: Decodable {
                let activeModuleCount: Int?
                let activeMomentCount: Int?
                let runwayMonths: String?
                let financialHealthScore: String?
                let attentionCount: Int?
            }

            struct LifeModuleCard: Decodable {
                let active: Bool?
                let statusLabel: String?
                let runwayMonths: String?
                let score: String?
                let revenueMomPct: Int?
                let expenseMomPct: Int?
            }

            struct LifeModules: Decodable {
                let teamOperations: LifeModuleCard?
                let runway: LifeModuleCard?
                let businessOperations: LifeModuleCard?
                let vendorOperations: LifeModuleCard?
            }

            struct LifeSignal: Decodable, Identifiable {
                var id: String { signalId }
                let signalId: String
                let signalType: String?
                let title: String
                let family: String?
                let statusLabel: String?
                let severity: String?
                let metricValue: AnyDecodable?
            }

            struct LifeActivity: Decodable, Identifiable {
                var id: String { "\(activityCode)-\(occurredAt)-\(title)" }
                let activityCode: String
                let title: String
                let occurredAt: String
                let family: String?
                let description: String?
            }

            struct LifeJourney: Decodable, Identifiable {
                var id: String { "\(familyCode)-\(createdAt)" }
                let familyCode: String
                let family: String?
                let title: String
                let createdAt: String
            }

            struct LifeTrendPoint: Decodable, Identifiable {
                var id: String { month }
                let month: String
                let financialHealthScore: Int?
                let teamScore: Int?
                let runwayScore: Int?
                let opsScore: Int?
            }

            struct LifeTrends: Decodable {
                let status: String?
                let series: [LifeTrendPoint]?
            }
        }
    }

    struct BusinessWeeklyReportPayload: Decodable {
        let title: String?
        let sections: [BusinessWeeklyReportSection]?
        let generatedAt: String?
        let period: String?
        let note: String?

        struct BusinessWeeklyReportSection: Decodable, Identifiable {
            var id: String { heading ?? UUID().uuidString }
            let heading: String?
            let items: [String]?
        }
    }

    struct BusinessShareLinkPayload: Decodable {
        let shareUrl: String?
        let shareToken: String?
        let expiresAt: String?
        let note: String?
    }

    struct BusinessMemoryPayload: Decodable {
        let momentId: String?
        let facet: String?
        let title: String?
        let companyId: String?
        let businessFamily: String?
        let status: String?
        let payload: MemoryInner?

        struct MemoryInner: Decodable {
            let dataQuality: String?
            let items: [BusinessMemoryItem]?
            let memoryCount: Int?

            struct BusinessMemoryItem: Decodable, Identifiable {
                var id: String { memoryId ?? title ?? "memory" }
                let memoryId: String?
                let title: String?
            }
        }
    }

    struct BusinessFinanceFacetPayload: Decodable {
        let momentId: String?
        let facet: String?
        let title: String?
        let companyId: String?
        let businessFamily: String?
        let status: String?
        let payload: BusinessFinancePayload?
    }

    struct BusinessActionsPayload: Decodable {
        let momentId: String?
        let companyId: String?
        let businessFamily: String?
        let availableActions: [BusinessActionItem]?

        struct BusinessActionItem: Decodable {
            let actionCode: String
            let label: String?
            let enabled: Bool?
        }
    }

    struct CompanyMemberPayload: Decodable, Identifiable {
        var id: String { membershipId }
        let membershipId: String
        let userId: String
        let membershipType: String
        let status: String
        let displayName: String?
    }

    struct CompanyMembersPayload: Decodable {
        let companyId: String?
        let members: [CompanyMemberPayload]
    }

    struct CreateBusinessExpenseResult: Decodable {
        let expenseId: String
        let momentId: String
        let companyId: String?
        let amount: String
        let currencyCode: String
        let categoryCode: String?
        let status: String
        let approvalRequestId: String?
        let version: Int?
    }

    struct CreateBusinessRevenueResult: Decodable {
        let revenueId: String
        let companyId: String?
        let amount: String
        let currencyCode: String
        let status: String
    }

    struct CreateBusinessInvoiceResult: Decodable {
        let invoiceId: String
        let companyId: String?
        let invoiceNumber: String
        let subtotalAmount: String?
        let taxAmount: String?
        let totalAmount: String?
        let status: String
    }

    struct DecideApprovalResult: Decodable {
        let approvalRequestId: String
        let decision: String
        let expenseId: String?
        let expenseStatus: String?
    }

    struct AddCompanyMemberResult: Decodable {
        let membershipId: String
        let companyId: String
        let userId: String
        let membershipType: String
        let status: String
    }

    struct BusinessInvoiceLineInput: Encodable {
        let description: String
        let quantity: String
        let unitPrice: String
        let taxAmount: String?
    }

    struct ForecastLineInput: Encodable {
        let lineLabel: String
        let amount: String
        let currencyCode: String?
        let periodLabel: String?
    }

    struct BusinessCapacityPayload: Decodable {
        let capacityPct: Int?
        let note: String?
    }

    struct BusinessWorkloadDepartmentPayload: Decodable {
        let name: String
        let count: Int
    }

    struct BusinessWorkloadPayload: Decodable {
        let byDepartment: [BusinessWorkloadDepartmentPayload]
        let note: String?

        init(from decoder: Decoder) throws {
            let c = try decoder.container(keyedBy: CodingKeys.self)
            byDepartment = try c.decodeIfPresent([BusinessWorkloadDepartmentPayload].self, forKey: .byDepartment) ?? []
            note = try c.decodeIfPresent(String.self, forKey: .note)
        }

        private enum CodingKeys: String, CodingKey {
            case byDepartment
            case note
        }
    }

    func getBusinessCapacity(momentId: String) async throws -> BusinessCapacityPayload {
        try await authorizedGet(path: "v1/business/moments/\(momentId)/capacity")
    }

    func getBusinessWorkload(momentId: String) async throws -> BusinessWorkloadPayload {
        try await authorizedGet(path: "v1/business/moments/\(momentId)/workload")
    }

    func getBusinessPulse(momentId: String) async throws -> BusinessPulsePayload {
        try await authorizedGet(path: "v1/business/moments/\(momentId)/pulse")
    }

    func getBusinessLife(momentId: String) async throws -> BusinessLifePayload {
        try await authorizedGet(path: "v1/business/moments/\(momentId)/life")
    }

    func getBusinessWeeklyReport(momentId: String, period: String = "7d") async throws -> BusinessWeeklyReportPayload {
        try await authorizedGet(
            path: "v1/business/moments/\(momentId)/weekly-report",
            query: ["period": period]
        )
    }

    func createBusinessShareLink(momentId: String) async throws -> BusinessShareLinkPayload {
        struct EmptyBody: Codable {}
        return try await authorizedPost(
            path: "v1/business/moments/\(momentId)/share-link",
            body: EmptyBody(),
            idempotencyKey: UUID().uuidString
        )
    }

    func getBusinessMemory(momentId: String) async throws -> BusinessMemoryPayload {
        try await authorizedGet(path: "v1/business/moments/\(momentId)/memory")
    }

    func getBusinessFinance(momentId: String) async throws -> BusinessFinanceFacetPayload {
        try await authorizedGet(path: "v1/business/moments/\(momentId)/finance")
    }

    func getBusinessActions(momentId: String) async throws -> BusinessActionsPayload {
        try await authorizedGet(path: "v1/business/moments/\(momentId)/actions")
    }

    struct BusinessProjectionPayload: Decodable {
        let momentId: String?
        let payload: AnyDecodable?
        let items: [AnyDecodable]?
    }

    func getBusinessMomDeltas(momentId: String) async throws -> BusinessProjectionPayload {
        try await authorizedGet(path: "v1/business/moments/\(momentId)/mom-deltas")
    }

    func getBusinessProgressSnapshot(momentId: String) async throws -> BusinessProjectionPayload {
        try await authorizedGet(path: "v1/business/moments/\(momentId)/progress-snapshot")
    }

    func getBusinessRoster(momentId: String) async throws -> BusinessProjectionPayload {
        try await authorizedGet(path: "v1/business/moments/\(momentId)/roster")
    }

    func listBusinessExpenses(momentId: String) async throws -> BusinessProjectionPayload {
        try await authorizedGet(path: "v1/business/moments/\(momentId)/expenses")
    }

    func listBusinessRevenues(momentId: String) async throws -> BusinessProjectionPayload {
        try await authorizedGet(path: "v1/business/moments/\(momentId)/revenues")
    }

    func listBusinessInvoices(momentId: String) async throws -> BusinessProjectionPayload {
        try await authorizedGet(path: "v1/business/moments/\(momentId)/invoices")
    }

    func listBusinessIssues(momentId: String) async throws -> BusinessProjectionPayload {
        try await authorizedGet(path: "v1/business/moments/\(momentId)/issues")
    }

    func listBusinessImprovements(momentId: String) async throws -> BusinessProjectionPayload {
        try await authorizedGet(path: "v1/business/moments/\(momentId)/improvements")
    }

    func listBusinessUpdates(momentId: String) async throws -> BusinessProjectionPayload {
        try await authorizedGet(path: "v1/business/moments/\(momentId)/updates")
    }

    func listBusinessApprovals(momentId: String) async throws -> BusinessProjectionPayload {
        try await authorizedGet(path: "v1/business/moments/\(momentId)/approvals")
    }

    func listBusinessMemories(momentId: String) async throws -> BusinessProjectionPayload {
        try await authorizedGet(path: "v1/business/moments/\(momentId)/memories")
    }

    func listBusinessActivity(momentId: String, limit: Int = 20) async throws -> [ActivityItemPayload] {
        let page: CursorPagePayload<ActivityItemPayload> =
            try await authorizedGet(
                path: "v1/business/moments/\(momentId)/activity",
                query: ["limit": String(limit)]
            )
        return page.items
    }

    struct BusinessTimelineItem: Decodable, Identifiable {
        var id: String { eventId }
        let eventId: String
        let eventType: String
        let title: String
        let category: String
        let description: String?
        let occurredAt: String
    }

    struct BusinessTimelineKpis: Decodable {
        let spendEvents: Int?
        let issueCount: Int?
        let highPriorityIssues: Int?
        let updateCount: Int?
        let activeContracts: Int?
        let vendorCount: Int?
    }

    struct BusinessTimelinePayload: Decodable {
        let momentId: String
        let companyId: String
        let items: [BusinessTimelineItem]
        let kpis: BusinessTimelineKpis?
    }

    func getBusinessMomentTimeline(momentId: String, limit: Int = 50) async throws -> BusinessTimelinePayload {
        try await authorizedGet(
            path: "v1/business/moments/\(momentId)/moments",
            query: ["limit": String(limit)]
        )
    }

    func createBusinessMemory(
        momentId: String,
        title: String,
        body: String? = nil,
        memoryType: String? = nil,
        idempotencyKey: String = UUID().uuidString
    ) async throws -> BusinessCreateMemoryResult {
        struct Body: Encodable {
            let title: String
            let body: String?
            let memoryType: String?
        }
        return try await authorizedPost(
            path: "v1/business/moments/\(momentId)/memories",
            body: Body(title: title, body: body, memoryType: memoryType),
            idempotencyKey: idempotencyKey
        )
    }

    struct BusinessCreateMemoryResult: Decodable {
        let memoryId: String
        let momentId: String
        let title: String
    }

    func createBusinessExpense(
        momentId: String,
        amount: String,
        currencyCode: String,
        description: String? = nil,
        merchantName: String? = nil,
        categoryCode: String? = nil,
        vendorId: String? = nil,
        idempotencyKey: String = UUID().uuidString
    ) async throws -> CreateBusinessExpenseResult {
        struct Body: Encodable {
            let amount: String
            let currencyCode: String
            let description: String?
            let merchantName: String?
            let categoryCode: String?
            let vendorId: String?
        }
        return try await authorizedPost(
            path: "v1/moments/\(momentId)/business-expenses",
            body: Body(
                amount: amount,
                currencyCode: currencyCode,
                description: description,
                merchantName: merchantName,
                categoryCode: categoryCode,
                vendorId: vendorId
            ),
            idempotencyKey: idempotencyKey
        )
    }

    func createBusinessRevenue(
        momentId: String,
        amount: String,
        currencyCode: String,
        description: String? = nil,
        categoryCode: String? = nil,
        idempotencyKey: String = UUID().uuidString
    ) async throws -> CreateBusinessRevenueResult {
        struct Body: Encodable {
            let amount: String
            let currencyCode: String
            let description: String?
            let categoryCode: String?
        }
        return try await authorizedPost(
            path: "v1/moments/\(momentId)/revenues",
            body: Body(
                amount: amount,
                currencyCode: currencyCode,
                description: description,
                categoryCode: categoryCode
            ),
            idempotencyKey: idempotencyKey
        )
    }

    func createBusinessInvoice(
        momentId: String,
        invoiceNumber: String,
        invoiceDate: String,
        dueDate: String? = nil,
        currencyCode: String,
        lines: [BusinessInvoiceLineInput],
        idempotencyKey: String = UUID().uuidString
    ) async throws -> CreateBusinessInvoiceResult {
        struct Body: Encodable {
            let invoiceNumber: String
            let invoiceDate: String
            let dueDate: String?
            let currencyCode: String
            let lines: [BusinessInvoiceLineInput]
        }
        return try await authorizedPost(
            path: "v1/moments/\(momentId)/invoices",
            body: Body(
                invoiceNumber: invoiceNumber,
                invoiceDate: invoiceDate,
                dueDate: dueDate,
                currencyCode: currencyCode,
                lines: lines
            ),
            idempotencyKey: idempotencyKey
        )
    }

    func decideApproval(
        approvalRequestId: String,
        decision: String,
        reason: String? = nil,
        idempotencyKey: String = UUID().uuidString
    ) async throws -> DecideApprovalResult {
        struct Body: Encodable {
            let decision: String
            let reason: String?
        }
        return try await authorizedPost(
            path: "v1/approvals/\(approvalRequestId)/decide",
            body: Body(decision: decision, reason: reason),
            idempotencyKey: idempotencyKey
        )
    }

    func listCompanyMembers(companyId: String) async throws -> [CompanyMemberPayload] {
        let page: CompanyMembersPayload =
            try await authorizedGet(path: "v1/companies/\(companyId)/members")
        return page.members
    }

    func addCompanyMember(
        companyId: String,
        userId: String,
        membershipType: String = "MEMBER",
        idempotencyKey: String = UUID().uuidString
    ) async throws -> AddCompanyMemberResult {
        struct Body: Encodable {
            let userId: String
            let membershipType: String
        }
        return try await authorizedPost(
            path: "v1/companies/\(companyId)/members",
            body: Body(userId: userId, membershipType: membershipType),
            idempotencyKey: idempotencyKey
        )
    }

    // MARK: - Business Operations writers

    struct CreateVendorResult: Decodable {
        let vendorId: String
        let companyId: String?
        let name: String?
    }

    struct UpdateVendorResult: Decodable {
        let vendorId: String
        let companyId: String?
    }

    struct CreateVendorContractResult: Decodable {
        let vendorContractId: String
    }

    struct CreateSlaDefinitionResult: Decodable {
        let slaDefinitionId: String
    }

    struct CreateSlaCheckResult: Decodable {
        let slaCheckId: String
        let vendorId: String?
    }

    struct CreateIssueResult: Decodable {
        let issueId: String
        let momentId: String?
    }

    struct CreateImprovementResult: Decodable {
        let improvementId: String
        let momentId: String?
    }

    struct CreateBusinessUpdateResult: Decodable {
        let updateId: String
        let momentId: String?
    }

    struct CreateApprovalRequestResult: Decodable {
        let approvalRequestId: String
        let momentId: String?
    }

    // MARK: - Business Deployment Closure result types

    struct CreateMilestoneResult: Decodable {
        let milestoneId: String
        let momentId: String?
    }

    struct CreateRiskResult: Decodable {
        let riskId: String
        let momentId: String?
    }

    struct CreateTaxObligationResult: Decodable {
        let taxObligationId: String
        let momentId: String?
    }

    struct CreateForecastScenarioResult: Decodable {
        let forecastScenarioId: String
        let momentId: String?
    }

    struct CreateInvestorUpdateResult: Decodable {
        let investorUpdateId: String
        let momentId: String?
    }

    struct CreateBudgetAlertResult: Decodable {
        let budgetAlertId: String
        let momentId: String?
    }

    struct CreateBusinessReviewResult: Decodable {
        let businessReviewId: String
        let momentId: String?
    }

    struct CreateDecisionResult: Decodable {
        let decisionId: String
        let momentId: String?
    }

    struct CreateMeetingRecordResult: Decodable {
        let meetingRecordId: String
        let momentId: String?
    }

    struct CreateRecognitionResult: Decodable {
        let recognitionId: String
        let momentId: String?
    }

    struct CreateRetrospectiveResult: Decodable {
        let retrospectiveId: String
        let momentId: String?
    }

    struct CreateActivityLogEntryResult: Decodable {
        let activityLogEntryId: String
        let momentId: String?
    }

    struct VendorItem: Decodable {
        let vendorId: String
        let name: String
        let vendorType: String?
        let status: String?
    }

    struct VendorListResult: Decodable {
        let items: [VendorItem]
    }

    // MARK: - Business Deployment Closure POST methods

    func createMilestone(
        momentId: String,
        title: String,
        targetAt: String? = nil,
        status: String? = nil,
        idempotencyKey: String = UUID().uuidString
    ) async throws -> CreateMilestoneResult {
        struct Body: Encodable { let title: String; let targetAt: String?; let status: String? }
        return try await authorizedPost(
            path: "v1/moments/\(momentId)/milestones",
            body: Body(title: title, targetAt: targetAt, status: status),
            idempotencyKey: idempotencyKey
        )
    }

    func createRisk(
        momentId: String,
        title: String,
        description: String? = nil,
        likelihood: String? = nil,
        impact: String? = nil,
        mitigationText: String? = nil,
        idempotencyKey: String = UUID().uuidString
    ) async throws -> CreateRiskResult {
        struct Body: Encodable { let title: String; let description: String?; let likelihood: String?; let impact: String?; let mitigationText: String? }
        return try await authorizedPost(
            path: "v1/moments/\(momentId)/risks",
            body: Body(title: title, description: description, likelihood: likelihood, impact: impact, mitigationText: mitigationText),
            idempotencyKey: idempotencyKey
        )
    }

    func createTaxObligation(
        momentId: String,
        title: String,
        taxType: String? = nil,
        amount: String? = nil,
        currencyCode: String? = nil,
        dueDate: String? = nil,
        notes: String? = nil,
        idempotencyKey: String = UUID().uuidString
    ) async throws -> CreateTaxObligationResult {
        struct Body: Encodable { let title: String; let taxType: String?; let amount: String?; let currencyCode: String?; let dueDate: String?; let notes: String? }
        return try await authorizedPost(
            path: "v1/moments/\(momentId)/tax-obligations",
            body: Body(title: title, taxType: taxType, amount: amount, currencyCode: currencyCode, dueDate: dueDate, notes: notes),
            idempotencyKey: idempotencyKey
        )
    }

    func createForecastScenario(
        momentId: String,
        name: String,
        horizonMonths: Int? = nil,
        assumptions: String? = nil,
        lines: [ForecastLineInput]? = nil,
        idempotencyKey: String = UUID().uuidString
    ) async throws -> CreateForecastScenarioResult {
        struct Body: Encodable { let name: String; let horizonMonths: Int?; let assumptions: String?; let lines: [ForecastLineInput]? }
        return try await authorizedPost(
            path: "v1/moments/\(momentId)/forecast-scenarios",
            body: Body(name: name, horizonMonths: horizonMonths, assumptions: assumptions, lines: lines),
            idempotencyKey: idempotencyKey
        )
    }

    func createInvestorUpdate(
        momentId: String,
        updateType: String? = nil,
        subject: String,
        keyMetrics: String? = nil,
        runwayStatus: String? = nil,
        highlights: String? = nil,
        nextSteps: String? = nil,
        idempotencyKey: String = UUID().uuidString
    ) async throws -> CreateInvestorUpdateResult {
        struct Body: Encodable { let updateType: String?; let subject: String; let keyMetrics: String?; let runwayStatus: String?; let highlights: String?; let nextSteps: String? }
        return try await authorizedPost(
            path: "v1/moments/\(momentId)/investor-updates",
            body: Body(updateType: updateType, subject: subject, keyMetrics: keyMetrics, runwayStatus: runwayStatus, highlights: highlights, nextSteps: nextSteps),
            idempotencyKey: idempotencyKey
        )
    }

    func createBudgetAlert(
        momentId: String,
        title: String,
        metricLabel: String? = nil,
        thresholdValue: String? = nil,
        currencyCode: String? = nil,
        severity: String? = nil,
        note: String? = nil,
        idempotencyKey: String = UUID().uuidString
    ) async throws -> CreateBudgetAlertResult {
        struct Body: Encodable { let title: String; let metricLabel: String?; let thresholdValue: String?; let currencyCode: String?; let severity: String?; let note: String? }
        return try await authorizedPost(
            path: "v1/moments/\(momentId)/budget-alerts",
            body: Body(title: title, metricLabel: metricLabel, thresholdValue: thresholdValue, currencyCode: currencyCode, severity: severity, note: note),
            idempotencyKey: idempotencyKey
        )
    }

    func createBusinessReview(
        momentId: String,
        period: String? = nil,
        summary: String,
        outcome: String? = nil,
        idempotencyKey: String = UUID().uuidString
    ) async throws -> CreateBusinessReviewResult {
        struct Body: Encodable { let period: String?; let summary: String; let outcome: String? }
        return try await authorizedPost(
            path: "v1/moments/\(momentId)/business-reviews",
            body: Body(period: period, summary: summary, outcome: outcome),
            idempotencyKey: idempotencyKey
        )
    }

    func createDecision(
        momentId: String,
        title: String,
        decisionText: String,
        rationale: String? = nil,
        idempotencyKey: String = UUID().uuidString
    ) async throws -> CreateDecisionResult {
        struct Body: Encodable { let title: String; let decisionText: String; let rationale: String? }
        return try await authorizedPost(
            path: "v1/moments/\(momentId)/decisions",
            body: Body(title: title, decisionText: decisionText, rationale: rationale),
            idempotencyKey: idempotencyKey
        )
    }

    func createMeetingRecord(
        momentId: String,
        title: String,
        meetingAt: String? = nil,
        attendeesText: String? = nil,
        notes: String? = nil,
        decisionsText: String? = nil,
        idempotencyKey: String = UUID().uuidString
    ) async throws -> CreateMeetingRecordResult {
        struct Body: Encodable { let title: String; let meetingAt: String?; let attendeesText: String?; let notes: String?; let decisionsText: String? }
        return try await authorizedPost(
            path: "v1/moments/\(momentId)/meeting-records",
            body: Body(title: title, meetingAt: meetingAt, attendeesText: attendeesText, notes: notes, decisionsText: decisionsText),
            idempotencyKey: idempotencyKey
        )
    }

    func createRecognition(
        momentId: String,
        recipientName: String,
        recognitionType: String? = nil,
        whyText: String,
        idempotencyKey: String = UUID().uuidString
    ) async throws -> CreateRecognitionResult {
        struct Body: Encodable { let recipientName: String; let recognitionType: String?; let whyText: String }
        return try await authorizedPost(
            path: "v1/moments/\(momentId)/recognitions",
            body: Body(recipientName: recipientName, recognitionType: recognitionType, whyText: whyText),
            idempotencyKey: idempotencyKey
        )
    }

    func createRetrospective(
        momentId: String,
        wentWell: String? = nil,
        improveNext: String? = nil,
        idempotencyKey: String = UUID().uuidString
    ) async throws -> CreateRetrospectiveResult {
        struct Body: Encodable { let wentWell: String?; let improveNext: String? }
        return try await authorizedPost(
            path: "v1/moments/\(momentId)/retrospectives",
            body: Body(wentWell: wentWell, improveNext: improveNext),
            idempotencyKey: idempotencyKey
        )
    }

    func createActivityLogEntry(
        momentId: String,
        title: String,
        ownerLabel: String? = nil,
        categoryCode: String? = nil,
        idempotencyKey: String = UUID().uuidString
    ) async throws -> CreateActivityLogEntryResult {
        struct Body: Encodable { let title: String; let ownerLabel: String?; let categoryCode: String? }
        return try await authorizedPost(
            path: "v1/moments/\(momentId)/activity-log-entries",
            body: Body(title: title, ownerLabel: ownerLabel, categoryCode: categoryCode),
            idempotencyKey: idempotencyKey
        )
    }

    func listCompanyVendors(companyId: String) async throws -> VendorListResult {
        return try await authorizedGet(path: "v1/companies/\(companyId)/vendors")
    }

    func createCompanyVendor(
        companyId: String,
        name: String,
        vendorType: String? = nil,
        idempotencyKey: String = UUID().uuidString
    ) async throws -> CreateVendorResult {
        struct Body: Encodable {
            let name: String
            let vendorType: String?
        }
        return try await authorizedPost(
            path: "v1/companies/\(companyId)/vendors",
            body: Body(name: name, vendorType: vendorType),
            idempotencyKey: idempotencyKey
        )
    }

    func patchCompanyVendor(
        companyId: String,
        vendorId: String,
        name: String? = nil,
        vendorType: String? = nil,
        status: String? = nil,
        note: String? = nil,
        idempotencyKey: String = UUID().uuidString
    ) async throws -> UpdateVendorResult {
        struct Body: Encodable {
            let name: String?
            let vendorType: String?
            let status: String?
            let note: String?
        }
        return try await authorizedPatch(
            path: "v1/companies/\(companyId)/vendors/\(vendorId)",
            body: Body(name: name, vendorType: vendorType, status: status, note: note),
            idempotencyKey: idempotencyKey
        )
    }

    func createVendorContract(
        companyId: String,
        vendorId: String,
        contractName: String,
        contractReference: String? = nil,
        startDate: String? = nil,
        endDate: String? = nil,
        contractValue: String? = nil,
        currencyCode: String? = nil,
        idempotencyKey: String = UUID().uuidString
    ) async throws -> CreateVendorContractResult {
        struct Body: Encodable {
            let contractName: String
            let contractReference: String?
            let startDate: String?
            let endDate: String?
            let contractValue: String?
            let currencyCode: String?
        }
        return try await authorizedPost(
            path: "v1/companies/\(companyId)/vendors/\(vendorId)/contracts",
            body: Body(
                contractName: contractName,
                contractReference: contractReference,
                startDate: startDate,
                endDate: endDate,
                contractValue: contractValue,
                currencyCode: currencyCode
            ),
            idempotencyKey: idempotencyKey
        )
    }

    func createSlaDefinition(
        companyId: String,
        vendorId: String,
        name: String,
        metricCode: String,
        comparator: String,
        targetValue: Double? = nil,
        unitCode: String? = nil,
        measurementPeriod: String? = nil,
        vendorContractId: String? = nil,
        idempotencyKey: String = UUID().uuidString
    ) async throws -> CreateSlaDefinitionResult {
        struct Body: Encodable {
            let name: String
            let metricCode: String
            let targetValue: Double?
            let comparator: String
            let unitCode: String?
            let measurementPeriod: String?
            let vendorContractId: String?
        }
        return try await authorizedPost(
            path: "v1/companies/\(companyId)/vendors/\(vendorId)/sla-definitions",
            body: Body(
                name: name,
                metricCode: metricCode,
                targetValue: targetValue,
                comparator: comparator,
                unitCode: unitCode,
                measurementPeriod: measurementPeriod,
                vendorContractId: vendorContractId
            ),
            idempotencyKey: idempotencyKey
        )
    }

    func createSlaCheck(
        companyId: String,
        slaDefinitionId: String,
        result: String,
        observedValue: Double? = nil,
        note: String? = nil,
        idempotencyKey: String = UUID().uuidString
    ) async throws -> CreateSlaCheckResult {
        struct Body: Encodable {
            let result: String
            let observedValue: Double?
            let note: String?
        }
        return try await authorizedPost(
            path: "v1/companies/\(companyId)/sla-definitions/\(slaDefinitionId)/checks",
            body: Body(result: result, observedValue: observedValue, note: note),
            idempotencyKey: idempotencyKey
        )
    }

    func createBusinessIssue(
        momentId: String,
        title: String,
        description: String? = nil,
        severity: String? = nil,
        vendorId: String? = nil,
        idempotencyKey: String = UUID().uuidString
    ) async throws -> CreateIssueResult {
        struct Body: Encodable {
            let title: String
            let description: String?
            let severity: String?
            let vendorId: String?
        }
        return try await authorizedPost(
            path: "v1/moments/\(momentId)/issues",
            body: Body(title: title, description: description, severity: severity, vendorId: vendorId),
            idempotencyKey: idempotencyKey
        )
    }

    struct CreateIssueEvidenceResult: Decodable {
        let evidenceId: String
        let issueId: String?
    }

    func createIssueEvidence(
        momentId: String,
        issueId: String,
        note: String? = nil,
        url: String? = nil,
        idempotencyKey: String = UUID().uuidString
    ) async throws -> CreateIssueEvidenceResult {
        struct Body: Encodable {
            let note: String?
            let url: String?
        }
        return try await authorizedPost(
            path: "v1/moments/\(momentId)/issues/\(issueId)/evidence",
            body: Body(note: note, url: url),
            idempotencyKey: idempotencyKey
        )
    }

    func createBusinessImprovement(
        momentId: String,
        title: String,
        description: String? = nil,
        categoryCode: String? = nil,
        impactEstimate: String? = nil,
        idempotencyKey: String = UUID().uuidString
    ) async throws -> CreateImprovementResult {
        struct Body: Encodable {
            let title: String
            let description: String?
            let categoryCode: String?
            let impactEstimate: String?
        }
        return try await authorizedPost(
            path: "v1/moments/\(momentId)/improvements",
            body: Body(
                title: title,
                description: description,
                categoryCode: categoryCode,
                impactEstimate: impactEstimate
            ),
            idempotencyKey: idempotencyKey
        )
    }

    func createBusinessUpdate(
        momentId: String,
        body: String,
        title: String? = nil,
        idempotencyKey: String = UUID().uuidString
    ) async throws -> CreateBusinessUpdateResult {
        struct Body: Encodable {
            let title: String?
            let body: String
        }
        return try await authorizedPost(
            path: "v1/moments/\(momentId)/business-updates",
            body: Body(title: title, body: body),
            idempotencyKey: idempotencyKey
        )
    }

    func createBusinessApprovalRequest(
        momentId: String,
        title: String,
        amount: String? = nil,
        currencyCode: String? = nil,
        note: String? = nil,
        idempotencyKey: String = UUID().uuidString
    ) async throws -> CreateApprovalRequestResult {
        struct Body: Encodable {
            let title: String
            let amount: String?
            let currencyCode: String?
            let note: String?
        }
        return try await authorizedPost(
            path: "v1/moments/\(momentId)/approval-requests",
            body: Body(title: title, amount: amount, currencyCode: currencyCode, note: note),
            idempotencyKey: idempotencyKey
        )
    }

    struct PersonalProjectionPayload: Decodable {
        let momentId: String?
        let payload: AnyDecodable?
        let items: [AnyDecodable]?
    }

    struct ExpenseAttachmentsPayload: Decodable {
        let items: [AnyDecodable]?
    }

    func listExpenseAttachments(momentId: String, expenseId: String) async throws -> ExpenseAttachmentsPayload {
        try await authorizedGet(path: "v1/moments/\(momentId)/expenses/\(expenseId)/attachments")
    }

    func deleteExpenseAttachment(
        momentId: String,
        expenseId: String,
        uploadId: String,
        idempotencyKey: String = UUID().uuidString
    ) async throws {
        struct EmptyBody: Codable {}
        let _: EmptyBody = try await authorizedDelete(
            path: "v1/moments/\(momentId)/expenses/\(expenseId)/attachments/\(uploadId)",
            body: EmptyBody(),
            idempotencyKey: idempotencyKey
        )
    }

    func getCompany(companyId: String) async throws -> PersonalProjectionPayload {
        try await authorizedGet(path: "v1/companies/\(companyId)")
    }

    func patchCompany(
        companyId: String,
        body: [String: Any],
        idempotencyKey: String = UUID().uuidString
    ) async throws -> PersonalProjectionPayload {
        struct Body: Encodable {
            let values: [String: JSONEncodableValue]
            init(_ dict: [String: Any]) { values = JSONEncodableValue.map(dict) }
            func encode(to encoder: Encoder) throws {
                var container = encoder.singleValueContainer()
                try container.encode(values)
            }
        }
        return try await authorizedPatch(
            path: "v1/companies/\(companyId)",
            body: Body(body),
            idempotencyKey: idempotencyKey
        )
    }

    func patchLocation(
        companyId: String,
        locationId: String,
        body: [String: Any],
        idempotencyKey: String = UUID().uuidString
    ) async throws -> PersonalProjectionPayload {
        struct Body: Encodable {
            let values: [String: JSONEncodableValue]
            init(_ dict: [String: Any]) { values = JSONEncodableValue.map(dict) }
            func encode(to encoder: Encoder) throws {
                var container = encoder.singleValueContainer()
                try container.encode(values)
            }
        }
        return try await authorizedPatch(
            path: "v1/companies/\(companyId)/locations/\(locationId)",
            body: Body(body),
            idempotencyKey: idempotencyKey
        )
    }

    func listTeams(companyId: String) async throws -> PersonalProjectionPayload {
        try await authorizedGet(path: "v1/companies/\(companyId)/teams")
    }

    func createTeam(
        companyId: String,
        body: [String: Any],
        idempotencyKey: String = UUID().uuidString
    ) async throws -> PersonalProjectionPayload {
        struct Body: Encodable {
            let values: [String: JSONEncodableValue]
            init(_ dict: [String: Any]) { values = JSONEncodableValue.map(dict) }
            func encode(to encoder: Encoder) throws {
                var container = encoder.singleValueContainer()
                try container.encode(values)
            }
        }
        return try await authorizedPost(
            path: "v1/companies/\(companyId)/teams",
            body: Body(body),
            idempotencyKey: idempotencyKey
        )
    }

    func getMomentActivity(momentId: String, limit: Int = 20) async throws -> [ActivityItemPayload] {
        let page: CursorPagePayload<ActivityItemPayload> =
            try await authorizedGet(path: "v1/moments/\(momentId)/activity", query: ["limit": String(limit)])
        return page.items
    }

    func getIncome(momentId: String, incomeId: String) async throws -> PersonalProjectionPayload {
        try await authorizedGet(path: "v1/moments/\(momentId)/income/\(incomeId)")
    }

    func patchIncome(
        momentId: String,
        incomeId: String,
        body: [String: Any],
        idempotencyKey: String = UUID().uuidString
    ) async throws -> PersonalProjectionPayload {
        struct Body: Encodable {
            let values: [String: JSONEncodableValue]
            init(_ dict: [String: Any]) { values = JSONEncodableValue.map(dict) }
            func encode(to encoder: Encoder) throws {
                var container = encoder.singleValueContainer()
                try container.encode(values)
            }
        }
        return try await authorizedPatch(
            path: "v1/moments/\(momentId)/income/\(incomeId)",
            body: Body(body),
            idempotencyKey: idempotencyKey
        )
    }

    func getPersonalMoodHistory(momentId: String) async throws -> PersonalProjectionPayload {
        try await authorizedGet(path: "v1/personal/moments/\(momentId)/mood-history")
    }

    func getPersonalAdjustmentInsight(momentId: String) async throws -> PersonalProjectionPayload {
        try await authorizedGet(path: "v1/personal/moments/\(momentId)/adjustment-insight")
    }

    func getPersonalActivitySummary(momentId: String) async throws -> PersonalProjectionPayload {
        try await authorizedGet(path: "v1/personal/moments/\(momentId)/activity-summary")
    }

    func getPersonalMoneyJourney(momentId: String) async throws -> PersonalProjectionPayload {
        try await authorizedGet(path: "v1/personal/moments/\(momentId)/money-journey")
    }

    func getPersonalFutureRuntimeSummary(momentId: String) async throws -> PersonalProjectionPayload {
        try await authorizedGet(path: "v1/personal/moments/\(momentId)/future-runtime-summary")
    }

    func getPersonalFutureInventory(momentId: String) async throws -> PersonalProjectionPayload {
        try await authorizedGet(path: "v1/personal/moments/\(momentId)/future-inventory")
    }

    func getPersonalFutureJourney(momentId: String) async throws -> PersonalProjectionPayload {
        try await authorizedGet(path: "v1/personal/moments/\(momentId)/future-journey")
    }

    func patchPersonalFutureProfile(
        momentId: String,
        body: [String: Any],
        idempotencyKey: String = UUID().uuidString
    ) async throws -> PersonalProjectionPayload {
        struct Body: Encodable {
            let values: [String: JSONEncodableValue]
            init(_ dict: [String: Any]) { values = JSONEncodableValue.map(dict) }
            func encode(to encoder: Encoder) throws {
                var container = encoder.singleValueContainer()
                try container.encode(values)
            }
        }
        return try await authorizedPatch(
            path: "v1/personal/moments/\(momentId)/future-profile",
            body: Body(body),
            idempotencyKey: idempotencyKey
        )
    }

    func getPersonalLifestyleRuntimeSummary(momentId: String) async throws -> PersonalProjectionPayload {
        try await authorizedGet(path: "v1/personal/moments/\(momentId)/lifestyle-runtime-summary")
    }

    func getPersonalLifestyleInventory(momentId: String) async throws -> PersonalProjectionPayload {
        try await authorizedGet(path: "v1/personal/moments/\(momentId)/lifestyle-inventory")
    }

    func getPersonalLifestyleJourney(momentId: String) async throws -> PersonalProjectionPayload {
        try await authorizedGet(path: "v1/personal/moments/\(momentId)/lifestyle-journey")
    }

    func patchPersonalLifestyleProfile(
        momentId: String,
        body: [String: Any],
        idempotencyKey: String = UUID().uuidString
    ) async throws -> PersonalProjectionPayload {
        struct Body: Encodable {
            let values: [String: JSONEncodableValue]
            init(_ dict: [String: Any]) { values = JSONEncodableValue.map(dict) }
            func encode(to encoder: Encoder) throws {
                var container = encoder.singleValueContainer()
                try container.encode(values)
            }
        }
        return try await authorizedPatch(
            path: "v1/personal/moments/\(momentId)/lifestyle-profile",
            body: Body(body),
            idempotencyKey: idempotencyKey
        )
    }

    func getPersonalRelationshipsRuntimeSummary(momentId: String) async throws -> PersonalProjectionPayload {
        try await authorizedGet(path: "v1/personal/moments/\(momentId)/relationships-runtime-summary")
    }

    func getPersonalRelationshipsConnections(momentId: String) async throws -> PersonalProjectionPayload {
        try await authorizedGet(path: "v1/personal/moments/\(momentId)/relationships-connections")
    }

    func getPersonalRelationshipsJourney(momentId: String) async throws -> PersonalProjectionPayload {
        try await authorizedGet(path: "v1/personal/moments/\(momentId)/relationships-journey")
    }

    func patchPersonalRelationshipsProfile(
        momentId: String,
        body: [String: Any],
        idempotencyKey: String = UUID().uuidString
    ) async throws -> PersonalProjectionPayload {
        struct Body: Encodable {
            let values: [String: JSONEncodableValue]
            init(_ dict: [String: Any]) { values = JSONEncodableValue.map(dict) }
            func encode(to encoder: Encoder) throws {
                var container = encoder.singleValueContainer()
                try container.encode(values)
            }
        }
        return try await authorizedPatch(
            path: "v1/personal/moments/\(momentId)/relationships-profile",
            body: Body(body),
            idempotencyKey: idempotencyKey
        )
    }

    func patchPersonalLifeOpsProfile(
        momentId: String,
        body: [String: Any],
        idempotencyKey: String = UUID().uuidString
    ) async throws -> PersonalProjectionPayload {
        struct Body: Encodable {
            let values: [String: JSONEncodableValue]
            init(_ dict: [String: Any]) { values = JSONEncodableValue.map(dict) }
            func encode(to encoder: Encoder) throws {
                var container = encoder.singleValueContainer()
                try container.encode(values)
            }
        }
        return try await authorizedPatch(
            path: "v1/personal/moments/\(momentId)/life-ops-profile",
            body: Body(body),
            idempotencyKey: idempotencyKey
        )
    }

    func listGroupVendors(momentId: String) async throws -> PersonalProjectionPayload {
        try await authorizedGet(path: "v1/group/moments/\(momentId)/vendors")
    }

    func listMemoryMedia(momentId: String, memoryId: String) async throws -> PersonalProjectionPayload {
        try await authorizedGet(path: "v1/moments/\(momentId)/memories/\(memoryId)/media")
    }

    func ingestTelemetry(_ payload: TelemetryIngestPayload) async throws {
        var request = URLRequest(url: APIConfig.baseURL.appendingPathComponent("v1/telemetry/events"))
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue("true", forHTTPHeaderField: "ngrok-skip-browser-warning")
        if Auth.auth().currentUser != nil {
            let token = try await AuthTokenCache.shared.get()
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        request.httpBody = try JSONEncoder().encode(payload)

        let (_, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse, (200 ..< 300).contains(http.statusCode) else {
            throw URLError(.badServerResponse)
        }
    }

    private func authorizedGet<T: Decodable>(
        path: String,
        query: [String: String] = [:],
        forceRefreshToken: Bool = false,
        networkAttempt: Int = 0
    ) async throws -> T {
        guard Auth.auth().currentUser != nil else {
            throw APIErrorKind.unauthenticated("UNAUTHORIZED")
        }
        let token = try await AuthTokenCache.shared.get(forceRefresh: forceRefreshToken)
        var components = URLComponents(
            url: APIConfig.baseURL.appendingPathComponent(path),
            resolvingAgainstBaseURL: false
        )!
        if !query.isEmpty {
            components.queryItems = query.map { URLQueryItem(name: $0.key, value: $0.value) }
        }
        var request = URLRequest(url: components.url!)
        request.httpMethod = "GET"
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue(QaCorrelationHolder.takeCorrelationId(), forHTTPHeaderField: "X-Correlation-Id")
        if let runId = QaCorrelationHolder.peekRunId() {
            request.setValue(runId, forHTTPHeaderField: "X-Maestro-Run-Id")
        }
        request.setValue("true", forHTTPHeaderField: "ngrok-skip-browser-warning")

        let data: Data
        let response: URLResponse
        do {
            (data, response) = try await session.data(for: request)
        } catch {
            // Bounded GET-only network retry (one extra attempt).
            if networkAttempt < 1 {
                return try await authorizedGet(
                    path: path,
                    query: query,
                    forceRefreshToken: forceRefreshToken,
                    networkAttempt: networkAttempt + 1
                )
            }
            throw APIErrorKind.network(Self.networkFailureMessage(error))
        }
        guard let http = response as? HTTPURLResponse else {
            throw APIErrorKind.network("NETWORK_UNAVAILABLE")
        }
        if http.statusCode == 401, !forceRefreshToken {
            return try await authorizedGet(path: path, query: query, forceRefreshToken: true)
        }
        guard (200 ..< 300).contains(http.statusCode) else {
            let body = try? decoder.decode(APIErrorBody.self, from: data)
            throw APIErrorKind.from(status: http.statusCode, code: body?.code, message: body?.message)
        }
        let envelope = try decoder.decode(SuccessEnvelope<T>.self, from: data)
        return envelope.data
    }

    private func authorizedPostWithHints<B: Encodable>(
        path: String,
        body: B,
        idempotencyKey: String,
        forceRefreshToken: Bool = false
    ) async throws -> CreateMomentAPIResponse {
        guard Auth.auth().currentUser != nil else {
            throw APIErrorKind.unauthenticated("UNAUTHORIZED")
        }
        let token = try await AuthTokenCache.shared.get(forceRefresh: forceRefreshToken)
        var request = URLRequest(url: APIConfig.baseURL.appendingPathComponent(path))
        request.httpMethod = "POST"
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue(idempotencyKey, forHTTPHeaderField: "Idempotency-Key")
        request.setValue(QaCorrelationHolder.takeCorrelationId(), forHTTPHeaderField: "X-Correlation-Id")
        if let runId = QaCorrelationHolder.peekRunId() {
            request.setValue(runId, forHTTPHeaderField: "X-Maestro-Run-Id")
        }
        request.setValue("true", forHTTPHeaderField: "ngrok-skip-browser-warning")
        request.httpBody = try JSONEncoder().encode(body)

        let data: Data
        let response: URLResponse
        do {
            (data, response) = try await session.data(for: request)
        } catch {
            throw APIErrorKind.network(Self.networkFailureMessage(error))
        }
        guard let http = response as? HTTPURLResponse else {
            throw APIErrorKind.network("NETWORK_UNAVAILABLE")
        }
        if http.statusCode == 401, !forceRefreshToken {
            return try await authorizedPostWithHints(
                path: path,
                body: body,
                idempotencyKey: idempotencyKey,
                forceRefreshToken: true
            )
        }
        guard (200 ..< 300).contains(http.statusCode) else {
            let errBody = try? decoder.decode(APIErrorBody.self, from: data)
            throw APIErrorKind.from(status: http.statusCode, code: errBody?.code, message: errBody?.message)
        }
        let envelope = try decoder.decode(SuccessEnvelopeWithHints<CreateMomentResult>.self, from: data)
        return CreateMomentAPIResponse(
            result: envelope.data,
            projectionHints: envelope.projectionHints ?? []
        )
    }

    private func authorizedPost<T: Decodable, B: Encodable>(
        path: String,
        body: B,
        idempotencyKey: String,
        forceRefreshToken: Bool = false
    ) async throws -> T {
        guard Auth.auth().currentUser != nil else {
            throw APIErrorKind.unauthenticated("UNAUTHORIZED")
        }
        let token = try await AuthTokenCache.shared.get(forceRefresh: forceRefreshToken)
        var request = URLRequest(url: APIConfig.baseURL.appendingPathComponent(path))
        request.httpMethod = "POST"
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue(idempotencyKey, forHTTPHeaderField: "Idempotency-Key")
        request.setValue("true", forHTTPHeaderField: "ngrok-skip-browser-warning")
        request.httpBody = try JSONEncoder().encode(body)

        let data: Data
        let response: URLResponse
        do {
            (data, response) = try await session.data(for: request)
        } catch {
            throw APIErrorKind.network(Self.networkFailureMessage(error))
        }
        guard let http = response as? HTTPURLResponse else {
            throw APIErrorKind.network("NETWORK_UNAVAILABLE")
        }
        if http.statusCode == 401, !forceRefreshToken {
            return try await authorizedPost(
                path: path,
                body: body,
                idempotencyKey: idempotencyKey,
                forceRefreshToken: true
            )
        }
        guard (200 ..< 300).contains(http.statusCode) else {
            let errBody = try? decoder.decode(APIErrorBody.self, from: data)
            throw APIErrorKind.from(status: http.statusCode, code: errBody?.code, message: errBody?.message)
        }
        let envelope = try decoder.decode(SuccessEnvelope<T>.self, from: data)
        return envelope.data
    }

    private func authorizedPatch<T: Decodable, B: Encodable>(
        path: String,
        body: B,
        idempotencyKey: String? = nil,
        forceRefreshToken: Bool = false
    ) async throws -> T {
        guard Auth.auth().currentUser != nil else {
            throw APIErrorKind.unauthenticated("UNAUTHORIZED")
        }
        let token = try await AuthTokenCache.shared.get(forceRefresh: forceRefreshToken)
        var request = URLRequest(url: APIConfig.baseURL.appendingPathComponent(path))
        request.httpMethod = "PATCH"
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        if let idempotencyKey {
            request.setValue(idempotencyKey, forHTTPHeaderField: "Idempotency-Key")
        }
        request.setValue("true", forHTTPHeaderField: "ngrok-skip-browser-warning")
        request.httpBody = try JSONEncoder().encode(body)

        let data: Data
        let response: URLResponse
        do {
            (data, response) = try await session.data(for: request)
        } catch {
            throw APIErrorKind.network(Self.networkFailureMessage(error))
        }
        guard let http = response as? HTTPURLResponse else {
            throw APIErrorKind.network("NETWORK_UNAVAILABLE")
        }
        if http.statusCode == 401, !forceRefreshToken {
            return try await authorizedPatch(
                path: path,
                body: body,
                idempotencyKey: idempotencyKey,
                forceRefreshToken: true
            )
        }
        guard (200 ..< 300).contains(http.statusCode) else {
            let errBody = try? decoder.decode(APIErrorBody.self, from: data)
            throw APIErrorKind.from(status: http.statusCode, code: errBody?.code, message: errBody?.message)
        }
        let envelope = try decoder.decode(SuccessEnvelope<T>.self, from: data)
        return envelope.data
    }

    private func authorizedDelete<T: Decodable>(
        path: String,
        forceRefreshToken: Bool = false
    ) async throws -> T {
        try await authorizedDelete(path: path, body: Optional<String>.none as String?, idempotencyKey: nil, forceRefreshToken: forceRefreshToken)
    }

    private func authorizedDelete<T: Decodable, B: Encodable>(
        path: String,
        body: B?,
        idempotencyKey: String? = nil,
        forceRefreshToken: Bool = false
    ) async throws -> T {
        guard Auth.auth().currentUser != nil else {
            throw APIErrorKind.unauthenticated("UNAUTHORIZED")
        }
        let token = try await AuthTokenCache.shared.get(forceRefresh: forceRefreshToken)
        var request = URLRequest(url: APIConfig.baseURL.appendingPathComponent(path))
        request.httpMethod = "DELETE"
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue("true", forHTTPHeaderField: "ngrok-skip-browser-warning")
        if let idempotencyKey {
            request.setValue(idempotencyKey, forHTTPHeaderField: "Idempotency-Key")
        }
        if let body {
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
            request.httpBody = try JSONEncoder().encode(body)
        }

        let data: Data
        let response: URLResponse
        do {
            (data, response) = try await session.data(for: request)
        } catch {
            throw APIErrorKind.network(Self.networkFailureMessage(error))
        }
        guard let http = response as? HTTPURLResponse else {
            throw APIErrorKind.network("NETWORK_UNAVAILABLE")
        }
        if http.statusCode == 401, !forceRefreshToken {
            return try await authorizedDelete(
                path: path,
                body: body,
                idempotencyKey: idempotencyKey,
                forceRefreshToken: true
            )
        }
        guard (200 ..< 300).contains(http.statusCode) else {
            let errBody = try? decoder.decode(APIErrorBody.self, from: data)
            throw APIErrorKind.from(status: http.statusCode, code: errBody?.code, message: errBody?.message)
        }
        let envelope = try decoder.decode(SuccessEnvelope<T>.self, from: data)
        return envelope.data
    }

    private static func networkFailureMessage(_ error: Error) -> String {
        let ns = error as NSError
        if ns.domain == NSURLErrorDomain {
            let detail: String = switch ns.code {
            case NSURLErrorNotConnectedToInternet: "device is offline"
            case NSURLErrorTimedOut: "request timed out"
            case NSURLErrorCannotFindHost: "host not found (DNS)"
            case NSURLErrorCannotConnectToHost: "could not connect to host"
            case NSURLErrorNetworkConnectionLost: "connection lost"
            case NSURLErrorSecureConnectionFailed: "TLS/SSL failed"
            case NSURLErrorAppTransportSecurityRequiresSecureConnection: "ATS blocked insecure URL"
            default: "NSURLError \(ns.code)"
            }
            return """
            Cannot reach the API at \(APIConfig.baseURLDescription) (\(detail)). \
            Confirm the backend is running and Info.plist MomentraAPIBaseURL \
            matches your computer’s LAN IP (same as Android API_BASE_URL). \
            Do not use 127.0.0.1 on a physical device.
            """
        }
        return error.localizedDescription
    }
}
