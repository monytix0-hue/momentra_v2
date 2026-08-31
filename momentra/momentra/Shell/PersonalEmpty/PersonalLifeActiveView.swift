import SwiftUI

/// Figma `1047:7689` body — Personal Life populated (cross-moment).
struct PersonalLifeActiveView: View {
    let refreshToken: UInt64
    var onLogRecovery: () -> Void = {}

    @State private var life: APIClient.PersonalLifePayload?
    @State private var loading = true
    @State private var error: String?
    @State private var selectedChip = "Life Health"

    private let bg = Color(red: 0.078, green: 0.071, blue: 0.106)
    private let card = Color(red: 0.110, green: 0.106, blue: 0.180)
    private let cardAlt = Color(red: 0.086, green: 0.106, blue: 0.149)
    private let text = Color(red: 0.898, green: 0.878, blue: 0.933)
    private let muted = Color(red: 0.788, green: 0.769, blue: 0.847)
    private let dim = Color(red: 0.549, green: 0.549, blue: 0.620)
    private let purple = Color(red: 0.486, green: 0.361, blue: 0.988)
    private let green = Color(red: 0.063, green: 0.725, blue: 0.506)
    private let red = Color(red: 0.937, green: 0.267, blue: 0.267)
    private let amber = Color(red: 0.961, green: 0.620, blue: 0.043)
    private let blue = Color(red: 0.231, green: 0.510, blue: 0.965)
    private let pink = Color(red: 0.882, green: 0.165, blue: 0.620)

