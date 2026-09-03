import Foundation

/// Shared Life Ops derived metrics for Pulse / Moments / Memory populated screens.
enum PersonalLifeOpsDerived {
    static func scoreNumber(_ raw: String?) -> Double? {
        guard let s = raw?.trimmingCharacters(in: .whitespacesAndNewlines), !s.isEmpty else { return nil }
        return Double(s)
    }

    static func displayScore(_ raw: String?) -> String {
        guard var s = raw?.trimmingCharacters(in: .whitespacesAndNewlines), !s.isEmpty else { return "—" }
        if s.hasSuffix(".00") { s.removeLast(3) }
        else if s.hasSuffix(".0") { s.removeLast(2) }
        if let n = Double(s) { return String(Int(n.rounded())) }
        return s
    }

    /// Pressure ← 100 − recovery when recovery exists.
    static func pressure(fromRecovery recovery: String?) -> String {
        guard let n = scoreNumber(recovery) else { return "—" }
        return String(Int((100 - n).rounded()))
    }

    static func attentionDisplay(count: Int?) -> String {
        guard let n = count, n > 0 else { return "—" }
        return String(min(100, 40 + n * 8))
    }

    static func statusBadge(forScore raw: String?, axis: String) -> String {
        guard let n = scoreNumber(raw) else { return "Empty" }
        switch axis.lowercased() {
        case "pressure":
            if n >= 70 { return "High" }
            if n >= 45 { return "Moderate" }
            return "Low"
        case "recovery":
            if n >= 75 { return "Strong" }
            if n >= 50 { return "Steady" }
            return "Low"
        case "discipline":
            if n >= 80 { return "Excellent" }
            if n >= 55 { return "Good" }
            return "Building"
        case "attention":
            if n >= 75 { return "Good" }
            if n >= 45 { return "Fair" }
            return "Low"
        default:
            if n >= 80 { return "Excellent" }
            if n >= 60 { return "Good" }
            return "Building"
        }
    }

    static func stageBand(wellbeing: String?) -> String {
        guard let n = scoreNumber(wellbeing) else { return "Stabilizing" }
        if n >= 75 { return "Thriving" }
        if n >= 50 { return "Structured" }
        return "Stabilizing"
    }

    static func streakDays(from occurredAtDates: [String]) -> Int {
        let days = Set(occurredAtDates.compactMap { dayKey(from: $0) })
        guard !days.isEmpty else { return 0 }
        var streak = 0
        var cursor = Calendar.current.startOfDay(for: Date())
        let formatter = DateFormatter()
        formatter.calendar = Calendar(identifier: .gregorian)
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "yyyy-MM-dd"
        while true {
            let key = formatter.string(from: cursor)
            if days.contains(key) {
                streak += 1
                guard let prev = Calendar.current.date(byAdding: .day, value: -1, to: cursor) else { break }
                cursor = prev
            } else {
                break
            }
        }
        return streak
    }

    static func dayKey(from iso: String) -> String? {
        guard let date = parseISO(iso) else { return nil }
        let f = DateFormatter()
        f.calendar = Calendar(identifier: .gregorian)
        f.locale = Locale(identifier: "en_US_POSIX")
        f.dateFormat = "yyyy-MM-dd"
        return f.string(from: date)
    }

    static func parseISO(_ iso: String) -> Date? {
        let a = ISO8601DateFormatter()
        a.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        let b = ISO8601DateFormatter()
        b.formatOptions = [.withInternetDateTime]
        return a.date(from: iso) ?? b.date(from: iso)
    }

    static func relativeTime(_ iso: String) -> String {
        guard let date = parseISO(iso) else { return iso }
        let seconds = Int(Date().timeIntervalSince(date))
        if seconds < 60 { return "Just now" }
        if seconds < 3600 { return "\(seconds / 60)m ago" }
        if seconds < 86_400 { return "\(seconds / 3600)h ago" }
        if seconds < 172_800 { return "Yesterday" }
        return "\(seconds / 86_400)d ago"
    }

    struct DriverItem: Identifiable {
        let id = UUID()
        let label: String
        let helping: Bool
    }

    static func helpingHurting(from activities: [(code: String, title: String)]) -> (helping: [DriverItem], hurting: [DriverItem]) {
        var helping: [DriverItem] = []
        var hurting: [DriverItem] = []
        for item in activities.prefix(8) {
            let code = item.code.uppercased()
            if code.contains("RECOVERY") || code.contains("MILESTONE") || code.contains("LEARNING") || code.contains("OPPORTUNITY") || code.contains("PROGRESS") || code.contains("EXPERIENCE") || code.contains("WELLBEING") || code.contains("DISCOVERY") || code.contains("CREATION") {
                let label: String
                if code.contains("MILESTONE") { label = "Vision +" }
                else if code.contains("LEARNING") { label = "Growth +" }
                else if code.contains("OPPORTUNITY") { label = "Opportunity +" }
                else if code.contains("PROGRESS") { label = "Momentum +" }
                else if code.contains("EXPERIENCE") { label = "Joy +" }
                else if code.contains("WELLBEING") { label = "Vitality +" }
                else if code.contains("DISCOVERY") || code.contains("CREATION") { label = "Exploration +" }
                else if code.contains("RECOVERY") { label = "Recovery +" }
                else { label = item.title }
                helping.append(DriverItem(label: label, helping: true))
            } else if code.contains("MOOD") {
                helping.append(DriverItem(label: "Mood · \(item.title)", helping: true))
            } else if code.contains("RHYTHM") || code.contains("WELLBEING") || code.contains("PIVOT") || code.contains("LIFESTYLE") {
                helping.append(DriverItem(label: code.contains("PIVOT") ? "Pivot +" : "Focus +", helping: true))
            } else if code.contains("EXPENSE") {
                hurting.append(DriverItem(label: "Spend · \(item.title)", helping: false))
            }
        }
        return (Array(helping.prefix(3)), Array(hurting.prefix(3)))
    }

    static func identityLabel(wellbeing: String?, recovery: String?, activityCount: Int) -> (title: String, confidence: String, body: String) {
        guard activityCount > 0 || scoreNumber(wellbeing) != nil || scoreNumber(recovery) != nil else {
            return ("Building Operator", "Low confidence", "Log recovery, mood, and spend to reveal your operating identity.")
        }
        let stage = stageBand(wellbeing: wellbeing)
        switch stage {
        case "Thriving":
            return ("Adaptive Operator", "\(min(92, 55 + activityCount * 3))% confidence", "You respond best when structure and recovery work together.")
        case "Structured":
            return ("Structured Operator", "\(min(85, 45 + activityCount * 3))% confidence", "Your rhythm is forming — keep pairing pressure with recovery.")
        default:
            return ("Stabilizing Operator", "Building…", "Early signals show up once recovery and attention logs accumulate.")
        }
    }
}
