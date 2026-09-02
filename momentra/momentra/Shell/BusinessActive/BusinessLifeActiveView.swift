import SwiftUI

/// Figma `695:9782` company-unified Business Life — live bind; honest empties.
struct BusinessLifeActiveView: View {
    let refreshToken: UInt64
    let momentId: String?
    let momentTitle: String?
    var momentTypeCode: String? = nil
    var onViewReport: () -> Void = {}

    @State private var life: APIClient.BusinessLifePayload?
    @State private var loading = true
    @State private var error: String?
    @State private var filter: CompanyLifeFilter = .all
    @State private var report: APIClient.BusinessWeeklyReportPayload?
    @State private var showReport = false
    @State private var actionMessage: String?
    @State private var shareBusy = false

    private var inner: APIClient.BusinessLifePayload.LifeInner? { life?.payload }
    private var kpis: APIClient.BusinessLifePayload.LifeInner.LifeKpis? { inner?.kpis }

    private var score: String {
        guard let raw = kpis?.financialHealthScore?.trimmingCharacters(in: .whitespacesAndNewlines),
              !raw.isEmpty else { return "—" }
        if let n = Double(raw) { return "\(Int(n))" }
        return raw.allSatisfy(\.isNumber) ? raw : "—"
    }

    private var hasLiveScore: Bool { score != "—" }
    private var attention: Int { kpis?.attentionCount ?? 0 }

    private var narrative: String {
        if !hasLiveScore {
            return attention > 0 ? "Needs attention" : "Awaiting live company signal"
        }
        let n = Int(score) ?? 0
        if n >= 80 && attention == 0 { return "Healthy" }
        if n >= 60 { return "Watch" }
        return "Needs focus"
    }

    private var subtitle: String {
        if !hasLiveScore {
            return "Activate modules and log activity to surface company health."
        }
        if attention > 0 {
            return "\(attention) open signal\(attention == 1 ? "" : "s") need review across modules."
        }
        return "Modules with live data are within available thresholds. No critical alerts invented."
    }

    private var signals: [APIClient.BusinessLifePayload.LifeInner.LifeSignal] {
        let all = inner?.signals ?? []
        guard let key = filter.familyKey else { return all }
        return all.filter { ($0.family ?? "").uppercased() == key }
    }

    private var activity: [APIClient.BusinessLifePayload.LifeInner.LifeActivity] {
        let all = inner?.activity ?? []
        guard let key = filter.familyKey else { return all }
        return all.filter { ($0.family ?? "").uppercased() == key }
    }

    private var journey: [APIClient.BusinessLifePayload.LifeInner.LifeJourney] {
        let all = inner?.journey ?? []
        guard let key = filter.familyKey else { return all }
        let needle: String = {
            switch key {
            case "TEAM_OPS": return "TEAM"
            case "RUNWAY": return "RUNWAY"
            default: return "OPERATIONS"
            }
        }()
        return all.filter {
            ($0.family ?? "").uppercased() == key || $0.familyCode.uppercased().contains(needle)
        }
    }

