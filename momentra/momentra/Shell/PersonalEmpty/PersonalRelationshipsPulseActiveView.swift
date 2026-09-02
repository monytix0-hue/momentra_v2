import SwiftUI

/// Figma `505:11793` — Relationships populated Pulse.
/// S2 G2: no invented bond axes / %; awaiting/unavailable when API fields missing; real activity only.
struct PersonalRelationshipsPulseActiveView: View {
    let refreshToken: UInt64
    let momentTitle: String?
    let momentId: String?
    var onAddExpense: () -> Void = {}
    var onRelationshipsQuickAdd: (RelationshipsQuickAddKind) -> Void = { _ in }
    var onOpenRecentActivity: () -> Void = {}

    @State private var loading = true
    @State private var error: String?
    @State private var pulse: APIClient.PersonalPulsePayload?
    @State private var bondScore: Int?
    @State private var bondAxes: PersonalRelationshipsDerived.BondAxes = .init(trust: "—", care: "—", support: "—", presence: "—")
    @State private var activities: [RelationshipsActivityItem] = []
    @State private var hasSpend = false

    private let bg = Color(hex: "#14121B")
    private let card = Color(hex: "#1C1B2E")
    private let pink = Color(hex: "#E12A9E")
    private let text = Color(hex: "#E5E0EE")
    private let muted = Color(hex: "#C9C4D8")
    private let dim = Color(hex: "#8C8C9E")

    private var axisEntries: [(String, String)] {
        [("Trust", bondAxes.trust), ("Support", bondAxes.support), ("Presence", bondAxes.presence), ("Care", bondAxes.care)]
    }

    var body: some View {
        Group {
            if loading && activities.isEmpty && bondScore == nil {
                ProgressView().tint(pink).frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                NativeDashboardScaffold(background: bg) {

                    NativeListSection {

                    VStack(spacing: 12) {
                        if let error { Text(error).font(.system(size: 12)).foregroundStyle(.red) }
                        if let momentTitle, !momentTitle.isEmpty {
                            Text(momentTitle).font(.system(size: 11, weight: .semibold)).foregroundStyle(muted)
                                .frame(maxWidth: .infinity, alignment: .leading)
                        }
                        bondHero
                        capacityBars
                        lifeSignals
                        driversAndState
                        recentPreview
                        sharedSpend
                        trends
                        protectCard
                        statusPills
                        intelligence
                        aiSoon
                        launcher
                        Spacer().frame(height: 24)
                    }
                

                    }

                }
                .background(bg)
            }
        }
        .task(id: "\(refreshToken)-\(momentId ?? "")") { await load() }
    }

    private func load() async {
        error = nil
        if let cached = PersonalTabDataCache.peekPulse(momentId: momentId) {
            applyPulseTab(cached.pulse, cached.activities)
            loading = false
        } else {
            loading = true
        }
        do {
            let data = try await PersonalTabLoad.loadPulseTab(momentId: momentId)
            applyPulseTab(data.pulse, data.activities)
        } catch {
            self.error = error.localizedDescription
            activities = []
            bondScore = nil
            hasSpend = false
        }
        loading = false
    }

    private func applyPulseTab(_ pulse: APIClient.PersonalPulsePayload, _ acts: [APIClient.ActivityItemPayload]) {
        self.pulse = pulse
        bondScore = PersonalRelationshipsDerived.bondIndex(pulse: pulse)
        bondAxes = PersonalRelationshipsDerived.bondAxes(pulse: pulse)
        if let map = pulse.widgetPayload?["spendByCurrency"]?.value as? [String: Any] {
            hasSpend = !map.isEmpty
        } else {
            hasSpend = false
        }
        activities = RelationshipsActivityModels.from(api: acts)
    }

