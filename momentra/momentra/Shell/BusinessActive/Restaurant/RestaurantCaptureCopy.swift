import Foundation

/// Figma RST-QA-02…06 money / stock copy catalog (Ember · Spice Route).
/// Parallel to `RetailCaptureCopy` / `ServiceCaptureCopy` — never teal Service or rose Retail.
/// Tagline: Order. Serve. Collect.
enum RestaurantCaptureCopy {
    static let tagline = "Order. Serve. Collect."
    static let draftFooter = "Draft preserved if you close or open a picker"
    static let draftUntilPosted = "Saved as draft until posted"
    static let savedToast = "✓ Saved · sheet closes and returns back"

    /// Phase 4 Quick Add money / stock modes.
    static let moneyModes: Set<String> = [
        "PURCHASE", "COLLECTION", "PAYMENT", "SPEND", "STOCK",
    ]

    /// Phase 7 Budget / Issue (+ Phase 8 HTR adjacency).
    static let opsModes: Set<String> = [
        "BUDGET", "TARGET", "ISSUE", "UPDATE", "APPROVAL", "HOLD", "TRANSFER", "REPRINT",
    ]

    /// Phase 5 dual-mode order sheet (`ORDER` hub tile).
    static let orderModes: Set<String> = ["ORDER"]

    static let soMenuCategories = ["Mains", "Breads", "Starters", "Drinks", "Dessert"]
    static let poRawCategories = ["Dairy", "Grains", "Oils", "Spices", "Produce", "Pack"]
    static let soChannelChips = ["Dine-in", "Takeaway", "Catering"]

    static func orderModeChips() -> [String] { ["Sales Order", "Purchase Order"] }

    /// Figma SO status `Preparing|Placed|Ready` · PO `Open|Partial|Received`.
    static func orderStateChips(isPurchase: Bool) -> [String] {
        isPurchase ? ["Open", "Partial", "Received"] : ["Preparing", "Placed", "Ready"]
    }

    static func modeChips(_ mode: String) -> [String] {
        switch mode.uppercased() {
        case "PURCHASE": return ["Ingredients", "Supplies"]
        case "COLLECTION": return ["Platform", "Customer"]
        case "PAYMENT": return ["Supplier", "Other"]
        case "SPEND": return ["Kitchen", "Platform", "Running"]
        case "STOCK": return ["Count", "Waste", "Usage"]
        case "ORDER": return orderModeChips()
        case "BUDGET", "TARGET": return ["Sales", "Covers"]
        case "ISSUE", "UPDATE", "APPROVAL": return ["Issue", "Approval", "Update"]
        default: return []
        }
    }

    static let issueSeverities = ["Alert", "Watch", "Info"]
    static let menuCategories = soMenuCategories
    static let rawCategories = poRawCategories
    static let vendorCategories = ["Produce", "Dairy", "Dry", "Meat & Fish", "Beverages", "Packaging"]
    static let stationTypes = ["Kitchen", "Service", "Bar"]
    static let stationPrinters = ["Kitchen printer 1", "Bar printer", "Screen only"]
    static let inviteRoles = ["Owner", "Manager", "Cashier", "Kitchen", "Waiter"]

    static let budgetFixturePrevious = "350000"
    static let budgetFixtureNew = "400000"
    static let issueFixtureTitle = "Spoilage · dal"
    static let issueFixtureOwner = "Chef Ama"
    static let issueFixtureLink = "Stock waste"

    static let menuWhy =
        "Menu lists saved masters. GST badge is the dish rate (Restaurant 5%); tax posts on bill / POS Sale only."
    static let vendorsWhy =
        "Vendor is a master write. GSTIN here drives input tax credit only when a Purchase / GRN posts — never on PO save."
    static let stationsWhy =
        "Stations are KOT routing targets. Busy = open tickets over threshold; status is live, not a setting."
    static let inviteWhy =
        "Invite grants a role and its access. Joining ≠ approval ≠ payment."
    static let dishWhy =
        "Create Dish is a master write. Sales Order only picks saved dishes. GST rate here drives bill tax; posting still happens on bill / POS Sale only."
    static let rawWhy =
        "Create Raw Material is a master write. Purchase Order only references saved items; input GST and stock post on Purchase / GRN — never on save PO."
    static let vendorFormWhy =
        "Vendor is a master write. GSTIN state code decides CGST+SGST vs IGST on purchases."
    static let stationFormWhy =
        "Stations are KOT routing targets. Dishes pick a station in Create menu dish."
    static let holdWhy =
        "Hold parks a draft order on this device. No revenue, no stock, no KOT until resume / bill."
    static let transferWhy =
        "Transfer moves stock between stations at cost. No revenue and no GST within the same GSTIN."
    static let reprintReceiptWhy =
        "Reprint re-renders an existing posted bill. Never allocates a new bill id or posts revenue."

