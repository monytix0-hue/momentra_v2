import SwiftUI

/// Figma `505:12365` — Lifestyle Vitality Index Pulse.
struct PersonalLifestylePulseActiveView: View {
    let refreshToken: UInt64
    let momentTitle: String?
    let momentId: String?
    var onAddExpense: () -> Void = {}
    var onLifestyleQuickAdd: (LifestyleQuickAddKind) -> Void = { _ in }
    var onViewAllActivity: () -> Void = {}

    @State private var loading = true
    @State private var error: String?
    @State private var pulse: APIClient.PersonalPulsePayload?

    private let bg = Color(hex: "#14121B")
    private let card = Color(hex: "#152022")
    private let teal = Color(hex: "#0EA5A4")
    private let tealSoft = Color(hex: "#5EEAD4")
    private let text = Color(hex: "#E5E0EE")
    private let muted = Color(hex: "#C9C4D8")
    private let dim = Color(hex: "#8C8C9E")

    private var vitalityScore: Int? { PersonalLifestyleDerived.vitalityIndex(pulse: pulse) }
    private var axes: PersonalLifestyleDerived.AxisScores { PersonalLifestyleDerived.axisScores(pulse: pulse) }
    private var spendPairs: [(String, String)] { PersonalLifestyleDerived.spendPairs(pulse: pulse) }

    var body: some View {
        Group {
            if loading && pulse == nil {
                ProgressView().tint(teal).frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                ScrollView {
                    VStack(spacing: 12) {
                        if let error { Text(error).font(.system(size: 12)).foregroundStyle(.red) }
                        if let momentTitle, !momentTitle.isEmpty {
                            Text(momentTitle).font(.system(size: 11, weight: .semibold)).foregroundStyle(muted)
                                .frame(maxWidth: .infinity, alignment: .leading)
                        }
                        vitalityHero
                        metricTiles
                        momentumCard
                        spendCard
                        protectCard
                        quickAddRow
                        Spacer().frame(height: 24)
                    }
                    .padding(.horizontal, 12)
                    .padding(.vertical, 10)
                }
                .background(bg)
            }
        }
        .task(id: "\(refreshToken)-\(momentId ?? "")") { await load() }
    }

    private func load() async {
        error = nil
        if let cached = PersonalTabDataCache.peekPulse(momentId: momentId) {
            pulse = cached.pulse
            loading = false
        } else {
            loading = true
        }
        do {
            let data = try await PersonalTabLoad.loadPulseTab(momentId: momentId)
            pulse = data.pulse
        } catch {
            self.error = error.localizedDescription
            pulse = nil
        }
        loading = false
    }

