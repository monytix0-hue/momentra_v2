import Foundation

struct ShellBootstrap: Equatable {
    let identity: ShellIdentity
    let supportedContexts: [AppContextKind]
    let currentlySelectedContext: AppContextKind
    let personalMoments: [MomentSummary]
    let groupMoments: [MomentSummary]
    let businessMoments: [MomentSummary]
    let companies: [CompanySummary]
    let selectedCompany: CompanySummary?
    let capabilities: [String]
    let roles: [String]
    let preferencesTimezone: String
    let preferencesLocale: String?
}

enum BootstrapCacheStore {
    private static let defaults = UserDefaults.standard

    private static func key(_ userId: String) -> String { "momentra_bootstrap_v1_\(userId)" }
    private static func savedAtKey(_ userId: String) -> String { "momentra_bootstrap_saved_at_\(userId)" }

    static func save(userId: String, data: Data) {
        guard !userId.isEmpty else { return }
        defaults.set(data, forKey: key(userId))
        defaults.set(Date().timeIntervalSince1970 * 1000, forKey: savedAtKey(userId))
    }

    static func loadData(userId: String) -> Data? {
        guard !userId.isEmpty else { return nil }
        return defaults.data(forKey: key(userId))
    }

    static func savedAtMs(userId: String) -> TimeInterval? {
        guard !userId.isEmpty else { return nil }
        let at = defaults.double(forKey: savedAtKey(userId))
        return at > 0 ? at : nil
    }

    static func isFresh(userId: String, maxAgeMs: TimeInterval = 30_000) -> Bool {
        guard let at = savedAtMs(userId: userId) else { return false }
        return Date().timeIntervalSince1970 * 1000 - at < maxAgeMs
    }

    static func clear(userId: String?) {
        guard let userId, !userId.isEmpty else { return }
        defaults.removeObject(forKey: key(userId))
        defaults.removeObject(forKey: savedAtKey(userId))
    }
}

extension MeBootstrap {
    func toShellBootstrap() -> ShellBootstrap {
        func parseContext(_ raw: String?) -> AppContextKind? {
            AppContextKind(rawValue: raw?.uppercased() ?? "")
        }
        let contexts = (supportedContexts ?? ["PERSONAL", "CIRCLE"]).compactMap(parseContext)
        return ShellBootstrap(
            identity: ShellIdentity(
                userId: userId,
                displayName: displayName,
                email: email,
                firebaseUid: firebaseUid
            ),
            supportedContexts: contexts.isEmpty ? [.personal, .circle] : contexts,
            currentlySelectedContext: parseContext(currentlySelectedContext) ?? .personal,
            personalMoments: (activeMoments?.personal ?? []).map {
                MomentSummary(momentId: $0.momentId, title: $0.title, status: $0.status, momentTypeCode: $0.momentTypeCode)
            },
            groupMoments: (activeMoments?.group ?? []).map {
                MomentSummary(momentId: $0.momentId, title: $0.title, status: $0.status, momentTypeCode: $0.momentTypeCode)
            },
            businessMoments: (activeMoments?.business ?? []).map {
                MomentSummary(
                    momentId: $0.momentId,
                    title: $0.title,
                    status: $0.status,
                    momentTypeCode: $0.momentTypeCode,
                    companyId: $0.companyId
                )
            },
            companies: (companies ?? []).map {
                CompanySummary(companyId: $0.companyId, displayName: $0.displayName)
            },
            selectedCompany: selectedCompany.map {
                CompanySummary(companyId: $0.companyId, displayName: $0.displayName)
            } ?? (companies ?? []).first.map {
                CompanySummary(companyId: $0.companyId, displayName: $0.displayName)
            },
            capabilities: capabilities ?? [],
            roles: roles ?? [],
            preferencesTimezone: preferences?.timezone ?? timezone ?? "UTC",
            preferencesLocale: preferences?.locale ?? locale
        )
    }
}