    var body: some View {
        Group {
            if loading && life == nil {
                ProgressView().tint(CompanyLifeColors.indigo)
            } else {
                NativeDashboardScaffold(background: CompanyLifeColors.bg) {
                    NativeListSection {
                        VStack(alignment: .leading, spacing: 24) {
                            if let error {
                                Text(error)
                                    .font(.caption)
                                    .foregroundStyle(CompanyLifeColors.red)
                            }
                            if let actionMessage {
                                Text(actionMessage)
                                    .font(.caption)
                                    .foregroundStyle(CompanyLifeColors.indigo)
                            }
                            if let momentTitle, !momentTitle.isEmpty {
                                Text(momentTitle)
                                    .font(.plusJakarta(size: 12, weight: .semibold))
                                    .foregroundStyle(CompanyLifeColors.secondary)
                            }

                            CompanyLifeFilterChips(selected: $filter)

                            CompanyLifeHealthHeader(
                                score: score,
                                narrative: narrative,
                                subtitle: subtitle,
                                activeModules: "\(kpis?.activeModuleCount ?? 0) MODULE\((kpis?.activeModuleCount ?? 0) == 1 ? "" : "S")",
                                totalMoments: "\(kpis?.activeMomentCount ?? 0) MOMENT\((kpis?.activeMomentCount ?? 0) == 1 ? "" : "S")",
                                avgRunway: {
                                    if let m = kpis?.runwayMonths?.trimmingCharacters(in: .whitespacesAndNewlines),
                                       !m.isEmpty {
                                        return "\(m) MONTHS"
                                    }
                                    return "—"
                                }()
                            )

                            CompanyLifeModuleCards(
                                team: inner?.modules?.teamOperations,
                                runway: inner?.modules?.runway,
                                ops: inner?.modules?.businessOperations,
                                vendor: inner?.modules?.vendorOperations
                            )

                            CompanyLifeSignalsSection(signals: signals)
                            CompanyLifeActivitySection(items: activity)
                            CompanyLifeJourneySection(steps: journey)
                            CompanyLifeTrendsSection(trends: inner?.trends)
                        }
                    }
                }
                .nativeStickyFooter(background: CompanyLifeColors.bg) {
                    VStack(spacing: 12) {
                        CompanyLifeGradientButton(
                            label: "View Detailed Report",
                            enabled: momentId != nil && !(momentId?.isEmpty ?? true),
                            action: loadReport
                        )
                        CompanyLifeOutlineButton(
                            label: shareBusy ? "Sharing…" : "Share with Team",
                            enabled: momentId != nil && !(momentId?.isEmpty ?? true) && !shareBusy,
                            action: shareDashboard
                        )
                    }
                }
            }
        }
        .background(CompanyLifeColors.bg)
        .sheet(isPresented: $showReport) {
            NavigationStack {
                List {
                    if let sections = report?.sections, !sections.isEmpty {
                        ForEach(sections) { section in
                            Section(section.heading ?? "Section") {
                                ForEach(section.items ?? [], id: \.self) { item in
                                    Text(item)
                                }
                            }
                        }
                    } else {
                        Text(report?.note ?? "No activity in this period.")
                    }
                }
                .navigationTitle(report?.title ?? "Weekly Report")
                .toolbar {
                    ToolbarItem(placement: .confirmationAction) {
                        Button("Done") { showReport = false }
                    }
                }
            }
        }
        .task(id: "\(refreshToken)-\(momentId ?? "")") { await load() }
    }

    private func load() async {
        guard let momentId else {
            loading = false
            error = "Select a Business Moment."
            return
        }
        error = nil
        if let cached = BusinessTabDataCache.peekPulse(momentId)?.life {
            life = cached
            loading = false
        } else {
            loading = life == nil
        }
        do {
            life = try await APIClient.shared.getBusinessLife(momentId: momentId)
        } catch {
            self.error = error.localizedDescription
        }
        loading = false
    }

    private func loadReport() {
        guard let momentId else { return }
        Task {
            do {
                report = try await APIClient.shared.getBusinessWeeklyReport(momentId: momentId)
                showReport = true
            } catch {
                actionMessage = error.localizedDescription
                onViewReport()
            }
        }
    }

    private func shareDashboard() {
        guard let momentId else { return }
        shareBusy = true
        Task {
            defer { shareBusy = false }
            do {
                let link = try await APIClient.shared.createBusinessShareLink(momentId: momentId)
                if let url = link.shareUrl, !url.isEmpty {
                    #if canImport(UIKit)
                    await MainActor.run {
                        let activity = UIActivityViewController(activityItems: [url], applicationActivities: nil)
                        UIApplication.shared.firstKeyWindow?.rootViewController?.present(activity, animated: true)
                    }
                    #endif
                }
                actionMessage = link.note ?? "Share link created"
            } catch {
                actionMessage = error.localizedDescription
            }
        }
    }
}

#if canImport(UIKit)
private extension UIApplication {
    var firstKeyWindow: UIWindow? {
        connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first { $0.isKeyWindow }
    }
}
#endif