    private var vitalityHero: some View {
        VStack(spacing: 16) {
            Text("VITALITY INDEX").font(.system(size: 11, weight: .bold)).foregroundStyle(dim)
            HStack(alignment: .bottom, spacing: 2) {
                Text(vitalityScore.map(String.init) ?? "—")
                    .font(.system(size: 40, weight: .bold)).foregroundStyle(text)
                if vitalityScore != nil {
                    Text("/100").font(.system(size: 16)).foregroundStyle(muted).padding(.bottom, 8)
                }
            }
            Text(PersonalLifestyleDerived.networkStability(pulse: pulse))
                .font(.system(size: 13)).foregroundStyle(tealSoft)
            ZStack {
                Circle().stroke(teal.opacity(0.2), lineWidth: 14).frame(width: 140, height: 140)
                if let vitalityScore {
                    Circle()
                        .trim(from: 0, to: CGFloat(vitalityScore) / 100)
                        .stroke(teal, style: StrokeStyle(lineWidth: 14, lineCap: .round))
                        .rotationEffect(.degrees(-90))
                        .frame(width: 140, height: 140)
                    Text("\(vitalityScore)").font(.system(size: 28, weight: .bold)).foregroundStyle(text)
                } else {
                    Text("—").font(.system(size: 28, weight: .bold)).foregroundStyle(muted)
                }
            }
            HStack {
                ForEach(Array(zip(["Joy", "Fulfillment", "Vitality", "Exploration"], [axes.joy, axes.fulfillment, axes.vitality, axes.exploration])), id: \.0) { label, value in
                    VStack(spacing: 2) {
                        Text(label).font(.system(size: 10)).foregroundStyle(dim)
                        Text(value).font(.system(size: 14, weight: .bold)).foregroundStyle(text)
                    }
                    .frame(maxWidth: .infinity)
                }
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity)
        .background(LinearGradient(colors: [Color(hex: "#0D2A2A"), card], startPoint: .top, endPoint: .bottom))
        .overlay(RoundedRectangle(cornerRadius: 24).stroke(teal.opacity(0.25)))
        .clipShape(RoundedRectangle(cornerRadius: 24))
    }

    private var metricTiles: some View {
        VStack(spacing: 8) {
            HStack(spacing: 8) {
                metricTile("Joy", axes.joy)
                metricTile("Fulfillment", axes.fulfillment)
            }
            HStack(spacing: 8) {
                metricTile("Vitality", axes.vitality)
                metricTile("Exploration", axes.exploration)
            }
        }
    }

    private func metricTile(_ label: String, _ value: String) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(label).font(.system(size: 11, weight: .bold)).foregroundStyle(dim)
            Text(value).font(.system(size: 22, weight: .bold)).foregroundStyle(text)
            Text(PersonalLifeOpsDerived.statusBadge(forScore: value, axis: label))
                .font(.system(size: 10)).foregroundStyle(tealSoft)
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(hex: "#161B26"))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.08)))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var momentumCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("TODAY'S MOMENTUM").font(.system(size: 11, weight: .bold)).foregroundStyle(dim)
            let labels = [
                axes.joy == "—" ? "Joy pending" : "Joy Rising",
                PersonalLifestyleDerived.experienceCount(pulse: pulse) <= 0 ? "Ritual quiet" : "Experiences logged",
                pulse?.moodState?.isEmpty == false ? (pulse?.moodState ?? "Mood pending") : "Mood pending",
                spendPairs.isEmpty ? "Budget quiet" : "Budget active",
            ]
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 8) {
                ForEach(labels, id: \.self) { label in
                    Text(label)
                        .font(.system(size: 11))
                        .foregroundStyle(text)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                        .background(teal.opacity(0.12))
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                }
            }
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(card)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.08)))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var spendCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text("LIFESTYLE SPEND").font(.system(size: 11, weight: .bold)).foregroundStyle(dim)
                Spacer()
                Text("+ Add Expense").font(.system(size: 11, weight: .semibold)).foregroundStyle(teal)
                    .onTapGesture(perform: onAddExpense)
            }
            if spendPairs.isEmpty {
                Text("Not projected yet — log spend to surface a snapshot.")
                    .font(.system(size: 12)).foregroundStyle(muted)
            } else {
                ForEach(spendPairs, id: \.0) { currency, amount in
                    HStack {
                        Text(currency).foregroundStyle(muted)
                        Spacer()
                        Text(amount).fontWeight(.semibold).foregroundStyle(text)
                    }
                    .font(.system(size: 12))
                }
            }
        }
        .padding(14)
        .background(card)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.08)))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var protectCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Protect a ritual").font(.system(size: 16, weight: .bold)).foregroundStyle(text)
            Text("Log one experience to protect your lifestyle rhythm.")
                .font(.system(size: 13)).foregroundStyle(muted)
            Text("Log Experience")
                .font(.system(size: 15, weight: .bold))
                .foregroundStyle(bg)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .background(teal)
                .clipShape(Capsule())
                .onTapGesture { onLifestyleQuickAdd(.experience) }
        }
        .padding(16)
        .background(Color(hex: "#0D2A2A"))
        .overlay(RoundedRectangle(cornerRadius: 20).stroke(teal.opacity(0.45), lineWidth: 1.5))
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }

    private var quickAddRow: some View {
        HStack {
            ForEach([
                ("✨", LifestyleQuickAddKind.experience),
                ("🌿", LifestyleQuickAddKind.wellbeing),
                ("🔍", LifestyleQuickAddKind.discovery),
                ("🎨", LifestyleQuickAddKind.expression),
                ("⚙", LifestyleQuickAddKind.adjust),
            ], id: \.0) { emoji, kind in
                Text(emoji)
                    .font(.system(size: 20))
                    .frame(width: 52, height: 52)
                    .background(card)
                    .overlay(RoundedRectangle(cornerRadius: 16).stroke(teal.opacity(0.25)))
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                    .onTapGesture { onLifestyleQuickAdd(kind) }
            }
        }
    }
}
