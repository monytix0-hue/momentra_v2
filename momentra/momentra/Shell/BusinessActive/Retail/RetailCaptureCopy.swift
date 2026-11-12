import SwiftUI

/// Mirrored Figma RTL-QA-02…10 copy catalog (money + ops).
/// Structure mirrors `ServiceCaptureCopy` — rose Harbor Mart, never teal Service.
/// Tagline: Stock. Sell. Settle.
enum RetailCaptureCopy {
    static let tagline = "Stock. Sell. Settle."
    static let draftFooter = "Draft preserved if you close or open a picker"
    static let savedToast = "✓ Saved · sheet closes and returns back"
    static let moreDetailsHint = "Notes · Proof · Tags"
    static let purchaseMoreDetailsHint = "Supplier GSTIN 27 · intra-state · Regular"
    static let stockMoreDetailsHint = "Transfer stock · between locations"
    static let statusMoreDetailsHint = "Notes · Photo · Tags"
    static let orderSoMoreDetailsHint = "Notes · Delivery · Tags"
    static let orderPoMoreDetailsHint = "Notes · Terms · Tags"
    static let budgetMoreDetailsHint = "Notes · Attachment · Tags"
    static let issueMoreDetailsHint = "Notes · Evidence · Tags"
    static let soCommitmentFooter = "Commitment only · revenue, stock & GST post on Sale / POS"
    static let poCommitmentFooter = "Commitment only · AP, stock & input GST post on Purchase / GRN"

    /// Harbor Mart fixtures for RTL-QA-09 / QA-10 visual parity (projection overrides when present).
    static let budgetFixturePrevious = "500000"
    static let budgetFixtureNew = "600000"
    static let budgetFixtureReason = "Festive demand"
    static let budgetFixtureOwner = "Kiran Rao"
    static let issueFixtureTitle = "POS shift unreconciled"
    static let issueFixtureDetail = "Cash vs UPI variance"
    static let issueFixtureCode = "REG-2"
    static let issueFixtureOwner = "Kiran Rao"
    static let issueFixtureLink = "Register 2 · today"

    /// Money modes for Phase 3 Quick Adds.
    static let moneyModes: Set<String> = ["PURCHASE", "COLLECTION", "PAYMENT"]

    /// Phase 4 ops QA modes — SO/PO/Stock/Status.
    static let opsModes: Set<String> = ["ORDER", "STOCK", "STATUS"]

    static let soCatalogueCategories = ["Staples", "Snacks", "Personal care", "Beverages", "Home care"]
    static let catalogueCategories = soCatalogueCategories

    /// Subcategory chips keyed by Category (payload.subcategory).
    static func catalogueSubcategories(_ category: String) -> [String] {
        switch category {
        case "Staples": return ["Grains", "Oils", "Pulses", "Spices", "Salt and sugar"]
        case "Snacks": return ["Biscuits", "Namkeen", "Chips", "Sweets"]
        case "Personal care": return ["Soap", "Hair", "Oral", "Skin"]
        case "Beverages": return ["Soft drinks", "Juice", "Tea and coffee", "Water"]
        case "Home care": return ["Cleaners", "Detergent", "Kitchen"]
        default: return []
        }
    }

    static func defaultSubcategory(_ category: String) -> String {
        catalogueSubcategories(category).first ?? ""
    }
    static let poStockCategories = ["Staples", "Beverages", "Dairy", "Packaging", "Home care", "Consumables"]

    static func modeChips(_ mode: String) -> [String] {
        switch mode.uppercased() {
        case "PURCHASE": return ["Goods", "Supplies"]
        case "COLLECTION": return []
        case "PAYMENT": return []
        case "STOCK": return ["Set current", "Add", "Reduce"]
        case "ORDER": return ["Sales order", "Purchase order"]
        case "STATUS": return ["Dispatch", "Fulfil", "Pick"]
        case "BUDGET", "TARGET": return ["Sales target", "Budget"]
        case "ISSUE", "UPDATE", "APPROVAL": return ["Issue", "Approval", "Update"]
        case "SPEND": return ["Store", "Ops", "Other"]
        default: return []
        }
    }

