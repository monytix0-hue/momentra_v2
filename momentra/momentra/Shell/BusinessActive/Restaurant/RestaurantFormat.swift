import SwiftUI

/// Formatting helpers for the restaurant section.
///
/// The Figma frames always show money in Indian compact notation (`₹3.2L`, `₹28K`,
/// `₹1.1L`) — never a raw `320000.0000`. Missing money is `—`, never `₹0`, and a
/// missing denominator is `Unavailable`, never `0%`.
enum RestaurantFormat {

    /// Locale-aware compact money: `₹3.2L`, `₹28K`, `₹1.1L`, `₹42K`.
    static func compact(_ raw: String?, currency: String?, visible: Bool = true) -> String {
        guard visible, let raw, !raw.isEmpty, let value = Decimal(string: raw) else { return "—" }
        let symbol = (currency?.isEmpty ?? true) || currency == "INR" ? "₹" : "\(currency!) "
        let negative = value < 0
        let magnitude = magnitude(value)
        let body: String
        switch magnitude {
        case 10_000_000...: body = trim(magnitude / 10_000_000) + "Cr"
        case 100_000...: body = trim(magnitude / 100_000) + "L"
        case 1_000...: body = trim(magnitude / 1_000) + "K"
        default:
            var rounded = Decimal()
            var v = magnitude
            NSDecimalRound(&rounded, &v, 0, .plain)
            body = "\(rounded)"
        }
        return (negative ? "−" : "") + symbol + body
    }

    /// Exact money for statements: `₹3,20,000`.
    static func exact(_ raw: String?, currency: String?, visible: Bool = true) -> String {
        guard visible, let raw, !raw.isEmpty, let value = Decimal(string: raw) else { return "—" }
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        formatter.maximumFractionDigits = 2
        formatter.locale = ((currency?.isEmpty ?? true) || currency == "INR")
            ? Locale(identifier: "en_IN")
            : Locale(identifier: "en_US")
        let number = NSDecimalNumber(decimal: value)
        let text = formatter.string(from: number) ?? raw
        if (currency?.isEmpty ?? true) || currency == "INR" { return "₹\(text)" }
        return "\(text) \(currency!)"
    }

    /// `80%` from a part/whole pair. Returns `nil` when the whole is zero — never `0%`.
    static func percent(_ part: String?, _ whole: String?) -> String? {
        guard let fraction = fraction(part, whole) else { return nil }
        return "\(Int((fraction * 100).rounded()))%"
    }

    /// Fraction for a progress ring; `nil` when there is no configured whole.
    static func fraction(_ part: String?, _ whole: String?) -> Double? {
        guard let p = part.flatMap({ Decimal(string: $0) }),
              let w = whole.flatMap({ Decimal(string: $0) }),
              w > 0 else { return nil }
        let ratio = (p as NSDecimalNumber).doubleValue / (w as NSDecimalNumber).doubleValue
        return min(max(ratio, 0), 1)
    }

    /// `Nov 2026`.
    static func month(_ iso: String?) -> String {
        format(iso, "MMM yyyy")
    }

    /// `Nov 2026 · INR`.
    static func periodLine(_ start: String?, currency: String?) -> String {
        "\(month(start)) · \(currency?.isEmpty == false ? currency! : "INR")"
    }

    /// `29 Oct 2026`.
    static func day(_ iso: String?) -> String {
        format(iso, "dd MMM yyyy")
    }

    /// `Today · 29 Oct 2026`.
    static func todayLine() -> String {
        "Today · \(day(isoDate(Date())))"
    }

    /// `02 Nov`.
    static func shortDay(_ iso: String?) -> String? {
        guard let date = parse(iso) else { return nil }
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "dd MMM"
        return formatter.string(from: date)
    }

    /// `As of 29 Oct`.
    static func asOf(_ iso: String? = nil) -> String {
        "As of \(shortDay(iso) ?? shortDay(isoDate(Date())) ?? "")"
    }

    /// `186` — count KPI, never money.
    static func count(_ value: Int) -> String { "\(value)" }

