import Foundation

enum GroupBudgetUtils {
    static let customOption = "Custom…"
    static let presetOptions = ["₹80,000", "₹50,000", "₹1,20,000", "₹25,000", customOption]
    static let purchaseAmountOptions = ["₹25,000", "₹50,000", "₹1,00,000", customOption]
    static let livingBudgetOptions = ["₹25,000", "₹40,000", "₹60,000", customOption]

    static func parseDisplayToApiAmount(_ display: String) -> String? {
        let digits = display
            .replacingOccurrences(of: "₹", with: "")
            .replacingOccurrences(of: ",", with: "")
            .replacingOccurrences(of: " ", with: "")
            .trimmingCharacters(in: .whitespacesAndNewlines)
        guard !digits.isEmpty else { return nil }
        let pattern = "^\\d+(\\.\\d{1,4})?$"
        guard digits.range(of: pattern, options: .regularExpression) != nil else { return nil }
        return digits
    }

    static func resolveBudgetAmount(displayBudget: String, customAmount: String) -> String? {
        if displayBudget == customOption {
            return parseDisplayToApiAmount(customAmount)
        }
        return parseDisplayToApiAmount(displayBudget)
    }

    static func formatApiAmountForDisplay(_ amount: String, currencyCode: String = "INR") -> String {
        guard let value = Double(amount) else { return amount }
        let prefix: String = switch currencyCode {
        case "INR": "₹"
        case "USD": "$"
        case "EUR": "€"
        default: "\(currencyCode) "
        }
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        formatter.groupingSeparator = ","
        formatter.maximumFractionDigits = value.truncatingRemainder(dividingBy: 1) == 0 ? 0 : 2
        let body = formatter.string(from: NSNumber(value: value)) ?? amount
        return "\(prefix)\(body)"
    }

    static func formatCustomAmountInput(_ raw: String) -> String {
        let digits = raw.filter(\.isNumber)
        guard !digits.isEmpty, let num = Int64(digits) else { return "" }
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        formatter.groupingSeparator = ","
        formatter.maximumFractionDigits = 0
        return formatter.string(from: NSNumber(value: num)) ?? digits
    }

    static func summaryLabel(displayBudget: String, customAmount: String) -> String {
        if displayBudget == customOption {
            if let parsed = parseDisplayToApiAmount(customAmount) {
                return formatApiAmountForDisplay(parsed)
            }
            return "Custom amount"
        }
        return displayBudget
    }
}
