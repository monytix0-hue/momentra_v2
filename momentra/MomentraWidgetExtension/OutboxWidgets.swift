import ActivityKit
import WidgetKit
import SwiftUI

private let outboxAppGroup = "group.resolvingpoint.momentra"

private func outboxStoredKey(_ key: String) -> String {
    UserDefaults(suiteName: outboxAppGroup)?.string(forKey: key) ?? ""
}

// MARK: - Outbox monitor

struct OutboxMonitorEntry: TimelineEntry {
    let date: Date
    let pendingCount: Int
    let failedCount: Int
    let failedMessage: String?
    let lastUpdated: Date?
}

struct OutboxMonitorProvider: TimelineProvider {
    func placeholder(in context: Context) -> OutboxMonitorEntry {
        OutboxMonitorEntry(date: Date(), pendingCount: 0, failedCount: 0, failedMessage: nil, lastUpdated: nil)
    }
    func getSnapshot(in context: Context, completion: @escaping (OutboxMonitorEntry) -> Void) {
        completion(read())
    }
    func getTimeline(in context: Context, completion: @escaping (Timeline<OutboxMonitorEntry>) -> Void) {
        // The outbox can turn over on any save; refresh every 15 minutes.
        completion(Timeline(entries: [read()], policy: .after(Date().addingTimeInterval(15 * 60))))
    }
    private func read() -> OutboxMonitorEntry {
        let uid = outboxStoredKey("widget_last_user_id")
        let snap = WidgetSnapshotManager.readOutbox(userId: uid)
        return OutboxMonitorEntry(
            date: Date(),
            pendingCount: snap?.pendingCount ?? 0,
            failedCount: snap?.failedCount ?? 0,
            failedMessage: snap?.failedMessage,
            lastUpdated: snap.map { Date(timeIntervalSince1970: $0.lastUpdated) }
        )
    }
}

struct OutboxMonitorEntryView: View {
    var entry: OutboxMonitorEntry
    private var isEmpty: Bool { entry.pendingCount == 0 && entry.failedCount == 0 }
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Sync").font(.caption).foregroundStyle(.secondary)
            if isEmpty {
                Label("All caught up", systemImage: "checkmark.circle.fill")
                    .font(.caption)
                    .foregroundStyle(.green)
            } else {
                HStack(spacing: 12) {
                    if entry.pendingCount > 0 {
                        Label("\(entry.pendingCount) waiting", systemImage: "arrow.triangle.2.circlepath")
                            .font(.caption)
                    }
                    if entry.failedCount > 0 {
                        Label("\(entry.failedCount) failed", systemImage: "exclamationmark.triangle.fill")
                            .font(.caption)
                            .foregroundStyle(.orange)
                    }
                }
                if let message = entry.failedMessage, !message.isEmpty {
                    Text(message).font(.caption2).lineLimit(2).foregroundStyle(.secondary)
                }
            }
            if let updated = entry.lastUpdated {
                Text(updated, style: .relative)
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
        .padding()
        .accessibilityElement(children: .combine)
    }
}

struct OutboxMonitorWidget: Widget {
    let kind: String = "OutboxMonitorWidget"
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: OutboxMonitorProvider()) { entry in
            OutboxMonitorEntryView(entry: entry)
        }
        .configurationDisplayName("Sync Status")
        .description("Changes waiting to reach the server.")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}

// MARK: - Quick actions

// Static deep-link widget. `momentra://` opens the app; the per-tile routes below are
// placeholders until the app declares dedicated quick-add URLs.
struct QuickActionsEntry: TimelineEntry {
    let date: Date
}

struct QuickActionsProvider: TimelineProvider {
    func placeholder(in context: Context) -> QuickActionsEntry { QuickActionsEntry(date: Date()) }
    func getSnapshot(in context: Context, completion: @escaping (QuickActionsEntry) -> Void) {
        completion(QuickActionsEntry(date: Date()))
    }
    func getTimeline(in context: Context, completion: @escaping (Timeline<QuickActionsEntry>) -> Void) {
        completion(Timeline(entries: [QuickActionsEntry(date: Date())], policy: .never))
    }
}

struct QuickActionsEntryView: View {
    var entry: QuickActionsEntry
    private let actions: [(title: String, glyph: String, url: String)] = [
        ("Expense", "creditcard", "momentra://"),
        ("Sale", "bag", "momentra://"),
        ("Collection", "list.clipboard", "momentra://"),
        ("Note", "square.and.pencil", "momentra://"),
    ]
    var body: some View {
        VStack(spacing: 8) {
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 8) {
                ForEach(actions, id: \.title) { action in
                    Link(destination: URL(string: action.url) ?? URL(string: "momentra://")!) {
                        VStack(spacing: 4) {
                            Image(systemName: action.glyph)
                                .font(.system(size: 18, weight: .semibold))
                            Text(action.title).font(.caption2)
                        }
                        .frame(maxWidth: .infinity, minHeight: 44)
                        .background(Color.secondary.opacity(0.15))
                        .clipShape(RoundedRectangle(cornerRadius: 10))
                    }
                }
            }
            Text("Opens Momentra").font(.caption2).foregroundStyle(.secondary)
        }
        .padding()
    }
}

struct QuickActionsWidget: Widget {
    let kind: String = "QuickActionsWidget"
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: QuickActionsProvider()) { entry in
            QuickActionsEntryView(entry: entry)
        }
        .configurationDisplayName("Quick Actions")
        .description("Shortcuts back into the app.")
        .supportedFamilies([.systemMedium])
    }
}

// MARK: - Live Activity

@available(iOS 16.1, *)
struct OutboxFlushWidgetLiveActivity: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: OutboxFlushAttributes.self) { context in
            VStack(alignment: .leading, spacing: 6) {
                HStack {
                    Text("Syncing changes").font(.caption).bold()
                    Spacer()
                    Text("\(context.state.synced) / \(context.attributes.total)")
                        .font(.caption)
                        .monospacedDigit()
                        .foregroundStyle(.secondary)
                }
                Text(context.state.currentItemDescription)
                    .font(.caption2)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
                ProgressView(
                    value: Double(context.state.synced),
                    total: Double(max(context.attributes.total, 1))
                )
                .tint(.green)
            }
            .padding(12)
            .activityBackgroundTint(Color.black.opacity(0.85))
            .activitySystemActionForegroundColor(.white)
        } dynamicIsland: { context in
            DynamicIsland {
                DynamicIslandExpandedRegion(.leading) {
                    Text("\(context.state.synced)")
                        .font(.caption)
                        .monospacedDigit()
                }
                DynamicIslandExpandedRegion(.trailing) {
                    Text("\(context.state.remaining) left")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
                DynamicIslandExpandedRegion(.center) {
                    Text("Syncing").font(.caption).bold()
                }
                DynamicIslandExpandedRegion(.bottom) {
                    Text(context.state.currentItemDescription)
                        .font(.caption2)
                        .lineLimit(1)
                }
            } compactLeading: {
                Image(systemName: "arrow.triangle.2.circlepath")
            } compactTrailing: {
                Text("\(context.state.synced)")
                    .font(.caption2)
                    .monospacedDigit()
            } minimal: {
                Image(systemName: "arrow.triangle.2.circlepath")
            }
        }
    }
}
