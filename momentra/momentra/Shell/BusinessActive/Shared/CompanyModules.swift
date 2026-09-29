import Foundation

/// Company module toggles from profileJson.settings.modules; cached per company in UserDefaults.
enum CompanyModules {
    static let knownKeys: Set<String> = [
        "teamOps",
        "dailyBusiness",
        "money",
        "vendors",
        "projects",
        "events",
    ]

    private static let defaultsKeyPrefix = "momentra.company.module."

    static func moduleKey(for kind: BusinessSetupKind) -> String? {
        switch kind {
        case .teamOperations: return "teamOps"
        case .businessRunway: return "money"
        case .businessOperations: return "dailyBusiness"
        }
    }

    static func moduleKey(for theme: BusinessActiveTheme) -> String {
        switch theme.typeLabel {
        case BusinessActiveTheme.businessRunway.typeLabel: return "money"
        case BusinessActiveTheme.businessOperations.typeLabel: return "dailyBusiness"
        default: return "teamOps"
        }
    }

    static func saveModules(companyId: String, modules: [String: Bool]) {
        let id = companyId.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !id.isEmpty, !modules.isEmpty else { return }
        for (key, enabled) in modules {
            let k = key.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !k.isEmpty else { continue }
            UserDefaults.standard.set(enabled, forKey: prefKey(companyId: id, moduleKey: k))
        }
    }

    static func isEnabled(companyId: String?, key: String) -> Bool {
        let id = (companyId ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
        guard !id.isEmpty else { return true }
        let fullKey = prefKey(companyId: id, moduleKey: key)
        if UserDefaults.standard.object(forKey: fullKey) == nil { return true }
        return UserDefaults.standard.bool(forKey: fullKey)
    }

    static func rehydrateFromProfile(companyId: String, profileJson: [String: AnyDecodable]?) {
        let modules = modulesFromProfile(profileJson)
        guard !modules.isEmpty else { return }
        saveModules(companyId: companyId, modules: modules)
    }

    static func modulesFromProfile(_ profileJson: [String: AnyDecodable]?) -> [String: Bool] {
        guard let profileJson else { return [:] }
        let settings = companyNestedMap(profileJson["settings"])
        guard let modules = settings["modules"] as? [String: Any] else { return [:] }
        var out: [String: Bool] = [:]
        for (key, value) in modules {
            out[key] = companyBool(value, default: true)
        }
        return out
    }

    private static func prefKey(companyId: String, moduleKey: String) -> String {
        defaultsKeyPrefix + companyId + "." + moduleKey
    }

    private static func companyNestedMap(_ value: AnyDecodable?) -> [String: Any] {
        guard let raw = value?.value else { return [:] }
        return raw as? [String: Any] ?? [:]
    }

    private static func companyBool(_ value: Any?, default defaultValue: Bool) -> Bool {
        switch value {
        case let b as Bool: return b
        case let n as NSNumber: return n.intValue != 0
        case let s as String: return s.lowercased() == "true"
        default: return defaultValue
        }
    }
}
