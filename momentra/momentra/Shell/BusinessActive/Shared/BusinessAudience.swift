import Foundation

/// Business audience for India micro-retail vs growing business.
enum BusinessAudience {
    static let smallShop = "SMALL_SHOP"
    static let growing = "GROWING"
    static let prefKey = "audience"

    private static let defaultsKeyPrefix = "momentra.business.audience."

    static func normalize(_ raw: String?) -> String {
        let value = (raw ?? "").trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        return value == smallShop ? smallShop : growing
    }

    static func isSmallShop(_ raw: String?) -> Bool {
        normalize(raw) == smallShop
    }

    static func isSmallShop(preferences: [String: AnyDecodable]?) -> Bool {
        isSmallShop(preferences?[prefKey]?.value as? String)
    }

    /// Nil when preferences have no audience key (fall back to company cache).
    static func audienceFromPreferences(_ preferences: [String: AnyDecodable]?) -> String? {
        guard let preferences, preferences[prefKey] != nil else { return nil }
        return normalize(preferences[prefKey]?.value as? String)
    }

    static func saveForCompany(companyId: String, audience: String) {
        let id = companyId.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !id.isEmpty else { return }
        UserDefaults.standard.set(normalize(audience), forKey: defaultsKeyPrefix + id)
    }

    static func forCompany(companyId: String?, profileAudience: String? = nil) -> String {
        let id = (companyId ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
        guard !id.isEmpty else { return growing }
        if let cached = UserDefaults.standard.string(forKey: defaultsKeyPrefix + id),
           !cached.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return normalize(cached)
        }
        if let profileAudience, !profileAudience.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            saveForCompany(companyId: id, audience: profileAudience)
            return normalize(profileAudience)
        }
        return growing
    }

    /// Resolve order: moment prefs.audience if present → company local cache → Growing.
    static func isSmallShopMoment(momentId: String?, fallbackCompanyId: String? = nil) async -> Bool {
        guard let momentId, !momentId.isEmpty else {
            if let fallbackCompanyId {
                return isSmallShop(forCompany(companyId: fallbackCompanyId))
            }
            return false
        }
        do {
            let prefill = try await APIClient.shared.getDomainSetupPrefill(momentId: momentId)
            if let audience = audienceFromPreferences(prefill.preferences) {
                return audience == smallShop
            }
            let companyId = (prefill.companyId?.trimmingCharacters(in: .whitespacesAndNewlines)).flatMap {
                $0.isEmpty ? nil : $0
            } ?? fallbackCompanyId
            return isSmallShop(forCompany(companyId: companyId))
        } catch {
            if let fallbackCompanyId {
                return isSmallShop(forCompany(companyId: fallbackCompanyId))
            }
            return false
        }
    }
}
