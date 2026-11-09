import Foundation
import SwiftUI

// MARK: - Vertical

/// The two operating verticals this build ships. Both read the same projection; they differ in
/// accent, copy, and which noun the story uses.
enum BusinessOperatingVertical: String, CaseIterable {
    case trading = "TRADING_WHOLESALE"
    case distribution = "DISTRIBUTION_DEALERSHIP"

    var domain: BusinessDomain {
        switch self {
        case .trading: return .trading
        case .distribution: return .distribution
        }
    }

    var accent: Color { BusinessTokens.Colors.accent(for: domain) }

    var canvas: Color {
        switch self {
        case .trading: return BusinessTokens.Colors.tradingCanvas
        case .distribution: return BusinessTokens.Colors.distributionCanvas
        }
    }

    var surface: Color {
        switch self {
        case .trading: return BusinessTokens.Colors.tradingSurface
        case .distribution: return BusinessTokens.Colors.distributionSurface
        }
    }

    var inset: Color {
        switch self {
        case .trading: return BusinessTokens.Colors.tradingInset
        case .distribution: return BusinessTokens.Colors.distributionInset
        }
    }

    var border: Color { BusinessTokens.Colors.borderDefault }
    var ink: Color { BusinessTokens.Colors.textPrimary }
    var body: Color { BusinessTokens.Colors.textSecondary }
    var muted: Color { BusinessTokens.Colors.textMuted }
    var onAccent: Color {
        switch self {
        case .trading: return BusinessTokens.Colors.tradingOnAccent
        case .distribution: return BusinessTokens.Colors.distributionOnAccent
        }
    }

    /// `Buyers` for a wholesale trade, `Dealers` for a dealership.
    var partyNoun: String {
        switch self {
        case .trading: return "Buyers"
        case .distribution: return "Dealers"
        }
    }

    var motionLine: String {
        switch self {
        case .trading: return "Source. Sell. Collect."
        case .distribution: return "Source. Move. Settle."
        }
    }

    var storyNoun: String { self == .trading ? "trading" : "distribution" }

    /// The eight chapters the backend composes for this vertical.
    var storyPages: [String] {
        switch self {
        case .trading: return ["Moment", "Motion", "Orders", "Money", "Stock", "Network", "Attention", "So far"]
        case .distribution: return ["Moment", "Motion", "Dealers", "Money", "Stock", "Network", "Attention", "So far"]
        }
    }

    static func forSubtype(_ subtype: String?) -> BusinessOperatingVertical? {
        guard let subtype else { return nil }
        return BusinessOperatingVertical(rawValue: subtype.uppercased())
    }
}

// MARK: - Sheet catalogue

/// One `View all ›` surface. The eighteen per vertical map onto a board section `code`, or onto a
/// Pulse metric group when `pulseSurface` is true.
struct OperatingSheetSpec: Identifiable, Equatable {
    let code: String
    let title: String
    let eyebrow: String
    let emptyTitle: String
    let emptyBody: String
    var pulseSurface: Bool = false

    var id: String { code }
}

