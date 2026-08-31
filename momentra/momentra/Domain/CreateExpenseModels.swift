import Foundation

struct CreateExpenseOutcome: Equatable {
    let expenseId: String
    let momentId: String
    let amount: String
    let currencyCode: String
    let status: String
    let version: Int
    let projectionHints: [ProjectionHint]
}

enum ExpenseMoney {
    private static let pattern = try! NSRegularExpression(pattern: #"^[0-9]+(\.[0-9]{1,4})?$"#)

    static func normalize(_ raw: String) -> String? {
        var trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
            .replacingOccurrences(of: ",", with: ".")
        if trimmed.isEmpty { return nil }
        if trimmed.hasPrefix("-") || trimmed.hasPrefix("+") { return nil }
        while trimmed.hasPrefix("0"), trimmed.count > 1, !trimmed.hasPrefix("0.") {
            trimmed.removeFirst()
        }
        if trimmed.hasPrefix(".") { trimmed = "0" + trimmed }
        if trimmed.hasSuffix(".") { trimmed.removeLast() }
        if trimmed.isEmpty { trimmed = "0" }
        let range = NSRange(trimmed.startIndex..<trimmed.endIndex, in: trimmed)
        guard pattern.firstMatch(in: trimmed, range: range) != nil else { return nil }
        return trimmed
    }

    static func isValidPositive(_ raw: String) -> Bool {
        guard let n = normalize(raw) else { return false }
        let parts = n.split(separator: ".", omittingEmptySubsequences: false)
        let whole = Int(parts[0]) ?? 0
        let fracStr = parts.count > 1 ? String(parts[1]).padding(toLength: 4, withPad: "0", startingAt: 0) : "0000"
        let frac = Int(fracStr) ?? 0
        return whole > 0 || frac > 0
    }

    static func validateForSubmit(_ raw: String) -> String? {
        guard isValidPositive(raw) else { return nil }
        return normalize(raw)
    }
}
