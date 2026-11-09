import Foundation
import SwiftUI
import Combine

/// The kinds of business a moment can be created for.
///
/// `trading` and `distribution` are the two operating verticals: they carry a `subtype` that the
/// six-section setup document (`TRD-SETUP-v6` / `DST-SETUP-v6`) and the live operating screens
/// both read. The remaining kinds keep their own family code and are set up with the compact
/// form, because the catalogue/warehouse document belongs to trading and distribution only.
enum BusinessSetupKind: String, CaseIterable, Equatable, Identifiable {
    case marketing, trading, distribution, retail, manufacturing, service, restaurant

    var id: String { rawValue }
    var title: String {
        switch self {
        case .marketing: return "Marketing"
        case .trading: return "Trading"
        case .distribution: return "Distribution"
        case .retail: return "Retail"
        case .manufacturing: return "Manufacturing"
        case .service: return "Service"
        case .restaurant: return "Restaurant"
        }
    }
    var body: String {
        switch self {
        case .trading, .distribution: return "Source, move, and collect across wholesale or distribution."
        case .marketing: return "Budget, channels, people, and results for a campaign."
        case .retail: return "A store, a food outlet, or a consumer service."
        case .manufacturing: return "Standard production or made-to-order job work."
        case .service: return "A consumer service you sell and deliver."
        case .restaurant: return "A food outlet with counter and table sales."
        }
    }
    var maestroTag: String { rawValue }
    var activateColor: Color { accent }

    /// The two operating verticals, or `nil` for a family that has no operating screens.
    var vertical: BusinessOperatingVertical? {
        switch self {
        case .trading: return .trading
        case .distribution: return .distribution
        default: return nil
        }
    }

    /// `TRADING_WHOLESALE` / `DISTRIBUTION_DEALERSHIP`, or the family's own type code.
    var subtype: String {
        switch self {
        case .trading: return BusinessSetupDocSpec.tradingSubtype
        case .distribution: return BusinessSetupDocSpec.distributionSubtype
        case .marketing: return "CLIENT_CAMPAIGN"
        case .retail: return "RETAIL_STORE"
        case .manufacturing: return "STANDARD_PRODUCTION"
        case .service: return "CONSUMER_SERVICE"
        case .restaurant: return "RESTAURANT_FOOD"
        }
    }

    /// The backend's `businessSetup.familyCode` for this kind.
    var familyCode: String {
        switch self {
        case .trading, .distribution: return "TRADING_DISTRIBUTION"
        case .marketing: return "MARKETING_CAMPAIGNS"
        case .retail, .service, .restaurant: return "RETAIL_CONSUMER"
        case .manufacturing: return "MANUFACTURING_PRODUCTION"
        }
    }

    var accent: Color {
        switch self {
        case .trading: return BusinessOperatingVertical.trading.accent
        case .distribution: return BusinessOperatingVertical.distribution.accent
        default: return .indigo
        }
    }

    /// §03 `Channel` chips, in frame order. Empty for a kind without the document.
    var channels: [String] {
        guard let vertical else { return [] }
        return BusinessSetupDocSpec.channels(vertical: vertical)
    }

    /// §02 `Roles on invite`, in frame order.
    var inviteRoles: [BusinessSetupRoleSpec] {
        guard let vertical else { return [] }
        return BusinessSetupDocSpec.roles(vertical: vertical)
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
    /// Every preference key this entry's setup screen is allowed to send. The submit path
    /// filters against it, so a key missing here is silently dropped on the wire.
    var allowedKeys: Set<String> = []
}

enum BusinessSetupCatalog {
    static func forKind(_ kind: BusinessSetupKind) -> BusinessSetupCatalogEntry {
        // Every business family shares the operating preference vocabulary, so the allowlist is
        // the same for all of them. The document emits only these keys.
        let allowed = BusinessSetupDocSpec.preferenceKeys
        if let vertical = kind.vertical {
            return BusinessSetupCatalogEntry(
                familyCode: kind.familyCode,
                title: kind.title,
                subtitle: kind.body,
                momentTypeCode: kind.subtype,
                activateLabel: "Start this business",
                defaultTitle: kind.title,
                defaultPreferences: BusinessSetupDocSpec.defaultPreferences(vertical: vertical),
                allowedKeys: allowed
            )
        }
        return BusinessSetupCatalogEntry(
            familyCode: kind.familyCode,
            title: kind.title,
            subtitle: kind.body,
            momentTypeCode: kind.subtype,
            activateLabel: "Activate",
            defaultTitle: kind.title,
            defaultPreferences: [:],
            allowedKeys: allowed
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
/// Does not accept bare codes (those stay `GroupJoinLink`) to avoid colliding with group invites.
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

    /// Accept bare 8-char codes when the user typed them into Company Setup.
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
    /// Prefer company `/c/` markers, then group `/j/` / bare short codes.
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
