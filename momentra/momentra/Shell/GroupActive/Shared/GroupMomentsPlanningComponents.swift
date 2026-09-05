import SwiftUI

// MARK: - Types (APIClient nesting)

typealias GroupPlanningItem = APIClient.GroupLifePayload.LifeInner.PlanningItem
typealias GroupUpdateItem = APIClient.GroupLifePayload.LifeInner.UpdateItem

// MARK: - Helpers

/// Open planning items sorted by dueAt then createdAt; take up to `limit`.
func recentOpenPlanningItems(
    _ items: [GroupPlanningItem],
    limit: Int = 5
) -> [GroupPlanningItem] {
    func isOpen(_ status: String?) -> Bool {
        guard let status, !status.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return true }
        let upper = status.uppercased()
        return upper != "DONE" && upper != "CANCELLED"
    }

    return items
        .filter { isOpen($0.status) }
        .sorted { a, b in
            let aDue = parsePlanningInstant(a.dueAt)?.timeIntervalSince1970 ?? Double.greatestFiniteMagnitude
            let bDue = parsePlanningInstant(b.dueAt)?.timeIntervalSince1970 ?? Double.greatestFiniteMagnitude
            if aDue != bDue { return aDue < bDue }
            let aCreated = parsePlanningInstant(a.createdAt)?.timeIntervalSince1970 ?? 0
            let bCreated = parsePlanningInstant(b.createdAt)?.timeIntervalSince1970 ?? 0
            return aCreated > bCreated
        }
        .prefix(limit)
        .map { $0 }
}

func parsePlanningInstant(_ iso: String?) -> Date? {
    guard let iso, !iso.isEmpty else { return nil }
    let withFraction = ISO8601DateFormatter()
    withFraction.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
    if let date = withFraction.date(from: iso) { return date }
    let plain = ISO8601DateFormatter()
    plain.formatOptions = [.withInternetDateTime]
    if let date = plain.date(from: iso) { return date }
    let dayOnly = DateFormatter()
    dayOnly.calendar = Calendar(identifier: .gregorian)
    dayOnly.locale = Locale(identifier: "en_US_POSIX")
    dayOnly.timeZone = .current
    dayOnly.dateFormat = "yyyy-MM-dd"
    return dayOnly.date(from: String(iso.prefix(10)))
}

func planningItemDayKey(_ item: GroupPlanningItem) -> Date? {
    guard let due = parsePlanningInstant(item.dueAt) else { return nil }
    return Calendar.current.startOfDay(for: due)
}

func formatPlanningTime(_ iso: String?) -> String? {
    guard let date = parsePlanningInstant(iso) else { return nil }
    let out = DateFormatter()
    out.locale = .current
    out.dateFormat = "h:mm a"
    return out.string(from: date)
}

func formatPlanningDayChip(_ day: Date, today: Date = Calendar.current.startOfDay(for: Date())) -> String {
    let cal = Calendar.current
    let start = cal.startOfDay(for: day)
    if start == today { return "Today" }
    if let tomorrow = cal.date(byAdding: .day, value: 1, to: today), start == tomorrow {
        return "Tomorrow"
    }
    let out = DateFormatter()
    out.locale = .current
    out.dateFormat = "EEE d"
    return out.string(from: start)
}

func isUrgentUpdate(_ item: GroupUpdateItem) -> Bool {
    (item.urgencyCode ?? "").caseInsensitiveCompare("URGENT") == .orderedSame
}

/// Completion percent for planning items: DONE / (OPEN+IN_PROGRESS+DONE). Zero when none countable.
func planningPlansPercent(_ items: [GroupPlanningItem]) -> Int {
    var done = 0
    var countable = 0
    for item in items {
        let status = (item.status ?? "").uppercased()
        if status == "CANCELLED" { continue }
        countable += 1
        if status == "DONE" { done += 1 }
    }
    guard countable > 0 else { return 0 }
    return Int((Double(done) / Double(countable) * 100).rounded())
}

func formatRelativeShort(_ iso: String?) -> String {
    guard let date = parsePlanningInstant(iso) else { return "" }
    let seconds = Int(-date.timeIntervalSinceNow)
    if seconds < 60 { return "just now" }
    if seconds < 3600 { return "\(seconds / 60)m ago" }
    if seconds < 86_400 { return "\(seconds / 3600)h ago" }
    if seconds < 86_400 * 7 { return "\(seconds / 86_400)d ago" }
    let out = DateFormatter()
    out.dateFormat = "d MMM"
    return out.string(from: date)
}

