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
        case "HOLD": return "Freeze open order or dispatch"
        case "TRANSFER": return "Move qty between locations"
        case "REPRINT": return "Read only · never posts"
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

    /// Sticky footer meta — prefer [amountRaw] from typed amount or lines/qty×price total.
    static func stickyMeta(
        mode: String,
        currency: String = "INR",
        amountRaw: String? = nil,
        receivedRaw: String? = nil,
        paymentChoice: String? = nil,
        receiptState: String? = nil,
        quantity: String? = nil,
        unit: String? = nil,
        approvalChoice: String? = nil
    ) -> String {
        let shown = formatStickyAmount(currency: currency, raw: amountRaw)
        let choice = paymentChoice ?? ""
        let receipt = receiptState ?? ""
        switch mode.uppercased() {
        case "SALE":
            if choice.caseInsensitiveCompare("Part paid") == .orderedSame {
                if let total = Decimal(string: amountRaw ?? ""),
                   let paid = Decimal(string: receivedRaw ?? ""),
                   total > paid {
                    let rem = NSDecimalNumber(decimal: total - paid).stringValue
                    return "Part paid · \(formatStickyAmount(currency: currency, raw: rem)) stays in AR"
                }
                return shown != "—" ? "Part paid · \(shown) stays in AR" : "Part paid · remaining stays in AR"
            }
            if choice.caseInsensitiveCompare("Received") == .orderedSame {
                return shown != "—" ? "Received · \(shown) · no open AR" : "Received · no open AR"
            }
            return shown != "—" ? "Pay later · \(shown) stays in AR" : "Pay later · full amount stays in AR"
        case "PURCHASE":
            let goods = (receipt.localizedCaseInsensitiveContains("later")
                || receipt.localizedCaseInsensitiveContains("await")
                || receipt.localizedCaseInsensitiveContains("pending"))
                ? "goods pending" : "goods received now"
            if choice.caseInsensitiveCompare("Part paid") == .orderedSame {
                return shown != "—" ? "\(shown) payable · part paid · \(goods)" : "Part paid · payable remains"
            }
            if choice.caseInsensitiveCompare("Paid") == .orderedSame {
                return shown != "—" ? "Paid · \(shown) · \(goods)" : "Paid · \(goods)"
            }
            return shown != "—" ? "\(shown) payable · \(goods)" : "Payable · \(goods)"
        case "COLLECTION", "PAYMENT":
            return "Allocated \(shown) of \(shown)"
        case "SPEND":
            if choice.caseInsensitiveCompare("Paid") == .orderedSame {
                return shown != "—" ? "Paid · \(shown) · expense + disbursement" : "Paid · posts expense + disbursement"
            }
            return shown != "—" ? "Pay later · \(shown) · needs payee" : "Pay later · needs payee"
        case "STOCK":
            let qty = (quantity?.isEmpty == false ? quantity : amountRaw) ?? "—"
            let u = (unit?.isEmpty == false ? unit : nil) ?? "pc"
            return "Prior — → \(qty ?? "—") → result \(u)"
        case "RETURN":
            let qty = (quantity?.isEmpty == false ? quantity : nil) ?? "0"
            return "Credit \(shown) · +\(qty) pc to stock"
        case "BUDGET", "TARGET":
            return shown != "—" ? "Revision \(shown) · v3 replaces v2 · prior kept" : "v3 replaces v2 · prior kept"
        case "UPDATE", "ISSUE":
            return "Adds to Needs Attention"
        case "APPROVAL":
            if approvalChoice?.caseInsensitiveCompare("Reimburse") == .orderedSame {
                return "Reimburse posts a separate payment"
            }
            return "Decision only · money is separate"
        case "ORDER":
            return "Commitment \(shown) · nothing posted"
        case "SHIPMENT", "DELIVERY":
            let qty = (quantity?.isEmpty == false ? quantity : nil) ?? "—"
            return "−\(qty) pc stock · money stays on Collection"
        default:
            return shown != "—" ? shown : "Draft · Nothing saved yet"
        }
    }

    private static func formatStickyAmount(currency: String, raw: String?) -> String {
        let value = raw?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        if value.isEmpty || value == "—" { return "—" }
        let code = currency.isEmpty ? "INR" : currency
        let formatted: String = {
            guard let number = Decimal(string: value) else { return value }
            let formatter = NumberFormatter()
            formatter.numberStyle = .decimal
            formatter.maximumFractionDigits = 2
            formatter.minimumFractionDigits = 0
            if code.uppercased() == "INR" {
                formatter.locale = Locale(identifier: "en_IN")
            }
            return formatter.string(from: number as NSDecimalNumber) ?? value
        }()
        if code.uppercased() == "INR" { return "₹\(formatted)" }
        return "\(formatted) \(code)"
    }
}

