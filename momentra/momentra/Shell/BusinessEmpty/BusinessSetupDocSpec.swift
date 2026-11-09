import Foundation

/// One numbered section of the six-section setup document.
struct BusinessSetupSectionSpec: Identifiable, Equatable {
    let number: String
    let title: String
    let subtitle: String

    var id: String { number }
}

/// One `Roles on invite` chip. `label` is the operator word the frame prints; `code` is the
/// nearest operating capability the backend accepts, because anything else is dropped.
struct BusinessSetupRoleSpec: Identifiable, Equatable, Hashable {
    let code: String
    let label: String

    var id: String { code }
}

/// Live §04 counts, read from the company's masters tables. A zero count is never printed.
struct BusinessSetupMasterCounts: Equatable {
    var skus: Int = 0
    var priceLists: Int = 0
    var warehouses: Int = 0
    var routes: Int = 0
    var principals: Int = 0
    var dealers: Int = 0

    var isEmpty: Bool {
        skus == 0 && priceLists == 0 && warehouses == 0 && routes == 0
            && principals == 0 && dealers == 0
    }
}

/// One §02 invitee, before the invite is redeemed. The role is held as the wire code, not the
/// chip label, so an invite minted for a code the chip set no longer offers round-trips
/// unchanged instead of degrading to the default role.
struct BusinessSetupInvitee: Identifiable, Equatable, Hashable {
    var id: String
    var name: String
    var roleCode: String
    var phone: String = ""

    /// The chip label for this invitee's role; unknown codes still print as words.
    var roleLabel: String { BusinessSetupDocSpec.roleLabel(roleCode) }
}

/// The seven field groups the document edits. Kept as one value so a sheet write and a rerender
/// can never lose a half-typed field.
struct BusinessSetupDocState: Equatable {
    // MARK: Kind

    var vertical: BusinessOperatingVertical = .trading

    // MARK: §01 Business Basics

    var businessName: String = ""
    var companyName: String = ""
    var gstin: String = ""
    var currency: String = BusinessSetupDocSpec.currencyLabels[0]
    var startDate: String = BusinessSetupDocSpec.todayIso()
    var endDate: String = ""
    var description: String = ""
    var location: String = ""

    // MARK: §02 People & Roles

    /// The code of the chip selected in `Roles on invite`; the code is what mints the invite.
    var inviteRole: String = BusinessSetupDocSpec.defaultInviteRole

    /// Everyone §02 has invited. The owner is never in here; they are the row above.
    var invitees: [BusinessSetupInvitee] = []

    // MARK: §03 Channels & Monthly Plan

    var channels: Set<String> = []
    var month: String = BusinessSetupDocSpec.thisMonth()
    var budget: String = ""
    var target: String = ""
    var metric: String = BusinessSetupDocSpec.metricLabel("REVENUE")

    // MARK: §05 GST & Tax Details

    var gstOpen: Bool = true
    var gstTracking: String = "Yes"
    var gstRegistration: String = "Regular"
    var gstState: String = ""
    var gstPricing: String = "GST is added separately"
    var gstEffectiveFrom: String = ""

    init(vertical: BusinessOperatingVertical = .trading, initialName: String = "") {
        self.vertical = vertical
        businessName = initialName
        inviteRole = BusinessSetupDocSpec.defaultInviteRole
    }

    var subtypeCode: String { BusinessSetupDocSpec.subtype(vertical) }

    var roles: [BusinessSetupRoleSpec] { BusinessSetupDocSpec.roles(vertical) }
    var channelChoices: [String] { BusinessSetupDocSpec.channels(vertical: vertical) }

    /// The chip label for the selected code, for the rows and the footnote.
    var inviteRoleLabel: String { BusinessSetupDocSpec.roleLabel(inviteRole) }

    /// Switching the kind chip keeps everything typed, but clears the chip sets that are keyed to
    /// the old vertical — a `Retail dealer` channel is not a channel of a Trading moment.
    mutating func switchVertical(_ next: BusinessOperatingVertical) {
        guard next != vertical else { return }
        vertical = next
        let allowed = Set(channelChoices)
        channels = channels.intersection(allowed)
        // The chip set is identical in both verticals; re-validate anyway so an old saved code can
        // never outlive the chip that showed it.
        if !roles.contains(where: { $0.code == inviteRole }) {
            inviteRole = BusinessSetupDocSpec.defaultInviteRole
        }
    }

    mutating func toggleChannel(_ channel: String) {
        if channels.contains(channel) {
            channels.remove(channel)
        } else {
            channels.insert(channel)
        }
    }

    var hasChannels: Bool { !channels.isEmpty }

