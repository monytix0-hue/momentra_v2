import WidgetKit
import SwiftUI

private let groupAppGroup = "group.resolvingpoint.momentra"

private func groupStoredKey(_ key: String) -> String {
    UserDefaults(suiteName: groupAppGroup)?.string(forKey: key) ?? ""
}

// MARK: - Pulse

struct GroupPulseEntry: TimelineEntry {
    let date: Date
    let participantCount: Int?
    let attentionCount: Int?
    let openTaskCount: Int?
    let title: String?
}

struct GroupPulseProvider: TimelineProvider {
    func placeholder(in context: Context) -> GroupPulseEntry {
        GroupPulseEntry(date: Date(), participantCount: 6, attentionCount: 1, openTaskCount: 3, title: "Trip")
    }
    func getSnapshot(in context: Context, completion: @escaping (GroupPulseEntry) -> Void) {
        completion(read())
    }
    func getTimeline(in context: Context, completion: @escaping (Timeline<GroupPulseEntry>) -> Void) {
        completion(Timeline(entries: [read()], policy: .after(Date().addingTimeInterval(30 * 60))))
    }
    private func read() -> GroupPulseEntry {
        let mid = groupStoredKey("widget_last_moment_id")
        let snap = WidgetSnapshotManager.readGroupPulse(momentId: mid)
        return GroupPulseEntry(
            date: Date(),
            participantCount: snap?.participantCount,
            attentionCount: snap?.attentionCount,
            openTaskCount: snap?.openTaskCount,
            title: snap?.title
        )
    }
}

struct GroupPulseEntryView: View {
    var entry: GroupPulseEntry
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(entry.title ?? "Group").font(.caption).foregroundStyle(.secondary)
            HStack(spacing: 10) {
                Label("\(entry.participantCount ?? 0)", systemImage: "person.2")
                    .font(.caption)
                Label("\(entry.openTaskCount ?? 0)", systemImage: "checklist")
                    .font(.caption)
                if let attention = entry.attentionCount, attention > 0 {
                    Label("\(attention)", systemImage: "exclamationmark.triangle.fill")
                        .font(.caption)
                        .foregroundStyle(.orange)
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
        .padding()
    }
}

struct GroupPulseWidget: Widget {
    let kind: String = "GroupPulseWidget"
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: GroupPulseProvider()) { entry in
            GroupPulseEntryView(entry: entry)
        }
        .configurationDisplayName("Group Pulse")
        .description("Members, open tasks and attention.")
        .supportedFamilies([.systemSmall])
    }
}

// MARK: - Finance

struct GroupFinanceEntry: TimelineEntry {
    let date: Date
    let expenseTotal: String?
    let budgetTotal: String?
    let outstandingTotal: String?
    let currencyCode: String
    let viewerNetPosition: String?
}

struct GroupFinanceProvider: TimelineProvider {
    func placeholder(in context: Context) -> GroupFinanceEntry {
        GroupFinanceEntry(date: Date(), expenseTotal: "18,000", budgetTotal: "25,000", outstandingTotal: "4,500", currencyCode: "INR", viewerNetPosition: "+1,200")
    }
    func getSnapshot(in context: Context, completion: @escaping (GroupFinanceEntry) -> Void) {
        completion(read())
    }
    func getTimeline(in context: Context, completion: @escaping (Timeline<GroupFinanceEntry>) -> Void) {
        completion(Timeline(entries: [read()], policy: .after(Date().addingTimeInterval(30 * 60))))
    }
    private func read() -> GroupFinanceEntry {
        let mid = groupStoredKey("widget_last_moment_id")
        let snap = WidgetSnapshotManager.readGroupFinance(momentId: mid)
        return GroupFinanceEntry(
            date: Date(),
            expenseTotal: snap?.expenseTotal,
            budgetTotal: snap?.budgetTotal,
            outstandingTotal: snap?.outstandingTotal,
            currencyCode: snap?.currencyCode ?? "INR",
            viewerNetPosition: snap?.viewerNetPosition
        )
    }
}