    /// Replaces raw `1234.0000` tokens inside a board `detail` sentence.
    static func detail(_ detail: String?, currency: String?) -> String {
        guard let detail, !detail.isEmpty else { return "—" }
        guard let regex = try? NSRegularExpression(pattern: #"\d+\.\d{4}(?:\s+[A-Z]{3})?"#) else { return detail }
        var result = detail
        let matches = regex.matches(in: detail, range: NSRange(detail.startIndex..., in: detail))
        for match in matches.reversed() {
            guard let range = Range(match.range, in: detail) else { continue }
            let token = String(detail[range]).split(separator: " ").first.map(String.init) ?? ""
            let replacement = exact(token, currency: currency)
            if let target = Range(match.range, in: result) {
                result.replaceSubrange(target, with: replacement)
            }
        }
        return result
    }

    static func parse(_ iso: String?) -> Date? {
        guard let iso, !iso.isEmpty else { return nil }
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter.date(from: String(iso.prefix(10)))
    }

    static func isoDate(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter.string(from: date)
    }

    private static func format(_ iso: String?, _ pattern: String) -> String {
        let date = parse(iso) ?? Date()
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = pattern
        return formatter.string(from: date)
    }

    private static func magnitude(_ value: Decimal) -> Decimal {
        value < 0 ? -value : value
    }

    private static func trim(_ value: Decimal) -> String {
        var rounded = Decimal()
        var v = value
        NSDecimalRound(&rounded, &v, 2, .plain)
        var text = "\(rounded)"
        while text.hasSuffix("0") { text.removeLast() }
        if text.hasSuffix(".") { text.removeLast() }
        return text
    }
}

/// Ember colour for an attention severity, matching the Figma severity chips.
func restaurantSeverityColor(_ severity: String?) -> Color {
    switch severity?.uppercased() {
    case "HIGH", "CRITICAL": return RestaurantEmber.negative
    case "MEDIUM": return RestaurantEmber.warning
    default: return RestaurantEmber.ember
    }
}

/// Short label for an attention row: `Unavailable` when no source event exists.
func restaurantSeverityLabel(kind: String?, severity: String?, hasSource: Bool) -> String {
    guard hasSource else { return "Unavailable" }
    let kind = kind?.uppercased() ?? ""
    if kind.hasPrefix("OVERDUE") { return "Overdue" }
    if kind.hasPrefix("DUE") { return "Due" }
    switch kind {
    case "APPROVAL": return "Approval"
    case "LOW_STOCK": return "Reorder"
    case "CONFLICT": return "Conflict"
    case "CHARGE_PENDING": return "Charge"
    default:
        if kind.contains("BUDGET") { return "Budget" }
        return severity?.uppercased() == "HIGH" ? "Alert" : "Open"
    }
}

/// Human label for an operating event type, in kitchen language.
func restaurantEventLabel(_ code: String) -> String {
    switch code {
    case "SALE": return "Bill"
    case "PURCHASE": return "Purchase"
    case "SPEND": return "Expense"
    case "PAYMENT": return "Payment"
    case "COLLECTION": return "Collection"
    case "STOCK": return "Stock move"
    case "ORDER", "CUSTOMER_ORDER": return "Order"
    case "ISSUE": return "Issue"
    case "UPDATE": return "Update"
    case "APPROVAL": return "Approval"
    case "BUDGET": return "Budget"
    case "TARGET": return "Target"
    case "SHIFT": return "Shift"
    case "REVISION": return "Revision"
    default:
        return code.lowercased().replacingOccurrences(of: "_", with: " ").capitalized
    }
}

/// `Done` / `Open` / `Part` … for a payment or approval status.
func restaurantPrettyStatus(_ status: String) -> String {
    switch status {
    case "COLLECTED", "PAID", "SETTLED": return "Done"
    case "PENDING": return "Open"
    case "PARTIAL": return "Part"
    case "OVERDUE": return "Overdue"
    case "CANCELLED": return "Void"
    default: return status.lowercased().capitalized
    }
}

/// Order status → the kitchen-facing word (`PLACED` → `Sent`, `PREPARING` → `Firing`).
func restaurantOrderStatusLabel(_ status: String?) -> String {
    switch (status ?? "").uppercased() {
    case "PLACED": return "Sent"
    case "PREPARING": return "Firing"
    case "READY": return "Ready"
    case "COMPLETED": return "Served"
    case "CANCELLED": return "Void"
    case "ACCEPTED": return "Accepted"
    default:
        let raw = status?.trimmingCharacters(in: .whitespaces) ?? ""
        return raw.isEmpty ? "Open" : raw.lowercased().capitalized
    }
}