/// The eighteen surfaces in Figma order — identical codes for both verticals, because the board
/// contract is shared. Copy lives in the vertical, not the code.
enum OperatingSheetCatalog {
    static let all: [OperatingSheetSpec] = [
        OperatingSheetSpec(
            code: "TARGET_BUDGET",
            title: "Target & Budget",
            eyebrow: "What this period is aiming at",
            emptyTitle: "No target set",
            emptyBody: "Set a sales target and a period budget to measure the business against them.",
            pulseSurface: true
        ),
        OperatingSheetSpec(
            code: "FINANCE",
            title: "Finance",
            eyebrow: "Recorded sales, money in, money still out",
            emptyTitle: "No finance recorded",
            emptyBody: "Sales, collections, and payables appear once recorded.",
            pulseSurface: true
        ),
        OperatingSheetSpec(
            code: "PAYMENT_READINESS",
            title: "Payment Readiness",
            eyebrow: "What can be collected and what must be paid",
            emptyTitle: "Nothing awaiting settlement",
            emptyBody: "Open receivables and payables appear here as they are recorded.",
            pulseSurface: true
        ),
        OperatingSheetSpec(
            code: "ATTENTION",
            title: "Needs Attention",
            eyebrow: "Overdue, blocked, and waiting on a decision",
            emptyTitle: "Nothing is waiting",
            emptyBody: "Approvals, overdue items, and blockers will show here.",
            pulseSurface: true
        ),
        OperatingSheetSpec(
            code: "ACTIVITY",
            title: "Activity",
            eyebrow: "Everything recorded in this moment",
            emptyTitle: "Nothing recorded yet",
            emptyBody: "Sales, purchases, and money collection will show here.",
            pulseSurface: true
        ),
        OperatingSheetSpec(
            code: "GST",
            title: "GST Position",
            eyebrow: "Indicative tax on recorded records",
            emptyTitle: "No GST recorded",
            emptyBody: "GST appears on records once tracking is on and a rate is supplied.",
            pulseSurface: true
        ),
        OperatingSheetSpec(
            code: "orders",
            title: "Orders & Commitments",
            eyebrow: "Commitments are not revenue until billed",
            emptyTitle: "No orders yet",
            emptyBody: "Buyer orders and their dispatches will show here."
        ),
        OperatingSheetSpec(
            code: "stock",
            title: "Stock & Products",
            eyebrow: "Owned on-hand only — consignment stays out",
            emptyTitle: "No stock recorded",
            emptyBody: "Open stock, purchases, and dispatches build the on-hand position."
        ),
        OperatingSheetSpec(
            code: "network",
            title: "Buyers, Suppliers & Channels",
            eyebrow: "Who the business runs through",
            emptyTitle: "No people yet",
            emptyBody: "Named buyers, suppliers, and channels appear as you record them."
        ),
        OperatingSheetSpec(
            code: "M4",
            title: "Open Commitments",
            eyebrow: "What is still owed in either direction",
            emptyTitle: "No open commitments",
            emptyBody: "Open orders, unsettled money, and dispatches in transit appear here."
        ),
        OperatingSheetSpec(
            code: "L1",
            title: "Current Business",
            eyebrow: "Where the business stands today",
            emptyTitle: "Nothing recorded yet",
            emptyBody: "Board updates and the live position will show here."
        ),
        OperatingSheetSpec(
            code: "L2",
            title: "Buyers & Channels",
            eyebrow: "Attributed sales by channel",
            emptyTitle: "No channels recorded",
            emptyBody: "Channel-attributed sales appear once a channel is chosen."
        ),
        OperatingSheetSpec(
            code: "L3",
            title: "Money Moving",
            eyebrow: "Collected, paid, and the pay lag between",
            emptyTitle: "No settlement yet",
            emptyBody: "Collections and payments will show here."
        ),
        OperatingSheetSpec(
            code: "L4",
            title: "People & Activity",
            eyebrow: "Who recorded what",
            emptyTitle: "No activity yet",
            emptyBody: "Stock moves, issues, and updates will show here."
        ),
        OperatingSheetSpec(
            code: "ME1",
            title: "Monthly Memory",
            eyebrow: "Each month this business has recorded",
            emptyTitle: "No month recorded",
            emptyBody: "Months appear once records exist in them."
        ),
        OperatingSheetSpec(
            code: "ME2",
            title: "Money Memory",
            eyebrow: "Collected, paid, and returned",
            emptyTitle: "No money memory",
            emptyBody: "Settlements and returns will show here."
        ),
        OperatingSheetSpec(
            code: "fulfilment",
            title: "Fulfilment Memory",
            eyebrow: "Delivered, received, and how late",
            emptyTitle: "No fulfilment recorded",
            emptyBody: "Dispatch and receipt status build this picture."
        ),
        OperatingSheetSpec(
            code: "ME4",
            title: "Patterns & Decisions",
            eyebrow: "What repeats and what is waiting on approval",
            emptyTitle: "No pattern yet",
            emptyBody: "Repeated issues and pending approvals will show here."
        ),
    ]

    /// The six codes that read a Pulse metric group rather than a board section.
    static let pulseCodes: Set<String> = ["TARGET_BUDGET", "FINANCE", "PAYMENT_READINESS", "ATTENTION", "ACTIVITY", "GST"]

    static func spec(forCode code: String) -> OperatingSheetSpec? {
        all.first { $0.code == code }
    }

    /// Board section code → the surface it opens. The Pulse surfaces are opened by their own
    /// buttons, so only the twelve board codes route here.
    static func code(forSection sectionCode: String) -> String? {
        guard all.contains(where: { $0.code == sectionCode }) else { return nil }
        return sectionCode
    }
}

// MARK: - Capture layout

/// The twelve QA capture frames — `TRD-QA-01..12` / `DST-QA-01..12`.
enum OperatingCaptureLayout: String, CaseIterable {
    case sale = "SALE"
    case purchase = "PURCHASE"
    case collection = "COLLECTION"
    case payment = "PAYMENT"
    case spend = "SPEND"
    case stock = "STOCK"
    case saleReturn = "RETURN"
    case budget = "BUDGET"
    case update = "UPDATE"
    case approval = "APPROVAL"
    case order = "ORDER"
    case shipment = "SHIPMENT"