struct GroupFinanceEntryView: View {
    var entry: GroupFinanceEntry
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Group Finance").font(.caption).foregroundStyle(.secondary)
            HStack(alignment: .firstTextBaseline, spacing: 6) {
                Text(entry.expenseTotal ?? "--").font(.system(size: 24, weight: .heavy))
                Text(entry.currencyCode).font(.caption)
            }
            HStack(spacing: 12) {
                Label(entry.budgetTotal ?? "--", systemImage: "chart.pie").font(.caption2)
                Label(entry.outstandingTotal ?? "--", systemImage: "exclamationmark").font(.caption2).foregroundStyle(.orange)
            }
            if let net = entry.viewerNetPosition, !net.isEmpty {
                Text("You · \(net)").font(.caption2).foregroundStyle(.secondary)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
        .padding()
    }
}

struct GroupFinanceWidget: Widget {
    let kind: String = "GroupFinanceWidget"
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: GroupFinanceProvider()) { entry in
            GroupFinanceEntryView(entry: entry)
        }
        .configurationDisplayName("Group Finance")
        .description("Shared expenses, budget and your position.")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}

// MARK: - Activity

struct GroupActivityEntry: TimelineEntry {
    let date: Date
    let items: [ActivityFeedWidgetSnapshot.ActivityRow]
}

struct GroupActivityProvider: TimelineProvider {
    func placeholder(in context: Context) -> GroupActivityEntry {
        GroupActivityEntry(date: Date(), items: [])
    }
    func getSnapshot(in context: Context, completion: @escaping (GroupActivityEntry) -> Void) {
        completion(read())
    }
    func getTimeline(in context: Context, completion: @escaping (Timeline<GroupActivityEntry>) -> Void) {
        completion(Timeline(entries: [read()], policy: .after(Date().addingTimeInterval(30 * 60))))
    }
    private func read() -> GroupActivityEntry {
        let mid = groupStoredKey("widget_last_moment_id")
        let snap = WidgetSnapshotManager.readGroupActivity(momentId: mid)
        return GroupActivityEntry(date: Date(), items: Array((snap?.items ?? []).prefix(3)))
    }
}

struct GroupActivityEntryView: View {
    var entry: GroupActivityEntry
    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("Group Activity").font(.caption).foregroundStyle(.secondary)
            if entry.items.isEmpty {
                Text("Nothing shared yet.").font(.caption2).foregroundStyle(.secondary)
            } else {
                ForEach(Array(entry.items.enumerated()), id: \.offset) { _, row in
                    VStack(alignment: .leading, spacing: 1) {
                        Text(row.title).font(.caption).lineLimit(1)
                        if let subtitle = row.subtitle, !subtitle.isEmpty {
                            Text(subtitle).font(.caption2).foregroundStyle(.secondary).lineLimit(1)
                        }
                    }
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
        .padding()
    }
}

struct GroupActivityWidget: Widget {
    let kind: String = "GroupActivityWidget"
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: GroupActivityProvider()) { entry in
            GroupActivityEntryView(entry: entry)
        }
        .configurationDisplayName("Group Activity")
        .description("Latest shared activity.")
        .supportedFamilies([.systemMedium])
    }
}

// MARK: - Memory

struct GroupMemoryEntry: TimelineEntry {
    let date: Date
    let memoryCount: Int?
    let recentTitles: [String]
}

struct GroupMemoryProvider: TimelineProvider {
    func placeholder(in context: Context) -> GroupMemoryEntry {
        GroupMemoryEntry(date: Date(), memoryCount: 4, recentTitles: [])
    }
    func getSnapshot(in context: Context, completion: @escaping (GroupMemoryEntry) -> Void) {
        completion(read())
    }
    func getTimeline(in context: Context, completion: @escaping (Timeline<GroupMemoryEntry>) -> Void) {
        completion(Timeline(entries: [read()], policy: .after(Date().addingTimeInterval(60 * 60))))
    }
    private func read() -> GroupMemoryEntry {
        let mid = groupStoredKey("widget_last_moment_id")
        let snap = WidgetSnapshotManager.readGroupMemory(momentId: mid)
        return GroupMemoryEntry(
            date: Date(),
            memoryCount: snap?.memoryCount,
            recentTitles: Array((snap?.recentTitles ?? []).prefix(3))
        )
    }
}

