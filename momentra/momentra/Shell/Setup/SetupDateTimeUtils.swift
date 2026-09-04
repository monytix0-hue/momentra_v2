import Foundation

enum SetupDateTimeUtils {
    static func formatDateDisplay(_ iso: String?) -> String {
        guard let iso, !iso.isEmpty else { return "Select date" }
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withFullDate]
        if let date = formatter.date(from: String(iso.prefix(10))) {
            return date.formatted(date: .abbreviated, time: .omitted)
        }
        return iso
    }

    static func formatDateTimeDisplay(_ iso: String?) -> String {
        guard let iso, !iso.isEmpty else { return "Select date & time" }
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        if let date = formatter.date(from: iso) ?? ISO8601DateFormatter().date(from: iso) {
            return date.formatted(date: .abbreviated, time: .shortened)
        }
        return iso
    }

    static func formatDateRangeDisplay(startIso: String?, endIso: String?) -> String {
        let start = formatDateDisplay(startIso)
        let end = formatDateDisplay(endIso)
        if (startIso ?? "").isEmpty && (endIso ?? "").isEmpty { return "Select dates" }
        if (endIso ?? "").isEmpty || start == end { return start }
        return "\(start) – \(end)"
    }

    static func isoDateToStartInstant(_ iso: String?) -> String? {
        guard let iso, !iso.isEmpty else { return nil }
        var components = DateComponents()
        let parts = iso.prefix(10).split(separator: "-").compactMap { Int($0) }
        guard parts.count == 3 else { return nil }
        components.year = parts[0]
        components.month = parts[1]
        components.day = parts[2]
        components.hour = 0
        components.minute = 0
        components.second = 0
        components.timeZone = TimeZone(secondsFromGMT: 0)
        guard let date = Calendar.current.date(from: components) else { return nil }
        return ISO8601DateFormatter().string(from: date)
    }

    static func isoDateToEndInstant(_ iso: String?) -> String? {
        guard let iso, !iso.isEmpty else { return nil }
        var components = DateComponents()
        let parts = iso.prefix(10).split(separator: "-").compactMap { Int($0) }
        guard parts.count == 3 else { return nil }
        components.year = parts[0]
        components.month = parts[1]
        components.day = parts[2]
        components.hour = 23
        components.minute = 59
        components.second = 0
        components.timeZone = TimeZone(secondsFromGMT: 0)
        guard let date = Calendar.current.date(from: components) else { return nil }
        return ISO8601DateFormatter().string(from: date)
    }

    static func localDateString(from date: Date) -> String {
        let formatter = DateFormatter()
        formatter.calendar = Calendar(identifier: .gregorian)
        formatter.dateFormat = "yyyy-MM-dd"
        formatter.timeZone = .current
        return formatter.string(from: date)
    }

    static func localDateTimeString(from date: Date) -> String {
        let formatter = DateFormatter()
        formatter.calendar = Calendar(identifier: .gregorian)
        formatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss"
        formatter.timeZone = .current
        return formatter.string(from: date)
    }

    static func formatTimeDisplay(_ iso: String?) -> String {
        guard let iso, !iso.isEmpty else { return "Select time" }
        let formatter = DateFormatter()
        formatter.dateFormat = "HH:mm:ss"
        formatter.timeZone = .current
        var date = formatter.date(from: iso)
        if date == nil {
            formatter.dateFormat = "HH:mm"
            date = formatter.date(from: iso)
        }
        guard let date else { return iso }
        return date.formatted(date: .omitted, time: .shortened)
    }

    static func localTimeString(from date: Date) -> String {
        let formatter = DateFormatter()
        formatter.calendar = Calendar(identifier: .gregorian)
        formatter.dateFormat = "HH:mm"
        formatter.timeZone = .current
        return formatter.string(from: date)
    }

    static func timeFromIso(_ iso: String?) -> Date {
        guard let iso, !iso.isEmpty else {
            return Calendar.current.date(bySettingHour: 9, minute: 0, second: 0, of: Date()) ?? Date()
        }
        let formatter = DateFormatter()
        formatter.calendar = Calendar(identifier: .gregorian)
        formatter.timeZone = .current
        formatter.dateFormat = "HH:mm:ss"
        if let date = formatter.date(from: iso) { return date }
        formatter.dateFormat = "HH:mm"
        return formatter.date(from: iso)
            ?? Calendar.current.date(bySettingHour: 9, minute: 0, second: 0, of: Date())
            ?? Date()
    }

    static func dateFromIso(_ iso: String?) -> Date {
        guard let iso, !iso.isEmpty else { return Date() }
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        formatter.timeZone = .current
        return formatter.date(from: String(iso.prefix(10))) ?? Date()
    }

    /// Combine local `yyyy-MM-dd` + `HH:mm` into an offset ISO-8601 datetime for API `.datetime()`.
    static func combineLocalDateTimeIso(date: String?, time: String?) -> String? {
        let dateRaw = (date ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
        let timeRaw = (time ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
        guard !dateRaw.isEmpty || !timeRaw.isEmpty else { return nil }
        let cal = Calendar.current
        var comps = DateComponents()
        let daySource = dateRaw.isEmpty ? Date() : dateFromIso(dateRaw)
        let day = cal.dateComponents([.year, .month, .day], from: daySource)
        comps.year = day.year
        comps.month = day.month
        comps.day = day.day
        if timeRaw.isEmpty {
            comps.hour = 0
            comps.minute = 0
        } else {
            let t = cal.dateComponents([.hour, .minute], from: timeFromIso(timeRaw))
            comps.hour = t.hour
            comps.minute = t.minute
        }
        comps.second = 0
        guard let combined = cal.date(from: comps) else { return nil }
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime]
        formatter.timeZone = .current
        return formatter.string(from: combined)
    }
}
