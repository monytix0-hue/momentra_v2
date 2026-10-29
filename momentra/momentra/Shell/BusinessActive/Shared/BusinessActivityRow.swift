import SwiftUI

/// Shared Business activity row, mirroring `GroupActivityRow` (Figma 584:15872):
/// a 2pt accent bar, a 36pt accent-tinted glyph tile, the event title, a muted meta line,
/// the relative time, and the right-aligned amount.
///
/// Replaces the 14 per-module copies ("private fun ActivityRow" on Android,
/// "private func activityRow" on iOS) that each wrapped a whole bordered card per event.
/// Rows are separated by a hairline, not by a card.
struct BusinessActivityRow: View {
    let row: OperatingActivityPayload
    let model: OperatingProjectionPayload
    var accent: Color = Color(hex: "#818CF8")
    var showDivider: Bool = true
    var onEdit: (() -> Void)? = nil

    private var amount: String? {
        guard model.financeVisible == true else { return nil }
        let text = BusinessActivityFormat.amount(row.amount, currency: model.currency)
        return text == "—" ? nil : text
    }

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 12) {
                RoundedRectangle(cornerRadius: 1)
                    .fill(accent.opacity(0.4))
                    .frame(width: 2, height: 36)

                Text(BusinessActivityFormat.glyph(row.eventType))
                    .font(.plusJakarta(size: 16))
                    .frame(width: 36, height: 36)
                    .background(accent.opacity(0.12), in: RoundedRectangle(cornerRadius: 10))

                VStack(alignment: .leading, spacing: 2) {
                    Text(BusinessActivityFormat.title(row))
                        .font(.plusJakarta(size: 14, weight: .semibold))
                        .foregroundStyle(BusinessSheetTokens.text)
                        .lineLimit(2)
                    let meta = BusinessActivityFormat.meta(row)
                    if !meta.isEmpty {
                        Text(meta)
                            .font(.plusJakarta(size: 12))
                            .foregroundStyle(BusinessSheetTokens.muted)
                            .lineLimit(1)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                VStack(alignment: .trailing, spacing: 2) {
                    if let amount {
                        Text(amount)
                            .font(.plusJakarta(size: 13, weight: .semibold))
                            .foregroundStyle(accent)
                    }
                    Text(BusinessActivityFormat.relativeTime(row.occurredAt))
                        .font(.plusJakarta(size: 11))
                        .foregroundStyle(BusinessSheetTokens.muted)
                }
            }
            .padding(.vertical, 12)
            .contentShape(Rectangle())
            .onTapGesture { onEdit?() }
            .allowsHitTesting(onEdit != nil)

            if showDivider {
                Rectangle()
                    .fill(BusinessSheetTokens.border)
                    .frame(height: 1)
                    .padding(.leading, 50)
            }
        }
    }
}

/// A run of activity rows as one continuous list with hairlines between rows.
struct BusinessActivityList: View {
    let rows: [OperatingActivityPayload]
    let model: OperatingProjectionPayload
    var accent: Color = Color(hex: "#818CF8")
    var onEdit: ((OperatingActivityPayload) -> Void)? = nil

    var body: some View {
        VStack(spacing: 0) {
            ForEach(Array(rows.enumerated()), id: \.element.id) { index, row in
                BusinessActivityRow(
                    row: row,
                    model: model,
                    accent: accent,
                    showDivider: index < rows.count - 1,
                    onEdit: onEdit.map { edit in { edit(row) } }
                )
            }
        }
    }
}

enum BusinessActivityFormat {
    /// "WALK_IN_SALE" -> "Walk-in sale"
    static func label(_ eventType: String) -> String {
        guard !eventType.isEmpty else { return "Activity" }
        let words = eventType.replacingOccurrences(of: "_", with: " ").lowercased()
        return words.prefix(1).uppercased() + words.dropFirst()
    }

    static func title(_ row: OperatingActivityPayload) -> String {
        let trimmed = row.title.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? label(row.eventType) : trimmed
    }

