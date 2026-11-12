import SwiftUI

/// Mirrored Figma MKT-OWN-QA-01…04 / 07…10 (+ HOLD/TRANSFER chrome) copy catalog
/// (SoT with APK `MarketingOwnQuickAddCopy.kt`).
/// Context lines are `{brand} · Own Business Campaign · {suffix}`.
/// Invoice / Collection / Reprint are Client-only — not on the Own hub.
enum MarketingOwnQuickAddCopy {
    static let moreDetailsLabel = "More details"
    static let moreDetailsHint = "Notes · Attachment · Tags"
    static let stickyMeta = "Draft preserved if you close or open a picker"
    /// Masters Invite WHY — soft-magenta body `#FAE8FF` (Figma `2584:29990`).
    static let inviteWhy = "Invite grants a role and its access. Joining ≠ approval ≠ payment."

    /// Resolve Figma catalog key from capture eventType + mode.
    static func catalogMode(eventType: String, mode: String) -> String {
        let et = eventType.uppercased()
        let m = mode.uppercased()
        if et == "ACTIVITY" || m == "ACTIVITY" { return "ACTIVITY" }
        if et == "PROGRESS" { return "PROGRESS" }
        if et == "REVISION" || m == "BUDGET" || m == "TARGET" || m == "REVISION" { return "BUDGET" }
        if et == "APPROVAL" || m == "APPROVAL" { return "APPROVAL" }
        if et == "UPDATE" || m == "UPDATE" || et == "ISSUE" || m == "ISSUE" { return "UPDATE" }
        if et == "RESULT" { return "RESULT" }
        if et == "SPEND" || m == "SPEND" { return "SPEND" }
        if et == "PAYMENT" || m == "PAYMENT" { return "PAYMENT" }
        if m == "HOLD" || et == "HOLD" { return "HOLD" }
        if m == "TRANSFER" || et == "TRANSFER" { return "TRANSFER" }
        return m.isEmpty ? et : m
    }

    /// - Parameters:
    ///   - apAmount: compact AP when projection has open payable (e.g. `₹40K`) — Payment only.
    ///   - budgetAmount: compact plan amount when present — Budget only.
    ///   - budgetPacing: projection pacing string when present — Budget only.
    /// Missing projection figures fall back to the static Figma catalog line.
    static func context(
        mode: String,
        brand: String?,
        period: String? = nil,
        apAmount: String? = nil,
        budgetAmount: String? = nil,
        budgetPacing: String? = nil
    ) -> String {
        let trimmed = brand?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let prefix = trimmed.isEmpty ? "Campaign" : trimmed
        let key = mode.uppercased()
        // Figma Progress / Payment context lines are standalone (no brand prefix).
        if key == "PROGRESS" { return "Pick an open activity in this Moment" }
        if key == "PAYMENT" {
            let ap = apAmount?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
            if !ap.isEmpty, ap != "—", ap != "₹0" {
                return "Settle an open payable · AP \(ap)"
            }
            return "Settle an open payable · AP"
        }
        let suffix = contextSuffix(mode: mode)
        let periodBit = period?.trimmingCharacters(in: .whitespacesAndNewlines)
        let hasPeriod = !(periodBit ?? "").isEmpty
        var budgetBits: [String] = []
        if let budgetAmount {
            let amt = budgetAmount.trimmingCharacters(in: .whitespacesAndNewlines)
            if !amt.isEmpty, amt != "—" { budgetBits.append(amt) }
        }
        if let budgetPacing {
            let pace = budgetPacing.trimmingCharacters(in: .whitespacesAndNewlines)
            if !pace.isEmpty { budgetBits.append(pace) }
        }
        let isBudget = key == "BUDGET" || key == "TARGET" || key == "REVISION"
        let budgetTail = (isBudget && !budgetBits.isEmpty) ? budgetBits.joined(separator: " · ") : nil
        // Activity Figma: `{brand} · Own Business Campaign · {period}` (no extra suffix).
        if key == "ACTIVITY", hasPeriod {
            return "\(prefix) · Own Business Campaign · \(periodBit!)"
        }
        if let suffix, hasPeriod, let budgetTail {
            return "\(prefix) · Own Business Campaign · \(periodBit!) · \(suffix) · \(budgetTail)"
        }
        if let suffix, let budgetTail {
            return "\(prefix) · Own Business Campaign · \(suffix) · \(budgetTail)"
        }
        if let suffix, hasPeriod {
            return "\(prefix) · Own Business Campaign · \(periodBit!) · \(suffix)"
        }
        if let suffix {
            return "\(prefix) · Own Business Campaign · \(suffix)"
        }
        if hasPeriod {
            return "\(prefix) · Own Business Campaign · \(periodBit!)"
        }
        return "\(prefix) · Own Business Campaign"
    }

    static func contextSuffix(mode: String) -> String? {
        switch mode.uppercased() {
        case "ACTIVITY": return nil
        case "PROGRESS": return "Pick an open activity in this Moment"
        case "SPEND": return "Spend ≠ payment · creates payable"
        case "PAYMENT": return "Settle an open payable · AP"
        case "BUDGET", "TARGET", "REVISION": return "Versioned plan · effective-dated"
        case "APPROVAL": return "Decision only · no disbursement"
        case "UPDATE", "ISSUE": return "Visible to team"
        case "RESULT": return "Record a measured outcome"
        case "HOLD": return "Pause campaign · no spend / billing / GST"
        case "TRANSFER": return "Move planned budget between channels"
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
        case "BUDGET", "TARGET", "REVISION":
            if let budgetVersion { return "Save revision v\(budgetVersion)" }
            return "Save revision"
        case "APPROVAL": return "Save decision"
        case "UPDATE", "ISSUE": return "Post update"
        case "RESULT": return "Save result"
        case "HOLD": return "Hold campaign"
        case "TRANSFER": return "Confirm transfer"
        default: return "Save"
        }
    }

    static func title(mode: String) -> String {
        switch mode.uppercased() {
        case "ACTIVITY": return "Add Activity"
        case "PROGRESS": return "Update Progress"
        case "SPEND": return "Record Spend"
        case "PAYMENT": return "Record Payment"
        case "BUDGET", "TARGET", "REVISION": return "Budget / Target Revision"
        case "APPROVAL": return "Approval / Decision"
        case "UPDATE", "ISSUE": return "Team Update / Issue"
        case "RESULT": return "Campaign Result"
        case "HOLD": return "Campaign hold"
        case "TRANSFER": return "Budget transfer"
        default: return "Quick Add"
        }
    }
}
