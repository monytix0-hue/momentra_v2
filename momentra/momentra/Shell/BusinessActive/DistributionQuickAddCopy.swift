import SwiftUI

/// Mirrored Figma DST-QA-01…12 copy catalog (SoT with APK `DistributionQuickAddCopy.kt`).
/// Brand prefix is `{brand} Distribution · ` where blank brand falls back to `"Distribution"`.
enum DistributionQuickAddCopy {
    static let moreDetailsLabel = "More details · notes, attachments, tags"

    static func context(mode: String, brand: String?) -> String {
        let prefix = (brand?.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty == false)
            ? brand!.trimmingCharacters(in: .whitespacesAndNewlines)
            : "Distribution"
        guard let suffix = contextSuffix(mode: mode) else { return "\(prefix) Distribution" }
        return "\(prefix) Distribution · \(suffix)"
    }

    static func contextSuffix(mode: String) -> String? {
        switch mode.uppercased() {
        case "SALE": return "Sale ≠ receipt · stock only on dispatch"
        case "PURCHASE": return "Purchase ≠ payment · stock only when received"
        case "COLLECTION": return "Never posts a new sale · allocates to open AR"
        case "PAYMENT", "REIMBURSE": return "Never posts a new purchase · allocates to open AP"
        case "SPEND": return "Expense ≠ payment · payable if unpaid"
        case "STOCK": return "Reconciliation/adjustment — not a purchase"
        case "RETURN": return "Reversal only · stock changes need explicit goods movement"
        case "BUDGET", "TARGET": return "Versioned plan · never mutates past actuals"
        case "UPDATE", "ISSUE": return "Operational only — never money or stock by inference"
        case "APPROVAL": return "Approval ≠ disbursement · maker-checker aware"
        case "ORDER": return "Commitment only — no sale, purchase, stock, or money"
        case "SHIPMENT", "DELIVERY": return "Physical movement only — not a new sale or purchase"
        case OperatingCaptureLayout.holdMode: return "Freeze open order or dispatch"
        case OperatingCaptureLayout.transferMode: return "Move qty between locations"
        case OperatingCaptureLayout.reprintMode: return "Read only · never posts"
        default: return nil
        }
    }

    static func why(mode: String) -> String {
        switch mode.uppercased() {
        case "SALE":
            return "Mode chips re-render the form — item fields hidden in Quick amount. Part paid posts one sale + linked partial receipt + remaining AR. Itemized total is read-only sum of lines."
        case "PURCHASE":
            return "Itemized + received-now adds stock; receive-later leaves goods pending for Delivery → Incoming. Payable remains until Payment allocates."
        case "COLLECTION":
            return "Single-source default; multi-allocation only when enabled. Allocations must equal collection amount — no over-allocation."
        case "PAYMENT", "REIMBURSE":
            return "Only unpaid/part-paid posted purchase/expense sources. Sum of allocations must equal payment."
        case "SPEND":
            return "Paid posts expense + linked disbursement once. Pay later requires payee and due date when policy demands."
        case "STOCK":
            return "Set current reconciles to a target qty. Never invents a purchase or sale. Amount-only sales do not move stock."
        case "RETURN":
            return "Credit cannot exceed eligible source. Stock changes only when goods movement is explicit — not inferred from credit amount."
        case "BUDGET", "TARGET":
            return "Preserves prior version. Pulse Target & Budget uses the effective version for the selected period."
        case "UPDATE", "ISSUE":
            return "Issue fields appear only for Issue mode. Creates Needs Attention + Activity entries, not financial facts."
        case "APPROVAL":
            return "Only role-authorized actions shown. Self-approval blocked when maker-checker enabled. Reimburse posts payment separately from approval."
        case "ORDER":
            return "Save creates commitment. Record Sale / Record Purchase select eligible orders and prefill unexecuted lines. Direct sale without order remains allowed."
        case "SHIPMENT", "DELIVERY":
            return "Outgoing reduces undelivered sale qty + stock once. Incoming reduces unreceived purchase qty + increases stock. Money settlement stays on Collection/Payment."
        case OperatingCaptureLayout.holdMode:
            return "Hold pauses a delivery order. No dispatch, no stock move and no GST; resume continues the same DO."
        case OperatingCaptureLayout.transferMode:
            return "Transfer moves stock between warehouses at cost: − from, + to on confirm. No revenue, no AP, and no GST within the same GSTIN (inter-state branch transfer needs an e-way bill)."
        case OperatingCaptureLayout.reprintMode:
            return "Reprint re-renders the posted invoice with the same number and GST split (IGST for inter-state dealers). No new sale, no stock or AR change."
        default:
            return "One capture posts one event. Nothing here rewrites an approved record."
        }
    }

    static func cta(mode: String) -> String {
        switch mode.uppercased() {
        case "SALE": return "Save sale"
        case "PURCHASE": return "Save purchase"
        case "COLLECTION": return "Save collection"
        case "PAYMENT": return "Save payment"
        case "SPEND": return "Save expense"
        case "STOCK": return "Save stock update"
        case "RETURN": return "Save return"
        case "BUDGET", "TARGET": return "Save revision"
        case "UPDATE", "ISSUE": return "Post"
        case "APPROVAL": return "Confirm action"
        case "ORDER": return "Save order"
        case "SHIPMENT", "DELIVERY": return "Save delivery"
        case OperatingCaptureLayout.holdMode: return "Hold DO"
        case OperatingCaptureLayout.transferMode: return "Confirm transfer"
        case OperatingCaptureLayout.reprintMode: return "Reprint · duplicate copy"
        default: return "Save"
        }
    }
}