    /// §04 rows and §06 activation only need a name to exist; everything else may be blank.
    var canStart: Bool {
        let named = !businessName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        let budgetOk = (Decimal(string: BusinessSetupDocSpec.plainAmount(budget)) ?? 0) > 0
        if vertical.isMarketing {
            let partyOk = !location.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            let startOk = startDate.count >= 10
            return named && partyOk && budgetOk && startOk && hasChannels
        }
        let targetOk = (Decimal(string: BusinessSetupDocSpec.plainAmount(target)) ?? 0) > 0
        return named && hasChannels && budgetOk && targetOk
    }

    /// The body the document sends. `partyName` carries the trading party (the company the moment
    /// trades with) and `identityName` the moment name, exactly as §01 labels them.
    func preferences(ownerName: String) -> [String: Any] {
        BusinessSetupDocSpec.preferences(
            vertical: vertical,
            identityName: businessName,
            partyName: location,
            description: description,
            currency: BusinessSetupDocSpec.currencyCode(currency),
            startDate: startDate,
            endDate: endDate,
            location: location,
            channels: channelChoices.filter { channels.contains($0) },
            monthlyBudget: BusinessSetupDocSpec.plainAmount(budget),
            monthlyTarget: BusinessSetupDocSpec.plainAmount(target),
            targetMetric: BusinessSetupDocSpec.metricCode(metric),
            participants: BusinessSetupDocSpec.participants(ownerName: ownerName, invitees: invitees),
            gstin: gstin,
            gstState: gstState,
            gstRegistration: BusinessSetupDocSpec.gstRegistrationCode(gstRegistration),
            gstTracking: BusinessSetupDocSpec.gstTrackingCode(gstTracking),
            salePriceIncludesGst: BusinessSetupDocSpec.gstPricingCode(gstPricing),
            gstEffectiveFrom: gstEffectiveFrom
        )
    }

    /// Prefill from a saved draft, so reopening a draft continues where the operator stopped.
    mutating func apply(preferences: [String: Any]) {
        func string(_ key: String) -> String? {
            guard let value = preferences[key] as? String else { return nil }
            let trimmed = value.trimmingCharacters(in: .whitespacesAndNewlines)
            return trimmed.isEmpty ? nil : trimmed
        }
        if let subtype = string("subtype"),
           let saved = BusinessOperatingVertical.forSubtype(subtype) {
            vertical = saved
        }
        if let value = string("identityName") { businessName = value }
        if let value = string("partyName") { location = value }
        if let value = string("description") { description = value }
        if let value = string("location"), location.isEmpty { location = value }
        if let value = string("gstin") { gstin = value }
        if let value = string("gstState") { gstState = value }
        if let value = string("startDate") { startDate = value }
        if let value = string("endDate") { endDate = value }
        if let value = string("gstEffectiveFrom") { gstEffectiveFrom = value }
        if let value = string("currency") {
            currency = BusinessSetupDocSpec.currencyLabels.first { $0.hasPrefix(value) } ?? currency
        }
        if let value = preferences["channels"] as? [String], !value.isEmpty {
            channels = Set(value)
        }
        if let value = string("monthlyBudget") { budget = BusinessSetupDocSpec.formatIndianAmount(value) }
        if let value = string("monthlyTarget") { target = BusinessSetupDocSpec.formatIndianAmount(value) }
        if let value = string("targetMetric") { metric = BusinessSetupDocSpec.metricLabel(value) }
        if let value = string("gstTracking") {
            gstTracking = value.uppercased() == "NOT_NOW" ? "Not now" : "Yes"
        }
        if let value = string("gstRegistration") {
            gstRegistration = BusinessSetupDocSpec.gstRegistrationLabels.first {
                BusinessSetupDocSpec.gstRegistrationCode($0) == value.uppercased()
            } ?? gstRegistration
        }
        if let value = string("salePriceIncludesGst") {
            gstPricing = BusinessSetupDocSpec.gstPricingLabels.first {
                BusinessSetupDocSpec.gstPricingCode($0) == value.uppercased()
            } ?? gstPricing
        }
        inviteRole = roles.contains(where: { $0.code == inviteRole })
            ? inviteRole
            : BusinessSetupDocSpec.defaultInviteRole
        invitees = BusinessSetupDocSpec.invitees(preferences: preferences)
    }
}

/// The six sections, the chip sets, the §04 count subtitles, and the preference keys, as pure
/// data — so a unit test can assert them without a SwiftUI render.
enum BusinessSetupDocSpec {
    static let tradingSubtype = "TRADING_WHOLESALE"
    static let distributionSubtype = "DISTRIBUTION_DEALERSHIP"

    static let catalogueInvite = "Name, barcode, MRP, case size, HSN, GST"
    static let warehouseInvite = "Godowns hold stock · routes carry it · branches receive"
    static let networkInvite = "GSTIN on file · ITC posts on GRN, never on PO save"

