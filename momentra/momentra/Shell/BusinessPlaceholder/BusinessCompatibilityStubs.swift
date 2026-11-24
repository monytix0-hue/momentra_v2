import Foundation
import SwiftUI
import Combine

/// Minimal Business setup kinds retained so Company Setup / MomentCreate still compile
/// after Phase 1C wiped Business operating UI. Vertical-specific setup docs are gone.
enum BusinessSetupKind: String, CaseIterable, Equatable, Identifiable {
    case marketing, marketingOwn, trading, distribution, retail, manufacturing, madeToOrder, service, restaurant

    var id: String { rawValue }

    var title: String {
        switch self {
        case .marketing: return "Client campaign"
        case .marketingOwn: return "Own business"
        case .trading: return "Trading"
        case .distribution: return "Distribution"
        case .retail: return "Retail"
        case .manufacturing: return "Manufacturing"
        case .madeToOrder: return "Job order"
        case .service: return "Service"
        case .restaurant: return "Restaurant"
        }
    }

    var body: String {
        switch self {
        case .trading, .distribution: return "Source, move, and collect across wholesale or distribution."
        case .marketing, .marketingOwn: return "Budget, channels, people, and results for a campaign."
        case .retail: return "A store, a food outlet, or a consumer service."
        case .manufacturing, .madeToOrder: return "Standard production or made-to-order job work."
        case .service: return "A consumer service you sell and deliver."
        case .restaurant: return "A food outlet with counter and table sales."
        }
    }

    var maestroTag: String { rawValue }
    var activateColor: Color { accent }

    var subtype: String {
        switch self {
        case .trading: return "TRADING_WHOLESALE"
        case .distribution: return "DISTRIBUTION_DEALERSHIP"
        case .marketing: return "CLIENT_CAMPAIGN"
        case .marketingOwn: return "OWN_BUSINESS_CAMPAIGN"
        case .retail: return "RETAIL_STORE"
        case .manufacturing: return "STANDARD_PRODUCTION"
        case .madeToOrder: return "MADE_TO_ORDER"
        case .service: return "CONSUMER_SERVICE"
        case .restaurant: return "RESTAURANT_FOOD"
        }
    }

    var familyCode: String {
        switch self {
        case .trading, .distribution: return "TRADING_DISTRIBUTION"
        case .marketing, .marketingOwn: return "MARKETING_CAMPAIGNS"
        case .retail, .service, .restaurant: return "RETAIL_CONSUMER"
        case .manufacturing, .madeToOrder: return "MANUFACTURING_PRODUCTION"
        }
    }

    var accent: Color {
        BusinessSetupTheme.forTypeCode(subtype).accent
    }
}

enum BusinessAudience {
    static let prefKey = "audience"
    static let smallShop = "smallShop"
    static let growing = "growing"
    static func isSmallShop(_ audience: String) -> Bool { audience == "smallShop" }
    static func forCompany(companyId: String?) -> String { "smallShop" }
    static func saveForCompany(companyId: String, audience: String) {}
}

enum CompanyModules {
    static func rehydrateFromProfile(companyId: String, profileJson: [String: Any]?) {}
    static func moduleKey(for kind: BusinessSetupKind) -> String? { kind.rawValue }
    static func isEnabled(companyId: String?, key: String) -> Bool { true }
    static func saveModules(companyId: String, modules: [String: Bool]) {}
}

class OperatingChromeState: ObservableObject {
    static let shared = OperatingChromeState()
    @Published var active = false
    @Published var previewStoryId: String? = nil
}

enum BusinessGapPage: String, Identifiable {
    case expense, spendEntry
    var id: String { rawValue }
}

struct PendingBusinessActivation: Identifiable {
    let id = UUID()
    var momentId: String = ""
    var title: String = ""
    var momentTypeCode: String = ""
    var status: String? = nil
}

struct BusinessSetupCatalogEntry {
    let familyCode: String
    let title: String
    let subtitle: String
    let momentTypeCode: String
    let activateLabel: String
    let defaultTitle: String
    let defaultPreferences: [String: Any]
    var allowedKeys: Set<String> = []
}

enum BusinessSetupCatalog {
    static func forKind(_ kind: BusinessSetupKind) -> BusinessSetupCatalogEntry {
        let spec = BusinessSetupDocuments.spec(for: kind.subtype)
        let keys = Set(spec.sections.flatMap { $0.fields.map(\.key) })
        return BusinessSetupCatalogEntry(
            familyCode: kind.familyCode,
            title: kind.title,
            subtitle: kind.body,
            momentTypeCode: kind.subtype,
            activateLabel: spec.activateLabel,
            defaultTitle: kind.title,
            defaultPreferences: [:],
            allowedKeys: keys
        )
    }
}

