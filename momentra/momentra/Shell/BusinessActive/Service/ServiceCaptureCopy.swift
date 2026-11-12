import SwiftUI

/// Mirrored Figma SVC-QA-01…10 / Hold·Transfer·Reprint copy catalog.
/// CTAs, Why notes, mode chips, and titles lock to Android `ServiceCaptureCopy`
/// (Book. Deliver. Charge.).
enum ServiceCaptureCopy {
    static let tagline = "Book. Deliver. Charge."
    static let draftFooter = "Draft preserved if you close or open a picker"
    static let savedToast = "✓ Saved · sheet closes and returns back"
    static let moreDetailsHint = "Notes · Attachment · Tags"

    /// Why-note body cream from SVC Figma (`#FEF3C7`).
    static let whyBody = Color(hex: "#FEF3C7")
    /// Inactive chip border (`#0F766E`).
    static let chipBorder = Color(hex: "#0F766E")
    /// Ink on lime/teal CTA (`#042F2E`).
    static let inkOnAccent = Color(hex: "#042F2E")

    static func modeChips(_ mode: String) -> [String] {
        switch mode.uppercased() {
        case "SERVICE": return ["From booking", "Walk-in charge", "Reprint"]
        case "PURCHASE", "VENDOR": return ["Consumables", "Equipment"]
        case "STOCK": return ["Set current", "Add", "Reduce"]
        case "APPOINTMENT", "BOOKING": return ["Appointment", "Walk-in"]
        case "STATUS": return ["Start", "Complete", "Reschedule"]
        case "BUDGET", "TARGET": return ["Charges", "Sessions"]
        case "ISSUE", "UPDATE", "APPROVAL": return ["Issue", "Approval", "Update"]
        default: return []
        }
    }

    static func amountLabel(_ mode: String) -> String {
        switch mode.uppercased() {
        case "SERVICE": return "Charge amount"
        case "PURCHASE", "VENDOR": return "Purchase amount"
        case "COLLECTION": return "Collect now"
        case "PAYMENT", "REIMBURSE": return "Pay now"
        case "SPEND": return "Expense"
        case "APPOINTMENT", "BOOKING": return "Estimate"
        case "BUDGET", "TARGET": return "New target"
        default: return "Amount"
        }
    }

    static func amountHint(_ mode: String) -> String {
        switch mode.uppercased() {
        case "SERVICE": return "From booking or walk-in"
        case "PURCHASE", "VENDOR": return "Supplier total"
        case "COLLECTION": return "Package balance"
        case "PAYMENT", "REIMBURSE": return "Open AP"
        case "SPEND": return "Running cost"
        case "APPOINTMENT", "BOOKING": return "Not revenue"
        case "BUDGET", "TARGET": return "Effective-dated"
        default: return "Positive amount"
        }
    }

    static func why(_ mode: String) -> String {
        switch mode.uppercased() {
        case "SERVICE":
            return "Finalized charge posts revenue + output GST once (CGST+SGST in MH, IGST if place of supply is another state). Booking/completion alone do not."
        case "PURCHASE", "VENDOR":
            return "Stock only when received. AP + input GST from posted purchase — CGST+SGST if supplier in MH, IGST if out of state. A PO is commitment only."
        case "COLLECTION":
            return "Allocates charge AR — never creates new revenue. Deposit allocation is explicit."
        case "PAYMENT", "REIMBURSE":
            return "Settles open AP only."
        case "SPEND":
            return "Service running cost; payment state separate."
        case "STOCK":
            return "Optional consumables — never invent usage from booking."
        case "APPOINTMENT", "BOOKING":
            return "Commitment only — no revenue, no GST. SAC + rate shown as estimate; tax posts on Finalize Charge by place of supply."
        case "STATUS":
            return "Operational/appointment state only — does not post charge."
        case "BUDGET", "TARGET":
            return "Versioned + effective-dated."
        case "ISSUE", "UPDATE", "APPROVAL":
            return "Audit / action — approval ≠ disbursement."
        case "HOLD":
            return "Hold reserves a slot only. No booking commitment, no revenue, no GST until Finalize Charge; expires if not confirmed."
        case "TRANSFER":
            return "Transfer reassigns staff or room on an open booking. No revenue, no charge, no GST; commission follows the pro who completes."
        case "REPRINT":
            return "Reprint re-renders the posted invoice with the same number and GST split (IGST if client is out of state). No new charge, no revenue or tender change."
        default:
            return "One capture posts one event. Booking ≠ revenue; GST posts on Finalize Charge."
        }
    }

    static func cta(_ mode: String) -> String {
        switch mode.uppercased() {
        case "SERVICE": return "Post charge"
        case "PURCHASE", "VENDOR": return "Post purchase"
        case "COLLECTION": return "Post collection"
        case "PAYMENT", "REIMBURSE": return "Post payment"
        case "SPEND": return "Post expense"
        case "STOCK": return "Post stock update"
        case "APPOINTMENT", "BOOKING": return "Save booking"
        case "STATUS": return "Update status"
        case "BUDGET", "TARGET": return "Save revision"
        case "ISSUE", "UPDATE", "APPROVAL": return "Post"
        case "HOLD": return "Hold slot · HLD-22"
        case "TRANSFER": return "Confirm transfer · 2 bookings"
        case "REPRINT": return "Reprint · duplicate copy"
        default: return "Save"
        }
    }

    static func sheetTitle(_ mode: String) -> String {
        switch mode.uppercased() {
        case "SERVICE": return "Finalize Charge"
        case "PURCHASE", "VENDOR": return "Record Purchase"
        case "COLLECTION": return "Record Collection"
        case "PAYMENT", "REIMBURSE": return "Record Payment"
        case "SPEND": return "Record Expense"
        case "STOCK": return "Stock Update"
        case "APPOINTMENT", "BOOKING": return "Booking"
        case "STATUS": return "Status / Assignment"
        case "BUDGET", "TARGET": return "Budget / Target Revision"
        case "ISSUE", "UPDATE", "APPROVAL": return "Issue / Approval / Update"
        case "HOLD": return "Appointment hold"
        case "TRANSFER": return "Staff / room transfer"
        case "REPRINT": return "Reprint invoice"
        default: return mode
        }
    }

    static var serviceModes: Set<String> {
        [
            "SERVICE", "PURCHASE", "COLLECTION", "PAYMENT", "SPEND", "STOCK",
            "APPOINTMENT", "BOOKING", "STATUS", "BUDGET", "TARGET", "ISSUE", "UPDATE", "APPROVAL",
            "HOLD", "TRANSFER", "REPRINT",
        ]
    }
}
