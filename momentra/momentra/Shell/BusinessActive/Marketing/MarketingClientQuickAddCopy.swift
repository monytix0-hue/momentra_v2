import SwiftUI

/// Mirrored Figma MKT-QA-01…10 (+08 Approval, HOLD/TRANSFER/REPRINT) copy catalog
/// (SoT with APK `MarketingClientQuickAddCopy.kt`).
/// Context lines are `{brand} · Client Campaign · {suffix}`.
enum MarketingClientQuickAddCopy {
    static let moreDetailsLabel = "More details"
    static let moreDetailsHint = "Notes · Attachment · Tags"
    static let stickyMeta = "Draft preserved if you close or open a picker"
    static let clientOnlyChip = "Client only"
    static let clientOnlyGate = "Client only · Hidden & server-rejected in Own Business mode"

    /// Resolve Figma catalog key from capture eventType + mode.
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

    static func context(mode: String, brand: String?, period: String? = nil) -> String {
        let trimmed = brand?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let prefix = trimmed.isEmpty ? "Campaign" : trimmed
        let suffix = contextSuffix(mode: mode)
        let periodBit = period?.trimmingCharacters(in: .whitespacesAndNewlines)
        let hasPeriod = !(periodBit ?? "").isEmpty
        if let suffix, hasPeriod {
            return "\(prefix) · Client Campaign · \(periodBit!) · \(suffix)"
        }
        if let suffix {
            return "\(prefix) · Client Campaign · \(suffix)"
        }
        if hasPeriod {
            return "\(prefix) · Client Campaign · \(periodBit!)"
        }
        return "\(prefix) · Client Campaign"
    }

    static func contextSuffix(mode: String) -> String? {
        switch mode.uppercased() {
        case "ACTIVITY": return "Plan item · no money"
        case "PROGRESS": return "Pick an open activity"
        case "SPEND": return "Spend ≠ payment · creates payable"
        case "PAYMENT": return "Settle open payable · AP"
        case "INVOICE": return "Client Campaign · raises AR"
        case "COLLECTION": return "Against open invoice · AR"
        case "BUDGET", "TARGET", "REVISION": return "Versioned plan · effective-dated"
        case "APPROVAL": return "Decision only · no disbursement"
        case "UPDATE", "ISSUE": return "Visible to team"
        case "RESULT": return "Record a measured outcome"
        case "HOLD": return "Pause campaign · no spend / billing / GST"
        case "TRANSFER": return "Move planned budget between channels"
        case "REPRINT": return "Same invoice no. · no new sale / AR"
        default: return nil
        }
    }

    static func why(mode: String) -> String {
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
        case "HOLD":
            return "Hold pauses a campaign. No spend, no billing and no GST; resume continues the same campaign."
        case "TRANSFER":
            return "Transfer moves planned budget between channels: − from, + to on confirm. No revenue, no AP and no GST — spend posts only on vendor bill."
        case "REPRINT":
            return "Reprint re-renders the posted invoice with the same number and GST split (IGST for out-of-state clients). No new sale, no AR change."
        default:
            return "One capture posts one event. Nothing here rewrites an approved record."
        }
    }

    static func cta(mode: String, budgetVersion: Int? = nil) -> String {
        switch mode.uppercased() {
        case "ACTIVITY": return "Save activity"
        case "PROGRESS": return "Save progress"
        case "SPEND": return "Save spend"
        case "PAYMENT": return "Save payment"
        case "INVOICE": return "Save invoice"
        case "COLLECTION": return "Save collection"
        case "BUDGET", "TARGET", "REVISION":
            if let budgetVersion { return "Save revision v\(budgetVersion)" }
            return "Save revision"
        case "APPROVAL": return "Save decision"
        case "UPDATE", "ISSUE": return "Post update"
        case "RESULT": return "Save result"
        case "HOLD": return "Hold campaign"
        case "TRANSFER": return "Confirm transfer"
        case "REPRINT": return "Reprint · duplicate copy"
        default: return "Save"
        }
    }

    static func title(mode: String) -> String {
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
}