    /// Event, counterparty, quantity, payment state and the calendar day — one muted line.
    static func meta(_ row: OperatingActivityPayload) -> String {
        var bits: [String] = [label(row.eventType)]
        if let party = row.counterpartyName?.trimmingCharacters(in: .whitespacesAndNewlines), !party.isEmpty {
            bits.append(party)
        }
        let qty = [row.quantity, row.unit]
            .compactMap { $0?.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
            .joined(separator: " ")
        if !qty.isEmpty { bits.append(qty) }
        if let payment = row.paymentStatus, !payment.isEmpty, payment != "NONE" { bits.append(payment) }
        if let status = row.status, !status.isEmpty, status != "COMPLETED", status != "NONE" { bits.append(status) }
        let day = String(row.occurredAt.prefix(10))
        if !day.isEmpty { bits.append(day) }
        return bits.joined(separator: " · ")
    }

    static func glyph(_ eventType: String) -> String {
        let upper = eventType.uppercased()
        if upper.contains("SALE") || upper.contains("REVENUE") { return "🧾" }
        if upper.contains("PURCHASE") || upper.contains("SUPPLIER") { return "📦" }
        if upper.contains("COLLECT") || upper.contains("PAYMENT") || upper.contains("SETTLE") { return "💰" }
        if upper.contains("SPEND") || upper.contains("EXPENSE") || upper.contains("COST") { return "💸" }
        if upper.contains("STOCK") || upper.contains("DELIVERY") || upper.contains("SHIPMENT") { return "🚚" }
        if upper.contains("ORDER") { return "📝" }
        if upper.contains("SERVICE") || upper.contains("APPOINTMENT") || upper.contains("BOOKING") { return "🎧" }
        if upper.contains("PRODUCTION") || upper.contains("JOB") || upper.contains("BATCH") { return "🏭" }
        if upper.contains("CAMPAIGN") || upper.contains("MARKETING") || upper.contains("LEAD") { return "📣" }
        if upper.contains("RETURN") || upper.contains("VOID") { return "↩️" }
        return "📌"
    }

    /// "Today, 4:12 PM" / "Yesterday, 9:30 AM" / "12 Oct, 6:05 PM" — matches Group.
    static func relativeTime(_ raw: String) -> String {
        guard !raw.isEmpty else { return "" }
        let iso = ISO8601DateFormatter()
        iso.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        let fallback = ISO8601DateFormatter()
        fallback.formatOptions = [.withInternetDateTime]
        guard let date = iso.date(from: raw) ?? fallback.date(from: raw) else {
            return String(raw.prefix(10))
        }
        let calendar = Calendar.current
        let timeFormatter = DateFormatter()
        timeFormatter.locale = .current
        timeFormatter.dateFormat = "h:mm a"
        if calendar.isDateInToday(date) {
            return "Today, \(timeFormatter.string(from: date))"
        }
        if calendar.isDateInYesterday(date) {
            return "Yesterday, \(timeFormatter.string(from: date))"
        }
        let dayFormatter = DateFormatter()
        dayFormatter.locale = .current
        dayFormatter.dateFormat = "d MMM"
        return "\(dayFormatter.string(from: date)), \(timeFormatter.string(from: date))"
    }

    /// Sum of quantity × unit price across itemized lines; nil when no line has a price.
    static func lineTotal(quantity: String?, price: String?) -> String? {
        guard let q = Double((quantity ?? "").trimmingCharacters(in: .whitespaces)),
              let p = Double((price ?? "").trimmingCharacters(in: .whitespaces)) else { return nil }
        let total = q * p
        guard total > 0 else { return nil }
        let rounded = total.rounded()
        if abs(total - rounded) < 0.005 { return "\(Int(rounded))" }
        return String(format: "%.2f", total)
    }

    static func money(_ total: Double, currency: String?) -> String {
        guard total > 0 else { return "—" }
        let code = (currency?.isEmpty == false) ? currency! : "INR"
        let symbol = code.uppercased() == "INR" ? "₹" : "\(code) "
        let rounded = total.rounded()
        if abs(total - rounded) < 0.005 { return "\(symbol)\(Int(rounded))" }
        return String(format: "%@%.2f", symbol, total)
    }

    static func amount(_ raw: String?, currency: String?) -> String {
        guard let raw, !raw.isEmpty, let value = Double(raw.replacingOccurrences(of: ",", with: "")) else {
            return "—"
        }
        let code = (currency?.isEmpty == false) ? currency! : "INR"
        let symbol = code.uppercased() == "INR" ? "₹" : "\(code) "
        let rounded = value.rounded()
        if abs(value - rounded) < 0.005 {
            return "\(symbol)\(Int(rounded))"
        }
        return String(format: "%@%.2f", symbol, value)
    }
}
