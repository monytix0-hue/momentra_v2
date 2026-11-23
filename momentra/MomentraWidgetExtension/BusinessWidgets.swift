import WidgetKit
import SwiftUI

struct BusinessPulseEntry: TimelineEntry {
    let date: Date
    let runwayMonths: String?
    let financialHealthScore: String?
    let activeMomentCount: Int?
    let attentionCount: Int?
}

struct BusinessPulseProvider: TimelineProvider {
    func placeholder(in context: Context) -> BusinessPulseEntry {
        BusinessPulseEntry(date: Date(), runwayMonths: "6", financialHealthScore: "B+", activeMomentCount: 3, attentionCount: 1)
    }
    func getSnapshot(in context: Context, completion: @escaping (BusinessPulseEntry) -> Void) {
        completion(read())
    }
    func getTimeline(in context: Context, completion: @escaping (Timeline<BusinessPulseEntry>) -> Void) {
        completion(Timeline(entries: [read()], policy: .after(Date().addingTimeInterval(30 * 60))))
    }
    private func read() -> BusinessPulseEntry {
        let mid = UserDefaults(suiteName: "group.resolvingpoint.momentra")?.string(forKey: "widget_last_moment_id") ?? ""
        let snap = WidgetSnapshotManager.readBusinessPulse(momentId: mid)
        return BusinessPulseEntry(date: Date(), runwayMonths: snap?.runwayMonths, financialHealthScore: snap?.financialHealthScore, activeMomentCount: snap?.activeMomentCount, attentionCount: snap?.attentionCount)
    }
}

struct BusinessPulseEntryView: View {
    var entry: BusinessPulseEntry
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Business Pulse").font(.caption).foregroundStyle(.secondary)
            HStack {
                Text(entry.financialHealthScore ?? "--").font(.system(size: 28, weight: .heavy))
                VStack(alignment: .leading) {
                    Text("Health").font(.caption)
                    if let runway = entry.runwayMonths {
                        Text("\(runway) months runway").font(.caption2)
                    }
                }
            }
            if (entry.attentionCount ?? 0) > 0 {
                Label("\(entry.attentionCount ?? 0) need attention", systemImage: "exclamationmark.triangle.fill").font(.caption).foregroundStyle(.orange)
            }
        }.padding()
    }
}

struct BusinessPulseWidget: Widget {
    let kind: String = "BusinessPulseWidget"
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: BusinessPulseProvider()) { entry in
            BusinessPulseEntryView(entry: entry)
        }
        .configurationDisplayName("Business Pulse")
        .description("Runway, health score, and attention.")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}

struct BusinessFinanceEntry: TimelineEntry {
    let date: Date
    let expenseTotal: String?
    let revenueTotal: String?
    let outstandingTotal: String?
    let currencyCode: String
}

struct BusinessFinanceProvider: TimelineProvider {
    func placeholder(in context: Context) -> BusinessFinanceEntry {
        BusinessFinanceEntry(date: Date(), expenseTotal: "50,000", revenueTotal: "1,20,000", outstandingTotal: "15,000", currencyCode: "INR")
    }
    func getSnapshot(in context: Context, completion: @escaping (BusinessFinanceEntry) -> Void) {
        completion(read())
    }
    func getTimeline(in context: Context, completion: @escaping (Timeline<BusinessFinanceEntry>) -> Void) {
        completion(Timeline(entries: [read()], policy: .after(Date().addingTimeInterval(30 * 60))))
    }
    private func read() -> BusinessFinanceEntry {
        let mid = UserDefaults(suiteName: "group.resolvingpoint.momentra")?.string(forKey: "widget_last_moment_id") ?? ""
        let snap = WidgetSnapshotManager.readBusinessFinance(momentId: mid)
        return BusinessFinanceEntry(date: Date(), expenseTotal: snap?.expenseTotal, revenueTotal: snap?.revenueTotal, outstandingTotal: snap?.invoiceOutstandingTotal, currencyCode: snap?.currencyCode ?? "INR")
    }
}

struct BusinessFinanceEntryView: View {
    var entry: BusinessFinanceEntry
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Business Finance").font(.caption).foregroundStyle(.secondary)
            HStack {
                Text(entry.expenseTotal ?? "--").font(.system(size: 24, weight: .heavy))
                Text(entry.currencyCode).font(.caption)
            }
            HStack(spacing: 12) {
                Label(entry.revenueTotal ?? "--", systemImage: "arrow.up").font(.caption).foregroundStyle(.green)
                Label(entry.outstandingTotal ?? "--", systemImage: "exclamationmark").font(.caption).foregroundStyle(.orange)
            }
        }.padding()
    }
}

struct BusinessFinanceWidget: Widget {
    let kind: String = "BusinessFinanceWidget"
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: BusinessFinanceProvider()) { entry in
            BusinessFinanceEntryView(entry: entry)
        }
        .configurationDisplayName("Business Finance")
        .description("Expenses, revenue, and outstanding.")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}