    // MARK: - Kind

    static let manufacturingPlantSubtype = "STANDARD_PRODUCTION"
    static let manufacturingJobSubtype = "MADE_TO_ORDER"

    static func subtype(_ vertical: BusinessOperatingVertical) -> String {
        switch vertical {
        case .trading: return tradingSubtype
        case .distribution: return distributionSubtype
        case .standardProduction: return manufacturingPlantSubtype
        case .madeToOrder: return manufacturingJobSubtype
        case .clientCampaign: return "CLIENT_CAMPAIGN"
        case .ownBusinessCampaign: return "OWN_BUSINESS_CAMPAIGN"
        case .retailStore: return "RETAIL_STORE"
        case .restaurantFood: return "RESTAURANT_FOOD"
        case .consumerService: return "CONSUMER_SERVICE"
        }
    }

    static func vertical(forSubtype code: String?) -> BusinessOperatingVertical? {
        BusinessOperatingVertical.forSubtype(code)
    }

    static func kindLabel(_ vertical: BusinessOperatingVertical) -> String {
        switch vertical {
        case .trading: return "Trading"
        case .distribution: return "Distribution"
        case .standardProduction: return "Plant"
        case .madeToOrder: return "Workshop"
        case .clientCampaign: return "Client Campaign"
        case .ownBusinessCampaign: return "Own Business Campaign"
        case .retailStore: return "Retail store"
        case .restaurantFood: return "Restaurant"
        case .consumerService: return "Service"
        }
    }

    static let tradingChannels = ["Retail buyers", "Wholesale", "Modern trade", "HoReCa", "Online"]
    static let distributionChannels = ["Retail dealers", "Wholesale", "Modern trade", "HoReCa", "Van sales"]
    static let manufacturingChannels = ["OEM", "Contract", "Job work", "Private label", "Direct"]
    static let madeToOrderChannels = ["Custom jobs", "Contract", "Repair", "Private label", "Direct"]
    static let marketingChannels = [
        "Paid Social", "Search Ads", "Display / Video Ads", "Organic Social",
        "Email", "Influencer", "SEO / Content", "SMS / Messaging",
    ]

    static let retailChannels = ["Walk-in", "Online", "Wholesale", "Marketplace", "Delivery"]
    static let restaurantChannels = ["Dine-in", "Takeaway", "Delivery", "Catering", "Events"]
    static let serviceChannels = ["Appointment", "Home visit", "On-site", "Remote", "Walk-in"]

    static func channels(vertical: BusinessOperatingVertical) -> [String] {
        switch vertical {
        case .trading: return tradingChannels
        case .distribution: return distributionChannels
        case .standardProduction: return manufacturingChannels
        case .madeToOrder: return madeToOrderChannels
        case .clientCampaign, .ownBusinessCampaign: return marketingChannels
        case .retailStore: return retailChannels
        case .restaurantFood: return restaurantChannels
        case .consumerService: return serviceChannels
        }
    }

    static let tradingRoles = [
        BusinessSetupRoleSpec(code: "FINANCE", label: "Finance"),
        BusinessSetupRoleSpec(code: "APPROVER", label: "Approver"),
        BusinessSetupRoleSpec(code: "OPERATIONS_LEAD", label: "Operations"),
        BusinessSetupRoleSpec(code: "CONTRIBUTOR", label: "Contributor"),
    ]

    /// Dist invite chips keep the four mintable API codes; labels follow DST-MASTER Invite & QR.
    static let distributionRoles = [
        BusinessSetupRoleSpec(code: "FINANCE", label: "Manager"),
        BusinessSetupRoleSpec(code: "APPROVER", label: "Warehouse"),
        BusinessSetupRoleSpec(code: "OPERATIONS_LEAD", label: "Route lead"),
        BusinessSetupRoleSpec(code: "CONTRIBUTOR", label: "Salesman"),
    ]

    static let manufacturingRoles = [
        BusinessSetupRoleSpec(code: "FINANCE", label: "Finance"),
        BusinessSetupRoleSpec(code: "APPROVER", label: "Approver"),
        BusinessSetupRoleSpec(code: "OPERATIONS_LEAD", label: "Operations Lead"),
        BusinessSetupRoleSpec(code: "CONTRIBUTOR", label: "Contributor"),
    ]

    static let marketingRoles = [
        BusinessSetupRoleSpec(code: "FINANCE", label: "Finance"),
        BusinessSetupRoleSpec(code: "APPROVER", label: "Approver"),
        BusinessSetupRoleSpec(code: "OPERATIONS_LEAD", label: "Campaign Lead"),
        BusinessSetupRoleSpec(code: "CONTRIBUTOR", label: "Contributor"),
    ]