    var qaCode: String {
        let index = (Self.allCases.firstIndex(of: self) ?? 0) + 1
        return String(format: "QA-%02d", index)
    }

    var label: String {
        switch self {
        case .sale: return "Sale"
        case .purchase: return "Purchase"
        case .collection: return "Collection"
        case .payment: return "Payment"
        case .spend: return "Expense"
        case .stock: return "Stock"
        case .saleReturn: return "Return"
        case .budget: return "Budget"
        case .update: return "Update / Issue"
        case .approval: return "Approval / Reimbursement"
        case .order: return "Order"
        case .shipment: return "Delivery"
        }
    }

    static func forMode(_ mode: String) -> OperatingCaptureLayout? {
        switch mode.trimmingCharacters(in: .whitespacesAndNewlines).uppercased() {
        case "SALE", "SERVICE": return .sale
        case "PURCHASE", "VENDOR": return .purchase
        case "COLLECTION": return .collection
        case "PAYMENT", "REIMBURSE": return .payment
        case "SPEND": return .spend
        case "STOCK", "TRANSFER", "HOLD": return .stock
        case "RETURN": return .saleReturn
        case "BUDGET", "TARGET": return .budget
        case "UPDATE", "ISSUE": return .update
        case "APPROVAL": return .approval
        case "ORDER": return .order
        case "SHIPMENT", "DELIVERY": return .shipment
        default: return nil
        }
    }

    /// The three modes that have their own frame on top of the twelve.
    static let holdMode = "HOLD"
    static let transferMode = "TRANSFER"
    static let reprintMode = "REPRINT"

    static let holdChoices = ["No hold", "Hold order", "Hold dispatch"]
    static let reprintKinds = ["INVOICE", "CHALLAN"]
}

// MARK: - Masters

/// The six masters tabs (`2566:31888`-`2566:32380`, `2558:29420`-`2558:29912`).
enum OperatingMasterTab: String, CaseIterable, Identifiable {
    case principals
    case catalogue
    case locations
    case priceLists
    case invite

    var id: String { rawValue }

    var title: String {
        switch self {
        case .principals: return "Principals & dealers"
        case .catalogue: return "Catalogue & SKU"
        case .locations: return "Warehouses & routes"
        case .priceLists: return "Price lists"
        case .invite: return "Invite & QR"
        }
    }

    var singular: String {
        switch self {
        case .principals: return "principal or dealer"
        case .catalogue: return "SKU"
        case .locations: return "warehouse or route"
        case .priceLists: return "price list"
        case .invite: return "invite"
        }
    }

    var note: String {
        switch self {
        case .principals:
            return "Identity is stable here: a dealer row is referenced by id, never by its typed name."
        case .catalogue:
            return "A SKU carries unit precision and standing prices. A posted bill never rewrites it."
        case .locations:
            return "A route belongs to a warehouse. Moving stock between them is a transfer, not a sale."
        case .priceLists:
            return "A price list is effective-dated. Two lists may overlap in name but never in a day."
        case .invite:
            return "Invite grants a role and its access. Joining ≠ approval ≠ payment."
        }
    }

    var kinds: Set<String> {
        switch self {
        case .principals: return ["PRINCIPAL", "DEALER", "SUPPLIER", "BUYER", "CUSTOMER", "CHANNEL"]
        case .catalogue: return ["SKU", "PRODUCT", "BATCH"]
        case .locations: return ["WAREHOUSE", "LOCATION", "ROUTE"]
        case .priceLists, .invite: return []
        }
    }

    var defaultKind: String {
        switch self {
        case .principals: return "DEALER"
        case .catalogue: return "SKU"
        case .locations: return "WAREHOUSE"
        case .priceLists: return "PRICE_LIST"
        case .invite: return "CHANNEL"
        }
    }
}

// MARK: - Drafts (pure mapping)

/// What the master form collects, independent of SwiftUI, so the wire body can be tested.
struct OperatingMasterDraft {
    var kind: String
    var name: String = ""
    var code: String = ""
    var unit: String = ""
    var sellPrice: String = ""
    var purchasePrice: String = ""
    var mrp: String = ""
    var hsn: String = ""
    var gstRate: String = ""
    var gstin: String = ""
    var billingState: String = ""
    var parentMasterId: String = ""
    var momentId: String? = nil

    /// Blank means "not supplied", never an empty value on the wire.
    private func nz(_ raw: String) -> String? {
        let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? nil : trimmed
    }

