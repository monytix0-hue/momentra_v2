import Combine
import Foundation

@MainActor
final class OperatingChromeState: ObservableObject {
    static let shared = OperatingChromeState()
    @Published var participantNames: [String] = []
    @Published var editing: OperatingActivityPayload?
    @Published var settling: OperatingPaymentPayload?
    /// Appointment (or job) selected from Needs Attention to finalize a charge.
    @Published var charging: OperatingActivityPayload?
    /// Hint for Quick Add when attention asks for stock or budget revision.
    @Published var quickAddHint: String?
}

@MainActor
func openOperatingAttention(
    _ item: OperatingAttentionPayload,
    model: OperatingProjectionPayload,
    onOpenQuickAdd: (() -> Void)? = nil
) {
    let sourceId = item.sourceEventId ?? (item.id.hasPrefix("open-") ? String(item.id.dropFirst(5)) : item.id)
    switch item.cta {
    case "collect", "pay":
        if let payment = (model.paymentReadiness ?? []).first(where: { $0.id == sourceId }) {
            OperatingChromeState.shared.settling = payment
        }
    case "booking", "approval":
        if let row = (model.activity ?? []).first(where: { $0.id == sourceId }) {
            OperatingChromeState.shared.editing = row
        }
    case "charge":
        if let row = (model.activity ?? []).first(where: { $0.id == sourceId }) {
            OperatingChromeState.shared.charging = row
        }
    case "stock", "budget", "shipment":
        OperatingChromeState.shared.quickAddHint = item.cta
        onOpenQuickAdd?()
    default:
        if let payment = (model.paymentReadiness ?? []).first(where: { $0.id == sourceId }) {
            OperatingChromeState.shared.settling = payment
        } else if let row = (model.activity ?? []).first(where: { $0.id == sourceId }) {
            OperatingChromeState.shared.editing = row
        }
    }
}

func operatingAttentionCtaLabel(_ item: OperatingAttentionPayload) -> String {
    switch item.cta {
    case "collect": return "Collect"
    case "pay": return "Pay"
    case "booking": return "Update booking"
    case "charge": return "Finalize charge"
    case "approval": return "Review"
    case "stock": return "Review stock"
    case "budget": return "Review budget"
    case "shipment": return "Record shipment"
    default: return "Open"
    }
}

func formatOperatingAmount(_ raw: String?, currency: String?, visible: Bool) -> String {
    guard visible, let raw, !raw.isEmpty, let value = Decimal(string: raw) else { return "—" }
    let format = NumberFormatter()
    format.numberStyle = .decimal
    format.locale = Locale(identifier: "en_IN")
    format.maximumFractionDigits = 2
    let text = format.string(from: value as NSDecimalNumber) ?? raw
    let code = (currency?.isEmpty == false ? currency! : "INR")
    return code == "INR" ? "₹\(text)" : "\(code) \(text)"
}

func formatOperatingDetail(_ detail: String, currency: String?) -> String {
    guard let regex = try? NSRegularExpression(pattern: #"\d+\.\d{4}(?:\s+[A-Z]{3})?"#) else { return detail }
    let range = NSRange(detail.startIndex..., in: detail)
    var result = detail
    for match in regex.matches(in: detail, range: range).reversed() {
        guard let span = Range(match.range, in: detail) else { continue }
        let token = String(detail[span]).split(separator: " ").first.map(String.init) ?? ""
        let formatted = formatOperatingAmount(token, currency: currency, visible: true)
        result.replaceSubrange(span, with: formatted)
    }
    return result
}