    static func roles(_ vertical: BusinessOperatingVertical) -> [BusinessSetupRoleSpec] {
        switch vertical {
        case .trading: return tradingRoles
        case .distribution: return distributionRoles
        case .standardProduction, .madeToOrder: return manufacturingRoles
        case .clientCampaign, .ownBusinessCampaign: return marketingRoles
        case .retailStore, .restaurantFood, .consumerService: return tradingRoles
        }
    }

    /// Every chip is one of the four codes `POST /v1/company/invites` accepts
    /// (`OPERATING_INVITE_ROLES`), so nothing the document offers can be silently refused.
    static let defaultInviteRole = "OPERATIONS_LEAD"

    static func roleLabel(_ code: String?, vertical: BusinessOperatingVertical = .trading) -> String {
        guard let code, !code.isEmpty else { return roleLabel(defaultInviteRole, vertical: vertical) }
        if let hit = roles(vertical).first(where: { $0.code == code })?.label { return hit }
        return roles(.trading).first { $0.code == code }?.label ?? humanized(code)
    }

    static func roleCode(_ label: String, vertical: BusinessOperatingVertical = .trading) -> String {
        if let hit = roles(vertical).first(where: { $0.label == label })?.code { return hit }
        return roles(.trading).first { $0.label == label }?.code ?? defaultInviteRole
    }

    /// `FINANCE` and `APPROVER` mint an admin membership; the other two mint a member.
    static func membershipType(for code: String) -> String {
        code == "FINANCE" || code == "APPROVER" ? "ADMIN" : "MEMBER"
    }

    /// What the selected role can reach, printed under the chips.
    static func roleAccess(_ code: String, vertical: BusinessOperatingVertical = .trading) -> String {
        if vertical == .distribution {
            switch code {
            case "FINANCE": return "money, settlements and finance boards"
            case "APPROVER": return "warehouse stock, GRNs and holds"
            case "OPERATIONS_LEAD": return "routes, dispatches, dealers and orders"
            default: return "records route activity; money waits for approval"
            }
        }
        if vertical.isManufacturing {
            switch code {
            case "FINANCE": return "money, settlements and finance boards"
            case "APPROVER": return "approves production and stock adjustments"
            case "OPERATIONS_LEAD": return "jobs, lines, materials and fulfilment"
            default: return "records plant activity; money waits for approval"
            }
        }
        switch code {
        case "FINANCE": return "money, payments and finance boards"
        case "APPROVER": return "approves money and stock adjustments"
        case "OPERATIONS_LEAD": return "sales, stock, purchases and orders"
        default: return "records activity; money waits for approval"
        }
    }

    private static func humanized(_ code: String) -> String {
        code.lowercased()
            .replacingOccurrences(of: "_", with: " ")
            .replacingOccurrences(of: " lead", with: "")
            .capitalized
    }

    /// §04 row four: `Principals & dealers` for trading, `Suppliers & buyers` for distribution.
    static func networkLabel(_ vertical: BusinessOperatingVertical) -> String {
        switch vertical {
        case .trading: return "Principals & dealers"
        case .distribution: return "Suppliers & buyers"
        case .standardProduction, .madeToOrder: return "Vendors & customers"
        case .clientCampaign: return "Clients & vendors"
        case .ownBusinessCampaign: return "Vendors & creators"
        case .retailStore: return "Suppliers & customers"
        case .restaurantFood: return "Vendors & guests"
        case .consumerService: return "Clients & staff"
        }
    }

    // MARK: - Sections