    var body: some View {
        Group {
            if loading && life == nil {
                ProgressView().tint(purple).frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                ScrollView {
                    VStack(spacing: 0) {
                        chipRow
                        VStack(spacing: 16) {
                            if (life?.dataQuality ?? "FIGMA_SEEDED").uppercased() == "FIGMA_SEEDED" {
                                Text("Life sections are provisional (API_GAP). Only active area count is live. Seeded scores are layout reference — not production metrics.")
                                    .font(.system(size: 11))
                                    .foregroundStyle(amber)
                                    .padding(12)
                                    .frame(maxWidth: .infinity, alignment: .leading)
                                    .background(card)
                                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(amber.opacity(0.35), lineWidth: 1))
                                    .clipShape(RoundedRectangle(cornerRadius: 12))
                            }
                            if let error {
                                Text(error).font(.system(size: 12)).foregroundStyle(red)
                            }
                            healthSummary
                            driftCard
                            leverageCard
                            balanceSection
                            emotionalTrendCard
                            dominantEmotionCard
                            happyDriversCard
                            journeyCard
                            aiInsightsCard
                            Spacer().frame(height: 24)
                        }
                        .padding(16)
                    }
                }
                .background(bg)
            }
        }
        .task(id: refreshToken) { await load() }
    }

    private func load() async {
        error = nil
        if let cached = PersonalTabDataCache.peekLife() {
            life = cached
            loading = false
        } else if life != nil {
            loading = false
        } else {
            loading = true
        }
        do {
            let payload = try await APIClient.shared.getPersonalLife()
            life = payload
            PersonalTabDataCache.putLife(payload)
        } catch {
            self.error = error.localizedDescription
        }
        loading = false
    }

    // MARK: - Chips

    private var chipRow: some View {
        VStack(spacing: 0) {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                    chip("Life Health", color: purple)
                    chip("Future Building", color: blue)
                    chip("Lifestyle", color: amber)
                    Spacer(minLength: 8)
                    Text("⚙")
                        .font(.system(size: 12))
                        .foregroundStyle(Color(red: 0.5, green: 0.5, blue: 0.58))
                        .frame(width: 32, height: 32)
                        .background(cardAlt)
                        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.08)))
                        .clipShape(RoundedRectangle(cornerRadius: 16))
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 10)
            }
            Rectangle().fill(Color.white.opacity(0.06)).frame(height: 1)
        }
        .background(Color(red: 0.047, green: 0.059, blue: 0.082))
    }

    private func chip(_ label: String, color: Color) -> some View {
        let active = selectedChip == label
        return HStack(spacing: 6) {
            Circle().fill(color).frame(width: 6, height: 6)
            Text(label)
                .font(.system(size: active ? 12 : 11, weight: active ? .semibold : .medium))
                .foregroundStyle(active ? Color.white : dim)
        }
        .padding(.horizontal, active ? 12 : 10)
        .padding(.vertical, 6)
        .background(active ? purple.opacity(0.12) : cardAlt)
        .overlay(
            RoundedRectangle(cornerRadius: 20)
                .stroke(active ? purple.opacity(0.5) : Color.white.opacity(0.08), lineWidth: active ? 1.5 : 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .onTapGesture { selectedChip = label }
    }

    // MARK: - Sections

    private var healthSummary: some View {
        VStack(alignment: .leading, spacing: 20) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("PERSONAL LIFE HEALTH")
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundStyle(dim)
                    HStack(alignment: .bottom, spacing: 2) {
                        Text("\(life?.score ?? 82)")
                            .font(.system(size: 48, weight: .bold))
                            .foregroundStyle(text)
                        Text("/\(life?.scoreMax ?? 100)")
                            .font(.system(size: 16))
                            .foregroundStyle(muted)
                            .padding(.bottom, 10)
                    }
                    Text(life?.statusLabel ?? "Stable and Growing")
                        .font(.system(size: 14))
                        .foregroundStyle(text)
                    Text(life?.trendLabel ?? "▲ +6 this month")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(green)
                }
                Spacer()
                scoreRing
            }
            if let insight = life?.insight, !insight.isEmpty {
                Text("\"\(insight)\"")
                    .font(.system(size: 13))
                    .foregroundStyle(muted)
            }
            let areas = life?.areaScores ?? []
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 8) {
                ForEach(areas, id: \.code) { area in
                    HStack(spacing: 8) {
                        Circle().fill(Color(hex: area.color)).frame(width: 8, height: 8)
                        Text("\(area.label): \(area.score)")
                            .font(.system(size: 12))
                            .foregroundStyle(text)
                    }
                    .padding(10)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(cardAlt.opacity(0.6))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }
            }
        }
        .padding(20)
        .background(
            ZStack(alignment: .bottomTrailing) {
                card
                Circle()
                    .fill(Color(red: 0.486, green: 0.227, blue: 0.929).opacity(0.18))
                    .frame(width: 160, height: 160)
                    .blur(radius: 30)
                    .offset(x: 40, y: 40)
            }
        )
        .overlay(RoundedRectangle(cornerRadius: 24).stroke(Color.white.opacity(0.08)))
        .clipShape(RoundedRectangle(cornerRadius: 24))
    }

    private var scoreRing: some View {
        let colors = (life?.areaScores ?? []).map { Color(hex: $0.color) }
        let palette = colors.isEmpty ? [blue, green, amber, pink] : colors
        let score = life?.score ?? 82
        return ZStack {
            ForEach(Array(palette.enumerated()), id: \.offset) { i, c in
                Circle()
                    .trim(from: 0, to: 0.72)
                    .stroke(c.opacity(0.85), style: StrokeStyle(lineWidth: 8, lineCap: .round))
                    .rotationEffect(.degrees(-90 + Double(i) * 20))
                    .padding(CGFloat(i) * 12)
            }
            Text("\(score)")
                .font(.system(size: 18, weight: .bold))
                .foregroundStyle(text)
        }
        .frame(width: 110, height: 110)
    }

    @ViewBuilder
    private var driftCard: some View {
        if let drift = life?.drift {
            VStack(alignment: .leading, spacing: 12) {
                Text(drift.title)
                    .font(.system(size: 11, weight: .bold))
                    .foregroundStyle(red)
                Text(drift.headline)
                    .font(.system(size: 18, weight: .bold))
                    .foregroundStyle(text)
                Text(drift.body)
                    .font(.system(size: 13))
                    .foregroundStyle(muted)
                Text(drift.ctaLabel)
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(red)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 10)
                    .background(Color(red: 0.102, green: 0.071, blue: 0.094))
                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(red.opacity(0.25)))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
            }
            .padding(20)
            .background(Color(red: 0.165, green: 0.082, blue: 0.125))
            .overlay(RoundedRectangle(cornerRadius: 20).stroke(red.opacity(0.35)))
            .shadow(color: red.opacity(0.28), radius: 16)
            .clipShape(RoundedRectangle(cornerRadius: 20))
        }
    }

    @ViewBuilder
    private var leverageCard: some View {
        if let lev = life?.leverage {
            VStack(alignment: .leading, spacing: 14) {
                HStack(spacing: 6) {
                    Text("🎯")
                    Text(lev.title)
                        .font(.system(size: 11, weight: .bold))
                        .foregroundStyle(green)
                }
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(lev.actionTitle)
                            .font(.system(size: 18, weight: .bold))
                            .foregroundStyle(text)
                        Text(lev.actionBody)
                            .font(.system(size: 12))
                            .foregroundStyle(muted)
                    }
                    Spacer()
                    Text(lev.ctaLabel)
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(green)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(green.opacity(0.15))
                        .overlay(RoundedRectangle(cornerRadius: 10).stroke(green.opacity(0.4)))
                        .clipShape(RoundedRectangle(cornerRadius: 10))
                        .onTapGesture(perform: onLogRecovery)
                }
                Rectangle().fill(Color.white.opacity(0.08)).frame(height: 1)
                Text("EXPECTED IMPACT")
                    .font(.system(size: 10, weight: .semibold))
                    .foregroundStyle(dim)
                HStack {
                    ForEach(lev.impacts ?? [], id: \.label) { impact in
                        VStack(alignment: .leading, spacing: 4) {
                            Text(impact.label).font(.system(size: 11)).foregroundStyle(dim)
                            Text(impact.delta)
                                .font(.system(size: 16, weight: .bold))
                                .foregroundStyle(impact.tone == "up" ? green : impact.tone == "down" ? red : muted)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                    }
                }
            }
            .padding(20)
            .background(card)
            .overlay(RoundedRectangle(cornerRadius: 20).stroke(green.opacity(0.25)))
            .clipShape(RoundedRectangle(cornerRadius: 20))
        }
    }

    @ViewBuilder
    private var balanceSection: some View {
        let axes = life?.balance ?? []
        if !axes.isEmpty {
            VStack(alignment: .leading, spacing: 12) {
                Text("Life Balance Model")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(text)
                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                    ForEach(axes, id: \.code) { axis in
                        let badgeColor: Color = {
                            switch axis.badgeTone {
                            case "amber": return amber
                            case "green": return green
                            case "blue": return blue
                            case "pink": return pink
                            default: return purple
                            }
                        }()
                        VStack(alignment: .leading, spacing: 8) {
                            HStack {
                                Text(axis.label)
                                    .font(.system(size: 10, weight: .bold))
                                    .foregroundStyle(dim)
                                Spacer()
                                Text(axis.badge)
                                    .font(.system(size: 8, weight: .bold))
                                    .foregroundStyle(badgeColor)
                                    .padding(.horizontal, 6)
                                    .padding(.vertical, 2)
                                    .background(badgeColor.opacity(0.15))
                                    .clipShape(RoundedRectangle(cornerRadius: 6))
                            }
                            Text("\(axis.score)")
                                .font(.system(size: 24, weight: .bold))
                                .foregroundStyle(text)
                        }
                        .padding(14)
                        .background(cardAlt)
                        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.08)))
                        .clipShape(RoundedRectangle(cornerRadius: 16))
                    }
                }
            }
        }
    }

    @ViewBuilder
    private var emotionalTrendCard: some View {
        if let trend = life?.emotionalTrend {
            VStack(alignment: .leading, spacing: 12) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Emotional Trend")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(text)
                    Text(trend.subtitle)
                        .font(.system(size: 12))
                        .foregroundStyle(dim)
                }
                EmotionalTrendChartView(series: trend.series ?? [])
                    .frame(height: 120)
                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 8) {
                    ForEach(trend.series ?? [], id: \.code) { s in
                        HStack(spacing: 6) {
                            Circle().fill(Color(hex: s.color)).frame(width: 6, height: 6)
                            Text(s.label).font(.system(size: 11)).foregroundStyle(muted)
                        }
                    }
                }
            }
            .padding(16)
            .background(card)
            .overlay(RoundedRectangle(cornerRadius: 20).stroke(Color.white.opacity(0.08)))
            .clipShape(RoundedRectangle(cornerRadius: 20))
        }
    }

    @ViewBuilder
    private var dominantEmotionCard: some View {
        if let dom = life?.dominantEmotion {
            VStack(alignment: .leading, spacing: 12) {
                Text(dom.title)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(text)
                HStack(spacing: 16) {
                    DominantDonutView(segments: dom.segments ?? [])
                        .frame(width: 80, height: 80)
                    VStack(alignment: .leading, spacing: 8) {
                        Text(dom.headline)
                            .font(.system(size: 13))
                            .foregroundStyle(text)
                        HStack(spacing: 10) {
                            ForEach(Array((dom.segments ?? []).filter { $0.label != "Connection" && $0.label != "Other" }.prefix(3)), id: \.label) { seg in
                                Text("\(seg.label) (\(seg.percent)%)")
                                    .font(.system(size: 11))
                                    .foregroundStyle(dim)
                            }
                        }
                    }
                }
            }
            .padding(16)
            .background(card)
            .overlay(RoundedRectangle(cornerRadius: 20).stroke(Color.white.opacity(0.08)))
            .clipShape(RoundedRectangle(cornerRadius: 20))
        }
    }

    @ViewBuilder
    private var happyDriversCard: some View {
        if let happy = life?.happyDrivers {
            VStack(alignment: .leading, spacing: 12) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(happy.title)
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(text)
                    Text(happy.subtitle)
                        .font(.system(size: 12))
                        .foregroundStyle(dim)
                }
                ForEach(happy.items ?? [], id: \.self) { item in
                    HStack(spacing: 10) {
                        Text("✨")
                            .font(.system(size: 10))
                            .frame(width: 22, height: 22)
                            .background(purple.opacity(0.15))
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                        Text(item).font(.system(size: 13)).foregroundStyle(text)
                    }
                    .padding(.vertical, 4)
                }
            }
            .padding(16)
            .background(card)
            .overlay(RoundedRectangle(cornerRadius: 20).stroke(Color.white.opacity(0.08)))
            .clipShape(RoundedRectangle(cornerRadius: 20))
        }
    }

    @ViewBuilder
    private var journeyCard: some View {
        if let journey = life?.journey {
            VStack(alignment: .leading, spacing: 12) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(journey.title)
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(text)
                    Text(journey.subtitle)
                        .font(.system(size: 12))
                        .foregroundStyle(dim)
                }
                ForEach(Array((journey.items ?? []).enumerated()), id: \.offset) { _, item in
                    HStack(spacing: 12) {
                        Text(item.icon)
                            .frame(width: 36, height: 36)
                            .background(cardAlt)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                        VStack(alignment: .leading, spacing: 2) {
                            Text(item.title).font(.system(size: 13)).foregroundStyle(text)
                            Text(item.whenLabel ?? "").font(.system(size: 11)).foregroundStyle(dim)
                        }
                        Spacer()
                        let tone: Color = item.tone == "up" ? green : item.tone == "down" ? red : muted
                        Text(item.value)
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundStyle(tone)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(tone.opacity(0.12))
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                    }
                }
            }
            .padding(16)
            .background(card)
            .overlay(RoundedRectangle(cornerRadius: 20).stroke(Color.white.opacity(0.08)))
            .clipShape(RoundedRectangle(cornerRadius: 20))
        }
    }

    @ViewBuilder
    private var aiInsightsCard: some View {
        if let ai = life?.aiInsights {
            VStack(alignment: .leading, spacing: 12) {
                HStack(spacing: 6) {
                    Text("✨")
                    Text(ai.title)
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(text)
                }
                Text(ai.lead)
                    .font(.system(size: 13))
                    .foregroundStyle(text)
                Text(ai.body)
                    .font(.system(size: 12))
                    .foregroundStyle(muted)
            }
            .padding(20)
            .background(
                LinearGradient(colors: [Color(red: 0.118, green: 0.102, blue: 0.196), card], startPoint: .topLeading, endPoint: .bottomTrailing)
            )
            .overlay(RoundedRectangle(cornerRadius: 20).stroke(purple.opacity(0.25)))
            .clipShape(RoundedRectangle(cornerRadius: 20))
        }
    }
}

