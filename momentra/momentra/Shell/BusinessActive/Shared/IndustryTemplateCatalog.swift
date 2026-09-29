import Foundation

struct IndustryTemplate: Identifiable, Equatable {
    let id: String
    let label: String
    let subtitle: String
    let modules: [String: Bool]
    let setupKinds: [BusinessSetupKind]
    let profileDefaults: [String: Any]
    let isCustom: Bool

    static func == (lhs: IndustryTemplate, rhs: IndustryTemplate) -> Bool {
        lhs.id == rhs.id
    }
}

enum IndustryTemplateCatalog {
    static let profileKey = "industryTemplate"

    static let kirana = IndustryTemplate(
        id: "KIRANA",
        label: "Kirana / Grocery",
        subtitle: "Counter sales, suppliers, and udhaar",
        modules: [
            "money": true,
            "dailyBusiness": true,
            "teamOps": false,
            "vendors": true,
            "projects": false,
            "events": false,
        ],
        setupKinds: [.businessRunway, .businessOperations],
        profileDefaults: [
            "industry": "Retail / Kirana",
            "taxSystem": "GST",
            "taxModeHint": "Composition or Regular GST",
            "hubHint": "Khata and cash sales ready",
        ],
        isCustom: false
    )

    static let petRetail = IndustryTemplate(
        id: "PET_RETAIL",
        label: "Pet store / Specialty retail",
        subtitle: "Higher ticket sales, stock, and invoices",
        modules: [
            "money": true,
            "dailyBusiness": true,
            "teamOps": false,
            "vendors": true,
            "projects": false,
            "events": false,
        ],
        setupKinds: [.businessRunway, .businessOperations],
        profileDefaults: [
            "industry": "Pet store / Specialty",
            "taxSystem": "GST",
            "taxModeHint": "Regular GST",
            "hubHint": "Invoices and Khata for regulars",
        ],
        isCustom: false
    )

    static let workshop = IndustryTemplate(
        id: "WORKSHOP",
        label: "Micro-workshop",
        subtitle: "Make, track spend, and work with a small team",
        modules: [
            "money": true,
            "dailyBusiness": true,
            "teamOps": true,
            "vendors": true,
            "projects": false,
            "events": false,
        ],
        setupKinds: [.businessRunway, .businessOperations, .teamOperations],
        profileDefaults: [
            "industry": "Manufacturing / Workshop",
            "taxSystem": "GST",
            "hubHint": "Money, daily ops, and team together",
        ],
        isCustom: false
    )

    static let service = IndustryTemplate(
        id: "SERVICE",
        label: "Service / Repair shop",
        subtitle: "Jobs, estimates mindset, and collections",
        modules: [
            "money": true,
            "dailyBusiness": true,
            "teamOps": false,
            "vendors": true,
            "projects": false,
            "events": false,
        ],
        setupKinds: [.businessRunway, .businessOperations],
        profileDefaults: [
            "industry": "Services",
            "taxSystem": "GST",
            "taxModeHint": "Services GST",
            "hubHint": "Khata for open jobs and invoices",
        ],
        isCustom: false
    )

    static let custom = IndustryTemplate(
        id: "CUSTOM",
        label: "Custom / Blank",
        subtitle: "Pick Money, Daily Business, and Team yourself",
        modules: [
            "money": true,
            "dailyBusiness": true,
            "teamOps": false,
            "vendors": true,
            "projects": false,
            "events": false,
        ],
        setupKinds: [],
        profileDefaults: [:],
        isCustom: true
    )

    static let all: [IndustryTemplate] = [kirana, petRetail, workshop, service, custom]

    static func byId(_ id: String?) -> IndustryTemplate? {
        let key = (id ?? "").trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        guard !key.isEmpty else { return nil }
        return all.first { $0.id == key }
    }

    static func labelFor(_ id: String?) -> String {
        byId(id)?.label ?? "—"
    }

    static func suggest(sells: String?, bills: String?) -> IndustryTemplate {
        let s = (sells ?? "").lowercased()
        // billHow alone must never force SERVICE — only sell-what drives the suggestor.
        if s.contains("service") || s.contains("repair") { return service }
        if s.contains("made") || s.contains("workshop") || s.contains("manufactur") { return workshop }
        if s.contains("physical") || s.contains("item") || s.contains("grocery") || s.contains("kirana") { return kirana }
        if s.contains("pet") || s.contains("specialty") || s.contains("boutique") { return petRetail }
        return kirana
    }

    static func hubHint(from profileJson: [String: AnyDecodable]?) -> String? {
        if let direct = profileJson?["hubHint"]?.value as? String {
            let trimmed = direct.trimmingCharacters(in: .whitespacesAndNewlines)
            if !trimmed.isEmpty { return trimmed }
        }
        if let raw = profileJson?[profileKey]?.value as? String,
           let hint = byId(raw)?.profileDefaults["hubHint"] as? String {
            let trimmed = hint.trimmingCharacters(in: .whitespacesAndNewlines)
            if !trimmed.isEmpty { return trimmed }
        }
        return nil
    }

    static func hubHint(from profileJson: [String: Any]?) -> String? {
        if let direct = profileJson?["hubHint"] as? String {
            let trimmed = direct.trimmingCharacters(in: .whitespacesAndNewlines)
            if !trimmed.isEmpty { return trimmed }
        }
        if let raw = profileJson?[profileKey] as? String,
           let hint = byId(raw)?.profileDefaults["hubHint"] as? String {
            let trimmed = hint.trimmingCharacters(in: .whitespacesAndNewlines)
            if !trimmed.isEmpty { return trimmed }
        }
        return nil
    }

    private static let hubHintDefaultsPrefix = "momentra.company.hubHint."

    static func saveHubHint(companyId: String, hint: String?) {
        let id = companyId.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !id.isEmpty else { return }
        let value = (hint ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
        if value.isEmpty {
            UserDefaults.standard.removeObject(forKey: hubHintDefaultsPrefix + id)
        } else {
            UserDefaults.standard.set(value, forKey: hubHintDefaultsPrefix + id)
        }
    }

    static func hubHintForCompany(companyId: String?) -> String? {
        let id = (companyId ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
        guard !id.isEmpty else { return nil }
        let value = UserDefaults.standard.string(forKey: hubHintDefaultsPrefix + id)?
            .trimmingCharacters(in: .whitespacesAndNewlines)
        return (value?.isEmpty == false) ? value : nil
    }

    static func rehydrateHubHint(companyId: String, profileJson: [String: AnyDecodable]?) {
        if let hint = hubHint(from: profileJson) {
            saveHubHint(companyId: companyId, hint: hint)
        }
    }

    static func setupKinds(for template: IndustryTemplate, customModules: [String: Bool]) -> [BusinessSetupKind] {
        if !template.isCustom { return template.setupKinds }
        var kinds: [BusinessSetupKind] = []
        if customModules["money"] != false { kinds.append(.businessRunway) }
        if customModules["dailyBusiness"] != false { kinds.append(.businessOperations) }
        if customModules["teamOps"] == true { kinds.append(.teamOperations) }
        return kinds
    }

    static func modules(for template: IndustryTemplate, customModules: [String: Bool]) -> [String: Bool] {
        if template.isCustom {
            return template.modules.merging(customModules) { _, new in new }
        }
        return template.modules
    }
}