    private var bondHero: some View {
        VStack(spacing: 16) {
            Text("BOND INDEX").font(.system(size: 11, weight: .bold)).foregroundStyle(dim)
            HStack(alignment: .bottom, spacing: 2) {
                Text(bondScore.map(String.init) ?? "—")
                    .font(.system(size: 40, weight: .bold)).foregroundStyle(text)
                Text("/100").font(.system(size: 16)).foregroundStyle(muted).padding(.bottom, 8)
            }
            Text(PersonalRelationshipsDerived.bondSubtitle(pulse: pulse))
                .font(.system(size: 13))
                .foregroundStyle(Color(hex: "#F472B6"))
            ZStack {
                Circle()
                    .stroke(pink.opacity(0.2), lineWidth: 14)
                    .frame(width: 140, height: 140)
                if let bondScore {
                    Circle()
                        .trim(from: 0, to: CGFloat(bondScore) / 100)
                        .stroke(pink, style: StrokeStyle(lineWidth: 14, lineCap: .round))
                        .rotationEffect(.degrees(-90))
                        .frame(width: 140, height: 140)
                        .shadow(color: pink.opacity(0.45), radius: 12)
                    Text("\(bondScore)").font(.system(size: 28, weight: .bold)).foregroundStyle(text)
                } else {
                    Text("—").font(.system(size: 28, weight: .bold)).foregroundStyle(muted)
                }
            }
            HStack {
                ForEach(axisEntries, id: \.0) { label, value in
                    VStack(spacing: 2) {
                        Text(label).font(.system(size: 10)).foregroundStyle(dim)
                        Text(value)
                            .font(.system(size: 14, weight: .bold))
                            .foregroundStyle(text)
                            .minimumScaleFactor(0.7)
                            .lineLimit(1)
                    }
                    .frame(maxWidth: .infinity)
                }
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity)
        .background(
            LinearGradient(colors: [Color(hex: "#2A1530"), card], startPoint: .top, endPoint: .bottom)
        )
        .overlay(RoundedRectangle(cornerRadius: 24).stroke(pink.opacity(0.25)))
        .clipShape(RoundedRectangle(cornerRadius: 24))
    }

    private var capacityBars: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("CAPACITY & SUPPORT").font(.system(size: 11, weight: .bold)).foregroundStyle(dim)
            Text("Unavailable — capacity metrics are not projected yet.")
                .font(.system(size: 12))
                .foregroundStyle(muted)
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(hex: "#161B26"))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.08)))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var lifeSignals: some View {
        HStack(spacing: 8) {
            signal("Trust +", pink) { onRelationshipsQuickAdd(.connection) }
            signal("Care →", Color(hex: "#3B82F6")) { onRelationshipsQuickAdd(.support) }
            Text("₹")
                .font(.system(size: 16, weight: .bold))
                .foregroundStyle(Color(hex: "#7C5CFC"))
                .frame(width: 44, height: 44)
                .background(Color(hex: "#7C5CFC").opacity(0.25))
                .overlay(Circle().stroke(Color(hex: "#7C5CFC").opacity(0.5)))
                .clipShape(Circle())
                .onTapGesture(perform: onAddExpense)
        }
    }

    private func signal(_ label: String, _ tint: Color, action: @escaping () -> Void) -> some View {
        Text(label)
            .font(.system(size: 14, weight: .semibold))
            .foregroundStyle(text)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(tint.opacity(0.18))
            .overlay(RoundedRectangle(cornerRadius: 16).stroke(tint.opacity(0.4)))
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .onTapGesture(perform: action)
    }

    private var driversAndState: some View {
        HStack(alignment: .top, spacing: 10) {
            VStack(alignment: .leading, spacing: 8) {
                Text("SCORE DRIVERS").font(.system(size: 10, weight: .bold)).foregroundStyle(dim)
                Text("Unavailable")
                    .font(.system(size: 12))
                    .foregroundStyle(muted)
                Text("Driver deltas need projected bond axes.")
                    .font(.system(size: 11))
                    .foregroundStyle(dim)
            }
            .padding(12)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(card)
            .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.08)))
            .clipShape(RoundedRectangle(cornerRadius: 16))

            VStack(alignment: .leading, spacing: 8) {
                Text("CURRENT STATE").font(.system(size: 10, weight: .bold)).foregroundStyle(dim)
                Text("Awaiting")
                    .font(.system(size: 12))
                    .foregroundStyle(muted)
                Text("Stress / capacity rings arrive with API fields.")
                    .font(.system(size: 11))
                    .foregroundStyle(dim)
            }
            .padding(12)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(card)
            .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.08)))
            .clipShape(RoundedRectangle(cornerRadius: 16))
        }
    }

    private var recentPreview: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text("RECENT ACTIVITY").font(.system(size: 11, weight: .bold)).foregroundStyle(dim)
                Spacer()
                Text("View All").font(.system(size: 12, weight: .bold)).foregroundStyle(pink)
                    .onTapGesture(perform: onOpenRecentActivity)
            }
            Text("Latest logs across your personal moments.").font(.system(size: 11)).foregroundStyle(muted)
            if activities.isEmpty {
                Text("No relationship activity yet. Log a connection to start this feed.")
                    .font(.system(size: 12))
                    .foregroundStyle(muted)
                    .padding(.vertical, 8)
            } else {
                ForEach(activities.prefix(3)) { item in
                    HStack(spacing: 10) {
                        Text(item.emoji).frame(width: 32, height: 32).background(pink).clipShape(RoundedRectangle(cornerRadius: 16))
                        VStack(alignment: .leading, spacing: 2) {
                            Text(item.title).font(.system(size: 13, weight: .semibold)).foregroundStyle(text)
                            Text(item.whenLabel).font(.system(size: 11)).foregroundStyle(dim)
                        }
                        Spacer()
                        Text("Logged")
                            .font(.system(size: 11, weight: .heavy))
                            .foregroundStyle(bg)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 4)
                            .background(pink)
                            .clipShape(Capsule())
                    }
                    .contentShape(Rectangle())
                    .onTapGesture(perform: onOpenRecentActivity)
                }
            }
        }
        .padding(14)
        .background(card)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.08)))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var sharedSpend: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("SHARED SPEND").font(.system(size: 11, weight: .bold)).foregroundStyle(dim)
            if hasSpend {
                Text("Spend is present on this moment. Category mix % is unavailable until projected.")
                    .font(.system(size: 12))
                    .foregroundStyle(muted)
            } else {
                Text("Unavailable — no shared spend projected for this moment yet.")
                    .font(.system(size: 12))
                    .foregroundStyle(muted)
            }
            HStack {
                Text("ACCOUNTS").font(.system(size: 11, weight: .bold)).foregroundStyle(dim)
                Spacer()
                Text("+ Add Account").font(.system(size: 11, weight: .semibold)).foregroundStyle(pink)
                    .onTapGesture(perform: onAddExpense)
            }
            Text("Account bond scores unavailable.")
                .font(.system(size: 12))
                .foregroundStyle(muted)
        }
        .padding(14)
        .background(card)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.08)))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var trends: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("CURRENT TRENDS (30 DAYS)").font(.system(size: 11, weight: .bold)).foregroundStyle(dim)
            Text("Unavailable — trend % is not projected yet.")
                .font(.system(size: 12))
                .foregroundStyle(muted)
            RoundedRectangle(cornerRadius: 8)
                .fill(Color.white.opacity(0.04))
                .frame(height: 80)
                .overlay(
                    Text("Awaiting data")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(dim)
                )
        }
        .padding(14)
        .background(card)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.08)))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var protectCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Protect Connection 💖").font(.system(size: 16, weight: .bold)).foregroundStyle(text)
            Text("Add one recovery block before your next high-pressure engagement.")
                .font(.system(size: 13)).foregroundStyle(muted)
            Text("Log Connection")
                .font(.system(size: 15, weight: .bold))
                .foregroundStyle(bg)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .background(pink)
                .clipShape(Capsule())
                .shadow(color: pink.opacity(0.4), radius: 10)
                .onTapGesture { onRelationshipsQuickAdd(.connection) }
        }
        .padding(16)
        .background(Color(hex: "#2A1524"))
        .overlay(RoundedRectangle(cornerRadius: 20).stroke(pink.opacity(0.45), lineWidth: 1.5))
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }

    private var statusPills: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(["Trust · Awaiting", "Care · Awaiting", "Mood · Awaiting"], id: \.self) { label in
                    Text(label).font(.system(size: 11)).foregroundStyle(text)
                        .padding(.horizontal, 12).padding(.vertical, 8)
                        .background(Color(hex: "#161B26"))
                        .overlay(Capsule().stroke(pink.opacity(0.3)))
                        .clipShape(Capsule())
                }
            }
        }
    }

    private var intelligence: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 8) {
                Text("INTELLIGENCE").font(.system(size: 11, weight: .bold)).foregroundStyle(dim)
                Text(activities.isEmpty ? "AWAITING" : "ACTIVE")
                    .font(.system(size: 9, weight: .bold))
                    .foregroundStyle(Color(hex: "#10B981"))
                    .padding(.horizontal, 6).padding(.vertical, 2)
                    .background(Color(hex: "#10B981").opacity(0.2)).clipShape(RoundedRectangle(cornerRadius: 6))
            }
            Text(
                activities.isEmpty
                    ? "Log connections and support to unlock relationship intelligence."
                    : "Recent relationship logs are flowing into this moment’s pulse."
            )
            .font(.system(size: 13)).foregroundStyle(text)
        }
        .padding(14)
        .background(card)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color(hex: "#7C5CFC").opacity(0.3)))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var aiSoon: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("✨ AI Insights").font(.system(size: 14, weight: .semibold)).foregroundStyle(text)
            Text("Coming Soon").font(.system(size: 12, weight: .bold)).foregroundStyle(pink)
            Text("Momentra will soon analyze recent activity to uncover emerging relationship patterns.")
                .font(.system(size: 12)).foregroundStyle(muted)
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(hex: "#161B26"))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.08)))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var launcher: some View {
        HStack {
            ForEach([
                ("💬", RelationshipsQuickAddKind.connection),
                ("🫶", .shared),
                ("📅", .investment),
                ("⚡", .support),
                ("🙂", .adjust),
            ], id: \.0) { emoji, kind in
                Text(emoji)
                    .font(.system(size: 20))
                    .frame(width: 52, height: 52)
                    .background(card)
                    .overlay(RoundedRectangle(cornerRadius: 16).stroke(pink.opacity(0.25)))
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                    .onTapGesture { onRelationshipsQuickAdd(kind) }
            }
        }
        .frame(maxWidth: .infinity)
    }
}
