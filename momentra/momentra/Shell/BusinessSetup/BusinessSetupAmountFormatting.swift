import Foundation

/// Live Indian amount grouping for Business Setup Amount fields.
/// Strips non-digits except one decimal point; groups the integer with
/// Indian commas (`##,##,###`); clamps fraction to at most 2 digits.
enum BusinessSetupAmountFormatting {

    static func isAmountHelper(_ helper: String) -> Bool {
        helper == "Amount" || helper.hasPrefix("Amount ")
    }

    static func plainAmount(_ raw: String) -> String {
        var out = ""
        var sawDot = false
        for ch in raw {
            if ch.isNumber {
                out.append(ch)
            } else if ch == ".", !sawDot {
                out.append(".")
                sawDot = true
            }
        }
        return out
    }

    static func formatIndianAmount(_ raw: String) -> String {
        let cleaned = plainAmount(raw)
        if cleaned.isEmpty { return "" }
        let parts = cleaned.split(separator: ".", omittingEmptySubsequences: false)
        let intRaw = String(parts.first ?? "")
        let frac: String = {
            guard parts.count > 1 else { return "" }
            let digits = String(parts[1].prefix(2))
            return "." + digits
        }()
        let trimmed = intRaw.drop { $0 == "0" }
        let digits: String = {
            if trimmed.isEmpty {
                return (!intRaw.isEmpty || !frac.isEmpty) ? "0" : ""
            }
            return String(trimmed)
        }()
        if digits.isEmpty { return "" }
        return indianGroup(digits) + frac
    }

    private static func indianGroup(_ digits: String) -> String {
        if digits.count <= 3 { return digits }
        let tail = String(digits.suffix(3))
        let head = String(digits.dropLast(3))
        let groupedHead = String(head.reversed())
            .chunked(into: 2)
            .joined(separator: ",")
        return String(groupedHead.reversed()) + "," + tail
    }
}

private extension String {
    func chunked(into size: Int) -> [String] {
        guard size > 0, !isEmpty else { return [] }
        var result: [String] = []
        var index = startIndex
        while index < endIndex {
            let next = self.index(index, offsetBy: size, limitedBy: endIndex) ?? endIndex
            result.append(String(self[index..<next]))
            index = next
        }
        return result
    }
}