func formatPollClosesMeta(closesAt: String?, totalVotes: Int?) -> String {
    let votes = totalVotes ?? 0
    let votePart = votes == 1 ? "1 vote" : "\(votes) votes"
    guard let closes = parsePlanningInstant(closesAt) else { return votePart }
    let remaining = closes.timeIntervalSinceNow
    if remaining <= 0 { return "Ended · \(votePart)" }
    if remaining < 3600 {
        let mins = max(1, Int(remaining / 60))
        return "Ends in \(mins)m · \(votePart)"
    }
    if remaining < 86_400 {
        let hours = Int(remaining / 3600)
        return "Ends in \(hours)h · \(votePart)"
    }
    let days = Int(remaining / 86_400)
    if days == 1 { return "Ends tomorrow · \(votePart)" }
    return "Ends in \(days)d · \(votePart)"
}

/// Status pill text for polls list (Figma): "Ends in 2h", "Ends tomorrow", "Closed".
func formatPollEndsTag(closesAt: String?, status: String?) -> String {
    let upper = (status ?? "").uppercased()
    if upper == "CLOSED" || upper == "CANCELLED" { return "Closed" }
    guard let closes = parsePlanningInstant(closesAt) else {
        return upper == "OPEN" || upper.isEmpty ? "Open" : upper.capitalized
    }
    let remaining = closes.timeIntervalSinceNow
    if remaining <= 0 { return "Closed" }
    if remaining < 3600 {
        let mins = max(1, Int(remaining / 60))
        return "Ends in \(mins)m"
    }
    if remaining < 86_400 {
        let hours = Int(remaining / 3600)
        return "Ends in \(hours)h"
    }
    let days = Int(remaining / 86_400)
    if days == 1 { return "Ends tomorrow" }
    return "Ends in \(days)d"
}

func initialsFromName(_ name: String?) -> String {
    let parts = (name ?? "")
        .split(separator: " ")
        .filter { !$0.isEmpty }
    if parts.isEmpty { return "?" }
    if parts.count == 1 { return String(parts[0].prefix(2)).uppercased() }
    return "\(parts[0].prefix(1))\(parts[1].prefix(1))".uppercased()
}

func formatBookingDay(_ iso: String?) -> String? {
    guard let date = parsePlanningInstant(iso) else { return nil }
    let out = DateFormatter()
    out.dateFormat = "d MMM"
    return out.string(from: date)
}

func formatBookingDayTime(_ iso: String?) -> String? {
    guard let date = parsePlanningInstant(iso) else { return nil }
    let out = DateFormatter()
    out.dateFormat = "d MMM · h:mm a"
    return out.string(from: date)
}

func formatItineraryDayLabel(dayIndex: Int, date: Date) -> String {
    let out = DateFormatter()
    out.dateFormat = "d MMM"
    return "DAY \(dayIndex) • \(out.string(from: date).uppercased())"
}

/// Distinct due-days for itinerary preview, preserving chronological order.
func itineraryDayGroups(
    _ items: [GroupPlanningItem],
    limit: Int = 3
) -> [(day: Date, items: [GroupPlanningItem])] {
    let open = recentOpenPlanningItems(items, limit: 50)
    var orderedDays: [Date] = []
    var buckets: [Date: [GroupPlanningItem]] = [:]
    for item in open {
        guard let day = planningItemDayKey(item) else { continue }
        if buckets[day] == nil {
            orderedDays.append(day)
            buckets[day] = []
        }
        buckets[day, default: []].append(item)
    }
    return orderedDays.prefix(limit).compactMap { day in
        guard let dayItems = buckets[day], !dayItems.isEmpty else { return nil }
        return (day, dayItems)
    }
}

// MARK: - Schedule sheet

struct PlanningScheduleSheet: View {
    let items: [GroupPlanningItem]
    var momentTypeCode: String? = nil
    var accent: Color = Color(hex: "#14B8A6")
    var surface: Color = Color(hex: "#1C1A24")
    var field: Color = Color(hex: "#252230")
    var border: Color = Color(hex: "#322E40")
    var text: Color = .white
    var muted: Color = Color(hex: "#9E9AA8")
    var onDismiss: () -> Void

    @State private var selectedDay: Date?

    private var today: Date { Calendar.current.startOfDay(for: Date()) }

    private var dayKeys: [Date] {
        let fromItems = items.compactMap { planningItemDayKey($0) }
        return Array(Set([today] + fromItems)).sorted()
    }

    private var activeDay: Date {
        selectedDay ?? dayKeys.first ?? today
    }

    private var dayItems: [GroupPlanningItem] {
        items
            .filter { planningItemDayKey($0) == activeDay }
            .sorted {
                (parsePlanningInstant($0.dueAt)?.timeIntervalSince1970 ?? Double.greatestFiniteMagnitude)
                    < (parsePlanningInstant($1.dueAt)?.timeIntervalSince1970 ?? Double.greatestFiniteMagnitude)
            }
    }

