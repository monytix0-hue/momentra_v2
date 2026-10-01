import SwiftUI

/// Company Life. The payload is the company. The selected moment only sets the lens.
struct BusinessLifeActiveView: View {
    let refreshToken: UInt64
    let momentId: String?
    let momentTitle: String?
    var momentTypeCode: String? = nil
    var companyId: String? = nil
    var onViewReport: () -> Void = {}
    var onOpenFinance: () -> Void = {}
    var onOpenVendor: () -> Void = {}

    @State private var life: APIClient.BusinessLifePayload?
    @State private var shownCompany: String?
    @State private var loading = true
    @State private var error: String?
    @State private var lens: BusinessLifeLens = .overview
    @State private var movement: BusinessLifeMovementTab = .activity
    @State private var report: APIClient.BusinessWeeklyReportPayload?
    @State private var showReport = false
    @State private var actionMessage: String?
    @State private var shareBusy = false
    @State private var retry = 0

    private var companyState: BusinessLifeCompanyState {
        buildBusinessLifeCompanyState(
            facts: businessLifeFacts(from: life?.payload),
            now: Date(),
            timeZone: .current
        )
    }

    var body: some View {
        Group {
            if loading && life == nil {
                ProgressView().tint(CompanyLifeColors.indigo)
            } else {
                NativeDashboardScaffold(background: CompanyLifeColors.bg) {
                    NativeListSection {
                        VStack(alignment: .leading, spacing: 16) {
                            Text("Company life")
                                .font(.plusJakarta(size: 22, weight: .semibold))
                                .foregroundStyle(CompanyLifeColors.text)
                            lensChips
                            if let error {
                                Text(error)
                                    .font(.caption)
                                    .foregroundStyle(CompanyLifeColors.red)
                                if life == nil {
                                    Button("Try again") { retry += 1 }
                                        .font(.caption)
                                }
                            }
                            if let actionMessage {
                                Text(actionMessage)
                                    .font(.caption)
                                    .foregroundStyle(CompanyLifeColors.indigo)
                            }
                            if life != nil {
                                companyBlocks
                            }
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
        .onChange(of: companyId) { _, next in
            if let shownCompany, let next, next != shownCompany {
                life = nil
                self.shownCompany = next
            }
        }
        .onChange(of: momentTypeCode) { _, code in
            let next = businessLifeLens(momentTypeCode: code)
            lens = next
            movement = movementTab(for: next)
        }
        .onAppear {
            let next = businessLifeLens(momentTypeCode: momentTypeCode)
            lens = next
            movement = movementTab(for: next)
        }
        .task(id: "\(refreshToken)-\(momentId ?? "")-\(companyId ?? "")-\(retry)") { await load() }
    }

    private var lensChips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(BusinessLifeLens.allCases, id: \.self) { item in
                    Button {
                        lens = item
                        movement = movementTab(for: item)
                    } label: {
                        Text(item.label)
                            .font(.plusJakarta(size: 13, weight: .semibold))
                            .foregroundStyle(CompanyLifeColors.text)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 8)
                            .background(item == lens ? CompanyLifeColors.indigoSolid : CompanyLifeColors.card)
                            .clipShape(Capsule())
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private var companyBlocks: some View {
        let state = companyState
        return VStack(alignment: .leading, spacing: 22) {
            block("Company overview") {
                ForEach(Array(state.overview.enumerated()), id: \.offset) { _, line in
                    pair(line.title, line.state)
                }
            }
            block("This week") {
                Text(state.thisWeek)
                    .font(.plusJakarta(size: 14))
                    .foregroundStyle(CompanyLifeColors.text)
            }
            block("Where the business moved") {
                HStack(spacing: 8) {
                    movementChip("Activity", selected: movement == .activity) { movement = .activity }
                    movementChip("Money", selected: movement == .money) { movement = .money }
                }
                if movement == .activity {
                    if state.activity.isEmpty {
                        muted("Nothing recorded yet.")
                    } else {
                        ForEach(Array(state.activity.enumerated()), id: \.offset) { _, item in
                            Text(item.title.isEmpty ? "Activity" : item.title)
                                .font(.plusJakarta(size: 14))
                                .foregroundStyle(CompanyLifeColors.text)
                        }
                    }
                } else if state.moneyMovement.isEmpty {
                    muted("No money figures recorded.")
                } else {
                    ForEach(Array(state.moneyMovement.enumerated()), id: \.offset) { _, line in
                        pair(line.label, line.value)
                    }
                }
            }
            block("Needs attention") {
                if state.needsAttention.isEmpty {
                    muted("Nothing needs attention.")
                } else {
                    ForEach(Array(state.needsAttention.enumerated()), id: \.offset) { _, title in
                        Text(title).font(.plusJakarta(size: 14)).foregroundStyle(CompanyLifeColors.text)
                    }
                }
            }
            block("What's working") {
                if state.working.isEmpty {
                    muted("Nothing marked as working.")
                } else {
                    ForEach(Array(state.working.enumerated()), id: \.offset) { _, title in
                        Text(title).font(.plusJakarta(size: 14)).foregroundStyle(CompanyLifeColors.text)
                    }
                }
            }
            block("Financial position") {
                Text(businessLifeFinanceScope)
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(CompanyLifeColors.secondary)
                if state.financialPosition.isEmpty {
                    muted("No company totals yet.")
                } else {
                    ForEach(Array(state.financialPosition.enumerated()), id: \.offset) { _, line in
                        pair(line.label, line.value)
                    }
                }
            }
        }
    }

    private func block<Content: View>(_ title: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(title)
                .font(.plusJakarta(size: 16, weight: .semibold))
                .foregroundStyle(CompanyLifeColors.text)
            content()
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(CompanyLifeColors.card)
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func pair(_ label: String, _ value: String) -> some View {
        HStack {
            Text(label)
                .font(.plusJakarta(size: 14))
                .foregroundStyle(CompanyLifeColors.secondary)
            Spacer()
            Text(value)
                .font(.plusJakarta(size: 14, weight: .medium))
                .foregroundStyle(CompanyLifeColors.text)
        }
    }

    private func movementChip(_ label: String, selected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(label)
                .font(.plusJakarta(size: 12))
                .foregroundStyle(CompanyLifeColors.text)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(selected ? CompanyLifeColors.indigo : CompanyLifeColors.bg)
                .clipShape(Capsule())
        }
        .buttonStyle(.plain)
    }

    private func muted(_ text: String) -> some View {
        Text(text)
            .font(.plusJakarta(size: 14))
            .foregroundStyle(CompanyLifeColors.secondary)
    }

    private func load() async {
        guard let momentId else {
            loading = false
            error = "Select a Business Moment."
            return
        }
        error = nil
        if life == nil, let cached = BusinessTabDataCache.peekPulse(momentId)?.life {
            life = cached
        }
        loading = life == nil
        do {
            let incoming = try await APIClient.shared.getBusinessLife(momentId: momentId)
            if let companyId, let incomingCompany = incoming.companyId, incomingCompany != companyId {
                loading = false
                return
            }
            if companyId == nil, let shownCompany, let incomingCompany = incoming.companyId, incomingCompany != shownCompany {
                loading = false
                return
            }
            life = incoming
            shownCompany = incoming.companyId ?? companyId
            BusinessTabDataCache.putLife(momentId, incoming)
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