private struct EmotionalTrendChartView: View {
    let series: [APIClient.PersonalLifePayload.LifeEmotionSeries]

    var body: some View {
        GeometryReader { geo in
            ZStack {
                ForEach([0.25, 0.5, 0.75], id: \.self) { f in
                    Path { p in
                        let y = geo.size.height * f
                        p.move(to: CGPoint(x: 0, y: y))
                        p.addLine(to: CGPoint(x: geo.size.width, y: y))
                    }
                    .stroke(Color.white.opacity(0.06), lineWidth: 1)
                }
                ForEach(series, id: \.code) { s in
                    let pts = s.points ?? []
                    if pts.count >= 2 {
                        Path { path in
                            for (i, v) in pts.enumerated() {
                                let x = geo.size.width * CGFloat(i) / CGFloat(max(pts.count - 1, 1))
                                let y = geo.size.height * (1 - CGFloat(v / 100.0).clamped(to: 0...1))
                                if i == 0 { path.move(to: CGPoint(x: x, y: y)) }
                                else { path.addLine(to: CGPoint(x: x, y: y)) }
                            }
                        }
                        .stroke(Color(hex: s.color), style: StrokeStyle(lineWidth: 2, lineCap: .round, lineJoin: .round))
                    }
                }
            }
        }
    }
}

private struct DominantDonutView: View {
    let segments: [APIClient.PersonalLifePayload.LifeEmotionSegment]

    var body: some View {
        let total = max(segments.reduce(0) { $0 + $1.percent }, 1)
        Canvas { context, size in
            var start = Angle.degrees(-90)
            let stroke: CGFloat = 12
            let rect = CGRect(x: stroke / 2, y: stroke / 2, width: size.width - stroke, height: size.height - stroke)
            for seg in segments {
                let sweep = Angle.degrees(360 * Double(seg.percent) / Double(total))
                var path = Path()
                path.addArc(center: CGPoint(x: size.width / 2, y: size.height / 2), radius: (size.width - stroke) / 2, startAngle: start, endAngle: start + sweep, clockwise: false)
                context.stroke(path, with: .color(Color(hex: seg.color)), style: StrokeStyle(lineWidth: stroke, lineCap: .butt))
                start = start + sweep
            }
            _ = rect
        }
    }
}

private extension Comparable {
    func clamped(to range: ClosedRange<Self>) -> Self {
        min(max(self, range.lowerBound), range.upperBound)
    }
}