    var body: some View {
        NativeSheetScaffold(
            title: "Schedule",
            onClose: onDismiss,
            background: surface
        ) {
            VStack(alignment: .leading, spacing: 14) {
                Text("Plans by day")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(muted)

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(dayKeys, id: \.self) { day in
                            let selected = day == activeDay
                            Button {
                                selectedDay = day
                            } label: {
                                Text(formatPlanningDayChip(day, today: today))
                                    .font(.plusJakarta(size: 13, weight: selected ? .bold : .semibold))
                                    .foregroundStyle(selected ? Color.white : muted)
                                    .padding(.horizontal, 14)
                                    .padding(.vertical, 8)
                                    .background(selected ? accent : field)
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 20)
                                            .stroke(selected ? accent : border, lineWidth: 1)
                                    )
                                    .clipShape(RoundedRectangle(cornerRadius: 20))
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }

                ScrollView {
                    LazyVStack(alignment: .leading, spacing: 8) {
                        if dayItems.isEmpty {
                            Text("No plans for this day")
                                .font(.plusJakarta(size: 13))
                                .foregroundStyle(muted)
                                .padding(.vertical, 24)
                        } else {
                            ForEach(Array(dayItems.enumerated()), id: \.offset) { _, item in
                                PlanningScheduleRow(
                                    item: item,
                                    momentTypeCode: momentTypeCode,
                                    field: field,
                                    border: border,
                                    text: text,
                                    muted: muted,
                                    accent: accent
                                )
                            }
                        }
                    }
                    .frame(minHeight: 280, alignment: .top)
                }
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 28)
        }
        .onAppear {
            if selectedDay == nil {
                selectedDay = dayKeys.first ?? today
            }
        }
    }
}

struct PlanningScheduleRow: View {
    let item: GroupPlanningItem
    let momentTypeCode: String?
    var field: Color
    var border: Color
    var text: Color
    var muted: Color
    var accent: Color

    var body: some View {
        let category = GroupPlanningCategoryCatalog.label(forCode: item.categoryCode, momentTypeCode: momentTypeCode)
        let time = formatPlanningTime(item.dueAt) ?? "All day"
        HStack(alignment: .center, spacing: 10) {
            Text(time)
                .font(.plusJakarta(size: 12, weight: .bold))
                .foregroundStyle(accent)
                .padding(.trailing, 4)
            VStack(alignment: .leading, spacing: 4) {
                Text(category)
                    .font(.plusJakarta(size: 10, weight: .semibold))
                    .foregroundStyle(accent)
                Text(item.title ?? item.planningItemId ?? "Plan")
                    .font(.plusJakarta(size: 13, weight: .semibold))
                    .foregroundStyle(text)
            }
            Spacer(minLength: 0)
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(field)
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(border, lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

// MARK: - Row helpers

struct MomentsPlanningHeader: View {
    let title: String
    var text: Color
    var muted: Color
    var accent: Color
    var onOpenSchedule: () -> Void

    var body: some View {
        HStack {
            Text(title)
                .font(.plusJakarta(size: 16, weight: .bold))
                .foregroundStyle(text)
            Spacer()
            Button(action: onOpenSchedule) {
                Image(systemName: "calendar")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundStyle(accent)
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Schedule")
        }
    }
}

struct MomentsPlanningRecentRow: View {
    let item: GroupPlanningItem
    let momentTypeCode: String?
    var text: Color
    var muted: Color
    var accent: Color
    var field: Color
    var border: Color

    var body: some View {
        let category = GroupPlanningCategoryCatalog.label(forCode: item.categoryCode, momentTypeCode: momentTypeCode)
        let time = formatPlanningTime(item.dueAt)
        VStack(alignment: .leading, spacing: 6) {
            HStack(spacing: 8) {
                Text(category)
                    .font(.plusJakarta(size: 10, weight: .bold))
                    .foregroundStyle(accent)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3)
                    .background(accent.opacity(0.15))
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                if let time, !time.isEmpty {
                    Text(time)
                        .font(.plusJakarta(size: 11))
                        .foregroundStyle(muted)
                }
            }
            Text(item.title ?? item.planningItemId ?? "Plan")
                .font(.plusJakarta(size: 13, weight: .semibold))
                .foregroundStyle(text)
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(field)
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(border, lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

struct MomentsUrgentUpdateRow: View {
    let item: GroupUpdateItem
    var text: Color
    var muted: Color
    var field: Color
    var border: Color

    var body: some View {
        let urgent = isUrgentUpdate(item)
        let accentBorder = urgent ? Color(hex: "#F59E0B") : border
        VStack(alignment: .leading, spacing: 6) {
            if urgent {
                Text("Urgent")
                    .font(.plusJakarta(size: 10, weight: .bold))
                    .foregroundStyle(Color(hex: "#F87171"))
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3)
                    .background(Color(hex: "#F87171").opacity(0.15))
                    .clipShape(RoundedRectangle(cornerRadius: 8))
            }
            Text(item.message ?? item.updateId ?? "Update")
                .font(.plusJakarta(size: 13))
                .foregroundStyle(text)
            if let created = item.createdAt, !created.isEmpty {
                Text(String(created.prefix(16)).replacingOccurrences(of: "T", with: " "))
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(muted)
            }
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(field)
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(accentBorder, lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}