    static func orderStateChips(isPurchase: Bool) -> [String] {
        isPurchase ? ["Sent", "Draft", "Partial"] : ["Confirmed", "Hold", "Draft"]
    }

    static func statusNextChips() -> [String] {
        ["Ready to pick", "Picking", "Ready", "Dispatched"]
    }

    static func tenderChips(_ mode: String) -> [String] {
        switch mode.uppercased() {
        case "PURCHASE": return ["Pay later", "Pay now", "Part"]
        case "COLLECTION": return ["UPI", "Cash", "Card", "Bank"]
        case "PAYMENT": return ["Bank", "UPI", "Cash", "Cheque"]
        default: return ["UPI", "Cash", "Card"]
        }
    }

    static func defaultTender(_ mode: String) -> String {
        switch mode.uppercased() {
        case "PURCHASE": return "Pay later"
        case "COLLECTION": return "UPI"
        case "PAYMENT": return "Bank"
        default: return "UPI"
        }
    }

    static func sheetTitle(_ mode: String, orderSide: String? = nil) -> String {
        switch mode.uppercased() {
        case "PURCHASE": return "Record Purchase"
        case "COLLECTION": return "Record Collection"
        case "PAYMENT": return "Record Payment"
        case "SPEND": return "Record Expense"
        case "STOCK": return "Stock Update"
        case "ORDER":
            return orderSide == "Purchase order" ? "Purchase Order" : "Sales Order"
        case "STATUS": return "Status / Assignment"
        case "BUDGET", "TARGET": return "Budget / Target Revision"
        case "ISSUE", "UPDATE", "APPROVAL": return "Issue / Approval / Update"
        default: return mode
        }
    }

    static func qaCode(_ mode: String) -> String {
        switch mode.uppercased() {
        case "PURCHASE": return "RTL-QA-02"
        case "COLLECTION": return "RTL-QA-03"
        case "PAYMENT": return "RTL-QA-04"
        case "SPEND": return "RTL-QA-05"
        case "STOCK": return "RTL-QA-06"
        case "ORDER": return "RTL-QA-07"
        case "STATUS": return "RTL-QA-08"
        case "BUDGET", "TARGET": return "RTL-QA-09"
        case "ISSUE", "UPDATE", "APPROVAL": return "RTL-QA-10"
        default: return "RTL-QA"
        }
    }

    /// Context strip under title — Harbor Mart fixtures bind to live brand when available.
    /// Figma money strips use store+area (`Harbor Mart Andheri`); Status/Budget/Issue use brand only.
    static func contextLine(
        _ mode: String,
        brand: String,
        location: String = "Back store",
        orderSide: String? = nil,
        storeArea: String = "Andheri"
    ) -> String {
        let name = brand.isEmpty ? "Harbor Mart" : brand
        let area = storeArea.isEmpty ? "Andheri" : storeArea
        let storeLine = Self.storeLine(brand: name, area: area)
        switch mode.uppercased() {
        case "PURCHASE":
            return "\(location.isEmpty ? "Back store" : location) · \(storeLine) · GST on"
        case "COLLECTION":
            return "Register 2 · \(storeLine) · AR settle"
        case "PAYMENT":
            return "AP desk · \(storeLine) · settle supplier"
        case "STOCK":
            return "Floor count · Register aisle · \(area)"
        case "ORDER":
            if orderSide == "Purchase order" {
                return "Supplier commitment · \(storeLine) · not AP"
            }
            return "Customer commitment · \(storeLine) · not revenue"
        case "STATUS":
            return "Floor board · \(Self.brandOnly(name)) · ops only"
        case "SPEND":
            return "Store ops · \(Self.brandOnly(name)) · cost"
        case "BUDGET", "TARGET":
            return "Owner plan · \(Self.brandOnly(name)) · versioned"
        case "ISSUE", "UPDATE", "APPROVAL":
            return "Register 2 · \(Self.brandOnly(name)) · audit trail"
        default:
            return "\(name) · \(tagline)"
        }
    }

    /// Android `RetailCaptureCopy.storeLine` — Harbor Mart → `Harbor Mart Andheri` when area absent.
    private static func storeLine(brand: String, area: String) -> String {
        if brand.localizedCaseInsensitiveContains(area) { return brand }
        if brand.compare("Harbor Mart", options: .caseInsensitive) == .orderedSame {
            return "Harbor Mart \(area)"
        }
        return brand
    }

