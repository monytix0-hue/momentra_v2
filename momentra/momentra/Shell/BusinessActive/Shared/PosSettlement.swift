import Foundation

/// Shared POS payable + split-tender helpers for Retail / Restaurant Charge.
enum PosSettlement {
    static let tenderMethods = ["UPI", "Cash", "Card"]

    static func parseAmount(_ raw: String?) -> Decimal {
        let trimmed = (raw ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty, let value = Decimal(string: trimmed), value >= 0 else { return 0 }
        return value
    }

    static func money(_ value: Decimal) -> String {
        var rounded = value
        var result = Decimal()
        NSDecimalRound(&result, &rounded, 4, .plain)
        return NSDecimalNumber(decimal: result).stringValue
    }

    static func payable(gross: Decimal, discount: Decimal, cessAmount: Decimal = 0) -> Decimal {
        max(0, gross - max(0, discount)) + max(0, cessAmount)
    }

    static func buildPayments(
        split: Bool,
        primary: String,
        payable: Decimal,
        splitAmounts: [String: String],
        openAr: Bool = false
    ) -> [OperatingPaymentInput]? {
        guard !openAr, payable > 0 else { return nil }
        if !split {
            return [OperatingPaymentInput(method: primary, amount: money(payable))]
        }
        let rows = tenderMethods.compactMap { method -> OperatingPaymentInput? in
            let amount = parseAmount(splitAmounts[method])
            guard amount > 0 else { return nil }
            return OperatingPaymentInput(method: method, amount: money(amount))
        }
        return rows.isEmpty ? nil : rows
    }

    static func splitSumsToPayable(_ splitAmounts: [String: String], payable: Decimal) -> Bool {
        let sum = tenderMethods.reduce(Decimal.zero) { $0 + parseAmount(splitAmounts[$1]) }
        var left = sum
        var right = payable
        var a = Decimal()
        var b = Decimal()
        NSDecimalRound(&a, &left, 2, .plain)
        NSDecimalRound(&b, &right, 2, .plain)
        return a == b
    }

    static func tenderLabel(split: Bool, primary: String, payments: [OperatingPaymentInput]?) -> String {
        guard split else { return primary }
        let parts = (payments ?? []).map { "\($0.method) \($0.amount)" }
        return parts.isEmpty ? "Split" : parts.joined(separator: " + ")
    }
}
