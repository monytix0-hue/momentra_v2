import Foundation

/// Mirrored Figma MKT-QA-01…10 copy catalog (SoT with Android `MarketingClientQuickAddCopy.kt`).
enum MarketingClientQuickAddCopy {
    static let moreDetailsLabel = "More details"
    static let moreDetailsHint = "Notes · Attachment · Tags"
    static let stickyMeta = "Draft preserved if you close or open a picker"
    static let clientOnlyChip = "Client only"
    static let clientOnlyGate = "Client only · Hidden & server-rejected in Own Business mode"

    static func catalogMode(eventType: String, mode: String) -> String {
        let et = eventType.uppercased()
        let m = mode.uppercased()
        if et == "ACTIVITY" || m == "ACTIVITY" { return "ACTIVITY" }
        if et == "PROGRESS" { return "PROGRESS" }
        if et == "INVOICE" || (m == "SALE" && et == "INVOICE") { return "INVOICE" }
        if et == "REVISION" || m == "BUDGET" || m == "TARGET" || m == "REVISION" { return "BUDGET" }
        if et == "APPROVAL" || m == "APPROVAL" { return "APPROVAL" }
        if et == "UPDATE" || m == "UPDATE" || et == "ISSUE" || m == "ISSUE" { return "UPDATE" }
        if et == "RESULT" { return "RESULT" }
        if et == "SPEND" || m == "SPEND" { return "SPEND" }
        if et == "PAYMENT" || m == "PAYMENT" { return "PAYMENT" }
        if et == "COLLECTION" || m == "COLLECTION" { return "COLLECTION" }
        if m == "HOLD" || et == "HOLD" { return "HOLD" }
        if m == "TRANSFER" || et == "TRANSFER" { return "TRANSFER" }
        if m == "REPRINT" || et == "REPRINT" { return "REPRINT" }
        return m.isEmpty ? et : m
    }

    static func title(_ mode: String) -> String {
        switch mode.uppercased() {
        case "ACTIVITY": return "Add Activity"
        case "PROGRESS": return "Update Progress"
        case "SPEND": return "Record Spend"
        case "PAYMENT": return "Record Payment"
        case "INVOICE": return "Record Invoice"
        case "COLLECTION": return "Record Collection"
        case "BUDGET", "TARGET", "REVISION": return "Budget / Target Revision"
        case "APPROVAL": return "Approval / Decision"
        case "UPDATE", "ISSUE": return "Team Update / Issue"
        case "RESULT": return "Campaign Result"
        case "HOLD": return "Campaign hold"
        case "TRANSFER": return "Budget transfer"
        case "REPRINT": return "Reprint invoice"
        default: return "Quick Add"
        }
    }

    static func hubTileLabel(_ code: String) -> String {
        switch code.uppercased() {
        case "ACTIVITY": return "Activity"
        case "PROGRESS": return "Progress"
        case "SPEND": return "Spend"
        case "PAYMENT": return "Payment"
        case "INVOICE": return "Invoice"
        case "COLLECTION": return "Collection"
        case "REVISION": return "Revision"
        case "APPROVAL": return "Approval"
        case "UPDATE": return "Update"
        case "RESULT": return "Result"
        default: return title(code)
        }
    }

    static func why(_ mode: String) -> String {
        switch mode.uppercased() {
        case "ACTIVITY":
            return "An activity is a plan item — it carries no money. Spend and results are recorded against it later."
        case "PROGRESS":
            return "Incremental vs cumulative is explicit so updates are never double-counted."
        case "SPEND":
            return "Spend ≠ payment. Recording spend creates a payable; cash leaves only when you record a Payment."
        case "PAYMENT":
            return "A payment only settles an open payable — it never creates new spend or changes budget use."
        case "INVOICE":
            return "Invoice ≠ collection. Raising an invoice never implies cash received."
        case "COLLECTION":
            return "Collection reduces AR on an existing invoice — it never creates duplicate revenue."
        case "BUDGET", "TARGET", "REVISION":
            return "Revisions are versioned and effective-dated. Past achievement keeps its original denominator."
        case "APPROVAL":
            return "Approval ≠ disbursement — paying is a separate Payment. You cannot approve your own controlled action."
        case "UPDATE", "ISSUE":
            return "Issues and risks raise alerts on Pulse; plain updates do not."
        case "RESULT":
            return "Empty ≠ zero: leave blank when not measured. ROAS is not profit; attributed sales ≠ booked revenue."
        default:
            return "One capture posts one event. Nothing here rewrites an approved record."
        }
    }

    static func cta(_ mode: String) -> String {
        switch mode.uppercased() {
        case "ACTIVITY": return "Save activity"
        case "PROGRESS": return "Save progress"
        case "SPEND": return "Save spend"
        case "PAYMENT": return "Save payment"
        case "INVOICE": return "Save invoice"
        case "COLLECTION": return "Save collection"
        case "BUDGET", "TARGET", "REVISION": return "Save revision"
        case "APPROVAL": return "Save decision"
        case "UPDATE", "ISSUE": return "Post update"
        case "RESULT": return "Save result"
        default: return "Save"
        }
    }
}