    /// The six sections, in the order the frames stack them. Both verticals carry all six.
    static func sections(_ vertical: BusinessOperatingVertical) -> [BusinessSetupSectionSpec] {
        let noun: String
        switch vertical {
        case .trading: noun = "trading"
        case .distribution: noun = "distribution"
        case .standardProduction: noun = "plant"
        case .madeToOrder: noun = "job order"
        case .clientCampaign: noun = "client campaign"
        case .ownBusinessCampaign: noun = "own campaign"
        case .retailStore: noun = "retail"
        case .restaurantFood: noun = "restaurant"
        case .consumerService: noun = "service"
        }
        if vertical.isMarketing {
            return [
                BusinessSetupSectionSpec(
                    number: "01",
                    title: "Business Basics",
                    subtitle: vertical == .ownBusinessCampaign
                        ? "Name the campaign and what you promote. Mode is fixed for this Moment."
                        : "Name the campaign and the client it is run for. Mode is fixed for this Moment."
                ),
                BusinessSetupSectionSpec(
                    number: "02",
                    title: "People & Roles",
                    subtitle: "Invite teammates and pick a role on Invite & QR. No separate Add person."
                ),
                BusinessSetupSectionSpec(
                    number: "03",
                    title: "Catalogue, Stock & Network",
                    subtitle: vertical == .ownBusinessCampaign
                        ? "Vendors · creators · agency/media · platforms. No client in Own Business."
                        : "Clients, vendors, channels & teams your campaign reads from."
                ),
                BusinessSetupSectionSpec(
                    number: "04",
                    title: "Channels & Monthly Plan",
                    subtitle: vertical == .ownBusinessCampaign
                        ? "Channels, own marketing budget and targets."
                        : "Channels, client commercials, budget and targets."
                ),
                BusinessSetupSectionSpec(number: "05", title: "GST & Tax Details", subtitle: "Set up GST once to make buying and selling easier."),
                BusinessSetupSectionSpec(number: "06", title: "Review your setup", subtitle: ""),
            ]
        }
        let mastersTitle = vertical.isManufacturing
            ? "Parts, Lines & Network"
            : "Catalogue, Warehouses & Network"
        let mastersSubtitle = vertical.isManufacturing
            ? "Parts, lines, stores and vendors your jobs and sales read from."
            : "Masters your orders, dispatches and purchases read from. Each opens its own sheet."
        let channelSubtitle = vertical.isManufacturing
            ? "Where do jobs and sales land? Choose at least one."
            : "Where do you sell? Choose at least one."
        return [
            BusinessSetupSectionSpec(number: "01", title: "Business Basics", subtitle: "A home for this \(noun) business."),
            BusinessSetupSectionSpec(
                number: "02",
                title: "People & Roles",
                subtitle: "Invite teammates and pick a role on Invite & QR. No separate Add person."
            ),
            BusinessSetupSectionSpec(
                number: "03",
                title: "Channels & Monthly Plan",
                subtitle: channelSubtitle
            ),
            BusinessSetupSectionSpec(
                number: "04",
                title: mastersTitle,
                subtitle: mastersSubtitle
            ),
            BusinessSetupSectionSpec(number: "05", title: "GST & Tax Details", subtitle: "Set up GST once to make buying and selling easier."),
            BusinessSetupSectionSpec(number: "06", title: "Review your setup", subtitle: ""),
        ]
    }

    // MARK: - §02 copy

    /// §02's `Default …` footnote, keyed off the default invite role.
    static func defaultRoleNote(_ vertical: BusinessOperatingVertical) -> String {
        let code = defaultInviteRole
        return "Default \(roleLabel(code)) · \(roleAccess(code)). Role applies when they join."
    }

    /// The access line under the `Roles on invite` chips.
    static func inviteAccessNote(_ code: String, vertical: BusinessOperatingVertical = .trading) -> String {
        "Access: \(roleAccess(code, vertical: vertical))."
    }

    static func teamMeta(status: String, role: String) -> String {
        let verb = status.uppercased() == "PENDING" ? "Invite sent" : "Added"
        return "\(verb) · \(role)"
    }

    /// §02 rows for the wire. The owner is always first, because the backend normaliser re-inserts
    /// them if the list omits them and an invitee must never be mistaken for the owner.
    static func participants(ownerName: String, invitees: [BusinessSetupInvitee]) -> [[String: String]] {
        var rows: [[String: String]] = [
            ["displayName": ownerName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                ? "Owner"
                : ownerName, "role": "OWNER_ADMIN"],
        ]
        for invitee in invitees {
            let name = invitee.name.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !name.isEmpty else { continue }
            let code = invitee.roleCode.trimmingCharacters(in: .whitespacesAndNewlines)
            rows.append(["displayName": name, "role": code.isEmpty ? defaultInviteRole : code])
        }
        return rows
    }