enum BusinessQuickAddKind: String, Identifiable {
    case expense, revenue, invoice
    var id: String { rawValue }
}

struct PendingCompanyJoin: Identifiable {
    let id: String
    var code: String { id }
}

/// Company invite deeplinks — `momentra://c/{code}` / `momentra.tech/c/{code}`.
enum CompanyJoinLink {
    private static let shortPattern = #"^[a-hj-np-z2-9]{8}$"#

    static func displayPath(code: String) -> String {
        "https://momentra.tech/c/\(code.trimmingCharacters(in: .whitespacesAndNewlines).lowercased())"
    }

    static func qrPayload(code: String) -> String {
        displayPath(code: code)
    }

    static func parse(_ raw: String) -> String? {
        let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return nil }
        if let fromSlash = extractFromSlashes(trimmed) { return fromSlash }
        guard let url = URL(string: trimmed) else { return nil }
        let scheme = url.scheme?.lowercased() ?? ""
        let host = url.host?.lowercased() ?? ""
        let parts = url.path.split(separator: "/").map(String.init)

        if scheme == "momentra" {
            if host == "c" || host == "company" {
                return sanitize(parts.last)
            }
            if parts.first == "c" || parts.first == "company" {
                return sanitize(parts.dropFirst().first)
            }
        }
        if Self.isInviteHost(host),
           let first = parts.first, first == "c" || first == "company" {
            return sanitize(parts.dropFirst().first)
        }
        return nil
    }

    static func parseTyped(_ raw: String) -> String? {
        if let linked = parse(raw) { return linked }
        return sanitize(raw.trimmingCharacters(in: .whitespacesAndNewlines).lowercased())
    }

    private static func extractFromSlashes(_ raw: String) -> String? {
        let parts = raw.split(separator: "?").first.map(String.init) ?? raw
        let segs = parts.split(separator: "#").first.map(String.init) ?? parts
        let pieces = segs.split(separator: "/").map(String.init)
        if let marker = pieces.firstIndex(where: { $0.lowercased() == "c" || $0.lowercased() == "company" }) {
            return sanitize(pieces.dropFirst(marker + 1).first)
        }
        return nil
    }

    private static func sanitize(_ raw: String?) -> String? {
        guard let raw else { return nil }
        let value = raw.trimmingCharacters(in: CharacterSet(charactersIn: "/")).lowercased()
        if value.range(of: shortPattern, options: [.regularExpression, .caseInsensitive]) != nil { return value }
        return nil
    }

    private static func isInviteHost(_ host: String) -> Bool {
        [
            "momentra.tech",
            "www.momentra.tech",
            "momentra.app",
            "www.momentra.app",
            "momentra-v2.web.app",
        ].contains(host)
    }
}

enum InviteResult {
    case company(String)
    case group(String)
}

enum InviteJoinLink {
    static func parse(_ raw: String) -> InviteResult? {
        if let company = CompanyJoinLink.parse(raw) { return .company(company) }
        if let group = GroupJoinLink.parse(raw) { return .group(group) }
        return nil
    }
}

struct IndustryTemplate: Identifiable {
    let id: String
    let label: String
    let subtitle: String
    let profileDefaults: [String: Any]
    let isCustom: Bool
}

enum IndustryTemplateCatalog {
    static let profileKey = "industryTemplate"
    static let kirana = IndustryTemplate(
        id: "kirana",
        label: "Retail / Kirana",
        subtitle: "Counter billing & stock",
        profileDefaults: ["industry": "Retail / Kirana"],
        isCustom: false
    )
    static let all: [IndustryTemplate] = [kirana]
    static func suggest(sells: String, bills: String) -> IndustryTemplate { kirana }
    static func setupKinds(for template: IndustryTemplate, customModules: [String: Bool]) -> [BusinessSetupKind] {
        BusinessSetupKind.allCases
    }
    static func modules(for template: IndustryTemplate, customModules: [String: Bool]) -> [String: Bool] {
        ["money": true, "dailyBusiness": true, "teamOps": true]
    }
    static func hubHint(from profile: [String: Any]) -> String? { nil }
    static func saveHubHint(companyId: String, hint: String) {}
    static func rehydrateHubHint(companyId: String, profileJson: [String: Any]?) {}
}

extension View {
    func maestroKeyboardDone() -> some View {
        self
    }
}
