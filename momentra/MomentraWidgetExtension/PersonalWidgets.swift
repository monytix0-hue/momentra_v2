import WidgetKit
import SwiftUI

// Personal widgets read the same App Group container `WidgetSnapshotManager` writes to.
private let personalAppGroup = "group.resolvingpoint.momentra"

private func personalStoredKey(_ key: String) -> String {
    UserDefaults(suiteName: personalAppGroup)?.string(forKey: key) ?? ""
}

// MARK: - Pulse

struct PersonalPulseEntry: TimelineEntry {
    let date: Date
    let recoveryScore: String?
    let activeMomentCount: Int
    let attentionCount: Int?
    let moodState: String?
}

struct PersonalPulseProvider: TimelineProvider {
    func placeholder(in context: Context) -> PersonalPulseEntry {
        PersonalPulseEntry(date: Date(), recoveryScore: "72", activeMomentCount: 2, attentionCount: 0, moodState: "Steady")
    }
    func getSnapshot(in context: Context, completion: @escaping (PersonalPulseEntry) -> Void) {
        completion(read())
    }
    func getTimeline(in context: Context, completion: @escaping (Timeline<PersonalPulseEntry>) -> Void) {
        completion(Timeline(entries: [read()], policy: .after(Date().addingTimeInterval(30 * 60))))
    }
    private func read() -> PersonalPulseEntry {
        let uid = personalStoredKey("widget_last_user_id")
        let snap = WidgetSnapshotManager.readPersonalPulse(userId: uid)
        return PersonalPulseEntry(
            date: Date(),
            recoveryScore: snap?.recoveryScore,
            activeMomentCount: snap?.activeMomentCount ?? 0,
            attentionCount: snap?.attentionCount,
            moodState: snap?.moodState
        )
    }
}

struct PersonalPulseEntryView: View {
    var entry: PersonalPulseEntry
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Personal Pulse").font(.caption).foregroundStyle(.secondary)
            HStack(alignment: .firstTextBaseline, spacing: 6) {
                Text(entry.recoveryScore ?? "--")
                    .font(.system(size: 28, weight: .heavy))
                Text("recovery").font(.caption).foregroundStyle(.secondary)
            }
            if let mood = entry.moodState, !mood.isEmpty {
                Text("Mood · \(mood)").font(.caption2)
            }
            HStack(spacing: 10) {
                Label("\(entry.activeMomentCount) active", systemImage: "circle.grid.2x2")
                    .font(.caption2)
                if let attention = entry.attentionCount, attention > 0 {
                    Label("\(attention)", systemImage: "exclamationmark.triangle.fill")
                        .font(.caption2)
                        .foregroundStyle(.orange)
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
        .padding()
        .accessibilityElement(children: .combine)
    }
}

struct PersonalPulseWidget: Widget {
    let kind: String = "PersonalPulseWidget"
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: PersonalPulseProvider()) { entry in
            PersonalPulseEntryView(entry: entry)
        }
        .configurationDisplayName("Personal Pulse")
        .description("Recovery, mood and what needs attention today.")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}

// MARK: - Life

struct PersonalLifeEntry: TimelineEntry {
    let date: Date
    let score: Int?
    let scoreMax: Int?
    let statusLabel: String?
    let trendLabel: String?
    let topAreas: [PersonalLifeWidgetSnapshot.LifeAreaScore]
}

struct PersonalLifeProvider: TimelineProvider {
    func placeholder(in context: Context) -> PersonalLifeEntry {
        PersonalLifeEntry(date: Date(), score: 68, scoreMax: 100, statusLabel: "Balanced", trendLabel: "Up 3", topAreas: [])
    }
    func getSnapshot(in context: Context, completion: @escaping (PersonalLifeEntry) -> Void) {
        completion(read())
    }
    func getTimeline(in context: Context, completion: @escaping (Timeline<PersonalLifeEntry>) -> Void) {
        completion(Timeline(entries: [read()], policy: .after(Date().addingTimeInterval(60 * 60))))
    }
    private func read() -> PersonalLifeEntry {
        let uid = personalStoredKey("widget_last_user_id")
        let snap = WidgetSnapshotManager.readPersonalLife(userId: uid)
        return PersonalLifeEntry(
            date: Date(),
            score: snap?.score,
            scoreMax: snap?.scoreMax,
            statusLabel: snap?.statusLabel,
            trendLabel: snap?.trendLabel,
            topAreas: Array((snap?.areaScores ?? []).prefix(3))
        )
    }
}

struct PersonalLifeEntryView: View {
    var entry: PersonalLifeEntry
    private var scoreLine: String {
        guard let score = entry.score else { return "--" }
        if let max = entry.scoreMax { return "\(score)/\(max)" }
        return "\(score)"
    }
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Life").font(.caption).foregroundStyle(.secondary)
            HStack(alignment: .firstTextBaseline, spacing: 6) {
                Text(scoreLine).font(.system(size: 24, weight: .heavy))
                if let trend = entry.trendLabel, !trend.isEmpty {
                    Text(trend).font(.caption2).foregroundStyle(.green)
                }
            }
            if let status = entry.statusLabel, !status.isEmpty {
                Text(status).font(.caption2)
            }
            ForEach(entry.topAreas, id: \.code) { area in
                HStack {
                    Text(area.label).font(.caption2).lineLimit(1)
                    Spacer()
                    Text(area.score.map(String.init) ?? "--").font(.caption2).bold()
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
        .padding()
    }
}

struct PersonalLifeWidget: Widget {
    let kind: String = "PersonalLifeWidget"
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: PersonalLifeProvider()) { entry in
            PersonalLifeEntryView(entry: entry)
        }
        .configurationDisplayName("Personal Life")
        .description("Life score and the areas driving it.")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}

// MARK: - Activity feed

struct PersonalActivityEntry: TimelineEntry {
    let date: Date
    let items: [ActivityFeedWidgetSnapshot.ActivityRow]
}

struct PersonalActivityProvider: TimelineProvider {
    func placeholder(in context: Context) -> PersonalActivityEntry {
        PersonalActivityEntry(date: Date(), items: [])
    }
    func getSnapshot(in context: Context, completion: @escaping (PersonalActivityEntry) -> Void) {
        completion(read())
    }
    func getTimeline(in context: Context, completion: @escaping (Timeline<PersonalActivityEntry>) -> Void) {
        completion(Timeline(entries: [read()], policy: .after(Date().addingTimeInterval(30 * 60))))
    }
    private func read() -> PersonalActivityEntry {
        let uid = personalStoredKey("widget_last_user_id")
        let snap = WidgetSnapshotManager.readPersonalActivity(userId: uid)
        return PersonalActivityEntry(date: Date(), items: Array((snap?.items ?? []).prefix(3)))
    }
}

struct PersonalActivityEntryView: View {
    var entry: PersonalActivityEntry
    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("Recent Activity").font(.caption).foregroundStyle(.secondary)
            if entry.items.isEmpty {
                Text("Nothing recorded yet.").font(.caption2).foregroundStyle(.secondary)
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

struct PersonalActivityWidget: Widget {
    let kind: String = "PersonalActivityWidget"
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: PersonalActivityProvider()) { entry in
            PersonalActivityEntryView(entry: entry)
        }
        .configurationDisplayName("Personal Activity")
        .description("Latest personal activity.")
        .supportedFamilies([.systemMedium])
    }
}
