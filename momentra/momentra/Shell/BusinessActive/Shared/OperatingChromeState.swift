import Combine
import Foundation

@MainActor
final class OperatingChromeState: ObservableObject {
    static let shared = OperatingChromeState()
    @Published var participantNames: [String] = []
    @Published var editing: OperatingActivityPayload?
    @Published var settling: OperatingPaymentPayload?
}

@MainActor
func operatingAttentionCtaLabel(_ item: OperatingAttentionPayload) -> String {
    switch item.cta {
    case "approval": return "Review"
    case "collect": return "Collect"
    case "pay": return "Pay"
    case "stock": return "Update stock"
    case "shipment": return "Record shipment"
    case "booking": return "Open booking"
    case "charge": return "Record charge"
    case "budget": return "Review budget"
    default: return "Open"
    }
}

@MainActor
final class OperatingQuickAddHint {
    static var value: String?
}

@MainActor
func openOperatingAttention(
    _ item: OperatingAttentionPayload,
    model: OperatingProjectionPayload,
    onOpenQuickAdd: () -> Void
) {
    let sourceId = item.sourceEventId
        ?? (item.id.hasPrefix("open-") ? String(item.id.dropFirst(5)) : nil)
    switch item.cta {
    case "collect", "pay":
        if let sourceId, let payment = model.paymentReadiness?.first(where: { $0.id == sourceId }) {
            OperatingChromeState.shared.settling = payment
            return
        }
    case "booking", "approval":
        if let sourceId, let row = model.activity?.first(where: { $0.id == sourceId }) {
            OperatingChromeState.shared.editing = row
            return
        }
    case "stock", "shipment", "budget":
        OperatingQuickAddHint.value = item.cta
        onOpenQuickAdd()
        return
    default:
        break
    }
    if item.id.hasPrefix("open-"), let sourceId,
       let payment = model.paymentReadiness?.first(where: { $0.id == sourceId }) {
        OperatingChromeState.shared.settling = payment
        return
    }
    if let sourceId, let row = model.activity?.first(where: { $0.id == sourceId }) {
        OperatingChromeState.shared.editing = row
        return
    }
    onOpenQuickAdd()
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