    static func tenderChips(_ mode: String) -> [String] {
        switch mode.uppercased() {
        case "PURCHASE": return ["Pay later", "UPI now", "Cash"]
        case "COLLECTION": return ["Bank settle", "UPI", "Cash"]
        case "PAYMENT": return ["UPI", "Bank", "Cash"]
        case "SPEND": return ["Paid now", "Pay later"]
        default: return ["UPI", "Cash"]
        }
    }

    static func defaultTender(_ mode: String) -> String {
        switch mode.uppercased() {
        case "PURCHASE": return "Pay later"
        case "COLLECTION": return "Bank settle"
        case "PAYMENT": return "UPI"
        case "SPEND": return "Paid now"
        default: return "UPI"
        }
    }

    static func defaultModeChip(_ mode: String) -> String {
        modeChips(mode).first ?? ""
    }

    static func sheetTitle(_ mode: String, orderSide: String? = nil) -> String {
        switch mode.uppercased() {
        case "PURCHASE": return "Record Purchase"
        case "COLLECTION": return "Record Collection"
        case "PAYMENT": return "Record Payment"
        case "SPEND": return "Record Expense"
        case "STOCK": return "Stock Update"
        case "ORDER":
            return orderSide == "Purchase Order" ? "Purchase Order" : "Sales Order"
        case "BUDGET", "TARGET": return "Budget / Target Revision"
        case "ISSUE", "UPDATE", "APPROVAL": return "Issue / Approval / Update"
        case "HOLD": return "Hold / park order"
        case "TRANSFER": return "Transfer stock"
        case "REPRINT": return "Reprint receipt"
        default: return mode
        }
    }

    static func sheetSubtitle(_ mode: String, orderSide: String? = nil) -> String {
        switch mode.uppercased() {
        case "PURCHASE": return "Receive ingredients · stock on accept"
        case "ORDER":
            return orderSide == "Purchase Order"
                ? "Supplier commitment · add raw materials"
                : "Customer commitment · add from menu"
        default: return ""
        }
    }

    static func qaCode(_ mode: String, orderSide: String? = nil) -> String {
        switch mode.uppercased() {
        case "PURCHASE": return "RST-QA-02"
        case "COLLECTION": return "RST-QA-03"
        case "PAYMENT": return "RST-QA-04"
        case "SPEND": return "RST-QA-05"
        case "STOCK": return "RST-QA-06"
        case "ORDER": return orderSide == "Purchase Order" ? "RST-QA-07-PO" : "RST-QA-07"
        default: return "RST-QA"
        }
    }

    static func amountLabel(_ mode: String, orderSide: String? = nil) -> String {
        switch mode.uppercased() {
        case "PURCHASE": return "Purchase total"
        case "COLLECTION": return "Collect now"
        case "PAYMENT": return "Pay now"
        case "SPEND": return "Expense"
        case "STOCK": return "Qty delta"
        case "ORDER":
            return orderSide == "Purchase Order" ? "PO value" : "Order value"
        default: return "Amount"
        }
    }

    static func scanLabel(_ mode: String) -> String {
        switch mode.uppercased() {
        case "PURCHASE": return "Scan invoice / GRN"
        case "STOCK": return "Scan / pick item"
        default: return "Scan"
        }
    }

    static func scanPlaceholder(_ mode: String) -> String {
        switch mode.uppercased() {
        case "PURCHASE": return "INV · vendor"
        case "STOCK": return "Item · store"
        default: return "Scan · tap"
        }
    }