    /// Reads `participants` back into §02 rows, skipping the owner row.
    static func invitees(preferences: [String: Any]) -> [BusinessSetupInvitee] {
        guard let rows = preferences["participants"] as? [Any] else { return [] }
        return rows.compactMap { row in
            guard let row = row as? [String: Any],
                  let rawName = row["displayName"] as? String
            else { return nil }
            let name = rawName.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !name.isEmpty else { return nil }
            let code = (row["role"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines).uppercased() ?? ""
            guard code != "OWNER_ADMIN" else { return nil }
            return BusinessSetupInvitee(
                id: "saved:" + name.lowercased(),
                name: name,
                roleCode: code.isEmpty ? defaultInviteRole : code,
                phone: (row["phone"] as? String) ?? ""
            )
        }
    }

    static let approvalWhyTitle = "No approval to start"

    static func approvalWhyBody(owner: String, name: String) -> String {
        let who = owner.isEmpty ? "You" : owner
        let moment = name.isEmpty ? "this moment" : name
        return "\(who) can open \(moment) without a manager. Invite is not approval and not payment."
    }

    /// §02 avatar initials — `Kiran Rao` → `KR`, a single word → its first letter.
    static func initials(_ name: String) -> String {
        let parts = name.split(whereSeparator: { $0 == " " || $0 == "\t" }).filter { !$0.isEmpty }
        guard let first = parts.first else { return "" }
        if parts.count == 1 {
            return String(first.prefix(1)).uppercased()
        }
        return (String(first.prefix(1)) + String(parts[parts.count - 1].prefix(1))).uppercased()
    }

    // MARK: - §03 copy

    static let targetWhyTitle = "How your target is counted"
    static let targetWhyBody =
        "Posted invoices count toward the target. Orders and delivery orders do not. "
        + "Purchases and expenses count once toward the budget."

    static func channelHint(selected: Int) -> String {
        "\(selected) selected. These are the places you sell or serve. They are not a different kind of business."
    }

    static let metricLabels = ["Revenue", "Orders", "Count"]

    static func metricLabel(_ metric: String) -> String {
        switch metric.uppercased() {
        case "REVENUE": return "Revenue"
        case "ORDERS": return "Orders"
        case "CUSTOM_COUNT": return "Count"
        // The frame never shows a raw enum, so anything unrecognised reads as a count.
        default: return metric.isEmpty ? "Revenue" : "Count"
        }
    }

    static func metricCode(_ label: String) -> String {
        switch label {
        case "Orders": return "ORDERS"
        case "Count": return "CUSTOM_COUNT"
        default: return "REVENUE"
        }
    }

    // MARK: - §04 subtitles

    /// §04 row one. With nothing in the catalogue the row falls back to the invitation copy —
    /// a zero is never printed, because `0 SKUs` reads like a real, empty catalogue.
    static func catalogueSubtitle(_ counts: BusinessSetupMasterCounts) -> String {
        guard counts.skus > 0 else { return catalogueInvite }
        var parts = ["\(counts.skus) SKU\(plural(counts.skus))"]
        if counts.priceLists > 0 {
            parts.append("\(counts.priceLists) price list\(plural(counts.priceLists))")
        }
        parts.append("HSN + GST on each")
        return parts.joined(separator: " · ")
    }

    /// §04 row three. Named warehouses first, then the route count.
    static func warehouseSubtitle(names: [String], counts: BusinessSetupMasterCounts) -> String {
        guard counts.warehouses > 0 else { return warehouseInvite }
        let head = names.filter { !$0.isEmpty }.prefix(3).joined(separator: " · ")
        let routes = counts.routes > 0 ? " · \(counts.routes) route\(plural(counts.routes))" : ""
        if head.isEmpty {
            return "\(counts.warehouses) warehouse\(plural(counts.warehouses))\(routes)"
        }
        return head + routes
    }

    /// §04 row four — network contacts for the vertical.
    static func networkSubtitle(_ vertical: BusinessOperatingVertical, counts: BusinessSetupMasterCounts) -> String {
        let primary: String
        let secondary: String
        switch vertical {
        case .trading:
            primary = "principal"; secondary = "dealer"
        case .distribution:
            primary = "supplier"; secondary = "buyer"
        case .standardProduction, .madeToOrder:
            primary = "vendor"; secondary = "customer"
        case .clientCampaign:
            primary = "client"; secondary = "vendor"
        case .ownBusinessCampaign:
            primary = "vendor"; secondary = "creator"
        case .retailStore:
            primary = "supplier"; secondary = "customer"
        case .restaurantFood:
            primary = "vendor"; secondary = "guest"
        case .consumerService:
            primary = "client"; secondary = "staff"
        }
        guard counts.principals > 0 || counts.dealers > 0 else {
            return vertical.isMarketing
                ? "Add the network this campaign reads from"
                : networkInvite
        }
        var parts: [String] = []
        if counts.principals > 0 {
            parts.append("\(counts.principals) \(primary)\(plural(counts.principals))")
        }
        if counts.dealers > 0 {
            parts.append("\(counts.dealers) \(secondary)\(plural(counts.dealers))")
        }
        if !vertical.isMarketing { parts.append("GSTIN on file") }
        return parts.joined(separator: " · ")
    }

    /// §04 row five — the invite role chips, in frame order.
    static func inviteSubtitle(_ vertical: BusinessOperatingVertical) -> String {
        roles(vertical).map(\.label).joined(separator: " · ")
    }

    /// §04's closing footnote; the noun tracks the vertical.
    static func networkFootnote(_ vertical: BusinessOperatingVertical) -> String {
        let noun: String
        switch vertical {
        case .trading: noun = "principals"
        case .distribution: noun = "buyers"
        case .standardProduction, .madeToOrder: noun = "customers"
        case .clientCampaign: noun = "clients"
        case .ownBusinessCampaign: noun = "vendors"
        case .retailStore: noun = "customers"
        case .restaurantFood: noun = "guests"
        case .consumerService: noun = "clients"
        }
        return "Warehouses hold stock; routes carry it; \(noun) carry GSTIN for ITC on GRN. "
            + "GST posts on invoice or GRN — never on SO / PO save."
    }

    // MARK: - §05 copy

    static let gstSkipCaveat =
        "Skipping means tax tracking is disabled in Momentra. It does not mean the business is legally GST-exempt."
    static let gstNote = "Your GSTIN stays the one entered above. It is stored as a reference only."

    static let gstRegistrationLabels = ["Regular", "Composition", "Unregistered", "Not sure"]
    static let gstPricingLabels = ["Prices include GST", "GST is added separately"]
    static let gstTrackingLabels = ["Yes", "Not now"]
    static let currencyLabels = ["INR · Indian Rupee (₹)", "USD · US Dollar", "AED · UAE Dirham"]

    static func currencyCode(_ label: String) -> String {
        let code = label.split(separator: "·").first.map(String.init)?.trimmingCharacters(in: .whitespaces) ?? ""
        return code.isEmpty ? "INR" : code.uppercased()
    }

    static func gstPricingCode(_ label: String) -> String {
        label.lowercased().hasPrefix("prices include") ? "YES" : "NO"
    }

    static func gstRegistrationCode(_ label: String) -> String {
        switch label {
        case "Regular": return "REGULAR"
        case "Composition": return "COMPOSITION"
        case "Unregistered": return "UNREGISTERED"
        case "Not sure": return "NOT_SURE"
        default: return ""
        }
    }

    static func gstTrackingCode(_ label: String) -> String {
        switch label {
        case "Yes": return "YES"
        case "Not now": return "NOT_NOW"
        default: return ""
        }
    }

    // MARK: - §06 copy

    /// A draft is started; a live moment is saved. Printing `Start this business` over a moment
    /// that is already running would read like a second, duplicate business.
    static func activateLabel(status: String) -> String {
        status.uppercased() == "ACTIVE" ? "Save changes" : "Start this business"
    }

    static func activatingLabel(status: String) -> String {
        status.uppercased() == "ACTIVE" ? "Saving…" : "Starting…"
    }

    static func statusPillLabel(status: String) -> String {
        status.uppercased() == "ACTIVE" ? "Business is live" : "Setup in progress"
    }

    /// Only a draft may be discarded on Close or promoted on Start; an ACTIVE moment is edited in
    /// place, because discarding it would delete a record the operator is still running.
    static func isDiscardable(_ status: String) -> Bool {
        status.uppercased() == "DRAFT"
    }

    static func isPromotable(_ status: String) -> Bool {
        status.uppercased() == "DRAFT"
    }

    /// §06's second line: `Trading · INR · From 12 October 2026 · Still open`.
    static func momentLine(
        vertical: BusinessOperatingVertical,
        currency: String,
        startDate: String,
        endDate: String
    ) -> String {
        let kind = kindLabel(vertical)
        let code = currency.isEmpty ? "INR" : currency
        let from = startDate.isEmpty ? "Today" : dateLabel(startDate)
        let to = endDate.isEmpty ? "Still open" : "Until \(dateLabel(endDate))"
        return "\(kind) · \(code) · From \(from) · \(to)"
    }

    /// §06's `October 2026 budget` / `October 2026 sales target` key prefixes.
    static func monthLabel(_ yearMonth: String) -> String {
        let parts = yearMonth.split(separator: "-")
        guard parts.count >= 2,
              let year = Int(parts[0]),
              let month = Int(parts[1]),
              (1 ... 12).contains(month)
        else { return yearMonth.isEmpty ? "This month" : yearMonth }
        return "\(monthName(month)) \(year)"
    }

    /// `2026-10-12` → `12 October 2026`, exactly as the frame prints it.
    static func dateLabel(_ iso: String) -> String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "yyyy-MM-dd"
        let head = String(iso.prefix(10))
        guard let date = formatter.date(from: head) else { return iso }
        let calendar = Calendar(identifier: .gregorian)
        let day = calendar.component(.day, from: date)
        let month = calendar.component(.month, from: date)
        let year = calendar.component(.year, from: date)
        return "\(day) \(monthName(month)) \(year)"
    }