// MARK: - Amount-first hero (APK AmountBlock parity)

/// 40pt amount/qty hero with currency / unit pill — Trading capture (no OperatingTheme dependency).
struct TradingAmountHero: View {
    let label: String
    var currency: String = "INR"
    var accent: Color = Color(red: 0.06, green: 0.73, blue: 0.51)
    @Binding var text: String
    var hint: String = ">0"
    var isQuantity: Bool = false
    var readOnly: Bool = false
    var unitOverride: String? = nil

    private var unitLabel: String {
        if let unitOverride { return unitOverride }
        if isQuantity { return currency.isEmpty ? "Qty" : currency }
        return currency.uppercased() == "INR" ? "INR ₹" : currency
    }

    private var displayAmount: String {
        if isQuantity || text.isEmpty { return text }
        if currency.uppercased() == "INR" {
            let formatted = BusinessSetupAmountFormatting.formatIndianAmount(text)
            return formatted.isEmpty ? text : formatted
        }
        return text
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text(label)
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(Color.white.opacity(0.55))
                Spacer()
                Text(unitLabel)
                    .font(.system(size: 12, weight: .bold))
                    .foregroundStyle(accent)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 4)
                    .background(accent.opacity(0.12))
                    .clipShape(Capsule())
            }
            if readOnly {
                Text(text.isEmpty ? "0.00" : displayAmount)
                    .font(.system(size: 40, weight: .bold))
                    .foregroundStyle(Color.white)
                    .tracking(-1)
            } else {
                TextField(isQuantity ? "0" : "0.00", text: $text)
                    .font(.system(size: 40, weight: .bold))
                    .foregroundStyle(Color.white)
                    .keyboardType(.decimalPad)
                    .tracking(-1)
                    .onChange(of: text) { _, newValue in
                        guard !isQuantity else { return }
                        let formatted = currency.uppercased() == "INR"
                            ? BusinessSetupAmountFormatting.formatIndianAmount(newValue)
                            : BusinessSetupAmountFormatting.plainAmount(newValue)
                        if formatted != newValue {
                            text = formatted
                        }
                    }
            }
            Text(hint)
                .font(.system(size: 12))
                .foregroundStyle(Color.white.opacity(0.45))
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(red: 0.09, green: 0.11, blue: 0.15))
        .overlay(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .stroke(accent.opacity(0.33), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
    }
}

// MARK: - More details accordion

struct TradingMoreDetails: View {
    var accent: Color = Color(red: 0.06, green: 0.73, blue: 0.51)
    var label: String = TradingQuickAddCopy.moreDetailsLabel
    @Binding var open: Bool
    @Binding var note: String

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Button {
                open.toggle()
            } label: {
                HStack {
                    Text(label)
                        .font(.system(size: 13, weight: .medium))
                        .foregroundStyle(accent)
                    Spacer()
                    Text(open ? "∧" : "∨")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundStyle(accent)
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 12)
                .overlay(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .strokeBorder(style: StrokeStyle(lineWidth: 1, dash: [5, 4]))
                        .foregroundStyle(accent.opacity(0.45))
                )
            }
            .buttonStyle(.plain)

            if open {
                VStack(alignment: .leading, spacing: 6) {
                    Text("Note")
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(Color.white.opacity(0.55))
                    TextField("Optional", text: $note, axis: .vertical)
                        .font(.system(size: 15))
                        .foregroundStyle(Color.white)
                        .lineLimit(3...6)
                        .padding(.horizontal, 14)
                        .padding(.vertical, 12)
                        .background(Color(red: 0.09, green: 0.11, blue: 0.15))
                        .overlay(
                            RoundedRectangle(cornerRadius: 14, style: .continuous)
                                .stroke(Color.white.opacity(0.08), lineWidth: 1)
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                }
            }
        }
    }
}
