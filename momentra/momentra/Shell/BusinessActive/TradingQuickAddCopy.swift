import SwiftUI

/// Mirrored Figma TRD-QA-01…12 copy catalog (SoT with APK `TradingQuickAddCopy.kt`).
/// Brand prefix is `{brand} Trading · ` where blank brand falls back to `"Trading"`.
enum TradingQuickAddCopy {
    static let moreDetailsLabel = "More details · notes, attachments, tags"

    static func context(mode: String, brand: String?) -> String {
        let prefix = (brand?.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty == false)
            ? brand!.trimmingCharacters(in: .whitespacesAndNewlines)
            : "Trading"
        guard let suffix = contextSuffix(mode: mode) else { return "\(prefix) Trading" }
        return "\(prefix) Trading · \(suffix)"
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
        default: return "Save"
        }
    }
}

// MARK: - Amount-first hero (APK AmountBlock parity)

/// 40pt amount/qty hero with currency / unit pill — Trading + Marketing capture.
struct TradingAmountHero: View {
    let theme: OperatingTheme
    let label: String
    var currency: String = "INR"
    @Binding var text: String
    var hint: String = ">0"
    var isQuantity: Bool = false
    /// When set (e.g. Own Progress `%`), replaces the currency pill text.
    var unitOverride: String? = nil

    private var unitTint: Color {
        theme.isMarketingOwnCapture ? theme.accentSoft : theme.accent
    }

    private var unitLabel: String {
        if let unitOverride { return unitOverride }
        if isQuantity { return currency.isEmpty ? "Qty" : currency }
        return currency.uppercased() == "INR" ? "INR ₹" : currency
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text(label)
                    .font(.plusJakarta(size: 12, weight: .medium))
                    .foregroundStyle(theme.captureLabel)
                Spacer()
                Text(unitLabel)
                    .font(.plusJakarta(size: 12, weight: .bold))
                    .foregroundStyle(unitTint)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 4)
                    .background(unitTint.opacity(0.12))
                    .clipShape(Capsule())
            }
            TextField(isQuantity ? "0" : "0.00", text: $text)
                .font(.plusJakarta(size: 40, weight: .bold))
                .foregroundStyle(theme.captureValue)
                .keyboardType(.decimalPad)
                .tracking(-1)
            Text(hint)
                .font(.plusJakarta(size: 12))
                .foregroundStyle(theme.captureHint)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(theme.captureField)
        .overlay(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .stroke(theme.accent.opacity(0.33), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
    }
}

// MARK: - More details accordion

struct TradingMoreDetails: View {
    let theme: OperatingTheme
    @Binding var open: Bool
    @Binding var note: String

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Button {
                open.toggle()
            } label: {
                VStack(alignment: .leading, spacing: 4) {
                    HStack {
                        Text(
                            theme.isMarketingClientCapture
                                ? MarketingClientQuickAddCopy.moreDetailsLabel
                                : (theme.isDistributionCapture ? DistributionQuickAddCopy.moreDetailsLabel : TradingQuickAddCopy.moreDetailsLabel)
                        )
                            .font(.plusJakarta(size: 13, weight: .medium))
                            .foregroundStyle(theme.accent)
                        Spacer()
                        Text(open ? "∧" : "∨")
                            .font(.plusJakarta(size: 12, weight: .bold))
                            .foregroundStyle(theme.accent)
                    }
                    if theme.isMarketingClientCapture, !open {
                        Text(MarketingClientQuickAddCopy.moreDetailsHint)
                            .font(.plusJakarta(size: 12, weight: .medium))
                            .foregroundStyle(theme.captureHint)
                    }
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 12)
                .overlay(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .strokeBorder(
                            style: StrokeStyle(lineWidth: 1, dash: [5, 4])
                        )
                        .foregroundStyle(theme.accent.opacity(0.45))
                )
            }
            .buttonStyle(.plain)

            if open {
                VStack(alignment: .leading, spacing: 6) {
                    Text("Note")
                        .font(.plusJakarta(size: 12, weight: .medium))
                        .foregroundStyle(theme.captureLabel)
                    TextField("Optional", text: $note, axis: .vertical)
                        .font(.plusJakarta(size: 15))
                        .foregroundStyle(theme.captureValue)
                        .lineLimit(3...6)
                        .padding(.horizontal, 14)
                        .padding(.vertical, 12)
                        .background(theme.captureField)
                        .overlay(
                            RoundedRectangle(cornerRadius: 14, style: .continuous)
                                .stroke(Color.white.opacity(0.08), lineWidth: 1)
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                    Text("Attachments and tags — add from the full editor when available.")
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(theme.captureHint)
                }
            }
        }
    }
}