    static func todayIso() -> String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter.string(from: Date())
    }

    static func thisMonth() -> String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "yyyy-MM"
        return formatter.string(from: Date())
    }

    private static func monthName(_ month: Int) -> String {
        let names = [
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December",
        ]
        guard (1 ... 12).contains(month) else { return "" }
        return names[month - 1]
    }

    // MARK: - Money

    /// Digits and at most one dot: what the amount inputs actually hold.
    static func plainAmount(_ raw: String) -> String {
        var out = ""
        var sawDot = false
        for character in raw {
            if character.isNumber {
                out.append(character)
            } else if character == ".", !sawDot {
                out.append(character)
                sawDot = true
            }
        }
        return out
    }

    /// Indian grouping — `6000000` → `60,00,000` — so the field reads the way the frame shows it.
    static func formatIndianAmount(_ raw: String) -> String {
        let cleaned = plainAmount(raw)
        guard !cleaned.isEmpty else { return "" }
        let split = cleaned.split(separator: ".", maxSplits: 1, omittingEmptySubsequences: false)
        let integerRaw = String(split[0])
        let fraction = split.count > 1 ? "." + String(split[1].prefix(2)) : ""
        var digits = integerRaw
        while digits.count > 1, digits.hasPrefix("0") {
            digits.removeFirst()
        }
        guard !digits.isEmpty else { return fraction.isEmpty ? "" : "0" + fraction }
        return groupIndian(digits) + fraction
    }

    private static func groupIndian(_ digits: String) -> String {
        guard digits.count > 3 else { return digits }
        let tail = String(digits.suffix(3))
        var head = String(digits.dropLast(3))
        var groups: [String] = []
        while head.count > 2 {
            groups.insert(String(head.suffix(2)), at: 0)
            head = String(head.dropLast(2))
        }
        if !head.isEmpty { groups.insert(head, at: 0) }
        return groups.joined(separator: ",") + "," + tail
    }

    // MARK: - Preferences

    /// Every preference key the document emits. Asserted as a subset of the catalogue allowlist so
    /// a new field can never be silently dropped on its way to the backend.
    static let preferenceKeys: Set<String> = [
        "subtype", "identityName", "partyName", "description", "currency",
        "startDate", "endDate", "location", "channels", "monthlyBudget",
        "monthlyTarget", "targetMetric", "participants", "counterparties", "gstin",
        "gstState", "gstStateCode", "gstRegistration", "gstTracking", "salePriceIncludesGst",
        "purchasePriceIncludesGst", "gstEffectiveFrom",
    ]

    static func defaultPreferences(vertical: BusinessOperatingVertical) -> [String: Any] {
        preferences(
            vertical: vertical,
            identityName: kindLabel(vertical),
            partyName: "",
            currency: "INR",
            startDate: todayIso()
        )
    }

    static func preferences(
        vertical: BusinessOperatingVertical,
        identityName: String,
        partyName: String,
        description: String = "",
        currency: String = "INR",
        startDate: String = "",
        endDate: String = "",
        location: String = "",
        channels: [String] = [],
        monthlyBudget: String = "",
        monthlyTarget: String = "",
        targetMetric: String = "REVENUE",
        participants: [[String: String]] = [],
        gstin: String = "",
        gstState: String = "",
        gstRegistration: String = "",
        gstTracking: String = "",
        salePriceIncludesGst: String = "",
        purchasePriceIncludesGst: String? = nil,
        gstEffectiveFrom: String = ""
    ) -> [String: Any] {
        [
            "subtype": subtype(vertical),
            "identityName": identityName.trimmingCharacters(in: .whitespacesAndNewlines),
            "partyName": partyName.trimmingCharacters(in: .whitespacesAndNewlines),
            "description": description.trimmingCharacters(in: .whitespacesAndNewlines),
            "currency": currency.isEmpty ? "INR" : currency.uppercased(),
            "startDate": startDate,
            "endDate": endDate,
            "location": location.trimmingCharacters(in: .whitespacesAndNewlines),
            "channels": channels,
            "monthlyBudget": monthlyBudget,
            "monthlyTarget": monthlyTarget,
            "targetMetric": targetMetric.uppercased().isEmpty ? "REVENUE" : targetMetric.uppercased(),
            "participants": participants,
            "counterparties": [],
            "gstin": gstin.trimmingCharacters(in: .whitespacesAndNewlines).uppercased(),
            // Canonical name plus the two-digit code, so jurisdiction never depends on spelling.
            "gstState": IndiaStateCatalog.canonical(gstState).isEmpty
                ? gstState.trimmingCharacters(in: .whitespacesAndNewlines)
                : IndiaStateCatalog.canonical(gstState),
            "gstStateCode": IndiaStateCatalog.codeOf(gstState),
            "gstRegistration": gstRegistration.uppercased(),
            "gstTracking": gstTracking.uppercased(),
            "salePriceIncludesGst": salePriceIncludesGst.uppercased(),
            "purchasePriceIncludesGst": (purchasePriceIncludesGst ?? salePriceIncludesGst).uppercased(),
            "gstEffectiveFrom": gstEffectiveFrom,
        ]
    }

    private static func plural(_ count: Int) -> String {
        count == 1 ? "" : "s"
    }
}
