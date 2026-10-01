import Foundation
import SwiftUI

enum MomentCardTapDestination: String, Equatable {
    /// M2: open existing activity detail/edit flow.
    case activityDetail
    /// Reserved for M5 clustered / cinematic moment detail.
    case momentDetail
}

enum MomentSourceIconKind: String, Equatable {
    case spend, income, mood, recovery, attention
    case milestone, progress, learning, opportunity, pivot
    case experience, wellbeing, discovery
    case connection, shared, support
    case generic

    var systemImage: String {
        switch self {
        case .spend: return "cart.fill"
        case .income: return "arrow.down.circle.fill"
        case .mood: return "face.smiling"
        case .recovery: return "waveform.path.ecg"
        case .attention: return "scope"
        case .milestone: return "flag.fill"
        case .progress: return "chart.line.uptrend.xyaxis"
        case .learning: return "book.fill"
        case .opportunity: return "sparkles"
        case .pivot: return "arrow.triangle.2.circlepath"
        case .experience: return "sparkles"
        case .wellbeing: return "heart.fill"
        case .discovery: return "safari.fill"
        case .connection: return "person.2.fill"
        case .shared: return "hands.clap.fill"
        case .support: return "heart.circle.fill"
        case .generic: return "bolt.fill"
        }
    }

    static func from(activityCode: String) -> MomentSourceIconKind {
        let code = activityCode.uppercased()
        if code.contains("EXPENSE") { return .spend }
        if code.contains("INCOME") { return .income }
        if code.contains("MOOD") { return .mood }
        if code.contains("RECOVERY") { return .recovery }
        if code.contains("ATTENTION") { return .attention }
        if code.contains("MILESTONE") { return .milestone }
        if code.contains("PROGRESS") { return .progress }
        if code.contains("LEARNING") { return .learning }
        if code.contains("OPPORTUNITY") { return .opportunity }
        if code.contains("PIVOT") { return .pivot }
        if code.contains("EXPERIENCE") { return .experience }
        if code.contains("WELLBEING") { return .wellbeing }
        if code.contains("DISCOVERY") || code.contains("CREATION") { return .discovery }
        if code.contains("CONNECTION") { return .connection }
        if code.contains("SHARED") { return .shared }
        if code.contains("SUPPORT") { return .support }
        return .generic
    }
}

/// Canonical Moment card for Pulse Recent (M1) and Moments stream (M2).
/// v1 maps 1:1 from an activity row; M5 clusters can set multiple `sourceActivityIds`.
struct MomentCardModel: Identifiable, Equatable {
    let id: String
    /// What happened.
    let title: String
    /// Short description only (not category dump).
    let subtitle: String?
    /// Dining / Work / Family / Learning, etc.
    let contextLabel: String?
    let family: PersonalPulseFamily
    let amountLabel: String?
    /// Human signal — not a score.
    let signalLabel: String?
    let peopleCount: Int?
    let occurredAt: String
    let sourceIconKind: MomentSourceIconKind
    let activityCode: String
    let sourceActivityIds: [String]
    let tapDestination: MomentCardTapDestination
    /// M5B — signed media URLs when present; empty for activity-only cards.
    let mediaUrls: [String]

    static func from(
        activity: APIClient.ActivityItemPayload,
        family: PersonalPulseFamily
    ) -> MomentCardModel {
        let code = activity.activityCode.uppercased()
        let context = PersonalActivityTimelineDerived.categoryChip(activity)
            ?? typeContextLabel(code: code, family: family)
        let subtitle: String? = {
            if let mood = PersonalActivityTimelineDerived.feelingsChip(activity) { return mood }
            if let desc = activity.activityPayload?.description?
                .trimmingCharacters(in: .whitespacesAndNewlines),
               !desc.isEmpty,
               desc.count <= 80,
               !PersonalActivityTimelineDerived.isEssayTitle(desc) {
                return desc
            }
            return nil
        }()
        let signal = signalLabel(code: code, family: family)
        let sourceId = activity.activityPayload?.activityId
            ?? activity.activityPayload?.expenseId
            ?? activity.activityPayload?.incomeId
            ?? activity.id
        return MomentCardModel(
            id: activity.id,
            title: PersonalActivityTimelineDerived.displayTitle(activity),
            subtitle: subtitle,
            contextLabel: context,
            family: family,
            amountLabel: PersonalActivityTimelineDerived.amountLabel(activity),
            signalLabel: signal,
            peopleCount: nil,
            occurredAt: activity.occurredAt,
            sourceIconKind: MomentSourceIconKind.from(activityCode: activity.activityCode),
            activityCode: activity.activityCode,
            sourceActivityIds: [sourceId],
            tapDestination: .activityDetail,
            mediaUrls: []
        )
    }

