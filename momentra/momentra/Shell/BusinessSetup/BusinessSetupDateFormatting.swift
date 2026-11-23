import Foundation

// Facts for create:
// Callers: BusinessSetupDocumentKit.DateRow (~line DateRow body), FieldRow forceUppercase via
// forceUppercaseInput, BusinessSetupShell fieldKey routing.
// No existing file serves day vs planMonth (YYYY-MM) setup display; SetupDateTimeUtils is day ISO only.
// Schemas: prefs startDate/endDate/gstEffectiveFrom as YYYY-MM-DD; planMonth as YYYY-MM; gstin uppercase.
// User verbatim: "Implement the plan as specified, it is attached for your reference."

/// Display / storage helpers for Business Setup DateRow pickers.
enum BusinessSetupDateFormatting {
    enum Mode {
        case day
        case month
    }

    static func modeForKey(_ fieldKey: String) -> Mode {
        fieldKey == "planMonth" ? .month : .day
    }

    static func allowClearForKey(_ fieldKey: String) -> Bool {
        fieldKey == "endDate"
    }

    static func forceUppercaseInput(_ raw: String) -> String {
        raw.uppercased()
    }

    static func formatDisplay(_ value: String, mode: Mode, placeholder: String) -> String {
        let raw = value.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !raw.isEmpty else { return placeholder }
        guard let date = parseToDate(raw) else { return value }
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.calendar = Calendar(identifier: .gregorian)
        switch mode {
        case .day:
            formatter.dateFormat = "d MMMM yyyy"
        case .month:
            formatter.dateFormat = "MMMM yyyy"
        }
        return formatter.string(from: date)
    }

    static func toStorage(from date: Date, mode: Mode) -> String {
        let formatter = DateFormatter()
        formatter.calendar = Calendar(identifier: .gregorian)
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = .current
        switch mode {
        case .day:
            formatter.dateFormat = "yyyy-MM-dd"
        case .month:
            formatter.dateFormat = "yyyy-MM"
        }
        return formatter.string(from: date)
    }

    static func parseToDate(_ value: String) -> Date? {
        let raw = value.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !raw.isEmpty else { return nil }
        let formatter = DateFormatter()
        formatter.calendar = Calendar(identifier: .gregorian)
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = .current
        if raw.range(of: #"^\d{4}-\d{2}-\d{2}"#, options: .regularExpression) != nil {
            formatter.dateFormat = "yyyy-MM-dd"
            return formatter.date(from: String(raw.prefix(10)))
        }
        if raw.range(of: #"^\d{4}-\d{2}$"#, options: .regularExpression) != nil {
            formatter.dateFormat = "yyyy-MM-dd"
            return formatter.date(from: "\(raw)-01")
        }
        formatter.dateFormat = "d MMMM yyyy"
        if let date = formatter.date(from: raw) { return date }
        formatter.dateFormat = "MMMM yyyy"
        return formatter.date(from: raw)
    }
}