struct BusinessLifeEntry: TimelineEntry {
    let date: Date
    let activeModuleCount: Int?
    let activeMomentCount: Int?
    let runwayMonths: String?
    let attentionCount: Int?
}

struct BusinessLifeProvider: TimelineProvider {
    func placeholder(in context: Context) -> BusinessLifeEntry {
        BusinessLifeEntry(date: Date(), activeModuleCount: 4, activeMomentCount: 3, runwayMonths: "6", attentionCount: 1)
    }
    func getSnapshot(in context: Context, completion: @escaping (BusinessLifeEntry) -> Void) {
        completion(read())
    }
    func getTimeline(in context: Context, completion: @escaping (Timeline<BusinessLifeEntry>) -> Void) {
        completion(Timeline(entries: [read()], policy: .after(Date().addingTimeInterval(30 * 60))))
    }
    private func read() -> BusinessLifeEntry {
        let mid = UserDefaults(suiteName: "group.resolvingpoint.momentra")?.string(forKey: "widget_last_moment_id") ?? ""
        let snap = WidgetSnapshotManager.readBusinessLife(momentId: mid)
        return BusinessLifeEntry(date: Date(), activeModuleCount: snap?.activeModuleCount, activeMomentCount: snap?.activeMomentCount, runwayMonths: snap?.runwayMonths, attentionCount: snap?.attentionCount)
    }
}

struct BusinessLifeEntryView: View {
    var entry: BusinessLifeEntry
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Business Life").font(.caption).foregroundStyle(.secondary)
            HStack(spacing: 12) {
                Label("\(entry.activeModuleCount ?? 0)", systemImage: "square.grid.2x2").font(.caption)
                Label("\(entry.activeMomentCount ?? 0)", systemImage: "briefcase").font(.caption)
            }
            if let runway = entry.runwayMonths {
                Text("\(runway) months runway").font(.caption2)
            }
        }.padding()
    }
}

struct BusinessLifeWidget: Widget {
    let kind: String = "BusinessLifeWidget"
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: BusinessLifeProvider()) { entry in
            BusinessLifeEntryView(entry: entry)
        }
        .configurationDisplayName("Business Life")
        .description("Active modules and runway.")
        .supportedFamilies([.systemSmall])
    }
}

struct BusinessTimelineEntry: TimelineEntry {
    let date: Date
    let items: [BusinessTimelineWidgetSnapshot.TimelineRow]
    let kpis: BusinessTimelineWidgetSnapshot.TimelineKpis
}

struct BusinessTimelineProvider: TimelineProvider {
    func placeholder(in context: Context) -> BusinessTimelineEntry {
        BusinessTimelineEntry(date: Date(), items: [], kpis: .init(spendEvents: 0, issueCount: 0, openIssueCount: 0, updateCount: 0))
    }
    func getSnapshot(in context: Context, completion: @escaping (BusinessTimelineEntry) -> Void) {
        completion(read())
    }
    func getTimeline(in context: Context, completion: @escaping (Timeline<BusinessTimelineEntry>) -> Void) {
        completion(Timeline(entries: [read()], policy: .after(Date().addingTimeInterval(30 * 60))))
    }
    private func read() -> BusinessTimelineEntry {
        let mid = UserDefaults(suiteName: "group.resolvingpoint.momentra")?.string(forKey: "widget_last_moment_id") ?? ""
        let snap = WidgetSnapshotManager.readBusinessTimeline(momentId: mid)
        return BusinessTimelineEntry(date: Date(), items: snap?.items.prefix(3).map { $0 } ?? [], kpis: snap?.kpis ?? .init(spendEvents: 0, issueCount: 0, openIssueCount: 0, updateCount: 0))
    }
}

struct BusinessTimelineEntryView: View {
    var entry: BusinessTimelineEntry
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Timeline").font(.caption).foregroundStyle(.secondary)
            HStack(spacing: 12) {
                Label("\(entry.kpis.issueCount ?? 0)", systemImage: "exclamationmark.triangle").font(.caption)
                Label("\(entry.kpis.updateCount ?? 0)", systemImage: "bubble.left").font(.caption)
            }
            ForEach(entry.items.prefix(2), id: \.title) { item in
                Text("• \(item.title)").font(.caption).lineLimit(1)
            }
        }.padding()
    }
}

struct BusinessTimelineWidget: Widget {
    let kind: String = "BusinessTimelineWidget"
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: BusinessTimelineProvider()) { entry in
            BusinessTimelineEntryView(entry: entry)
        }
        .configurationDisplayName("Business Timeline")
        .description("Recent events and KPIs.")
        .supportedFamilies([.systemMedium])
    }
}

struct BusinessWeeklyEntry: TimelineEntry {
    let date: Date
    let title: String?
    let period: String?
    let sections: [BusinessWeeklyWidgetSnapshot.Section]
}

