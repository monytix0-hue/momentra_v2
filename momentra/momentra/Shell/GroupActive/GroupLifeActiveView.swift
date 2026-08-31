import SwiftUI

/// Figma 1267:11212 — Group Life intelligence (shared across all group moment types).
enum GroupLifeQuickAction {
    case experience, purchase, living, goal, community
}

struct GroupLifeActiveView: View {
    let refreshToken: UInt64
    let momentId: String?
    let momentTitle: String?
    var onQuickAction: (GroupLifeQuickAction) -> Void = { _ in }

    @State private var life: APIClient.GroupLifePayload?
    @State private var loading = true
    @State private var error: String?

    private let bg = Color(hex: "#131313")
    private let card = Color(hex: "#1C1926")
    private let border = Color(hex: "#2A2733")
    private let text = Color(hex: "#E5E0EE")
    private let muted = Color(hex: "#9CA3AF")
    private let secondary = Color(hex: "#C9C4D8")
    private let orange = Color(hex: "#F97316")
    private let yellow = Color(hex: "#EAB308")
    private let teal = Color(hex: "#14B8A6")
    private let green = Color(hex: "#22C55E")
    private let purple = Color(hex: "#A855F7")
    private let blue = Color(hex: "#6366F1")

    private var payload: APIClient.GroupLifePayload.LifeInner? { life?.payload }