    private static func brandOnly(_ storeLine: String) -> String {
        if storeLine.localizedCaseInsensitiveContains("Harbor Mart") { return "Harbor Mart" }
        if let space = storeLine.lastIndex(of: " ") {
            let head = String(storeLine[..<space])
            return head.isEmpty ? storeLine : head
        }
        return storeLine.isEmpty ? "Harbor Mart" : storeLine
    }

    static func amountLabel(_ mode: String) -> String {
        switch mode.uppercased() {
        case "PURCHASE": return "Purchase total"
        case "COLLECTION": return "Collect now"
        case "PAYMENT": return "Pay now"
        case "SPEND": return "Expense"
        default: return "Amount"
        }
    }

    static func why(_ mode: String, orderSide: String? = nil) -> String {
        switch mode.uppercased() {
        case "PURCHASE":
            return "Stock += accepted qty only on receipt. AP from posted purchase; payment allocates later — never double-cost."
        case "COLLECTION":
            return "Allocates open AR — never creates new sales. Receipt evidence ≠ separate revenue."
        case "PAYMENT":
            return "Settles open AP only — never posts new purchase cost."
        case "SPEND":
            return "Expense is running cost; payment state is separate. Never invent stock from expense."
        case "STOCK":
            return "Quantity delta only — never invent unit cost or BOM. Damage uses Reduce + reason."
        case "ORDER":
            if orderSide == "Purchase order" {
                return "Purchase order is commitment only — no AP, no stock, no GST posting. Cost + stock on goods receipt / posted Purchase. Partial receive stays on PO."
            }
            return "Sales order is commitment only — revenue + stock on posted Sale / POS Charge. Hold parks draft. Never posts GST as sale by itself."
        case "STATUS":
            return "Operational state only — does not post revenue or stock by itself. Sale / POS Charge posts facts."
        case "BUDGET", "TARGET":
            return "Versioned + effective-dated plan. Never rewrite posted sales to match target."
        case "ISSUE", "UPDATE", "APPROVAL":
            return "Audit / action / permission — approval ≠ disbursement. Shift variance opens Issue, not a sale edit."
        default:
            return "One capture posts one event. Collection/payment allocate — never duplicate facts."
        }
    }

    static func cta(
        _ mode: String,
        amount: String? = nil,
        orderSide: String? = nil,
        revisionKind: String? = nil
    ) -> String {
        let base: String
        switch mode.uppercased() {
        case "PURCHASE": base = "Post purchase"
        case "COLLECTION": base = "Post collection"
        case "PAYMENT": base = "Post payment"
        case "SPEND": base = "Post expense"
        case "STOCK": return "Post stock update"
        case "STATUS": return "Update status"
        case "ORDER":
            base = orderSide == "Purchase order" ? "Save purchase order" : "Save sales order"
        case "BUDGET", "TARGET": base = "Save revision"
        case "ISSUE", "UPDATE", "APPROVAL":
            // Figma `2399:30597` — Post issue / Post approval / Post update (never "Submit …").
            switch revisionKind ?? "Issue" {
            case "Approval": return "Post approval"
            case "Update": return "Post update"
            default: return "Post issue"
            }
        default: base = "Save"
        }
        if let amount, !amount.isEmpty, amount != "—" {
            return "\(base) · \(amount)"
        }
        return base
    }

    static func footerCaption(_ mode: String, orderSide: String? = nil) -> String {
        switch mode.uppercased() {
        case "ORDER":
            return orderSide == "Purchase order" ? poCommitmentFooter : soCommitmentFooter
        default:
            return draftFooter
        }
    }

    static func scanPlaceholder(_ mode: String) -> String {
        switch mode.uppercased() {
        case "PURCHASE": return "SKU code · tap to scan"
        case "STOCK": return "SKU code · tap to scan"
        default: return "Scan · tap"
        }
    }

    static func allocateTrailing(openCount: Int, party: String) -> String {
        let partyBit = party.isEmpty ? "" : " · \(party)"
        return "\(openCount) open\(partyBit)"
    }
}