struct BusinessWeeklyProvider: TimelineProvider {
    func placeholder(in context: Context) -> BusinessWeeklyEntry {
        BusinessWeeklyEntry(date: Date(), title: "Weekly Report", period: "7d", sections: [])
    }
    func getSnapshot(in context: Context, completion: @escaping (BusinessWeeklyEntry) -> Void) {
        completion(read())
    }
    func getTimeline(in context: Context, completion: @escaping (Timeline<BusinessWeeklyEntry>) -> Void) {
        completion(Timeline(entries: [read()], policy: .after(Date().addingTimeInterval(60 * 60))))
    }
    private func read() -> BusinessWeeklyEntry {
        let mid = UserDefaults(suiteName: "group.resolvingpoint.momentra")?.string(forKey: "widget_last_moment_id") ?? ""
        let snap = WidgetSnapshotManager.readBusinessWeekly(momentId: mid, period: "7d")
        return BusinessWeeklyEntry(date: Date(), title: snap?.title, period: snap?.period, sections: snap?.sections ?? [])
    }
}

struct BusinessWeeklyEntryView: View {
    var entry: BusinessWeeklyEntry
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(entry.title ?? "Weekly Report").font(.caption).foregroundStyle(.secondary)
            ForEach(entry.sections.prefix(2), id: \.heading) { section in
                VStack(alignment: .leading, spacing: 2) {
                    Text(section.heading ?? "").font(.caption).bold()
                    ForEach(section.items?.prefix(2) ?? [], id: \.self) { item in
                        Text("• \(item)").font(.caption2).lineLimit(1)
                    }
                }
            }
        }.padding()
    }
}

struct BusinessWeeklyWidget: Widget {
    let kind: String = "BusinessWeeklyWidget"
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: BusinessWeeklyProvider()) { entry in
            BusinessWeeklyEntryView(entry: entry)
        }
        .configurationDisplayName("Weekly Report")
        .description("Latest generated weekly report sections.")
        .supportedFamilies([.systemMedium])
    }
}

struct BusinessRosterEntry: TimelineEntry {
    let date: Date
    let members: [BusinessRosterWidgetSnapshot.RosterMember]
}

struct BusinessRosterProvider: TimelineProvider {
    func placeholder(in context: Context) -> BusinessRosterEntry {
        BusinessRosterEntry(date: Date(), members: [])
    }
    func getSnapshot(in context: Context, completion: @escaping (BusinessRosterEntry) -> Void) {
        completion(read())
    }
    func getTimeline(in context: Context, completion: @escaping (Timeline<BusinessRosterEntry>) -> Void) {
        completion(Timeline(entries: [read()], policy: .after(Date().addingTimeInterval(30 * 60))))
    }
    private func read() -> BusinessRosterEntry {
        let mid = UserDefaults(suiteName: "group.resolvingpoint.momentra")?.string(forKey: "widget_last_moment_id") ?? ""
        let snap = WidgetSnapshotManager.readBusinessRoster(momentId: mid)
        return BusinessRosterEntry(date: Date(), members: snap?.members ?? [])
    }
}

struct BusinessRosterEntryView: View {
    var entry: BusinessRosterEntry
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Team").font(.caption).foregroundStyle(.secondary)
            ForEach(entry.members.prefix(5), id: \.displayName) { m in
                Text(m.displayName ?? "Member").font(.caption).lineLimit(1)
            }
        }.padding()
    }
}

struct BusinessRosterWidget: Widget {
    let kind: String = "BusinessRosterWidget"
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: BusinessRosterProvider()) { entry in
            BusinessRosterEntryView(entry: entry)
        }
        .configurationDisplayName("Business Team")
        .description("Company roster for the active moment.")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}

struct KhataEntry: TimelineEntry {
    let date: Date
    let parties: [KhataWidgetSnapshot.Party]
}

struct KhataProvider: TimelineProvider {
    func placeholder(in context: Context) -> KhataEntry {
        KhataEntry(date: Date(), parties: [])
    }
    func getSnapshot(in context: Context, completion: @escaping (KhataEntry) -> Void) {
        completion(read())
    }
    func getTimeline(in context: Context, completion: @escaping (Timeline<KhataEntry>) -> Void) {
        completion(Timeline(entries: [read()], policy: .after(Date().addingTimeInterval(30 * 60))))
    }
    private func read() -> KhataEntry {
        let cid = UserDefaults(suiteName: "group.resolvingpoint.momentra")?.string(forKey: "widget_last_company_id") ?? ""
        let snap = WidgetSnapshotManager.readKhata(companyId: cid)
        return KhataEntry(date: Date(), parties: snap?.parties ?? [])
    }
}

struct KhataEntryView: View {
    var entry: KhataEntry
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Khata").font(.caption).foregroundStyle(.secondary)
            ForEach(entry.parties.prefix(3), id: \.name) { party in
                HStack {
                    Text(party.name).font(.caption).lineLimit(1)
                    Spacer()
                    Text(party.balanceDue).font(.caption).foregroundStyle(party.overdue ? .red : .primary)
                }
            }
        }.padding()
    }
}

struct KhataWidget: Widget {
    let kind: String = "KhataWidget"
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: KhataProvider()) { entry in
            KhataEntryView(entry: entry)
        }
        .configurationDisplayName("Khata Balances")
        .description("Party balances due.")
        .supportedFamilies([.systemMedium])
    }
}