    var body: some View {
        Group {
            if loading && life == nil {
                ProgressView().tint(teal)
            } else {
                ScrollView {
                    VStack(alignment: .leading, spacing: 16) {
                        if let error {
                            Text(error)
                                .font(.caption)
                                .foregroundStyle(Color(hex: "#F87171"))
                        }
                        if let momentTitle, !momentTitle.isEmpty {
                            Text(momentTitle)
                                .font(.plusJakarta(size: 11, weight: .semibold))
                                .foregroundStyle(secondary)
                        }
                        Text("Group Life")
                            .font(.plusJakarta(size: 20, weight: .heavy))
                            .foregroundStyle(text)

                        radarCard

                        sectionLabel("AT A GLANCE")
                        HStack(spacing: 6) {
                            glanceChip("EXP", payload?.domains?.experience, orange)
                            glanceChip("PUR", payload?.domains?.purchase, yellow)
                            glanceChip("LIV", payload?.domains?.living, teal)
                            glanceChip("GOL", payload?.domains?.goal, green)
                            glanceChip("COM", payload?.domains?.community, purple)
                        }

                        sectionLabel("BALANCE MODEL")
                        lifeCard {
                            balanceRow("PARTICIPATION", payload?.balance?.participation, teal)
                            balanceRow("CONTRIBUTION", payload?.balance?.contribution, orange)
                            balanceRow("COORDINATION", payload?.balance?.coordination, blue)
                            balanceRow("PROGRESS", payload?.balance?.progress, yellow)
                            balanceRow("COMMUNITY", payload?.balance?.community, teal)
                        }

                        sectionLabel("WHAT DRIVES YOUR GROUP")
                        if let drivers = payload?.drivers, !drivers.isEmpty {
                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(spacing: 10) {
                                    ForEach(drivers) { d in
                                        VStack(alignment: .leading, spacing: 6) {
                                            Text(d.title ?? d.domain ?? "")
                                                .font(.plusJakarta(size: 13, weight: .bold))
                                                .foregroundStyle(text)
                                                .lineLimit(2)
                                            Text(d.detail ?? "")
                                                .font(.plusJakarta(size: 11))
                                                .foregroundStyle(secondary)
                                                .lineLimit(4)
                                        }
                                        .padding(12)
                                        .frame(width: 200, alignment: .leading)
                                        .background(card)
                                        .overlay(RoundedRectangle(cornerRadius: 14).stroke(border, lineWidth: 1))
                                        .clipShape(RoundedRectangle(cornerRadius: 14))
                                    }
                                }
                            }
                        } else {
                            honestEmpty("Driver insights appear when domain scores diverge.")
                        }

                        HStack(spacing: 10) {
                            honestEmpty("Group drift alerts need a longer activity history.", eyebrow: "GROUP DRIFT")
                            honestEmpty("Highest leverage unlocks with analytics history.", eyebrow: "HIGHEST LEVERAGE")
                        }

                        sectionLabel("EVOLUTION")
                        honestEmpty("Sparklines need month-over-month history (coming soon).")

                        sectionLabel("WHAT CHANGED THIS MONTH")
                        honestEmpty("Monthly deltas require historical snapshots.")

                        sectionLabel("GROUP JOURNEY")
                        honestEmpty("Journey timeline lights up as multi-moment history accumulates.")

                        sectionLabel("MOMENTRA INTELLIGENCE")
                        honestEmpty("AI narrative insights are not available yet.")

                        sectionLabel("QUICK ACTIONS")
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 8) {
                                quickPill("Experience", orange) { onQuickAction(.experience) }
                                quickPill("Purchase", yellow) { onQuickAction(.purchase) }
                                quickPill("Living", teal) { onQuickAction(.living) }
                                quickPill("Goal", green) { onQuickAction(.goal) }
                                quickPill("Community", purple) { onQuickAction(.community) }
                            }
                        }

                        if let items = payload?.planningItems, !items.isEmpty {
                            sectionLabel("OPEN PLANS")
                            ForEach(items.indices, id: \.self) { i in
                                listRow(items[i].title ?? items[i].planningItemId ?? "")
                            }
                        }
                        if let items = payload?.bookings, !items.isEmpty {
                            sectionLabel("BOOKINGS")
                            ForEach(items.indices, id: \.self) { i in
                                listRow(items[i].title ?? items[i].bookingId ?? "")
                            }
                        }
                        if let items = payload?.updates, !items.isEmpty {
                            sectionLabel("UPDATES")
                            ForEach(Array(items.prefix(10).enumerated()), id: \.offset) { _, item in
                                listRow(item.message ?? item.updateId ?? "")
                            }
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                    .padding(.bottom, 56)
                }
            }
        }
        .background(bg)
        .task(id: "\(refreshToken)-\(momentId ?? "")") { await load() }
    }

    private var radarCard: some View {
        let healthLabel = payload?.health?.label ?? "—"
        let healthScore = payload?.health?.score
        let living = payload?.domains?.living
        let community = payload?.domains?.community
        let goal = payload?.domains?.goal
        let purchase = payload?.domains?.purchase
        let experience = payload?.domains?.experience
        let scores: [CGFloat] = [
            CGFloat(living?.score ?? 0) / 100,
            CGFloat(community?.score ?? 0) / 100,
            CGFloat(goal?.score ?? 0) / 100,
            CGFloat(purchase?.score ?? 0) / 100,
            CGFloat(experience?.score ?? 0) / 100,
        ]
        let accents = [teal, purple, green, yellow, orange]
        let labels: [(String, String, Color)] = [
            ("LIVING", living?.label ?? "—", teal),
            ("COMMUNITY", community?.label ?? "—", purple),
            ("GOAL", goal?.label ?? "—", green),
            ("PURCHASE", purchase?.label ?? "—", yellow),
            ("EXPERIENCE", experience?.label ?? "—", orange),
        ]

        return ZStack {
            RoundedRectangle(cornerRadius: 18)
                .fill(card)
                .overlay(RoundedRectangle(cornerRadius: 18).stroke(border, lineWidth: 1))
            LifeRadarCanvas(scores: scores, accents: accents, grid: border, fill: purple)
                .padding(28)
            VStack(spacing: 2) {
                Text(healthLabel)
                    .font(.plusJakarta(size: 36, weight: .heavy))
                    .foregroundStyle(text)
                Text("HEALTH SCORE")
                    .font(.plusJakarta(size: 10, weight: .bold))
                    .foregroundStyle(muted)
                if healthScore == nil {
                    Text("No signal yet")
                        .font(.plusJakarta(size: 11))
                        .foregroundStyle(secondary)
                        .padding(.top, 2)
                }
            }
            VStack {
                domainCorner(labels[0])
                Spacer()
                HStack {
                    domainCorner(labels[4])
                    Spacer()
                    domainCorner(labels[1])
                }
                Spacer()
                HStack {
                    domainCorner(labels[3])
                    Spacer()
                    domainCorner(labels[2])
                }
            }
            .padding(10)
        }
        .frame(height: 280)
    }

    private func domainCorner(_ item: (String, String, Color)) -> some View {
        VStack(spacing: 1) {
            Text(item.0)
                .font(.plusJakarta(size: 9, weight: .bold))
                .foregroundStyle(item.2)
            Text(item.1)
                .font(.plusJakarta(size: 12, weight: .bold))
                .foregroundStyle(text)
        }
    }

    private func sectionLabel(_ t: String) -> some View {
        Text(t)
            .font(.plusJakarta(size: 10, weight: .bold))
            .foregroundStyle(muted)
            .tracking(0.8)
    }

    private func lifeCard<Content: View>(@ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            content()
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(card)
        .overlay(RoundedRectangle(cornerRadius: 14).stroke(border, lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }

    private func honestEmpty(_ body: String, eyebrow: String? = nil) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            if let eyebrow {
                Text(eyebrow)
                    .font(.plusJakarta(size: 10, weight: .bold))
                    .foregroundStyle(muted)
            }
            Text(body)
                .font(.plusJakarta(size: 12))
                .foregroundStyle(secondary)
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(card)
        .overlay(RoundedRectangle(cornerRadius: 14).stroke(border, lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }

    private func glanceChip(_ code: String, _ metric: APIClient.GroupLifePayload.DomainMetric?, _ accent: Color) -> some View {
        VStack(spacing: 6) {
            Rectangle().fill(accent).frame(height: 2)
            Text(code)
                .font(.plusJakarta(size: 9))
                .foregroundStyle(muted)
            Text(metric?.label ?? "—")
                .font(.plusJakarta(size: 14, weight: .bold))
                .foregroundStyle(text)
        }
        .padding(.vertical, 8)
        .padding(.horizontal, 4)
        .frame(maxWidth: .infinity)
        .background(card)
        .overlay(RoundedRectangle(cornerRadius: 10).stroke(border, lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 10))
    }

    private func balanceRow(_ name: String, _ bar: APIClient.GroupLifePayload.BalanceBar?, _ accent: Color) -> some View {
        let value = bar?.value
        return VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(name)
                    .font(.plusJakarta(size: 10))
                    .foregroundStyle(secondary)
                Spacer()
                Text(value == nil ? "—" : (bar?.label ?? "\(value!)"))
                    .font(.plusJakarta(size: 11, weight: .semibold))
                    .foregroundStyle(text)
            }
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule().fill(border)
                    Capsule()
                        .fill(value == nil ? border : accent)
                        .frame(width: geo.size.width * CGFloat(min(max(value ?? 0, 0), 100)) / 100)
                }
            }
            .frame(height: 6)
        }
    }

    private func quickPill(_ label: String, _ accent: Color, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(label)
                .font(.plusJakarta(size: 12, weight: .semibold))
                .foregroundStyle(text)
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .background(accent.opacity(0.18))
                .overlay(Capsule().stroke(accent.opacity(0.45), lineWidth: 1))
                .clipShape(Capsule())
        }
        .buttonStyle(.plain)
    }

    private func listRow(_ t: String) -> some View {
        Text(t)
            .font(.plusJakarta(size: 13))
            .foregroundStyle(secondary)
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(card)
            .clipShape(RoundedRectangle(cornerRadius: 10))
    }

    private func load() async {
        guard let momentId else {
            loading = false
            error = "Select a Group Moment."
            return
        }
        error = nil
        if let cached = GroupTabDataCache.peekLife(momentId) {
            life = cached
            loading = false
        }
        if life == nil, GroupTabDataCache.peekPulse(momentId) != nil {
            loading = false
        } else if life == nil {
            loading = true
        }
        do {
            let loaded = try await APIClient.shared.getGroupLife(momentId: momentId)
            life = loaded
            GroupTabDataCache.putLife(momentId, loaded)
        } catch {
            self.error = error.localizedDescription
        }
        loading = false
    }
}