    /// Context strip under title — live brand when present; Spice Route fixtures for visual QA only.
    static func contextLine(
        _ mode: String,
        brand: String,
        kitchen: String = "Kitchen",
        owner: String = "Kiran Rao",
        party: String = "",
        detail: String = "",
        orderSide: String? = nil
    ) -> String {
        let place = brand.isEmpty ? "Spice Route Kitchen" : brand
        let desk = kitchen.isEmpty ? place : (place.localizedCaseInsensitiveContains("Kitchen") ? place : "\(place)")
        switch mode.uppercased() {
        case "ORDER":
            if orderSide == "Purchase Order" {
                return "Supplier commitment · \(desk) · not AP yet"
            }
            let channel = detail.isEmpty ? "Dine-in" : detail
            return "\(channel) · \(desk) · commitment only"
        case "PURCHASE":
            return sheetSubtitle("PURCHASE")
        case "COLLECTION":
            let head = detail.isEmpty ? "Platform AR" : detail
            let who = party.isEmpty ? "Platform AR" : party
            return "\(head) · \(who) · \(desk) · \(owner)"
        case "PAYMENT":
            let who = party.isEmpty ? "open AP" : "\(party) · open AP"
            return "\(who) · \(desk) · \(owner)"
        case "SPEND":
            let head = detail.isEmpty ? (party.isEmpty ? "Kitchen cost" : party) : detail
            return "\(head) · \(desk) · \(owner)"
        case "STOCK":
            let head = detail.isEmpty ? (party.isEmpty ? "Stock move" : party) : detail
            return "\(head) · \(desk) · \(owner)"
        default:
            return "\(place) · \(tagline)"
        }
    }

    static func why(_ mode: String, orderSide: String? = nil) -> String {
        switch mode.uppercased() {
        case "PURCHASE":
            return "Stock += accepted qty only. No automatic recipe deduction (v5 exclusion)."
        case "COLLECTION":
            return "Platform receivable needs explicit settlement — fees must not duplicate cost."
        case "PAYMENT":
            return "Settles open AP only — never invents a purchase."
        case "SPEND":
            return "Kitchen/platform running cost; payment state separate from the expense fact."
        case "STOCK":
            return "Quantity delta only — never invent recipe consumption."
        case "ORDER":
            if orderSide == "Purchase Order" {
                return "Raw material Add builds PO lines only. AP and stock post on Purchase / GRN receive — never on save PO. Recipe deduction still excluded in v5."
            }
            return "Menu Add only builds the commitment cart. Revenue posts on bill / POS Sale. KOT send is separate and never posts."
        case "BUDGET", "TARGET":
            return "Versioned + effective-dated. Achievement uses posted bills only."
        case "ISSUE", "UPDATE", "APPROVAL":
            return "Audit / action — approval ≠ disbursement."
        case "HOLD": return holdWhy
        case "TRANSFER": return transferWhy
        case "REPRINT": return reprintReceiptWhy
        default:
            return "One capture posts one event. Collection/payment allocate — never duplicate facts."
        }
    }

    static func cta(_ mode: String, amount: String? = nil, orderSide: String? = nil, revisionKind: String? = nil) -> String {
        let base: String
        switch mode.uppercased() {
        case "PURCHASE": base = "Post purchase"
        case "COLLECTION": base = "Post collection"
        case "PAYMENT": base = "Post payment"
        case "SPEND": base = "Post expense"
        case "STOCK": return "Post stock update"
        case "ORDER":
            base = orderSide == "Purchase Order" ? "Save purchase order" : "Save sales order"
        case "BUDGET", "TARGET": base = "Save revision"
        case "ISSUE", "UPDATE", "APPROVAL":
            switch revisionKind ?? "Issue" {
            case "Approval": base = "Post approval"
            case "Update": base = "Post update"
            default: base = "Post issue"
            }
        case "HOLD": base = "Hold order"
        case "TRANSFER": base = "Confirm transfer"
        case "REPRINT": base = "Reprint receipt"
        default: base = "Save"
        }
        if let amount, !amount.isEmpty, amount != "—" {
            return "\(base) · \(amount)"
        }
        return base
    }

    static func footerCaption(_ mode: String, orderSide: String? = nil) -> String {
        switch mode.uppercased() {
        case "PURCHASE": return draftFooter
        case "ORDER":
            if orderSide == "Purchase Order" {
                return draftFooter
            }
            return "Draft preserved · Send KOT from ticket after save"
        default: return draftUntilPosted
        }
    }

    static let moreDetailsHint = "Notes · Proof · Tags"

    /// Maps Figma Count / Waste / Usage → contract stock reason seed.
    static func stockReasonSeed(_ modeChip: String) -> String {
        switch modeChip {
        case "Waste": return "waste"
        case "Usage": return "usage"
        default: return "stocktake"
        }
    }
}