    /// GST applicability is derived: a rate is the only thing that makes a master taxable.
    var gstApplicable: String? { nz(gstRate) == nil ? nil : "YES" }

    var canSave: Bool { !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }

    func toCreateBody() -> OperatingMasterCreateBody {
        OperatingMasterCreateBody(
            kind: kind,
            name: name.trimmingCharacters(in: .whitespacesAndNewlines),
            code: nz(code),
            momentId: momentId,
            unit: nz(unit),
            sellPrice: nz(sellPrice),
            mrp: nz(mrp),
            purchasePrice: nz(purchasePrice),
            gstApplicable: gstApplicable,
            gstRate: nz(gstRate),
            hsn: nz(hsn),
            gstin: nz(gstin),
            billingState: nz(billingState),
            parentMasterId: nz(parentMasterId)
        )
    }

    func toUpdateBody(expectedVersion: Int) -> OperatingMasterUpdateBody {
        OperatingMasterUpdateBody(
            name: name.trimmingCharacters(in: .whitespacesAndNewlines),
            code: nz(code),
            unit: nz(unit),
            sellPrice: nz(sellPrice),
            mrp: nz(mrp),
            purchasePrice: nz(purchasePrice),
            gstApplicable: gstApplicable,
            gstRate: nz(gstRate),
            hsn: nz(hsn),
            gstin: nz(gstin),
            billingState: nz(billingState),
            parentMasterId: nz(parentMasterId),
            expectedVersion: expectedVersion
        )
    }
}

// MARK: - Contract normalisation

/// The capture layouts speak in operator words (`In`, `Out`, `Owned`) while the contract
/// accepts a fixed vocabulary (`ADD`, `REDUCE`, `OWNED`). The mapping lives here, in pure code,
/// so both a test and the sheet can use the same one — a wrong literal is a rejected write.
enum OperatingContractVocabulary {
    /// `SET` replaces the count, `ADD`/`REDUCE` move it, `TRANSFER` moves it between places.
    static func stockAction(_ raw: String) -> String {
        switch raw.trimmingCharacters(in: .whitespaces).uppercased() {
        case "IN", "ADD", "RECEIVED": return "ADD"
        case "OUT", "REDUCE", "ISSUED": return "REDUCE"
        case "SET", "COUNT": return "SET"
        case "TRANSFER", "MOVE": return "TRANSFER"
        default: return "ADD"
        }
    }

    /// The server accepts these reasons only; anything else is folded into `correction`.
    static func stockReason(_ raw: String) -> String? {
        let trimmed = raw.trimmingCharacters(in: .whitespaces)
        guard !trimmed.isEmpty else { return nil }
        switch trimmed.lowercased() {
        case "opening": return "opening"
        case "stocktake", "count": return "stocktake"
        case "damage", "damaged": return "damage"
        case "waste": return "waste"
        case "return", "returned": return "return"
        case "correction", "replenishment", "replenish": return "correction"
        case "transfer", "move": return "TRANSFER"
        default: return "correction"
        }
    }

    /// Consignment stock is never counted as owned on-hand, so the two words must not drift.
    static func ownership(_ raw: String) -> String {
        raw.trimmingCharacters(in: .whitespaces).uppercased().hasPrefix("CONSIGN") ? "CONSIGNMENT" : "OWNED"
    }

    /// `No hold` is a release, never an absence of an answer.
    static func holdStatus(_ raw: String) -> String {
        raw.trimmingCharacters(in: .whitespaces).lowercased() == "no hold" ? "RELEASED" : "HELD"
    }
}

// MARK: - Number formatting

/// Money is formatted per locale; a missing figure is an em dash, never a zero.
enum OperatingMoneyFormat {
    static func format(_ currency: String?, _ raw: String?, visible: Bool = true) -> String {
        guard visible, let raw, !raw.isEmpty else { return "—" }
        guard let value = Decimal(string: raw) else { return raw }
        let code = currency?.uppercased() ?? "INR"
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        formatter.minimumFractionDigits = 0
        formatter.maximumFractionDigits = 2
        formatter.locale = Locale(identifier: code == "INR" ? "en_IN" : "en_US")
        let text = formatter.string(from: value as NSDecimalNumber) ?? raw
        return code == "INR" ? "₹\(text)" : "\(text) \(code)"
    }

    /// The tile value plus its unit, except when the value already reads as a sentence
    /// (`Not recorded`), which must stand alone.
    static func tileValue(_ value: String, unit: String?) -> String {
        guard let unit, !unit.isEmpty, value != "Not recorded" else { return value }
        return "\(value) \(unit)"
    }
}