private struct LifeRadarCanvas: View {
    let scores: [CGFloat]
    let accents: [Color]
    let grid: Color
    let fill: Color

    var body: some View {
        Canvas { context, size in
            let cx = size.width / 2
            let cy = size.height / 2
            let r = min(size.width, size.height) / 2
            let n = 5
            func point(_ i: Int, scale: CGFloat) -> CGPoint {
                let angle = (-90.0 + Double(i) * 72.0) * .pi / 180
                return CGPoint(x: cx + r * scale * CGFloat(cos(angle)), y: cy + r * scale * CGFloat(sin(angle)))
            }
            for ring in [CGFloat(0.35), 0.65, 1] {
                var path = Path()
                for i in 0..<n {
                    let p = point(i, scale: ring)
                    if i == 0 { path.move(to: p) } else { path.addLine(to: p) }
                }
                path.closeSubpath()
                context.stroke(path, with: .color(grid), lineWidth: 1.5)
            }
            for i in 0..<n {
                var spoke = Path()
                spoke.move(to: CGPoint(x: cx, y: cy))
                spoke.addLine(to: point(i, scale: 1))
                context.stroke(spoke, with: .color(grid), lineWidth: 1)
            }
            var data = Path()
            for i in 0..<n {
                let scale = 0.12 + scores[i] * 0.88
                let p = point(i, scale: scale)
                if i == 0 { data.move(to: p) } else { data.addLine(to: p) }
            }
            data.closeSubpath()
            context.fill(data, with: .color(fill.opacity(0.22)))
            context.stroke(data, with: .color(fill.opacity(0.85)), lineWidth: 2.5)
            for i in 0..<n {
                let scale = 0.12 + scores[i] * 0.88
                let p = point(i, scale: scale)
                let rect = CGRect(x: p.x - 4, y: p.y - 4, width: 8, height: 8)
                context.fill(Path(ellipseIn: rect), with: .color(accents[i]))
            }
        }
    }
}