struct GroupMemoryEntryView: View {
    var entry: GroupMemoryEntry
    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("Group Memory").font(.caption).foregroundStyle(.secondary)
            HStack(alignment: .firstTextBaseline, spacing: 6) {
                Text("\(entry.memoryCount ?? 0)").font(.system(size: 24, weight: .heavy))
                Text("shared").font(.caption2).foregroundStyle(.secondary)
            }
            ForEach(entry.recentTitles, id: \.self) { title in
                Text("• \(title)").font(.caption2).lineLimit(1)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
        .padding()
    }
}

struct GroupMemoryWidget: Widget {
    let kind: String = "GroupMemoryWidget"
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: GroupMemoryProvider()) { entry in
            GroupMemoryEntryView(entry: entry)
        }
        .configurationDisplayName("Group Memory")
        .description("Shared memories in this moment.")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}

// MARK: - Attendance

struct GroupAttendanceEntry: TimelineEntry {
    let date: Date
    let confirmed: Int
    let pending: Int
    let declined: Int
}

struct GroupAttendanceProvider: TimelineProvider {
    func placeholder(in context: Context) -> GroupAttendanceEntry {
        GroupAttendanceEntry(date: Date(), confirmed: 5, pending: 2, declined: 0)
    }
    func getSnapshot(in context: Context, completion: @escaping (GroupAttendanceEntry) -> Void) {
        completion(read())
    }
    func getTimeline(in context: Context, completion: @escaping (Timeline<GroupAttendanceEntry>) -> Void) {
        completion(Timeline(entries: [read()], policy: .after(Date().addingTimeInterval(30 * 60))))
    }
    private func read() -> GroupAttendanceEntry {
        let mid = groupStoredKey("widget_last_moment_id")
        let snap = WidgetSnapshotManager.readGroupAttendance(momentId: mid)
        return GroupAttendanceEntry(
            date: Date(),
            confirmed: snap?.confirmed ?? 0,
            pending: snap?.pending ?? 0,
            declined: snap?.declined ?? 0
        )
    }
}

struct GroupAttendanceEntryView: View {
    var entry: GroupAttendanceEntry
    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("Attendance").font(.caption).foregroundStyle(.secondary)
            HStack(spacing: 12) {
                Label("\(entry.confirmed)", systemImage: "checkmark.circle.fill")
                    .font(.caption).foregroundStyle(.green)
                Label("\(entry.pending)", systemImage: "clock")
                    .font(.caption).foregroundStyle(.orange)
                Label("\(entry.declined)", systemImage: "xmark.circle")
                    .font(.caption).foregroundStyle(.secondary)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
        .padding()
    }
}

struct GroupAttendanceWidget: Widget {
    let kind: String = "GroupAttendanceWidget"
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: GroupAttendanceProvider()) { entry in
            GroupAttendanceEntryView(entry: entry)
        }
        .configurationDisplayName("Group Attendance")
        .description("Confirmed, pending and declined.")
        .supportedFamilies([.systemSmall])
    }
}

// MARK: - Roster

struct GroupRosterEntry: TimelineEntry {
    let date: Date
    let members: [GroupRosterWidgetSnapshot.RosterMember]
}

struct GroupRosterProvider: TimelineProvider {
    func placeholder(in context: Context) -> GroupRosterEntry {
        GroupRosterEntry(date: Date(), members: [])
    }
    func getSnapshot(in context: Context, completion: @escaping (GroupRosterEntry) -> Void) {
        completion(read())
    }
    func getTimeline(in context: Context, completion: @escaping (Timeline<GroupRosterEntry>) -> Void) {
        completion(Timeline(entries: [read()], policy: .after(Date().addingTimeInterval(30 * 60))))
    }
    private func read() -> GroupRosterEntry {
        let mid = groupStoredKey("widget_last_moment_id")
        let snap = WidgetSnapshotManager.readGroupRoster(momentId: mid)
        return GroupRosterEntry(date: Date(), members: snap?.members ?? [])
    }
}

struct GroupRosterEntryView: View {
    var entry: GroupRosterEntry
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("People").font(.caption).foregroundStyle(.secondary)
            ForEach(entry.members.prefix(5), id: \.displayName) { m in
                Text(m.displayName ?? "Member").font(.caption).lineLimit(1)
            }
        }.padding()
    }
}

struct GroupRosterWidget: Widget {
    let kind: String = "GroupRosterWidget"
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: GroupRosterProvider()) { entry in
            GroupRosterEntryView(entry: entry)
        }
        .configurationDisplayName("Group Roster")
        .description("People in the active group moment.")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}