    private static func typeContextLabel(code: String, family: PersonalPulseFamily) -> String? {
        if code.contains("EXPENSE") { return nil }
        if code.contains("MOOD") { return "Mood" }
        if code.contains("RECOVERY") { return "Recovery" }
        if code.contains("ATTENTION") { return "Attention" }
        if code.contains("MILESTONE") { return "Milestone" }
        if code.contains("PROGRESS") { return "Progress" }
        if code.contains("LEARNING") { return "Learning" }
        if code.contains("EXPERIENCE") { return "Experience" }
        if code.contains("CONNECTION") { return "Connection" }
        if code.contains("SHARED") { return "Shared" }
        if code.contains("SUPPORT") { return "Support" }
        switch family {
        case .lifeOperations: return "Everyday"
        case .futureBuilding: return "Future"
        case .lifestyle: return "Lifestyle"
        case .relationships: return "People"
        }
    }

    private static func signalLabel(code: String, family: PersonalPulseFamily) -> String? {
        if code.contains("RECOVERY") { return "Recovery" }
        if code.contains("ATTENTION") { return "Discipline" }
        if code.contains("MILESTONE") { return "Milestone" }
        if code.contains("PROGRESS") { return "Progress" }
        if code.contains("EXPERIENCE") { return "Joy" }
        if code.contains("CONNECTION") { return "Presence" }
        if code.contains("EXPENSE"), family == .lifeOperations { return nil }
        return nil
    }
}

enum MomentCardTime {
    /// Render-time label from `occurredAt` (Today / Yesterday / clock / date).
    static func label(for occurredAt: String) -> String {
        guard let date = parse(occurredAt) else { return "" }
        let cal = Calendar.current
        if cal.isDateInToday(date) {
            let f = DateFormatter()
            f.dateFormat = "h:mm a"
            return f.string(from: date)
        }
        if cal.isDateInYesterday(date) { return "Yesterday" }
        let f = DateFormatter()
        f.dateFormat = "d MMM"
        return f.string(from: date)
    }

    static func parse(_ raw: String) -> Date? {
        let iso = ISO8601DateFormatter()
        iso.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        if let d = iso.date(from: raw) { return d }
        iso.formatOptions = [.withInternetDateTime]
        if let d = iso.date(from: raw) { return d }
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
        if let d = f.date(from: raw) { return d }
        f.dateFormat = "yyyy-MM-dd'T'HH:mm:ssZ"
        return f.date(from: raw)
    }
}

enum MomentStreamGroup: String, Equatable {
    case today = "Today"
    case yesterday = "Yesterday"
    case earlierThisWeek = "Earlier this week"
    case earlierMonth // uses month name

    static func group(for occurredAt: String, now: Date = Date()) -> (MomentStreamGroup, String) {
        guard let date = MomentCardTime.parse(occurredAt) else {
            return (.earlierMonth, "Earlier")
        }
        let cal = Calendar.current
        if cal.isDateInToday(date) { return (.today, MomentStreamGroup.today.rawValue) }
        if cal.isDateInYesterday(date) { return (.yesterday, MomentStreamGroup.yesterday.rawValue) }
        if let weekAgo = cal.date(byAdding: .day, value: -7, to: now), date >= weekAgo {
            return (.earlierThisWeek, MomentStreamGroup.earlierThisWeek.rawValue)
        }
        let f = DateFormatter()
        f.dateFormat = "MMMM"
        return (.earlierMonth, f.string(from: date))
    }

    static func sections(from cards: [MomentCardModel]) -> [(header: String, cards: [MomentCardModel])] {
        var order: [String] = []
        var map: [String: [MomentCardModel]] = [:]
        for card in cards {
            let (_, header) = group(for: card.occurredAt)
            if map[header] == nil {
                order.append(header)
                map[header] = []
            }
            map[header]?.append(card)
        }
        return order.map { ($0, map[$0] ?? []) }
    }
}
