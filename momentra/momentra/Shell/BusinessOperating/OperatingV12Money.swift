import Foundation

/// Money display for populated Trading / Distribution boards.
/// Default [grouped]: Indian commas + max 2 decimals (`₹9,40,000`, `₹86,000.50`).
enum OperatingV12Money {
    static func parse(_ raw: String?, currency: String? = nil) -> Double? {
        let value = raw?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        if value.isEmpty { return nil }
        let code = (currency ?? "INR").uppercased()
        let numeric = value
            .replacingOccurrences(of: code, with: "", options: .caseInsensitive)
            .replacingOccurrences(of: "₹", with: "")
            .replacingOccurrences(of: ",", with: "")
            .replacingOccurrences(of: "−", with: "-")
            .trimmingCharacters(in: .whitespacesAndNewlines)
        return Double(numeric)
    }

    static func grouped(_ amount: Double, currency: String?) -> String {
        let code = (currency ?? "INR").uppercased()
        let symbol = code == "INR" ? "₹" : "\(code) "
        let sign = amount < 0 ? "−" : ""
        return symbol + sign + indianGroupedNumber(abs(amount))
    }

    /// Indian grouping (`##,##,###.##`) with at most 2 fraction digits.
    static func indianGroupedNumber(_ magnitude: Double) -> String {
        let hasFraction = abs(magnitude - floor(magnitude)) > 1e-9
        let scaled = hasFraction
            ? String(format: "%.2f", magnitude)
            : String(format: "%.0f", magnitude)
        let parts = scaled.split(separator: ".", maxSplits: 1, omittingEmptySubsequences: false)
        let intPart = String(parts[0])
        let frac = parts.count > 1 ? String(parts[1]) : nil
        let grouped: String
        if intPart.count <= 3 {
            grouped = intPart
        } else {
            let last3 = String(intPart.suffix(3))
            var rest = String(intPart.dropLast(3))
            var chunks: [String] = []
            while rest.count > 2 {
                chunks.insert(String(rest.suffix(2)), at: 0)
                rest = String(rest.dropLast(2))
            }
            if !rest.isEmpty { chunks.insert(rest, at: 0) }
            grouped = chunks.joined(separator: ",") + "," + last3
        }
        if let frac { return "\(grouped).\(frac)" }
        return grouped
    }

    static func grouped(_ raw: String?, currency: String?) -> String {
        let value = raw?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        if value.isEmpty { return "—" }
        guard let amount = parse(value, currency: currency) else { return value }
        return grouped(amount, currency: currency)
    }

    static func amount(_ raw: String?, currency: String?) -> String {
        grouped(raw, currency: currency)
    }

    static func number(_ raw: String?) -> Double? { parse(raw) }

    static func tile(_ metric: OperatingBoardMetricPayload, currency: String?) -> String {
        let isCount = (metric.unit ?? "").caseInsensitiveCompare("count") == .orderedSame
        let text: String
        if isCount {
            let raw = metric.value?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
            if raw.isEmpty {
                text = "—"
            } else if let amount = parse(raw, currency: currency) {
                text = indianGroupedNumber(abs(amount))
            } else {
                text = raw
            }
        } else {
            text = grouped(metric.value, currency: currency)
        }
        if let unit = metric.unit?.trimmingCharacters(in: .whitespacesAndNewlines),
           !unit.isEmpty,
           !isCount {
            return "\(text) \(unit)"
        }
        return text
    }

    static func compact(_ amount: Double, currency: String?) -> String {
        let code = (currency ?? "INR").uppercased()
        let symbol = code == "INR" ? "₹" : "\(code) "
        let magnitude = abs(amount)
        let sign = amount < 0 ? "−" : ""
        func trim(_ text: String) -> String {
            text.hasSuffix(".0") ? String(text.dropLast(2)) : text
        }
        switch magnitude {
        case 0:
            return "\(symbol)0"
        case ..<1_000:
            return symbol + sign + String(format: "%.0f", magnitude)
        case ..<100_000:
            return symbol + sign + trim(String(format: "%.1fK", magnitude / 1_000))
        case ..<10_000_000:
            return symbol + sign + trim(String(format: "%.1fL", magnitude / 100_000))
        default:
            return symbol + sign + trim(String(format: "%.1fCr", magnitude / 10_000_000))
        }
    }
}
